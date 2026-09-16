package com.csse3200.game.components.projectile;

import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.components.Component;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.services.ServiceLocator;

/**
 * Drives a projectile entity's lifecycle: delegates per-frame movement to a swappable {@link
 * ProjectileMovementStrategy} (see that interface for why movement is pulled out into its own
 * type), and despawns the projectile once it has travelled {@code maxRange} world units from its
 * spawn point - measured as straight-line (Euclidean) distance from spawn, so this works regardless
 * of whether the configured movement strategy actually travels in a straight line.
 *
 * <p>A projectile can also be despawned earlier by something else - e.g. {@link
 * ProjectileHitComponent} - firing a {@code "projectileExpired"} event on the same entity, which
 * this component listens for. This keeps collision/damage resolution completely decoupled from
 * despawn/lifecycle logic: {@link ProjectileHitComponent} does not need to know this class exists.
 *
 * <p><b>Despawning does not call {@link Entity#dispose()}.</b> Instead it disables the entity and
 * moves it far outside the playable area. {@code Entity.dispose()} unregisters the entity from
 * {@link com.csse3200.game.entities.EntityService}, which removes it from the same array {@code
 * EntityService.update()} is iterating over ({@code entity.earlyUpdate(); entity.update();} for
 * every registered entity) - calling it from inside this component's own {@code update()} would
 * mutate that array mid-iteration. Nowhere else in the codebase disposes an entity from within its
 * own update loop for the same reason; the existing (non-physical) player arrow - {@link
 * com.csse3200.game.components.player.ArrowMovementComponent} - already uses this same
 * disable-and-relocate pattern for exactly this reason, and this class follows it too.
 */
public class ProjectileComponent extends Component {
  private static final float DESPAWN_POSITION = -1000f;

  private final ProjectileMovementStrategy movementStrategy;
  private final float maxRange;
  private Vector2 spawnPosition;
  private boolean expired = false;

  /**
   * @param movementStrategy how this projectile moves each frame - swap implementations to change
   *     movement type (straight-line, slow "follow", lobbed/bow-arc, ...) without changing this
   *     class or anything that spawns projectiles.
   * @param maxRange maximum distance, in world units, the projectile can travel from its spawn
   *     point before it despawns. Must be positive.
   * @throws IllegalArgumentException if maxRange is not positive.
   */
  public ProjectileComponent(ProjectileMovementStrategy movementStrategy, float maxRange) {
    if (maxRange <= 0) {
      throw new IllegalArgumentException("maxRange must be positive");
    }
    this.movementStrategy = movementStrategy;
    this.maxRange = maxRange;
  }

  @Override
  public void create() {
    spawnPosition = entity.getPosition();
    movementStrategy.start(entity);
    entity.getEvents().addListener("projectileExpired", this::despawn);
  }

  @Override
  public void update() {
    if (expired) {
      return;
    }

    float delta = ServiceLocator.getTimeSource().getDeltaTime();
    movementStrategy.update(entity, delta);

    if (entity.getPosition().dst(spawnPosition) >= maxRange) {
      despawn();
    }
  }

  /**
   * @return true once this projectile has despawned, either because it ran out of range or because
   *     something else (e.g. a hit) ended its flight early.
   */
  public boolean isExpired() {
    return expired;
  }

  private void despawn() {
    if (expired) {
      return;
    }
    expired = true;

    // Stop the body drifting on forever with whatever velocity it last had - it's about to be
    // teleported far away and disabled, but the underlying Box2D body otherwise keeps simulating.
    PhysicsComponent physicsComponent = entity.getComponent(PhysicsComponent.class);
    if (physicsComponent != null) {
      physicsComponent.getBody().setLinearVelocity(Vector2.Zero);
    }

    entity.setEnabled(false);
    entity.setPosition(DESPAWN_POSITION, DESPAWN_POSITION);
  }
}
