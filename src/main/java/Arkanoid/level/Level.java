package Arkanoid.level;

import Arkanoid.model.Brick;
import Arkanoid.model.BrickType;
import Arkanoid.model.MovingBrick;
import Arkanoid.util.Constants;
import javafx.scene.paint.Color;

/**
 * Concrete Level implementation built from {@link LevelData}.
 * Converts {@code LevelData.BrickData} instances into concrete bricks using layout constants
 * and exposes derived per-level settings (ball speed, initial lives, background).
 */
public class Level extends AbstractLevel {
    private LevelData levelData;

    public Level(LevelData levelData) {
        super(levelData.getLevelNumber(), levelData.getName());
        this.levelData = levelData;
    }

    @Override
    public void initialize() {
        bricks.clear();

        if (levelData.getBricks() != null) {
            // Load from LevelData
            for (LevelData.BrickData brickData : levelData.getBricks()) {
                Brick brick = createBrickFromData(brickData);
                bricks.add(brick);
            }
        } else {
            // Fallback: create default layout when no data exists
            createDefaultLevel();
        }

        // Backup initial state for later resets
        backupInitialState();
    }

    @Override
    public void reset() {
        // Re-initialize level from levelData
        initialize();
    }

    /** Creates a brick from BrickData. */
    private Brick createBrickFromData(LevelData.BrickData data) {
        // Position computed from row/col
        double x = Constants.BRICK_OFFSET_X + data.getCol() * (Constants.BRICK_WIDTH + Constants.BRICK_PADDING);
        double y = Constants.BRICK_OFFSET_Y + data.getRow() * (Constants.BRICK_HEIGHT + Constants.BRICK_PADDING);

        // Parse type
        BrickType type = parseBrickType(data.getType());

        // Parse color
        Color color = parseColor(data.getColor());

        // Moving brick support
        boolean moving = Boolean.TRUE.equals(data.getMoving());
        if (moving) {
            String dir = data.getDirection() == null ? "HORIZONTAL" : data.getDirection().toUpperCase();
            double speed = data.getSpeed() != null ? data.getSpeed() : 40.0; // px/s default
            double range = data.getRange() != null ? data.getRange() : 60.0; // px default

            MovingBrick.Axis axis = "VERTICAL".equals(dir) ? MovingBrick.Axis.VERTICAL : MovingBrick.Axis.HORIZONTAL;
            return new MovingBrick(x, y, Constants.BRICK_WIDTH, Constants.BRICK_HEIGHT, type, color, axis, speed, range);
        }

        return new Brick(x, y, Constants.BRICK_WIDTH, Constants.BRICK_HEIGHT, type, color);
    }

    /** Parses string to BrickType enum. */
    private BrickType parseBrickType(String typeStr) {
        if (typeStr == null) return BrickType.NORMAL;

        switch (typeStr.toUpperCase()) {
            case "HARD":
                return BrickType.HARD;
            case "UNBREAKABLE":
                return BrickType.UNBREAKABLE;
            default:
                return BrickType.NORMAL;
        }
    }

    /** Parses a hex color string to Color. */
    private Color parseColor(String colorStr) {
        if (colorStr == null || colorStr.isEmpty()) {
            return Constants.BRICK_COLORS[0]; // Default red
        }

        try {
            return Color.web(colorStr);
        } catch (Exception e) {
            return Constants.BRICK_COLORS[0];
        }
    }

    /** Creates a default full grid when no data exists. */
    private void createDefaultLevel() {
        for (int row = 0; row < Constants.BRICK_ROWS; row++) {
            for (int col = 0; col < Constants.BRICK_COLS; col++) {
                double x = Constants.BRICK_OFFSET_X + col * (Constants.BRICK_WIDTH + Constants.BRICK_PADDING);
                double y = Constants.BRICK_OFFSET_Y + row * (Constants.BRICK_HEIGHT + Constants.BRICK_PADDING);

                Color color = Constants.BRICK_COLORS[row % Constants.BRICK_COLORS.length];
                BrickType type = BrickType.NORMAL;

                bricks.add(new Brick(x, y, Constants.BRICK_WIDTH, Constants.BRICK_HEIGHT, type, color));
            }
        }
    }

    /** Returns the configured ball speed for this level. */
    public double getBallSpeed() {
        return levelData.getBallSpeed();
    }

    /** Returns the initial lives for this level. */
    public int getInitialLives() {
        return levelData.getLives();
    }

    /**
     * Returns the background image path for this level.
     * @return path to background image (e.g., "/images/level/space.png") or null for default
     */
    public String getBackgroundImage() {
        return levelData.getBackgroundImage();
    }
}