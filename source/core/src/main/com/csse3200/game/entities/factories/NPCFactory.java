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
  private static final String CENTAUR_ATLAS_PATH = "images/enemies/centaur.atlas";
  private static final String CERBERUS_ATLAS_PATH = "images/enemies/cerberus.atlas";
  private static final String MEDUSA_ATLAS_PATH = "images/enemies/medusa.atlas";
  private static final String ZEUS_ATLAS_PATH = "images/enemies/zeus.atlas";
  // this could be the ranged harpy and the above is the melee one to differentiate
  // 0on screen but if too hard all g.
  private static final String HARPY_ATLAS_PATH = "images/enemies/harpy.atlas";

  private static final NPCConfigs configs =
      FileLoader.readClass(NPCConfigs.class, "configs/NPCs.json");

  // The speed effect counts down once per frame, so seconds are converted at 60 frames a second.
  private static final int TICKS_PER_SECOND = 60;

  // Task priorities: chase is 10, so ranged sits above it (the Skeleton pattern) and melee sits
  // above ranged so a close player is meleed rather than shot.
  private static final int MELEE_TASK_PRIORITY = 15;
  private static final int RANGED_TASK_PRIORITY = 12;

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
    AnimationRenderComponent animator =
        new AnimationRenderComponent(loadIndependentAtlas(CENTAUR_ATLAS_PATH), true);
    animator.addAnimation("centaur_idle_l", 0.1f, Animation.PlayMode.LOOP);
    animator.addAnimation("centaur_idle_r", 0.1f, Animation.PlayMode.LOOP);
    animator.addAnimation("centaur_run_l", 0.1f, Animation.PlayMode.LOOP);
    animator.addAnimation("centaur_run_r", 0.1f, Animation.PlayMode.LOOP);
    animator.addAnimation("centaur_swing_l", 0.1f, Animation.PlayMode.NORMAL);
    animator.addAnimation("centaur_swing_r", 0.1f, Animation.PlayMode.NORMAL);
    animator.addAnimation("centaur_death_l", 0.1f, Animation.PlayMode.NORMAL);
    animator.addAnimation("centaur_death_r", 0.1f, Animation.PlayMode.NORMAL);

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
        .addComponent(new CentaurAnimationController());

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

    centaur.setScale(scale, scale * (53f / 66f));
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
    AnimationRenderComponent animator =
        new AnimationRenderComponent(loadIndependentAtlas(CYCLOPS_ATLAS_PATH), true);
    animator.addAnimation("cyclops_idle_l", 0.1f, Animation.PlayMode.LOOP);
    animator.addAnimation("cyclops_idle_r", 0.1f, Animation.PlayMode.LOOP);
    animator.addAnimation("cyclops_walk_l", 0.1f, Animation.PlayMode.LOOP);
    animator.addAnimation("cyclops_walk_r", 0.1f, Animation.PlayMode.LOOP);
    animator.addAnimation("cyclops_rock_l", 0.1f, Animation.PlayMode.NORMAL);
    animator.addAnimation("cyclops_rock_r", 0.1f, Animation.PlayMode.NORMAL);
    animator.addAnimation("cyclops_stomp_l", 0.1f, Animation.PlayMode.NORMAL);
    animator.addAnimation("cyclops_stomp_r", 0.1f, Animation.PlayMode.NORMAL);
    animator.addAnimation("cyclops_death_l", 0.1f, Animation.PlayMode.NORMAL);
    animator.addAnimation("cyclops_death_r", 0.1f, Animation.PlayMode.NORMAL);
    animator.addAnimation("cyclops_taunt_l", 0.1f, Animation.PlayMode.NORMAL);
    animator.addAnimation("cyclops_taunt_r", 0.1f, Animation.PlayMode.NORMAL);
    animator.addAnimation("cyclops_eye_scratch_l", 0.1f, Animation.PlayMode.NORMAL);
    animator.addAnimation("cyclops_eye_scratch_r", 0.1f, Animation.PlayMode.NORMAL);

    // Configure weapon animation component for laser overlay
    EnemyWeaponAnimationComponent weaponAnimator =
        new EnemyWeaponAnimationComponent(loadIndependentAtlas(CYCLOPS_ATLAS_PATH));
    weaponAnimator.setScaleMultiplier(1.0f);
    weaponAnimator.setOffset(0f, 0f);
    weaponAnimator.addAnimation("cyclops_laser_l", 0.1f, Animation.PlayMode.NORMAL);
    weaponAnimator.addAnimation("cyclops_laser_r", 0.1f, Animation.PlayMode.NORMAL);

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
        .addComponent(weaponAnimator)
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
   * Creates Medusa, a mini-boss that paces on the spot, bites up close and petrifies the player
   * with a ranged gaze.
   *
   * <p>PLACEHOLDER BUILD: made only from existing classes. She uses the Cyclops atlas and animation
   * controller, a natural melee weapon and a natural gaze weapon (like the Cyclops), and freezes
   * the player through the existing {@code "applySpeedEffect"} event (the same one lightning uses)
   * instead of a petrify component.
   *
   * @param target entity to attack (the player); must not be null
   * @return Medusa, ready to register
   * @throws IllegalArgumentException if target is null or a design rule is broken
   */
  public static Entity createMedusa(Entity target) {
    if (target == null) {
      throw new IllegalArgumentException("Medusa needs a target");
    }
    MedusaConfig config = configs.medusa;
    validateMeleeRangeBelowRangedRange(config.melee.range, config.ranged.range);
    // The attack that petrifies must wait at least twice as long as the petrify lasts.
    validateCooldownCoversEffect(config.ranged.cooldown, config.petrify.duration);

    float scale = 2.0f;
    Vector2 collisionScale = new Vector2(0.4f, 0.5f);
    // TODO (S7): createBaseStationaryNPC() has no chase task and paces with a plain
    // PlatformWanderTask, which can drift because it re-anchors its range each time it restarts.
    // Replace the wander task below with BoundedPlatformWanderTask(pacingRadius, 2f, ...).
    Entity medusa = createBaseStationaryNPC();

    // More gold because no weapon drops (natural weapons never go in the inventory).
    InventoryComponent inventory = new InventoryComponent(12);

    // TODO (ART): Medusa needs her own atlas (idle, walk, melee, gaze, left and right). Until it
    // exists she borrows the Cyclops atlas, animations and controller.
    AnimationRenderComponent animator =
        new AnimationRenderComponent(loadIndependentAtlas(CYCLOPS_ATLAS_PATH), true);
    animator.addAnimation("cyclops_idle_l", 0.1f, Animation.PlayMode.LOOP);
    animator.addAnimation("cyclops_idle_r", 0.1f, Animation.PlayMode.LOOP);
    animator.addAnimation("cyclops_walk_l", 0.1f, Animation.PlayMode.LOOP);
    animator.addAnimation("cyclops_walk_r", 0.1f, Animation.PlayMode.LOOP);

    WeaponItem bite = WeaponItem.natural("Medusa Bite", config.baseAttack, config.melee.windup);
    WeaponItem gaze = WeaponItem.natural("Medusa Gaze", config.baseAttack, config.ranged.windup);

    medusa
        .addComponent(new CombatStatsComponent(config.health, config.baseAttack))
        .addComponent(
            new MeleeAttackComponent(
                config.melee.range, config.melee.cooldown, config.melee.knockback, bite))
        .addComponent(
            new RangedAttackComponent(
                config.ranged.range, config.ranged.cooldown, config.ranged.knockback, gaze))
        .addComponent(new EnemyTypeComponent(EnemyType.MEDUSA))
        .addComponent(inventory)
        .addComponent(new ItemDropComponent())
        .addComponent(new EnemyDeathComponent())
        .addComponent(animator)
        // TODO (ART): replace with MedusaAnimationController.
        .addComponent(new CyclopsAnimationController());

    medusa
        .getComponent(RangedAttackComponent.class)
        .setProjectileSpeed(config.ranged.projectileSpeed);

    // TODO (S7): replace this listener with a PetrifyEffectComponent on Medusa, wired to the same
    // "rangedAttackHit" event, with the duration read from MedusaConfig.petrify.
    int petrifyTicks = Math.round(config.petrify.duration * TICKS_PER_SECOND);
    medusa
        .getEvents()
        .addListener(
            "rangedAttackHit",
            (Entity hit) -> hit.getEvents().trigger("applySpeedEffect", petrifyTicks, 0f));

    animator.scaleEntity();
    medusa
        .getComponent(AITaskComponent.class)
        .addTask(
            new PlatformWanderTask(
                new Vector2(config.pacing.radius * 2f, 2f), 2f, (scale * collisionScale.x) / 2))
        .addTask(
            new RangedAttackTask(
                target, RANGED_TASK_PRIORITY, config.ranged.range, ProjectileType.ARROW))
        .addTask(new MeleeAttackTask(target, MELEE_TASK_PRIORITY, config.melee.range));

    medusa.setScale(scale, scale * (48f / 64f));
    PhysicsUtils.setScaledCollider(medusa, collisionScale.x, collisionScale.y);
    return medusa;
  }

  /**
   * Creates Cerberus, a stationary mini-boss that bites anything inside its reach, very often.
   *
   * <p>PLACEHOLDER BUILD: skeleton atlas and animation controller, one natural melee weapon. The
   * melee task's range is her effective aggro radius, because she has no chase task.
   *
   * @param target entity to attack; must not be null
   * @return Cerberus, ready to register
   * @throws IllegalArgumentException if target is null
   */
  public static Entity createCerberus(Entity target) {
    if (target == null) {
      throw new IllegalArgumentException("Cerberus needs a target");
    }
    CerberusConfig config = configs.cerberus;
    float scale = 1.5f;
    Vector2 collisionScale = new Vector2(0.45f, 0.6f);
    Entity cerberus = createBaseStationaryNPC();

    InventoryComponent inventory = new InventoryComponent(12);

    // TODO (ART): Cerberus needs her own atlas (idle and attack, left and right; she cannot
    // walk). Until it exists she borrows the skeleton atlas, animations and controller.
    AnimationRenderComponent animator =
        new AnimationRenderComponent(loadIndependentAtlas(SKELETON_ATLAS_PATH), true);
    animator.addAnimation("idlel", 0.1f, Animation.PlayMode.LOOP);
    animator.addAnimation("idler", 0.1f, Animation.PlayMode.LOOP);
    animator.addAnimation("walkl", 0.1f, Animation.PlayMode.LOOP);
    animator.addAnimation("walkr", 0.1f, Animation.PlayMode.LOOP);

    WeaponItem bite = WeaponItem.natural("Cerberus Bite", config.baseAttack, config.melee.windup);

    cerberus
        .addComponent(new CombatStatsComponent(config.health, config.baseAttack))
        .addComponent(
            new MeleeAttackComponent(
                config.melee.range, config.melee.cooldown, config.melee.knockback, bite))
        .addComponent(new EnemyTypeComponent(EnemyType.CERBERUS))
        .addComponent(inventory)
        .addComponent(new ItemDropComponent())
        .addComponent(new EnemyDeathComponent())
        .addComponent(animator)
        // TODO (ART): replace with CerberusAnimationController.
        .addComponent(new SkeletonAnimationController());

    animator.scaleEntity();
    cerberus
        .getComponent(AITaskComponent.class)
        .addTask(new MeleeAttackTask(target, MELEE_TASK_PRIORITY, config.melee.range));

    cerberus.setScale(scale, scale);
    PhysicsUtils.setScaledCollider(cerberus, collisionScale.x, collisionScale.y);
    return cerberus;
  }

  /**
   * Creates Zeus, a mini-boss that fights with a tier-2 sword up close and throws lightning from
   * range. Moves and chases like the other platformer enemies.
   *
   * <p>PLACEHOLDER BUILD: Cyclops atlas and animation controller. His sword is a real weapon (so it
   * drops on death); his lightning is a natural ranged weapon firing the existing LIGHTNING
   * projectile.
   *
   * @param target entity to chase and attack; must not be null
   * @return Zeus, ready to register
   * @throws IllegalArgumentException if target is null or a design rule is broken
   */
  public static Entity createZeus(Entity target) {
    if (target == null) {
      throw new IllegalArgumentException("Zeus needs a target");
    }
    ZeusConfig config = configs.zeus;
    validateMeleeRangeBelowRangedRange(config.melee.range, config.ranged.range);

    float scale = 2.0f;
    Vector2 collisionScale = new Vector2(0.4f, 0.5f);
    Entity zeus = createBasePlatformerNPC(target, (scale * collisionScale.x) / 2);

    InventoryComponent inventory = new InventoryComponent(3);
    inventory.addItem(new WeaponGenerator().generateWeapon(WeaponType.SWORD, 2));

    // TODO (ART): Zeus needs his own atlas (idle, walk, sword swing, lightning cast). Until it
    // exists he borrows the Cyclops atlas, animations and controller.
    AnimationRenderComponent animator =
        new AnimationRenderComponent(loadIndependentAtlas(CYCLOPS_ATLAS_PATH), true);
    animator.addAnimation("cyclops_idle_l", 0.1f, Animation.PlayMode.LOOP);
    animator.addAnimation("cyclops_idle_r", 0.1f, Animation.PlayMode.LOOP);
    animator.addAnimation("cyclops_walk_l", 0.1f, Animation.PlayMode.LOOP);
    animator.addAnimation("cyclops_walk_r", 0.1f, Animation.PlayMode.LOOP);

    WeaponItem lightning =
        WeaponItem.natural("Zeus Lightning", config.baseAttack, config.ranged.windup);

    zeus.addComponent(new CombatStatsComponent(config.health, config.baseAttack))
        .addComponent(
            new MeleeAttackComponent(
                config.melee.range,
                config.melee.cooldown,
                config.melee.knockback,
                (WeaponItem) inventory.getItem(1)))
        .addComponent(
            new RangedAttackComponent(
                config.ranged.range, config.ranged.cooldown, config.ranged.knockback, lightning))
        .addComponent(new EnemyTypeComponent(EnemyType.ZEUS))
        .addComponent(inventory)
        .addComponent(new ItemDropComponent())
        .addComponent(new EnemyDeathComponent())
        .addComponent(animator)
        // TODO (ART): replace with ZeusAnimationController.
        .addComponent(new CyclopsAnimationController());

    zeus.getComponent(RangedAttackComponent.class)
        .setProjectileSpeed(config.ranged.projectileSpeed);
    // The aimed flag has no effect on lightning (it falls from above the target) but is read from
    // the config so every ranged enemy is wired the same way.
    zeus.getComponent(RangedAttackComponent.class).setAimed(config.ranged.aimed);

    animator.scaleEntity();
    zeus.getComponent(AITaskComponent.class)
        .addTask(new MeleeAttackTask(target, MELEE_TASK_PRIORITY, config.melee.range))
        .addTask(
            new RangedAttackTask(
                target, RANGED_TASK_PRIORITY, config.ranged.range, ProjectileType.LIGHTNING));

    zeus.setScale(scale, scale * (48f / 64f));
    PhysicsUtils.setScaledCollider(zeus, collisionScale.x, collisionScale.y);
    return zeus;
  }

  /**
   * Creates a melee Harpy: a flying enemy armed with a tier-1 sword, mirroring the melee Skeleton.
   *
   * <p>PLACEHOLDER BUILD: skeleton atlas and animation controller.
   *
   * @param target entity to chase; must not be null
   * @return a flying melee enemy
   * @throws IllegalArgumentException if target is null
   */
  public static Entity createHarpy(Entity target) {
    if (target == null) {
      throw new IllegalArgumentException("A Harpy needs a target");
    }
    HarpyConfig config = configs.harpy;
    float scale = 1.0f;
    Vector2 collisionScale = new Vector2(0.45f, 0.6f);
    Entity harpy = createBaseFlyingNPC(target, config.flightDamping);

    InventoryComponent inventory = new InventoryComponent(3);
    inventory.addItem(new WeaponGenerator().generateWeapon(WeaponType.SWORD, 1));

    // TODO (ART): the Harpy needs its own atlas (flap and attack, left and right). Until it
    // exists it borrows the skeleton atlas, animations and controller.
    AnimationRenderComponent animator =
        new AnimationRenderComponent(loadIndependentAtlas(SKELETON_ATLAS_PATH), true);
    animator.addAnimation("idlel", 0.1f, Animation.PlayMode.LOOP);
    animator.addAnimation("idler", 0.1f, Animation.PlayMode.LOOP);
    animator.addAnimation("walkl", 0.1f, Animation.PlayMode.LOOP);
    animator.addAnimation("walkr", 0.1f, Animation.PlayMode.LOOP);

    harpy
        .addComponent(new CombatStatsComponent(config.health, config.baseAttack))
        .addComponent(
            new MeleeAttackComponent(
                config.melee.range,
                config.melee.cooldown,
                config.melee.knockback,
                (WeaponItem) inventory.getItem(1)))
        .addComponent(new EnemyTypeComponent(EnemyType.HARPY))
        .addComponent(inventory)
        .addComponent(new ItemDropComponent())
        .addComponent(new EnemyDeathComponent())
        .addComponent(animator)
        // TODO (ART): replace with HarpyAnimationController.
        .addComponent(new SkeletonAnimationController());

    animator.scaleEntity();
    harpy
        .getComponent(AITaskComponent.class)
        .addTask(new MeleeAttackTask(target, MELEE_TASK_PRIORITY, config.melee.range));

    harpy.setScale(scale, scale);
    PhysicsUtils.setScaledCollider(harpy, collisionScale.x, collisionScale.y);
    return harpy;
  }

  /**
   * Creates a ranged Harpy: a flying enemy armed with a tier-1 bow that fires aimed arrows,
   * mirroring the ranged Skeleton. Aimed arrows let it hit a player above or below it.
   *
   * <p>PLACEHOLDER BUILD: skeleton atlas and animation controller.
   *
   * @param target entity to chase and shoot; must not be null
   * @return a flying ranged enemy
   * @throws IllegalArgumentException if target is null
   */
  public static Entity createRangedHarpy(Entity target) {
    if (target == null) {
      throw new IllegalArgumentException("A Harpy needs a target");
    }
    RangedHarpyConfig config = configs.rangedHarpy;
    float scale = 1.0f;
    Vector2 collisionScale = new Vector2(0.45f, 0.6f);
    Entity harpy = createBaseFlyingNPC(target, config.flightDamping);

    InventoryComponent inventory = new InventoryComponent(3);
    inventory.addItem(new WeaponGenerator().generateWeapon(WeaponType.BOW, 1));

    // TODO (ART): same placeholder atlas as the melee Harpy.
    AnimationRenderComponent animator =
        new AnimationRenderComponent(loadIndependentAtlas(SKELETON_ATLAS_PATH), true);
    animator.addAnimation("idlel", 0.1f, Animation.PlayMode.LOOP);
    animator.addAnimation("idler", 0.1f, Animation.PlayMode.LOOP);
    animator.addAnimation("walkl", 0.1f, Animation.PlayMode.LOOP);
    animator.addAnimation("walkr", 0.1f, Animation.PlayMode.LOOP);

    harpy
        .addComponent(new CombatStatsComponent(config.health, config.baseAttack))
        .addComponent(
            new RangedAttackComponent(
                config.ranged.range,
                config.ranged.cooldown,
                config.ranged.knockback,
                (WeaponItem) inventory.getItem(1)))
        .addComponent(new EnemyTypeComponent(EnemyType.RANGED_HARPY))
        .addComponent(inventory)
        .addComponent(new ItemDropComponent())
        .addComponent(new EnemyDeathComponent())
        .addComponent(animator)
        // TODO (ART): replace with HarpyAnimationController.
        .addComponent(new SkeletonAnimationController());

    RangedAttackComponent ranged = harpy.getComponent(RangedAttackComponent.class);
    ranged.setProjectileSpeed(config.ranged.projectileSpeed);
    ranged.setAimed(config.ranged.aimed);

    animator.scaleEntity();
    harpy
        .getComponent(AITaskComponent.class)
        .addTask(
            new RangedAttackTask(
                target, MELEE_TASK_PRIORITY, config.ranged.range, ProjectileType.ARROW));

    harpy.setScale(scale, scale);
    PhysicsUtils.setScaledCollider(harpy, collisionScale.x, collisionScale.y);
    return harpy;
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

  /**
   * Creates a grounded NPC with physics, collision and an EMPTY task list, for enemies that never
   * chase (Medusa, Cerberus). The caller adds whatever tasks it needs. Differs from {@code
   * createBasePlatformerNPC} by having no wander, chase or default melee task at all.
   *
   * @return a base entity with grounded movement switched on
   */
  private static Entity createBaseStationaryNPC() {
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
   * Creates an NPC that flies: wander and chase tasks like the ground NPC, but free to move in both
   * axes and unaffected by gravity. No touch damage (attacks are attack components).
   *
   * @param target entity to chase
   * @param flightDamping linear damping on the physics body so knockback fades; not negative
   * @return a flying base entity
   */
  private static Entity createBaseFlyingNPC(Entity target, float flightDamping) {
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

    PhysicsUtils.setScaledCollider(npc, 0.9f, 0.7f);
    // Not grounded, so the wander and chase tasks steer in both axes.
    npc.getComponent(PhysicsMovementComponent.class).setGroundedMovement(false);
    // TODO (S7): replace these two lines with .addComponent(new FlightComponent(damping)) above.
    npc.getComponent(PhysicsComponent.class).getBody().setGravityScale(0f);
    npc.getComponent(PhysicsComponent.class).getBody().setLinearDamping(flightDamping);
    return npc;
  }

  /**
   * An enemy with both attacks must out-reach itself with the ranged one: melee range strictly
   * below ranged range. Otherwise the task priorities that switch between the two attacks behave
   * confusingly instead of failing clearly.
   *
   * @param meleeRange the melee attack's range
   * @param rangedRange the ranged attack's range
   * @throws IllegalArgumentException if meleeRange is not strictly less than rangedRange
   */
  private static void validateMeleeRangeBelowRangedRange(float meleeRange, float rangedRange) {
    if (!(meleeRange < rangedRange)) {
      throw new IllegalArgumentException(
          "melee range " + meleeRange + " must be below ranged range " + rangedRange);
    }
  }

  /**
   * Team design rule: an attack that applies a timed effect to the player must have a cooldown of
   * at least twice the effect's duration, so the player always gets a window to act.
   *
   * @param cooldown the attack's cooldown in seconds
   * @param effectSeconds the effect's duration in seconds
   * @throws IllegalArgumentException if cooldown is less than twice effectSeconds (exactly twice is
   *     allowed)
   */
  private static void validateCooldownCoversEffect(float cooldown, float effectSeconds) {
    if (cooldown < 2f * effectSeconds) {
      throw new IllegalArgumentException(
          "cooldown " + cooldown + " must be at least " + (2f * effectSeconds));
    }
  }

  private NPCFactory() {
    throw new IllegalStateException("Instantiating static util class");
  }
}
