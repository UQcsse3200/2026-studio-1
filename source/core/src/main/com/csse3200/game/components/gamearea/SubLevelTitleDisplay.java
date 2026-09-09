package com.csse3200.game.components.gamearea;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.ScreenUtils;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.ui.UIComponent;

/** Large arrival title over a dimmed, soft-focus-style view of the current sub-level. */
public class SubLevelTitleDisplay extends UIComponent {
  private static final float FADE_IN_SECONDS = 0.35f;
  private static final float HOLD_SECONDS = 2.3f;
  private static final float FADE_OUT_SECONDS = 0.75f;
  private static final float DISPLAY_SECONDS = FADE_IN_SECONDS + HOLD_SECONDS + FADE_OUT_SECONDS;
  private static final float BACKGROUND_ALPHA = 0.68f;
  private static final float BLUR_ALPHA = 0.8f;
  private static final int BLUR_DOWNSCALE = 10;

  private final Entity player;
  private Image blurredBackground;
  private Image backgroundVeil;
  private Label title;
  private Texture blurredTexture;
  private float elapsed;
  private boolean captureRequested = true;

  public SubLevelTitleDisplay(Entity player) {
    this.player = player;
  }

  @Override
  public void create() {
    super.create();
    Label.LabelStyle style = new Label.LabelStyle(skin.get("large", Label.LabelStyle.class));
    style.fontColor = Color.GOLD;

    blurredBackground = new Image();
    blurredBackground.setTouchable(Touchable.disabled);
    stage.addActor(blurredBackground);

    backgroundVeil = new Image(skin.getRegion("scrollbar-android"));
    backgroundVeil.setColor(0f, 0f, 0f, 0f);
    backgroundVeil.setTouchable(Touchable.disabled);
    stage.addActor(backgroundVeil);

    title = new Label("DUNGEON", style);
    title.setFontScale(3.2f);
    title.setColor(1f, 1f, 1f, 0f);
    title.pack();
    stage.addActor(title);
    player.getEvents().addListener("subLevelEntered", this::showTitle);
  }

  @Override
  public void update() {
    elapsed = Math.min(DISPLAY_SECONDS, elapsed + Gdx.graphics.getDeltaTime());
    float opacity = getOpacity();
    blurredBackground.setVisible(opacity > 0f && blurredTexture != null);
    blurredBackground.setColor(1f, 1f, 1f, BLUR_ALPHA * opacity);
    backgroundVeil.setVisible(opacity > 0f);
    backgroundVeil.setColor(0f, 0f, 0f, BACKGROUND_ALPHA * opacity);
    title.setVisible(opacity > 0f);
    title.setColor(1f, 1f, 1f, opacity);
  }

  @Override
  public void draw(SpriteBatch batch) {
    if (captureRequested) {
      batch.flush();
      captureBlurredBackground();
      captureRequested = false;
    }

    float width = stage.getViewport().getWorldWidth();
    float height = stage.getViewport().getWorldHeight();
    blurredBackground.setSize(width, height);
    blurredBackground.setPosition(0f, 0f);
    backgroundVeil.setSize(width, height);
    backgroundVeil.setPosition(0f, 0f);
    title.setPosition((width - title.getWidth()) / 2f, (height - title.getHeight()) / 2f);
  }

  @Override
  public void dispose() {
    super.dispose();
    if (blurredTexture != null) {
      blurredTexture.dispose();
    }
    blurredBackground.remove();
    backgroundVeil.remove();
    title.remove();
  }

  private void showTitle(String name) {
    title.setText(name);
    title.pack();
    elapsed = 0f;
    captureRequested = true;
  }

  private void captureBlurredBackground() {
    int width = Gdx.graphics.getWidth();
    int height = Gdx.graphics.getHeight();
    Pixmap source = ScreenUtils.getFrameBufferPixmap(0, 0, width, height);
    Pixmap reduced =
        new Pixmap(
            Math.max(1, width / BLUR_DOWNSCALE),
            Math.max(1, height / BLUR_DOWNSCALE),
            Pixmap.Format.RGBA8888);
    reduced.drawPixmap(source, 0, 0, width, height, 0, 0, reduced.getWidth(), reduced.getHeight());
    source.dispose();

    if (blurredTexture != null) {
      blurredTexture.dispose();
    }
    blurredTexture = new Texture(reduced);
    blurredTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
    reduced.dispose();

    TextureRegion region = new TextureRegion(blurredTexture);
    region.flip(false, true);
    blurredBackground.setDrawable(new TextureRegionDrawable(region));
  }

  private float getOpacity() {
    if (elapsed < FADE_IN_SECONDS) {
      return elapsed / FADE_IN_SECONDS;
    }
    if (elapsed < FADE_IN_SECONDS + HOLD_SECONDS) {
      return 1f;
    }
    return Math.max(0f, (DISPLAY_SECONDS - elapsed) / FADE_OUT_SECONDS);
  }
}
