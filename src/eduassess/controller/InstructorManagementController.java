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
    private ObservableList<Course> selectedCourses = FXCollections.observableArrayList();
    private Course selectedCourse;
    @FXML
    private ComboBox<Instructor> instructorComboBox;
    @FXML
    private ComboBox<String> yearLevelComboBox;
    @FXML
    private ComboBox<String> semesterComboBox;
    @FXML
    private Label userNameLabel;
    @FXML
    private TableView<Course> instructorsTable;
    @FXML
    private TableColumn<Course, Boolean> selectColumn;
    @FXML
    private TableColumn<Course, String> courseCodeColumn;
    @FXML
    private TableColumn<Course, String> courseNameColumn;
    @FXML
    private TableColumn<Course, String> courseInstructorColumn;
    @FXML
    private TableColumn<Course, Void> manageColumn;

    @FXML
    private Label statusLabel;

    String selectedYearLevel = null;
    String selectedSemester = null;

    private UserDAO userDAO;
    private CourseDAO courseDAO;
    private InstructorDAO instructorDAO;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        userDAO = new UserDAO();
        courseDAO = new CourseDAO();
        instructorDAO = new InstructorDAO();

        // Initialize year level and semester combo boxes
        try {
            List<Course> allCourses = courseDAO.getAllCourses();

            // Get distinct year levels
            List<String> yearLevels = allCourses.stream()
                    .map(Course::getYearLevel)
                    .distinct()
                    .collect(Collectors.toList());

            // Get distinct semesters
            List<String> semesters = allCourses.stream()
                    .map(Course::getSemester)
                    .distinct()
                    .collect(Collectors.toList());

            // Populate combo boxes
            yearLevelComboBox.setItems(FXCollections.observableArrayList(yearLevels));
            semesterComboBox.setItems(FXCollections.observableArrayList(semesters));

        } catch (Exception e) {
            Logger.getLogger(InstructorManagementController.class.getName()).log(Level.SEVERE, null, e);
        }

        // Initialize table columns
        selectColumn.setCellValueFactory(cellData -> cellData.getValue().selectedProperty());
        selectColumn.setCellFactory(col -> new TableCell<Course, Boolean>() {
            private final CheckBox checkBox = new CheckBox();
            {
                checkBox.selectedProperty().addListener((obs, wasSelected, isSelected) -> {
                    if (isSelected != null) {
                        getTableView().getItems().get(getIndex()).setSelected(isSelected);
                    }
                });
            }

            @Override
            protected void updateItem(Boolean item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    checkBox.setSelected(item != null && item);
                    setGraphic(checkBox);
                }
            }
        });
        courseCodeColumn.setCellValueFactory(new PropertyValueFactory<>("courseCode"));
        courseNameColumn.setCellValueFactory(new PropertyValueFactory<>("courseName"));
        courseInstructorColumn.setCellValueFactory(new PropertyValueFactory<>("instructorName"));

        // Initialize instructor combo box
        try {
            List<Instructor> instructors = instructorDAO.getAllInstructors();
            ObservableList<Instructor> instructorList = FXCollections.observableArrayList(instructors);
            instructorComboBox.setItems(instructorList);

        } catch (Exception e) {
            Logger.getLogger(InstructorManagementController.class.getName()).log(Level.SEVERE, null, e);
        }

        // Add Manage button column
        manageColumn.setCellFactory(col -> new TableCell<Course, Void>() {
            private final Button unassignButton = new Button("Dismiss");
            {
                unassignButton.setStyle(
                        "-fx-background-color: #ef4444;" +
                                "-fx-text-fill: white;" +
                                "-fx-font-weight: bold;" +
                                "-fx-padding: 5 15;" +
                                "-fx-cursor: hand;");
                unassignButton.setOnAction(event -> {
                    Course course = getTableView().getItems().get(getIndex());
                    if (course != null && course.getInstructorName() != null
                            && !course.getInstructorName().trim().isEmpty()) {
                        // Unassign logic: set instructor to null and update in DB
                        try {
                            instructorDAO.unassignInstructorFromCourse(course.getInstructorId(),
                                    course.getCourseCode());
                            course.setInstructorName("");
                            getTableView().refresh();
                            statusLabel.setText("Instructor unassigned from course " + course.getCourseCode());
                        } catch (Exception e) {
                            statusLabel.setText("Failed to unassign instructor: " + e.getMessage());
                        }
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    Course course = getTableView().getItems().get(getIndex());
                    if (course != null && course.getInstructorName() != null
                            && !course.getInstructorName().trim().isEmpty()
                            && !course.getInstructorName().equals("Not Assigned")) {
                        VBox buttonBox = new VBox(5, unassignButton);
                        setGraphic(buttonBox);
                    } else {
                        setGraphic(null);
                    }
                }
            }
        });

        // Load instructors and courses
        loadInstructors();
        loadCourses();

        // Add listener to instructor selection
        instructorComboBox.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            loadAssignedCourses(newVal);
        });

        // Add listeners to yearLevelComboBox and semesterComboBox to update TableView
        yearLevelComboBox.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            loadCourses();
        });
        semesterComboBox.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            loadCourses();
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
            if (yearLevelComboBox != null && yearLevelComboBox.getValue() != null) {
                selectedYearLevel = yearLevelComboBox.getValue();
            }
            if (semesterComboBox != null && semesterComboBox.getValue() != null) {
                selectedSemester = semesterComboBox.getValue();
            }

            // First get all assigned courses with instructor info
            List<Instructor> allAssignedCourses = instructorDAO.getAllAssignedCourses();

            List<Course> filteredCourses = allCourses.stream().map(course -> {
                Course courseWithInstructor = new Course();
                courseWithInstructor.setCourseCode(course.getCourseCode());
                courseWithInstructor.setCourseName(course.getCourseName());
                courseWithInstructor.setYearLevel(course.getYearLevel());
                courseWithInstructor.setSemester(course.getSemester());

                // Find instructor for this course
                Optional<Instructor> instructorForCourse = allAssignedCourses.stream()
                        .filter(i -> i.getCourse_Code().equals(course.getCourseCode()))
                        .findFirst();

                if (instructorForCourse.isPresent()) {
                    courseWithInstructor.setInstructorName(instructorForCourse.get().getFullName());
                    courseWithInstructor.setInstructorId(instructorForCourse.get().getIdNumber());
                } else {
                    courseWithInstructor.setInstructorName("Not Assigned");
                    courseWithInstructor.setInstructorId(0);
                }
                return courseWithInstructor;
            }).collect(Collectors.toList());

            // Apply filters
            if (selectedYearLevel != null) {
                filteredCourses = filteredCourses.stream()
                        .filter(course -> selectedYearLevel.equals(course.getYearLevel()))
                        .collect(Collectors.toList());
            }
            if (selectedSemester != null) {
                filteredCourses = filteredCourses.stream()
                        .filter(course -> selectedSemester.equals(course.getSemester()))
                        .collect(Collectors.toList());
            }

            // Set the filtered courses to the table view
            instructorsTable.setItems(FXCollections.observableArrayList(filteredCourses));

            if (selectedInstructor != null) {
                List<Instructor> assignedCourses = instructorDAO
                        .getCoursesForInstructor(selectedInstructor.getIdNumber());
                List<String> assignedCourseCodes = assignedCourses.stream()
                        .map(Instructor::getCourse_Code)
                        .collect(Collectors.toList());
                filteredCourses = filteredCourses.stream()
                        .filter(course -> !assignedCourseCodes.contains(course.getCourseCode()))
                        .collect(Collectors.toList());
            }
        } catch (SQLException e) {
            showAlert("Error", "Failed to load courses: " + e.getMessage());
        }
    }

    private void loadAssignedCourses(Instructor instructor) {
        try {
            List<Course> allCourses = courseDAO.getAllCourses();
            List<Instructor> assignedCourses;

            if (instructor != null) {
                assignedCourses = instructorDAO.getCoursesForInstructor(instructor.getIdNumber());
            } else {
                assignedCourses = instructorDAO.getAllAssignedCourses();
            }

            // Map instructor assignments to courses
            ObservableList<Course> coursesWithInstructors = FXCollections.observableArrayList();
            for (Course course : allCourses) {
                Course courseWithInstructor = new Course();
                courseWithInstructor.setCourseCode(course.getCourseCode());
                courseWithInstructor.setCourseName(course.getCourseName());
                courseWithInstructor.setYearLevel(course.getYearLevel());
                courseWithInstructor.setSemester(course.getSemester());

                // Find instructor for this course
                Optional<Instructor> instructorForCourse = assignedCourses.stream()
                        .filter(i -> i.getCourse_Code().equals(course.getCourseCode()))
                        .findFirst();

                if (instructorForCourse.isPresent()) {
                    courseWithInstructor.setInstructorName(instructorForCourse.get().getFullName());
                    courseWithInstructor.setInstructorId(instructorForCourse.get().getIdNumber());
                } else {
                    courseWithInstructor.setInstructorName("Not Assigned");
                    courseWithInstructor.setInstructorId(0);
                }

                coursesWithInstructors.add(courseWithInstructor);
            }

            instructorsTable.setItems(coursesWithInstructors);
        } catch (SQLException e) {
            showAlert("Error loading courses", e.getMessage());
        }
    }

    @FXML
    private void refreshData() throws SQLException {
        List<Course> allCourses = courseDAO.getAllCourses();
        loadInstructors();
        loadCourses();
        if (allCourses != null) {
            instructorsTable.setItems(FXCollections.observableArrayList(allCourses));
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
            boolean success = instructorDAO.unassignInstructorFromCourse(instructor.getIdNumber(),
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
        selectedYearLevel = yearLevelComboBox.getValue();
        selectedSemester = semesterComboBox.getValue();

        if (selectedInstructor == null) {
            showAlert("Selection Required", "Please select an instructor.");
            return;
        }

        // Get selected courses from checkboxes
        List<Course> selectedCourses = instructorsTable.getItems().stream()
                .filter(Course::isSelected)
                .collect(Collectors.toList());

        if (selectedCourses.isEmpty()) {
            showAlert("Selection Required", "Please select at least one course to assign.");
            return;
        }

        try {
            int successCount = 0;
            for (Course course : selectedCourses) {
                System.out.println("Attempting to assign course " + course.getCourseCode() +
                        " to instructor " + selectedInstructor.getIdNumber() +
                        " for " + selectedYearLevel + " " + selectedSemester);

                // Check if course is already assigned to instructor for this year/semester
                if (instructorDAO.isCourseAssignedToInstructor(selectedInstructor.getIdNumber(),
                        course.getCourseCode())) {
                    System.out.println("Course already assigned to instructor");
                    continue;
                }

                boolean success = instructorDAO.assignInstructorToCourse(
                        selectedInstructor.getIdNumber(),
                        course.getCourseCode());

                if (success) {
                    successCount++;
                    System.out.println("Course assignment successful");
                } else {
                    System.out.println("Course assignment failed without exception");
                }
            }

            if (successCount > 0) {
                loadAssignedCourses(selectedInstructor);
                loadCourses();
                statusLabel.setText(successCount + " course(s) successfully assigned to instructor.");
                showAlert("Success", successCount + " course(s) successfully assigned to instructor.");
            } else {
                statusLabel.setText("No courses were assigned. Some may already be assigned.");
                showAlert("Info", "No courses were assigned. Some may already be assigned.");
            }
        } catch (Exception e) {
            System.err.println("Unexpected error during course assignment: " + e.getMessage());
            e.printStackTrace();
            showAlert("Error", "An unexpected error occurred. Please try again.");
            statusLabel.setText("Unexpected error during course assignment.");
        }
        // MySQL duplicate entry error code

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
    private void handleRoomUtilization(){
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/eduasses/views/RoomUtilization.fxml"));
            Parent roomUtilization = loader.load();
            Stage stage = (Stage) userNameLabel.getScene().getWindow();
            Scene scene = new Scene(roomUtilization);
            stage.setScene(scene);
            stage.centerOnScreen();
        } catch (IOException e){
            e.printStackTrace();
            showAlert("Error", "Failed to load Room Utilization, Please try again.");
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