package com.csse3200.game.components.pet;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.Component;
import com.csse3200.game.components.projectile.ProjectileMovementStrategy;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.physics.BodyUserData;
import com.csse3200.game.physics.PhysicsLayer;
import com.csse3200.game.physics.components.HitboxComponent;
import com.csse3200.game.services.ServiceLocator;

/**
 * Controls a pet projectile's flight, contact damage, and deferred cleanup.
 *
 * <p>Only the enemy confirmed by the player hit can take damage. Damage uses the owner's combat
 * stats for kill credit, without emitting {@code playerAttackHit} and triggering another assist.
 */
public class PetProjectileComponent extends Component {
  private static final float LIFETIME = 4f;

  private final Entity pet;
  private final Entity target;
  private final int damage;
  private final ProjectileMovementStrategy movement;
  private Entity owner;
  private HitboxComponent hitbox;
  private float timeAlive;
  private boolean expired;

  /**
   * @param pet companion that fired the projectile, with a {@link PetComponent}
   * @param target enemy that the player hit
   * @param damage non-negative damage dealt on contact
   * @param movement projectile's flight behaviour
   */
  public PetProjectileComponent(
      Entity pet, Entity target, int damage, ProjectileMovementStrategy movement) {
    if (pet == null || target == null || damage < 0 || movement == null) {
      throw new IllegalArgumentException(
          "Pet projectile requires a pet, target, damage and movement");
    }
    this.pet = pet;
    this.target = target;
    this.damage = damage;
    this.movement = movement;
  }

  @Override
  public void create() {
    PetComponent petComponent = pet.getComponent(PetComponent.class);
    hitbox = entity.getComponent(HitboxComponent.class);
    if (petComponent == null || hitbox == null) {
      throw new IllegalStateException("Pet projectile requires a pet owner and a hitbox");
    }
    owner = petComponent.getOwner();
    movement.start(entity);
    entity.getEvents().addListener("collisionStart", this::onCollisionStart);
  }

  @Override
  public void update() {
    if (expired || entity.isDisposed()) {
      return;
    }
    float delta = Math.max(0f, ServiceLocator.getTimeSource().getDeltaTime());
    timeAlive += delta;
    if (timeAlive >= LIFETIME || !canAttack()) {
      expire();
      return;
    }
    movement.update(entity, delta);
  }

  private void onCollisionStart(Fixture me, Fixture other) {
    if (expired || entity.isDisposed() || me != hitbox.getFixture()) {
      return;
    }
    if (!canAttack()) {
      expire();
      return;
    }

    short layer = other.getFilterData().categoryBits;
    if (PhysicsLayer.contains(PhysicsLayer.OBSTACLE, layer)) {
      expire();
      return;
    }
    if (!PhysicsLayer.contains(PhysicsLayer.NPC, layer)) {
      return;
    }
    Object userData = other.getBody().getUserData();
    if (!(userData instanceof BodyUserData bodyUserData) || bodyUserData.entity != target) {
      return;
    }

    // Resolve once before damage triggers death listeners or another fixture reports contact.
    expire();
    target
        .getComponent(CombatStatsComponent.class)
        .hit(owner.getComponent(CombatStatsComponent.class), damage);
  }

  private boolean canAttack() {
    return !pet.isDisposed()
        && target != owner
        && target != pet
        && isAlive(owner)
        && isAlive(target);
  }

  private static boolean isAlive(Entity candidate) {
    if (candidate.isDisposed()) {
      return false;
    }
    CombatStatsComponent stats = candidate.getComponent(CombatStatsComponent.class);
    return stats != null && !stats.isDead();
  }

  private void expire() {
    if (expired) {
      return;
    }
    expired = true;
    // Both collision callbacks and EntityService.update must finish before removing a body/entity.
    Gdx.app.postRunnable(entity::dispose);
  }

  @Override
  public void dispose() {
    expired = true;
  }
}
