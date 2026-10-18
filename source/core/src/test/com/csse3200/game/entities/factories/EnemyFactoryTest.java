package com.csse3200.game.entities.factories;

import static com.badlogic.gdx.graphics.g2d.Animation.PlayMode.LOOP;
import static com.badlogic.gdx.graphics.g2d.Animation.PlayMode.NORMAL;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.Mockito.mock;

import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.csse3200.game.ai.tasks.AITaskComponent;
import com.csse3200.game.ai.tasks.PriorityTask;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.enemies.*;
import com.csse3200.game.components.enemies.EnemyType;
import com.csse3200.game.components.enemies.animation.*;
import com.csse3200.game.components.enemies.attacks.*;
import com.csse3200.game.components.enemies.tasks.*;
import com.csse3200.game.components.loot.*;
import com.csse3200.game.components.player.InventoryComponent;
import com.csse3200.game.components.player.ItemDropComponent;
import com.csse3200.game.components.tasks.ChaseTask;
import com.csse3200.game.components.tasks.WanderTask;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.configs.BaseEntityConfig;
import com.csse3200.game.entities.configs.NPCConfigs;
import com.csse3200.game.entities.spawn.EntitySpawnRegistry;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.physics.PhysicsLayer;
import com.csse3200.game.physics.PhysicsService;
import com.csse3200.game.physics.components.ColliderComponent;
import com.csse3200.game.physics.components.HitboxComponent;
import com.csse3200.game.physics.components.PhysicsMovementComponent;
import com.csse3200.game.rendering.AnimationRenderComponent;
import com.csse3200.game.rendering.EnemyWeaponAnimationComponent;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Covers {@link EnemyFactory} and nothing else: every creator, every private helper (reached by
 * reflection) and the spawn registration. Friendly NPCs are tested in NPCFactoryTest.
 */
@ExtendWith(GameExtension.class)
class EnemyFactoryTest {
  private static final String EVERY_ENEMY =
      "com.csse3200.game.entities.factories.EnemyFactoryTest#everyEnemy";
  private static final float TOLERANCE = 1e-4f;
  private static final String SKELETON_ATLAS = "images/enemies/skeleton.atlas";
  private static final String SWORD_ATLAS = "images/enemy_weapons/enemy_sword.atlas";
  private static final String BOW_ATLAS = "images/enemy_weapons/enemy_bow.atlas";

  private Entity player;
  private NPCConfigs configs;

  /** Values mutated by the rule-violation tests, put back after each test. */
  private float savedMedusaMelee;

  private float savedMedusaCooldown;
  private float savedZeusMelee;

  @BeforeEach
  void setUp() throws ReflectiveOperationException {
    ServiceLocator.registerPhysicsService(new PhysicsService());
    ServiceLocator.registerRenderService(mock(RenderService.class));
    ServiceLocator.registerResourceService(mock(ResourceService.class));
    ServiceLocator.registerTimeSource(mock(GameTime.class));
    player =
        new Entity()
            .addComponent(new CombatStatsComponent(100, 0))
            .addComponent(new HitboxComponent().setLayer(PhysicsLayer.PLAYER));
    configs = (NPCConfigs) read(null, EnemyFactory.class, "configs");
    savedMedusaMelee = configs.medusa.melee.range;
    savedMedusaCooldown = configs.medusa.ranged.cooldown;
    savedZeusMelee = configs.zeus.melee.range;
  }

  @AfterEach
  void tearDown() {
    configs.medusa.melee.range = savedMedusaMelee;
    configs.medusa.ranged.cooldown = savedMedusaCooldown;
    configs.zeus.melee.range = savedZeusMelee;
    EntitySpawnRegistry.clear();
    EnemyFactory.registerEnemySpawns();
  }

  // ---------------------------------------------------------------------------------------
  // shared data and reflection helpers
  // ---------------------------------------------------------------------------------------

  /** What the factory is expected to build for one enemy. */
  private record Spec(
      String name,
      Function<Entity, Entity> factory,
      String label,
      int gold,
      WeaponType weapon,
      int tier,
      float scale,
      float heightRatio,
      float collisionX,
      float collisionY,
      boolean grounded,
      List<String> animations) {
    @Override
    public String toString() {
      return name;
    }
  }

  static Stream<Spec> everyEnemy() {
    return Stream.of(
        new Spec(
            "skeleton",
            EnemyFactory::createSkeleton,
            "Skeleton",
            3,
            WeaponType.SWORD,
            1,
            1f,
            1f,
            0.45f,
            0.6f,
            true,
            List.of("idlel", "idler", "walkl", "walkr")),
        new Spec(
            "ranged skeleton",
            EnemyFactory::createRangedSkeleton,
            "Ranged Skeleton",
            3,
            WeaponType.BOW,
            1,
            1f,
            1f,
            0.45f,
            0.6f,
            true,
            List.of("idlel", "idler", "walkl", "walkr")),
        new Spec(
            "minotaur",
            EnemyFactory::createMinotaur,
            "Minotaur",
            12,
            WeaponType.AXE,
            2,
            2.5f,
            80f / 96f,
            0.5f,
            0.7f,
            true,
            List.of(
                "minotaur_idle_l",
                "minotaur_idle_r",
                "minotaur_walk_l",
                "minotaur_walk_r",
                "minotaur_charge_l",
                "minotaur_charge_r",
                "minotaur_swing_l",
                "minotaur_swing_r")),
        new Spec(
            "centaur",
            EnemyFactory::createCentaur,
            "Centaur",
            12,
            WeaponType.BOW,
            1,
            4f,
            53f / 66f,
            0.4f,
            0.5f,
            true,
            List.of(
                "centaur_idle_l",
                "centaur_idle_r",
                "centaur_run_l",
                "centaur_run_r",
                "centaur_swing_l",
                "centaur_swing_r",
                "centaur_death_l",
                "centaur_death_r")),
        new Spec(
            "cyclops",
            EnemyFactory::createCyclops,
            "Cyclops",
            12,
            null,
            0,
            2f,
            48f / 64f,
            0.4f,
            0.5f,
            true,
            List.of(
                "cyclops_idle_l",
                "cyclops_idle_r",
                "cyclops_walk_l",
                "cyclops_walk_r",
                "cyclops_rock_l",
                "cyclops_rock_r",
                "cyclops_stomp_l",
                "cyclops_stomp_r",
                "cyclops_death_l",
                "cyclops_death_r",
                "cyclops_taunt_l",
                "cyclops_taunt_r",
                "cyclops_eye_scratch_l",
                "cyclops_eye_scratch_r")),
        new Spec(
            "medusa",
            EnemyFactory::createMedusa,
            "Medusa",
            12,
            null,
            0,
            2f,
            91f / 128f,
            0.4f,
            0.5f,
            true,
            List.of(
                "gorgon_idle_l",
                "gorgon_idle_r",
                "gorgon_walk_l",
                "gorgon_walk_r",
                "gorgon_attack_l",
                "gorgon_attack_r",
                "gorgon_petrify_l",
                "gorgon_petrify_r",
                "gorgon_death_l",
                "gorgon_death_r")),
        new Spec(
            "cerberus",
            EnemyFactory::createCerberus,
            "Cerberus",
            12,
            null,
            0,
            1.5f,
            96f / 150f,
            0.45f,
            0.6f,
            true,
            List.of("cerberus_l", "cerberus_r")),
        new Spec(
            "zeus",
            EnemyFactory::createZeus,
            "Zeus",
            20,
            null,
            0,
            2f,
            68f / 37f,
            0.4f,
            0.5f,
            true,
            List.of(
                "zeus_idle_l",
                "zeus_idle_r",
                "zeus_walk_l",
                "zeus_walk_r",
                "zeus_p_strike_l",
                "zeus_p_strike_r",
                "zeus_slam_l",
                "zeus_slam_r",
                "zeus_death_l",
                "zeus_death_r")),
        new Spec(
            "harpy",
            EnemyFactory::createHarpy,
            "Harpy",
            3,
            WeaponType.SWORD,
            1,
            1f,
            25f / 38f,
            0.45f,
            0.6f,
            false,
            List.of("harpy_y_l", "harpy_y_r")),
        new Spec(
            "ranged harpy",
            EnemyFactory::createRangedHarpy,
            "Ranged Harpy",
            3,
            WeaponType.BOW,
            1,
            1f,
            25f / 38f,
            0.45f,
            0.6f,
            false,
            List.of("harpy_y_l", "harpy_y_r")));
  }

  private BaseEntityConfig configFor(String name) {
    return switch (name) {
      case "skeleton" -> configs.skeleton;
      case "ranged skeleton" -> configs.rangedSkeleton;
      case "minotaur" -> configs.minotaur;
      case "centaur" -> configs.centaur;
      case "cyclops" -> configs.cyclops;
      case "medusa" -> configs.medusa;
      case "cerberus" -> configs.cerberus;
      case "zeus" -> configs.zeus;
      case "harpy" -> configs.harpy;
      case "ranged harpy" -> configs.rangedHarpy;
      default -> throw new IllegalArgumentException(name);
    };
  }

  /** Reads a private field, static when {@code owner} is null. */
  private static Object read(Object owner, Class<?> declaring, String name)
      throws ReflectiveOperationException {
    Field field = declaring.getDeclaredField(name);
    field.setAccessible(true);
    return field.get(owner);
  }

  /** Reads a field by name from the object's class or any parent; null if there is none. */
  private static Object fieldOrNull(Object owner, String name) {
    for (Class<?> type = owner.getClass(); type != null; type = type.getSuperclass()) {
      try {
        Field field = type.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(owner);
      } catch (NoSuchFieldException e) {
        // try the parent class
      } catch (IllegalAccessException e) {
        return null;
      }
    }
    return null;
  }

  @SuppressWarnings("unchecked")
  private static List<PriorityTask> tasksOf(Entity entity) throws ReflectiveOperationException {
    return (List<PriorityTask>)
        read(entity.getComponent(AITaskComponent.class), AITaskComponent.class, "priorityTasks");
  }

  private static long countTasks(Entity entity, Class<? extends PriorityTask> type)
      throws ReflectiveOperationException {
    return tasksOf(entity).stream().filter(type::isInstance).count();
  }

  /** The tasks of one type, in the order they were added. */
  private static List<PriorityTask> tasksOfType(Entity entity, Class<? extends PriorityTask> type)
      throws ReflectiveOperationException {
    List<PriorityTask> found = new ArrayList<>();
    for (PriorityTask task : tasksOf(entity)) {
      if (type.isInstance(task)) {
        found.add(task);
      }
    }
    return found;
  }

  /** A task's priority field, or null if the task keeps it under another name. */
  private static Integer priorityField(PriorityTask task) {
    Object value = fieldOrNull(task, "priority");
    return value instanceof Integer i ? i : null;
  }

  /** Calls a private static EnemyFactory method, throwing whatever it threw. */
  private static Object call(String name, Class<?>[] types, Object... args) throws Throwable {
    Method method = EnemyFactory.class.getDeclaredMethod(name, types);
    method.setAccessible(true);
    try {
      return method.invoke(null, args);
    } catch (InvocationTargetException e) {
      throw e.getCause();
    }
  }

  /** {minX, minY, maxX, maxY} of the box in an entity's collider. */
  private static float[] colliderBounds(Entity entity) throws ReflectiveOperationException {
    FixtureDef definition =
        (FixtureDef)
            read(
                entity.getComponent(ColliderComponent.class),
                ColliderComponent.class,
                "fixtureDef");
    PolygonShape shape = (PolygonShape) definition.shape;
    Vector2 vertex = new Vector2();
    float[] bounds = {Float.MAX_VALUE, Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE};
    for (int i = 0; i < shape.getVertexCount(); i++) {
      shape.getVertex(i, vertex);
      bounds[0] = Math.min(bounds[0], vertex.x);
      bounds[1] = Math.min(bounds[1], vertex.y);
      bounds[2] = Math.max(bounds[2], vertex.x);
      bounds[3] = Math.max(bounds[3], vertex.y);
    }
    return bounds;
  }

  private static float colliderWidth(Entity entity) throws ReflectiveOperationException {
    float[] b = colliderBounds(entity);
    return b[2] - b[0];
  }

  private static float colliderHeight(Entity entity) throws ReflectiveOperationException {
    float[] b = colliderBounds(entity);
    return b[3] - b[1];
  }

  @SuppressWarnings("unchecked")
  private static Map<String, Animation<TextureRegion>> animationsOf(Object animator) {
    Object map = fieldOrNull(animator, "animations");
    assumeTrue(map instanceof Map, "animation map not reachable by reflection");
    return (Map<String, Animation<TextureRegion>>) map;
  }

  private static WeaponItem weaponOf(Entity enemy) {
    return (WeaponItem) enemy.getComponent(InventoryComponent.class).getItem(1);
  }

  private static AnimationRenderComponent newAnimator(String path) throws Throwable {
    return (AnimationRenderComponent)
        call("newAnimator", new Class<?>[] {String.class, boolean.class}, path, true);
  }

  // =======================================================================================
  // EVERY ENEMY
  // =======================================================================================

  @Nested
  @DisplayName("every enemy")
  class EveryEnemy {

    @ParameterizedTest(name = "{0} builds")
    @MethodSource(EVERY_ENEMY)
    void shouldBuild(Spec spec) {
      assertNotNull(spec.factory().apply(player));
    }

    @ParameterizedTest(name = "{0} rejects a null target by name")
    @MethodSource(EVERY_ENEMY)
    void shouldRejectANullTargetNamingTheEnemy(Spec spec) {
      IllegalArgumentException thrown =
          assertThrows(IllegalArgumentException.class, () -> spec.factory().apply(null));

      assertEquals(spec.label() + " needs a target", thrown.getMessage());
    }

    @ParameterizedTest(name = "{0} carries its type label")
    @MethodSource(EVERY_ENEMY)
    void shouldCarryItsTypeLabel(Spec spec) {
      assertEquals(
          spec.label(),
          spec.factory().apply(player).getComponent(EnemyTypeComponent.class).getEnemyLabel());
    }

    @ParameterizedTest(name = "{0} has the shared parts")
    @MethodSource(EVERY_ENEMY)
    void shouldHaveTheSharedParts(Spec spec) {
      Entity enemy = spec.factory().apply(player);

      assertNotNull(enemy.getComponent(CombatStatsComponent.class));
      assertNotNull(enemy.getComponent(InventoryComponent.class));
      assertNotNull(enemy.getComponent(ItemDropComponent.class));
      assertNotNull(enemy.getComponent(EnemyDeathComponent.class));
      assertNotNull(enemy.getComponent(AnimationRenderComponent.class));
      assertNotNull(enemy.getComponent(AITaskComponent.class));
      assertNotNull(enemy.getComponent(HazardContactDamageComponent.class));
    }

    @ParameterizedTest(name = "{0} takes health and base attack from the config")
    @MethodSource(EVERY_ENEMY)
    void shouldTakeHealthAndBaseAttackFromTheConfig(Spec spec) {
      CombatStatsComponent stats =
          spec.factory().apply(player).getComponent(CombatStatsComponent.class);
      BaseEntityConfig config = configFor(spec.name());

      assertEquals(config.health, stats.getHealth(), "health");
      assertEquals(config.baseAttack, stats.getBaseAttack(), "base attack, not health");
    }

    @ParameterizedTest(name = "{0} pays its gold")
    @MethodSource(EVERY_ENEMY)
    void shouldPayItsGold(Spec spec) {
      assertEquals(
          spec.gold(),
          spec.factory().apply(player).getComponent(InventoryComponent.class).getGold());
    }

    @ParameterizedTest(name = "{0} carries the right weapon")
    @MethodSource(EVERY_ENEMY)
    void shouldCarryTheRightWeapon(Spec spec) {
      Entity enemy = spec.factory().apply(player);

      if (spec.weapon() == null) {
        assertNull(weaponOf(enemy), "natural weapons never sit in the inventory");
      } else {
        assertEquals(spec.weapon(), weaponOf(enemy).getWeaponType());
        assertEquals(spec.tier(), weaponOf(enemy).getTier());
      }
    }

    @ParameterizedTest(name = "{0} has its size and collider")
    @MethodSource(EVERY_ENEMY)
    void shouldHaveItsSizeAndCollider(Spec spec) throws ReflectiveOperationException {
      Entity enemy = spec.factory().apply(player);

      assertEquals(spec.scale(), enemy.getScale().x, TOLERANCE);
      assertEquals(spec.scale() * spec.heightRatio(), enemy.getScale().y, TOLERANCE);
      assertEquals(enemy.getScale().x * spec.collisionX(), colliderWidth(enemy), TOLERANCE);
      assertEquals(enemy.getScale().y * spec.collisionY(), colliderHeight(enemy), TOLERANCE);
    }

    @ParameterizedTest(name = "{0} moves the right way and follows the hazard rules")
    @MethodSource(EVERY_ENEMY)
    void shouldMoveTheRightWayAndFollowTheHazardRules(Spec spec) {
      Entity enemy = spec.factory().apply(player);

      assertEquals(
          spec.grounded(), enemy.getComponent(PhysicsMovementComponent.class).isGroundedMovement());
      assertEquals(spec.grounded(), enemy.getComponent(HazardAvoidanceComponent.class) != null);
      assertEquals(!spec.grounded(), enemy.getComponent(FlightComponent.class) != null);
      assertEquals(PhysicsLayer.NPC, enemy.getComponent(HitboxComponent.class).getLayer());
    }

    @ParameterizedTest(name = "{0} registers its animations")
    @MethodSource(EVERY_ENEMY)
    void shouldRegisterItsAnimations(Spec spec) {
      AnimationRenderComponent animator =
          spec.factory().apply(player).getComponent(AnimationRenderComponent.class);

      for (String animation : spec.animations()) {
        assertTrue(animator.hasAnimation(animation), spec.name() + " is missing " + animation);
      }
    }

    @ParameterizedTest(name = "{0} gets its own inventory and animator each build")
    @MethodSource(EVERY_ENEMY)
    void shouldBuildAFreshEntityEveryTime(Spec spec) {
      Entity first = spec.factory().apply(player);
      Entity second = spec.factory().apply(player);

      assertNotSame(first, second);
      assertNotSame(
          first.getComponent(InventoryComponent.class),
          second.getComponent(InventoryComponent.class));
      assertNotSame(
          first.getComponent(AnimationRenderComponent.class),
          second.getComponent(AnimationRenderComponent.class));
    }

    @ParameterizedTest(name = "{0} keeps its windups shorter than its cooldowns")
    @MethodSource(EVERY_ENEMY)
    void shouldKeepWindupsShorterThanCooldowns(Spec spec) {
      Entity enemy = spec.factory().apply(player);
      MeleeAttackComponent melee = enemy.getComponent(MeleeAttackComponent.class);
      RangedAttackComponent ranged = enemy.getComponent(RangedAttackComponent.class);

      assertTrue(
          melee != null || ranged != null || enemy.getComponent(RockAttackComponent.class) != null);
      if (melee != null) {
        assertTrue(melee.getWindupDuration() < melee.getCooldown());
      }
      if (ranged != null) {
        assertTrue(ranged.getWindupDuration() < ranged.getCooldown());
      }
    }
  }

  // =======================================================================================
  // CREATORS
  // =======================================================================================

  @Nested
  @DisplayName("createSkeleton")
  class Skeleton {
    @Test
    void shouldMeleeWithItsSwordFromTheConfig() throws ReflectiveOperationException {
      Entity skeleton = EnemyFactory.createSkeleton(player);
      MeleeAttackComponent melee = skeleton.getComponent(MeleeAttackComponent.class);

      assertEquals(configs.skeleton.melee.range, melee.getRange(), TOLERANCE);
      assertEquals(configs.skeleton.melee.cooldown, melee.getCooldown(), TOLERANCE);
      assertEquals(configs.skeleton.melee.knockback, melee.getKnockback(), TOLERANCE);
      assertEquals(weaponOf(skeleton).getDamage(), melee.getDamage());
      assertNull(skeleton.getComponent(RangedAttackComponent.class));
    }

    @Test
    void shouldHaveTheSkeletonAndSwordControllers() {
      Entity skeleton = EnemyFactory.createSkeleton(player);

      assertNotNull(skeleton.getComponent(SkeletonAnimationController.class));
      assertNotNull(skeleton.getComponent(EnemyWeaponAnimationComponent.class));
      assertNotNull(skeleton.getComponent(SkeletonWeaponAnimationController.class));
    }

    @Test
    void shouldWanderChaseAndMelee() throws ReflectiveOperationException {
      Entity skeleton = EnemyFactory.createSkeleton(player);

      assertEquals(1, countTasks(skeleton, PlatformWanderTask.class));
      assertEquals(1, countTasks(skeleton, ChaseTask.class));
      assertEquals(1, countTasks(skeleton, MeleeAttackTask.class));
      assertEquals(0, countTasks(skeleton, RangedAttackTask.class));
      assertEquals(0, countTasks(skeleton, ChargeTask.class));
    }
  }

  @Nested
  @DisplayName("createRangedSkeleton")
  class RangedSkeleton {
    @Test
    void shouldShootWithItsBowFromTheConfig() {
      Entity skeleton = EnemyFactory.createRangedSkeleton(player);
      RangedAttackComponent ranged = skeleton.getComponent(RangedAttackComponent.class);

      assertEquals(configs.rangedSkeleton.ranged.range, ranged.getRange(), TOLERANCE);
      assertEquals(configs.rangedSkeleton.ranged.cooldown, ranged.getCooldown(), TOLERANCE);
      assertEquals(
          configs.rangedSkeleton.ranged.projectileSpeed, ranged.getProjectileSpeed(), TOLERANCE);
      assertEquals(configs.rangedSkeleton.ranged.aimed, ranged.isAimed());
      assertNull(skeleton.getComponent(MeleeAttackComponent.class));
    }

    @Test
    void shouldHaveTheSkeletonAndBowControllers() {
      Entity skeleton = EnemyFactory.createRangedSkeleton(player);

      assertNotNull(skeleton.getComponent(SkeletonAnimationController.class));
      assertNotNull(skeleton.getComponent(EnemyWeaponAnimationComponent.class));
      assertNotNull(skeleton.getComponent(SkeletonWeaponAnimationController.class));
    }

    @Test
    void shouldWanderChaseAndShootAtThePrimaryPriority() throws ReflectiveOperationException {
      Entity skeleton = EnemyFactory.createRangedSkeleton(player);

      assertEquals(1, countTasks(skeleton, PlatformWanderTask.class));
      assertEquals(1, countTasks(skeleton, ChaseTask.class));
      assertEquals(1, countTasks(skeleton, RangedAttackTask.class));
      assertEquals(0, countTasks(skeleton, MeleeAttackTask.class));
      Integer priority = priorityField(tasksOfType(skeleton, RangedAttackTask.class).getFirst());
      assumeTrue(priority != null);
      assertEquals(15, priority);
    }
  }

  @Nested
  @DisplayName("createHarpy and createRangedHarpy")
  class Harpies {
    @Test
    void shouldGiveTheHarpyFlightAndASword() throws ReflectiveOperationException {
      Entity harpy = EnemyFactory.createHarpy(player);

      assertEquals(
          configs.harpy.flightDamping,
          harpy.getComponent(FlightComponent.class).getLinearDamping(),
          TOLERANCE);
      assertEquals(
          configs.harpy.melee.range,
          harpy.getComponent(MeleeAttackComponent.class).getRange(),
          TOLERANCE);
      assertNull(harpy.getComponent(RangedAttackComponent.class));
      assertNotNull(harpy.getComponent(HarpyAnimationController.class));
      assertEquals(1, countTasks(harpy, MeleeAttackTask.class));
      assertEquals(1, countTasks(harpy, ChaseTask.class));
      assertEquals(0, countTasks(harpy, PlatformWanderTask.class), "flyers do not need a floor");
      assertTrue(tasksOf(harpy).stream().anyMatch(task -> task.getClass() == WanderTask.class));
    }

    @Test
    void shouldGiveTheRangedHarpyFlightAndAnAimedBow() throws ReflectiveOperationException {
      Entity harpy = EnemyFactory.createRangedHarpy(player);
      RangedAttackComponent ranged = harpy.getComponent(RangedAttackComponent.class);

      assertEquals(
          configs.rangedHarpy.flightDamping,
          harpy.getComponent(FlightComponent.class).getLinearDamping(),
          TOLERANCE);
      assertEquals(configs.rangedHarpy.ranged.range, ranged.getRange(), TOLERANCE);
      assertTrue(ranged.isAimed());
      assertNull(harpy.getComponent(MeleeAttackComponent.class));
      assertNotNull(harpy.getComponent(HarpyAnimationController.class));
      assertEquals(1, countTasks(harpy, RangedAttackTask.class));
      Integer priority = priorityField(tasksOfType(harpy, RangedAttackTask.class).getFirst());
      assumeTrue(priority != null);
      assertEquals(15, priority);
    }

    @Test
    void shouldUseTheNormalFrameTimeForBothHarpies() {
      for (Entity harpy :
          List.of(EnemyFactory.createHarpy(player), EnemyFactory.createRangedHarpy(player))) {
        Animation<TextureRegion> left =
            animationsOf(harpy.getComponent(AnimationRenderComponent.class)).get("harpy_y_l");
        assertEquals(0.1f, left.getFrameDuration(), TOLERANCE);
        assertEquals(Animation.PlayMode.LOOP, left.getPlayMode());
      }
    }
  }

  @Nested
  @DisplayName("createMinotaur")
  class Minotaur {
    @Test
    void shouldSwingAnAxeWithNoWindup() {
      Entity minotaur = EnemyFactory.createMinotaur(player);

      assertEquals(0f, weaponOf(minotaur).getWindupDuration(), TOLERANCE);
      assertEquals(
          configs.minotaur.melee.range,
          minotaur.getComponent(MeleeAttackComponent.class).getRange(),
          TOLERANCE);
      assertNotNull(minotaur.getComponent(MinotaurAnimationController.class));
    }

    @Test
    void shouldChargeAndTouchHurt() throws ReflectiveOperationException {
      Entity minotaur = EnemyFactory.createMinotaur(player);

      assertNotNull(minotaur.getComponent(ChargeComponent.class));
      assertNotNull(minotaur.getComponent(TouchAttackComponent.class));
      assertEquals(1, countTasks(minotaur, MeleeAttackTask.class));
      assertEquals(1, countTasks(minotaur, ChargeTask.class));
      assertEquals(1, countTasks(minotaur, ChaseTask.class));
      assertEquals(0, countTasks(minotaur, RangedAttackTask.class));
    }

    @Test
    void shouldStandItsColliderOnItsFeet() throws ReflectiveOperationException {
      Entity minotaur = EnemyFactory.createMinotaur(player);
      float[] bounds = colliderBounds(minotaur);

      float expectedBottom = 8f * minotaur.getScale().y / 80f;
      assertEquals(expectedBottom, bounds[1], TOLERANCE);
      assertEquals(minotaur.getScale().x / 2f, (bounds[0] + bounds[2]) / 2f, TOLERANCE);
    }
  }

  @Nested
  @DisplayName("createCentaur")
  class Centaur {
    @Test
    void shouldShootAndCharge() throws ReflectiveOperationException {
      Entity centaur = EnemyFactory.createCentaur(player);

      assertEquals(
          configs.centaur.ranged.range,
          centaur.getComponent(RangedAttackComponent.class).getRange(),
          TOLERANCE);
      assertNull(centaur.getComponent(MeleeAttackComponent.class));
      assertNotNull(centaur.getComponent(ChargeComponent.class));
      assertNotNull(centaur.getComponent(TouchAttackComponent.class));
      assertNotNull(centaur.getComponent(CentaurAnimationController.class));
      assertEquals(1, countTasks(centaur, RangedAttackTask.class));
      assertEquals(1, countTasks(centaur, ChargeTask.class));
      assertEquals(1, countTasks(centaur, ChaseTask.class));
    }

    @Test
    void shouldUseTheRangedPriority() throws ReflectiveOperationException {
      Entity centaur = EnemyFactory.createCentaur(player);
      Integer priority = priorityField(tasksOfType(centaur, RangedAttackTask.class).getFirst());

      assumeTrue(priority != null);
      assertEquals(12, priority);
    }
  }

  @Nested
  @DisplayName("createCyclops")
  class Cyclops {
    @Test
    void shouldHaveStompRockAndLaser() {
      Entity cyclops = EnemyFactory.createCyclops(player);

      assertNotNull(cyclops.getComponent(MeleeAttackComponent.class));
      assertNotNull(cyclops.getComponent(RockAttackComponent.class));
      assertNotNull(cyclops.getComponent(LaserAttackComponent.class));
      assertNotNull(cyclops.getComponent(EnemyWeaponAnimationComponent.class), "laser overlay");
      assertNotNull(cyclops.getComponent(CyclopsAnimationController.class));
    }

    @Test
    void shouldStompUpCloseWithTheConfigRange() throws ReflectiveOperationException {
      Entity cyclops = EnemyFactory.createCyclops(player);

      assertEquals(
          configs.cyclops.melee.range,
          cyclops.getComponent(MeleeAttackComponent.class).getRange(),
          TOLERANCE);
      assertEquals(1, countTasks(cyclops, MeleeAttackTask.class));
      assertEquals(1, countTasks(cyclops, ChaseTask.class));
    }

    @Test
    void shouldChooseLaserBeforeRockByPriority() throws ReflectiveOperationException {
      Entity cyclops = EnemyFactory.createCyclops(player);
      List<PriorityTask> ranged = tasksOfType(cyclops, RangedAttackTask.class);

      assertEquals(2, ranged.size(), "one for the laser and one for the rock");
      Integer laser = priorityField(ranged.get(0));
      Integer rock = priorityField(ranged.get(1));
      assumeTrue(laser != null && rock != null);
      assertEquals(13, laser);
      assertEquals(12, rock);
    }

    @Test
    void shouldGiveTheLaserOverlayItsOwnAnimations() {
      EnemyWeaponAnimationComponent overlay =
          EnemyFactory.createCyclops(player).getComponent(EnemyWeaponAnimationComponent.class);
      Map<String, Animation<TextureRegion>> animations = animationsOf(overlay);

      assertTrue(animations.containsKey("cyclops_laser_l"));
      assertTrue(animations.containsKey("cyclops_laser_r"));
    }
  }

  @Nested
  @DisplayName("createMedusa")
  class Medusa {
    @Test
    void shouldBiteAndGazeWithMeleeShorterThanRanged() {
      Entity medusa = EnemyFactory.createMedusa(player);
      MeleeAttackComponent melee = medusa.getComponent(MeleeAttackComponent.class);
      RangedAttackComponent ranged = medusa.getComponent(RangedAttackComponent.class);

      assertEquals(configs.medusa.melee.range, melee.getRange(), TOLERANCE);
      assertEquals(configs.medusa.ranged.range, ranged.getRange(), TOLERANCE);
      assertTrue(melee.getRange() < ranged.getRange());
      assertNotNull(medusa.getComponent(MedusaAnimationController.class));
    }

    @Test
    void shouldPetrifyOnTheGaze() {
      assertNotNull(EnemyFactory.createMedusa(player).getComponent(PetrifyEffectComponent.class));
    }

    @Test
    void shouldPaceInsteadOfChasing() throws ReflectiveOperationException {
      Entity medusa = EnemyFactory.createMedusa(player);

      assertEquals(1, countTasks(medusa, BoundedPlatformWanderTask.class));
      assertEquals(0, countTasks(medusa, ChaseTask.class));
      assertEquals(1, countTasks(medusa, RangedAttackTask.class));
      assertEquals(1, countTasks(medusa, MeleeAttackTask.class));
    }

    @Test
    void shouldRejectMeleeRangeNotBelowRangedRange() {
      configs.medusa.melee.range = configs.medusa.ranged.range;

      assertThrows(IllegalArgumentException.class, () -> EnemyFactory.createMedusa(player));
    }

    @Test
    void shouldRejectACooldownShorterThanTwiceThePetrifyDuration() {
      configs.medusa.ranged.cooldown = configs.medusa.petrify.duration;

      assertThrows(IllegalArgumentException.class, () -> EnemyFactory.createMedusa(player));
    }
  }

  @Nested
  @DisplayName("createCerberus")
  class Cerberus {
    @Test
    void shouldOnlyBite() {
      Entity cerberus = EnemyFactory.createCerberus(player);
      MeleeAttackComponent bite = cerberus.getComponent(MeleeAttackComponent.class);

      assertEquals(configs.cerberus.melee.range, bite.getRange(), TOLERANCE);
      assertEquals(configs.cerberus.melee.cooldown, bite.getCooldown(), TOLERANCE);
      assertNull(cerberus.getComponent(RangedAttackComponent.class));
      assertNotNull(cerberus.getComponent(CerberusAnimationController.class));
    }

    @Test
    void shouldPaceInsteadOfChasing() throws ReflectiveOperationException {
      Entity cerberus = EnemyFactory.createCerberus(player);

      assertEquals(1, countTasks(cerberus, BoundedPlatformWanderTask.class));
      assertEquals(1, countTasks(cerberus, MeleeAttackTask.class));
      assertEquals(0, countTasks(cerberus, ChaseTask.class));
      assertEquals(0, countTasks(cerberus, RangedAttackTask.class));
    }
  }

  @Nested
  @DisplayName("createZeus")
  class Zeus {
    @Test
    void shouldHaveSwordAndAimedLightning() {
      Entity zeus = EnemyFactory.createZeus(player);
      MeleeAttackComponent melee = zeus.getComponent(MeleeAttackComponent.class);
      RangedAttackComponent bolt = zeus.getComponent(RangedAttackComponent.class);

      assertEquals(configs.zeus.melee.range, melee.getRange(), TOLERANCE);
      assertEquals(configs.zeus.ranged.range, bolt.getRange(), TOLERANCE);
      assertTrue(melee.getRange() < bolt.getRange());
      assertEquals(configs.zeus.ranged.aimed, bolt.isAimed());
      assertNotNull(zeus.getComponent(ZeusAnimationController.class));
    }

    @Test
    void shouldWanderChaseMeleeAndShoot() throws ReflectiveOperationException {
      Entity zeus = EnemyFactory.createZeus(player);

      assertEquals(1, countTasks(zeus, PlatformWanderTask.class));
      assertEquals(1, countTasks(zeus, ChaseTask.class));
      assertEquals(1, countTasks(zeus, MeleeAttackTask.class));
      assertEquals(1, countTasks(zeus, RangedAttackTask.class));
      assertEquals(0, countTasks(zeus, ChargeTask.class));
    }

    @Test
    void shouldRejectMeleeRangeNotBelowRangedRange() {
      configs.zeus.melee.range = configs.zeus.ranged.range;

      assertThrows(IllegalArgumentException.class, () -> EnemyFactory.createZeus(player));
    }
  }

  // =======================================================================================
  // PRIVATE HELPERS, by reflection
  // =======================================================================================

  @Nested
  @DisplayName("helpers")
  class Helpers {

    @Test
    void requireTargetShouldAcceptATargetAndRejectNull() throws Throwable {
      Class<?>[] types = {Entity.class, String.class};

      assertDoesNotThrow(() -> call("requireTarget", types, player, "Zeus"));
      IllegalArgumentException thrown =
          assertThrows(
              IllegalArgumentException.class, () -> call("requireTarget", types, null, "Zeus"));
      assertEquals("Zeus needs a target", thrown.getMessage());
    }

    @Test
    void loadIndependentAtlasShouldReturnANewAtlasEachCall() throws Throwable {
      Class<?>[] types = {String.class};
      TextureAtlas first = (TextureAtlas) call("loadIndependentAtlas", types, SKELETON_ATLAS);
      TextureAtlas second = (TextureAtlas) call("loadIndependentAtlas", types, SKELETON_ATLAS);

      assertNotSame(first, second);
    }

    @Test
    void loadIndependentAtlasShouldThrowForAMissingFile() {
      assertThrows(
          GdxRuntimeException.class,
          () ->
              call("loadIndependentAtlas", new Class<?>[] {String.class}, "images/no-such.atlas"));
    }

    @Test
    void newEnemyBodyShouldBuildGroundedAndFlyingBodies() throws Throwable {
      Class<?>[] types = {AITaskComponent.class, boolean.class};
      AITaskComponent ai = new AITaskComponent();
      Entity grounded = (Entity) call("newEnemyBody", types, ai, true);
      Entity flying = (Entity) call("newEnemyBody", types, new AITaskComponent(), false);

      assertSame(ai, grounded.getComponent(AITaskComponent.class));
      assertNotNull(grounded.getComponent(ColliderComponent.class));
      assertEquals(PhysicsLayer.NPC, grounded.getComponent(HitboxComponent.class).getLayer());
      assertTrue(grounded.getComponent(PhysicsMovementComponent.class).isGroundedMovement());
      assertFalse(flying.getComponent(PhysicsMovementComponent.class).isGroundedMovement());
      assertEquals(grounded.getScale().x * 0.9f, colliderWidth(grounded), TOLERANCE);
      assertEquals(grounded.getScale().y * 0.7f, colliderHeight(grounded), TOLERANCE);
    }

    @Test
    void createBasePlatformerEnemyShouldWanderAndChase() throws Throwable {
      Class<?>[] types = {Entity.class, float.class};
      Entity base = (Entity) call("createBasePlatformerEnemy", types, player, 0.2f);

      assertTrue(base.getComponent(PhysicsMovementComponent.class).isGroundedMovement());
      assertEquals(1, countTasks(base, PlatformWanderTask.class));
      assertEquals(1, countTasks(base, ChaseTask.class));
      Integer priority = priorityField(tasksOfType(base, ChaseTask.class).getFirst());
      assumeTrue(priority != null);
      assertEquals(10, priority);
    }

    @Test
    void createBasePlatformerEnemyShouldRejectANullTarget() {
      IllegalArgumentException thrown =
          assertThrows(
              IllegalArgumentException.class,
              () ->
                  call(
                      "createBasePlatformerEnemy",
                      new Class<?>[] {Entity.class, float.class},
                      null,
                      0.2f));

      assertEquals("Platformer Enemy needs a target", thrown.getMessage());
    }

    @Test
    void createBaseStationaryEnemyShouldHaveNoTasks() throws Throwable {
      Entity base = (Entity) call("createBaseStationaryEnemy", new Class<?>[0]);

      assertTrue(base.getComponent(PhysicsMovementComponent.class).isGroundedMovement());
      assertTrue(tasksOf(base).isEmpty());
    }

    @Test
    void createBaseFlyingEnemyShouldFlyWithTheGivenDamping() throws Throwable {
      Class<?>[] types = {Entity.class, float.class};
      Entity base = (Entity) call("createBaseFlyingEnemy", types, player, 3f);

      assertFalse(base.getComponent(PhysicsMovementComponent.class).isGroundedMovement());
      assertEquals(3f, base.getComponent(FlightComponent.class).getLinearDamping(), TOLERANCE);
      assertEquals(1, countTasks(base, ChaseTask.class));
      assertTrue(tasksOf(base).stream().anyMatch(task -> task.getClass() == WanderTask.class));
    }

    @Test
    void createBaseFlyingEnemyShouldRejectNegativeDamping() {
      assertThrows(
          RuntimeException.class,
          () ->
              call(
                  "createBaseFlyingEnemy",
                  new Class<?>[] {Entity.class, float.class},
                  player,
                  -1f));
    }

    @Test
    void armedInventoryShouldHoldGoldAndAGeneratedWeaponInSlotOne() throws Throwable {
      Class<?>[] types = {int.class, WeaponType.class, int.class};

      for (WeaponType type : List.of(WeaponType.SWORD, WeaponType.BOW, WeaponType.AXE)) {
        InventoryComponent inventory =
            (InventoryComponent) call("armedInventory", types, 7, type, 2);
        WeaponItem weapon = (WeaponItem) inventory.getItem(1);

        assertEquals(7, inventory.getGold());
        assertEquals(type, weapon.getWeaponType());
        assertEquals(2, weapon.getTier());
      }
    }

    @Test
    void newAnimatorShouldPassOwnershipThrough() throws Throwable {
      Class<?>[] types = {String.class, boolean.class};
      AnimationRenderComponent owning =
          (AnimationRenderComponent) call("newAnimator", types, SKELETON_ATLAS, true);
      AnimationRenderComponent borrowing =
          (AnimationRenderComponent) call("newAnimator", types, SKELETON_ATLAS, false);

      Object ownsOwning = fieldOrNull(owning, "ownsAtlas");
      Object ownsBorrowing = fieldOrNull(borrowing, "ownsAtlas");
      assumeTrue(ownsOwning != null && ownsBorrowing != null);
      assertEquals(true, ownsOwning);
      assertEquals(false, ownsBorrowing);
      assertNotSame(fieldOrNull(owning, "atlas"), fieldOrNull(borrowing, "atlas"));
    }

    @Test
    void addAnimationsShouldRegisterEveryNameOnAnAnimator() throws Throwable {
      Class<?>[] types = {
        AnimationRenderComponent.class, float.class, Animation.PlayMode.class, String[].class
      };
      AnimationRenderComponent animator = newAnimator(SKELETON_ATLAS);

      call("addAnimations", types, animator, 0.1f, LOOP, new String[] {"idlel", "idler"});
      call("addAnimations", types, animator, 0.2f, NORMAL, new String[] {"walkl"});
      call("addAnimations", types, animator, 0.3f, NORMAL, new String[0]);

      assertTrue(animator.hasAnimation("idlel"));
      assertTrue(animator.hasAnimation("idler"));
      assertTrue(animator.hasAnimation("walkl"));
      assertFalse(animator.hasAnimation("walkr"), "nothing registered for the empty call");
    }

    @Test
    void addAnimationsShouldRegisterEveryNameOnAnOverlay() throws Throwable {
      Class<?>[] types = {
        EnemyWeaponAnimationComponent.class, float.class, Animation.PlayMode.class, String[].class
      };
      TextureAtlas atlas =
          (TextureAtlas) call("loadIndependentAtlas", new Class<?>[] {String.class}, SWORD_ATLAS);
      EnemyWeaponAnimationComponent overlay = new EnemyWeaponAnimationComponent(atlas);

      call("addAnimations", types, overlay, 0.25f, NORMAL, new String[] {"sword_l", "sword_r"});
      call("addAnimations", types, overlay, 0.25f, NORMAL, new String[0]);

      Map<String, Animation<TextureRegion>> animations = animationsOf(overlay);
      assertEquals(2, animations.size());
      assertEquals(0.25f, animations.get("sword_l").getFrameDuration(), TOLERANCE);
      assertEquals(Animation.PlayMode.NORMAL, animations.get("sword_r").getPlayMode());
    }

    @Test
    void skeletonWeaponAnimatorShouldBuildTheSwordAndBowOverlays() throws Throwable {
      Class<?>[] types = {String.class, String.class, float.class};
      EnemyWeaponAnimationComponent sword =
          (EnemyWeaponAnimationComponent)
              call("skeletonWeaponAnimator", types, SWORD_ATLAS, "sword", 0.08f);
      EnemyWeaponAnimationComponent bow =
          (EnemyWeaponAnimationComponent)
              call("skeletonWeaponAnimator", types, BOW_ATLAS, "bow", 0.1f);

      Map<String, Animation<TextureRegion>> swordAnimations = animationsOf(sword);
      assertEquals(
          Set.of("default_l", "default_r", "sword_l", "sword_r"), swordAnimations.keySet());
      assertEquals(0.1f, swordAnimations.get("default_l").getFrameDuration(), TOLERANCE);
      assertEquals(Animation.PlayMode.LOOP, swordAnimations.get("default_r").getPlayMode());
      assertEquals(0.08f, swordAnimations.get("sword_l").getFrameDuration(), TOLERANCE);
      assertEquals(Animation.PlayMode.NORMAL, swordAnimations.get("sword_r").getPlayMode());
      assertEquals(Set.of("default_l", "default_r", "bow_l", "bow_r"), animationsOf(bow).keySet());
    }

    @Test
    void meleeAttackShouldCopyTheConfigAndUseTheWeapon() throws Throwable {
      WeaponItem weapon = WeaponItem.natural("Test Bite", 9, 0.1f);
      MeleeAttackComponent melee =
          (MeleeAttackComponent)
              call(
                  "meleeAttack",
                  new Class<?>[] {configs.skeleton.melee.getClass(), WeaponItem.class},
                  configs.skeleton.melee,
                  weapon);

      assertEquals(configs.skeleton.melee.range, melee.getRange(), TOLERANCE);
      assertEquals(configs.skeleton.melee.cooldown, melee.getCooldown(), TOLERANCE);
      assertEquals(configs.skeleton.melee.knockback, melee.getKnockback(), TOLERANCE);
      assertEquals(9, melee.getDamage());
    }

    @Test
    void rangedAttackShouldCopyTheConfigSpeedAndAimFlag() throws Throwable {
      WeaponItem weapon = WeaponItem.natural("Test Bolt", 4, 0.1f);
      Class<?>[] types = {configs.rangedHarpy.ranged.getClass(), WeaponItem.class};
      RangedAttackComponent aimed =
          (RangedAttackComponent) call("rangedAttack", types, configs.rangedHarpy.ranged, weapon);
      RangedAttackComponent flat =
          (RangedAttackComponent)
              call("rangedAttack", types, configs.rangedSkeleton.ranged, weapon);

      assertEquals(configs.rangedHarpy.ranged.range, aimed.getRange(), TOLERANCE);
      assertEquals(
          configs.rangedHarpy.ranged.projectileSpeed, aimed.getProjectileSpeed(), TOLERANCE);
      assertEquals(configs.rangedHarpy.ranged.aimed, aimed.isAimed());
      assertEquals(configs.rangedSkeleton.ranged.aimed, flat.isAimed());
    }

    @Test
    void addChargePartsShouldAddTheChargeAndTheTouchAttack() throws Throwable {
      Entity enemy = new Entity();
      ChargeComponent charge =
          (ChargeComponent)
              call(
                  "addChargeParts",
                  new Class<?>[] {Entity.class, configs.minotaur.charge.getClass()},
                  enemy,
                  configs.minotaur.charge);

      assertSame(charge, enemy.getComponent(ChargeComponent.class));
      assertNotNull(enemy.getComponent(TouchAttackComponent.class));
    }

    @Test
    void addCommonPartsShouldAddStatsTypeInventoryDropDeathAndAnimator() throws Throwable {
      Entity enemy = new Entity();
      InventoryComponent inventory = new InventoryComponent(5);
      AnimationRenderComponent animator = newAnimator(SKELETON_ATLAS);

      call(
          "addCommonParts",
          new Class<?>[] {
            Entity.class,
            BaseEntityConfig.class,
            EnemyType.class,
            InventoryComponent.class,
            AnimationRenderComponent.class
          },
          enemy,
          configs.skeleton,
          EnemyType.SKELETON,
          inventory,
          animator);

      CombatStatsComponent stats = enemy.getComponent(CombatStatsComponent.class);
      assertEquals(configs.skeleton.health, stats.getHealth());
      assertEquals(configs.skeleton.baseAttack, stats.getBaseAttack());
      assertEquals("Skeleton", enemy.getComponent(EnemyTypeComponent.class).getEnemyLabel());
      assertSame(inventory, enemy.getComponent(InventoryComponent.class));
      assertNotNull(enemy.getComponent(ItemDropComponent.class));
      assertNotNull(enemy.getComponent(EnemyDeathComponent.class));
      assertSame(animator, enemy.getComponent(AnimationRenderComponent.class));
    }

    @Test
    void addMeleeTaskShouldAddOneMeleeTaskAtPriorityFifteen() throws Throwable {
      Entity enemy = new Entity().addComponent(new AITaskComponent());

      call(
          "addMeleeTask",
          new Class<?>[] {Entity.class, Entity.class, float.class},
          enemy,
          player,
          2f);

      assertEquals(1, countTasks(enemy, MeleeAttackTask.class));
      Integer priority = priorityField(tasksOfType(enemy, MeleeAttackTask.class).getFirst());
      assumeTrue(priority != null);
      assertEquals(15, priority);
    }

    @Test
    void addRangedTaskShouldAddATaskWithAndWithoutAnEventName() throws Throwable {
      Entity enemy = new Entity().addComponent(new AITaskComponent());
      Class<?>[] types = {
        Entity.class,
        Entity.class,
        int.class,
        float.class,
        com.csse3200.game.components.projectile.ProjectileType.class,
        String.class
      };
      var arrow = com.csse3200.game.components.projectile.ProjectileType.ARROW;

      call("addRangedTask", types, enemy, player, 12, 6f, arrow, null);
      call("addRangedTask", types, enemy, player, 13, 7f, arrow, "laserAttack");

      List<PriorityTask> tasks = tasksOfType(enemy, RangedAttackTask.class);
      assertEquals(2, tasks.size());
      Integer first = priorityField(tasks.get(0));
      Integer second = priorityField(tasks.get(1));
      assumeTrue(first != null && second != null);
      assertEquals(12, first);
      assertEquals(13, second);
    }

    @Test
    void addChargeTaskShouldAddOneChargeTask() throws Throwable {
      Entity enemy = new Entity().addComponent(new AITaskComponent());
      ChargeComponent charge = new ChargeComponent(1f, 0.5f, 3f, 2f, 2f);

      call(
          "addChargeTask",
          new Class<?>[] {Entity.class, Entity.class, ChargeComponent.class, float.class},
          enemy,
          player,
          charge,
          5f);

      assertEquals(1, countTasks(enemy, ChargeTask.class));
    }

    @Test
    void addHazardRulesShouldAvoidHazardsOnlyOnTheGround() throws Throwable {
      Class<?>[] types = {Entity.class, boolean.class};
      Entity walker = new Entity();
      Entity flyer = new Entity();

      call("addHazardRules", types, walker, true);
      call("addHazardRules", types, flyer, false);

      assertNotNull(walker.getComponent(HazardContactDamageComponent.class));
      assertNotNull(walker.getComponent(HazardAvoidanceComponent.class));
      assertNotNull(flyer.getComponent(HazardContactDamageComponent.class));
      assertNull(flyer.getComponent(HazardAvoidanceComponent.class));
    }

    @Test
    void finishEnemyShouldSetTheScaleColliderAndHazardRules() throws Throwable {
      Entity body = (Entity) call("createBaseStationaryEnemy", new Class<?>[0]);
      AnimationRenderComponent animator = newAnimator(SKELETON_ATLAS);
      call(
          "addAnimations",
          new Class<?>[] {
            AnimationRenderComponent.class, float.class, Animation.PlayMode.class, String[].class
          },
          animator,
          0.1f,
          LOOP,
          new String[] {"idlel", "idler"});
      body.addComponent(animator);

      Entity result =
          (Entity)
              call(
                  "finishEnemy",
                  new Class<?>[] {
                    Entity.class,
                    AnimationRenderComponent.class,
                    float.class,
                    float.class,
                    float.class,
                    float.class
                  },
                  body,
                  animator,
                  2f,
                  0.5f,
                  0.4f,
                  0.6f);

      assertSame(body, result);
      assertEquals(2f, body.getScale().x, TOLERANCE);
      assertEquals(1f, body.getScale().y, TOLERANCE);
      assertEquals(0.8f, colliderWidth(body), TOLERANCE);
      assertEquals(0.6f, colliderHeight(body), TOLERANCE);
      assertNotNull(body.getComponent(HazardContactDamageComponent.class));
      assertNotNull(body.getComponent(HazardAvoidanceComponent.class));
    }

    @Test
    void setFootAlignedColliderShouldLiftTheBoxByTheFootPadding() throws Throwable {
      Entity body = (Entity) call("createBaseStationaryEnemy", new Class<?>[0]);
      body.setScale(4f, 3.333f);

      call(
          "setFootAlignedCollider",
          new Class<?>[] {Entity.class, float.class, float.class},
          body,
          0.8f,
          0.7f);

      float[] bounds = colliderBounds(body);
      assertEquals(3.2f, bounds[2] - bounds[0], TOLERANCE);
      assertEquals(3.333f * 0.7f, bounds[3] - bounds[1], TOLERANCE);
      assertEquals(8f * 3.333f / 80f, bounds[1], TOLERANCE);
      assertEquals(2f, (bounds[0] + bounds[2]) / 2f, TOLERANCE);
    }

    @Test
    void validateMeleeRangeBelowRangedRangeShouldAcceptOnlyAStrictlyShorterMeleeRange()
        throws Throwable {
      Class<?>[] types = {float.class, float.class};

      assertDoesNotThrow(() -> call("validateMeleeRangeBelowRangedRange", types, 1f, 2f));
      for (float[] pair : new float[][] {{2f, 2f}, {3f, 2f}, {Float.NaN, 2f}, {1f, Float.NaN}}) {
        assertThrows(
            IllegalArgumentException.class,
            () -> call("validateMeleeRangeBelowRangedRange", types, pair[0], pair[1]),
            pair[0] + " / " + pair[1]);
      }
    }

    @Test
    void validateMeleeRangeBelowRangedRangeShouldNameBothRanges() {
      IllegalArgumentException thrown =
          assertThrows(
              IllegalArgumentException.class,
              () ->
                  call(
                      "validateMeleeRangeBelowRangedRange",
                      new Class<?>[] {float.class, float.class},
                      3f,
                      2f));

      assertEquals("melee range 3.0 must be below ranged attack range 2.0", thrown.getMessage());
    }
  }

  // =======================================================================================
  // SPAWN REGISTRATION AND STRUCTURE
  // =======================================================================================

  @Nested
  @DisplayName("registerEnemySpawns")
  class SpawnRegistration {
    private static final Map<String, String> LABEL_BY_NAME =
        Map.of(
            "skeleton", "Skeleton",
            "ranged-skeleton", "Ranged Skeleton",
            "harpy", "Harpy",
            "ranged-harpy", "Ranged Harpy",
            "centaur", "Centaur",
            "minotaur", "Minotaur",
            "cyclops", "Cyclops",
            "cerberus", "Cerberus",
            "medusa", "Medusa",
            "zeus", "Zeus");

    @Test
    void shouldRegisterExactlyTheTenEnemyNames() {
      EnemyFactory.registerEnemySpawns();

      assertEquals(LABEL_BY_NAME.keySet(), EntitySpawnRegistry.registeredNames());
    }

    @Test
    void shouldBuildTheMatchingEnemyForEachName() {
      EnemyFactory.registerEnemySpawns();

      LABEL_BY_NAME.forEach(
          (name, label) ->
              assertEquals(
                  label,
                  EntitySpawnRegistry.create(name, player)
                      .getComponent(EnemyTypeComponent.class)
                      .getEnemyLabel(),
                  name));
    }

    @Test
    void shouldBeSafeToCallTwice() {
      EnemyFactory.registerEnemySpawns();
      EnemyFactory.registerEnemySpawns();

      assertEquals(10, EntitySpawnRegistry.registeredNames().size());
    }

    @Test
    void shouldLeaveANameAnotherTeamRegisteredAlone() {
      Entity sentinel = new Entity();
      EntitySpawnRegistry.register("skeleton", p -> sentinel);

      EnemyFactory.registerEnemySpawns();

      assertSame(sentinel, EntitySpawnRegistry.create("skeleton", player));
      assertTrue(EntitySpawnRegistry.isRegistered("zeus"));
    }

    @Test
    void shouldRegisterNoNpcName() {
      EnemyFactory.registerEnemySpawns();

      for (String name : EntitySpawnRegistry.registeredNames()) {
        assertFalse(name.startsWith("npc:"), name);
      }
    }
  }

  @Nested
  @DisplayName("structure")
  class Structure {
    @Test
    void shouldNotBeInstantiable() throws ReflectiveOperationException {
      Constructor<EnemyFactory> constructor = EnemyFactory.class.getDeclaredConstructor();
      constructor.setAccessible(true);

      InvocationTargetException thrown =
          assertThrows(InvocationTargetException.class, constructor::newInstance);

      assertInstanceOf(IllegalStateException.class, thrown.getCause());
    }
  }
}
