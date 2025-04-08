package eduassess.model;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ActivityLog {
    private final StringProperty timestamp;
    private final StringProperty user;
    private final StringProperty action;
    private final StringProperty details;

    public ActivityLog(LocalDateTime timestamp, String user, String action, String details) {
        this.timestamp = new SimpleStringProperty(timestamp.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        this.user = new SimpleStringProperty(user);
        this.action = new SimpleStringProperty(action);
        this.details = new SimpleStringProperty(details);
    }

    // Getters for properties
    public StringProperty timestampProperty() {
        return timestamp;
    }

    public StringProperty userProperty() {
        return user;
    }

    public StringProperty actionProperty() {
        return action;
    }

    public StringProperty detailsProperty() {
        return details;
    }

    // Getters for values
    public String getTimestamp() {
        return timestamp.get();
    }

    public String getUser() {
        return user.get();
    }

    public String getAction() {
        return action.get();
    }

    public String getDetails() {
        return details.get();
    }
}