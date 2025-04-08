/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/javafx/FXML2.java to edit this template
 */
package eduassess.controller;

import eduassess.dao.UserDAO;
import eduassess.model.User;
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
import javafx.scene.text.Font;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;
import javafx.scene.control.DialogPane;
import javafx.scene.layout.VBox;
import java.io.IOException;
import javafx.event.ActionEvent;

/**
 *
 * @author black
 */
public class LoginController implements Initializable {

    private double xOffset = 0;
    private double yOffset = 0;

    @FXML
    private VBox loginForm;

    @FXML
    private TextField emailField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Button loginButton;

    @FXML
    private Label createAccountLabel;

    private UserDAO userDAO;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        userDAO = new UserDAO();

        // Load Font Awesome
        try {
            Font.loadFont(getClass().getResourceAsStream("/eduassess/fonts/fa-solid-900.ttf"), 10);
        } catch (Exception e) {
            System.err.println("Error loading Font Awesome: " + e.getMessage());
        }

        // Set up field focus listeners
        setupFieldFocusListeners();

        // Set up button and label actions
        loginButton.setOnAction(e -> handleLogin());
        createAccountLabel.setOnMouseClicked(e -> navigateToCreateAccount());

        setupDraggable();
    }

    private void setupFieldFocusListeners() {
        // Email field
        emailField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                emailField.setPromptText("");
            } else {
                if (emailField.getText().isEmpty()) {
                    emailField.setPromptText("ID Number");
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
    }

    private void setupDraggable() {
        loginForm.setOnMousePressed(event -> {
            xOffset = event.getSceneX();
            yOffset = event.getSceneY();
        });

        loginForm.setOnMouseDragged(event -> {
            Stage stage = (Stage) loginForm.getScene().getWindow();
            stage.setX(event.getScreenX() - xOffset);
            stage.setY(event.getScreenY() - yOffset);
        });
    }

    @FXML
    public void handleLogin() {
        String idNumber = emailField.getText();
        String password = passwordField.getText();

        // Validate input
        if (idNumber.isEmpty() || password.isEmpty()) {
            showAlert("Error", "Please enter both ID Number and Password!");
            return;
        }

        // Authenticate user
        User user = userDAO.authenticateUser(idNumber, password);
        if (user == null) {
            showAlert("Error", "Invalid ID Number or Password!");
            return;
        }

        // Show welcome popup
        Alert welcomeAlert = new Alert(Alert.AlertType.INFORMATION);
        welcomeAlert.setTitle("Welcome to EduAssess");
        welcomeAlert.setHeaderText("Welcome back, " + user.getFullName() + "!");
        welcomeAlert.setContentText("Loading your dashboard...");
        welcomeAlert.getDialogPane().getStyleClass().add("welcome-alert");

        // Add custom styling to the alert
        DialogPane dialogPane = welcomeAlert.getDialogPane();
        dialogPane.setStyle(
                "-fx-background-color: white;" +
                        "-fx-border-color: #3b82f6;" +
                        "-fx-border-width: 2px;" +
                        "-fx-border-radius: 5px;");

        // Get the header text and style it
        Label headerLabel = (Label) dialogPane.lookup(".header-panel .label");
        if (headerLabel != null) {
            headerLabel.setStyle(
                    "-fx-font-size: 18px;" +
                            "-fx-font-weight: bold;" +
                            "-fx-text-fill: #1f2937;");
        }

        // Get the content text and style it
        Label contentLabel = (Label) dialogPane.lookup(".content");
        if (contentLabel != null) {
            contentLabel.setStyle(
                    "-fx-font-size: 14px;" +
                            "-fx-text-fill: #6b7280;");
        }

        welcomeAlert.show();

        // Navigate to dashboard after 3 seconds
        Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(3), event -> {
            welcomeAlert.close();
            try {
                // Determine which dashboard to load based on user type
                String dashboardPath;
                if (user.isAdmin()) {
                    dashboardPath = "/eduassess/views/AdminDashboard.fxml";
                } else if (user.isInstructor()) {
                    dashboardPath = "/eduassess/views/InstructorDashboard.fxml"; // Instructor dashboard
                } else {
                    dashboardPath = "/eduassess/views/StudentDashboard.fxml";
                }

                // Load the appropriate dashboard
                FXMLLoader loader = new FXMLLoader(getClass().getResource(dashboardPath));
                Parent root = loader.load();

                // Get the controller and set the user data
                if (user.isAdmin()) {
                    AdminDashboardController controller = loader.getController();
                    controller.initData(user);
                } else if (user.isInstructor()) {
                    InstructorDashboardController controller = loader.getController();
                    controller.initData(user);
                } else {
                    StudentDashboardController controller = loader.getController();
                    controller.initData(user);
                }

                // Show the dashboard
                Stage stage = (Stage) loginButton.getScene().getWindow();
                Scene scene = new Scene(root);
                stage.setScene(scene);

                // Center the window on screen
                stage.setX((Screen.getPrimary().getVisualBounds().getWidth() - stage.getWidth()) / 2);
                stage.setY((Screen.getPrimary().getVisualBounds().getHeight() - stage.getHeight()) / 2);

                stage.show();
            } catch (Exception e) {
                e.printStackTrace();
                showAlert("Error", "Failed to load dashboard. Please try again.");
            }
        }));
        timeline.play();
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    @FXML
    public void navigateToCreateAccount() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/eduassess/views/CreateAccount.fxml"));
            Stage stage = (Stage) createAccountLabel.getScene().getWindow();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleCreateAccount(ActionEvent event) {
        try {
            Stage stage = (Stage) loginButton.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("../views/StudentAccountType.fxml"));
            Parent studentAccountTypeView = loader.load();
            Scene scene = new Scene(studentAccountTypeView);
            stage.setScene(scene);
            stage.centerOnScreen();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Navigation Error", "Could not open account type selection screen.");
        }
    }
}
