package com.csse3200.game.components.player;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.rendering.RenderComponent;
import com.csse3200.game.services.ServiceLocator;

/** Draws a brief impact flash over the enemy struck by the player's special attack. */
public class SpecialAttackEffectComponent extends RenderComponent {
  private static final String EFFECT_TEXTURE = "images/Shield.png";
  private static final float EFFECT_DURATION = 0.35f;
  private static final float INITIAL_SCALE = 1.5f;
  private static final float SCALE_GROWTH = 0.8f;

  private Texture texture;
  private Entity target;
  private float elapsed;

  @Override
  public void create() {
    super.create();
    texture = ServiceLocator.getResourceService().getAsset(EFFECT_TEXTURE, Texture.class);
    entity.getEvents().addListener("specialAttackHit", this::showImpact);
  }

  @Override
  public void update() {
    if (target == null) {
      return;
    }

    elapsed += ServiceLocator.getTimeSource().getDeltaTime();
    if (elapsed >= EFFECT_DURATION) {
      target = null;
    }
  }

  @Override
  public float getZIndex() {
    return target == null ? super.getZIndex() : -target.getPosition().y - 0.01f;
  }

  @Override
  protected void draw(SpriteBatch batch) {
    if (target == null) {
      return;
    }

    float progress = Math.min(elapsed / EFFECT_DURATION, 1f);
    float scale = INITIAL_SCALE + SCALE_GROWTH * progress;
    Vector2 targetPosition = target.getPosition();
    Vector2 targetScale = target.getScale();
    float width = targetScale.x * scale;
    float height = targetScale.y * scale;
    Color previousColor = batch.getColor().cpy();

    batch.setColor(1f, 0.3f, 0.08f, 1f - progress);
    batch.draw(
        texture,
        targetPosition.x + (targetScale.x - width) / 2f,
        targetPosition.y + (targetScale.y - height) / 2f,
        width,
        height);
    batch.setColor(previousColor);
  }

  private void showImpact(Entity target) {
    this.target = target;
    elapsed = 0f;
  }
}
