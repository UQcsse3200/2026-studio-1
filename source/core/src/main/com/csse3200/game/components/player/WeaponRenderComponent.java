package com.csse3200.game.components.player;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.rendering.RenderComponent;
import com.csse3200.game.services.ServiceLocator;

/** Renders the equipped weapon beside the player and provides a basic sword swing animation. */
public class WeaponRenderComponent extends RenderComponent {
  private static final float WEAPON_WIDTH = 0.6f;
  private static final float WEAPON_HEIGHT = 0.6f;
  private static final float SWING_DURATION = 0.3f;

  private final Texture texture;

  private boolean swinging;
  private float swingTime;

  public WeaponRenderComponent(String texturePath) {
    texture = ServiceLocator.getResourceService().getAsset(texturePath, Texture.class);
  }

  @Override
  public void create() {
    super.create();
    entity.getEvents().addListener("swordAttack", this::startSwing);
  }

  private void startSwing(int damage) {
    swinging = true;
    swingTime = 0f;
  }

  @Override
  public void update() {
    if (!swinging) {
      return;
    }

    swingTime += ServiceLocator.getTimeSource().getDeltaTime();

    if (swingTime >= SWING_DURATION) {
      swinging = false;
      swingTime = 0f;
    }
  }

  @Override
  protected void draw(SpriteBatch batch) {
    Vector2 playerPosition = entity.getPosition();
    Vector2 playerScale = entity.getScale();

    float weaponX = playerPosition.x + playerScale.x * 0.7f;
    float weaponY = playerPosition.y + playerScale.y * 0.35f;

    float rotation = 0f;

    if (swinging) {
      float progress = swingTime / SWING_DURATION;
      float swingAngle = -45f + (90f * progress);
      rotation = swingAngle;
    }

    batch.draw(
        texture,
        weaponX,
        weaponY,
        WEAPON_WIDTH / 2f,
        WEAPON_HEIGHT / 2f,
        WEAPON_WIDTH,
        WEAPON_HEIGHT,
        1f,
        1f,
        rotation,
        0,
        0,
        texture.getWidth(),
        texture.getHeight(),
        false,
        false);
  }
}
