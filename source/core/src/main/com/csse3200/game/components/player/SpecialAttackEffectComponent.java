package com.csse3200.game.components.player;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.rendering.RenderComponent;
import com.csse3200.game.services.ServiceLocator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/** Animates a slash over each enemy struck by a player special attack. */
public class SpecialAttackEffectComponent extends RenderComponent {
  private static final String SLASH_FRAME_PATH = "images/effects/special-slash/Slash_color4_frame";
  private static final int SLASH_FRAME_COUNT = 9;
  private static final float SLASH_FRAME_DURATION = 0.05f;
  private static final float SLASH_EFFECT_DURATION = SLASH_FRAME_COUNT * SLASH_FRAME_DURATION;
  private static final float EFFECT_SCALE = 2.2f;
  private static final String AREA_FRAME_PATH = "images/effects/area-vortex/Effect_TheVortex_1_";
  private static final int AREA_FRAME_COUNT = 30;
  private static final float AREA_FRAME_DURATION = 1f / 30f;
  private static final float AREA_EFFECT_DIAMETER = 4f;

  private Texture[] slashFrames;
  private Texture[] areaFrames;
  private final Map<Entity, Float> slashTargets = new HashMap<>();
  private Vector2 areaAttackPosition;
  private float areaAttackElapsed;

  @Override
  public void create() {
    super.create();
    slashFrames = new Texture[SLASH_FRAME_COUNT];
    for (int i = 0; i < SLASH_FRAME_COUNT; i++) {
      slashFrames[i] = new Texture(Gdx.files.internal(SLASH_FRAME_PATH + (i + 1) + ".png"));
      slashFrames[i].setFilter(TextureFilter.Nearest, TextureFilter.Nearest);
    }
    areaFrames = new Texture[AREA_FRAME_COUNT];
    for (int i = 0; i < AREA_FRAME_COUNT; i++) {
      areaFrames[i] =
          new Texture(Gdx.files.internal(AREA_FRAME_PATH + String.format("%03d", i) + ".png"));
      areaFrames[i].setFilter(TextureFilter.Linear, TextureFilter.Linear);
    }
    entity.getEvents().addListener("specialAttackHit", this::showImpact);
    entity.getEvents().addListener("areaAttackStarted", this::showAreaAttack);
  }

  @Override
  public void update() {
    float deltaTime = ServiceLocator.getTimeSource().getDeltaTime();
    Iterator<Map.Entry<Entity, Float>> iterator = slashTargets.entrySet().iterator();
    while (iterator.hasNext()) {
      Map.Entry<Entity, Float> target = iterator.next();
      float elapsed = target.getValue() + deltaTime;
      if (elapsed >= SLASH_EFFECT_DURATION) {
        iterator.remove();
      } else {
        target.setValue(elapsed);
      }
    }

    if (areaAttackPosition != null) {
      areaAttackElapsed += deltaTime;
      if (areaAttackElapsed >= AREA_FRAME_COUNT * AREA_FRAME_DURATION) {
        areaAttackPosition = null;
      }
    }
  }

  @Override
  public void dispose() {
    if (slashFrames != null) {
      for (Texture frame : slashFrames) {
        if (frame != null) {
          frame.dispose();
        }
      }
    }
    if (areaFrames != null) {
      for (Texture frame : areaFrames) {
        if (frame != null) {
          frame.dispose();
        }
      }
    }
    super.dispose();
  }

  @Override
  public float getZIndex() {
    if (areaAttackPosition == null && slashTargets.isEmpty()) {
      return super.getZIndex();
    }

    float lowestY = entity.getPosition().y;
    for (Entity target : slashTargets.keySet()) {
      lowestY = Math.min(lowestY, target.getPosition().y);
    }
    return -lowestY - 0.01f;
  }

  @Override
  protected void draw(SpriteBatch batch) {
    if (areaAttackPosition != null) {
      int frameIndex =
          Math.min((int) (areaAttackElapsed / AREA_FRAME_DURATION), AREA_FRAME_COUNT - 1);
      float halfSize = AREA_EFFECT_DIAMETER / 2f;
      batch.draw(
          areaFrames[frameIndex],
          areaAttackPosition.x - halfSize,
          areaAttackPosition.y - halfSize,
          AREA_EFFECT_DIAMETER,
          AREA_EFFECT_DIAMETER);
    }

    for (Map.Entry<Entity, Float> impact : slashTargets.entrySet()) {
      Entity target = impact.getKey();
      int frameIndex =
          Math.min((int) (impact.getValue() / SLASH_FRAME_DURATION), SLASH_FRAME_COUNT - 1);
      Vector2 targetPosition = target.getPosition();
      Vector2 targetScale = target.getScale();
      float size = Math.max(targetScale.x, targetScale.y) * EFFECT_SCALE;
      batch.draw(
          slashFrames[frameIndex],
          targetPosition.x + (targetScale.x - size) / 2f,
          targetPosition.y + (targetScale.y - size) / 2f,
          size,
          size);
    }
  }

  private void showImpact(Entity target) {
    slashTargets.put(target, 0f);
  }

  private void showAreaAttack() {
    areaAttackPosition = entity.getCenterPosition().cpy();
    areaAttackElapsed = 0f;
  }
}
