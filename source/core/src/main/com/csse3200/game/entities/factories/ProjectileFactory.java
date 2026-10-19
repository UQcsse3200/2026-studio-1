package com.csse3200.game.entities.factories;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.enemies.attacks.LightningFreezeComponent;
import com.csse3200.game.components.projectile.*;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.physics.PhysicsLayer;
import com.csse3200.game.physics.components.HitboxComponent;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.rendering.TextureRenderComponent;

/**
 * Creates the Cyclops's two enemy projectiles. Built like {@code ArrowFactory} so hit handling,
 * range expiry and the shooter's hit re-announcement work the same way. Only the texture, size and
 * flight path differ.
 *
 * <p><b>Style reference:</b> {@code ArrowFactory.createAimedArrow} (body, layers, hit component)
 * and its private flight-body helper.
 *
 * <p><b>Assets:</b> keep the texture paths in the two constants below. Use existing images as
 * placeholders until rock and laser art exists.
 */
public class ProjectileFactory {

  private static final String LIGHTNING_IMAGE = "images/enemies/lightning.png";
  private static final String LASER_TEXTURE = "images/items/laser.png";
  private static final String ROCK_TEXTURE = "images/items/rock.png";

  private static final Vector2 LIGHTNING_SCALE = new Vector2(0.3125f, 3.125f);
  private static final Vector2 LASER_SCALE = new Vector2(1.2f, 0.5f);
  private static final Vector2 ROCK_SCALE = new Vector2(0.5f, 0.5f);

  private ProjectileFactory() {
    // Utility class
  }

  /**
   * Creates a lightning bolt that spawns in the sky above a target position and falls straight
   * down. On contact with {@code targetLayer} it deals damage and freezes the target via the
   * target's {@code SpeedEffectComponent} (through the "applySpeedEffect" event).
   *
   * @param targetPosition position of the target at the moment of the strike. Sampled once, so a
   *     moving target can dodge.
   * @param skyOffset how far above the target the bolt spawns.
   * @param speed fall speed in world units/second.
   * @param damage damage on hit; 0 for a pure freeze.
   * @param freezeTicks how many ticks the target is frozen for (about 60 per second).
   * @param targetLayer physics layer the bolt hits (e.g. {@link PhysicsLayer#PLAYER}).
   * @return the bolt entity, not yet registered with the entity service.
   */
  public static Entity createLightning(
      Vector2 targetPosition,
      float skyOffset,
      float speed,
      int damage,
      int freezeTicks,
      short targetLayer) {

    Entity bolt =
        new Entity()
            .addComponent(new TextureRenderComponent(LIGHTNING_IMAGE))
            .addComponent(new PhysicsComponent().setBodyType(BodyType.KinematicBody))
            .addComponent(new HitboxComponent())
            .addComponent(new CombatStatsComponent(1, damage))
            // No blocking layer: lightning strikes through platforms.
            // Pass PhysicsLayer.OBSTACLE instead to have ceilings to shield the player.
            .addComponent(new ProjectileHitComponent(targetLayer, PhysicsLayer.NONE, 0f))
            .addComponent(new LightningFreezeComponent(freezeTicks))
            .addComponent(
                new ProjectileComponent(new VerticalMovementStrategy(speed), skyOffset + 2f));

    bolt.setScale(LIGHTNING_SCALE);
    // Entity position is the bottom-left corner, so shift left by half the width to centre the
    // bolt on the target, and spawn it skyOffset above.
    bolt.setPosition(targetPosition.x - LIGHTNING_SCALE.x / 2f, targetPosition.y + skyOffset);

    return bolt;
  }

  /**
   * Creates an eye laser: a fast, thin projectile flying straight along a direction.
   *
   * @param position launch position (the eye), the centre of the laser (not its corner); not null
   * @param direction direction of travel; not zero length, not non-finite
   * @param speed speed in world units per second; greater than zero
   * @param maxRange distance before it despawns; greater than zero
   * @param damage damage on contact with the target layer
   * @param knockback knockback magnitude; 0 disables it
   * @param targetLayer the layer it damages
   * @return the laser entity, not yet registered
   * @throws IllegalArgumentException if a number or the direction is invalid
   */
  public static Entity createLaser(
      Vector2 position,
      Vector2 direction,
      float speed,
      float maxRange,
      int damage,
      float knockback,
      short targetLayer) {
    if (position == null || direction == null) {
      throw new IllegalArgumentException("position and direction must not be null");
    }
    if (!Float.isFinite(direction.x) || !Float.isFinite(direction.y) || direction.isZero()) {
      throw new IllegalArgumentException("direction must be finite and not zero length");
    }
    requireFinitePositive(speed, "speed");
    requireFinitePositive(maxRange, "maxRange");

    ProjectileMovementStrategy strategy = new AimedLineMovementStrategy(speed, direction);
    // The base wants the bottom-left corner; the launch position is the laser's centre.
    Vector2 bottomLeft = position.cpy().sub(LASER_SCALE.x / 2f, LASER_SCALE.y / 2f);
    // Dynamic like the aimed arrow, so it is stopped by obstacles.
    Entity laser =
        ArrowFactory.createProjectileBase(
            LASER_TEXTURE,
            BodyType.DynamicBody,
            strategy,
            maxRange,
            damage,
            knockback,
            targetLayer,
            PhysicsLayer.OBSTACLE,
            bottomLeft,
            LASER_SCALE.cpy(),
            direction.x < 0f);

    laser.getComponent(TextureRenderComponent.class).setRotationDegrees(direction.angleDeg());
    return laser;
  }

  /**
   * Creates a lobbed rock. Its launch velocity is chosen so that, under world gravity, it reaches
   * {@code targetPosition} while travelling {@code horizontalSpeed} sideways.
   *
   * @param position launch position, the centre of the rock (not its corner); not null
   * @param targetPosition the point to land on, read once at launch; not null
   * @param horizontalSpeed sideways speed in world units per second; greater than zero
   * @param maxRange distance before it despawns; greater than zero
   * @param damage damage on contact with the target layer
   * @param knockback knockback magnitude; 0 disables it
   * @param targetLayer the layer it damages
   * @return the rock entity, not yet registered
   * @throws IllegalArgumentException if a number is invalid or a position is null
   */
  public static Entity createRock(
      Vector2 position,
      Vector2 targetPosition,
      float horizontalSpeed,
      float maxRange,
      int damage,
      float knockback,
      short targetLayer) {
    if (position == null || targetPosition == null) {
      throw new IllegalArgumentException("position and targetPosition must not be null");
    }
    requireFinitePositive(horizontalSpeed, "horizontalSpeed");
    requireFinitePositive(maxRange, "maxRange");

    ProjectileMovementStrategy strategy = new ArcMovementStrategy(horizontalSpeed, targetPosition);
    // The base wants the bottom-left corner; the launch position is the rock's centre.
    Vector2 bottomLeft = position.cpy().sub(ROCK_SCALE.x / 2f, ROCK_SCALE.y / 2f);
    // Dynamic so it touches walls; its arc strategy switches gravity back on when it starts.
    return ArrowFactory.createProjectileBase(
        ROCK_TEXTURE,
        BodyType.DynamicBody,
        strategy,
        maxRange,
        damage,
        knockback,
        targetLayer,
        PhysicsLayer.OBSTACLE,
        bottomLeft,
        ROCK_SCALE.cpy(),
        targetPosition.x < position.x);
  }

  private static void requireFinitePositive(float value, String name) {
    if (!(value > 0f) || Float.isInfinite(value)) {
      throw new IllegalArgumentException(name + " must be positive and finite");
    }
  }
}
