package eduassess.model;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class Assessment {
    private final StringProperty name;
    private final StringProperty subject;
    private final StringProperty deadline;
    private final StringProperty status;

    public Assessment(String name, String subject, String deadline, String status) {
        this.name = new SimpleStringProperty(name);
        this.subject = new SimpleStringProperty(subject);
        this.deadline = new SimpleStringProperty(deadline);
        this.status = new SimpleStringProperty(status);
    }

    // Property getters
    public StringProperty nameProperty() {
        return name;
    }

    public StringProperty subjectProperty() {
        return subject;
    }

    public StringProperty deadlineProperty() {
        return deadline;
    }

    public StringProperty statusProperty() {
        return status;
    }

    // Value getters
    public String getName() {
        return name.get();
    }

    public String getSubject() {
        return subject.get();
    }

    public String getDeadline() {
        return deadline.get();
    }

    public String getStatus() {
        return status.get();
    }

    // Value setters
    public void setName(String name) {
        this.name.set(name);
    }

    public void setSubject(String subject) {
        this.subject.set(subject);
    }

    public void setDeadline(String deadline) {
        this.deadline.set(deadline);
    }

    public void setStatus(String status) {
        this.status.set(status);
    }
}