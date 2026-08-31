package com.csse3200.game.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.GL20;
import com.csse3200.game.GdxGame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeathScreen extends ScreenAdapter {
  private static final Logger logger = LoggerFactory.getLogger(DeathScreen.class);

  private final GdxGame game;

  public DeathScreen(GdxGame game) {
    this.game = game;
  }

  @Override
  public void show() {
    logger.info("Showing death screen");
  }

  /**
   * @param delta The time in seconds since the last render.
   */
  @Override
  public void render(float delta) {
    Gdx.gl.glClearColor(0, 0, 0, 1);
    Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
  }

  @Override
  public void dispose() {
    logger.info("Disposing death screen");
  }
}
