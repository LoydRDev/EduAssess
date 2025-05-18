package eduassess.dao;

import eduassess.model.Course;
import eduassess.model.Instructor;
import eduassess.util.DatabaseConnection;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InstructorDAO {

    private String sql;
    private String courseName;
    private String instructorName;

    public boolean isCourseAssignedToInstructor(int instructorId, String courseCode) throws SQLException {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            String sql = "SELECT * FROM instructor_courses WHERE instructor_IDNumber = ? AND course_Code = ? AND status = 'Active'";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, instructorId);
            pstmt.setString(2, courseCode);
            ResultSet rs = pstmt.executeQuery();
            return rs.next();
        } finally {
            if (conn != null) {
                conn.close();
            }
        }
    }

    public boolean assignInstructorToCourse(int instructorId, String courseCode) {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            System.out.println("Database connection established: " + (conn != null && !conn.isClosed()));
            conn.setAutoCommit(false);
            System.out.println("Auto-commit set to false");

            // Verify if instructor exists
            String verifyInstructorSql = "SELECT * FROM instructor WHERE instructor_IDNumber = ?";
            PreparedStatement verifyStmt = conn.prepareStatement(verifyInstructorSql);
            verifyStmt.setInt(1, instructorId);
            ResultSet verifyRs = verifyStmt.executeQuery();

            if (!verifyRs.next()) {
                System.err.println("Instructor not found in database!");
                conn.rollback();
                return false;
            }

            // Check if course exists
            String courseSql = "SELECT * FROM courses WHERE course_Code = ?";
            PreparedStatement courseStmt = conn.prepareStatement(courseSql);
            courseStmt.setString(1, courseCode);
            ResultSet courseRs = courseStmt.executeQuery();

            if (!courseRs.next()) {
                System.err.println("Course not found!");
                conn.rollback();
                return false;
            }

            // Check if instructor is already assigned to this course
            String checkAssignSql = "SELECT * FROM instructor_courses WHERE instructor_IDNumber = ? AND course_Code = ? AND status = 'Active'";
            PreparedStatement checkStmt = conn.prepareStatement(checkAssignSql);
            checkStmt.setInt(1, instructorId);
            checkStmt.setString(2, courseCode);
            ResultSet checkRs = checkStmt.executeQuery();

            if (checkRs.next()) {
                System.err.println("Instructor is already assigned to this course!");
                conn.rollback();
                return false;
            }

            // Fetch instructor name
            String instructorName = getInstructorNameById(instructorId);
            if (instructorName == null) {
                System.err.println("Instructor name not found!");
                conn.rollback();
                return false;
            }

            // Fetch course name
            String courseName = getCourseNameByCourseCode(courseCode);
            if (courseName == null) {
                System.err.println("Course name not found!");
                conn.rollback();
                return false;
            }

            // Assign instructor to course with names
            String sql = "INSERT INTO instructor_courses (instructor_IDNumber, instructor_name, course_Code, course_Name, status, assigned_date) VALUES (?, ?, ?, ?, 'Active', CURRENT_TIMESTAMP)";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, instructorId);
            pstmt.setString(2, instructorName);
            pstmt.setString(3, courseCode);
            pstmt.setString(4, courseName);
            int rowsAffected = pstmt.executeUpdate();
            System.out.println("Rows affected by insert: " + rowsAffected);

            // Update courses table with instructor ID
            String updateCourseSql = "UPDATE courses SET instructor_IDNumber = ? WHERE course_Code = ?";
            PreparedStatement updateStmt = conn.prepareStatement(updateCourseSql);
            updateStmt.setInt(1, instructorId);
            updateStmt.setString(2, courseCode);
            int coursesUpdated = updateStmt.executeUpdate();
            System.out.println("Rows affected by course update: " + coursesUpdated);

            conn.commit();
            System.out.println("Transaction committed successfully");
            System.out.println("Connection status after commit: " + (conn != null && !conn.isClosed()));
            return true;

        } catch (SQLException e) {
            String errorMessage = String.format(
                    "Error during instructor assignment: %s (SQL State: %s, Error Code: %d)",
                    e.getMessage(), e.getSQLState(), e.getErrorCode());
            System.err.println("\n" + errorMessage);
            System.err.println("Failed SQL: " + sql);
            System.err.println(
                    "Parameters: " + instructorId + ", " + instructorName + ", " + courseCode + ", " + courseName);
            e.printStackTrace();

            try {
                if (conn != null) {
                    conn.rollback();
                    System.err.println("Transaction rolled back successfully");
                }
            } catch (SQLException ex) {
                System.err.println("Error during rollback: " + ex.getMessage());
                ex.printStackTrace();
            }
            return false;
        } finally {
            try {
                if (conn != null) {
                    conn.setAutoCommit(true);
                    conn.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public List<Instructor> getAllAssignedCourses() throws SQLException {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            String sql = "SELECT ic.instructor_IDNumber, ic.instructor_name, ic.course_Code, ic.course_Name " +
                    "FROM instructor_courses ic " +
                    "WHERE ic.status = 'Active'";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            ResultSet rs = pstmt.executeQuery();

            List<Instructor> assignedCourses = new ArrayList<>();
            while (rs.next()) {
                Instructor instructor = new Instructor(
                        rs.getInt("instructor_IDNumber"),
                        rs.getString("instructor_name"),
                        rs.getString("course_Code"),
                        rs.getString("course_Name"));
                assignedCourses.add(instructor);
            }
            return assignedCourses;
        } finally {
            if (conn != null) {
                conn.close();
            }
        }
    }

    public boolean unassignInstructorFromCourse(int instructorId, String courseCode) throws SQLException {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            if (conn == null || conn.isClosed()) {
                System.err.println("Database connection failed");
                return false;
            }
            conn.setAutoCommit(false);

            // Verify if the assignment exists and is active
            String verifySql = "SELECT * FROM instructor_courses WHERE instructor_IDNumber = ? AND course_Code = ? AND status = 'Active'";
            PreparedStatement verifyStmt = conn.prepareStatement(verifySql);
            verifyStmt.setInt(1, instructorId);
            verifyStmt.setString(2, courseCode);
            ResultSet verifyRs = verifyStmt.executeQuery();

            if (!verifyRs.next()) {
                System.err.println("No active assignment found for this instructor and course!");
                conn.rollback();
                return false;
            }

            // Unassign instructor from course and delete from instructor_course table
            String sql = "UPDATE instructor_courses SET status = 'Inactive' WHERE instructor_IDNumber = ? AND course_Code = ?";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, instructorId);
            pstmt.setString(2, courseCode);
            int rowsAffected = pstmt.executeUpdate();

            // Delete from instructor_course table
            String deleteSql = "DELETE FROM instructor_courses WHERE instructor_IDNumber = ? AND course_Code = ?";
            PreparedStatement deleteStmt = conn.prepareStatement(deleteSql);
            deleteStmt.setInt(1, instructorId);
            deleteStmt.setString(2, courseCode);
            deleteStmt.executeUpdate();

            // Clear instructor from courses table
            String updateCourseSql = "UPDATE courses SET instructor_IDNumber = NULL WHERE course_Code = ? AND instructor_IDNumber = ?";
            PreparedStatement updateStmt = conn.prepareStatement(updateCourseSql);
            updateStmt.setString(1, courseCode);
            updateStmt.setInt(2, instructorId);
            updateStmt.executeUpdate();

            if (rowsAffected <= 0) {
                conn.rollback();
                return false;
            }

            conn.commit();

            // Refresh instructor data after successful unassignment
            List<Instructor> updatedInstructors = getAllAssignedCourses();
            return true;
        } catch (SQLException e) {
            System.err.println("SQL Error during unassignment: " + e.getMessage());
            try {
                if (conn != null)
                    conn.rollback();
            } catch (SQLException ex) {
                System.err.println("Rollback failed: " + ex.getMessage());
            }
            return false;
        } finally {
            try {
                if (conn != null) {
                    conn.setAutoCommit(true);
                    conn.close();
                }
            } catch (SQLException e) {
                System.err.println("Error closing connection: " + e.getMessage());
            }
        }
    }

    public List<Instructor> getCoursesForInstructor(int instructorId) throws SQLException {
        List<Instructor> instructor = new ArrayList<>();

        String sql = "SELECT ic.course_Code, ic.course_Name, ic.instructor_name, ic.instructor_IDNumber " +
                "FROM instructor_courses ic " +
                "WHERE ic.instructor_IDNumber = ?";

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, instructorId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Instructor instructors = new Instructor(
                        rs.getInt("instructor_IDNumber"),
                        rs.getString("instructor_name"),
                        rs.getString("course_Code"),
                        rs.getString("course_Name"));
                instructor.add(instructors);
            }
        }
        return instructor;
    }

    public List<Map<String, String>> getCourseInstructors(String courseCode) {
        List<Map<String, String>> instructors = new ArrayList<>();
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            String sql = "SELECT ic.*, c.descriptive_Title as course_Name, u.full_name as instructor_name, u.email " +
                    "FROM instructor_courses ic " +
                    "JOIN courses c ON ic.course_Code = c.course_Code " +
                    "JOIN users u ON ic.instructor_IDNumber = u.id_number " +
                    "WHERE ic.course_Code = ? AND ic.status = 'Active'";

            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, courseCode);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                Map<String, String> instructor = new HashMap<>();
                instructor.put("instructorId", rs.getString("instructor_IDNumber"));
                instructor.put("instructorName", rs.getString("instructor_name"));
                instructor.put("email", rs.getString("email"));
                instructor.put("assignedDate", rs.getTimestamp("assigned_date").toString());
                instructors.add(instructor);
            }
        } catch (SQLException e) {
            String errorMessage = String.format(
                    "Error retrieving course instructors: %s (SQL State: %s, Error Code: %d)",
                    e.getMessage(), e.getSQLState(), e.getErrorCode());
            System.err.println("\n" + errorMessage);
            e.printStackTrace();
        } finally {
            try {
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public List<Instructor> getAllInstructors() {
        List<Instructor> instructors = new ArrayList<>();
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            String sql = "SELECT DISTINCT u.id_number as instructor_IDNumber, u.full_name as instructor_name " +
                    "FROM users u " +
                    "WHERE u.user_type = 'Instructor' AND u.status = 'Active'";

            PreparedStatement pstmt = conn.prepareStatement(sql);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                Instructor instructor = new Instructor(
                        rs.getInt("instructor_IDNumber"),
                        rs.getString("instructor_name"),
                        null,
                        null);

                instructors.add(instructor);
            }
        } catch (SQLException e) {
            String errorMessage = String.format(
                    "Error retrieving all instructors: %s (SQL State: %s, Error Code: %d)",
                    e.getMessage(), e.getSQLState(), e.getErrorCode());
            System.err.println("\n" + errorMessage);
            e.printStackTrace();
        } finally {
            try {
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return instructors;
    }

    public String getInstructorNameById(int instructorId) {
        List<Instructor> instructors = getAllInstructors();
        for (Instructor instructor : instructors) {
            if (instructor.getIdNumber().equals(instructorId)) {
                return instructor.getFullName();
            }
        }
        return null;
    }

    public String getCourseNameByCourseCode(String courseCode) {
        CourseDAO courseDAO = new CourseDAO();
        List<Course> courses = courseDAO.getAllCourses();
        for (Course course : courses) {
            if (course.getCourseCode().equals(courseCode)) {
                return course.getCourseName();
            }
        }
        return null;
    }
}