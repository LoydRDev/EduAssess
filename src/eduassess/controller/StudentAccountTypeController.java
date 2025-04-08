package eduassess.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Dialog;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.VBox;
import javafx.scene.control.Label;
import javafx.event.ActionEvent;
import eduassess.dao.CourseDAO;
import java.io.IOException;
import java.util.Optional;
import java.util.List;
import eduassess.model.Course;
import eduassess.model.User;
import eduassess.util.SessionManager;
import javafx.animation.PauseTransition;
import javafx.util.Duration;

public class StudentAccountTypeController {
    private CourseDAO courseDAO;
    private String studentId;
    private String courseCode;

    public StudentAccountTypeController() {
        courseDAO = new CourseDAO();
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    private String getCurrentStudentId() {
        return studentId;
    }

    @FXML
    private void handleNewAccount(ActionEvent event) {
        // For new accounts, automatically set to First Year, First Semester
        String studentId = getCurrentStudentId();
        if (courseDAO.updateStudentYearAndSemester(studentId, "FirstYear", "FirstSemester")) {
            // Get available courses for first year, first semester
            List<Course> availableCourses = courseDAO.getAvailableCourses(studentId, "FirstYear", "FirstSemester");
            if (availableCourses != null && !availableCourses.isEmpty()) {
                boolean allEnrollmentsSuccessful = true;
                // Enroll in all available courses
                for (Course course : availableCourses) {
                    if (!courseDAO.enrollStudentInCourse(studentId, course.getCourseCode(), "FirstYear",
                            "FirstSemester")) {
                        allEnrollmentsSuccessful = false;
                        showError("Enrollment Error", "Failed to enroll in course: " + course.getCourseCode());
                    }
                }

                if (allEnrollmentsSuccessful) {
                    navigateToStudentDashboard(event);
                }
            } else {
                showError("Enrollment Error", "No courses available for First Year, First Semester.");
            }
        } else {
            showError("Update Error", "Failed to update student information.");
        }
    }

    @FXML
    private void handleOldAccount(ActionEvent event) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Select Year and Semester");
        dialog.setHeaderText("Please select your year level and semester");

        // Create the combo boxes
        ComboBox<String> yearCombo = new ComboBox<>();
        yearCombo.getItems().addAll("FirstYear", "SecondYear", "ThirdYear", "FourthYear");
        yearCombo.setPromptText("Select Year Level");

        ComboBox<String> semesterCombo = new ComboBox<>();
        semesterCombo.getItems().addAll("FirstSemester", "SecondSemester");
        semesterCombo.setPromptText("Select Semester");

        // Create layout
        VBox content = new VBox(10);
        content.getChildren().addAll(
                new Label("Year Level:"),
                yearCombo,
                new Label("Semester:"),
                semesterCombo);

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            String yearLevel = yearCombo.getValue();
            String semester = semesterCombo.getValue();

            if (yearLevel != null && semester != null) {
                String studentId = getCurrentStudentId();
                if (courseDAO.updateStudentYearAndSemester(studentId, yearLevel, semester)) {
                    // Get available courses for selected year and semester
                    List<Course> availableCourses = courseDAO.getAvailableCourses(studentId, yearLevel, semester);
                    if (availableCourses != null && !availableCourses.isEmpty()) {
                        boolean allEnrollmentsSuccessful = true;
                        // Enroll in all available courses
                        for (Course course : availableCourses) {
                            if (!courseDAO.enrollStudentInCourse(studentId, course.getCourseCode(), yearLevel,
                                    semester)) {
                                allEnrollmentsSuccessful = false;
                                showError("Enrollment Error", "Failed to enroll in course: " + course.getCourseCode());
                            }
                        }

                        if (allEnrollmentsSuccessful) {
                            navigateToStudentDashboard(event);
                        }
                    } else {
                        showError("Enrollment Error", "No courses available for " + yearLevel + ", " + semester);
                    }
                } else {
                    showError("Update Error", "Failed to update student information.");
                }
            }
        }
    }

    @FXML
    private void handleTransferee(ActionEvent event) {
        // For future development
        showAlert("Coming Soon", "Transferee account creation will be available soon.");
    }

    @FXML
    private void handleBack(ActionEvent event) {
        try {
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/eduassess/views/CreateAccount.fxml"));
            Parent createAccountView = loader.load();
            Scene scene = new Scene(createAccountView);
            stage.setScene(scene);
            stage.centerOnScreen();
        } catch (IOException e) {
            e.printStackTrace();
            showError("Navigation Error", "Could not return to create account screen.");
        }
    }

    private void navigateToStudentDashboard(ActionEvent event) {
        try {
            // Get the current user from session or create a new session
            final User currentUser;
            User tempUser = SessionManager.getCurrentUser();
            if (tempUser == null || !tempUser.getIdNumber().equals(studentId)) {
                // Fetch user data from database if needed
                CourseDAO courseDAO = new CourseDAO();
                tempUser = courseDAO.getStudentById(studentId);
                if (tempUser != null) {
                    SessionManager.setCurrentUser(tempUser);
                }
            }

            if (tempUser == null) {
                showError("Session Error", "Could not initialize user session.");
                return;
            }

            currentUser = tempUser; // Make currentUser effectively final

            // Create a pause transition for visual feedback
            PauseTransition pause = new PauseTransition(Duration.seconds(1.5));
            pause.setOnFinished(e -> {
                try {
                    Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                    FXMLLoader loader = new FXMLLoader(
                            getClass().getResource("/eduassess/views/StudentDashboard.fxml"));
                    Parent dashboardView = loader.load();

                    StudentDashboardController controller = loader.getController();
                    controller.initData(currentUser);

                    Scene scene = new Scene(dashboardView);
                    stage.setScene(scene);
                    stage.centerOnScreen();
                } catch (IOException ex) {
                    ex.printStackTrace();
                    showError("Navigation Error", "Could not open student dashboard.");
                }
            });
            pause.play();
        } catch (Exception ex) {
            ex.printStackTrace();
            showError("System Error", "An unexpected error occurred.");
        }
    }

    private void showError(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}