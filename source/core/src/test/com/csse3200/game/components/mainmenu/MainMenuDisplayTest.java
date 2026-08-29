package com.csse3200.game.components.mainmenu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.graphics.Texture;
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
class MainMenuDisplayTest {
  private MainMenuDisplay display;

  @BeforeEach
  void beforeEach() {
    RenderService renderService = new RenderService();
    renderService.setStage(mock(Stage.class));
    ServiceLocator.registerRenderService(renderService);

    ResourceService resourceService = mock(ResourceService.class);
    when(resourceService.getAsset(eq("images/box_boy_title.png"), eq(Texture.class)))
        .thenReturn(mock(Texture.class));
    ServiceLocator.registerResourceService(resourceService);

    display = new MainMenuDisplay();
    Entity entity = new Entity().addComponent(display);
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
  void navigateDownOnLastButtonShouldWrapToFirst() {
    display.selectedIndex = display.buttons.length - 1;

    display.navigateDown();

    assertEquals(0, display.selectedIndex);
  }

  @Test
  void navigateUpOnFirstButtonShouldWrapToLast() {
    display.navigateUp();

    assertEquals(display.buttons.length - 1, display.selectedIndex);
  }

  @Test
  void repeatedNavigateDownShouldCycleThroughAllButtonsAndReturnToStart() {
    int buttonCount = display.buttons.length;
    for (int i = 0; i < buttonCount; i++) {
      display.navigateDown();
    }

    assertEquals(0, display.selectedIndex);
  }
}
