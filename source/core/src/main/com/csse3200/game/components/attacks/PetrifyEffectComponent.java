package com.csse3200.game.components.attacks;

import com.csse3200.game.components.Component;
import com.csse3200.game.entities.Entity;

/**
 * Petrifies (fully immobilises) whatever this projectile hits, for a configured number of ticks, by
 * triggering the target's own {@code "applySpeedEffect"} event with a {@code 0f} speed multiplier -
 * the same mechanism {@link com.csse3200.game.components.effects.SpeedEffectComponent} already uses
 * to revert itself once the duration elapses.
 *
 * <p>Deliberately mirrors {@link LightningFreezeComponent}'s structure (attach to a projectile
 * entity, listen for that entity's own {@code "projectileHit"} event): the two effects are
 * mechanically identical (an on-hit, timed, zero-speed stun), just thematically different (Medusa's
 * gaze vs. Zeus's lightning) and with no accompanying sound effect, since no petrify-specific sound
 * asset exists yet.
 */
public class PetrifyEffectComponent extends Component {
  private final int petrifyTicks;

  /**
   * @param petrifyTicks number of ticks the hit target is petrified for; {@code 0} applies the
   *     effect instantly with no duration (matches {@code SpeedEffectComponent}'s "instant,
   *     permanent" convention for a duration of zero).
   * @throws IllegalArgumentException if {@code petrifyTicks} is negative
   */
  public PetrifyEffectComponent(int petrifyTicks) {
    if (petrifyTicks < 0) {
      throw new IllegalArgumentException("petrifyTicks must not be negative.");
    }
    this.petrifyTicks = petrifyTicks;
  }

  @Override
  public void create() {
    entity.getEvents().addListener("projectileHit", this::onHit);
  }

  /**
   * Event handler for this projectile's own {@code "projectileHit"} event. Petrifies {@code target}
   * by triggering its {@code "applySpeedEffect"} event - a no-op if the target has no {@link
   * com.csse3200.game.components.effects.SpeedEffectComponent} listening for it.
   *
   * @param target the entity the gaze projectile hit
   */
  private void onHit(Entity target) {
    if (target == null) {
      return;
    }
    target.getEvents().trigger("applySpeedEffect", petrifyTicks, 0f);
  }
}
