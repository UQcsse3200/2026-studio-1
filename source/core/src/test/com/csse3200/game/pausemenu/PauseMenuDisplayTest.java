package com.csse3200.game.pausemenu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

import com.badlogic.gdx.scenes.scene2d.Stage;
import com.csse3200.game.difficulty.Difficulty;
import com.csse3200.game.difficulty.DifficultyService;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Regression coverage for the pause menu's difficulty label: a non-selectable label on the main
 * pause page reading "Difficulty: Easy/Normal/Hard", refreshed every time the pause menu opens.
 * Also confirms adding it didn't change the main page's navigable items.
 */
@ExtendWith(GameExtension.class)
class PauseMenuDisplayTest {
  private PauseMenuDisplay display;
  private PauseMenuComponent pauseMenuComponent;

  @BeforeEach
  void beforeEach() {
    DifficultyService.setCurrent(Difficulty.NORMAL);

    RenderService renderService = new RenderService();
    renderService.setStage(mock(Stage.class));
    ServiceLocator.registerRenderService(renderService);
    ServiceLocator.registerResourceService(mock(ResourceService.class));
    ServiceLocator.registerTimeSource(mock(GameTime.class));

    pauseMenuComponent = new PauseMenuComponent();
    display = new PauseMenuDisplay();
    Entity entity = new Entity().addComponent(pauseMenuComponent).addComponent(display);
    entity.create();
  }

  @AfterEach
  void afterEach() {
    DifficultyService.setCurrent(Difficulty.NORMAL);
  }

  /** Flips the pause menu open and runs one draw() so the "menu just opened" refresh fires. */
  private void openPauseMenu() {
    pauseMenuComponent.toggleIsPaused();
    display.draw(null);
  }

  @Test
  void difficultyLabelReadsNormalByDefault() {
    openPauseMenu();

    assertEquals("Difficulty: Normal", display.difficultyLabel.getText().toString());
  }

  @Test
  void difficultyLabelReadsHardWhenDifficultyServiceIsHard() {
    DifficultyService.setCurrent(Difficulty.HARD);

    openPauseMenu();

    assertEquals("Difficulty: Hard", display.difficultyLabel.getText().toString());
  }

  @Test
  void mainPageNavigableItemsAreUnchanged() {
    openPauseMenu();

    // The difficulty label was added to the main panel, but must not be a navigable item: 5
    // items ("Resume", "Restart", "Settings", "Main Menu", "Save") still cycle exactly as before.
    assertEquals(0, display.mainIndex);
    for (int i = 0; i < 5; i++) {
      display.navigateDown();
    }
    assertEquals(0, display.mainIndex, "Expected 5 navigable items to cycle back to the start.");
  }
}
