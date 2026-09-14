package com.csse3200.game.perks;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.csse3200.game.GdxGame;
import com.csse3200.game.GdxGame.ScreenType;
import com.csse3200.game.ui.UIComponent;
import java.util.List;

public class PerksMenuDisplay extends UIComponent {
  private static final Color PANEL_COLOR = new Color(0.03f, 0.06f, 0.04f, 0.95f);
  private static final Color UNLOCKED_TEXT = Color.CYAN;
  private static final Color LOCKED_TEXT = new Color(0.6f, 0.6f, 0.6f, 1f);
  private static final Color SELECTED_TEXT = Color.YELLOW;
  private static final Color HIGHLIGHT_BG = new Color(0.15f, 0.35f, 0.55f, 0.9f);
  private static final String colour = "white";
  private final GdxGame game;
  private Label[] nameLabels;
  private Label backLabel;
  private List<Perk> perks;
  private int selectedIndex = 0;
  private int backIndex;

  public PerksMenuDisplay(GdxGame game) {
    super();
    this.game = game;
  }

  @Override
  public void create() {
    super.create();
    perks = PerkService.getAllPerks();
    backIndex = perks.size();
    addActors();
    registerEventListeners();
  }

  private void addActors() {
    Table listPanel = new Table();
    listPanel.setBackground(skin.newDrawable(colour, PANEL_COLOR));
    listPanel.pad(20f, 30f, 20f, 30f);
    listPanel.left();

    Label title = createLabel("PERKS", UNLOCKED_TEXT);
    listPanel.add(title).left().padBottom(15f);
    listPanel.row();

    nameLabels = new Label[perks.size()];
    for (int i = 0; i < perks.size(); i++) {
      Perk perk = perks.get(i);

      Table row = new Table();
      Label nameLabel = createLabel(perk.getName(), textColorFor(perk));
      Label progressLabel = createLabel(perk.getProgressText(), textColorFor(perk));
      nameLabels[i] = nameLabel;

      row.add(nameLabel).width(220f).left();
      row.add(progressLabel).width(100f).right();
      addRowInteraction(row, i);
      listPanel.add(row).pad(6f, 15f, 6f, 15f).left().fillX();
      listPanel.row();
    }

    backLabel = createLabel("Back", LOCKED_TEXT);
    Table backRow = new Table();
    backRow.add(backLabel).pad(6f, 15f, 6f, 15f).left();
    addRowInteraction(backRow, backIndex);
    listPanel.add(backRow).left().padTop(10f).fillX();

    Table root = new Table();
    root.setFillParent(true);
    root.left();
    root.padLeft(60f);
    root.add(listPanel).top();
    stage.addActor(root);

    updateHighlight();
  }

  private Label createLabel(String text, Color color) {
    Label label = new Label(text, skin);
    Label.LabelStyle style = new Label.LabelStyle(label.getStyle());
    style.fontColor = color;
    label.setStyle(style);
    return label;
  }

  private Color textColorFor(Perk perk) {
    return perk.isUnlocked() ? UNLOCKED_TEXT : LOCKED_TEXT;
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
    entity.getEvents().addListener("perkNavigateUp", this::navigateUp);
    entity.getEvents().addListener("perkNavigateDown", this::navigateDown);
    entity.getEvents().addListener("confirmSelection", this::confirmSelection);
    entity.getEvents().addListener("escapePressed", this::exitMenu);
  }

  private void navigateUp() {
    int count = backIndex + 1;
    selectedIndex = (selectedIndex - 1 + count) % count;
    updateHighlight();
  }

  private void navigateDown() {
    int count = backIndex + 1;
    selectedIndex = (selectedIndex + 1) % count;
    updateHighlight();
  }

  private void confirmSelection() {
    if (selectedIndex == backIndex) {
      exitMenu();
    }
    // Perk rows are informational only - selecting one does nothing further.
  }

  private void exitMenu() {
    game.setScreen(ScreenType.MAIN_MENU);
  }

  private void updateHighlight() {
    for (int i = 0; i < nameLabels.length; i++) {
      boolean selected = i == selectedIndex;
      Table row = (Table) nameLabels[i].getParent();
      row.setBackground(selected ? skin.newDrawable(colour, HIGHLIGHT_BG) : null);
      nameLabels[i].getStyle().fontColor = selected ? SELECTED_TEXT : textColorFor(perks.get(i));
    }
    boolean backSelected = selectedIndex == backIndex;
    backLabel.getStyle().fontColor = backSelected ? SELECTED_TEXT : LOCKED_TEXT;
    Table backRow = (Table) backLabel.getParent();
    backRow.setBackground(backSelected ? skin.newDrawable(colour, HIGHLIGHT_BG) : null);
  }

  @Override
  public void draw(SpriteBatch batch) {
    // draw is handled by the stage
  }
}
