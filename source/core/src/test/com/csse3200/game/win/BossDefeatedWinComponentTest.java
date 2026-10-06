package com.csse3200.game.win;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.events.listeners.EventListener0;
import com.csse3200.game.extensions.GameExtension;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tests {@link BossDefeatedWinComponent}: attached to the final boss, it announces {@code
 * "finalBossDefeated"} on the entity it was given, once, when the boss dies. The game gives it a
 * listener owned by the level; the entity standing in for that here is called the player.
 *
 * <p>Fixture: a player entity with a listener counting the announcement, and a boss entity with the
 * component and real combat stats, so a death can be caused the way the game causes it (health to
 * zero) as well as by firing the {@code "death"} event directly.
 */
@ExtendWith(GameExtension.class)
class BossDefeatedWinComponentTest {
  private Entity player;
  private AtomicInteger announcements;

  @BeforeEach
  void setUp() {
    player = new Entity();
    player.create();
    announcements = new AtomicInteger();
    player
        .getEvents()
        .addListener(
            BossDefeatedWinComponent.FINAL_BOSS_DEFEATED_EVENT,
          announcements::incrementAndGet);
  }

  // ---------- the event name ----------

  @Test
  void shouldAnnounceUnderTheNameTheLevelListensFor() {
    assertEquals("finalBossDefeated", BossDefeatedWinComponent.FINAL_BOSS_DEFEATED_EVENT);
  }

  // ---------- construction ----------

  @Test
  void shouldRejectANullPlayer() {
    assertThrows(IllegalArgumentException.class, () -> new BossDefeatedWinComponent(null));
  }

  @Test
  void shouldStartWithNothingAnnounced() {
    BossDefeatedWinComponent component = new BossDefeatedWinComponent(player);

    assertFalse(component.hasAnnounced());
  }

  // ---------- announcing ----------

  @Test
  void shouldAnnounceNothingBeforeTheBossDies() {
    createBoss();

    assertEquals(0, announcements.get());
  }

  @Test
  void shouldAnnounceOnThePlayerWhenTheBossDies() {
    Entity boss = createBoss();

    boss.getEvents().trigger("death");

    assertEquals(1, announcements.get());
    assertTrue(boss.getComponent(BossDefeatedWinComponent.class).hasAnnounced());
  }

  @Test
  void shouldAnnounceWhenTheBossHealthReachesZeroTheWayTheGameKillsAnEnemy() {
    Entity boss = createBoss();

    boss.getComponent(CombatStatsComponent.class).setHealth(0);

    assertEquals(1, announcements.get());
  }

  @Test
  void shouldAnnounceOnlyOnceEvenIfDeathIsReportedAgain() {
    Entity boss = createBoss();

    boss.getEvents().trigger("death");
    boss.getEvents().trigger("death");
    boss.getEvents().trigger("death");

    assertEquals(1, announcements.get());
  }

  @Test
  void shouldAnnounceOnThePlayerAndNotOnTheBoss() {
    Entity boss = createBoss();
    AtomicInteger onBoss = new AtomicInteger();
    boss.getEvents()
        .addListener(
            BossDefeatedWinComponent.FINAL_BOSS_DEFEATED_EVENT,
          onBoss::incrementAndGet);

    boss.getEvents().trigger("death");

    assertEquals(0, onBoss.get());
    assertEquals(1, announcements.get());
  }

  @Test
  void shouldIgnoreOtherEventsOnTheBoss() {
    Entity boss = createBoss();

    boss.getEvents().trigger("rangedAttackWindup");
    boss.getEvents().trigger("chargeEnd");
    boss.getEvents().trigger("updateHealth", 10);

    assertEquals(0, announcements.get());
  }

  @Test
  void shouldNotAnnounceForADamagedBossThatSurvives() {
    Entity boss = createBoss();

    boss.getComponent(CombatStatsComponent.class).setHealth(1);

    assertEquals(0, announcements.get());
  }

  // ---------- several bosses, one player ----------

  @Test
  void shouldAnnounceForEachComponentOnItsOwnBossAndNotForOtherEnemies() {
    Entity boss = createBoss();
    Entity bystander = new Entity().addComponent(new CombatStatsComponent(10, 0));
    bystander.create();

    bystander.getEvents().trigger("death");

    assertEquals(0, announcements.get(), "an enemy without the component never ends the game");
    boss.getEvents().trigger("death");
    assertEquals(1, announcements.get());
  }

  @Test
  void shouldKeepSeparateBossesIndependent() {
    // The component is meant for one boss only, but two instances must not share their "announced"
    // flag.
    Entity first = createBoss();
    Entity second = createBoss();

    first.getEvents().trigger("death");

    assertTrue(first.getComponent(BossDefeatedWinComponent.class).hasAnnounced());
    assertFalse(second.getComponent(BossDefeatedWinComponent.class).hasAnnounced());
  }

  @Test
  void shouldNotThrowWhenNobodyIsListeningOnThePlayer() {
    Entity lonelyPlayer = new Entity();
    lonelyPlayer.create();
    Entity boss = new Entity().addComponent(new BossDefeatedWinComponent(lonelyPlayer));
    boss.create();

    assertDoesNotThrow(() -> boss.getEvents().trigger("death"));
  }

  // ---------- helpers ----------

  private Entity createBoss() {
    Entity boss =
        new Entity()
            .addComponent(new CombatStatsComponent(100, 10))
            .addComponent(new BossDefeatedWinComponent(player));
    boss.create();
    return boss;
  }
}
