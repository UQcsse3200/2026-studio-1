package com.csse3200.game.components.player;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Action;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.csse3200.game.components.player.GamblingCatalogs.CatalogId;
import com.csse3200.game.components.player.GamblingCatalogs.PrizeEntry;
import com.csse3200.game.components.player.GamblingCatalogs.SpinCatalog;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Scene2D gambling result display used by ShopDisplay.
 *
 * <p>The visual is a centered square lottery result box rather than a physical wheel. During a
 * spin, the result text rapidly cycles through available prizes and then stops on the selected
 * prize.
 */
public class GamblingWheel extends Stack {

  private static final float DISPLAY_SIZE = 300f;
  private static final float RESULT_BOX_SIZE = 220f;
  private static final float BORDER_SIZE = 5f;

  private static final float ANIMATION_START_INTERVAL = 0.08f;
  private static final float ANIMATION_END_INTERVAL = 0.28f;
  private static final int RANDOM_PREVIEW_COUNT = 14;
  private static final float WIN_SCALE = 1.08f;

  private static final Color INNER_COLOR = new Color(0.13f, 0.16f, 0.25f, 1f);
  private static final Color BORDER_COLOR = new Color(1.0f, 0.72f, 0.18f, 1f);
  private static final Color TEXT_COLOR = new Color(1f, 1f, 1f, 1f);
  private static final Color WIN_TEXT_COLOR = new Color(1.0f, 0.84f, 0.25f, 1f);
  private static final Color SUBTITLE_COLOR = new Color(0.70f, 0.70f, 0.75f, 1f);

  private final Label.LabelStyle labelStyle;
  private final Random random = new Random();

  private Table resultBox;
  private Label resultLabel;
  private Label statusLabel;

  private Texture borderTexture;
  private Texture innerTexture;

  private SpinCatalog catalog;
  private CatalogId catalogId;

  private boolean spinning;

  public GamblingWheel(Label.LabelStyle labelStyle) {
    this.labelStyle = labelStyle;
    setSize(DISPLAY_SIZE, DISPLAY_SIZE);
    build();
  }

  private void build() {
    clearChildren();

    // Container table centered in this 300x300 widget
    Table centerContainer = new Table();
    centerContainer.setFillParent(true);
    centerContainer.center();

    resultBox = new Table();
    resultBox.setTransform(true);
    resultBox.setSize(RESULT_BOX_SIZE, RESULT_BOX_SIZE);
    resultBox.setOrigin(RESULT_BOX_SIZE / 2f, RESULT_BOX_SIZE / 2f);

    // Golden border background
    borderTexture = createSolidTexture(1, 1, BORDER_COLOR);
    resultBox.setBackground(new TextureRegionDrawable(new TextureRegion(borderTexture)));
    resultBox.pad(BORDER_SIZE);

    // Inner dark container
    Table innerBox = new Table();
    innerTexture = createSolidTexture(1, 1, INNER_COLOR);
    innerBox.setBackground(new TextureRegionDrawable(new TextureRegion(innerTexture)));
    innerBox.pad(10f);

    // Status label at the top
    statusLabel = new Label("GAMBLING", labelStyle);
    statusLabel.setAlignment(Align.center);
    statusLabel.setColor(SUBTITLE_COLOR);
    statusLabel.setFontScale(0.85f);

    // Main prize label in the center
    resultLabel = new Label("READY", labelStyle);
    resultLabel.setAlignment(Align.center);
    resultLabel.setColor(TEXT_COLOR);
    resultLabel.setFontScale(1.15f);
    resultLabel.setWrap(true);

    innerBox.add(statusLabel).growX().top().padTop(6f).row();
    innerBox.add(resultLabel).grow().center().pad(4f);

    resultBox.add(innerBox).grow();
    centerContainer.add(resultBox).size(RESULT_BOX_SIZE, RESULT_BOX_SIZE);

    add(centerContainer);
  }

  private Texture createSolidTexture(int width, int height, Color color) {
    Pixmap pixmap = new Pixmap(width, height, Pixmap.Format.RGBA8888);
    pixmap.setColor(color);
    pixmap.fill();
    Texture texture = new Texture(pixmap);
    pixmap.dispose();
    return texture;
  }

  public void setCatalog(CatalogId catalogId, SpinCatalog catalog) {
    this.catalogId = catalogId;
    this.catalog = catalog;

    clearActions();
    resultBox.clearActions();

    spinning = false;
    resultBox.setScale(1f);

    resultLabel.setColor(TEXT_COLOR);
    resultLabel.setFontScale(1.15f);

    if (catalogId == CatalogId.PREMIUM) {
      statusLabel.setText("PREMIUM GAMBLING");
    } else {
      statusLabel.setText("STANDARD GAMBLING");
    }

    if (catalog == null) {
      resultLabel.setText("NO PRIZES");
      return;
    }

    PrizeEntry<?> firstPrize = catalog.getPrize(1);
    if (firstPrize != null) {
      resultLabel.setText(getPrizeShortName(firstPrize));
    } else {
      resultLabel.setText("READY");
    }
  }

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

    List<String> prizeNames = getPrizeNames();
    if (prizeNames.isEmpty()) {
      return;
    }

    spinning = true;

    clearActions();
    resultBox.clearActions();
    resultBox.setScale(1f);

    resultLabel.setColor(TEXT_COLOR);
    resultLabel.setFontScale(1.15f);
    statusLabel.setText("DRAWING...");

    List<Action> actions = new ArrayList<>();

    for (int i = 0; i < RANDOM_PREVIEW_COUNT; i++) {
      actions.add(Actions.run(() -> showRandomPrize(prizeNames)));
      float progress = (float) i / (float) Math.max(1, RANDOM_PREVIEW_COUNT - 1);
      float delay =
          ANIMATION_START_INTERVAL + (ANIMATION_END_INTERVAL - ANIMATION_START_INTERVAL) * progress;
      actions.add(Actions.delay(delay));
    }

    actions.add(Actions.run(() -> finishSpin(winningPrize, onFinished)));
    resultBox.addAction(Actions.sequence(actions.toArray(new Action[0])));
  }

  private void showRandomPrize(List<String> prizeNames) {
    if (prizeNames == null || prizeNames.isEmpty()) {
      return;
    }
    String randomPrize = prizeNames.get(random.nextInt(prizeNames.size()));
    resultLabel.setText(randomPrize);
    resultLabel.setColor(TEXT_COLOR);
    resultLabel.setFontScale(1.15f);
  }

  private void finishSpin(PrizeEntry<?> winningPrize, Runnable onFinished) {
    spinning = false;
    String prizeName = getPrizeShortName(winningPrize);

    statusLabel.setText("WINNER");
    resultLabel.setText("★ " + prizeName + " ★");
    resultLabel.setColor(WIN_TEXT_COLOR);
    resultLabel.setFontScale(1.25f);

    resultBox.clearActions();
    resultBox.addAction(
        Actions.sequence(
            Actions.scaleTo(WIN_SCALE, WIN_SCALE, 0.12f),
            Actions.scaleTo(1f, 1f, 0.12f),
            Actions.run(
                () -> {
                  if (onFinished != null) {
                    onFinished.run();
                  }
                })));
  }

  public boolean isSpinning() {
    return spinning;
  }

  public SpinCatalog getCatalog() {
    return catalog;
  }

  public CatalogId getCatalogId() {
    return catalogId;
  }

  private List<String> getPrizeNames() {
    List<String> names = new ArrayList<>();
    if (catalog == null) {
      return names;
    }
    for (int slot = 1; slot <= SpinCatalog.PRIZE_SLOT_COUNT; slot++) {
      PrizeEntry<?> prize = catalog.getPrize(slot);
      if (prize != null) {
        names.add(getPrizeShortName(prize));
      }
    }
    return names;
  }

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

  public void reset() {
    clearActions();
    resultBox.clearActions();
    spinning = false;
    resultBox.setScale(1f);

    resultLabel.setColor(TEXT_COLOR);
    resultLabel.setFontScale(1.15f);

    if (catalogId == CatalogId.PREMIUM) {
      statusLabel.setText("PREMIUM GAMBLING");
    } else {
      statusLabel.setText("STANDARD GAMBLING");
    }

    if (catalog != null) {
      PrizeEntry<?> firstPrize = catalog.getPrize(1);
      if (firstPrize != null) {
        resultLabel.setText(getPrizeShortName(firstPrize));
      } else {
        resultLabel.setText("READY");
      }
    } else {
      resultLabel.setText("READY");
    }
  }

  public void dispose() {
    if (borderTexture != null) {
      borderTexture.dispose();
      borderTexture = null;
    }
    if (innerTexture != null) {
      innerTexture.dispose();
      innerTexture = null;
    }
  }
}
