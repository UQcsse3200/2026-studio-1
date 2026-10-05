package com.csse3200.game.components.attacks;

import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.components.loot.WeaponItem;
import com.csse3200.game.components.projectile.ProjectileType;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.factories.ProjectileFactory;
import com.csse3200.game.physics.PhysicsLayer;

/**
 * The Cyclops's eye laser: a very fast, straight, thin projectile fired from eye height straight at
 * the target's position. Announces and listens under the event prefix {@code "laserAttack"}.
 *
 * <p>Reuses the straight-line aimed movement ({@code AimedLineMovementStrategy}) that the aimed
 * arrows already use, so there is no new physics here.
 *
 * <p><b>Limitations:</b> it is a fast projectile, not an instant beam, so it can be dodged at the
 * edge of its range. It needs a laser texture (placeholder until the art exists). Look it up with
 * {@code getComponent(LaserAttackComponent.class)}.
 *
 * <p><b>Style reference:</b> {@link RockAttackComponent} (same constructor shape).
 */
public class LaserAttackComponent extends RangedAttackComponent {
  private static final String EVENT_PREFIX = "laserAttack";

  private final float spawnHeightFraction;
  private final float damageMultiplier;

  /**
   * Creates a laser attack.
   *
   * @param range maximum distance to the target; greater than zero
   * @param cooldown seconds between shots; greater than zero
   * @param knockback knockback on a hit; not negative
   * @param weapon the natural weapon (for example "Cyclops Laser"); not null
   * @param spawnHeightFraction where the laser leaves the owner, from 0 (feet) to 1 (top)
   * @param damageMultiplier scales the weapon damage; greater than zero
   * @throws IllegalArgumentException same rules as the rock constructor
   */
  public LaserAttackComponent(
      float range,
      float cooldown,
      float knockback,
      WeaponItem weapon,
      float spawnHeightFraction,
      float damageMultiplier) {
    super(range, cooldown, knockback, weapon, EVENT_PREFIX);
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
   * Builds the laser. The projectile type requested by the task is ignored.
   *
   * @param target the entity being shot at
   * @param projectile ignored by this attack
   * @return the laser entity, not yet registered
   */
  @Override
  protected Entity createProjectile(Entity target, ProjectileType projectile) {
    Vector2 position = entity.getPosition();
    Vector2 scale = entity.getScale();
    Vector2 launch =
        new Vector2(position.x + scale.x / 2f, position.y + scale.y * spawnHeightFraction);
    Vector2 direction = target.getCenterPosition().sub(launch);
    if (direction.isZero()) {
      direction.set(1f, 0f);
    }
    int laserDamage = Math.max(1, Math.round(getDamage() * damageMultiplier));
    return ProjectileFactory.createLaser(
        launch,
        direction,
        getProjectileSpeed(),
        getRange(),
        laserDamage,
        getKnockback(),
        PhysicsLayer.PLAYER);
  }

  /**
   * @return where the laser leaves the owner, from 0 (feet) to 1 (top)
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
