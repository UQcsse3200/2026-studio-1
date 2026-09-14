package com.csse3200.game.perks;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.csse3200.game.ui.UIComponent;
import java.util.List;

/**
 * Lists every registered perk: unlocked ones shown normally, locked ones dimmed with a progress
 * readout (e.g. "23/50"). Reuses the same dark-panel/left-aligned/cyan-highlight visual language as
 * PauseMenuDisplay/MainMenuDisplay for consistency.
 *
 * <p>Designed to be attached, independently, to more than one screen's ui entity (e.g. both
 * MainMenuScreen and MainGameScreen) - each instance owns its own PerkTrackerMenuComponent for
 * open/close state, but all instances read the same static PerkService, so progress is always
 * consistent regardless of which screen it's viewed from.
 */
public class PerkTrackerDisplay extends UIComponent {
  private static final Color PANEL_COLOR = new Color(0.03f, 0.06f, 0.04f, 0.95f);
  private static final Color UNLOCKED_TEXT = Color.CYAN;
  private static final Color LOCKED_TEXT = new Color(0.6f, 0.6f, 0.6f, 1f);
  private static final Color HIGHLIGHT_BG = new Color(0.15f, 0.35f, 0.55f, 0.9f);

  private PerkTrackerMenuComponent perkTrackerMenu;
  private Table root;
  private Table listPanel;
  private Label[] nameLabels;
  private Label[] progressLabels;
  private List<Perk> perks;
  private int selectedIndex = 0;
  private boolean wasOpen = false;

  @Override
  public void create() {
    super.create();
    perkTrackerMenu = entity.getComponent(PerkTrackerMenuComponent.class);
    perks = PerkService.getAllPerks();
    addActors();
    registerEventListeners();
  }

  private void addActors() {
    listPanel = new Table();
    listPanel.setBackground(skin.newDrawable("white", PANEL_COLOR));
    listPanel.pad(20f, 30f, 20f, 30f);
    listPanel.left();

    Label title = createLabel("PERKS", UNLOCKED_TEXT);
    listPanel.add(title).left().padBottom(15f);
    listPanel.row();

    nameLabels = new Label[perks.size()];
    progressLabels = new Label[perks.size()];
    for (int i = 0; i < perks.size(); i++) {
      Perk perk = perks.get(i);

      Table row = new Table();
      Label nameLabel = createLabel(perk.getName(), textColorFor(perk));
      Label progressLabel = createLabel(perk.getProgressText(), textColorFor(perk));
      nameLabels[i] = nameLabel;
      progressLabels[i] = progressLabel;

      row.add(nameLabel).width(220f).left();
      row.add(progressLabel).width(100f).right();
      listPanel.add(row).pad(6f, 15f, 6f, 15f).left().fillX();
      listPanel.row();
    }

    root = new Table();
    root.setFillParent(true);
    root.left();
    root.padLeft(60f);
    root.add(listPanel).top();
    root.setVisible(false);
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

  private void registerEventListeners() {
    entity.getEvents().addListener("perkNavigateUp", this::navigateUp);
    entity.getEvents().addListener("perkNavigateDown", this::navigateDown);
  }

  private void navigateUp() {
    if (perks.isEmpty()) {
      return;
    }
    selectedIndex = (selectedIndex - 1 + perks.size()) % perks.size();
    updateHighlight();
  }

  private void navigateDown() {
    if (perks.isEmpty()) {
      return;
    }
    selectedIndex = (selectedIndex + 1) % perks.size();
    updateHighlight();
  }

  private void updateHighlight() {
    for (int i = 0; i < nameLabels.length; i++) {
      boolean selected = i == selectedIndex;
      Table row = (Table) nameLabels[i].getParent();
      row.setBackground(selected ? skin.newDrawable("white", HIGHLIGHT_BG) : null);
    }
  }

  /**
   * Refreshes text/colour for every row from the latest PerkService state - call this whenever the
   * tracker is (re)opened, since perks may have unlocked since it was last shown.
   */
  private void refreshRows() {
    for (int i = 0; i < perks.size(); i++) {
      Perk perk = perks.get(i);
      nameLabels[i].getStyle().fontColor = textColorFor(perk);
      progressLabels[i].getStyle().fontColor = textColorFor(perk);
      progressLabels[i].setText(perk.getProgressText());
    }
  }

  @Override
  public void draw(SpriteBatch batch) {
    boolean isOpen = perkTrackerMenu != null && perkTrackerMenu.isOpen();
    root.setVisible(isOpen);

    if (isOpen && !wasOpen) {
      selectedIndex = 0;
      refreshRows();
      updateHighlight();
    }
    wasOpen = isOpen;
  }

  @Override
  public void dispose() {
    root.clear();
    super.dispose();
  }
}
