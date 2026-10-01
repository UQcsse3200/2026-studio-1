package com.csse3200.game.components.npc;

import com.badlogic.gdx.Gdx;
import com.csse3200.game.components.Component;
import com.csse3200.game.rendering.AnimationRenderComponent;

/** Plays a one-shot splash animation, then removes its entity. */
public class SplashEffectComponent extends Component {
  private AnimationRenderComponent animator;
  private boolean removing = false;

  @Override
  public void create() {
    animator = entity.getComponent(AnimationRenderComponent.class);
    if (animator != null) {
      animator.startAnimation("splash");
    }
  }

  @Override
  public void update() {
    if (removing) {
      return;
    }
    if (animator == null || animator.isFinished()) {
      removing = true;
      Gdx.app.postRunnable(entity::dispose);
    }
  }
}
