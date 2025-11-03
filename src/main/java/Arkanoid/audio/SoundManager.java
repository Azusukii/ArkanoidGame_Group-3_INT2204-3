package Arkanoid.audio;

import javafx.application.Platform;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class SoundManager {
    private static SoundManager instance;
    private final Map<String, SoundInterface> sounds = new HashMap<>();

    // Mix levels
    private float bgVolume = 0.6f;
    private float ambientVolume = 0.12f;

    // Alternating background players
    private MediaPlayer bgPlayer1;
    private MediaPlayer bgPlayer2;
    // Single timer for ducking to avoid spawning many Timer threads
    private java.util.Timer duckTimer;
    private java.util.TimerTask duckTask;

    private SoundManager() { }

    /** @return singleton instance. */
    public static SoundManager getInstance() {
        if (instance == null) instance = new SoundManager();
        return instance;
    }

    // Catalog management
    /** Register a sound in the catalog. */
    public void addSound(String name, SoundInterface sound) { sounds.put(name, sound); }

    /** Play a sound by name; applies short ducking for effects. */
    public void playSound(String name) {
        SoundInterface sound = sounds.get(name);
        if (sound == null) {
            System.err.println("Sound not found: " + name);
            return;
        }
        // Simple ducking when an effect plays
        if (name.startsWith("effect_")) {
            duckBackgroundForMillis(250);
        }
        sound.play();
    }

    /** Stop a sound by name. */
    public void stopSound(String name) {
        SoundInterface sound = sounds.get(name);
        if (sound != null) sound.stop();
    }

    /** Set volume for a named sound (0.0-1.0). */
    public void setVolume(String name, float volume) {
        SoundInterface sound = sounds.get(name);
        if (sound instanceof SoundAbstract) ((SoundAbstract) sound).setVolume(volume);
    }

    /** @return set of available sound names. */
    public Set<String> getSoundNames() { return sounds.keySet(); }

    /** Stop and clear all registered sounds. */
    public void clearSounds() {
        for (SoundInterface s : sounds.values()) s.stop();
        sounds.clear();
    }

    // Load defaults from resources and set mix
    public void loadDefaultSounds() {
        // Effects
        addSound("effect_wall", new Effect("/sounds/effects/wall.wav"));
        addSound("effect_paddle", new Effect("/sounds/effects/paddle.wav"));
        addSound("effect_brick", new Effect("/sounds/effects/brick_break.wav"));
        addSound("effect_score", new Effect("/sounds/effects/score.wav"));

        // Title and jingles
        addSound("music_title", new Music("/sounds/musics/title.wav", false));
        addSound("music_gameover", new Music("/sounds/musics/gameover.wav", false));
        addSound("music_stage_start", new Music("/sounds/musics/stagestart.wav", false));

        // Ambient loop (much quieter than background)
        Ambient ambient = new Ambient("/sounds/ambient/ambient.mp3");
        if (ambient != null) ambient.setVolume(ambientVolume);
        addSound("ambient_bg", ambient);

        // Init alternating background players (two tracks)
        initBackgroundPlayers("/sounds/background/background1.mp3", "/sounds/background/background2.mp3");

        // Default mix
        setVolume("effect_wall", 1.0f);
        setVolume("effect_paddle", 1.0f);
        setVolume("effect_brick", 1.0f);
        setVolume("effect_score", 1.0f);

        setBackgroundVolume(bgVolume);
        setVolume("music_title", 0.7f);
        setVolume("music_gameover", 0.8f);
        setVolume("music_stage_start", 0.9f);
    }

    /** Stop all sounds and background players. */
    public void stopAll() {
        for (String name : sounds.keySet()) stopSound(name);
        stopBackgroundAlternating();
    }

    // Alternating background implementation
    private void initBackgroundPlayers(String res1, String res2) {
        try {
            var u1 = SoundManager.class.getResource(res1);
            var u2 = SoundManager.class.getResource(res2);
            if (u1 != null) bgPlayer1 = new MediaPlayer(new Media(u1.toExternalForm()));
            if (u2 != null) bgPlayer2 = new MediaPlayer(new Media(u2.toExternalForm()));
            if (bgPlayer1 != null && bgPlayer2 != null) {
                bgPlayer1.setOnEndOfMedia(() -> switchTo(bgPlayer2));
                bgPlayer2.setOnEndOfMedia(() -> switchTo(bgPlayer1));
                bgPlayer1.setCycleCount(1);
                bgPlayer2.setCycleCount(1);
                bgPlayer1.setVolume(bgVolume);
                bgPlayer2.setVolume(bgVolume);
            }
        } catch (Exception ex) {
            System.err.println("Failed to init background players: " + ex.getMessage());
            bgPlayer1 = null; bgPlayer2 = null;
        }
    }

    private void switchTo(MediaPlayer next) {
        try {
            if (next == null) return;
            next.stop();
            next.seek(javafx.util.Duration.ZERO);
            next.setVolume(bgVolume);
            next.play();
        } catch (Exception ignored) {}
    }

    /** Start alternating playback (track1 -> track2 -> track1 …). */
    public void startBackgroundAlternating() {
        if (bgPlayer1 == null || bgPlayer2 == null) return;
        Platform.runLater(() -> {
            try {
                bgPlayer1.stop();
                bgPlayer2.stop();
                bgPlayer1.seek(javafx.util.Duration.ZERO);
                bgPlayer1.setVolume(bgVolume);
                bgPlayer1.play();
            } catch (Exception ignored) {}
        });
    }

    /** Stop alternating playback. */
    public void stopBackgroundAlternating() {
        Platform.runLater(() -> {
            try { if (bgPlayer1 != null) bgPlayer1.stop(); } catch (Exception ignored) {}
            try { if (bgPlayer2 != null) bgPlayer2.stop(); } catch (Exception ignored) {}
        });
    }

    /** Set background volume (0.0-1.0). */
    public void setBackgroundVolume(float volume) {
        bgVolume = Math.max(0f, Math.min(1f, volume));
        Platform.runLater(() -> {
            try { if (bgPlayer1 != null) bgPlayer1.setVolume(bgVolume); } catch (Exception ignored) {}
            try { if (bgPlayer2 != null) bgPlayer2.setVolume(bgVolume); } catch (Exception ignored) {}
        });
    }

    /** Set ambient loop volume (0.0-1.0). */
    public void setAmbientVolume(float volume) {
        ambientVolume = Math.max(0f, Math.min(1f, volume));
        setVolume("ambient_bg", ambientVolume);
    }

    /** Set effects group volume by updating all effect_* sounds. */
    public void setEffectsVolume(float volume) {
        float vol = Math.max(0f, Math.min(1f, volume));
        for (String name : sounds.keySet()) {
            if (name.startsWith("effect_")) {
                setVolume(name, vol);
            }
        }
    }

    /** Briefly reduce background/ambient so effects cut through. */
    private void duckBackgroundForMillis(long ms) {
        float priorBg = bgVolume;
        float priorAmb = ambientVolume;
        float duckedBg = priorBg * 0.6f;
        float duckedAmb = priorAmb * 0.7f;

        setBackgroundVolume(duckedBg);
        setVolume("ambient_bg", duckedAmb);

        // Initialize single daemon timer lazily
        if (duckTimer == null) {
            duckTimer = new java.util.Timer(true);
        }

        // Cancel any pending restore task to avoid piling up
        if (duckTask != null) {
            try { duckTask.cancel(); } catch (Exception ignored) {}
            duckTask = null;
        }

        duckTask = new java.util.TimerTask() {
            @Override public void run() {
                setBackgroundVolume(priorBg);
                setVolume("ambient_bg", priorAmb);
                // Clear reference so GC can collect the task
                duckTask = null;
            }
        };
        try {
            duckTimer.schedule(duckTask, ms);
        } catch (IllegalStateException ignored) {
            // Timer may have been cancelled; recreate and reschedule once
            duckTimer = new java.util.Timer(true);
            duckTimer.schedule(duckTask, ms);
        }
    }
}
