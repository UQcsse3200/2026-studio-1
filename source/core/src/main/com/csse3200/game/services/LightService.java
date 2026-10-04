package com.csse3200.game.services;

import box2dLight.RayHandler;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.Disposable;

public class LightService implements Disposable {
  private final RayHandler rayHandler;

  public LightService(World world) {
    RayHandler.useDiffuseLight(true); // static; must precede construction
    rayHandler = new RayHandler(world);
    rayHandler.setAmbientLight(1f, 1f, 1f, 1f); // full bright by default
  }

  public RayHandler getRayHandler() {
    return rayHandler;
  }

  public void setAmbient(Color c, float intensity) {
    rayHandler.setAmbientLight(c.r, c.g, c.b, intensity); // tune visually
  }

  public void render(OrthographicCamera camera) {
    rayHandler.setCombinedMatrix(camera);
    rayHandler.updateAndRender();
  }

  public void resize(int w, int h) {
    rayHandler.resizeFBO(w / 4, h / 4);
  }

  @Override
  public void dispose() {
    rayHandler.dispose();
  }
}
