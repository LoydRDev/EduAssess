package eduassess.controller;

import eduassess.model.Course;
import eduassess.model.CourseEnrollment;
import eduassess.model.User;
import eduassess.dao.CourseDAO;
import eduassess.util.SessionManager;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.cell.PropertyValueFactory;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import javafx.util.Callback;
import java.io.IOException;

public class NotEnrolledCoursesController implements Initializable {
    @FXML
    private TableView<Course> notEnrolledCoursesTable;
    @FXML
    private TableColumn<Course, String> courseCodeColumn;
    @FXML
    private TableColumn<Course, String> courseTitleColumn;
    @FXML
    private TableColumn<Course, String> prerequisitesColumn;
    @FXML
    private TableColumn<Course, String> unitsColumn;
    @FXML
    private TableColumn<Course, Void> enrollColumn;

    private User currentUser;
    private CourseDAO courseDAO;
    private List<CourseEnrollment> enrolledCourses;

    public NotEnrolledCoursesController() {
        courseDAO = new CourseDAO();
        currentUser = SessionManager.getCurrentUser();
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupTable();

        // Get current user from session
        currentUser = SessionManager.getCurrentUser();
        if (currentUser != null) {
            loadAvailableCourses();
        }
    }

    public void initData(User user) {
        this.currentUser = user;
        loadAvailableCourses();
    }

    private void setupTable() {
        // Set up cell value factories
        courseCodeColumn.setCellValueFactory(new PropertyValueFactory<>("courseCode"));
        courseTitleColumn.setCellValueFactory(new PropertyValueFactory<>("courseName"));
        prerequisitesColumn.setCellValueFactory(new PropertyValueFactory<>("prerequisites"));
        unitsColumn.setCellValueFactory(new PropertyValueFactory<>("units"));

        // Format prerequisites column
        prerequisitesColumn.setCellFactory(column -> new TableCell<Course, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || item.trim().isEmpty() || item.equalsIgnoreCase("n/a")) {
                    setText("None");
                } else {
                    setText(item);
                }
            }
        });

        // Setup enroll column with button
        enrollColumn.setCellFactory(col -> new TableCell<Course, Void>() {
            private final Button enrollButton = new Button("Enroll");
            {
                enrollButton.setStyle(
                        "-fx-background-color: #2196F3;" + // Blue color
                                "-fx-text-fill: white;" +
                                "-fx-font-weight: bold;" +
                                "-fx-padding: 5 15;" +
                                "-fx-cursor: hand;");

                enrollButton.setOnAction(event -> {
                    Course course = getTableView().getItems().get(getIndex());
                    if (course != null) {
                        handleEnrollment(course);
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(enrollButton);
                }
            }
        });

        // Set table style
        notEnrolledCoursesTable.setStyle(
                "-fx-background-color: white;" +
                        "-fx-border-color: #e2e8f0;" +
                        "-fx-border-radius: 4px;");

        // Set column resize policy
        notEnrolledCoursesTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    private void loadAvailableCourses() {
        try {
            if (currentUser == null) {
                showErrorAlert("Error", "No user data available");
                return;
            }

            // Get all courses
            List<Course> allCourses = courseDAO.getAllCourses();

            // Get enrolled courses
            enrolledCourses = courseDAO.getStudentCourses(currentUser.getIdNumber());

            // Filter out already enrolled courses
            List<String> enrolledCourseCodes = enrolledCourses.stream()
                    .map(CourseEnrollment::getCourseCode)
                    .collect(Collectors.toList());

            List<Course> availableCourses = allCourses.stream()
                    .filter(course -> !enrolledCourseCodes.contains(course.getCourseCode()))
                    .collect(Collectors.toList());

            // Update table with available courses
            notEnrolledCoursesTable.setItems(FXCollections.observableArrayList(availableCourses));

        } catch (Exception e) {
            e.printStackTrace();
            showErrorAlert("Error", "Failed to load available courses: " + e.getMessage());
        }
    }

    private void handleEnrollment(Course course) {
        if (currentUser == null || course == null) {
            showErrorAlert("Error", "Invalid course or user data");
            return;
        }

        try {
            // Check prerequisites
            String prerequisites = course.getPrerequisites();
            if (prerequisites != null && !prerequisites.isEmpty() && !prerequisites.equalsIgnoreCase("n/a")) {
                String missingPrerequisites = getMissingPrerequisites(prerequisites, enrolledCourses);
                if (!missingPrerequisites.isEmpty()) {
                    showErrorAlert("Enrollment Error",
                            "Prerequisites not met for this course:\n" + missingPrerequisites);
                    return;
                }
            }

            // Get year level and semester from the course
            String yearLevel = course.getYearLevel();
            String semester = course.getSemester();

            // Enroll student in the course
            boolean success = courseDAO.enrollStudentInCourse(currentUser.getIdNumber(), course.getCourseCode(),
                    yearLevel, semester);

            if (success) {
                showAlert(Alert.AlertType.INFORMATION, "Success", "Successfully enrolled in " + course.getCourseName());
                loadAvailableCourses(); // Refresh the available courses list
            } else {
                showErrorAlert("Error", "Failed to enroll in course. Please try again.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            showErrorAlert("Error", "An error occurred during enrollment: " + e.getMessage());
        }
    }

    private String getMissingPrerequisites(String prerequisites, List<CourseEnrollment> enrolledCourses) {
        if (prerequisites == null || prerequisites.isEmpty() || prerequisites.equalsIgnoreCase("n/a")) {
            return "";
        }

        if (enrolledCourses == null || enrolledCourses.isEmpty()) {
            return prerequisites;
        }

        String[] requiredCourses = prerequisites.split(",");
        StringBuilder missingPrerequisites = new StringBuilder();

        for (String prerequisite : requiredCourses) {
            prerequisite = prerequisite.trim();
            boolean prerequisiteMet = false;

            for (CourseEnrollment enrolledCourse : enrolledCourses) {
                if (enrolledCourse.getCourseCode().equals(prerequisite)) {
                    String grade = enrolledCourse.getGrade();
                    if (grade == null || grade.isEmpty() || grade.equals("Not Graded")) {
                        missingPrerequisites.append("- ").append(prerequisite)
                                .append(" (not yet graded)\n");
                    } else {
                        try {
                            // Try to parse as numeric grade
                            double numericGrade = Double.parseDouble(grade);
                            if (numericGrade >= 75.0) { // Passing grade threshold
                                prerequisiteMet = true;
                                break;
                            }
                        } catch (NumberFormatException e) {
                            // Handle text-based grades
                            String upperGrade = grade.toUpperCase();
                            if (upperGrade.equals("PASSED") || upperGrade.equals("P") ||
                                    upperGrade.equals("SATISFACTORY") || upperGrade.equals("S")) {
                                prerequisiteMet = true;
                                break;
                            }
                        }
                        if (!prerequisiteMet) {
                            missingPrerequisites.append("- ").append(prerequisite)
                                    .append(" (grade requirement not met)\n");
                        }
                    }
                    break;
                }
            }

            if (!prerequisiteMet && !missingPrerequisites.toString().contains(prerequisite)) {
                missingPrerequisites.append("- ").append(prerequisite)
                        .append(" (not taken)\n");
            }
        }

        return missingPrerequisites.toString();
    }

    private void showErrorAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    @FXML
    private void handleDashboard() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/eduassess/views/StudentDashboard.fxml"));
            Parent dashboardView = loader.load();
            StudentDashboardController controller = loader.getController();
            controller.initData(currentUser);

            Stage stage = (Stage) notEnrolledCoursesTable.getScene().getWindow();
            Scene scene = new Scene(dashboardView);
            stage.setScene(scene);
            stage.centerOnScreen();
        } catch (IOException e) {
            e.printStackTrace();
            showErrorAlert("Error", "Failed to return to dashboard");
        }
    }
}