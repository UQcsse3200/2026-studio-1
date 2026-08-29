package com.csse3200.game.pausemenu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import com.badlogic.gdx.scenes.scene2d.Stage;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class PauseMenuDisplayTest {
  private PauseMenuDisplay display;

  @BeforeEach
  void beforeEach() {
    RenderService renderService = new RenderService();
    renderService.setStage(mock(Stage.class));
    ServiceLocator.registerRenderService(renderService);

    // MusicSlider()'s ChangeListener (fired by setValue() in navigateLeft/navigateRight)
    // reads the resource service for the background music asset - a plain mock returning
    // null for getAsset() satisfies its "if (music != null)" guard, no real asset needed.
    ServiceLocator.registerResourceService(mock(ResourceService.class));

    display = new PauseMenuDisplay();
    Entity entity = new Entity().addComponent(new PauseMenuComponent()).addComponent(display);
    entity.create();
  }

  @Test
  void shouldStartAtFirstButton() {
    assertEquals(0, display.selectedIndex);
  }

  @Test
  void navigateDownShouldMoveForwardByOne() {
    display.navigateDown();
    assertEquals(1, display.selectedIndex);
  }

  @Test
  void navigateDownOnLastItemShouldWrapToFirst() {
    // The slider is the last navigable item, at index buttons.length (3).
    display.selectedIndex = display.buttons.length;

    display.navigateDown();

    assertEquals(0, display.selectedIndex);
  }

  @Test
  void navigateUpOnFirstItemShouldWrapToLast() {
    display.navigateUp();

    assertEquals(display.buttons.length, display.selectedIndex);
  }

  @Test
  void repeatedNavigateDownShouldCycleThroughAllItemsAndReturnToStart() {
    int itemCount = display.buttons.length + 1;
    for (int i = 0; i < itemCount; i++) {
      display.navigateDown();
    }

    assertEquals(0, display.selectedIndex);
  }

  @Test
  void navigateLeftAndRightShouldDoNothingWhenAButtonIsSelected() {
    // selectedIndex starts at 0, a button, not the slider. Seed a known mid-range value -
    // Music_vol is static, so a previous test's slider position can otherwise leak in here.
    display.musicSlider.setValue(0.5f);
    float initialValue = display.musicSlider.getValue();

    display.navigateLeft();
    assertEquals(initialValue, display.musicSlider.getValue());

    display.navigateRight();
    assertEquals(initialValue, display.musicSlider.getValue());
  }

  @Test
  void navigateLeftAndRightShouldAdjustSliderWhenSliderIsSelected() {
    display.selectedIndex = display.buttons.length;
    // Seed a known mid-range value - Music_vol is static, so a previous test's slider
    // position can otherwise leak in here and leave no room to move up or down.
    display.musicSlider.setValue(0.5f);
    float initialValue = display.musicSlider.getValue();

    display.navigateRight();
    assertTrue(display.musicSlider.getValue() > initialValue);

    float afterRight = display.musicSlider.getValue();
    display.navigateLeft();
    assertTrue(display.musicSlider.getValue() < afterRight);
  }

  @Test
  void repeatedNavigateRightShouldNotPushSliderAboveOne() {
    display.selectedIndex = display.buttons.length;
    display.musicSlider.setValue(1f);

    for (int i = 0; i < 5; i++) {
      display.navigateRight();
    }

    assertTrue(display.musicSlider.getValue() <= 1f);
  }

  @Test
  void repeatedNavigateLeftShouldNotPushSliderBelowZero() {
    display.selectedIndex = display.buttons.length;
    display.musicSlider.setValue(0f);

    for (int i = 0; i < 5; i++) {
      display.navigateLeft();
    }

    assertTrue(display.musicSlider.getValue() >= 0f);
  }
}
