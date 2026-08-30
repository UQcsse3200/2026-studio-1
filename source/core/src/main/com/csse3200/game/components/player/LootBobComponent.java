package com.csse3200.game.components.player;

import com.csse3200.game.components.Component;
import com.csse3200.game.services.ServiceLocator;

// Gives a ground loot entity a small floating animation.
public class LootBobComponent extends Component {
  private static final float BOB_HEIGHT = 0.12f;
  private static final float BOB_SPEED = 3f;

  private float startY;
  private float time;

  @Override
  public void create() {
    startY = entity.getPosition().y;
  }

  @Override
  public void update() {
    time += ServiceLocator.getTimeSource().getDeltaTime();

    float offset = (float) Math.sin(time * BOB_SPEED) * BOB_HEIGHT;

    entity.setPosition(entity.getPosition().x, startY + offset);
  }
}
