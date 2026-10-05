package com.csse3200.game.components.npc;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.csse3200.game.components.Component;
import com.csse3200.game.physics.PhysicsLayer;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.services.ServiceLocator;
import java.util.function.Consumer;

/** Drives a thrown potion's arc and smashes it on the ground, a wall or the player. */
public class SplashPotionComponent extends Component {
  private static final float MAX_FLIGHT_SECONDS = 4f;
  private static final float ARM_SECONDS = 0.5f;
  private static final short SMASH_LAYERS = PhysicsLayer.OBSTACLE | PhysicsLayer.PLAYER;

  private final Vector2 launchVelocity;
  private final Consumer<Vector2> onSmash;
  private float flightTime = 0f;
  private boolean expired = false;

  /**
   * @param launchVelocity the potion's starting velocity, in world units/second.
   * @param onSmash called with the potion's centre when it smashes.
   */
  public SplashPotionComponent(Vector2 launchVelocity, Consumer<Vector2> onSmash) {
    this.launchVelocity = launchVelocity.cpy();
    this.onSmash = onSmash;
  }

  @Override
  public void create() {
    Body body = entity.getComponent(PhysicsComponent.class).getBody();
    body.setLinearDamping(0f);
    body.setLinearVelocity(launchVelocity);
    entity.getEvents().addListener("collisionStart", this::onCollisionStart);
  }

  @Override
  public void update() {
    flightTime += ServiceLocator.getTimeSource().getDeltaTime();
    if (!expired && flightTime >= MAX_FLIGHT_SECONDS) {
      despawn();
    }
  }

  /** Smashes the potion on the first ground, wall, platform or player it touches once armed. */
  private void onCollisionStart(Fixture me, Fixture other) {
    if (expired
        || flightTime < ARM_SECONDS
        || !PhysicsLayer.contains(SMASH_LAYERS, other.getFilterData().categoryBits)) {
      return;
    }
    onSmash.accept(entity.getCenterPosition());
    despawn();
  }

  /** Removes the potion after the current physics step. */
  private void despawn() {
    expired = true;
    Gdx.app.postRunnable(entity::dispose);
  }
}
