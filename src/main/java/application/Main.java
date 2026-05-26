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
import javafx.stage.FileChooser;
import java.io.File;

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
        FileChooser fc = new FileChooser();
        fc.setTitle("Open Flight Dataset");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        File selectedFile = fc.showOpenDialog(stage);
        if (selectedFile == null) return;

        System.out.println("Loading dataset: " + selectedFile.getAbsolutePath());
        javafx.concurrent.Task<Integer> task = MemoryLoader.loadAsync(selectedFile);
        task.setOnSucceeded(e -> {
            System.out.println("Loaded " + task.getValue() + " flights.");
            reloadMainScene(stage);
        });
        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            String detail = (ex != null && ex.getMessage() != null) ? ex.getMessage() : "unknown error";
            System.err.println("Failed to load " + selectedFile + ": " + detail);
            if (ex != null) ex.printStackTrace();

            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                    javafx.scene.control.Alert.AlertType.ERROR);
            alert.setTitle("Load failed");
            alert.setHeaderText("Couldn't load " + selectedFile.getName());
            alert.setContentText("The file isn't a valid flights CSV. "
                    + "The current dataset is unchanged.\n\n" + detail);
            alert.initOwner(stage);
            alert.show();
        });
        Thread t = new Thread(task, "csv-loader");
        t.setDaemon(true);
        t.start();
    }

    /**
     * After a successful dataset swap, reload Main.fxml fresh into the shell's
     * centre. A new Controller instance runs initialize() over the now-updated
     * MemoryLoader data — map dots, filter dropdowns, autocomplete, and the
     * title all refresh in one pass.
     */
    private static void reloadMainScene(Stage stage) {
        try {
            Parent newRoot = FXMLLoader.load(Main.class.getResource("/Main.fxml"));
            Scene s = stage.getScene();
            if (s != null && s.getRoot() instanceof BorderPane) {
                ((BorderPane) s.getRoot()).setCenter(newRoot);
            }
        } catch (java.io.IOException ex) {
            ex.printStackTrace();
        }
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
