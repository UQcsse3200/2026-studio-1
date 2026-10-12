package com.csse3200.game.entities.factories;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.ai.tasks.AITaskComponent;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.QuestGiverComponent;
import com.csse3200.game.components.enemies.*;
import com.csse3200.game.components.enemies.animation.*;
import com.csse3200.game.components.enemies.tasks.*;
import com.csse3200.game.components.loot.*;
import com.csse3200.game.components.npc.DialogueComponent;
import com.csse3200.game.components.npc.DialogueProximityComponent;
import com.csse3200.game.components.npc.DisplayDialogue;
import com.csse3200.game.components.npc.HeadbuttAttackComponent;
import com.csse3200.game.components.npc.NameGenerator;
import com.csse3200.game.components.npc.PotionThrowComponent;
import com.csse3200.game.components.npc.ProvokedComponent;
import com.csse3200.game.components.npc.ShopkeeperComponent;
import com.csse3200.game.components.player.InventoryComponent;
import com.csse3200.game.components.player.ItemDropComponent;
import com.csse3200.game.components.tasks.*;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.configs.NPCConfigs;
import com.csse3200.game.entities.spawn.EntitySpawnRegistry;
import com.csse3200.game.files.FileLoader;
import com.csse3200.game.physics.PhysicsLayer;
import com.csse3200.game.physics.PhysicsUtils;
import com.csse3200.game.physics.components.ColliderComponent;
import com.csse3200.game.physics.components.HitboxComponent;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.physics.components.PhysicsMovementComponent;
import com.csse3200.game.rendering.AnimationRenderComponent;
import java.util.ArrayList;
import java.util.List;

/**
 * Factory to create non-playable character (NPC) entities with predefined components.
 *
 * <p>Each NPC entity type should have a creation method that returns a corresponding entity.
 * Predefined entity properties can be loaded from configs stored as JSON files which are defined in
 * "NPCConfigs".
 *
 * <p>If needed, this factory can be separated into more specific factories for entities with
 * similar characteristics.
 */
public class NPCFactory {
  private static final String SHOP_NPC_ATLAS_PATH = "images/npcs/npc_shop.atlas";
  private static final String WIZARD_NPC_ATLAS_PATH = "images/npcs/npc2.atlas";
  private static final String PHILOSOPHER_NPC_ATLAS_PATH = "images/npcs/npc1.atlas";
  private static final String SATYR_NPC_ATLAS_PATH = "images/npcs/npc3.atlas";
  private static final float FRIENDLY_NPC_HEIGHT = 47f / 42f;
  private static final float FRIENDLY_NPC_COLLIDER_WIDTH = 0.9f;
  private static final float FRIENDLY_NPC_WANDER_RANGE = 1.5f;
  private static final int FRIENDLY_NPC_HEALTH = 50;
  private static final int FRIENDLY_NPC_STAND_PRIORITY = 5;

  private static final NPCConfigs configs =
      FileLoader.readClass(NPCConfigs.class, "configs/NPCs.json");
  private static final NameGenerator nameGenerator = new NameGenerator();

  /**
   * Creates a generic NPC to be used as a base entity by more specific NPC creation methods.
   *
   * @return entity
   */
  private static Entity createBaseNPC(Entity target) {
    AITaskComponent aiComponent =
        new AITaskComponent()
            .addTask(new WanderTask(new Vector2(2f, 2f), 2f))
            .addTask(new ChaseTask(target, 10, 3f, 4f));

    Entity npc =
        new Entity()
            .addComponent(new PhysicsComponent())
            .addComponent(new PhysicsMovementComponent())
            .addComponent(new ColliderComponent())
            .addComponent(new HitboxComponent().setLayer(PhysicsLayer.NPC))
            .addComponent(aiComponent);

    PhysicsUtils.setScaledCollider(npc, 0.9f, 0.4f);
    // Let gravity pull the NPC down instead of the wander/chase AI flying it directly toward
    npc.getComponent(PhysicsMovementComponent.class).setGroundedMovement(true);
    return npc;
  }

  /** Registers the friendly NPC spawn names used by map "npc" markers. */
  public static void registerNpcSpawns() {
    EntitySpawnRegistry.register("npc:shop", NPCFactory::createShopNPC);
    EntitySpawnRegistry.register("npc:wizard", NPCFactory::createWizardNPC);
    EntitySpawnRegistry.register("npc:philosopher", NPCFactory::createPhilosopherNPC);
    EntitySpawnRegistry.register("npc:satyr", NPCFactory::createSatyrNPC);
  }

  /**
   * Creates a shopkeeper NPC that opens the shop and cannot be hurt.
   *
   * @return entity
   */
  public static Entity createShopNPC(Entity player) {
    Entity npc = createAnimatedNPC(player, "Hermes", SHOP_NPC_ATLAS_PATH);
    npc.addComponent(new ShopkeeperComponent(player));
    npc.getComponent(DialogueComponent.class).changeQuestType("shieldscollectedquest");
    npc.getComponent(QuestGiverComponent.class)
        .setItemToGive(new WeaponGenerator().generateWeapon(WeaponType.BOW, 2));
    npc.getComponent(DialogueComponent.class).changeAmountXToDo(2);
    return npc;
  }

  /**
   * Creates a wizard NPC that throws a poison potion at the player after being hit.
   *
   * @return entity
   */
  public static Entity createWizardNPC(Entity player) {
    float throwRange = 6f;
    Entity wizard = makeKillable(createAnimatedNPC(player, "Wizard", WIZARD_NPC_ATLAS_PATH));

    // Create loot on drop
    int numGold = 1;
    InventoryComponent inventory = new InventoryComponent(numGold);
    List<Item> items = new ArrayList<>();

    ConsumableGenerator consumableGenerator = new ConsumableGenerator();
    items.add(consumableGenerator.generateConsumable(ConsumableType.REGENERATION, 1));
    for (Item item : items) {
      inventory.addItem(item);
    }

    // Add necessary components to the entity
    wizard
        .addComponent(new ProvokedComponent(8f))
        .addComponent(new PotionThrowComponent(throwRange))
        .addComponent(inventory)
        .addComponent(new ItemDropComponent());
    wizard
        .getComponent(AITaskComponent.class)
        .addTask(new RetaliateTask(player, 10, throwRange, "throwPoisonPotion"));
    wizard.getComponent(DialogueComponent.class).changeQuestType("enemiesquest");
    wizard
        .getComponent(QuestGiverComponent.class)
        .setItemToGive(new WeaponGenerator().generateWeapon(WeaponType.SWORD, 3));
    wizard.getComponent(DialogueComponent.class).changeAmountXToDo(3);
    return wizard;
  }

  /**
   * Creates an old philosopher NPC.
   *
   * @return entity
   */
  public static Entity createPhilosopherNPC(Entity player) {
    Entity npc = makeKillable(createAnimatedNPC(player, "Philosopher", PHILOSOPHER_NPC_ATLAS_PATH));
    npc.getComponent(QuestGiverComponent.class).setGoldToGive(100);
    return npc;
  }

  /**
   * Creates a satyr NPC that headbutts the player after being hit.
   *
   * @return entity
   */
  public static Entity createSatyrNPC(Entity player) {
    Entity satyr = makeKillable(createAnimatedNPC(player, "Satyr", SATYR_NPC_ATLAS_PATH));

    // Configure headbutt animations
    AnimationRenderComponent animator = satyr.getComponent(AnimationRenderComponent.class);
    animator.addAnimation("windupr", 0.25f, Animation.PlayMode.NORMAL);
    animator.addAnimation("windupl", 0.25f, Animation.PlayMode.NORMAL);
    animator.addAnimation("charger", 0.1f, Animation.PlayMode.LOOP);
    animator.addAnimation("chargel", 0.1f, Animation.PlayMode.LOOP);
    animator.addAnimation("headbuttr", 0.1f, Animation.PlayMode.NORMAL);
    animator.addAnimation("headbuttl", 0.1f, Animation.PlayMode.NORMAL);

    // Add necessary components to the entity
    satyr
        .addComponent(new ProvokedComponent(8f, 1.5f))
        .addComponent(new HeadbuttAttackComponent(player));
    satyr
        .getComponent(AITaskComponent.class)
        .addTask(new RetaliateTask(player, 10, 4f, "headbutt"));
    satyr.getComponent(DialogueComponent.class).changeQuestType("goldspentquest");
    satyr.getComponent(QuestGiverComponent.class).setGoldToGive(1);
    satyr.getComponent(DialogueComponent.class).changeAmountXToDo(50);
    return satyr;
  }

  /**
   * Creates a friendly NPC with walk and idle animations from its own atlas.
   *
   * @return entity
   */
  private static Entity createAnimatedNPC(Entity player, String speakerName, String atlasPath) {
    AnimationRenderComponent animator =
        new AnimationRenderComponent(loadIndependentAtlas(atlasPath), true);
    animator.addAnimation("walkr", 0.15f, Animation.PlayMode.LOOP);
    animator.addAnimation("walkl", 0.15f, Animation.PlayMode.LOOP);
    animator.addAnimation("idler", 0.15f, Animation.PlayMode.LOOP);
    animator.addAnimation("idlel", 0.15f, Animation.PlayMode.LOOP);

    Entity npc =
        new Entity().addComponent(animator).addComponent(new SkeletonAnimationController());
    animator.scaleEntity();
    return addFriendlyNpcParts(npc, player, speakerName);
  }

  /**
   * Loads a fresh, independently-owned {@link TextureAtlas} from the given internal file path,
   * bypassing {@code ResourceService}'s asset cache entirely.
   *
   * <p>Unlike {@code ServiceLocator.getResourceService().getAsset(path, TextureAtlas.class)}, which
   * returns the same shared instance for a given path on every call, this method constructs a
   * brand-new, independent {@code TextureAtlas} each time it is called — even for the same {@code
   * path}. This guarantees that each entity's {@link AnimationRenderComponent} holds an atlas
   * object no other entity references, so that entity's eventual disposal (which calls {@code
   * atlas.dispose()}) cannot invalidate another still-living entity's sprite.
   *
   * @param path internal file path to the {@code .atlas} file, e.g. {@code
   *     "images/enemies/skeleton.atlas"}
   * @return a new, independently-owned {@code TextureAtlas} loaded from that path
   * @throws com.badlogic.gdx.utils.GdxRuntimeException if the file does not exist or cannot be
   *     parsed as a texture atlas — same failure behaviour as libGDX's own atlas loading
   */
  private static TextureAtlas loadIndependentAtlas(String path) {
    FileHandle fileHandle = Gdx.files.internal(path);
    return new TextureAtlas(fileHandle);
  }

  /**
   * Adds the physics, dialogue and wandering shared by all friendly NPCs.
   *
   * @return entity
   */
  private static Entity addFriendlyNpcParts(Entity npc, Entity player, String speakerName) {
    npc.addComponent(new PhysicsComponent())
        .addComponent(new PhysicsMovementComponent())
        .addComponent(new ColliderComponent())
        .addComponent(new QuestGiverComponent(player))
        // NPC dialogue
        .addComponent(
            new DialogueComponent(
                new String[] {
                  "Hello, here's a quest!",
                  "Here's the progress of your quest: ",
                  "I've cleared your quest!",
                  "Thanks for the help",
                  "Hmm, something went wrong with completing your quest..."
                },
                "jumpquest",
                10))
        .addComponent(new DisplayDialogue(speakerName))
        .addComponent(new DialogueProximityComponent(player, 2f));

    npc.scaleHeight(FRIENDLY_NPC_HEIGHT);

    float floorCollisionScale = 0.5f - FRIENDLY_NPC_COLLIDER_WIDTH / 2f;
    npc.addComponent(
        new AITaskComponent()
            .addTask(
                new PlatformWanderTask(
                    new Vector2(FRIENDLY_NPC_WANDER_RANGE, FRIENDLY_NPC_WANDER_RANGE),
                    2f,
                    floorCollisionScale))
            .addTask(new StandStillTask(player, FRIENDLY_NPC_STAND_PRIORITY)));

    PhysicsUtils.setScaledCollider(npc, FRIENDLY_NPC_COLLIDER_WIDTH, 0.7f);
    npc.getComponent(PhysicsMovementComponent.class).setGroundedMovement(true);
    return npc;
  }

  /**
   * Gives an NPC a hitbox and health so it can be killed.
   *
   * @return entity
   */
  private static Entity makeKillable(Entity npc) {
    npc.addComponent(new HitboxComponent().setLayer(PhysicsLayer.NPC))
        .addComponent(new CombatStatsComponent(FRIENDLY_NPC_HEALTH, 0))
        .addComponent(new EnemyDeathComponent());
    return npc;
  }

  private NPCFactory() {
    throw new IllegalStateException("Instantiating static util class");
  }
}
