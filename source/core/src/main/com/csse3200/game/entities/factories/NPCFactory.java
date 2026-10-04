package com.csse3200.game.entities.factories;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.ai.tasks.AITaskComponent;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.EnemyType;
import com.csse3200.game.components.QuestGiverComponent;
import com.csse3200.game.components.attacks.*;
import com.csse3200.game.components.loot.*;
import com.csse3200.game.components.npc.*;
import com.csse3200.game.components.player.InventoryComponent;
import com.csse3200.game.components.player.ItemDropComponent;
import com.csse3200.game.components.projectile.ProjectileType;
import com.csse3200.game.components.tasks.*;
import com.csse3200.game.entities.Entity;
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
import com.csse3200.game.rendering.TextureRenderComponent;
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
  private static final String SKELETON_ATLAS_PATH = "images/enemies/skeleton.atlas";
  private static final String SKELETON_SWORD_ATLAS_PATH =
      "images/skeleton_weapons/skeleton_sword.atlas";
  private static final String SKELETON_BOW_ATLAS_PATH =
      "images/skeleton_weapons/skeleton_bow.atlas";
  private static final String MINOTAUR_ATLAS_PATH = "images/enemies/minotaur.atlas";
  private static final String CYCLOPS_ATLAS_PATH = "images/enemies/cyclops.atlas";

  private static final NPCConfigs configs =
      FileLoader.readClass(NPCConfigs.class, "configs/NPCs.json");

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
        new AnimationRenderComponent(loadIndependentAtlas(SKELETON_ATLAS_PATH));
    animator.addAnimation("idlel", 0.1f, Animation.PlayMode.LOOP);
    animator.addAnimation("idler", 0.1f, Animation.PlayMode.LOOP);
    animator.addAnimation("walkl", 0.1f, Animation.PlayMode.LOOP);
    animator.addAnimation("walkr", 0.1f, Animation.PlayMode.LOOP);

    // Configure weapon animation component
    EnemyWeaponAnimationComponent weaponAnimator =
        new EnemyWeaponAnimationComponent(loadIndependentAtlas(SKELETON_SWORD_ATLAS_PATH));
    weaponAnimator.addAnimation("default_l", 0.1f, Animation.PlayMode.LOOP);
    weaponAnimator.addAnimation("default_r", 0.1f, Animation.PlayMode.LOOP);
    weaponAnimator.addAnimation("sword_l", 0.08f, Animation.PlayMode.NORMAL);
    weaponAnimator.addAnimation("sword_r", 0.08f, Animation.PlayMode.NORMAL);

    // Add necessary components to the entity
    skeleton
        .addComponent(new CombatStatsComponent(config.health, config.baseAttack))
        .addComponent(new EnemyTypeComponent(EnemyType.SKELETON))
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
        new AnimationRenderComponent(loadIndependentAtlas(SKELETON_ATLAS_PATH), true);
    animator.addAnimation("idlel", 0.1f, Animation.PlayMode.LOOP);
    animator.addAnimation("idler", 0.1f, Animation.PlayMode.LOOP);
    animator.addAnimation("walkl", 0.1f, Animation.PlayMode.LOOP);
    animator.addAnimation("walkr", 0.1f, Animation.PlayMode.LOOP);

    // Configure weapon animation component
    EnemyWeaponAnimationComponent weaponAnimator =
        new EnemyWeaponAnimationComponent(loadIndependentAtlas(SKELETON_BOW_ATLAS_PATH));
    weaponAnimator.addAnimation("default_l", 0.1f, Animation.PlayMode.LOOP);
    weaponAnimator.addAnimation("default_r", 0.1f, Animation.PlayMode.LOOP);
    weaponAnimator.addAnimation("bow_l", 0.1f, Animation.PlayMode.NORMAL);
    weaponAnimator.addAnimation("bow_r", 0.1f, Animation.PlayMode.NORMAL);

    // Add necessary components to the entity
    rangedSkeleton
        .addComponent(new CombatStatsComponent(config.health, config.baseAttack))
        .addComponent(new EnemyTypeComponent(EnemyType.RANGED_SKELETON))
        .addComponent(
            new RangedAttackComponent(
                config.ranged.range,
                config.ranged.cooldown,
                config.ranged.knockback,
                (WeaponItem) inventory.getItem(1)))
        .addComponent(inventory)
        .addComponent(new ItemDropComponent())
        .addComponent(animator)
        .addComponent(new EnemyDeathComponent())
        .addComponent(new SkeletonAnimationController())
        .addComponent(weaponAnimator)
        .addComponent(new SkeletonWeaponAnimationController());

    rangedSkeleton
        .getComponent(RangedAttackComponent.class)
        .setProjectileSpeed(config.ranged.projectileSpeed);

    rangedSkeleton.getComponent(AnimationRenderComponent.class).scaleEntity();

    // Attack from range instead of flying/chasing all the way onto the target - see
    // RangedAttackTask's Javadoc for why a higher priority than ChaseTask is what achieves this.
    rangedSkeleton
        .getComponent(AITaskComponent.class)
        .addTask(new RangedAttackTask(target, 15, config.ranged.range, ProjectileType.ARROW));

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
    float scale = 4.0f;
    Vector2 collisionScale = new Vector2(0.8f, 0.7f);
    Entity minotaur = createBasePlatformerNPC(target, (scale * collisionScale.x) / 2);
    MinotaurConfig config = configs.minotaur;

    // Create loot on drop
    int numGold = 3;
    InventoryComponent inventory = new InventoryComponent(numGold);
    List<Item> items = new ArrayList<>();

    WeaponGenerator weaponGenerator = new WeaponGenerator();
    WeaponItem weapon = weaponGenerator.generateWeapon(WeaponType.AXE, 2);
    weapon.setWindupDuration(0f);
    items.add(weapon);
    for (Item item : items) {
      inventory.addItem(item);
    }

    // Configure animation component
    AnimationRenderComponent animator =
        new AnimationRenderComponent(loadIndependentAtlas(MINOTAUR_ATLAS_PATH), true);
    animator.addAnimation("minotaur_idle_l", 0.1f, Animation.PlayMode.LOOP);
    animator.addAnimation("minotaur_idle_r", 0.1f, Animation.PlayMode.LOOP);
    animator.addAnimation("minotaur_walk_l", 0.1f, Animation.PlayMode.LOOP);
    animator.addAnimation("minotaur_walk_r", 0.1f, Animation.PlayMode.LOOP);
    animator.addAnimation("minotaur_charge_l", 0.1f, Animation.PlayMode.LOOP);
    animator.addAnimation("minotaur_charge_r", 0.1f, Animation.PlayMode.LOOP);
    animator.addAnimation("minotaur_swing_l", 0.1f, Animation.PlayMode.NORMAL);
    animator.addAnimation("minotaur_swing_r", 0.1f, Animation.PlayMode.NORMAL);

    // Add necessary components to the entity
    minotaur
        .addComponent(new CombatStatsComponent(config.health, config.baseAttack))
        .addComponent(inventory)
        .addComponent(new EnemyTypeComponent(EnemyType.MINOTAUR))
        .addComponent(
            new MeleeAttackComponent(
                config.melee.range,
                config.melee.cooldown,
                config.melee.knockback,
                (WeaponItem) inventory.getItem(1)))
        .addComponent(
            new ChargeComponent(
                config.charge.duration,
                config.charge.windupDuration,
                config.charge.cooldown,
                config.charge.damageMultiplier,
                config.charge.speedMultiplier))
        .addComponent(new ItemDropComponent())
        .addComponent(new EnemyDeathComponent())
        .addComponent(animator)
        .addComponent(new MinotaurAnimationController());

    minotaur.getComponent(AnimationRenderComponent.class).scaleEntity();

    // Attack from range instead of flying/chasing all the way onto the target - see
    // RangedAttackTask's Javadoc for why a higher priority than ChaseTask is what achieves this.
    minotaur
        .getComponent(AITaskComponent.class)
        .addTask(new MeleeAttackTask(target, 15, config.melee.range))
        .addTask(
            (new ChargeTask(
                target,
                minotaur.getComponent(ChargeComponent.class),
                config.charge.aggroRadius,
                16,
                -1)));

    minotaur.setScale(scale, scale * (80f / 96f));

    // 16 pixels shorter from below to make the minotaur's feet align with the floor.
    // The sprite frame is 80px high, so 16px is (16f / 80f) of the entity's height.
    float pixelHeight = minotaur.getScale().y / 80f;
    float bottomOffset = 16f * pixelHeight;
    float boxWidth = minotaur.getScale().x * collisionScale.x;
    float boxHeight = (minotaur.getScale().y * collisionScale.y) - bottomOffset;
    Vector2 boxSize = new Vector2(boxWidth, boxHeight);
    Vector2 boxPosition = new Vector2(minotaur.getScale().x / 2f, bottomOffset + (boxHeight / 2f));
    minotaur.getComponent(ColliderComponent.class).setAsBox(boxSize, boxPosition);
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
    float scale = 4.0f;
    Vector2 collisionScale = new Vector2(0.4f, 0.5f);
    Entity centaur = createBasePlatformerNPC(target, (scale * collisionScale.x) / 2);
    CentaurConfig config = configs.centaur;

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
    // TODO: THIS WILL BE CHANGED TO CENTAUR ANIMATION AND SPRITES
    AnimationRenderComponent animator =
        new AnimationRenderComponent(loadIndependentAtlas(SKELETON_ATLAS_PATH), true);
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
                (WeaponItem) inventory.getItem(1)))
        .addComponent(
            new ChargeComponent(
                config.charge.duration,
                config.charge.windupDuration,
                config.charge.cooldown,
                config.charge.damageMultiplier,
                config.charge.speedMultiplier))
        .addComponent(new EnemyTypeComponent(EnemyType.CENTAUR))
        .addComponent(inventory)
        .addComponent(new ItemDropComponent())
        .addComponent(animator)
        .addComponent(new EnemyDeathComponent())
        .addComponent(new SkeletonAnimationController());

    centaur
        .getComponent(RangedAttackComponent.class)
        .setProjectileSpeed(config.ranged.projectileSpeed);

    centaur.getComponent(AnimationRenderComponent.class).scaleEntity();

    // Attack from range instead of flying/chasing all the way onto the target - see
    // RangedAttackTask's Javadoc for why a higher priority than ChaseTask is what achieves this.
    centaur
        .getComponent(AITaskComponent.class)
        .addTask(new RangedAttackTask(target, 10, config.ranged.range, ProjectileType.ARROW))
        .addTask(
            (new ChargeTask(
                target,
                centaur.getComponent(ChargeComponent.class),
                config.charge.aggroRadius,
                16,
                -1)));

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
    float scale = 2.0f;
    Vector2 collisionScale = new Vector2(0.4f, 0.5f);
    Entity cyclops = createBasePlatformerNPC(target, (scale * collisionScale.x) / 2);
    CyclopsConfig config = configs.cyclops;

    // Create loot on drop - more gold dropped due to no weapons being dropped (no inventory)
    int numGold = 12;
    InventoryComponent inventory = new InventoryComponent(numGold);

    // Configure animation component
    // TODO: CYCLOPS ATTACK ANIMATION TO DO
    AnimationRenderComponent animator =
        new AnimationRenderComponent(loadIndependentAtlas(CYCLOPS_ATLAS_PATH), true);
    animator.addAnimation("cyclops_idle_l", 0.1f, Animation.PlayMode.LOOP);
    animator.addAnimation("cyclops_idle_r", 0.1f, Animation.PlayMode.LOOP);
    animator.addAnimation("cyclops_walk_l", 0.1f, Animation.PlayMode.LOOP);
    animator.addAnimation("cyclops_walk_r", 0.1f, Animation.PlayMode.LOOP);

    // Cyclops carries no weapon - natural attacks flow through the same weapon-based attack
    // constructors an armed enemy uses, via WeaponItem.natural(...), rather than each attack
    // component needing its own parallel no-weapon constructor (see WeaponItem#natural).
    WeaponItem naturalFists =
        WeaponItem.natural("Cyclops Fists", config.baseAttack, config.melee.cooldown - 1);
    WeaponItem naturalRockThrow =
        WeaponItem.natural("Cyclops Rock Throw", config.baseAttack, config.ranged.cooldown - 1);

    // Add necessary components to the entity
    cyclops
        .addComponent(new CombatStatsComponent(config.health, config.baseAttack))
        .addComponent(
            new MeleeAttackComponent(
                config.melee.range, config.melee.cooldown, config.melee.knockback, naturalFists))
        .addComponent(
            new RangedAttackComponent(
                config.ranged.range,
                config.ranged.cooldown,
                config.ranged.knockback,
                naturalRockThrow))
        .addComponent(new EnemyTypeComponent(EnemyType.CYCLOPS))
        .addComponent(inventory)
        .addComponent(new ItemDropComponent())
        .addComponent(new EnemyDeathComponent())
        .addComponent(animator)
        .addComponent(new CyclopsAnimationController());

    cyclops
        .getComponent(RangedAttackComponent.class)
        .setProjectileSpeed(config.ranged.projectileSpeed);

    cyclops.getComponent(AnimationRenderComponent.class).scaleEntity();

    // Attack from range instead of flying/chasing all the way onto the target - see
    // RangedAttackTask's Javadoc for why a higher priority than ChaseTask is what achieves this.
    cyclops
        .getComponent(AITaskComponent.class)
        .addTask(new MeleeAttackTask(target, 10, config.melee.range))
        .addTask(new RangedAttackTask(target, 9, config.ranged.range, ProjectileType.ARROW));

    cyclops.setScale(scale, scale * (48f / 64f));
    PhysicsUtils.setScaledCollider(cyclops, collisionScale.x, collisionScale.y);
    return cyclops;
  }

  /**
   * Creates a Medusa entity: a mini-boss who paces near her spawn, melees up close with natural
   * claws, and petrifies the player from range with her gaze.
   *
   * <p>Structurally identical to {@link #createCyclops}: a ground mini-boss with both a melee and
   * a ranged attack, neither backed by a carried weapon. Medusa's gaze reuses the same {@code
   * WeaponItem.natural(...)} pattern as Cyclops's rock throw, just fired as {@link
   * ProjectileType#GAZE} instead of {@link ProjectileType#ARROW} so it petrifies rather than knocks
   * back on a landed hit (see {@link PetrifyEffectComponent}).
   *
   * @param target entity to chase
   * @return Medusa entity as a mini-boss enemy
   */
  public static Entity createMedusa(Entity target) {
    float scale = 2.0f;
    Vector2 collisionScale = new Vector2(0.4f, 0.5f);
    Entity medusa = createBasePlatformerNPC(target, (scale * collisionScale.x) / 2);
    MedusaConfig config = configs.medusa;

    // Create loot on drop - more gold dropped due to no weapons being dropped (no inventory)
    int numGold = 12;
    InventoryComponent inventory = new InventoryComponent(numGold);

    // Configure animation component
    // TODO: MEDUSA'S OWN SPRITES AND ATTACK ANIMATIONS - reuses the Cyclops atlas as a
    // placeholder mini-boss sprite in the meantime, same as Centaur reuses the Skeleton atlas.
    AnimationRenderComponent animator =
        new AnimationRenderComponent(loadIndependentAtlas(CYCLOPS_ATLAS_PATH), true);
    animator.addAnimation("cyclops_idle_l", 0.1f, Animation.PlayMode.LOOP);
    animator.addAnimation("cyclops_idle_r", 0.1f, Animation.PlayMode.LOOP);
    animator.addAnimation("cyclops_walk_l", 0.1f, Animation.PlayMode.LOOP);
    animator.addAnimation("cyclops_walk_r", 0.1f, Animation.PlayMode.LOOP);

    // Medusa carries no weapons - both attacks flow through the same weapon-based attack
    // constructors an armed enemy uses, via WeaponItem.natural(...), exactly like Cyclops's fists
    // and rock throw.
    WeaponItem naturalClaws =
        WeaponItem.natural("Medusa Claws", config.baseAttack, config.melee.cooldown - 1);
    WeaponItem naturalGaze =
        WeaponItem.natural("Medusa Gaze", config.baseAttack, config.ranged.cooldown - 1);

    // Add necessary components to the entity
    medusa
        .addComponent(new CombatStatsComponent(config.health, config.baseAttack))
        .addComponent(
            new MeleeAttackComponent(
                config.melee.range, config.melee.cooldown, config.melee.knockback, naturalClaws))
        .addComponent(
            new RangedAttackComponent(
                config.ranged.range, config.ranged.cooldown, config.ranged.knockback, naturalGaze))
        .addComponent(new EnemyTypeComponent(EnemyType.MEDUSA))
        .addComponent(inventory)
        .addComponent(new ItemDropComponent())
        .addComponent(new EnemyDeathComponent())
        .addComponent(animator)
        .addComponent(new CyclopsAnimationController());

    medusa
        .getComponent(RangedAttackComponent.class)
        .setProjectileSpeed(config.ranged.projectileSpeed);
    medusa.getComponent(RangedAttackComponent.class).setPetrifyTicks(config.petrifyTicks);

    medusa.getComponent(AnimationRenderComponent.class).scaleEntity();

    // Attack from range instead of flying/chasing all the way onto the target - see
    // RangedAttackTask's Javadoc for why a higher priority than ChaseTask is what achieves this.
    medusa
        .getComponent(AITaskComponent.class)
        .addTask(new MeleeAttackTask(target, 10, config.melee.range))
        .addTask(new RangedAttackTask(target, 9, config.ranged.range, ProjectileType.GAZE));

    medusa.setScale(scale, scale * (48f / 64f));
    PhysicsUtils.setScaledCollider(medusa, collisionScale.x, collisionScale.y);
    return medusa;
  }

  /**
   * Creates a Cerberus entity: a stationary mini-boss who bites very often. Unlike every other
   * enemy in this factory, Cerberus does not wander or chase - he waits at his spawn point and only
   * ever runs a {@link MeleeAttackTask}, which naturally goes idle (a no-op, not an error - see
   * {@link com.csse3200.game.ai.tasks.AITaskComponent#update}) whenever the target is out of reach.
   *
   * <p>"Three heads" biting very often is represented as a single {@link MeleeAttackComponent} with
   * a deliberately short cooldown (see {@link CerberusConfig}) rather than three separate attack
   * components - modelling three literal simultaneous attacks would add complexity disproportionate
   * to the actual gameplay effect of "bites often".
   *
   * @param target entity to bite once in range
   * @return Cerberus entity as a stationary mini-boss enemy
   */
  public static Entity createCerberus(Entity target) {
    float scale = 3.0f;
    Vector2 collisionScale = new Vector2(0.8f, 0.6f);
    Entity cerberus = createStationaryPlatformerNPC();
    CerberusConfig config = configs.cerberus;

    // Create loot on drop - more gold dropped due to no weapons being dropped (no inventory)
    int numGold = 12;
    InventoryComponent inventory = new InventoryComponent(numGold);

    // Configure animation component
    // TODO: CERBERUS' OWN SPRITES AND BITE ANIMATION - reuses the Minotaur atlas as a placeholder
    // in the meantime (its "swing" animation doubles as a bite), same as Centaur/Medusa reuse
    // other enemies' atlases until Cerberus's own art lands.
    AnimationRenderComponent animator =
        new AnimationRenderComponent(loadIndependentAtlas(MINOTAUR_ATLAS_PATH), true);
    animator.addAnimation("minotaur_idle_l", 0.1f, Animation.PlayMode.LOOP);
    animator.addAnimation("minotaur_idle_r", 0.1f, Animation.PlayMode.LOOP);
    animator.addAnimation("minotaur_walk_l", 0.1f, Animation.PlayMode.LOOP);
    animator.addAnimation("minotaur_walk_r", 0.1f, Animation.PlayMode.LOOP);
    animator.addAnimation("minotaur_swing_l", 0.1f, Animation.PlayMode.NORMAL);
    animator.addAnimation("minotaur_swing_r", 0.1f, Animation.PlayMode.NORMAL);

    // Cerberus carries no weapon - his bite flows through the same weapon-based attack
    // constructor an armed enemy uses, via WeaponItem.natural(...), exactly like Cyclops's fists.
    WeaponItem naturalBite =
        WeaponItem.natural("Cerberus Bite", config.baseAttack, config.melee.cooldown - 1);

    // Add necessary components to the entity
    cerberus
        .addComponent(new CombatStatsComponent(config.health, config.baseAttack))
        .addComponent(
            new MeleeAttackComponent(
                config.melee.range, config.melee.cooldown, config.melee.knockback, naturalBite))
        .addComponent(new EnemyTypeComponent(EnemyType.CERBERUS))
        .addComponent(inventory)
        .addComponent(new ItemDropComponent())
        .addComponent(new EnemyDeathComponent())
        .addComponent(animator)
        // MinotaurAnimationController works unmodified with no ChargeComponent attached - every
        // branch that reads it already null-checks, so Cerberus simply never enters a charge
        // state and plays its idle/walk/swing (bite) animations exactly as intended.
        .addComponent(new MinotaurAnimationController());

    cerberus.getComponent(AnimationRenderComponent.class).scaleEntity();

    cerberus
        .getComponent(AITaskComponent.class)
        .addTask(new MeleeAttackTask(target, 10, config.melee.range));

    cerberus.setScale(scale, scale * (80f / 96f));
    PhysicsUtils.setScaledCollider(cerberus, collisionScale.x, collisionScale.y);
    return cerberus;
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

  /**
   * Creates a generic NPC that falls with gravity but never wanders or chases, to be used as a base
   * entity by stationary mini-boss creation methods (currently just Cerberus).
   *
   * <p>Deliberately has no {@code target} parameter and adds no {@link WanderTask}/{@link
   * ChaseTask}/{@link MeleeAttackTask} of its own - unlike {@link #createBasePlatformerNPC}, which
   * always adds all three. {@link com.csse3200.game.ai.tasks.AITaskComponent} already has no issue
   * running with zero eligible tasks (see its own Javadoc), so the caller is free to add only the
   * attack task(s) it actually needs. No raycast-based floor positioning is needed here, unlike
   * {@link #createBasePlatformerNPC} - with no wander task ever moving this entity off its spawn
   * point, gravity alone is enough to settle it on the ground once.
   *
   * @return entity
   */
  private static Entity createStationaryPlatformerNPC() {
    Entity npc =
        new Entity()
            .addComponent(new PhysicsComponent())
            .addComponent(new PhysicsMovementComponent())
            .addComponent(new ColliderComponent())
            .addComponent(new HitboxComponent().setLayer(PhysicsLayer.NPC))
            .addComponent(new AITaskComponent());

    PhysicsUtils.setScaledCollider(npc, 0.9f, 0.7f);
    npc.getComponent(PhysicsMovementComponent.class).setGroundedMovement(true);
    return npc;
  }

  /**
   * Creates a passive traveler NPC that wanders but cannot attack the player.
   *
   * @return entity
   */
  public static Entity createTravelerNPC(Entity player) {
    final float colliderWidthFraction = 0.9f;
    // Matches the player's rendered height: PlayerFactory scales box_boy_leaf.png (792x1000) to
    // width 1, height 1000/792 via TextureRenderComponent.scaleEntity().
    final float playerHeight = 1000f / 792f;
    String[] dialoguetext = {"Hello", "Good luck"};
    Entity npc =
        new Entity()
            .addComponent(new PhysicsComponent())
            .addComponent(new PhysicsMovementComponent())
            .addComponent(new ColliderComponent())
            .addComponent(new HitboxComponent().setLayer(PhysicsLayer.NPC))
            .addComponent(new CombatStatsComponent(50, 0))
            .addComponent(new TextureRenderComponent("images/enemies/npc_traveler.png"))
            // npc dialogue
            .addComponent(new DialogueComponent(dialoguetext))
            .addComponent(new DisplayDialogue("Traveler"))
            .addComponent(new QuestGiverComponent(player, 10))
            .addComponent(new DialogueProximityComponent(player, 2f));

    npc.getComponent(TextureRenderComponent.class).scaleEntity();
    npc.scaleHeight(playerHeight);

    // rayCastPositionScale is a fraction of the entity's own width (0 = sprite edge, 0.5 =
    // centre), so the fraction that lines the raycast up with the collider's edge is
    // 0.5 - colliderWidthFraction / 2, independent of the entity's absolute scale.
    float floorCollisionScale = 0.5f - colliderWidthFraction / 2f;
    // Wander range is a hard guarantee, not just a low-probability-of-falling value: at spawn
    // TRAVELER_NPC_SPAWN=(4, 13) in level1-greek.json, the entity's left edge (position.x) is
    // 1.782, on a floor patch spanning world x=[1.0, 3.0] (wall to the left, a non-solid ladder
    // tile from x=3.0). With this entity's width (0.937) and 0.9-fraction collider (0.843 wide,
    // 0.047 margin each side of the sprite), the tightest constraint is the ladder side: the
    // largest half-range that still keeps the collider's right edge >=0.05 inside the floor at
    // the worst-case wander target is (3.0 - 0.05 - 1.782 - 0.937 + 0.047) = 0.279. Using 0.25
    // (half-range) keeps a comfortable ~0.08 margin from the ladder edge, and an even larger
    // ~0.58 margin from the wall on the left, at the furthest wander position on either side.
    npc.addComponent(
        new AITaskComponent()
            .addTask(new PlatformWanderTask(new Vector2(0.5f, 0.5f), 2f, floorCollisionScale)));

    PhysicsUtils.setScaledCollider(npc, colliderWidthFraction, 0.7f);
    return npc;
  }

  private NPCFactory() {
    throw new IllegalStateException("Instantiating static util class");
  }
}
