package com.csse3200.game.perks;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import com.csse3200.game.ui.UIComponent;
import java.util.List;

/**
 * Perk-selection screen shown after death, between the player clicking "Try Again" on the death
 * screen and the new player entity actually being created (see {@code MainGameScreen}). Lets the
 * player choose up to {@value PerkService#MAX_ACTIVE_PERKS} perks to have active for their next
 * life - each reward component (e.g. {@code ShieldComponent}) only applies its bonus if its perk is
 * active.
 *
 * <p>Card colours: grey = locked, white = unlocked but not active, gold = active. A cyan border
 * shows which card currently has keyboard focus (see {@link PerkSelectionInputComponent}); it's
 * independent of the fill colour, so focus and activation state are always both visible at once.
 * Selecting (click, or Enter/Space on a focused card) toggles: locked cards ignore it, white
 * activates (if there's room, per {@link PerkService#activate}), gold deactivates.
 */
public class PerkSelectionDisplay extends UIComponent {
  private static final Color SCRIM_COLOR = new Color(0f, 0f, 0f, 0.75f);
  private static final Color PANEL_COLOR = new Color(0.03f, 0.06f, 0.04f, 0.95f);
  private static final Color TITLE_TEXT_COLOR = Color.WHITE;
  private static final Color LOCKED_COLOR = Color.GRAY;
  private static final Color UNLOCKED_COLOR = Color.WHITE;
  private static final Color ACTIVE_COLOR = Color.GOLD;
  private static final Color CARD_TEXT_COLOR = Color.BLACK;
  private static final Color FOCUS_BORDER_COLOR = Color.CYAN;
  private static final Color NO_BORDER_COLOR = Color.CLEAR;
  private static final float CARD_SIZE = 150f;
  private static final float BORDER_PAD = 4f;

  private final Runnable onContinue;
  private Table rootTable;
  private Table[] cardFrames;
  private TextButton continueButton;
  private List<Perk> perks;

  /** 0..perks.size()-1 selects a card; perks.size() selects the Continue button. */
  private int selectedIndex = 0;

  /**
   * @param onContinue called once the player confirms their selection and the screen closes - this
   *     is where reviving the player actually happens (see {@code MainGameScreen})
   */
  public PerkSelectionDisplay(Runnable onContinue) {
    super();
    this.onContinue = onContinue;
  }

  @Override
  public void create() {
    super.create();

    perks = PerkService.getAllPerks();

    // Full-screen dim scrim behind the popup, so whatever's underneath (the Main Menu's title
    // art, the death screen's world view, etc.) is hidden rather than visually competing with
    // the cards - this was the actual bug before: there was no backdrop at all, just floating
    // cards directly on top of whatever screen this was shown over.
    rootTable = new Table();
    rootTable.setFillParent(true);
    rootTable.setBackground(skin.newDrawable("white", SCRIM_COLOR));

    Table popup = new Table(skin);
    popup.setBackground(skin.newDrawable("white", PANEL_COLOR));
    popup.pad(30f);

    Label title = createTitleLabel("CHOOSE YOUR PERKS");
    popup.add(title).padBottom(20f);
    popup.row();

    Table cardsRow = new Table();
    cardFrames = new Table[perks.size()];
    for (int i = 0; i < perks.size(); i++) {
      cardFrames[i] = buildCardFrame(perks.get(i), i);
      cardsRow.add(cardFrames[i]).pad(10f);
    }
    popup.add(cardsRow);
    popup.row();

    continueButton = new TextButton("Continue", skin);
    continueButton.addListener(
        new ChangeListener() {
          @Override
          public void changed(ChangeEvent event, Actor actor) {
            close();
          }
        });
    popup.add(continueButton).padTop(20f);

    rootTable.add(popup);
    stage.addActor(rootTable);
    rootTable.setVisible(false);

    registerEventListeners();
  }

  private Table buildCardFrame(Perk perk, int index) {
    Table card = new Table(skin);
    card.setBackground(skin.newDrawable("white", colorFor(perk)));
    card.pad(10f);

    Label nameLabel = createLabel(perk.getName());
    nameLabel.setWrap(true);
    nameLabel.setAlignment(Align.center);
    Label statusLabel = createLabel(perk.getProgressText());

    // Explicit width + wrap so a longer name (e.g. "Shield Enhancement") breaks onto a second
    // line within the card instead of being clipped at the card's edge.
    card.add(nameLabel).width(CARD_SIZE - 20f).padBottom(8f).row();
    card.add(statusLabel);

    card.addListener(
        new InputListener() {
          @Override
          public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
            selectedIndex = index;
            onConfirm();
            return true;
          }
        });

    // The border is a slightly larger frame behind the card, visible only as the padding gap -
    // same "background peeking through padding" trick used for highlights elsewhere in this
    // project, kept independent of the card's own state colour.
    Table frame = new Table();
    frame.setBackground(skin.newDrawable("white", NO_BORDER_COLOR));
    frame.add(card).width(CARD_SIZE).height(CARD_SIZE).pad(BORDER_PAD);
    return frame;
  }

  private Label createLabel(String text) {
    Label label = new Label(text, skin);
    Label.LabelStyle style = new Label.LabelStyle(label.getStyle());
    style.fontColor = CARD_TEXT_COLOR;
    label.setStyle(style);
    return label;
  }

  private Label createTitleLabel(String text) {
    Label label = new Label(text, skin, "title");
    Label.LabelStyle style = new Label.LabelStyle(label.getStyle());
    style.fontColor = TITLE_TEXT_COLOR;
    label.setStyle(style);
    return label;
  }

  private Color colorFor(Perk perk) {
    if (!perk.isUnlocked()) {
      return LOCKED_COLOR;
    }
    return perk.isActive() ? ACTIVE_COLOR : UNLOCKED_COLOR;
  }

  private void registerEventListeners() {
    entity.getEvents().addListener("perkSelectNavigateLeft", this::navigateLeft);
    entity.getEvents().addListener("perkSelectNavigateRight", this::navigateRight);
    entity.getEvents().addListener("perkSelectNavigateDown", this::navigateToContinue);
    entity.getEvents().addListener("perkSelectNavigateUp", this::navigateToCards);
    entity.getEvents().addListener("perkSelectConfirm", this::onConfirm);
  }

  void navigateLeft() {
    if (selectedIndex < perks.size()) {
      selectedIndex = (selectedIndex - 1 + perks.size()) % perks.size();
      updateFocus();
    }
  }

  void navigateRight() {
    if (selectedIndex < perks.size()) {
      selectedIndex = (selectedIndex + 1) % perks.size();
      updateFocus();
    }
  }

  void navigateToContinue() {
    selectedIndex = perks.size();
    updateFocus();
  }

  void navigateToCards() {
    if (selectedIndex == perks.size()) {
      selectedIndex = 0;
      updateFocus();
    }
  }

  /** Enter/Space, or a click, on whichever is currently selected. */
  private void onConfirm() {
    if (selectedIndex == perks.size()) {
      close();
      return;
    }

    Perk perk = perks.get(selectedIndex);
    if (!perk.isUnlocked()) {
      return;
    }
    if (perk.isActive()) {
      PerkService.deactivate(perk.getId());
    } else {
      PerkService.activate(perk.getId());
    }
    refreshCard(selectedIndex);
  }

  private void refreshCard(int index) {
    Table card = (Table) cardFrames[index].getChild(0);
    card.setBackground(skin.newDrawable("white", colorFor(perks.get(index))));
  }

  private void updateFocus() {
    for (int i = 0; i < cardFrames.length; i++) {
      cardFrames[i].setBackground(
          skin.newDrawable("white", i == selectedIndex ? FOCUS_BORDER_COLOR : NO_BORDER_COLOR));
    }
    continueButton.setColor(selectedIndex == perks.size() ? FOCUS_BORDER_COLOR : Color.WHITE);
  }

  public void show() {
    selectedIndex = 0;
    for (int i = 0; i < perks.size(); i++) {
      refreshCard(i);
    }
    updateFocus();
    rootTable.setVisible(true);
  }

  /**
   * @return whether this screen is currently visible - used by {@link PerkSelectionInputComponent}
   *     to gate keyboard input
   */
  public boolean isVisible() {
    return rootTable.isVisible();
  }

  private void close() {
    rootTable.setVisible(false);
    if (onContinue != null) {
      onContinue.run();
    }
  }

  @Override
  protected void draw(SpriteBatch batch) {
    // Drawing is handled by the Stage.
  }

  @Override
  public void dispose() {
    rootTable.clear();
    super.dispose();
  }
}
