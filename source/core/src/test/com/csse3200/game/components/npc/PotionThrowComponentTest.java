package com.csse3200.game.components.npc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.physics.box2d.Filter;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.physics.PhysicsLayer;
import com.csse3200.game.physics.PhysicsService;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

/**
 * Tests {@link PotionThrowComponent}: the Wizard's poison potion. The entity service is mocked, so
 * every entity the component spawns (potion, splash, poison icon) is captured instead of added to a
 * world, and the tests fly the potion by hand: two quarter-second updates arm it, then a collision
 * smashes it. The physics world is real but never stepped, so a potion smashes where it was
 * spawned: at (0.5, 1.3) for a thrower at the origin.
 */
@ExtendWith(GameExtension.class)
class PotionThrowComponentTest {
  private static final float RANGE = 6f;
  private static final float STEP = 0.25f;
  private static final float DELTA = 1e-4f;

  private Application originalApp;
  private EntityService entityService;
  private Entity thrower;
  private Entity target;
  private CombatStatsComponent targetStats;

  @BeforeEach
  void beforeEach() {
    originalApp = Gdx.app;
    Gdx.app = mock(Application.class);
    ServiceLocator.registerPhysicsService(new PhysicsService());
    ServiceLocator.registerRenderService(mock(RenderService.class));
    GameTime gameTime = mock(GameTime.class);
    when(gameTime.getDeltaTime()).thenReturn(STEP);
    ServiceLocator.registerTimeSource(gameTime);

    Texture texture = mock(Texture.class);
    when(texture.getWidth()).thenReturn(1);
    when(texture.getHeight()).thenReturn(1);
    ResourceService resources = mock(ResourceService.class);
    when(resources.getAsset(anyString(), eq(Texture.class))).thenReturn(texture);
    ServiceLocator.registerResourceService(resources);

    entityService = mock(EntityService.class);
    ServiceLocator.registerEntityService(entityService);

    thrower = new Entity().addComponent(new PotionThrowComponent(RANGE));
    thrower.setPosition(0f, 0f);
    thrower.setScale(1f, 1f);
    thrower.create();

    // Close enough to be caught by the splash of a potion that smashes at the thrower's head.
    targetStats = new CombatStatsComponent(100, 0);
    target = new Entity().addComponent(targetStats);
    target.setPosition(1f, 0.5f);
    target.setScale(1f, 1f);
    target.create();
  }

  @AfterEach
  void afterEach() {
    Gdx.app = originalApp;
  }

  // ---------- throwing ----------

  @Test
  void shouldThrowAPotionAtATargetInRange() {
    throwPotion();

    Entity potion = registeredEntities(1).get(0);
    assertNotNull(potion.getComponent(SplashPotionComponent.class));
  }

  @Test
  void shouldThrowAtATargetExactlyAtMaximumRange() {
    target.setPosition(RANGE, 0f);

    throwPotion();

    registeredEntities(1);
  }

  @Test
  void shouldNotThrowAtATargetOutOfRange() {
    target.setPosition(RANGE + 1f, 0f);

    throwPotion();

    verify(entityService, never()).register(any(Entity.class));
  }

  @Test
  void shouldNotThrowWithoutATarget() {
    thrower.getEvents().trigger("throwPoisonPotion", (Entity) null);

    verify(entityService, never()).register(any(Entity.class));
  }

  @Test
  void shouldStartThePotionJustAboveTheThrowersHead() {
    throwPotion();

    Entity potion = registeredEntities(1).get(0);
    assertEquals(0.25f, potion.getPosition().x, DELTA);
    assertEquals(1.05f, potion.getPosition().y, DELTA);
    assertEquals(0.5f, potion.getScale().x, DELTA);
    assertEquals(0.5f, potion.getScale().y, DELTA);
  }

  @Test
  void shouldLaunchRightwardsForATargetOnTheRight() {
    throwPotion();
    Entity potion = registeredEntities(1).get(0);

    potion.create();

    float vx = potion.getComponent(PhysicsComponent.class).getBody().getLinearVelocity().x;
    float vy = potion.getComponent(PhysicsComponent.class).getBody().getLinearVelocity().y;
    assertTrue(vx > 0f, "the potion should travel towards a target on the right");
    assertTrue(vy > 0f, "the potion should rise first");
  }

  @Test
  void shouldLaunchLeftwardsForATargetOnTheLeft() {
    target.setPosition(-3f, 0.5f);
    throwPotion();
    Entity potion = registeredEntities(1).get(0);

    potion.create();

    float vx = potion.getComponent(PhysicsComponent.class).getBody().getLinearVelocity().x;
    assertTrue(vx < 0f, "the potion should travel towards a target on the left");
  }

  // ---------- smashing ----------

  @Test
  void shouldPoisonATargetCaughtInTheSplash() {
    throwAndSmash();

    List<Entity> spawned = registeredEntities(3);
    assertTrue(hasPoison(spawned), "a poison effect should have been started");
  }

  @Test
  void shouldNotPoisonATargetOutsideTheSplash() {
    target.setPosition(5f, 0f);

    throwAndSmash();

    assertFalse(hasPoison(registeredEntities(2)), "only the potion and its splash");
  }

  @Test
  void shouldLetAShieldChargeAbsorbThePoison() {
    targetStats.setShieldHits(1);

    throwAndSmash();

    assertFalse(hasPoison(registeredEntities(2)));
    assertEquals(0, targetStats.getShieldHits());
  }

  @Test
  void shouldNotPoisonATargetThatNoLongerExists() {
    throwPotion();
    Entity potion = registeredEntities(1).get(0);
    target.dispose();

    smash(potion);

    assertFalse(hasPoison(registeredEntities(2)));
  }

  @Test
  void shouldNotPoisonATargetWithoutCombatStats() {
    Entity prop = new Entity();
    prop.setPosition(1f, 0.5f);
    prop.create();

    thrower.getEvents().trigger("throwPoisonPotion", prop);
    smash(registeredEntities(1).get(0));

    assertFalse(hasPoison(registeredEntities(2)));
  }

  // ---------- helpers ----------

  private void throwPotion() {
    thrower.getEvents().trigger("throwPoisonPotion", target);
  }

  private void throwAndSmash() {
    throwPotion();
    smash(registeredEntities(1).get(0));
  }

  /** Brings the potion into the world, arms it and lets it hit the ground. */
  private void smash(Entity potion) {
    potion.create();
    SplashPotionComponent flight = potion.getComponent(SplashPotionComponent.class);
    flight.update();
    flight.update();

    Filter filter = new Filter();
    filter.categoryBits = PhysicsLayer.OBSTACLE;
    Fixture ground = mock(Fixture.class);
    when(ground.getFilterData()).thenReturn(filter);
    potion.getEvents().trigger("collisionStart", mock(Fixture.class), ground);
  }

  /** Every entity registered so far, checking that exactly this many were. */
  private List<Entity> registeredEntities(int expected) {
    ArgumentCaptor<Entity> captor = ArgumentCaptor.forClass(Entity.class);
    verify(entityService, times(expected)).register(captor.capture());
    return captor.getAllValues();
  }

  private static boolean hasPoison(List<Entity> entities) {
    return entities.stream().anyMatch(e -> e.getComponent(PoisonEffectComponent.class) != null);
  }
}
