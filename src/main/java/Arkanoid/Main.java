package Arkanoid;

import Arkanoid.level.LevelSelectionView;
import Arkanoid.manager.GameManager;
import Arkanoid.manager.HighScoreManager;
import Arkanoid.model.GameState;
import Arkanoid.view.GameView;
import Arkanoid.view.StartMenuView;
import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.stage.Stage;

/**
 * JavaFX application entry point. Wires together manager, views, and main loop.
 * Applies proper resource management between state transitions.
 */
public class Main extends Application {
    private GameManager gameManager;
    private GameView gameView;
    private LevelSelectionView levelSelectionView;
    private StartMenuView startMenuView;
    private AnimationTimer gameLoop;
    private Stage primaryStage;
    private HighScoreManager highScoreManager;
    private GameState lastState = null;

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;

        // Initialize game manager
        gameManager = new GameManager();
        highScoreManager = new HighScoreManager();

        // Initialize game view
        gameView = new GameView(gameManager);

        // Initialize level selection view
        levelSelectionView = new LevelSelectionView(primaryStage, gameManager.getLevelManager());
        levelSelectionView.setCallback(new LevelSelectionView.LevelSelectionCallback() {
            @Override
            public void onLevelSelected(int levelNumber) {
                gameManager.selectLevel(levelNumber);
                showGameView();
            }

            @Override
            public void onBack() {
                gameManager.setCurrentState(GameState.MENU);
                showStartMenu();
            }
        });

        // ESC returns to Start Menu
        gameView.getInputHandler().setOnShowStartMenu(this::showStartMenu);

        // Start menu (mouse-friendly)
        startMenuView = new StartMenuView(primaryStage);
        startMenuView.setOnStart(this::showLevelSelection);
        startMenuView.setOnSettings(startMenuView::showSettingsInline);
        startMenuView.setOnHighscores(() -> startMenuView.showLeaderboardInline(highScoreManager.getTopScores()));

        // Set up stage
        primaryStage.setTitle("Arkanoid Game");
        startMenuView.show();
        primaryStage.setResizable(false);
        primaryStage.show();

        // Start game loop
        startGameLoop();
    }

    private void showGameView() {
        primaryStage.setScene(gameView.getScene());
        primaryStage.setTitle("Arkanoid Game");

        // Re-bind ESC -> Start Menu
        gameView.getInputHandler().setOnShowStartMenu(this::showStartMenu);
    }

    private void showLevelSelection() {
        gameManager.cleanup();

        // Enter MENU state and play title music when opening level selection
        gameManager.showLevelSelection();
        levelSelectionView.refresh();
        levelSelectionView.show();
        primaryStage.setTitle("Arkanoid - Level Selection");
    }

    private void showStartMenu() {
        // Cleanup game state and go to Start Menu view
        gameManager.cleanup();
        gameManager.setCurrentState(GameState.MENU);
        startMenuView.show();
        primaryStage.setTitle("Arkanoid - Menu");
    }

    private void startGameLoop() {
        final long[] lastUpdate = {System.nanoTime()};

        gameLoop = new AnimationTimer() {
            @Override
            public void handle(long now) {
                double deltaTime = (now - lastUpdate[0]) / 1_000_000_000.0;
                lastUpdate[0] = now;

                deltaTime = Math.min(deltaTime, 0.05);

                // Detect state transitions for dialogs like GAME_OVER
                GameState current = gameManager.getCurrentState();
                if (current != lastState) {
                    onStateChanged(current);
                    lastState = current;
                }

                // ⚠️ CRITICAL: Chỉ update khi ở GameView VÀ đang PLAYING
                if (primaryStage.getScene() == gameView.getScene()) {
                    gameManager.update(deltaTime);
                    gameView.render(gameManager);
                }
            }
        };

        gameLoop.start();
    }

    private void onStateChanged(GameState state) {
        if (state == GameState.MENU) {
            // Always force switch to StartMenu scene when entering MENU
            javafx.application.Platform.runLater(this::showStartMenu);
        } else if (state == GameState.GAME_OVER) {
            javafx.application.Platform.runLater(() -> {
                int finalScore = (gameManager != null && gameManager.getScoreManager() != null)
                        ? gameManager.getScoreManager().getScore() : 0;

                if (gameView != null) {
                    gameView.showGameOverNamePrompt(finalScore, name -> {
                        if (highScoreManager != null) {
                            highScoreManager.addScore(name, finalScore);
                        }
                        if (startMenuView != null) {
                            startMenuView.show();
                            startMenuView.showLeaderboardInline(highScoreManager.getTopScores());
                        }
                    }, () -> {
                        if (startMenuView != null) {
                            startMenuView.show();
                            startMenuView.showLeaderboardInline(highScoreManager.getTopScores());
                        }
                    });
                }
            });
        }
    }

    @Override
    public void stop() {
        if (gameLoop != null) {
            gameLoop.stop();
        }

        if (gameManager != null) {
            gameManager.shutdown();
        }

    }

    public static void main(String[] args) {
        launch(args);
    }
}