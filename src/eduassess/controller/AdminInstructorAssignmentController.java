package eduassess.controller;

import eduassess.dao.CourseDAO;
import eduassess.dao.UserDAO;
import eduassess.model.Course;
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
import java.util.List;
import java.util.ResourceBundle;

public class AdminInstructorAssignmentController implements Initializable {
    @FXML
    private ComboBox<User> instructorComboBox;
    @FXML
    private ComboBox<Course> courseComboBox;
    @FXML
    private TableView<Course> assignedCoursesTable;
    @FXML
    private TableColumn<Course, String> courseCodeColumn;
    @FXML
    private TableColumn<Course, String> courseNameColumn;

    private UserDAO userDAO;
    private CourseDAO courseDAO;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        userDAO = new UserDAO();
        courseDAO = new CourseDAO();

        // Initialize table columns
        courseCodeColumn.setCellValueFactory(new PropertyValueFactory<>("courseCode"));
        courseNameColumn.setCellValueFactory(new PropertyValueFactory<>("courseName"));

        // Load instructors and courses
        loadInstructors();
        loadCourses();

        // Add listener to instructor selection
        instructorComboBox.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                loadAssignedCourses(newVal);
            }
        });
    }

    private void loadInstructors() {
        try {
            List<User> instructors = userDAO.getAllInstructors();
            instructorComboBox.setItems(FXCollections.observableArrayList(instructors));
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Error loading instructors", e.getMessage());
        }
    }

    private void loadCourses() {
        List<Course> courses = courseDAO.getAllCourses();
        courseComboBox.setItems(FXCollections.observableArrayList(courses));
    }

    private void loadAssignedCourses(User instructor) {
        try {
            List<Course> assignedCourses = courseDAO.getCoursesForInstructor(instructor.getId());
            assignedCoursesTable.setItems(FXCollections.observableArrayList(assignedCourses));
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Error loading assigned courses", e.getMessage());
        }
    }

    @FXML
    private void handleAssignCourse() {
        User selectedInstructor = instructorComboBox.getValue();
        Course selectedCourse = courseComboBox.getValue();

        if (selectedInstructor == null || selectedCourse == null) {
            showAlert(Alert.AlertType.WARNING, "Selection Required", "Please select both an instructor and a course.");
            return;
        }

        try {
            courseDAO.assignCourseToInstructor(selectedCourse.getCourseCode(), selectedInstructor.getId());
            loadAssignedCourses(selectedInstructor);
            showAlert(Alert.AlertType.INFORMATION, "Success", "Course successfully assigned to instructor.");
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Error assigning course", e.getMessage());
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
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to return to dashboard.");
        }
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}