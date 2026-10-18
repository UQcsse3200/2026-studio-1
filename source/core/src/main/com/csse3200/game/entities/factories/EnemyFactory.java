package com.csse3200.game.entities.factories;

import static com.badlogic.gdx.graphics.g2d.Animation.PlayMode.LOOP;
import static com.badlogic.gdx.graphics.g2d.Animation.PlayMode.NORMAL;
import static com.csse3200.game.components.loot.WeaponType.*;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.csse3200.game.ai.tasks.AITaskComponent;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.EnemyType;
import com.csse3200.game.components.enemies.*;
import com.csse3200.game.components.enemies.animation.*;
import com.csse3200.game.components.enemies.attacks.*;
import com.csse3200.game.components.enemies.attacks.RangedAttackComponent;
import com.csse3200.game.components.enemies.tasks.*;
import com.csse3200.game.components.loot.*;
import com.csse3200.game.components.npc.NameComponent;
import com.csse3200.game.components.npc.NameGenerator;
import com.csse3200.game.components.player.InventoryComponent;
import com.csse3200.game.components.player.ItemDropComponent;
import com.csse3200.game.components.projectile.ProjectileType;
import com.csse3200.game.components.tasks.ChaseTask;
import com.csse3200.game.components.tasks.WanderTask;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.configs.BaseEntityConfig;
import com.csse3200.game.entities.configs.NPCConfigs;
import com.csse3200.game.entities.configs.enemies.*;
import com.csse3200.game.entities.configs.enemies.attacks.ChargeAttackConfig;
import com.csse3200.game.entities.configs.enemies.attacks.MeleeAttackConfig;
import com.csse3200.game.entities.configs.enemies.attacks.RangedAttackConfig;
import com.csse3200.game.entities.spawn.EntitySpawnRegistry;
import com.csse3200.game.files.FileLoader;
import com.csse3200.game.perks.tortoises.TortoiseDropComponent;
import com.csse3200.game.physics.PhysicsLayer;
import com.csse3200.game.physics.PhysicsUtils;
import com.csse3200.game.physics.components.ColliderComponent;
import com.csse3200.game.physics.components.HitboxComponent;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.physics.components.PhysicsMovementComponent;
import com.csse3200.game.rendering.AnimationRenderComponent;
import com.csse3200.game.rendering.EnemyWeaponAnimationComponent;

/**
 * Builds every enemy in the game. Each creator reads that enemy's settings from {@code
 * configs/NPCs.json} and returns an entity that is ready to be positioned and spawned. Friendly
 * NPCs are built by {@code com.csse3200.game.entities.factories.NPCFactory}; neither factory calls
 * the other.
 *
 * <p>Every method is static and the class cannot be instantiated. The private helpers at the bottom
 * hold the parts the ten creators share: argument checks, the three base bodies, loot, animation,
 * attack components, AI tasks, size and collider.
 */
public final class EnemyFactory {
  // ---------- Already in the file (keep as they are) ----------
  /** Skeleton, Ranged Skeleton. */
  private static final String SKELETON_ATLAS_PATH = "images/enemies/skeleton.atlas";

  /** Skeleton sword overlay. */
  private static final String SKELETON_SWORD_ATLAS_PATH = "images/enemy_weapons/enemy_sword.atlas";

  /** Ranged Skeleton bow overlay. */
  private static final String SKELETON_BOW_ATLAS_PATH = "images/enemy_weapons/enemy_bow.atlas";

  /** Minotaur. */
  private static final String MINOTAUR_ATLAS_PATH = "images/enemies/minotaur.atlas";

  /** Cyclops body and laser overlay. */
  private static final String CYCLOPS_ATLAS_PATH = "images/enemies/cyclops.atlas";

  /** Centaur. */
  private static final String CENTAUR_ATLAS_PATH = "images/enemies/centaur.atlas";

  /** Cerberus. */
  private static final String CERBERUS_ATLAS_PATH = "images/enemies/cerberus.atlas";

  /** Medusa (the file is called gorgon). */
  private static final String MEDUSA_ATLAS_PATH = "images/enemies/gorgon.atlas";

  /** Zeus. */
  private static final String ZEUS_ATLAS_PATH = "images/enemies/zeus.atlas";

  /** Harpy, Ranged Harpy. */
  private static final String HARPY_ATLAS_PATH = "images/enemies/harpy_y.atlas";

  /** NPC settings read once from {@code configs/NPCs.json}. */
  private static final NPCConfigs configs =
      FileLoader.readClass(NPCConfigs.class, "configs/NPCs.json");

  /** Every melee task. */
  private static final int MELEE_TASK_PRIORITY = 15;

  /** Centaur, Cyclops rock, Medusa, Zeus. */
  private static final int RANGED_TASK_PRIORITY = 12;

  /** Cyclops laser. */
  private static final int LASER_TASK_PRIORITY = 13;

  // ---------- AI task numbers (currently bare numbers in the code) ----------
  /** The chase task in the platformer and flying bases. */
  private static final int CHASE_TASK_PRIORITY = 10;

  /** Minotaur and Centaur charge task, active priority. */
  private static final int CHARGE_TASK_PRIORITY = 16;

  /** Charge task, inactive priority (the last argument). */
  private static final int CHARGE_TASK_IDLE_PRIORITY = -1;

  /**
   * Ranged Skeleton (bare 15 today) and Ranged Harpy (uses MELEE_TASK_PRIORITY today). New name for
   * an enemy whose ranged attack is its main attack. Same value, so no behaviour change.
   */
  private static final int PRIMARY_RANGED_TASK_PRIORITY = 15;

  /** ChaseTask view distance, all three bases. */
  private static final float CHASE_VIEW_DISTANCE = 3f;

  /** ChaseTask maximum chase distance. */
  private static final float CHASE_MAX_DISTANCE = 4f;

  /** Wander range, used for both x and y. */
  private static final float WANDER_RANGE = 2f;

  /** Wait between wanders, including Medusa's bounded wander. */
  private static final float WANDER_WAIT_SECONDS = 2f;

  /**
   * Floor check sits at half the collider width: scale * collisionX * FLOOR_PROBE_FRACTION.
   * Replaces the repeated / 2.
   */
  private static final float FLOOR_PROBE_FRACTION = 0.5f;

  // ---------- Body and collider ----------
  /** newEnemyBody, setScaledCollider width. */
  private static final float BODY_COLLIDER_WIDTH_SCALE = 0.9f;

  /** newEnemyBody, setScaledCollider height. */
  private static final float BODY_COLLIDER_HEIGHT_SCALE = 0.7f;

  /** Skeleton, Ranged Skeleton, Cerberus, both Harpies. */
  private static final float SMALL_COLLISION_X = 0.45f;

  /** Same five. */
  private static final float SMALL_COLLISION_Y = 0.6f;

  /** Centaur, Cyclops, Medusa, Zeus. */
  private static final float MEDIUM_COLLISION_X = 0.4f;

  /** Same four. */
  private static final float MEDIUM_COLLISION_Y = 0.5f;

  /** Minotaur. */
  private static final float MINOTAUR_COLLISION_X = 0.8f;

  /** Minotaur. */
  private static final float MINOTAUR_COLLISION_Y = 0.7f;

  /** Minotaur sprite frame height, for the foot-aligned collider. */
  private static final float MINOTAUR_FRAME_HEIGHT_PIXELS = 80f;

  /** Empty pixels under the Minotaur's feet. */
  private static final float MINOTAUR_FOOT_PADDING_PIXELS = 16f;

  // ---------- Size of each enemy ----------
  /** Skeleton, Ranged Skeleton, Harpy, Ranged Harpy. */
  private static final float SMALL_ENEMY_SCALE = 1f;

  /** Minotaur, Centaur. */
  private static final float LARGE_ENEMY_SCALE = 4f;

  /** Cyclops, Medusa, Zeus. */
  private static final float BOSS_SCALE = 2f;

  /** Cerberus. */
  private static final float CERBERUS_SCALE = 1.5f;

  /** Skeleton, Ranged Skeleton (height equals width). */
  private static final float SQUARE_HEIGHT_RATIO = 1f;

  /** Minotaur. */
  private static final float MINOTAUR_HEIGHT_RATIO = 80f / 96f;

  /** Centaur. */
  private static final float CENTAUR_HEIGHT_RATIO = 53f / 66f;

  /** Cyclops. */
  private static final float CYCLOPS_HEIGHT_RATIO = 48f / 64f;

  /** Medusa. */
  private static final float MEDUSA_HEIGHT_RATIO = 91f / 128f;

  /** Cerberus. */
  private static final float CERBERUS_HEIGHT_RATIO = 96f / 150f;

  /** Zeus. */
  private static final float ZEUS_HEIGHT_RATIO = 68f / 37f;

  /** Harpy, Ranged Harpy. */
  private static final float HARPY_HEIGHT_RATIO = 25f / 38f;

  // ---------- Loot ----------
  /** Skeleton, Ranged Skeleton, both Harpies. */
  private static final int NORMAL_ENEMY_GOLD = 3;

  /** Cerberus, Medusa, Minotaur, Cyclops, Centaur (the BossRoster mini boss types). */
  private static final int MINI_BOSS_GOLD = 12;

  /** Zeus (the BossRoster final boss). */
  private static final int FINAL_BOSS_GOLD = 20;

  /** Zeus's fixed tortoise. */
  private static final String ZEUS_TORTOISE_ID = "zeus_tortoise";

  /** inventory.getItem(1), the slot every armed enemy's weapon sits in. */
  private static final int WEAPON_SLOT = 1;

  /** Skeleton sword, Ranged Skeleton bow, Centaur bow, Harpy sword, Ranged Harpy bow. */
  private static final int BASIC_WEAPON_TIER = 1;

  /** Minotaur axe only. */
  private static final int STRONG_WEAPON_TIER = 2;

  /** The Minotaur's axe has no windup; its charge is the telegraph. */
  private static final float MINOTAUR_AXE_WINDUP_SECONDS = 0f;

  // ---------- Animation ----------
  /** Most animations. */
  private static final float DEFAULT_FRAME_TIME = 0.1f;

  /** Zeus walk and strike, Skeleton sword swing. */
  private static final float FAST_FRAME_TIME = 0.08f;

  /** Medusa and Zeus death animations. */
  private static final float DEATH_FRAME_TIME = 0.15f;

  /** Overlay animation names such as sword_l. */
  private static final String LEFT_SUFFIX = "_l";

  /** Overlay animation names such as sword_r. */
  private static final String RIGHT_SUFFIX = "_r";

  /** Both skeletons (workbook #343 item). */
  private static final String SKELETON_IDLE_LEFT = "idlel";

  /** Both skeletons. */
  private static final String SKELETON_IDLE_RIGHT = "idler";

  /** Both skeletons. */
  private static final String SKELETON_WALK_LEFT = "walkl";

  /** Both skeletons. */
  private static final String SKELETON_WALK_RIGHT = "walkr";

  /** Sword and bow overlays. */
  private static final String WEAPON_DEFAULT_LEFT = "default_l";

  /** Sword and bow overlays. */
  private static final String WEAPON_DEFAULT_RIGHT = "default_r";

  /** Sword overlay swing, plus LEFT_SUFFIX or RIGHT_SUFFIX. */
  private static final String SWORD_ATTACK_NAME = "sword";

  /** Bow overlay draw, plus a suffix. */
  private static final String BOW_ATTACK_NAME = "bow";

  /** Sword swing. */
  private static final float SWORD_ATTACK_FRAME_TIME = 0.08f;

  /** Bow draw. */
  private static final float BOW_ATTACK_FRAME_TIME = 0.1f;

  /** Cyclops laser overlay scale multiplier. */
  private static final float LASER_OVERLAY_SCALE = 1f;

  /** Cyclops laser overlay offset, x and y. */
  private static final float LASER_OVERLAY_OFFSET = 0f;

  // ---------- Events ----------
  /** Cyclops laser task. */
  private static final String LASER_ATTACK_EVENT = "laserAttack";

  /** Cyclops rock task. */
  private static final String ROCK_ATTACK_EVENT = "rockAttack";

  // ---------- Natural weapon names ----------
  /** Cyclops stomp. */
  private static final String CYCLOPS_FISTS_NAME = "Cyclops Fists";

  /** Cyclops rock. */
  private static final String CYCLOPS_ROCK_NAME = "Cyclops Rock Throw";

  /** Cyclops laser. */
  private static final String CYCLOPS_LASER_NAME = "Cyclops Laser";

  /** Medusa melee. */
  private static final String MEDUSA_BITE_NAME = "Medusa Bite";

  /** Medusa ranged. */
  private static final String MEDUSA_GAZE_NAME = "Medusa Gaze";

  /** Cerberus melee. */
  private static final String CERBERUS_BITE_NAME = "Cerberus Bite";

  /** Zeus ranged. */
  private static final String ZEUS_LIGHTNING_NAME = "Zeus Lightning";

  /** Zeus melee; a natural weapon, so it never drops. */
  private static final String ZEUS_SWORD_NAME = "Zeus Sword";

  // ---------- Rules and messages ----------
  /** requireTarget builds enemyName + this. Replaces five different messages. */
  private static final String TARGET_REQUIRED_SUFFIX = " needs a target";

  // ---------- Spawn names (only if registerEnemySpawns moves into this class) ----------
  /** createSkeleton. */
  private static final String SPAWN_SKELETON = "skeleton";

  /** createRangedSkeleton. */
  private static final String SPAWN_RANGED_SKELETON = "ranged-skeleton";

  /** createHarpy. */
  private static final String SPAWN_HARPY = "harpy";

  /** createRangedHarpy. */
  private static final String SPAWN_RANGED_HARPY = "ranged-harpy";

  /** createCyclops. */
  private static final String SPAWN_CYCLOPS = "cyclops";

  /** createMinotaur. */
  private static final String SPAWN_MINOTAUR = "minotaur";

  /** createCentaur. */
  private static final String SPAWN_CENTAUR = "centaur";

  /** createCerberus. */
  private static final String SPAWN_CERBERUS = "cerberus";

  /** createMedusa. */
  private static final String SPAWN_MEDUSA = "medusa";

  /** createZeus. */
  private static final String SPAWN_ZEUS = "zeus";

  // ======================= Creators =======================

  /**
   * Creates a Skeleton: a ground enemy with a tier-1 sword that walks, chases and melees.
   *
   * @param target entity to chase and attack; must not be null
   * @return the Skeleton, ready to be positioned and spawned
   * @throws IllegalArgumentException if target is null
   */
  public static Entity createSkeleton(Entity target) {
    String enemyLabel = new EnemyTypeComponent(EnemyType.SKELETON).getEnemyLabel();
    requireTarget(target, enemyLabel);
    SkeletonConfig config = configs.skeleton;
    float scale = SMALL_ENEMY_SCALE;
    Entity skeleton =
        createBasePlatformerEnemy(target, scale * SMALL_COLLISION_X * FLOOR_PROBE_FRACTION);
    InventoryComponent inventory = armedInventory(NORMAL_ENEMY_GOLD, SWORD, BASIC_WEAPON_TIER);
    WeaponItem weapon = (WeaponItem) inventory.getItem(WEAPON_SLOT);
    AnimationRenderComponent animator = newAnimator(SKELETON_ATLAS_PATH, true);
    addAnimations(
        animator,
        DEFAULT_FRAME_TIME,
        LOOP,
        SKELETON_IDLE_LEFT,
        SKELETON_IDLE_RIGHT,
        SKELETON_WALK_LEFT,
        SKELETON_WALK_RIGHT);
    EnemyWeaponAnimationComponent weaponAnimator =
        skeletonWeaponAnimator(
            SKELETON_SWORD_ATLAS_PATH, SWORD_ATTACK_NAME, SWORD_ATTACK_FRAME_TIME);
    addCommonParts(skeleton, config, EnemyType.SKELETON, inventory, animator);
    skeleton.addComponent(meleeAttack(config.melee, weapon));
    skeleton.addComponent(new SkeletonAnimationController());
    skeleton.addComponent(weaponAnimator);
    skeleton.addComponent(new SkeletonWeaponAnimationController());
    addMeleeTask(skeleton, target, config.melee.range);

    return finishEnemy(
        skeleton, animator, scale, SQUARE_HEIGHT_RATIO, SMALL_COLLISION_X, SMALL_COLLISION_Y);
  }

  /**
   * Creates a Ranged Skeleton: a ground archer with a tier-1 bow that shoots from a distance
   * instead of closing in.
   *
   * @param target entity to chase and shoot; must not be null
   * @return the Ranged Skeleton, ready to be positioned and spawned
   * @throws IllegalArgumentException if target is null
   */
  public static Entity createRangedSkeleton(Entity target) {
    String enemyLabel = new EnemyTypeComponent(EnemyType.RANGED_SKELETON).getEnemyLabel();
    requireTarget(target, enemyLabel);
    RangedSkeletonConfig config = configs.rangedSkeleton;
    float scale = SMALL_ENEMY_SCALE;
    Entity rangedSkeleton =
        createBasePlatformerEnemy(target, scale * SMALL_COLLISION_X * FLOOR_PROBE_FRACTION);
    InventoryComponent inventory = armedInventory(NORMAL_ENEMY_GOLD, BOW, BASIC_WEAPON_TIER);
    WeaponItem weapon = (WeaponItem) inventory.getItem(WEAPON_SLOT);
    AnimationRenderComponent animator = newAnimator(SKELETON_ATLAS_PATH, true);
    addAnimations(
        animator,
        DEFAULT_FRAME_TIME,
        LOOP,
        SKELETON_IDLE_LEFT,
        SKELETON_IDLE_RIGHT,
        SKELETON_WALK_LEFT,
        SKELETON_WALK_RIGHT);
    EnemyWeaponAnimationComponent weaponAnimator =
        skeletonWeaponAnimator(SKELETON_BOW_ATLAS_PATH, BOW_ATTACK_NAME, BOW_ATTACK_FRAME_TIME);
    addCommonParts(rangedSkeleton, config, EnemyType.RANGED_SKELETON, inventory, animator);
    rangedSkeleton.addComponent(rangedAttack(config.ranged, weapon));
    rangedSkeleton.addComponent(new SkeletonAnimationController());
    rangedSkeleton.addComponent(weaponAnimator);
    rangedSkeleton.addComponent(new SkeletonWeaponAnimationController());
    addRangedTask(
        rangedSkeleton,
        target,
        PRIMARY_RANGED_TASK_PRIORITY,
        config.ranged.range,
        ProjectileType.ARROW,
        null);

    return finishEnemy(
        rangedSkeleton, animator, scale, SQUARE_HEIGHT_RATIO, SMALL_COLLISION_X, SMALL_COLLISION_Y);
  }

  /**
   * Creates a melee Harpy: a flying enemy armed with a tier-1 sword, mirroring the melee Skeleton.
   *
   * <p>PLACEHOLDER BUILD: skeleton animation controller is replaced by the Harpy's own.
   *
   * @param target entity to chase; must not be null
   * @return a flying melee enemy
   * @throws IllegalArgumentException if target is null
   */
  public static Entity createHarpy(Entity target) {
    String enemyLabel = new EnemyTypeComponent(EnemyType.HARPY).getEnemyLabel();
    requireTarget(target, enemyLabel);
    HarpyConfig config = configs.harpy;
    float scale = SMALL_ENEMY_SCALE;
    Entity harpy = createBaseFlyingEnemy(target, config.flightDamping);
    InventoryComponent inventory = armedInventory(NORMAL_ENEMY_GOLD, SWORD, BASIC_WEAPON_TIER);
    WeaponItem weapon = (WeaponItem) inventory.getItem(WEAPON_SLOT);
    AnimationRenderComponent animator = newAnimator(HARPY_ATLAS_PATH, true);
    addAnimations(animator, DEFAULT_FRAME_TIME, LOOP, "harpy_y_l", "harpy_y_r");
    addCommonParts(harpy, config, EnemyType.HARPY, inventory, animator);
    harpy.addComponent(meleeAttack(config.melee, weapon));
    harpy.addComponent(new HarpyAnimationController());
    addMeleeTask(harpy, target, config.melee.range);
    return finishEnemy(
        harpy, animator, scale, HARPY_HEIGHT_RATIO, SMALL_COLLISION_X, SMALL_COLLISION_Y);
  }

  /**
   * Creates a ranged Harpy: a flying enemy armed with a tier-1 bow that fires aimed arrows,
   * mirroring the Ranged Skeleton. Aimed arrows let it hit a player above or below it.
   *
   * @param target entity to chase and shoot; must not be null
   * @return a flying ranged enemy
   * @throws IllegalArgumentException if target is null
   */
  public static Entity createRangedHarpy(Entity target) {
    String enemyLabel = new EnemyTypeComponent(EnemyType.RANGED_HARPY).getEnemyLabel();
    requireTarget(target, enemyLabel);
    RangedHarpyConfig config = configs.rangedHarpy;
    float scale = SMALL_ENEMY_SCALE;
    // flying entity is created.
    Entity rangedHarpy = createBaseFlyingEnemy(target, config.flightDamping);
    InventoryComponent inventory = armedInventory(NORMAL_ENEMY_GOLD, BOW, BASIC_WEAPON_TIER);
    WeaponItem weapon = (WeaponItem) inventory.getItem(WEAPON_SLOT);
    AnimationRenderComponent animator = newAnimator(HARPY_ATLAS_PATH, true);
    addAnimations(animator, DEFAULT_FRAME_TIME, LOOP, "harpy_y_l", "harpy_y_r");
    addCommonParts(rangedHarpy, config, EnemyType.RANGED_HARPY, inventory, animator);
    // sets projectile speed and aimed weapon
    rangedHarpy.addComponent(rangedAttack(config.ranged, weapon));
    rangedHarpy.addComponent(new HarpyAnimationController());
    addRangedTask(
        rangedHarpy,
        target,
        PRIMARY_RANGED_TASK_PRIORITY,
        config.ranged.range,
        ProjectileType.ARROW,
        null);

    return finishEnemy(
        rangedHarpy, animator, scale, HARPY_HEIGHT_RATIO, SMALL_COLLISION_X, SMALL_COLLISION_Y);
  }

  /**
   * Creates a Minotaur: a large mini boss with an axe that charges at its target. It pays {@code
   * MINI_BOSS_GOLD} and drops its tier-2 axe.
   *
   * @param target entity to chase and charge at; must not be null
   * @return the Minotaur, ready to be positioned and spawned
   * @throws IllegalArgumentException if target is null
   */
  public static Entity createMinotaur(Entity target) {
    String enemyLabel = new EnemyTypeComponent(EnemyType.MINOTAUR).getEnemyLabel();
    requireTarget(target, enemyLabel);
    MinotaurConfig config = configs.minotaur;
    float scale = LARGE_ENEMY_SCALE;

    Entity minotaur =
        createBasePlatformerEnemy(target, scale * MINOTAUR_COLLISION_X * FLOOR_PROBE_FRACTION);
    // inventory = armedInventory(MINI_BOSS_GOLD, WeaponType.AXE, STRONG_WEAPON_TIER)
    InventoryComponent inventory = armedInventory(MINI_BOSS_GOLD, AXE, STRONG_WEAPON_TIER);
    WeaponItem weapon = (WeaponItem) inventory.getItem(WEAPON_SLOT);
    // NO WIND UP - the change is the telegraph
    weapon.setWindupDuration(MINOTAUR_AXE_WINDUP_SECONDS);
    AnimationRenderComponent animator = newAnimator(MINOTAUR_ATLAS_PATH, true);
    addAnimations(
        animator,
        DEFAULT_FRAME_TIME,
        LOOP,
        "minotaur_idle_l",
        "minotaur_idle_r",
        "minotaur_walk_l",
        "minotaur_walk_r",
        "minotaur_charge_l",
        "minotaur_charge_r");
    addAnimations(animator, DEFAULT_FRAME_TIME, NORMAL, "minotaur_swing_l", "minotaur_swing_r");
    addCommonParts(minotaur, config, EnemyType.MINOTAUR, inventory, animator);
    minotaur.addComponent(meleeAttack(config.melee, weapon));
    minotaur.addComponent(new MinotaurAnimationController());
    // charge component + touch attack
    ChargeComponent charge = addChargeParts(minotaur, config.charge);
    addMeleeTask(minotaur, target, config.melee.range);
    addChargeTask(minotaur, target, charge, config.charge.aggroRadius);
    finishEnemy(
        minotaur,
        animator,
        scale,
        MINOTAUR_HEIGHT_RATIO,
        MINOTAUR_COLLISION_X,
        MINOTAUR_COLLISION_Y);
    // replaces the collider just set
    setFootAlignedCollider(minotaur, MINOTAUR_COLLISION_X, MINOTAUR_COLLISION_Y);
    return minotaur;
  }

  /**
   * Creates a Centaur: a large mini boss with a tier-1 bow that shoots from range and charges. It
   * pays {@code MINI_BOSS_GOLD}.
   *
   * @param target entity to chase and shoot; must not be null
   * @return the Centaur, ready to be positioned and spawned
   * @throws IllegalArgumentException if target is null
   */
  public static Entity createCentaur(Entity target) {
    String enemyLabel = new EnemyTypeComponent(EnemyType.CENTAUR).getEnemyLabel();
    requireTarget(target, enemyLabel);
    CentaurConfig config = configs.centaur;
    float scale = LARGE_ENEMY_SCALE;
    Entity centaur =
        createBasePlatformerEnemy(target, scale * MEDIUM_COLLISION_X * FLOOR_PROBE_FRACTION);
    InventoryComponent inventory =
        armedInventory(MINI_BOSS_GOLD, WeaponType.BOW, BASIC_WEAPON_TIER);
    WeaponItem weapon = (WeaponItem) inventory.getItem(WEAPON_SLOT);
    // weapon = inventory item in WEAPON_SLOT
    AnimationRenderComponent animator = newAnimator(CENTAUR_ATLAS_PATH, true);
    addAnimations(
        animator,
        DEFAULT_FRAME_TIME,
        LOOP,
        "centaur_idle_l",
        "centaur_idle_r",
        "centaur_run_l",
        "centaur_run_r");
    addAnimations(
        animator,
        DEFAULT_FRAME_TIME,
        NORMAL,
        "centaur_swing_l",
        "centaur_swing_r",
        "centaur_death_l",
        "centaur_death_r");
    addCommonParts(centaur, config, EnemyType.CENTAUR, inventory, animator);
    centaur.addComponent(rangedAttack(config.ranged, weapon));
    centaur.addComponent(new CentaurAnimationController());
    ChargeComponent charge = addChargeParts(centaur, config.charge);
    addRangedTask(
        centaur, target, RANGED_TASK_PRIORITY, config.ranged.range, ProjectileType.ARROW, null);
    addChargeTask(centaur, target, charge, config.charge.aggroRadius);
    return finishEnemy(
        centaur, animator, scale, CENTAUR_HEIGHT_RATIO, MEDIUM_COLLISION_X, MEDIUM_COLLISION_Y);
  }

  /**
   * Creates a Cyclops: a mini boss with a stomp, a lobbed rock and an eye laser.
   *
   * <p>Task priorities decide which attack runs, because the highest priority among the tasks whose
   * range covers the target wins and the ranged task ignores cooldown: the stomp ({@code
   * MELEE_TASK_PRIORITY}) up close, the laser ({@code LASER_TASK_PRIORITY}) at middle range and the
   * rock ({@code RANGED_TASK_PRIORITY}) only beyond the laser's range. So the rock is never thrown
   * at close range and the laser is never fired at long range.
   *
   * <p>Pays {@code MINI_BOSS_GOLD} and carries no weapon item: its attacks are natural weapons and
   * never drop.
   *
   * @param target entity to chase and attack; must not be null
   * @return the Cyclops, ready to be positioned and spawned
   * @throws IllegalArgumentException if target is null
   */
  public static Entity createCyclops(Entity target) {
    String enemyLabel = new EnemyTypeComponent(EnemyType.CYCLOPS).getEnemyLabel();
    requireTarget(target, enemyLabel);
    CyclopsConfig config = configs.cyclops;
    float scale = BOSS_SCALE;
    Entity cyclops =
        createBasePlatformerEnemy(target, scale * MEDIUM_COLLISION_X * FLOOR_PROBE_FRACTION);
    cyclops.getComponent(NameComponent.class).setName("Cyclops");
    InventoryComponent inventory = new InventoryComponent(MINI_BOSS_GOLD); // gold only
    AnimationRenderComponent animator = newAnimator(CYCLOPS_ATLAS_PATH, true);
    addAnimations(
        animator,
        DEFAULT_FRAME_TIME,
        LOOP,
        "cyclops_idle_l",
        "cyclops_idle_r",
        "cyclops_walk_l",
        "cyclops_walk_r");
    addAnimations(
        animator,
        DEFAULT_FRAME_TIME,
        NORMAL,
        "cyclops_rock_l",
        "cyclops_rock_r",
        "cyclops_stomp_l",
        "cyclops_stomp_r",
        "cyclops_death_l",
        "cyclops_death_r",
        "cyclops_taunt_l",
        "cyclops_taunt_r",
        "cyclops_eye_scratch_l",
        "cyclops_eye_scratch_r");
    EnemyWeaponAnimationComponent laserOverlay =
        new EnemyWeaponAnimationComponent(loadIndependentAtlas(CYCLOPS_ATLAS_PATH));
    laserOverlay.setScaleMultiplier(LASER_OVERLAY_SCALE);
    laserOverlay.setOffset(LASER_OVERLAY_OFFSET, LASER_OVERLAY_OFFSET);
    addAnimations(laserOverlay, DEFAULT_FRAME_TIME, NORMAL, "cyclops_laser_l", "cyclops_laser_r");
    WeaponItem fists =
        WeaponItem.natural(CYCLOPS_FISTS_NAME, config.baseAttack, config.melee.windup);
    WeaponItem rock = WeaponItem.natural(CYCLOPS_ROCK_NAME, config.baseAttack, config.rock.windup);
    WeaponItem laser =
        WeaponItem.natural(CYCLOPS_LASER_NAME, config.baseAttack, config.laser.windup);
    addCommonParts(cyclops, config, EnemyType.CYCLOPS, inventory, animator);
    cyclops.addComponent(meleeAttack(config.melee, fists));
    cyclops.addComponent(
        new RockAttackComponent(
            config.rock.range,
            config.rock.cooldown,
            config.rock.knockback,
            rock,
            config.rock.spawnHeightFraction,
            config.rock.damageMultiplier));
    cyclops.getComponent(RockAttackComponent.class).setProjectileSpeed(config.rock.projectileSpeed);
    cyclops.addComponent(
        new LaserAttackComponent(
            config.laser.range,
            config.laser.cooldown,
            config.laser.knockback,
            laser,
            config.laser.spawnHeightFraction,
            config.laser.damageMultiplier));
    cyclops
        .getComponent(LaserAttackComponent.class)
        .setProjectileSpeed(config.laser.projectileSpeed);
    cyclops.addComponent(laserOverlay);
    cyclops.addComponent(new CyclopsAnimationController());
    addMeleeTask(cyclops, target, config.melee.range);
    addRangedTask(
        cyclops,
        target,
        LASER_TASK_PRIORITY,
        config.laser.range,
        ProjectileType.LASER,
        LASER_ATTACK_EVENT);
    addRangedTask(
        cyclops,
        target,
        RANGED_TASK_PRIORITY,
        config.rock.range,
        ProjectileType.ROCK,
        ROCK_ATTACK_EVENT);
    return finishEnemy(
        cyclops, animator, scale, CYCLOPS_HEIGHT_RATIO, MEDIUM_COLLISION_X, MEDIUM_COLLISION_Y);
  }

  /**
   * Creates Medusa, a mini boss that paces on the spot, bites up close and petrifies the player
   * with a ranged gaze. Pays {@code MINI_BOSS_GOLD}; her bite and gaze are natural weapons and
   * never drop.
   *
   * <p>PLACEHOLDER BUILD: she uses the Cyclops-style natural weapons and the gorgon atlas. A hit
   * freezes the player through the existing {@code "applySpeedEffect"} event, the same one
   * lightning uses.
   *
   * @param target entity to attack (the player); must not be null
   * @return Medusa, ready to be positioned and spawned
   * @throws IllegalArgumentException if target is null or a design rule is broken
   */
  public static Entity createMedusa(Entity target) {
    String enemyLabel = new EnemyTypeComponent(EnemyType.MEDUSA).getEnemyLabel();
    requireTarget(target, enemyLabel);
    MedusaConfig config = configs.medusa;
    validateMeleeRangeBelowRangedRange(config.melee.range, config.ranged.range);
    // Checks the duration and the cooldown rule, and converts seconds to ticks.
    PetrifyEffectComponent petrify =
        PetrifyEffectComponent.forShooter(config.petrify, config.ranged.cooldown);
    float scale = BOSS_SCALE;
    Entity medusa = createBaseStationaryEnemy();
    medusa.getComponent(NameComponent.class).setName("Medusa");
    InventoryComponent inventory = new InventoryComponent(MINI_BOSS_GOLD);
    AnimationRenderComponent animator = newAnimator(MEDUSA_ATLAS_PATH, true);
    addAnimations(
        animator,
        DEFAULT_FRAME_TIME,
        LOOP,
        "gorgon_idle_l",
        "gorgon_idle_r",
        "gorgon_walk_l",
        "gorgon_walk_r");
    addAnimations(animator, DEFAULT_FRAME_TIME, NORMAL, "gorgon_attack_l", "gorgon_attack_r");
    addAnimations(animator, DEATH_FRAME_TIME, NORMAL, "gorgon_dead_l", "gorgon_dead_r");
    WeaponItem bite = WeaponItem.natural(MEDUSA_BITE_NAME, config.baseAttack, config.melee.windup);
    WeaponItem gaze = WeaponItem.natural(MEDUSA_GAZE_NAME, config.baseAttack, config.ranged.windup);
    addCommonParts(medusa, config, EnemyType.MEDUSA, inventory, animator);
    medusa.addComponent(meleeAttack(config.melee, bite));
    medusa.addComponent(rangedAttack(config.ranged, gaze));
    medusa.addComponent(new MedusaAnimationController());
    // listens for her "rangedAttackHit" and petrifies whatever the gaze landed on
    medusa.addComponent(petrify);
    medusa
        .getComponent(AITaskComponent.class)
        .addTask(
            new BoundedPlatformWanderTask(
                config.pacing.radius,
                WANDER_WAIT_SECONDS,
                scale * MEDIUM_COLLISION_X * FLOOR_PROBE_FRACTION));
    addRangedTask(
        medusa, target, RANGED_TASK_PRIORITY, config.ranged.range, ProjectileType.ARROW, null);
    addMeleeTask(medusa, target, config.melee.range);
    return finishEnemy(
        medusa, animator, scale, MEDUSA_HEIGHT_RATIO, MEDIUM_COLLISION_X, MEDIUM_COLLISION_Y);
  }

  /**
   * Creates Cerberus, a stationary mini boss that bites anything inside its reach, very often. Pays
   * {@code MINI_BOSS_GOLD}; the bite is a natural weapon and never drops.
   *
   * <p>PLACEHOLDER BUILD: skeleton atlas and animation controller. The melee task's range is her
   * effective aggro radius, because she has no chase task.
   *
   * @param target entity to attack; must not be null
   * @return Cerberus, ready to be positioned and spawned
   * @throws IllegalArgumentException if target is null
   */
  public static Entity createCerberus(Entity target) {
    String enemyLabel = new EnemyTypeComponent(EnemyType.CERBERUS).getEnemyLabel();
    requireTarget(target, enemyLabel);
    CerberusConfig config = configs.cerberus;
    float scale = CERBERUS_SCALE;
    Entity cerberus = createBaseStationaryEnemy();
    InventoryComponent inventory = new InventoryComponent(MINI_BOSS_GOLD);
    AnimationRenderComponent animator = newAnimator(CERBERUS_ATLAS_PATH, true);
    addAnimations(animator, DEFAULT_FRAME_TIME, LOOP, "cerberus_l", "cerberus_r");
    WeaponItem bite =
        WeaponItem.natural(CERBERUS_BITE_NAME, config.baseAttack, config.melee.windup);
    addCommonParts(cerberus, config, EnemyType.CERBERUS, inventory, animator);
    cerberus.addComponent(meleeAttack(config.melee, bite));
    cerberus.addComponent(new CerberusAnimationController());
    // she paces on the spot instead of chasing
    cerberus
        .getComponent(AITaskComponent.class)
        .addTask(
            new BoundedPlatformWanderTask(
                config.pacing.radius,
                WANDER_WAIT_SECONDS,
                scale * SMALL_COLLISION_X * FLOOR_PROBE_FRACTION));
    addMeleeTask(cerberus, target, config.melee.range);
    return finishEnemy(
        cerberus, animator, scale, CERBERUS_HEIGHT_RATIO, SMALL_COLLISION_X, SMALL_COLLISION_Y);
  }

  /**
   * Creates Zeus, the final boss. He fights with a natural sword up close and throws lightning from
   * range, and moves and chases like the other platformer enemies.
   *
   * <p>Drops {@code FINAL_BOSS_GOLD} gold (his tortoise drop is not wired in yet). He carries no
   * weapon item: his sword and lightning are natural weapons and never drop.
   *
   * <p>PLACEHOLDER BUILD: Cyclops atlas and animation controller. The lightning fires the existing
   * LIGHTNING projectile.
   *
   * @param target entity to chase and attack; must not be null
   * @return Zeus, ready to be positioned and spawned
   * @throws IllegalArgumentException if target is null or a design rule is broken
   */
  public static Entity createZeus(Entity target) {
    String enemyLabel = new EnemyTypeComponent(EnemyType.ZEUS).getEnemyLabel();
    requireTarget(target, enemyLabel);
    ZeusConfig config = configs.zeus;
    validateMeleeRangeBelowRangedRange(config.melee.range, config.ranged.range);
    float scale = BOSS_SCALE;
    Entity zeus =
        createBasePlatformerEnemy(target, scale * MEDIUM_COLLISION_X * FLOOR_PROBE_FRACTION);
    zeus.getComponent(NameComponent.class).setName("Zeus");
    // NO SWORD FOR ZEUS
    InventoryComponent inventory = new InventoryComponent(FINAL_BOSS_GOLD);
    AnimationRenderComponent animator = newAnimator(ZEUS_ATLAS_PATH, true);
    addAnimations(animator, DEFAULT_FRAME_TIME, LOOP, "zeus_idle_l", "zeus_idle_r");
    addAnimations(animator, FAST_FRAME_TIME, LOOP, "zeus_walk_l", "zeus_walk_r");
    addAnimations(animator, FAST_FRAME_TIME, NORMAL, "zeus_p_strike_l", "zeus_p_strike_r");
    addAnimations(animator, DEFAULT_FRAME_TIME, NORMAL, "zeus_slam_l", "zeus_slam_r");
    addAnimations(animator, DEATH_FRAME_TIME, NORMAL, "zeus_death_l", "zeus_death_r");
    WeaponItem sword = WeaponItem.natural(ZEUS_SWORD_NAME, config.baseAttack, config.melee.windup);
    WeaponItem lightning =
        WeaponItem.natural(ZEUS_LIGHTNING_NAME, config.baseAttack, config.ranged.windup);
    addCommonParts(zeus, config, EnemyType.ZEUS, inventory, animator);
    zeus.addComponent(meleeAttack(config.melee, sword));
    zeus.addComponent(rangedAttack(config.ranged, lightning));
    zeus.addComponent(new ZeusAnimationController());
    // Zeus's tortoise drop goes here once TortoiseDropComponent is on the branch (see the
    // commented addTortoiseDrop helper below).
    addTortoiseDrop(zeus, ZEUS_TORTOISE_ID);
    addMeleeTask(zeus, target, config.melee.range);
    addRangedTask(
        zeus, target, RANGED_TASK_PRIORITY, config.ranged.range, ProjectileType.LIGHTNING, null);
    return finishEnemy(
        zeus, animator, scale, ZEUS_HEIGHT_RATIO, MEDIUM_COLLISION_X, MEDIUM_COLLISION_Y);
  }

  // ======================= Helpers (private static) =======================

  /**
   * Throws if an enemy is built without something to attack. Every creator calls this first, so a
   * missing target fails at construction with the enemy's name in the message instead of later,
   * inside a task.
   *
   * @param target the entity the enemy will chase and attack
   * @param enemyName the name used in the message, for example "Zeus"
   * @throws IllegalArgumentException if target is null; the message is enemyName followed by {@code
   *     TARGET_REQUIRED_SUFFIX}
   */
  private static void requireTarget(Entity target, String enemyName) {
    if (target == null) {
      throw new IllegalArgumentException(enemyName + TARGET_REQUIRED_SUFFIX);
    }
  }

  /**
   * Loads a fresh, independently owned {@link TextureAtlas} from an internal path, bypassing {@code
   * ResourceService}'s cache. Each enemy then holds an atlas no other entity references, so
   * disposing one enemy's atlas cannot break another enemy's sprite.
   *
   * @param path internal path to the {@code .atlas} file, for example {@code
   *     "images/enemies/skeleton.atlas"}
   * @return a new atlas loaded from that path, a different object on every call
   * @throws GdxRuntimeException if the file is missing or is not an atlas
   */
  private static TextureAtlas loadIndependentAtlas(String path) {
    FileHandle fileHandle = Gdx.files.internal(path);
    return new TextureAtlas(fileHandle);
  }

  /**
   * Builds the body every enemy starts from: physics, movement, collider, an NPC-layer hitbox and
   * the given AI component, with the collider scaled to {@code BODY_COLLIDER_WIDTH_SCALE} by {@code
   * BODY_COLLIDER_HEIGHT_SCALE}.
   *
   * @param aiComponent the task list this enemy will use; the caller may add tasks to it later
   * @param grounded true if gravity applies and movement is along the ground, false if the enemy
   *     flies
   * @return a new entity that is not yet created
   */
  private static final NameGenerator NAME_GENERATOR = new NameGenerator();

  private static Entity newEnemyBody(AITaskComponent aiComponent, boolean grounded) {
    Entity enemyBody = new Entity();
    enemyBody.addComponent(new NameComponent(NAME_GENERATOR.generateName()));
    enemyBody
        .addComponent(new PhysicsComponent())
        .addComponent(new PhysicsMovementComponent())
        .addComponent(new ColliderComponent())
        .addComponent(new HitboxComponent().setLayer(PhysicsLayer.NPC))
        .addComponent(aiComponent);
    PhysicsUtils.setScaledCollider(
        enemyBody, BODY_COLLIDER_WIDTH_SCALE, BODY_COLLIDER_HEIGHT_SCALE);
    enemyBody.getComponent(PhysicsMovementComponent.class).setGroundedMovement(grounded);
    return enemyBody;
  }

  /**
   * Creates a grounded enemy base that falls with gravity, wanders along platforms and chases the
   * target. It has no attack task: each creator adds its own melee, ranged or charge task.
   *
   * @param target the entity to chase; must not be null
   * @param floorCollisionScale how far from the centre the floor check looks, so the enemy turns
   *     back at a platform edge
   * @return a base entity with grounded movement switched on
   * @throws IllegalArgumentException if target is null
   */
  private static Entity createBasePlatformerEnemy(Entity target, final float floorCollisionScale) {
    requireTarget(target, "Platformer Enemy");
    AITaskComponent ai = new AITaskComponent();
    ai.addTask(
            new PlatformWanderTask(
                new Vector2(WANDER_RANGE, WANDER_RANGE), WANDER_WAIT_SECONDS, floorCollisionScale))
        .addTask(
            new ChaseTask(target, CHASE_TASK_PRIORITY, CHASE_VIEW_DISTANCE, CHASE_MAX_DISTANCE));
    return newEnemyBody(ai, true);
  }

  /**
   * Creates a grounded enemy base with physics, collision and an EMPTY task list, for enemies that
   * never chase (Medusa, Cerberus). The caller adds whatever tasks it needs.
   *
   * @return a base entity with grounded movement switched on and no tasks
   */
  private static Entity createBaseStationaryEnemy() {
    return newEnemyBody(new AITaskComponent(), true);
  }

  /**
   * Creates a flying enemy base: wander and chase tasks, free movement in both axes and no gravity.
   * Touch damage is not used; attacks are attack components.
   *
   * @param target entity to chase
   * @param flightDamping linear damping on the physics body so knockback fades; not negative
   * @return a flying base entity with grounded movement switched off and a {@link FlightComponent}
   */
  private static Entity createBaseFlyingEnemy(Entity target, float flightDamping) {
    AITaskComponent ai = new AITaskComponent();
    ai.addTask(new WanderTask(new Vector2(WANDER_RANGE, WANDER_RANGE), WANDER_WAIT_SECONDS))
        .addTask(
            new ChaseTask(target, CHASE_TASK_PRIORITY, CHASE_VIEW_DISTANCE, CHASE_MAX_DISTANCE));
    Entity body = newEnemyBody(ai, false);
    // within flight constructor, negative, infinite or Nan damping throws exception
    body.addComponent(new FlightComponent(flightDamping));
    return body;
  }

  /**
   * Builds the loot an armed enemy drops: some gold and one generated weapon in {@code
   * WEAPON_SLOT}.
   *
   * @param gold gold dropped on death
   * @param weaponType the kind of weapon, for example {@code WeaponType.BOW}
   * @param tier weapon tier, {@code BASIC_WEAPON_TIER} or {@code STRONG_WEAPON_TIER}
   * @return a new inventory holding the gold and the weapon
   */
  private static InventoryComponent armedInventory(int gold, WeaponType weaponType, int tier) {

    InventoryComponent inventory = new InventoryComponent(gold);
    WeaponItem weapon = new WeaponGenerator().generateWeapon(weaponType, tier);
    // automatically lands in WEAPON_SLOT - slot 1
    inventory.addItem(weapon);
    return inventory;
  }

  /**
   * Creates an animation component on an independently loaded atlas.
   *
   * @param atlasPath internal path to the {@code .atlas} file
   * @param ownsAtlas true if the component should dispose the atlas when the enemy is disposed;
   *     pass true for atlases loaded by {@link #loadIndependentAtlas}, because nothing else will
   *     free them
   * @return a new animation component, with no animations added yet
   */
  private static AnimationRenderComponent newAnimator(String atlasPath, boolean ownsAtlas) {
    TextureAtlas atlas = loadIndependentAtlas(atlasPath);
    return new AnimationRenderComponent(atlas, ownsAtlas);
  }

  /**
   * Registers several animations that share a frame time and play mode.
   *
   * @param animator the component to add them to
   * @param frameTime seconds each frame is shown
   * @param playMode LOOP for idle and walk, NORMAL for one-shot attacks and deaths
   * @param names animation names exactly as written in the atlas; none is allowed and adds nothing
   */
  private static void addAnimations(
      AnimationRenderComponent animator,
      float frameTime,
      Animation.PlayMode playMode,
      String... names) {
    for (String animationName : names) {
      animator.addAnimation(animationName, frameTime, playMode);
    }
  }

  // Same method for the weapon overlay: the two classes share no parent that has addAnimation.
  private static void addAnimations(
      EnemyWeaponAnimationComponent overlay,
      float frameTime,
      Animation.PlayMode playMode,
      String... names) {
    for (String weaponName : names) {
      overlay.addAnimation(weaponName, frameTime, playMode);
    }
  }

  /**
   * Builds the sword or bow overlay drawn over a skeleton: two looping idle animations and a left
   * and right one-shot attack.
   *
   * @param atlasPath the overlay atlas, {@code SKELETON_SWORD_ATLAS_PATH} or {@code
   *     SKELETON_BOW_ATLAS_PATH}
   * @param attackName {@code SWORD_ATTACK_NAME} or {@code BOW_ATTACK_NAME}; the suffixes are added
   *     here
   * @param attackFrameTime seconds per frame of the attack animation
   * @return an overlay with four animations registered
   */
  private static EnemyWeaponAnimationComponent skeletonWeaponAnimator(
      String atlasPath, String attackName, float attackFrameTime) {
    EnemyWeaponAnimationComponent overlay =
        new EnemyWeaponAnimationComponent(loadIndependentAtlas(atlasPath));
    addAnimations(overlay, DEFAULT_FRAME_TIME, LOOP, WEAPON_DEFAULT_LEFT, WEAPON_DEFAULT_RIGHT);
    addAnimations(
        overlay,
        attackFrameTime,
        Animation.PlayMode.NORMAL,
        attackName + LEFT_SUFFIX,
        attackName + RIGHT_SUFFIX);
    return overlay;
  }

  /**
   * Creates a melee attack from an enemy's config block.
   *
   * @param melee the config's range, cooldown and knockback
   * @param weapon the weapon whose damage and windup the attack uses; natural weapons are allowed
   * @return a new melee attack component
   * @throws IllegalArgumentException if the component rejects the numbers, for example a windup not
   *     shorter than the cooldown
   */
  private static MeleeAttackComponent meleeAttack(MeleeAttackConfig melee, WeaponItem weapon) {
    return new MeleeAttackComponent(melee.range, melee.cooldown, melee.knockback, weapon);
  }

  /**
   * Creates a ranged attack from an enemy's config block, including its projectile speed and
   * whether it aims. Setting aimed for every ranged enemy is safe because only Zeus and the Ranged
   * Harpy have it true in {@code NPCs.json}.
   *
   * @param ranged the config's range, cooldown, knockback, projectile speed and aimed flag
   * @param weapon the weapon whose damage and windup the attack uses
   * @return a new ranged attack component
   */
  private static RangedAttackComponent rangedAttack(RangedAttackConfig ranged, WeaponItem weapon) {
    RangedAttackComponent rangedAttack =
        new RangedAttackComponent(ranged.range, ranged.cooldown, ranged.knockback, weapon);
    rangedAttack.setProjectileSpeed(ranged.projectileSpeed);
    rangedAttack.setAimed(ranged.aimed);
    return rangedAttack;
  }

  /**
   * Adds a charge and the touch attack that deals its damage to an enemy.
   *
   * @param enemy the enemy being built
   * @param charge Config the charge config: duration, windup, cooldown, multipliers, knockback,
   *     endOnHit
   * @return the charge component, which {@link #addChargeTask} needs
   */
  private static ChargeComponent addChargeParts(Entity enemy, ChargeAttackConfig charge) {
    ChargeComponent chargeAttack =
        new ChargeComponent(
            charge.duration,
            charge.windupDuration,
            charge.cooldown,
            charge.damageMultiplier,
            charge.speedMultiplier);
    chargeAttack.setEndOnHit(charge.endOnHit);
    enemy.addComponent(chargeAttack);
    enemy.addComponent(new TouchAttackComponent(PhysicsLayer.PLAYER, charge.knockback));
    return chargeAttack;
  }

  /**
   * Adds the parts every enemy has: combat stats, type label, inventory, item drop, death handler
   * and animator.
   *
   * @param enemy the enemy being built
   * @param config the enemy's config block, for health and base attack
   * @param enemyType the label its death handler reads
   * @param inventory what it drops
   * @param animator its sprite animations
   */
  private static void addCommonParts(
      Entity enemy,
      BaseEntityConfig config,
      EnemyType enemyType,
      InventoryComponent inventory,
      AnimationRenderComponent animator) {
    enemy.addComponent(new CombatStatsComponent(config.health, config.baseAttack));
    enemy.addComponent(new EnemyTypeComponent(enemyType));
    enemy.addComponent(inventory);
    enemy.addComponent(new ItemDropComponent());
    enemy.addComponent(new EnemyDeathComponent());
    enemy.addComponent(animator);
  }

  /**
   * Adds a melee task at {@code MELEE_TASK_PRIORITY}, above chase so a close player is meleed.
   *
   * @param enemy the enemy being built; must already have an AI component
   * @param target the entity to attack
   * @param range melee range, normally from the config
   */
  private static void addMeleeTask(Entity enemy, Entity target, float range) {
    enemy
        .getComponent(AITaskComponent.class)
        .addTask(new MeleeAttackTask(target, MELEE_TASK_PRIORITY, range));
  }

  /**
   * Adds a ranged task. The Cyclops passes an event name so its rock and laser use their own
   * events.
   *
   * @param enemy the enemy being built; must already have an AI component
   * @param target the entity to shoot
   * @param priority {@code RANGED_TASK_PRIORITY}, {@code LASER_TASK_PRIORITY} or {@code
   *     PRIMARY_RANGED_TASK_PRIORITY}
   * @param range range at which the task starts
   * @param projectileType what is fired
   * @param eventName the event the task triggers, or null for the default
   */
  private static void addRangedTask(
      Entity enemy,
      Entity target,
      int priority,
      float range,
      ProjectileType projectileType,
      String eventName) {
    RangedAttackTask task;
    if (eventName == null) {
      task = new RangedAttackTask(target, priority, range, projectileType);
    } else {
      task = new RangedAttackTask(target, priority, range, projectileType, eventName);
    }
    enemy.getComponent(AITaskComponent.class).addTask(task);
  }

  /**
   * Adds a charge task that is active at {@code CHARGE_TASK_PRIORITY} and idle at {@code
   * CHARGE_TASK_IDLE_PRIORITY}.
   *
   * @param enemy the enemy being built; must already have an AI component
   * @param target the entity to charge at
   * @param charge the enemy's charge component from {@link #addChargeParts}
   * @param aggroRadius distance at which the charge begins
   */
  private static void addChargeTask(
      Entity enemy, Entity target, ChargeComponent charge, float aggroRadius) {
    enemy
        .getComponent(AITaskComponent.class)
        .addTask(
            new ChargeTask(
                target, charge, aggroRadius, CHARGE_TASK_PRIORITY, CHARGE_TASK_IDLE_PRIORITY));
  }

  /**
   * Gives an enemy the hazard rules every enemy follows. Every enemy takes hazard damage. Only
   * grounded enemies also avoid hazards, because a flyer would only be stopped by a tile it can fly
   * over.
   *
   * @param enemy the enemy being built, not yet created
   * @param grounded true if the enemy walks on the ground, false if it flies
   */
  private static void addHazardRules(Entity enemy, boolean grounded) {
    // every enemy takes hazard damage
    enemy.addComponent(new HazardContactDamageComponent());
    // flyers would only be stopped by tiles they can fly over
    if (grounded) {
      enemy.addComponent(new HazardAvoidanceComponent());
    }
  }

  /**
   * Finishes an enemy: hazard rules, sprite-based sizing, final scale and scaled collider.
   *
   * @param enemy the enemy being built
   * @param animator its animator, used to size the sprite
   * @param scale width of the enemy in world units
   * @param heightRatio height as a fraction of width, taken from the sprite frame
   * @param collisionScaleX collider width as a fraction of the enemy's width
   * @param collisionScaleY collider height as a fraction of the enemy's height
   * @return the same enemy, ready to return from its creator
   */
  private static Entity finishEnemy(
      Entity enemy,
      AnimationRenderComponent animator,
      float scale,
      float heightRatio,
      float collisionScaleX,
      float collisionScaleY) {
    boolean grounded = enemy.getComponent(PhysicsMovementComponent.class).isGroundedMovement();
    addHazardRules(enemy, grounded);
    animator.scaleEntity();
    enemy.setScale(scale, scale * heightRatio);
    PhysicsUtils.setScaledCollider(enemy, collisionScaleX, collisionScaleY);
    return enemy;
  }

  /**
   * Replaces the Minotaur's collider with a box whose bottom is 16 of the sprite's 80 pixels above
   * the entity's bottom edge, so his feet touch the floor.
   *
   * @param minotaur the Minotaur after its scale has been set
   * @param collisionScaleX collider width as a fraction of the entity's width
   * @param collisionScaleY collider height as a fraction of the entity's height, before the offset
   */
  private static void setFootAlignedCollider(
      Entity minotaur, float collisionScaleX, float collisionScaleY) {
    float pixelHeight = minotaur.getScale().y / MINOTAUR_FRAME_HEIGHT_PIXELS;
    float bottomOffset = MINOTAUR_FOOT_PADDING_PIXELS * pixelHeight;
    float boxWidth = minotaur.getScale().x * collisionScaleX;
    float boxHeight = minotaur.getScale().y * collisionScaleY;
    Vector2 boxPosition = new Vector2(minotaur.getScale().x / 2, bottomOffset + boxHeight / 2);
    minotaur
        .getComponent(ColliderComponent.class)
        .setAsBox(new Vector2(boxWidth, boxHeight), boxPosition);
  }

  /**
   * An enemy with both attacks must out-reach itself with the ranged one: melee range strictly
   * below ranged range. Written as a negated less-than so a NaN range is rejected too.
   *
   * @param meleeRange the melee attack's range
   * @param rangedRange the ranged attack's range
   * @throws IllegalArgumentException if meleeRange is not strictly less than rangedRange
   */
  private static void validateMeleeRangeBelowRangedRange(float meleeRange, float rangedRange) {
    // negated form so Nan is rejected too.
    if (!(meleeRange < rangedRange)) {
      throw new IllegalArgumentException(
          "melee range " + meleeRange + " must be below ranged attack range " + rangedRange);
    }
  }

  /**
   * Makes an enemy leave a tortoise behind when it dies. A tortoise is a world pickup, not an
   * inventory item, so it cannot go through the enemy's {@code InventoryComponent}; this attaches a
   * {@code TortoiseDropComponent} that calls {@code TortoiseFactory.createTortoise(tortoiseId)} at
   * the enemy's position on the {@code "death"} event.
   *
   * @param enemy the enemy to attach the drop to
   * @param tortoiseId the id handed to {@code TortoiseFactory}, so the ledger counts it once
   * @return the same enemy, to allow chaining
   * @throws IllegalArgumentException if enemy is null, or tortoiseId is null or blank
   */
  private static Entity addTortoiseDrop(Entity enemy, String tortoiseId) {
    if (enemy == null) {
      throw new IllegalArgumentException("enemy cannot be null.");
    }
    if (tortoiseId == null || tortoiseId.isBlank()) {
      throw new IllegalArgumentException("Tortoise id cannot be blank or null.");
    }
    // on "death": TortoiseFactory.createTortoise(tortoiseId) at the enemy's position, once
    enemy.addComponent(new TortoiseDropComponent(tortoiseId));
    return enemy;
  }

  /**
   * Registers the shipped enemy spawn names with {@link EntitySpawnRegistry}. Safe to call
   * repeatedly, and a name a team has registered for itself is left alone.
   */
  public static void registerEnemySpawns() {
    EntitySpawnRegistry.registerIfAbsent(SPAWN_SKELETON, EnemyFactory::createSkeleton);
    EntitySpawnRegistry.registerIfAbsent(SPAWN_RANGED_SKELETON, EnemyFactory::createRangedSkeleton);
    EntitySpawnRegistry.registerIfAbsent(SPAWN_HARPY, EnemyFactory::createHarpy);
    EntitySpawnRegistry.registerIfAbsent(SPAWN_RANGED_HARPY, EnemyFactory::createRangedHarpy);
    EntitySpawnRegistry.registerIfAbsent(SPAWN_CENTAUR, EnemyFactory::createCentaur);
    EntitySpawnRegistry.registerIfAbsent(SPAWN_MINOTAUR, EnemyFactory::createMinotaur);
    EntitySpawnRegistry.registerIfAbsent(SPAWN_CYCLOPS, EnemyFactory::createCyclops);
    EntitySpawnRegistry.registerIfAbsent(SPAWN_CERBERUS, EnemyFactory::createCerberus);
    EntitySpawnRegistry.registerIfAbsent(SPAWN_MEDUSA, EnemyFactory::createMedusa);
    EntitySpawnRegistry.registerIfAbsent(SPAWN_ZEUS, EnemyFactory::createZeus);
  }

  /** Static creators only; this class cannot be instantiated. */
  private EnemyFactory() {
    throw new IllegalStateException("Instantiating static util class");
  }
}
