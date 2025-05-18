package eduassess.dao;

import eduassess.model.User;
import eduassess.util.DatabaseConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserDAO {

    public boolean createUser(User user) {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            // First, insert into users table
            String userSql = "INSERT INTO users (id_number, full_name, email, password, user_type) VALUES (?, ?, ?, ?, ?)";
            PreparedStatement userStmt = conn.prepareStatement(userSql);
            userStmt.setString(1, user.getIdNumber());
            userStmt.setString(2, user.getFullName());
            userStmt.setString(3, user.getEmail());
            userStmt.setString(4, user.getPassword());
            userStmt.setString(5, user.getUserType());
            userStmt.executeUpdate();

            // Then, insert into the appropriate role-specific table
            if ("STUDENT".equals(user.getUserType())) {
                String studentSql = "INSERT INTO student (student_IDNumber, student_Name, student_Email, student_Type) VALUES (?, ?, ?, 'REGULAR')";
                PreparedStatement studentStmt = conn.prepareStatement(studentSql);
                studentStmt.setString(1, user.getIdNumber());
                studentStmt.setString(2, user.getFullName());
                studentStmt.setString(3, user.getEmail());
                studentStmt.executeUpdate();
            } else if ("INSTRUCTOR".equals(user.getUserType())) {
                String instructorSql = "INSERT INTO instructor (instructor_IDNumber, instructor_Name, department) VALUES (?, ?, 'UNASSIGNED')";
                PreparedStatement instructorStmt = conn.prepareStatement(instructorSql);
                instructorStmt.setString(1, user.getIdNumber());
                instructorStmt.setString(2, user.getFullName());
                instructorStmt.executeUpdate();
            }

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

    public User authenticateUser(String idNumber, String password) {
        String sql = "SELECT * FROM users WHERE id_number = ? AND password = ?";

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, idNumber);
            pstmt.setString(2, password);

            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                User user = new User(
                        rs.getString("id_number"),
                        rs.getString("full_name"),
                        rs.getString("email"),
                        rs.getString("user_type"));
                user.setId(rs.getInt("id"));
                user.setPassword(rs.getString("password"));
                user.setStatus(rs.getString("status"));
                return user;
            }
            return null;

        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public boolean isEmailExists(String email) {
        String sql = "SELECT COUNT(*) FROM users WHERE email = ?";

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, email);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
            return false;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean isIdNumberExists(String idNumber) {
        String sql = "SELECT COUNT(*) FROM users WHERE id_number = ?";

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, idNumber);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
            return false;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public List<User> getRecentUsers() {
        String sql = "SELECT * FROM users WHERE user_type != 'ADMIN' ORDER BY created_at DESC LIMIT 10";
        List<User> users = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                User user = new User(
                        rs.getString("id_number"),
                        rs.getString("full_name"),
                        rs.getString("email"),
                        rs.getString("user_type"));
                user.setId(rs.getInt("id"));
                user.setStatus(rs.getString("status"));
                users.add(user);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return users;
    }

    public int getTotalUsers() {
        String sql = "SELECT COUNT(*) FROM users WHERE user_type != 'ADMIN'";
        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public int getTotalStudents() {
        String sql = "SELECT COUNT(*) FROM users WHERE user_type = 'STUDENT'";
        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public List<User> getAllInstructors() throws SQLException {
        List<User> instructors = new ArrayList<>();
        String sql = "SELECT u.* FROM users u JOIN instructor i ON u.id_number = i.instructor_IDNumber WHERE u.user_type = 'INSTRUCTOR' AND u.status = 'Active'";

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                User instructor = new User(
                        rs.getString("id_number"),
                        rs.getString("full_name"),
                        rs.getString("email"),
                        rs.getString("user_type"));
                instructor.setId(rs.getInt("id"));
                instructor.setStatus(rs.getString("status"));
                instructors.add(instructor);
            }
        }
        return instructors;
    }

    public int getTotalInstructors() {
        String sql = "SELECT COUNT(*) FROM users WHERE user_type = 'INSTRUCTOR'";
        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public boolean updateUser(User user) {
        String sql = "UPDATE users SET full_name = ?, email = ?, id_number = ?, user_type = ?, status = ? WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, user.getFullName());
            pstmt.setString(2, user.getEmail());
            pstmt.setString(3, user.getIdNumber());
            pstmt.setString(4, user.getUserType());
            pstmt.setString(5, user.getStatus());
            pstmt.setInt(6, user.getId());

            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean deleteUser(int userId) {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            // First get the id_number for the user
            String getIdSql = "SELECT id_number, user_type FROM users WHERE id = ?";
            PreparedStatement getIdStmt = conn.prepareStatement(getIdSql);
            getIdStmt.setInt(1, userId);
            ResultSet rs = getIdStmt.executeQuery();

            if (rs.next()) {
                String idNumber = rs.getString("id_number");
                String userType = rs.getString("user_type");

                if ("STUDENT".equals(userType)) {
                    // Delete from student table (child of users)
                    String deleteStudentSql = "DELETE FROM student WHERE student_IDNumber = ?";
                    PreparedStatement deleteStudentStmt = conn.prepareStatement(deleteStudentSql);
                    deleteStudentStmt.setString(1, idNumber);
                    deleteStudentStmt.executeUpdate();
                }

                // Finally delete from users table (parent table)
                String deleteUserSql = "DELETE FROM users WHERE id = ?";
                PreparedStatement deleteUserStmt = conn.prepareStatement(deleteUserSql);
                deleteUserStmt.setInt(1, userId);
                deleteUserStmt.executeUpdate();

                conn.commit();
                return true;
            }

            conn.rollback();
            return false;
        } catch (SQLException e) {
            System.err.println("\nError during user deletion: " + e.getMessage());
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

    public boolean verifyStudentInfo(String idNumber, String fullName) {
        String sql = "SELECT * FROM student WHERE student_IDNumber = ? AND student_Name = ?";

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, idNumber);
            pstmt.setString(2, fullName);

            ResultSet rs = pstmt.executeQuery();
            return rs.next(); // Returns true if student exists, false otherwise

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public String getStudentCourse(String idNumber) {
        String sql = "SELECT course_Code FROM courses WHERE student_IDNumber = ?";

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, idNumber);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return rs.getString("course_Code");
            }
            return null;

        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public String getStudentYear(String idNumber) {
        String sql = "SELECT studentYearLevel FROM student WHERE student_IDNumber = ?";

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, idNumber);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return rs.getString("studentYearLevel");
            }
            return null;

        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public List<Map<String, String>> getAllUsers(String userType) {
        String sql = "SELECT id_number, full_name FROM users WHERE user_type = ?";
        List<Map<String, String>> users = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, userType);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                Map<String, String> user = new HashMap<>();
                user.put("id_number", rs.getString("id_number"));
                user.put("full_name", rs.getString("full_name"));
                users.add(user);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return users;
    }

}