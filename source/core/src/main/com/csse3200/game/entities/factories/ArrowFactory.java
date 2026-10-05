package com.csse3200.game.entities.factories;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.attacks.LightningFreezeComponent;
import com.csse3200.game.components.player.ArrowMovementComponent;
import com.csse3200.game.components.player.PlayerProjectileHitComponent;
import com.csse3200.game.components.projectile.AimedLineMovementStrategy;
import com.csse3200.game.components.projectile.ProjectileComponent;
import com.csse3200.game.components.projectile.ProjectileHitComponent;
import com.csse3200.game.components.projectile.StraightLineMovementStrategy;
import com.csse3200.game.components.projectile.VerticalMovementStrategy;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.physics.PhysicsLayer;
import com.csse3200.game.physics.components.HitboxComponent;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.rendering.TextureRenderComponent;

/** Factory for creating arrow projectiles. */
public class ArrowFactory {

  /**
   * Creates a player arrow that travels in a straight line and hits enemies or obstacles. Kept
   * separate from {@link #createRangedArrow} below because player arrows route damage through the
   * player's combat stats.
   */
  public static Entity createArrow(Vector2 position, Vector2 direction, int damage, Entity owner) {
    if (position == null || direction == null || direction.isZero() || owner == null) {
      throw new IllegalArgumentException("Arrow position, direction, and owner must be valid.");
    }

    Vector2 normalizedDirection = direction.cpy().nor();
    float arrowWidth = 0.5f;
    float arrowHeight = 0.2f;
    Vector2 spawnCenter =
        position
            .cpy()
            .mulAdd(normalizedDirection, owner.getScale().x / 2f + arrowWidth / 2f + 0.02f);

    TextureRenderComponent renderer = new TextureRenderComponent("images/items/arrow.png");
    renderer.setRotationDegrees(normalizedDirection.angleDeg());

    Entity arrow =
        new Entity()
            .addComponent(renderer)
            .addComponent(new PhysicsComponent().setBodyType(BodyType.KinematicBody))
            .addComponent(
                new HitboxComponent()
                    .setLayer(PhysicsLayer.DEFAULT)
                    .setMask((short) (PhysicsLayer.NPC | PhysicsLayer.OBSTACLE)))
            .addComponent(new ArrowMovementComponent(normalizedDirection))
            .addComponent(new PlayerProjectileHitComponent(damage, owner));

    arrow.setScale(arrowWidth, arrowHeight);
    arrow.setPosition(spawnCenter.x - arrowWidth / 2f, spawnCenter.y - arrowHeight / 2f);

    return arrow;
  }

  /**
   * Creates an arrow projectile for a ranged enemy attack (e.g. a skeleton ranger): a real entity
   * that flies out, only deals damage on actual (Box2D) contact with {@code targetLayer}, is
   * blocked by anything on {@link PhysicsLayer#OBSTACLE} in its way, and despawns once it has
   * travelled {@code maxRange} world units without hitting anything.
   *
   * <p>Movement is a straight horizontal line (x-axis only) at a constant speed by default, via
   * {@link StraightLineMovementStrategy} - see {@link ProjectileComponent} for how to swap in a
   * different movement type later (a slower "follow" arrow, a lobbed/bow-arc shot, ...) without
   * changing anything here.
   *
   * @param position spawn position (world units) - typically just in front of the shooter, not its
   *     exact center, so the arrow doesn't spawn inside the shooter's own collider.
   * @param movingRight true to fire in the +x direction (target is to the right), false for -x.
   * @param speed travel speed in world units/second.
   * @param maxRange maximum distance the arrow can travel before despawning; should normally match
   *     the firing {@link com.csse3200.game.components.attacks.RangedAttackComponent}'s configured
   *     range.
   * @param damage damage dealt to whatever the arrow hits on {@code targetLayer}.
   * @param knockback knockback magnitude applied on a successful hit; {@code 0f} disables it.
   * @param targetLayer the physics layer the arrow deals damage to on contact (e.g. {@link
   *     PhysicsLayer#PLAYER}).
   * @return the arrow entity, not yet registered with the entity service.
   */
  public static Entity createRangedArrow(
      Vector2 position,
      boolean movingRight,
      float speed,
      float maxRange,
      int damage,
      float knockback,
      short targetLayer) {
    Entity arrow =
        new Entity()
            .addComponent(new TextureRenderComponent("images/items/arrow.png"))
            // Dynamic, not kinematic: Box2D only creates a contact when at least one of the two
            // bodies is dynamic, so a kinematic arrow never reported touching a static wall.
            .addComponent(new PhysicsComponent().setBodyType(BodyType.DynamicBody))
            // Not on any layer of its own - nothing in the game currently needs to detect the
            // arrow itself via collision filtering, only the other way around (below).
            .addComponent(new HitboxComponent())
            // Carries the arrow's damage value only; health is irrelevant since the arrow itself
            // never takes damage. Lets ProjectileHitComponent reuse CombatStatsComponent#hit(...)
            // exactly like every other attack component in the game does.
            .addComponent(new CombatStatsComponent(1, damage))
            .addComponent(new ProjectileHitComponent(targetLayer, PhysicsLayer.OBSTACLE, knockback))
            .addComponent(
                new ProjectileComponent(
                    new StraightLineMovementStrategy(speed, movingRight), maxRange));

    configureFlightBody(arrow);
    arrow.setPosition(position);
    arrow.setScale(0.5f, 0.2f);
    // The source art faces right by default; mirror it for a leftward shot so the arrow
    // visually points the direction it's actually travelling.
    arrow.getComponent(TextureRenderComponent.class).setFlipX(!movingRight);

    return arrow;
  }

  /**
   * Creates an enemy arrow that flies in a straight line along {@code direction} (for example from
   * a Harpy to the player's centre), so a shooter above or below its target can still hit it.
   * Everything else matches {@link #createRangedArrow}: damage only on real contact with {@code
   * targetLayer}, stopped by {@link PhysicsLayer#OBSTACLE}, despawned after {@code maxRange}.
   *
   * <p>The sprite is mirrored when the shot travels leftward, as for the x-only arrow. Rotating the
   * sprite to match a diagonal flight is not done here.
   *
   * @param position where the arrow's bottom-left corner starts
   * @param direction direction of travel; must not be null, zero length or non-finite
   * @param speed travel speed in world units/second; must be positive
   * @param maxRange maximum distance travelled before despawning; must be positive
   * @param damage damage dealt on a hit
   * @param knockback knockback magnitude on a hit; 0 disables it
   * @param targetLayer physics layer the arrow can damage
   * @return the arrow entity, not yet registered with the entity service
   * @throws IllegalArgumentException if position or direction is invalid, or speed or maxRange is
   *     not positive
   */
  public static Entity createAimedArrow(
      Vector2 position,
      Vector2 direction,
      float speed,
      float maxRange,
      int damage,
      float knockback,
      short targetLayer) {
    if (position == null) {
      throw new IllegalArgumentException("Arrow position must not be null.");
    }
    if (maxRange <= 0) {
      throw new IllegalArgumentException("maxRange must be positive");
    }
    // the strategy validates speed and direction (null, zero, non-finite)
    AimedLineMovementStrategy movement = new AimedLineMovementStrategy(speed, direction);
    Entity arrow =
        new Entity()
            .addComponent(new TextureRenderComponent("images/items/arrow.png"))
            .addComponent(new PhysicsComponent().setBodyType(BodyType.DynamicBody))
            .addComponent(new HitboxComponent())
            .addComponent(new CombatStatsComponent(1, damage))
            .addComponent(new ProjectileHitComponent(targetLayer, PhysicsLayer.OBSTACLE, knockback))
            .addComponent(new ProjectileComponent(movement, maxRange));
    configureFlightBody(arrow);
    arrow.setPosition(position);
    arrow.setScale(0.5f, 0.2f);
    arrow.getComponent(TextureRenderComponent.class).setFlipX(direction.x < 0f);
    return arrow;
  }

  /**
   * Makes an arrow's dynamic body fly like a bullet: no gravity pulling it down, no damping slowing
   * it and continuous collision detection (the default body has ground-friction damping, meant for
   * walking characters).
   */
  private static void configureFlightBody(Entity arrow) {
    arrow.getComponent(PhysicsComponent.class).getBody().setGravityScale(0f);
    arrow.getComponent(PhysicsComponent.class).getBody().setLinearDamping(0f);
    // a fast arrow must not skip over a thin collider between two physics steps
    arrow.getComponent(PhysicsComponent.class).getBody().setBullet(true);
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
    Vector2 scale = new Vector2(0.3125f, 3.125f);

    Entity bolt =
        new Entity()
            .addComponent(new TextureRenderComponent("images/enemies/lightning.png"))
            .addComponent(new PhysicsComponent().setBodyType(BodyType.KinematicBody))
            .addComponent(new HitboxComponent())
            .addComponent(new CombatStatsComponent(1, damage))
            // No blocking layer: lightning strikes through platforms. Pass PhysicsLayer.OBSTACLE
            // instead to have ceilings to shield the player.
            .addComponent(new ProjectileHitComponent(targetLayer, PhysicsLayer.NONE, 0f))
            .addComponent(new LightningFreezeComponent(freezeTicks))
            .addComponent(
                new ProjectileComponent(new VerticalMovementStrategy(speed), skyOffset + 2f));

    bolt.setScale(scale);
    // Entity position is the bottom-left corner, so shift left by half the width to centre the
    // bolt on the target, and spawn it skyOffset above.
    bolt.setPosition(targetPosition.x - scale.x / 2f, targetPosition.y + skyOffset);

    return bolt;
  }

  private ArrowFactory() {
    throw new IllegalStateException("Instantiating static util class");
  }
}
