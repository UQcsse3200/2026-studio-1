package com.csse3200.game.rendering;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

@ExtendWith(GameExtension.class)
class SpinningTextureRenderComponentTest {
  private static final String TEXTURE = "images/test.png";
  private static final float DEGREES_PER_SECOND = 90f;

  private GameTime time;
  private SpinningTextureRenderComponent spinner;

  @BeforeEach
  void setUp() {
    time = mock(GameTime.class);
    when(time.getDeltaTime()).thenReturn(0f);
    ServiceLocator.registerTimeSource(time);

    ResourceService resources = mock(ResourceService.class);
    when(resources.getAsset(TEXTURE, Texture.class)).thenReturn(mock(Texture.class));
    ServiceLocator.registerResourceService(resources);

    spinner = new SpinningTextureRenderComponent(TEXTURE, DEGREES_PER_SECOND);
    new Entity().addComponent(spinner);
  }

  @Test
  void shouldStartUnrotated() {
    assertEquals(0f, drawnRotation(), 0.001f);
  }

  @Test
  void shouldIncreaseRotationOnUpdate() {
    when(time.getDeltaTime()).thenReturn(0.5f);

    spinner.update();
    assertEquals(45f, drawnRotation(), 0.001f);

    spinner.update();
    assertEquals(90f, drawnRotation(), 0.001f);
  }

  @Test
  void shouldNotRotateWhilePaused() {
    when(time.getDeltaTime()).thenReturn(0.5f);
    spinner.update();

    when(time.getDeltaTime()).thenReturn(0f);
    spinner.update();
    spinner.update();

    assertEquals(45f, drawnRotation(), 0.001f);
  }

  @Test
  void shouldWrapRotationPastFullTurn() {
    when(time.getDeltaTime()).thenReturn(5f);

    spinner.update();

    assertEquals(90f, drawnRotation(), 0.001f);
  }

  /** Draws once and returns the rotation passed to the sprite batch. */
  private float drawnRotation() {
    SpriteBatch batch = mock(SpriteBatch.class);
    spinner.draw(batch);
    ArgumentCaptor<Float> rotation = ArgumentCaptor.forClass(Float.class);
    verify(batch)
        .draw(
            any(TextureRegion.class),
            anyFloat(),
            anyFloat(),
            anyFloat(),
            anyFloat(),
            anyFloat(),
            anyFloat(),
            eq(1f),
            eq(1f),
            rotation.capture());
    return rotation.getValue();
  }
}
