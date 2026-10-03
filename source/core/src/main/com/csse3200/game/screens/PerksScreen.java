package com.csse3200.game.screens;

import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.csse3200.game.GdxGame;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.entities.factories.RenderFactory;
import com.csse3200.game.input.InputDecorator;
import com.csse3200.game.input.InputService;
import com.csse3200.game.perks.PerkDefinitions;
import com.csse3200.game.perks.PerkSelectionDisplay;
import com.csse3200.game.perks.PerkSelectionInputComponent;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.rendering.Renderer;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The game screen hosting the perk-selection UI as a standalone screen, reached from the Main
 * Menu's "Perks" button.
 *
 * <p>Reuses {@link PerkSelectionDisplay} completely unchanged - same grid-of-cards layout,
 * locked/unlocked/active colours, click and keyboard nav - it's the exact same component shown as
 * an overlay after death in {@code MainGameScreen}. There, something else triggers {@code .show()}
 * later; here, this screen's only content IS that display, so {@code .show()} is called immediately
 * after creating it. "Continue" (or Escape, once added) returns to the Main Menu.
 */
public class PerksScreen extends ScreenAdapter {
  private static final Logger logger = LoggerFactory.getLogger(PerksScreen.class);

  private final GdxGame game;
  private final Renderer renderer;

  public PerksScreen(GdxGame game) {
    this.game = game;

    logger.debug("Initialising perks screen services");
    ServiceLocator.registerInputService(new InputService());
    ServiceLocator.registerResourceService(new ResourceService());
    ServiceLocator.registerEntityService(new EntityService());
    ServiceLocator.registerRenderService(new RenderService());
    PerkDefinitions.registerAll();

    renderer = RenderFactory.createRenderer();

    createUI();
  }

  @Override
  public void render(float delta) {
    ServiceLocator.getEntityService().update();
    renderer.render();
  }

  @Override
  public void resize(int width, int height) {
    renderer.resize(width, height);
    logger.trace("Resized renderer: ({} x {})", width, height);
  }

  @Override
  public void dispose() {
    logger.debug("Disposing perks screen");

    renderer.dispose();
    ServiceLocator.getRenderService().dispose();
    ServiceLocator.getEntityService().dispose();

    ServiceLocator.clear();
  }

  /**
   * Creates the perks screen's ui: the perk-selection display and its input handling, shown
   * immediately since this screen has nothing else on it.
   */
  private void createUI() {
    logger.debug("Creating ui");
    Stage stage = ServiceLocator.getRenderService().getStage();

    PerkSelectionDisplay perkSelectionDisplay =
        new PerkSelectionDisplay(() -> game.setScreen(GdxGame.ScreenType.MAIN_MENU));

    Entity ui = new Entity();
    ui.addComponent(perkSelectionDisplay)
        .addComponent(new PerkSelectionInputComponent())
        .addComponent(new InputDecorator(stage, 10));

    ServiceLocator.getEntityService().register(ui);
    perkSelectionDisplay.show();
  }
}
