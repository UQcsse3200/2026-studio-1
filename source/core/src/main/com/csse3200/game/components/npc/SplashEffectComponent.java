package com.csse3200.game.components.npc;

import com.badlogic.gdx.Gdx;
import com.csse3200.game.components.Component;
import com.csse3200.game.rendering.AnimationRenderComponent;

/** Plays a one-shot effect animation, then removes its entity. */
public class SplashEffectComponent extends Component {
  private final String animationName;
  private AnimationRenderComponent animator;
  private boolean removing = false;

  /**
   * @param animationName name of the animation to play once.
   */
  public SplashEffectComponent(String animationName) {
    this.animationName = animationName;
  }

  @Override
  public void create() {
    animator = entity.getComponent(AnimationRenderComponent.class);
    if (animator != null) {
      animator.startAnimation(animationName);
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
