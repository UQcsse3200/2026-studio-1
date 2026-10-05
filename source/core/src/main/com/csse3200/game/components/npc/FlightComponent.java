package com.csse3200.game.components.npc;

import com.csse3200.game.components.Component;
import com.csse3200.game.physics.components.PhysicsComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
