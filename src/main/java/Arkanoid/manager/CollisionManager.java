package Arkanoid.manager;

import Arkanoid.model.Ball;
import Arkanoid.model.Brick;
import Arkanoid.model.Paddle;
import Arkanoid.model.PowerUps;

import java.util.List;


/**
 * Detects and resolves collisions between the ball, paddle and bricks.
 * Updates ball directions and applies brick hit logic.
 */
public class CollisionManager {
    private static final double EPSILON = 0.1; // small offset to avoid re-colliding next frame

    // Xử lý va chạm bóng - paddle: nảy lên, chỉnh góc theo vị trí va chạm, đặt bóng ngay trên paddle
    public void checkBallPaddleCollision(Ball ball, Paddle paddle) {
        if (ball.intersects(paddle) && ball.getVelocityY() > 0) {
            ball.reverseY();

            // Calculate hit position on paddle (-1 to 1)
            double hitPosition = (ball.getCenterX() - paddle.getCenterX()) / (paddle.getWidth() / 2);
            hitPosition = Math.max(-1, Math.min(1, hitPosition));

            ball.adjustAngle(hitPosition);

            // Move ball above paddle to prevent sticking
            double newY = paddle.getY() - ball.getHeight() - EPSILON;
            ball.setY(newY);
            // Keep smoothed position in sync if available
            ball.setSmoothY(newY);
            Arkanoid.audio.SoundManager.getInstance().playSound("effect_paddle");
        }
    }

    // Xử lý va chạm bóng - gạch bằng AABB; trả về gạch trúng để áp dụng điểm/sát thương
    public Brick checkBallBrickCollision(Ball ball, List<Brick> bricks) {
        for (Brick brick : bricks) {
            if (!brick.isDestroyed() && ball.intersects(brick)) {
                // Compute centers and overlaps
                double dx = ball.getCenterX() - brick.getCenterX();
                double dy = ball.getCenterY() - brick.getCenterY();

                double halfW = (ball.getWidth() / 2.0) + (brick.getWidth() / 2.0);
                double halfH = (ball.getHeight() / 2.0) + (brick.getHeight() / 2.0);

                double overlapX = halfW - Math.abs(dx);
                double overlapY = halfH - Math.abs(dy);

                if (overlapX < overlapY) {
                    // Resolve horizontally (left/right)
                    if (dx > 0) {
                        // Ball is to the right of brick -> push to the right side
                        double newX = brick.getX() + brick.getWidth() + EPSILON;
                        ball.setX(newX);
                        ball.setSmoothX(newX);
                    } else {
                        // Ball is to the left of brick -> push to the left side
                        double newX = brick.getX() - ball.getWidth() - EPSILON;
                        ball.setX(newX);
                        ball.setSmoothX(newX);
                    }
                    ball.reverseX();
                } else {
                    // Resolve vertically (top/bottom)
                    if (dy > 0) {
                        // Ball is below brick -> push below
                        double newY = brick.getY() + brick.getHeight() + EPSILON;
                        ball.setY(newY);
                        ball.setSmoothY(newY);
                    } else {
                        // Ball is above brick -> push above
                        double newY = brick.getY() - ball.getHeight() - EPSILON;
                        ball.setY(newY);
                        ball.setSmoothY(newY);
                    }
                    ball.reverseY();
                }

                return brick;
            }
        }
        return null;
    }

    // Kiểm tra paddle có nhặt Power-up nào trong danh sách không
    public PowerUps checkPaddlePowerUpCollision(Paddle paddle, List<PowerUps> powerUps) {
        for (PowerUps powerUp : powerUps) {
            if (!powerUp.isCollected() && paddle.intersects(powerUp)) {
                powerUp.collect();
                return powerUp;
            }
        }
        return null;
    }
}