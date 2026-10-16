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
  private static final String texturePath = "images/items/arrow.png";
  private static final BodyDef.BodyType bodyType = BodyDef.BodyType.KinematicBody;
  private static final ProjectileMovementStrategy movement =
      new AimedLineMovementStrategy(2.0f, new Vector2(20, 20));
  private static final float maxRange = 5.9f;
  private static final int damage = 3;
  private static final float knockback = 2.9f;
  private static final short targetLayer = PhysicsLayer.PLAYER;
  private static final short blockingLayer = PhysicsLayer.OBSTACLE;
  private static final Vector2 position = new Vector2(3, 2);
  private static final Vector2 scale = new Vector2(1.5f, 1.5f);
  private static final boolean mirrored = false;

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
                bodyType,
                movement,
                maxRange,
                damage,
                knockback,
                targetLayer,
                blockingLayer,
                position,
                scale,
                mirrored),
        "texturePath must not be blank");
    assertThrows(
        IllegalArgumentException.class,
        () ->
            ArrowFactory.createProjectileBase(
                "",
                bodyType,
                movement,
                maxRange,
                damage,
                knockback,
                targetLayer,
                blockingLayer,
                position,
                scale,
                mirrored),
        "texturePath must not be blank");
  }

  @Test
  public void shouldRejectNullBodyType() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            ArrowFactory.createProjectileBase(
                texturePath,
                null,
                movement,
                maxRange,
                damage,
                knockback,
                targetLayer,
                blockingLayer,
                position,
                scale,
                mirrored),
        "bodyType must not be null");
  }

  @Test
  public void shouldRejectNullMovement() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            ArrowFactory.createProjectileBase(
                texturePath,
                bodyType,
                null,
                maxRange,
                damage,
                knockback,
                targetLayer,
                blockingLayer,
                position,
                scale,
                mirrored),
        "texturePath must not be blank");
  }

  @Test
  public void shouldRejectNullPosition() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            ArrowFactory.createProjectileBase(
                texturePath,
                bodyType,
                movement,
                maxRange,
                damage,
                knockback,
                targetLayer,
                blockingLayer,
                null,
                scale,
                mirrored),
        "Position must not be null");
  }

  /** checks a exception is thrown for null scale */
  @Test
  public void shouldRejectANullScale() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            ArrowFactory.createProjectileBase(
                texturePath,
                bodyType,
                movement,
                maxRange,
                damage,
                knockback,
                targetLayer,
                blockingLayer,
                position,
                null,
                mirrored),
        "Should throw exception for scale being null");
  }

  /** Checks that scale works with valid scale vector input */
  @Test
  public void shouldBuildWithValidScale() {
    Entity projectile =
        ArrowFactory.createProjectileBase(
            texturePath,
            bodyType,
            movement,
            maxRange,
            damage,
            knockback,
            targetLayer,
            blockingLayer,
            position,
            scale,
            mirrored);
    assertEquals(
        scale.x,
        projectile.getScale().x,
        "Expected x axis scale was " + scale.x + " but scale was " + projectile.getScale().x);
    assertEquals(
        scale.y,
        projectile.getScale().y,
        "Expected y axis scale was " + scale.y + " but scale was " + projectile.getScale().y);
  }

  @Test
  public void shouldBuildWithValidComponentsPresent() {
    Entity projectile =
        ArrowFactory.createProjectileBase(
            texturePath,
            bodyType,
            movement,
            maxRange,
            damage,
            knockback,
            targetLayer,
            blockingLayer,
            position,
            scale,
            mirrored);
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
  public void shouldBuildWithValidDamageAndKnockback() {
    Entity projectile =
        ArrowFactory.createProjectileBase(
            texturePath,
            bodyType,
            movement,
            maxRange,
            damage,
            knockback,
            targetLayer,
            blockingLayer,
            position,
            scale,
            mirrored);
    assertEquals(damage, projectile.getComponent(CombatStatsComponent.class).getBaseAttack());
    assertEquals(
        knockback, projectile.getComponent(ProjectileHitComponent.class).getKnockbackForce());
  }

  @Test
  public void shouldBuildWithValidBodyTypeAndPhysicsLayers() {
    Entity projectile =
        ArrowFactory.createProjectileBase(
            texturePath,
            bodyType,
            movement,
            maxRange,
            damage,
            knockback,
            targetLayer,
            blockingLayer,
            position,
            scale,
            mirrored);
    assertEquals(bodyType, projectile.getComponent(PhysicsComponent.class).getBody().getType());
    assertEquals(
        targetLayer, projectile.getComponent(ProjectileHitComponent.class).getTargetLayer());
    assertEquals(
        blockingLayer, projectile.getComponent(ProjectileHitComponent.class).getBlockingLayer());
  }

  @Test
  public void shouldBuildWithValidMovementStrategyAndRange() {
    Entity projectile =
        ArrowFactory.createProjectileBase(
            texturePath,
            bodyType,
            movement,
            maxRange,
            damage,
            knockback,
            targetLayer,
            blockingLayer,
            position,
            scale,
            mirrored);
    assertEquals(
        movement, projectile.getComponent(ProjectileComponent.class).getMovementStrategy());
    assertEquals(maxRange, projectile.getComponent(ProjectileComponent.class).getMaxRange());
  }

  public void shouldNotBeFlippedOnDefault() {
    Entity projectile =
        ArrowFactory.createProjectileBase(
            texturePath,
            bodyType,
            movement,
            maxRange,
            damage,
            knockback,
            targetLayer,
            blockingLayer,
            position,
            scale,
            mirrored);
    assertFalse(
        projectile.getComponent(TextureRenderComponent.class).isFlipX(),
        "When mirrored is false; " + "mirrored is: " + mirrored + "Image should not be flipped.");
  }

  @Test
  public void shouldMirrorThePictureWhenAsked() {
    // note that the default mirrored value is false; so !mirrored = true which is what is wanted
    // for this test
    Entity projectile =
        ArrowFactory.createProjectileBase(
            texturePath,
            bodyType,
            movement,
            maxRange,
            damage,
            knockback,
            targetLayer,
            blockingLayer,
            position,
            scale,
            !mirrored);
    assertTrue(
        projectile.getComponent(TextureRenderComponent.class).isFlipX(),
        "When mirrored is true; " + "mirrored is: " + mirrored + "Image should also be flipped.");
  }
}
