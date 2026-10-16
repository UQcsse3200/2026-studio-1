package com.csse3200.game.entities.factories;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.projectile.AimedLineMovementStrategy;
import com.csse3200.game.components.projectile.ProjectileComponent;
import com.csse3200.game.components.projectile.ProjectileHitComponent;
import com.csse3200.game.components.projectile.ProjectileMovementStrategy;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.physics.PhysicsLayer;
import com.csse3200.game.physics.PhysicsService;
import com.csse3200.game.physics.components.HitboxComponent;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.rendering.TextureRenderComponent;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
public class ArrowFactoryProjectileBaseTest {
  private static final String ARROW_IMAGE = "images/items/arrow.png";
  private static final BodyDef.BodyType BODY_TYPE = BodyDef.BodyType.KinematicBody;
  private static final ProjectileMovementStrategy PROJECTILE_MOVEMENT_STRATEGY =
      new AimedLineMovementStrategy(2.0f, new Vector2(20, 20));
  private static final float MAX_RANGE = 5.9f;
  private static final int DAMAGE = 3;
  private static final float KNOCKBACK = 2.9f;
  private static final short TARGET_LAYER = PhysicsLayer.PLAYER;
  private static final short BLOCKING_LAYER = PhysicsLayer.OBSTACLE;
  private static final Vector2 POSITION = new Vector2(3, 2);
  private static final Vector2 SCALE = new Vector2(1.5f, 1.5f);
  private static final boolean MIRRORED = false;

  @BeforeEach
  void setUp() {
    Texture texture = mock(Texture.class);
    ResourceService resources = mock(ResourceService.class);
    when(resources.getAsset(anyString(), eq(Texture.class))).thenReturn(texture);

    ServiceLocator.registerResourceService(resources);
    ServiceLocator.registerRenderService(mock(RenderService.class));
    ServiceLocator.registerPhysicsService(new PhysicsService());
    ServiceLocator.registerEntityService(new EntityService());
    GameTime time = mock(GameTime.class);
    when(time.getDeltaTime()).thenReturn(0.02f);
    ServiceLocator.registerTimeSource(time);
  }

  @Test
  void shouldRejectNullOrBlankTexturePath() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            ArrowFactory.createProjectileBase(
                null,
                BODY_TYPE,
                PROJECTILE_MOVEMENT_STRATEGY,
                MAX_RANGE,
                DAMAGE,
                KNOCKBACK,
                TARGET_LAYER,
                BLOCKING_LAYER,
                POSITION,
                SCALE,
                MIRRORED),
        "texturePath must not be blank");
    assertThrows(
        IllegalArgumentException.class,
        () ->
            ArrowFactory.createProjectileBase(
                "",
                BODY_TYPE,
                PROJECTILE_MOVEMENT_STRATEGY,
                MAX_RANGE,
                DAMAGE,
                KNOCKBACK,
                TARGET_LAYER,
                BLOCKING_LAYER,
                POSITION,
                SCALE,
                MIRRORED),
        "texturePath must not be blank");
  }

  @Test
  void shouldRejectNullBodyType() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            ArrowFactory.createProjectileBase(
                ARROW_IMAGE,
                null,
                PROJECTILE_MOVEMENT_STRATEGY,
                MAX_RANGE,
                DAMAGE,
                KNOCKBACK,
                TARGET_LAYER,
                BLOCKING_LAYER,
                POSITION,
                SCALE,
                MIRRORED),
        "bodyType must not be null");
  }

  @Test
  void shouldRejectNullMovement() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            ArrowFactory.createProjectileBase(
                ARROW_IMAGE,
                BODY_TYPE,
                null,
                MAX_RANGE,
                DAMAGE,
                KNOCKBACK,
                TARGET_LAYER,
                BLOCKING_LAYER,
                POSITION,
                SCALE,
                MIRRORED),
        "texturePath must not be blank");
  }

  @Test
  void shouldRejectNullPosition() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            ArrowFactory.createProjectileBase(
                ARROW_IMAGE,
                BODY_TYPE,
                PROJECTILE_MOVEMENT_STRATEGY,
                MAX_RANGE,
                DAMAGE,
                KNOCKBACK,
                TARGET_LAYER,
                BLOCKING_LAYER,
                null,
                SCALE,
                MIRRORED),
        "Position must not be null");
  }

  /** checks a exception is thrown for null scale */
  @Test
  void shouldRejectANullScale() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            ArrowFactory.createProjectileBase(
                ARROW_IMAGE,
                BODY_TYPE,
                PROJECTILE_MOVEMENT_STRATEGY,
                MAX_RANGE,
                DAMAGE,
                KNOCKBACK,
                TARGET_LAYER,
                BLOCKING_LAYER,
                POSITION,
                null,
                MIRRORED),
        "Should throw exception for scale being null");
  }

  /** Checks that scale works with valid scale vector input */
  @Test
  void shouldBuildWithValidScale() {
    Entity projectile =
        ArrowFactory.createProjectileBase(
            ARROW_IMAGE,
            BODY_TYPE,
            PROJECTILE_MOVEMENT_STRATEGY,
            MAX_RANGE,
            DAMAGE,
            KNOCKBACK,
            TARGET_LAYER,
            BLOCKING_LAYER,
            POSITION,
            SCALE,
            MIRRORED);
    assertEquals(
        SCALE.x,
        projectile.getScale().x,
        "Expected x axis scale was " + SCALE.x + " but scale was " + projectile.getScale().x);
    assertEquals(
        SCALE.y,
        projectile.getScale().y,
        "Expected y axis scale was " + SCALE.y + " but scale was " + projectile.getScale().y);
  }

  @Test
  void shouldBuildWithValidComponentsPresent() {
    Entity projectile =
        ArrowFactory.createProjectileBase(
            ARROW_IMAGE,
            BODY_TYPE,
            PROJECTILE_MOVEMENT_STRATEGY,
            MAX_RANGE,
            DAMAGE,
            KNOCKBACK,
            TARGET_LAYER,
            BLOCKING_LAYER,
            POSITION,
            SCALE,
            MIRRORED);
    assertNotNull(
        projectile.getComponent(ProjectileComponent.class),
        "Projectile must have a Projectile component.");
    assertNotNull(
        projectile.getComponent(TextureRenderComponent.class),
        "Projectile must have a texture render component.");
    assertNotNull(
        projectile.getComponent(PhysicsComponent.class),
        "Projectile must have a Physics component.");
    assertNotNull(
        projectile.getComponent(HitboxComponent.class), "Projectile must have a Hitbox component.");
    assertNotNull(
        projectile.getComponent(CombatStatsComponent.class),
        "Projectile must have a CombatStats component.");
    assertNotNull(
        projectile.getComponent(ProjectileHitComponent.class),
        "Projectile must hav a ProjectileHit component.");
  }

  @Test
  void shouldBuildWithValidDamageAndKnockback() {
    Entity projectile =
        ArrowFactory.createProjectileBase(
            ARROW_IMAGE,
            BODY_TYPE,
            PROJECTILE_MOVEMENT_STRATEGY,
            MAX_RANGE,
            DAMAGE,
            KNOCKBACK,
            TARGET_LAYER,
            BLOCKING_LAYER,
            POSITION,
            SCALE,
            MIRRORED);
    assertEquals(DAMAGE, projectile.getComponent(CombatStatsComponent.class).getBaseAttack());
    assertEquals(
        KNOCKBACK, projectile.getComponent(ProjectileHitComponent.class).getKnockbackForce());
  }

  @Test
  void shouldBuildWithValidBodyTypeAndPhysicsLayers() {
    Entity projectile =
        ArrowFactory.createProjectileBase(
            ARROW_IMAGE,
            BODY_TYPE,
            PROJECTILE_MOVEMENT_STRATEGY,
            MAX_RANGE,
            DAMAGE,
            KNOCKBACK,
            TARGET_LAYER,
            BLOCKING_LAYER,
            POSITION,
            SCALE,
            MIRRORED);
    assertEquals(BODY_TYPE, projectile.getComponent(PhysicsComponent.class).getBody().getType());
    assertEquals(
        TARGET_LAYER, projectile.getComponent(ProjectileHitComponent.class).getTargetLayer());
    assertEquals(
        BLOCKING_LAYER, projectile.getComponent(ProjectileHitComponent.class).getBlockingLayer());
  }

  @Test
  void shouldBuildWithValidMovementStrategyAndRange() {
    Entity projectile =
        ArrowFactory.createProjectileBase(
            ARROW_IMAGE,
            BODY_TYPE,
            PROJECTILE_MOVEMENT_STRATEGY,
            MAX_RANGE,
            DAMAGE,
            KNOCKBACK,
            TARGET_LAYER,
            BLOCKING_LAYER,
            POSITION,
            SCALE,
            MIRRORED);
    assertEquals(
        PROJECTILE_MOVEMENT_STRATEGY,
        projectile.getComponent(ProjectileComponent.class).getMovementStrategy());
    assertEquals(MAX_RANGE, projectile.getComponent(ProjectileComponent.class).getMaxRange());
  }

  public void shouldNotBeFlippedOnDefault() {
    Entity projectile =
        ArrowFactory.createProjectileBase(
            ARROW_IMAGE,
            BODY_TYPE,
            PROJECTILE_MOVEMENT_STRATEGY,
            MAX_RANGE,
            DAMAGE,
            KNOCKBACK,
            TARGET_LAYER,
            BLOCKING_LAYER,
            POSITION,
            SCALE,
            MIRRORED);
    assertFalse(
        projectile.getComponent(TextureRenderComponent.class).isFlipX(),
        "When mirrored is false; " + "mirrored is: " + MIRRORED + "Image should not be flipped.");
  }

  @Test
  void shouldMirrorThePictureWhenAsked() {
    // note that the default mirrored value is false; so !mirrored = true which is what is wanted
    // for this test
    Entity projectile =
        ArrowFactory.createProjectileBase(
            ARROW_IMAGE,
            BODY_TYPE,
            PROJECTILE_MOVEMENT_STRATEGY,
            MAX_RANGE,
            DAMAGE,
            KNOCKBACK,
            TARGET_LAYER,
            BLOCKING_LAYER,
            POSITION,
            SCALE,
            !MIRRORED);
    assertTrue(
        projectile.getComponent(TextureRenderComponent.class).isFlipX(),
        "When mirrored is true; " + "mirrored is: " + MIRRORED + "Image should also be flipped.");
  }
}
