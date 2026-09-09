package com.csse3200.game.pausemenu;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Event;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Slider;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.csse3200.game.services.ServiceLocator;
import com.csse3200.game.ui.UIComponent;

public class PauseMenuDisplay extends UIComponent {

  private enum MenuState {
    MAIN,
    SETTINGS,
    AUDIO
  }

  private static final Color OVERLAY_COLOR = new Color(0f, 0f, 0f, 0.45f);
  private static final Color PANEL_COLOR = new Color(0.03f, 0.06f, 0.04f, 0.95f);
  private static final Color SELECTED_BG = new Color(0.15f, 0.35f, 0.55f, 0.9f);
  private static final Color SELECTED_TEXT = Color.CYAN;
  private static final Color UNSELECTED_TEXT = Color.WHITE;
  private static final float INACTIVE_PANEL_ALPHA = 0.55f;
  private static final float PANEL_GAP = 25f;
  private static final float MUSIC_STEP = 0.05f;

  private static final String PREFS_NAME = "pause_menu_settings";
  private static final String MUSIC_VOLUME_KEY = "musicVolume";
  private static final float DEFAULT_MUSIC_VOL = 0.8f;

  private static final String[] MAIN_ITEMS = {"Resume", "Restart", "Main Menu", "Settings"};
  private static final String[] SETTINGS_ITEMS = {"Audio", "Back"};
  private static final String[] AUDIO_ITEMS = {"Music Volume", "Back"};

  private final Preferences prefs = (Gdx.app != null) ? Gdx.app.getPreferences(PREFS_NAME) : null;
  private float musicVol =
      (prefs != null) ? prefs.getFloat(MUSIC_VOLUME_KEY, DEFAULT_MUSIC_VOL) : DEFAULT_MUSIC_VOL;
  private boolean musicVolumeApplied = false;

  private Table pauseOverlay;
  private Table root;
  private Table mainPanel;
  private Table settingsPanel;
  private Table audioPanel;

  private Label[] mainLabels;
  private Label[] settingsLabels;
  private Label[] audioLabels;
  private Slider musicSlider;
  private Label musicValueLabel;

  private PauseMenuComponent pauseMenu;
  private MenuState state = MenuState.MAIN;
  private int mainIndex = 0;
  private int settingsIndex = 0;
  private int audioIndex = 0;
  private boolean wasPaused = false;

  @Override
  public void create() {
    super.create();
    pauseMenu = entity.getComponent(PauseMenuComponent.class);
    addActors();
    registerEventListeners();
  }

  private void addActors() {
    pauseOverlay = new Table();
    pauseOverlay.setFillParent(true);
    pauseOverlay.setBackground(skin.newDrawable("white", OVERLAY_COLOR));
    pauseOverlay.setVisible(false);
    stage.addActor(pauseOverlay);

    mainLabels = new Label[MAIN_ITEMS.length];
    mainPanel = buildPanel(MAIN_ITEMS, mainLabels);

    settingsLabels = new Label[SETTINGS_ITEMS.length];
    settingsPanel = buildPanel(SETTINGS_ITEMS, settingsLabels);

    audioLabels = new Label[AUDIO_ITEMS.length];
    audioPanel = buildAudioPanel();

    root = new Table();
    root.setFillParent(true);
    root.left();
    root.padLeft(60f);
    root.add(mainPanel).top();
    root.add(settingsPanel).top().padLeft(PANEL_GAP);
    root.add(audioPanel).top().padLeft(PANEL_GAP);
    root.setVisible(false);
    stage.addActor(root);
  }

  private Label createLabel(String text) {
    Label label = new Label(text, skin);
    Label.LabelStyle style = new Label.LabelStyle(label.getStyle());
    style.fontColor = UNSELECTED_TEXT;
    label.setStyle(style);
    return label;
  }

  private Table buildPanel(String[] items, Label[] labelsOut) {
    Table panel = new Table();
    panel.setBackground(skin.newDrawable("white", PANEL_COLOR));
    panel.pad(20f, 30f, 20f, 30f);
    panel.left();
    for (int i = 0; i < items.length; i++) {
      Label label = createLabel(items[i]);
      labelsOut[i] = label;
      Table row = new Table();
      row.add(label).pad(6f, 15f, 6f, 15f).left();
      panel.add(row).left().padBottom(4f).fillX();
      panel.row();
    }
    return panel;
  }

  private Table buildAudioPanel() {
    Table panel = new Table();
    panel.setBackground(skin.newDrawable("white", PANEL_COLOR));
    panel.pad(20f, 30f, 20f, 30f);
    panel.left();

    Label musicLabel = createLabel(AUDIO_ITEMS[0]);
    audioLabels[0] = musicLabel;
    musicSlider = new Slider(0f, 1f, 0.01f, false, skin);
    musicSlider.setValue(musicVol);
    musicValueLabel = createLabel(String.format("%.0f%%", musicVol * 100));
    musicSlider.addListener(
        (Event event) -> {
          musicVol = musicSlider.getValue();
          musicValueLabel.setText(String.format("%.0f%%", musicVol * 100));
          applyMusicVolume();
          if (prefs != null) {
            prefs.putFloat(MUSIC_VOLUME_KEY, musicVol);
            prefs.flush();
          }
          return true;
        });
    Table sliderRow = new Table();
    sliderRow.add(musicLabel).padRight(15f);
    sliderRow.add(musicSlider).width(160f).padRight(10f);
    sliderRow.add(musicValueLabel);
    panel.add(sliderRow).pad(6f, 15f, 6f, 15f).left();
    panel.row();

    Label backLabel = createLabel(AUDIO_ITEMS[1]);
    audioLabels[1] = backLabel;
    Table backRow = new Table();
    backRow.add(backLabel).pad(6f, 15f, 6f, 15f).left();
    panel.add(backRow).left().padBottom(4f).fillX();

    return panel;
  }

  private void applyMusicVolume() {
    Music music =
        ServiceLocator.getResourceService()
            .getAsset(PauseMenuComponent.BACKGROUND_MUSIC, Music.class);
    if (music != null) {
      music.setVolume(musicVol);
      musicVolumeApplied = true;
    }
  }

  private void registerEventListeners() {
    entity.getEvents().addListener("navigateUp", this::navigateUp);
    entity.getEvents().addListener("navigateDown", this::navigateDown);
    entity.getEvents().addListener("navigateLeft", this::navigateLeft);
    entity.getEvents().addListener("navigateRight", this::navigateRight);
    entity.getEvents().addListener("confirmSelection", this::confirmSelection);
  }

  private void navigateUp() {
    int count = currentItemCount();
    setCurrentIndex((currentIndex() - 1 + count) % count);
  }

  private void navigateDown() {
    int count = currentItemCount();
    setCurrentIndex((currentIndex() + 1) % count);
  }

  private void navigateLeft() {
    if (state != MenuState.AUDIO || audioIndex != 0) {
      return;
    }
    musicSlider.setValue(Math.max(0f, musicSlider.getValue() - MUSIC_STEP));
  }

  private void navigateRight() {
    if (state != MenuState.AUDIO || audioIndex != 0) {
      return;
    }
    musicSlider.setValue(Math.min(1f, musicSlider.getValue() + MUSIC_STEP));
  }

  private int currentItemCount() {
    return switch (state) {
      case MAIN -> MAIN_ITEMS.length;
      case SETTINGS -> SETTINGS_ITEMS.length;
      case AUDIO -> AUDIO_ITEMS.length;
    };
  }

  private int currentIndex() {
    return switch (state) {
      case MAIN -> mainIndex;
      case SETTINGS -> settingsIndex;
      case AUDIO -> audioIndex;
    };
  }

  private void setCurrentIndex(int index) {
    switch (state) {
      case MAIN -> mainIndex = index;
      case SETTINGS -> settingsIndex = index;
      case AUDIO -> audioIndex = index;
    }
    refreshHighlights();
  }

  private void confirmSelection() {
    switch (state) {
      case MAIN -> confirmMain();
      case SETTINGS -> confirmSettings();
      case AUDIO -> confirmAudio();
    }
  }

  private void confirmMain() {
    switch (mainIndex) {
      case 0 -> entity.getEvents().trigger("resumeClicked");
      case 1 -> entity.getEvents().trigger("restartClicked");
      case 2 -> entity.getEvents().trigger("mainMenuClicked");
      case 3 -> {
        state = MenuState.SETTINGS;
        settingsIndex = 0;
        refreshPanels();
      }
      default -> {}
    }
  }

  private void confirmSettings() {
    switch (settingsIndex) {
      case 0 -> {
        state = MenuState.AUDIO;
        audioIndex = 0;
        refreshPanels();
      }
      case 1 -> {
        state = MenuState.MAIN;
        refreshPanels();
      }
      default -> {}
    }
  }

  private void confirmAudio() {
    if (audioIndex == 1) {
      state = MenuState.SETTINGS;
      refreshPanels();
    }
  }

  /** Shows/hides and dims panels to reflect the current drill-down depth. */
  private void refreshPanels() {
    settingsPanel.setVisible(state != MenuState.MAIN);
    audioPanel.setVisible(state == MenuState.AUDIO);

    mainPanel.getColor().a = (state == MenuState.MAIN) ? 1f : INACTIVE_PANEL_ALPHA;
    settingsPanel.getColor().a = (state == MenuState.SETTINGS) ? 1f : INACTIVE_PANEL_ALPHA;
    audioPanel.getColor().a = (state == MenuState.AUDIO) ? 1f : INACTIVE_PANEL_ALPHA;

    refreshHighlights();
  }

  /** Re-applies the selected/unselected look to every visible panel's rows. */
  private void refreshHighlights() {
    highlightPanel(mainLabels, mainIndex, state == MenuState.MAIN);
    highlightPanel(settingsLabels, settingsIndex, state == MenuState.SETTINGS);
    highlightPanel(audioLabels, audioIndex, state == MenuState.AUDIO);
  }

  private void highlightPanel(Label[] labels, int selectedIndex, boolean isActivePanel) {
    for (int i = 0; i < labels.length; i++) {
      boolean selected = isActivePanel && i == selectedIndex;
      labels[i].getStyle().fontColor = selected ? SELECTED_TEXT : UNSELECTED_TEXT;
      Table row = (Table) labels[i].getParent();
      row.setBackground(selected ? skin.newDrawable("white", SELECTED_BG) : null);
    }
  }

  @Override
  public void draw(SpriteBatch batch) {
    if (!musicVolumeApplied) {
      applyMusicVolume();
    }

    boolean isPaused = pauseMenu.isPaused();
    pauseOverlay.setVisible(isPaused);
    root.setVisible(isPaused);

    if (isPaused && !wasPaused) {
      state = MenuState.MAIN;
      mainIndex = 0;
      settingsIndex = 0;
      audioIndex = 0;
      refreshPanels();
    }
    wasPaused = isPaused;
  }

  @Override
  public void dispose() {
    root.clear();
    super.dispose();
  }
}