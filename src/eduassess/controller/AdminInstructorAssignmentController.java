package eduassess.controller;

import eduassess.dao.CourseDAO;
import eduassess.dao.InstructorCourseDAO;
import eduassess.dao.UserDAO;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

public class AdminInstructorAssignmentController implements Initializable {
    private final InstructorCourseDAO instructorCourseDAO;
    private final CourseDAO courseDAO;
    private final UserDAO userDAO;

    @FXML
    private ComboBox<String> instructorComboBox;

    @FXML
    private ComboBox<String> courseComboBox;

    @FXML
    private TableView<Map<String, String>> assignmentsTable;

    @FXML
    private TableColumn<Map<String, String>, String> instructorCol;

    @FXML
    private TableColumn<Map<String, String>, String> courseCol;

    @FXML
    private TableColumn<Map<String, String>, String> dateCol;

    @FXML
    private TableColumn<Map<String, String>, String> actionCol;

    public AdminInstructorAssignmentController() {
        this.instructorCourseDAO = new InstructorCourseDAO();
        this.courseDAO = new CourseDAO();
        this.userDAO = new UserDAO();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupTable();
        loadData();
    }

    private void setupTable() {
        instructorCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
                data.getValue().get("instructorName")));

        courseCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
                data.getValue().get("courseName")));

        dateCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
                data.getValue().get("assignedDate")));

        actionCol.setCellFactory(col -> new TableCell<Map<String, String>, String>() {
            private final Button unassignButton = new Button("Unassign");

            {
                unassignButton.setOnAction(event -> {
                    Map<String, String> assignment = getTableView().getItems().get(getIndex());
                    unassignCourse(assignment);
                });
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(unassignButton);
                }
            }
        });
    }

    private void loadData() {
        // Load instructors
        List<Map<String, String>> instructors = userDAO.getAllUsers("INSTRUCTOR");
        instructorComboBox.setItems(FXCollections.observableArrayList(
                instructors.stream()
                        .map(instructor -> instructor.get("id_number") + " - " + instructor.get("full_name"))
                        .toList()));

        // Load courses
        List<Map<String, String>> courses = courseDAO.getAllCourses();
        courseComboBox.setItems(FXCollections.observableArrayList(
                courses.stream()
                        .map(course -> course.get("course_Code") + " - " + course.get("course_Name"))
                        .toList()));

        refreshAssignmentsTable();
    }

    @FXML
    private void assignCourse() {
        String selectedInstructor = instructorComboBox.getValue();
        String selectedCourse = courseComboBox.getValue();

        if (selectedInstructor == null || selectedCourse == null) {
            showAlert(Alert.AlertType.WARNING, "Please select both instructor and course");
            return;
        }

        String instructorId = selectedInstructor.split(" - ")[0];
        String courseCode = selectedCourse.split(" - ")[0];

        if (instructorCourseDAO.assignInstructorToCourse(instructorId, courseCode)) {
            showAlert(Alert.AlertType.INFORMATION, "Course assigned successfully");
            refreshAssignmentsTable();
        } else {
            showAlert(Alert.AlertType.ERROR, "Failed to assign course");
        }
    }

    private void unassignCourse(Map<String, String> assignment) {
        String instructorId = assignment.get("instructorId");
        String courseCode = assignment.get("courseCode");

        if (instructorCourseDAO.unassignInstructorFromCourse(instructorId, courseCode)) {
            showAlert(Alert.AlertType.INFORMATION, "Course unassigned successfully");
            refreshAssignmentsTable();
        } else {
            showAlert(Alert.AlertType.ERROR, "Failed to unassign course");
        }
    }

    private void refreshAssignmentsTable() {
        List<Map<String, String>> allAssignments = courseDAO.getAllCourses().stream()
                .flatMap(course -> instructorCourseDAO.getCourseInstructors(course.get("course_Code")).stream()
                        .map(instructor -> {
                            Map<String, String> assignment = new HashMap<>();
                            assignment.put("instructorId", instructor.get("instructorId"));
                            assignment.put("instructorName", instructor.get("instructorName"));
                            assignment.put("courseCode", course.get("course_Code"));
                            assignment.put("courseName", course.get("course_name"));
                            assignment.put("assignedDate", instructor.get("assignedDate"));
                            return assignment;
                        }))
                .toList();

        assignmentsTable.setItems(FXCollections.observableArrayList(allAssignments));
    }

    private void showAlert(Alert.AlertType alertType, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(alertType.toString());
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}