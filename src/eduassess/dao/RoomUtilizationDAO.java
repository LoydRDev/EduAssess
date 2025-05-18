package eduassess.dao;

import eduassess.model.RoomUtilization;
import eduassess.util.DatabaseConnection;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class RoomUtilizationDAO {

    public List<RoomUtilization> getRoomUtilization() {
        String sql = """
                SELECT r.room_id, r.room_name, r.capacity,
                       COUNT(b.booking_id) as bookings_count,
                       ROUND(COUNT(b.booking_id) * 100.0 / r.capacity, 2) as utilization_rate
                FROM rooms r
                LEFT JOIN bookings b ON r.room_id = b.room_id
                GROUP BY r.room_id, r.room_name, r.capacity
                ORDER BY utilization_rate DESC
                """;

        List<RoomUtilization> utilizations = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                utilizations.add(new RoomUtilization(
                        rs.getString("room_id"),
                        rs.getString("room_name"),
                        rs.getInt("capacity"),
                        rs.getInt("bookings_count"),
                        rs.getDouble("utilization_rate")));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return utilizations;
    }

    public boolean bookRoom(RoomUtilization booking) {
        String sql = """
                INSERT INTO bookings (room_id, start_time, end_time, course_code, purpose)
                VALUES (?, ?, ?, ?, ?)
                """;

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, booking.getRoomId());
            pstmt.setString(2, booking.getStartTime());
            pstmt.setString(3, booking.getEndTime());
            pstmt.setString(4, booking.getCourseCode());
            pstmt.setString(5, booking.getPurpose());

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean saveRoomBooking(RoomUtilization booking) {
        String sql = """
                INSERT INTO bookings (room_id, course_code, booking_date, start_time, end_time, purpose)
                VALUES (?, ?, ?, ?, ?, ?)
                """;

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, booking.getRoomId());
            pstmt.setString(2, booking.getCourseCode());
            pstmt.setDate(3, Date.valueOf(booking.getBookingDate()));
            pstmt.setString(4, booking.getStartTime());
            pstmt.setString(5, booking.getEndTime());
            pstmt.setString(6, booking.getPurpose());

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public List<RoomUtilization> getRoomSchedule(String roomId) {
        String sql = """
                SELECT b.booking_id, b.start_time, b.end_time,
                       c.course_Code, c.descriptive_Title,
                       i.instructor_Name
                FROM bookings b
                JOIN courses c ON b.course_Code = c.course_Code
                JOIN instructor i ON c.instructor_IDNumber = i.instructor_IDNumber
                WHERE b.room_id = ?
                ORDER BY b.start_time
                """;

        List<RoomUtilization> schedules = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, roomId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                schedules.add(new RoomUtilization(
                        roomId,
                        rs.getTimestamp("start_time").toString(),
                        rs.getTimestamp("end_time").toString(),
                        rs.getString("course_Code"),
                        rs.getString("descriptive_Title"),
                        rs.getString("instructor_Name")));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return schedules;
    }
}