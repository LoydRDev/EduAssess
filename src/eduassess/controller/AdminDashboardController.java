package eduassess.controller;

import eduassess.dao.UserDAO;
import eduassess.model.User;
import eduassess.util.SessionManager;
import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Callback;

public class AdminDashboardController implements Initializable {
    private double xOffset = 0;
    private double yOffset = 0;
    private UserDAO userDAO;

    @FXML
    private HBox header;

    @FXML
    private Label userNameLabel;
    @FXML
    private Label totalUsersLabel;
    @FXML
    private Label totalStudentsLabel;
    @FXML
    private Label totalInstructorsLabel;
    @FXML
    private Label totalAssessmentsLabel;

    @FXML
    private TableView<User> recentUsersTable;
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

    @FXML
    private TableColumn<User, Void> actionsColumn;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        userDAO = new UserDAO();
        setupDraggable();
        setupTables();

        // Set the user name from the current session
        User currentUser = SessionManager.getCurrentUser();
        if (currentUser != null) {
            userNameLabel.setText(currentUser.getFullName());
        }

        loadDashboardData();
    }

    private void setupDraggable() {
        header.setOnMousePressed(event -> {
            xOffset = event.getSceneX();
            yOffset = event.getSceneY();
        });

        header.setOnMouseDragged(event -> {
            Stage stage = (Stage) header.getScene().getWindow();
            stage.setX(event.getScreenX() - xOffset);
            stage.setY(event.getScreenY() - yOffset);
        });
    }

    public void initData(User user) {
        userNameLabel.setText(user.getFullName());
        loadDashboardData();
    }

    private void setupTables() {
        // Configure Recent Users table columns
        idNumberColumn.setCellValueFactory(cellData -> cellData.getValue().idNumberProperty());
        fullNameColumn.setCellValueFactory(cellData -> cellData.getValue().fullNameProperty());
        userTypeColumn.setCellValueFactory(cellData -> cellData.getValue().userTypeProperty());
        emailColumn.setCellValueFactory(cellData -> cellData.getValue().emailProperty());
        statusColumn.setCellValueFactory(cellData -> cellData.getValue().statusProperty());

        // Configure actions column
        actionsColumn.setCellFactory(param -> new TableCell<User, Void>() {
            private final Button editButton = new Button("Edit");
            private final Button deleteButton = new Button("Delete");
            private final HBox buttons = new HBox(10, editButton, deleteButton);

            {
                editButton.getStyleClass().add("table-button");
                deleteButton.getStyleClass().add("table-button");
                deleteButton.setStyle("-fx-text-fill: #ef4444;"); // Red color for delete

                editButton.setOnAction(event -> {
                    User user = getTableView().getItems().get(getIndex());
                    showEditDialog(user);
                });

                deleteButton.setOnAction(event -> {
                    User user = getTableView().getItems().get(getIndex());
                    handleDelete(user);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(buttons);
                }
            }
        });
    }

    private void loadDashboardData() {
        // Load statistics
        totalUsersLabel.setText(String.valueOf(userDAO.getTotalUsers()));
        totalStudentsLabel.setText(String.valueOf(userDAO.getTotalStudents()));
        totalInstructorsLabel.setText(String.valueOf(userDAO.getTotalInstructors()));
        totalAssessmentsLabel.setText("0"); // TODO: Implement assessment counting

        // Load recent users data
        loadRecentUsers();
    }

    private void loadRecentUsers() {
        recentUsersTable.setItems(FXCollections.observableArrayList(userDAO.getRecentUsers()));
    }

    @FXML
    private void handleAddInstructor() {
        Dialog<User> dialog = new Dialog<>();
        dialog.setTitle("Add Instructor");
        dialog.setHeaderText("Enter Instructor Details");
        dialog.initModality(Modality.APPLICATION_MODAL);

        // Create the dialog content
        VBox content = new VBox(10);
        TextField idNumberField = new TextField();
        idNumberField.setPromptText("ID Number");
        TextField fullNameField = new TextField();
        fullNameField.setPromptText("Full Name");
        TextField emailField = new TextField();
        emailField.setPromptText("Email");
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Password");
        PasswordField confirmPasswordField = new PasswordField();
        confirmPasswordField.setPromptText("Confirm Password");

        content.getChildren().addAll(
                new Label("ID Number:"), idNumberField,
                new Label("Full Name:"), fullNameField,
                new Label("Email:"), emailField,
                new Label("Password:"), passwordField,
                new Label("Confirm Password:"), confirmPasswordField);

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        // Disable OK button initially
        Node okButton = dialog.getDialogPane().lookupButton(ButtonType.OK);
        okButton.setDisable(true);

        // Add listeners to enable/disable OK button based on input validation
        idNumberField.textProperty().addListener((obs, oldVal, newVal) -> validateInput(okButton, idNumberField,
                fullNameField, emailField, passwordField, confirmPasswordField));
        fullNameField.textProperty().addListener((obs, oldVal, newVal) -> validateInput(okButton, idNumberField,
                fullNameField, emailField, passwordField, confirmPasswordField));
        emailField.textProperty().addListener((obs, oldVal, newVal) -> validateInput(okButton, idNumberField,
                fullNameField, emailField, passwordField, confirmPasswordField));
        passwordField.textProperty().addListener((obs, oldVal, newVal) -> validateInput(okButton, idNumberField,
                fullNameField, emailField, passwordField, confirmPasswordField));
        confirmPasswordField.textProperty().addListener((obs, oldVal, newVal) -> validateInput(okButton, idNumberField,
                fullNameField, emailField, passwordField, confirmPasswordField));

        dialog.setResultConverter(buttonType -> {
            if (buttonType == ButtonType.OK) {
                if (!passwordField.getText().equals(confirmPasswordField.getText())) {
                    showAlert("Error", "Passwords do not match!");
                    return null;
                }

                // Check if ID number already exists
                if (userDAO.isIdNumberExists(idNumberField.getText())) {
                    showAlert("Error", "ID Number already exists!");
                    return null;
                }

                // Check if email already exists
                if (userDAO.isEmailExists(emailField.getText())) {
                    showAlert("Error", "Email already exists!");
                    return null;
                }

                User newInstructor = new User(
                        idNumberField.getText(),
                        fullNameField.getText(),
                        emailField.getText(),
                        "INSTRUCTOR");
                newInstructor.setPassword(passwordField.getText());
                return newInstructor;
            }
            return null;
        });

        Optional<User> result = dialog.showAndWait();
        result.ifPresent(instructor -> {
            if (userDAO.createUser(instructor)) {
                loadDashboardData(); // Refresh the table
                showAlert("Success", "Instructor added successfully!");
            } else {
                showAlert("Error", "Failed to add instructor. Please try again.");
            }
        });
    }

    private void validateInput(Node okButton, TextField idNumberField, TextField fullNameField,
            TextField emailField, PasswordField passwordField, PasswordField confirmPasswordField) {
        boolean isValid = !idNumberField.getText().trim().isEmpty() &&
                !fullNameField.getText().trim().isEmpty() &&
                !emailField.getText().trim().isEmpty() &&
                !passwordField.getText().trim().isEmpty() &&
                !confirmPasswordField.getText().trim().isEmpty() &&
                emailField.getText().matches("^[A-Za-z0-9+_.-]+@(.+)$") &&
                passwordField.getText().equals(confirmPasswordField.getText()) &&
                passwordField.getText().length() >= 6;

        okButton.setDisable(!isValid);
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

    private void showEditDialog(User user) {
        Dialog<User> dialog = new Dialog<>();
        dialog.setTitle("Edit User");
        dialog.setHeaderText("Edit User Details");

        // Create the dialog content
        VBox content = new VBox(10);
        TextField fullNameField = new TextField(user.getFullName());
        TextField emailField = new TextField(user.getEmail());
        TextField idNumberField = new TextField(user.getIdNumber());
        ComboBox<String> userTypeCombo = new ComboBox<>(FXCollections.observableArrayList("STUDENT", "INSTRUCTOR"));
        ComboBox<String> statusCombo = new ComboBox<>(FXCollections.observableArrayList("ACTIVE", "INACTIVE"));

        userTypeCombo.setValue(user.getUserType());
        statusCombo.setValue(user.getStatus());

        content.getChildren().addAll(
                new Label("Full Name:"), fullNameField,
                new Label("Email:"), emailField,
                new Label("ID Number:"), idNumberField,
                new Label("User Type:"), userTypeCombo,
                new Label("Status:"), statusCombo);

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(buttonType -> {
            if (buttonType == ButtonType.OK) {
                User updatedUser = new User(
                        idNumberField.getText(),
                        fullNameField.getText(),
                        emailField.getText(),
                        userTypeCombo.getValue());
                updatedUser.setId(user.getId());
                updatedUser.setStatus(statusCombo.getValue());
                return updatedUser;
            }
            return null;
        });

        Optional<User> result = dialog.showAndWait();
        result.ifPresent(updatedUser -> {
            if (userDAO.updateUser(updatedUser)) {
                loadDashboardData(); // Refresh the table
                showAlert("Success", "User updated successfully!");
            } else {
                showAlert("Error", "Failed to update user. Please try again.");
            }
        });
    }

    private void handleDelete(User user) {
        Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmDialog.setTitle("Confirm Delete");
        confirmDialog.setHeaderText("Delete User");
        confirmDialog.setContentText("Are you sure you want to delete this user?");

        confirmDialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                if (userDAO.deleteUser(user.getId())) {
                    loadDashboardData(); // Refresh the table
                    showAlert("Success", "User deleted successfully!");
                } else {
                    showAlert("Error", "Failed to delete user. Please try again.");
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
}