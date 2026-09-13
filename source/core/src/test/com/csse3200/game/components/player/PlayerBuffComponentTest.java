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
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class PlayerBuffComponentTest {
  private GameTime time;
  private Entity player;
  private PlayerBuffComponent buffs;
  private CombatStatsComponent stats;

  @BeforeEach
  void setUp() {
    time = mock(GameTime.class);
    when(time.getTime()).thenReturn(0L);
    ServiceLocator.registerTimeSource(time);

    player =
        new Entity()
            .addComponent(new CombatStatsComponent(100, 10))
            .addComponent(new PlayerBuffComponent());
    player.create();
    buffs = player.getComponent(PlayerBuffComponent.class);
    stats = player.getComponent(CombatStatsComponent.class);
  }

  /** Acceptance criterion: a damage buff applies immediately and reverts after its duration. */
  @Test
  void shouldApplyDamageBuffAndRevertAfterDuration() {
    assertTrue(buffs.applyBuff(BuffStat.DAMAGE, 2f, 5f));
    assertEquals(20, stats.getBaseAttack());

    when(time.getTime()).thenReturn(4999L);
    player.update();
    assertEquals(20, stats.getBaseAttack());
    assertTrue(buffs.hasBuff(BuffStat.DAMAGE));

    when(time.getTime()).thenReturn(5000L);
    player.update();
    assertEquals(10, stats.getBaseAttack());
    assertFalse(buffs.hasBuff(BuffStat.DAMAGE));
  }

  /** Acceptance criterion: a speed buff applies immediately and reverts after its duration. */
  @Test
  void shouldApplySpeedBuffAndRevertAfterDuration() {
    assertEquals(1f, buffs.getSpeedMultiplier());

    assertTrue(buffs.applyBuff(BuffStat.SPEED, 1.5f, 10f));
    assertEquals(1.5f, buffs.getSpeedMultiplier());

    when(time.getTime()).thenReturn(9000L);
    player.update();
    assertEquals(1.5f, buffs.getSpeedMultiplier());

    when(time.getTime()).thenReturn(10000L);
    player.update();
    assertEquals(1f, buffs.getSpeedMultiplier());
    assertFalse(buffs.hasBuff(BuffStat.SPEED));
  }

  @Test
  void shouldExpireOnlyTheBuffThatHasElapsed() {
    buffs.applyBuff(BuffStat.DAMAGE, 2f, 5f);
    buffs.applyBuff(BuffStat.SPEED, 1.5f, 20f);
    assertEquals(2, buffs.getActiveBuffs().size());

    when(time.getTime()).thenReturn(5000L);
    player.update();

    assertEquals(1, buffs.getActiveBuffs().size());
    assertFalse(buffs.hasBuff(BuffStat.DAMAGE));
    assertTrue(buffs.hasBuff(BuffStat.SPEED));
    assertEquals(10, stats.getBaseAttack());
    assertEquals(1.5f, buffs.getSpeedMultiplier());
  }

  @Test
  void shouldTriggerEventsForBuffLifecycle() {
    List<String> events = new ArrayList<>();
    player.getEvents().addListener("buffApplied", (ActiveBuff b) -> events.add("applied"));
    player.getEvents().addListener("buffExpired", (ActiveBuff b) -> events.add("expired"));

    buffs.applyBuff(BuffStat.DAMAGE, 2f, 5f);
    assertEquals(List.of("applied"), events);

    when(time.getTime()).thenReturn(5000L);
    player.update();
    assertEquals(List.of("applied", "expired"), events);
  }

  @Test
  void shouldExposeActiveBuffsForUi() {
    buffs.applyBuff(BuffStat.DAMAGE, 2f, 5f);

    ActiveBuff buff = buffs.getActiveBuffs().get(0);
    assertEquals(BuffStat.DAMAGE, buff.getStat());
    assertEquals(2f, buff.getMagnitude());
    assertEquals(5f, buff.getDurationSeconds());
    assertEquals(5000L, buff.getEndTime());
    assertEquals(5f, buff.getRemainingSeconds(0L));
    assertEquals(2f, buff.getRemainingSeconds(3000L));
    assertEquals(0f, buff.getRemainingSeconds(9000L));
  }

  @Test
  void shouldReturnUnmodifiableActiveBuffs() {
    buffs.applyBuff(BuffStat.DAMAGE, 2f, 5f);

    assertThrows(UnsupportedOperationException.class, () -> buffs.getActiveBuffs().clear());
  }

  @Test
  void shouldRejectBuffsThatChangeNothing() {
    assertFalse(buffs.applyBuff(BuffStat.DAMAGE, 2f, 0f));
    assertFalse(buffs.applyBuff(BuffStat.DAMAGE, 1f, 5f));
    assertFalse(buffs.applyBuff(BuffStat.DAMAGE, 0f, 5f));
    assertFalse(buffs.applyBuff(null, 2f, 5f));
    assertTrue(buffs.getActiveBuffs().isEmpty());
    assertEquals(10, stats.getBaseAttack());
  }

  @Test
  void shouldStackThenFullyRevertSpeedBuffs() {
    buffs.applyBuff(BuffStat.SPEED, 2f, 5f);
    buffs.applyBuff(BuffStat.SPEED, 2f, 10f);
    assertEquals(4f, buffs.getSpeedMultiplier());

    when(time.getTime()).thenReturn(5000L);
    player.update();
    assertEquals(2f, buffs.getSpeedMultiplier());

    when(time.getTime()).thenReturn(10000L);
    player.update();
    assertEquals(1f, buffs.getSpeedMultiplier());
  }
}
