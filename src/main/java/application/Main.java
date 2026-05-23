package application;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

public class Main extends Application {
    @Override
    public void start(Stage primaryStage) {
        try {
            AnchorPane root = FXMLLoader.load(getClass().getResource("/Main.fxml"));
            BorderPane borderPane = new BorderPane();
            Scene scene = new Scene(borderPane, 1200, 800);
            scene.getStylesheets().add(getClass().getResource("application.css").toExternalForm());

            borderPane.setCenter(root);

            Image icon = new Image("logo.png");
            primaryStage.getIcons().add(icon);

            primaryStage.setTitle("SkyScraper");
            primaryStage.setResizable(false);
            primaryStage.centerOnScreen();
            primaryStage.setScene(scene);
            primaryStage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}