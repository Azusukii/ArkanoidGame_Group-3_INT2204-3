package Arkanoid.view;

import Arkanoid.audio.AudioSetting;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

/**
 * Modal Settings window with inline styling (no external CSS).
 */
public class SettingsView {

    // --- Styles ---
    private static final String STYLE_DIALOG_ROOT =
            "-fx-background-color: #E3F2FD;";

    private static final String STYLE_LABEL =
            "-fx-font-family: 'Arial', sans-serif;" +
            "-fx-font-size: 14px;" +
            "-fx-font-weight: bold;" +
            "-fx-text-fill: #0D47A1;";

    private static final String STYLE_BUTTON_IDLE =
            "-fx-background-color: #1976D2;" +
            "-fx-text-fill: white;" +
            "-fx-background-radius: 20px;" +
            "-fx-border-radius: 20px;" +
            "-fx-font-family: 'Arial', sans-serif;" +
            "-fx-font-size: 14px;" +
            "-fx-font-weight: bold;" +
            "-fx-pref-height: 35px;" +
            "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.2), 5, 0, 0, 2);";

    private static final String STYLE_BUTTON_HOVER =
            "-fx-background-color: #42A5F5;" +
            "-fx-text-fill: white;" +
            "-fx-background-radius: 20px;" +
            "-fx-border-radius: 20px;" +
            "-fx-font-family: 'Arial', sans-serif;" +
            "-fx-font-size: 14px;" +
            "-fx-font-weight: bold;" +
            "-fx-pref-height: 35px;" +
            "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.2), 5, 0, 0, 2);";
    // --- End Styles ---

    public static void showSettings() {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Settings");
        dialog.setResizable(false);

        AudioSetting settings = AudioSetting.getInstance();

        // Labels
        Label musicLabel = new Label("Background Volume");
        musicLabel.setStyle(STYLE_LABEL);
        Label effectsLabel = new Label("Effects Volume");
        effectsLabel.setStyle(STYLE_LABEL);
        Label ambientLabel = new Label("Ambient Volume");
        ambientLabel.setStyle(STYLE_LABEL);

        // Sliders
        Slider music = new Slider(0, 1, settings.getMusicVolume());
        music.setShowTickLabels(true);
        music.setShowTickMarks(true);
        music.setMajorTickUnit(0.25);

        Slider effects = new Slider(0, 1, settings.getEffectVolume());
        effects.setShowTickLabels(true);
        effects.setShowTickMarks(true);
        effects.setMajorTickUnit(0.25);

        Slider ambient = new Slider(0, 1, settings.getAmbientVolume());
        ambient.setShowTickLabels(true);
        ambient.setShowTickMarks(true);
        ambient.setMajorTickUnit(0.25);

        // Buttons
        Button apply = new Button("Apply");
        apply.setStyle(STYLE_BUTTON_IDLE);
        apply.setMaxWidth(Double.MAX_VALUE);

        Button save = new Button("Save");
        save.setStyle(STYLE_BUTTON_IDLE);
        save.setMaxWidth(Double.MAX_VALUE);

        Button close = new Button("Close");
        close.setStyle(STYLE_BUTTON_IDLE);
        close.setMaxWidth(Double.MAX_VALUE);

        // Button actions
        apply.setOnAction(e -> {
            settings.setMusicVolume((float) music.getValue());
            settings.setEffectVolume((float) effects.getValue());
            settings.setAmbientVolume((float) ambient.getValue());
            settings.apply();
        });
        save.setOnAction(e -> {
            settings.setMusicVolume((float) music.getValue());
            settings.setEffectVolume((float) effects.getValue());
            settings.setAmbientVolume((float) ambient.getValue());
            settings.apply();
            settings.saveSettings();
        });
        close.setOnAction(e -> dialog.close());
        
        // Buttons HBox
        HBox buttonBox = new HBox(10, apply, save, close);
        buttonBox.setAlignment(Pos.CENTER);

        // Root layout
        VBox root = new VBox(10,
                musicLabel, music,
                effectsLabel, effects,
                ambientLabel, ambient,
                buttonBox);
        root.setPadding(new Insets(20));
        root.setStyle(STYLE_DIALOG_ROOT);

        Scene scene = new Scene(root, 380, 320);

        // Hover behavior
        setupButtonHover(apply, scene);
        setupButtonHover(save, scene);
        setupButtonHover(close, scene);
        
        dialog.setScene(scene);
        dialog.showAndWait();
    }
    
    /** Adds hover styles and cursor changes to a button. */
    private static void setupButtonHover(Button button, Scene scene) {
        button.setOnMouseEntered(e -> {
            button.setStyle(STYLE_BUTTON_HOVER);
            scene.setCursor(Cursor.HAND);
        });

        button.setOnMouseExited(e -> {
            button.setStyle(STYLE_BUTTON_IDLE);
            scene.setCursor(Cursor.DEFAULT);
        });
    }
}