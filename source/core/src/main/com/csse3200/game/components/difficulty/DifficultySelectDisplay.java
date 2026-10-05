package com.csse3200.game.components.difficulty;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.csse3200.game.ui.UIComponent;

public class DifficultySelectDisplay extends UIComponent {
  private static final float Z_INDEX = 2f;

  // Same look as the main menu

  private static final float LEFT_PANEL_WIDTH_FRACTION = 0.30f;
  private static final float MENU_ITEM_FONT_SCALE = 1f;
  private static final float HEADING_FONT_SCALE = 0.8f;
  private static final float HINT_FONT_SCALE = 0.8f;

  static final String[] MENU_ITEMS = {"Easy", "Normal", "Hard", "Back"};
  static final String[] EVENTS = {"easy", "normal", "hard", "back"};
  private static final String[] DESCRIPTIONS = {
    "A gentler run. Enemies are weaker and go down faster.",
    "The standard experience, as the game is meant to be played.",
    "For experienced players. Enemies hit harder and take more hits to beat.",
    "Return to the main menu."
  };
  private static final int DEFAULT_INDEX = 1; // Normal

  Label[] buttons;
  Label descriptionLabel;
  int selectedIndex = DEFAULT_INDEX;

  // Same full-screen background image as the main menu - held as a field (unlike
  // MainMenuDisplay) so it can be disposed in dispose() rather than leaked.
  private Texture backgroundTexture;

  @Override
  public void create() {
    super.create();
    addActors();
    entity.getEvents().addListener("navigateUp", this::navigateUp);
    entity.getEvents().addListener("navigateDown", this::navigateDown);
    entity.getEvents().addListener("confirmSelection", this::confirmSelection);
  }

  private void addActors() {
    float screenWidth = Gdx.graphics.getWidth();
    float screenHeight = Gdx.graphics.getHeight();
    float panelWidth = screenWidth * LEFT_PANEL_WIDTH_FRACTION;

    backgroundTexture = new Texture(Gdx.files.internal("images/ui/main-menu-bg.png"));
    Image background = new Image(backgroundTexture);
    background.setFillParent(true);
    stage.addActor(background);

    // Left panel: the options
    Table optionsPanel = new Table();
    optionsPanel.center();

    buttons = new Label[MENU_ITEMS.length];
    for (int i = 0; i < MENU_ITEMS.length; i++) {
      Label label = createMenuItemLabel(MENU_ITEMS[i]);
      buttons[i] = label;
      Table row = new Table();
      row.setBackground(skin.getDrawable("button"));
      row.add(label).pad(8f, 20f, 8f, 20f).left().expandX();
      row.setTouchable(Touchable.enabled); // the whole frame responds, not just the label pixels
      addRowInteraction(row, i);
      optionsPanel.add(row).width(panelWidth * 0.75f).padBottom(4f);
      optionsPanel.row();
    }

    // Right side: heading (directly on the background image, like the main menu's title), then
    // the description and controls hint sharing one dark panel for guaranteed contrast.
    Label heading = new Label("Choose Difficulty", skin, "title");
    heading.setFontScale(HEADING_FONT_SCALE);

    descriptionLabel = createLabel("", 1f);
    descriptionLabel.setWrap(true);

    Label hint = createLabel("Up/Down: select   Enter: confirm   Esc: back", HINT_FONT_SCALE);

    Table darkPanel = new Table();
    darkPanel.setBackground(skin.getDrawable("window"));
    darkPanel.add(descriptionLabel).width(screenWidth * 0.40f).pad(20f).row();
    darkPanel.add(hint).padLeft(20f).padRight(20f).padBottom(20f).left();

    Table rightSide = new Table();
    rightSide.add(heading).padBottom(30f).row();
    rightSide.add(darkPanel);

    Table root = new Table();
    root.setFillParent(true);
    root.left().top();
    root.add(optionsPanel).width(panelWidth).height(screenHeight);
    root.add(rightSide).expand();
    stage.addActor(root);

    updateHighlight();
  }

  private Label createLabel(String text, float scale) {
    Label label = new Label(text, skin);
    label.setFontScale(scale);
    return label;
  }

  /** Menu item labels use the "subtitle" style, matching MainMenuDisplay's own menu items. */
  private Label createMenuItemLabel(String text) {
    Label label = new Label(text, skin, "subtitle");
    label.setFontScale(MENU_ITEM_FONT_SCALE);
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
      Table row = (Table) buttons[i].getParent();
      row.setBackground(skin.getDrawable(selected ? "button-pressed" : "button"));
    }
    descriptionLabel.setText(DESCRIPTIONS[selectedIndex]);
  }

  private void confirmSelection() {
    entity.getEvents().trigger(EVENTS[selectedIndex]);
  }

  @Override
  public void draw(SpriteBatch batch) {
    // Drawn by the stage
  }

  @Override
  public float getZIndex() {
    return Z_INDEX;
  }

  @Override
  public void dispose() {
    backgroundTexture.dispose();
    super.dispose();
  }
}
