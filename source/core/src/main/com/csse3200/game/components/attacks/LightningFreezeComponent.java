package com.csse3200.game.components.attacks;

import com.badlogic.gdx.audio.Sound;
import com.csse3200.game.components.Component;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.pausemenu.AudioSettings;
import com.csse3200.game.services.ServiceLocator;

public class LightningFreezeComponent extends Component {
  private final int freezeTicks;

  public LightningFreezeComponent(int freezeTicks) {
    this.freezeTicks = freezeTicks;
  }

  @Override
  public void create() {
    entity.getEvents().addListener("projectileHit", this::onHit);
  }

  private void onHit(Entity target) {
    Sound zapSound = ServiceLocator.getResourceService().getAsset("sounds/zap.mp3", Sound.class);
    zapSound.play(AudioSettings.getEffectiveEffectsVolume());
    target.getEvents().trigger("applySpeedEffect", freezeTicks, 0f);
  }
}
