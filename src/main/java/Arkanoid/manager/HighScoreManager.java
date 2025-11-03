package Arkanoid.manager;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * File-backed high score manager.
 * Stores entries as lines in the format: name|score using UTF-8 encoding.
 */
public class HighScoreManager {
    private final File storeFile;
    private final int maxEntries;

    public static class Entry {
        public final String name;
        public final int score;
        public Entry(String name, int score) { this.name = name; this.score = score; }
        @Override public String toString() { return name + " - " + score; }
    }

    /** Creates a manager using default path and capacity. */
    public HighScoreManager() { this("highscores.txt", 10); }

    /**
     * Creates a manager with a custom file path and maximum entries.
     */
    public HighScoreManager(String path, int maxEntries) {
        this.storeFile = new File(path);
        this.maxEntries = Math.max(1, maxEntries);
    }

    /**
     * Adds a score, sanitizes the name, sorts descending, and trims to capacity.
     */
    public synchronized void addScore(String name, int score) {
        List<Entry> entries = load();
        entries.add(new Entry(sanitize(name), Math.max(0, score)));
        entries.sort(Comparator.comparingInt((Entry e) -> e.score).reversed());
        if (entries.size() > maxEntries) entries = new ArrayList<>(entries.subList(0, maxEntries));
        save(entries);
    }

    /** Returns an unmodifiable view of the current high score list. */
    public synchronized List<Entry> getTopScores() {
        return Collections.unmodifiableList(load());
    }

    /** Sanitizes the provided name to a safe, displayable form. */
    private String sanitize(String name) {
        if (name == null) return "Player";
        String s = name.trim();
        if (s.isEmpty()) s = "Player";
        if (s.length() > 20) s = s.substring(0, 20);
        return s.replaceAll("[\n\r|]", " ");
    }

    /** Loads entries from disk, skipping malformed lines. */
    private List<Entry> load() {
        List<Entry> list = new ArrayList<>();
        if (!storeFile.exists()) return list;
        try (BufferedReader br = new BufferedReader(new FileReader(storeFile, StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                int sep = line.lastIndexOf('|');
                if (sep <= 0 || sep >= line.length() - 1) continue;
                String name = line.substring(0, sep);
                int score;
                try { score = Integer.parseInt(line.substring(sep + 1)); } catch (Exception ignored) { continue; }
                list.add(new Entry(name, score));
            }
        } catch (Exception ignored) {}
        list.sort(Comparator.comparingInt((Entry e) -> e.score).reversed());
        if (list.size() > maxEntries) return new ArrayList<>(list.subList(0, maxEntries));
        return list;
    }

    /** Saves the provided entries to disk, truncating existing content. */
    private void save(List<Entry> entries) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(storeFile, StandardCharsets.UTF_8, false))) {
            for (Entry e : entries) {
                bw.write(e.name + "|" + e.score);
                bw.newLine();
            }
        } catch (Exception ignored) {}
    }
}


