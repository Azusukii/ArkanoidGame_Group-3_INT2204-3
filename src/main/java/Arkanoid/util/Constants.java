package Arkanoid.util;

import javafx.scene.paint.Color;

/**
 * Central place for gameplay constants, colors, dimensions and scores.
 */
public class Constants {
    // Comparison epsilon
    public static final double EPSILON = 1e-6;

    // Window dimensions
    public static final int WINDOW_WIDTH = 860;
    public static final int WINDOW_HEIGHT = 600;

    // Paddle
    public static final int PADDLE_WIDTH = 200;
    public static final int PADDLE_HEIGHT = 15;
    public static final int PADDLE_Y = 550;
    public static final double PADDLE_SPEED = 12.5;
    public static final Color PADDLE_COLOR = Color.DODGERBLUE;

    // Ball
    public static final int BALL_RADIUS = 12;
    public static final double BALL_SPEED = 6.5;
    public static final Color BALL_COLOR = Color.WHITE;

    // Bricks
    public static final int BRICK_WIDTH = 80;
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
    public static final int POWERUP_SIZE = 40; // x2 size for power-up sprites
    public static final double POWERUP_FALL_SPEED = 1.5;
    // Power-up palette (per spec)
    public static final Color POWERUP_EXPAND_COLOR = Color.YELLOW;
    public static final Color POWERUP_SHRINK_COLOR = Color.RED;
    public static final Color POWERUP_SPEED_UP_COLOR = Color.PINK;
    public static final Color POWERUP_MULTI_BALL_COLOR = Color.BLUE;
    public static final double POWERUP_DURATION = 10000;

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

    // Soundtrack

    public static final String SOUND_TITLE = "Resources/Soundtrack/title.wav";
    public static final String SOUND_STAGE_START = "Resources/Soundtrack/stagestart.wav";
    public static final String SOUND_PADDLE_HIT = "Resources/Soundtrack/paddle.wav";
    public static final String SOUND_WALL_HIT = "Resources/Soundtrack/wall.wav";
    public static final String SOUND_SCORE = "Resources/Soundtrack/score.wav";
    public static final String SOUND_GAME_OVER = "Resources/Soundtrack/gameover.wav";
    public static final String SOUND_BRICK_BREAK = "Resources/Soundtrack/brick_break.wav";

}