package application;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class Main extends Application {
    @Override
    public void start(Stage primaryStage) {
        try {
            BorderPane shell = new BorderPane();
            TitleBar titleBar = new TitleBar(primaryStage);
            shell.setTop(titleBar);

            AnchorPane mainContent = FXMLLoader.load(getClass().getResource("/Main.fxml"));
            shell.setCenter(mainContent);

            Scene scene = new Scene(shell, 1200, 800);
            scene.getStylesheets().add(getClass().getResource("application.css").toExternalForm());

            Image icon = new Image("logo.png");
            primaryStage.getIcons().add(icon);

            primaryStage.initStyle(StageStyle.UNDECORATED);
            primaryStage.setTitle("SkyScraper");
            primaryStage.setResizable(true);
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