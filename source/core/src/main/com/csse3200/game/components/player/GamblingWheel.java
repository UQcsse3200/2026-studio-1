package com.csse3200.game.components.player;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
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
 * <p>The wheel is purely visual. It does not choose a prize. The caller supplies the prize returned
 * by ShopComponent.buySpin().
 */
public class GamblingWheel extends Stack {

  private static final float WHEEL_SIZE = 300f;
  private static final float LABEL_RADIUS = 96f;
  private static final float SPIN_DURATION = 2.4f;

  private static final Color[] SEGMENT_COLORS = {
    new Color(0.18f, 0.20f, 0.27f, 1f),
    new Color(0.28f, 0.20f, 0.18f, 1f),
    new Color(0.18f, 0.26f, 0.23f, 1f),
    new Color(0.24f, 0.19f, 0.29f, 1f),
    new Color(0.25f, 0.24f, 0.17f, 1f)
  };

  private final Label.LabelStyle labelStyle;

  private Image wheelImage;
  private Table labelsTable;

  private Texture wheelTexture;

  private SpinCatalog catalog;
  private CatalogId catalogId;

  private boolean spinning;

  public GamblingWheel(Label.LabelStyle labelStyle) {
    this.labelStyle = labelStyle;

    setSize(WHEEL_SIZE, WHEEL_SIZE);

    build();
  }

  private void build() {
    wheelTexture = createWheelTexture();

    wheelImage = new Image(new TextureRegionDrawable(new TextureRegion(wheelTexture)));

    wheelImage.setSize(WHEEL_SIZE, WHEEL_SIZE);
    wheelImage.setOrigin(WHEEL_SIZE / 2f, WHEEL_SIZE / 2f);

    add(wheelImage);

    labelsTable = new Table();
    labelsTable.setFillParent(true);

    add(labelsTable);
  }

  /** Loads the current gambling catalog into the wheel. */
  public void setCatalog(CatalogId catalogId, SpinCatalog catalog) {
    this.catalogId = catalogId;
    this.catalog = catalog;

    rebuildLabels();
  }

  private void rebuildLabels() {
    labelsTable.clearChildren();

    if (catalog == null) {
      return;
    }

    for (int slot = 1; slot <= SpinCatalog.PRIZE_SLOT_COUNT; slot++) {
      PrizeEntry<?> prize = catalog.getPrize(slot);

      if (prize == null) {
        continue;
      }

      Label label = new Label(getPrizeShortName(prize), labelStyle);

      label.setAlignment(Align.center);
      label.setColor(Color.WHITE);

      /*
       * The labels are positioned around the wheel using absolute
       * coordinates inside the Stack.
       */
      float angle = 90f - ((slot - 0.5f) * 360f / SpinCatalog.PRIZE_SLOT_COUNT);

      double radians = Math.toRadians(angle);

      float x = WHEEL_SIZE / 2f + (float) Math.cos(radians) * LABEL_RADIUS - 35f;

      float y = WHEEL_SIZE / 2f + (float) Math.sin(radians) * LABEL_RADIUS - 12f;

      label.setPosition(x, y);
      label.setSize(70f, 24f);

      labelsTable.addActor(label);
    }
  }

  /**
   * Animates the wheel to the supplied prize slot.
   *
   * <p>The slot must already have been selected by the gameplay/backend system. This method never
   * performs random selection.
   */
  public void spinToSlot(int prizeSlot, Runnable onFinished) {
    if (spinning || catalog == null) {
      return;
    }

    if (prizeSlot < 1 || prizeSlot > SpinCatalog.PRIZE_SLOT_COUNT) {
      return;
    }

    spinning = true;

    /*
     * Five equal sectors.
     *
     * Slot 1 is centred at 90 degrees,
     * slot 2 at 18 degrees, etc.
     */
    float sectorSize = 360f / SpinCatalog.PRIZE_SLOT_COUNT;

    float targetAngle = 90f - ((prizeSlot - 0.5f) * sectorSize);

    float currentRotation = normalize(wheelImage.getRotation());

    float currentToTarget = normalize(targetAngle - currentRotation);

    /*
     * Add several complete rotations so the spin feels substantial.
     */
    float totalRotation = 360f * 5f + currentToTarget;

    wheelImage.clearActions();

    wheelImage.addAction(
        Actions.sequence(
            Actions.rotateBy(totalRotation, SPIN_DURATION),
            Actions.run(
                () -> {
                  spinning = false;

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

    if (product instanceof GamblingCatalogs.ItemPrize) {
      return ((GamblingCatalogs.ItemPrize) product).getDisplayName();
    }

    if (product instanceof GamblingCatalogs.GoldPrize) {
      return ((GamblingCatalogs.GoldPrize) product).getAmount() + " G";
    }

    if (product instanceof ShopComponent.Pet) {
      return ((ShopComponent.Pet) product).getName();
    }

    if (product instanceof ShopComponent.Upgrade) {
      return ((ShopComponent.Upgrade) product).getName();
    }

    return "?";
  }

  /**
   * Creates the actual wheel graphic.
   *
   * <p>Generated with Pixmap so no additional asset is required.
   */
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

      /*
       * Approximate the sector using triangles.
       */
      int steps = 20;

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

    /*
     * Outer ring.
     */
    pixmap.setColor(new Color(0.91f, 0.77f, 0.42f, 1f));

    for (int i = 0; i < 6; i++) {
      pixmap.drawCircle(size / 2, size / 2, size / 2 - 4 - i);
    }

    Texture texture = new Texture(pixmap);

    pixmap.dispose();

    return texture;
  }

  @Override
  public void act(float delta) {
    super.act(delta);
  }

  public void dispose() {
    if (wheelTexture != null) {
      wheelTexture.dispose();
      wheelTexture = null;
    }
  }
}
