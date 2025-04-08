package eduassess.model;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.Button;
import java.time.LocalDateTime;

public class PendingEvaluation {
    private final StringProperty studentId;
    private final StringProperty studentName;
    private final StringProperty courseCode;
    private final StringProperty courseName;
    private final ObjectProperty<LocalDateTime> requestDate;
    private final StringProperty grade;
    private final Button submitButton;

    public PendingEvaluation(String studentId, String studentName, String courseCode,
            LocalDateTime requestDate) {
        this.studentId = new SimpleStringProperty(studentId);
        this.studentName = new SimpleStringProperty(studentName);
        this.courseCode = new SimpleStringProperty(courseCode);
        this.courseName = new SimpleStringProperty("");
        this.requestDate = new SimpleObjectProperty<>(requestDate);
        this.grade = new SimpleStringProperty("");
        this.submitButton = new Button("Submit Grade");

        // Style the submit button
        submitButton.setStyle(
                "-fx-background-color: #4CAF50;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-weight: bold;" +
                        "-fx-padding: 5 15;" +
                        "-fx-cursor: hand;");
    }

    // Constructor with course name
    public PendingEvaluation(String studentId, String studentName, String courseCode,
            String courseName, LocalDateTime requestDate) {
        this(studentId, studentName, courseCode, requestDate);
        this.courseName.set(courseName);
    }

    // Getters for JavaFX properties
    public String getStudentId() {
        return studentId.get();
    }

    public StringProperty studentIdProperty() {
        return studentId;
    }

    public String getStudentName() {
        return studentName.get();
    }

    public StringProperty studentNameProperty() {
        return studentName;
    }

    public String getCourseCode() {
        return courseCode.get();
    }

    public StringProperty courseCodeProperty() {
        return courseCode;
    }

    public String getCourseName() {
        return courseName.get();
    }

    public StringProperty courseNameProperty() {
        return courseName;
    }

    public void setCourseName(String courseName) {
        this.courseName.set(courseName);
    }

    public LocalDateTime getRequestDate() {
        return requestDate.get();
    }

    public ObjectProperty<LocalDateTime> requestDateProperty() {
        return requestDate;
    }

    public String getGrade() {
        return grade.get();
    }

    public void setGrade(String grade) {
        this.grade.set(grade);
    }

    public StringProperty gradeProperty() {
        return grade;
    }

    public Button getSubmitButton() {
        return submitButton;
    }
}