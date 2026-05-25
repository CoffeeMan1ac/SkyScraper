package application;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.stage.Stage;

/**
 * Custom undecorated-window title bar. Always pinned to the top of the
 * BorderPane shell used by Main.java; survives scene-content swaps.
 *
 *   [ 📁 ]  SkyScraper — N flights · file.csv         [ ● ● ● ]
 *
 * Left: folder-icon button (file open — wired in a later commit).
 * Center-left: the app title / status text.
 * Right: macOS-style colored window buttons — yellow minimise, green fullscreen,
 *        red close. Drag the bar to move the window, double-click to toggle
 *        fullscreen.
 */
public final class TitleBar extends HBox {

    private static final double BAR_HEIGHT = 30;
    private static final double BTN_SIZE = 12;

    private static final String BG_COLOR    = "#b5b6b8";   // slightly lighter than app's #A7A8AA
    private static final String TEXT_COLOR  = "#2a2a2a";
    private static final String COLOR_MIN   = "#ffbd2e";
    private static final String COLOR_FULL  = "#28c940";
    private static final String COLOR_CLOSE = "#ff5f57";

    private final Stage stage;
    private final Label titleLabel = new Label("SkyScraper");
    private final Button folderButton;

    private double dragOffsetX;
    private double dragOffsetY;

    public TitleBar(Stage stage) {
        this.stage = stage;
        setAlignment(Pos.CENTER_LEFT);
        setPrefHeight(BAR_HEIGHT);
        setMinHeight(BAR_HEIGHT);
        setMaxHeight(BAR_HEIGHT);
        setStyle("-fx-background-color: " + BG_COLOR + ";");
        setPadding(new Insets(0, 12, 0, 8));
        setSpacing(10);

        folderButton = buildFolderButton();
        titleLabel.setStyle("-fx-text-fill: " + TEXT_COLOR + "; -fx-font-size: 12;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button minBtn   = buildCircleButton(COLOR_MIN,   () -> stage.setIconified(true));
        Button fullBtn  = buildCircleButton(COLOR_FULL,  () -> stage.setFullScreen(!stage.isFullScreen()));
        Button closeBtn = buildCircleButton(COLOR_CLOSE, stage::close);

        HBox windowButtons = new HBox(8, minBtn, fullBtn, closeBtn);
        windowButtons.setAlignment(Pos.CENTER);

        getChildren().addAll(folderButton, titleLabel, spacer, windowButtons);

        // Drag the bar to move the window.
        setOnMousePressed(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                dragOffsetX = e.getSceneX();
                dragOffsetY = e.getSceneY();
            }
        });
        setOnMouseDragged(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                stage.setX(e.getScreenX() - dragOffsetX);
                stage.setY(e.getScreenY() - dragOffsetY);
            }
        });
        // Double-click toggles fullscreen — replaces the OS-bar gesture we lose
        // by going undecorated.
        setOnMouseClicked(e -> {
            if (e.getClickCount() == 2 && e.getButton() == MouseButton.PRIMARY) {
                stage.setFullScreen(!stage.isFullScreen());
            }
        });
    }

    public void setTitle(String t) {
        titleLabel.setText(t);
        stage.setTitle(t);   // keeps taskbar/iconified-state label in sync
    }

    public Button getFolderButton() {
        return folderButton;
    }

    private Button buildCircleButton(String color, Runnable action) {
        Button b = new Button();
        b.setPrefSize(BTN_SIZE, BTN_SIZE);
        b.setMinSize(BTN_SIZE, BTN_SIZE);
        b.setMaxSize(BTN_SIZE, BTN_SIZE);
        b.setFocusTraversable(false);
        b.setStyle(
                "-fx-background-color: " + color + ";"
              + "-fx-background-radius: " + (BTN_SIZE / 2.0) + ";"
              + "-fx-padding: 0;"
              + "-fx-border-color: rgba(0,0,0,0.18);"
              + "-fx-border-radius: " + (BTN_SIZE / 2.0) + ";"
        );
        b.setCursor(Cursor.HAND);
        b.setOnAction(e -> action.run());
        return b;
    }

    private Button buildFolderButton() {
        // Material-style "folder_open" path scaled to ~16 px.
        SVGPath icon = new SVGPath();
        icon.setContent("M2,4 L8,4 L10,6 L18,6 L18,16 L2,16 Z");
        icon.setFill(Color.web(TEXT_COLOR));
        icon.setStroke(Color.TRANSPARENT);

        Button b = new Button();
        b.setGraphic(icon);
        b.setFocusTraversable(false);
        b.setStyle("-fx-background-color: transparent; -fx-padding: 4 8 4 8; -fx-cursor: hand;");
        b.setOnAction(e -> {
            // Wired to FileChooser in a later commit.
            System.out.println("Open dataset — not yet wired.");
        });
        return b;
    }

    /** Convenience: locate the TitleBar from anywhere in the scene graph. */
    public static TitleBar of(Node anyNodeInScene) {
        if (anyNodeInScene == null) return null;
        Scene s = anyNodeInScene.getScene();
        if (s == null) return null;
        if (s.getRoot() instanceof BorderPane) {
            Node top = ((BorderPane) s.getRoot()).getTop();
            if (top instanceof TitleBar) return (TitleBar) top;
        }
        return null;
    }

    /** Convenience: swap the BorderPane shell's centre (used by scene-switch handlers). */
    public static void swapCenter(Node anyNodeInScene, javafx.scene.Parent newCenter) {
        if (anyNodeInScene == null) return;
        Scene s = anyNodeInScene.getScene();
        if (s == null) return;
        if (s.getRoot() instanceof BorderPane) {
            ((BorderPane) s.getRoot()).setCenter(newCenter);
        } else {
            // Fallback for any path that hasn't been migrated.
            s.setRoot(newCenter);
        }
    }
}
