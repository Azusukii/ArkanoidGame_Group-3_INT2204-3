package Arkanoid.view;

import Arkanoid.manager.GameManager;
import Arkanoid.renderer.Renderer;
import Arkanoid.util.Constants;
import Arkanoid.util.InputHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.paint.Color;

/**
 * Owns the JavaFX Scene and Canvas, binds input handlers, and provides inline overlays.
 */
public class GameView {
    private Scene scene;
    private Canvas canvas;
    private GraphicsContext gc;
    private Renderer renderer;
    private InputHandler inputHandler;
    private StackPane root;

    // Overlay for inline dialogs (e.g., Game Over name input)
    private StackPane overlay;

    public GameView(GameManager gameManager) {
        // Create canvas
        canvas = new Canvas(Constants.WINDOW_WIDTH, Constants.WINDOW_HEIGHT);
        gc = canvas.getGraphicsContext2D();

        // Create renderer
        renderer = new Renderer(gc);

        // Create input handler
        inputHandler = new InputHandler(gameManager);

        // Create scene
        root = new StackPane(canvas);
        scene = new Scene(root, Constants.WINDOW_WIDTH, Constants.WINDOW_HEIGHT);

        // Make root focusable to receive input
        root.setFocusTraversable(true);

        // Set up input handling
        scene.setOnKeyPressed(inputHandler::handleKeyPressed);
        scene.setOnKeyReleased(inputHandler::handleKeyReleased);
    }

    /** Renders one frame using the internal Renderer. */
    // Vẽ một khung hình mới dựa trên trạng thái game hiện tại
    public void render(GameManager gameManager) {
        renderer.render(gameManager);
    }

    /** @return the JavaFX Scene that hosts the Canvas and input handlers. */
    public Scene getScene() {
        return scene;
    }

    /** @return the InputHandler so callers can set callbacks. */
    public InputHandler getInputHandler() {
        return inputHandler;
    }

    /** Requests focus on the root so input handler works. */
    public void requestFocus() {
        if (root != null) {
            root.requestFocus();
        }
    }

    /** Cleans up resources and detaches handlers. */
    public void cleanup() {
        // Clear event handlers to prevent memory leaks
        if (scene != null) {
            scene.setOnKeyPressed(null);
            scene.setOnKeyReleased(null);
        }
    }

    // Inline Styled Overlay (similar look to StartMenuView)
    private static final String STYLE_SCENE_DIM = "-fx-background-color: rgba(0,0,0,0.65);";
    private static final String STYLE_PANEL_ROOT =
            "-fx-background-color: white;" +
            "-fx-background-radius: 25px;" +
            "-fx-border-color: #0D47A1;" +
            "-fx-border-width: 10px;" +
            "-fx-border-radius: 20px;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 15, 0, 0, 5);";
    private static final String STYLE_TITLE =
            "-fx-font-family: 'Arial Black', sans-serif;" +
            "-fx-font-size: 36px;" +
            "-fx-text-fill: #0D47A1;";
    private static final String STYLE_CONTENT_LABEL =
            "-fx-font-family: 'Arial', sans-serif;" +
            "-fx-font-size: 16px;" +
            "-fx-text-fill: #0D47A1;";
    private static final String STYLE_BUTTON_IDLE =
            "-fx-background-color: #1976D2;" +
            "-fx-text-fill: white;" +
            "-fx-background-radius: 30px;" +
            "-fx-border-radius: 30px;" +
            "-fx-font-family: 'Arial', sans-serif;" +
            "-fx-font-size: 16px;" +
            "-fx-font-weight: bold;" +
            "-fx-pref-height: 42px;" +
            "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.2), 5, 0, 0, 2);";
    private static final String STYLE_BUTTON_HOVER =
            "-fx-background-color: #42A5F5;" +
            "-fx-text-fill: white;" +
            "-fx-background-radius: 30px;" +
            "-fx-border-radius: 30px;" +
            "-fx-font-family: 'Arial', sans-serif;" +
            "-fx-font-size: 16px;" +
            "-fx-font-weight: bold;" +
            "-fx-pref-height: 42px;" +
            "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.2), 5, 0, 0, 2);";

    private void applyHover(Button b) {
        b.setOnMouseEntered(e -> b.setStyle(STYLE_BUTTON_HOVER));
        b.setOnMouseExited(e -> b.setStyle(STYLE_BUTTON_IDLE));
    }

    /**
     * Shows an inline overlay prompting for player name on Game Over.
     */
    // Hiển thị overlay nhập tên khi Game Over (không mở cửa sổ mới)
    public void showGameOverNamePrompt(int finalScore,
                                       java.util.function.Consumer<String> onSubmit,
                                       Runnable onSkip) {
        hideOverlay();

        // Dim layer
        overlay = new StackPane();
        overlay.setStyle(STYLE_SCENE_DIM);
        overlay.setPrefSize(Constants.WINDOW_WIDTH, Constants.WINDOW_HEIGHT);
        overlay.setPickOnBounds(true);

        // Content panel
        Label title = new Label("GAME OVER");
        title.setStyle(STYLE_TITLE);

        Label info = new Label("Final Score: " + finalScore);
        info.setStyle(STYLE_CONTENT_LABEL);

        HBox nameRow = new HBox(8);
        nameRow.setAlignment(Pos.CENTER);
        Label nameLabel = new Label("Your Name:");
        nameLabel.setStyle(STYLE_CONTENT_LABEL);
        TextField nameField = new TextField("Player");
        nameField.setPrefColumnCount(16);
        nameRow.getChildren().addAll(nameLabel, nameField);

        Button saveBtn = new Button("Save & View Leaderboard");
        saveBtn.setStyle(STYLE_BUTTON_IDLE);
        applyHover(saveBtn);
        saveBtn.setOnAction(e -> {
            String name = nameField.getText() == null ? "Player" : nameField.getText().trim();
            hideOverlay();
            if (onSubmit != null) onSubmit.accept(name.isEmpty() ? "Player" : name);
        });

        Button skipBtn = new Button("Skip");
        skipBtn.setStyle(STYLE_BUTTON_IDLE);
        applyHover(skipBtn);
        skipBtn.setOnAction(e -> {
            hideOverlay();
            if (onSkip != null) onSkip.run();
        });

        HBox actions = new HBox(10, saveBtn, skipBtn);
        actions.setAlignment(Pos.CENTER);

        VBox panel = new VBox(14, title, info, nameRow, actions);
        panel.setAlignment(Pos.CENTER);
        panel.setPadding(new Insets(26, 36, 26, 36));
        panel.setStyle(STYLE_PANEL_ROOT);
        panel.setMaxWidth(480);

        overlay.getChildren().add(panel);
        StackPane.setAlignment(panel, Pos.CENTER);

        root.getChildren().add(overlay);

        // Focus text field for convenience
        nameField.requestFocus();
        nameField.positionCaret(nameField.getText().length());
    }

    public void hideOverlay() {
        if (overlay != null) {
            root.getChildren().remove(overlay);
            overlay = null;
        }
    }
}