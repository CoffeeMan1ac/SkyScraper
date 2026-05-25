package application;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

public class Main extends Application {
    @Override
    public void start(Stage primaryStage) {
        try {
            BorderPane shell = new BorderPane();
            // File-open lives inside Main.fxml as an ordinary in-scene button.
            // On Sway/Wayland the top strip of the client area can swallow
            // pointer events (GTK CSD shadow zone), so we don't put
            // actionable controls in BorderPane.top.

            AnchorPane mainContent = FXMLLoader.load(getClass().getResource("/Main.fxml"));
            shell.setCenter(mainContent);

            Scene scene = new Scene(shell, 1200, 800);
            scene.getStylesheets().add(getClass().getResource("application.css").toExternalForm());

            scene.getAccelerators().put(
                    KeyCombination.keyCombination("Shortcut+O"),
                    () -> onOpenDataset(primaryStage));

            Image icon = new Image("logo.png");
            primaryStage.getIcons().add(icon);

            primaryStage.setTitle("SkyScraper");
            primaryStage.setResizable(true);
            primaryStage.centerOnScreen();
            primaryStage.setScene(scene);
            primaryStage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static void onOpenDataset(Stage stage) {
        System.out.println("Open dataset — not yet wired.");
        // FileChooser will be wired in the next commit.
    }

    /** Shared helper: replace the shell's centre content (used by scene-switch handlers). */
    public static void swapCenter(Node anyNodeInScene, Parent newCenter) {
        if (anyNodeInScene == null || newCenter == null) return;
        Scene s = anyNodeInScene.getScene();
        if (s == null) return;
        if (s.getRoot() instanceof BorderPane) {
            ((BorderPane) s.getRoot()).setCenter(newCenter);
        } else {
            s.setRoot(newCenter);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}