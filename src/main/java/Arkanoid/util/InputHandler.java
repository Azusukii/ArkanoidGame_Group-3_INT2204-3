package Arkanoid.util;

import Arkanoid.manager.GameManager;
import Arkanoid.model.GameState;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

/**
 * Centralizes keyboard input handling and routes actions by current {@link GameState}.
 * Exposes a callback for showing the Start Menu.
 */
public class InputHandler {
    private final GameManager gameManager;
    private Runnable onShowStartMenu; // Callback to show Start Menu UI

    public InputHandler(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    public void handleKeyPressed(KeyEvent event) {
        KeyCode code = event.getCode();
        GameState state = gameManager.getCurrentState();

        switch (state) {
            case MENU -> handleMenuInput(code);
            case PLAYING -> handlePlayingInput(code, true);
            case PAUSED -> handlePausedInput(code);
            case GAME_OVER -> handleGameOverInput(code);
            case LEVEL_COMPLETE -> handleLevelCompleteInput(code);
        }
    }

    public void handleKeyReleased(KeyEvent event) {
        if (gameManager.getCurrentState() == GameState.PLAYING) {
            handlePlayingInput(event.getCode(), false);
        }
    }

    // Xử lý phím trong trạng thái MENU
    private void handleMenuInput(KeyCode code) {
        switch (code) {
            case ESCAPE -> { System.exit(0); }
            default -> {
                // ignore other keys
            }
        }
    }

    // Xử lý phím trong trạng thái PLAYING
    private void handlePlayingInput(KeyCode code, boolean pressed) {
        switch (code) {
            case LEFT, A -> gameManager.getPaddle().setMovingLeft(pressed);
            case RIGHT, D -> gameManager.getPaddle().setMovingRight(pressed);
            case SPACE -> {
                if (pressed) gameManager.launchBall();
            }
            case P -> {
                if (pressed) gameManager.pauseGame();
            }
            case ESCAPE -> {
                if (pressed) {
                    // ESC: return to Start Menu
                    gameManager.setCurrentState(GameState.MENU);
                    if (onShowStartMenu != null) onShowStartMenu.run();
                }
            }
            default -> {
                // ignore other keys
            }
        }
    }

    // Xử lý phím trong trạng thái PAUSED
    private void handlePausedInput(KeyCode code) {
        switch (code) {
            case P -> gameManager.pauseGame(); // resume
            case ESCAPE -> {
                // ESC from paused -> return to Start Menu
                gameManager.setCurrentState(GameState.MENU);
                if (onShowStartMenu != null) onShowStartMenu.run();
            }
            default -> {
                // ignore other keys
            }
        }
    }

    // Xử lý phím trong trạng thái GAME OVER
    private void handleGameOverInput(KeyCode code) {
        switch (code) {
            case SPACE -> gameManager.startGame();
            case ESCAPE -> {
                gameManager.setCurrentState(GameState.MENU);
                if (onShowStartMenu != null) onShowStartMenu.run();
            }
            default -> {
                // ignore other keys
            }
        }
    }

    // Xử lý phím trong trạng thái LEVEL COMPLETE
    private void handleLevelCompleteInput(KeyCode code) {
        switch (code) {
            case SPACE -> gameManager.nextLevel();
            case ESCAPE -> {
                gameManager.setCurrentState(GameState.MENU);
                if (onShowStartMenu != null) onShowStartMenu.run();
            }
            default -> {
                // ignore other keys
            }
        }
    }

    /** Sets callback to show Start Menu when requested by input. */
    public void setOnShowStartMenu(Runnable callback) {
        this.onShowStartMenu = callback;
    }
}
