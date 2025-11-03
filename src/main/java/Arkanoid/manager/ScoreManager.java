package Arkanoid.manager;

/**
 * Tracks score, high score, current level index and remaining lives.
 * Provides helpers for adding score, managing lives, and progressing to the next level.
 */
public class ScoreManager {
    private int score;
    private int highScore;
    private int level;
    private int lives;

    public ScoreManager() {
        this.score = 0;
        this.highScore = 0;
        this.level = 1;
        this.lives = 3;
    }

    public void addScore(int points) {
        score += points;
        if (score > highScore) {
            highScore = score;
        }
    }

    public void loseLife() {
        lives--;
    }

    public void addLife() {
        lives++;
    }

    public void nextLevel() {
        level++;
    }

    public void reset() {
        this.score = 0;
        this.level = 1;
        this.lives = 3;
    }

    public boolean isGameOver() {
        return lives <= 0;
    }

    // Getters
    public int getScore() {
        return score;
    }

    public int getHighScore() {
        return highScore;
    }

    public int getLevel() {
        return level;
    }

    public int getLives() {
        return lives;
    }

    public void setLives(int lives) {
        this.lives = lives;
    }
}