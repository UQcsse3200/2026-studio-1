package com.csse3200.game.components.enemies.attacks;

import com.badlogic.gdx.audio.Sound;
import com.csse3200.game.components.Component;
import com.csse3200.game.components.effects.FrozenCause;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.pausemenu.AudioSettings;
import com.csse3200.game.services.ServiceLocator;

/**
 * Freezes whatever a lightning bolt hits, for a set time, by triggering the target's own {@code
 * "applySpeedEffect"} event with a {@code 0f} speed multiplier. Just before that, it tells the
 * target why it is being stopped ({@link FrozenCause#FROZEN}) so the screen can show it. Listens
 * for the bolt's own {@code "projectileHit"} event.
 */
public class LightningFreezeComponent extends Component {
  private final int freezeTicks;

  public LightningFreezeComponent(int freezeTicks) {
    this.freezeTicks = freezeTicks;
  }

  @Override
  public void create() {
    entity.getEvents().addListener("projectileHit", this::onHit);
  }

  /**
   * Carries out logic for what happens when lightning hits an entity. Names the cause, freezes the
   * entity and plays a zap sound effect.
   *
   * @param target the entity getting hit by lightning
   */
  private void onHit(Entity target) {
    Sound zapSound = ServiceLocator.getResourceService().getAsset("sounds/zap.mp3", Sound.class);
    zapSound.play(AudioSettings.getEffectiveEffectsVolume());
    target.getEvents().trigger(FrozenCause.STATUS_CAUSE_EVENT, FrozenCause.FROZEN);
    target.getEvents().trigger("applySpeedEffect", freezeTicks, 0f);
  }
}
