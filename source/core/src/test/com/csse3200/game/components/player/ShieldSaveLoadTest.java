package com.csse3200.game.components.player;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.csse3200.game.Quests.Quest;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.files.GameSaveData;
import com.csse3200.game.files.LoadService;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;
import java.util.ArrayList;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Save/load tests for {@link ShieldComponent} and {@link BallisticShieldComponent}: reading the
 * time left on an active shield, and putting back held and active shields on load.
 */
@ExtendWith(GameExtension.class)
class ShieldSaveLoadTest {
  private static final long DURATION_MILLIS = 30000L;
  private static final int SHIELDS_COLLECTED = 3;

  private GameTime timeSource;
  private Entity player;
  private ShieldComponent shield;
  private BallisticShieldComponent ballisticShield;

  @BeforeEach
  void setUp() {
    Quest.resetAll();
    timeSource = mock(GameTime.class);
    when(timeSource.getTime()).thenReturn(0L);
    ServiceLocator.registerTimeSource(timeSource);

    player = createPlayer();
    shield = player.getComponent(ShieldComponent.class);
    ballisticShield = player.getComponent(BallisticShieldComponent.class);
  }

  @AfterEach
  void tearDown() {
    Quest.resetAll();
  }

  private Entity createPlayer() {
    Entity entity =
        new Entity()
            .addComponent(new CombatStatsComponent(100, 10))
            .addComponent(new ShieldComponent(DURATION_MILLIS))
            .addComponent(new BallisticShieldComponent(DURATION_MILLIS));
    entity.create();
    return entity;
  }

  private void setTime(long millis) {
    when(timeSource.getTime()).thenReturn(millis);
  }

  // ShieldComponent

  @Test
  void remainingTimeIsZeroWhenShieldIsNotActive() {
    assertEquals(0L, shield.getRemainingMillis());

    shield.grantShield();

    assertEquals(0L, shield.getRemainingMillis(), "a held but unused shield has no time running");
  }

  @Test
  void remainingTimeCountsDownWhileActive() {
    shield.grantShield();
    shield.activateShield();

    assertEquals(DURATION_MILLIS, shield.getRemainingMillis());

    setTime(10000L);

    assertEquals(20000L, shield.getRemainingMillis());
  }

  @Test
  void remainingTimeIsZeroAfterShieldRunsOut() {
    shield.grantShield();
    shield.activateShield();

    setTime(DURATION_MILLIS + 1000L);
    player.update();

    assertFalse(shield.isActive());
    assertEquals(0L, shield.getRemainingMillis());
  }

  @Test
  void restoreHeldShieldGivesBackAShieldThatCanBeActivated() {
    shield.restoreHeldShield(true);

    assertTrue(shield.hasShield());
    assertTrue(shield.activateShield());
    assertTrue(shield.isActive());
  }

  @Test
  void restoreHeldShieldFalseRemovesAHeldShield() {
    shield.grantShield();

    shield.restoreHeldShield(false);

    assertFalse(shield.hasShield());
  }

  @Test
  void restoreHeldShieldDoesNotCountAsANewPickupForQuests() {
    int before = Quest.exportCounters()[SHIELDS_COLLECTED];

    shield.restoreHeldShield(true);

    assertEquals(before, Quest.exportCounters()[SHIELDS_COLLECTED]);
  }

  @Test
  void restoreActiveStartsShieldWithTheTimeItHadLeft() {
    setTime(50000L);

    shield.restoreActive(5000L);

    assertTrue(shield.isActive());
    assertEquals(5000L, shield.getRemainingMillis());
  }

  @Test
  void restoredShieldStillRunsOutOnTime() {
    shield.restoreActive(5000L);

    setTime(4000L);
    player.update();
    assertTrue(shield.isActive());

    setTime(5000L);
    player.update();
    assertFalse(shield.isActive());
  }

  @Test
  void restoreActiveIgnoresZeroOrNegativeTime() {
    shield.restoreActive(0L);
    assertFalse(shield.isActive());

    shield.restoreActive(-100L);
    assertFalse(shield.isActive());
  }

  @Test
  void restoreActiveFiresShieldActivatedEvent() {
    long[] reported = {0L};
    player.getEvents().addListener("shieldActivated", (Long millis) -> reported[0] = millis);

    shield.restoreActive(5000L);

    assertEquals(5000L, reported[0]);
  }

  // BallisticShieldComponent

  @Test
  void ballisticRemainingTimeCountsDownWhileActive() {
    assertEquals(0L, ballisticShield.getRemainingMillis());

    ballisticShield.grantShield();
    ballisticShield.activateBallisticShield();
    setTime(12000L);

    assertEquals(18000L, ballisticShield.getRemainingMillis());
  }

  @Test
  void ballisticRestoreHeldShieldGivesBackAShieldThatCanBeActivated() {
    ballisticShield.restoreHeldShield(true);

    assertTrue(ballisticShield.hasShield());
    assertTrue(ballisticShield.activateBallisticShield());
  }

  @Test
  void ballisticRestoreActiveStartsWithTheTimeItHadLeftAndRunsOut() {
    ballisticShield.restoreActive(3000L);

    assertTrue(ballisticShield.isActive());
    assertEquals(3000L, ballisticShield.getRemainingMillis());

    setTime(3000L);
    player.update();

    assertFalse(ballisticShield.isActive());
  }

  @Test
  void ballisticRestoreActiveIgnoresZeroOrNegativeTime() {
    ballisticShield.restoreActive(0L);
    ballisticShield.restoreActive(-1L);

    assertFalse(ballisticShield.isActive());
  }

  // Full save then load through LoadService

  @Test
  void savedShieldStateComesBackOnANewPlayer() {
    shield.grantShield();
    shield.activateShield();
    shield.grantShield();
    ballisticShield.grantShield();
    setTime(10000L);

    GameSaveData data = new GameSaveData();
    data.shieldHeld = shield.hasShield();
    data.shieldRemainingMillis = shield.getRemainingMillis();
    data.ballisticShieldHeld = ballisticShield.hasShield();
    data.ballisticShieldRemainingMillis = ballisticShield.getRemainingMillis();

    // A new screen starts its game clock from zero again.
    setTime(0L);
    Entity loadedPlayer = createPlayer();
    LoadService.apply(loadedPlayer, data, 100f, 100f, new ArrayList<>());

    ShieldComponent loadedShield = loadedPlayer.getComponent(ShieldComponent.class);
    BallisticShieldComponent loadedBallistic =
        loadedPlayer.getComponent(BallisticShieldComponent.class);
    assertTrue(loadedShield.isActive());
    assertEquals(20000L, loadedShield.getRemainingMillis());
    assertTrue(loadedShield.hasShield());
    assertTrue(loadedBallistic.hasShield());
    assertFalse(loadedBallistic.isActive());
  }

  @Test
  void loadingASaveWithNoShieldsLeavesShieldsEmpty() {
    Entity loadedPlayer = createPlayer();

    LoadService.apply(loadedPlayer, new GameSaveData(), 100f, 100f, new ArrayList<>());

    ShieldComponent loadedShield = loadedPlayer.getComponent(ShieldComponent.class);
    BallisticShieldComponent loadedBallistic =
        loadedPlayer.getComponent(BallisticShieldComponent.class);
    assertFalse(loadedShield.hasShield());
    assertFalse(loadedShield.isActive());
    assertFalse(loadedBallistic.hasShield());
    assertFalse(loadedBallistic.isActive());
  }
}
