package eduassess.model;

import javafx.beans.property.*;

public class Course {
    private StringProperty courseCode;
    private StringProperty courseName;
    private IntegerProperty pendingCount;
    private StringProperty instructorId;
    private StringProperty prerequisites;
    private IntegerProperty units;
    private DoubleProperty recommendationScore;
    private StringProperty yearLevel;
    private StringProperty semester;
    private BooleanProperty failedCourse;
    private boolean isFailedCourse;

    public boolean isFailedCourse() {
        return isFailedCourse;
    }

    public void setFailedCourse(boolean failedCourse) {
        isFailedCourse = failedCourse;
    }

    public Course(String courseCode, String courseName, String prerequisites, int units, double recommendationScore,
            String yearLevel, String semester) {
        this.courseCode = new SimpleStringProperty(courseCode);
        this.courseName = new SimpleStringProperty(courseName);
        this.prerequisites = new SimpleStringProperty(prerequisites);
        this.units = new SimpleIntegerProperty(units);
        this.recommendationScore = new SimpleDoubleProperty(recommendationScore);
        this.pendingCount = new SimpleIntegerProperty(0);
        this.instructorId = new SimpleStringProperty("");
        this.yearLevel = new SimpleStringProperty(yearLevel);
        this.semester = new SimpleStringProperty(semester);
        this.failedCourse = new SimpleBooleanProperty(false);
        this.isFailedCourse = false;
    }

    // Add a simpler constructor for evaluation requests
    public Course(String courseCode, String courseName, int pendingCount) {
        this.courseCode = new SimpleStringProperty(courseCode);
        this.courseName = new SimpleStringProperty(courseName);
        this.pendingCount = new SimpleIntegerProperty(pendingCount);
        this.instructorId = new SimpleStringProperty("");
        this.prerequisites = new SimpleStringProperty("");
        this.units = new SimpleIntegerProperty(0);
        this.recommendationScore = new SimpleDoubleProperty(0.0);
        this.yearLevel = new SimpleStringProperty("");
        this.semester = new SimpleStringProperty("");
        this.failedCourse = new SimpleBooleanProperty(false);
        this.isFailedCourse = false;
    }

    // Property Getters
    public StringProperty courseCodeProperty() {
        return courseCode;
    }

    public StringProperty courseNameProperty() {
        return courseName;
    }

    public IntegerProperty pendingCountProperty() {
        return pendingCount;
    }

    public StringProperty instructorIdProperty() {
        return instructorId;
    }

    public StringProperty prerequisitesProperty() {
        return prerequisites;
    }

    public IntegerProperty unitsProperty() {
        return units;
    }

    public DoubleProperty recommendationScoreProperty() {
        return recommendationScore;
    }

    // Value Getters
    public String getCourseCode() {
        return courseCode.get();
    }

    public String getCourseName() {
        return courseName.get();
    }

    public int getPendingCount() {
        return pendingCount.get();
    }

    public String getInstructorId() {
        return instructorId.get();
    }

    public String getPrerequisites() {
        return prerequisites.get();
    }

    public int getUnits() {
        return units.get();
    }

    public double getRecommendationScore() {
        return recommendationScore.get();
    }

    public String getYearLevel() {
        return yearLevel.get();
    }

    public String getSemester() {
        return semester.get();
    }

    public StringProperty yearLevelProperty() {
        return yearLevel;
    }

    public StringProperty semesterProperty() {
        return semester;
    }

    // Setters
    public void setCourseCode(String courseCode) {
        this.courseCode.set(courseCode);
    }

    public void setCourseName(String courseName) {
        this.courseName.set(courseName);
    }

    public void setPendingCount(int pendingCount) {
        this.pendingCount.set(pendingCount);
    }

    public void setInstructorId(String instructorId) {
        this.instructorId.set(instructorId);
    }

    public void setPrerequisites(String prerequisites) {
        this.prerequisites.set(prerequisites);
    }

    public void setUnits(int units) {
        this.units.set(units);
    }

    public void setRecommendationScore(double recommendationScore) {
        this.recommendationScore.set(recommendationScore);
    }

    public BooleanProperty failedCourseProperty() {
        return failedCourse;
    }

    @Override
    public String toString() {
        return getCourseCode() + " - " + getCourseName()
                + (getPendingCount() > 0 ? " (" + getPendingCount() + " pending)" : "")
                + (isFailedCourse() ? " (Failed)" : "");
    }
}