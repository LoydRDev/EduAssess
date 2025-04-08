package eduassess.controller;

import eduassess.model.User;
import eduassess.model.Course;
import eduassess.model.Grade;
import eduassess.model.PendingEvaluation;
import eduassess.dao.CourseDAO;
import eduassess.dao.GradeDAO;
import eduassess.util.SessionManager;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Alert;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.collections.FXCollections;
import javafx.stage.Modality;
import java.io.IOException;
import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import javafx.scene.input.KeyCode;
import javafx.collections.ObservableList;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.text.Text;
import java.util.List;
import javafx.util.StringConverter;
import javafx.application.Platform;

public class InstructorDashboardController implements Initializable {
    @FXML
    private Label userNameLabel;
    @FXML
    private Label instructorIdLabel;
    @FXML
    private Label departmentLabel;
    @FXML
    private Label totalClassesLabel;
    @FXML
    private Label totalStudentsLabel;
    @FXML
    private Label pendingGradesLabel;
    @FXML
    private Text totalCoursesText;
    @FXML
    private Text totalStudentsText;
    @FXML
    private ComboBox<User> studentComboBox;
    @FXML
    private TableView<PendingEvaluation> evaluationsTable;
    @FXML
    private TableColumn<PendingEvaluation, String> studentIdColumn;
    @FXML
    private TableColumn<PendingEvaluation, String> studentNameColumn;
    @FXML
    private TableColumn<PendingEvaluation, String> courseCodeColumn;
    @FXML
    private TableColumn<PendingEvaluation, LocalDateTime> requestDateColumn;
    @FXML
    private TableColumn<PendingEvaluation, String> gradeColumn;
    @FXML
    private TableColumn<PendingEvaluation, Button> actionColumn;

    private User currentUser;
    private CourseDAO courseDAO;
    private GradeDAO gradeDAO;
    private ObservableList<PendingEvaluation> pendingEvaluations;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        System.out.println("\n=== Initializing InstructorDashboardController ===");

        // Initialize DAOs
        courseDAO = new CourseDAO();
        gradeDAO = new GradeDAO();

        // Initialize the observable list
        pendingEvaluations = FXCollections.observableArrayList();

        // Set up table and student combo box
        setupTable();
        setupStudentComboBox();

        // Set initial table items
        evaluationsTable.setItems(pendingEvaluations);

        System.out.println("Initialization complete");
    }

    public void initData(User user) {
        this.currentUser = user;
        if (currentUser != null) {
            userNameLabel.setText(currentUser.getFullName());
            instructorIdLabel.setText("ID: " + currentUser.getIdNumber());
            loadDashboardData();
        }
    }

    private void loadDashboardData() {
        if (currentUser == null)
            return;

        try {
            // Update statistics
            int totalClasses = courseDAO.getInstructorTotalCourses(currentUser.getIdNumber());
            int totalStudents = courseDAO.getInstructorTotalStudents(currentUser.getIdNumber());
            int pendingGrades = gradeDAO.getPendingGradesCount(currentUser.getIdNumber());

            totalClassesLabel.setText(String.valueOf(totalClasses));
            totalStudentsLabel.setText(String.valueOf(totalStudents));
            pendingGradesLabel.setText(String.valueOf(pendingGrades));

            // Load students with pending evaluations into combo box
            List<User> studentsWithPendingEvals = courseDAO.getAllPendingEvaluations(currentUser.getIdNumber());
            ObservableList<User> uniqueStudents = FXCollections.observableArrayList(
                    studentsWithPendingEvals.stream()
                            .distinct()
                            .toList());
            studentComboBox.getItems().clear();
            studentComboBox.getItems().add(null); // Add "All Students" option
            studentComboBox.getItems().addAll(uniqueStudents);

            // Load all pending evaluations initially
            loadAllPendingEvaluations();
        } catch (Exception e) {
            System.err.println("Error loading dashboard data: " + e.getMessage());
            e.printStackTrace();
            showErrorAlert("Error", "Failed to load dashboard data. Please try again.");
        }
    }

    private void loadAllPendingEvaluations() {
        System.out.println("\n=== Loading all pending evaluations ===");

        try {
            // Clear existing items
            pendingEvaluations.clear();
            evaluationsTable.getItems().clear();

            // Get all students awaiting evaluation
            List<User> students = courseDAO.getAllPendingEvaluations(currentUser.getIdNumber());
            System.out.println("Found " + students.size() + " students awaiting evaluation");

            // Create evaluation objects for each student and add to observable list
            ObservableList<PendingEvaluation> newEvaluations = FXCollections.observableArrayList();

            for (User student : students) {
                System.out.println("Adding student: " + student.getIdNumber() + " - " + student.getFullName());

                PendingEvaluation evaluation = new PendingEvaluation(
                        student.getIdNumber(),
                        student.getFullName(),
                        student.getCourseCode(),
                        student.getCourseName(),
                        student.getRequestDate() != null ? student.getRequestDate() : LocalDateTime.now());

                newEvaluations.add(evaluation);
            }

            // Set the table items directly with the new list
            evaluationsTable.setItems(newEvaluations);
            pendingEvaluations = newEvaluations;

            // Refresh the table
            evaluationsTable.refresh();

            System.out.println("Total pending evaluations loaded: " + evaluationsTable.getItems().size());

            // Show message if no evaluations found
            if (evaluationsTable.getItems().isEmpty()) {
                Platform.runLater(() -> {
                    showAlert(Alert.AlertType.INFORMATION, "No Evaluations",
                            "No pending evaluations found");
                });
            }
        } catch (Exception e) {
            System.err.println("Error loading pending evaluations: " + e.getMessage());
            e.printStackTrace();
            Platform.runLater(() -> {
                showErrorAlert("Error", "Failed to load pending evaluations. Please try again.");
            });
        }
    }

    private void setupTable() {
        // Set up cell value factories
        studentIdColumn.setCellValueFactory(new PropertyValueFactory<>("studentId"));
        studentNameColumn.setCellValueFactory(new PropertyValueFactory<>("studentName"));
        courseCodeColumn.setCellValueFactory(new PropertyValueFactory<>("courseCode"));
        requestDateColumn.setCellValueFactory(new PropertyValueFactory<>("requestDate"));
        gradeColumn.setCellValueFactory(new PropertyValueFactory<>("grade"));

        // Format the request date column
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");
        requestDateColumn.setCellFactory(column -> {
            return new TableCell<PendingEvaluation, LocalDateTime>() {
                @Override
                protected void updateItem(LocalDateTime item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                    } else {
                        setText(formatter.format(item));
                    }
                }
            };
        });

        // Setup grade column with TextField
        gradeColumn.setCellFactory(column -> new TableCell<PendingEvaluation, String>() {
            private final TextField textField = new TextField();

            {
                textField.setPromptText("Enter Grade");
                textField.setStyle(
                        "-fx-background-color: white;" +
                                "-fx-border-color: #e2e8f0;" +
                                "-fx-border-radius: 4px;" +
                                "-fx-padding: 2px;");

                // Add input validation
                textField.textProperty().addListener((observable, oldValue, newValue) -> {
                    if (!newValue.matches("^[0-9]*(\\.[0-9]{0,2})?$|^INC$|^DRP$")) {
                        textField.setText(oldValue);
                    }
                });

                // Update grade on focus lost or enter pressed
                textField.setOnAction(event -> commitEdit());
                textField.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
                    if (!isNowFocused) {
                        commitEdit();
                    }
                });
            }

            private void commitEdit() {
                PendingEvaluation evaluation = getTableRow().getItem();
                if (evaluation != null) {
                    String value = textField.getText().trim();
                    // Validate numeric grade range
                    if (!value.equals("INC") && !value.equals("DRP")) {
                        try {
                            double grade = Double.parseDouble(value);
                            if (grade < 1.0 || grade > 5.0) {
                                textField.setText(evaluation.getGrade());
                                return;
                            }
                        } catch (NumberFormatException e) {
                            textField.setText(evaluation.getGrade());
                            return;
                        }
                    }
                    evaluation.setGrade(value);
                }
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    PendingEvaluation evaluation = getTableRow().getItem();
                    if (evaluation != null) {
                        textField.setText(evaluation.getGrade());
                        setGraphic(textField);
                    } else {
                        setGraphic(null);
                    }
                }
            }
        });

        // Setup action column with submit button
        actionColumn.setCellFactory(column -> new TableCell<PendingEvaluation, Button>() {
            private final Button submitButton = new Button("Submit Grade");

            {
                submitButton.setStyle(
                        "-fx-background-color: #4CAF50;" +
                                "-fx-text-fill: white;" +
                                "-fx-font-weight: bold;" +
                                "-fx-padding: 5 15;" +
                                "-fx-cursor: hand;" +
                                "-fx-border-radius: 4px;" +
                                "-fx-background-radius: 4px;");

                submitButton.setOnMouseEntered(
                        e -> submitButton.setStyle(
                                "-fx-background-color: #45a049;" +
                                        "-fx-text-fill: white;" +
                                        "-fx-font-weight: bold;" +
                                        "-fx-padding: 5 15;" +
                                        "-fx-cursor: hand;" +
                                        "-fx-border-radius: 4px;" +
                                        "-fx-background-radius: 4px;"));

                submitButton.setOnMouseExited(
                        e -> submitButton.setStyle(
                                "-fx-background-color: #4CAF50;" +
                                        "-fx-text-fill: white;" +
                                        "-fx-font-weight: bold;" +
                                        "-fx-padding: 5 15;" +
                                        "-fx-cursor: hand;" +
                                        "-fx-border-radius: 4px;" +
                                        "-fx-background-radius: 4px;"));
            }

            @Override
            protected void updateItem(Button item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    PendingEvaluation evaluation = getTableRow().getItem();
                    if (evaluation != null) {
                        submitButton.setOnAction(event -> handleGradeSubmission(evaluation));
                        setGraphic(submitButton);
                    } else {
                        setGraphic(null);
                    }
                }
            }
        });

        // Set table style
        evaluationsTable.setStyle(
                "-fx-background-color: white;" +
                        "-fx-border-color: #e2e8f0;" +
                        "-fx-border-radius: 4px;");

        // Set column resize policy
        evaluationsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // Set column widths
        double tableWidth = evaluationsTable.getPrefWidth();
        studentIdColumn.setPrefWidth(tableWidth * 0.15);
        studentNameColumn.setPrefWidth(tableWidth * 0.25);
        courseCodeColumn.setPrefWidth(tableWidth * 0.15);
        requestDateColumn.setPrefWidth(tableWidth * 0.15);
        gradeColumn.setPrefWidth(tableWidth * 0.15);
        actionColumn.setPrefWidth(tableWidth * 0.15);

        // Set table items
        evaluationsTable.setItems(pendingEvaluations);
    }

    private void setupStudentComboBox() {
        studentComboBox.setConverter(new StringConverter<User>() {
            @Override
            public String toString(User student) {
                if (student == null)
                    return "All Students";
                return student.getIdNumber() + " - " + student.getFullName();
            }

            @Override
            public User fromString(String string) {
                return null; // Not needed for this implementation
            }
        });

        // Add listener for student selection changes
        studentComboBox.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null) {
                // Load all pending evaluations
                loadAllPendingEvaluations();
            } else {
                // Load evaluations for selected student
                loadStudentEvaluations(newVal);
            }
        });

        // Style the ComboBox
        studentComboBox.setStyle(
                "-fx-background-color: white;" +
                        "-fx-border-color: #e2e8f0;" +
                        "-fx-border-radius: 4px;" +
                        "-fx-padding: 4px;");

        // Add "All Students" option
        studentComboBox.getItems().add(null);
    }

    private void loadStudentEvaluations(User student) {
        System.out.println("\n=== Loading evaluations for student: " + student.getIdNumber() + " ===");

        try {
            // Clear existing items
            pendingEvaluations.clear();
            evaluationsTable.getItems().clear();

            // Get all evaluations for this student
            List<User> students = courseDAO.getAllPendingEvaluations(currentUser.getIdNumber()).stream()
                    .filter(s -> s.getIdNumber().equals(student.getIdNumber()))
                    .toList();

            // Create evaluation objects and add to observable list
            ObservableList<PendingEvaluation> newEvaluations = FXCollections.observableArrayList();

            for (User s : students) {
                PendingEvaluation evaluation = new PendingEvaluation(
                        s.getIdNumber(),
                        s.getFullName(),
                        s.getCourseCode(),
                        s.getCourseName(),
                        s.getRequestDate() != null ? s.getRequestDate() : LocalDateTime.now());

                newEvaluations.add(evaluation);
            }

            // Set the table items
            evaluationsTable.setItems(newEvaluations);
            pendingEvaluations = newEvaluations;

            // Refresh the table
            evaluationsTable.refresh();

            System.out.println("Total evaluations loaded: " + evaluationsTable.getItems().size());

            if (evaluationsTable.getItems().isEmpty()) {
                Platform.runLater(() -> {
                    showAlert(Alert.AlertType.INFORMATION, "No Evaluations",
                            "No pending evaluations found for student " + student.getFullName());
                });
            }
        } catch (Exception e) {
            System.err.println("Error loading student evaluations: " + e.getMessage());
            e.printStackTrace();
            Platform.runLater(() -> {
                showErrorAlert("Error", "Failed to load student evaluations. Please try again.");
            });
        }
    }

    private void handleGradeSubmission(PendingEvaluation evaluation) {
        String grade = evaluation.getGrade();
        if (grade == null || grade.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Error", "Please select a grade before submitting.");
            return;
        }

        // Save the grade and update the evaluation request status
        boolean success = courseDAO.submitGrade(
                evaluation.getStudentId(),
                evaluation.getCourseCode(),
                grade);

        if (success) {
            Platform.runLater(() -> {
                showAlert(Alert.AlertType.INFORMATION, "Success", "Grade submitted successfully!");
                pendingEvaluations.remove(evaluation);
                // Refresh all data
                loadDashboardData();
            });
        } else {
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to submit grade. Please try again.");
        }
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Platform.runLater(() -> {
            Alert alert = new Alert(type);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(content);
            alert.show(); // Using show() instead of showAndWait() to avoid blocking
        });
    }

    @FXML
    private void handleViewClasses() {
        // TODO: Implement view all classes functionality
    }

    @FXML
    private void handleGradeEntry() {
        // TODO: Implement grade entry view
    }

    @FXML
    private void handleStudentRecords() {
        // TODO: Implement student records view
    }

    @FXML
    private void handleProfile() {
        // TODO: Implement profile view
    }

    @FXML
    private void handleLogout() {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Confirm Logout");
            alert.setHeaderText("Logout");
            alert.setContentText("Are you sure you want to logout?");

            Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
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

    private void showErrorAlert(String title, String content) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(content);
            alert.show(); // Using show() instead of showAndWait() to avoid blocking
        });
    }
}