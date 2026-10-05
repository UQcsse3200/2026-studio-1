package com.csse3200.game.components.boss;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.csse3200.game.areas.terrain.map.JsonMapLoader;
import com.csse3200.game.areas.terrain.map.LevelView;
import com.csse3200.game.areas.terrain.map.MapDataLevelView;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.boss.ZeusBossComponent.Action;
import com.csse3200.game.components.boss.ZeusBossComponent.Phase;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.entities.configs.enemies.ZeusConfig;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

@ExtendWith(GameExtension.class)
class ZeusBossComponentTest {
  private static final float STEP = 0.05f;
  private static final float FLOOR_Y = 1.5f;
  private static final int PLAYER_HEALTH = 1000;

  private ZeusConfig config;
  private Entity player;
  private Entity zeus;
  private ZeusBossComponent boss;

  @BeforeEach
  void setUp() {
    GameTime time = mock(GameTime.class);
    when(time.getDeltaTime()).thenReturn(STEP);
    ServiceLocator.registerTimeSource(time);
    ServiceLocator.registerEntityService(mock(EntityService.class));
    Gdx.app = mock(Application.class);

    config = new ZeusConfig();
    config.health = 100;
    config.baseAttack = 15;

    player = new Entity().addComponent(new CombatStatsComponent(PLAYER_HEALTH, 10));
    player.setScale(0.75f, 0.75f);
    player.create();

    LevelView level = new MapDataLevelView(new JsonMapLoader().load("maps/level3.json"));
    boss = new ZeusBossComponent(player, config, 1f, 1f);
    boss.setLevel(level);
    zeus =
        new Entity()
            .addComponent(new CombatStatsComponent(config.health, config.baseAttack))
            .addComponent(boss);
    zeus.setScale(1.5f, 2f);
    // Where the level spawns him: centred on tile (26, 3), sunk into the floor until he is placed.
    zeus.setPosition(12.5f, 0.75f);
    zeus.create();
  }

  @Test
  void readsTheArenaFromTheLevel() {
    assertEquals(FLOOR_Y, boss.getFloorY(), 0.001f);
    assertEquals(9.5f, boss.getPerchY(), 0.001f);
    assertEquals(1f, boss.getArenaLeft(), 0.001f);
    assertEquals(25f, boss.getArenaRight(), 0.001f);
    // He starts floating just over the floor rather than sunk into it.
    assertEquals(FLOOR_Y + 0.1f, zeus.getPosition().y, 0.001f);
  }

  @Test
  void choosesThePhaseFromRemainingHealth() {
    assertEquals(Phase.FLOOR, ZeusBossComponent.phaseFor(1f, config));
    assertEquals(Phase.FLOOR, ZeusBossComponent.phaseFor(0.71f, config));
    assertEquals(Phase.THRONE, ZeusBossComponent.phaseFor(0.7f, config));
    assertEquals(Phase.THRONE, ZeusBossComponent.phaseFor(0.36f, config));
    assertEquals(Phase.ENRAGED, ZeusBossComponent.phaseFor(0.35f, config));
  }

  @Test
  void floatsTowardsThePlayerAndSwingsWhenInReach() {
    standPlayerAt(9f, FLOOR_Y);
    float startX = zeus.getPosition().x;

    runFor(1f);
    assertTrue(zeus.getPosition().x < startX, "Zeus should close on a player to his left");

    standPlayerAt(zeus.getCenterPosition().x - 1f, FLOOR_Y);
    runUntil(Action.MELEE, 4f);
    runFor(ZeusBossComponent.MELEE_DURATION);

    assertEquals(PLAYER_HEALTH - config.baseAttack, playerHealth());
  }

  @Test
  void aSwingMissesAPlayerWhoStepsBehindHim() {
    standPlayerAt(zeus.getCenterPosition().x - 1f, FLOOR_Y);
    runUntil(Action.MELEE, 4f);

    // Inside the wind-up, the player crosses to his other side.
    standPlayerAt(zeus.getCenterPosition().x + 1f, FLOOR_Y);
    runFor(ZeusBossComponent.MELEE_DURATION);

    assertEquals(PLAYER_HEALTH, playerHealth());
  }

  @Test
  void throwsABoltAlongTheFloorAfterItsWarning() {
    standPlayerAt(4f, FLOOR_Y);
    runUntil(Action.BOLT, 4f);

    assertTrue(boss.isTelegraphing());
    assertTrue(boss.getBolts().isEmpty(), "No bolt should fly during the wind-up");

    runFor(ZeusBossComponent.BOLT_WINDUP + STEP);
    assertEquals(1, boss.getBolts().size());
    ZeusBossComponent.Bolt bolt = boss.getBolts().getFirst();
    assertEquals(-ZeusBossComponent.BOLT_SPEED, bolt.getVelocity().x, 0.001f);
    assertEquals(0f, bolt.getVelocity().y, 0.001f);
  }

  @Test
  void aBoltHurtsThePlayerOnceAndIsBlockedByTheStoneBlock() {
    // Out in the open, on the near side of the block at x = 8 to 9. Zeus holds his ground.
    config.moveSpeed = 0f;
    standPlayerAt(10f, FLOOR_Y);
    runUntil(Action.BOLT, 4f);
    freezeZeus();
    runFor(3f);
    assertEquals(PLAYER_HEALTH - config.boltDamage, playerHealth());
    assertTrue(boss.getBolts().isEmpty());

    // Behind the block, the next bolts never arrive.
    standPlayerAt(6f, FLOOR_Y);
    thaw();
    runUntil(Action.BOLT, 4f);
    freezeZeus();
    runFor(4f);
    assertEquals(PLAYER_HEALTH - config.boltDamage, playerHealth());
  }

  @Test
  void risesToTheThroneAndStrikesTheGroundUnderThePlayer() {
    standPlayerAt(4f, FLOOR_Y);
    zeus.getComponent(CombatStatsComponent.class).setHealth(60);
    runFor(2f);

    assertEquals(Phase.THRONE, boss.getPhase());
    assertEquals(boss.getPerchY(), zeus.getPosition().y, 0.2f);

    runUntil(Action.STRIKE, 4f);
    assertEquals(1, boss.getStrikes().size());
    ZeusBossComponent.Strike strike = boss.getStrikes().getFirst();
    assertEquals(player.getCenterPosition().x, strike.getX(), 0.001f);
    assertEquals(FLOOR_Y, strike.getGroundY(), 0.001f);

    // Nothing hurts during the warning; the column does when it lands.
    runFor(ZeusBossComponent.STRIKE_WARNING - 2 * STEP);
    assertEquals(PLAYER_HEALTH, playerHealth());
    runFor(4 * STEP);
    assertEquals(PLAYER_HEALTH - config.strikeDamage, playerHealth());
  }

  @Test
  void aStrikeMissesAPlayerWhoMovesOffTheMarkedSpot() {
    standPlayerAt(4f, FLOOR_Y);
    zeus.getComponent(CombatStatsComponent.class).setHealth(60);
    runFor(2f);
    runUntil(Action.STRIKE, 4f);

    standPlayerAt(6f, FLOOR_Y);
    runFor(ZeusBossComponent.STRIKE_WARNING + ZeusBossComponent.STRIKE_DURATION);

    assertEquals(PLAYER_HEALTH, playerHealth());
  }

  @Test
  void aStrikeLandsOnThePlatformThePlayerStandsOn() {
    // The slab at tiles x 7 to 12, row 6 from the bottom: its surface is at y = 3.5.
    assertEquals(3.5f, boss.groundBelow(4f, 3.5f), 0.001f);
    assertEquals(3.5f, boss.groundBelow(4f, 4.6f), 0.001f);
    assertEquals(FLOOR_Y, boss.groundBelow(4f, 3f), 0.001f);
  }

  @Test
  void slamsDownEnragedBehindAShockwave() {
    standPlayerAt(4f, FLOOR_Y);
    zeus.getComponent(CombatStatsComponent.class).setHealth(30);
    runUntil(Action.SHOCKWAVE, 6f);

    assertEquals(Phase.ENRAGED, boss.getPhase());
    assertEquals(FLOOR_Y + 0.1f, zeus.getPosition().y, 0.01f);
    assertTrue(boss.getShockwaveWarning() >= 0f);
    assertTrue(boss.getWaves().isEmpty(), "No wave should run during the warning");

    freezeZeus();
    runFor(ZeusBossComponent.SHOCKWAVE_WARNING + STEP);
    assertEquals(2, boss.getWaves().size());

    // The wave crosses the floor to the player, hurts them once, and dies at the wall.
    runFor(6f);
    assertEquals(PLAYER_HEALTH - config.shockwaveDamage, playerHealth());
    assertTrue(boss.getWaves().isEmpty());
  }

  @Test
  void aShockwavePassesUnderAPlayerOffTheFloor() {
    standPlayerAt(4f, FLOOR_Y + 1f);
    zeus.getComponent(CombatStatsComponent.class).setHealth(30);
    runUntil(Action.SHOCKWAVE, 6f);
    freezeZeus();
    runFor(6f);

    assertEquals(PLAYER_HEALTH, playerHealth());
  }

  @Test
  void firesDefeatedAndRemovesHimselfOnceHisDeathHasPlayed() {
    boolean[] defeated = {false};
    zeus.getEvents().addListener(ZeusBossComponent.DEFEATED_EVENT, () -> defeated[0] = true);
    standPlayerAt(4f, FLOOR_Y);
    runUntil(Action.BOLT, 4f);
    runFor(1f);

    zeus.getComponent(CombatStatsComponent.class).setHealth(0);
    assertTrue(boss.getBolts().isEmpty(), "His attacks should die with him");
    assertFalse(defeated[0], "The death should play out first");

    runFor(1.2f);
    assertTrue(defeated[0]);
    ArgumentCaptor<Runnable> disposal = ArgumentCaptor.forClass(Runnable.class);
    verify(Gdx.app).postRunnable(disposal.capture());
    disposal.getValue().run();
    assertTrue(zeus.isDisposed());
  }

  @Test
  void resetStartsTheFightOverAgainstANewPlayer() {
    standPlayerAt(4f, FLOOR_Y);
    zeus.getComponent(CombatStatsComponent.class).setHealth(30);
    runUntil(Action.SHOCKWAVE, 6f);
    runFor(1f);

    Entity newPlayer = new Entity().addComponent(new CombatStatsComponent(PLAYER_HEALTH, 10));
    newPlayer.setScale(0.75f, 0.75f);
    newPlayer.setPosition(4f, FLOOR_Y);
    newPlayer.create();
    boss.reset(newPlayer);

    assertEquals(Phase.FLOOR, boss.getPhase());
    assertEquals(1f, boss.getHealthFraction(), 0.001f);
    assertTrue(boss.getWaves().isEmpty());
    assertEquals(13.25f, zeus.getCenterPosition().x, 0.001f);

    // He now fights the new player, not the old one.
    runUntil(Action.BOLT, 4f);
    runFor(4f);
    assertEquals(PLAYER_HEALTH, playerHealth());
    assertTrue(newPlayer.getComponent(CombatStatsComponent.class).getHealth() < PLAYER_HEALTH);
  }

  private void standPlayerAt(float centreX, float y) {
    player.setPosition(centreX - player.getScale().x / 2f, y);
  }

  private int playerHealth() {
    return player.getComponent(CombatStatsComponent.class).getHealth();
  }

  /** Stops Zeus choosing anything new, so one attack can be followed to its end. */
  private void freezeZeus() {
    config.moveSpeed = 0f;
    frozen = true;
  }

  private void thaw() {
    frozen = false;
  }

  private boolean frozen;

  private void runFor(float seconds) {
    for (float t = 0f; t < seconds; t += STEP) {
      step();
    }
  }

  private void runUntil(Action action, float limit) {
    for (float t = 0f; t < limit && boss.getAction() != action; t += STEP) {
      step();
    }
    assertEquals(action, boss.getAction());
  }

  private void step() {
    if (frozen && boss.getAction() == Action.IDLE) {
      // Hold him between attacks by keeping his cooldown from running out.
      boss.holdAttacks();
    }
    zeus.update();
  }
}
