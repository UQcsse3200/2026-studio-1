package com.csse3200.game.components.player;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class BallisticShieldComponentTest {
  private static final long DURATION_MILLIS = 30000L;

  private GameTime timeSource;
  private Entity player;
  private Entity enemy;
  private BallisticShieldComponent shield;
  private CombatStatsComponent playerStats;
  private CombatStatsComponent enemyStats;

  @BeforeEach
  void setUp() {
    timeSource = mock(GameTime.class);
    when(timeSource.getTime()).thenReturn(0L);
    ServiceLocator.registerTimeSource(timeSource);

    player =
        new Entity()
            .addComponent(new CombatStatsComponent(100, 10))
            .addComponent(new BallisticShieldComponent(DURATION_MILLIS));
    player.create();

    enemy = new Entity().addComponent(new CombatStatsComponent(100, 10));
    enemy.create();

    shield = player.getComponent(BallisticShieldComponent.class);
    playerStats = player.getComponent(CombatStatsComponent.class);
    enemyStats = enemy.getComponent(CombatStatsComponent.class);
  }

  @Test
  void shouldNotActivateWithoutAGrantedShield() {
    assertFalse(shield.activateBallisticShield());
    assertFalse(shield.isActive());
  }

  @Test
  void shouldActivateAndConsumeGrantedShield() {
    shield.grantShield();

    assertTrue(shield.hasShield());
    assertTrue(shield.activateBallisticShield());
    assertTrue(shield.isActive());
    assertFalse(shield.hasShield());
  }

  @Test
  void shouldNotActivateTwice() {
    shield.grantShield();
    assertTrue(shield.activateBallisticShield());

    assertFalse(shield.activateBallisticShield());
  }

  @Test
  void shouldBlockIncomingDamageAndRetaliate() {
    shield.grantShield();
    shield.activateBallisticShield();

    playerStats.hit(enemyStats, 40);

    assertEquals(100, playerStats.getHealth(), "the player should take no damage");
    assertEquals(90, enemyStats.getHealth(), "the enemy should take 25% retaliation damage");
  }

  @Test
  void shouldApplyDamageNormallyWhenShieldIsInactive() {
    playerStats.hit(enemyStats, 40);

    assertEquals(60, playerStats.getHealth());
    assertEquals(100, enemyStats.getHealth());
  }

  @Test
  void shouldExpireAfterItsDuration() {
    shield.grantShield();
    shield.activateBallisticShield();

    when(timeSource.getTime()).thenReturn(DURATION_MILLIS + 1);
    player.update();

    assertFalse(shield.isActive());
  }

  @Test
  void shouldRemainActiveBeforeItsDurationExpires() {
    shield.grantShield();
    shield.activateBallisticShield();

    when(timeSource.getTime()).thenReturn(DURATION_MILLIS - 1);
    player.update();

    assertTrue(shield.isActive());
  }

  @Test
  void shouldRejectNonPositiveDuration() {
    assertThrows(IllegalArgumentException.class, () -> new BallisticShieldComponent(0L));
    assertThrows(IllegalArgumentException.class, () -> new BallisticShieldComponent(-1L));
  }
}
