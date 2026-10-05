package com.csse3200.game.entities.factories;

import com.badlogic.gdx.graphics.g2d.Animation.PlayMode;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.boss.BossHealthBarDisplay;
import com.csse3200.game.components.boss.ZeusBossComponent;
import com.csse3200.game.components.boss.ZeusEffectsRenderComponent;
import com.csse3200.game.difficulty.Difficulty;
import com.csse3200.game.difficulty.DifficultyService;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.configs.NPCConfigs;
import com.csse3200.game.entities.configs.enemies.ZeusConfig;
import com.csse3200.game.files.FileLoader;
import com.csse3200.game.physics.PhysicsLayer;
import com.csse3200.game.physics.components.HitboxComponent;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.rendering.AnimationRenderComponent;
import com.csse3200.game.services.ServiceLocator;

/** Factory for Zeus, the boss of the level 3 throne room. */
public class ZeusFactory {
  private static final String ATLAS = "images/enemies/zeus.atlas";

  /** Zeus is drawn three tiles wide and four tall. */
  private static final Vector2 SIZE = new Vector2(1.5f, 2f);

  /** The part of the sprite the player can hit: his body, without the reach of his arms. */
  private static final Vector2 BODY = new Vector2(0.9f, 1.85f);

  private static final String[] SIDES = {"right", "left"};

  /**
   * Creates Zeus. He is hit like any other enemy, through a hitbox on the NPC layer, but has no
   * solid collider and no gravity: he floats, and {@link ZeusBossComponent} moves him.
   *
   * @param target the player he fights
   * @return the boss entity
   */
  public static Entity createZeus(Entity target) {
    ZeusConfig config = FileLoader.readClass(NPCConfigs.class, "configs/NPCs.json").zeus;
    // Zeus attacks through his own component rather than the shared attack components, so the
    // difficulty the spawn registry applies to those is applied to him here instead.
    Difficulty difficulty = DifficultyService.getCurrent();
    int health = Math.max(1, Math.round(config.health * difficulty.getEnemyHealthMultiplier()));

    AnimationRenderComponent animator =
        new AnimationRenderComponent(
            ServiceLocator.getResourceService().getAsset(ATLAS, TextureAtlas.class));
    for (String side : SIDES) {
      animator.addAnimation("zeus-idle-" + side, 1f / 6f, PlayMode.LOOP);
      animator.addAnimation("zeus-float-" + side, 1f / 8f, PlayMode.LOOP);
      animator.addAnimation("zeus-melee-" + side, 1f / 12f, PlayMode.NORMAL);
      animator.addAnimation("zeus-cast-" + side, 0.15f, PlayMode.NORMAL);
      animator.addAnimation("zeus-hurt-" + side, 1f / 8f, PlayMode.LOOP);
      animator.addAnimation("zeus-enraged-idle-" + side, 1f / 8f, PlayMode.LOOP);
      animator.addAnimation("zeus-death-" + side, 1f / 8f, PlayMode.NORMAL);
    }

    PhysicsComponent physics = new PhysicsComponent();
    physics.getBody().setGravityScale(0f);

    Entity zeus =
        new Entity()
            .addComponent(physics)
            .addComponent(new HitboxComponent().setLayer(PhysicsLayer.NPC))
            .addComponent(new CombatStatsComponent(health, config.baseAttack))
            .addComponent(animator)
            .addComponent(
                new ZeusBossComponent(
                    target,
                    config,
                    difficulty.getEnemyDamageMultiplier(),
                    difficulty.getAttackCooldownMultiplier()))
            .addComponent(new ZeusEffectsRenderComponent())
            .addComponent(new BossHealthBarDisplay("ZEUS"));

    zeus.setScale(SIZE);
    zeus.getComponent(HitboxComponent.class)
        .setAsBoxAligned(BODY, PhysicsComponent.AlignX.CENTER, PhysicsComponent.AlignY.BOTTOM);
    return zeus;
  }

  private ZeusFactory() {
    throw new IllegalStateException("Instantiating static util class");
  }
}
