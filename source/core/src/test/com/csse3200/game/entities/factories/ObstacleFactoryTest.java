package com.csse3200.game.entities.factories;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.physics.PhysicsLayer;
import com.csse3200.game.physics.PhysicsService;
import com.csse3200.game.physics.components.ColliderComponent;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.rendering.TextureRenderComponent;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class ObstacleFactoryTest {
  @BeforeEach
  void setUp() {
    ServiceLocator.registerPhysicsService(new PhysicsService());
  }

  @Test
  void shouldCreateNonBlockingHazardSensor() {
    Entity hazard = ObstacleFactory.createHazardTile(1f, 1f);

    hazard.create();

    ColliderComponent collider = hazard.getComponent(ColliderComponent.class);
    PhysicsComponent physics = hazard.getComponent(PhysicsComponent.class);
    assertEquals(PhysicsLayer.HAZARD, collider.getLayer());
    assertTrue(collider.getFixture().isSensor());
    assertEquals(BodyType.StaticBody, physics.getBody().getType());
  }

  @Test
  void shouldCreateOneWayPlatformAsAnObstacle() {
    Entity platform = ObstacleFactory.createOneWayPlatform(3f, 0.1f);

    platform.create();

    ColliderComponent collider = platform.getComponent(ColliderComponent.class);
    PhysicsComponent physics = platform.getComponent(PhysicsComponent.class);
    assertTrue(PhysicsLayer.contains(PhysicsLayer.PLATFORM, collider.getLayer()));
    assertTrue(PhysicsLayer.contains(PhysicsLayer.OBSTACLE, collider.getLayer()));
    assertEquals(BodyType.StaticBody, physics.getBody().getType());
  }

  @Test
  void shouldCreateTreeScaledToItsTexture() {
    registerTexture(32, 64);

    Entity tree = ObstacleFactory.createTree();

    assertNotNull(tree.getComponent(TextureRenderComponent.class));
    assertEquals(2.5f, tree.getScale().y, 1e-5f);
    assertEquals(1.25f, tree.getScale().x, 1e-5f);
    assertEquals(PhysicsLayer.OBSTACLE, tree.getComponent(ColliderComponent.class).getLayer());
  }

  @Test
  void shouldCreateTexturedPlatformOfGivenSize() {
    registerTexture(16, 16);

    Entity platform = ObstacleFactory.createPlatform(4f, 0.5f);
    platform.create();

    assertNotNull(platform.getComponent(TextureRenderComponent.class));
    assertEquals(new Vector2(4f, 0.5f), platform.getScale());
    assertStaticObstacle(platform);
  }

  @Test
  void shouldCreateInvisibleWall() {
    Entity wall = ObstacleFactory.createWall(1f, 10f);
    wall.create();

    assertEquals(new Vector2(1f, 10f), wall.getScale());
    assertStaticObstacle(wall);
  }

  @Test
  void shouldCreateSolidTile() {
    Entity tile = ObstacleFactory.createSolidTile(0.5f, 0.5f);
    tile.create();

    assertEquals(new Vector2(0.5f, 0.5f), tile.getScale());
    assertStaticObstacle(tile);
  }

  @Test
  void shouldCreateFloorTile() {
    Entity floor = ObstacleFactory.createFloorTile(0.5f, 0.1f);
    floor.create();

    assertEquals(new Vector2(0.5f, 0.1f), floor.getScale());
    assertStaticObstacle(floor);
  }

  private static void assertStaticObstacle(Entity entity) {
    ColliderComponent collider = entity.getComponent(ColliderComponent.class);
    assertEquals(PhysicsLayer.OBSTACLE, collider.getLayer());
    assertFalse(collider.getFixture().isSensor());
    assertEquals(
        BodyType.StaticBody, entity.getComponent(PhysicsComponent.class).getBody().getType());
  }

  private static void registerTexture(int width, int height) {
    Texture texture = mock(Texture.class);
    when(texture.getWidth()).thenReturn(width);
    when(texture.getHeight()).thenReturn(height);
    ResourceService resources = mock(ResourceService.class);
    when(resources.getAsset(anyString(), any())).thenReturn(texture);
    ServiceLocator.registerResourceService(resources);
    ServiceLocator.registerRenderService(mock(RenderService.class));
  }
}
