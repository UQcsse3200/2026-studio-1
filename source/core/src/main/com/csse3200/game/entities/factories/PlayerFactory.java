package com.csse3200.game.entities.factories;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.csse3200.game.areas.terrain.map.LevelMapData;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.PlatformerComponent;
import com.csse3200.game.components.effects.HealthEffectComponent;
import com.csse3200.game.components.effects.SpeedEffectComponent;
import com.csse3200.game.components.loot.ConsumableGenerator;
import com.csse3200.game.components.loot.ConsumableType;
import com.csse3200.game.components.loot.Item;
import com.csse3200.game.components.loot.ItemType;
import com.csse3200.game.components.loot.WeaponGenerator;
import com.csse3200.game.components.loot.WeaponItem;
import com.csse3200.game.components.loot.WeaponType;
import com.csse3200.game.components.pet.PetManagerComponent;
import com.csse3200.game.components.player.BallisticShieldComponent;
import com.csse3200.game.components.player.BallisticShieldRenderComponent;
import com.csse3200.game.components.player.ConsumableUseComponent;
import com.csse3200.game.components.player.DeathLootDropComponent;
import com.csse3200.game.components.player.DeathStateComponent;
import com.csse3200.game.components.player.GamblingCatalogs;
import com.csse3200.game.components.player.InventoryComponent;
import com.csse3200.game.components.player.InventoryDisplay;
import com.csse3200.game.components.player.ItemDropComponent;
import com.csse3200.game.components.player.LadderComponent;
import com.csse3200.game.components.player.PlayerActions;
import com.csse3200.game.components.player.PlayerAnimationController;
import com.csse3200.game.components.player.PlayerBuffComponent;
import com.csse3200.game.components.player.PlayerRegenComponent;
import com.csse3200.game.components.player.PlayerStatsDisplay;
import com.csse3200.game.components.player.QuestDisplay;
import com.csse3200.game.components.player.ShieldComponent;
import com.csse3200.game.components.player.ShieldRenderComponent;
import com.csse3200.game.components.player.ShopComponent;
import com.csse3200.game.components.player.ShopDisplay;
import com.csse3200.game.components.player.SpecialAttackCooldownDisplay;
import com.csse3200.game.components.player.SpecialAttackEffectComponent;
import com.csse3200.game.components.player.StaminaComponent;
import com.csse3200.game.components.player.SubLevelTravelComponent;
import com.csse3200.game.components.player.Tutorial;
import com.csse3200.game.components.player.UpgradeActivationFlashComponent;
import com.csse3200.game.components.player.WeaponAttackComponent;
import com.csse3200.game.components.player.WeaponDisplay;
import com.csse3200.game.components.player.WeaponRenderComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.configs.PlayerConfig;
import com.csse3200.game.files.FileLoader;
import com.csse3200.game.input.InputComponent;
import com.csse3200.game.perks.TimeFreezeComponent;
import com.csse3200.game.physics.PhysicsLayer;
import com.csse3200.game.physics.PhysicsUtils;
import com.csse3200.game.physics.components.ColliderComponent;
import com.csse3200.game.physics.components.HitboxComponent;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.rendering.PlayerRenderComponent;
import com.csse3200.game.services.ServiceLocator;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Factory to create a player entity.
 *
 * <p>Predefined player properties are loaded from a config stored as a json file and should have
 * the properties stored in 'PlayerConfig'.
 */
public class PlayerFactory {
  static Texture size = new Texture("images/knight_default.png");
  static TextureAtlas playerAtlas = new TextureAtlas("images/Knight.atlas");
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
    ConsumableGenerator consumableGenerator = new ConsumableGenerator();
    WeaponItem startingWeapon = weaponGenerator.generateWeapon(WeaponType.SWORD, 1);
    WeaponItem startingBow = weaponGenerator.generateWeapon(WeaponType.BOW, 1);
    WeaponItem startingDagger = weaponGenerator.generateWeapon(WeaponType.DAGGER, 1);
    startingDagger.setQuantity(20);

    InventoryComponent inventory = new InventoryComponent(stats.gold);
    inventory.addItem(startingWeapon);
    inventory.addItem(startingBow);
    inventory.addItem(startingDagger);

    ShopComponent shop =
        installDefaultCatalog(new ShopComponent(), weaponGenerator, consumableGenerator);

    Entity player =
        new Entity()
            .addComponent(new PhysicsComponent())
            .addComponent(new ColliderComponent())
            .addComponent(new HitboxComponent().setLayer(PhysicsLayer.PLAYER))
            .addComponent(new PlayerActions())
            .addComponent(new StaminaComponent())
            .addComponent(new CombatStatsComponent(stats.health, stats.baseAttack))

            // Death state and death loot
            .addComponent(new DeathStateComponent())
            .addComponent(new DeathLootDropComponent())

            // Player features
            .addComponent(new ConsumableUseComponent(stats.health))
            .addComponent(new ShieldComponent())
            .addComponent(new ShieldRenderComponent())
            .addComponent(new UpgradeActivationFlashComponent())
            .addComponent(new BallisticShieldComponent())
            .addComponent(new BallisticShieldRenderComponent())
            .addComponent(new PlayerBuffComponent())
            .addComponent(new PlayerRegenComponent())
            .addComponent(new TimeFreezeComponent())
            .addComponent(inventory)
            .addComponent(new ItemDropComponent())
            .addComponent(inputComponent)
            .addComponent(new PetManagerComponent())
            .addComponent(new PlatformerComponent(2, true, 1, false, 1))
            .addComponent(new PlayerStatsDisplay())
            .addComponent(new SpecialAttackCooldownDisplay())
            .addComponent(new QuestDisplay())
            .addComponent(new Tutorial())
            .addComponent(new InventoryDisplay())
            .addComponent(new WeaponDisplay(startingWeapon))
            .addComponent(new WeaponAttackComponent(startingWeapon))
            .addComponent(new SpecialAttackEffectComponent())
            .addComponent(new WeaponRenderComponent())
            .addComponent(shop)
            .addComponent(new ShopDisplay())
            .addComponent(new WeaponRenderComponent())
            .addComponent(new SpeedEffectComponent())
            .addComponent(new HealthEffectComponent());

    PlayerRenderComponent animator = new PlayerRenderComponent(playerAtlas);

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

    animator.addAnimation("climb", 0.15f, Animation.PlayMode.LOOP);
    animator.addAnimation("hurt", 0.1f, Animation.PlayMode.NORMAL);
    animator.addAnimation("death", 0.1f, Animation.PlayMode.NORMAL);
    animator.addAnimation("health", 0.1f, Animation.PlayMode.NORMAL);
    animator.addAnimation("lefthurt", 0.1f, Animation.PlayMode.NORMAL);
    animator.addAnimation("leftdeath", 0.1f, Animation.PlayMode.NORMAL);
    animator.addAnimation("lefthealth", 0.1f, Animation.PlayMode.NORMAL);

    player.addComponent(animator).addComponent(new PlayerAnimationController());
    player.setScale(0.75f, (float) size.getHeight() / size.getWidth());
    animator.startAnimation("Idle");

    if (mapData != null) {
      player.addComponent(new LadderComponent(mapData));
      player.addComponent(new SubLevelTravelComponent(mapData));
    }

    // The map uses 0.5 world units per tile. Keep the player just over one tile wide
    // and under two tiles tall so doorway and ladder clearances match the authored layout.
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

  /**
   * Fills an empty shop with the default item, upgrade, pet, and gambling products.
   *
   * <p>Item listings and item prizes use {@code weaponGenerator} and {@code consumableGenerator}.
   * {@link #createPlayer()} passes the same weapon generator that built the starting weapons. A
   * listing buy price is the smallest cost whose 60 percent refund equals that item's sell price.
   *
   * @param shop shop to fill
   * @param weaponGenerator generator for weapon prizes
   * @param consumableGenerator generator for consumable prizes
   * @return the same shop
   */
  public static ShopComponent installDefaultCatalog(
      ShopComponent shop,
      WeaponGenerator weaponGenerator,
      ConsumableGenerator consumableGenerator) {
    Item potion = consumableGenerator.generateConsumable(ConsumableType.HEALTH_POTION, 1);
    shop.setItemListing(
        1, new ShopComponent.ShopListing<>(potion, buyPriceFor(potion.getSellPrice())));

    Item sword = weaponGenerator.generateWeapon(WeaponType.SWORD, 1);
    shop.setItemListing(
        2, new ShopComponent.ShopListing<>(sword, buyPriceFor(sword.getSellPrice())));
    shop.setUpgradeListing(
        1, new ShopComponent.ShopListing<>(new ShopComponent.Upgrade("Health Upgrade"), 15));
    shop.setPetListing(1, new ShopComponent.ShopListing<>(new ShopComponent.Pet("Bird"), 20));
    shop.setPetListing(2, new ShopComponent.ShopListing<>(new ShopComponent.Pet("Bat"), 30));
    shop.setPetListing(3, new ShopComponent.ShopListing<>(new ShopComponent.Pet("Spirit"), 40));

    shop.setGamblingCatalogs(
        new GamblingCatalogs(
            standardSpinCatalog(consumableGenerator, weaponGenerator),
            premiumSpinCatalog(consumableGenerator, weaponGenerator)));
    return shop;
  }

  /**
   * Builds the Standard ticket. Weights sum to 100, so each weight reads as a percentage.
   *
   * @param consumableGenerator generator for consumable prizes
   * @param weaponGenerator generator for weapon prizes
   * @return Standard catalog at spin price 20
   */
  private static GamblingCatalogs.SpinCatalog standardSpinCatalog(
      ConsumableGenerator consumableGenerator, WeaponGenerator weaponGenerator) {
    Map<Integer, GamblingCatalogs.PrizeEntry<GamblingCatalogs.Prize>> prizes = new HashMap<>();
    prizes.put(1, prize(consumablePrize(consumableGenerator, ConsumableType.HEALTH_POTION, 1), 40));
    prizes.put(2, prize(consumablePrize(consumableGenerator, ConsumableType.SPEED_BUFF, 1), 25));
    prizes.put(3, prize(new GamblingCatalogs.GoldPrize(15), 20));
    prizes.put(4, prize(weaponPrize(weaponGenerator, WeaponType.SWORD, 1), 10));
    prizes.put(5, prize(new ShopComponent.Pet("Bird"), 5));
    return new GamblingCatalogs.SpinCatalog(20, prizes);
  }

  /**
   * Builds the Premium ticket. Weights sum to 100, so each weight reads as a percentage.
   *
   * @param consumableGenerator generator for consumable prizes
   * @param weaponGenerator generator for weapon prizes
   * @return Premium catalog at spin price 60
   */
  private static GamblingCatalogs.SpinCatalog premiumSpinCatalog(
      ConsumableGenerator consumableGenerator, WeaponGenerator weaponGenerator) {
    Map<Integer, GamblingCatalogs.PrizeEntry<GamblingCatalogs.Prize>> prizes = new HashMap<>();
    prizes.put(1, prize(consumablePrize(consumableGenerator, ConsumableType.REGENERATION, 2), 35));
    prizes.put(2, prize(new GamblingCatalogs.GoldPrize(30), 15));
    prizes.put(3, prize(weaponPrize(weaponGenerator, WeaponType.BOW, 3), 20));
    prizes.put(4, prize(new ShopComponent.Upgrade("Regen on Kill"), 25));
    prizes.put(5, prize(new ShopComponent.Pet("Spirit"), 5));
    return new GamblingCatalogs.SpinCatalog(60, prizes);
  }

  private static GamblingCatalogs.ItemPrize consumablePrize(
      ConsumableGenerator consumableGenerator, ConsumableType type, int tier) {
    Supplier<Item> factory = () -> consumableGenerator.generateConsumable(type, tier);
    return new GamblingCatalogs.ItemPrize(factory.get().getName(), ItemType.CONSUMABLE, factory);
  }

  private static GamblingCatalogs.ItemPrize weaponPrize(
      WeaponGenerator weaponGenerator, WeaponType type, int tier) {
    Supplier<Item> factory = () -> weaponGenerator.generateWeapon(type, tier);
    return new GamblingCatalogs.ItemPrize(factory.get().getName(), ItemType.WEAPON, factory);
  }

  /**
   * Returns the smallest buy price whose 60 percent integer refund equals {@code sellPrice}.
   *
   * @param sellPrice generator sell price
   * @return listing buy price
   */
  private static int buyPriceFor(int sellPrice) {
    return (sellPrice * 5 + 2) / 3;
  }

  private static GamblingCatalogs.PrizeEntry<GamblingCatalogs.Prize> prize(
      GamblingCatalogs.Prize product, int weight) {
    return new GamblingCatalogs.PrizeEntry<>(product, weight);
  }

  private PlayerFactory() {
    throw new IllegalStateException("Instantiating static util class");
  }
}
