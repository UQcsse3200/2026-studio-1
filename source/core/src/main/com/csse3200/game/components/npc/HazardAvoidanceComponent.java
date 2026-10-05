package com.csse3200.game.components.npc;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.Shape;
import com.csse3200.game.components.Component;
import com.csse3200.game.physics.PhysicsLayer;
import com.csse3200.game.physics.components.ColliderComponent;
import com.csse3200.game.physics.components.MovementGuard;
import com.csse3200.game.physics.components.PhysicsMovementComponent;
import com.csse3200.game.physics.raycast.RaycastHit;
import com.csse3200.game.services.ServiceLocator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Makes a grounded enemy stop at the edge of a hazard instead of walking into it. It is the {@link
 * com.csse3200.game.physics.components.MovementGuard} of its entity: each frame {@code
 * PhysicsMovementComponent} asks it whether walking in a direction is blocked.
 *
 * <p><b>How it decides.</b> It casts one short horizontal ray, in the direction of travel, starting
 * at the collider's leading edge and a little above its feet, and looks only at the hazard layer.
 * If the ray hits a hazard within the look-ahead distance, the walk is blocked for that frame.
 * Nothing is remembered, so an enemy whose target moves to the other side simply walks away again.
 *
 * <p><b>Why a collider edge and not the sprite.</b> A Minotaur sprite is 4 units wide but its
 * collider is 3.2. Starting the ray at the sprite edge would stop it a unit too early.
 *
 * <p><b>Limitations:</b> it only sees hazards at foot height straight ahead. It does not path
 * around a hazard (enemies cannot jump), so a hazard strip across a corridor is a wall to them. It
 * does not stop knockback from pushing an enemy into a hazard. Attach it to grounded enemies only:
 * flyers hover over hazards and the Cerberus never moves.
 *
 * <p><b>Style reference:</b> {@code PlatformWanderTask} (the ray down for ledges) and {@code
 * FlightComponent} (a small component that configures a sibling component in {@code create()}).
 */
public class HazardAvoidanceComponent extends Component implements MovementGuard {
  private static final Logger logger = LoggerFactory.getLogger(HazardAvoidanceComponent.class);

  /**
   * How far ahead of the collider, in world units, a hazard is noticed. About one tile and a
   * quarter.
   */
  public static final float DEFAULT_LOOKAHEAD = 0.6f;

  /**
   * How far above the collider's bottom edge the ray runs, so it meets a tile standing on the
   * floor.
   */
  private static final float RAY_HEIGHT = 0.3f;

  private final float lookahead;

  /** Reused for every raycast so that no result object is allocated per frame. */
  private final RaycastHit hit = new RaycastHit();

  /** The solid collider whose leading edge the ray starts from; set in {@link #create()}. */
  private ColliderComponent collider;

  /** Creates the component with {@link #DEFAULT_LOOKAHEAD}. */
  public HazardAvoidanceComponent() {
    this.lookahead = DEFAULT_LOOKAHEAD;
  }

  /**
   * Creates the component with a chosen look-ahead.
   *
   * @param lookahead how far ahead of the collider's leading edge to look, in world units; greater
   *     than zero and finite
   * @throws IllegalArgumentException if the look-ahead is zero, negative, not a number or infinite
   */
  public HazardAvoidanceComponent(float lookahead) throws IllegalArgumentException {
    if (lookahead <= 0) {
      throw new IllegalArgumentException("Look Ahead cannot be zero or negative.");
    }
    if (Float.isNaN(lookahead)) {
      throw new IllegalArgumentException("Look Ahead must be a valid number.");
    }
    if (lookahead == Float.POSITIVE_INFINITY || lookahead == Float.NEGATIVE_INFINITY) {
      throw new IllegalArgumentException("Lookahead must be finite.");
    }
    this.lookahead = lookahead;
  }

  /** Registers this component as the movement guard of its entity. */
  @Override
  public void create() {
    PhysicsMovementComponent physicsMovement =
        this.getEntity().getComponent(PhysicsMovementComponent.class);
    if (physicsMovement == null) {
      logger.error("The hazard avoidance requires a movement component to work.");
      return;
    }
    collider = this.getEntity().getComponent(ColliderComponent.class);
    physicsMovement.setMovementGuard(this);
  }

  /**
   * Says whether walking in this direction runs into a hazard within the look-ahead.
   *
   * @param direction the unit direction about to be walked; read only
   * @return true if a hazard lies ahead within the look-ahead, false otherwise (including when the
   *     direction has no sideways part)
   */
  @Override
  public boolean blocks(Vector2 direction) {
    if (direction.x == 0) {
      return false;
    }
    int side = direction.x > 0 ? 1 : -1;

    if (collider == null) {
      return false;
    }
    Fixture fixture = collider.getFixture();
    if (fixture == null || fixture.getBody() == null) {
      return false;
    }
    Shape shape = fixture.getShape();
    if (!(shape instanceof PolygonShape)) {
      return false;
    }

    // Bounding box of the collider in world space, from its polygon vertices.
    PolygonShape polygon = (PolygonShape) shape;
    int vertexCount = polygon.getVertexCount();
    if (vertexCount == 0) {
      return false;
    }
    float minX = Float.POSITIVE_INFINITY;
    float maxX = Float.NEGATIVE_INFINITY;
    float minY = Float.POSITIVE_INFINITY;

    Vector2 vertex = new Vector2();
    for (int i = 0; i < vertexCount; i++) {
      polygon.getVertex(i, vertex);
      Vector2 world = fixture.getBody().getWorldPoint(vertex);
      minX = Math.min(minX, world.x);
      maxX = Math.max(maxX, world.x);
      minY = Math.min(minY, world.y);
    }

    float edgeX = side > 0 ? maxX : minX;
    float rayY = minY + RAY_HEIGHT;
    System.out.println("Start and End Y coordinate " + minY + " + " + RAY_HEIGHT + " = " + rayY);
    Vector2 start = new Vector2(edgeX, rayY);
    System.out.println("Start x coordinate: " + edgeX);
    Vector2 end = new Vector2(edgeX + side * lookahead, rayY);
    System.out.println(
        "End x coordinate "
            + edgeX
            + " + "
            + side * lookahead
            + " = "
            + (edgeX + (side * lookahead)));

    return ServiceLocator.getPhysicsService()
        .getPhysics()
        .raycast(start, end, PhysicsLayer.HAZARD, hit);
  }

  /**
   * @return the look-ahead distance in world units
   */
  public float getLookahead() {
    return lookahead;
  }
}
