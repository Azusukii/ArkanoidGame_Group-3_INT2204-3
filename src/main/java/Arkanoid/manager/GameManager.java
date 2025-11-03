package Arkanoid.manager;

import Arkanoid.level.Level;
import Arkanoid.level.LevelManager;
import Arkanoid.model.*;
import Arkanoid.util.Constants;
import Arkanoid.audio.SoundManager;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Central coordinator for game state, entities and level progression.
 * FIXED: Proper thread management, no memory leaks, cancellable tasks.
 */
public class GameManager {
    private GameState currentState;
    private Paddle paddle;
    private List<Ball> balls;
    private List<Brick> bricks;
    private List<PowerUps> powerUps;
    private CollisionManager collisionManager;
    private ScoreManager scoreManager;
    private Random random;

    // Level Management
    private LevelManager levelManager;
    private Level currentLevel;

    // PowerUp timing
    private final Map<PowerUpType, Double> activePowerUps;

    // ✅ Thread scheduler (single instance, reused)
    private final ScheduledExecutorService scheduler;

    // ✅ Track scheduled task để có thể cancel
    private ScheduledFuture<?> stageStartTask;

    public GameManager() {
        this.currentState = GameState.MENU;
        this.collisionManager = new CollisionManager();
        this.scoreManager = new ScoreManager();
        this.random = new Random();
        this.activePowerUps = new HashMap<>();

        // ✅ Initialize scheduler once
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "GameManager-Scheduler");
            t.setDaemon(true); // Daemon thread tự động dừng khi app tắt
            return t;
        });

        // Initialize Level Manager
        this.levelManager = new LevelManager();
        int detected = Arkanoid.level.LevelLoader.countAvailableLevels(50);
        if (detected <= 0) detected = 3;
        this.levelManager.loadLevels(detected);

        // Load sounds
        SoundManager.getInstance().loadDefaultSounds();
        initializeGame();
    }

    private void initializeGame() {
        paddle = new Paddle();
        balls = new ArrayList<>();
        balls.add(new Ball(paddle));
        bricks = new ArrayList<>();
        powerUps = new ArrayList<>();

        loadCurrentLevel();
    }

    private void loadCurrentLevel() {
        currentLevel = levelManager.getCurrentLevel();

        if (currentLevel != null) {
            System.out.println("Loading level: " + currentLevel.getLevelName() +
                    " (Level " + currentLevel.getLevelNumber() + ")");

            bricks.clear();
            bricks.addAll(currentLevel.getBricks());

            System.out.println("Loaded " + bricks.size() + " bricks from level data");

            try {
                double levelBallSpeed = currentLevel.getBallSpeed();
                int levelLives = currentLevel.getInitialLives();

                if (levelLives > 0) {
                    scoreManager.setLives(levelLives);
                }

                for (Ball b : balls) {
                    b.setBaseSpeed(levelBallSpeed);
                }
            } catch (Exception ignored) {
            }

            // Debug
            for (int i = 0; i < Math.min(3, bricks.size()); i++) {
                Brick b = bricks.get(i);
                System.out.println("   Brick " + i + ": type=" + b.getType() +
                        ", pos=(" + b.getX() + "," + b.getY() + ")");
            }
        } else {
            System.out.println("Current level is NULL! Using legacy level generation");
            createLegacyLevel();
        }
    }

    private void createLegacyLevel() {
        bricks.clear();
        int level = scoreManager.getLevel();

        for (int row = 0; row < Constants.BRICK_ROWS; row++) {
            for (int col = 0; col < Constants.BRICK_COLS; col++) {
                double x = Constants.BRICK_OFFSET_X + col * (Constants.BRICK_WIDTH + Constants.BRICK_PADDING);
                double y = Constants.BRICK_OFFSET_Y + row * (Constants.BRICK_HEIGHT + Constants.BRICK_PADDING);

                BrickType type = determineBrickType(row, level);
                Brick brick = new Brick(
                        x, y,
                        Constants.BRICK_WIDTH,
                        Constants.BRICK_HEIGHT,
                        type,
                        Constants.BRICK_COLORS[row % Constants.BRICK_COLORS.length]
                );
                bricks.add(brick);
            }
        }
    }

    private BrickType determineBrickType(int row, int level) {
        int chance = random.nextInt(100);
        if (level > 3 && row < 2 && chance < 20)
            return BrickType.UNBREAKABLE;
        else if (level > 1 && chance < 30)
            return BrickType.HARD;
        return BrickType.NORMAL;
    }

    public void update(double deltaTime) {
        if (currentState != GameState.PLAYING) return;

        paddle.update(deltaTime);

        for (Brick brick : bricks) {
            brick.update(deltaTime);
        }

        Iterator<Ball> ballIterator = balls.iterator();
        while (ballIterator.hasNext()) {
            Ball ball = ballIterator.next();
            ball.update(deltaTime);

            if (ball.isOutOfBounds()) {
                ballIterator.remove();
                if (balls.isEmpty()) {
                    scoreManager.loseLife();
                    if (scoreManager.isGameOver()) {
                        currentState = GameState.GAME_OVER;
                        SoundManager sm = SoundManager.getInstance();
                        sm.stopAll();
                        sm.playSound("music_gameover");
                    } else {
                        resetBall();
                    }
                }
            } else {
                checkCollisions(ball);
            }
        }

        Iterator<PowerUps> powerUpIterator = powerUps.iterator();
        while (powerUpIterator.hasNext()) {
            PowerUps powerUp = powerUpIterator.next();
            powerUp.update();

            if (powerUp.isOutOfBounds()) {
                powerUpIterator.remove();
                continue;
            }

            if (!powerUp.isCollected() && paddle.intersects(powerUp)) {
                powerUp.collect();
                applyPowerUp(powerUp.getType());
                scoreManager.addScore(Constants.SCORE_POWERUP);
                powerUpIterator.remove();
            }
        }

        updateActivePowerUps();

        if (isLevelComplete()) {
            currentState = GameState.LEVEL_COMPLETE;
        }
    }

    private void checkCollisions(Ball ball) {
        collisionManager.checkBallPaddleCollision(ball, paddle);

        Brick hitBrick = collisionManager.checkBallBrickCollision(ball, bricks);
        if (hitBrick != null) {
            boolean destroyed = hitBrick.hit();
            if (destroyed) {
                scoreManager.addScore(hitBrick.getScore());
                SoundManager.getInstance().playSound("effect_brick");
                SoundManager.getInstance().playSound("effect_score");

                if (random.nextInt(100) < 15) {
                    spawnPowerUp(hitBrick.getCenterX(), hitBrick.getCenterY());
                }

                // Remove destroyed brick so it no longer renders or collides
                bricks.remove(hitBrick);
            }
        }
    }

    private void spawnPowerUp(double x, double y) {
        PowerUpType[] types = PowerUpType.values();
        PowerUpType type = types[random.nextInt(types.length)];
        powerUps.add(new PowerUps(x, y, type));
    }

    private void applyPowerUp(PowerUpType type) {
        double now = System.currentTimeMillis(); // ✅ Đổi thành double

        switch (type) {
            case EXPAND_PADDLE:
                paddle.expand();
                activePowerUps.put(type, now + Constants.POWERUP_DURATION);
                break;

            case SHRINK_PADDLE:
                paddle.shrink();
                activePowerUps.put(type, now + Constants.POWERUP_DURATION);
                break;

            case SPEED_UP_BALL:
                balls.forEach(Ball::increaseSpeed);
                activePowerUps.put(type, now + Constants.POWERUP_DURATION);
                break;

            case SPEED_DOWN_BALL:
                balls.forEach(Ball::decreaseSpeed);
                activePowerUps.put(type, now + Constants.POWERUP_DURATION);
                break;

            case EXTRA_LIFE:
                scoreManager.addLife();
                break;

            case MULTI_BALL:
                if (balls.size() < 5) {
                    Ball baseBall = balls.get(0);
                    Ball newBall = new Ball(paddle);
                    newBall.setX(baseBall.getX());
                    newBall.setY(baseBall.getY());
                    newBall.setVelocityX(baseBall.getVelocityX() * (random.nextBoolean() ? 1 : -1));
                    newBall.setVelocityY(baseBall.getVelocityY());
                    newBall.launch();
                    balls.add(newBall);
                }
                break;
        }
    }

    private void updateActivePowerUps() {
        double now = System.currentTimeMillis(); // ✅ Đổi thành double
        Iterator<Map.Entry<PowerUpType, Double>> iterator = activePowerUps.entrySet().iterator(); // ✅ Đổi Long thành Double

        while (iterator.hasNext()) {
            Map.Entry<PowerUpType, Double> entry = iterator.next(); // ✅ Đổi Long thành Double
            if (now > entry.getValue()) {
                deactivatePowerUp(entry.getKey());
                iterator.remove();
            }
        }
    }

    private void deactivatePowerUp(PowerUpType type) {
        switch (type) {
            case EXPAND_PADDLE:
            case SHRINK_PADDLE:
                paddle.resetSize();
                break;

            case SPEED_UP_BALL:
            case SPEED_DOWN_BALL:
                balls.forEach(Ball::resetSpeed);
                break;

            default:
                break;
        }
    }

    private boolean isLevelComplete() {
        if (currentLevel != null) {
            return currentLevel.isCompleted();
        }

        for (Brick brick : bricks) {
            if (!brick.isDestroyed() && brick.getType() != BrickType.UNBREAKABLE) {
                return false;
            }
        }
        return true;
    }

    public void startGame() {
        currentState = GameState.PLAYING;
        scoreManager.reset();
        levelManager.restartGame();
        initializeGame();

        SoundManager sm = SoundManager.getInstance();
        sm.stopAll();
        sm.playSound("music_stage_start");
        scheduleStageStartStop();
    }

    public void pauseGame() {
        if (currentState == GameState.PLAYING) currentState = GameState.PAUSED;
        else if (currentState == GameState.PAUSED) currentState = GameState.PLAYING;
    }

    public void nextLevel() {
        boolean hasNextLevel = levelManager.nextLevel();

        if (hasNextLevel) {
            scoreManager.nextLevel();
            currentLevel = levelManager.getCurrentLevel();
            resetLevel();
            currentState = GameState.PLAYING;

            SoundManager sm = SoundManager.getInstance();
            sm.playSound("music_stage_start");
            scheduleStageStartStop();
        } else {
            currentState = GameState.GAME_OVER;
            System.out.println("Congratulations! You completed all levels!");

            SoundManager sm = SoundManager.getInstance();
            sm.stopAll();
            sm.playSound("music_title");
        }
    }

    private void resetLevel() {
        if (levelManager != null) {
            currentLevel = levelManager.getCurrentLevel();
        }

        paddle.reset();
        paddle.setMovingLeft(false);
        paddle.setMovingRight(false);

        balls.clear();
        Ball newBall = new Ball(paddle);
        if (currentLevel != null) {
            newBall.setBaseSpeed(currentLevel.getBallSpeed());
        }
        balls.add(newBall);

        powerUps.clear();
        activePowerUps.clear();

        if (currentLevel != null) {
            currentLevel.reset();
            bricks.clear();
            bricks.addAll(currentLevel.getBricks());
            System.out.println("Reset level: " + currentLevel.getLevelName());
            scoreManager.setLives(currentLevel.getInitialLives());
        } else {
            loadCurrentLevel();
        }
    }

    private void resetBall() {
        balls.clear();
        balls.add(new Ball(paddle));
    }

    public void launchBall() {
        for (Ball ball : balls) {
            if (ball.isStuck()) ball.launch();
        }
    }

    public void selectLevel(int levelNumber) {
        if (levelManager.selectLevel(levelNumber)) {
            // ⚠️ CRITICAL: Cleanup trước khi load level mới
            cleanup();

            currentLevel = levelManager.getCurrentLevel();
            resetLevel();
            currentState = GameState.PLAYING;
            System.out.println("Selected Level " + currentLevel.getLevelNumber() + ": " + currentLevel.getLevelName());
        } else {
            System.out.println("Cannot select level " + levelNumber + " (does not exist)");
        }
    }

    public void showLevelSelection() {
        // ⚠️ CRITICAL: Cleanup TRƯỚC khi đổi state
        cleanup();

        currentState = GameState.MENU;
        SoundManager sm = SoundManager.getInstance();
        sm.stopAll();
        sm.playSound("music_title");
    }

    /**
     * ✅ Schedule với khả năng cancel task cũ
     */
    private void scheduleStageStartStop() {
        // ✅ Cancel task cũ trước khi tạo mới
        cancelStageStartTask();

        // ✅ Lưu reference để có thể cancel sau này
        stageStartTask = scheduler.schedule(() -> {
            SoundManager sm = SoundManager.getInstance();
            sm.stopSound("music_stage_start");
            sm.startBackgroundAlternating();
            sm.playSound("ambient_bg");

            // ✅ Clear reference sau khi task hoàn thành
            stageStartTask = null;
        }, 5, TimeUnit.SECONDS);
    }

    /**
     * ✅ Cancel pending stage start task
     */
    private void cancelStageStartTask() {
        if (stageStartTask != null && !stageStartTask.isDone()) {
            stageStartTask.cancel(false); // false = không interrupt nếu đang chạy
            System.out.println("🔴 Cancelled pending stage start task");
        }
        stageStartTask = null;
    }

    /**
     * ✅ Cleanup khi thoát game hoặc về menu
     */
    public void cleanup() {
        System.out.println("🧹 Starting cleanup...");

        // ⚠️ CRITICAL: Cancel scheduled tasks TRƯỚC
        cancelStageStartTask();

        // ⚠️ CRITICAL: Clear collections và null references
        if (balls != null) {
            balls.clear();
            // balls = null; // Không null vì sẽ reuse
        }
        if (bricks != null) {
            bricks.clear();
        }
        if (powerUps != null) {
            powerUps.clear();
        }
        if (activePowerUps != null) {
            activePowerUps.clear();
        }

        // ⚠️ CRITICAL: Stop ALL sounds
        try {
            SoundManager.getInstance().stopAll();
        } catch (Exception e) {
            System.err.println("Error stopping sounds: " + e.getMessage());
        }

        System.out.println("🧹 GameManager cleaned up");
    }

    /**
     * ✅ Shutdown scheduler khi app đóng (gọi từ Application.stop())
     */
    public void shutdown() {
        cancelStageStartTask();

        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(2, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }

        System.out.println("🛑 GameManager scheduler shutdown complete");
    }

    // Getters
    public GameState getCurrentState() { return currentState; }
    public Paddle getPaddle() { return paddle; }
    public List<Ball> getBalls() { return balls; }
    public List<Brick> getBricks() { return bricks; }
    public List<PowerUps> getPowerUps() { return powerUps; }
    public ScoreManager getScoreManager() { return scoreManager; }
    public LevelManager getLevelManager() { return levelManager; }
    public Level getCurrentLevel() { return currentLevel; }
    public void setCurrentState(GameState gameState) {
        // ⚠️ CRITICAL: Cleanup khi chuyển state
        if (gameState == GameState.MENU && currentState != GameState.MENU) {
            cleanup();
        }

        this.currentState = gameState;
    }
}