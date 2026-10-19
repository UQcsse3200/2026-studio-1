package com.csse3200.game.components.npc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.rendering.AnimationRenderComponent;
import com.csse3200.game.services.ServiceLocator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

/**
 * Tests {@link SplashEffectComponent}: a one-shot effect that plays an animation, then removes its
 * entity. The animator is mocked so the tests decide when the animation is finished, and the Gdx
 * application is mocked so the scheduled removal can be captured and run by hand.
 */
@ExtendWith(GameExtension.class)
class SplashEffectComponentTest {
  private Application originalApp;
  private EntityService entityService;
  private AnimationRenderComponent animator;
  private Entity effect;

  @BeforeEach
  void beforeEach() {
    originalApp = Gdx.app;
    Gdx.app = mock(Application.class);
    entityService = mock(EntityService.class);
    ServiceLocator.registerEntityService(entityService);

    animator = mock(AnimationRenderComponent.class);
    effect = new Entity().addComponent(animator).addComponent(new SplashEffectComponent("splash"));
    effect.create();
  }

  @AfterEach
  void afterEach() {
    Gdx.app = originalApp;
  }

  @Test
  void shouldPlayItsAnimationWhenCreated() {
    verify(animator).startAnimation("splash");
  }

  @Test
  void shouldKeepItsEntityWhileTheAnimationIsPlaying() {
    when(animator.isFinished()).thenReturn(false);

    effect.getComponent(SplashEffectComponent.class).update();

    verify(Gdx.app, never()).postRunnable(any(Runnable.class));
  }

  @Test
  void shouldRemoveItsEntityOnceTheAnimationHasFinished() {
    when(animator.isFinished()).thenReturn(true);

    effect.getComponent(SplashEffectComponent.class).update();
    runScheduledRemoval();

    verify(entityService).unregister(effect);
  }

  @Test
  void shouldScheduleTheRemovalOnlyOnce() {
    when(animator.isFinished()).thenReturn(true);
    SplashEffectComponent component = effect.getComponent(SplashEffectComponent.class);

    component.update();
    component.update();

    verify(Gdx.app, times(1)).postRunnable(any(Runnable.class));
  }

  @Test
  void shouldRemoveItsEntityAtOnceWhenThereIsNoAnimator() {
    Entity bare = new Entity().addComponent(new SplashEffectComponent("splash"));
    bare.create();

    bare.getComponent(SplashEffectComponent.class).update();
    runScheduledRemoval();

    verify(entityService).unregister(bare);
  }

  /** Runs the removal that the component handed to the (mocked) Gdx application. */
  private void runScheduledRemoval() {
    ArgumentCaptor<Runnable> removal = ArgumentCaptor.forClass(Runnable.class);
    verify(Gdx.app).postRunnable(removal.capture());
    removal.getValue().run();
  }
}
