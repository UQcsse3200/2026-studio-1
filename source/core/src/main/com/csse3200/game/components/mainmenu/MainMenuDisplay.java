package com.csse3200.game.components.mainmenu;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.ui.Cell;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.csse3200.game.ui.UIComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MainMenuDisplay extends UIComponent {
  private static final Logger logger = LoggerFactory.getLogger(MainMenuDisplay.class);
  private static final float Z_INDEX = 2f;

  private static final Color PANEL_COLOR = new Color(0f, 0f, 0f, 0.5f);
  private static final Color SELECTED_BG = new Color(0.15f, 0.35f, 0.55f, 0.9f);
  private static final Color SELECTED_TEXT = Color.CYAN;
  private static final Color UNSELECTED_TEXT = Color.WHITE;
  private static final float TITLE_SPLIT_FRACTION = 0.55f;
  private static final float MENU_ITEM_FONT_SCALE = 1.6f;

  private static final String[] MENU_ITEMS = {"Start", "Load", "Settings", "Perks", "Exit"};

  Label[] buttons;
  int selectedIndex = 0;

  @Override
  public void create() {
    super.create();
    addActors();
    registerEventListeners();
  }

  private void addActors() {
    Image background = new Image(new Texture(Gdx.files.internal("images/ui/main-menu-bg.png")));
    background.setFillParent(true);
    stage.addActor(background);

    Table titleStack = buildTitleStack();

    Table optionsPanel = new Table();
    optionsPanel.center();

    buttons = new Label[MENU_ITEMS.length];
    for (int i = 0; i < MENU_ITEMS.length; i++) {
      Label label = createLabel(MENU_ITEMS[i]);
      buttons[i] = label;
      Table row = new Table();
      row.setBackground(skin.getDrawable("button"));
      row.add(label).pad(12f, 25f, 12f, 25f).center();
      addRowInteraction(row, i);
      optionsPanel.add(row).center().padBottom(8f).fillX();
      optionsPanel.row();
    }
    float screenWidth = Gdx.graphics.getWidth();
    float screenHeight = Gdx.graphics.getHeight();
    Table root = new Table();
    root.setFillParent(true);
    root.center();

    root.add(titleStack).width(screenWidth * 0.45f).center().padRight(50f);

    root.add(optionsPanel).width(screenWidth * 0.28f).center().padLeft(40f);

    stage.addActor(root);

    updateHighlight();
  }

  private Table buildTitleStack() {
    Table titleStack = new Table();

    Label title = new Label("ASCENT", skin, "title");
    Label subtitle = new Label("OF THE MORTAL", skin, "title");

    title.setFontScale(1.2f);
    subtitle.setFontScale(0.8f);

    titleStack.add(title).center().row();
    titleStack.add(subtitle).center().padTop(8f);

    return titleStack;
  }

  private Label createLabel(String text) {
    Label label = new Label(text, skin, "subtitle");
    label.setFontScale(1.15f);
    return label;
  }

  private void addRowInteraction(Table row, int rowIndex) {
    row.addListener(
        new InputListener() {
          @Override
          public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
            selectedIndex = rowIndex;
            updateHighlight();
          }

          @Override
          public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
            selectedIndex = rowIndex;
            confirmSelection();
            return true;
          }
        });
  }

  private void applyUniformRowWidths(Table panel, float width) {
    for (Cell<?> cell : panel.getCells()) {
      cell.width(width);
    }
    panel.invalidateHierarchy();
  }

  private void registerEventListeners() {
    entity.getEvents().addListener("navigateUp", this::navigateUp);
    entity.getEvents().addListener("navigateDown", this::navigateDown);
    entity.getEvents().addListener("confirmSelection", this::confirmSelection);
  }

  void navigateUp() {
    selectedIndex = (selectedIndex - 1 + buttons.length) % buttons.length;
    updateHighlight();
  }

  void navigateDown() {
    selectedIndex = (selectedIndex + 1) % buttons.length;
    updateHighlight();
  }

  void updateHighlight() {
    for (int i = 0; i < buttons.length; i++) {
      boolean selected = i == selectedIndex;

      Label label = buttons[i];
      Table row = (Table) label.getParent();

      row.setBackground(skin.getDrawable(selected ? "button-pressed" : "button"));

      label.getStyle().fontColor = Color.WHITE;
    }
  }

  private void confirmSelection() {
    updateHighlight();
    switch (selectedIndex) {
      case 0 -> entity.getEvents().trigger("start");
      case 1 -> entity.getEvents().trigger("load");
      case 2 -> entity.getEvents().trigger("settings");
      case 3 -> entity.getEvents().trigger("perks");
      case 4 -> entity.getEvents().trigger("exit");
      default -> {}
    }
  }

  @Override
  public void draw(SpriteBatch batch) {}

  @Override
  public float getZIndex() {
    return Z_INDEX;
  }

  @Override
  public void dispose() {
    super.dispose();
  }
}
