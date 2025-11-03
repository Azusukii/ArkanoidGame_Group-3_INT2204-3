package Arkanoid.model;

import Arkanoid.util.Constants;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

/**
 * Falling power-up pickup with a short label per type.
 * Applies effects when collected by the paddle and tracks remaining duration for timed effects.
 */
public class PowerUps extends MoveableObject {
    private PowerUpType type;
    private boolean collected;
    private double timeleft;
    // Cache images per power-up type
    private static java.util.Map<PowerUpType, javafx.scene.image.Image> IMAGE_CACHE;
    public PowerUps(double x, double y, PowerUpType type) {
        super(x, y, Constants.POWERUP_SIZE, Constants.POWERUP_SIZE, Constants.POWERUP_FALL_SPEED);
        this.type = type;
        this.velocityY = speed;
        this.collected = false;
        this.timeleft = Constants.POWERUP_DURATION;
    }

    /** Updates falling motion at default tick rate. */
    @Override
    public void update() {
        move();
    }

    /** Updates falling motion and counts down remaining time when collected. */
    @Override
    public void update(double deltaTime) {
        move(deltaTime);
        if (collected && timeleft > 0) {
            timeleft -= deltaTime;
        }
    }

    @Override
    public void render(GraphicsContext gc) {
        if (collected) return;

        // Try draw sprite image; fallback to colored circle + letter
        javafx.scene.image.Image img = getTypeImage(type);
        if (img != null) {
            gc.drawImage(img, x, y, width, height);
        } else {
            // Set color based on type
            gc.setFill(getColor());
            gc.fillOval(x, y, width, height);

            // Add border
            gc.setStroke(Color.WHITE);
            gc.setLineWidth(2);
            gc.strokeOval(x, y, width, height);

            // Draw icon/letter
            gc.setFill(Color.WHITE);
            gc.setFont(Font.font("Arial", 12));
            String letter = getIconLetter();
            gc.fillText(letter, x + width / 2 - 4, y + height / 2 + 4);
        }
    }

    private javafx.scene.image.Image getTypeImage(PowerUpType t) {
        if (t == null) return null;
        if (IMAGE_CACHE == null) IMAGE_CACHE = new java.util.EnumMap<>(PowerUpType.class);
        javafx.scene.image.Image cached = IMAGE_CACHE.get(t);
        if (cached != null) return cached;

        String fileName = switch (t) {
            case BULLET -> "BULLET.png";
            case MULTI_BALL -> "MULTI_BALL.png";
            case EXPAND_PADDLE -> "EXPAND_PADDLE.png";
            case SHRINK_PADDLE -> "SHRINK_PADDLE.png";
            case SPEED_UP_BALL -> "SPEED_UP_BALL.png";
        };
        String path = "/images/powerup/" + fileName;
        try {
            var stream = PowerUps.class.getResourceAsStream(path);
            if (stream == null) return null;
            // Let drawImage scale to our width/height; load at natural size
            javafx.scene.image.Image img = new javafx.scene.image.Image(stream);
            stream.close();
            IMAGE_CACHE.put(t, img);
            return img;
        } catch (Exception ignored) {
            return null;
        }
    }

    private Color getColor() {
        switch (type) {
            case EXPAND_PADDLE:
                return Constants.POWERUP_EXPAND_COLOR;
            case SHRINK_PADDLE:
                return Constants.POWERUP_SHRINK_COLOR;
            case SPEED_UP_BALL:
                return Constants.POWERUP_SPEED_UP_COLOR;
            case MULTI_BALL:
                return Constants.POWERUP_MULTI_BALL_COLOR;
            case BULLET:
                return Color.PURPLE;
            default:
                return Color.WHITE;
        }
    }

    private String getIconLetter() {
        switch (type) {
            case EXPAND_PADDLE:
                return "E";
            case SHRINK_PADDLE:
                return "S";
            case SPEED_UP_BALL:
                return "+";
            case MULTI_BALL:
                return "M";
            case BULLET:
                return "B";
            default:
                return "?";
        }
    }

    /** @return true if the power-up has fallen below the bottom of the screen. */
    public boolean isOutOfBounds() {
        return y > Constants.WINDOW_HEIGHT;
    }

    /** Marks this power-up as collected by the paddle. */
    public void collect() {
        collected = true;
    }

    /** @return true if the power-up has been collected. */
    public boolean isCollected() {
        return collected;
    }

    /** @return true if a collected, timed power-up has fully expired. */
    public boolean isExpired() {
        return isCollected() && timeleft <= 0;
    }

    /** @return the type of this power-up. */
    public PowerUpType getType() {
        return type;
    }
}