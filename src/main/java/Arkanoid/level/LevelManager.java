package Arkanoid.level;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages level progression and selection.
 * Holds a list of loaded Level instances, tracks the current index,
 * and provides navigation (next/previous/select) as well as unlock logic.
 */
public class LevelManager {
    private List<Level> levels;
    private int currentLevelIndex;
    private int totalLevels;
    private int highestUnlockedLevel;

    public LevelManager() {
        this.levels = new ArrayList<>();
        this.currentLevelIndex = 0;
        this.highestUnlockedLevel = Integer.MAX_VALUE; // Unlock all levels by default
    }

    /**
     * Loads up to maxLevels from LevelLoader and initializes each level.
     */
    // Tải danh sách level từ dữ liệu (resources hoặc filesystem)
    public void loadLevels(int maxLevels) {
        levels.clear();
        this.totalLevels = maxLevels;

        for (int i = 1; i <= maxLevels; i++) {
            LevelData levelData = LevelLoader.loadLevel(i);

            if (levelData != null) {
                Level level = new Level(levelData);
                level.initialize();
                levels.add(level);
            } else {
                // If loading fails, create a default sample level
                LevelData defaultData = LevelLoader.createSampleLevel(i, "Level " + i);
                Level level = new Level(defaultData);
                level.initialize();
                levels.add(level);
            }
        }
    }

    /** Returns the currently selected Level or null if none. */
    // Lấy level hiện tại (theo chỉ số đang chọn)
    public Level getCurrentLevel() {
        if (levels.isEmpty()) return null;
        return levels.get(currentLevelIndex);
    }

    /** Advances to next level if available.
     * @return true if moved to next level
     */
    // Chuyển sang level tiếp theo nếu còn
    public boolean nextLevel() {
        if (currentLevelIndex < levels.size() - 1) {
            currentLevelIndex++;

            // Unlock newly reached level if needed
            if (currentLevelIndex + 1 > highestUnlockedLevel) {
                highestUnlockedLevel = currentLevelIndex + 1;
            }

            return true;
        }
    return false; // No more levels
    }

    /** Moves back to previous level if possible.
     * @return true if moved to previous level
     */
    // Quay về level trước nếu có
    public boolean previousLevel() {
        if (currentLevelIndex > 0) {
            currentLevelIndex--;
            return true;
        }
        return false;
    }

    /** Selects a specific level if it exists and is unlocked.
     * @param levelNumber 1-based index
     * @return true if selection succeeded
     */
    // Chọn level theo số (1-based) nếu hợp lệ và được mở khóa
    public boolean selectLevel(int levelNumber) {
        int index = levelNumber - 1;

        if (index >= 0 && index < levels.size() && levelNumber <= highestUnlockedLevel) {
            currentLevelIndex = index;
            return true;
        }
        return false;
    }

    /** Re-initializes the current level to its initial state. */
    // Reset lại level hiện tại về trạng thái ban đầu
    public void resetCurrentLevel() {
        if (getCurrentLevel() != null) {
            getCurrentLevel().reset();
        }
    }

    /** Resets progression to level 1 and resets all levels. */
    // Bắt đầu lại từ level 1 và reset tất cả level
    public void restartGame() {
        currentLevelIndex = 0;
        for (Level level : levels) {
            level.reset();
        }
    }

    /** @return true if the last level is selected and completed. */
    public boolean isGameComplete() {
        return currentLevelIndex >= levels.size() - 1 &&
                getCurrentLevel() != null &&
                getCurrentLevel().isCompleted();
    }

    /** @return current level number (1-based). */
    public int getCurrentLevelNumber() {
        return currentLevelIndex + 1;
    }

    /** @return total number of loaded levels. */
    public int getTotalLevels() {
        return levels.size();
    }

    /** @return highest unlocked level number. */
    public int getHighestUnlockedLevel() {
        return highestUnlockedLevel;
    }

    /** Sets the highest unlocked level, clamped to total levels. */
    public void setHighestUnlockedLevel(int level) {
        this.highestUnlockedLevel = Math.min(level, totalLevels);
    }

    /** @return true if the given 1-based level is unlocked. */
    public boolean isLevelUnlocked(int levelNumber) {
        return levelNumber <= highestUnlockedLevel;
    }

    /** Unlocks all levels. */
    public void unlockAllLevels() {
        highestUnlockedLevel = totalLevels;
    }

    /** @return a copy of the internal level list. */
    public List<Level> getAllLevels() {
        return new ArrayList<>(levels);
    }

    /** @return overall progress percentage across levels. */
    public double getProgress() {
        if (totalLevels == 0) return 0.0;
        return (double) (currentLevelIndex + 1) / totalLevels * 100.0;
    }
}