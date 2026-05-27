package application;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.input.Dragboard;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
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

            // Drop overlay sits over the shell so it stays in place across
            // scene swaps (Main ⇄ Graphs/Results).
            StackPane dragOverlay = new StackPane();
            dragOverlay.setStyle("-fx-background-color: rgba(0,0,0,0.55);");
            Label dropLabel = new Label("Drop dataset here");
            dropLabel.setStyle("-fx-text-fill: white; -fx-font-size: 36px; -fx-font-weight: bold;");
            dragOverlay.getChildren().add(dropLabel);
            dragOverlay.setVisible(false);
            dragOverlay.setMouseTransparent(true);

            StackPane root = new StackPane(shell, dragOverlay);

            Scene scene = new Scene(root, 1200, 800);
            scene.getStylesheets().add(getClass().getResource("application.css").toExternalForm());

            scene.getAccelerators().put(
                    KeyCombination.keyCombination("Shortcut+O"),
                    () -> onOpenDataset(primaryStage));

            scene.setOnDragOver(e -> {
                if (e.getDragboard().hasFiles()) {
                    e.acceptTransferModes(TransferMode.COPY);
                }
                e.consume();
            });
            // Show on any drag-enter; setOnDragOver below filters to files for the
            // actual drop. Calling dragboard.hasFiles() here triggers a synchronous
            // GTK mime-type query that re-enters JavaFX's DnDGesture state machine
            // and crashes it with IndexOutOfBoundsException on Linux.
            scene.setOnDragEntered(e -> {
                dragOverlay.setVisible(true);
                e.consume();
            });
            scene.setOnDragExited(e -> {
                dragOverlay.setVisible(false);
                e.consume();
            });
            scene.setOnDragDropped(e -> {
                Dragboard db = e.getDragboard();
                boolean accepted = false;
                if (db.hasFiles() && !db.getFiles().isEmpty()) {
                    loadDataset(primaryStage, db.getFiles().get(0));
                    accepted = true;
                }
                dragOverlay.setVisible(false);
                e.setDropCompleted(accepted);
                e.consume();
            });

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
        loadDataset(stage, selectedFile);
    }

    /** Loads a user-chosen dataset asynchronously; on success it's added to the recents list. */
    static void loadDataset(Stage stage, File file) {
        loadDatasetInternal(stage, file, true);
    }

    /** Loads the bundled sample. Not added to recents — it's always reachable via the Sample menu item. */
    static void loadBundledSample(Stage stage) {
        loadDatasetInternal(stage, new File("flights_sample.csv"), false);
    }

    private static void loadDatasetInternal(Stage stage, File file, boolean addToRecents) {
        System.out.println("Loading dataset: " + file.getAbsolutePath());
        javafx.concurrent.Task<Integer> task = MemoryLoader.loadAsync(file);
        task.setOnSucceeded(e -> {
            System.out.println("Loaded " + task.getValue() + " flights.");
            if (addToRecents) MemoryLoader.addRecentDataset(file);
            reloadMainScene(stage);
        });
        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            String detail = (ex != null && ex.getMessage() != null) ? ex.getMessage() : "unknown error";
            System.err.println("Failed to load " + file + ": " + detail);
            if (ex != null) ex.printStackTrace();

            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                    javafx.scene.control.Alert.AlertType.ERROR);
            alert.setTitle("Load failed");
            alert.setHeaderText("Couldn't load " + file.getName());
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
            BorderPane shell = findShell(stage.getScene());
            if (shell != null) shell.setCenter(newRoot);
        } catch (java.io.IOException ex) {
            ex.printStackTrace();
        }
    }

    /** Shared helper: replace the shell's centre content (used by scene-switch handlers). */
    public static void swapCenter(Node anyNodeInScene, Parent newCenter) {
        if (anyNodeInScene == null || newCenter == null) return;
        Scene s = anyNodeInScene.getScene();
        if (s == null) return;
        BorderPane shell = findShell(s);
        if (shell != null) {
            shell.setCenter(newCenter);
        } else {
            s.setRoot(newCenter);
        }
    }

    /** Returns the current centre node of the shell — used by callers that
     *  want to swap something in and then restore the previous centre later
     *  (e.g. Results → Details → back to the same table). */
    public static Parent getShellCenter(Node anyNodeInScene) {
        if (anyNodeInScene == null) return null;
        Scene s = anyNodeInScene.getScene();
        if (s == null) return null;
        BorderPane shell = findShell(s);
        if (shell == null) return null;
        Node c = shell.getCenter();
        return (c instanceof Parent) ? (Parent) c : null;
    }

    /** Returns the BorderPane shell, whether it's the scene root or a child of the StackPane root that hosts the drag overlay. */
    private static BorderPane findShell(Scene s) {
        if (s == null) return null;
        Parent root = s.getRoot();
        if (root instanceof BorderPane) return (BorderPane) root;
        if (root instanceof StackPane) {
            for (Node n : ((StackPane) root).getChildren()) {
                if (n instanceof BorderPane) return (BorderPane) n;
            }
        }
        return null;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
