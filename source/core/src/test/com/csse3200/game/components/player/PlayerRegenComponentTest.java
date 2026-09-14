package com.csse3200.game.components.player;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.csse3200.game.components.attacks.CombatStatsComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class PlayerRegenComponentTest {
  private GameTime time;
  private Entity player;
  private PlayerRegenComponent regen;
  private CombatStatsComponent stats;

  @BeforeEach
  void setUp() {
    time = mock(GameTime.class);
    when(time.getTime()).thenReturn(0L);
    ServiceLocator.registerTimeSource(time);

    player =
        new Entity()
            .addComponent(new CombatStatsComponent(100, 10))
            .addComponent(new ConsumableUseComponent(100))
            .addComponent(new PlayerRegenComponent());
    player.create();
    regen = player.getComponent(PlayerRegenComponent.class);
    stats = player.getComponent(CombatStatsComponent.class);
    stats.setHealth(50);
  }

  /** Acceptance criterion: regeneration heals gradually rather than all at once. */
  @Test
  void shouldHealOneTickPerSecond() {
    assertTrue(regen.startRegen(5, 3f), "a valid regeneration should start");
    assertEquals(50, stats.getHealth(), "no health should be restored before the first tick");

    when(time.getTime()).thenReturn(999L);
    player.update();
    assertEquals(50, stats.getHealth(), "no health should be restored just before one second");

    when(time.getTime()).thenReturn(1000L);
    player.update();
    assertEquals(55, stats.getHealth(), "the first tick should restore 5 health at one second");

    when(time.getTime()).thenReturn(2000L);
    player.update();
    assertEquals(60, stats.getHealth(), "the second tick should restore another 5 health");
  }

  /** Acceptance criterion: healing stops when the duration ends and leaves nothing behind. */
  @Test
  void shouldStopHealingAfterTheDurationEnds() {
    regen.startRegen(5, 3f);

    when(time.getTime()).thenReturn(3000L);
    player.update();
    assertEquals(65, stats.getHealth(), "three ticks of 5 should have healed 15 health");
    assertFalse(regen.isRegenerating(), "regeneration should stop once its duration ends");

    when(time.getTime()).thenReturn(9000L);
    player.update();
    assertEquals(65, stats.getHealth(), "no healing should happen after the duration ends");
  }

  /** Acceptance criterion: regeneration is capped at maximum health. */
  @Test
  void shouldNotHealAboveMaximumHealth() {
    stats.setHealth(98);
    regen.startRegen(5, 3f);

    when(time.getTime()).thenReturn(1000L);
    player.update();
    assertEquals(100, stats.getHealth(), "healing should stop at maximum health");

    when(time.getTime()).thenReturn(2000L);
    player.update();
    assertEquals(100, stats.getHealth(), "a tick at maximum health should not overheal");
  }

  /** A stronger regeneration potion replaces a weaker one, matching the buff stacking rule. */
  @Test
  void shouldReplaceAWeakerRegenerationWithAStrongerOne() {
    regen.startRegen(5, 10f);

    assertTrue(regen.startRegen(10, 10f), "a stronger regeneration should replace a weaker one");
    when(time.getTime()).thenReturn(1000L);
    player.update();
    assertEquals(60, stats.getHealth(), "the tick should use the stronger heal of 10");
  }

  /** A weaker potion is rejected while a stronger one runs, so it stays in the inventory. */
  @Test
  void shouldRejectAWeakerRegenerationWhileAStrongerOneIsActive() {
    regen.startRegen(10, 10f);

    assertFalse(
        regen.startRegen(5, 10f), "a weaker regeneration should be rejected while stronger runs");
    when(time.getTime()).thenReturn(1000L);
    player.update();
    assertEquals(60, stats.getHealth(), "the tick should still use the stronger heal of 10");
  }

  /** Ticks missed because several seconds passed in one frame are still applied. */
  @Test
  void shouldApplyEveryTickThatElapsedSinceTheLastUpdate() {
    regen.startRegen(5, 5f);

    when(time.getTime()).thenReturn(3000L);
    player.update();
    assertEquals(65, stats.getHealth(), "all three ticks missed in one slow frame should apply");
  }

  /** A regeneration with no healing or no duration is rejected. */
  @Test
  void shouldRejectRegenerationThatWouldDoNothing() {
    assertFalse(regen.startRegen(0, 5f), "a regeneration that heals nothing should be rejected");
    assertFalse(regen.startRegen(5, 0f), "a regeneration with no duration should be rejected");
    assertFalse(regen.isRegenerating(), "nothing should be regenerating after two rejections");
  }
}
