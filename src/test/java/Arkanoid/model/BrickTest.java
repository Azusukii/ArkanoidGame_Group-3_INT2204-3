package Arkanoid.model;

import javafx.scene.paint.Color;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class BrickTest {
    Brick normal = new Brick(0, 0, 50, 20, BrickType.NORMAL, Color.RED);
    Brick hard = new Brick(0, 0, 50, 20, BrickType.HARD, Color.BLUE);
    Brick unbreakable = new Brick(0, 0, 50, 20, BrickType.UNBREAKABLE, Color.GRAY);

    /**
     * Test the hit logic of bricks.
     */
    @Test
    public void testHit() {
        // Test code for Brick hit logic
        assertFalse(unbreakable.hit());
        assertTrue(normal.hit());
    // NORMAL was already destroyed; subsequent hits remain true (destroyed)
    assertTrue(normal.isDestroyed());
    // HARD now breaks in 1 hit
    assertTrue(hard.hit());
    assertTrue(hard.isDestroyed());
    }

    /**
     * Test the score values of different brick types.
     */
    @Test
    public void testScore() {
        assertEquals(20, hard.getScore());
        assertEquals(10, normal.getScore());
        assertEquals(0, unbreakable.getScore());
    }
}

