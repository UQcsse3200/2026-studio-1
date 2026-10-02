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
  private static final String FRAME_PATH = "images/effects/special-slash/Slash_color4_frame";
  private static final int FRAME_COUNT = 9;
  private static final float FRAME_DURATION = 0.05f;
  private static final float EFFECT_DURATION = FRAME_COUNT * FRAME_DURATION;
  private static final float EFFECT_SCALE = 2.2f;

  private Texture[] frames;
  private final Map<Entity, Float> targets = new HashMap<>();

  @Override
  public void create() {
    super.create();
    frames = new Texture[FRAME_COUNT];
    for (int i = 0; i < FRAME_COUNT; i++) {
      frames[i] = new Texture(Gdx.files.internal(FRAME_PATH + (i + 1) + ".png"));
      frames[i].setFilter(TextureFilter.Nearest, TextureFilter.Nearest);
    }
    entity.getEvents().addListener("specialAttackHit", this::showImpact);
    entity.getEvents().addListener("areaAttackHit", this::showImpact);
  }

  @Override
  public void update() {
    if (targets.isEmpty()) {
      return;
    }

    float deltaTime = ServiceLocator.getTimeSource().getDeltaTime();
    Iterator<Map.Entry<Entity, Float>> iterator = targets.entrySet().iterator();
    while (iterator.hasNext()) {
      Map.Entry<Entity, Float> target = iterator.next();
      float elapsed = target.getValue() + deltaTime;
      if (elapsed >= EFFECT_DURATION) {
        iterator.remove();
      } else {
        target.setValue(elapsed);
      }
    }
  }

  @Override
  public void dispose() {
    if (frames != null) {
      for (Texture frame : frames) {
        if (frame != null) {
          frame.dispose();
        }
      }
    }
    super.dispose();
  }

  @Override
  public float getZIndex() {
    float lowestY = Float.POSITIVE_INFINITY;
    for (Entity target : targets.keySet()) {
      lowestY = Math.min(lowestY, target.getPosition().y);
    }
    return targets.isEmpty() ? super.getZIndex() : -lowestY - 0.01f;
  }

  @Override
  protected void draw(SpriteBatch batch) {
    for (Map.Entry<Entity, Float> impact : targets.entrySet()) {
      Entity target = impact.getKey();
      int frameIndex = Math.min((int) (impact.getValue() / FRAME_DURATION), FRAME_COUNT - 1);
      Vector2 targetPosition = target.getPosition();
      Vector2 targetScale = target.getScale();
      float size = Math.max(targetScale.x, targetScale.y) * EFFECT_SCALE;
      batch.draw(
          frames[frameIndex],
          targetPosition.x + (targetScale.x - size) / 2f,
          targetPosition.y + (targetScale.y - size) / 2f,
          size,
          size);
    }
  }

  private void showImpact(Entity target) {
    targets.put(target, 0f);
  }
}
