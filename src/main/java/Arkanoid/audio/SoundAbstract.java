package Arkanoid.audio;

import javafx.scene.media.AudioClip;
import javafx.scene.media.MediaException;
import java.io.File;

public abstract class SoundAbstract implements SoundInterface{
    protected float volume = 1.0f;
    protected AudioClip clip;
    /** Loads an audio clip from classpath ("/...") or filesystem path using JavaFX AudioClip. */
    public void load(String soundPath) {
        String url;
        if (soundPath != null && soundPath.startsWith("/")) {
            var res = SoundAbstract.class.getResource(soundPath);
            if (res == null) {
                System.err.println("Resource not found: " + soundPath + " (audio will be disabled for this clip)");
                clip = null;
                return;
            }
            url = res.toExternalForm();
        } else {
            url = new File(soundPath).toURI().toString();
        }
        try {
            clip = new AudioClip(url);
            setVolume(volume);
        } catch (MediaException ex) {
            System.err.println("Failed to load sound '" + soundPath + "': " + ex.getMessage());
            clip = null;
        }
    }
    /** Sets volume (0.0 to 1.0). */
    public void setVolume(float volume) {
        this.volume = volume;
    if (clip != null) clip.setVolume(Math.max(0.0, Math.min(1.0, (double) volume)));
    }

    /** Returns current logical volume value (0.0 to 1.0). */
    public float getVolume() {
        return volume;
    }

}
