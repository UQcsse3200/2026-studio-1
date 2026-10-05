package com.csse3200.game.components.boss;

import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.rendering.RenderComponent;
import com.csse3200.game.services.ServiceLocator;

/**
 * Draws Zeus's attacks and their warnings from the state held by {@link ZeusBossComponent}: thrown
 * bolts, ground-strike markers and columns, shockwaves and the floor lighting up ahead of them, and
 * an arrow at the edge of the screen when he winds up out of view.
 *
 * <p>Everything here is a warning or an effect in one place in the world; nothing flashes the
 * screen.
 */
public class ZeusEffectsRenderComponent extends RenderComponent {
  static final String BOLT_SHEET = "images/effects/zeus-bolt-projectile-4f.png";
  static final String ARROW_SHEET = "images/effects/zeus-edge-arrow-4f.png";
  static final String STRIKE_SHEET = "images/effects/zeus-ground-strike-6f.png";
  static final String SHOCKWAVE_SHEET = "images/effects/zeus-shockwave-6f.png";
  static final String MARKER_SHEET = "images/effects/zeus-strike-marker-2f.png";
  static final String GLOW = "images/effects/lighting/glow-electric.png";

  private static final float TILE = 0.5f;
  private static final float BOLT_WIDTH = 0.75f;
  private static final float BOLT_HEIGHT = 0.375f;
  private static final float MARKER_HEIGHT = 0.25f;
  private static final float ARROW_SIZE = 0.6f;
  private static final float ARROW_MARGIN = 0.15f;

  /** How long before a strike lands its warning brightens. */
  private static final float STRIKE_FINAL_WARNING = 0.3f;

  private ZeusBossComponent boss;
  private TextureRegion[] boltFrames;
  private TextureRegion[] arrowFrames;
  private TextureRegion[] strikeFrames;
  private TextureRegion[] waveFrames;
  private TextureRegion[] markerFrames;
  private Texture glow;
  private float elapsed;

  @Override
  public void create() {
    super.create();
    boss = entity.getComponent(ZeusBossComponent.class);
    boltFrames = frames(BOLT_SHEET, 4);
    arrowFrames = frames(ARROW_SHEET, 4);
    strikeFrames = frames(STRIKE_SHEET, 6);
    waveFrames = frames(SHOCKWAVE_SHEET, 6);
    markerFrames = frames(MARKER_SHEET, 2);
    glow = ServiceLocator.getResourceService().getAsset(GLOW, Texture.class);
  }

  private static TextureRegion[] frames(String sheetPath, int count) {
    Texture sheet = ServiceLocator.getResourceService().getAsset(sheetPath, Texture.class);
    int width = sheet.getWidth() / count;
    TextureRegion[] frames = new TextureRegion[count];
    for (int i = 0; i < count; i++) {
      frames[i] = new TextureRegion(sheet, i * width, 0, width, sheet.getHeight());
    }
    return frames;
  }

  @Override
  public void update() {
    elapsed += ServiceLocator.getTimeSource().getDeltaTime();
  }

  @Override
  protected void draw(SpriteBatch batch) {
    drawShockwaves(batch);
    drawStrikes(batch);
    drawBolts(batch);
    drawEdgeArrow(batch);
  }

  private void drawBolts(SpriteBatch batch) {
    for (ZeusBossComponent.Bolt bolt : boss.getBolts()) {
      Vector2 centre = bolt.getPosition();
      drawGlow(batch, centre.x, centre.y, 1.5f, 0.7f);
      batch.draw(
          looping(boltFrames, bolt.getAge(), 14f),
          centre.x - BOLT_WIDTH / 2f,
          centre.y - BOLT_HEIGHT / 2f,
          BOLT_WIDTH / 2f,
          BOLT_HEIGHT / 2f,
          BOLT_WIDTH,
          BOLT_HEIGHT,
          1f,
          1f,
          bolt.getVelocity().angleDeg());
    }
  }

  private void drawStrikes(SpriteBatch batch) {
    for (ZeusBossComponent.Strike strike : boss.getStrikes()) {
      float untilLanding = strike.getWarning() - strike.getAge();
      float left = strike.getX() - TILE / 2f;
      if (untilLanding > 0f) {
        // The warning: a gold marker and a glow on the spot, brighter just before it lands.
        boolean last = untilLanding <= STRIKE_FINAL_WARNING;
        drawGlow(
            batch, strike.getX(), strike.getGroundY() + 0.1f, last ? 2f : 1.5f, last ? 1f : 0.5f);
        batch.draw(
            looping(markerFrames, strike.getAge(), last ? 12f : 6f),
            left,
            strike.getGroundY(),
            TILE,
            MARKER_HEIGHT);
        batch.draw(
            strikeFrames[last ? 1 : 0],
            left,
            strike.getGroundY(),
            TILE,
            ZeusBossComponent.STRIKE_HEIGHT);
      } else {
        int frame = Math.min(5, 2 + (int) (-untilLanding * 10f));
        drawGlow(batch, strike.getX(), strike.getGroundY() + 0.5f, 3f, 0.9f);
        batch.draw(
            strikeFrames[frame], left, strike.getGroundY(), TILE, ZeusBossComponent.STRIKE_HEIGHT);
      }
    }
  }

  private void drawShockwaves(SpriteBatch batch) {
    float floorY = boss.getFloorY();

    // Before the wave: the floor lights up outwards from Zeus, tile by tile.
    float warning = boss.getShockwaveWarning();
    if (warning >= 0f) {
      float centre = entity.getCenterPosition().x;
      float reach = warning * ZeusBossComponent.SHOCKWAVE_LEAD;
      drawLitFloor(batch, centre - reach, centre + reach, floorY);
    }

    for (ZeusBossComponent.Wave wave : boss.getWaves()) {
      float direction = wave.getDirection();
      float front = wave.getX() + direction * ZeusBossComponent.SHOCKWAVE_WIDTH / 2f;
      float lead = front + direction * ZeusBossComponent.SHOCKWAVE_LEAD;
      drawLitFloor(batch, Math.min(front, lead), Math.max(front, lead), floorY);

      // The art faces right; a negative width mirrors it for a wave running left.
      float width = ZeusBossComponent.SHOCKWAVE_WIDTH * direction;
      drawGlow(batch, wave.getX(), floorY + 0.2f, 1.6f, 0.6f);
      batch.draw(
          looping(waveFrames, elapsed, 12f),
          wave.getX() - width / 2f,
          floorY,
          width,
          ZeusBossComponent.SHOCKWAVE_HEIGHT);
    }
  }

  /** Draws a marker on every floor tile between two world x positions, inside the arena. */
  private void drawLitFloor(SpriteBatch batch, float from, float to, float floorY) {
    from = Math.max(from, boss.getArenaLeft());
    to = Math.min(to, boss.getArenaRight());
    TextureRegion marker = looping(markerFrames, elapsed, 6f);
    for (int tile = MathUtils.floor(from / TILE); tile * TILE < to; tile++) {
      batch.draw(marker, tile * TILE, floorY, TILE, MARKER_HEIGHT);
    }
  }

  /** Points at Zeus from the edge of the screen while he winds up an attack out of view. */
  private void drawEdgeArrow(SpriteBatch batch) {
    if (!boss.isTelegraphing()) {
      return;
    }
    // The batch holds the camera's orthographic projection, so the view can be read back from it.
    float[] projection = batch.getProjectionMatrix().val;
    float halfWidth = 1f / projection[Matrix4.M00];
    float halfHeight = 1f / projection[Matrix4.M11];
    float viewX = -projection[Matrix4.M03] / projection[Matrix4.M00];
    float viewY = -projection[Matrix4.M13] / projection[Matrix4.M11];

    Vector2 centre = entity.getCenterPosition();
    float offsetX = centre.x - viewX;
    float offsetY = centre.y - viewY;
    boolean outsideX = Math.abs(offsetX) > halfWidth;
    boolean outsideY = Math.abs(offsetY) > halfHeight;
    if (!outsideX && !outsideY) {
      return;
    }

    float edgeX = halfWidth - ARROW_MARGIN - ARROW_SIZE / 2f;
    float edgeY = halfHeight - ARROW_MARGIN - ARROW_SIZE / 2f;
    float x = viewX + MathUtils.clamp(offsetX, -edgeX, edgeX);
    float y = viewY + MathUtils.clamp(offsetY, -edgeY, edgeY);
    // The art points right; turn it to face whichever edge Zeus is beyond.
    float angle = outsideX ? (offsetX > 0f ? 0f : 180f) : (offsetY > 0f ? 90f : 270f);

    batch.draw(
        looping(arrowFrames, elapsed, 8f),
        x - ARROW_SIZE / 2f,
        y - ARROW_SIZE / 2f,
        ARROW_SIZE / 2f,
        ARROW_SIZE / 2f,
        ARROW_SIZE,
        ARROW_SIZE,
        1f,
        1f,
        angle);
  }

  /** Adds a soft electric light to whatever is beneath it, centred on a point. */
  private void drawGlow(SpriteBatch batch, float x, float y, float size, float alpha) {
    int sourceBlend = batch.getBlendSrcFunc();
    int destinationBlend = batch.getBlendDstFunc();
    batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
    batch.setColor(1f, 1f, 1f, alpha);
    batch.draw(glow, x - size / 2f, y - size / 2f, size, size);
    batch.setColor(1f, 1f, 1f, 1f);
    batch.setBlendFunction(sourceBlend, destinationBlend);
  }

  private static TextureRegion looping(TextureRegion[] frames, float time, float fps) {
    return frames[Math.floorMod((int) (time * fps), frames.length)];
  }

  /** Effects are drawn over the characters, and under the weather. */
  @Override
  public float getZIndex() {
    return 1000f;
  }
}
