package com.csse3200.game.perks.tortoises;

import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.csse3200.game.physics.PhysicsService;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import java.lang.reflect.Field;

/** Shared set-up for the tortoise tests. Not a test class. */
final class TortoiseTestSupport {
  static final String ATLAS_PATH = "images/tortoise.atlas";

  /**
   * TortoiseRegistry keeps its Preferences in a static field that is filled when the class first
   * loads. If another test loaded it before libGDX was running, the field stays null for the whole
   * test run, so tests that need real found-state skip instead of failing.
   */
  static void assumePreferencesAvailable() {
    try {
      Field field = TortoiseRegistry.class.getDeclaredField("prefs");
      field.setAccessible(true);
      assumeTrue(
          field.get(null) != null,
          "TortoiseRegistry loaded before libGDX, so it has no Preferences");
    } catch (ReflectiveOperationException e) {
      throw new IllegalStateException(e);
    }
  }

  /** Registers what TortoiseFactory.createTortoise needs: the atlas and a physics service. */
  static void registerFactoryServices() {
    TextureAtlas atlas = new TextureAtlas(Gdx.files.internal(ATLAS_PATH));
    ResourceService resourceService = mock(ResourceService.class);
    when(resourceService.getAsset(ATLAS_PATH, TextureAtlas.class)).thenReturn(atlas);
    ServiceLocator.registerResourceService(resourceService);
    ServiceLocator.registerPhysicsService(new PhysicsService());
  }

  /** Runs the work with Gdx.app swapped for the given application, then puts the old one back. */
  static void withApp(Application app, Runnable work) {
    Application saved = Gdx.app;
    Gdx.app = app;
    try {
      work.run();
    } finally {
      Gdx.app = saved;
    }
  }

  /** Reads a private field, for state that has no getter. */
  static Object readField(Object target, String name) {
    try {
      Field field = target.getClass().getDeclaredField(name);
      field.setAccessible(true);
      return field.get(target);
    } catch (ReflectiveOperationException e) {
      throw new IllegalStateException(e);
    }
  }

  private TortoiseTestSupport() {
    throw new IllegalStateException("Utility class");
  }
}
