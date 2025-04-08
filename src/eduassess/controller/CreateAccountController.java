package eduassess.controller;

import eduassess.dao.UserDAO;
import eduassess.model.User;
import java.awt.Desktop;
import java.net.URI;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.animation.PauseTransition;
import javafx.util.Duration;
import java.io.IOException;
import javafx.application.Platform;

public class CreateAccountController implements Initializable {

    @FXML
    private TextField fullNameField;

    @FXML
    private TextField emailField;

    @FXML
    private TextField idNumberField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private PasswordField confirmPasswordField;

    @FXML
    private Button createAccountButton;

    @FXML
    private Label backToLoginLabel;

    @FXML
    private Label titleLabel;

    private UserDAO userDAO;
    private String accountType;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        userDAO = new UserDAO();
        accountType = "STUDENT"; // Set default account type

        // Set up field focus listeners
        setupFieldFocusListeners();

        // Set up button and label actions
        createAccountButton.setOnAction(e -> handleCreateAccount());
        backToLoginLabel.setOnMouseClicked(e -> navigateToLogin());
    }

    public void initData(String accountType) {
        this.accountType = accountType != null ? accountType : "STUDENT";
        // Update UI based on account type
        if (accountType != null && accountType.equals("TRANSFEREE")) {
            // Add any specific UI changes for transferee accounts
            titleLabel.setText("Create Transferee Account");
        } else {
            titleLabel.setText("Create New Student Account");
        }
    }

    private void setupFieldFocusListeners() {
        // Full Name field
        fullNameField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                fullNameField.setPromptText("");
            } else {
                if (fullNameField.getText().isEmpty()) {
                    fullNameField.setPromptText("Full Name");
                }
            }
        });

        // Email field
        emailField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                emailField.setPromptText("");
            } else {
                if (emailField.getText().isEmpty()) {
                    emailField.setPromptText("Email Address");
                }
            }
        });

        // ID Number field
        idNumberField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                idNumberField.setPromptText("");
            } else {
                if (idNumberField.getText().isEmpty()) {
                    idNumberField.setPromptText("ID Number");
                }
            }
        });

        // Password field
        passwordField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                passwordField.setPromptText("");
            } else {
                if (passwordField.getText().isEmpty()) {
                    passwordField.setPromptText("Password");
                }
            }
        });

        // Confirm Password field
        confirmPasswordField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                confirmPasswordField.setPromptText("");
            } else {
                if (confirmPasswordField.getText().isEmpty()) {
                    confirmPasswordField.setPromptText("Confirm Password");
                }
            }
        });
    }

    @FXML
    public void handleCreateAccount() {
        // Validate fields
        if (fullNameField.getText().isEmpty() ||
                emailField.getText().isEmpty() ||
                idNumberField.getText().isEmpty() ||
                passwordField.getText().isEmpty() ||
                confirmPasswordField.getText().isEmpty()) {
            showAlert("Error", "All fields are required!");
            return;
        }

        // Validate password match
        if (!passwordField.getText().equals(confirmPasswordField.getText())) {
            showAlert("Error", "Passwords do not match!");
            return;
        }

        // Validate email format
        if (!emailField.getText().matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            showAlert("Error", "Invalid email format!");
            return;
        }

        // Check if email already exists
        if (userDAO.isEmailExists(emailField.getText())) {
            showAlert("Error", "Email already exists!");
            return;
        }

        // Check if ID number already exists
        if (userDAO.isIdNumberExists(idNumberField.getText())) {
            showAlert("Error", "ID Number already exists!");
            return;
        }

        // Create user object
        User user = new User(
                idNumberField.getText(),
                fullNameField.getText(),
                emailField.getText(),
                accountType);
        user.setPassword(passwordField.getText());

        // Save to database
        if (userDAO.createUser(user)) {
            // Show success alert without waiting
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Success");
            alert.setHeaderText(null);
            alert.setContentText("Account created successfully!");
            alert.show();

            // Create a pause transition for 1.5 seconds
            PauseTransition pause = new PauseTransition(Duration.seconds(1.5));
            pause.setOnFinished(event -> {
                // Close the alert if it's still showing
                alert.close();
                // Navigate to student account type view with student ID
                navigateToStudentAccountType(idNumberField.getText());
            });
            pause.play();
        } else {
            showAlert("Error", "Failed to create account. Please try again.");
        }
    }

    private void showAlert(String title, String content) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(content);
            alert.show(); // Using show() instead of showAndWait() to avoid blocking
        });
    }

    @FXML
    public void navigateToLogin() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/eduassess/views/Login.fxml"));
            Stage stage = (Stage) backToLoginLabel.getScene().getWindow();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void openRegistrationPortal() {
        try {
            Desktop.getDesktop().browse(new URI("https://banportal.uc.edu.ph/registration"));
        } catch (Exception e) {
            e.printStackTrace();
            // Show error dialog if browser cannot be opened
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Cannot Open Browser");
            alert.setContentText("Please visit https://banportal.uc.edu.ph/registration manually to register.");
            alert.showAndWait();
        }
    }

    private void navigateToStudentAccountType(String studentId) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/eduassess/views/StudentAccountType.fxml"));
            Parent studentAccountTypeView = loader.load();

            StudentAccountTypeController controller = loader.getController();
            controller.setStudentId(studentId);

            Stage stage = (Stage) createAccountButton.getScene().getWindow();
            Scene scene = new Scene(studentAccountTypeView);
            stage.setScene(scene);
            stage.centerOnScreen();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Error", "Could not navigate to account type selection screen.");
        }
    }
}