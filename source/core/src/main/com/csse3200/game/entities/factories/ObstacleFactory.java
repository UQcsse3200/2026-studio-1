package com.csse3200.game.entities.factories;

import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.physics.PhysicsLayer;
import com.csse3200.game.physics.PhysicsUtils;
import com.csse3200.game.physics.components.ColliderComponent;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.rendering.TextureRenderComponent;

/**
 * Factory to create obstacle entities.
 *
 * <p>Each obstacle entity type should have a creation method that returns a corresponding entity.
 */
public class ObstacleFactory {

  /**
   * Creates a tree entity.
   *
   * @return entity
   */
  public static Entity createTree() {
    Entity tree =
        new Entity()
            .addComponent(new TextureRenderComponent("images/tree.png"))
            .addComponent(new PhysicsComponent())
            .addComponent(new ColliderComponent().setLayer(PhysicsLayer.OBSTACLE));

    tree.getComponent(PhysicsComponent.class).setBodyType(BodyType.StaticBody);
    tree.getComponent(TextureRenderComponent.class).scaleEntity();
    tree.scaleHeight(2.5f);
    PhysicsUtils.setScaledCollider(tree, 0.5f, 0.2f);
    return tree;
  }

  /**
   * Creates an invisible physics wall.
   *
   * @param width Wall width in world units
   * @param height Wall height in world units
   * @return Wall entity of given width and height
   */
  public static Entity createWall(float width, float height) {
    Entity wall =
        new Entity()
            .addComponent(new PhysicsComponent().setBodyType(BodyType.StaticBody))
            .addComponent(new ColliderComponent().setLayer(PhysicsLayer.OBSTACLE));
    wall.setScale(width, height);
    return wall;
  }

  public static Entity createPlatform(float width, float height) {
    Entity platform =
        new Entity()
            .addComponent(new PhysicsComponent().setBodyType(BodyType.StaticBody))
            .addComponent(new ColliderComponent().setLayer(PhysicsLayer.OBSTACLE));

    platform.setScale(width, height);

    return platform;
  }

  /**
   * Creates a solid terrain tile collider.
   *
   * @param width width of the tile in world units
   * @param height height of the tile in world units
   * @return static solid collider entity
   */
  public static Entity createSolidTile(float width, float height) {
    Entity tile =
        new Entity()
            .addComponent(new PhysicsComponent())
            .addComponent(new ColliderComponent().setLayer(PhysicsLayer.OBSTACLE));

    tile.getComponent(PhysicsComponent.class).setBodyType(BodyType.StaticBody);

    tile.setScale(width, height);

    return tile;
  }

  /**
   * Creates a platform terrain tile collider.
   *
   * @param width width of the platform in world units
   * @param height height of the platform in world units
   * @return static platform collider entity
   */
  public static Entity createPlatformTile(float width, float height) {
    Entity platform =
        new Entity()
            .addComponent(new PhysicsComponent())
            .addComponent(new ColliderComponent().setLayer(PhysicsLayer.OBSTACLE));

    platform.getComponent(PhysicsComponent.class).setBodyType(BodyType.StaticBody);

    platform.setScale(width, height);

    return platform;
  }

  /**
   * Creates a hazard terrain tile collider.
   *
   * <p>The hazard is a sensor, meaning it detects contact with the player without physically
   * preventing the player from moving through it.
   *
   * @param width width of the hazard in world units
   * @param height height of the hazard in world units
   * @return static hazard sensor entity
   */
  public static Entity createHazardTile(float width, float height) {
    Entity hazard =
        new Entity()
            .addComponent(new PhysicsComponent())
            .addComponent(new ColliderComponent().setLayer(PhysicsLayer.OBSTACLE).setSensor(true));

    hazard.getComponent(PhysicsComponent.class).setBodyType(BodyType.StaticBody);

    hazard.setScale(width, height);

    return hazard;
  }

  private ObstacleFactory() {
    throw new IllegalStateException("Instantiating static util class");
  }
}
