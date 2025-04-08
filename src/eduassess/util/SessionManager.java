package eduassess.util;

import eduassess.model.User;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

public class SessionManager {
    private static User currentUser;
    private static Stage mainStage;

    public static void initSession(User user, Stage stage) {
        currentUser = user;
        mainStage = stage;
    }

    public static User getCurrentUser() {
        return currentUser;
    }

    public static void setCurrentUser(User user) {
        currentUser = user;
    }

    public static void logout() {
        currentUser = null;
        if (mainStage != null) {
            // Load the login view
            try {
                FXMLLoader loader = new FXMLLoader(SessionManager.class.getResource("/eduassess/view/Login.fxml"));
                Parent root = loader.load();
                Scene scene = new Scene(root);
                mainStage.setScene(scene);
                mainStage.show();
            } catch (Exception e) {
                e.printStackTrace();
                showError("Error during logout", "Could not return to login screen.");
            }
        }
    }

    private static void showError(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}