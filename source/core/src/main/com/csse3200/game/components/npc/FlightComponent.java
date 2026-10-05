package com.csse3200.game.components.npc;

import com.csse3200.game.components.Component;
import com.csse3200.game.physics.components.PhysicsComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Makes an entity fly: zeroes its physics body's gravity scale once created, so the world's gravity
 * never pulls it down between steering updates, and applies linear damping so knockback impulses
 * fade out instead of carrying the entity away indefinitely (ground NPCs get the same effect for
 * free from floor friction, which a flying entity has none of).
 *
 * <p>Pairs with a {@link com.csse3200.game.physics.components.PhysicsMovementComponent} left in its
 * default (non-grounded) mode, which already steers toward the full 2D direction to a target -
 * including the vertical component that a grounded entity deliberately ignores (see {@link
 * com.csse3200.game.physics.components.PhysicsMovementComponent#setGroundedMovement}). With gravity
 * zeroed, that steering becomes the only vertical force acting on the entity, which is what keeps a
 * flying enemy (Harpy, Ranged Harpy) airborne while it wanders or chases instead of sinking like a
 * grounded one.
 *
 * <p>Must be added to an entity that also has a {@link PhysicsComponent}, and relies on {@link
 * PhysicsComponent#create()} having already run - true for every entity built the normal way, where
 * component creation order follows {@code addComponent} order and {@code PhysicsComponent} is
 * always added first (see {@code NPCFactory}'s {@code createBaseFlyingNPC}).
 */
public class FlightComponent extends Component {
  private float linearDamping;
  private static final Logger logger = LoggerFactory.getLogger(FlightComponent.class);

  /**
   * Creates a flight component.
   *
   * @param linearDamping how quickly velocity fades when nothing is steering; must not be negative
   *     (0 means no damping)
   * @throws IllegalArgumentException if negative or not a finite number
   */
  public FlightComponent(float linearDamping) throws IllegalArgumentException {
    if (linearDamping < 0) {
      throw new IllegalArgumentException("LinearDamping must not be negative.");
    }
    if (linearDamping == Float.POSITIVE_INFINITY || linearDamping == Float.NEGATIVE_INFINITY) {
      throw new IllegalArgumentException("LinearDamping must be a finite number.");
    }
    if (Float.isNaN(linearDamping)) {
      throw new IllegalArgumentException("LinearDamping must be a number.");
    }
    this.linearDamping = linearDamping;
  }

  /** Removes gravity from this entity's body and applies the configured damping. */
  @Override
  public void create() {
    PhysicsComponent physics = this.entity.getComponent(PhysicsComponent.class);
    if (physics == null || physics.getBody() == null) {
      logger.error("flight cannot be enabled on {}: it has no physics body", entity);
      return;
    }
    physics.getBody().setGravityScale(0f);
    physics.getBody().setLinearDamping(linearDamping);
  }

  /**
   * Returns the configured damping.
   *
   * @return the damping passed to the constructor
   */
  public float getLinearDamping() {
    return this.linearDamping;
  }
}
