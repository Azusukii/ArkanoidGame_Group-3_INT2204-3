package Arkanoid.level;

import Arkanoid.util.Constants;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

/**
 * Simple JavaFX UI for selecting a level to play.
 * Shows a grid of buttons for each available level and a back button to return to the menu.
 * Uses a callback interface to notify the host when a level is chosen.
 */
public class LevelSelectionView {
    private final Stage stage;
    private final LevelManager levelManager;
    private LevelSelectionCallback callback;

    // UI Components
    private VBox mainContainer;
    private GridPane levelGrid;
    private Label titleLabel;
    private Button backButton;

    private Scene scene;

    /** Callback interface for level selection UI actions. */
    public interface LevelSelectionCallback {
        void onLevelSelected(int levelNumber);
        void onBack();
    }

    public LevelSelectionView(Stage stage, LevelManager levelManager) {
        this.stage = stage;
        this.levelManager = levelManager;
        initializeUI();
    }

    /** Initializes the selection UI components and scene. */
    private void initializeUI() {
        mainContainer = new VBox(20);
        mainContainer.setAlignment(Pos.TOP_CENTER);
        mainContainer.setPadding(new Insets(40));
        mainContainer.setStyle("-fx-background-color: #1a1a2e;");

        // Title
        titleLabel = new Label("SELECT LEVEL");
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 48));
        titleLabel.setTextFill(Color.WHITE);

        // Level Grid
        levelGrid = new GridPane();
        levelGrid.setHgap(20);
        levelGrid.setVgap(20);
        levelGrid.setAlignment(Pos.CENTER);

        createLevelButtons();

        // Back Button
        backButton = new Button("BACK TO MENU");
        backButton.setFont(Font.font("Arial", FontWeight.BOLD, 20));
        backButton.setPrefSize(250, 50);
        styleButton(backButton, "#e74c3c");

        backButton.setOnAction(e -> {
            if (callback != null) {
                callback.onBack();
            }
        });

        mainContainer.getChildren().addAll(titleLabel, levelGrid, backButton);

        // Create scene on init
        scene = new Scene(mainContainer, Constants.WINDOW_WIDTH, Constants.WINDOW_HEIGHT);

        // Handle ESC to go back to menu
        scene.setOnKeyPressed(e -> {
            switch (e.getCode()) {
                case ESCAPE:
                    if (callback != null) {
                        callback.onBack();
                    }
                    break;
                default:
                    break;
            }
        });
    }

    /** Creates level buttons based on the number of loaded levels. */
    private void createLevelButtons() {
        levelGrid.getChildren().clear();

        int totalLevels = levelManager.getTotalLevels();
        int cols = 5; // 5 levels per row

        for (int i = 1; i <= totalLevels; i++) {
            Button levelButton = createLevelButton(i);
            int row = (i - 1) / cols;
            int col = (i - 1) % cols;
            levelGrid.add(levelButton, col, row);
        }
    }

    private Button createLevelButton(int levelNumber) {
        Button button = new Button(String.valueOf(levelNumber));
        button.setFont(Font.font("Arial", FontWeight.BOLD, 24));
        button.setPrefSize(100, 100);

        boolean isUnlocked = levelManager.isLevelUnlocked(levelNumber);

        if (isUnlocked) {
            styleButton(button, "#3498db");
            button.setOnAction(e -> {
                if (callback != null) {
                    callback.onLevelSelected(levelNumber);
                }
            });

            if (levelNumber == levelManager.getCurrentLevelNumber()) {
                styleButton(button, "#2ecc71");
            }
        } else {
            button.setText("LOCKED");
            styleButton(button, "#7f8c8d");
            button.setDisable(true);
        }

    return button;
    }

    private void styleButton(Button button, String color) {
        button.setStyle(
                "-fx-background-color: " + color + ";" +
                        "-fx-text-fill: white;" +
                        "-fx-background-radius: 10;" +
                        "-fx-border-radius: 10;" +
                        "-fx-border-color: white;" +
                        "-fx-border-width: 2;" +
                        "-fx-cursor: hand;"
        );

        button.setOnMouseEntered(e -> {
            if (!button.isDisabled()) {
                button.setStyle(
                        "-fx-background-color: derive(" + color + ", 20%);" +
                                "-fx-text-fill: white;" +
                                "-fx-background-radius: 10;" +
                                "-fx-border-radius: 10;" +
                                "-fx-border-color: white;" +
                                "-fx-border-width: 3;" +
                                "-fx-cursor: hand;" +
                                "-fx-scale-x: 1.05;" +
                                "-fx-scale-y: 1.05;"
                );
            }
        });

        button.setOnMouseExited(e -> {
            if (!button.isDisabled()) {
                styleButton(button, color); // Reset
            }
        });
    }

    /** Shows the level selection screen on the provided Stage. */
    public void show() {
        createLevelButtons(); // Refresh buttons
        stage.setScene(scene);
        stage.setTitle("Arkanoid - Level Selection");
    }

    /** @return the JavaFX Scene instance used by this view. */
    public Scene getScene() {
        return scene;
    }

    /** Rebuilds the level grid (e.g., after unlocking). */
    public void refresh() {
        createLevelButtons();
    }

    /** Sets the callback invoked when selecting a level or going back. */
    public void setCallback(LevelSelectionCallback callback) {
        this.callback = callback;
    }

    /** @return the root container for embedders or styling. */
    public VBox getMainContainer() {
        return mainContainer;
    }
}
