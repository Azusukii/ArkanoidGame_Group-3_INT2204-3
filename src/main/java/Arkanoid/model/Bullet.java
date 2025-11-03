package Arkanoid.model;

import Arkanoid.util.Constants;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;

/** Simple upward-moving projectile for BULLET power-up. */
public class Bullet extends MoveableObject {
    private static final double BULLET_SPEED = 9.0; // slower travel speed
    private static final double WIDTH = 24.0; // x2 size
    private static final double HEIGHT = 48.0; // x2 size

    private static Image ROCKET_IMG;

    public Bullet(double x, double y) {
        super(x, y, WIDTH, HEIGHT, BULLET_SPEED);
        this.velocityX = 0;
        this.velocityY = -BULLET_SPEED;

        // Lazy-load rocket image once
        if (ROCKET_IMG == null) {
            try {
                var stream = Bullet.class.getResourceAsStream("/images/powerup/rocket.png");
                if (stream != null) {
                    ROCKET_IMG = new Image(stream, WIDTH, HEIGHT, false, true);
                    stream.close();
                }
            } catch (Exception ignored) {}
        }
    }

    @Override
    public void update() { update(1.0 / 60.0); }

    @Override
    public void update(double deltaTime) {
        move(deltaTime);
    }

    @Override
    public void render(GraphicsContext gc) {
        if (ROCKET_IMG != null) {
            gc.drawImage(ROCKET_IMG, x, y, width, height);
        } else {
            gc.setFill(Color.PURPLE);
            gc.fillRect(x, y, width, height);
        }
    }

    public boolean isOutOfBounds() {
        return y + height < 0 || y > Constants.WINDOW_HEIGHT;
    }
}


