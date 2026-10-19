package com.csse3200.game.components.npc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.Animation.PlayMode;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.badlogic.gdx.physics.box2d.Filter;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.physics.BodyUserData;
import com.csse3200.game.physics.PhysicsLayer;
import com.csse3200.game.physics.PhysicsService;
import com.csse3200.game.physics.components.ColliderComponent;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.rendering.AnimationRenderComponent;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tests {@link HeadbuttAttackComponent}: the Satyr's wind-up, charge, headbutt and recovery.
 *
 * <p>Time is stepped by hand with a fixed delta that is exact in binary (0.25s), so the attack's
 * timers land on whole steps: two updates finish the 0.5s wind-up, a charge lasts at most five
 * updates (1.2s limit), the headbutt takes one update (0.25s) and the recovery two (0.4s). The
 * physics world is real but never stepped, so the tests read the velocity the component sets.
 */
@ExtendWith(GameExtension.class)
class HeadbuttAttackComponentTest {
  private static final float STEP = 0.25f;
  private static final float CHARGE_SPEED = 5f;
  private static final int HEADBUTT_DAMAGE = 10;
  private static final float DELTA = 1e-6f;

  private EntityService entityService;
  private CombatStatsComponent playerStats;
  private Entity player;
  private Entity satyr;
  private ProvokedComponent provoked;
  private HeadbuttAttackComponent headbutt;
  private AnimationRenderComponent animator;

  @BeforeEach
  void beforeEach() {
    ServiceLocator.registerPhysicsService(new PhysicsService());
    ServiceLocator.registerRenderService(mock(RenderService.class));
    GameTime gameTime = mock(GameTime.class);
    when(gameTime.getDeltaTime()).thenReturn(STEP);
    ServiceLocator.registerTimeSource(gameTime);
    entityService = mock(EntityService.class);
    ServiceLocator.registerEntityService(entityService);

    // The target stands to the right of the satyr, with a real body so it can be knocked back.
    playerStats = new CombatStatsComponent(100, 0);
    player =
        new Entity()
            .addComponent(playerStats)
            .addComponent(new PhysicsComponent())
            .addComponent(new ColliderComponent());
    player.setPosition(3f, 0f);
    player.create();

    animator =
        new AnimationRenderComponent(
            new TextureAtlas(Gdx.files.internal("images/npcs/npc3.atlas")), true);
    for (String name :
        new String[] {
          "idler", "idlel", "windupr", "windupl", "charger", "chargel", "headbuttr", "headbuttl"
        }) {
      animator.addAnimation(name, 0.25f, PlayMode.LOOP);
    }

    provoked = new ProvokedComponent(8f);
    headbutt = new HeadbuttAttackComponent(player);
    satyr =
        new Entity()
            .addComponent(new PhysicsComponent())
            .addComponent(animator)
            .addComponent(provoked)
            .addComponent(headbutt);
    satyr.setPosition(0f, 0f);
    satyr.setScale(1f, 1f);
    satyr.create();
  }

  // ---------- starting the attack ----------

  @Test
  void shouldMarkTheSatyrAsAttackingWhenTheHeadbuttStarts() {
    startHeadbutt();

    assertTrue(provoked.isAttacking());
  }

  @Test
  void shouldIgnoreAHeadbuttWithoutATarget() {
    satyr.getEvents().trigger("headbutt", (Entity) null);

    assertFalse(provoked.isAttacking());
  }

  @Test
  void shouldIgnoreAnotherHeadbuttWhileOneIsRunning() {
    startHeadbutt();
    provoked.setAttacking(false);

    startHeadbutt();

    assertFalse(provoked.isAttacking(), "a second event must not restart a running attack");
  }

  @Test
  void shouldDoNothingWhileThereIsNoAttack() {
    setSatyrVelocity(3f);

    headbutt.update();

    assertEquals(3f, satyrVelocityX(), DELTA);
    assertFalse(provoked.isAttacking());
  }

  // ---------- wind-up and charge ----------

  @Test
  void shouldStandStillDuringTheWindup() {
    setSatyrVelocity(4f);
    startHeadbutt();

    update(1);

    assertEquals(0f, satyrVelocityX(), DELTA);
  }

  @Test
  void shouldChargeRightAfterTheWindupWhenTheTargetIsOnTheRight() {
    addFloor();
    startHeadbutt();

    update(3);

    assertEquals(CHARGE_SPEED, satyrVelocityX(), DELTA);
  }

  @Test
  void shouldChargeLeftAfterTheWindupWhenTheTargetIsOnTheLeft() {
    player.setPosition(-4f, 0f);
    addFloor();
    startHeadbutt();

    update(3);

    assertEquals(-CHARGE_SPEED, satyrVelocityX(), DELTA);
  }

  @Test
  void shouldStopTheChargeAtAPlatformEdge() {
    // No floor in the world, so there is no ground ahead of the satyr.
    startHeadbutt();

    update(3);

    assertEquals(0f, satyrVelocityX(), DELTA);
    assertTrue(provoked.isAttacking(), "the satyr still has to recover");
  }

  @Test
  void shouldStopTheChargeAfterItsMaximumTime() {
    addFloor();
    startHeadbutt();
    update(2);

    update(4);
    assertEquals(CHARGE_SPEED, satyrVelocityX(), DELTA);

    update(1);
    assertEquals(0f, satyrVelocityX(), DELTA);
  }

  @Test
  void shouldStopTheChargeWhenBumpingIntoAnotherBody() {
    addFloor();
    startHeadbutt();
    update(2);

    satyr
        .getEvents()
        .trigger("collisionStart", mock(Fixture.class), solidFixtureOn(PhysicsLayer.NPC));
    update(1);

    assertEquals(0f, satyrVelocityX(), DELTA);
  }

  @Test
  void shouldKeepChargingWhenTouchingAnObstacle() {
    addFloor();
    startHeadbutt();
    update(2);

    satyr
        .getEvents()
        .trigger("collisionStart", mock(Fixture.class), solidFixtureOn(PhysicsLayer.OBSTACLE));
    update(1);

    assertEquals(CHARGE_SPEED, satyrVelocityX(), DELTA);
  }

  // ---------- hitting the player ----------

  @Test
  void shouldDamageThePlayerOnContactAndSpawnAnImpact() {
    startHeadbutt();
    update(2);

    touchPlayer();
    update(1);

    assertEquals(100 - HEADBUTT_DAMAGE, playerStats.getHealth());
    verify(entityService).register(any(Entity.class));
  }

  @Test
  void shouldKnockThePlayerAwayFromTheSatyr() {
    startHeadbutt();
    update(2);

    touchPlayer();
    update(1);

    assertTrue(playerVelocityX() > 0f, "a target on the right is pushed right");
  }

  @Test
  void shouldKnockAPlayerOnTheLeftToTheLeft() {
    player.setPosition(-4f, 0f);
    startHeadbutt();
    update(2);

    touchPlayer();
    update(1);

    assertTrue(playerVelocityX() < 0f, "a target on the left is pushed left");
  }

  @Test
  void shouldLetAShieldChargeAbsorbTheHit() {
    playerStats.setShieldHits(1);
    startHeadbutt();
    update(2);

    touchPlayer();
    update(1);

    assertEquals(100, playerStats.getHealth());
    assertEquals(0, playerStats.getShieldHits());
    assertEquals(0f, playerVelocityX(), DELTA, "a blocked hit must not knock the player back");
  }

  @Test
  void shouldNotHitAPlayerWhoHasAlreadyLeftTheContact() {
    addFloor();
    startHeadbutt();
    update(2);

    touchPlayer();
    satyr.getEvents().trigger("collisionEnd", mock(Fixture.class), fixtureOn(PhysicsLayer.PLAYER));
    update(1);

    assertEquals(100, playerStats.getHealth());
    assertEquals(CHARGE_SPEED, satyrVelocityX(), DELTA);
  }

  @Test
  void shouldFinishTheAttackAfterTheHitAndTheRecovery() {
    startHeadbutt();
    update(2);
    touchPlayer();

    update(1); // hit
    update(1); // headbutt over, recovering
    update(1);
    assertTrue(provoked.isAttacking(), "still recovering");

    update(1);
    assertFalse(provoked.isAttacking(), "attack over, control handed back");
  }

  @Test
  void shouldFinishTheAttackAfterAChargeThatEndedAtAnEdge() {
    startHeadbutt();
    update(3); // wind-up, then the charge ends straight away (no floor)

    update(1);
    assertTrue(provoked.isAttacking(), "still recovering");

    update(1);
    assertFalse(provoked.isAttacking(), "attack over, control handed back");
  }

  // ---------- animation ----------

  @Test
  void shouldPlayEachStageFacingTheTarget() {
    startHeadbutt();

    update(1);
    assertEquals("windupr", animator.getCurrentAnimation());

    update(1);
    assertEquals("charger", animator.getCurrentAnimation());
  }

  @Test
  void shouldPlayTheLeftFacingAnimationsForATargetOnTheLeft() {
    player.setPosition(-4f, 0f);
    startHeadbutt();

    update(1);
    assertEquals("windupl", animator.getCurrentAnimation());

    update(1);
    assertEquals("chargel", animator.getCurrentAnimation());
  }

  @Test
  void shouldPlayTheHeadbuttAnimationAfterAHit() {
    startHeadbutt();
    update(2);
    touchPlayer();

    update(1);

    assertEquals("headbuttr", animator.getCurrentAnimation());
  }

  @Test
  void shouldReturnToIdleWhileRecovering() {
    startHeadbutt();
    update(3); // charge ends at once (no floor) and the satyr starts recovering

    update(1);

    assertEquals("idler", animator.getCurrentAnimation());
  }

  // ---------- helpers ----------

  private void startHeadbutt() {
    satyr.getEvents().trigger("headbutt", player);
  }

  private void update(int steps) {
    for (int i = 0; i < steps; i++) {
      headbutt.update();
    }
  }

  private void touchPlayer() {
    satyr
        .getEvents()
        .trigger("collisionStart", mock(Fixture.class), fixtureOn(PhysicsLayer.PLAYER));
  }

  private float satyrVelocityX() {
    return bodyOf(satyr).getLinearVelocity().x;
  }

  private float playerVelocityX() {
    return bodyOf(player).getLinearVelocity().x;
  }

  private void setSatyrVelocity(float x) {
    bodyOf(satyr).setLinearVelocity(new Vector2(x, 0f));
  }

  private static Body bodyOf(Entity entity) {
    return entity.getComponent(PhysicsComponent.class).getBody();
  }

  /** A static floor just below the satyr, wide enough to be under the charge in both directions. */
  private void addFloor() {
    Entity floor =
        new Entity()
            .addComponent(new PhysicsComponent().setBodyType(BodyType.StaticBody))
            .addComponent(new ColliderComponent().setLayer(PhysicsLayer.OBSTACLE));
    floor.setScale(20f, 1f);
    floor.setPosition(-5f, -1.05f);
    floor.create();
  }

  /** A fixture on the given physics layer, the only thing the component reads for the player. */
  private static Fixture fixtureOn(short layer) {
    Filter filter = new Filter();
    filter.categoryBits = layer;
    Fixture fixture = mock(Fixture.class);
    when(fixture.getFilterData()).thenReturn(filter);
    return fixture;
  }

  /** A solid fixture on the given layer, attached to a body that belongs to another entity. */
  private static Fixture solidFixtureOn(short layer) {
    Fixture fixture = fixtureOn(layer);
    BodyUserData data = new BodyUserData();
    data.entity = new Entity();
    Body body = mock(Body.class);
    when(body.getUserData()).thenReturn(data);
    when(fixture.getBody()).thenReturn(body);
    return fixture;
  }
}
