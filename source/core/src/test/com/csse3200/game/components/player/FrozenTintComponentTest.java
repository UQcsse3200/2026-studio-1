package com.csse3200.game.components.player;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.floatThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.csse3200.game.components.effects.FrozenCause;
import com.csse3200.game.components.effects.SpeedEffectComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;

/**
 * Tests {@link FrozenTintComponent}, the coloured overlay drawn over a stopped player. Follows
 * {@link UpgradeActivationFlashComponentTest}: a mock SpriteBatch records what was drawn, and the
 * overlay must always put the batch colour back to white as its last action.
 */
@ExtendWith(GameExtension.class)
class FrozenTintComponentTest {
  private static final String TEXTURE_PATH = "images/Shield.png";

  private Entity entity;
  private Texture texture;
  private FrozenTintComponent tint;

  @BeforeEach
  void setUp() {
    texture = mock(Texture.class);
    ResourceService resources = mock(ResourceService.class);
    when(resources.getAsset(TEXTURE_PATH, Texture.class)).thenReturn(texture);
    ServiceLocator.registerResourceService(resources);
    ServiceLocator.registerRenderService(new RenderService());

    tint = new FrozenTintComponent();
    entity = new Entity().addComponent(tint);
    entity.create();
  }

  /** Checks the colour of the one tinted draw: red, green and blue, at half see-through. */
  private void verifyTintedDraw(SpriteBatch batch, float r, float g, float b) {
    InOrder order = inOrder(batch);
    order
        .verify(batch)
        .setColor(
            floatThat(red -> Math.abs(red - r) < 0.001f),
            floatThat(green -> Math.abs(green - g) < 0.001f),
            floatThat(blue -> Math.abs(blue - b) < 0.001f),
            floatThat(alpha -> Math.abs(alpha - 0.5f) < 0.001f));
    // Default entity: position (0, 0), scale (1, 1). Padded 1.4x and centred: size 1.4, x/y -0.2.
    order
        .verify(batch)
        .draw(
            eq(texture),
            floatThat(x -> Math.abs(x - (-0.2f)) < 0.001f),
            floatThat(y -> Math.abs(y - (-0.2f)) < 0.001f),
            floatThat(w -> Math.abs(w - 1.4f) < 0.001f),
            floatThat(h -> Math.abs(h - 1.4f) < 0.001f));
    order.verify(batch).setColor(Color.WHITE);
    verifyNoMoreInteractions(batch);
  }

  @Test
  void shouldDrawNothingWhileNotFrozen() {
    SpriteBatch batch = mock(SpriteBatch.class);

    tint.render(batch);

    verifyNoInteractions(batch);
  }

  @Test
  void shouldDrawTheBlueGreyOverlayThenResetToWhiteWhenFrozen() {
    entity.getEvents().trigger(SpeedEffectComponent.FROZEN_START_EVENT);
    SpriteBatch batch = mock(SpriteBatch.class);

    tint.render(batch);

    verifyTintedDraw(batch, 0.55f, 0.75f, 1f);
  }

  @Test
  void shouldDrawTheStoneGreyOverlayWhenPetrified() {
    entity.getEvents().trigger(FrozenCause.STATUS_CAUSE_EVENT, FrozenCause.PETRIFIED);
    entity.getEvents().trigger(SpeedEffectComponent.FROZEN_START_EVENT);
    SpriteBatch batch = mock(SpriteBatch.class);

    tint.render(batch);

    verifyTintedDraw(batch, 0.62f, 0.62f, 0.62f);
  }

  @Test
  void shouldStopDrawingWhenTheFreezeEnds() {
    entity.getEvents().trigger(SpeedEffectComponent.FROZEN_START_EVENT);
    entity.getEvents().trigger(SpeedEffectComponent.FROZEN_END_EVENT);
    SpriteBatch batch = mock(SpriteBatch.class);

    tint.render(batch);

    verifyNoInteractions(batch);
  }

  @Test
  void shouldStopDrawingOnDeath() {
    entity.getEvents().trigger(SpeedEffectComponent.FROZEN_START_EVENT);
    entity.getEvents().trigger("death");
    SpriteBatch batch = mock(SpriteBatch.class);

    tint.render(batch);

    verifyNoInteractions(batch);
  }

  @Test
  void shouldForgetThePetrifiedCauseForTheNextFreeze() {
    entity.getEvents().trigger(FrozenCause.STATUS_CAUSE_EVENT, FrozenCause.PETRIFIED);
    entity.getEvents().trigger(SpeedEffectComponent.FROZEN_START_EVENT);
    entity.getEvents().trigger(SpeedEffectComponent.FROZEN_END_EVENT);

    entity.getEvents().trigger(SpeedEffectComponent.FROZEN_START_EVENT); // no cause named
    SpriteBatch batch = mock(SpriteBatch.class);
    tint.render(batch);

    verifyTintedDraw(batch, 0.55f, 0.75f, 1f);
  }

  @Test
  void shouldChooseTheFrozenColourForAnUnknownOrMissingCause() {
    Color frozen = FrozenTintComponent.colourFor(FrozenCause.FROZEN);

    assertEquals(frozen, FrozenTintComponent.colourFor("Burning"));
    assertEquals(frozen, FrozenTintComponent.colourFor(null));
  }

  @Test
  void shouldChooseADifferentColourWhenPetrified() {
    Color petrified = FrozenTintComponent.colourFor(FrozenCause.PETRIFIED);

    assertEquals(0.62f, petrified.r, 0.001f);
    assertNotEquals(FrozenTintComponent.colourFor(FrozenCause.FROZEN), petrified);
  }
}
