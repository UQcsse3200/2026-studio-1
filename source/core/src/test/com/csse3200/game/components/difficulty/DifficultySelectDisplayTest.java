package com.csse3200.game.components.difficulty;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Regression coverage for each option row being fully clickable: Table defaults to
 * Touchable.childrenOnly, which would only let the label's own pixels respond to hover/clicks,
 * ignoring the rest of the row's "button" frame.
 */
@ExtendWith(GameExtension.class)
class DifficultySelectDisplayTest {
  private DifficultySelectDisplay display;

  @BeforeEach
  void beforeEach() {
    RenderService renderService = new RenderService();
    renderService.setStage(mock(Stage.class));
    ServiceLocator.registerRenderService(renderService);
    ServiceLocator.registerResourceService(mock(ResourceService.class));

    display = new DifficultySelectDisplay();
    Entity entity = new Entity().addComponent(display);
    entity.create();
  }

  @Test
  void everyOptionRowIsFullyTouchable() {
    for (int i = 0; i < display.buttons.length; i++) {
      Table row = (Table) display.buttons[i].getParent();
      assertEquals(
          Touchable.enabled,
          row.getTouchable(),
          "Expected the \""
              + DifficultySelectDisplay.MENU_ITEMS[i]
              + "\" row to be fully clickable (Touchable.enabled), not just its label text.");
    }
  }
}
