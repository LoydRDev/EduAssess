
package eduassess.controller;

import eduassess.dao.RoomUtilizationDAO;
import eduassess.dao.UserDAO;
import eduassess.dao.InstructorDAO;
import eduassess.model.Instructor;
import eduassess.model.RoomUtilization;
import eduassess.model.User;
import java.time.LocalDate;
import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.stream.Collectors;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

public class RoomUtilizationController implements Initializable {
    @FXML
    private TextField roomIdField;
    @FXML
    private ComboBox<String> courseCodeComboBox;
    @FXML
    private TextField startTimeField;
    @FXML
    private TextField endTimeField;
    @FXML
    private TextField purposeField;

    @FXML
    private DatePicker datePicker;

    @FXML
    private Label totalRoomsLabel;
    @FXML
    private Label avgUtilizationLabel;
    @FXML
    private Label totalUsersLabel;
    @FXML
    private Label totalStudentsLabel;
    @FXML
    private Label totalInstructorsLabel;
    @FXML
    private Label totalAssessmentsLabel;

    @FXML
    private Label userNameLabel;

    @FXML
    private TableView<User> recentUsersTable;
    @FXML
    private TableView<RoomUtilization> roomUtilizationTable;
    @FXML
    private TableColumn<User, String> idNumberColumn;
    @FXML
    private TableColumn<User, String> fullNameColumn;
    @FXML
    private TableColumn<User, String> userTypeColumn;
    @FXML
    private TableColumn<User, String> emailColumn;
    @FXML
    private TableColumn<User, String> statusColumn;

    private RoomUtilizationDAO roomUtilizationDAO;
    private InstructorDAO instructorDAO;
    private UserDAO userDAO;

    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        roomUtilizationDAO = new RoomUtilizationDAO();
        instructorDAO = new InstructorDAO();
        userDAO = new UserDAO();

        try {
            loadRoomUtilizationData();
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Error", "Failed to load room utilization data: " + e.getMessage());
        }
    }

    private void loadRoomUtilizationData() throws SQLException {
        try {
            // Load statistics
            if (totalUsersLabel != null) totalUsersLabel.setText(String.valueOf(userDAO.getTotalUsers()));
            if (totalStudentsLabel != null) totalStudentsLabel.setText(String.valueOf(userDAO.getTotalStudents()));
            if (totalInstructorsLabel != null) totalInstructorsLabel.setText(String.valueOf(userDAO.getTotalInstructors()));
            if (totalAssessmentsLabel != null) totalAssessmentsLabel.setText("0"); // TODO: Implement assessment counting

            // Load courses with assigned instructors
            if (courseCodeComboBox != null && instructorDAO != null) {
                List<String> coursesWithInstructors = instructorDAO.getAllAssignedCourses().stream()
                        .map(Instructor::getCourse_Code)
                        .distinct() // Remove duplicates
                        .sorted() // Sort alphabetically
                        .collect(Collectors.toList());
                courseCodeComboBox.setItems(FXCollections.observableArrayList(coursesWithInstructors));
                
                // Add a prompt text to guide users
                courseCodeComboBox.setPromptText("Select Course Code");
            }

            // Load room utilization data
            if (roomUtilizationDAO != null) {
                List<RoomUtilization> utilizations = roomUtilizationDAO.getRoomUtilization();
                
                if (totalRoomsLabel != null) {
                    totalRoomsLabel.setText(String.valueOf(utilizations.size()));
                }
                
                if (avgUtilizationLabel != null) {
                    double avgUtilization = utilizations.stream()
                            .mapToDouble(RoomUtilization::getUtilizationRate)
                            .average().orElse(0.0);
                    avgUtilizationLabel.setText(String.format("%.1f%%", avgUtilization));
                }

                if (roomUtilizationTable != null) {
                    roomUtilizationTable.setItems(FXCollections.observableArrayList(utilizations));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new SQLException("Failed to load room utilization data: " + e.getMessage());
        }
    }

    @FXML
    private void handleViewRoomSchedule(RoomUtilization room) {
        // Show detailed schedule for selected room
        List<RoomUtilization> schedule = roomUtilizationDAO.getRoomSchedule(room.getRoomId());

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Room Schedule: " + room.getRoomName());
        dialog.setHeaderText("Upcoming bookings for " + room.getRoomName());

        TableView<RoomUtilization> scheduleTable = new TableView<>();

        TableColumn<RoomUtilization, String> timeCol = new TableColumn<>("Time");
        TableColumn<RoomUtilization, String> courseCol = new TableColumn<>("Course");
        TableColumn<RoomUtilization, String> instructorCol = new TableColumn<>("Instructor");

        timeCol.setCellValueFactory(cellData -> new SimpleStringProperty(
                cellData.getValue().getStartTime() + " - " + cellData.getValue().getEndTime()));
        courseCol.setCellValueFactory(cellData -> new SimpleStringProperty(
                cellData.getValue().getCourseCode() + " - " + cellData.getValue().getCourseTitle()));
        instructorCol
                .setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getInstructorName()));

        scheduleTable.getColumns().addAll(timeCol, courseCol, instructorCol);
        scheduleTable.setItems(FXCollections.observableArrayList(schedule));

        dialog.getDialogPane().setContent(scheduleTable);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.showAndWait();
    }

    @FXML
    private void backToDashboard() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("../views/AdminDashboard.fxml"));
            Parent dashboardView = loader.load();
            Stage stage = (Stage) totalRoomsLabel.getScene().getWindow();
            Scene scene = new Scene(dashboardView);
            stage.setScene(scene);
            stage.centerOnScreen();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Error", "Failed to return to dashboard.");
        }
    }

    @FXML
    private void handleInstructorManagement() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("../views/InstructorManagement.fxml"));
            Parent instructorManagementView = loader.load();
            Stage stage = (Stage) userNameLabel.getScene().getWindow();
            Scene scene = new Scene(instructorManagementView);
            stage.setScene(scene);
            stage.centerOnScreen();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Error", "Failed to load Instructor Management. Please try again.");
        }
    }

    @FXML
    private void handleRoomUtilization() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/eduassess/views/RoomUtilization.fxml"));
            Parent roomUtilizationView = loader.load();
            Stage stage = (Stage) userNameLabel.getScene().getWindow();
            Scene scene = new Scene(roomUtilizationView);
            stage.setScene(scene);
            stage.centerOnScreen();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Error", "Failed to load Room Utilization. Please try again.");
        }
    }

    @FXML
    private void handleBookRoomPage() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/eduassess/views/BookRoom.fxml"));
            Parent root = loader.load();
            Stage popupStage = new Stage();
            popupStage.setTitle("Book Room");
            popupStage.setScene(new Scene(root));
            popupStage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            popupStage.setResizable(false);
            popupStage.centerOnScreen();
            popupStage.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Failed to open Book Room dialog");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
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

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    @FXML
    private void handleBookRoom() {
        try {
            // Get values from form
            String roomId = roomIdField.getText().trim();
            LocalDate date = datePicker.getValue();
            String selectedCourse = courseCodeComboBox.getValue();
            String startTime = startTimeField.getText().trim();
            String endTime = endTimeField.getText().trim();
            String purpose = purposeField.getText().trim();

            // Validate inputs
            if (roomId.isEmpty() || selectedCourse.isEmpty() || date == null ||
                    startTime.isEmpty() || endTime.isEmpty() || purpose.isEmpty()) {
                showAlert("Error", "All fields are required");
                return;
            }

            // Validate time format (HH:MM)
            if (!startTime.matches("^([0-1]?[0-9]|2[0-3]):[0-5][0-9]$") ||
                    !endTime.matches("^([0-1]?[0-9]|2[0-3]):[0-5][0-9]$")) {
                showAlert("Error", "Time must be in HH:MM format");
                return;
            }

            // Create booking
            RoomUtilization booking = new RoomUtilization();
            booking.setRoomId(roomId);
            booking.setCourseCode(selectedCourse);
            booking.setBookingDate(date);
            booking.setStartTime(startTime);
            booking.setEndTime(endTime);
            booking.setPurpose(purpose);

            // Save booking
            roomUtilizationDAO.saveRoomBooking(booking);
            showAlert("Success", "Room booked successfully");

            // Refresh data
            loadRoomUtilizationData();

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Failed to book room: " + e.getMessage());
        }
    }
}
