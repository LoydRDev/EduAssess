package eduassess.model;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.IntegerProperty;
import java.time.LocalDateTime;

public class User {
    private final IntegerProperty id;
    private final StringProperty idNumber;
    private final StringProperty fullName;
    private final StringProperty email;
    private final StringProperty userType;
    private final StringProperty status;
    private String password;
    private LocalDateTime requestDate;
    private String courseCode;
    private String courseName;

    public User(String idNumber, String fullName, String email, String userType) {
        this.id = new SimpleIntegerProperty(0);
        this.idNumber = new SimpleStringProperty(idNumber);
        this.fullName = new SimpleStringProperty(fullName);
        this.email = new SimpleStringProperty(email);
        this.userType = new SimpleStringProperty(userType);
        this.status = new SimpleStringProperty("Active");
        this.requestDate = null;
        this.courseCode = null;
        this.courseName = null;
    }

    // Property getters
    public IntegerProperty idProperty() {
        return id;
    }

    public StringProperty idNumberProperty() {
        return idNumber;
    }

    public StringProperty fullNameProperty() {
        return fullName;
    }

    public StringProperty emailProperty() {
        return email;
    }

    public StringProperty userTypeProperty() {
        return userType;
    }

    public StringProperty statusProperty() {
        return status;
    }

    // Value getters
    public int getId() {
        return id.get();
    }

    public String getIdNumber() {
        return idNumber.get();
    }

    public String getFullName() {
        return fullName.get();
    }

    public String getEmail() {
        return email.get();
    }

    public String getUserType() {
        return userType.get();
    }

    public String getStatus() {
        return status.get();
    }

    public String getPassword() {
        return password;
    }

    public LocalDateTime getRequestDate() {
        return requestDate;
    }

    public String getCourseCode() {
        return courseCode;
    }

    public String getCourseName() {
        return courseName;
    }

    // Value setters
    public void setId(int id) {
        this.id.set(id);
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setStatus(String status) {
        this.status.set(status);
    }

    public void setIdNumber(String idNumber) {
        this.idNumber.set(idNumber);
    }

    public void setFullName(String fullName) {
        this.fullName.set(fullName);
    }

    public void setUserType(String userType) {
        this.userType.set(userType);
    }

    public void setRequestDate(LocalDateTime requestDate) {
        this.requestDate = requestDate;
    }

    public void setCourseCode(String courseCode) {
        this.courseCode = courseCode;
    }

    public void setCourseName(String courseName) {
        this.courseName = courseName;
    }

    // Utility methods
    public boolean isAdmin() {
        return "ADMIN".equals(getUserType());
    }

    public boolean isInstructor() {
        return "INSTRUCTOR".equals(getUserType());
    }

    public boolean isStudent() {
        return "STUDENT".equals(getUserType());
    }
}