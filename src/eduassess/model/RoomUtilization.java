package eduassess.model;

import java.time.LocalDate;

public class RoomUtilization {
    private String roomId;
    private String roomName;
    private int capacity;
    private int bookingsCount;
    private double utilizationRate;
    private String purpose;
    private LocalDate bookingDate;
    

    public RoomUtilization() {
        // Default constructor
    }

    // For schedule view
    private String startTime;
    private String endTime;
    private String courseCode;
    private String courseTitle;
    private String instructorName;

    public RoomUtilization(String roomId, String roomName, int capacity, int bookingsCount, double utilizationRate) {
        this.roomId = roomId;
        this.roomName = roomName;
        this.capacity = capacity;
        this.bookingsCount = bookingsCount;
        this.utilizationRate = utilizationRate;
    }

    public RoomUtilization(String roomId, String startTime, String endTime, String courseCode, String courseTitle,
            String instructorName) {
        this.roomId = roomId;
        this.startTime = startTime;
        this.endTime = endTime;
        this.courseCode = courseCode;
        this.courseTitle = courseTitle;
        this.instructorName = instructorName;
    }

    // Getters and setters
    public String getRoomId() {
        return roomId;
    }

    public String getRoomName() {
        return roomName;
    }

    public int getCapacity() {
        return capacity;
    }

    public int getBookingsCount() {
        return bookingsCount;
    }

    public double getUtilizationRate() {
        return utilizationRate;
    }

    public String getStartTime() {
        return startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public String getCourseCode() {
        return courseCode;
    }

    public String getCourseTitle() {
        return courseTitle;
    }

    public String getInstructorName() {
        return instructorName;
    }

    public String getPurpose() {
        return purpose;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public void setRoomName(String roomName) {
        this.roomName = roomName;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public void setBookingsCount(int bookingsCount) {
        this.bookingsCount = bookingsCount;
    }

    public void setUtilizationRate(double utilizationRate) {
        this.utilizationRate = utilizationRate;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public void setCourseCode(String courseCode) {
        this.courseCode = courseCode;
    }

    public void setCourseTitle(String courseTitle) {
        this.courseTitle = courseTitle;
    }

    public void setInstructorName(String instructorName) {
        this.instructorName = instructorName;
    }

    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }

    public LocalDate getBookingDate() {
        return bookingDate;
    }

    public void setBookingDate(LocalDate bookingDate) {
        this.bookingDate = bookingDate;
    }
}