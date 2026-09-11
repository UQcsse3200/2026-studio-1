package com.csse3200.game.pausemenu;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Event;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
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

  private static final Color PANEL_COLOR = new Color(0.03f, 0.06f, 0.04f, 0.95f);
  private static final Color SELECTED_BG = new Color(0.15f, 0.35f, 0.55f, 0.9f);
  private static final Color SELECTED_TEXT = Color.CYAN;
  private static final Color UNSELECTED_TEXT = Color.WHITE;
  private static final float INACTIVE_PANEL_ALPHA = 0.55f;
  private static final float PANEL_GAP = 25f;
  private static final float VOLUME_STEP = 0.05f;

  private static final long HOLD_INITIAL_DELAY_MS = 400;
  private static final long HOLD_REPEAT_INTERVAL_MS = 80;

  private boolean leftHeld = false;
  private boolean rightHeld = false;
  private long leftHoldStart = 0;
  private long leftLastRepeat = 0;
  private long rightHoldStart = 0;
  private long rightLastRepeat = 0;

  private static final String PREFS_NAME = "pause_menu_settings";
  private static final String MASTER_VOLUME_KEY = "master";
  private static final String MUSIC_VOLUME_KEY = "music";
  private static final String EFFECTS_VOLUME_KEY = "effects";
  private static final float DEFAULT_MASTER_VOL = 1f;
  private static final float DEFAULT_MUSIC_VOL = 0.8f;
  private static final float DEFAULT_EFFECTS_VOL = 1f;

  private static final String[] MAIN_ITEMS = {"Resume", "Restart", "Settings", "Main Menu"};
  private static final String[] SETTINGS_ITEMS = {"Audio", "Back"};
  private static final String[] AUDIO_ITEMS = {"Master ", "Music ", "Effects ", "Back"};
  private static final int AUDIO_BACK_INDEX = 3;

  private final Preferences prefs = (Gdx.app != null) ? Gdx.app.getPreferences(PREFS_NAME) : null;
  private float masterVol =
      (prefs != null) ? prefs.getFloat(MASTER_VOLUME_KEY, DEFAULT_MASTER_VOL) : DEFAULT_MASTER_VOL;
  private float musicVol =
      (prefs != null) ? prefs.getFloat(MUSIC_VOLUME_KEY, DEFAULT_MUSIC_VOL) : DEFAULT_MUSIC_VOL;
  private float effectsVol =
      (prefs != null)
          ? prefs.getFloat(EFFECTS_VOLUME_KEY, DEFAULT_EFFECTS_VOL)
          : DEFAULT_EFFECTS_VOL;
  private boolean musicVolumeApplied = false;

  private Table root;
  private Table mainPanel;
  private Table settingsPanel;
  private Table audioPanel;

  private Label[] mainLabels;
  private Label[] settingsLabels;
  private Label[] audioLabels;
  private Slider masterSlider;
  private Slider musicSlider;
  private Slider effectsSlider;
  private Label masterValueLabel;
  private Label musicValueLabel;
  private Label effectsValueLabel;

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
    AudioSettings.setMasterVolume(masterVol);
    AudioSettings.setEffectsVolume(effectsVol);
    addActors();
    registerEventListeners();
  }

  private void addActors() {
    

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
    MenuState ownerState = panelStateFor(items);
    for (int i = 0; i < items.length; i++) {
      Label label = createLabel(items[i]);
      labelsOut[i] = label;
      Table row = new Table();
      row.add(label).pad(6f, 15f, 6f, 15f).left();
      addRowInteraction(row, ownerState, i, true);
      panel.add(row).left().padBottom(4f).fillX();
      panel.row();
    }
    applyUniformRowWidths(panel);
    return panel;
  }

  private MenuState panelStateFor(String[] items) {
    if (items == MAIN_ITEMS) {
      return MenuState.MAIN;
    }
    return MenuState.SETTINGS;
  }

  private void addRowInteraction(Table row, MenuState ownerState, int rowIndex, boolean clickConfirms) {
    row.addListener(
        new InputListener() {
          @Override
          public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
            if (state != ownerState) {
              return;
            }
            setCurrentIndex(rowIndex);
          }

          @Override
          public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
            if (state != ownerState || !clickConfirms) {
              return false;
            }
            setCurrentIndex(rowIndex);
            confirmSelection();
            return true;
          }
        });
  }

  private Table buildAudioPanel() {
    Table panel = new Table();
    panel.setBackground(skin.newDrawable("white", PANEL_COLOR));
    panel.pad(20f, 30f, 20f, 30f);
    panel.left();

    masterValueLabel = createLabel("");
    masterSlider = buildSlider(masterVol, masterValueLabel, this::onMasterChanged);
    audioLabels[0] = addSliderRow(panel, AUDIO_ITEMS[0], masterSlider, masterValueLabel, 0);

    musicValueLabel = createLabel("");
    musicSlider = buildSlider(musicVol, musicValueLabel, this::onMusicChanged);
    audioLabels[1] = addSliderRow(panel, AUDIO_ITEMS[1], musicSlider, musicValueLabel, 1);

    effectsValueLabel = createLabel("");
    effectsSlider = buildSlider(effectsVol, effectsValueLabel, this::onEffectsChanged);
    audioLabels[2] = addSliderRow(panel, AUDIO_ITEMS[2], effectsSlider, effectsValueLabel, 2);

    Label backLabel = createLabel(AUDIO_ITEMS[AUDIO_BACK_INDEX]);
    audioLabels[AUDIO_BACK_INDEX] = backLabel;
    Table backRow = new Table();
    backRow.add(backLabel).pad(6f, 15f, 6f, 15f).left();
    addRowInteraction(backRow, MenuState.AUDIO, AUDIO_BACK_INDEX, true);
    panel.add(backRow).left().padBottom(4f).fillX();

    updateVolumeLabel(masterValueLabel, masterVol);
    updateVolumeLabel(musicValueLabel, musicVol);
    updateVolumeLabel(effectsValueLabel, effectsVol);
    applyUniformRowWidths(panel);
    return panel;
  }

  private Slider buildSlider(float initialValue, Label valueLabel, java.util.function.Consumer<Float> onChange) {
    Slider slider = new Slider(0f, 1f, 0.01f, false, skin);
    slider.setValue(initialValue);
    slider.addListener(
        (Event event) -> {
          float value = slider.getValue();
          updateVolumeLabel(valueLabel, value);
          onChange.accept(value);
          return true;
        });
    return slider;
  }

  private Label addSliderRow(
      Table panel, String labelText, Slider slider, Label valueLabel, int rowIndex) {
    Label rowLabel = createLabel(labelText);
    Table row = new Table();
    row.add(rowLabel).width(150f).padRight(15f);
    row.add(slider).width(160f).padRight(10f);
    row.add(valueLabel);
    addRowInteraction(row, MenuState.AUDIO, rowIndex, false);
    panel.add(row).pad(6f, 15f, 6f, 15f).left();
    panel.row();
    return rowLabel;
  }

  private void updateVolumeLabel(Label label, float value) {
    label.setText(String.format("%.0f%%", value * 100));
  }

  private void onMasterChanged(float value) {
    masterVol = value;
    AudioSettings.setMasterVolume(masterVol);
    applyMusicVolume();
    savePrefs();
  }

  private void onMusicChanged(float value) {
    musicVol = value;
    applyMusicVolume();
    savePrefs();
  }

  private void onEffectsChanged(float value) {
    effectsVol = value;
    AudioSettings.setEffectsVolume(effectsVol);
    savePrefs();
  }

  private void savePrefs() {
    if (prefs == null) {
      return;
    }
    prefs.putFloat(MASTER_VOLUME_KEY, masterVol);
    prefs.putFloat(MUSIC_VOLUME_KEY, musicVol);
    prefs.putFloat(EFFECTS_VOLUME_KEY, effectsVol);
    prefs.flush();
  }

  private void applyMusicVolume() {
    Music music =
        ServiceLocator.getResourceService()
            .getAsset(PauseMenuComponent.BACKGROUND_MUSIC, Music.class);
    if (music != null) {
      music.setVolume(musicVol * masterVol);
      musicVolumeApplied = true;
    }
  }

  private void registerEventListeners() {
    entity.getEvents().addListener("navigateUp", this::navigateUp);
    entity.getEvents().addListener("navigateDown", this::navigateDown);
    entity.getEvents().addListener("navigateLeft", this::navigateLeft);
    entity.getEvents().addListener("navigateRight", this::navigateRight);
    entity.getEvents().addListener("confirmSelection", this::confirmSelection);
    entity.getEvents().addListener("escapePressed", this::handleEscape);
    entity.getEvents().addListener("leftPressed", this::onLeftPressed);
    entity.getEvents().addListener("leftReleased", this::onLeftReleased);
    entity.getEvents().addListener("rightPressed", this::onRightPressed);
    entity.getEvents().addListener("rightReleased", this::onRightReleased);
  }

  private void navigateUp() {
    int count = currentItemCount();
    setCurrentIndex((currentIndex() - 1 + count) % count);
  }

  private void navigateDown() {
    int count = currentItemCount();
    setCurrentIndex((currentIndex() + 1) % count);
  }
    private void onLeftPressed() {
    if (!leftHeld) {
      leftHeld = true;
      leftHoldStart = ServiceLocator.getTimeSource().getTime();
    }
  }

  private void onLeftReleased() {
    leftHeld = false;
  }

  private void onRightPressed() {
    if (!rightHeld) {
      rightHeld = true;
      rightHoldStart = ServiceLocator.getTimeSource().getTime();
      }
    }

  private void onRightReleased() {
    rightHeld = false;
  }

  private void navigateLeft() {
    adjustCurrentSlider(-VOLUME_STEP);
  }

  private void navigateRight() {
    adjustCurrentSlider(VOLUME_STEP);
  }

  /** Left/Right only do anything in the AUDIO panel, and only on a slider row (not Back). */
  private void adjustCurrentSlider(float delta) {
    if (state != MenuState.AUDIO) {
      return;
    }
    Slider slider =
        switch (audioIndex) {
          case 0 -> masterSlider;
          case 1 -> musicSlider;
          case 2 -> effectsSlider;
          default -> null;
        };
    if (slider != null) {
      slider.setValue(Math.max(0f, Math.min(1f, slider.getValue() + delta)));
    }
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
      case 2 -> {
        state = MenuState.SETTINGS;
        settingsIndex = 0;
        refreshPanels();
      }
      case 3 -> entity.getEvents().trigger("mainMenuClicked");
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
    if (audioIndex == AUDIO_BACK_INDEX) {
      state = MenuState.SETTINGS;
      refreshPanels();
    }
  }
  private void handleEscape() {
    switch (state) {
      case MAIN -> entity.getEvents().trigger("resumeClicked");
      case SETTINGS -> {
        state = MenuState.MAIN;
        refreshPanels();
      }
      case AUDIO -> {
        state = MenuState.SETTINGS;
        refreshPanels();
        }
      }
    }

  private void refreshPanels() {
    settingsPanel.setVisible(state != MenuState.MAIN);
    audioPanel.setVisible(state == MenuState.AUDIO);

    mainPanel.getColor().a = (state == MenuState.MAIN) ? 1f : INACTIVE_PANEL_ALPHA;
    settingsPanel.getColor().a = (state == MenuState.SETTINGS) ? 1f : INACTIVE_PANEL_ALPHA;
    audioPanel.getColor().a = (state == MenuState.AUDIO) ? 1f : INACTIVE_PANEL_ALPHA;

    refreshHighlights();
  }

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
      row.setBackground(selected ? skin.newDrawable("button", SELECTED_BG) : null);
    }
  }

  @Override
  public void draw(SpriteBatch batch) {
    long now = ServiceLocator.getTimeSource().getTime();
    if (leftHeld
      && now - leftHoldStart >= HOLD_INITIAL_DELAY_MS
      && now - leftLastRepeat >= HOLD_REPEAT_INTERVAL_MS) {
        adjustCurrentSlider(-VOLUME_STEP);
        leftLastRepeat = now;
      }
    if (rightHeld
      && now - rightHoldStart >= HOLD_INITIAL_DELAY_MS
      && now - rightLastRepeat >= HOLD_REPEAT_INTERVAL_MS) {
        adjustCurrentSlider(VOLUME_STEP);
        rightLastRepeat = now;
      }
    if (!musicVolumeApplied) {
      applyMusicVolume();
    }

    boolean isPaused = pauseMenu.isPaused();
    if (!isPaused) {
      leftHeld = false;
      rightHeld = false;
    }
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
  private void applyUniformRowWidths(Table panel) {
  float maxWidth = 0f;
  for (com.badlogic.gdx.scenes.scene2d.ui.Cell<?> cell : panel.getCells()) {
    Table row = (Table) cell.getActor();
    if (row != null) {
      maxWidth = Math.max(maxWidth, row.getPrefWidth());
}
  }
  for (com.badlogic.gdx.scenes.scene2d.ui.Cell<?> cell : panel.getCells()) {
    cell.width(maxWidth);
  }
  panel.invalidateHierarchy();
}

  @Override
  public void dispose() {
    root.clear();
    super.dispose();
  }
}