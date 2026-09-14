package com.csse3200.game.components.player;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.csse3200.game.components.attacks.CombatStatsComponent;
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
    assertTrue(buffs.applyBuff(BuffStat.DAMAGE, 2f, 5f), "a valid damage buff should apply");
    assertEquals(20, stats.getBaseAttack(), "a 2x damage buff should double base attack of 10");

    when(time.getTime()).thenReturn(4999L);
    player.update();
    assertEquals(20, stats.getBaseAttack(), "the buff should still be active just before 5s");
    assertTrue(buffs.hasBuff(BuffStat.DAMAGE), "the damage buff should still be listed at 4.999s");

    when(time.getTime()).thenReturn(5000L);
    player.update();
    assertEquals(10, stats.getBaseAttack(), "base attack should revert once the buff expires");
    assertFalse(buffs.hasBuff(BuffStat.DAMAGE), "the damage buff should be gone at 5s");
  }

  /** Acceptance criterion: a speed buff applies immediately and reverts after its duration. */
  @Test
  void shouldApplySpeedBuffAndRevertAfterDuration() {
    assertEquals(1f, buffs.getSpeedMultiplier(), "speed should start unbuffed");

    assertTrue(buffs.applyBuff(BuffStat.SPEED, 1.5f, 10f), "a valid speed buff should apply");
    assertEquals(1.5f, buffs.getSpeedMultiplier(), "speed should be 1.5x while buffed");

    when(time.getTime()).thenReturn(9000L);
    player.update();
    assertEquals(1.5f, buffs.getSpeedMultiplier(), "the speed buff should still be active at 9s");

    when(time.getTime()).thenReturn(10000L);
    player.update();
    assertEquals(1f, buffs.getSpeedMultiplier(), "speed should revert once the buff expires");
    assertFalse(buffs.hasBuff(BuffStat.SPEED), "the speed buff should be gone at 10s");
  }

  @Test
  void shouldExpireOnlyTheBuffThatHasElapsed() {
    buffs.applyBuff(BuffStat.DAMAGE, 2f, 5f);
    buffs.applyBuff(BuffStat.SPEED, 1.5f, 20f);
    assertEquals(2, buffs.getActiveBuffs().size(), "both buffs should start active");

    when(time.getTime()).thenReturn(5000L);
    player.update();

    assertEquals(1, buffs.getActiveBuffs().size(), "only the 5s buff should have expired");
    assertFalse(buffs.hasBuff(BuffStat.DAMAGE), "the 5s damage buff should have expired");
    assertTrue(buffs.hasBuff(BuffStat.SPEED), "the 20s speed buff should still be active");
    assertEquals(10, stats.getBaseAttack(), "base attack should be back to normal");
    assertEquals(1.5f, buffs.getSpeedMultiplier(), "speed should still be buffed");
  }

  @Test
  void shouldTriggerEventsForBuffLifecycle() {
    List<String> events = new ArrayList<>();
    player.getEvents().addListener("buffApplied", (ActiveBuff b) -> events.add("applied"));
    player.getEvents().addListener("buffExpired", (ActiveBuff b) -> events.add("expired"));

    buffs.applyBuff(BuffStat.DAMAGE, 2f, 5f);
    assertEquals(List.of("applied"), events, "applying a buff should fire buffApplied");

    when(time.getTime()).thenReturn(5000L);
    player.update();
    assertEquals(List.of("applied", "expired"), events, "expiry should fire buffExpired");
  }

  @Test
  void shouldExposeActiveBuffsForUi() {
    buffs.applyBuff(BuffStat.DAMAGE, 2f, 5f);

    ActiveBuff buff = buffs.getActiveBuffs().get(0);
    assertEquals(BuffStat.DAMAGE, buff.getStat(), "the listed buff should be the damage buff");
    assertEquals(2f, buff.getMagnitude(), "the listed buff should keep its magnitude");
    assertEquals(5f, buff.getDurationSeconds(), "the listed buff should keep its duration");
    assertEquals(5000L, buff.getEndTime(), "a 5s buff started at 0 should end at 5000ms");
    assertEquals(5f, buff.getRemainingSeconds(0L), "all 5s should remain at the start");
    assertEquals(2f, buff.getRemainingSeconds(3000L), "2s should remain after 3s");
    assertEquals(0f, buff.getRemainingSeconds(9000L), "remaining time should not go negative");
  }

  @Test
  void shouldReturnUnmodifiableActiveBuffs() {
    buffs.applyBuff(BuffStat.DAMAGE, 2f, 5f);
    List<ActiveBuff> active = buffs.getActiveBuffs();

    assertThrows(
        UnsupportedOperationException.class,
        active::clear,
        "callers should not be able to change the active buff list");
  }

  @Test
  void shouldRejectBuffsThatChangeNothing() {
    assertFalse(buffs.applyBuff(BuffStat.DAMAGE, 2f, 0f), "a zero duration should be rejected");
    assertFalse(buffs.applyBuff(BuffStat.DAMAGE, 1f, 5f), "a 1.0 multiplier should be rejected");
    assertFalse(buffs.applyBuff(BuffStat.DAMAGE, 0f, 5f), "a zero multiplier should be rejected");
    assertFalse(buffs.applyBuff(null, 2f, 5f), "a missing stat should be rejected");
    assertTrue(buffs.getActiveBuffs().isEmpty(), "no rejected buff should become active");
    assertEquals(10, stats.getBaseAttack(), "base attack should be unchanged");
  }

  /**
   * Stacking rule: only one buff per stat is ever active. Drinking a stronger potion replaces the
   * weaker one rather than multiplying with it, so two speed potions cannot compound into a speed
   * the game was never balanced for.
   */
  @Test
  void shouldKeepOnlyTheStrongestBuffForAStat() {
    buffs.applyBuff(BuffStat.SPEED, 1.5f, 5f);
    buffs.applyBuff(BuffStat.SPEED, 2f, 5f);

    assertEquals(1, buffs.getActiveBuffs().size(), "only one speed buff should be active");
    assertEquals(
        2f, buffs.getSpeedMultiplier(), "the stronger 2x buff should replace 1.5x, not multiply");
  }

  /** Stacking rule: re-drinking a potion restarts its timer instead of adding a second buff. */
  @Test
  void shouldRefreshDurationWhenTheSameBuffIsReapplied() {
    buffs.applyBuff(BuffStat.SPEED, 2f, 5f);

    when(time.getTime()).thenReturn(3000L);
    buffs.applyBuff(BuffStat.SPEED, 2f, 5f);
    assertEquals(1, buffs.getActiveBuffs().size(), "re-drinking should not add a second buff");

    // Under the old rule the first buff expired here; the refresh now runs it to 8000.
    when(time.getTime()).thenReturn(5000L);
    player.update();
    assertTrue(buffs.hasBuff(BuffStat.SPEED), "the refreshed buff should outlast the original 5s");
    assertEquals(1, buffs.getActiveBuffs().size(), "there should still be exactly one buff");

    when(time.getTime()).thenReturn(8000L);
    player.update();
    assertFalse(buffs.hasBuff(BuffStat.SPEED), "the refreshed buff should expire at 8s");
    assertEquals(1f, buffs.getSpeedMultiplier(), "speed should revert once it expires");
  }

  /**
   * A weaker potion drunk while a stronger buff is active is rejected, so {@code
   * ConsumableUseComponent} leaves it in the inventory rather than wasting it.
   */
  @Test
  void shouldRejectAWeakerBuffWhileAStrongerOneIsActive() {
    buffs.applyBuff(BuffStat.SPEED, 2f, 10f);

    assertFalse(
        buffs.applyBuff(BuffStat.SPEED, 1.5f, 10f),
        "a weaker speed buff should be rejected while a stronger one is active");
    assertEquals(2f, buffs.getSpeedMultiplier(), "the stronger buff should stay in effect");
    assertEquals(1, buffs.getActiveBuffs().size(), "there should still be exactly one buff");
  }

  /**
   * The unbuffed base attack is read when a damage buff starts, not once in create(), so a base
   * attack changed elsewhere (a level up, a new weapon) is not clobbered when the buff expires.
   */
  @Test
  void shouldUseTheCurrentBaseAttackWhenABuffStarts() {
    stats.setBaseAttack(20);

    buffs.applyBuff(BuffStat.DAMAGE, 2f, 5f);
    assertEquals(40, stats.getBaseAttack(), "the buff should double the current base attack");

    when(time.getTime()).thenReturn(5000L);
    player.update();
    assertEquals(
        20, stats.getBaseAttack(), "expiry should restore the changed value, not the original 10");
  }

  /** Acceptance criterion: Resistance reduces incoming damage and stops doing so on expiry. */
  @Test
  void shouldReduceIncomingDamageWhileResistanceIsActive() {
    assertEquals(1f, buffs.getIncomingDamageMultiplier(), "incoming damage should start normal");

    assertTrue(
        buffs.applyBuff(BuffStat.RESISTANCE, 0.5f, 5f), "a valid resistance buff should apply");
    assertEquals(
        0.5f, buffs.getIncomingDamageMultiplier(), "resistance should halve incoming damage");

    when(time.getTime()).thenReturn(5000L);
    player.update();
    assertEquals(
        1f, buffs.getIncomingDamageMultiplier(), "incoming damage should revert on expiry");
    assertFalse(buffs.hasBuff(BuffStat.RESISTANCE), "the resistance buff should be gone at 5s");
  }

  /**
   * For resistance a lower multiplier is the stronger potion, so "strongest wins" has to compare
   * the other way around for it.
   */
  @Test
  void shouldTreatLowerResistanceAsTheStrongerPotion() {
    assertTrue(buffs.applyBuff(BuffStat.RESISTANCE, 0.8f, 5f), "the first resistance should apply");

    assertTrue(
        buffs.applyBuff(BuffStat.RESISTANCE, 0.5f, 5f),
        "a lower, stronger resistance should replace a weaker one");
    assertEquals(
        0.5f, buffs.getIncomingDamageMultiplier(), "the stronger 0.5 resistance should apply");

    assertFalse(
        buffs.applyBuff(BuffStat.RESISTANCE, 0.8f, 5f),
        "a higher, weaker resistance should be rejected");
    assertEquals(
        0.5f, buffs.getIncomingDamageMultiplier(), "the stronger resistance should stay in effect");
    assertEquals(1, buffs.getActiveBuffs().size(), "there should be exactly one resistance buff");
  }

  /** Buffs on different stats are independent and still apply together. */
  @Test
  void shouldKeepBuffsOnDifferentStatsSeparate() {
    buffs.applyBuff(BuffStat.SPEED, 2f, 5f);
    buffs.applyBuff(BuffStat.DAMAGE, 2f, 5f);

    assertEquals(2, buffs.getActiveBuffs().size(), "speed and damage buffs should both be active");
    assertEquals(2f, buffs.getSpeedMultiplier(), "the speed buff should apply");
    assertEquals(2f, buffs.getDamageMultiplier(), "the damage buff should apply");
  }
}
