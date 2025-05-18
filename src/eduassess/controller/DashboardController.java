package eduassess.controller;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;

public class DashboardController implements Initializable {
    @FXML
    private Label userNameLabel;

    @FXML
    private Label totalAssessmentsLabel;

    @FXML
    private Label completedAssessmentsLabel;

    @FXML
    private Label pendingAssessmentsLabel;

    @FXML
    private TableView<?> recentAssessmentsTable;

    @FXML
    private TableColumn<?, ?> titleColumn;

    @FXML
    private TableColumn<?, ?> dateColumn;

    @FXML
    private TableColumn<?, ?> statusColumn;

    @FXML
    private TableColumn<?, ?> scoreColumn;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Set user name (this should come from your login session)
        userNameLabel.setText("John Doe");

        // Set statistics (these should come from your database)
        totalAssessmentsLabel.setText("12");
        completedAssessmentsLabel.setText("8");
        pendingAssessmentsLabel.setText("4");

        // Initialize table columns
        setupTableColumns();

        // Load recent assessments data
        loadRecentAssessments();
    }

    private void setupTableColumns() {
        // Configure table columns
        // This will be implemented when we create the Assessment model
    }

    private void loadRecentAssessments() {
        // Load recent assessments from database
        // This will be implemented when we create the Assessment model
    }

    @FXML
    private void handleNewAssessment() {
        // Handle creating new assessment
        System.out.println("New Assessment button clicked");
    }

    @FXML
    private void handleLogout() {
        // Show confirmation dialog
        Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmDialog.setTitle("Confirm Logout");
        confirmDialog.setHeaderText("Are you sure you want to logout?");
        confirmDialog.setContentText("You will be returned to the login screen.");

        // Style the confirmation dialog
        DialogPane dialogPane = confirmDialog.getDialogPane();
        dialogPane.setStyle(
                "-fx-background-color: white;" +
                        "-fx-border-color: #ef4444;" +
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

        // Show dialog and wait for response
        confirmDialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    Parent root = FXMLLoader.load(getClass().getResource("/eduassess/views/Login.fxml"));
                    Stage stage = (Stage) userNameLabel.getScene().getWindow();
                    Scene scene = new Scene(root);
                    stage.setScene(scene);

                    // Center the window on screen
                    stage.setX((Screen.getPrimary().getVisualBounds().getWidth() - stage.getWidth()) / 2);
                    stage.setY((Screen.getPrimary().getVisualBounds().getHeight() - stage.getHeight()) / 2);

                    stage.show();
                } catch (Exception e) {
                    e.printStackTrace();
                    showAlert("Error", "Failed to logout. Please try again.");
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

    public void setUserName(String name) {
        userNameLabel.setText(name);
    }
}