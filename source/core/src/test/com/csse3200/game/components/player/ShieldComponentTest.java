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
class ShieldComponentTest {
  private static final long DURATION_MILLIS = 30000L;

  private GameTime timeSource;
  private Entity player;
  private ShieldComponent shield;
  private CombatStatsComponent stats;

  @BeforeEach
  void setUp() {
    timeSource = mock(GameTime.class);
    when(timeSource.getTime()).thenReturn(0L);
    ServiceLocator.registerTimeSource(timeSource);

    player =
        new Entity()
            .addComponent(new CombatStatsComponent(100, 10))
            .addComponent(new ShieldComponent(DURATION_MILLIS));
    player.create();
    shield = player.getComponent(ShieldComponent.class);
    stats = player.getComponent(CombatStatsComponent.class);
  }

  /** Acceptance criterion: activation only succeeds once a shield has actually been picked up. */
  @Test
  void shouldNotActivateWithoutAGrantedShield() {
    assertFalse(shield.activateShield(), "activation should fail with no shield held");
    assertFalse(shield.isActive());
  }

  /** Acceptance criterion: pressing the shield key activates a held shield. */
  @Test
  void shouldActivateAGrantedShield() {
    shield.grantShield();

    assertTrue(shield.activateShield(), "activation should succeed once a shield is held");
    assertTrue(shield.isActive());
    assertFalse(shield.hasShield(), "the held shield should be consumed on activation");
  }

  /** Acceptance criterion: an already-active shield cannot be re-activated or stacked. */
  @Test
  void shouldNotActivateTwice() {
    shield.grantShield();
    shield.activateShield();

    assertFalse(shield.activateShield(), "re-activating an active shield should be a no-op");
  }

  /** Acceptance criterion: damage taken while the shield is active is fully blocked. */
  @Test
  void shouldBlockDamageWhileActive() {
    shield.grantShield();
    shield.activateShield();

    stats.setHealth(60);
    player.update(); // reversion is applied on the following update, not synchronously

    assertEquals(100, stats.getHealth(), "damage should be fully reverted while shielded");
    assertTrue(shield.isActive(), "the shield should remain active after blocking damage");
  }

  /** Acceptance criterion: damage is applied normally once the shield is no longer active. */
  @Test
  void shouldNotBlockDamageWhenNotActive() {
    stats.setHealth(60);
    player.update();

    assertEquals(60, stats.getHealth(), "damage should apply normally without an active shield");
  }

  /** Acceptance criterion: the shield automatically expires after its configured duration. */
  @Test
  void shouldExpireAfterItsDuration() {
    shield.grantShield();
    shield.activateShield();
    assertTrue(shield.isActive());

    when(timeSource.getTime()).thenReturn(DURATION_MILLIS + 1);
    player.update();

    assertFalse(shield.isActive(), "the shield should deactivate once its duration elapses");
  }

  /** Acceptance criterion: the shield keeps blocking right up until its duration elapses. */
  @Test
  void shouldNotExpireBeforeItsDuration() {
    shield.grantShield();
    shield.activateShield();

    when(timeSource.getTime()).thenReturn(DURATION_MILLIS - 1);
    player.update();

    assertTrue(shield.isActive(), "the shield should still be active just before its duration ends");
  }

  /** Acceptance criterion: damage taken right up to expiry is still blocked. */
  @Test
  void shouldBlockDamageRightUpToExpiry() {
    shield.grantShield();
    shield.activateShield();

    when(timeSource.getTime()).thenReturn(DURATION_MILLIS - 1);
    stats.setHealth(40);
    player.update();

    assertEquals(100, stats.getHealth(), "damage just before expiry should still be blocked");
  }

  /** A shield cannot be constructed with a non-positive duration. */
  @Test
  void shouldRejectNonPositiveDuration() {
    assertThrows(IllegalArgumentException.class, () -> new ShieldComponent(0L));
    assertThrows(IllegalArgumentException.class, () -> new ShieldComponent(-1L));
  }
}