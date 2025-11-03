package Arkanoid.renderer;

import Arkanoid.level.Level;
import Arkanoid.manager.GameManager;
import Arkanoid.manager.ScoreManager;
import Arkanoid.model.*;
import Arkanoid.util.Constants;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Renders the entire game based on the current GameState.
 * Uses cached and pre-scaled resources for performance.
 */
public class Renderer {
    private final GraphicsContext gc;

    // Static cache to avoid reloading images for each Renderer instance
    private static Map<String, Image> cachedBrickImages = null;
    private static Image cachedDefaultBackground = null;
    private static WritableImage cachedDefaultScaledBg = null;

    // Cache for level backgrounds (key = backgroundPath)
    private static final Map<String, Image> cachedLevelBackgrounds = new HashMap<>();
    private static final Map<String, WritableImage> cachedScaledLevelBgs = new HashMap<>();

    private final Map<String, Image> brickImages;
    private final Image defaultBackgroundImage;
    private final WritableImage defaultScaledBackground;

    // Current level background reference
    private String currentBgPath = null;
    private WritableImage currentScaledBg = null;

    // Cached heart image for lives UI
    private static Image cachedHeartImage = null;

    public Renderer(GraphicsContext gc) {
        this.gc = gc;

        // Cache brick images
        if (cachedBrickImages == null) {
            cachedBrickImages = new HashMap<>();
            cachedBrickImages.put("NORMAL", loadImage("/images/bricks/brick_normal.png"));
            cachedBrickImages.put("HARD", loadImage("/images/bricks/brick_hard.png"));
            cachedBrickImages.put("UNBREAKABLE", loadImage("/images/bricks/brick_unbreakable.png"));
            cachedBrickImages.put("BROKEN", loadImage("/images/bricks/brick_broken.png"));
        }
        this.brickImages = cachedBrickImages;

        // Load default background only once
        if (cachedDefaultBackground == null) {
            cachedDefaultBackground = loadImage("/images/level/space.png");
            if (cachedDefaultBackground != null) {
                cachedDefaultScaledBg = prescaleBackground(cachedDefaultBackground);
            }
        }
        this.defaultBackgroundImage = cachedDefaultBackground;
        this.defaultScaledBackground = cachedDefaultScaledBg;

        // Start with default background
        this.currentScaledBg = cachedDefaultScaledBg;

        // Load heart image once for UI
        if (cachedHeartImage == null) {
            cachedHeartImage = loadImage("/images/powerup/heart.png");
        }
    }

    /**
     * Pre-scales the background image to window size for faster draws.
     */
    private WritableImage prescaleBackground(Image original) {
        if (original == null) return null;

        try {
            double targetWidth = Constants.WINDOW_WIDTH;
            double targetHeight = Constants.WINDOW_HEIGHT;

            if (Math.abs(original.getWidth() - targetWidth) < 1 &&
                    Math.abs(original.getHeight() - targetHeight) < 1) {
                return null; // Dùng ảnh gốc luôn
            }

            Canvas tempCanvas = new Canvas(targetWidth, targetHeight);
            GraphicsContext tempGc = tempCanvas.getGraphicsContext2D();
            tempGc.drawImage(original, 0, 0, targetWidth, targetHeight);

            WritableImage scaled = new WritableImage((int)targetWidth, (int)targetHeight);
            SnapshotParameters params = new SnapshotParameters();
            params.setFill(Color.TRANSPARENT);
            tempCanvas.snapshot(params, scaled);

            return scaled;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private Image loadImage(String path) {
        try {
            InputStream stream = getClass().getResourceAsStream(path);
            if (stream == null) {
                return null;
            }

            // ✅ Load với smooth=false để tiết kiệm bộ nhớ
            Image img = new Image(stream, 0, 0, true, false);
            stream.close(); // ✅ Đóng stream sau khi load

            return img;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Renders a frame based on the game state and ensures backgrounds are cached.
     */
    public void render(GameManager gameManager) {
        GameState state = gameManager.getCurrentState();

        // Xác định background cần sử dụng theo level/state
        String desiredBgPath = null;
        Level currentLevel = gameManager.getCurrentLevel();

        if (currentLevel != null && state == GameState.PLAYING) {
            desiredBgPath = currentLevel.getBackgroundImage();
        }

        if (!isSameBackground(desiredBgPath, currentBgPath)) {
            loadLevelBackground(desiredBgPath);
        }

        // Vẽ nền đã cache để tối ưu hiệu năng
        drawBackground();

        switch (state) {
            case MENU -> { /* keep only background */ }
            case PLAYING, PAUSED -> {
                // Vẽ gameplay: gạch, paddle, bóng, power-up, đạn, UI
                renderGame(gameManager);
                if (state == GameState.PAUSED) renderPauseOverlay();
            }
            case GAME_OVER -> {
                // Vẫn vẽ gameplay làm nền, sau đó phủ lớp Game Over
                renderGame(gameManager);
                renderGameOver(gameManager.getScoreManager());
            }
            case LEVEL_COMPLETE -> {
                // Vẽ gameplay làm nền, sau đó phủ lớp Level Complete
                renderGame(gameManager);
                renderLevelComplete(gameManager);
            }
        }
    }

    private boolean isSameBackground(String path1, String path2) {
        if (path1 == null && path2 == null) return true;
        if (path1 == null || path2 == null) return false;
        return path1.equals(path2);
    }

    private void loadLevelBackground(String backgroundPath) {
        if (backgroundPath == null || backgroundPath.trim().isEmpty()) {
            currentBgPath = null;
            currentScaledBg = defaultScaledBackground;
            return;
        }

        if (cachedScaledLevelBgs.containsKey(backgroundPath)) {
            currentBgPath = backgroundPath;
            currentScaledBg = cachedScaledLevelBgs.get(backgroundPath);
            return;
        }

        try {
            Image originalBg = cachedLevelBackgrounds.get(backgroundPath);
            if (originalBg == null) {
                originalBg = loadImage(backgroundPath);
                if (originalBg != null) {
                    cachedLevelBackgrounds.put(backgroundPath, originalBg);
                }
            }

            if (originalBg != null) {
                WritableImage scaledBg = prescaleBackground(originalBg);
                cachedScaledLevelBgs.put(backgroundPath, scaledBg);

                currentBgPath = backgroundPath;
                currentScaledBg = scaledBg;
            } else {
                currentBgPath = null;
                currentScaledBg = defaultScaledBackground;
            }

        } catch (Exception e) {
            e.printStackTrace();
            currentBgPath = null;
            currentScaledBg = defaultScaledBackground;
        }
    }

    /** Draws background from cache when possible. */
    private void drawBackground() {
        if (currentScaledBg != null) {
            gc.drawImage(currentScaledBg, 0, 0);
        } else if (currentBgPath != null) {
            Image original = cachedLevelBackgrounds.get(currentBgPath);
            if (original != null) {
                gc.drawImage(original, 0, 0, Constants.WINDOW_WIDTH, Constants.WINDOW_HEIGHT);
            } else {
                drawDefaultBackground();
            }
        } else {
            drawDefaultBackground();
        }
    }

    private void drawDefaultBackground() {
        if (defaultScaledBackground != null) {
            gc.drawImage(defaultScaledBackground, 0, 0);
        } else if (defaultBackgroundImage != null) {
            gc.drawImage(defaultBackgroundImage, 0, 0,
                    Constants.WINDOW_WIDTH, Constants.WINDOW_HEIGHT);
        } else {
            gc.setFill(Constants.BACKGROUND_COLOR);
            gc.fillRect(0, 0, Constants.WINDOW_WIDTH, Constants.WINDOW_HEIGHT);
        }
    }

    private void renderGame(GameManager gameManager) {
        for (Brick brick : gameManager.getBricks()) {
            if (brick.isDestroyed()) continue; // do not draw destroyed bricks
            String type = brick.getType().name();
            Image img;
            if (brick.getType() == BrickType.HARD && brick instanceof Brick) {
                // Use broken sprite if HARD has been damaged
                if (((Brick) brick).isDamaged()) {
                    img = brickImages.get("BROKEN");
                } else {
                    img = brickImages.get("HARD");
                }
            } else {
                img = brickImages.get(type);
            }
            if (img != null) {
                gc.drawImage(img, brick.getX(), brick.getY(),
                        brick.getWidth(), brick.getHeight());
            } else {
                brick.render(gc);
            }
        }

        for (PowerUps powerUp : gameManager.getPowerUps()) {
            powerUp.render(gc);
        }

        gameManager.getPaddle().render(gc);
        for (Ball ball : gameManager.getBalls()) {
            ball.render(gc);
        }

        // Render bullets if present via reflection of a getter (not exposed): draw from manager state indirectly
        try {
            java.lang.reflect.Method m = gameManager.getClass().getDeclaredMethod("getPowerUps");
        } catch (Exception ignored) {}

        // Best-effort: bullets rendered by checking a known field via reflection
        try {
            java.lang.reflect.Field f = gameManager.getClass().getDeclaredField("bullets");
            f.setAccessible(true);
            Object list = f.get(gameManager);
            if (list instanceof java.util.List<?> l) {
                for (Object o : l) {
                    if (o instanceof Arkanoid.model.Bullet b) {
                        b.render(gc);
                    }
                }
            }
        } catch (Exception ignored) {}

        renderUI(gameManager);
    }

    private void renderUI(GameManager gameManager) {
        ScoreManager scoreManager = gameManager.getScoreManager();
        Level currentLevel = gameManager.getCurrentLevel();

        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Arial", Constants.UI_FONT_SIZE));

        // Draw framed score box at top-left
        double boxX = 10;
        double boxY = 8;
        double boxW = 180;
        double boxH = 34;
        gc.setFill(Color.rgb(0, 0, 0, 0.4));
        gc.fillRoundRect(boxX, boxY, boxW, boxH, 8, 8);
        gc.setStroke(Color.WHITE);
        gc.setLineWidth(2);
        gc.strokeRoundRect(boxX, boxY, boxW, boxH, 8, 8);
        gc.setTextAlign(TextAlignment.LEFT);
        gc.setFill(Color.WHITE); // ensure text is visible after dark box fill
        gc.fillText("Score: " + scoreManager.getScore(), boxX + 10, boxY + 23);

        gc.setTextAlign(TextAlignment.CENTER);
        if (currentLevel != null) {
            gc.fillText(currentLevel.getLevelName() + " (" +
                            currentLevel.getLevelNumber() + "/" +
                            gameManager.getLevelManager().getTotalLevels() + ")",
                    Constants.WINDOW_WIDTH / 2.0, 25);
        }

        // Draw hearts for lives (3 icons), top-right
        int maxHearts = 3;
        int lives = Math.max(0, Math.min(maxHearts, scoreManager.getLives()));
        double heartSize = 22;
        double spacing = 8;
        double startX = Constants.WINDOW_WIDTH - 10 - (maxHearts * heartSize + (maxHearts - 1) * spacing);
        double y = 8;
        for (int i = 0; i < maxHearts; i++) {
            double x = startX + i * (heartSize + spacing);
            double alpha = (i < lives) ? 1.0 : 0.25;
            if (cachedHeartImage != null) {
                gc.setGlobalAlpha(alpha);
                gc.drawImage(cachedHeartImage, x, y, heartSize, heartSize);
                gc.setGlobalAlpha(1.0);
            } else {
                gc.setFill(Color.color(1, 0.2, 0.3, alpha));
                gc.fillOval(x, y, heartSize, heartSize);
            }
        }
    }

    private void renderMenu() {
        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Arial", 60));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.fillText("ARKANOID", Constants.WINDOW_WIDTH / 2.0, 150);

        gc.setFont(Font.font("Arial", 30));
        gc.fillText("Press SPACE to Start", Constants.WINDOW_WIDTH / 2.0, 250);

        gc.setFill(Color.CYAN);
        gc.setFont(Font.font("Arial", 26));
        gc.fillText("Press L for Level Selection", Constants.WINDOW_WIDTH / 2.0, 300);

        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Arial", 20));
        gc.fillText("Controls:", Constants.WINDOW_WIDTH / 2.0, 360);
        gc.setFont(Font.font("Arial", 16));
        gc.fillText("LEFT/RIGHT or A/D - Move Paddle", Constants.WINDOW_WIDTH / 2.0, 390);
        gc.fillText("SPACE - Launch Ball", Constants.WINDOW_WIDTH / 2.0, 415);
        gc.fillText("P - Pause Game", Constants.WINDOW_WIDTH / 2.0, 440);
        gc.fillText("ESC - Return to Menu", Constants.WINDOW_WIDTH / 2.0, 465);
    }

    private void renderPauseOverlay() {
        gc.setFill(Color.rgb(0, 0, 0, 0.7));
        gc.fillRect(0, 0, Constants.WINDOW_WIDTH, Constants.WINDOW_HEIGHT);

        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Arial", 48));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.fillText("PAUSED", Constants.WINDOW_WIDTH / 2.0, Constants.WINDOW_HEIGHT / 2.0 - 20);
        gc.setFont(Font.font("Arial", 20));
        gc.fillText("Press P to Resume", Constants.WINDOW_WIDTH / 2.0, Constants.WINDOW_HEIGHT / 2.0 + 30);
        gc.fillText("Press ESC for Menu", Constants.WINDOW_WIDTH / 2.0, Constants.WINDOW_HEIGHT / 2.0 + 60);
    }

    private void renderGameOver(ScoreManager scoreManager) {
        gc.setFill(Color.rgb(0, 0, 0, 0.8));
        gc.fillRect(0, 0, Constants.WINDOW_WIDTH, Constants.WINDOW_HEIGHT);
        gc.setFill(Color.RED);
        gc.setFont(Font.font("Arial", 60));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.fillText("GAME OVER", Constants.WINDOW_WIDTH / 2.0, 250);
        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Arial", 30));
        gc.fillText("Final Score: " + scoreManager.getScore(), Constants.WINDOW_WIDTH / 2.0, 320);
        gc.fillText("High Score: " + scoreManager.getHighScore(), Constants.WINDOW_WIDTH / 2.0, 360);
        gc.setFont(Font.font("Arial", 20));
        gc.fillText("Press SPACE to Try Again", Constants.WINDOW_WIDTH / 2.0, 420);
        gc.fillText("Press ESC for Menu", Constants.WINDOW_WIDTH / 2.0, 450);
    }

    private void renderLevelComplete(GameManager gameManager) {
        ScoreManager scoreManager = gameManager.getScoreManager();
        Level currentLevel = gameManager.getCurrentLevel();

        gc.setFill(Color.rgb(0, 0, 0, 0.8));
        gc.fillRect(0, 0, Constants.WINDOW_WIDTH, Constants.WINDOW_HEIGHT);
        gc.setFill(Color.GOLD);
        gc.setFont(Font.font("Arial", 60));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.fillText("LEVEL COMPLETE!", Constants.WINDOW_WIDTH / 2.0, 220);
        gc.setFill(Color.LIGHTGREEN);
        gc.setFont(Font.font("Arial", 30));
        if (currentLevel != null) {
            gc.fillText(currentLevel.getLevelName(), Constants.WINDOW_WIDTH / 2.0, 270);
        }
        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Arial", 28));
        gc.fillText("Score: " + scoreManager.getScore(), Constants.WINDOW_WIDTH / 2.0, 330);
    }

    /** Clears static caches (call on application shutdown if needed). */
    public static void clearCache() {
        cachedBrickImages = null;
        cachedDefaultBackground = null;
        cachedDefaultScaledBg = null;
        cachedLevelBackgrounds.clear();
        cachedScaledLevelBgs.clear();
    }
}