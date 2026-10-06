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
import com.csse3200.game.entities.spawn.EntitySpawnRegistry;
import com.csse3200.game.files.FileLoader;
import com.csse3200.game.physics.PhysicsLayer;
import com.csse3200.game.physics.PhysicsUtils;
import com.csse3200.game.physics.components.ColliderComponent;
import com.csse3200.game.physics.components.HitboxComponent;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.physics.components.PhysicsMovementComponent;
import com.csse3200.game.rendering.AnimationRenderComponent;
import com.csse3200.game.rendering.EnemyWeaponAnimationComponent;
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
  private static final String SKELETON_SWORD_ATLAS_PATH = "images/enemy_weapons/enemy_sword.atlas";
  private static final String SKELETON_BOW_ATLAS_PATH = "images/enemy_weapons/enemy_bow.atlas";
  private static final String MINOTAUR_ATLAS_PATH = "images/enemies/minotaur.atlas";
  private static final String CYCLOPS_ATLAS_PATH = "images/enemies/cyclops.atlas";
  private static final String CENTAUR_ATLAS_PATH = "images/enemies/centaur.atlas";
  private static final String CERBERUS_ATLAS_PATH = "images/enemies/cerberus.atlas";
  private static final String MEDUSA_ATLAS_PATH = "images/enemies/gorgon.atlas";
  private static final String ZEUS_ATLAS_PATH = "images/enemies/zeus.atlas";
  // Harpy and ranged harpy both use harpy_y atlas
  private static final String HARPY_ATLAS_PATH = "images/enemies/harpy_y.atlas";
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

  // The speed effect counts down once per frame, so seconds are converted at 60 frames a second.
  private static final int TICKS_PER_SECOND = 60;

  // Task priorities: chase is 10, so ranged sits above it (the Skeleton pattern) and melee sits
  // above ranged so a close player is meleed rather than shot.
  private static final int MELEE_TASK_PRIORITY = 15;
  private static final int RANGED_TASK_PRIORITY = 12;
  private static final int LASER_TASK_PRIORITY = 13;

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

    boolean grounded = skeleton.getComponent(PhysicsMovementComponent.class).isGroundedMovement();

    addHazardRules(skeleton, grounded);

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

    boolean grounded =
        rangedSkeleton.getComponent(PhysicsMovementComponent.class).isGroundedMovement();

    addHazardRules(rangedSkeleton, grounded);

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
        .addComponent(new TouchAttackComponent(PhysicsLayer.PLAYER, config.charge.knockback))
        .addComponent(new ItemDropComponent())
        .addComponent(new EnemyDeathComponent())
        .addComponent(animator)
        .addComponent(new MinotaurAnimationController());

    minotaur.getComponent(AnimationRenderComponent.class).scaleEntity();

    minotaur.getComponent(ChargeComponent.class).setEndOnHit(config.charge.endOnHit);

    boolean grounded = minotaur.getComponent(PhysicsMovementComponent.class).isGroundedMovement();

    addHazardRules(minotaur, grounded);

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
        .addComponent(new TouchAttackComponent(PhysicsLayer.PLAYER, config.charge.knockback))
        .addComponent(new EnemyTypeComponent(EnemyType.CENTAUR))
        .addComponent(inventory)
        .addComponent(new ItemDropComponent())
        .addComponent(animator)
        .addComponent(new EnemyDeathComponent())
        .addComponent(new CentaurAnimationController());

    centaur.getComponent(ChargeComponent.class).setEndOnHit(config.charge.endOnHit);

    centaur
        .getComponent(RangedAttackComponent.class)
        .setProjectileSpeed(config.ranged.projectileSpeed);

    centaur.getComponent(AnimationRenderComponent.class).scaleEntity();

    boolean grounded = centaur.getComponent(PhysicsMovementComponent.class).isGroundedMovement();

    addHazardRules(centaur, grounded);

    // Attack from range instead of flying/chasing all the way onto the target - see
    // RangedAttackTask's Javadoc for why a higher priority than ChaseTask is what achieves this.
    centaur
        .getComponent(AITaskComponent.class)
        .addTask(
            new RangedAttackTask(
                target, RANGED_TASK_PRIORITY, config.ranged.range, ProjectileType.ARROW))
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
   * Creates a Cyclops (a mini-boss with a stomp, a lobbed rock and an eye laser).
   *
   * <p>Task priorities decide which attack runs, because the highest priority among the tasks whose
   * range covers the target wins and the ranged task ignores cooldown: the stomp (15) up close, the
   * laser (13) at middle range and the rock (12) only beyond the laser's range. So the rock is
   * never thrown at close range and the laser is never fired at long range.
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
    // constructors an armed enemy uses, via WeaponItem.natural(...). Each windup comes from the
    // config instead of cooldown - 1.
    WeaponItem fists = WeaponItem.natural("Cyclops Fists", config.baseAttack, config.melee.windup);
    WeaponItem rockThrow =
        WeaponItem.natural("Cyclops Rock Throw", config.baseAttack, config.rock.windup);
    WeaponItem laserBeam =
        WeaponItem.natural("Cyclops Laser", config.baseAttack, config.laser.windup);

    // Add necessary components to the entity
    cyclops
        .addComponent(new CombatStatsComponent(config.health, config.baseAttack))
        .addComponent(
            new MeleeAttackComponent(
                config.melee.range, config.melee.cooldown, config.melee.knockback, fists))
        .addComponent(
            new RockAttackComponent(
                config.rock.range,
                config.rock.cooldown,
                config.rock.knockback,
                rockThrow,
                config.rock.spawnHeightFraction,
                config.rock.damageMultiplier))
        .addComponent(
            new LaserAttackComponent(
                config.laser.range,
                config.laser.cooldown,
                config.laser.knockback,
                laserBeam,
                config.laser.spawnHeightFraction,
                config.laser.damageMultiplier))
        .addComponent(new EnemyTypeComponent(EnemyType.CYCLOPS))
        .addComponent(inventory)
        .addComponent(new ItemDropComponent())
        .addComponent(new EnemyDeathComponent())
        .addComponent(animator)
        .addComponent(weaponAnimator)
        .addComponent(new CyclopsAnimationController());

    // Look the components up by their own class: the base class lookup does not find subclasses.
    cyclops.getComponent(RockAttackComponent.class).setProjectileSpeed(config.rock.projectileSpeed);
    cyclops
        .getComponent(LaserAttackComponent.class)
        .setProjectileSpeed(config.laser.projectileSpeed);

    cyclops.getComponent(AnimationRenderComponent.class).scaleEntity();

    boolean grounded = cyclops.getComponent(PhysicsMovementComponent.class).isGroundedMovement();

    addHazardRules(cyclops, grounded);

    // All three sit above the chase task (priority 10), so the Cyclops stops to attack.
    cyclops
        .getComponent(AITaskComponent.class)
        .addTask(new MeleeAttackTask(target, MELEE_TASK_PRIORITY, config.melee.range))
        .addTask(
            new RangedAttackTask(
                target,
                LASER_TASK_PRIORITY,
                config.laser.range,
                ProjectileType.ARROW,
                "laserAttack"))
        .addTask(
            new RangedAttackTask(
                target,
                RANGED_TASK_PRIORITY,
                config.rock.range,
                ProjectileType.ARROW,
                "rockAttack"));

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

    // Medusa uses gorgon atlas with idle, walk, attack, and death animations
    AnimationRenderComponent animator =
        new AnimationRenderComponent(loadIndependentAtlas(MEDUSA_ATLAS_PATH), true);
    animator.addAnimation("gorgon_idle_l", 0.1f, Animation.PlayMode.LOOP);
    animator.addAnimation("gorgon_idle_r", 0.1f, Animation.PlayMode.LOOP);
    animator.addAnimation("gorgon_walk_l", 0.1f, Animation.PlayMode.LOOP);
    animator.addAnimation("gorgon_walk_r", 0.1f, Animation.PlayMode.LOOP);
    animator.addAnimation("gorgon_attack_l", 0.1f, Animation.PlayMode.NORMAL);
    animator.addAnimation("gorgon_attack_r", 0.1f, Animation.PlayMode.NORMAL);
    animator.addAnimation("gorgon_dead_l", 0.15f, Animation.PlayMode.NORMAL);
    animator.addAnimation("gorgon_dead_r", 0.15f, Animation.PlayMode.NORMAL);

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
        .addComponent(new MedusaAnimationController());

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

    boolean grounded = medusa.getComponent(PhysicsMovementComponent.class).isGroundedMovement();

    addHazardRules(medusa, grounded);

    animator.scaleEntity();
    medusa
        .getComponent(AITaskComponent.class)
        .addTask(
            new BoundedPlatformWanderTask(config.pacing.radius, 2f, (scale * collisionScale.x) / 2))
        .addTask(
            new RangedAttackTask(
                target, RANGED_TASK_PRIORITY, config.ranged.range, ProjectileType.ARROW))
        .addTask(new MeleeAttackTask(target, MELEE_TASK_PRIORITY, config.melee.range));

    medusa.setScale(scale, scale * (91f / 128f));
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

    // Cerberus uses her own atlas with left and right walking animations
    AnimationRenderComponent animator =
        new AnimationRenderComponent(loadIndependentAtlas(CERBERUS_ATLAS_PATH), true);
    animator.addAnimation("cerberus_l", 0.1f, Animation.PlayMode.LOOP);
    animator.addAnimation("cerberus_r", 0.1f, Animation.PlayMode.LOOP);

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
        .addComponent(new CerberusAnimationController());

    boolean grounded = cerberus.getComponent(PhysicsMovementComponent.class).isGroundedMovement();

    addHazardRules(cerberus, grounded);

    animator.scaleEntity();
    cerberus
        .getComponent(AITaskComponent.class)
        .addTask(new MeleeAttackTask(target, MELEE_TASK_PRIORITY, config.melee.range));

    cerberus.setScale(scale, scale * (96f / 150f));
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

    // Zeus uses zeus atlas with idle, walk, strike, slam (lightning cast), and death animations
    AnimationRenderComponent animator =
        new AnimationRenderComponent(loadIndependentAtlas(ZEUS_ATLAS_PATH), true);
    animator.addAnimation("zeus_idle_l", 0.1f, Animation.PlayMode.LOOP);
    animator.addAnimation("zeus_idle_r", 0.1f, Animation.PlayMode.LOOP);
    animator.addAnimation("zeus_walk_l", 0.08f, Animation.PlayMode.LOOP);
    animator.addAnimation("zeus_walk_r", 0.08f, Animation.PlayMode.LOOP);
    animator.addAnimation("zeus_p_strike_l", 0.08f, Animation.PlayMode.NORMAL);
    animator.addAnimation("zeus_p_strike_r", 0.08f, Animation.PlayMode.NORMAL);
    animator.addAnimation("zeus_slam_l", 0.1f, Animation.PlayMode.NORMAL);
    animator.addAnimation("zeus_slam_r", 0.1f, Animation.PlayMode.NORMAL);
    animator.addAnimation("zeus_death_l", 0.15f, Animation.PlayMode.NORMAL);
    animator.addAnimation("zeus_death_r", 0.15f, Animation.PlayMode.NORMAL);

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
        .addComponent(new ZeusAnimationController());

    zeus.getComponent(RangedAttackComponent.class)
        .setProjectileSpeed(config.ranged.projectileSpeed);
    // The aimed flag has no effect on lightning (it falls from above the target) but is read from
    // the config so every ranged enemy is wired the same way.
    zeus.getComponent(RangedAttackComponent.class).setAimed(config.ranged.aimed);

    boolean grounded = zeus.getComponent(PhysicsMovementComponent.class).isGroundedMovement();

    addHazardRules(zeus, grounded);

    animator.scaleEntity();
    zeus.getComponent(AITaskComponent.class)
        .addTask(new MeleeAttackTask(target, MELEE_TASK_PRIORITY, config.melee.range))
        .addTask(
            new RangedAttackTask(
                target, RANGED_TASK_PRIORITY, config.ranged.range, ProjectileType.LIGHTNING));

    zeus.setScale(scale, scale * (68f / 37f));
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

    // Harpy uses harpy_y atlas with left and right movement animations
    AnimationRenderComponent animator =
        new AnimationRenderComponent(loadIndependentAtlas(HARPY_ATLAS_PATH), true);
    animator.addAnimation("harpy_y_l", 0.1f, Animation.PlayMode.LOOP);
    animator.addAnimation("harpy_y_r", 0.1f, Animation.PlayMode.LOOP);

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
        .addComponent(new HarpyAnimationController());

    animator.scaleEntity();
    harpy
        .getComponent(AITaskComponent.class)
        .addTask(new MeleeAttackTask(target, MELEE_TASK_PRIORITY, config.melee.range));

    harpy.setScale(scale, scale * (25f / 38f));
    PhysicsUtils.setScaledCollider(harpy, collisionScale.x, collisionScale.y);
    return harpy;
  }

  /**
   * Creates a ranged Harpy: a flying enemy armed with a tier-1 bow that fires aimed arrows,
   * mirroring the ranged Skeleton. Aimed arrows let it hit a player above or below it.
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

    // Ranged Harpy uses harpy_y atlas with left and right movement animations
    AnimationRenderComponent animator =
        new AnimationRenderComponent(loadIndependentAtlas(HARPY_ATLAS_PATH), true);
    animator.addAnimation("harpy_y_l", 0.1f, Animation.PlayMode.LOOP);
    animator.addAnimation("harpy_y_r", 0.1f, Animation.PlayMode.LOOP);

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
        .addComponent(new HarpyAnimationController());

    RangedAttackComponent ranged = harpy.getComponent(RangedAttackComponent.class);
    ranged.setProjectileSpeed(config.ranged.projectileSpeed);
    ranged.setAimed(config.ranged.aimed);

    animator.scaleEntity();
    harpy
        .getComponent(AITaskComponent.class)
        .addTask(
            new RangedAttackTask(
                target, MELEE_TASK_PRIORITY, config.ranged.range, ProjectileType.ARROW));

    harpy.setScale(scale, scale * (25f / 38f));
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
            .addComponent(new FlightComponent(flightDamping))
            .addComponent(new HitboxComponent().setLayer(PhysicsLayer.NPC))
            .addComponent(aiComponent);

    PhysicsUtils.setScaledCollider(npc, 0.9f, 0.7f);
    // Not grounded, so the wander and chase tasks steer in both axes.
    npc.getComponent(PhysicsMovementComponent.class).setGroundedMovement(false);
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

  /**
   * Gives an enemy the hazard rules every enemy follows. Every enemy takes hazard damage. Only
   * grounded enemies also avoid hazards, because a flyer would only ever be stopped by a tile it
   * can fly over.
   *
   * @param npc the enemy being built, not yet created
   * @param grounded true if the enemy walks on the ground, false if it flies
   */
  private static void addHazardRules(Entity npc, boolean grounded) {
    npc.addComponent(new HazardContactDamageComponent());
    if (grounded) {
      npc.addComponent(new HazardAvoidanceComponent());
    }
  }

  private NPCFactory() {
    throw new IllegalStateException("Instantiating static util class");
  }
}
