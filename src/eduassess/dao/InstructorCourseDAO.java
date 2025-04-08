package eduassess.dao;

import eduassess.util.DatabaseConnection;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InstructorCourseDAO {

    public boolean assignInstructorToCourse(String instructorId, String courseCode) {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            // Verify if instructor exists
            String verifyInstructorSql = "SELECT * FROM instructor WHERE instructor_IDNumber = ?";
            PreparedStatement verifyStmt = conn.prepareStatement(verifyInstructorSql);
            verifyStmt.setString(1, instructorId);
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
            checkStmt.setString(1, instructorId);
            checkStmt.setString(2, courseCode);
            ResultSet checkRs = checkStmt.executeQuery();

            if (checkRs.next()) {
                System.err.println("Instructor is already assigned to this course!");
                conn.rollback();
                return false;
            }

            // Assign instructor to course
            String sql = "INSERT INTO instructor_courses (instructor_IDNumber, course_Code, status, assigned_date) VALUES (?, ?, 'Active', CURRENT_TIMESTAMP)";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, instructorId);
            pstmt.setString(2, courseCode);
            pstmt.executeUpdate();

            conn.commit();
            return true;

        } catch (SQLException e) {
            String errorMessage = String.format(
                    "Error during instructor assignment: %s (SQL State: %s, Error Code: %d)",
                    e.getMessage(), e.getSQLState(), e.getErrorCode());
            System.err.println("\n" + errorMessage);
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

    public boolean unassignInstructorFromCourse(String instructorId, String courseCode) {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            // Verify if the assignment exists and is active
            String verifySql = "SELECT * FROM instructor_courses WHERE instructor_IDNumber = ? AND course_Code = ? AND status = 'Active'";
            PreparedStatement verifyStmt = conn.prepareStatement(verifySql);
            verifyStmt.setString(1, instructorId);
            verifyStmt.setString(2, courseCode);
            ResultSet verifyRs = verifyStmt.executeQuery();

            if (!verifyRs.next()) {
                System.err.println("No active assignment found for this instructor and course!");
                conn.rollback();
                return false;
            }

            // Unassign instructor from course
            String sql = "UPDATE instructor_courses SET status = 'Inactive' WHERE instructor_IDNumber = ? AND course_Code = ?";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, instructorId);
            pstmt.setString(2, courseCode);
            pstmt.executeUpdate();

            conn.commit();
            return true;

        } catch (SQLException e) {
            String errorMessage = String.format(
                    "Error during instructor unassignment: %s (SQL State: %s, Error Code: %d)",
                    e.getMessage(), e.getSQLState(), e.getErrorCode());
            System.err.println("\n" + errorMessage);
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

    public List<Map<String, String>> getInstructorCourses(String instructorId) {
        List<Map<String, String>> courses = new ArrayList<>();
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            String sql = "SELECT ic.*, c.course_Name, u.full_name as instructor_name " +
                    "FROM instructor_courses ic " +
                    "JOIN courses c ON ic.course_Code = c.course_Code " +
                    "JOIN users u ON ic.instructor_IDNumber = u.id_number " +
                    "WHERE ic.instructor_IDNumber = ? AND ic.status = 'Active'";

            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, instructorId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                Map<String, String> course = new HashMap<>();
                course.put("courseCode", rs.getString("course_Code"));
                course.put("courseName", rs.getString("course_Name"));
                course.put("instructorName", rs.getString("instructor_name"));
                course.put("assignedDate", rs.getTimestamp("assigned_date").toString());
                courses.add(course);
            }
        } catch (SQLException e) {
            String errorMessage = String.format(
                    "Error retrieving instructor courses: %s (SQL State: %s, Error Code: %d)",
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
        return courses;
    }

    public List<Map<String, String>> getCourseInstructors(String courseCode) {
        List<Map<String, String>> instructors = new ArrayList<>();
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            String sql = "SELECT ic.*, c.course_Name, u.full_name as instructor_name, u.email " +
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
        return instructors;
    }
}