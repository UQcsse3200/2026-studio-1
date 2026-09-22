package com.csse3200.game.entities.factories;

import com.csse3200.game.areas.terrain.map.LevelMapData;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.PlatformerComponent;
import com.csse3200.game.components.loot.WeaponGenerator;
import com.csse3200.game.components.loot.WeaponItem;
import com.csse3200.game.components.loot.WeaponType;
import com.csse3200.game.components.pet.PetManagerComponent;
import com.csse3200.game.components.player.ConsumableUseComponent;
import com.csse3200.game.components.player.DeathStateComponent;
import com.csse3200.game.components.player.InventoryComponent;
import com.csse3200.game.components.player.InventoryDisplay;
import com.csse3200.game.components.player.ItemDropComponent;
import com.csse3200.game.components.player.LadderComponent;
import com.csse3200.game.components.player.PlayerActions;
import com.csse3200.game.components.player.PlayerBuffComponent;
import com.csse3200.game.components.player.PlayerRegenComponent;
import com.csse3200.game.components.player.PlayerStatsDisplay;
import com.csse3200.game.components.player.QuestDisplay;
import com.csse3200.game.components.player.ShieldComponent;
import com.csse3200.game.components.player.ShieldRenderComponent;
import com.csse3200.game.components.player.ShopComponent;
import com.csse3200.game.components.player.ShopDisplay;
import com.csse3200.game.components.player.StaminaComponent;
import com.csse3200.game.components.player.SubLevelTravelComponent;
import com.csse3200.game.components.player.WeaponAttackComponent;
import com.csse3200.game.components.player.WeaponDisplay;
import com.csse3200.game.components.player.WeaponRenderComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.configs.PlayerConfig;
import com.csse3200.game.files.FileLoader;
import com.csse3200.game.input.InputComponent;
import com.csse3200.game.physics.PhysicsLayer;
import com.csse3200.game.physics.PhysicsUtils;
import com.csse3200.game.physics.components.ColliderComponent;
import com.csse3200.game.physics.components.HitboxComponent;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.rendering.TextureRenderComponent;
import com.csse3200.game.services.ServiceLocator;

/**
 * Factory to create a player entity.
 *
 * <p>Predefined player properties are loaded from a config stored as a json file and should have
 * the properties stores in 'PlayerConfig'.
 */
public class PlayerFactory {
  private static final PlayerConfig stats =
      FileLoader.readClass(PlayerConfig.class, "configs/player.json");

  /**
   * Create a player entity.
   *
   * @return entity
   */
  public static Entity createPlayer() {
    return createPlayer(null);
  }

  /** Create a player, optionally enabling ladder traversal for a tile map. */
  public static Entity createPlayer(LevelMapData mapData) {
    InputComponent inputComponent =
        ServiceLocator.getInputService().getInputFactory().createForPlayer();

    WeaponGenerator weaponGenerator = new WeaponGenerator();
    WeaponItem startingWeapon = weaponGenerator.generateWeapon(WeaponType.SWORD, 1);
    WeaponItem startingBow = weaponGenerator.generateWeapon(WeaponType.BOW, 1);
    WeaponItem startingDagger = weaponGenerator.generateWeapon(WeaponType.DAGGER, 1);
    startingDagger.setQuantity(20);

    InventoryComponent inventory = new InventoryComponent(stats.gold);
    inventory.addItem(startingWeapon);
    inventory.addItem(startingBow);
    inventory.addItem(startingDagger);

    Entity player =
        new Entity()
            .addComponent(new TextureRenderComponent("images/player/box_boy_leaf.png"))
            .addComponent(new PhysicsComponent())
            .addComponent(new ColliderComponent())
            .addComponent(new HitboxComponent().setLayer(PhysicsLayer.PLAYER))
            .addComponent(new PlayerActions())
                .addComponent(new StaminaComponent())
            .addComponent(new CombatStatsComponent(stats.health, stats.baseAttack))

            // Death State
            .addComponent(new DeathStateComponent())

            // Existing main/team features
            .addComponent(new ConsumableUseComponent(stats.health))
            .addComponent(new ShieldComponent())
            .addComponent(new ShieldRenderComponent())
            .addComponent(new PlayerBuffComponent())
            .addComponent(new PlayerRegenComponent())
            .addComponent(inventory)
            .addComponent(new ItemDropComponent())
            .addComponent(inputComponent)
            .addComponent(new PetManagerComponent())
            .addComponent(new PlatformerComponent(5, true, 1, false, 1))
            .addComponent(new PlayerStatsDisplay())
            .addComponent(new QuestDisplay())
            .addComponent(new InventoryDisplay())
            .addComponent(new WeaponDisplay(startingWeapon))
            .addComponent(new WeaponAttackComponent(startingWeapon))
            .addComponent(new WeaponRenderComponent())
            .addComponent(new ShopComponent().seedDefaultCatalog())
            .addComponent(new ShopDisplay());

    if (mapData != null) {
      player.addComponent(new LadderComponent(mapData));
      player.addComponent(new SubLevelTravelComponent());
    }

    // The map uses 0.5 world units per tile. Keep the player just over one tile wide and under
    // two tiles tall so doorway and ladder clearances match the authored layout.
    player.getComponent(TextureRenderComponent.class).scaleEntity();
    // The box-boy sprite has a much denser silhouette than the skeleton atlas, so use a slightly
    // smaller rendered body to make both characters occupy the same visual footprint.
    player.setScale(0.75f, 0.75f);
    PhysicsUtils.setScaledCollider(player, 0.5f, 0.28f);
    player
        .getComponent(HitboxComponent.class)
        .setAsBoxAligned(
            new com.badlogic.gdx.math.Vector2(0.5f, 0.8f),
            PhysicsComponent.AlignX.CENTER,
            PhysicsComponent.AlignY.BOTTOM);
    player.getComponent(ColliderComponent.class).setDensity(1.5f);

    return player;
  }

  private PlayerFactory() {
    throw new IllegalStateException("Instantiating static util class");
  }
}
