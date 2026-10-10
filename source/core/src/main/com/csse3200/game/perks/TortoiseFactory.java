package com.csse3200.game.perks;

import com.badlogic.gdx.graphics.g2d.Animation.PlayMode;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.physics.PhysicsLayer;
import com.csse3200.game.physics.components.ColliderComponent;
import com.csse3200.game.physics.components.HitboxComponent;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.rendering.AnimationRenderComponent;
import com.csse3200.game.services.ServiceLocator;
import java.util.Collection;

/**
 * Builds the hidden tortoise collectible entity for the "Time Freeze" perk.
 *
 * <p>Physics/collider setup mirrors {@code LootFactory}: a solid fixture so it can sit on terrain
 * without falling through, and an ITEM-layer hitbox so {@link TortoiseComponent} can detect the
 * player touching it. Unlike loot, this uses a static body - a hidden tortoise should never move
 * once placed, not fall or get pushed around.
 *
 * <p>Visuals come from {@code images/tortoise.atlas} (backed by a 144x32 spritesheet), a hand-built
 * atlas over a 144x32 spritesheet (18x16 px cells): an 8-frame "walk" cycle (row 0, unused for now
 * - a hidden tortoise never moves) and a 6-frame "idle" loop (row 1, columns 2-7), which is what
 * actually plays here.
 */
public final class TortoiseFactory {
  private static final String ATLAS_PATH = "images/tortoise.atlas";
  private static final float IDLE_FRAME_DURATION = 0.1f;

  /**
   * Creates one specific hidden tortoise, ready to be positioned and spawned (e.g. via {@code
   * GameArea.spawnEntityAt}).
   *
   * @param tortoiseId stable id for this tortoise (e.g. {@code "level1_a"}) - every tortoise placed
   *     in the game needs its own distinct id, so each is tracked and counted separately
   * @return the new tortoise entity
   */
  public static Entity createTortoise(String tortoiseId) {
    TextureAtlas atlas =
        ServiceLocator.getResourceService().getAsset(ATLAS_PATH, TextureAtlas.class);
    AnimationRenderComponent animator = new AnimationRenderComponent(atlas);
    animator.addAnimation("idle", IDLE_FRAME_DURATION, PlayMode.LOOP);

    Entity tortoise = new Entity();
    tortoise
        .addComponent(animator)
        .addComponent(new PhysicsComponent().setBodyType(BodyType.StaticBody))
        .addComponent(
            new ColliderComponent().setLayer(PhysicsLayer.ITEM).setMask(PhysicsLayer.OBSTACLE))
        .addComponent(new HitboxComponent().setLayer(PhysicsLayer.ITEM))
        .addComponent(new TortoiseComponent(tortoiseId));

    animator.startAnimation("idle");
    return tortoise;
  }

  /**
   * @param tortoiseId stable id for a specific tortoise, as passed to {@link
   *     #createTortoise(String)}
   * @return {@code true} if that tortoise has already been found - callers (e.g. {@code
   *     LevelGameArea}) should skip spawning it at all once this is true
   */
  public static boolean isFound(String tortoiseId) {
    return TortoiseRegistry.isFound(tortoiseId);
  }

  /**
   * Clears every tortoise's found state, so all of them can be found again. Call this on a fresh
   * game start (not a loaded save, not a death/revive) - see {@code MainGameScreen}'s constructor.
   */
  public static void resetAll() {
    TortoiseRegistry.resetAll();
  }

  /** Save/load: replaces the found tortoises with the ones listed in a save file. */
  public static void restoreFound(Collection<String> tortoiseIds) {
    TortoiseRegistry.resetAll();
    if (tortoiseIds == null) {
      return;
    }
    for (String id : tortoiseIds) {
      if (id != null && !id.isBlank()) {
        TortoiseRegistry.markFound(id);
      }
    }
  }

  private TortoiseFactory() {
    throw new IllegalStateException("Instantiating static util class");
  }
}
