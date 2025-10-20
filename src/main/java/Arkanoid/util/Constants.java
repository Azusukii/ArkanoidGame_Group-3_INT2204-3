package Arkanoid.util;

import javafx.scene.paint.Color;

public class Constants {
    // Window dimensions
    public static final int WINDOW_WIDTH = 800;
    public static final int WINDOW_HEIGHT = 600;

    // Paddle
    public static final int PADDLE_WIDTH = 200;
    public static final int PADDLE_HEIGHT = 15;
    public static final int PADDLE_Y = 550;
    public static final double PADDLE_SPEED = 6.5;
    public static final Color PADDLE_COLOR = Color.DODGERBLUE;

    // Ball
    public static final int BALL_RADIUS = 8;
    public static final double BALL_SPEED = 4.5;
    public static final Color BALL_COLOR = Color.WHITE;

    // Bricks
    public static final int BRICK_WIDTH = 75;
    public static final int BRICK_HEIGHT = 20;
    public static final int BRICK_ROWS = 8;
    public static final int BRICK_COLS = 10;
    public static final int BRICK_PADDING = 5;
    public static final int BRICK_OFFSET_X = 10;
    public static final int BRICK_OFFSET_Y = 50;

    // Brick colors by row
    public static final Color[] BRICK_COLORS = {
            Color.RED,
            Color.ORANGE,
            Color.YELLOW,
            Color.GREEN,
            Color.CYAN,
            Color.BLUE,
            Color.PURPLE,
            Color.PINK
    };

    // Brick types
    public static final int NORMAL_BRICK = 1;
    public static final int HARD_BRICK = 2;
    public static final int UNBREAKABLE_BRICK = 3;

    // Power-ups
    public static final int POWERUP_SIZE = 20;
    public static final double POWERUP_FALL_SPEED = 0.5;
    public static final Color POWERUP_EXPAND_COLOR = Color.GREEN;
    public static final Color POWERUP_SHRINK_COLOR = Color.RED;
    public static final Color POWERUP_SPEED_UP_COLOR = Color.YELLOW;
    public static final Color POWERUP_SPEED_DOWN_COLOR = Color.CYAN;
    public static final Color POWERUP_EXTRA_LIFE_COLOR = Color.PINK;
    public static final Color POWERUP_MULTI_BALL_COLOR = Color.ORANGE;

    // Game settings
    public static final int INITIAL_LIVES = 3;
    public static final int FPS = 60;
    public static final long FRAME_TIME = 1000000000 / FPS;

    // Scoring
    public static final int SCORE_PER_BRICK = 10;
    public static final int SCORE_MULTIPLIER_HARD = 2;
    public static final int SCORE_POWERUP = 50;

    // UI
    public static final int UI_FONT_SIZE = 20;
    public static final Color UI_TEXT_COLOR = Color.WHITE;
    public static final Color BACKGROUND_COLOR = Color.BLACK;
}