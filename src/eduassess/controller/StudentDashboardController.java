package eduassess.controller;

import eduassess.model.CourseEnrollment;
import eduassess.model.User;
import eduassess.model.Course;
import eduassess.dao.CourseDAO;
import eduassess.util.SessionManager;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.collections.FXCollections;
import javafx.scene.control.cell.PropertyValueFactory;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Optional;
import java.util.Set;
import java.util.HashSet;
import javafx.scene.layout.VBox;
import javafx.event.ActionEvent;
import javafx.scene.layout.HBox;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.Node;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.geometry.Pos;
import javafx.stage.Stage;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import javafx.application.Platform;
import javafx.scene.control.TableCell;
import javafx.scene.control.Button;

public class StudentDashboardController implements Initializable {
    @FXML
    private Label userNameLabel;
    @FXML
    private Label studentIdLabel;
    @FXML
    private Label yearSemesterLabel;
    @FXML
    private Label enrolledCoursesLabel;
    @FXML
    private Label totalUnitsLabel;

    @FXML
    private TableView<CourseEnrollment> enrolledCoursesTable;
    @FXML
    private TableColumn<CourseEnrollment, String> courseCodeColumn;
    @FXML
    private TableColumn<CourseEnrollment, String> courseTitleColumn;
    @FXML
    private TableColumn<CourseEnrollment, String> instructorColumn;
    @FXML
    private TableColumn<CourseEnrollment, String> unitsColumn;
    @FXML
    private TableColumn<CourseEnrollment, String> statusColumn;
    @FXML
    private TableColumn<CourseEnrollment, String> gradeColumn;
    @FXML
    private TableColumn<CourseEnrollment, Void> evaluateColumn;

    private User currentUser;
    private CourseDAO courseDAO;
    private Course course;

    public StudentDashboardController() {
        courseDAO = new CourseDAO();
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Initialize labels with default values
        if (userNameLabel != null)
            userNameLabel.setText("");
        if (studentIdLabel != null)
            studentIdLabel.setText("");
        if (yearSemesterLabel != null)
            yearSemesterLabel.setText("");
        if (enrolledCoursesLabel != null)
            enrolledCoursesLabel.setText("0");
        if (totalUnitsLabel != null)
            totalUnitsLabel.setText("0");

        setupTable();

        // Set the user name from the current session
        User currentUser = SessionManager.getCurrentUser();
        if (currentUser != null) {
            initData(currentUser);
        }
    }

    private void setupTable() {
        try {
            // Set up cell value factories with null checks
            if (courseCodeColumn != null)
                courseCodeColumn.setCellValueFactory(new PropertyValueFactory<>("courseCode"));
            if (courseTitleColumn != null)
                courseTitleColumn.setCellValueFactory(new PropertyValueFactory<>("courseName"));
            if (instructorColumn != null)
                instructorColumn.setCellValueFactory(new PropertyValueFactory<>("instructorName"));
            if (unitsColumn != null)
                unitsColumn.setCellValueFactory(new PropertyValueFactory<>("units"));
            if (statusColumn != null)
                statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
            if (gradeColumn != null)
                gradeColumn.setCellValueFactory(new PropertyValueFactory<>("grade"));

            // Setup the Status column (formerly Get Evaluated)
            if (evaluateColumn != null) {
                evaluateColumn.setText("Status");
                evaluateColumn.setCellFactory(column -> new TableCell<CourseEnrollment, Void>() {
                    @Override
                    protected void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty) {
                            setGraphic(null);
                        } else {
                            CourseEnrollment course = getTableRow().getItem();
                            if (course != null) {
                                Button evaluationButton = course.getCourseEvaluationButton();
                                if (evaluationButton != null) {
                                    setGraphic(evaluationButton);
                                    evaluationButton.setOnAction(event -> handleGetEvaluated(course));
                                }
                            }
                        }
                    }
                });
            }

            // Set table style
            if (enrolledCoursesTable != null) {
                enrolledCoursesTable.setStyle(
                        "-fx-background-color: white;" +
                                "-fx-border-color: #e2e8f0;" +
                                "-fx-border-radius: 4px;");

                // Set column resize policy
                enrolledCoursesTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

                // Set column widths
                double tableWidth = enrolledCoursesTable.getPrefWidth();
                if (courseCodeColumn != null)
                    courseCodeColumn.setPrefWidth(tableWidth * 0.15);
                if (courseTitleColumn != null)
                    courseTitleColumn.setPrefWidth(tableWidth * 0.25);
                if (instructorColumn != null)
                    instructorColumn.setPrefWidth(tableWidth * 0.20);
                if (unitsColumn != null)
                    unitsColumn.setPrefWidth(tableWidth * 0.10);
                if (statusColumn != null)
                    statusColumn.setPrefWidth(tableWidth * 0.10);
                if (gradeColumn != null)
                    gradeColumn.setPrefWidth(tableWidth * 0.10);
                if (evaluateColumn != null)
                    evaluateColumn.setPrefWidth(tableWidth * 0.10);
            }
        } catch (Exception e) {
            e.printStackTrace();
            showErrorAlert("Error", "Failed to setup table: " + e.getMessage());
        }
    }

    @FXML
    private void handleEnrollment() {
        try {
            // Get current student's year and semester
            String yearSemester = courseDAO.getStudentYearAndSemester(currentUser.getIdNumber());
            if (yearSemester == null) {
                showErrorAlert("Error", "Could not determine student's year and semester");
                return;
            }

            // Check unit restrictions
            Map<String, Integer> restrictions = getUnitRestrictions();
            int maxUnits = restrictions.getOrDefault(yearSemester, 0);

            if (maxUnits == 0) {
                showErrorAlert("Error", "No unit restrictions defined for your year and semester");
                return;
            }

            // Get current enrolled units
            int currentUnits = courseDAO.getStudentTotalUnits(currentUser.getIdNumber());
            if (currentUnits >= maxUnits) {
                showErrorAlert("Enrollment Error",
                        "You have reached the maximum allowed units (" + maxUnits + ") for " + yearSemester);
                return;
            }

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
            showErrorAlert("Error", "Failed to load enrollment view");
        }
    }

    private void handleGetEvaluated(CourseEnrollment course) {
        if (course == null || currentUser == null) {
            showErrorAlert("Error", "Invalid course or user data");
            return;
        }

        try {
            // Check if course is already being evaluated
            if (course.isPending()) {
                showAlert(Alert.AlertType.INFORMATION, "Info", "Evaluation request is already pending");
                return;
            }

            // Request evaluation from instructor
            boolean success = courseDAO.requestEvaluation(currentUser.getIdNumber(), course.getCourseCode());

            if (success) {
                course.setPending(true); // Set the button to pending state
                showAlert(Alert.AlertType.INFORMATION, "Success", "Evaluation request sent to instructor");
                // Don't refresh the entire dashboard, just update the button state
                enrolledCoursesTable.refresh();
            } else {
                showErrorAlert("Error", "Failed to request evaluation");
            }
        } catch (Exception e) {
            e.printStackTrace();
            showErrorAlert("Error", "An error occurred while requesting evaluation");
        }
    }

    public void initData(User user) {
        this.currentUser = user;
        userNameLabel.setText(user.getFullName());
        studentIdLabel.setText("ID: " + user.getIdNumber());
        studentIdLabel.setStyle("-fx-font-weight: bold");
        loadDashboardData();
    }

    private Map<String, Integer> getUnitRestrictions() {
        Map<String, Integer> restrictions = new HashMap<>();
        restrictions.put("FirstYear-FirstSemester", 23);
        restrictions.put("FirstYear-SecondSemester", 23);
        restrictions.put("SecondYear-FirstSemester", 23);
        restrictions.put("SecondYear-SecondSemester", 23);
        restrictions.put("ThirdYear-FirstSemester", 24);
        restrictions.put("ThirdYear-SecondSemester", 24);
        restrictions.put("FourthYear-FirstSemester", 12);
        restrictions.put("FourthYear-SecondSemester", 9);
        return restrictions;
    }

    private void loadDashboardData() {
        try {
            if (currentUser == null) {
                showErrorAlert("Error", "No user data available");
                clearDashboard();
                return;
            }

            // Load enrolled courses with their actual pending status from the database
            List<CourseEnrollment> courses = courseDAO.getStudentCourses(currentUser.getIdNumber());

            // Reset evaluation status for re-enrolled courses
            for (CourseEnrollment course : courses) {
                if (course.getGrade() != null && course.getGrade().matches("\\d+\\.\\d*")) {
                    double numericGrade = Double.parseDouble(course.getGrade());
                    if (numericGrade > 3.0) { // If it's a failed grade
                        course.setGrade("Not Graded"); // Reset grade for re-enrolled course
                        course.setPending(false); // Reset pending status
                        course.updateEvaluationButton(); // Update button state
                    }
                }
            }

            // Check if all courses are passed and update year/semester if needed
            if (!courses.isEmpty()) {
                boolean allCoursesPassed = true;
                boolean hasIncompleteGrades = false;
                List<String> failedCourses = new ArrayList<>();

                for (CourseEnrollment course : courses) {
                    String grade = course.getGrade();
                    if (grade == null || grade.equals("Not Graded")) {
                        allCoursesPassed = false;
                        break;
                    } else if (grade.equals("INC") || grade.equals("DRP")) {
                        hasIncompleteGrades = true;
                        allCoursesPassed = false;
                        break;
                    } else if (grade.matches("\\d+\\.\\d*")) {
                        double numericGrade = Double.parseDouble(grade);
                        if (numericGrade > 3.0) { // Failed if grade is greater than 3.0
                            allCoursesPassed = false;
                            failedCourses.add(course.getCourseCode() + " - " + course.getCourseName());
                        }
                    } else if (grade.equals("FAILED")) {
                        allCoursesPassed = false;
                        failedCourses.add(course.getCourseCode() + " - " + course.getCourseName());
                    }
                }

                // If there are failed courses but we're not breaking early, show which ones
                // failed
                if (!allCoursesPassed && !failedCourses.isEmpty()) {
                    StringBuilder message = new StringBuilder("The following courses need to be retaken:\n");
                    for (String failedCourse : failedCourses) {
                        message.append("- ").append(failedCourse).append("\n");
                    }
                    showAlert(Alert.AlertType.INFORMATION, "Course Status", message.toString());
                }

                if (allCoursesPassed && !hasIncompleteGrades) {
                    // Get current year and semester
                    String currentYearLevel = courses.get(0).getYearLevel();
                    String currentSemester = courses.get(0).getSemester();

                    // Calculate next semester and year level
                    String tempYearLevel = currentYearLevel;
                    String tempSemester = currentSemester;

                    if (currentSemester.equals("FirstSemester")) {
                        tempSemester = "SecondSemester";
                    } else {
                        tempSemester = "FirstSemester";
                        tempYearLevel = getNextYearLevel(currentYearLevel);
                    }

                    // Store in final variables for use in lambda
                    final String nextYearLevel = tempYearLevel;
                    final String nextSemester = tempSemester;

                    // Update student's year and semester
                    if (courseDAO.updateStudentYearAndSemester(currentUser.getIdNumber(), nextYearLevel,
                            nextSemester)) {
                        // Get available courses for next semester/year
                        List<Course> availableCourses = courseDAO.getAllCourses().stream()
                                .filter(c -> c.getYearLevel().equals(nextYearLevel)
                                        && c.getSemester().equals(nextSemester))
                                .collect(Collectors.toList());

                        // Enroll in all available courses
                        boolean enrollmentSuccess = true;
                        for (Course course : availableCourses) {
                            if (!courseDAO.enrollStudentInCourse(currentUser.getIdNumber(), course.getCourseCode(),
                                    nextYearLevel, nextSemester)) {
                                enrollmentSuccess = false;
                                break;
                            }
                        }

                        String message = String.format("Successfully advanced to %s - %s\n",
                                formatYearLevel(nextYearLevel),
                                formatSemester(nextSemester));
                        if (enrollmentSuccess) {
                            message += String.format("Automatically enrolled in %d courses for the new semester.",
                                    availableCourses.size());
                        } else {
                            message += "Some courses could not be enrolled automatically.";
                        }

                        showAlert(Alert.AlertType.INFORMATION, "Academic Progress", message);
                        loadDashboardData(); // Refresh dashboard data
                    }
                }
            }

            // The pending status should now be correctly set by the DAO layer

            // Clear existing items
            if (enrolledCoursesTable != null) {
                enrolledCoursesTable.getItems().clear();
            }

            if (courses != null && !courses.isEmpty()) {
                // Update table with new data
                if (enrolledCoursesTable != null) {
                    enrolledCoursesTable.setItems(FXCollections.observableArrayList(courses));
                }

                // Update statistics
                if (enrolledCoursesLabel != null) {
                    enrolledCoursesLabel.setText(String.valueOf(courses.size()));
                }

                // Calculate total units safely
                int totalUnits = courses.stream()
                        .mapToInt(c -> {
                            try {
                                return Integer.parseInt(c.getUnits().trim());
                            } catch (NumberFormatException e) {
                                System.err.println("Invalid units format for course: " + c.getCourseCode());
                                return 0;
                            }
                        })
                        .sum();
                if (totalUnitsLabel != null) {
                    totalUnitsLabel.setText(String.valueOf(totalUnits));
                }

                // Get year and semester from the first course
                CourseEnrollment firstCourse = courses.get(0);
                String yearLevel = firstCourse.getYearLevel();
                String semester = firstCourse.getSemester();
                if (yearSemesterLabel != null) {
                    yearSemesterLabel.setText(formatYearLevel(yearLevel) + " - " + formatSemester(semester));
                }

                // Refresh the table
                if (enrolledCoursesTable != null) {
                    enrolledCoursesTable.refresh();
                }
            } else {
                clearDashboard();
                showAlert(Alert.AlertType.INFORMATION, "No Courses", "You are not enrolled in any courses.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            clearDashboard();
            showErrorAlert("Error", "Failed to load dashboard data: " + e.getMessage());
        }
    }

    private void clearDashboard() {
        if (enrolledCoursesTable != null) {
            enrolledCoursesTable.setItems(FXCollections.observableArrayList());
        }
        if (enrolledCoursesLabel != null) {
            enrolledCoursesLabel.setText("0");
        }
        if (totalUnitsLabel != null) {
            totalUnitsLabel.setText("0");
        }
        if (yearSemesterLabel != null
                && (currentUser == null || courseDAO.getStudentCourses(currentUser.getIdNumber()).isEmpty())) {
            yearSemesterLabel.setText("Not Enrolled");
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

    private void showErrorAlert(String title, String message) {
        showAlert(Alert.AlertType.ERROR, title, message);
    }

    private String getNextYearLevel(String currentYearLevel) {
        switch (currentYearLevel) {
            case "FirstYear":
                return "SecondYear";
            case "SecondYear":
                return "ThirdYear";
            case "ThirdYear":
                return "FourthYear";
            default:
                return currentYearLevel;
        }
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(type);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.show(); // Using show() instead of showAndWait() to avoid blocking
        });
    }

    @FXML
    private void handleRecommendations() {
        if (currentUser == null) {
            showErrorAlert("Error", "User session not found");
            return;
        }

        try {
            // Get all enrolled courses
            var courses = courseDAO.getStudentCourses(currentUser.getIdNumber());
            if (courses == null || courses.isEmpty()) {
                showAlert(Alert.AlertType.ERROR, "Cannot Access Recommendations", "No enrolled courses found");
                return;
            }

            // Check for unevaluated courses and pending evaluations
            boolean hasUnevaluatedCourses = courses.stream()
                    .anyMatch(course -> course.getGrade() == null || course.getGrade().isEmpty());
            boolean hasPendingEvaluations = courses.stream()
                    .anyMatch(CourseEnrollment::isPending);

            if (hasUnevaluatedCourses || hasPendingEvaluations) {
                String message = hasPendingEvaluations
                        ? "Please wait for pending course evaluations to be completed before accessing recommendations."
                        : "Please ensure all your courses have been evaluated before accessing recommendations.";
                showAlert(Alert.AlertType.ERROR, "Cannot Access Recommendations", message);
                return;
            }

            // If all checks pass, navigate to recommendations
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/eduassess/views/Recommendations.fxml"));
            Parent recommendationsView = loader.load();
            RecommendationsController controller = loader.getController();
            controller.initData(currentUser);

            Stage stage = (Stage) userNameLabel.getScene().getWindow();
            Scene scene = new Scene(recommendationsView);
            stage.setScene(scene);
            stage.centerOnScreen();
        } catch (Exception e) {
            e.printStackTrace();
            showErrorAlert("Error", "Failed to load recommendations view");
        }
    }

    @FXML
    private void handleHistory() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/eduassess/views/History.fxml"));
            Parent historyView = loader.load();
            HistoryController controller = loader.getController();
            controller.initData(currentUser);

            Stage stage = (Stage) userNameLabel.getScene().getWindow();
            Scene scene = new Scene(historyView);
            stage.setScene(scene);
            stage.centerOnScreen();
        } catch (IOException e) {
            e.printStackTrace();
            showErrorAlert("Error", "Failed to load history view");
        }
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

    @FXML
    private void handleEnroll(ActionEvent event) {
        // Create a dialog for course enrollment
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Course Enrollment");
        dialog.setHeaderText("Select courses to enroll");

        VBox content = new VBox(10);
        content.setPadding(new javafx.geometry.Insets(20));
        content.setStyle("-fx-background-color: white;");

        // Add enrollment form elements
        ComboBox<String> yearLevelCombo = new ComboBox<>();
        yearLevelCombo.getItems().addAll("FirstYear", "SecondYear", "ThirdYear", "FourthYear");
        yearLevelCombo.setPromptText("Select Year Level");
        yearLevelCombo.setStyle("-fx-pref-width: 200px;");

        ComboBox<String> semesterCombo = new ComboBox<>();
        semesterCombo.getItems().addAll("FirstSemester", "SecondSemester");
        semesterCombo.setPromptText("Select Semester");
        semesterCombo.setStyle("-fx-pref-width: 200px;");

        content.getChildren().addAll(
                new Label("Year Level:"),
                yearLevelCombo,
                new Label("Semester:"),
                semesterCombo);

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            String selectedYear = yearLevelCombo.getValue();
            String selectedSemester = semesterCombo.getValue();

            if (selectedYear != null && selectedSemester != null) {
                if (courseDAO.enrollStudentInCourse(currentUser.getIdNumber(), course.getCourseCode(), selectedYear,
                        selectedSemester)) {
                    showAlert(Alert.AlertType.ERROR, "Success", "Successfully enrolled in courses!");
                    loadDashboardData(); // Refresh the dashboard
                } else {
                    showAlert(Alert.AlertType.ERROR, "Error", "Failed to enroll in courses. Please try again.");
                }
            } else {
                showAlert(Alert.AlertType.ERROR, "Error", "Please select both Year Level and Semester.");
            }
        }
    }
}