package eduassess.model;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.StringProperty;

public class Grade {
    private final StringProperty studentId;
    private final StringProperty studentName;
    private final StringProperty courseCode;
    private final DoubleProperty grade;
    private final StringProperty date;
    private final StringProperty status;

    public Grade(String studentId, String studentName, String courseCode, double grade, String date, String status) {
        this.studentId = new SimpleStringProperty(studentId);
        this.studentName = new SimpleStringProperty(studentName);
        this.courseCode = new SimpleStringProperty(courseCode);
        this.grade = new SimpleDoubleProperty(grade);
        this.date = new SimpleStringProperty(date);
        this.status = new SimpleStringProperty(status);
    }

    // Property getters
    public StringProperty studentIdProperty() {
        return studentId;
    }

    public StringProperty studentNameProperty() {
        return studentName;
    }

    public StringProperty courseCodeProperty() {
        return courseCode;
    }

    public DoubleProperty gradeProperty() {
        return grade;
    }

    public StringProperty dateProperty() {
        return date;
    }

    public StringProperty statusProperty() {
        return status;
    }

    // Value getters
    public String getStudentId() {
        return studentId.get();
    }

    public String getStudentName() {
        return studentName.get();
    }

    public String getCourseCode() {
        return courseCode.get();
    }

    public double getGrade() {
        return grade.get();
    }

    public String getDate() {
        return date.get();
    }

    public String getStatus() {
        return status.get();
    }
}