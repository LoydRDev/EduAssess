package eduassess.dao;

import eduassess.model.Course;
import eduassess.model.CourseEnrollment;
import eduassess.model.Instructor;
import eduassess.model.User;
import eduassess.util.DatabaseConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class CourseDAO {

    public String getStudentYearAndSemester(String studentId) {
        String query = "SELECT year_level, semester FROM enrollments WHERE student_IDNumber = ? LIMIT 1";

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, studentId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                String yearLevel = rs.getString("year_level");
                String semester = rs.getString("semester");
                return yearLevel + " " + semester;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    public int getStudentTotalUnits(String studentId) {
        String query = "SELECT SUM(c.lec + c.lab) as total_units " +
                "FROM enrollments e " +
                "JOIN courses c ON e.course_Code = c.course_Code " +
                "WHERE e.student_IDNumber = ?";

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, studentId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getInt("total_units");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return 0;
    }

    public boolean enrollStudentInCourse(String studentId, String courseCode, String yearLevel, String semester) {
        System.out.println("\n=== Starting single course enrollment process ===");
        System.out.println("Student ID: " + studentId);
        System.out.println("Course Code: " + courseCode);
        System.out.println("Year Level: " + yearLevel);
        System.out.println("Semester: " + semester);

        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            // First verify if the student exists
            String verifyStudentSql = "SELECT * FROM student WHERE student_IDNumber = ?";
            PreparedStatement verifyStmt = conn.prepareStatement(verifyStudentSql);
            verifyStmt.setString(1, studentId);
            ResultSet verifyRs = verifyStmt.executeQuery();

            if (!verifyRs.next()) {
                System.err.println("Student not found in database!");
                conn.rollback();
                return false;
            }

            // Check if the course exists and matches the year level and semester
            String courseSql = "SELECT * FROM courses WHERE course_Code = ? AND course_YearLevel = ? AND course_Semester = ?";
            PreparedStatement courseStmt = conn.prepareStatement(courseSql);
            courseStmt.setString(1, courseCode);
            courseStmt.setString(2, yearLevel);
            courseStmt.setString(3, semester);
            ResultSet courseRs = courseStmt.executeQuery();

            if (!courseRs.next()) {
                System.err.println("Course not found or does not match year level and semester!");
                conn.rollback();
                return false;
            }

            // Check if student has failed this course before
            String checkGradeSql = "SELECT grade FROM grades WHERE student_IDNumber = ? AND course_Code = ? ORDER BY date_updated DESC LIMIT 1";
            PreparedStatement checkGradeStmt = conn.prepareStatement(checkGradeSql);
            checkGradeStmt.setString(1, studentId);
            checkGradeStmt.setString(2, courseCode);
            ResultSet checkGradeRs = checkGradeStmt.executeQuery();

            boolean isFailedCourse = false;
            if (checkGradeRs.next()) {
                try {
                    double grade = checkGradeRs.getDouble("grade");
                    isFailedCourse = grade > 3.0; // Failed if grade is greater than 3.0
                } catch (NumberFormatException e) {
                    String gradeStr = checkGradeRs.getString("grade");
                    isFailedCourse = "FAILED".equals(gradeStr);
                }
            }

            // For failed courses, remove existing enrollment and grade records before
            // re-enrolling
            if (isFailedCourse) {
                // Delete enrollment record
                String deleteEnrollSql = "DELETE FROM enrollments WHERE student_IDNumber = ? AND course_Code = ? AND year_level = ? AND semester = ?";
                PreparedStatement deleteEnrollStmt = conn.prepareStatement(deleteEnrollSql);
                deleteEnrollStmt.setString(1, studentId);
                deleteEnrollStmt.setString(2, courseCode);
                deleteEnrollStmt.setString(3, yearLevel);
                deleteEnrollStmt.setString(4, semester);
                deleteEnrollStmt.executeUpdate();

                // Delete grade record to ensure a fresh start
                String deleteGradeSql = "DELETE FROM grades WHERE student_IDNumber = ? AND course_Code = ?";
                PreparedStatement deleteGradeStmt = conn.prepareStatement(deleteGradeSql);
                deleteGradeStmt.setString(1, studentId);
                deleteGradeStmt.setString(2, courseCode);
                deleteGradeStmt.executeUpdate();

                System.out.println("Deleted previous grade record for re-enrollment.");
            } else {
                // For new enrollments, check if student is already enrolled
                String checkEnrollSql = "SELECT * FROM enrollments WHERE student_IDNumber = ? AND course_Code = ?";
                PreparedStatement checkEnrollStmt = conn.prepareStatement(checkEnrollSql);
                checkEnrollStmt.setString(1, studentId);
                checkEnrollStmt.setString(2, courseCode);
                ResultSet checkEnrollRs = checkEnrollStmt.executeQuery();

                if (checkEnrollRs.next()) {
                    System.err.println("Student is already enrolled in this course!");
                    conn.rollback();
                    return false;
                }
            }

            // Enroll student in the course
            String enrollSql = "INSERT INTO enrollments (student_IDNumber, course_Code, year_level, semester) VALUES (?, ?, ?, ?)";
            PreparedStatement enrollStmt = conn.prepareStatement(enrollSql);
            enrollStmt.setString(1, studentId);
            enrollStmt.setString(2, courseCode);
            enrollStmt.setString(3, yearLevel);
            enrollStmt.setString(4, semester);
            enrollStmt.executeUpdate();

            conn.commit();
            return true;

        } catch (SQLException e) {
            String errorMessage = String.format("Error during enrollment: %s (SQL State: %s, Error Code: %d)",
                    e.getMessage(), e.getSQLState(), e.getErrorCode());
            System.err.println("\n" + errorMessage);
            e.printStackTrace(); // Keep stack trace for debugging

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

    public List<CourseEnrollment> getStudentCourses(String studentId) {
        List<CourseEnrollment> courses = new ArrayList<>();
        String query = """
                SELECT c.course_Code, c.descriptive_Title, c.course_YearLevel,
                       c.course_Semester, (c.lec + c.lab) as units, e.status,
                       i.instructor_Name as instructor_name,
                       CASE WHEN ie.id IS NOT NULL THEN 1 ELSE 0 END as is_evaluated,
                       COALESCE(g.grade, 'Not Graded') as grade,
                       CASE WHEN er.student_IDNumber IS NOT NULL THEN 1 ELSE 0 END as has_pending_request
                FROM courses c
                JOIN enrollments e ON c.course_Code = e.course_Code
                LEFT JOIN instructor i ON c.instructor_IDNumber = i.instructor_IDNumber
                LEFT JOIN instructor_evaluations ie
                    ON c.course_Code = ie.course_Code
                    AND ie.student_IDNumber = ?
                LEFT JOIN grades g
                    ON c.course_Code = g.course_Code
                    AND g.student_IDNumber = ?
                LEFT JOIN evaluation_requests er
                    ON c.course_Code = er.course_Code
                    AND er.student_IDNumber = ?
                    AND er.status = 'PENDING'
                WHERE e.student_IDNumber = ?
                """;

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, studentId);
            stmt.setString(2, studentId);
            stmt.setString(3, studentId);
            stmt.setString(4, studentId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    boolean isEvaluated = rs.getInt("is_evaluated") == 1;
                    boolean hasPendingRequest = rs.getInt("has_pending_request") == 1;

                    CourseEnrollment course = new CourseEnrollment(
                            rs.getString("course_Code"),
                            rs.getString("descriptive_Title"),
                            rs.getString("course_YearLevel"),
                            rs.getString("course_Semester"),
                            String.valueOf(rs.getInt("units")),
                            rs.getString("status"),
                            rs.getString("instructor_name"),
                            isEvaluated || hasPendingRequest); // Course is considered evaluated if there's a completed
                                                               // evaluation or pending request
                    course.setGrade(rs.getString("grade"));
                    courses.add(course);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return courses;
    }

    public List<Course> getAllCourses() {
        List<Course> courses = new ArrayList<>();
        String sql = """
                SELECT course_Code, descriptive_Title as course_name,
                       course_YearLevel, course_Semester,
                       (lec + lab) as units, prerequisite
                FROM courses
                ORDER BY course_Code
                """;

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql);
                ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Course course = new Course(
                        rs.getString("course_Code"),
                        rs.getString("course_name"),
                        rs.getString("prerequisite"),
                        rs.getInt("units"),
                        1.0,
                        rs.getString("course_YearLevel"),
                        rs.getString("course_Semester"));
                courses.add(course);
            }
        } catch (SQLException e) {
            System.err.println("Error getting all courses: " + e.getMessage());
            e.printStackTrace();
        }
        return courses;
    }

    public boolean enrollStudent(String studentId, String courseCode) {
        System.out.println("\n=== Starting enrollment process from recommendations ===");
        System.out.println("Student ID: " + studentId);
        System.out.println("Course Code: " + courseCode);

        // Get the latest enrollment to determine year level and semester
        List<CourseEnrollment> enrollments = getStudentCourses(studentId);
        if (enrollments == null || enrollments.isEmpty()) {
            System.err.println("No enrollment history found for student!");
            return false;
        }

        // Get the latest enrollment's year level and semester
        CourseEnrollment latestEnrollment = enrollments.get(enrollments.size() - 1);
        String yearLevel = latestEnrollment.getYearLevel();
        String semester = latestEnrollment.getSemester();

        // Use the existing method with the determined year level and semester
        return enrollStudentInCourse(studentId, courseCode, yearLevel, semester);
    }

    /**
     * Gets all courses that a student has not enrolled in yet.
     * This method compares all available courses with the student's enrolled
     * courses
     * and returns only the courses that the student hasn't taken.
     * 
     * @param studentId The student's ID number
     * @return List of courses not enrolled by the student
     */
    public List<Course> getCoursesNotEnrolled(String studentId) {
        List<Course> allCourses = getAllCourses();
        List<CourseEnrollment> enrolledCourses = getStudentCourses(studentId);

        // If there are no enrolled courses, return all available courses
        if (enrolledCourses.isEmpty()) {
            return allCourses;
        }

        // Extract course codes of enrolled courses
        List<String> enrolledCourseCodes = enrolledCourses.stream()
                .map(CourseEnrollment::getCourseCode)
                .collect(Collectors.toList());

        // Filter out courses that the student has already enrolled in
        return allCourses.stream()
                .filter(course -> !enrolledCourseCodes.contains(course.getCourseCode()))
                .collect(Collectors.toList());
    }

    public boolean clearStudentEnrollments(String studentId) {
        System.out.println("\n=== Clearing student enrollments ===");
        System.out.println("Student ID: " + studentId);

        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            // First verify if the student exists
            String verifySql = "SELECT * FROM student WHERE student_IDNumber = ?";
            PreparedStatement verifyStmt = conn.prepareStatement(verifySql);
            verifyStmt.setString(1, studentId);
            ResultSet verifyRs = verifyStmt.executeQuery();

            if (!verifyRs.next()) {
                System.err.println("Student not found in database!");
                conn.rollback();
                return false;
            }

            // Clear existing enrollments for this student
            String clearSql = "DELETE FROM enrollments WHERE student_IDNumber = ?";
            System.out.println("\nExecuting clear SQL: " + clearSql);
            System.out.println("Parameters: [" + studentId + "]");

            PreparedStatement clearStmt = conn.prepareStatement(clearSql);
            clearStmt.setString(1, studentId);
            clearStmt.executeUpdate();

            conn.commit();
            return true;
        } catch (SQLException e) {
            System.err.println("\nError during clearing enrollments: " + e.getMessage());
            try {
                if (conn != null) {
                    conn.rollback();
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            e.printStackTrace();
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

    public User getStudentById(String studentId) {
        String sql = "SELECT student_IDNumber, student_Name, student_Email, student_Type FROM student WHERE student_IDNumber = ?";
        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, studentId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new User(
                            rs.getString("student_IDNumber"),
                            rs.getString("student_Name"),
                            rs.getString("student_Email"),
                            rs.getString("student_Type"));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error fetching student by ID: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    public boolean resetCourseEvaluation(String studentId, String courseCode) {
        System.out.println("\n=== Resetting course evaluation ===");
        System.out.println("Student ID: " + studentId);
        System.out.println("Course Code: " + courseCode);

        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            // Delete any existing evaluation records
            String deleteEvalSql = "DELETE FROM instructor_evaluations WHERE student_IDNumber = ? AND course_Code = ?";
            PreparedStatement deleteEvalStmt = conn.prepareStatement(deleteEvalSql);
            deleteEvalStmt.setString(1, studentId);
            deleteEvalStmt.setString(2, courseCode);
            deleteEvalStmt.executeUpdate();

            // Delete any pending evaluation requests
            String deleteRequestSql = "DELETE FROM evaluation_requests WHERE student_IDNumber = ? AND course_Code = ?";
            PreparedStatement deleteRequestStmt = conn.prepareStatement(deleteRequestSql);
            deleteRequestStmt.setString(1, studentId);
            deleteRequestStmt.setString(2, courseCode);
            deleteRequestStmt.executeUpdate();

            conn.commit();
            return true;

        } catch (SQLException e) {
            System.err.println("\nError during evaluation reset: " + e.getMessage());
            try {
                if (conn != null) {
                    conn.rollback();
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            e.printStackTrace();
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

    public boolean updateStudentYearAndSemester(String studentId, String yearLevel, String semester) {
        System.out.println("\n=== Updating student year and semester ===");
        System.out.println("Student ID: " + studentId);
        System.out.println("Year Level: " + yearLevel);
        System.out.println("Semester: " + semester);

        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            // First verify if the student exists
            String verifySql = "SELECT * FROM student WHERE student_IDNumber = ?";
            PreparedStatement verifyStmt = conn.prepareStatement(verifySql);
            verifyStmt.setString(1, studentId);
            ResultSet verifyRs = verifyStmt.executeQuery();

            if (!verifyRs.next()) {
                System.err.println("Student not found in database!");
                conn.rollback();
                return false;
            }

            // First update student year and semester
            String updateSql = "UPDATE student SET studentYearLevel = ?, studentSemesterLevel = ? WHERE student_IDNumber = ?";
            System.out.println("\nExecuting SQL: " + updateSql);
            System.out.println("Parameters: [" + yearLevel + ", " + semester + ", " + studentId + "]");

            PreparedStatement updateStmt = conn.prepareStatement(updateSql);
            updateStmt.setString(1, yearLevel);
            updateStmt.setString(2, semester);
            updateStmt.setString(3, studentId);
            int rowsAffected = updateStmt.executeUpdate();

            if (rowsAffected <= 0) {
                System.err.println("No rows were updated in student table!");
                conn.rollback();
                return false;
            }

            // Clear any existing enrollments for this student
            if (!clearStudentEnrollments(studentId)) {
                System.err.println("Failed to clear student enrollments!");
                conn.rollback();
                return false;
            }

            conn.commit();
            return true;

        } catch (SQLException e) {
            System.err.println("\nError during update: " + e.getMessage());
            System.err.println("SQL State: " + e.getSQLState());
            System.err.println("Error Code: " + e.getErrorCode());
            try {
                if (conn != null) {
                    conn.rollback();
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            e.printStackTrace();
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

    public boolean deleteFailedCourse(String studentId, String courseCode) {
        System.out.println("\n=== Starting failed course deletion process ===");
        System.out.println("Student ID: " + studentId);
        System.out.println("Course Code: " + courseCode);

        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            // Verify if the course exists and has a failing grade
            String verifyGradeSql = "SELECT grade FROM grades WHERE student_IDNumber = ? AND course_Code = ? ORDER BY date_updated DESC LIMIT 1";
            PreparedStatement verifyGradeStmt = conn.prepareStatement(verifyGradeSql);
            verifyGradeStmt.setString(1, studentId);
            verifyGradeStmt.setString(2, courseCode);
            ResultSet verifyGradeRs = verifyGradeStmt.executeQuery();

            boolean isFailedCourse = false;
            if (verifyGradeRs.next()) {
                try {
                    double grade = verifyGradeRs.getDouble("grade");
                    isFailedCourse = grade > 3.0; // Failed if grade is greater than 3.0
                } catch (NumberFormatException e) {
                    String gradeStr = verifyGradeRs.getString("grade");
                    isFailedCourse = "FAILED".equals(gradeStr);
                }
            }

            if (!isFailedCourse) {
                System.err.println("Course is not a failed course or does not exist!");
                conn.rollback();
                return false;
            }

            // Delete the enrollment
            String deleteEnrollSql = "DELETE FROM enrollments WHERE student_IDNumber = ? AND course_Code = ?";
            PreparedStatement deleteEnrollStmt = conn.prepareStatement(deleteEnrollSql);
            deleteEnrollStmt.setString(1, studentId);
            deleteEnrollStmt.setString(2, courseCode);
            int rowsDeleted = deleteEnrollStmt.executeUpdate();

            if (rowsDeleted == 0) {
                System.err.println("No enrollment found to delete!");
                conn.rollback();
                return false;
            }

            // Note: We keep the grade record for academic history

            conn.commit();
            System.out.println("Successfully deleted failed course enrollment.");
            return true;

        } catch (SQLException e) {
            System.err.println("\nError during failed course deletion: " + e.getMessage());
            System.err.println("SQL State: " + e.getSQLState());
            System.err.println("Error Code: " + e.getErrorCode());
            try {
                if (conn != null) {
                    conn.rollback();
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            e.printStackTrace();
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

    public int getEnrolledCoursesCount(String studentId) {
        String sql = "SELECT COUNT(*) FROM enrollments WHERE student_IDNumber = ?";

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, studentId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return rs.getInt(1);
            }

        } catch (SQLException e) {
            System.err.println("\nError getting enrolled courses count: " + e.getMessage());
            e.printStackTrace();
        }
        return 0;
    }

    public List<String> getEnrolledCoursesCodes(String studentId) {
        String sql = "SELECT c.course_Code FROM courses c " +
                "INNER JOIN enrollments e ON c.course_Code = e.course_Code " +
                "WHERE e.student_IDNumber = ? " +
                "ORDER BY c.course_YearLevel, c.course_Semester";

        List<String> courseCodes = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, studentId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                courseCodes.add(rs.getString("course_Code"));
            }

        } catch (SQLException e) {
            System.err.println("\nError getting enrolled course codes: " + e.getMessage());
            e.printStackTrace();
        }
        return courseCodes;
    }

    public boolean saveInstructorEvaluation(String studentId, String courseCode, int rating, String comments) {
        String sql = "INSERT INTO instructor_evaluations (student_IDNumber, course_Code, rating, comments) VALUES (?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, studentId);
            pstmt.setString(2, courseCode);
            pstmt.setInt(3, rating);
            pstmt.setString(4, comments);

            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;

        } catch (SQLException e) {
            System.err.println("\nError saving instructor evaluation: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public boolean hasEvaluatedCourse(String studentId, String courseCode) {
        String sql = "SELECT COUNT(*) FROM instructor_evaluations WHERE student_IDNumber = ? AND course_Code = ?";

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, studentId);
            pstmt.setString(2, courseCode);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
            return false;

        } catch (SQLException e) {
            System.err.println("\nError checking instructor evaluation: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public List<String> getStudentCompletedCourses(String studentId) {
        List<String> completedCourses = new ArrayList<>();
        String sql = """
                SELECT g.course_Code, g.grade
                FROM grades g
                JOIN enrollments e ON g.course_Code = e.course_Code AND g.student_IDNumber = e.student_IDNumber
                WHERE g.student_IDNumber = ?
                AND g.grade IS NOT NULL
                AND g.grade != 'Not Graded'
                GROUP BY g.course_Code, g.grade
                ORDER BY MAX(g.date_updated) DESC
                """;

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, studentId);
            ResultSet rs = pstmt.executeQuery();

            // Keep track of courses we've already processed to avoid duplicates
            // (we only want the most recent grade for each course)
            Set<String> processedCourses = new HashSet<>();

            while (rs.next()) {
                String courseCode = rs.getString("course_Code");
                String gradeStr = rs.getString("grade");

                // Skip if we've already processed this course
                if (processedCourses.contains(courseCode)) {
                    continue;
                }

                processedCourses.add(courseCode);

                // Check if the grade is passing
                boolean isPassing = false;

                if (gradeStr != null && !gradeStr.equals("Not Graded")) {
                    if (gradeStr.matches("\\d+\\.\\d*")) {
                        try {
                            double numericGrade = Double.parseDouble(gradeStr);
                            isPassing = numericGrade <= 3.0; // Passing if grade is 3.0 or less
                            System.out.println("Processing course " + courseCode + ": numeric grade = " + numericGrade
                                    + ", isPassing = " + isPassing);
                        } catch (NumberFormatException e) {
                            System.err.println("Error parsing grade for course " + courseCode + ": " + gradeStr);
                            continue;
                        }
                    } else {
                        // Consider any non-numeric grade as passing except for these specific values
                        isPassing = !gradeStr.equals("FAILED") && !gradeStr.equals("INC") && !gradeStr.equals("DRP");
                        System.out.println("Processing course " + courseCode + ": text grade = " + gradeStr
                                + ", isPassing = " + isPassing);
                    }

                    if (isPassing) {
                        completedCourses.add(courseCode);
                        System.out.println("Added passed course: " + courseCode);
                    }
                }
            }

        } catch (SQLException e) {
            System.err.println("\nError getting completed courses: " + e.getMessage());
            e.printStackTrace();
        }

        return completedCourses;
    }

    public List<CourseEnrollment> getStudentPassedCourses(String studentId) {
        if (studentId == null || studentId.trim().isEmpty()) {
            System.err.println("Invalid student ID provided");
            return new ArrayList<>();
        }

        List<CourseEnrollment> passedCourses = new ArrayList<>();
        String sql = """
                SELECT DISTINCT c.course_Code, c.descriptive_Title, c.course_YearLevel,
                       c.course_Semester, COALESCE((c.lec + c.lab), 0) as units,
                       COALESCE(e.status, 'Unknown') as status,
                       COALESCE(i.instructor_Name, 'TBA') as instructor_name,
                       g.grade
                FROM courses c
                JOIN enrollments e ON c.course_Code = e.course_Code AND e.student_IDNumber = ?
                LEFT JOIN instructor i ON c.instructor_IDNumber = i.instructor_IDNumber
                LEFT JOIN (
                    SELECT course_Code, student_IDNumber, grade
                    FROM grades
                    WHERE student_IDNumber = ?
                    AND grade IS NOT NULL
                    AND grade != 'Not Graded'
                    GROUP BY course_Code, student_IDNumber
                    HAVING grade = (
                        SELECT grade
                        FROM grades g2
                        WHERE g2.course_Code = grades.course_Code
                        AND g2.student_IDNumber = grades.student_IDNumber
                        ORDER BY date_updated DESC
                        LIMIT 1
                    )
                ) g ON c.course_Code = g.course_Code
                WHERE g.grade IS NOT NULL
                ORDER BY c.course_YearLevel, c.course_Semester
                """;

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, studentId);
            pstmt.setString(2, studentId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                try {
                    String gradeStr = rs.getString("grade");
                    boolean isPassing = false;

                    if (gradeStr != null && !gradeStr.trim().isEmpty() && !gradeStr.equals("Not Graded")) {
                        if (gradeStr.matches("\\d+(\\.\\d+)?")) {
                            try {
                                double numericGrade = Double.parseDouble(gradeStr.trim());
                                // Handle both 1.0-3.0 scale and 75-100 scale
                                if (numericGrade >= 1.0 && numericGrade <= 3.0) {
                                    // Philippine grading system: 1.0 is highest, 3.0 is passing
                                    isPassing = true;
                                } else if (numericGrade >= 75.0 && numericGrade <= 100.0) {
                                    // Percentage-based grading: 75-100 is passing
                                    isPassing = true;
                                } else {
                                    // Any other numeric grade is considered failing
                                    isPassing = false;
                                }
                            } catch (NumberFormatException e) {
                                System.err.println("Error parsing grade '" + gradeStr + "': " + e.getMessage());
                                continue;
                            }
                        } else {
                            // Consider text-based grades
                            String normalizedGrade = gradeStr.trim().toUpperCase();
                            // Only consider specific text grades as passing
                            isPassing = !normalizedGrade.equals("FAILED") &&
                                    !normalizedGrade.equals("INC") &&
                                    !normalizedGrade.equals("DRP") &&
                                    !normalizedGrade.equals("F") &&
                                    !normalizedGrade.equals("NOT GRADED") &&
                                    !normalizedGrade.equals("INCOMPLETE") &&
                                    !normalizedGrade.equals("DROPPED");
                        }

                        if (isPassing) {
                            try {
                                CourseEnrollment course = new CourseEnrollment(
                                        rs.getString("course_Code"),
                                        rs.getString("descriptive_Title"),
                                        rs.getString("course_YearLevel"),
                                        rs.getString("course_Semester"),
                                        String.valueOf(rs.getInt("units")),
                                        rs.getString("status"),
                                        rs.getString("instructor_name"),
                                        true);
                                course.setGrade(gradeStr);
                                passedCourses.add(course);
                            } catch (SQLException e) {
                                System.err.println("Error creating CourseEnrollment: " + e.getMessage());
                                continue;
                            }
                        }
                    }
                } catch (SQLException e) {
                    System.err.println("Error processing result set row: " + e.getMessage());
                    continue;
                }
            }
        } catch (SQLException e) {
            System.err.println("Database error in getStudentPassedCourses: " + e.getMessage());
            e.printStackTrace();
        }
        return passedCourses;
    }

    public List<Course> getInstructorCourses(String instructorId) {
        String sql = """
                SELECT c.*, COUNT(e.student_IDNumber) as student_count
                FROM courses c
                LEFT JOIN enrollments e ON c.course_Code = e.course_Code
                WHERE c.instructor_IDNumber = ?
                GROUP BY c.course_Code
                """;
        List<Course> courses = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, instructorId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                Course course = new Course(
                        rs.getString("course_Code"),
                        rs.getString("course_name"),
                        rs.getString("prerequisite"),
                        rs.getInt("units"),
                        1.0,
                        rs.getString("course_YearLevel"),
                        rs.getString("course_Semester"));
                courses.add(course);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return courses;
    }

    public int getInstructorTotalCourses(String instructorId) {
        String sql = "SELECT COUNT(*) FROM courses WHERE instructor_IDNumber = ?";
        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, instructorId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public int getInstructorTotalStudents(String instructorId) {
        String sql = """
                SELECT COUNT(DISTINCT e.student_IDNumber)
                FROM courses c
                JOIN enrollments e ON c.course_Code = e.course_Code
                WHERE c.instructor_IDNumber = ?
                """;
        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, instructorId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public List<User> getCourseStudents(String courseCode) {
        String sql = """
                SELECT s.*
                FROM student s
                JOIN enrollments e ON s.student_IDNumber = e.student_IDNumber
                WHERE e.course_Code = ?
                """;
        List<User> students = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, courseCode);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                students.add(new User(
                        rs.getString("student_IDNumber"),
                        rs.getString("student_Name"),
                        rs.getString("password"),
                        "STUDENT"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return students;
    }

    public boolean requestEvaluation(String studentId, String courseCode) {
        System.out.println("\n=== Requesting evaluation ===");
        System.out.println("Student ID: " + studentId);
        System.out.println("Course Code: " + courseCode);

        String sql = """
                INSERT INTO evaluation_requests (student_IDNumber, course_Code, request_date, status)
                VALUES (?, ?, NOW(), 'PENDING')
                ON DUPLICATE KEY UPDATE
                request_date = NOW(),
                status = 'PENDING'
                """;
        System.out.println("Executing SQL: " + sql);

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, studentId);
            pstmt.setString(2, courseCode);

            int rowsAffected = pstmt.executeUpdate();
            System.out.println("Rows affected: " + rowsAffected);
            return rowsAffected > 0;

        } catch (SQLException e) {
            System.err.println("Error requesting evaluation: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public List<Course> getCoursesForEvaluation(String instructorId) {
        System.out.println("\n=== Getting courses for evaluation ===");
        System.out.println("Instructor ID: " + instructorId);

        String sql = """
                SELECT c.course_Code, c.descriptive_Title as course_name,
                       COUNT(er.student_IDNumber) as pending_count
                FROM courses c
                LEFT JOIN evaluation_requests er ON c.course_Code = er.course_Code
                    AND er.status = 'PENDING'
                WHERE c.instructor_IDNumber = ?
                GROUP BY c.course_Code, c.descriptive_Title
                ORDER BY pending_count DESC, c.course_Code ASC
                """;

        List<Course> courses = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            System.out.println("\nExecuting query to find courses with pending evaluations");
            System.out.println("SQL: " + sql);

            pstmt.setString(1, instructorId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                String courseCode = rs.getString("course_Code");
                String courseName = rs.getString("course_name");
                int pendingCount = rs.getInt("pending_count");

                Course course = new Course(courseCode, courseName, pendingCount);
                courses.add(course);

                System.out.println("Found course: " + courseCode +
                        " - " + courseName +
                        " (Pending Count: " + pendingCount + ")");
            }

            if (courses.isEmpty()) {
                System.out.println("No courses found with pending evaluations for instructor " + instructorId);
            } else {
                System.out.println("Total courses found: " + courses.size());
            }

        } catch (SQLException e) {
            System.err.println("Error getting courses for evaluation: " + e.getMessage());
            e.printStackTrace();
        }
        return courses;
    }

    public Course getCourseByCode(String courseCode) {
        String sql = """
                SELECT c.course_Code, c.descriptive_Title as course_name,
                       c.course_YearLevel, c.course_Semester,
                       (c.lec + c.lab) as units, c.prerequisite
                FROM courses c
                WHERE c.course_Code = ?
                """;

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, courseCode);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return new Course(
                        rs.getString("course_Code"),
                        rs.getString("course_name"),
                        rs.getString("prerequisite"),
                        rs.getInt("units"),
                        1.0,
                        rs.getString("course_YearLevel"),
                        rs.getString("course_Semester"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<User> getStudentsAwaitingEvaluation(String courseCode) {
        System.out.println("\n=== Getting students awaiting evaluation ===");
        System.out.println("Course Code: " + courseCode);

        String sql = """
                SELECT DISTINCT s.student_IDNumber, s.student_Name, er.request_date, er.course_Code
                FROM student s
                INNER JOIN evaluation_requests er ON s.student_IDNumber = er.student_IDNumber
                WHERE er.course_Code = ?
                AND er.status = 'PENDING'
                ORDER BY er.request_date ASC
                """;
        System.out.println("Executing SQL: " + sql);

        List<User> students = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, courseCode);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                String studentId = rs.getString("student_IDNumber");
                String studentName = rs.getString("student_Name");
                LocalDateTime requestDate = rs.getTimestamp("request_date").toLocalDateTime();

                User student = new User(studentId, studentName, "", "STUDENT");
                student.setRequestDate(requestDate);
                students.add(student);

                System.out.println("Found student: " + studentId +
                        " - " + studentName +
                        " (Request Date: " + requestDate + ")");
            }

            System.out.println("Total students found: " + students.size());

        } catch (SQLException e) {
            String errorMessage = String.format(
                    "Error getting students awaiting evaluation: %s (SQL State: %s, Error Code: %d)",
                    e.getMessage(), e.getSQLState(), e.getErrorCode());
            System.err.println("\n" + errorMessage);
            e.printStackTrace(); // Keep stack trace for debugging
        }

        return students;
    }

    public boolean submitGrade(String studentId, String courseCode, String grade) {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            // Insert or update the grade
            String gradeSql = """
                    INSERT INTO grades (student_IDNumber, course_Code, grade)
                    VALUES (?, ?, ?)
                    ON DUPLICATE KEY UPDATE grade = ?
                    """;
            PreparedStatement gradeStmt = conn.prepareStatement(gradeSql);
            gradeStmt.setString(1, studentId);
            gradeStmt.setString(2, courseCode);
            gradeStmt.setString(3, grade);
            gradeStmt.setString(4, grade);
            gradeStmt.executeUpdate();

            // Update the evaluation request status
            String updateSql = """
                    UPDATE evaluation_requests
                    SET status = 'APPROVED'
                    WHERE student_IDNumber = ?
                    AND course_Code = ?
                    AND status = 'PENDING'
                    """;
            PreparedStatement updateStmt = conn.prepareStatement(updateSql);
            updateStmt.setString(1, studentId);
            updateStmt.setString(2, courseCode);
            updateStmt.executeUpdate();

            conn.commit();
            return true;

        } catch (SQLException e) {
            try {
                if (conn != null) {
                    conn.rollback();
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            e.printStackTrace();
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

    public boolean assignAllCoursesToInstructor(String instructorId) {
        System.out.println("\n=== Assigning all courses to instructor ===");
        System.out.println("Instructor ID: " + instructorId);

        String sql = "UPDATE courses SET instructor_IDNumber = ?";

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, instructorId);
            int rowsAffected = pstmt.executeUpdate();
            System.out.println("Updated " + rowsAffected + " courses");
            return rowsAffected > 0;

        } catch (SQLException e) {
            String errorMessage = String.format(
                    "Error assigning courses to instructor: %s (SQL State: %s, Error Code: %d)",
                    e.getMessage(), e.getSQLState(), e.getErrorCode());
            System.err.println("\n" + errorMessage);
            e.printStackTrace(); // Keep stack trace for debugging
            return false;
        }
    }

    public List<Course> getAvailableCourses(String studentId, String yearLevel, String semester) {
        System.out.println("\n=== Getting available courses ===");
        System.out.println("Student ID: " + studentId);
        System.out.println("Year Level: " + yearLevel);
        System.out.println("Semester: " + semester);

        String sql = """
                SELECT c.course_Code, c.descriptive_Title as course_name,
                       c.course_YearLevel, c.course_Semester,
                       (c.lec + c.lab) as units, c.prerequisite,
                       CASE
                           WHEN g.grade IS NOT NULL AND (g.grade > '3.0' OR g.grade IN ('F', 'INC', 'DRP')) THEN 1
                           ELSE 0
                       END as is_failed
                FROM courses c
                LEFT JOIN enrollments e ON c.course_Code = e.course_Code AND e.student_IDNumber = ?
                LEFT JOIN grades g ON c.course_Code = g.course_Code AND g.student_IDNumber = ?
                WHERE (
                    -- Include failed courses from any semester
                    (g.grade IS NOT NULL AND (g.grade > '3.0' OR g.grade IN ('F', 'INC', 'DRP')))
                    OR
                    -- Include courses from current semester
                    (c.course_YearLevel = ? AND c.course_Semester = ?)
                    OR
                    -- Include courses from next semester only
                    (c.course_YearLevel = ? AND c.course_Semester = ?)
                )
                AND (e.course_Code IS NULL OR g.grade IS NOT NULL)
                ORDER BY
                    CASE
                        WHEN g.grade IS NOT NULL AND (g.grade > '3.0' OR g.grade IN ('F', 'INC', 'DRP')) THEN 0
                        ELSE 1
                    END,
                    c.course_YearLevel,
                    c.course_Semester,
                    c.course_Code
                """;

        List<Course> availableCourses = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, studentId);
            pstmt.setString(2, studentId);
            pstmt.setString(3, yearLevel);
            pstmt.setString(4, semester);
            pstmt.setString(5, yearLevel);
            pstmt.setString(6, semester);

            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                Course course = new Course(
                        rs.getString("course_Code"),
                        rs.getString("course_name"),
                        rs.getString("prerequisite"),
                        rs.getInt("units"),
                        1.0,
                        rs.getString("course_YearLevel"),
                        rs.getString("course_Semester"));
                availableCourses.add(course);
            }

            System.out.println("Found " + availableCourses.size() + " available courses");

        } catch (SQLException e) {
            String errorMessage = String.format("Error getting available courses: %s (SQL State: %s, Error Code: %d)",
                    e.getMessage(), e.getSQLState(), e.getErrorCode());
            System.err.println("\n" + errorMessage);
            e.printStackTrace(); // Keep stack trace for debugging
        }

        return availableCourses;
    }

    public List<User> getAllPendingEvaluations(String instructorId) {
        System.out.println("\n=== Getting all pending evaluations for instructor ===");
        System.out.println("Instructor ID: " + instructorId);

        String sql = """
                SELECT DISTINCT
                    s.student_IDNumber,
                    s.student_Name,
                    er.request_date,
                    er.course_Code,
                    c.descriptive_Title as course_name
                FROM student s
                INNER JOIN evaluation_requests er ON s.student_IDNumber = er.student_IDNumber
                INNER JOIN courses c ON er.course_Code = c.course_Code
                WHERE c.instructor_IDNumber = ?
                AND er.status = 'PENDING'
                ORDER BY er.request_date ASC
                """;
        System.out.println("Executing SQL: " + sql);

        List<User> students = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, instructorId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                String studentId = rs.getString("student_IDNumber");
                String studentName = rs.getString("student_Name");
                LocalDateTime requestDate = rs.getTimestamp("request_date").toLocalDateTime();
                String courseCode = rs.getString("course_Code");
                String courseName = rs.getString("course_name");

                User student = new User(studentId, studentName, "", "STUDENT");
                student.setRequestDate(requestDate);
                student.setCourseCode(courseCode);
                student.setCourseName(courseName); // Make sure User class has this method
                students.add(student);

                System.out.println("Found student: " + studentId +
                        " - " + studentName +
                        " (Course: " + courseCode + " - " + courseName +
                        ", Request Date: " + requestDate + ")");
            }

            System.out.println("Total students found: " + students.size());

        } catch (SQLException e) {
            System.err.println("Error getting all pending evaluations: " + e.getMessage());
            e.printStackTrace();
        }
        return students;
    }
}
