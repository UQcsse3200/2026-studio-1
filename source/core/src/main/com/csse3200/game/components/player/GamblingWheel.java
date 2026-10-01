package com.csse3200.game.components.player;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.Action;
import com.badlogic.gdx.scenes.scene2d.Actor;
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
import com.csse3200.game.components.player.GamblingCatalogs.Prize;
import com.csse3200.game.components.player.GamblingCatalogs.PrizeEntry;
import com.csse3200.game.components.player.GamblingCatalogs.SpinCatalog;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Scene2D MOBA-style chest opening display used by ShopDisplay.
 *
 * <p>The class keeps the existing GamblingWheel name and public API for compatibility with
 * ShopDisplay.
 *
 * <p>Layout: a gold-framed panel with a header banner, an animation stage, and five inventory-style
 * slots (rarity-coloured frame, icon, name, drop rate text and a drop rate bar).
 *
 * <p>Opening sequence: the hex orb charges up and its glow turns into the colour of the prize
 * rarity, energy sparks are pulled into the orb and light rays fan out. The orb then bursts with a
 * flash and shockwaves, and a prize card slides in with a rarity banner. The result stays on screen
 * until the player clicks.
 *
 * <p>This class is presentation-only. It does not select, grant, or re-roll prizes.
 */
public class GamblingWheel extends Stack {

  private static final float DISPLAY_WIDTH = 440f;
  private static final float DISPLAY_HEIGHT = 300f;

  private static final float STAGE_WIDTH = 420f;
  private static final float STAGE_HEIGHT = 118f;

  private static final float ORB_SIZE = 76f;
  private static final float GLOW_SIZE = 190f;
  private static final float CARD_WIDTH = 240f;
  private static final float CARD_HEIGHT = 86f;
  private static final float CARD_Y = 26f;

  private static final float SLOT_WIDTH = 80f;
  private static final float SLOT_HEIGHT = 100f;
  private static final float SLOT_GAP = 4f;
  private static final float BAR_WIDTH = SLOT_WIDTH - 16f;

  private static final int RAY_COUNT = 16;
  private static final int SHAKE_COUNT = 10;
  private static final float SLOT_DIM_ALPHA = 0.45f;
  private static final float CLICK_LOCK_DELAY = 0.6f;

  private static final String FX_NAME = "fx";

  // MOBA style palette: dark navy panel, hextech gold trim.
  private static final Color PANEL_COLOR = new Color(0.03f, 0.05f, 0.09f, 1f);
  private static final Color HEADER_COLOR = new Color(0.10f, 0.09f, 0.06f, 1f);
  private static final Color GOLD_TRIM = new Color(0.78f, 0.62f, 0.28f, 1f);
  private static final Color SLOT_COLOR = new Color(0.07f, 0.10f, 0.16f, 1f);
  private static final Color BAR_BG_COLOR = new Color(0.02f, 0.03f, 0.05f, 1f);
  private static final Color HIGHLIGHT_COLOR = new Color(1.0f, 0.82f, 0.30f, 1f);
  private static final Color TEXT_COLOR = new Color(1f, 1f, 1f, 1f);
  private static final Color MUTED_TEXT_COLOR = new Color(0.70f, 0.70f, 0.75f, 1f);
  private static final Color WIN_TEXT_COLOR = new Color(1.0f, 0.84f, 0.25f, 1f);

  private static final Color ORB_STANDARD = new Color(0.30f, 0.65f, 1.00f, 1f);
  private static final Color ORB_PREMIUM = new Color(0.75f, 0.40f, 1.00f, 1f);

  private static final Color RARITY_COMMON = new Color(0.55f, 0.62f, 0.72f, 1f);
  private static final Color RARITY_RARE = new Color(0.20f, 0.60f, 0.95f, 1f);
  private static final Color RARITY_EPIC = new Color(0.68f, 0.30f, 0.92f, 1f);
  private static final Color RARITY_LEGENDARY = new Color(1.00f, 0.70f, 0.10f, 1f);

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
  private final List<Image> slotFrames = new ArrayList<>();
  private final List<Image> slotIconBgs = new ArrayList<>();
  private final List<Label> slotIconLabels = new ArrayList<>();
  private final List<Label> slotNameLabels = new ArrayList<>();
  private final List<Label> slotRateLabels = new ArrayList<>();
  private final List<Image> slotBarFills = new ArrayList<>();
  private final List<Texture> textures = new ArrayList<>();

  private TextureRegionDrawable whiteDrawable;
  private TextureRegionDrawable glowDrawable;
  private TextureRegionDrawable shockDrawable;
  private TextureRegionDrawable rayDrawable;

  private Table root;
  private Table slotsTable;

  private Group stageGroup;
  private Group raysGroup;
  private Image glow;
  private Group orbGroup;
  private Image orbCore;
  private Stack card;
  private Image cardBorder;
  private Image cardBanner;
  private Label cardRarityLabel;
  private Label cardNameLabel;
  private Label cardRateLabel;
  private Image flash;
  private Label hintLabel;

  private Label statusLabel;
  private Label resultLabel;

  private SpinCatalog catalog;
  private CatalogId catalogId;

  private Runnable pendingFinish;
  private boolean awaitingClick;
  private boolean spinning;

  /**
   * Creates the chest opening display.
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
    glowDrawable = drawableOf(createRadialTexture(128));
    shockDrawable = drawableOf(createRingTexture(128, 4));
    rayDrawable = drawableOf(createRayTexture(16, 128));

    Image panelBackground = new Image(whiteDrawable);
    panelBackground.setColor(PANEL_COLOR);
    add(panelBackground);

    root = new Table();
    root.setFillParent(true);
    add(root);

    Image frame =
        new Image(
            drawableOf(
                createBorderTexture((int) DISPLAY_WIDTH, (int) DISPLAY_HEIGHT, Color.WHITE, 3)));
    frame.setColor(GOLD_TRIM);
    frame.setTouchable(Touchable.disabled);
    add(frame);

    // Header banner.
    Stack header = new Stack();
    Image headerBar = new Image(whiteDrawable);
    headerBar.setColor(HEADER_COLOR);
    statusLabel = new Label("HEXTECH CHEST", labelStyle);
    statusLabel.setAlignment(Align.center);
    statusLabel.setColor(GOLD_TRIM);
    statusLabel.setFontScale(1.0f);
    header.add(headerBar);
    header.add(statusLabel);
    root.add(header).width(DISPLAY_WIDTH - 6f).height(28f).padTop(3f).row();

    Image divider = new Image(whiteDrawable);
    divider.setColor(GOLD_TRIM);
    root.add(divider).width(DISPLAY_WIDTH - 6f).height(2f).row();

    buildStage();
    // Group has no clipping of its own, so wrap the stage in a clipping Table. This keeps rays,
    // sparks and confetti inside the stage area.
    Table stageClip = new Table();
    stageClip.setClip(true);
    stageClip.add(stageGroup).size(STAGE_WIDTH, STAGE_HEIGHT);
    root.add(stageClip).size(STAGE_WIDTH, STAGE_HEIGHT).center().padTop(2f).row();

    buildSlots();
    root.add(slotsTable).center().padTop(4f).row();

    resultLabel = new Label("Choose a catalogue and spin!", labelStyle);
    resultLabel.setAlignment(Align.center);
    resultLabel.setColor(TEXT_COLOR);
    resultLabel.setFontScale(1.0f);
    resultLabel.setWrap(true);
    root.add(resultLabel).width(DISPLAY_WIDTH - 20f).height(30f).center().padTop(3f);
  }

  /** Builds the animated stage: rays, glow, hint, prize card, orb and flash overlay. */
  private void buildStage() {
    stageGroup = new Group();
    stageGroup.setSize(STAGE_WIDTH, STAGE_HEIGHT);
    stageGroup.setTouchable(Touchable.disabled);

    float cx = STAGE_WIDTH / 2f;
    float cy = STAGE_HEIGHT / 2f;

    // Light rays fanning out from the centre.
    raysGroup = new Group();
    raysGroup.setPosition(cx, cy);
    for (int i = 0; i < RAY_COUNT; i++) {
      Image ray = new Image(rayDrawable);
      ray.setSize(14f, 150f);
      ray.setOrigin(7f, 0f);
      ray.setPosition(-7f, 0f);
      ray.setRotation(i * (360f / RAY_COUNT));
      raysGroup.addActor(ray);
    }

    glow = new Image(glowDrawable);
    glow.setSize(GLOW_SIZE, GLOW_SIZE);
    glow.setOrigin(GLOW_SIZE / 2f, GLOW_SIZE / 2f);
    glow.setPosition(cx - GLOW_SIZE / 2f, cy - GLOW_SIZE / 2f);

    hintLabel = new Label("- Click to continue -", labelStyle);
    hintLabel.setAlignment(Align.center);
    hintLabel.setColor(MUTED_TEXT_COLOR);
    hintLabel.setFontScale(0.85f);
    hintLabel.setSize(STAGE_WIDTH, 20f);
    hintLabel.setPosition(0f, 2f);
    hintLabel.setVisible(false);

    buildCard();

    // Hex orb (the "chest").
    orbGroup = new Group();
    orbGroup.setSize(ORB_SIZE, ORB_SIZE);
    orbGroup.setOrigin(ORB_SIZE / 2f, ORB_SIZE / 2f);

    orbCore = new Image(drawableOf(createCircleTexture(64)));
    orbCore.setSize(ORB_SIZE - 8f, ORB_SIZE - 8f);
    orbCore.setPosition(4f, 4f);

    Image shine = new Image(glowDrawable);
    shine.setSize(34f, 34f);
    shine.setPosition(14f, ORB_SIZE - 14f - 34f + 4f);

    Image ring = new Image(shockDrawable);
    ring.setSize(ORB_SIZE, ORB_SIZE);
    ring.setColor(GOLD_TRIM);

    Label questionMark = new Label("?", labelStyle);
    questionMark.setAlignment(Align.center);
    questionMark.setColor(Color.WHITE);
    questionMark.setFontScale(2.2f);
    questionMark.setSize(ORB_SIZE, ORB_SIZE * 0.85f);

    orbGroup.addActor(orbCore);
    orbGroup.addActor(shine);
    orbGroup.addActor(ring);
    orbGroup.addActor(questionMark);

    flash = new Image(whiteDrawable);
    flash.setSize(STAGE_WIDTH, STAGE_HEIGHT);
    flash.setColor(1f, 1f, 1f, 0f);
    flash.setVisible(false);
    flash.setTouchable(Touchable.disabled);

    stageGroup.addActor(raysGroup);
    stageGroup.addActor(glow);
    stageGroup.addActor(hintLabel);
    stageGroup.addActor(card);
    stageGroup.addActor(orbGroup);
    stageGroup.addActor(flash);

    resetStageFx();
  }

  /** Builds the prize card: dark body, rarity border, rarity banner, name and drop rate. */
  private void buildCard() {
    card = new Stack();
    card.setTransform(true);
    card.setSize(CARD_WIDTH, CARD_HEIGHT);
    card.setOrigin(CARD_WIDTH / 2f, CARD_HEIGHT / 2f);
    card.setPosition((STAGE_WIDTH - CARD_WIDTH) / 2f, CARD_Y);
    card.setVisible(false);

    Image cardBg = new Image(whiteDrawable);
    cardBg.setColor(new Color(0.05f, 0.07f, 0.12f, 1f));

    cardBorder =
        new Image(
            drawableOf(createBorderTexture((int) CARD_WIDTH, (int) CARD_HEIGHT, Color.WHITE, 3)));

    Table content = new Table();
    content.pad(3f);

    Stack banner = new Stack();
    cardBanner = new Image(whiteDrawable);
    cardRarityLabel = new Label("", labelStyle);
    cardRarityLabel.setAlignment(Align.center);
    cardRarityLabel.setColor(Color.WHITE);
    cardRarityLabel.setFontScale(0.9f);
    banner.add(cardBanner);
    banner.add(cardRarityLabel);

    cardNameLabel = new Label("", labelStyle);
    cardNameLabel.setAlignment(Align.center);
    cardNameLabel.setColor(Color.WHITE);
    cardNameLabel.setFontScale(1.25f);
    cardNameLabel.setWrap(true);

    cardRateLabel = new Label("", labelStyle);
    cardRateLabel.setAlignment(Align.center);
    cardRateLabel.setColor(WIN_TEXT_COLOR);
    cardRateLabel.setFontScale(0.9f);

    content.add(banner).growX().height(22f).row();
    content.add(cardNameLabel).width(CARD_WIDTH - 24f).expandY().center().row();
    content.add(cardRateLabel).center().padBottom(4f);

    card.add(cardBg);
    card.add(content);
    card.add(cardBorder);
  }

  /** Builds the five inventory-style slots. */
  private void buildSlots() {
    slotsTable = new Table();
    slotsTable.defaults().pad(SLOT_GAP / 2f);

    Texture frameTexture = createBorderTexture((int) SLOT_WIDTH, (int) SLOT_HEIGHT, Color.WHITE, 2);
    Texture highlightTexture =
        createBorderTexture((int) SLOT_WIDTH, (int) SLOT_HEIGHT, HIGHLIGHT_COLOR, 4);

    for (int i = 0; i < SpinCatalog.PRIZE_SLOT_COUNT; i++) {
      Stack slot = new Stack();
      slot.setSize(SLOT_WIDTH, SLOT_HEIGHT);

      Image background = new Image(whiteDrawable);
      background.setColor(SLOT_COLOR);

      Image frame = new Image(drawableOf(frameTexture));
      frame.setColor(RARITY_COMMON);

      Table content = new Table();

      // Icon badge with the prize initial.
      Stack iconStack = new Stack();
      Image iconBg = new Image(whiteDrawable);
      iconBg.setColor(RARITY_COMMON);
      Label iconLabel = new Label("?", labelStyle);
      iconLabel.setAlignment(Align.center);
      iconLabel.setColor(Color.WHITE);
      iconLabel.setFontScale(1.0f);
      iconStack.add(iconBg);
      iconStack.add(iconLabel);
      content.add(iconStack).size(30f, 30f).padTop(6f).row();

      Label nameLabel = new Label("?", labelStyle);
      nameLabel.setAlignment(Align.center);
      nameLabel.setColor(TEXT_COLOR);
      nameLabel.setFontScale(0.8f);
      nameLabel.setWrap(true);
      content.add(nameLabel).width(SLOT_WIDTH - 8f).expandY().center().row();

      Label rateLabel = new Label("", labelStyle);
      rateLabel.setAlignment(Align.center);
      rateLabel.setColor(WIN_TEXT_COLOR);
      rateLabel.setFontScale(1.05f);
      content.add(rateLabel).center().row();

      // Drop rate bar.
      Group barGroup = new Group();
      barGroup.setSize(BAR_WIDTH, 4f);
      Image barBg = new Image(whiteDrawable);
      barBg.setColor(BAR_BG_COLOR);
      barBg.setSize(BAR_WIDTH, 4f);
      Image barFill = new Image(whiteDrawable);
      barFill.setColor(RARITY_COMMON);
      barFill.setSize(0f, 4f);
      barGroup.addActor(barBg);
      barGroup.addActor(barFill);
      content.add(barGroup).size(BAR_WIDTH, 4f).padTop(2f).padBottom(6f);

      Image highlight = new Image(drawableOf(highlightTexture));
      highlight.setVisible(false);

      slot.add(background);
      slot.add(frame);
      slot.add(content);
      slot.add(highlight);

      slotsTable.add(slot).size(SLOT_WIDTH, SLOT_HEIGHT);

      slotStacks.add(slot);
      slotFrames.add(frame);
      slotHighlights.add(highlight);
      slotIconBgs.add(iconBg);
      slotIconLabels.add(iconLabel);
      slotNameLabels.add(nameLabel);
      slotRateLabels.add(rateLabel);
      slotBarFills.add(barFill);
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
   * Plays the chest opening animation for the predetermined prize.
   *
   * <p>The winning prize has already been selected by ShopComponent. This method only animates.
   *
   * @param prizeSlot predetermined winning slot, one-based
   * @param onFinished callback executed after the player confirms the revealed result
   */
  public void spinToSlot(int prizeSlot, Runnable onFinished) {
    if (spinning || catalog == null) {
      return;
    }
    if (prizeSlot < 1 || prizeSlot > SpinCatalog.PRIZE_SLOT_COUNT) {
      return;
    }
    PrizeEntry<Prize> winningPrize = catalog.getPrize(prizeSlot);
    if (winningPrize == null) {
      return;
    }

    spinning = true;
    awaitingClick = false;

    clearActions();
    clearHighlights();
    setSlotsAlpha(SLOT_DIM_ALPHA + 0.25f);
    resetStageFx();

    resultLabel.setColor(TEXT_COLOR);
    resultLabel.setFontScale(1.0f);
    resultLabel.setText("Opening chest...");
    statusLabel.setText("OPENING...");

    startCharge(prizeSlot, winningPrize, onFinished);
  }

  /** Returns whether the animation is currently running (or a result is waiting for a click). */
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

  /** Resets the display to its idle state. */
  public void reset() {
    clearActions();
    for (Stack slot : slotStacks) {
      slot.clearActions();
      slot.setScale(1f);
    }

    spinning = false;
    awaitingClick = false;
    pendingFinish = null;

    clearHighlights();
    setSlotsAlpha(1f);
    resetStageFx();
    startIdle();

    resultLabel.setColor(TEXT_COLOR);
    resultLabel.setFontScale(1.0f);

    boolean premium = catalogId == CatalogId.PREMIUM;
    statusLabel.setText(premium ? "PREMIUM HEXTECH CHEST" : "STANDARD HEXTECH CHEST");

    if (catalog == null) {
      for (int i = 0; i < slotNameLabels.size(); i++) {
        slotNameLabels.get(i).setText("?");
        slotIconLabels.get(i).setText("?");
        slotRateLabels.get(i).setText("");
        slotFrames.get(i).setColor(RARITY_COMMON);
        slotIconBgs.get(i).setColor(RARITY_COMMON);
        slotBarFills.get(i).setColor(RARITY_COMMON);
        slotBarFills.get(i).setWidth(0f);
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

  /** Removes all transient effects and puts every stage actor in its static start state. */
  private void resetStageFx() {
    stageGroup.clearActions();

    // Remove leftover sparks, confetti and shockwaves.
    for (int i = stageGroup.getChildren().size - 1; i >= 0; i--) {
      Actor child = stageGroup.getChildren().get(i);
      if (FX_NAME.equals(child.getName())) {
        child.remove();
      }
    }

    float cx = STAGE_WIDTH / 2f;
    float cy = STAGE_HEIGHT / 2f;

    raysGroup.clearActions();
    raysGroup.setVisible(false);
    raysGroup.setRotation(0f);
    raysGroup.getColor().a = 0f;

    glow.clearActions();
    glow.setVisible(true);
    glow.setScale(1f);
    Color base = baseColor();
    glow.setColor(base.r, base.g, base.b, 0.35f);

    orbGroup.clearActions();
    orbGroup.setVisible(true);
    orbGroup.setScale(1f);
    orbGroup.setRotation(0f);
    orbGroup.getColor().a = 1f;
    orbGroup.setPosition(cx - ORB_SIZE / 2f, cy - ORB_SIZE / 2f);
    orbCore.clearActions();
    orbCore.setColor(base);

    card.clearActions();
    card.setVisible(false);
    card.setScale(1f);
    card.setRotation(0f);
    card.getColor().a = 1f;
    card.setPosition((STAGE_WIDTH - CARD_WIDTH) / 2f, CARD_Y);
    cardBorder.clearActions();

    flash.clearActions();
    flash.setVisible(false);

    hintLabel.clearActions();
    hintLabel.setVisible(false);
  }

  private Color baseColor() {
    return catalogId == CatalogId.PREMIUM ? ORB_PREMIUM : ORB_STANDARD;
  }

  /** Idle: the orb sways and its glow breathes. */
  private void startIdle() {
    orbGroup.addAction(
        Actions.forever(
            Actions.sequence(
                Actions.rotateTo(4f, 0.5f, Interpolation.sine),
                Actions.rotateTo(-4f, 1.0f, Interpolation.sine),
                Actions.rotateTo(0f, 0.5f, Interpolation.sine))));
    glow.addAction(
        Actions.forever(
            Actions.sequence(
                Actions.alpha(0.20f, 0.9f, Interpolation.sine),
                Actions.alpha(0.45f, 0.9f, Interpolation.sine))));
  }

  /**
   * Charge phase: the glow shifts to the rarity colour of the prize, rays fade in, sparks are
   * pulled into the orb and the orb shakes harder and harder. Ends with the burst and reveal.
   */
  private void startCharge(int prizeSlot, PrizeEntry<Prize> prize, Runnable onFinished) {
    float percent = percentOf(prize);
    Color rarity = rarityColor(percent);

    // Glow + core turn into the rarity colour (the tell-tale of MOBA chests).
    glow.clearActions();
    glow.addAction(
        Actions.parallel(
            Actions.color(new Color(rarity.r, rarity.g, rarity.b, 0.85f), 1.3f),
            Actions.scaleTo(1.5f, 1.5f, 1.3f, Interpolation.pow2Out)));
    orbCore.addAction(Actions.color(rarity, 1.3f));

    // Rays.
    for (Actor ray : raysGroup.getChildren()) {
      ray.setColor(rarity.r, rarity.g, rarity.b, 0.85f);
    }
    raysGroup.setVisible(true);
    raysGroup.getColor().a = 0f;
    raysGroup.addAction(Actions.alpha(0.7f, 1.3f));
    raysGroup.addAction(Actions.forever(Actions.rotateBy(-360f, 7f)));

    // Sparks converge on the orb.
    stageGroup.addAction(
        Actions.repeat(
            22, Actions.sequence(Actions.run(() -> spawnSpark(rarity)), Actions.delay(0.06f))));

    orbGroup.clearActions();
    orbGroup.addAction(buildOrbSequence(prizeSlot, prize, onFinished));
  }

  /** Hop -> shake (getting stronger) -> swell -> burst -> reveal. */
  private Action buildOrbSequence(int prizeSlot, PrizeEntry<Prize> prize, Runnable onFinished) {
    List<Action> a = new ArrayList<>();

    a.add(
        Actions.parallel(
            Actions.moveBy(0f, 8f, 0.15f, Interpolation.pow2Out),
            Actions.scaleTo(1.1f, 1.1f, 0.15f)));

    for (int i = 0; i < SHAKE_COUNT; i++) {
      float angle = (i % 2 == 0 ? 1f : -1f) * (6f + i * 2f);
      float duration = Math.max(0.045f, 0.10f - i * 0.006f);
      a.add(Actions.rotateTo(angle, duration));
    }
    a.add(Actions.rotateTo(0f, 0.06f));

    a.add(Actions.scaleTo(1.4f, 1.4f, 0.2f, Interpolation.swingOut));
    a.add(Actions.delay(0.08f));

    a.add(Actions.parallel(Actions.scaleTo(2.0f, 2.0f, 0.14f), Actions.alpha(0f, 0.14f)));
    a.add(Actions.run(() -> revealPrize(prizeSlot, prize, onFinished)));

    return Actions.sequence(a.toArray(new Action[0]));
  }

  /** Flash + shockwaves + confetti + card slide-in, then highlight the winning slot. */
  private void revealPrize(int prizeSlot, PrizeEntry<Prize> prize, Runnable onFinished) {
    int winnerIndex = prizeSlot - 1;
    float percent = percentOf(prize);
    Color rarity = rarityColor(percent);
    boolean rare = percent <= 15f;
    boolean legendary = percent <= 5f;

    orbGroup.setVisible(false);

    // Flash.
    flash.clearActions();
    flash.setColor(1f, 1f, 1f, 0.95f);
    flash.setVisible(true);
    flash.addAction(Actions.sequence(Actions.fadeOut(0.45f), Actions.visible(false)));

    // Shockwaves (two for rare prizes, a third for legendary).
    spawnShockwave(rarity, 0f);
    if (rare) {
      spawnShockwave(rarity, 0.12f);
    }
    if (legendary) {
      spawnShockwave(Color.WHITE, 0.24f);
    }

    // Backglow and rays stay behind the card until the player clicks.
    glow.clearActions();
    glow.setColor(rarity.r, rarity.g, rarity.b, 0.7f);
    glow.setScale(1.6f);
    glow.addAction(
        Actions.forever(
            Actions.sequence(
                Actions.alpha(0.45f, 0.7f, Interpolation.sine),
                Actions.alpha(0.8f, 0.7f, Interpolation.sine))));
    raysGroup.addAction(Actions.alpha(legendary ? 0.6f : 0.4f, 0.3f));

    // Prize card slides in with a rarity banner.
    cardBanner.setColor(rarity);
    cardBorder.setColor(rarity);
    cardRarityLabel.setText(rarityName(percent));
    cardNameLabel.setText(getPrizeShortName(prize));
    cardRateLabel.setText(formatRate(percent) + " chance");
    card.clearActions();
    card.setVisible(true);
    card.getColor().a = 0f;
    card.setScale(0.6f);
    card.setPosition(-CARD_WIDTH, CARD_Y);
    card.addAction(
        Actions.parallel(
            Actions.moveTo((STAGE_WIDTH - CARD_WIDTH) / 2f, CARD_Y, 0.5f, Interpolation.swingOut),
            Actions.scaleTo(1f, 1f, 0.5f, Interpolation.swingOut),
            Actions.alpha(1f, 0.25f)));
    if (rare) {
      cardBorder.addAction(
          Actions.forever(
              Actions.sequence(
                  Actions.alpha(0.5f, 0.45f, Interpolation.sine),
                  Actions.alpha(1f, 0.45f, Interpolation.sine))));
    }

    spawnConfetti(rarity, legendary ? 60 : rare ? 44 : 24);

    // Inventory slots: highlight the winner, dim the rest.
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

    // Hold the result. After a short lock the display waits for the player to click.
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

  /** A small light that flies into the orb. */
  private void spawnSpark(Color color) {
    float cx = STAGE_WIDTH / 2f;
    float cy = STAGE_HEIGHT / 2f;
    float angle = MathUtils.random(360f);
    float radius = MathUtils.random(80f, 130f);
    float size = MathUtils.random(8f, 14f);

    Image spark = new Image(glowDrawable);
    spark.setName(FX_NAME);
    spark.setTouchable(Touchable.disabled);
    spark.setSize(size, size);
    spark.setOrigin(size / 2f, size / 2f);
    spark.setColor(color);
    spark.setPosition(
        cx + MathUtils.cosDeg(angle) * radius - size / 2f,
        cy + MathUtils.sinDeg(angle) * radius - size / 2f);
    spark.addAction(
        Actions.sequence(
            Actions.parallel(
                Actions.moveTo(cx - size / 2f, cy - size / 2f, 0.45f, Interpolation.pow2In),
                Actions.scaleTo(0.2f, 0.2f, 0.45f)),
            Actions.removeActor()));
    stageGroup.addActor(spark);
  }

  /** An expanding ring from the orb position. */
  private void spawnShockwave(Color color, float delay) {
    float cx = STAGE_WIDTH / 2f;
    float cy = STAGE_HEIGHT / 2f;
    float size = 40f;

    Image ring = new Image(shockDrawable);
    ring.setName(FX_NAME);
    ring.setTouchable(Touchable.disabled);
    ring.setSize(size, size);
    ring.setOrigin(size / 2f, size / 2f);
    ring.setPosition(cx - size / 2f, cy - size / 2f);
    ring.setColor(color.r, color.g, color.b, 0.9f);
    ring.setVisible(false);
    ring.addAction(
        Actions.sequence(
            Actions.delay(delay),
            Actions.visible(true),
            Actions.parallel(
                Actions.scaleTo(9f, 9f, 0.7f, Interpolation.pow2Out), Actions.fadeOut(0.7f)),
            Actions.removeActor()));
    stageGroup.addActor(ring);
  }

  /** Bursts small coloured squares out of the orb position. */
  private void spawnConfetti(Color accent, int count) {
    float cx = STAGE_WIDTH / 2f;
    float cy = STAGE_HEIGHT / 2f;

    for (int i = 0; i < count; i++) {
      Image piece = new Image(whiteDrawable);
      piece.setName(FX_NAME);
      piece.setTouchable(Touchable.disabled);
      float size = MathUtils.random(4f, 8f);
      piece.setSize(size, size);
      piece.setOrigin(size / 2f, size / 2f);
      piece.setPosition(cx - size / 2f, cy - size / 2f);
      piece.setColor(
          MathUtils.randomBoolean(0.35f)
              ? accent
              : CONFETTI_COLORS[MathUtils.random(CONFETTI_COLORS.length - 1)]);

      float dx = MathUtils.random(-190f, 190f);
      float rise = MathUtils.random(15f, 55f);
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

  // ---------------------------------------------------------------------------------------------
  // Slot helpers
  // ---------------------------------------------------------------------------------------------

  /** Writes prize name, icon, drop rate, rate bar and rarity colour into every slot. */
  private void updatePrizeLabels() {
    for (int i = 0; i < SpinCatalog.PRIZE_SLOT_COUNT; i++) {
      PrizeEntry<Prize> prize = catalog.getPrize(i + 1);
      if (prize == null) {
        slotNameLabels.get(i).setText("?");
        slotIconLabels.get(i).setText("?");
        slotRateLabels.get(i).setText("");
        slotFrames.get(i).setColor(RARITY_COMMON);
        slotIconBgs.get(i).setColor(RARITY_COMMON);
        slotBarFills.get(i).setColor(RARITY_COMMON);
        slotBarFills.get(i).setWidth(0f);
        continue;
      }
      float percent = percentOf(prize);
      Color rarity = rarityColor(percent);
      slotNameLabels.get(i).setText(getPrizeShortName(prize));
      slotIconLabels.get(i).setText(getPrizeInitial(prize));
      slotRateLabels.get(i).setText(formatRate(percent));
      slotFrames.get(i).setColor(rarity);
      slotIconBgs.get(i).setColor(rarity);
      slotBarFills.get(i).setColor(rarity);
      slotBarFills.get(i).setWidth(BAR_WIDTH * MathUtils.clamp(percent / 100f, 0f, 1f));
    }
  }

  private void highlightSlot(int slotIndex) {
    if (slotIndex < 0 || slotIndex >= slotHighlights.size()) {
      return;
    }
    clearHighlights();
    Image highlight = slotHighlights.get(slotIndex);
    highlight.setVisible(true);
    highlight.addAction(
        Actions.forever(
            Actions.sequence(
                Actions.alpha(0.45f, 0.4f, Interpolation.sine),
                Actions.alpha(1f, 0.4f, Interpolation.sine))));
    slotNameLabels.get(slotIndex).setColor(WIN_TEXT_COLOR);
  }

  private void clearHighlights() {
    for (int i = 0; i < slotHighlights.size(); i++) {
      Image highlight = slotHighlights.get(i);
      highlight.clearActions();
      highlight.getColor().a = 1f;
      highlight.setVisible(false);
      slotNameLabels.get(i).setColor(TEXT_COLOR);
    }
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
      for (PrizeEntry<Prize> prize : catalog.getPrizes().values()) {
        total += prize.weight();
      }
    }
    return total;
  }

  /** Drop rate of a prize in percent (weight / sum of weights * 100). */
  private float percentOf(PrizeEntry<Prize> prize) {
    int total = totalWeight();
    if (prize == null || total <= 0) {
      return 0f;
    }
    return prize.weight() * 100f / total;
  }

  private static String formatRate(float percent) {
    if (Math.abs(percent - Math.round(percent)) < 0.05f) {
      return Math.round(percent) + "%";
    }
    return String.format(Locale.ROOT, "%.1f%%", percent);
  }

  private static Color rarityColor(float percent) {
    if (percent <= 5f) return RARITY_LEGENDARY;
    if (percent <= 15f) return RARITY_EPIC;
    if (percent <= 25f) return RARITY_RARE;
    return RARITY_COMMON;
  }

  private static String rarityName(float percent) {
    if (percent <= 5f) return "LEGENDARY";
    if (percent <= 15f) return "EPIC";
    if (percent <= 25f) return "RARE";
    return "COMMON";
  }

  /** Converts a prize into a short display name suitable for a small slot. */
  private String getPrizeShortName(PrizeEntry<Prize> prize) {
    if (prize == null) {
      return "?";
    }

    Object product = prize.product();

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

  /** One character shown in the slot icon badge. */
  private String getPrizeInitial(PrizeEntry<Prize> prize) {
    if (prize != null && prize.product() instanceof GamblingCatalogs.GoldPrize) {
      return "G";
    }
    String name = getPrizeShortName(prize);
    return name.isEmpty() ? "?" : name.substring(0, 1).toUpperCase(Locale.ROOT);
  }

  // ---------------------------------------------------------------------------------------------
  // Textures
  // ---------------------------------------------------------------------------------------------

  private TextureRegionDrawable drawableOf(Texture texture) {
    return new TextureRegionDrawable(new TextureRegion(texture));
  }

  private Texture register(Pixmap pixmap) {
    Texture texture = new Texture(pixmap);
    pixmap.dispose();
    textures.add(texture);
    return texture;
  }

  private Texture createTexture(int width, int height, Color color) {
    Pixmap pixmap = new Pixmap(width, height, Pixmap.Format.RGBA8888);
    pixmap.setColor(color);
    pixmap.fill();
    return register(pixmap);
  }

  /** Transparent rectangle with an outline. */
  private Texture createBorderTexture(int width, int height, Color color, int thickness) {
    Pixmap pixmap = new Pixmap(width, height, Pixmap.Format.RGBA8888);
    pixmap.setColor(0f, 0f, 0f, 0f);
    pixmap.fill();
    pixmap.setColor(color);
    pixmap.fillRectangle(0, 0, width, thickness);
    pixmap.fillRectangle(0, height - thickness, width, thickness);
    pixmap.fillRectangle(0, 0, thickness, height);
    pixmap.fillRectangle(width - thickness, 0, thickness, height);
    return register(pixmap);
  }

  /** Solid white disc (tinted at use). */
  private Texture createCircleTexture(int size) {
    Pixmap pixmap = new Pixmap(size, size, Pixmap.Format.RGBA8888);
    pixmap.setColor(0f, 0f, 0f, 0f);
    pixmap.fill();
    pixmap.setColor(1f, 1f, 1f, 1f);
    pixmap.fillCircle(size / 2, size / 2, size / 2 - 1);
    return register(pixmap);
  }

  /** White circle outline (tinted at use). */
  private Texture createRingTexture(int size, int thickness) {
    Pixmap pixmap = new Pixmap(size, size, Pixmap.Format.RGBA8888);
    pixmap.setColor(0f, 0f, 0f, 0f);
    pixmap.fill();
    pixmap.setColor(1f, 1f, 1f, 1f);
    for (int t = 0; t < thickness; t++) {
      pixmap.drawCircle(size / 2, size / 2, size / 2 - 2 - t);
    }
    return register(pixmap);
  }

  /** Soft white radial glow: opaque in the centre, transparent at the edge. */
  private Texture createRadialTexture(int size) {
    Pixmap pixmap = new Pixmap(size, size, Pixmap.Format.RGBA8888);
    pixmap.setBlending(Pixmap.Blending.None);
    pixmap.setColor(0f, 0f, 0f, 0f);
    pixmap.fill();
    float half = size / 2f;
    for (int y = 0; y < size; y++) {
      for (int x = 0; x < size; x++) {
        float dx = (x + 0.5f - half) / half;
        float dy = (y + 0.5f - half) / half;
        float d = (float) Math.sqrt(dx * dx + dy * dy);
        if (d < 1f) {
          float a = (1f - d) * (1f - d);
          pixmap.setColor(1f, 1f, 1f, a);
          pixmap.drawPixel(x, y);
        }
      }
    }
    return register(pixmap);
  }

  /** Light ray: opaque at the bottom (centre of the burst), fading out towards the top. */
  private Texture createRayTexture(int width, int height) {
    Pixmap pixmap = new Pixmap(width, height, Pixmap.Format.RGBA8888);
    pixmap.setBlending(Pixmap.Blending.None);
    pixmap.setColor(0f, 0f, 0f, 0f);
    pixmap.fill();
    float half = width / 2f;
    for (int y = 0; y < height; y++) {
      float along = y / (float) (height - 1);
      for (int x = 0; x < width; x++) {
        float across = 1f - Math.abs(x + 0.5f - half) / half;
        pixmap.setColor(1f, 1f, 1f, along * along * across);
        pixmap.drawPixel(x, y);
      }
    }
    return register(pixmap);
  }
}
