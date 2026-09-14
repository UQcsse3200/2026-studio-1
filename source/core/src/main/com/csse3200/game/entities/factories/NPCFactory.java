package com.csse3200.game.entities.factories;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.ai.tasks.AITaskComponent;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.attacks.*;
import com.csse3200.game.components.loot.*;
import com.csse3200.game.components.npc.EnemyDeathComponent;
import com.csse3200.game.components.npc.GhostAnimationController;
import com.csse3200.game.components.npc.SkeletonAnimationController;
import com.csse3200.game.components.npc.SkeletonWeaponAnimationController;
import com.csse3200.game.components.player.InventoryComponent;
import com.csse3200.game.components.player.ItemDropComponent;
import com.csse3200.game.components.tasks.*;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.configs.BaseEntityConfig;
import com.csse3200.game.entities.configs.GhostKingConfig;
import com.csse3200.game.entities.configs.NPCConfigs;
import com.csse3200.game.entities.configs.enemies.*;
import com.csse3200.game.files.FileLoader;
import com.csse3200.game.physics.PhysicsLayer;
import com.csse3200.game.physics.PhysicsUtils;
import com.csse3200.game.physics.components.ColliderComponent;
import com.csse3200.game.physics.components.HitboxComponent;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.physics.components.PhysicsMovementComponent;
import com.csse3200.game.rendering.AnimationRenderComponent;
import com.csse3200.game.rendering.EnemyWeaponAnimationComponent;
import com.csse3200.game.services.ServiceLocator;
import java.util.ArrayList;
import java.util.List;

/**
 * Factory to create non-playable character (NPC) entities with predefined components.
 *
 * <p>Each NPC entity type should have a creation method that returns a corresponding entity.
 * Predefined entity properties can be loaded from configs stored as json files which are defined in
 * "NPCConfigs".
 *
 * <p>If needed, this factory can be separated into more specific factories for entities with
 * similar characteristics.
 */
public class NPCFactory {
  private static final NPCConfigs configs =
      FileLoader.readClass(NPCConfigs.class, "configs/NPCs.json");

  /**
   * Creates a ghost entity.
   *
   * @param target entity to chase
   * @return entity
   */
  public static Entity createGhost(Entity target) {
    Entity ghost = createBaseNPC(target);
    BaseEntityConfig config = configs.ghost;

    AnimationRenderComponent animator =
        new AnimationRenderComponent(
            ServiceLocator.getResourceService()
                .getAsset("images/enemies/ghost.atlas", TextureAtlas.class));
    animator.addAnimation("angry_float", 0.1f, Animation.PlayMode.LOOP);
    animator.addAnimation("float", 0.1f, Animation.PlayMode.LOOP);

    ghost
        .addComponent(new CombatStatsComponent(config.health, config.baseAttack))
        .addComponent(animator)
        .addComponent(new GhostAnimationController());

    ghost.getComponent(AnimationRenderComponent.class).scaleEntity();

    return ghost;
  }

  /**
   * Creates a ghost king entity.
   *
   * @param target entity to chase
   * @return entity
   */
  public static Entity createGhostKing(Entity target) {
    Entity ghostKing = createBaseNPC(target);
    GhostKingConfig config = configs.ghostKing;

    AnimationRenderComponent animator =
        new AnimationRenderComponent(
            ServiceLocator.getResourceService()
                .getAsset("images/enemies/ghostKing.atlas", TextureAtlas.class));
    animator.addAnimation("float", 0.1f, Animation.PlayMode.LOOP);
    animator.addAnimation("angry_float", 0.1f, Animation.PlayMode.LOOP);

    ghostKing
        .addComponent(new CombatStatsComponent(config.health, config.baseAttack))
        .addComponent(animator)
        .addComponent(new GhostAnimationController());

    ghostKing.getComponent(AnimationRenderComponent.class).scaleEntity();
    return ghostKing;
  }

  /**
   * Creates a skeleton entity.
   *
   * @param target entity to chase
   * @return entity
   */
  public static Entity createSkeleton(Entity target) {
    float scale = 1.0f;
    Vector2 collisionScale = new Vector2(0.45f, 0.6f);
    Entity skeleton = createBasePlatformerNPC(target, (scale * collisionScale.x) / 2);
    SkeletonConfig config = configs.skeleton;

    // Create loot on drop
    int numGold = 3;
    InventoryComponent inventory = new InventoryComponent(numGold);
    List<Item> items = new ArrayList<>();

    WeaponGenerator weaponGenerator = new WeaponGenerator();
    items.add(weaponGenerator.generateWeapon(WeaponType.SWORD, 1));
    for (Item item : items) {
      inventory.addItem(item);
    }

    // Configure animation component
    AnimationRenderComponent animator =
        new AnimationRenderComponent(loadIndependentAtlas("images/enemies/skeleton.atlas"));
    animator.addAnimation("idlel", 0.1f, Animation.PlayMode.LOOP);
    animator.addAnimation("idler", 0.1f, Animation.PlayMode.LOOP);
    animator.addAnimation("walkl", 0.1f, Animation.PlayMode.LOOP);
    animator.addAnimation("walkr", 0.1f, Animation.PlayMode.LOOP);

    // Configure weapon animation component
    EnemyWeaponAnimationComponent weaponAnimator =
        new EnemyWeaponAnimationComponent(
            ServiceLocator.getResourceService()
                .getAsset("images/skeleton_weapons/skeleton_sword.atlas", TextureAtlas.class));
    weaponAnimator.addAnimation("default_l", 0.1f, Animation.PlayMode.LOOP);
    weaponAnimator.addAnimation("default_r", 0.1f, Animation.PlayMode.LOOP);
    weaponAnimator.addAnimation("sword_l", 0.08f, Animation.PlayMode.NORMAL);
    weaponAnimator.addAnimation("sword_r", 0.08f, Animation.PlayMode.NORMAL);

    // Add necessary components to the entity
    skeleton
        .addComponent(new CombatStatsComponent(config.health, config.baseAttack))
        .addComponent(
            new MeleeAttackComponent(
                config.melee.range,
                config.melee.cooldown,
                config.melee.knockback,
                (WeaponItem) inventory.getItem(1)))
        .addComponent(inventory)
        .addComponent(new ItemDropComponent())
        .addComponent(animator)
        .addComponent(new EnemyDeathComponent())
        .addComponent(new SkeletonAnimationController())
        .addComponent(new SkeletonAnimationController())
        .addComponent(weaponAnimator)
        .addComponent(new SkeletonWeaponAnimationController());

    skeleton.getComponent(AnimationRenderComponent.class).scaleEntity();
    skeleton.setScale(scale, scale);
    PhysicsUtils.setScaledCollider(skeleton, collisionScale.x, collisionScale.y);
    return skeleton;
  }

  /**
   * Creates a ranged skeleton entity (e.g. an archer-type enemy) that attacks from a distance
   * instead of approaching all the way up to its target.
   *
   * @param target entity to chase
   * @return entitys
   */
  public static Entity createRangedSkeleton(Entity target) {
    float scale = 1.0f;
    Vector2 collisionScale = new Vector2(0.45f, 0.6f);
    Entity rangedSkeleton = createBasePlatformerNPC(target, (scale * collisionScale.x) / 2);
    RangedSkeletonConfig config = configs.rangedSkeleton;

    // Create loot on drop
    int numGold = 3;
    InventoryComponent inventory = new InventoryComponent(numGold);
    List<Item> items = new ArrayList<>();

    WeaponGenerator weaponGenerator = new WeaponGenerator();
    items.add(weaponGenerator.generateWeapon(WeaponType.BOW, 1));
    for (Item item : items) {
      inventory.addItem(item);
    }

    // Configure animation component
    AnimationRenderComponent animator =
        new AnimationRenderComponent(loadIndependentAtlas("images/enemies/skeleton.atlas"));
    animator.addAnimation("idlel", 0.1f, Animation.PlayMode.LOOP);
    animator.addAnimation("idler", 0.1f, Animation.PlayMode.LOOP);
    animator.addAnimation("walkl", 0.1f, Animation.PlayMode.LOOP);
    animator.addAnimation("walkr", 0.1f, Animation.PlayMode.LOOP);

    // Configure weapon animation component
    EnemyWeaponAnimationComponent weaponAnimator =
        new EnemyWeaponAnimationComponent(
            ServiceLocator.getResourceService()
                .getAsset("images/skeleton_weapons/skeleton_bow.atlas", TextureAtlas.class));
    weaponAnimator.addAnimation("default_l", 0.1f, Animation.PlayMode.LOOP);
    weaponAnimator.addAnimation("default_r", 0.1f, Animation.PlayMode.LOOP);
    weaponAnimator.addAnimation("bow_l", 0.1f, Animation.PlayMode.NORMAL);
    weaponAnimator.addAnimation("bow_r", 0.1f, Animation.PlayMode.NORMAL);

    // Add necessary components to the entity
    rangedSkeleton
        .addComponent(new CombatStatsComponent(config.health, config.baseAttack))
        .addComponent(
            new RangedAttackComponent(
                config.ranged.range,
                config.ranged.cooldown,
                config.ranged.knockback,
                (WeaponItem) inventory.getItem(1),
                8f))
        .addComponent(inventory)
        .addComponent(new ItemDropComponent())
        .addComponent(animator)
        .addComponent(new EnemyDeathComponent())
        .addComponent(new SkeletonAnimationController())
        .addComponent(new SkeletonAnimationController())
        .addComponent(weaponAnimator)
        .addComponent(new SkeletonWeaponAnimationController());

    rangedSkeleton
        .getComponent(RangedAttackComponent.class)
        .setProjectileSpeed(config.ranged.projectileSpeed);

    rangedSkeleton.getComponent(AnimationRenderComponent.class).scaleEntity();

    // Attack from range instead of flying/chasing all the way onto the target - see
    // RangedAttackTask's javadoc for why a higher priority than ChaseTask is what achieves this.
    rangedSkeleton
        .getComponent(AITaskComponent.class)
        .addTask(new RangedAttackTask(target, 15, config.ranged.range));

    rangedSkeleton.setScale(scale, scale);
    PhysicsUtils.setScaledCollider(rangedSkeleton, collisionScale.x, collisionScale.y);
    return rangedSkeleton;
  }

  /**
   * Creates a Minotaur entity (e.g. a large enemy with an axe) that attacks through charging at
   * enemy with melee weapon and added damage.
   *
   * @param target entity to chase
   * @return minotaur entity that charges at target to attack
   */
  public static Entity createMinotaur(Entity target) {
    float scale = 2f;
    Vector2 collisionScale = new Vector2(0.4f, 0.5f);
    Entity minotaur = createBasePlatformerNPC(target, (scale * collisionScale.x) / 2);
    MinotaurConfig config = configs.minotaur;

    // Create loot on drop
    int numGold = 3;
    InventoryComponent inventory = new InventoryComponent(numGold);
    List<Item> items = new ArrayList<>();

    WeaponGenerator weaponGenerator = new WeaponGenerator();
    items.add(weaponGenerator.generateWeapon(WeaponType.SWORD, 1));
    for (Item item : items) {
      inventory.addItem(item);
    }

    // Configure animation component
    // TODO: THIS WILL BE CHANGED TO MINOTAUR ANIMATION AND SPRITES
    AnimationRenderComponent animator =
        new AnimationRenderComponent(loadIndependentAtlas("images/enemies/skeleton  .atlas"));
    animator.addAnimation("idlel", 0.1f, Animation.PlayMode.LOOP);
    animator.addAnimation("idler", 0.1f, Animation.PlayMode.LOOP);
    animator.addAnimation("walkl", 0.1f, Animation.PlayMode.LOOP);
    animator.addAnimation("walkr", 0.1f, Animation.PlayMode.LOOP);

    // Add necessary components to the entity
    minotaur
        .addComponent(new CombatStatsComponent(config.health, config.baseAttack))
        .addComponent(
            new MeleeAttackComponent(
                config.melee.range,
                config.melee.cooldown,
                config.melee.knockback,
                (WeaponItem) inventory.getItem(1)))
        .addComponent(inventory)
        .addComponent(new ItemDropComponent())
        .addComponent(animator)
        .addComponent(new EnemyDeathComponent())
        .addComponent(new SkeletonAnimationController());

    minotaur.getComponent(AnimationRenderComponent.class).scaleEntity();

    ChargeComponent chargeComponent =
        new ChargeComponent(
            config.charge.duration,
            config.charge.cooldown,
            config.charge.damageMultiplier,
            config.charge.speedMultiplier);

    // Attack from range instead of flying/chasing all the way onto the target - see
    // RangedAttackTask's javadoc for why a higher priority than ChaseTask is what achieves this.
    minotaur
        .getComponent(AITaskComponent.class)
        .addTask(new MeleeAttackTask(target, 10, config.melee.range))
        .addTask((new ChargeTask(target, chargeComponent, config.charge.aggroRadius, 15, 10)));

    minotaur.setScale(scale, scale);
    PhysicsUtils.setScaledCollider(minotaur, collisionScale.x, collisionScale.y);
    return minotaur;
  }

  /**
   * Creates a Centaur entity (e.g. a large enemy with a bow) that attacks from a distance while
   * charging towards the target.
   *
   * @param target entity to chase
   * @return centaur entity that charges at target to attack from a distance.
   */
  public static Entity createCentaur(Entity target) {
    float scale = 2f;
    Vector2 collisionScale = new Vector2(0.4f, 0.5f);
    Entity centaur = createBasePlatformerNPC(target, (scale * collisionScale.x) / 2);
    CentaurConfig config = configs.centaur;

    // Create loot on drop
    int numGold = 3;
    InventoryComponent inventory = new InventoryComponent(numGold);
    List<Item> items = new ArrayList<>();

    WeaponGenerator weaponGenerator = new WeaponGenerator();
    items.add(weaponGenerator.generateWeapon(WeaponType.SWORD, 1));
    for (Item item : items) {
      inventory.addItem(item);
    }

    // Configure animation component
    // TODO: THIS WILL BE CHANGED TO CENTAUR ANIMATION AND SPRITES
    AnimationRenderComponent animator =
        new AnimationRenderComponent(loadIndependentAtlas("images/enemies/skeleton.atlas"));
    animator.addAnimation("idlel", 0.1f, Animation.PlayMode.LOOP);
    animator.addAnimation("idler", 0.1f, Animation.PlayMode.LOOP);
    animator.addAnimation("walkl", 0.1f, Animation.PlayMode.LOOP);
    animator.addAnimation("walkr", 0.1f, Animation.PlayMode.LOOP);

    // Add necessary components to the entity
    centaur
        .addComponent(new CombatStatsComponent(config.health, config.baseAttack))
        .addComponent(
            new RangedAttackComponent(
                config.ranged.range,
                config.ranged.cooldown,
                config.ranged.knockback,
                (WeaponItem) inventory.getItem(1),
                8f))
        .addComponent(inventory)
        .addComponent(new ItemDropComponent())
        .addComponent(animator)
        .addComponent(new EnemyDeathComponent())
        .addComponent(new SkeletonAnimationController());

    centaur.getComponent(AnimationRenderComponent.class).scaleEntity();

    ChargeComponent chargeComponent =
        new ChargeComponent(
            config.charge.duration,
            config.charge.cooldown,
            config.charge.damageMultiplier,
            config.charge.speedMultiplier);

    // Attack from range instead of flying/chasing all the way onto the target - see
    // RangedAttackTask's javadoc for why a higher priority than ChaseTask is what achieves this.
    centaur
        .getComponent(AITaskComponent.class)
        .addTask(new RangedAttackTask(target, 10, config.ranged.range))
        .addTask((new ChargeTask(target, chargeComponent, config.charge.aggroRadius, 15, 10)));

    centaur.setScale(scale, scale);
    PhysicsUtils.setScaledCollider(centaur, collisionScale.x, collisionScale.y);
    return centaur;
  }

  /**
   * Creates a Cyclops (e.g. a mini-boss that has both melee and ranged attacks) that can attack
   * from close or far away.
   *
   * @param target entity to chase
   * @return Cyclops entity as a mini boss enemy
   */
  public static Entity createCyclops(Entity target) {
    float scale = 2f;
    Vector2 collisionScale = new Vector2(0.4f, 0.5f);
    Entity cyclops = createBasePlatformerNPC(target, (scale * collisionScale.x) / 2);
    CyclopsConfig config = configs.cyclops;

    // Create loot on drop
    int numGold = 3;
    InventoryComponent inventory = new InventoryComponent(numGold);
    List<Item> items = new ArrayList<>();

    WeaponGenerator weaponGenerator = new WeaponGenerator();
    items.add(weaponGenerator.generateWeapon(WeaponType.SWORD, 1));
    items.add(weaponGenerator.generateWeapon(WeaponType.BOW, 1));
    for (Item item : items) {
      inventory.addItem(item);
    }

    // Configure animation component
    // TODO: THIS WILL BE CHANGED TO CYCLOPS ANIMATION AND SPRITES
    AnimationRenderComponent animator =
        new AnimationRenderComponent(loadIndependentAtlas("images/enemies/skeleton.atlas"));
    animator.addAnimation("idlel", 0.1f, Animation.PlayMode.LOOP);
    animator.addAnimation("idler", 0.1f, Animation.PlayMode.LOOP);
    animator.addAnimation("walkl", 0.1f, Animation.PlayMode.LOOP);
    animator.addAnimation("walkr", 0.1f, Animation.PlayMode.LOOP);

    // Add necessary components to the entity
    cyclops
        .addComponent(new CombatStatsComponent(config.health, config.baseAttack))
        .addComponent(
            new RangedAttackComponent(
                config.ranged.range,
                config.ranged.cooldown,
                config.ranged.knockback,
                (WeaponItem) inventory.getItem(1),
                8f))
        .addComponent(
            new MeleeAttackComponent(
                config.melee.range,
                config.melee.cooldown,
                config.melee.knockback,
                (WeaponItem) inventory.getItem(2)))
        .addComponent(inventory)
        .addComponent(new ItemDropComponent())
        .addComponent(new EnemyDeathComponent())
        .addComponent(animator)
        .addComponent(new SkeletonAnimationController());

    cyclops.getComponent(AnimationRenderComponent.class).scaleEntity();

    // Attack from range instead of flying/chasing all the way onto the target - see
    // RangedAttackTask's javadoc for why a higher priority than ChaseTask is what achieves this.
    cyclops
        .getComponent(AITaskComponent.class)
        .addTask(new MeleeAttackTask(target, 10, config.melee.range))
        .addTask(new RangedAttackTask(target, 9, config.ranged.range));

    cyclops.setScale(scale, scale);
    PhysicsUtils.setScaledCollider(cyclops, collisionScale.x, collisionScale.y);
    return cyclops;
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
   * @param path internal file path to the {@code .atlas} file, e.g. {@code "images/skeleton.atlas"}
   * @return a new, independently-owned {@code TextureAtlas} loaded from that path
   * @throws com.badlogic.gdx.utils.GdxRuntimeException if the file does not exist or cannot be
   *     parsed as a texture atlas — same failure behaviour as libGDX's own atlas loading
   */
  private static TextureAtlas loadIndependentAtlas(String path) {
    FileHandle fileHandle = Gdx.files.internal(path);
    return new TextureAtlas(fileHandle);
  }

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
            .addComponent(new TouchAttackComponent(PhysicsLayer.PLAYER, 1.5f))
            .addComponent(aiComponent);

    PhysicsUtils.setScaledCollider(npc, 0.9f, 0.4f);
    // Let gravity pull the NPC down instead of the wander/chase AI flying it directly toward
    npc.getComponent(PhysicsMovementComponent.class).setGroundedMovement(true);
    return npc;
  }

  /**
   * Creates a generic NPC that falls with gravity to be used as a base entity by more specific
   * Platformer NPC creation methods.
   *
   * <p>Structure inspired by createBaseNPC function, but removes touch damage in favour of using
   * melee attacks
   *
   * @return entity
   */
  private static Entity createBasePlatformerNPC(Entity target, final float floorCollisionScale) {
    AITaskComponent aiComponent =
        new AITaskComponent()
            .addTask(new PlatformWanderTask(new Vector2(2f, 2f), 2f, floorCollisionScale))
            .addTask(new ChaseTask(target, 10, 3f, 4f))
            .addTask(new MeleeAttackTask(target, 15, 1f));
    Entity npc =
        new Entity()
            .addComponent(new PhysicsComponent())
            .addComponent(new PhysicsMovementComponent())
            .addComponent(new ColliderComponent())
            .addComponent(new HitboxComponent().setLayer(PhysicsLayer.NPC))
            .addComponent(aiComponent);

    PhysicsUtils.setScaledCollider(npc, 0.9f, 0.7f);
    npc.getComponent(PhysicsMovementComponent.class).setGroundedMovement(true);
    return npc;
  }

  private NPCFactory() {
    throw new IllegalStateException("Instantiating static util class");
  }
}
