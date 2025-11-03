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
 * OPTIMIZED: Pre-scaled backgrounds per level, cached resources, NO memory leaks.
 */
public class Renderer {
    private final GraphicsContext gc;

    // ✅ Static cache để tránh load lại ảnh mỗi lần tạo Renderer
    private static Map<String, Image> cachedBrickImages = null;
    private static Image cachedDefaultBackground = null;
    private static WritableImage cachedDefaultScaledBg = null;

    // ✅ Cache cho background theo level (key = backgroundPath)
    private static final Map<String, Image> cachedLevelBackgrounds = new HashMap<>();
    private static final Map<String, WritableImage> cachedScaledLevelBgs = new HashMap<>();

    private final Map<String, Image> brickImages;
    private final Image defaultBackgroundImage;
    private final WritableImage defaultScaledBackground;

    // ✅ Current level background (thay đổi theo level)
    private String currentBgPath = null;
    private WritableImage currentScaledBg = null;

    public Renderer(GraphicsContext gc) {
        this.gc = gc;

        // ✅ Cache brick images
        if (cachedBrickImages == null) {
            cachedBrickImages = new HashMap<>();
            cachedBrickImages.put("NORMAL", loadImage("/images/bricks/brick_normal.png"));
            cachedBrickImages.put("HARD", loadImage("/images/bricks/brick_hard.png"));
            cachedBrickImages.put("UNBREAKABLE", loadImage("/images/bricks/brick_unbreakable.png"));
            cachedBrickImages.put("BROKEN", loadImage("/images/bricks/brick_broken.png"));
            System.out.println("✅ Brick images cached");
        }
        this.brickImages = cachedBrickImages;

        // ✅ Load default background chỉ 1 lần
        if (cachedDefaultBackground == null) {
            cachedDefaultBackground = loadImage("/images/level/space.png");
            if (cachedDefaultBackground != null) {
                cachedDefaultScaledBg = prescaleBackground(cachedDefaultBackground);
                System.out.println("✅ Default background cached and pre-scaled");
            }
        }
        this.defaultBackgroundImage = cachedDefaultBackground;
        this.defaultScaledBackground = cachedDefaultScaledBg;

        // ✅ Ban đầu dùng default background
        this.currentScaledBg = cachedDefaultScaledBg;
    }

    /**
     * Pre-scale background image một lần duy nhất
     */
    private WritableImage prescaleBackground(Image original) {
        if (original == null) return null;

        try {
            double targetWidth = Constants.WINDOW_WIDTH;
            double targetHeight = Constants.WINDOW_HEIGHT;

            // Kiểm tra xem ảnh đã đúng kích thước chưa
            if (Math.abs(original.getWidth() - targetWidth) < 1 &&
                    Math.abs(original.getHeight() - targetHeight) < 1) {
                System.out.println("✅ Background đã đúng kích thước, không cần scale");
                return null; // Dùng ảnh gốc luôn
            }

            Canvas tempCanvas = new Canvas(targetWidth, targetHeight);
            GraphicsContext tempGc = tempCanvas.getGraphicsContext2D();
            tempGc.drawImage(original, 0, 0, targetWidth, targetHeight);

            WritableImage scaled = new WritableImage((int)targetWidth, (int)targetHeight);
            SnapshotParameters params = new SnapshotParameters();
            params.setFill(Color.TRANSPARENT);
            tempCanvas.snapshot(params, scaled);

            System.out.println("✅ Background pre-scaled: " + (int)targetWidth + "x" + (int)targetHeight);
            return scaled;

        } catch (Exception e) {
            System.err.println("⚠️ Không thể pre-scale background");
            e.printStackTrace();
            return null;
        }
    }

    private Image loadImage(String path) {
        try {
            InputStream stream = getClass().getResourceAsStream(path);
            if (stream == null) {
                System.err.println("⚠️ Không tìm thấy ảnh: " + path);
                return null;
            }

            // ✅ Load với smooth=false để tiết kiệm bộ nhớ
            Image img = new Image(stream, 0, 0, true, false);
            stream.close(); // ✅ Đóng stream sau khi load

            System.out.println("✅ Đã tải ảnh: " + path);
            return img;
        } catch (Exception e) {
            System.err.println("❌ Lỗi khi tải ảnh: " + path);
            e.printStackTrace();
            return null;
        }
    }

    /**
     * ⚠️ CRITICAL: CHỈ load background khi thay đổi, KHÔNG mỗi frame!
     */
    public void render(GameManager gameManager) {
        GameState state = gameManager.getCurrentState();

        // ⚠️ CRITICAL: Xác định background cần dùng
        String desiredBgPath = null;
        Level currentLevel = gameManager.getCurrentLevel();

        if (currentLevel != null && state == GameState.PLAYING) {
            desiredBgPath = currentLevel.getBackgroundImage();
        }

        // ⚠️ CRITICAL: CHỈ load nếu background thay đổi
        if (!isSameBackground(desiredBgPath, currentBgPath)) {
            loadLevelBackground(desiredBgPath);
        }

        drawBackground();

        switch (state) {
            case MENU -> renderMenu();
            case PLAYING, PAUSED -> {
                renderGame(gameManager);
                if (state == GameState.PAUSED) renderPauseOverlay();
            }
            case GAME_OVER -> {
                renderGame(gameManager);
                renderGameOver(gameManager.getScoreManager());
            }
            case LEVEL_COMPLETE -> {
                renderGame(gameManager);
                renderLevelComplete(gameManager);
            }
        }
    }

    /**
     * ⚠️ So sánh background path an toàn
     */
    private boolean isSameBackground(String path1, String path2) {
        if (path1 == null && path2 == null) return true;
        if (path1 == null || path2 == null) return false;
        return path1.equals(path2);
    }

    /**
     * ⚠️ CRITICAL: Load background CHỈ khi cần thiết
     */
    private void loadLevelBackground(String backgroundPath) {
        // Null hoặc empty = dùng default
        if (backgroundPath == null || backgroundPath.trim().isEmpty()) {
            currentBgPath = null;
            currentScaledBg = defaultScaledBackground;
            return;
        }

        // ⚠️ CRITICAL: Check cache TRƯỚC khi load
        if (cachedScaledLevelBgs.containsKey(backgroundPath)) {
            currentBgPath = backgroundPath;
            currentScaledBg = cachedScaledLevelBgs.get(backgroundPath);
            System.out.println("✅ Using cached background: " + backgroundPath);
            return;
        }

        // Load mới và cache (CHỈ khi chưa có trong cache)
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
                System.out.println("✅ Loaded and cached new background: " + backgroundPath);
            } else {
                System.err.println("⚠️ Failed to load background, using default");
                currentBgPath = null;
                currentScaledBg = defaultScaledBackground;
            }

        } catch (Exception e) {
            System.err.println("❌ Error loading level background: " + backgroundPath);
            e.printStackTrace();
            currentBgPath = null;
            currentScaledBg = defaultScaledBackground;
        }
    }

    /**
     * Vẽ background từ cache (cực nhanh, không tốn bộ nhớ)
     */
    private void drawBackground() {
        if (currentScaledBg != null) {
            gc.drawImage(currentScaledBg, 0, 0);
        } else if (currentBgPath != null) {
            // Fallback: vẽ trực tiếp nếu không có scaled version
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

        renderUI(gameManager);
    }

    private void renderUI(GameManager gameManager) {
        ScoreManager scoreManager = gameManager.getScoreManager();
        Level currentLevel = gameManager.getCurrentLevel();

        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Arial", Constants.UI_FONT_SIZE));

        gc.setTextAlign(TextAlignment.LEFT);
        gc.fillText("Score: " + scoreManager.getScore(), 10, 25);

        gc.setTextAlign(TextAlignment.CENTER);
        if (currentLevel != null) {
            gc.fillText(currentLevel.getLevelName() + " (" +
                            currentLevel.getLevelNumber() + "/" +
                            gameManager.getLevelManager().getTotalLevels() + ")",
                    Constants.WINDOW_WIDTH / 2.0, 25);
        }

        gc.setTextAlign(TextAlignment.RIGHT);
        gc.fillText("Lives: " + scoreManager.getLives(),
                Constants.WINDOW_WIDTH - 10, 25);
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

    /**
     * ✅ Phương thức dọn dẹp cache khi cần (gọi khi thoát game)
     */
    public static void clearCache() {
        cachedBrickImages = null;
        cachedDefaultBackground = null;
        cachedDefaultScaledBg = null;
        cachedLevelBackgrounds.clear();
        cachedScaledLevelBgs.clear();
        System.out.println("🧹 Renderer cache cleared");
    }
}