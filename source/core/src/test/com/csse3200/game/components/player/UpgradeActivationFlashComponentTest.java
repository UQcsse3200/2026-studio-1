package com.csse3200.game.components.player;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.floatThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import java.lang.reflect.Field;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;

/**
 * No ShieldRenderComponent test exists to match, so this stays minimal: flashTimeRemaining resets
 * on "upgradeActivated", counts down via update(), and draw() (a) does nothing while not flashing,
 * (b) tints gold (not white - a white tint over an opaque sprite is nearly invisible) and draws at
 * ShieldRenderComponent's own SCALE_PADDING (1.4x the player's scale, not the raw scale) so the
 * flash is actually visible, and (c) always resets the SpriteBatch colour back to white as its last
 * action whenever it does draw something - the exact discipline PlayerStatsDisplay.HealthHeartImage
 * .draw() uses, verified here via a mocked SpriteBatch and Mockito's InOrder +
 * verifyNoMoreInteractions.
 */
@ExtendWith(GameExtension.class)
class UpgradeActivationFlashComponentTest {
  private static final String TEXTURE_PATH = "images/Shield.png";

  private UpgradeActivationFlashComponent flash;
  private Entity entity;
  private Texture texture;

  @BeforeEach
  void beforeEach() {
    texture = mock(Texture.class);
    ResourceService resources = mock(ResourceService.class);
    when(resources.getAsset(TEXTURE_PATH, Texture.class)).thenReturn(texture);
    ServiceLocator.registerResourceService(resources);
    ServiceLocator.registerRenderService(new RenderService());
    ServiceLocator.registerTimeSource(new GameTime());

    Gdx.graphics = mock(Graphics.class);
    when(Gdx.graphics.getDeltaTime()).thenReturn(0.1f); // deterministic 0.1s-per-update() delta

    flash = new UpgradeActivationFlashComponent();
    entity = new Entity().addComponent(flash);
    entity.create();
  }

  private float flashTimeRemaining() throws Exception {
    Field field = UpgradeActivationFlashComponent.class.getDeclaredField("flashTimeRemaining");
    field.setAccessible(true);
    return (float) field.get(flash);
  }

  @Test
  void upgradeActivatedResetsFlashTimeRemainingToFlashDuration() throws Exception {
    entity.getEvents().trigger("upgradeActivated");

    assertEquals(0.4f, flashTimeRemaining(), 0.0001f);
  }

  @Test
  void retriggeringWhileAlreadyFadingRestartsRatherThanStacks() throws Exception {
    entity.getEvents().trigger("upgradeActivated");
    flash.update(); // 0.4 - 0.1 = 0.3s remaining
    assertEquals(0.3f, flashTimeRemaining(), 0.0001f);

    entity.getEvents().trigger("upgradeActivated"); // restarts - doesn't add to the 0.3s left

    assertEquals(0.4f, flashTimeRemaining(), 0.0001f);
  }

  @Test
  void updateCountsDownFlashTimeRemainingFlooredAtZero() throws Exception {
    entity.getEvents().trigger("upgradeActivated"); // 0.4s

    flash.update(); // 0.3s
    flash.update(); // 0.2s
    flash.update(); // 0.1s
    flash.update(); // 0.0s
    flash.update(); // would go negative - floored at 0

    assertEquals(0f, flashTimeRemaining(), 0.0001f);
  }

  @Test
  void drawDoesNothingWhenNotFlashing() {
    SpriteBatch batch = mock(SpriteBatch.class);

    flash.render(batch);

    verifyNoInteractions(batch);
  }

  @Test
  void drawTintsGoldThenDrawsPaddedThenResetsToWhiteAsItsFinalAction() {
    entity.getEvents().trigger("upgradeActivated");
    SpriteBatch batch = mock(SpriteBatch.class);

    flash.render(batch);

    // Default entity: position (0, 0), scale (1, 1). Padded at 1.4x (same as
    // ShieldRenderComponent), centred on the player: width/height = 1.4, x/y = -0.2.
    InOrder order = inOrder(batch);
    order
        .verify(batch)
        .setColor(
            floatThat(r -> Math.abs(r - 1f) < 0.001f),
            floatThat(g -> Math.abs(g - 0.85f) < 0.001f),
            floatThat(b -> Math.abs(b - 0.3f) < 0.001f),
            anyFloat()); // gold, not white - a white tint here would be nearly invisible
    order
        .verify(batch)
        .draw(
            eq(texture),
            floatThat(x -> Math.abs(x - (-0.2f)) < 0.001f),
            floatThat(y -> Math.abs(y - (-0.2f)) < 0.001f),
            floatThat(w -> Math.abs(w - 1.4f) < 0.001f), // padded width, not the raw scale of 1
            floatThat(h -> Math.abs(h - 1.4f) < 0.001f)); // padded height, not the raw scale of 1
    order.verify(batch).setColor(Color.WHITE);
    // setColor(WHITE) is genuinely the last thing draw() does - nothing else touches batch after.
    verifyNoMoreInteractions(batch);
  }
}
