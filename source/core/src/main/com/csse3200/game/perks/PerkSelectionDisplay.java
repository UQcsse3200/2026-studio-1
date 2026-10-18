package com.csse3200.game.perks;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;
import com.csse3200.game.ui.UIComponent;
import java.util.ArrayList;
import java.util.List;

/**
 * Perk-selection UI shared by the main-menu perks screen and the in-game death overlay. Both
 * variants use the same dark fantasy cards. The main-menu variant uses a lighter scrim, while the
 * death overlay darkens the game view to keep the player's attention on the selection.
 */
public class PerkSelectionDisplay extends UIComponent {
  private static final Color TITLE_TEXT_COLOR = new Color(0.95f, 0.85f, 0.55f, 1f);
  private static final Color FOCUS_BORDER_COLOR = new Color(0.12f, 0.92f, 0.85f, 1f);
  private static final float CARD_SIZE = 200f;
  private static final float CARD_HEIGHT = 286f;
  private static final float BORDER_PAD = 5f;
  private static final String WHITE = "white";

  private static final String[] PERK_TEXTURE_PATHS = {
    "images/ui/perks/shield-enhancement.png",
    "images/ui/perks/max-health.png",
    "images/ui/perks/time-freeze.png",
    "images/ui/perks/loot-drop.png"
  };

  private final Runnable onContinue;
  private final boolean mainMenuStyle;
  private Table rootTable;
  private Table[] cardFrames;
  private TextButton continueButton;
  private List<Perk> perks;
  private List<Texture> perkTextures;
  private boolean ownsPerkTextures;

  /** 0..perks.size()-1 selects a card; perks.size() selects Continue. */
  private int selectedIndex = 0;

  public PerkSelectionDisplay(Runnable onContinue) {
    this(onContinue, false);
  }

  public PerkSelectionDisplay(Runnable onContinue, boolean mainMenuStyle) {
    super();
    this.onContinue = onContinue;
    this.mainMenuStyle = mainMenuStyle;
  }

  public PerkSelectionDisplay(
      Runnable onContinue, boolean mainMenuStyle, List<Texture> perkTextures) {
    this(onContinue, mainMenuStyle);
    this.perkTextures = perkTextures;
  }

  @Override
  public void create() {
    super.create();
    perks = PerkService.getAllPerks();
    loadPerkTexturesIfNeeded();

    rootTable = new Table();
    rootTable.setFillParent(true);
    rootTable.setBackground(
        skin.newDrawable(
            WHITE,
            mainMenuStyle
                ? new Color(0f, 0f, 0f, 0.20f)
                : new Color(0.008f, 0.018f, 0.018f, 0.64f)));

    Table popup = new Table(skin);
    popup.setBackground(
        skin.newDrawable(
            WHITE,
            mainMenuStyle
                ? new Color(0.025f, 0.045f, 0.035f, 0.67f)
                : new Color(0.025f, 0.045f, 0.035f, 0.88f)));
    popup.pad(30f);
    popup.defaults().space(10f);

    Label title = createTitleLabel("CHOOSE YOUR PERKS");
    popup.add(title).padBottom(20f);
    popup.row();

    Table cardsRow = new Table();
    cardFrames = new Table[perks.size()];
    for (int i = 0; i < perks.size(); i++) {
      cardFrames[i] = buildCardFrame(perks.get(i), i);
      cardsRow.add(cardFrames[i]).pad(8f);
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
    popup.add(continueButton).padTop(18f);

    rootTable.add(popup);
    stage.addActor(rootTable);
    rootTable.setVisible(false);

    registerEventListeners();
  }

  /** Load icons for the death overlay; the standalone menu supplies its own shared textures. */
  private void loadPerkTexturesIfNeeded() {
    if (perkTextures != null) {
      return;
    }

    perkTextures = new ArrayList<>();
    ownsPerkTextures = true;
    for (String path : PERK_TEXTURE_PATHS) {
      perkTextures.add(new Texture(Gdx.files.internal(path)));
    }
  }

  /** Builds the same fantasy-style card in both the main menu and death overlay. */
  private Table buildCardFrame(Perk perk, int index) {
    Table frame = new Table(skin);
    Table card = new Table(skin);

    frame.setBackground(skin.newDrawable(WHITE, borderColorFor(perk, index)));
    frame.pad(3f);
    card.setBackground(skin.newDrawable(WHITE, panelColorFor(perk)));
    card.pad(12f);

    // Decorative top edge.
    Table accent = new Table();
    accent.setBackground(
        skin.newDrawable(
            WHITE,
            perk.isActive()
                ? new Color(0.95f, 0.69f, 0.28f, 1f)
                : new Color(0.18f, 0.58f, 0.54f, 1f)));
    card.add(accent).growX().height(3f).padBottom(10f).row();

    // Icon inset.
    if (perkTextures != null && index < perkTextures.size()) {
      Table iconPanel = new Table(skin);
      iconPanel.setBackground(skin.newDrawable(WHITE, new Color(0.10f, 0.14f, 0.15f, 1f)));
      Image icon = new Image(perkTextures.get(index));
      icon.setScaling(Scaling.fit);
      iconPanel.add(icon).size(68f, 68f).pad(5f);
      card.add(iconPanel).size(80f, 80f).padBottom(10f).row();
    }

    Label nameLabel =
        createCardLabel(
            perk.getName(),
            perk.isUnlocked()
                ? new Color(1f, 0.82f, 0.48f, 1f)
                : new Color(0.62f, 0.65f, 0.64f, 1f));
    nameLabel.setWrap(true);
    nameLabel.setAlignment(Align.center);
    card.add(nameLabel).width(CARD_SIZE - 34f).minHeight(42f).padBottom(8f).row();

    Label descriptionLabel =
        createCardLabel(perk.getDescription(), new Color(0.88f, 0.86f, 0.78f, 1f));
    descriptionLabel.setWrap(true);
    descriptionLabel.setAlignment(Align.center);
    card.add(descriptionLabel).width(CARD_SIZE - 34f).height(76f).padBottom(8f).row();

    Label statusLabel =
        createCardLabel(
            perk.getProgressText(),
            perk.isActive() ? new Color(1f, 0.78f, 0.32f, 1f) : new Color(0.47f, 0.88f, 0.78f, 1f));
    statusLabel.setAlignment(Align.center);

    Table badge = new Table(skin);
    badge.setBackground(skin.newDrawable(WHITE, new Color(0.09f, 0.14f, 0.14f, 1f)));
    badge.pad(5f, 9f, 5f, 9f);
    badge.add(statusLabel);
    card.add(badge).padTop(2f).row();

    frame.add(card).width(CARD_SIZE).height(CARD_HEIGHT).pad(BORDER_PAD);

    card.addListener(
        new InputListener() {
          @Override
          public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
            selectedIndex = index;
            onConfirm();
            updateFocus();
            return true;
          }
        });

    return frame;
  }

  private Color borderColorFor(Perk perk, int index) {
    if (selectedIndex == index) {
      return FOCUS_BORDER_COLOR;
    }
    if (perk.isActive()) {
      return new Color(0.88f, 0.62f, 0.24f, 1f);
    }
    if (perk.isUnlocked()) {
      return new Color(0.43f, 0.34f, 0.20f, 1f);
    }
    return new Color(0.23f, 0.28f, 0.28f, 1f);
  }

  private Color panelColorFor(Perk perk) {
    if (!perk.isUnlocked()) {
      return new Color(0.09f, 0.105f, 0.11f, 1f);
    }
    if (perk.isActive()) {
      return new Color(0.17f, 0.135f, 0.075f, 1f);
    }
    return new Color(0.055f, 0.08f, 0.085f, 1f);
  }

  private Label createCardLabel(String text, Color fontColor) {
    Label label = new Label(text, skin);
    Label.LabelStyle style = new Label.LabelStyle(label.getStyle());
    style.fontColor = fontColor;
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
    if (cardFrames == null || perks == null || index < 0 || index >= cardFrames.length) {
      return;
    }

    Perk perk = perks.get(index);
    Table frame = cardFrames[index];

    frame.setBackground(skin.newDrawable(WHITE, borderColorFor(perk, index)));

    // Some tests inject frames without the complete UI hierarchy.
    if (frame.getChildren().size == 0) {
      return;
    }

    Table card = (Table) frame.getChild(0);
    card.setBackground(skin.newDrawable(WHITE, panelColorFor(perk)));

    // The accent strip exists in fully constructed UI cards.
    if (card.getChildren().size == 0) {
      return;
    }

    Table accent = (Table) card.getChild(0);
    accent.setBackground(
        skin.newDrawable(
            WHITE,
            perk.isActive()
                ? new Color(0.95f, 0.69f, 0.28f, 1f)
                : new Color(0.18f, 0.58f, 0.54f, 1f)));
  }

  private void updateFocus() {
    if (cardFrames != null) {
      for (int i = 0; i < cardFrames.length; i++) {
        refreshCard(i);
      }
    }
    if (continueButton != null) {
      if (selectedIndex == perks.size()) {
        continueButton.setColor(Color.CYAN);
      } else {
        continueButton.setColor(Color.WHITE);
      }
    }
  }

  public void show() {
    if (rootTable == null) {
      return;
    }
    selectedIndex = 0;
    for (int i = 0; i < perks.size(); i++) {
      refreshCard(i);
    }
    updateFocus();
    rootTable.setVisible(true);
  }

  public boolean isVisible() {
    return rootTable != null && rootTable.isVisible();
  }

  private void close() {
    if (rootTable != null) {
      rootTable.setVisible(false);
    }
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
    if (rootTable != null) {
      rootTable.clear();
    }
    if (ownsPerkTextures && perkTextures != null) {
      for (Texture texture : perkTextures) {
        texture.dispose();
      }
      perkTextures.clear();
    }
    super.dispose();
  }
}
