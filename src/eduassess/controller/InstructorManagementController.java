package eduassess.controller;

import eduassess.dao.CourseDAO;
import eduassess.dao.InstructorDAO;
import eduassess.dao.UserDAO;
import eduassess.model.Course;
import eduassess.model.Instructor;
import eduassess.model.User;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

import java.io.IOException;

import java.net.URL;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javafx.scene.layout.VBox;

public class InstructorManagementController implements Initializable {
    @FXML
    private ComboBox<Instructor> instructorComboBox;
    @FXML
    private ComboBox<Course> courseComboBox;
    @FXML
    private Label userNameLabel;
    @FXML
    private TableView<Instructor> instructorsTable;
    @FXML
    private TableColumn<Instructor, String> courseCodeColumn;
    @FXML
    private TableColumn<Instructor, String> courseNameColumn;
    @FXML
    private TableColumn<Instructor, String> courseInstructorColumn;
    @FXML
    private TableColumn<Instructor, Void> unassignColumn;

    @FXML
    private Label statusLabel;

    private UserDAO userDAO;
    private CourseDAO courseDAO;
    private InstructorDAO instructorDAO;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        userDAO = new UserDAO();
        courseDAO = new CourseDAO();
        instructorDAO = new InstructorDAO();

        // Initialize table columns
        courseCodeColumn.setCellValueFactory(new PropertyValueFactory<>("course_Code"));
        courseNameColumn.setCellValueFactory(new PropertyValueFactory<>("course_Name"));
        courseInstructorColumn.setCellValueFactory(new PropertyValueFactory<>("fullName"));

        // Initialize instructor combo box
        try {
            List<Instructor> instructors = instructorDAO.getAllInstructors();
            ObservableList<Instructor> instructorList = FXCollections.observableArrayList(instructors);
            instructorComboBox.setItems(instructorList);

        } catch (Exception e) {
            Logger.getLogger(InstructorManagementController.class.getName()).log(Level.SEVERE, null, e);
        }

        // Add Unassign button column
        unassignColumn = new TableColumn<>("Manage");
        unassignColumn.setResizable(false);
        unassignColumn.setPrefWidth(156);
        unassignColumn.setCellFactory(col -> new TableCell<Instructor, Void>() {
            private final Button unassignButton = new Button("Dismiss");
            {
                unassignButton.setStyle(
                        "-fx-background-color: #ef4444;" +
                                "-fx-text-fill: white;" +
                                "-fx-font-weight: bold;" +
                                "-fx-padding: 5 15;" +
                                "-fx-cursor: hand;");
                unassignButton.setOnAction(event -> {
                    Instructor instructor = getTableView().getItems().get(getIndex());
                    try {
                        handleUnassignCourse(instructor);
                    } catch (SQLException ex) {
                        Logger.getLogger(InstructorManagementController.class.getName()).log(Level.SEVERE, null, ex);
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(unassignButton);
                }
            }
        });

        // Add the unassign column to the table view
        instructorsTable.getColumns().add(unassignColumn);

        // Load instructors and courses
        loadInstructors();
        loadCourses();

        // Add listener to instructor selection
        instructorComboBox.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            loadAssignedCourses(newVal);
        });

        // Load all assigned courses initially
        loadAssignedCourses(null);
    }

    private void loadInstructors() {
        List<Instructor> instructors = instructorDAO.getAllInstructors();
        instructorComboBox.setItems(FXCollections.observableArrayList(instructors));
    }

    private void loadCourses() {
        try {
            List<Course> allCourses = courseDAO.getAllCourses();
            Instructor selectedInstructor = instructorComboBox.getValue();

            if (selectedInstructor != null) {
                // Get assigned courses for this instructor
                List<Instructor> assignedCourses = instructorDAO
                        .getCoursesForInstructor(selectedInstructor.getIdNumber());
                List<String> assignedCourseCodes = assignedCourses.stream()
                        .map(Instructor::getCourse_Code)
                        .collect(Collectors.toList());

                // Filter out already assigned courses
                List<Course> availableCourses = allCourses.stream()
                        .filter(course -> !assignedCourseCodes.contains(course.getCourseCode()))
                        .collect(Collectors.toList());

                courseComboBox.setItems(FXCollections.observableArrayList(availableCourses));
            } else {
                courseComboBox.setItems(FXCollections.observableArrayList(allCourses));
            }
        } catch (SQLException e) {
            showAlert("Error", "Failed to load courses: " + e.getMessage());
        }
    }

    private void loadAssignedCourses(Instructor instructor) {
        try {
            List<Instructor> assignedCourses;
            if (instructor != null) {
                assignedCourses = instructorDAO.getCoursesForInstructor(instructor.getIdNumber());
            } else {
                assignedCourses = instructorDAO.getAllAssignedCourses();
            }
            instructorsTable.setItems(FXCollections.observableArrayList(assignedCourses));
        } catch (SQLException e) {
            showAlert("Error loading assigned courses", e.getMessage());
        }
    }

    @FXML
    private void refreshData() throws SQLException {
        List<Instructor> assignedCourses;
        assignedCourses = instructorDAO.getAllAssignedCourses();
        loadInstructors();
        loadCourses();
        if (assignedCourses != null) {
            instructorsTable.setItems(FXCollections.observableArrayList(assignedCourses));
        }
    }

    @FXML
    private void handleUnassignCourse(Instructor instructor) throws SQLException {
        if (instructor == null) {
            showAlert("Error", "No instructor selected.");
            return;
        }

        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Confirm Dismiss");
        confirmation.setHeaderText(instructor.getCourse_Name());
        confirmation.setContentText("Are you sure you want to dismiss " + instructor.getFullName() +
                " from " + instructor.getCourse_Name() + "?");

        Optional<ButtonType> result = confirmation.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            boolean success = instructorDAO.unassignInstructorFromCourse(
                    instructor.getIdNumber(),
                    instructor.getCourse_Code());
            if (success) {
                showAlert("Success", "Course successfully dismiss from instructor.");
                loadAssignedCourses(instructorComboBox.getValue());
                loadCourses();
            } else {
                showAlert("Error", "Failed to unassign course.");
            }
        }
    }

    @FXML
    private void handleAssignCourse() {
        Instructor selectedInstructor = instructorComboBox.getValue();
        Course selectedCourse = courseComboBox.getValue();

        if (selectedInstructor == null || selectedCourse == null) {
            showAlert("Selection Required", "Please select both an instructor and a course.");
            return;
        }

        try {
            System.out.println("Attempting to assign course " + selectedCourse.getCourseCode() +
                    " to instructor " + selectedInstructor.getIdNumber());

            // Check if course is already assigned to instructor
            if (instructorDAO.isCourseAssignedToInstructor(selectedInstructor.getIdNumber(),
                    selectedCourse.getCourseCode())) {
                System.out.println("Course already assigned to instructor");
                statusLabel.setText("This course is already assigned to the instructor.");
                showAlert("Duplicate Assignment", "This course is already assigned to the instructor.");
                return;
            }

            boolean success = instructorDAO.assignInstructorToCourse(
                    selectedInstructor.getIdNumber(),
                    selectedCourse.getCourseCode());

            if (success) {
                System.out.println("Course assignment successful");
                loadAssignedCourses(selectedInstructor);
                loadCourses();
                statusLabel.setText("Course successfully assigned to instructor.");
                showAlert("Success", "Course successfully assigned to instructor.");
            } else {
                System.out.println("Course assignment failed without exception");
                statusLabel.setText("Failed to assign course. Please check logs for details.");
                showAlert("Error", "Failed to assign course. Please check logs for details.");
            }
        } catch (SQLException e) {
            System.err.println("SQL Error during course assignment: " + e.getMessage());
            e.printStackTrace();
            if (e.getErrorCode() == 1062) { // MySQL duplicate entry error code
                statusLabel.setText("This course is already assigned to the instructor.");
                showAlert("Duplicate Assignment", "This course is already assigned to the instructor.");
            } else {
                statusLabel.setText("Database Error: Failed to assign course.");
                showAlert("Database Error", "Failed to assign course. Please try again or contact support.");
            }
        } catch (Exception e) {
            System.err.println("Unexpected error during course assignment: " + e.getMessage());
            e.printStackTrace();
            showAlert("Error", "An unexpected error occurred. Please try again.");
            statusLabel.setText("Unexpected error during course assignment.");
        }
    }

    @FXML
    private void backToDashboard() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("../views/AdminDashboard.fxml"));
            Parent dashboardView = loader.load();
            Stage stage = (Stage) instructorComboBox.getScene().getWindow();
            Scene scene = new Scene(dashboardView);
            stage.setScene(scene);
            stage.centerOnScreen();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Error", "Failed to return to dashboard.");
        }
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    @FXML
    private void handleInstructorManagement() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/eduassess/views/AdminInstructorAssignment.fxml"));
            Parent instructorAssignmentView = loader.load();
            Stage stage = (Stage) userNameLabel.getScene().getWindow();
            Scene scene = new Scene(instructorAssignmentView);
            stage.setScene(scene);
            stage.centerOnScreen();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Error", "Failed to load Instructor Management. Please try again.");
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
                    Stage stage = (Stage) userNameLabel.getScene().getWindow();
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("../views/Login.fxml"));
                    Parent loginView = loader.load();
                    Scene scene = new Scene(loginView);
                    stage.setScene(scene);
                    stage.centerOnScreen();
                    stage.show();
                } catch (IOException e) {
                    Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                    errorAlert.setTitle("Error");
                    errorAlert.setHeaderText("Logout Error");
                    errorAlert.setContentText("Could not return to login screen: " + e.getMessage());
                    errorAlert.showAndWait();
                }
            }
        });
    }

    private void handleUnassignInstructor(Course course) {
        Instructor selectedInstructor = instructorComboBox.getValue();
        if (selectedInstructor == null || course == null) {
            showAlert("Selection Required", "Please select an instructor and a course to unassign.");
            return;
        }
        loadAssignedCourses(selectedInstructor);
        showAlert("Success", "Instructor unassigned from course.");
    }
}