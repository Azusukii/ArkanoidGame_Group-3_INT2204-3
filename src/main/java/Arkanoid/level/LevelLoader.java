package Arkanoid.level;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Loads and saves level configurations from JSON files.
 * <p>
 * Prefers classpath resources under "/levels" packaged from src/main/resources.
 * Falls back to a development filesystem path (resources/levels) when running from IDE.
 * Also provides helpers to check existence and count available sequential levels.
 */
public class LevelLoader {
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private static final String LEVELS_PATH = "resources/levels/"; // fallback when running from IDE

    /**
     * Loads a level JSON by number from classpath (preferred) or filesystem fallback.
     * @param levelNumber 1-based level index (level1.json, level2.json, ...)
     * @return parsed {@link LevelData} or null on error/missing file
     */
    // Tải dữ liệu level theo số thứ tự từ resources hoặc fallback filesystem
    public static LevelData loadLevel(int levelNumber) {
    String filename = "/levels/level" + levelNumber + ".json"; // in resources

        try (InputStream is = LevelLoader.class.getResourceAsStream(filename)) {
            if (is != null) {
                // Load from classpath (resources)
                try (Reader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                    LevelData data = gson.fromJson(reader, LevelData.class);
                    return data;
                }
            } else {
                // Fallback: read from resources/levels on filesystem
                String fallback = LEVELS_PATH + "level" + levelNumber + ".json";
                if (Files.exists(Paths.get(fallback))) {
                    String json = new String(Files.readAllBytes(Paths.get(fallback)), StandardCharsets.UTF_8);
                    LevelData data = gson.fromJson(json, LevelData.class);
                    return data;
                } else {
                    System.err.println("Missing level file: " + fallback);
                }
            }
        } catch (Exception e) {
            System.err.println("Error loading level " + levelNumber + ": " + e.getMessage());
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Loads levels 1..maxLevel, returning an array (entries may be null if missing).
     */
    // Tải nhiều level 1..maxLevel (có thể có phần tử null nếu thiếu)
    public static LevelData[] loadAllLevels(int maxLevel) {
        LevelData[] levels = new LevelData[maxLevel];
        for (int i = 1; i <= maxLevel; i++) {
            levels[i - 1] = loadLevel(i);
        }
        return levels;
    }

    /**
     * Returns true if a level JSON exists either in classpath or filesystem fallback.
     */
    // Kiểm tra level có tồn tại (resources hoặc filesystem)
    public static boolean levelExists(int levelNumber) {
        String pathInResources = "/levels/level" + levelNumber + ".json";
        if (LevelLoader.class.getResource(pathInResources) != null)
            return true;

        return Files.exists(Paths.get(LEVELS_PATH + "level" + levelNumber + ".json"));
    }

    /**
     * Counts sequential level files starting from 1 up to the first gap or maxScan.
     */
    // Đếm số lượng level liên tiếp từ 1 đến khi gặp thiếu hoặc đạt maxScan
    public static int countAvailableLevels(int maxScan) {
        int count = 0;
        for (int i = 1; i <= maxScan; i++) {
            if (levelExists(i)) count++;
            else break; // stop at first missing sequence gap
        }
        return count;
    }

    /**
     * Saves the given level configuration to the filesystem fallback directory.
     * @return true on success, false on I/O error
     */
    // Lưu dữ liệu level ra thư mục fallback để chỉnh sửa/ghi file
    public static boolean saveLevel(LevelData levelData) {
    String filename = LEVELS_PATH + "level" + levelData.getLevelNumber() + ".json";

        try {
            // Create directory if missing
            new File(LEVELS_PATH).mkdirs();

            // Write JSON file
            try (Writer writer = new OutputStreamWriter(new FileOutputStream(filename), StandardCharsets.UTF_8)) {
                gson.toJson(levelData, writer);
            }

            return true;
        } catch (IOException e) {
            System.err.println("Error saving level: " + e.getMessage());
            return false;
        }
    }

    /**
     * Creates a simple grid-based sample level for debugging or exporting.
     */
    // Tạo level mẫu dạng lưới dùng cho debug hoặc export
    public static LevelData createSampleLevel(int levelNumber, String name) {
        LevelData levelData = new LevelData();
        levelData.setLevelNumber(levelNumber);
        levelData.setName(name);
        levelData.setBallSpeed(4.5);
        levelData.setLives(3);

        java.util.List<LevelData.BrickData> bricks = new java.util.ArrayList<>();
        for (int row = 0; row < 5; row++) {
            for (int col = 0; col < 10; col++) {
                LevelData.BrickData brick = new LevelData.BrickData(row, col, "NORMAL", getColorForRow(row));
                bricks.add(brick);
            }
        }
        levelData.setBricks(bricks);
        return levelData;
    }

    /**
     * Picks a color by row index for sample level creation.
     */
    // Chọn màu theo hàng khi tạo level mẫu
    private static String getColorForRow(int row) {
        String[] colors = {
                "#FF0000", // Red
                "#FFA500", // Orange
                "#FFFF00", // Yellow
                "#00FF00", // Green
                "#00FFFF", // Cyan
                "#0000FF", // Blue
                "#800080", // Purple
                "#FFC0CB"  // Pink
        };
        return colors[row % colors.length];
    }
}
