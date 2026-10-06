package com.csse3200.game.components.attacks;

import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.components.loot.WeaponItem;
import com.csse3200.game.components.projectile.ProjectileType;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.factories.ProjectileFactory;
import com.csse3200.game.physics.PhysicsLayer;

/**
 * The Cyclops's thrown rock: a heavy, slow projectile that follows an arc under gravity and leaves
 * from above the head. Announces and listens under the event prefix {@code "rockAttack"}.
 *
 * <p>The arc is worked out once, when the rock leaves, so that it would land on the target's
 * position at that moment at the configured sideways speed. It aims at a point and does not home,
 * so a target that keeps moving can be missed. That is the intended counterplay.
 *
 * <p><b>Limitations:</b> a rock only damages the target layer or stops on obstacles; it does not
 * break scenery. It needs a rock texture (placeholder until the art exists). Look it up with {@code
 * getComponent(RockAttackComponent.class)}, because the base class lookup will not find it.
 *
 * <p><b>Style reference:</b> {@link RangedAttackComponent} (constructor shape and events).
 */
public class RockAttackComponent extends RangedAttackComponent {
  private static final String EVENT_PREFIX = "rockAttack";

  private final float spawnHeightFraction;
  private final float damageMultiplier;

  /**
   * Creates a rock attack.
   *
   * @param range maximum distance to the target; greater than zero
   * @param cooldown seconds between throws; greater than zero
   * @param knockback knockback on a hit; not negative
   * @param weapon the natural weapon (for example "Cyclops Rock Throw"); not null
   * @param spawnHeightFraction where the rock leaves the owner, from 0 (feet) to 1 (top)
   * @param damageMultiplier scales the weapon damage; greater than zero
   * @throws IllegalArgumentException if the fraction is outside 0 to 1 or the multiplier is not
   *     greater than zero, plus every rule of the ranged constructor
   */
  public RockAttackComponent(
      float range,
      float cooldown,
      float knockback,
      WeaponItem weapon,
      float spawnHeightFraction,
      float damageMultiplier) {
    super(range, cooldown, knockback, weapon, EVENT_PREFIX);
    if (range <= 0f) {
      throw new IllegalArgumentException("range must be greater than zero");
    }
    if (!(spawnHeightFraction >= 0f && spawnHeightFraction <= 1f)) {
      throw new IllegalArgumentException("spawnHeightFraction must be 0 to 1");
    }
    if (Float.isNaN(damageMultiplier) || damageMultiplier <= 0f) {
      throw new IllegalArgumentException("damageMultiplier must be greater than zero");
    }
    this.spawnHeightFraction = spawnHeightFraction;
    this.damageMultiplier = damageMultiplier;
  }

  /**
   * Builds the rock. The projectile type requested by the task is ignored.
   *
   * @param target the entity being thrown at
   * @param projectile ignored by this attack
   * @return the rock entity, not yet registered
   */
  @Override
  protected Entity createProjectile(Entity target, ProjectileType projectile) {
    Vector2 position = entity.getPosition();
    Vector2 scale = entity.getScale();
    Vector2 launch =
        new Vector2(position.x + scale.x / 2f, position.y + scale.y * spawnHeightFraction);
    int rockDamage = Math.max(1, Math.round(getDamage() * damageMultiplier));
    return ProjectileFactory.createRock(
        launch,
        target.getCenterPosition(),
        getProjectileSpeed(),
        getRange(),
        rockDamage,
        getKnockback(),
        PhysicsLayer.PLAYER);
  }

  /**
   * @return where the rock leaves the owner, from 0 (feet) to 1 (top)
   */
  public float getSpawnHeightFraction() {
    return spawnHeightFraction;
  }

  /**
   * @return the damage multiplier applied to the weapon damage
   */
  public float getDamageMultiplier() {
    return damageMultiplier;
  }
}
