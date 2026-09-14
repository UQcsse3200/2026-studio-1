package com.csse3200.game.entities.factories;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.PlatformerComponent;
import com.csse3200.game.components.loot.WeaponGenerator;
import com.csse3200.game.components.loot.WeaponItem;
import com.csse3200.game.components.loot.WeaponType;
import com.csse3200.game.components.player.*;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.configs.PlayerConfig;
import com.csse3200.game.files.FileLoader;
import com.csse3200.game.input.InputComponent;
import com.csse3200.game.physics.PhysicsLayer;
import com.csse3200.game.physics.PhysicsUtils;
import com.csse3200.game.physics.components.ColliderComponent;
import com.csse3200.game.physics.components.HitboxComponent;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.rendering.PlayerRenderComponent;
import com.csse3200.game.services.ServiceLocator;

/**
 * Factory to create a player entity.
 *
 * <p>Predefined player properties are loaded from a config stored as a json file and should have
 * the properties stores in 'PlayerConfig'.
 */
public class PlayerFactory {
  static Texture size = new Texture("images/knight_default.png");
  private static final PlayerConfig stats =
      FileLoader.readClass(PlayerConfig.class, "configs/player.json");

  /**
   * Create a player entity.
   *
   * @return entity
   */
  public static Entity createPlayer() {
    InputComponent inputComponent =
        ServiceLocator.getInputService().getInputFactory().createForPlayer();

    WeaponGenerator weaponGenerator = new WeaponGenerator();
    WeaponItem startingWeapon = weaponGenerator.generateWeapon(WeaponType.SWORD, 1);

    Entity player =
        new Entity()
            .addComponent(new PhysicsComponent())
            .addComponent(new ColliderComponent())
            .addComponent(new HitboxComponent().setLayer(PhysicsLayer.PLAYER))
            .addComponent(new PlayerActions())
            .addComponent(new CombatStatsComponent(stats.health, stats.baseAttack))

            // Death State
            .addComponent(new DeathStateComponent())

            // Existing main/team features
            .addComponent(new ConsumableUseComponent(stats.health))
            .addComponent(new InventoryComponent(stats.gold))
            .addComponent(new ItemDropComponent())
            .addComponent(inputComponent)
            .addComponent(new PlatformerComponent(3))
            .addComponent(new PlayerStatsDisplay())
            .addComponent(new InventoryDisplay())
            .addComponent(new WeaponDisplay(startingWeapon))
            .addComponent(new WeaponAttackComponent(startingWeapon))
            .addComponent(new WeaponRenderComponent("images/sword.png"));
    PlayerRenderComponent animator =
        new PlayerRenderComponent(
            ServiceLocator.getResourceService().getAsset("images/knight.atlas", TextureAtlas.class),
            ServiceLocator.getResourceService()
                .getAsset("images/LeftKnight.atlas", TextureAtlas.class));

    animator.addAnimation("Idle", 0.1f, Animation.PlayMode.LOOP);
    animator.addAnimation("Jump", 0.1f, Animation.PlayMode.NORMAL);
    animator.addAnimation("Attacks", 0.1f, Animation.PlayMode.NORMAL);
    animator.addAnimation("crouchidle", 0.1f, Animation.PlayMode.LOOP);
    animator.addAnimation("Roll", 0.1f, Animation.PlayMode.NORMAL);
    animator.addAnimation("Slide", 0.1f, Animation.PlayMode.NORMAL);
    animator.addAnimation("Run", 0.1f, Animation.PlayMode.LOOP);
    animator.addAnimation("LeftIdle", 0.1f, Animation.PlayMode.LOOP);
    animator.addAnimation("LeftJump", 0.1f, Animation.PlayMode.NORMAL);
    animator.addAnimation("LeftAttacks", 0.1f, Animation.PlayMode.NORMAL);
    animator.addAnimation("Leftcrouchidle", 0.1f, Animation.PlayMode.LOOP);
    animator.addAnimation("LeftRoll", 0.1f, Animation.PlayMode.NORMAL);
    animator.addAnimation("LeftSlide", 0.1f, Animation.PlayMode.NORMAL);
    animator.addAnimation("LeftRun", 0.1f, Animation.PlayMode.LOOP);
    player.addComponent(animator).addComponent(new PlayerAnimationController());

    player.setScale(0.75f, (float) size.getHeight() / size.getWidth());
    PhysicsUtils.setScaledCollider(player, 0.6f, 0.3f);
    player.getComponent(ColliderComponent.class).setDensity(1.5f);
    animator.startAnimation("Idle");
    return player;
  }

  private PlayerFactory() {
    throw new IllegalStateException("Instantiating static util class");
  }
}
