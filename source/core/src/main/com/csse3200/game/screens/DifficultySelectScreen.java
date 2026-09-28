package com.csse3200.game.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.csse3200.game.GdxGame;
import com.csse3200.game.components.difficulty.DifficultySelectActions;
import com.csse3200.game.components.difficulty.DifficultySelectDisplay;
import com.csse3200.game.components.difficulty.DifficultySelectInputComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.entities.factories.RenderFactory;
import com.csse3200.game.input.InputDecorator;
import com.csse3200.game.input.InputService;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.rendering.Renderer;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Screen shown after pressing Start, where the player picks Easy, Normal or Hard. */
public class DifficultySelectScreen extends ScreenAdapter {
    private static final Logger logger = LoggerFactory.getLogger(DifficultySelectScreen.class);
    private final GdxGame game;
    private final Renderer renderer;

    public DifficultySelectScreen(GdxGame game) {
        this.game = game;

        logger.debug("Initialising difficulty select screen services");
        ServiceLocator.registerInputService(new InputService());
        ServiceLocator.registerResourceService(new ResourceService());
        ServiceLocator.registerEntityService(new EntityService());
        ServiceLocator.registerRenderService(new RenderService());

        renderer = RenderFactory.createRenderer();

        createUI();
    }

    @Override
    public void show() {
        // Same light-yellow background as the main menu
        Gdx.gl.glClearColor(248f / 255f, 249f / 255f, 178f / 255f, 1f);
    }

    @Override
    public void render(float delta) {
        ServiceLocator.getEntityService().update();
        renderer.render();
    }

    @Override
    public void resize(int width, int height) {
        renderer.resize(width, height);
    }

    @Override
    public void dispose() {
        logger.debug("Disposing difficulty select screen");
        renderer.dispose();
        ServiceLocator.getRenderService().dispose();
        ServiceLocator.getEntityService().dispose();
        ServiceLocator.clear();
    }


    private void createUI() {
        Stage stage = ServiceLocator.getRenderService().getStage();
        Entity ui = new Entity();
        ui.addComponent(new DifficultySelectDisplay())
                .addComponent(new InputDecorator(stage, 10))
                .addComponent(new DifficultySelectInputComponent())
                .addComponent(new DifficultySelectActions(game));
        ServiceLocator.getEntityService().register(ui);
    }
}
