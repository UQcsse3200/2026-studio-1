package com.csse3200.game.entities.factories;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.player.ArrowMovementComponent;
import com.csse3200.game.components.projectile.ProjectileComponent;
import com.csse3200.game.components.projectile.ProjectileHitComponent;
import com.csse3200.game.components.projectile.StraightLineMovementStrategy;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.physics.PhysicsLayer;
import com.csse3200.game.physics.components.HitboxComponent;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.rendering.TextureRenderComponent;

// Factory for creating arrow projectiles.
public class ArrowFactory {

  /**
   * Creates a purely cosmetic arrow with no physics or collision, used by the player's bow weapon
   * (see {@link com.csse3200.game.components.player.WeaponAttackComponent}). Kept separate from
   * {@link #createRangedArrow} below - which enemies use, and which does have real physics-based
   * collision - since the two have different requirements entirely.
   */
  public static Entity createArrow(Vector2 position, Vector2 direction) {
    Entity arrow =
        new Entity()
            .addComponent(new TextureRenderComponent("images/arrow.png"))
            .addComponent(new ArrowMovementComponent(direction));

    arrow.setPosition(position);
    arrow.setScale(0.5f, 0.2f);

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
   *     the firing {@link com.csse3200.game.components.RangedAttackComponent}'s configured range.
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
            .addComponent(new TextureRenderComponent("images/arrow.png"))
            .addComponent(new PhysicsComponent().setBodyType(BodyType.KinematicBody))
            // Not on any layer of its own - nothing in the game currently needs to detect the
            // arrow itself via collision filtering, only the other way around (below).
            .addComponent(new HitboxComponent().setLayer(PhysicsLayer.NONE))
            // Carries the arrow's damage value only; health is irrelevant since the arrow itself
            // never takes damage. Lets ProjectileHitComponent reuse CombatStatsComponent#hit(...)
            // exactly like every other attack component in the game does.
            .addComponent(new CombatStatsComponent(1, damage))
            .addComponent(new ProjectileHitComponent(targetLayer, PhysicsLayer.OBSTACLE, knockback))
            .addComponent(
                new ProjectileComponent(
                    new StraightLineMovementStrategy(speed, movingRight), maxRange));

    arrow.setPosition(position);
    arrow.setScale(0.5f, 0.2f);

    return arrow;
  }

  private ArrowFactory() {
    throw new IllegalStateException("Instantiating static util class");
  }
}
