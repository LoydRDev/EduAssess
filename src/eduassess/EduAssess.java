/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/javafx/FXML.java to edit this template
 */
package eduassess;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

/**
 *
 * @author black
 */
public class EduAssess extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        try {
            // Load the FXML file using absolute path from the root of the classpath
            Parent root = FXMLLoader.load(getClass().getResource("/eduassess/views/Login.fxml"));

            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.initStyle(StageStyle.UNDECORATED);
            stage.show();

            // Center the window on screen
            stage.setX((Screen.getPrimary().getVisualBounds().getWidth() - stage.getWidth()) / 2);
            stage.setY((Screen.getPrimary().getVisualBounds().getHeight() - stage.getHeight()) / 2);
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }

}
