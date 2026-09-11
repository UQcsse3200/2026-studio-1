package com.csse3200.game.components.mainmenu;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.csse3200.game.services.ServiceLocator;
import com.csse3200.game.ui.UIComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MainMenuDisplay extends UIComponent {
  private static final Logger logger = LoggerFactory.getLogger(MainMenuDisplay.class);
  private static final float Z_INDEX = 2f;

  private static final Color OVERLAY_COLOR = new Color(0f, 0f, 0f, 0.45f);
  private static final Color PANEL_COLOR = new Color(0.03f, 0.03f, 0.03f, 0.5f);
  private static final Color SELECTED_BG = new Color(0.15f, 0.35f, 0.55f, 0.9f);
  private static final Color SELECTED_TEXT = Color.CYAN;
  private static final Color UNSELECTED_TEXT = Color.WHITE;

  private static final String[] MENU_ITEMS = {"Start", "Load", "Settings", "Exit"};

  Label[] buttons;
  int selectedIndex = 0;

  @Override
  public void create() {
    super.create();
    addActors();
    registerEventListeners();
  }

  private void addActors() {
    Table overlay = new Table();
    overlay.setFillParent(true);
    overlay.setBackground(skin.newDrawable("white", OVERLAY_COLOR));
    stage.addActor(overlay);

    Image title =
        new Image(
            ServiceLocator.getResourceService()
                .getAsset("images/box_boy_title.png", Texture.class));

    Table panel = new Table();
    panel.setBackground(skin.newDrawable("white", PANEL_COLOR));
    panel.pad(20f, 30f, 20f, 30f);
    panel.left();

    buttons = new Label[MENU_ITEMS.length];
    for (int i = 0; i < MENU_ITEMS.length; i++) {
      Label label = createLabel(MENU_ITEMS[i]);
      buttons[i] = label;
      Table row = new Table();
      row.add(label).pad(6f, 15f, 6f, 15f).left();
      addRowInteraction(row, i);
      panel.add(row).left().padBottom(4f).fillX();
      panel.row();
    }
    ;

    Table root = new Table();
    root.setFillParent(true);
    root.add(title).padBottom(30f);
    root.row();
    root.add(panel);
    stage.addActor(root);

    updateHighlight();
  }

  private Label createLabel(String text) {
    Label label = new Label(text, skin);
    Label.LabelStyle style = new Label.LabelStyle(label.getStyle());
    style.fontColor = UNSELECTED_TEXT;
    label.setStyle(style);
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
      buttons[i].getStyle().fontColor = selected ? SELECTED_TEXT : UNSELECTED_TEXT;
      Table row = (Table) buttons[i].getParent();
      row.setBackground(selected ? skin.newDrawable("button", SELECTED_BG) : null);
    }
  }

  private void confirmSelection() {
    updateHighlight();
    switch (selectedIndex) {
      case 0 -> entity.getEvents().trigger("start");
      case 1 -> entity.getEvents().trigger("load");
      case 2 -> entity.getEvents().trigger("settings");
      case 3 -> entity.getEvents().trigger("exit");
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
