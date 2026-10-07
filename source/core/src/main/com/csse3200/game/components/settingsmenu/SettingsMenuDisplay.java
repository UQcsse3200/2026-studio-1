package com.csse3200.game.components.settingsmenu;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics.DisplayMode;
import com.badlogic.gdx.Graphics.Monitor;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Event;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Array;
import com.csse3200.game.GdxGame;
import com.csse3200.game.GdxGame.ScreenType;
import com.csse3200.game.files.UserSettings;
import com.csse3200.game.files.UserSettings.DisplaySettings;
import com.csse3200.game.pausemenu.KeybindSettings;
import com.csse3200.game.services.ServiceLocator;
import com.csse3200.game.ui.UIComponent;
import com.csse3200.game.utils.StringDecorator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SettingsMenuDisplay extends UIComponent {
  private static final Logger logger = LoggerFactory.getLogger(SettingsMenuDisplay.class);
  private static final String[] KEYBIND_ACTIONS = {
    "moveLeft",
    "moveRight",
    "moveDown",
    "jump",
    "dash",
    "slide",
    "attack",
    "specialAttack",
    "areaAttack",
    "bribe",
    "dropItem",
    "equipShield",
    "interact",
    "toggleQuestMenu",
    "toggleTutorial",
    "crouch",
    "timeFreeze",
    "pause",
    "hotbarSlot1",
    "hotbarSlot2",
    "hotbarSlot3",
    "hotbarSlot4",
    "hotbarSlot5"
  };
  private static final String[] KEYBIND_LABELS = {
    "Move Left",
    "Move Right",
    "Move Down",
    "Jump",
    "Dash",
    "Slide",
    "Attack",
    "Special Attack",
    "Area Attack",
    "Bribe",
    "Drop Item",
    "Equip Shield",
    "Interact",
    "Toggle Quest Menu",
    "Toggle Tutorial",
    "Crouch",
    "Time Freeze",
    "Pause",
    "Hotbar Slot 1",
    "Hotbar Slot 2",
    "Hotbar Slot 3",
    "Hotbar Slot 4",
    "Hotbar Slot 5"
  };
  private final GdxGame game;
  private Table rootTable;
  private Table settingsTable;
  private Table keybindsTable;
  private Stack contentStack;
  private TextField fpsText;
  private Label screenTitle;
  private CheckBox fullScreenCheck;
  private CheckBox vsyncCheck;
  private Slider uiScaleSlider;
  private SelectBox<StringDecorator<DisplayMode>> displayModeSelect;
  private String capturingAction;
  private Label[] keybindLabels;

  public SettingsMenuDisplay(GdxGame game) {
    super();
    this.game = game;
  }

  @Override
  public void create() {
    super.create();
    addActors();
    entity.getEvents().addListener("exitSettings", this::exitMenu);
    entity.getEvents().addListener("keybindChanged", this::endKeyCapture);
    entity.getEvents().addListener("keybindCaptureCancelled", this::endKeyCapture);
  }

  private void endKeyCapture() {
    capturingAction = null;
    refreshKeybindLabels();
  }

  private void addActors() {
    Image background = new Image(new Texture(Gdx.files.internal("images/ui/main-menu-bg.png")));
    background.setFillParent(true);
    stage.addActor(background);

    Image overlay = new Image(skin.getDrawable("black"));
    overlay.setFillParent(true);
    overlay.setColor(1f, 1f, 1f, 0.60f);
    stage.addActor(overlay);

    screenTitle = new Label("SETTINGS", skin, "title");
    screenTitle.setFontScale(0.95f);

    settingsTable = makeSettingsTable();
    keybindsTable = makeKeybindsTable();
    keybindsTable.setVisible(false);

    contentStack = new Stack();
    contentStack.add(settingsTable);
    contentStack.add(keybindsTable);

    Table menuBtns = makeMenuBtns();

    Image panelBackground = new Image(skin.getDrawable("black"));
    panelBackground.setColor(1f, 1f, 1f, 0.78f);

    Stack panel = new Stack();
    panel.add(panelBackground);
    panel.add(contentStack);

    rootTable = new Table();
    rootTable.setFillParent(true);
    rootTable.center();

    rootTable.add(screenTitle).center().padBottom(18f).row();
    rootTable.add(panel).width(820f).height(500f).center().row();
    rootTable.add(menuBtns).center().padTop(22f);

    stage.addActor(rootTable);
  }

  private Table makeSettingsTable() {
    UserSettings.Settings settings = UserSettings.get();

    Label fpsLabel = new Label("FPS Cap", skin, "subtitle");
    fpsLabel.setFontScale(0.75f);

    fpsText = new TextField(Integer.toString(settings.fps), skin);
    fpsText.setAlignment(1);

    Label fullScreenLabel = new Label("Fullscreen", skin, "subtitle");
    fullScreenLabel.setFontScale(0.75f);

    fullScreenCheck = new CheckBox("", skin);
    fullScreenCheck.setChecked(settings.fullscreen);

    Label vsyncLabel = new Label("VSync", skin, "subtitle");
    vsyncLabel.setFontScale(0.75f);

    vsyncCheck = new CheckBox("", skin);
    vsyncCheck.setChecked(settings.vsync);

    Label uiScaleLabel = new Label("UI Scale", skin, "subtitle");
    uiScaleLabel.setFontScale(0.75f);

    uiScaleSlider = new Slider(0.2f, 2f, 0.1f, false, skin);
    uiScaleSlider.setValue(settings.uiScale);

    Label uiScaleValue = new Label(String.format("%.2fx", settings.uiScale), skin, "subtitle");
    uiScaleValue.setFontScale(0.7f);

    Label displayModeLabel = new Label("Resolution", skin, "subtitle");
    displayModeLabel.setFontScale(0.75f);

    displayModeSelect = new SelectBox<>(skin);

    Monitor selectedMonitor = Gdx.graphics.getMonitor();
    displayModeSelect.setItems(getDisplayModes(selectedMonitor));
    displayModeSelect.setSelected(getActiveMode(displayModeSelect.getItems()));

    Table table = new Table();
    table.pad(35f, 55f, 35f, 55f);

    table.defaults().height(65f);

    table.add(fpsLabel).width(220f).right().padRight(30f);
    table.add(fpsText).width(140f).height(55f).left();
    table.row();

    table.add(fullScreenLabel).width(220f).right().padRight(30f);
    table.add(fullScreenCheck).left();
    table.row();

    table.add(vsyncLabel).width(220f).right().padRight(30f);
    table.add(vsyncCheck).left();
    table.row();

    Table scaleTable = new Table();
    scaleTable.add(uiScaleSlider).width(220f).height(40f).left();
    scaleTable.add(uiScaleValue).width(80f).padLeft(18f).left();

    table.add(uiScaleLabel).width(220f).right().padRight(30f);
    table.add(scaleTable).left();
    table.row();

    table.add(displayModeLabel).width(220f).right().padRight(30f);
    table.add(displayModeSelect).width(360f).height(55f).left();

    uiScaleSlider.addListener(
        (Event event) -> {
          float value = uiScaleSlider.getValue();
          uiScaleValue.setText(String.format("%.2fx", value));
          return true;
        });

    return table;
  }

  private Table makeKeybindsTable() {
    Table outer = new Table();
    outer.pad(20f);

    ScrollPane.ScrollPaneStyle style = new ScrollPane.ScrollPaneStyle();
    ScrollPane scroll = new ScrollPane(buildKeybindRows(), style);
    scroll.setScrollingDisabled(true, false);
    scroll.setFadeScrollBars(false);
    scroll.setOverscroll(false, false);
    scroll.setForceScroll(false, true);
    outer.add(scroll).width(700f).height(420f).center().row();

    return outer;
  }

  private Table buildKeybindRows() {
    Table table = new Table();
    table.pad(10f);
    keybindLabels = new Label[KEYBIND_ACTIONS.length];

    for (int i = 0; i < KEYBIND_ACTIONS.length; i++) {
      final String action = KEYBIND_ACTIONS[i];
      Table row = new Table();
      row.setBackground(skin.getDrawable("button"));
      Label label = new Label("", skin);
      row.add(label).expandX().left().pad(8f);
      refreshKeybindLabel(label, action);
      keybindLabels[i] = label;

      row.addListener(
          new InputListener() {
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
              if (capturingAction == null) {
                beginKeyCapture(action, label);
              }
              // Consume the press either way, so a click while already capturing does nothing.
              return true;
            }
          });

      table.add(row).growX().padBottom(4f).row();
    }

    return table;
  }

  private void refreshKeybindLabels() {
    if (keybindLabels == null) {
      return;
    }
    for (int i = 0; i < KEYBIND_ACTIONS.length; i++) {
      refreshKeybindLabel(keybindLabels[i], KEYBIND_ACTIONS[i]);
    }
  }

  private void beginKeyCapture(String action, Label label) {
    capturingAction = action;
    label.setText(KEYBIND_LABELS[indexOfAction(action)] + ": Press any key...");
    SettingsInputComponent input = entity.getComponent(SettingsInputComponent.class);
    if (input != null) {
      input.startKeyCapture(action);
    }
  }

  private void refreshKeybindLabel(Label label, String action) {
    int keycode = KeybindSettings.getKey(action);
    String keyName = keycode == KeybindSettings.UNBOUND ? "Unbound" : Input.Keys.toString(keycode);
    label.setText(KEYBIND_LABELS[indexOfAction(action)] + ": " + keyName);
  }

  private int indexOfAction(String action) {
    for (int i = 0; i < KEYBIND_ACTIONS.length; i++) {
      if (KEYBIND_ACTIONS[i].equals(action)) {
        return i;
      }
    }
    return -1;
  }

  private void showSettings() {
    capturingAction = null;
    stage.setKeyboardFocus(null);
    settingsTable.setVisible(true);
    keybindsTable.setVisible(false);
    refreshKeybindLabels();
    rootTable.invalidateHierarchy();
  }

  private StringDecorator<DisplayMode> getActiveMode(Array<StringDecorator<DisplayMode>> modes) {
    DisplayMode active = Gdx.graphics.getDisplayMode();
    for (StringDecorator<DisplayMode> stringMode : modes) {
      DisplayMode mode = stringMode.object;
      if (active.width == mode.width
          && active.height == mode.height
          && active.refreshRate == mode.refreshRate) {
        return stringMode;
      }
    }
    return null;
  }

  private Array<StringDecorator<DisplayMode>> getDisplayModes(Monitor monitor) {
    DisplayMode[] displayModes = Gdx.graphics.getDisplayModes(monitor);
    Array<StringDecorator<DisplayMode>> arr = new Array<>();
    for (DisplayMode displayMode : displayModes) {
      arr.add(new StringDecorator<>(displayMode, this::prettyPrint));
    }
    return arr;
  }

  private String prettyPrint(DisplayMode displayMode) {
    return displayMode.width + "x" + displayMode.height + ", " + displayMode.refreshRate + "hz";
  }

  private Table makeMenuBtns() {
    TextButton exitBtn = new TextButton("Exit", skin);
    TextButton applyBtn = new TextButton("Apply", skin);
    TextButton keybindsBtn = new TextButton("Keybinds", skin);

    exitBtn.getLabel().setFontScale(0.8f);
    keybindsBtn.getLabel().setFontScale(0.8f);
    applyBtn.getLabel().setFontScale(0.8f);

    exitBtn.addListener(
        new ChangeListener() {
          @Override
          public void changed(ChangeEvent changeEvent, Actor actor) {
            logger.debug("Exit button clicked");
            exitMenu();
          }
        });

    applyBtn.addListener(
        new ChangeListener() {
          @Override
          public void changed(ChangeEvent changeEvent, Actor actor) {
            logger.debug("Apply button clicked");
            applyChanges();
          }
        });

    keybindsBtn.addListener(
        new ChangeListener() {
          @Override
          public void changed(ChangeEvent changeEvent, Actor actor) {
            if (keybindsTable.isVisible()) {
              logger.debug("Back button clicked");
              showSettings();
              keybindsBtn.setText("Keybinds");
              screenTitle.setText("SETTINGS");
            } else {
              logger.debug("Keybinds button clicked");
              settingsTable.setVisible(false);
              keybindsTable.setVisible(true);
              keybindsBtn.setText("Back");
              screenTitle.setText("KEYBINDS");
            }

            rootTable.invalidateHierarchy();
          }
        });

    Table table = new Table();

    table.add(exitBtn).width(220f).height(75f).padRight(20f);
    table.add(keybindsBtn).width(260f).height(75f).padRight(20f);
    table.add(applyBtn).width(220f).height(75f);

    return table;
  }

  private void applyChanges() {
    UserSettings.Settings settings = UserSettings.get();
    Integer fpsVal = parseOrNull(fpsText.getText());
    if (fpsVal != null) {
      settings.fps = fpsVal;
    }
    settings.fullscreen = fullScreenCheck.isChecked();
    settings.uiScale = uiScaleSlider.getValue();
    settings.displayMode = new DisplaySettings(displayModeSelect.getSelected().object);
    settings.vsync = vsyncCheck.isChecked();
    UserSettings.set(settings, true);
  }

  private void exitMenu() {
    game.setScreen(ScreenType.MAIN_MENU);
  }

  private Integer parseOrNull(String num) {
    try {
      return Integer.parseInt(num, 10);
    } catch (NumberFormatException e) {
      return null;
    }
  }

  @Override
  protected void draw(SpriteBatch batch) {
    // Nothing to draw here: the Stage renders this menu's actors.
  }

  @Override
  public void update() {
    stage.act(ServiceLocator.getTimeSource().getDeltaTime());
  }

  @Override
  public void dispose() {
    rootTable.clear();
    super.dispose();
  }
}
