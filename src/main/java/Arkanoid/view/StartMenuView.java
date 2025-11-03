package Arkanoid.view;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane; 
import javafx.stage.Stage;

import Arkanoid.manager.HighScoreManager;

/**
 * Start menu with inline styles and embedded settings/leaderboard views.
 */
public class StartMenuView {
    private final Stage stage;
    private final Scene scene;
    private final StackPane root; 
    private Runnable onStart;
    private Runnable onSettings;
    private Runnable onHighscores;

    // --- CSS Styles ---

    // Scene root style
    private final String STYLE_SCENE_ROOT = 
        "-fx-background-color: #E3F2FD;"; 

    // Panel style
    private final String STYLE_PANEL_ROOT = 
        "-fx-background-color: white;" +
        "-fx-background-radius: 25px;" +
        "-fx-border-color: #0D47A1;" + 
        "-fx-border-width: 10px;" +
        "-fx-border-radius: 20px;" +
        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 15, 0, 0, 5);";

    // Title style
    private final String STYLE_TITLE = 
        "-fx-font-family: 'Arial Black', sans-serif;" +
        "-fx-font-size: 48px;" +
        "-fx-text-fill: #0D47A1;";

    // Subtitle style
    private final String STYLE_SUB_TITLE = 
        "-fx-font-family: 'Arial Black', sans-serif;" +
        "-fx-font-size: 36px;" +
        "-fx-text-fill: #0D47A1;";

    // Primary button style
    private final String STYLE_BUTTON_IDLE = 
        "-fx-background-color: #1976D2;" + 
        "-fx-text-fill: white;" +
        "-fx-background-radius: 30px;" +
        "-fx-border-radius: 30px;" +
        "-fx-font-family: 'Arial', sans-serif;" +
        "-fx-font-size: 18px;" +
        "-fx-font-weight: bold;" +
        "-fx-pref-height: 50px;" +
        "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.2), 5, 0, 0, 2);";

    private final String STYLE_BUTTON_HOVER = 
        "-fx-background-color: #42A5F5;" + 
        "-fx-text-fill: white;" +
        "-fx-background-radius: 30px;" +
        "-fx-border-radius: 30px;" +
        "-fx-font-family: 'Arial', sans-serif;" +
        "-fx-font-size: 18px;" +
        "-fx-font-weight: bold;" +
        "-fx-pref-height: 50px;" +
        "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.2), 5, 0, 0, 2);";
        
    // Small button style
    private final String STYLE_BUTTON_SM_IDLE = 
        "-fx-background-color: #1976D2;" +
        "-fx-text-fill: white;" +
        "-fx-background-radius: 20px;" +
        "-fx-border-radius: 20px;" +
        "-fx-font-family: 'Arial', sans-serif;" +
        "-fx-font-size: 14px;" +
        "-fx-font-weight: bold;" +
        "-fx-pref-height: 35px;" +
        "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.2), 5, 0, 0, 2);";

    private final String STYLE_BUTTON_SM_HOVER = 
        "-fx-background-color: #42A5F5;" +
        "-fx-text-fill: white;" +
        "-fx-background-radius: 20px;" +
        "-fx-border-radius: 20px;" +
        "-fx-font-family: 'Arial', sans-serif;" +
        "-fx-font-size: 14px;" +
        "-fx-font-weight: bold;" +
        "-fx-pref-height: 35px;" +
        "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.2), 5, 0, 0, 2);";

    // Content label style
    private final String STYLE_CONTENT_LABEL =
        "-fx-font-family: 'Arial', sans-serif;" +
        "-fx-font-size: 16px;" +
        "-fx-text-fill: #0D47A1;";
        
    // Slider label style
    private final String STYLE_SLIDER_LABEL =
        "-fx-font-family: 'Arial', sans-serif;" +
        "-fx-font-size: 14px;" +
        "-fx-font-weight: bold;" +
        "-fx-text-fill: #0D47A1;";

    // --- End Styles ---


    public StartMenuView(Stage stage) {
        this.stage = stage;
        this.root = new StackPane();
        this.root.setAlignment(Pos.CENTER);
        this.root.setPrefSize(800, 600);
        this.root.setStyle(STYLE_SCENE_ROOT); 

        this.scene = new Scene(this.root); 
        buildMenu(); 
    }

    public void setOnStart(Runnable r) { this.onStart = r; }
    public void setOnSettings(Runnable r) { this.onSettings = r; }
    public void setOnHighscores(Runnable r) { this.onHighscores = r; }

    public void show() {
        stage.setScene(scene);
        stage.setTitle("Arkanoid - Menu");
    }

    public void showSettingsInline() {
        Arkanoid.audio.AudioSetting settings = Arkanoid.audio.AudioSetting.getInstance();
        
        Label title = new Label("Sound Settings");
        title.setStyle(STYLE_SUB_TITLE);

        Label l1 = new Label("Background Volume");
        l1.setStyle(STYLE_SLIDER_LABEL);
        Slider s1 = new Slider(0, 1, settings.getMusicVolume());

        Label l2 = new Label("Effects Volume");
        l2.setStyle(STYLE_SLIDER_LABEL);
        Slider s2 = new Slider(0, 1, settings.getEffectVolume());

        Label l3 = new Label("Ambient Volume");
        l3.setStyle(STYLE_SLIDER_LABEL);
        Slider s3 = new Slider(0, 1, settings.getAmbientVolume());

        Button apply = new Button("Apply & Save");
        apply.setStyle(STYLE_BUTTON_SM_IDLE);
        apply.setOnAction(e -> {
            settings.setMusicVolume((float) s1.getValue());
            settings.setEffectVolume((float) s2.getValue());
            settings.setAmbientVolume((float) s3.getValue());
            settings.apply();
            settings.saveSettings();
        });

        Button back = new Button("Back");
        back.setStyle(STYLE_BUTTON_SM_IDLE);
        back.setOnAction(e -> buildMenu());
        
        HBox buttonBox = new HBox(10, apply, back);
        buttonBox.setAlignment(Pos.CENTER);

        setupButtonHover(apply, STYLE_BUTTON_SM_IDLE, STYLE_BUTTON_SM_HOVER);
        setupButtonHover(back, STYLE_BUTTON_SM_IDLE, STYLE_BUTTON_SM_HOVER);

        VBox pane = new VBox(10, title, l1, s1, l2, s2, l3, s3, buttonBox);
        pane.setAlignment(Pos.CENTER);
        pane.setStyle(STYLE_PANEL_ROOT);
        pane.setPadding(new Insets(30, 40, 30, 40));
        pane.setMaxWidth(400);

        swapContent(pane);
    }

    //
    // ▼▼▼ DÒNG NÀY ĐÃ ĐƯỢC SỬA ▼▼▼
    //
    public void showLeaderboardInline(java.util.List<HighScoreManager.Entry> entries) {
    //
    // ▲▲▲ DÒNG TRÊN ĐÃ ĐƯỢC SỬA (Từ ScoreEntry -> Entry) ▲▲▲
    //
        Label title = new Label("Leaderboard");
        title.setStyle(STYLE_SUB_TITLE);

        VBox list = new VBox(6);
        list.setAlignment(Pos.CENTER);
        
        if (entries == null || entries.isEmpty()) {
            Label line = new Label("No high scores yet!");
            line.setStyle(STYLE_CONTENT_LABEL);
            list.getChildren().add(line);
        } else {
            int rank = 1;
            // Mỗi entry đóng khung và in đậm toàn bộ dòng dạng: 1.name - score
            String STYLE_ENTRY_BOX =
                "-fx-background-color: #E3F2FD;" +
                "-fx-background-radius: 12px;" +
                "-fx-border-color: #0D47A1;" +
                "-fx-border-width: 2px;" +
                "-fx-border-radius: 12px;" +
                "-fx-padding: 8px 12px;";
            String STYLE_ENTRY_TEXT =
                "-fx-font-family: 'Arial Black', sans-serif;" +
                "-fx-font-size: 16px;" +
                "-fx-text-fill: #0D47A1;";

            for (var e : entries) {
                String text = String.format("%d.%s - %d", rank++, e.name, e.score);
                javafx.scene.control.Label lbl = new javafx.scene.control.Label(text);
                lbl.setStyle(STYLE_ENTRY_TEXT);

                javafx.scene.layout.HBox row = new javafx.scene.layout.HBox(lbl);
                row.setAlignment(Pos.CENTER_LEFT);
                row.setStyle(STYLE_ENTRY_BOX);
                row.setMaxWidth(320);
                list.getChildren().add(row);
            }
        }

        Button back = new Button("Back");
        back.setStyle(STYLE_BUTTON_SM_IDLE);
        back.setOnAction(ev -> buildMenu());
        setupButtonHover(back, STYLE_BUTTON_SM_IDLE, STYLE_BUTTON_SM_HOVER);

        VBox pane = new VBox(12, title, list, back);
        pane.setAlignment(Pos.CENTER);
        pane.setStyle(STYLE_PANEL_ROOT);
        pane.setPadding(new Insets(30, 40, 30, 40));
        pane.setMaxWidth(400);

        swapContent(pane);
    }

    /** Builds the main menu content. */
    private void buildMenu() {
        Label title = new Label("ARKANOID");
        title.setStyle(STYLE_TITLE);

        Button startBtn = new Button("Start Game"); 
        startBtn.setMaxWidth(Double.MAX_VALUE);
        startBtn.setStyle(STYLE_BUTTON_IDLE);
        startBtn.setOnAction(e -> { if (onStart != null) onStart.run(); });

        Button settingsBtn = new Button("Sound Settings");
        settingsBtn.setMaxWidth(Double.MAX_VALUE);
        settingsBtn.setStyle(STYLE_BUTTON_IDLE);
        settingsBtn.setOnAction(e -> { if (onSettings != null) onSettings.run(); else showSettingsInline(); });

        Button highBtn = new Button("High Scores");
        highBtn.setMaxWidth(Double.MAX_VALUE);
        highBtn.setStyle(STYLE_BUTTON_IDLE);
        highBtn.setOnAction(e -> {
            if (onHighscores != null) onHighscores.run();
        });
        
        VBox menuPane = new VBox(20, title, startBtn, settingsBtn, highBtn);
        menuPane.setAlignment(Pos.CENTER);
        menuPane.setStyle(STYLE_PANEL_ROOT);
        menuPane.setPadding(new Insets(30, 40, 30, 40));
        menuPane.setMaxWidth(400);

        setupButtonHover(startBtn, STYLE_BUTTON_IDLE, STYLE_BUTTON_HOVER);
        setupButtonHover(settingsBtn, STYLE_BUTTON_IDLE, STYLE_BUTTON_HOVER);
        setupButtonHover(highBtn, STYLE_BUTTON_IDLE, STYLE_BUTTON_HOVER);

        swapContent(menuPane);
    }

    /** Replaces content in the root stack pane. */
    private void swapContent(VBox content) {
        root.getChildren().setAll(content);
    }

    /** Applies hover styles to a button. */
    private void setupButtonHover(Button button, String idleStyle, String hoverStyle) {
        button.setOnMouseEntered(e -> {
            button.setStyle(hoverStyle);
            scene.setCursor(Cursor.HAND);
        });
        button.setOnMouseExited(e -> {
            button.setStyle(idleStyle);
            scene.setCursor(Cursor.DEFAULT);
        });
    }
}