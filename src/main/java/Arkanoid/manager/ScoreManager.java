package Arkanoid.manager;

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
        score = 0;
        level = 1;
        lives = 3;
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
}