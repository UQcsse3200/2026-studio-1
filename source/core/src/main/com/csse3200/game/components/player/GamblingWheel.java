package com.csse3200.game.components.player;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.csse3200.game.components.player.GamblingCatalogs.CatalogId;
import com.csse3200.game.components.player.GamblingCatalogs.PrizeEntry;
import com.csse3200.game.components.player.GamblingCatalogs.SpinCatalog;

/**
 * Scene2D gambling wheel used by ShopDisplay.
 *
 * <p>Both the graphic sectors and prize labels rotate synchronously. Label orientations are offset
 * to guarantee that the winning prize text is perfectly upright when resting under the top pointer.
 */
public class GamblingWheel extends Stack {

  private static final float WHEEL_SIZE = 300f;
  private static final float SPIN_DURATION = 2.4f;

  // Harmonized segment colors
  private static final Color[] SEGMENT_COLORS = {
    new Color(0.12f, 0.16f, 0.28f, 1f),
    new Color(0.42f, 0.15f, 0.14f, 1f),
    new Color(0.12f, 0.32f, 0.28f, 1f),
    new Color(0.34f, 0.18f, 0.42f, 1f),
    new Color(0.52f, 0.36f, 0.12f, 1f)
  };

  private final Label.LabelStyle labelStyle;

  private Group wheelGroup;
  private Image wheelImage;
  private Group labelsGroup;

  private Table pointerOverlay;
  private Image pointerArrow;

  private Texture wheelTexture;
  private Texture pointerTexture;

  private SpinCatalog catalog;
  private CatalogId catalogId;

  private boolean spinning;

  public GamblingWheel(Label.LabelStyle labelStyle) {
    this.labelStyle = labelStyle;
    setSize(WHEEL_SIZE, WHEEL_SIZE);
    build();
  }

  private void build() {
    wheelGroup = new Group();
    wheelGroup.setSize(WHEEL_SIZE, WHEEL_SIZE);
    wheelGroup.setOrigin(WHEEL_SIZE / 2f, WHEEL_SIZE / 2f);
    wheelGroup.setTransform(true);

    wheelTexture = createWheelTexture();
    wheelImage = new Image(new TextureRegionDrawable(new TextureRegion(wheelTexture)));
    wheelImage.setSize(WHEEL_SIZE, WHEEL_SIZE);
    wheelGroup.addActor(wheelImage);

    labelsGroup = new Group();
    labelsGroup.setSize(WHEEL_SIZE, WHEEL_SIZE);
    labelsGroup.setOrigin(WHEEL_SIZE / 2f, WHEEL_SIZE / 2f);
    wheelGroup.addActor(labelsGroup);

    add(wheelGroup);

    buildPointer();
  }

  /** Builds the stationary pointer indicator pointing downward at the wheel's top center. */
  private void buildPointer() {
    pointerOverlay = new Table();
    pointerOverlay.setFillParent(true);
    pointerOverlay.setTouchable(Touchable.disabled);

    pointerTexture = createPointerTexture();
    pointerArrow = new Image(new TextureRegionDrawable(new TextureRegion(pointerTexture)));
    pointerArrow.setSize(26f, 30f);
    pointerArrow.setOrigin(13f, 15f);

    pointerOverlay.top();
    pointerOverlay.add(pointerArrow).size(26f, 30f).padTop(-6f);

    add(pointerOverlay);
  }

  /** Creates the triangular needle pointer texture with a golden rim and crimson core. */
  private Texture createPointerTexture() {
    int w = 26;
    int h = 30;
    Pixmap pixmap = new Pixmap(w, h, Pixmap.Format.RGBA8888);
    pixmap.setColor(Color.CLEAR);
    pixmap.fill();

    // Outer golden border
    pixmap.setColor(new Color(1.0f, 0.84f, 0.25f, 1f));
    pixmap.fillTriangle(0, 0, w - 1, 0, w / 2, h - 1);

    // Inner crimson body
    pixmap.setColor(new Color(0.85f, 0.18f, 0.18f, 1f));
    pixmap.fillTriangle(3, 2, w - 4, 2, w / 2, h - 5);

    Texture texture = new Texture(pixmap);
    pixmap.dispose();
    return texture;
  }

  /** Loads the active gambling catalog into the wheel. */
  public void setCatalog(CatalogId catalogId, SpinCatalog catalog) {
    this.catalogId = catalogId;
    this.catalog = catalog;
    rebuildLabels();
  }

  /** Positions prize labels around the wheel with pre-calculated rotation offsets. */
  private void rebuildLabels() {
    labelsGroup.clearChildren();
    if (catalog == null) return;

    float sectorSize = 360f / SpinCatalog.PRIZE_SLOT_COUNT;
    float center = WHEEL_SIZE / 2f;
    float labelDistance = 90f;
    float labelWidth = 84f;
    float labelHeight = 24f;

    for (int slot = 1; slot <= SpinCatalog.PRIZE_SLOT_COUNT; slot++) {
      PrizeEntry<?> prize = catalog.getPrize(slot);
      if (prize == null) continue;

      Label label = new Label(getPrizeShortName(prize), labelStyle);
      label.setAlignment(Align.center);
      label.setColor(Color.WHITE);

      float slotMidAngle = 90f - ((slot - 0.5f) * sectorSize);
      double rad = Math.toRadians(slotMidAngle);

      float lx = center + (float) Math.cos(rad) * labelDistance - (labelWidth / 2f);
      float ly = center + (float) Math.sin(rad) * labelDistance - (labelHeight / 2f);

      label.setBounds(lx, ly, labelWidth, labelHeight);
      label.setOrigin(labelWidth / 2f, labelHeight / 2f);

      // Negate the spin rotation so text lands completely upright under the top pointer
      float targetRotation = (slot - 0.5f) * sectorSize;
      label.setRotation(-targetRotation);

      labelsGroup.addActor(label);
    }
  }

  /**
   * Rotates the entire wheel to align the target prize slot beneath the top needle indicator.
   *
   * @param prizeSlot designated winning slot (1..5)
   * @param onFinished callback invoked upon spin completion
   */
  public void spinToSlot(int prizeSlot, Runnable onFinished) {
    if (spinning || catalog == null) return;
    if (prizeSlot < 1 || prizeSlot > SpinCatalog.PRIZE_SLOT_COUNT) return;

    spinning = true;

    pointerArrow.clearActions();
    pointerArrow.setScale(1f);

    float sectorSize = 360f / SpinCatalog.PRIZE_SLOT_COUNT;
    float targetRotation = (prizeSlot - 0.5f) * sectorSize;

    float currentRotation = normalize(wheelGroup.getRotation());
    float delta = targetRotation - currentRotation;
    if (delta < 0) {
      delta += 360f;
    }

    float totalRotation = 360f * 5f + delta;

    wheelGroup.clearActions();
    wheelGroup.addAction(
        Actions.sequence(
            Actions.rotateBy(totalRotation, SPIN_DURATION),
            Actions.run(
                () -> {
                  spinning = false;

                  // Needle bounce animation on win
                  pointerArrow.addAction(
                      Actions.sequence(
                          Actions.scaleTo(1.4f, 1.4f, 0.15f),
                          Actions.scaleTo(0.9f, 0.9f, 0.12f),
                          Actions.scaleTo(1.2f, 1.2f, 0.12f),
                          Actions.scaleTo(1.0f, 1.0f, 0.1f)));

                  if (onFinished != null) {
                    onFinished.run();
                  }
                })));
  }

  public boolean isSpinning() {
    return spinning;
  }

  private float normalize(float angle) {
    angle %= 360f;
    if (angle < 0f) {
      angle += 360f;
    }
    return angle;
  }

  private String getPrizeShortName(PrizeEntry<?> prize) {
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

  /** Dynamically builds the multi-colored wheel texture using Pixmap. */
  private Texture createWheelTexture() {
    int size = (int) WHEEL_SIZE;
    Pixmap pixmap = new Pixmap(size, size, Pixmap.Format.RGBA8888);

    pixmap.setColor(Color.CLEAR);
    pixmap.fill();

    float center = size / 2f;
    float radius = size / 2f - 5f;

    for (int slot = 0; slot < SpinCatalog.PRIZE_SLOT_COUNT; slot++) {
      float start = 90f - slot * 360f / SpinCatalog.PRIZE_SLOT_COUNT;
      float end = start - 360f / SpinCatalog.PRIZE_SLOT_COUNT;

      pixmap.setColor(SEGMENT_COLORS[slot % SEGMENT_COLORS.length]);

      int steps = 24;
      for (int i = 0; i < steps; i++) {
        float a1 = start + (end - start) * i / steps;
        float a2 = start + (end - start) * (i + 1) / steps;

        float x1 = center + (float) Math.cos(Math.toRadians(a1)) * radius;
        float y1 = center + (float) Math.sin(Math.toRadians(a1)) * radius;
        float x2 = center + (float) Math.cos(Math.toRadians(a2)) * radius;
        float y2 = center + (float) Math.sin(Math.toRadians(a2)) * radius;

        pixmap.fillTriangle((int) center, (int) center, (int) x1, (int) y1, (int) x2, (int) y2);
      }
    }

    // Outer golden border
    pixmap.setColor(new Color(1.0f, 0.84f, 0.25f, 1f));
    for (int i = 0; i < 6; i++) {
      pixmap.drawCircle(size / 2, size / 2, size / 2 - 4 - i);
    }

    Texture texture = new Texture(pixmap);
    pixmap.dispose();
    return texture;
  }

  public void dispose() {
    if (wheelTexture != null) {
      wheelTexture.dispose();
      wheelTexture = null;
    }
    if (pointerTexture != null) {
      pointerTexture.dispose();
      pointerTexture = null;
    }
  }
}
