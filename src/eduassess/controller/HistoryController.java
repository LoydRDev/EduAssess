package eduassess.controller;

import eduassess.model.CourseEnrollment;
import eduassess.model.User;
import eduassess.dao.CourseDAO;
import eduassess.util.SessionManager;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.collections.FXCollections;
import javafx.scene.control.cell.PropertyValueFactory;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.io.IOException;
import javafx.application.Platform;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

public class HistoryController implements Initializable {
    @FXML
    private Label userNameLabel;
    @FXML
    private Label studentIdLabel;
    @FXML
    private Label passedCoursesLabel;
    @FXML
    private Label totalUnitsLabel;

    @FXML
    private TableView<CourseEnrollment> passedCoursesTable;
    @FXML
    private TableColumn<CourseEnrollment, String> courseCodeColumn;
    @FXML
    private TableColumn<CourseEnrollment, String> courseTitleColumn;
    @FXML
    private TableColumn<CourseEnrollment, String> instructorColumn;
    @FXML
    private TableColumn<CourseEnrollment, String> unitsColumn;
    @FXML
    private TableColumn<CourseEnrollment, String> yearLevelColumn;
    @FXML
    private TableColumn<CourseEnrollment, String> semesterColumn;
    @FXML
    private TableColumn<CourseEnrollment, String> gradeColumn;

    private User currentUser;
    private CourseDAO courseDAO;

    public HistoryController() {
        courseDAO = new CourseDAO();
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Initialize the table columns
        setupTable();

        // Set the user name from the current session
        User currentUser = SessionManager.getCurrentUser();
        if (currentUser != null) {
            initData(currentUser);
        }
    }

    private void setupTable() {
        // Set up cell value factories
        courseCodeColumn.setCellValueFactory(new PropertyValueFactory<>("courseCode"));
        courseTitleColumn.setCellValueFactory(new PropertyValueFactory<>("courseName"));
        instructorColumn.setCellValueFactory(new PropertyValueFactory<>("instructorName"));
        unitsColumn.setCellValueFactory(new PropertyValueFactory<>("units"));
        yearLevelColumn.setCellValueFactory(new PropertyValueFactory<>("yearLevel"));
        semesterColumn.setCellValueFactory(new PropertyValueFactory<>("semester"));
        gradeColumn.setCellValueFactory(new PropertyValueFactory<>("grade"));

        // Format year level and semester columns to be more readable
        yearLevelColumn.setCellFactory(column -> new TableCell<CourseEnrollment, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(formatYearLevel(item));
                }
            }
        });

        semesterColumn.setCellFactory(column -> new TableCell<CourseEnrollment, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(formatSemester(item));
                }
            }
        });
    }

    public void initData(User user) {
        this.currentUser = user;
        userNameLabel.setText(user.getFullName());
        studentIdLabel.setText("ID: " + user.getIdNumber());
        loadHistoryData();
    }

    private void loadHistoryData() {
        try {
            if (currentUser == null) {
                showAlert(Alert.AlertType.ERROR, "Error", "No user data available");
                return;
            }

            // Load passed courses
            List<CourseEnrollment> passedCourses = courseDAO.getStudentPassedCourses(currentUser.getIdNumber());

            if (passedCourses != null && !passedCourses.isEmpty()) {
                // Update table with passed courses
                passedCoursesTable.setItems(FXCollections.observableArrayList(passedCourses));

                // Update statistics
                passedCoursesLabel.setText(String.valueOf(passedCourses.size()));

                // Calculate total units
                int totalUnits = passedCourses.stream()
                        .mapToInt(c -> {
                            try {
                                return Integer.parseInt(c.getUnits().trim());
                            } catch (NumberFormatException e) {
                                return 0;
                            }
                        })
                        .sum();
                totalUnitsLabel.setText(String.valueOf(totalUnits));
            } else {
                // Clear the table if no passed courses
                passedCoursesTable.setItems(FXCollections.observableArrayList());
                passedCoursesLabel.setText("0");
                totalUnitsLabel.setText("0");
                showAlert(Alert.AlertType.INFORMATION, "No Passed Courses", "You don't have any passed courses yet.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to load history data: " + e.getMessage());
        }
    }

    private String formatYearLevel(String yearLevel) {
        if (yearLevel == null || yearLevel.isEmpty())
            return "Unknown Year";
        return switch (yearLevel) {
            case "FirstYear" -> "First Year";
            case "SecondYear" -> "Second Year";
            case "ThirdYear" -> "Third Year";
            case "FourthYear" -> "Fourth Year";
            default -> yearLevel;
        };
    }

    private String formatSemester(String semester) {
        if (semester == null || semester.isEmpty())
            return "Unknown Semester";
        return switch (semester) {
            case "FirstSemester" -> "First Semester";
            case "SecondSemester" -> "Second Semester";
            default -> semester;
        };
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML
    private void handleDashboard() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/eduassess/views/StudentDashboard.fxml"));
            Parent dashboardView = loader.load();
            StudentDashboardController controller = loader.getController();
            controller.initData(currentUser);

            Stage stage = (Stage) userNameLabel.getScene().getWindow();
            Scene scene = new Scene(dashboardView);
            stage.setScene(scene);
            stage.centerOnScreen();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to load dashboard view");
        }
    }

    @FXML
    private void handleRecommendations() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/eduassess/views/Recommendations.fxml"));
            Parent recommendationsView = loader.load();
            RecommendationsController controller = loader.getController();
            controller.initData(currentUser);

            Stage stage = (Stage) userNameLabel.getScene().getWindow();
            Scene scene = new Scene(recommendationsView);
            stage.setScene(scene);
            stage.centerOnScreen();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to load recommendations view");
        }
    }

    @FXML
    private void handleEnrollment() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/eduassess/views/NotEnrolledCourses.fxml"));
            Parent enrollmentView = loader.load();
            NotEnrolledCoursesController controller = loader.getController();
            controller.initData(currentUser);

            Stage stage = (Stage) userNameLabel.getScene().getWindow();
            Scene scene = new Scene(enrollmentView);
            stage.setScene(scene);
            stage.centerOnScreen();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to load enrollment view");
        }
    }
    
    private void showErrorAlert(String title, String content) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(content);
            alert.show(); // Using show() instead of showAndWait() to avoid blocking
        });
    }

    @FXML
    private void handleLogout() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirm Logout");
        alert.setHeaderText("Logout");
        alert.setContentText("Are you sure you want to logout?");

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    // Clear the current user session
                    currentUser = null;

                    // Get the current stage
                    Stage currentStage = (Stage) userNameLabel.getScene().getWindow();

                    // Load the login view
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/eduassess/views/Login.fxml"));
                    Parent loginView = loader.load();

                    // Create a new scene with the login view
                    Scene loginScene = new Scene(loginView);

                    // Set the scene on the current stage
                    currentStage.setScene(loginScene);
                    currentStage.setTitle("EduAssess - Login");
                    currentStage.centerOnScreen();

                } catch (IOException e) {
                    e.printStackTrace();
                    showErrorAlert("Error", "Failed to return to login screen. Please restart the application.");
                }
            }
        });
    }
}