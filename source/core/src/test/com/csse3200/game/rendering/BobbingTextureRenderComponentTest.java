package com.csse3200.game.rendering;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(GameExtension.class)
@ExtendWith(MockitoExtension.class)
class BobbingTextureRenderComponentTest {
  private static final float DELTA = 0.001f;

  @Mock Texture texture;
  @Mock SpriteBatch spriteBatch;
  @Mock Entity entity;

  private GameTime time;

  @BeforeEach
  void setUp() {
    time = mock(GameTime.class);
    ServiceLocator.registerTimeSource(time);
    ServiceLocator.registerRenderService(mock(RenderService.class));
  }

  /**
   * Builds a component with an amplitude and speed of 1, so the offset is simply the sine of the
   * elapsed seconds and the expected values are easy to check.
   *
   * @return a created component attached to the mock entity
   */
  private BobbingTextureRenderComponent makeComponent() {
    BobbingTextureRenderComponent component = new BobbingTextureRenderComponent(texture, 1f, 1f);
    component.setEntity(entity);
    component.create();
    return component;
  }

  @Test
  void shouldRestAtStartOfTheWave() {
    when(time.getTime()).thenReturn(0L);

    assertEquals(0f, makeComponent().getBobOffset(), DELTA);
  }

  @Test
  void shouldRiseThenFallOverOneCycle() {
    BobbingTextureRenderComponent component = makeComponent();

    // A quarter of the way through the wave the sprite is at its highest point.
    when(time.getTime()).thenReturn(1571L);
    assertEquals(1f, component.getBobOffset(), DELTA);

    // Half way through it is back at its resting position.
    when(time.getTime()).thenReturn(3142L);
    assertEquals(0f, component.getBobOffset(), DELTA);

    // Three quarters of the way through it is at its lowest point.
    when(time.getTime()).thenReturn(4712L);
    assertEquals(-1f, component.getBobOffset(), DELTA);
  }

  @Test
  void shouldNeverExceedItsAmplitude() {
    BobbingTextureRenderComponent component = new BobbingTextureRenderComponent(texture, 0.08f, 3f);
    component.setEntity(entity);
    component.create();

    for (long millis = 0; millis < 5000; millis += 50) {
      when(time.getTime()).thenReturn(millis);
      float offset = component.getBobOffset();
      assertTrue(Math.abs(offset) <= 0.08f + DELTA, "offset out of range at " + millis + "ms");
    }
  }

  @Test
  void shouldDrawAtRestingPositionWhenOffsetIsZero() {
    when(time.getTime()).thenReturn(0L);
    when(entity.getPosition()).thenReturn(new Vector2(2f, 2f));
    when(entity.getScale()).thenReturn(new Vector2(1f, 1f));

    makeComponent().render(spriteBatch);

    verify(spriteBatch).draw(texture, 2f, 2f, 1f, 1f);
  }

  @Test
  void shouldDrawAboveRestingPositionWhenBobbing() {
    when(time.getTime()).thenReturn(1571L);
    when(entity.getPosition()).thenReturn(new Vector2(2f, 2f));
    when(entity.getScale()).thenReturn(new Vector2(1f, 1f));

    BobbingTextureRenderComponent component = makeComponent();
    float expectedY = 2f + component.getBobOffset();
    component.render(spriteBatch);

    verify(spriteBatch).draw(texture, 2f, expectedY, 1f, 1f);
  }

  @Test
  void shouldOffsetEntitiesDifferentlySoTheyDoNotMoveTogether() {
    when(time.getTime()).thenReturn(0L);

    Entity second = mock(Entity.class);
    when(second.getId()).thenReturn(3);

    BobbingTextureRenderComponent first = makeComponent();
    BobbingTextureRenderComponent other = new BobbingTextureRenderComponent(texture, 1f, 1f);
    other.setEntity(second);
    other.create();

    assertTrue(Math.abs(first.getBobOffset() - other.getBobOffset()) > DELTA);
  }

  @Test
  void shouldScaleEntityToTextureRatio() {
    when(texture.getWidth()).thenReturn(16);
    when(texture.getHeight()).thenReturn(32);

    makeComponent().scaleEntity();

    verify(entity).setScale(1f, 2f);
  }
}
