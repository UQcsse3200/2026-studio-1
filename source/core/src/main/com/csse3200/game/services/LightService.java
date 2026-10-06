package com.csse3200.game.services;

import box2dLight.RayHandler;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.Disposable;

public class LightService implements Disposable {
  private final RayHandler rayHandler;
  private final Color currentColor = new Color(1f, 1f, 1f, 1f);
  private float currentIntensity = 1f;
  private final Color fromColor = new Color();
  private final Color toColor = new Color();
  private final Color tmp = new Color();
  private float fromIntensity;
  private float toIntensity;
  private float fadeElapsed;
  private float fadeDuration;
  private boolean fading;

  public LightService(World world) {
    RayHandler.useDiffuseLight(true); // static; must precede construction
    rayHandler = new RayHandler(world);
    rayHandler.setAmbientLight(1f, 1f, 1f, 1f); // full bright by default
  }

  public void setAmbient(Color c, float intensity) {
    fading = false;
    applyAmbient(c, intensity);
  }

  private void applyAmbient(Color c, float intensity) {
    currentColor.set(c);
    currentIntensity = intensity;
    rayHandler.setAmbientLight(c.r, c.g, c.b, intensity);
  }

  public void fadeAmbientTo(Color c, float intensity, float seconds) {
    if (seconds <= 0f) {
      setAmbient(c, intensity);
      return;
    }
    fromColor.set(currentColor);
    fromIntensity = currentIntensity;
    toColor.set(c);
    toIntensity = intensity;
    fadeElapsed = 0f;
    fadeDuration = seconds;
    fading = true;
  }

  public RayHandler getRayHandler() {
    return rayHandler;
  }

  public void render(OrthographicCamera camera) {
    if (fading) {
      fadeElapsed += Gdx.graphics.getDeltaTime();
      float t = Math.min(1f, fadeElapsed / fadeDuration);
      tmp.set(fromColor).lerp(toColor, t);
      applyAmbient(tmp, MathUtils.lerp(fromIntensity, toIntensity, t));
      if (t >= 1f) fading = false;
    }
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
