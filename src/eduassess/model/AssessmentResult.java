package eduassess.model;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class AssessmentResult {
    private final StringProperty assessmentName;
    private final StringProperty subject;
    private final StringProperty score;
    private final StringProperty completionDate;

    public AssessmentResult(String assessmentName, String subject, String score, String completionDate) {
        this.assessmentName = new SimpleStringProperty(assessmentName);
        this.subject = new SimpleStringProperty(subject);
        this.score = new SimpleStringProperty(score);
        this.completionDate = new SimpleStringProperty(completionDate);
    }

    // Property getters
    public StringProperty assessmentNameProperty() {
        return assessmentName;
    }

    public StringProperty subjectProperty() {
        return subject;
    }

    public StringProperty scoreProperty() {
        return score;
    }

    public StringProperty completionDateProperty() {
        return completionDate;
    }

    // Value getters
    public String getAssessmentName() {
        return assessmentName.get();
    }

    public String getSubject() {
        return subject.get();
    }

    public String getScore() {
        return score.get();
    }

    public String getCompletionDate() {
        return completionDate.get();
    }

    // Value setters
    public void setAssessmentName(String assessmentName) {
        this.assessmentName.set(assessmentName);
    }

    public void setSubject(String subject) {
        this.subject.set(subject);
    }

    public void setScore(String score) {
        this.score.set(score);
    }

    public void setCompletionDate(String completionDate) {
        this.completionDate.set(completionDate);
    }
}