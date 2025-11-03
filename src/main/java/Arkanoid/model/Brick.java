package Arkanoid.model;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * A single brick in the playfield with type-specific behavior.
 * Hard bricks require multiple hits; unbreakable bricks cannot be destroyed.
 */
public class Brick extends GameObject {
    private BrickType type;
    private int hitsRemaining;
    private Color color;
    private boolean destroyed;
    private boolean damaged; // show broken sprite for HARD after first hit
    
    public Brick(double x, double y, double width, double height, BrickType type, Color color) {
        super(x, y, width, height);
        this.type = type;
        this.color = color;
        this.hitsRemaining = type.getHits();
        this.destroyed = false;
    this.damaged = false;
    }
    
    /** Bricks are static; no-op update. */
    @Override
    public void update() {
        // Bricks don't move
    }
    
    /** Bricks are static; no-op update. */
    @Override
    public void update(double deltaTime) {
        // Bricks don't move
    }
    
    @Override
    public void render(GraphicsContext gc) {
        if (destroyed) return;

        // Default fallback if renderer image not used
        gc.setFill(color);
        gc.fillRoundRect(x + 1, y + 1, width - 2, height - 2, 5, 5);

        // Highlight
        gc.setFill(Color.rgb(255, 255, 255, 0.3));
        gc.fillRoundRect(x + 1, y + 1, width - 2, height / 2, 5, 5);

        // Border
        gc.setStroke(Color.rgb(0, 0, 0, 0.5));
        gc.setLineWidth(2);
        gc.strokeRoundRect(x + 1, y + 1, width - 2, height - 2, 5, 5);

        // Unbreakable indicator
        if (type == BrickType.UNBREAKABLE) {
            gc.setStroke(Color.YELLOW);
            gc.setLineWidth(2);
            gc.strokeLine(x + 5, y + height / 2, x + width - 5, y + height / 2);
        }
    }
    
    /**
     * Applies a hit to this brick.
     * @return true if the brick was destroyed by this hit
     */
    public boolean hit() {
        if (type == BrickType.UNBREAKABLE) {
            return false;
        }
        
        hitsRemaining--;
        
        if (hitsRemaining <= 0) {
            destroyed = true;
            return true;
        }
        
        // Mark damaged after first hit for HARD so renderer can show broken sprite
        if (type == BrickType.HARD) {
            damaged = true;
            color = color.darker();
        }
        
        return false;
    }
    
    /** @return true if this brick has been destroyed. */
    public boolean isDestroyed() {
        return destroyed;
    }

    /** @return true if the brick is damaged but not destroyed (used for HARD). */
    public boolean isDamaged() { return damaged; }
    
    /** @return the brick type controlling durability and score. */
    public BrickType getType() {
        return type;
    }
    
    /** @return score awarded when this brick is destroyed. */
    public int getScore() {
        switch (type) {
            case HARD:
                return 20;
            case UNBREAKABLE:
                return 0;
            default:
                return 10;
        }
    }

    // Expose position helpers for moving bricks
    public double getX() { return x; }
    public double getY() { return y; }
    public void setX(double x) { this.x = x; }
    public void setY(double y) { this.y = y; }
}