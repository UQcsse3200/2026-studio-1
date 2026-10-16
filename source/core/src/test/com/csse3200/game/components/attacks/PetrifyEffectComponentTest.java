package com.csse3200.game.components.attacks;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.csse3200.game.components.ComponentPriority;
import com.csse3200.game.components.effects.SpeedEffectComponent;
import com.csse3200.game.components.enemies.attacks.PetrifyEffectComponent;
import com.csse3200.game.components.player.PlayerActions;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.configs.NPCConfigs;
import com.csse3200.game.entities.configs.enemies.attacks.PetrifyConfig;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.files.FileLoader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tests {@link PetrifyEffectComponent}, which holds all the petrify logic: it petrifies a target
 * through the target's {@code "applySpeedEffect"} event, either from a projectile's own {@code
 * "projectileHit"} event (mirroring {@link LightningFreezeComponentTest}) or from a shooter's
 * {@code "rangedAttackHit"} event (how Medusa uses it). It also checks the config and the design
 * rule that the attack's cooldown is at least twice the petrify duration, when the shooter's
 * component is built.
 *
 * <p>The target is a mock {@code PlayerActions} behind a real {@code SpeedEffectComponent}, so the
 * speed modifier is observed without running a game.
 */
@ExtendWith(GameExtension.class)
class PetrifyEffectComponentTest {
  private static final float COOLDOWN = 7f;

  private PlayerActions playerActions;
  private SpeedEffectComponent speed;
  private Entity target;
  private Entity gaze;

  @BeforeEach
  void setUp() {
    playerActions = mock(PlayerActions.class);
    when(playerActions.getPrio()).thenReturn(ComponentPriority.LOW);
    target = targetWith(playerActions);
    speed = target.getComponent(SpeedEffectComponent.class);

    gaze = new Entity().addComponent(new PetrifyEffectComponent(90));
    gaze.create();
  }

  /** Builds a created entity whose speed effects land on the given mock. */
  private static Entity targetWith(PlayerActions actions) {
    Entity entity = new Entity().addComponent(actions).addComponent(new SpeedEffectComponent());
    entity.create();
    return entity;
  }

  /** Builds a PetrifyConfig of the given length in seconds. */
  private static PetrifyConfig config(float seconds) {
    PetrifyConfig petrify = new PetrifyConfig();
    petrify.duration = seconds;
    return petrify;
  }

  /** Builds a created shooter that petrifies for the given seconds with the given cooldown. */
  private static Entity shooter(float seconds, float cooldown) {
    Entity medusa =
        new Entity().addComponent(PetrifyEffectComponent.forShooter(config(seconds), cooldown));
    medusa.create();
    return medusa;
  }

  /** Calls the package-private cooldown rule by reflection, unwrapping the real exception. */
  private static void callRequireCooldown(float cooldown, float effectSeconds) throws Throwable {
    Method rule =
        PetrifyEffectComponent.class.getDeclaredMethod(
            "requireCooldownCoversEffect", float.class, float.class);
    rule.setAccessible(true);
    try {
      rule.invoke(null, cooldown, effectSeconds);
    } catch (InvocationTargetException e) {
      throw e.getCause();
    }
  }

  @Nested
  @DisplayName("built for a projectile")
  class OnAProjectile {

    /** Should reject negative petrify ticks. */
    @Test
    void shouldRejectNegativePetrifyTicks() {
      assertThrows(IllegalArgumentException.class, () -> new PetrifyEffectComponent(-1));
    }

    /** Should petrify target on projectile hit. */
    @Test
    void shouldPetrifyTargetOnProjectileHit() {
      gaze.getEvents().trigger("projectileHit", target);

      verify(playerActions).addSpeedModifier(any(), eq(0f));
    }

    /** Should unpetrify after configured ticks. */
    @Test
    void shouldUnpetrifyAfterConfiguredTicks() {
      gaze.getEvents().trigger("projectileHit", target);

      for (int i = 0; i < 89; i++) {
        speed.update();
      }
      verify(playerActions, never()).removeSpeedModifier(any());

      speed.update();
      verify(playerActions).removeSpeedModifier(any());
    }

    /** Should ignore target without speed effect component. */
    @Test
    void shouldIgnoreTargetWithoutSpeedEffectComponent() {
      Entity plain = new Entity();
      plain.create();

      assertDoesNotThrow(() -> gaze.getEvents().trigger("projectileHit", plain));
    }

    /** Should not throw when target is null. */
    @Test
    void shouldNotThrowWhenTargetIsNull() {
      assertDoesNotThrow(() -> gaze.getEvents().trigger("projectileHit", (Entity) null));
    }

    /** Should accept zero ticks as permanent. */
    @Test
    void shouldAcceptZeroTicksAsPermanent() {
      assertDoesNotThrow(() -> new PetrifyEffectComponent(0));
    }

    /** Should ignore the shooter event when built for a projectile. */
    @Test
    void shouldIgnoreTheShooterEventWhenBuiltForAProjectile() {
      gaze.getEvents().trigger("rangedAttackHit", target);

      verify(playerActions, never()).addSpeedModifier(any(), any(Float.class));
    }
  }

  @Nested
  @DisplayName("built for a shooter")
  class OnAShooter {

    /** Should petrify what the shooters attack hits. */
    @Test
    void shouldPetrifyWhatTheShootersAttackHits() {
      Entity medusa = shooter(3f, COOLDOWN);

      medusa.getEvents().trigger("rangedAttackHit", target);

      verify(playerActions).addSpeedModifier(any(), eq(0f));
    }

    /** Should not petrify before a hit. */
    @Test
    void shouldNotPetrifyBeforeAHit() {
      shooter(3f, COOLDOWN);

      verify(playerActions, never()).addSpeedModifier(any(), any(Float.class));
    }

    /** Should ignore the projectile event when built for a shooter. */
    @Test
    void shouldIgnoreTheProjectileEventWhenBuiltForAShooter() {
      Entity medusa = shooter(3f, COOLDOWN);

      medusa.getEvents().trigger("projectileHit", target);

      verify(playerActions, never()).addSpeedModifier(any(), any(Float.class));
    }

    /** Should convert seconds to ticks. */
    @Test
    void shouldConvertSecondsToTicks() {
      Entity medusa = shooter(2.5f, 5f);
      medusa.getEvents().trigger("rangedAttackHit", target);

      for (int i = 0; i < 149; i++) {
        speed.update();
      }
      verify(playerActions, never()).removeSpeedModifier(any());

      speed.update(); // round(2.5 * TICKS_PER_SECOND) = 150
      verify(playerActions).removeSpeedModifier(any());
    }

    /** Should freeze movement completely. */
    @Test
    void shouldFreezeMovementCompletely() {
      Entity medusa = shooter(3f, COOLDOWN);

      medusa.getEvents().trigger("rangedAttackHit", target);

      verify(playerActions)
          .addSpeedModifier(any(), eq(PetrifyEffectComponent.PETRIFY_SPEED_MULTIPLIER));
      assertEquals(0f, PetrifyEffectComponent.PETRIFY_SPEED_MULTIPLIER);
    }

    /** Should petrify each hit separately. */
    @Test
    void shouldPetrifyEachHitSeparately() {
      PlayerActions otherActions = mock(PlayerActions.class);
      when(otherActions.getPrio()).thenReturn(ComponentPriority.LOW);
      Entity other = targetWith(otherActions);
      Entity medusa = shooter(3f, COOLDOWN);

      medusa.getEvents().trigger("rangedAttackHit", target);
      medusa.getEvents().trigger("rangedAttackHit", other);

      verify(playerActions, times(1)).addSpeedModifier(any(), eq(0f));
      verify(otherActions, times(1)).addSpeedModifier(any(), eq(0f));
    }

    /** Should ignore a shooter hit on a target without speed effect component. */
    @Test
    void shouldIgnoreAShooterHitOnATargetWithoutSpeedEffectComponent() {
      Entity medusa = shooter(3f, COOLDOWN);
      Entity plain = new Entity();
      plain.create();

      assertDoesNotThrow(() -> medusa.getEvents().trigger("rangedAttackHit", plain));
    }

    /** Should not throw when the shooter hits nothing. */
    @Test
    void shouldNotThrowWhenTheShooterHitsNothing() {
      Entity medusa = shooter(3f, COOLDOWN);

      assertDoesNotThrow(() -> medusa.getEvents().trigger("rangedAttackHit", (Entity) null));
    }

    /** Should match the medusa config in npcs json. */
    @Test
    void shouldMatchTheMedusaConfigInNpcsJson() {
      NPCConfigs configs = FileLoader.readClass(NPCConfigs.class, "configs/NPCs.json");

      // 3 s petrify, 7 s cooldown
      assertDoesNotThrow(
          () ->
              PetrifyEffectComponent.forShooter(
                  configs.medusa.petrify, configs.medusa.ranged.cooldown));
    }
  }

  @Nested
  @DisplayName("building a shooter petrify")
  class BuildingForAShooter {

    /** Should reject a null config. */
    @Test
    void shouldRejectANullConfig() {
      assertThrows(
          IllegalArgumentException.class, () -> PetrifyEffectComponent.forShooter(null, 7f));
    }

    /** Should reject a zero duration (zero ticks would petrify forever). */
    @Test
    void shouldRejectAZeroDuration() {
      assertThrows(
          IllegalArgumentException.class, () -> PetrifyEffectComponent.forShooter(config(0f), 7f));
    }

    /** Should reject a negative duration. */
    @Test
    void shouldRejectANegativeDuration() {
      assertThrows(
          IllegalArgumentException.class, () -> PetrifyEffectComponent.forShooter(config(-1f), 7f));
    }

    /** Should reject a NaN duration. */
    @Test
    void shouldRejectANaNDuration() {
      assertThrows(
          IllegalArgumentException.class,
          () -> PetrifyEffectComponent.forShooter(config(Float.NaN), 7f));
    }

    /** Should reject an infinite duration. */
    @Test
    void shouldRejectAnInfiniteDuration() {
      assertThrows(
          IllegalArgumentException.class,
          () -> PetrifyEffectComponent.forShooter(config(Float.POSITIVE_INFINITY), 7f));
    }

    /** Should reject a duration shorter than one tick. */
    @Test
    void shouldRejectADurationShorterThanOneTick() {
      assertThrows(
          IllegalArgumentException.class,
          () -> PetrifyEffectComponent.forShooter(config(0.001f), 7f));
    }

    /** Should accept a cooldown more than twice the duration. */
    @Test
    void shouldAcceptACooldownMoreThanTwiceTheDuration() {
      assertDoesNotThrow(() -> PetrifyEffectComponent.forShooter(config(2f), 5f));
    }

    /** Should accept exactly twice the duration. */
    @Test
    void shouldAcceptExactlyTwiceTheDuration() {
      assertDoesNotThrow(() -> PetrifyEffectComponent.forShooter(config(2f), 4f));
    }

    /** Should reject just below twice the duration. */
    @Test
    void shouldRejectJustBelowTwiceTheDuration() {
      assertThrows(
          IllegalArgumentException.class,
          () -> PetrifyEffectComponent.forShooter(config(2f), 3.9f));
    }

    /** Should reject a NaN cooldown. */
    @Test
    void shouldRejectANaNCooldown() {
      assertThrows(
          IllegalArgumentException.class,
          () -> PetrifyEffectComponent.forShooter(config(2f), Float.NaN));
    }

    /** Should name the cooldown and the minimum in the message. */
    @Test
    void shouldNameTheCooldownAndTheMinimumInTheMessage() {
      IllegalArgumentException exception =
          assertThrows(
              IllegalArgumentException.class,
              () -> PetrifyEffectComponent.forShooter(config(2f), 3f));

      assertTrue(exception.getMessage().contains("3"));
      assertTrue(exception.getMessage().contains("4"));
    }
  }

  @Nested
  @DisplayName("rule, conversion and constants")
  class RuleAndConversion {

    /** Should accept any cooldown for a zero effect. */
    @Test
    void shouldAcceptAnyCooldownForAZeroEffect() {
      assertDoesNotThrow(() -> callRequireCooldown(0f, 0f));
    }

    /** Should accept a cooldown above the minimum. */
    @Test
    void shouldAcceptACooldownAboveTheMinimum() {
      assertDoesNotThrow(() -> callRequireCooldown(5f, 2f));
    }

    /** Should accept exactly the minimum. */
    @Test
    void shouldAcceptExactlyTheMinimum() {
      assertDoesNotThrow(() -> callRequireCooldown(4f, 2f));
    }

    /** Should reject just below the minimum. */
    @Test
    void shouldRejectJustBelowTheMinimum() {
      assertThrows(IllegalArgumentException.class, () -> callRequireCooldown(3.9f, 2f));
    }

    /** Should reject NaN. */
    @Test
    void shouldRejectNaN() {
      assertThrows(IllegalArgumentException.class, () -> callRequireCooldown(Float.NaN, 2f));
    }

    /** Should turn seconds into ticks. */
    @Test
    void shouldTurnSecondsIntoTicks() {
      assertEquals(0, PetrifyEffectComponent.toTicks(0f));
      assertEquals(60, PetrifyEffectComponent.toTicks(1f));
      assertEquals(150, PetrifyEffectComponent.toTicks(2.5f));
    }

    /** Should round to the nearest tick (60 ticks a second). */
    @Test
    void shouldRoundToTheNearestTick() {
      assertEquals(0, PetrifyEffectComponent.toTicks(0.008f));
      assertEquals(1, PetrifyEffectComponent.toTicks(0.009f));
      assertEquals(2, PetrifyEffectComponent.toTicks(0.025f));
    }

    /** Should keep the named constants. */
    @Test
    void shouldKeepTheNamedConstants() {
      assertEquals(60, PetrifyEffectComponent.TICKS_PER_SECOND);
      assertEquals(2f, PetrifyEffectComponent.COOLDOWN_EFFECT_MULTIPLIER);
      assertEquals(0f, PetrifyEffectComponent.PETRIFY_SPEED_MULTIPLIER);
      assertEquals("applySpeedEffect", PetrifyEffectComponent.APPLY_SPEED_EFFECT_EVENT);
      assertEquals("rangedAttackHit", PetrifyEffectComponent.RANGED_ATTACK_HIT_EVENT);
      assertEquals("projectileHit", PetrifyEffectComponent.PROJECTILE_HIT_EVENT);
    }
  }
}
