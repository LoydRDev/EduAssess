package eduassess.dao;

import eduassess.model.Grade;
import eduassess.util.DatabaseConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class GradeDAO {
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public List<Grade> getRecentGrades(String instructorId) {
        String sql = """
                SELECT g.*, s.student_Name
                FROM grades g
                JOIN student s ON g.student_IDNumber = s.student_IDNumber
                JOIN courses c ON g.course_Code = c.course_Code
                WHERE c.instructor_IDNumber = ?
                ORDER BY g.date_updated DESC
                LIMIT 10
                """;
        List<Grade> grades = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, instructorId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                grades.add(new Grade(
                        rs.getString("student_IDNumber"),
                        rs.getString("student_Name"),
                        rs.getString("course_Code"),
                        rs.getDouble("grade"),
                        rs.getTimestamp("date_updated").toLocalDateTime().format(formatter),
                        rs.getString("status")));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return grades;
    }

    public boolean saveGrade(String studentId, String courseCode, double grade) {
        String sql = """
                INSERT INTO grades (student_IDNumber, course_Code, grade, date_updated, status)
                VALUES (?, ?, ?, NOW(), 'PENDING')
                ON DUPLICATE KEY UPDATE
                grade = VALUES(grade),
                date_updated = NOW(),
                status = 'PENDING'
                """;

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, studentId);
            pstmt.setString(2, courseCode);
            pstmt.setDouble(3, grade);

            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public Double getStudentGrade(String studentId, String courseCode) {
        String sql = "SELECT grade FROM grades WHERE student_IDNumber = ? AND course_Code = ?";

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, studentId);
            pstmt.setString(2, courseCode);

            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getDouble("grade");
            }
            return null;

        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public int getPendingGradesCount(String instructorId) {
        String sql = """
                SELECT COUNT(DISTINCT er.student_IDNumber)
                FROM evaluation_requests er
                JOIN courses c ON er.course_Code = c.course_Code
                WHERE c.instructor_IDNumber = ?
                AND er.status = 'PENDING'
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

    public boolean approveGrade(int gradeId) {
        String sql = "UPDATE grades SET status = 'APPROVED' WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, gradeId);
            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean rejectGrade(int gradeId, String reason) {
        String sql = "UPDATE grades SET status = 'REJECTED', rejection_reason = ? WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, reason);
            pstmt.setInt(2, gradeId);
            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}