package com.csse3200.game.components.player;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.csse3200.game.areas.LevelGameArea;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.services.ServiceLocator;
import com.csse3200.game.ui.UIComponent;
import java.util.function.Supplier;

/** Displays the player's approximate progress through the game's three levels. */
public class ProgressionMiniMapDisplay extends UIComponent {
  static final int LEVEL_COUNT = 3;
  private static final float WIDTH = 86f;
  private static final float HEIGHT = 270f;
  private static final float LEFT_OFFSET = 8f;
  private static final float TOP_OFFSET = 340f;
  private static final String PLAYER_ICON = "images/knight_default.png";

  private final Supplier<LevelGameArea> areaSupplier;
  private ProgressionActor progressionActor;

  public ProgressionMiniMapDisplay(Supplier<LevelGameArea> areaSupplier) {
    this.areaSupplier = areaSupplier;
  }

  @Override
  public void create() {
    super.create();
    Texture playerTexture =
        ServiceLocator.getResourceService().getAsset(PLAYER_ICON, Texture.class);
    TextureRegion playerHead = new TextureRegion(playerTexture, 17, 0, 12, 15);
    progressionActor = new ProgressionActor(playerHead);
    progressionActor.setSize(WIDTH, HEIGHT);
    stage.addActor(progressionActor);
    update();
  }

  @Override
  public void update() {
    if (progressionActor == null) {
      return;
    }

    progressionActor.setPosition(LEFT_OFFSET, stage.getHeight() - TOP_OFFSET - HEIGHT);

    LevelGameArea area = areaSupplier.get();
    if (area == null || area.getPlayer() == null) {
      progressionActor.setVisible(false);
      return;
    }

    Entity player = area.getPlayer();
    progressionActor.setVisible(true);
    progressionActor.setProgress(
        overallProgress(
            area.getMapPath(),
            player.getCenterPosition(),
            area.getMapWorldWidth(),
            area.getMapWorldHeight()));
  }

  static float overallProgress(
      String mapPath, Vector2 playerPosition, float mapWidth, float mapHeight) {
    int levelIndex = levelIndex(mapPath);
    float levelProgress;

    // Levels 1 and 2 climb vertically. Level 3 is a horizontal palace approach.
    if (levelIndex == 2) {
      levelProgress = normalized(playerPosition.x, mapWidth);
    } else {
      levelProgress = normalized(playerPosition.y, mapHeight);
    }

    return (levelIndex + levelProgress) / LEVEL_COUNT;
  }

  static int levelIndex(String mapPath) {
    String normalizedPath = mapPath == null ? "" : mapPath.toLowerCase();
    if (normalizedPath.contains("level3")) {
      return 2;
    }
    if (normalizedPath.contains("level2")) {
      return 1;
    }
    return 0;
  }

  private static float normalized(float position, float mapSize) {
    if (mapSize <= 0f) {
      return 0f;
    }
    return MathUtils.clamp(position / mapSize, 0f, 1f);
  }

  @Override
  public void draw(SpriteBatch batch) {
    // Drawn by the Scene2D actor.
  }

  @Override
  public void dispose() {
    if (progressionActor != null) {
      progressionActor.remove();
    }
    super.dispose();
  }

  private static final class ProgressionActor extends Actor {
    private static final float LINE_WIDTH = 7f;
    private static final float DOT_SIZE = 12f;
    private static final float MARKER_WIDTH = 48f;
    private static final float MARKER_HEIGHT = 53f;
    private static final float MARKER_X_OFFSET = 8f;

    private final Drawable white = skin.newDrawable("white", Color.WHITE);
    private final TextureRegion playerHead;
    private float progress;

    private ProgressionActor(TextureRegion playerHead) {
      this.playerHead = playerHead;
    }

    private void setProgress(float progress) {
      this.progress = MathUtils.clamp(progress, 0f, 1f);
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
      float x = getX();
      float y = getY();
      float width = getWidth();
      float height = getHeight();
      float innerY = y + MARKER_HEIGHT / 2f;
      float innerHeight = height - MARKER_HEIGHT;
      float centreX = x + width / 2f;

      float markerY = innerY + progress * innerHeight - MARKER_HEIGHT / 2f;
      markerY = MathUtils.clamp(markerY, y, y + height - MARKER_HEIGHT);

      white.draw(batch, centreX - LINE_WIDTH / 2f, innerY, LINE_WIDTH, innerHeight);

      // Four boundary dots divide the line into the three level ranges.
      for (int i = 0; i <= LEVEL_COUNT; i++) {
        float dotY = innerY + innerHeight * i / LEVEL_COUNT;
        white.draw(batch, centreX - DOT_SIZE / 2f, dotY - DOT_SIZE / 2f, DOT_SIZE, DOT_SIZE);
      }

      Color previousBatchColor = new Color(batch.getColor());
      batch.setColor(Color.WHITE);
      batch.draw(
          playerHead,
          centreX - MARKER_WIDTH / 2f + MARKER_X_OFFSET,
          markerY,
          MARKER_WIDTH,
          MARKER_HEIGHT);
      batch.setColor(previousBatchColor);
    }
  }
}
