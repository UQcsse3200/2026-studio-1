package com.csse3200.game.components.player;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Array;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.events.EventHandler;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import java.lang.reflect.Field;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class PlayerStatsDisplayTest {

  private PlayerStatsDisplay display;
  private Texture green, yellow, red, empty;

  @BeforeEach
  void setUp() {
    green = mock(Texture.class);
    yellow = mock(Texture.class);
    red = mock(Texture.class);
    empty = mock(Texture.class);

    // PlayerStatsDisplay loads its heart textures by asset path on create(), so the mocked
    // ResourceService must resolve each path to the matching mock texture above.
    ResourceService resourceService = mock(ResourceService.class);
    when(resourceService.getAsset("images/heart-green.png", Texture.class)).thenReturn(green);
    when(resourceService.getAsset("images/heart-yellow.png", Texture.class)).thenReturn(yellow);
    when(resourceService.getAsset("images/heart.png", Texture.class)).thenReturn(red);
    when(resourceService.getAsset("images/heart-empty.png", Texture.class)).thenReturn(empty);
    ServiceLocator.registerResourceService(resourceService);

    // UIComponent.create() (the superclass) pulls its Stage from the RenderService, so the
    // stage must be stubbed here rather than injected directly into the display afterwards.
    RenderService renderService = mock(RenderService.class);
    when(renderService.getStage()).thenReturn(mock(Stage.class));
    ServiceLocator.registerRenderService(renderService);

    // Entity starts at full health; individual tests drive health changes explicitly via
    // updatePlayerHealthUI() rather than through this stat, since PlayerStatsDisplay only
    // reads it once, at creation time.
    CombatStatsComponent stats = mock(CombatStatsComponent.class);
    when(stats.getHealth()).thenReturn(100);

    Entity entity = mock(Entity.class);
    when(entity.getComponent(CombatStatsComponent.class)).thenReturn(stats);
    when(entity.getEvents()).thenReturn(mock(EventHandler.class));

    display = new PlayerStatsDisplay();
    setField(display, "entity", entity);
    display.create();
  }

  @Nested
  public class HeartColourTest {
    @Test
    public void highHealth_showsGreenHearts() {
      display.updatePlayerHealthUI(70); // 7 hearts -> green
      assertFilledTexture(green);
    }

    @Test
    public void midHealth_showsYellowHearts() {
      display.updatePlayerHealthUI(50); // 5 hearts -> yellow
      assertFilledTexture(yellow);
    }

    @Test
    public void lowHealth_showsRedHearts() {
      display.updatePlayerHealthUI(20); // 2 hearts -> red
      assertFilledTexture(red);
    }
  }

  @Nested
  class HealthLabelTest {
    @Test
    void updatePlayerHealthUI_setsExactHealthText() {
      display.updatePlayerHealthUI(42);

      Label label = getField(display, "healthLabel");
      assertEquals("Health = 42", label.getText().toString());
    }
  }

  @Nested
  class HealthOverUnderTest {
    @Test
    void negativeHealth_showsZeroFilledHearts() {
      display.updatePlayerHealthUI(-10);
      assertEquals(0, countFilledHearts());
    }

    @Test
    void healthAboveMax_isClampedToTenHearts() {
      display.updatePlayerHealthUI(150);
      assertEquals(10, countFilledHearts());
    }
  }

  /*
   *  Helpers
   */
  /** Counts hearts whose drawable is not the empty-heart texture. */
  private int countFilledHearts() {
    Array<Image> hearts = getField(display, "heartImages");
    int filled = 0;
    for (int i = 0; i < hearts.size; i++) {
      TextureRegionDrawable drawable = (TextureRegionDrawable) hearts.get(i).getDrawable();
      if (drawable.getRegion().getTexture() != empty) {
        filled++;
      }
    }
    return filled;
  }

  /** Asserts every non-empty heart uses exactly the given texture. */
  private void assertFilledTexture(Texture expected) {
    Array<Image> hearts = getField(display, "heartImages");
    for (int i = 0; i < hearts.size; i++) {
      TextureRegionDrawable drawable = (TextureRegionDrawable) hearts.get(i).getDrawable();
      Texture texture = drawable.getRegion().getTexture();
      if (texture != empty) {
        assertSame(expected, texture, "Filled heart at index " + i + " has wrong texture");
      }
    }
  }

  @SuppressWarnings("unchecked")
  private <T> T getField(Object obj, String name) {
    try {
      Field field = PlayerStatsDisplay.class.getDeclaredField(name);
      field.setAccessible(true);
      return (T) field.get(obj);
    } catch (Exception e) {
      fail("Cannot read field '" + name + "': " + e.getMessage());
      return null;
    }
  }

  private void setField(Object obj, String name, Object value) {
    try {
      Field field = findField(obj.getClass(), name);
      field.setAccessible(true);
      field.set(obj, value);
    } catch (Exception e) {
      fail("Cannot set field '" + name + "': " + e.getMessage());
    }
  }

  /** Walks up the class hierarchy since {@code entity} is declared on a superclass. */
  private Field findField(Class<?> clazz, String name) throws NoSuchFieldException {
    while (clazz != null) {
      try {
        return clazz.getDeclaredField(name);
      } catch (NoSuchFieldException e) {
        clazz = clazz.getSuperclass();
      }
    }
    throw new NoSuchFieldException(name);
  }
}
