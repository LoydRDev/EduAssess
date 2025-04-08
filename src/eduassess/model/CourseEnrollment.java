package eduassess.model;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.control.Button;

public class CourseEnrollment {
    private final StringProperty courseCode;
    private final StringProperty courseName;
    private final StringProperty yearLevel;
    private final StringProperty semester;
    private final StringProperty units;
    private final StringProperty status;
    private final StringProperty instructorName;
    private final StringProperty grade;
    private final Button courseEvaluationButton;
    private boolean isEvaluated;
    private boolean isPending;
    private final StringProperty prerequisites;

    public CourseEnrollment(String courseCode, String courseName, String yearLevel,
            String semester, String units, String status, String instructorName, boolean isEvaluated) {
        this(courseCode, courseName, yearLevel, semester, units, status, instructorName, isEvaluated, null);
    }

    public CourseEnrollment(String courseCode, String courseName, String yearLevel,
            String semester, String units, String status, String instructorName, boolean isEvaluated,
            String prerequisites) {
        this(courseCode, courseName, yearLevel, semester, units, status, instructorName, isEvaluated, prerequisites,
                "Not Graded");
    }

    public CourseEnrollment(String courseCode, String courseName, String yearLevel,
            String semester, String units, String status, String instructorName, boolean isEvaluated,
            String prerequisites, String grade) {
        this.courseCode = new SimpleStringProperty(courseCode);
        this.courseName = new SimpleStringProperty(courseName);
        this.yearLevel = new SimpleStringProperty(yearLevel);
        this.semester = new SimpleStringProperty(semester);
        this.units = new SimpleStringProperty(units);
        this.status = new SimpleStringProperty(status);
        this.instructorName = new SimpleStringProperty(instructorName != null ? instructorName : "Not Assigned");
        this.grade = new SimpleStringProperty(grade != null ? grade : "Not Graded");
        this.isEvaluated = isEvaluated;
        this.isPending = isEvaluated; // Set pending to match evaluated status from database
        this.courseEvaluationButton = new Button(getButtonText());
        this.prerequisites = new SimpleStringProperty(prerequisites != null ? prerequisites : "N/A");

        // Style the course evaluation button
        updateButtonStyle();
    }

    private String getButtonText() {
        String grade = this.grade.get();
        if (grade == null || grade.equals("Not Graded")) {
            if (isPending) {
                return "PENDING";
            } else if (!isEvaluated) {
                return "Get Evaluated";
            }
        }

        try {
            // Handle numeric grades (1.0-5.0 scale where ≤3.0 is passing)
            double numericGrade = Double.parseDouble(grade);
            if (numericGrade >= 1.0 && numericGrade <= 3.0) {
                return "PASSED";
            } else {
                return "FAILED";
            }
        } catch (NumberFormatException e) {
            // Handle non-numeric grades
            String upperGrade = grade.toUpperCase();
            if (upperGrade.equals("INC")) {
                return "INCOMPLETE";
            } else if (upperGrade.equals("DRP")) {
                return "DROPPED";
            } else if (upperGrade.equals("FAILED") || upperGrade.equals("F")) {
                return "FAILED";
            } else if (upperGrade.equals("PASSED") || upperGrade.equals("P")) {
                return "PASSED";
            }
        }

        // If we reach here, it means the grade is not recognized
        // Default to "Get Evaluated" only if not already evaluated
        return isEvaluated ? "FAILED" : "Get Evaluated";
    }

    private void updateButtonStyle() {
        String status = getButtonText();
        switch (status) {
            case "PASSED":
                courseEvaluationButton.setStyle(
                        "-fx-background-color: #4CAF50;" + // Green color for passed
                                "-fx-text-fill: white;" +
                                "-fx-font-weight: bold;" +
                                "-fx-padding: 5 15;" +
                                "-fx-cursor: default;" +
                                "-fx-opacity: 0.9;");
                courseEvaluationButton.setDisable(true);
                break;
            case "FAILED":
                courseEvaluationButton.setStyle(
                        "-fx-background-color: #f44336;" + // Red color for failed
                                "-fx-text-fill: white;" +
                                "-fx-font-weight: bold;" +
                                "-fx-padding: 5 15;" +
                                "-fx-cursor: default;" +
                                "-fx-opacity: 0.9;");
                courseEvaluationButton.setDisable(true);
                break;
            case "INCOMPLETE":
                courseEvaluationButton.setStyle(
                        "-fx-background-color: #ff9800;" + // Orange color for incomplete
                                "-fx-text-fill: white;" +
                                "-fx-font-weight: bold;" +
                                "-fx-padding: 5 15;" +
                                "-fx-cursor: default;" +
                                "-fx-opacity: 0.9;");
                courseEvaluationButton.setDisable(true);
                break;
            case "DROPPED":
                courseEvaluationButton.setStyle(
                        "-fx-background-color: #9e9e9e;" + // Gray color for dropped
                                "-fx-text-fill: white;" +
                                "-fx-font-weight: bold;" +
                                "-fx-padding: 5 15;" +
                                "-fx-cursor: default;" +
                                "-fx-opacity: 0.9;");
                courseEvaluationButton.setDisable(true);
                break;
            case "PENDING":
                courseEvaluationButton.setStyle(
                        "-fx-background-color: #FFA500;" + // Orange color for pending
                                "-fx-text-fill: white;" +
                                "-fx-font-weight: bold;" +
                                "-fx-padding: 5 15;" +
                                "-fx-cursor: default;");
                courseEvaluationButton.setDisable(true);
                break;
            default:
                courseEvaluationButton.setStyle(
                        "-fx-background-color: #2196F3;" + // Blue color for get evaluated
                                "-fx-text-fill: white;" +
                                "-fx-font-weight: bold;" +
                                "-fx-padding: 5 15;" +
                                "-fx-cursor: hand;");
                courseEvaluationButton.setDisable(false);
                break;
        }
    }

    // Getters for JavaFX properties
    public String getCourseCode() {
        return courseCode.get();
    }

    public StringProperty courseCodeProperty() {
        return courseCode;
    }

    // This method is redundant with courseCodeProperty() and causes confusion
    // Keeping it for backward compatibility but courseCodeProperty() should be used
    public StringProperty getCourseCodeProperty() {
        return courseCode;
    }

    public String getCourseName() {
        return courseName.get();
    }

    public StringProperty courseNameProperty() {
        return courseName;
    }

    // This method is redundant with courseNameProperty() and causes confusion
    // Keeping it for backward compatibility but courseNameProperty() should be used
    public StringProperty getCourseNameProperty() {
        return courseName;
    }

    public String getYearLevel() {
        return yearLevel.get();
    }

    public StringProperty yearLevelProperty() {
        return yearLevel;
    }

    public String getSemester() {
        return semester.get();
    }

    public StringProperty semesterProperty() {
        return semester;
    }

    public String getUnits() {
        return units.get();
    }

    public StringProperty unitsProperty() {
        return units;
    }

    // This method is redundant with unitsProperty() and causes confusion
    // Keeping it for backward compatibility but unitsProperty() should be used
    public StringProperty getUnitsProperty() {
        return units;
    }

    public String getStatus() {
        return status.get();
    }

    public StringProperty statusProperty() {
        return status;
    }

    // This method is redundant with statusProperty() and causes confusion
    // Keeping it for backward compatibility but statusProperty() should be used
    public StringProperty getStatusProperty() {
        return status;
    }

    public String getInstructorName() {
        return instructorName.get();
    }

    public StringProperty instructorNameProperty() {
        return instructorName;
    }

    // This method is redundant with instructorNameProperty() and causes confusion
    // Keeping it for backward compatibility but instructorNameProperty() should be
    // used
    public StringProperty getInstructorNameProperty() {
        return instructorName;
    }

    public StringProperty getGradeProperty() {
        return grade;
    }

    public Button getCourseEvaluationButton() {
        return courseEvaluationButton;
    }

    public boolean isEvaluated() {
        return isEvaluated;
    }

    public boolean isPending() {
        return isPending;
    }

    public void setEvaluated(boolean evaluated) {
        this.isEvaluated = evaluated;
        this.isPending = false;
        courseEvaluationButton.setText(getButtonText());
        updateButtonStyle();
    }

    public void setPending(boolean pending) {
        this.isPending = pending;
        courseEvaluationButton.setText(getButtonText());
        updateButtonStyle();
    }

    public void updateEvaluationButton() {
        courseEvaluationButton.setText(getButtonText());
        updateButtonStyle();
    }

    public String getGrade() {
        return grade.get();
    }

    public StringProperty gradeProperty() {
        return grade;
    }

    public void setGrade(String grade) {
        // Directly set the grade as provided by the instructor
        this.grade.set(grade);
        if (grade != null && !grade.equals("Not Graded")) {
            this.isEvaluated = true;
            this.isPending = false;
            courseEvaluationButton.setText(getButtonText());
            updateButtonStyle();
        }
    }

    public String getPrerequisites() {
        return prerequisites.get();
    }

    public StringProperty prerequisitesProperty() {
        return prerequisites;
    }
}