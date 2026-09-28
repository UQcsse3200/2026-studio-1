package com.csse3200.game.components.player;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.Action;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.csse3200.game.components.player.GamblingCatalogs.CatalogId;
import com.csse3200.game.components.player.GamblingCatalogs.PrizeEntry;
import com.csse3200.game.components.player.GamblingCatalogs.SpinCatalog;
import java.util.ArrayList;
import java.util.List;

/**
 * Scene2D blind-box display used by ShopDisplay.
 *
 * <p>The class keeps the existing GamblingWheel name and public API for compatibility with
 * ShopDisplay.
 *
 * <p>The display shows the five possible prizes together with their drop rate (weight / sum of
 * weights). When a spin starts, a mystery bag shakes with growing intensity, bursts open with a
 * flash and confetti, and reveals the prize card. The winning slot below is then highlighted.
 *
 * <p>This class is presentation-only. It does not select, grant, or re-roll prizes.
 */
public class GamblingWheel extends Stack {

  private static final float DISPLAY_WIDTH = 440f;
  private static final float DISPLAY_HEIGHT = 300f;

  private static final float STAGE_WIDTH = 420f;
  private static final float STAGE_HEIGHT = 115f;

  private static final float BAG_SIZE = 76f;
  private static final float CARD_WIDTH = 220f;
  private static final float CARD_HEIGHT = 78f;

  private static final float SLOT_WIDTH = 80f;
  private static final float SLOT_HEIGHT = 96f;
  private static final float SLOT_GAP = 4f;

  private static final int SHAKE_COUNT = 10;
  private static final float SLOT_DIM_ALPHA = 0.45f;
  private static final float CLICK_LOCK_DELAY = 0.6f;

  private static final Color PANEL_COLOR = new Color(0.06f, 0.08f, 0.14f, 1f);
  private static final Color SLOT_COLOR = new Color(0.12f, 0.15f, 0.23f, 1f);
  private static final Color HIGHLIGHT_COLOR = new Color(1.0f, 0.72f, 0.18f, 1f);
  private static final Color TEXT_COLOR = new Color(1f, 1f, 1f, 1f);
  private static final Color MUTED_TEXT_COLOR = new Color(0.70f, 0.70f, 0.75f, 1f);
  private static final Color WIN_TEXT_COLOR = new Color(1.0f, 0.84f, 0.25f, 1f);

  private static final Color BAG_STANDARD = new Color(0.90f, 0.55f, 0.22f, 1f);
  private static final Color BAG_PREMIUM = new Color(0.65f, 0.35f, 0.95f, 1f);

  private static final Color RARITY_COMMON = new Color(0.45f, 0.52f, 0.62f, 1f);
  private static final Color RARITY_RARE = new Color(0.18f, 0.55f, 0.90f, 1f);
  private static final Color RARITY_EPIC = new Color(0.62f, 0.28f, 0.88f, 1f);
  private static final Color RARITY_LEGENDARY = new Color(0.95f, 0.65f, 0.08f, 1f);

  private static final Color[] CONFETTI_COLORS = {
    new Color(1f, 0.35f, 0.35f, 1f),
    new Color(1f, 0.85f, 0.25f, 1f),
    new Color(0.35f, 0.85f, 0.45f, 1f),
    new Color(0.35f, 0.65f, 1f, 1f),
    new Color(0.85f, 0.45f, 1f, 1f)
  };

  private final Label.LabelStyle labelStyle;

  private final List<Stack> slotStacks = new ArrayList<>();
  private final List<Image> slotHighlights = new ArrayList<>();
  private final List<Image> slotAccents = new ArrayList<>();
  private final List<Label> slotNameLabels = new ArrayList<>();
  private final List<Label> slotRateLabels = new ArrayList<>();
  private final List<Texture> textures = new ArrayList<>();

  private TextureRegionDrawable whiteDrawable;

  private Table root;
  private Table slotsTable;

  private Group stageGroup;
  private Group bagGroup;
  private Image bagImage;
  private Table card;
  private Label cardNameLabel;
  private Label cardRateLabel;
  private Image flash;

  private Label statusLabel;
  private Label resultLabel;

  private SpinCatalog catalog;
  private CatalogId catalogId;

  private Label hintLabel;
  private Runnable pendingFinish;
  private boolean awaitingClick;

  private boolean spinning;

  /**
   * Creates the blind-box display.
   *
   * @param labelStyle style used for all labels
   */
  public GamblingWheel(Label.LabelStyle labelStyle) {
    this.labelStyle = labelStyle;
    setSize(DISPLAY_WIDTH, DISPLAY_HEIGHT);
    build();

    // After the result is revealed, the display stays until the player clicks anywhere on it.
    addListener(
        new ClickListener() {
          @Override
          public void clicked(InputEvent event, float x, float y) {
            confirmResult();
          }
        });
  }

  // ---------------------------------------------------------------------------------------------
  // Construction
  // ---------------------------------------------------------------------------------------------

  private void build() {
    whiteDrawable = drawableOf(createTexture(2, 2, Color.WHITE));

    Image panelBackground = new Image(drawableOf(createTexture(2, 2, PANEL_COLOR)));
    add(panelBackground);

    root = new Table();
    root.setFillParent(true);
    add(root);

    statusLabel = new Label("BLIND BOX", labelStyle);
    statusLabel.setAlignment(Align.center);
    statusLabel.setColor(MUTED_TEXT_COLOR);
    statusLabel.setFontScale(1.0f);
    root.add(statusLabel).width(DISPLAY_WIDTH).height(26f).center().top().row();

    buildStage();
    root.add(stageGroup).size(STAGE_WIDTH, STAGE_HEIGHT).center().padTop(2f).row();

    buildSlots();
    root.add(slotsTable).center().padTop(6f).row();

    resultLabel = new Label("Choose a catalogue and spin!", labelStyle);
    resultLabel.setAlignment(Align.center);
    resultLabel.setColor(TEXT_COLOR);
    resultLabel.setFontScale(1.0f);
    resultLabel.setWrap(true);
    root.add(resultLabel).width(DISPLAY_WIDTH - 20f).height(34f).center().padTop(6f);
  }

  /** Builds the animated area: prize card (hidden), mystery bag and flash overlay. */
  private void buildStage() {
    stageGroup = new Group();
    stageGroup.setSize(STAGE_WIDTH, STAGE_HEIGHT);
    stageGroup.setTouchable(Touchable.disabled);

    // Prize card, revealed after the bag bursts.
    card = new Table();
    card.setBackground(whiteDrawable);
    card.setTransform(true);
    card.pad(6f);
    card.setSize(CARD_WIDTH, CARD_HEIGHT);
    card.setOrigin(CARD_WIDTH / 2f, CARD_HEIGHT / 2f);
    card.setPosition((STAGE_WIDTH - CARD_WIDTH) / 2f, 24f);
    card.setVisible(false);

    cardNameLabel = new Label("", labelStyle);
    cardNameLabel.setAlignment(Align.center);
    cardNameLabel.setColor(Color.WHITE);
    cardNameLabel.setFontScale(1.25f);
    cardNameLabel.setWrap(true);

    cardRateLabel = new Label("", labelStyle);
    cardRateLabel.setAlignment(Align.center);
    cardRateLabel.setColor(WIN_TEXT_COLOR);
    cardRateLabel.setFontScale(0.95f);

    card.add(cardNameLabel).width(CARD_WIDTH - 20f).expandY().center().row();
    card.add(cardRateLabel).center().padBottom(2f);

    // Mystery bag.
    bagGroup = new Group();
    bagGroup.setSize(BAG_SIZE, BAG_SIZE);
    bagGroup.setOrigin(BAG_SIZE / 2f, BAG_SIZE / 2f);

    bagImage = new Image(drawableOf(createBagTexture()));
    bagImage.setSize(BAG_SIZE, BAG_SIZE);
    bagImage.setColor(BAG_STANDARD);

    Label questionMark = new Label("?", labelStyle);
    questionMark.setAlignment(Align.center);
    questionMark.setColor(Color.WHITE);
    questionMark.setFontScale(2.2f);
    questionMark.setSize(BAG_SIZE, BAG_SIZE * 0.85f);

    bagGroup.addActor(bagImage);
    bagGroup.addActor(questionMark);

    // White flash used when the bag bursts open.
    flash = new Image(whiteDrawable);
    flash.setSize(STAGE_WIDTH, STAGE_HEIGHT);
    flash.setColor(1f, 1f, 1f, 0f);
    flash.setVisible(false);
    flash.setTouchable(Touchable.disabled);

    hintLabel = new Label("- Click to continue -", labelStyle);
    hintLabel.setAlignment(Align.center);
    hintLabel.setColor(MUTED_TEXT_COLOR);
    hintLabel.setFontScale(0.85f);
    hintLabel.setSize(STAGE_WIDTH, 20f);
    hintLabel.setPosition(0f, 0f);
    hintLabel.setVisible(false);

    stageGroup.addActor(hintLabel);
    stageGroup.addActor(card);
    stageGroup.addActor(bagGroup);
    stageGroup.addActor(flash);

    resetBag();
  }

  /** Builds the five slots showing prize name and drop rate. */
  private void buildSlots() {
    slotsTable = new Table();
    slotsTable.defaults().pad(SLOT_GAP / 2f);

    Texture slotTexture = createTexture(2, 2, SLOT_COLOR);
    Texture borderTexture =
        createBorderTexture((int) SLOT_WIDTH, (int) SLOT_HEIGHT, HIGHLIGHT_COLOR, 3);

    for (int i = 0; i < SpinCatalog.PRIZE_SLOT_COUNT; i++) {
      Stack slot = new Stack();
      slot.setSize(SLOT_WIDTH, SLOT_HEIGHT);

      Image background = new Image(drawableOf(slotTexture));

      Image highlight = new Image(drawableOf(borderTexture));
      highlight.setVisible(false);

      Table content = new Table();

      Image accent = new Image(whiteDrawable);
      accent.setColor(RARITY_COMMON);
      content.add(accent).width(SLOT_WIDTH).height(4f).top().row();

      Label nameLabel = new Label("?", labelStyle);
      nameLabel.setAlignment(Align.center);
      nameLabel.setColor(TEXT_COLOR);
      nameLabel.setFontScale(0.85f);
      nameLabel.setWrap(true);
      content.add(nameLabel).width(SLOT_WIDTH - 8f).expandY().center().row();

      Label rateLabel = new Label("", labelStyle);
      rateLabel.setAlignment(Align.center);
      rateLabel.setColor(WIN_TEXT_COLOR);
      rateLabel.setFontScale(1.05f);
      content.add(rateLabel).center().padBottom(4f);

      slot.add(background);
      slot.add(highlight);
      slot.add(content);

      slotsTable.add(slot).size(SLOT_WIDTH, SLOT_HEIGHT);

      slotStacks.add(slot);
      slotHighlights.add(highlight);
      slotAccents.add(accent);
      slotNameLabels.add(nameLabel);
      slotRateLabels.add(rateLabel);
    }
  }

  // ---------------------------------------------------------------------------------------------
  // Public API
  // ---------------------------------------------------------------------------------------------

  /**
   * Sets the active gambling catalogue.
   *
   * @param catalogId catalogue identifier
   * @param catalog prize catalogue
   */
  public void setCatalog(CatalogId catalogId, SpinCatalog catalog) {
    this.catalogId = catalogId;
    this.catalog = catalog;
    reset();
  }

  /**
   * Plays the blind-box opening animation for the predetermined prize.
   *
   * <p>The winning prize has already been selected by ShopComponent. This method only animates.
   *
   * @param prizeSlot predetermined winning slot, one-based
   * @param onFinished callback executed after the reveal animation
   */
  public void spinToSlot(int prizeSlot, Runnable onFinished) {
    if (spinning || catalog == null) {
      return;
    }
    if (prizeSlot < 1 || prizeSlot > SpinCatalog.PRIZE_SLOT_COUNT) {
      return;
    }
    PrizeEntry<?> winningPrize = catalog.getPrize(prizeSlot);
    if (winningPrize == null) {
      return;
    }

    spinning = true;

    clearActions();
    clearHighlights();
    setSlotsAlpha(1f);
    card.clearActions();
    card.setVisible(false);
    resetBag();
    bagGroup.clearActions();

    resultLabel.setColor(TEXT_COLOR);
    resultLabel.setFontScale(1.0f);
    resultLabel.setText("Opening blind box...");
    statusLabel.setText("OPENING...");

    dimSlots();

    bagGroup.addAction(buildBagSequence(prizeSlot, winningPrize, onFinished));
  }

  /** Returns whether the animation is currently running. */
  public boolean isSpinning() {
    return spinning;
  }

  /** Returns the currently selected catalogue. */
  public SpinCatalog getCatalog() {
    return catalog;
  }

  /** Returns the currently selected catalogue ID. */
  public CatalogId getCatalogId() {
    return catalogId;
  }

  /** Resets the display to its idle state. */
  public void reset() {
    clearActions();
    for (Stack slot : slotStacks) {
      slot.clearActions();
      slot.setScale(1f);
    }
    card.clearActions();
    card.setVisible(false);
    flash.clearActions();
    flash.setVisible(false);

    spinning = false;
    awaitingClick = false;
    pendingFinish = null;
    hintLabel.clearActions();
    hintLabel.setVisible(false);
    clearHighlights();
    setSlotsAlpha(1f);
    resetBag();
    startBagIdle();

    resultLabel.setColor(TEXT_COLOR);
    resultLabel.setFontScale(1.0f);

    boolean premium = catalogId == CatalogId.PREMIUM;
    statusLabel.setText(premium ? "PREMIUM BLIND BOX" : "STANDARD BLIND BOX");
    bagImage.setColor(premium ? BAG_PREMIUM : BAG_STANDARD);

    if (catalog == null) {
      for (int i = 0; i < slotNameLabels.size(); i++) {
        slotNameLabels.get(i).setText("?");
        slotRateLabels.get(i).setText("");
        slotAccents.get(i).setColor(RARITY_COMMON);
      }
      resultLabel.setText(catalogId == null ? "Choose a catalogue and spin!" : "No prizes.");
      return;
    }

    updatePrizeLabels();
    resultLabel.setText("Ready to spin!");
  }

  /** Releases textures created by this widget. */
  public void dispose() {
    for (Texture texture : textures) {
      if (texture != null) {
        texture.dispose();
      }
    }
    textures.clear();
  }

  // ---------------------------------------------------------------------------------------------
  // Animation
  // ---------------------------------------------------------------------------------------------

  /** Shake (getting stronger) -> swell -> burst -> reveal. */
  private Action buildBagSequence(int prizeSlot, PrizeEntry<?> prize, Runnable onFinished) {
    List<Action> a = new ArrayList<>();

    // Small hop + grow to start.
    a.add(
        Actions.parallel(
            Actions.moveBy(0f, 8f, 0.15f, Interpolation.pow2Out),
            Actions.scaleTo(1.1f, 1.1f, 0.15f)));

    // Shake: amplitude increases, each swing gets faster.
    for (int i = 0; i < SHAKE_COUNT; i++) {
      float angle = (i % 2 == 0 ? 1f : -1f) * (8f + i * 2.5f);
      float duration = Math.max(0.045f, 0.10f - i * 0.006f);
      a.add(Actions.rotateTo(angle, duration));
    }
    a.add(Actions.rotateTo(0f, 0.06f));

    // Swell right before bursting.
    a.add(Actions.scaleTo(1.4f, 1.4f, 0.2f, Interpolation.swingOut));
    a.add(Actions.delay(0.08f));

    // Burst.
    a.add(Actions.parallel(Actions.scaleTo(2.0f, 2.0f, 0.14f), Actions.alpha(0f, 0.14f)));
    a.add(Actions.run(() -> revealPrize(prizeSlot, prize, onFinished)));

    return Actions.sequence(a.toArray(new Action[0]));
  }

  /** Flash + confetti + prize card pop, then highlight the winning slot. */
  private void revealPrize(int prizeSlot, PrizeEntry<?> prize, Runnable onFinished) {
    int winnerIndex = prizeSlot - 1;
    float percent = percentOf(prize);
    Color rarity = rarityColor(percent);

    bagGroup.setVisible(false);

    flash.clearActions();
    flash.setColor(1f, 1f, 1f, 0.95f);
    flash.setVisible(true);
    flash.addAction(Actions.sequence(Actions.fadeOut(0.4f), Actions.visible(false)));

    cardNameLabel.setText(getPrizeShortName(prize));
    cardRateLabel.setText(formatRate(percent) + " chance");
    card.setColor(rarity);
    card.clearActions();
    card.setVisible(true);
    card.setScale(0f);
    card.setRotation(-15f);
    card.addAction(
        Actions.parallel(
            Actions.scaleTo(1f, 1f, 0.5f, Interpolation.swingOut),
            Actions.rotateTo(0f, 0.5f, Interpolation.pow2Out)));

    // Rarer prize -> more confetti.
    spawnConfetti(rarity, percent <= 15f ? 44 : 26);

    highlightSlot(winnerIndex);
    dimSlotsExcept(winnerIndex);

    statusLabel.setText("WINNER");
    resultLabel.setText("You won: " + getPrizeShortName(prize));
    resultLabel.setColor(WIN_TEXT_COLOR);
    resultLabel.setFontScale(1.2f);

    Stack winnerSlot = slotStacks.get(winnerIndex);
    winnerSlot.setTransform(true);
    winnerSlot.setOrigin(SLOT_WIDTH / 2f, SLOT_HEIGHT / 2f);
    winnerSlot.addAction(
        Actions.sequence(
            Actions.scaleTo(1.15f, 1.15f, 0.15f, Interpolation.pow2Out),
            Actions.scaleTo(1f, 1f, 0.15f)));

    // Hold the result on screen. After a short lock (so the reveal animation is not skipped by
    // an accidental double click) the display waits for the player to click.
    pendingFinish = onFinished;
    addAction(
        Actions.sequence(
            Actions.delay(CLICK_LOCK_DELAY),
            Actions.run(
                () -> {
                  awaitingClick = true;
                  hintLabel.getColor().a = 1f;
                  hintLabel.setVisible(true);
                  hintLabel.addAction(
                      Actions.forever(
                          Actions.sequence(
                              Actions.alpha(0.3f, 0.6f, Interpolation.sine),
                              Actions.alpha(1f, 0.6f, Interpolation.sine))));
                })));
  }

  /**
   * Confirms the revealed result (what a click on the display does). Returns the display to idle
   * and runs the completion callback. Ignored unless the result is currently waiting for a click.
   */
  public void confirmResult() {
    if (!awaitingClick) {
      return;
    }
    Runnable callback = pendingFinish;
    reset();
    if (callback != null) {
      callback.run();
    }
  }

  /** Returns whether the result is revealed and waiting for the player to click. */
  public boolean isAwaitingClick() {
    return awaitingClick;
  }

  /** Bursts small coloured squares out of the bag position. */
  private void spawnConfetti(Color accent, int count) {
    float cx = STAGE_WIDTH / 2f;
    float cy = STAGE_HEIGHT / 2f;

    for (int i = 0; i < count; i++) {
      Image piece = new Image(whiteDrawable);
      float size = MathUtils.random(4f, 8f);
      piece.setSize(size, size);
      piece.setOrigin(size / 2f, size / 2f);
      piece.setPosition(cx - size / 2f, cy - size / 2f);
      piece.setTouchable(Touchable.disabled);
      piece.setColor(
          MathUtils.randomBoolean(0.3f)
              ? accent
              : CONFETTI_COLORS[MathUtils.random(CONFETTI_COLORS.length - 1)]);

      float dx = MathUtils.random(-130f, 130f);
      float rise = MathUtils.random(25f, 75f);
      float fall = rise + MathUtils.random(40f, 90f);
      float upTime = MathUtils.random(0.25f, 0.4f);
      float downTime = MathUtils.random(0.45f, 0.7f);

      piece.addAction(
          Actions.sequence(
              Actions.parallel(
                  Actions.sequence(
                      Actions.moveBy(dx * 0.6f, rise, upTime, Interpolation.pow2Out),
                      Actions.moveBy(dx * 0.4f, -fall, downTime, Interpolation.pow2In)),
                  Actions.rotateBy(MathUtils.random(-540f, 540f), upTime + downTime),
                  Actions.sequence(
                      Actions.delay(upTime + downTime * 0.4f), Actions.fadeOut(downTime * 0.6f))),
              Actions.removeActor()));

      stageGroup.addActor(piece);
    }
  }

  /** Resets the bag to its start transform (does not start any action). */
  private void resetBag() {
    if (bagGroup == null) {
      return;
    }
    bagGroup.clearActions();
    bagGroup.setVisible(true);
    bagGroup.setScale(1f);
    bagGroup.setRotation(0f);
    bagGroup.getColor().a = 1f;
    bagGroup.setPosition((STAGE_WIDTH - BAG_SIZE) / 2f, (STAGE_HEIGHT - BAG_SIZE) / 2f);
  }

  /** Gentle sway while waiting for the player to spin. */
  private void startBagIdle() {
    bagGroup.addAction(
        Actions.forever(
            Actions.sequence(
                Actions.rotateTo(4f, 0.5f, Interpolation.sine),
                Actions.rotateTo(-4f, 1.0f, Interpolation.sine),
                Actions.rotateTo(0f, 0.5f, Interpolation.sine))));
  }

  // ---------------------------------------------------------------------------------------------
  // Slot helpers
  // ---------------------------------------------------------------------------------------------

  /** Writes prize name, drop rate and rarity colour into every slot. */
  private void updatePrizeLabels() {
    for (int i = 0; i < SpinCatalog.PRIZE_SLOT_COUNT; i++) {
      PrizeEntry<?> prize = catalog.getPrize(i + 1);
      if (prize == null) {
        slotNameLabels.get(i).setText("?");
        slotRateLabels.get(i).setText("");
        slotAccents.get(i).setColor(RARITY_COMMON);
        continue;
      }
      float percent = percentOf(prize);
      slotNameLabels.get(i).setText(getPrizeShortName(prize));
      slotRateLabels.get(i).setText(formatRate(percent));
      slotAccents.get(i).setColor(rarityColor(percent));
    }
  }

  private void highlightSlot(int slotIndex) {
    if (slotIndex < 0 || slotIndex >= slotHighlights.size()) {
      return;
    }
    clearHighlights();
    slotHighlights.get(slotIndex).setVisible(true);
    slotNameLabels.get(slotIndex).setColor(WIN_TEXT_COLOR);
  }

  private void clearHighlights() {
    for (int i = 0; i < slotHighlights.size(); i++) {
      slotHighlights.get(i).setVisible(false);
      slotNameLabels.get(i).setColor(TEXT_COLOR);
    }
  }

  private void dimSlots() {
    setSlotsAlpha(SLOT_DIM_ALPHA + 0.25f);
  }

  private void dimSlotsExcept(int keepIndex) {
    for (int i = 0; i < slotStacks.size(); i++) {
      slotStacks.get(i).getColor().a = (i == keepIndex) ? 1f : SLOT_DIM_ALPHA;
    }
  }

  private void setSlotsAlpha(float alpha) {
    for (Stack slot : slotStacks) {
      slot.getColor().a = alpha;
    }
  }

  // ---------------------------------------------------------------------------------------------
  // Rate / naming helpers
  // ---------------------------------------------------------------------------------------------

  private int totalWeight() {
    int total = 0;
    if (catalog != null) {
      for (PrizeEntry<?> prize : catalog.getPrizes().values()) {
        total += prize.getWeight();
      }
    }
    return total;
  }

  /** Drop rate of a prize in percent (weight / sum of weights * 100). */
  private float percentOf(PrizeEntry<?> prize) {
    int total = totalWeight();
    if (prize == null || total <= 0) {
      return 0f;
    }
    return prize.getWeight() * 100f / total;
  }

  private static String formatRate(float percent) {
    if (Math.abs(percent - Math.round(percent)) < 0.05f) {
      return Math.round(percent) + "%";
    }
    return String.format(java.util.Locale.ROOT, "%.1f%%", percent);
  }

  private static Color rarityColor(float percent) {
    if (percent <= 5f) return RARITY_LEGENDARY;
    if (percent <= 15f) return RARITY_EPIC;
    if (percent <= 25f) return RARITY_RARE;
    return RARITY_COMMON;
  }

  /** Converts a prize into a short display name suitable for a small slot. */
  private String getPrizeShortName(PrizeEntry<?> prize) {
    if (prize == null) {
      return "?";
    }

    Object product = prize.getProduct();

    if (product instanceof GamblingCatalogs.ItemPrize itemPrize) {
      return itemPrize.getDisplayName();
    }
    if (product instanceof GamblingCatalogs.GoldPrize goldPrize) {
      return goldPrize.getAmount() + " G";
    }
    if (product instanceof ShopComponent.Pet pet) {
      return pet.getName();
    }
    if (product instanceof ShopComponent.Upgrade upgrade) {
      return upgrade.getName();
    }
    return "?";
  }

  // ---------------------------------------------------------------------------------------------
  // Textures
  // ---------------------------------------------------------------------------------------------

  private TextureRegionDrawable drawableOf(Texture texture) {
    return new TextureRegionDrawable(new TextureRegion(texture));
  }

  private Texture createTexture(int width, int height, Color color) {
    Pixmap pixmap = new Pixmap(width, height, Pixmap.Format.RGBA8888);
    pixmap.setColor(color);
    pixmap.fill();
    Texture texture = new Texture(pixmap);
    pixmap.dispose();
    textures.add(texture);
    return texture;
  }

  /** Transparent rectangle with a coloured outline. */
  private Texture createBorderTexture(int width, int height, Color color, int thickness) {
    Pixmap pixmap = new Pixmap(width, height, Pixmap.Format.RGBA8888);
    pixmap.setColor(0f, 0f, 0f, 0f);
    pixmap.fill();
    pixmap.setColor(color);
    pixmap.fillRectangle(0, 0, width, thickness);
    pixmap.fillRectangle(0, height - thickness, width, thickness);
    pixmap.fillRectangle(0, 0, thickness, height);
    pixmap.fillRectangle(width - thickness, 0, thickness, height);
    Texture texture = new Texture(pixmap);
    pixmap.dispose();
    textures.add(texture);
    return texture;
  }

  /** Grey-scale bag drawing so it can be tinted per catalogue (standard / premium). */
  private Texture createBagTexture() {
    Pixmap pixmap = new Pixmap(64, 64, Pixmap.Format.RGBA8888);
    pixmap.setColor(0f, 0f, 0f, 0f);
    pixmap.fill();

    // Body.
    pixmap.setColor(0.88f, 0.88f, 0.88f, 1f);
    pixmap.fillCircle(32, 38, 25);
    // Neck.
    pixmap.setColor(0.65f, 0.65f, 0.65f, 1f);
    pixmap.fillRectangle(23, 6, 18, 16);
    // Tie.
    pixmap.setColor(1f, 1f, 1f, 1f);
    pixmap.fillRectangle(18, 16, 28, 5);
    // Shine.
    pixmap.setColor(1f, 1f, 1f, 0.55f);
    pixmap.fillCircle(22, 32, 6);

    Texture texture = new Texture(pixmap);
    pixmap.dispose();
    textures.add(texture);
    return texture;
  }
}
