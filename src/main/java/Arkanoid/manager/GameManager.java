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
 * Coordinates game state, entities, level progression, and timed events.
 * Ensures proper thread management and cleanup between states.
 */
public class GameManager {
    private GameState currentState;
    private Paddle paddle;
    private List<Ball> balls;
    private List<Brick> bricks;
    private List<PowerUps> powerUps;
    private List<Bullet> bullets;
    private CollisionManager collisionManager;
    private ScoreManager scoreManager;
    private Random random;

    // Level Management
    private LevelManager levelManager;
    private Level currentLevel;

    // PowerUp timing
    private final Map<PowerUpType, Double> activePowerUps;
    // Bullet spawning timing
    private double bulletSpawnAccumulator = 0;
    private static final double BULLET_SPAWN_INTERVAL = 0.6; // seconds (slower fire rate)

    // Thread scheduler (single instance, reused)
    private final ScheduledExecutorService scheduler;

    // Track scheduled task for later cancellation
    private ScheduledFuture<?> stageStartTask;

    public GameManager() {
        this.currentState = GameState.MENU;
        this.collisionManager = new CollisionManager();
        this.scoreManager = new ScoreManager();
        this.random = new Random();
        this.activePowerUps = new HashMap<>();

        // Initialize scheduler once
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "GameManager-Scheduler");
            t.setDaemon(true);
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
        bullets = new ArrayList<>();

        loadCurrentLevel();
    }

    private void loadCurrentLevel() {
        currentLevel = levelManager.getCurrentLevel();

        if (currentLevel != null) {
            bricks.clear();
            bricks.addAll(currentLevel.getBricks());

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

        } else {
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

        // Cập nhật paddle theo thời gian (điều khiển trái/phải)
        paddle.update(deltaTime);

        for (Brick brick : bricks) {
            brick.update(deltaTime);
        }

        // Logic xử lý va chạm vật lý giữa bóng, gạch, tường, paddle
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
                        // Ensure no delayed background start runs after GAME OVER
                        cancelStageStartTask();
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

        // Rơi và nhặt Power-up (va chạm với paddle)
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

        // Thời gian hiệu lực của Power-up (tự hủy khi hết hạn)
        updateActivePowerUps();

        // Update bullets and handle collisions
        // Đạn bắn ra khi Power-up BULLET đang hoạt động
        updateBullets(deltaTime);

        if (isLevelComplete()) {
            currentState = GameState.LEVEL_COMPLETE;
        }
    }

    // Kiểm tra và xử lý va chạm giữa bóng với paddle và gạch
    private void checkCollisions(Ball ball) {
        collisionManager.checkBallPaddleCollision(ball, paddle);

        Brick hitBrick = collisionManager.checkBallBrickCollision(ball, bricks);
        if (hitBrick != null) {
            boolean destroyed = hitBrick.hit();
            if (destroyed) {
                scoreManager.addScore(hitBrick.getScore());
                SoundManager.getInstance().playSound("effect_brick");
                SoundManager.getInstance().playSound("effect_score");

                if (random.nextInt(100) < 40) {
                    spawnPowerUp(hitBrick.getCenterX(), hitBrick.getCenterY());
                }

                bricks.remove(hitBrick);
            }
        }
    }

    // Tạo Power-up với tỉ lệ xuất hiện có trọng số
    private void spawnPowerUp(double x, double y) {
        int roll = random.nextInt(100); // 0..99
        PowerUpType type;
        if (roll < 30) {
            type = PowerUpType.BULLET; // 0-29 (30%)
        } else if (roll < 60) {
            type = PowerUpType.MULTI_BALL; // 30-59 (30%)
        } else if (roll < 75) {
            type = PowerUpType.EXPAND_PADDLE; // 60-74 (15%)
        } else if (roll < 90) {
            type = PowerUpType.SHRINK_PADDLE; // 75-89 (15%)
        } else {
            type = PowerUpType.SPEED_UP_BALL; // 90-99 (10%)
        }
        powerUps.add(new PowerUps(x, y, type));
    }

    // Kích hoạt hiệu ứng Power-up và đặt thời gian hết hạn
    private void applyPowerUp(PowerUpType type) {
        double now = System.currentTimeMillis();

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

            case MULTI_BALL:
                createMultiBallForAll();
                break;

            case BULLET:
                activePowerUps.put(type, now + Constants.POWERUP_DURATION);
                break;
        }
    }

    /**
     * Spawn two additional balls at the current ball's position, spreading around its direction.
     * The original ball remains; total becomes three from the same point.
     */
    /**
     * Spawns two additional balls for each existing ball, fanning out by angle.
     */
    private void createMultiBallForAll() {
        if (balls.isEmpty()) return;
        List<Ball> snapshot = new ArrayList<>(balls);
        for (Ball ref : snapshot) {
            double spawnX = ref.getX();
            double spawnY = ref.getY();
            double base = (currentLevel != null) ? currentLevel.getBallSpeed() : ref.getBaseSpeed();

            double centerAngle = ref.isStuck() ? Math.toRadians(-90) : Math.atan2(ref.getVelocityY(), ref.getVelocityX());
            double[] offsets = new double[] { Math.toRadians(20), Math.toRadians(-20) };
            for (double off : offsets) {
                Ball nb = new Ball(paddle);
                nb.setBaseSpeed(base);
                nb.setX(spawnX);
                nb.setY(spawnY);
                nb.setSmoothX(spawnX);
                nb.setSmoothY(spawnY);
                nb.setStuck(false);
                double ang = centerAngle + off;
                nb.setVelocityX(base * Math.cos(ang));
                nb.setVelocityY(base * Math.sin(ang));
                balls.add(nb);
            }
        }
    }

    // Duyệt và vô hiệu hóa Power-up đã hết hạn
    private void updateActivePowerUps() {
        double now = System.currentTimeMillis();
        Iterator<Map.Entry<PowerUpType, Double>> iterator = activePowerUps.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<PowerUpType, Double> entry = iterator.next();
            if (now > entry.getValue()) {
                deactivatePowerUp(entry.getKey());
                iterator.remove();
            }
        }
    }

    // Kiểm tra trạng thái hoạt động của Power-up BULLET
    private boolean isBulletActive() {
        Double until = activePowerUps.get(PowerUpType.BULLET);
        return until != null && System.currentTimeMillis() <= until;
    }

    // Cập nhật bắn đạn theo chu kỳ khi BULLET đang hoạt động và xử lý va chạm với gạch
    private void updateBullets(double deltaTime) {
        // Spawn bullets at interval while active
        if (isBulletActive()) {
            bulletSpawnAccumulator += deltaTime;
            while (bulletSpawnAccumulator >= BULLET_SPAWN_INTERVAL) {
                bulletSpawnAccumulator -= BULLET_SPAWN_INTERVAL;
                double bx = paddle.getCenterX() - 2; // center 4px bullet
                double by = paddle.getY() - 10;
                bullets.add(new Bullet(bx, by));
            }
        } else {
            bulletSpawnAccumulator = 0;
        }

        // Move bullets and check brick impacts
        Iterator<Bullet> it = bullets.iterator();
        while (it.hasNext()) {
            Bullet bullet = it.next();
            bullet.update(deltaTime);
            if (bullet.isOutOfBounds()) {
                it.remove();
                continue;
            }

            Brick hit = null;
            for (Brick brick : bricks) {
                if (!brick.isDestroyed() && bullet.intersects(brick)) { hit = brick; break; }
            }
            if (hit != null) {
                if (hit.getType() == BrickType.UNBREAKABLE) {
                    it.remove();
                    continue;
                }
                boolean destroyed = hit.hit();
                if (destroyed) {
                    scoreManager.addScore(hit.getScore());
                    SoundManager.getInstance().playSound("effect_brick");
                    SoundManager.getInstance().playSound("effect_score");
                    bricks.remove(hit);
                }
                it.remove();
            }
        }
    }

    // Hoàn tác các thay đổi tạm thời khi Power-up kết thúc
    private void deactivatePowerUp(PowerUpType type) {
        switch (type) {
            case EXPAND_PADDLE:
            case SHRINK_PADDLE:
                paddle.resetSize();
                break;

            case SPEED_UP_BALL:
                balls.forEach(Ball::resetSpeed);
                break;

            case BULLET:
                // Stop spawning; existing bullets will clear naturally
                break;
            default:
                break;
        }
    }

    // Kiểm tra điều kiện hoàn thành level (không còn gạch phá được)
    private boolean isLevelComplete() {
        if (currentLevel != null) return currentLevel.isCompleted();

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
        // Lên lịch chuyển nhạc: dừng intro, bật background/ambient sau vài giây
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
            cleanup();

            currentLevel = levelManager.getCurrentLevel();
            resetLevel();
            currentState = GameState.PLAYING;
        } else {
            // level not found or locked
        }
    }

    public void showLevelSelection() {
        cleanup();

        currentState = GameState.MENU;
        SoundManager sm = SoundManager.getInstance();
        sm.stopAll();
        sm.playSound("music_title");
    }

    // Hẹn giờ chuyển đổi nhạc nền sau khi bắt đầu màn chơi
    private void scheduleStageStartStop() {
        cancelStageStartTask();

        stageStartTask = scheduler.schedule(() -> {
            SoundManager sm = SoundManager.getInstance();
            sm.stopSound("music_stage_start");
            sm.startBackgroundAlternating();
            sm.playSound("ambient_bg");
            stageStartTask = null;
        }, 5, TimeUnit.SECONDS);
    }

    // Hủy tác vụ hẹn giờ nếu còn đang chờ
    private void cancelStageStartTask() {
        if (stageStartTask != null && !stageStartTask.isDone()) stageStartTask.cancel(false);
        stageStartTask = null;
    }

    // Dọn dẹp tài nguyên khi thoát trận hoặc về menu
    public void cleanup() {
        cancelStageStartTask();
        if (balls != null) {
            balls.clear();
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
        if (bullets != null) {
            bullets.clear();
        }
        try {
            SoundManager.getInstance().stopAll();
        } catch (Exception e) {
            System.err.println("Error stopping sounds: " + e.getMessage());
        }
    }

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
    /**
     * Sets the current game state, applying safety cleanup and audio handling.
     */
    // Thay đổi trạng thái game; đảm bảo dọn dẹp và xử lý âm thanh an toàn
    public void setCurrentState(GameState gameState) {
        if (gameState == GameState.MENU && currentState != GameState.MENU) cleanup();
        if (gameState == GameState.GAME_OVER) {
            cancelStageStartTask();
            try { SoundManager.getInstance().stopBackgroundAlternating(); } catch (Exception ignored) {}
        }
        this.currentState = gameState;
    }
}