package com.csse3200.game.entities.factories;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import com.csse3200.game.AttackTestWorld;
import com.csse3200.game.ai.tasks.AITaskComponent;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.attacks.ChargeComponent;
import com.csse3200.game.components.attacks.LaserAttackComponent;
import com.csse3200.game.components.attacks.MeleeAttackComponent;
import com.csse3200.game.components.attacks.RangedAttackComponent;
import com.csse3200.game.components.attacks.RockAttackComponent;
import com.csse3200.game.components.attacks.TouchAttackComponent;
import com.csse3200.game.components.loot.WeaponItem;
import com.csse3200.game.components.loot.WeaponType;
import com.csse3200.game.components.npc.CerberusAnimationController;
import com.csse3200.game.components.npc.EnemyDeathComponent;
import com.csse3200.game.components.npc.EnemyTypeComponent;
import com.csse3200.game.components.npc.FlightComponent;
import com.csse3200.game.components.npc.HarpyAnimationController;
import com.csse3200.game.components.npc.HazardAvoidanceComponent;
import com.csse3200.game.components.npc.HazardContactDamageComponent;
import com.csse3200.game.components.player.InventoryComponent;
import com.csse3200.game.components.player.ItemDropComponent;
import com.csse3200.game.components.projectile.ProjectileType;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.configs.BaseEntityConfig;
import com.csse3200.game.entities.configs.NPCConfigs;
import com.csse3200.game.entities.spawn.DefaultEntitySpawns;
import com.csse3200.game.entities.spawn.EntitySpawnRegistry;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.files.FileLoader;
import com.csse3200.game.physics.PhysicsLayer;
import com.csse3200.game.physics.PhysicsService;
import com.csse3200.game.physics.components.HitboxComponent;
import com.csse3200.game.rendering.AnimationRenderComponent;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Tests that each NPC factory method wires the components, weapons and values its design needs,
 * and, for the two bosses with their own groups below, how their attacks behave once built. "Wired"
 * tests check what the built entity HAS; the entities are not created or run, so no physics
 * stepping happens in them.
 *
 * <p>The file is grouped with JUnit 5 nested classes, so the test report reads as a tree:
 *
 * <ul>
 *   <li><b>every enemy</b>: the rules all ten enemies share, run once per enemy.
 *   <li><b>touch damage</b>: only the charging enemies have it.
 *   <li><b>one group per enemy</b>: skeleton, ranged skeleton, minotaur, centaur, cyclops, medusa,
 *       cerberus, zeus and the harpies. The Cerberus and Zeus groups are split again into config
 *       (the rules {@code NPCs.json} must obey), factory wiring and attacks (behaviour with real
 *       attack components).
 *   <li><b>spawn registry</b>: the NPC spawn names.
 * </ul>
 *
 * <p>Every number is read from {@code configs/NPCs.json} through the config classes rather than
 * written out here, so changing a value in the JSON does not break these tests, and a value that
 * the factory forgets to read does. The exceptions are the behaviour groups for Zeus and the
 * Cerberus, which build real attack components from named constants at the top of each group.
 *
 * <p>The target is a stand-in for the player: it has combat stats and a hitbox on the player layer,
 * because the charging enemies' touch attack is built to hit the player layer.
 *
 * <p>The "attacks" and "bite" groups call {@code AttackTestWorld.create()}, which replaces the
 * registered services. They therefore sit apart from the wiring groups, which use the services
 * registered by {@link #setUp()}.
 */
@ExtendWith(GameExtension.class)
class NPCFactoryTest {
  // Nested classes cannot name a static method of this class by its short name in @MethodSource, so
  // the sources are given by full name here.
  private static final String EVERY_ENEMY =
      "com.csse3200.game.entities.factories.NPCFactoryTest#everyEnemy";
  private static final String NON_CHARGING_ENEMIES =
      "com.csse3200.game.entities.factories.NPCFactoryTest#nonChargingEnemies";
  private static final String ENEMIES_THAT_REJECT_NULL =
      "com.csse3200.game.entities.factories.NPCFactoryTest#newEnemiesAndSkeleton";

  private Entity player;
  private NPCConfigs configs;

  @BeforeEach
  void setUp() {
    ServiceLocator.registerPhysicsService(new PhysicsService());
    ServiceLocator.registerRenderService(mock(RenderService.class));
    ServiceLocator.registerResourceService(mock(ResourceService.class));
    ServiceLocator.registerTimeSource(mock(GameTime.class));
    player =
        new Entity()
            .addComponent(new CombatStatsComponent(100, 0))
            .addComponent(new HitboxComponent().setLayer(PhysicsLayer.PLAYER));
    configs = FileLoader.readClass(NPCConfigs.class, "configs/NPCs.json");
  }

  @AfterEach
  void resetRegistry() {
    EntitySpawnRegistry.clear();
    DefaultEntitySpawns.reset();
  }

  // ---------- shared data ----------

  // Arguments: name, factory, expected type label.
  static Stream<Arguments> everyEnemy() {
    return Stream.of(
        enemy("skeleton", NPCFactory::createSkeleton, "Skeleton"),
        enemy("ranged skeleton", NPCFactory::createRangedSkeleton, "Ranged Skeleton"),
        enemy("minotaur", NPCFactory::createMinotaur, "Minotaur"),
        enemy("centaur", NPCFactory::createCentaur, "Centaur"),
        enemy("cyclops", NPCFactory::createCyclops, "Cyclops"),
        enemy("medusa", NPCFactory::createMedusa, "Medusa"),
        enemy("cerberus", NPCFactory::createCerberus, "Cerberus"),
        enemy("zeus", NPCFactory::createZeus, "Zeus"),
        enemy("harpy", NPCFactory::createHarpy, "Harpy"),
        enemy("ranged harpy", NPCFactory::createRangedHarpy, "Ranged Harpy"));
  }

  static Stream<Arguments> nonChargingEnemies() {
    return everyEnemy()
        .filter(a -> !a.get()[0].equals("minotaur") && !a.get()[0].equals("centaur"));
  }

  static Stream<Arguments> newEnemiesAndSkeleton() {
    return everyEnemy()
        .filter(
            a ->
                a.get()[0].equals("skeleton")
                    || a.get()[0].equals("medusa")
                    || a.get()[0].equals("cerberus")
                    || a.get()[0].equals("zeus")
                    || a.get()[0].equals("harpy")
                    || a.get()[0].equals("ranged harpy"));
  }

  private static Arguments enemy(String name, Function<Entity, Entity> factory, String label) {
    return Arguments.of(name, factory, label);
  }

  /** The config block for an enemy by the name used in everyEnemy(). */
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
      default -> throw new IllegalArgumentException("no config for " + name);
    };
  }

  // =====================================================================================
  // EVERY ENEMY
  // =====================================================================================

  @Nested
  @DisplayName("every enemy")
  class EveryEnemy {

    @ParameterizedTest(name = "{0} builds")
    @MethodSource(EVERY_ENEMY)
    void shouldBuildEveryEnemyWithoutThrowing(
        String name, Function<Entity, Entity> factory, String label) {
      Entity enemy = factory.apply(player);

      assertNotNull(enemy, name + " must be built");
    }

    @ParameterizedTest(name = "{0} has the components every enemy needs")
    @MethodSource(EVERY_ENEMY)
    void shouldWireStatsTypeLootDeathAnimationAndAiOnEveryEnemy(
        String name, Function<Entity, Entity> factory, String label) {
      Entity enemy = factory.apply(player);

      assertNotNull(enemy.getComponent(CombatStatsComponent.class), name + " combat stats");
      assertNotNull(
          enemy.getComponent(EnemyTypeComponent.class),
          name + " needs an enemy type: the death handler reads its label");
      assertNotNull(enemy.getComponent(InventoryComponent.class), name + " inventory");
      assertNotNull(enemy.getComponent(ItemDropComponent.class), name + " item drop");
      assertNotNull(enemy.getComponent(EnemyDeathComponent.class), name + " death handler");
      assertNotNull(enemy.getComponent(AnimationRenderComponent.class), name + " animator");
      assertNotNull(enemy.getComponent(AITaskComponent.class), name + " AI");
    }

    @ParameterizedTest(name = "{0} is labelled {2}")
    @MethodSource(EVERY_ENEMY)
    void shouldLabelEveryEnemyWithItsType(
        String name, Function<Entity, Entity> factory, String label) {
      Entity enemy = factory.apply(player);

      assertEquals(label, enemy.getComponent(EnemyTypeComponent.class).getEnemyLabel());
    }

    @ParameterizedTest(name = "{0} takes its health and attack from the config")
    @MethodSource(EVERY_ENEMY)
    void shouldTakeHealthAndAttackFromTheConfig(
        String name, Function<Entity, Entity> factory, String label) {
      Entity enemy = factory.apply(player);
      BaseEntityConfig config = configFor(name);

      CombatStatsComponent stats = enemy.getComponent(CombatStatsComponent.class);
      assertEquals(config.health, stats.getHealth(), name + " health");
      assertEquals(config.baseAttack, stats.getBaseAttack(), name + " base attack");
    }

    @ParameterizedTest(name = "{0} has a positive size")
    @MethodSource(EVERY_ENEMY)
    void shouldGiveEveryEnemyAPositiveScale(
        String name, Function<Entity, Entity> factory, String label) {
      Entity enemy = factory.apply(player);

      assertTrue(enemy.getScale().x > 0f, name + " width");
      assertTrue(enemy.getScale().y > 0f, name + " height");
    }

    @ParameterizedTest(name = "{0} can attack")
    @MethodSource(EVERY_ENEMY)
    void shouldGiveEveryEnemyAtLeastOneAttack(
        String name, Function<Entity, Entity> factory, String label) {
      Entity enemy = factory.apply(player);

      assertTrue(
          enemy.getComponent(MeleeAttackComponent.class) != null
              || enemy.getComponent(RangedAttackComponent.class) != null
              || enemy.getComponent(RockAttackComponent.class) != null
              || enemy.getComponent(LaserAttackComponent.class) != null,
          name + " needs a melee or a ranged attack");
    }

    @ParameterizedTest(name = "{0} winds up for less time than its cooldown")
    @MethodSource(EVERY_ENEMY)
    void shouldKeepEveryWindupShorterThanItsCooldown(
        String name, Function<Entity, Entity> factory, String label) {
      Entity enemy = factory.apply(player);
      MeleeAttackComponent melee = enemy.getComponent(MeleeAttackComponent.class);
      RangedAttackComponent ranged = enemy.getComponent(RangedAttackComponent.class);
      RockAttackComponent rock = enemy.getComponent(RockAttackComponent.class);
      LaserAttackComponent laser = enemy.getComponent(LaserAttackComponent.class);

      if (melee != null) {
        assertTrue(melee.getWindupDuration() < melee.getCooldown(), name + " melee windup");
      }
      if (ranged != null) {
        assertTrue(ranged.getWindupDuration() < ranged.getCooldown(), name + " ranged windup");
      }
      if (rock != null) {
        assertTrue(rock.getWindupDuration() < rock.getCooldown(), name + " rock windup");
      }
      if (laser != null) {
        assertTrue(laser.getWindupDuration() < laser.getCooldown(), name + " laser windup");
      }
    }

    @ParameterizedTest(name = "{0}: two builds are independent entities")
    @MethodSource(EVERY_ENEMY)
    void shouldBuildAFreshEntityEveryTime(
        String name, Function<Entity, Entity> factory, String label) {
      Entity first = factory.apply(player);
      Entity second = factory.apply(player);

      assertNotSame(first, second, name);
      assertNotSame(
          first.getComponent(InventoryComponent.class),
          second.getComponent(InventoryComponent.class),
          name + " must not share an inventory");
      assertNotSame(
          first.getComponent(AnimationRenderComponent.class),
          second.getComponent(AnimationRenderComponent.class),
          name + " must not share an animator");
    }

    @ParameterizedTest(name = "{0} rejects a null target")
    @MethodSource(ENEMIES_THAT_REJECT_NULL)
    void shouldRejectANullTarget(String name, Function<Entity, Entity> factory, String label) {
      assertThrows(IllegalArgumentException.class, () -> factory.apply(null), name);
    }

    @ParameterizedTest(name = "{0} builds even when its target has no hitbox")
    @MethodSource(EVERY_ENEMY)
    void shouldBuildEveryEnemyForATargetWithNoHitbox(
        String name, Function<Entity, Entity> factory, String label) {
      // A bare target (stats only) must not crash a factory: the layer the touch attack hits is
      // the player layer constant, not something read from the target.
      Entity bareTarget = new Entity().addComponent(new CombatStatsComponent(100, 0));

      assertNotNull(factory.apply(bareTarget), name);
    }
  }

  // =====================================================================================
  // TOUCH DAMAGE: ONLY THE CHARGING ENEMIES
  // =====================================================================================

  @Nested
  @DisplayName("touch damage")
  class TouchDamage {

    @ParameterizedTest(name = "{0} attacks through an attack component, not touch damage")
    @MethodSource(NON_CHARGING_ENEMIES)
    void shouldNotGiveANonChargingEnemyTouchDamage(
        String name, Function<Entity, Entity> factory, String label) {
      Entity enemy = factory.apply(player);

      assertNull(enemy.getComponent(TouchAttackComponent.class), name);
    }

    @Test
    void shouldGiveTheChargingEnemiesATouchAttack() {
      // The charge deals its damage through the touch attack; without one a charge hurts nobody.
      assertNotNull(
          NPCFactory.createMinotaur(player).getComponent(TouchAttackComponent.class), "minotaur");
      assertNotNull(
          NPCFactory.createCentaur(player).getComponent(TouchAttackComponent.class), "centaur");
    }
  }

  // =====================================================================================
  // SKELETON
  // =====================================================================================

  @Nested
  @DisplayName("skeleton")
  class Skeleton {

    @Test
    void shouldArmTheSkeletonWithATierOneSwordItCanDrop() {
      Entity skeleton = NPCFactory.createSkeleton(player);

      InventoryComponent inventory = skeleton.getComponent(InventoryComponent.class);
      assertEquals(3, inventory.getGold());
      WeaponItem sword = (WeaponItem) inventory.getItem(1);
      assertEquals(WeaponType.SWORD, sword.getWeaponType());
      assertEquals(1, sword.getTier());
      MeleeAttackComponent melee = skeleton.getComponent(MeleeAttackComponent.class);
      assertEquals(configs.skeleton.melee.range, melee.getRange(), 1e-6f);
      assertEquals(configs.skeleton.melee.cooldown, melee.getCooldown(), 1e-6f);
      assertEquals(sword.getDamage(), melee.getDamage());
      assertNull(skeleton.getComponent(RangedAttackComponent.class));
    }
  }

  // =====================================================================================
  // RANGED SKELETON
  // =====================================================================================

  @Nested
  @DisplayName("ranged skeleton")
  class RangedSkeleton {

    @Test
    void shouldArmTheRangedSkeletonWithATierOneBowAndApplyTheProjectileSpeed() {
      Entity archer = NPCFactory.createRangedSkeleton(player);

      WeaponItem bow = (WeaponItem) archer.getComponent(InventoryComponent.class).getItem(1);
      assertEquals(WeaponType.BOW, bow.getWeaponType());
      RangedAttackComponent ranged = archer.getComponent(RangedAttackComponent.class);
      assertEquals(configs.rangedSkeleton.ranged.range, ranged.getRange(), 1e-6f);
      assertEquals(configs.rangedSkeleton.ranged.cooldown, ranged.getCooldown(), 1e-6f);
      assertEquals(
          configs.rangedSkeleton.ranged.projectileSpeed, ranged.getProjectileSpeed(), 1e-6f);
      assertEquals(bow.getDamage(), ranged.getDamage());
      assertEquals(bow.getWindupDuration(), ranged.getWindupDuration(), 1e-6f);
      assertNull(archer.getComponent(MeleeAttackComponent.class));
    }
  }

  // =====================================================================================
  // MINOTAUR
  // =====================================================================================

  @Nested
  @DisplayName("minotaur")
  class Minotaur {

    @Test
    void shouldArmTheMinotaurWithATierTwoAxeWithZeroWindupAndDropIt() {
      Entity minotaur = NPCFactory.createMinotaur(player);

      WeaponItem axe = (WeaponItem) minotaur.getComponent(InventoryComponent.class).getItem(1);
      assertEquals(WeaponType.AXE, axe.getWeaponType());
      assertEquals(2, axe.getTier());
      assertEquals(0f, axe.getWindupDuration(), 1e-6f, "the factory removes the axe's windup");
      MeleeAttackComponent melee = minotaur.getComponent(MeleeAttackComponent.class);
      assertEquals(configs.minotaur.melee.range, melee.getRange(), 1e-6f);
      assertEquals(configs.minotaur.melee.cooldown, melee.getCooldown(), 1e-6f);
      assertEquals(configs.minotaur.melee.knockback, melee.getKnockback(), 1e-6f);
      assertEquals(axe.getDamage(), melee.getDamage(), "weapon damage, with no charge bonus");
      assertEquals(0f, melee.getWindupDuration(), 1e-6f);
    }

    @Test
    void shouldGiveTheMinotaurAChargeFromItsConfig() {
      Entity minotaur = NPCFactory.createMinotaur(player);

      ChargeComponent charge = minotaur.getComponent(ChargeComponent.class);
      assertNotNull(charge);
      assertEquals(configs.minotaur.charge.duration, charge.getChargeDuration(), 1e-6f);
      assertEquals(configs.minotaur.charge.windupDuration, charge.getWindupDuration(), 1e-6f);
      assertEquals(
          1.0f,
          charge.getDamageMultiplier(),
          1e-6f,
          "the multiplier only applies while charging, so it reads 1 at rest");
      assertTrue(charge.isEndOnHit(), "a rush ends when it lands unless configured otherwise");
      assertEquals(3, minotaur.getComponent(InventoryComponent.class).getGold());
    }

    @Test
    void shouldMakeTheMinotaurWiderThanItIsTall() {
      Entity minotaur = NPCFactory.createMinotaur(player);

      assertEquals(4f, minotaur.getScale().x, 1e-4f);
      assertEquals(4f * (80f / 96f), minotaur.getScale().y, 1e-4f);
    }
  }

  // =====================================================================================
  // CENTAUR
  // =====================================================================================

  @Nested
  @DisplayName("centaur")
  class Centaur {

    @Test
    void shouldArmTheCentaurWithABowAndApplyItsRangedConfig() {
      Entity centaur = NPCFactory.createCentaur(player);

      WeaponItem bow = (WeaponItem) centaur.getComponent(InventoryComponent.class).getItem(1);
      assertEquals(WeaponType.BOW, bow.getWeaponType(), "a sword here would be rejected");
      RangedAttackComponent ranged = centaur.getComponent(RangedAttackComponent.class);
      assertEquals(configs.centaur.ranged.range, ranged.getRange(), 1e-6f);
      assertEquals(configs.centaur.ranged.cooldown, ranged.getCooldown(), 1e-6f);
      assertEquals(configs.centaur.ranged.knockback, ranged.getKnockback(), 1e-6f);
      assertEquals(configs.centaur.ranged.projectileSpeed, ranged.getProjectileSpeed(), 1e-6f);
      assertEquals(bow.getDamage(), ranged.getDamage());
    }

    @Test
    void shouldGiveTheCentaurAChargeFromItsConfig() {
      Entity centaur = NPCFactory.createCentaur(player);

      ChargeComponent charge = centaur.getComponent(ChargeComponent.class);
      assertNotNull(charge);
      assertEquals(configs.centaur.charge.duration, charge.getChargeDuration(), 1e-6f);
      assertEquals(configs.centaur.charge.windupDuration, charge.getWindupDuration(), 1e-6f);
      assertTrue(charge.isEndOnHit());
      assertNull(centaur.getComponent(MeleeAttackComponent.class));
    }
  }

  // =====================================================================================
  // CYCLOPS: STOMP, ROCK AND LASER
  // =====================================================================================

  @Nested
  @DisplayName("cyclops")
  class Cyclops {

    @Test
    void shouldGiveTheCyclopsAStompFromItsConfig() {
      Entity cyclops = NPCFactory.createCyclops(player);

      MeleeAttackComponent melee = cyclops.getComponent(MeleeAttackComponent.class);
      assertNotNull(melee);
      assertEquals(configs.cyclops.melee.range, melee.getRange(), 1e-6f);
      assertEquals(configs.cyclops.melee.cooldown, melee.getCooldown(), 1e-6f);
      assertEquals(configs.cyclops.melee.knockback, melee.getKnockback(), 1e-6f);
      assertEquals(configs.cyclops.melee.windup, melee.getWindupDuration(), 1e-6f);
    }

    @Test
    void shouldGiveTheCyclopsARockAttackFromItsConfig() {
      Entity cyclops = NPCFactory.createCyclops(player);

      RockAttackComponent rock = cyclops.getComponent(RockAttackComponent.class);
      assertNotNull(rock);
      assertEquals("rockAttack", rock.getEventPrefix());
      assertEquals(configs.cyclops.rock.range, rock.getRange(), 1e-6f);
      assertEquals(configs.cyclops.rock.cooldown, rock.getCooldown(), 1e-6f);
      assertEquals(configs.cyclops.rock.knockback, rock.getKnockback(), 1e-6f);
      assertEquals(configs.cyclops.rock.windup, rock.getWindupDuration(), 1e-6f);
      assertEquals(configs.cyclops.rock.projectileSpeed, rock.getProjectileSpeed(), 1e-6f);
      assertEquals(configs.cyclops.rock.spawnHeightFraction, rock.getSpawnHeightFraction(), 1e-6f);
      assertEquals(configs.cyclops.rock.damageMultiplier, rock.getDamageMultiplier(), 1e-6f);
    }

    @Test
    void shouldGiveTheCyclopsALaserAttackFromItsConfig() {
      Entity cyclops = NPCFactory.createCyclops(player);

      LaserAttackComponent laser = cyclops.getComponent(LaserAttackComponent.class);
      assertNotNull(laser);
      assertEquals("laserAttack", laser.getEventPrefix());
      assertEquals(configs.cyclops.laser.range, laser.getRange(), 1e-6f);
      assertEquals(configs.cyclops.laser.cooldown, laser.getCooldown(), 1e-6f);
      assertEquals(configs.cyclops.laser.knockback, laser.getKnockback(), 1e-6f);
      assertEquals(configs.cyclops.laser.windup, laser.getWindupDuration(), 1e-6f);
      assertEquals(configs.cyclops.laser.projectileSpeed, laser.getProjectileSpeed(), 1e-6f);
      assertEquals(
          configs.cyclops.laser.spawnHeightFraction, laser.getSpawnHeightFraction(), 1e-6f);
      assertEquals(configs.cyclops.laser.damageMultiplier, laser.getDamageMultiplier(), 1e-6f);
    }

    @Test
    void shouldGiveTheCyclopsThreeAttacksAndNoPlainRangedAttack() {
      Entity cyclops = NPCFactory.createCyclops(player);

      assertNotNull(cyclops.getComponent(MeleeAttackComponent.class));
      assertNotNull(cyclops.getComponent(RockAttackComponent.class));
      assertNotNull(cyclops.getComponent(LaserAttackComponent.class));
      assertNull(
          cyclops.getComponent(RangedAttackComponent.class),
          "the base class lookup does not find the rock or laser");
    }

    @Test
    void shouldOrderTheCyclopsRangesStompThenLaserThenRock() {
      // The task priorities 15, 13 and 12 only give stomp, then laser, then rock as the target
      // moves away if the ranges grow in that order.
      Entity cyclops = NPCFactory.createCyclops(player);

      float stomp = cyclops.getComponent(MeleeAttackComponent.class).getRange();
      float laser = cyclops.getComponent(LaserAttackComponent.class).getRange();
      float rock = cyclops.getComponent(RockAttackComponent.class).getRange();
      assertTrue(stomp < laser, "stomp range below laser range");
      assertTrue(laser < rock, "laser range below rock range");
    }

    @Test
    void shouldGiveTheCyclopsGoldOnlyBecauseNaturalWeaponsMustNeverDrop() {
      Entity cyclops = NPCFactory.createCyclops(player);

      InventoryComponent inventory = cyclops.getComponent(InventoryComponent.class);
      assertTrue(inventory.getGold() > 0, "the Cyclops pays out in gold");
      assertNull(inventory.getItem(1), "natural weapons live in the attack components");
    }
  }

  // =====================================================================================
  // MEDUSA
  // =====================================================================================

  @Nested
  @DisplayName("medusa")
  class Medusa {

    @Test
    void shouldGiveMedusaBothAttacksWithMeleeShorterThanRanged() {
      Entity medusa = NPCFactory.createMedusa(player);

      MeleeAttackComponent melee = medusa.getComponent(MeleeAttackComponent.class);
      RangedAttackComponent ranged = medusa.getComponent(RangedAttackComponent.class);
      assertNotNull(melee, "bite");
      assertNotNull(ranged, "gaze");
      assertTrue(melee.getRange() < ranged.getRange());
    }

    @Test
    void shouldKeepMedusasGazeCooldownAtLeastTwiceThePetrifyDuration() {
      Entity medusa = NPCFactory.createMedusa(player);

      RangedAttackComponent ranged = medusa.getComponent(RangedAttackComponent.class);
      assertTrue(ranged.getCooldown() >= 2f * configs.medusa.petrify.duration);
    }

    @Test
    void shouldTakeMedusasAttackNumbersFromTheConfig() {
      Entity medusa = NPCFactory.createMedusa(player);

      MeleeAttackComponent melee = medusa.getComponent(MeleeAttackComponent.class);
      RangedAttackComponent ranged = medusa.getComponent(RangedAttackComponent.class);
      assertEquals(configs.medusa.melee.range, melee.getRange(), 1e-6f);
      assertEquals(configs.medusa.melee.cooldown, melee.getCooldown(), 1e-6f);
      assertEquals(configs.medusa.ranged.range, ranged.getRange(), 1e-6f);
      assertEquals(configs.medusa.ranged.cooldown, ranged.getCooldown(), 1e-6f);
      assertEquals(configs.medusa.ranged.projectileSpeed, ranged.getProjectileSpeed(), 1e-6f);
    }

    @Test
    void shouldGiveMedusaGoldAndNoWeaponDrop() {
      Entity medusa = NPCFactory.createMedusa(player);

      InventoryComponent inventory = medusa.getComponent(InventoryComponent.class);
      assertTrue(inventory.getGold() > 0);
      assertNull(inventory.getItem(1), "natural weapons never drop");
    }
  }

  // =====================================================================================
  // CERBERUS
  // =====================================================================================

  @Nested
  @DisplayName("cerberus")
  class Cerberus {

    // ---------- the numbers in NPCs.json ----------

    @Nested
    @DisplayName("config")
    class Config {

      @Test
      void shouldLoadAMeleeBlock() {
        assertNotNull(configs.cerberus, "cerberus block missing from NPCs.json");
        assertNotNull(configs.cerberus.melee, "cerberus melee block missing");
      }

      @Test
      void shouldHavePositiveHealth() {
        assertTrue(configs.cerberus.health > 0, "cerberus health was " + configs.cerberus.health);
      }

      @Test
      void shouldHavePositiveBaseAttack() {
        assertTrue(configs.cerberus.baseAttack > 0);
      }

      @Test
      void shouldHaveAPositiveMeleeRange() {
        assertTrue(configs.cerberus.melee.range > 0);
      }

      @Test
      void shouldHaveAPositiveMeleeCooldown() {
        assertTrue(configs.cerberus.melee.cooldown > 0);
      }

      @Test
      void shouldNotHaveANegativeKnockback() {
        assertTrue(configs.cerberus.melee.knockback >= 0);
      }

      @Test
      void shouldKeepWindupBelowItsCooldown() {
        assertTrue(
            configs.cerberus.melee.windup < configs.cerberus.melee.cooldown,
            "the melee component rejects a windup that is not shorter than the cooldown");
      }

      @Test
      void shouldNotHaveANegativeWindup() {
        assertTrue(configs.cerberus.melee.windup >= 0);
      }

      @Test
      void shouldReachAtLeastAsFarAsZeusSword() {
        assertTrue(configs.cerberus.melee.range >= configs.zeus.melee.range);
      }

      @Test
      void shouldBiteFasterThanZeusSwings() {
        assertTrue(configs.cerberus.melee.cooldown < configs.zeus.melee.cooldown);
      }
    }

    // ---------- what the factory puts on him ----------

    @Nested
    @DisplayName("factory wiring")
    class Wiring {

      @Test
      void shouldGiveCerberusOnlyAMeleeBiteFromItsConfig() {
        Entity cerberus = NPCFactory.createCerberus(player);

        MeleeAttackComponent melee = cerberus.getComponent(MeleeAttackComponent.class);
        assertNotNull(melee);
        assertEquals(configs.cerberus.melee.range, melee.getRange(), 1e-6f);
        assertEquals(configs.cerberus.melee.cooldown, melee.getCooldown(), 1e-6f);
        assertEquals(configs.cerberus.melee.knockback, melee.getKnockback(), 1e-6f);
        assertNull(cerberus.getComponent(RangedAttackComponent.class));
      }

      @Test
      void shouldTakeTheBiteWindupFromTheConfig() {
        MeleeAttackComponent bite =
            NPCFactory.createCerberus(player).getComponent(MeleeAttackComponent.class);

        assertEquals(configs.cerberus.melee.windup, bite.getWindupDuration(), 1e-6f);
      }

      @Test
      void shouldPayOutGoldAndKeepTheBiteOutOfTheInventory() {
        InventoryComponent inventory =
            NPCFactory.createCerberus(player).getComponent(InventoryComponent.class);

        assertTrue(inventory.getGold() > 0, "the Cerberus should drop some gold");
        assertNull(inventory.getItem(1), "his bite is a natural weapon and must never drop");
      }

      @Test
      void shouldTakeHazardDamage() {
        assertNotNull(
            NPCFactory.createCerberus(player).getComponent(HazardContactDamageComponent.class),
            "every enemy takes hazard damage");
      }

      @Test
      void shouldNotFlyOrCharge() {
        Entity cerberus = NPCFactory.createCerberus(player);

        assertNull(cerberus.getComponent(FlightComponent.class));
        assertNull(cerberus.getComponent(ChargeComponent.class));
      }

      @Test
      void shouldGiveCerberusItsOwnAnimationControllerAndBothFacings() {
        Entity cerberus = NPCFactory.createCerberus(player);

        assertNotNull(cerberus.getComponent(CerberusAnimationController.class));
        AnimationRenderComponent animator = cerberus.getComponent(AnimationRenderComponent.class);
        assertTrue(animator.hasAnimation("cerberus_l"), "left-facing animation");
        assertTrue(animator.hasAnimation("cerberus_r"), "right-facing animation");
      }
    }

    // ---------- his bite in action ----------

    @Nested
    @DisplayName("bite")
    class Bite {
      private static final float RANGE = 4f;
      private static final float COOLDOWN = 1.5f;
      private static final float KNOCKBACK = 2f;
      private static final float WINDUP = 0.5f;
      private static final int BITE_DAMAGE = 12;
      private static final int TARGET_HEALTH = 100;

      private AttackTestWorld world;

      @BeforeEach
      void createWorld() {
        world = AttackTestWorld.create();
      }

      private MeleeAttackComponent newBite() {
        return new MeleeAttackComponent(
            RANGE, COOLDOWN, KNOCKBACK, AttackTestWorld.naturalWeapon(BITE_DAMAGE, WINDUP));
      }

      private Entity newCerberus() {
        return world.newAttacker(newBite());
      }

      private int healthOf(Entity entity) {
        return entity.getComponent(CombatStatsComponent.class).getHealth();
      }

      @Nested
      @DisplayName("construction")
      class Construction {

        @Test
        void shouldStoreTheBitesSettings() {
          MeleeAttackComponent bite = newBite();

          assertEquals(RANGE, bite.getRange(), 1e-6f);
          assertEquals(COOLDOWN, bite.getCooldown(), 1e-6f);
          assertEquals(KNOCKBACK, bite.getKnockback(), 1e-6f);
          assertEquals(WINDUP, bite.getWindupDuration(), 1e-6f);
          assertEquals(BITE_DAMAGE, bite.getDamage());
        }

        @Test
        void shouldRejectAWindupEqualToTheCooldown() {
          assertThrows(
              IllegalArgumentException.class,
              () ->
                  new MeleeAttackComponent(
                      RANGE,
                      WINDUP,
                      KNOCKBACK,
                      AttackTestWorld.naturalWeapon(BITE_DAMAGE, WINDUP)));
        }

        @Test
        void shouldRejectAWindupLongerThanTheCooldown() {
          assertThrows(
              IllegalArgumentException.class,
              () ->
                  new MeleeAttackComponent(
                      RANGE, WINDUP, KNOCKBACK, AttackTestWorld.naturalWeapon(BITE_DAMAGE, 2f)));
        }

        @Test
        void shouldRejectANegativeRange() {
          assertThrows(
              IllegalArgumentException.class,
              () ->
                  new MeleeAttackComponent(
                      -1f,
                      COOLDOWN,
                      KNOCKBACK,
                      AttackTestWorld.naturalWeapon(BITE_DAMAGE, WINDUP)));
        }

        @Test
        void shouldRejectAZeroCooldown() {
          assertThrows(
              IllegalArgumentException.class,
              () ->
                  new MeleeAttackComponent(
                      RANGE, 0f, KNOCKBACK, AttackTestWorld.naturalWeapon(BITE_DAMAGE, WINDUP)));
        }

        @Test
        void shouldRejectANegativeKnockback() {
          assertThrows(
              IllegalArgumentException.class,
              () ->
                  new MeleeAttackComponent(
                      RANGE, COOLDOWN, -1f, AttackTestWorld.naturalWeapon(BITE_DAMAGE, WINDUP)));
        }

        @Test
        void shouldRejectAMissingWeapon() {
          assertThrows(
              IllegalArgumentException.class,
              () -> new MeleeAttackComponent(RANGE, COOLDOWN, KNOCKBACK, null));
        }
      }

      @Nested
      @DisplayName("one bite")
      class OneBite {

        @Test
        void shouldBiteForTheNaturalWeaponsDamageAfterTheWindup() {
          Entity cerberus = newCerberus();
          Entity target = world.newTarget(3f, 0f, TARGET_HEALTH);

          cerberus.getEvents().trigger("meleeAttack", target);
          world.step(AttackTestWorld.framesFor(WINDUP) + 1, cerberus);

          // 12 is the weapon's damage; the owner's own base attack is 5, so 95 would mean it was
          // used.
          assertEquals(TARGET_HEALTH - BITE_DAMAGE, healthOf(target));
        }

        @Test
        void shouldAnnounceTheWindupWithTheTarget() {
          Entity cerberus = newCerberus();
          Entity target = world.newTarget(3f, 0f, TARGET_HEALTH);
          List<Entity> windups = AttackTestWorld.record(cerberus, "meleeAttackWindup");

          cerberus.getEvents().trigger("meleeAttack", target);

          assertEquals(1, windups.size());
          assertSame(target, windups.get(0));
        }

        @Test
        void shouldNotBiteBeforeTheWindupHasElapsed() {
          Entity cerberus = newCerberus();
          Entity target = world.newTarget(3f, 0f, TARGET_HEALTH);

          cerberus.getEvents().trigger("meleeAttack", target);
          world.step(AttackTestWorld.framesFor(WINDUP) - 5, cerberus);

          assertEquals(TARGET_HEALTH, healthOf(target));
        }

        @Test
        void shouldReachATargetExactlyAtRange() {
          Entity cerberus = newCerberus();
          Entity target = world.newTarget(RANGE, 0f, TARGET_HEALTH);

          cerberus.getEvents().trigger("meleeAttack", target);
          world.step(AttackTestWorld.framesFor(WINDUP) + 1, cerberus);

          assertEquals(TARGET_HEALTH - BITE_DAMAGE, healthOf(target));
        }

        @Test
        void shouldRefuseATargetJustBeyondRange() {
          Entity cerberus = newCerberus();
          Entity target = world.newTarget(RANGE + 0.5f, 0f, TARGET_HEALTH);
          List<Entity> windups = AttackTestWorld.record(cerberus, "meleeAttackWindup");

          cerberus.getEvents().trigger("meleeAttack", target);
          world.step(AttackTestWorld.framesFor(WINDUP) + 1, cerberus);

          assertTrue(windups.isEmpty());
          assertEquals(TARGET_HEALTH, healthOf(target));
        }

        @Test
        void shouldWhiffWhenTheTargetLeavesDuringTheWindup() {
          Entity cerberus = newCerberus();
          Entity target = world.newTarget(3f, 0f, TARGET_HEALTH);
          List<Entity> whiffs = AttackTestWorld.record(cerberus, "meleeAttackWhiff");

          cerberus.getEvents().trigger("meleeAttack", target);
          world.step(5, cerberus);
          target.setPosition(30f, 0f);
          world.step(AttackTestWorld.framesFor(WINDUP) + 1, cerberus);

          assertEquals(1, whiffs.size());
          assertEquals(TARGET_HEALTH, healthOf(target));
        }

        @Test
        void shouldWhiffWhenTheTargetDiesDuringTheWindup() {
          Entity cerberus = newCerberus();
          Entity target = world.newTarget(3f, 0f, TARGET_HEALTH);
          List<Entity> whiffs = AttackTestWorld.record(cerberus, "meleeAttackWhiff");

          cerberus.getEvents().trigger("meleeAttack", target);
          target.getComponent(CombatStatsComponent.class).setHealth(0);
          world.step(AttackTestWorld.framesFor(WINDUP) + 1, cerberus);

          assertEquals(1, whiffs.size());
          assertEquals(0, healthOf(target));
        }

        @Test
        void shouldIgnoreASecondTriggerDuringTheWindup() {
          Entity cerberus = newCerberus();
          Entity target = world.newTarget(3f, 0f, TARGET_HEALTH);
          List<Entity> windups = AttackTestWorld.record(cerberus, "meleeAttackWindup");

          cerberus.getEvents().trigger("meleeAttack", target);
          cerberus.getEvents().trigger("meleeAttack", target);
          world.step(AttackTestWorld.framesFor(WINDUP) + 1, cerberus);

          assertEquals(1, windups.size());
          assertEquals(TARGET_HEALTH - BITE_DAMAGE, healthOf(target), "one bite, not two");
        }

        @Test
        void shouldNotThrowForANullTarget() {
          Entity cerberus = newCerberus();

          assertDoesNotThrow(
              () -> {
                cerberus.getEvents().trigger("meleeAttack", (Entity) null);
                world.step(AttackTestWorld.framesFor(WINDUP) + 1, cerberus);
              });
        }
      }

      @Nested
      @DisplayName("the fast cooldown")
      class FastCooldown {

        @Test
        void shouldRefuseABiteBeforeTheCooldownHasPassed() {
          Entity cerberus = newCerberus();
          Entity target = world.newTarget(3f, 0f, TARGET_HEALTH);
          List<Entity> windups = AttackTestWorld.record(cerberus, "meleeAttackWindup");

          cerberus.getEvents().trigger("meleeAttack", target);
          world.step(AttackTestWorld.framesFor(WINDUP) + 1, cerberus);
          world.step(AttackTestWorld.framesFor(1.3f), cerberus);
          cerberus.getEvents().trigger("meleeAttack", target);

          assertEquals(1, windups.size(), "1.3 s after the hit is inside the 1.5 s cooldown");
        }

        @Test
        void shouldBiteAgainOnceTheCooldownHasPassed() {
          Entity cerberus = newCerberus();
          Entity target = world.newTarget(3f, 0f, TARGET_HEALTH);

          cerberus.getEvents().trigger("meleeAttack", target);
          world.step(AttackTestWorld.framesFor(WINDUP) + 1, cerberus);
          world.step(AttackTestWorld.framesFor(COOLDOWN) + 5, cerberus);
          cerberus.getEvents().trigger("meleeAttack", target);
          world.step(AttackTestWorld.framesFor(WINDUP) + 1, cerberus);

          assertEquals(TARGET_HEALTH - 2 * BITE_DAMAGE, healthOf(target));
        }

        @Test
        void shouldKeepBitingEveryCooldownWhileTheTargetStaysInReach() {
          Entity cerberus = newCerberus();
          Entity target = world.newTarget(3f, 0f, TARGET_HEALTH);

          for (int bite = 0; bite < 3; bite++) {
            cerberus.getEvents().trigger("meleeAttack", target);
            world.step(AttackTestWorld.framesFor(WINDUP) + 1, cerberus);
            world.step(AttackTestWorld.framesFor(COOLDOWN) + 5, cerberus);
          }

          assertEquals(TARGET_HEALTH - 3 * BITE_DAMAGE, healthOf(target));
        }
      }

      @Nested
      @DisplayName("knockback")
      class Knockback {

        @Test
        void shouldKnockTheTargetAwayFromTheCerberus() {
          Entity cerberus = newCerberus();
          Entity target = world.newTarget(3f, 0f, TARGET_HEALTH);

          cerberus.getEvents().trigger("meleeAttack", target);
          world.step(AttackTestWorld.framesFor(WINDUP) + 1, cerberus);

          assertTrue(AttackTestWorld.velocityOf(target).x > 0f, "pushed right, away from him");
        }

        @Test
        void shouldNotKnockTheTargetBackWhenKnockbackIsZero() {
          MeleeAttackComponent noKnockback =
              new MeleeAttackComponent(
                  RANGE, COOLDOWN, 0f, AttackTestWorld.naturalWeapon(BITE_DAMAGE, WINDUP));
          Entity cerberus = world.newAttacker(noKnockback);
          Entity target = world.newTarget(3f, 0f, TARGET_HEALTH);

          cerberus.getEvents().trigger("meleeAttack", target);
          world.step(AttackTestWorld.framesFor(WINDUP) + 1, cerberus);

          assertEquals(0f, AttackTestWorld.velocityOf(target).x, 1e-3f);
        }
      }

      @Nested
      @DisplayName("melee only, and stays put")
      class MeleeOnly {

        @Test
        void shouldIgnoreTheRangedTriggerBecauseItHasNoRangedAttack() {
          Entity cerberus = newCerberus();
          Entity target = world.newTarget(3f, 0f, TARGET_HEALTH);
          List<Entity> rangedWindups = AttackTestWorld.record(cerberus, "rangedAttackWindup");
          int before = world.mark();

          assertDoesNotThrow(
              () -> {
                cerberus.getEvents().trigger("rangedAttack", target, ProjectileType.ARROW);
                world.step(AttackTestWorld.framesFor(1f), cerberus);
              });

          assertTrue(rangedWindups.isEmpty());
          assertEquals(0, world.registeredSince(before).size(), "no projectile of any kind");
        }

        @Test
        void shouldStayWhereItIsWhileBiting() {
          Entity cerberus = newCerberus();
          Entity target = world.newTarget(3f, 0f, TARGET_HEALTH);

          cerberus.getEvents().trigger("meleeAttack", target);
          world.step(AttackTestWorld.framesFor(WINDUP + COOLDOWN), cerberus);

          assertEquals(0f, cerberus.getPosition().x, 1e-6f);
          assertEquals(0f, cerberus.getPosition().y, 1e-6f);
        }
      }
    }
  }

  // =====================================================================================
  // ZEUS
  // =====================================================================================

  @Nested
  @DisplayName("zeus")
  class Zeus {

    // ---------- the numbers in NPCs.json ----------

    @Nested
    @DisplayName("config")
    class Config {

      @Test
      void shouldLoadBothAttackBlocks() {
        assertNotNull(configs.zeus, "zeus block missing from NPCs.json");
        assertNotNull(configs.zeus.melee, "zeus melee block missing");
        assertNotNull(configs.zeus.ranged, "zeus ranged block missing");
      }

      @Test
      void shouldHavePositiveHealth() {
        assertTrue(configs.zeus.health > 0, "zeus health was " + configs.zeus.health);
      }

      @Test
      void shouldHavePositiveBaseAttack() {
        assertTrue(configs.zeus.baseAttack > 0, "zeus baseAttack was " + configs.zeus.baseAttack);
      }

      @Test
      void shouldHaveAPositiveMeleeRange() {
        assertTrue(configs.zeus.melee.range > 0);
      }

      @Test
      void shouldHaveAPositiveMeleeCooldown() {
        assertTrue(configs.zeus.melee.cooldown > 0);
      }

      @Test
      void shouldNotHaveANegativeMeleeKnockback() {
        assertTrue(configs.zeus.melee.knockback >= 0);
      }

      @Test
      void shouldHaveAPositiveBoltRange() {
        assertTrue(configs.zeus.ranged.range > 0);
      }

      @Test
      void shouldHaveAPositiveBoltCooldown() {
        assertTrue(configs.zeus.ranged.cooldown > 0);
      }

      @Test
      void shouldHaveAPositiveBoltSpeed() {
        assertTrue(configs.zeus.ranged.projectileSpeed > 0);
      }

      @Test
      void shouldNotHaveANegativeBoltKnockback() {
        assertTrue(configs.zeus.ranged.knockback >= 0);
      }

      @Test
      void shouldKeepMeleeReachBelowBoltRange() {
        assertTrue(
            configs.zeus.melee.range < configs.zeus.ranged.range,
            "the sword's reach must be shorter than the bolt's, or the bolt is never chosen");
      }

      @Test
      void shouldKeepBoltWindupBelowItsCooldown() {
        assertTrue(configs.zeus.ranged.windup < configs.zeus.ranged.cooldown);
      }

      @Test
      void shouldNotHaveANegativeBoltWindup() {
        assertTrue(configs.zeus.ranged.windup >= 0);
      }

      @Test
      void shouldAimTheBolt() {
        assertTrue(configs.zeus.ranged.aimed, "lightning must be aimed to hit above or below");
      }

      @Test
      void shouldKeepDamageMultiplierAndSpawnHeightInRange() {
        assertTrue(configs.zeus.ranged.damageMultiplier > 0);
        assertTrue(configs.zeus.ranged.spawnHeightFraction >= 0f);
        assertTrue(configs.zeus.ranged.spawnHeightFraction <= 1f);
      }
    }

    // ---------- what the factory puts on him ----------

    @Nested
    @DisplayName("factory wiring")
    class Wiring {

      @Test
      void shouldGiveZeusBothAttacksWithMeleeShorterThanRanged() {
        Entity zeus = NPCFactory.createZeus(player);

        MeleeAttackComponent melee = zeus.getComponent(MeleeAttackComponent.class);
        RangedAttackComponent ranged = zeus.getComponent(RangedAttackComponent.class);
        assertNotNull(melee);
        assertNotNull(ranged);
        assertTrue(melee.getRange() < ranged.getRange());
      }

      @Test
      void shouldTakeTheSwordNumbersFromTheConfig() {
        MeleeAttackComponent melee =
            NPCFactory.createZeus(player).getComponent(MeleeAttackComponent.class);

        assertEquals(configs.zeus.melee.range, melee.getRange(), 1e-6f);
        assertEquals(configs.zeus.melee.cooldown, melee.getCooldown(), 1e-6f);
        assertEquals(configs.zeus.melee.knockback, melee.getKnockback(), 1e-6f);
      }

      @Test
      void shouldTakeTheLightningNumbersFromTheConfig() {
        RangedAttackComponent bolt =
            NPCFactory.createZeus(player).getComponent(RangedAttackComponent.class);

        assertEquals(configs.zeus.ranged.range, bolt.getRange(), 1e-6f);
        assertEquals(configs.zeus.ranged.cooldown, bolt.getCooldown(), 1e-6f);
        assertEquals(configs.zeus.ranged.knockback, bolt.getKnockback(), 1e-6f);
        assertEquals(configs.zeus.ranged.projectileSpeed, bolt.getProjectileSpeed(), 1e-6f);
        assertEquals(configs.zeus.ranged.windup, bolt.getWindupDuration(), 1e-6f);
      }

      @Test
      void shouldAimTheLightning() {
        RangedAttackComponent bolt =
            NPCFactory.createZeus(player).getComponent(RangedAttackComponent.class);

        assertEquals(configs.zeus.ranged.aimed, bolt.isAimed());
        assertTrue(bolt.isAimed(), "lightning must be aimed to reach a player above or below");
      }

      @Test
      void shouldCarryASwordItCanDrop() {
        Entity zeus = NPCFactory.createZeus(player);

        WeaponItem sword = (WeaponItem) zeus.getComponent(InventoryComponent.class).getItem(1);
        assertEquals(WeaponType.SWORD, sword.getWeaponType());
        assertEquals(sword.getDamage(), zeus.getComponent(MeleeAttackComponent.class).getDamage());
      }

      @Test
      void shouldKeepTheLightningOutOfTheInventory() {
        InventoryComponent inventory =
            NPCFactory.createZeus(player).getComponent(InventoryComponent.class);

        assertNull(inventory.getItem(2), "his lightning is a natural weapon and must never drop");
      }

      @Test
      void shouldPayOutGold() {
        InventoryComponent inventory =
            NPCFactory.createZeus(player).getComponent(InventoryComponent.class);

        assertTrue(inventory.getGold() > 0, "Zeus should drop some gold");
      }

      @Test
      void shouldTakeHazardDamageAndAvoidHazards() {
        Entity zeus = NPCFactory.createZeus(player);

        assertNotNull(
            zeus.getComponent(HazardContactDamageComponent.class),
            "every enemy takes hazard damage");
        assertNotNull(
            zeus.getComponent(HazardAvoidanceComponent.class), "he walks, so he avoids hazards");
      }

      @Test
      void shouldNotFlyOrCharge() {
        Entity zeus = NPCFactory.createZeus(player);

        assertNull(zeus.getComponent(FlightComponent.class));
        assertNull(zeus.getComponent(ChargeComponent.class));
      }
    }

    // ---------- his two attacks in action ----------

    @Nested
    @DisplayName("attacks")
    class Attacks {
      private static final float MELEE_RANGE = 3f;
      private static final float MELEE_COOLDOWN = 4f;
      private static final float MELEE_KNOCKBACK = 2f;
      private static final float MELEE_WINDUP = 2f;
      private static final int SWORD_DAMAGE = 20;

      private static final float BOLT_RANGE = 9f;
      private static final float BOLT_COOLDOWN = 5f;
      private static final float BOLT_KNOCKBACK = 1f;
      private static final float BOLT_WINDUP = 1f;
      private static final int BOLT_DAMAGE = 10;

      private static final int TARGET_HEALTH = 100;

      private AttackTestWorld world;

      @BeforeEach
      void createWorld() {
        world = AttackTestWorld.create();
      }

      /** Zeus's two attacks on one owner: a sword melee and an aimed natural lightning bolt. */
      private Entity newZeus() {
        MeleeAttackComponent sword =
            new MeleeAttackComponent(
                MELEE_RANGE,
                MELEE_COOLDOWN,
                MELEE_KNOCKBACK,
                AttackTestWorld.naturalWeapon(SWORD_DAMAGE, MELEE_WINDUP));
        RangedAttackComponent bolt =
            new RangedAttackComponent(
                BOLT_RANGE,
                BOLT_COOLDOWN,
                BOLT_KNOCKBACK,
                AttackTestWorld.naturalWeapon(BOLT_DAMAGE, BOLT_WINDUP));
        return world.newAttacker(sword, bolt);
      }

      private void fireBolt(Entity zeus, Entity target) {
        zeus.getEvents().trigger("rangedAttack", target, ProjectileType.LIGHTNING);
      }

      private int healthOf(Entity entity) {
        return entity.getComponent(CombatStatsComponent.class).getHealth();
      }

      @Nested
      @DisplayName("sword")
      class Sword {

        @Test
        void shouldHitForTheSwordsDamageAfterTheWindup() {
          Entity zeus = newZeus();
          Entity target = world.newTarget(2f, 0f, TARGET_HEALTH);

          zeus.getEvents().trigger("meleeAttack", target);
          world.step(AttackTestWorld.framesFor(MELEE_WINDUP) + 1, zeus);

          // 20 is the weapon's damage. The owner's own base attack is 5, so 95 would mean it was
          // used.
          assertEquals(TARGET_HEALTH - SWORD_DAMAGE, healthOf(target));
        }

        @Test
        void shouldAnnounceTheWindupWithTheTarget() {
          Entity zeus = newZeus();
          Entity target = world.newTarget(2f, 0f, TARGET_HEALTH);
          List<Entity> windups = AttackTestWorld.record(zeus, "meleeAttackWindup");

          zeus.getEvents().trigger("meleeAttack", target);

          assertEquals(1, windups.size());
          assertSame(target, windups.get(0));
        }

        @Test
        void shouldNotHitBeforeTheWindupHasElapsed() {
          Entity zeus = newZeus();
          Entity target = world.newTarget(2f, 0f, TARGET_HEALTH);

          zeus.getEvents().trigger("meleeAttack", target);
          world.step(AttackTestWorld.framesFor(MELEE_WINDUP) - 5, zeus);

          assertEquals(TARGET_HEALTH, healthOf(target));
        }

        @Test
        void shouldHitATargetExactlyAtMeleeRange() {
          Entity zeus = newZeus();
          Entity target = world.newTarget(MELEE_RANGE, 0f, TARGET_HEALTH);

          zeus.getEvents().trigger("meleeAttack", target);
          world.step(AttackTestWorld.framesFor(MELEE_WINDUP) + 1, zeus);

          assertEquals(TARGET_HEALTH - SWORD_DAMAGE, healthOf(target));
        }

        @Test
        void shouldRefuseATargetJustBeyondMeleeRange() {
          Entity zeus = newZeus();
          Entity target = world.newTarget(MELEE_RANGE + 0.5f, 0f, TARGET_HEALTH);
          List<Entity> windups = AttackTestWorld.record(zeus, "meleeAttackWindup");

          zeus.getEvents().trigger("meleeAttack", target);
          world.step(AttackTestWorld.framesFor(MELEE_WINDUP) + 1, zeus);

          assertTrue(windups.isEmpty(), "out of reach means no windup");
          assertEquals(TARGET_HEALTH, healthOf(target));
        }

        @Test
        void shouldWhiffWhenTheTargetLeavesReachDuringTheWindup() {
          Entity zeus = newZeus();
          Entity target = world.newTarget(2f, 0f, TARGET_HEALTH);
          List<Entity> whiffs = AttackTestWorld.record(zeus, "meleeAttackWhiff");

          zeus.getEvents().trigger("meleeAttack", target);
          world.step(10, zeus);
          target.setPosition(30f, 0f);
          world.step(AttackTestWorld.framesFor(MELEE_WINDUP) + 1, zeus);

          assertEquals(1, whiffs.size());
          assertEquals(TARGET_HEALTH, healthOf(target), "a whiff deals no damage");
        }

        @Test
        void shouldWhiffWhenTheTargetDiesDuringTheWindup() {
          Entity zeus = newZeus();
          Entity target = world.newTarget(2f, 0f, TARGET_HEALTH);
          List<Entity> whiffs = AttackTestWorld.record(zeus, "meleeAttackWhiff");

          zeus.getEvents().trigger("meleeAttack", target);
          target.getComponent(CombatStatsComponent.class).setHealth(0);
          world.step(AttackTestWorld.framesFor(MELEE_WINDUP) + 1, zeus);

          assertEquals(1, whiffs.size());
          assertEquals(0, healthOf(target), "the dead target is not hit again");
        }

        @Test
        void shouldIgnoreASecondTriggerDuringTheWindup() {
          Entity zeus = newZeus();
          Entity target = world.newTarget(2f, 0f, TARGET_HEALTH);
          List<Entity> windups = AttackTestWorld.record(zeus, "meleeAttackWindup");

          zeus.getEvents().trigger("meleeAttack", target);
          zeus.getEvents().trigger("meleeAttack", target);
          world.step(AttackTestWorld.framesFor(MELEE_WINDUP) + 1, zeus);

          assertEquals(1, windups.size());
          assertEquals(TARGET_HEALTH - SWORD_DAMAGE, healthOf(target), "one swing, one hit");
        }

        @Test
        void shouldHonourTheMeleeCooldown() {
          Entity zeus = newZeus();
          Entity target = world.newTarget(2f, 0f, TARGET_HEALTH);
          List<Entity> windups = AttackTestWorld.record(zeus, "meleeAttackWindup");

          zeus.getEvents().trigger("meleeAttack", target);
          world.step(AttackTestWorld.framesFor(MELEE_WINDUP) + 1, zeus);
          zeus.getEvents().trigger("meleeAttack", target);
          world.step(AttackTestWorld.framesFor(1f), zeus);
          assertEquals(1, windups.size(), "a second swing straight after the hit is refused");

          world.step(AttackTestWorld.framesFor(MELEE_COOLDOWN), zeus);
          zeus.getEvents().trigger("meleeAttack", target);
          world.step(AttackTestWorld.framesFor(MELEE_WINDUP) + 1, zeus);

          assertEquals(2, windups.size(), "after the cooldown he can swing again");
          assertEquals(TARGET_HEALTH - 2 * SWORD_DAMAGE, healthOf(target));
        }

        @Test
        void shouldKnockTheTargetAwayFromHim() {
          Entity zeus = newZeus();
          Entity target = world.newTarget(2f, 0f, TARGET_HEALTH);

          zeus.getEvents().trigger("meleeAttack", target);
          world.step(AttackTestWorld.framesFor(MELEE_WINDUP) + 1, zeus);

          assertTrue(AttackTestWorld.velocityOf(target).x > 0f, "pushed right, away from Zeus");
        }

        @Test
        void shouldNotThrowForANullTarget() {
          Entity zeus = newZeus();

          assertDoesNotThrow(
              () -> {
                zeus.getEvents().trigger("meleeAttack", (Entity) null);
                world.step(AttackTestWorld.framesFor(MELEE_WINDUP) + 1, zeus);
              });
        }
      }

      @Nested
      @DisplayName("lightning")
      class Lightning {

        @Test
        void shouldWindUpThenCallDownOneBolt() {
          Entity zeus = newZeus();
          Entity target = world.newTarget(6f, 0f, TARGET_HEALTH);
          List<Entity> windups = AttackTestWorld.record(zeus, "rangedAttackWindup");
          List<Entity> fired = AttackTestWorld.record(zeus, "rangedAttackFired");
          int before = world.mark();

          fireBolt(zeus, target);
          world.step(AttackTestWorld.framesFor(BOLT_WINDUP) - 5, zeus);
          assertEquals(1, windups.size());
          assertTrue(fired.isEmpty(), "nothing is fired during the windup");
          assertEquals(0, world.registeredSince(before).size());

          world.step(10, zeus);

          assertEquals(1, fired.size());
          assertEquals(1, world.registeredSince(before).size(), "exactly one bolt");
        }

        @Test
        void shouldCallTheBoltDownAboveTheTarget() {
          Entity zeus = newZeus();
          Entity target = world.newTarget(6f, 0f, TARGET_HEALTH);
          int before = world.mark();

          fireBolt(zeus, target);
          world.step(AttackTestWorld.framesFor(BOLT_WINDUP) + 1, zeus);
          Entity bolt = world.registeredSince(before).get(0);

          assertEquals(
              target.getCenterPosition().x,
              bolt.getCenterPosition().x,
              0.2f,
              "the bolt is centred on the target");
          assertTrue(
              bolt.getCenterPosition().y > target.getCenterPosition().y,
              "and starts above the target, not beside it");
        }

        @Test
        void shouldAimAtWhereTheTargetIsWhenTheBoltIsCalled() {
          Entity zeus = newZeus();
          Entity target = world.newTarget(6f, 0f, TARGET_HEALTH);
          int before = world.mark();

          fireBolt(zeus, target);
          world.step(10, zeus);
          target.setPosition(8f, 0f);
          world.step(AttackTestWorld.framesFor(BOLT_WINDUP), zeus);
          Entity bolt = world.registeredSince(before).get(0);

          assertEquals(target.getCenterPosition().x, bolt.getCenterPosition().x, 0.2f);
        }

        @Test
        void shouldRefuseATargetBeyondBoltRange() {
          Entity zeus = newZeus();
          Entity target = world.newTarget(BOLT_RANGE + 5f, 0f, TARGET_HEALTH);
          List<Entity> windups = AttackTestWorld.record(zeus, "rangedAttackWindup");
          int before = world.mark();

          fireBolt(zeus, target);
          world.step(AttackTestWorld.framesFor(BOLT_WINDUP) + 1, zeus);

          assertTrue(windups.isEmpty());
          assertEquals(0, world.registeredSince(before).size());
        }

        @Test
        void shouldAcceptATargetExactlyAtBoltRange() {
          Entity zeus = newZeus();
          Entity target = world.newTarget(BOLT_RANGE, 0f, TARGET_HEALTH);
          List<Entity> windups = AttackTestWorld.record(zeus, "rangedAttackWindup");

          fireBolt(zeus, target);

          assertEquals(1, windups.size());
        }

        @Test
        void shouldIgnoreASecondTriggerDuringTheBoltWindup() {
          Entity zeus = newZeus();
          Entity target = world.newTarget(6f, 0f, TARGET_HEALTH);
          int before = world.mark();

          fireBolt(zeus, target);
          fireBolt(zeus, target);
          world.step(AttackTestWorld.framesFor(BOLT_WINDUP) + 1, zeus);

          assertEquals(1, world.registeredSince(before).size());
        }

        @Test
        void shouldHonourTheBoltCooldown() {
          Entity zeus = newZeus();
          Entity target = world.newTarget(6f, 0f, TARGET_HEALTH);
          int before = world.mark();

          fireBolt(zeus, target);
          world.step(AttackTestWorld.framesFor(BOLT_WINDUP) + 1, zeus);
          fireBolt(zeus, target);
          world.step(AttackTestWorld.framesFor(BOLT_WINDUP) + 1, zeus);
          assertEquals(1, world.registeredSince(before).size(), "the cooldown blocks a second");

          world.step(AttackTestWorld.framesFor(BOLT_COOLDOWN), zeus);
          fireBolt(zeus, target);
          world.step(AttackTestWorld.framesFor(BOLT_WINDUP) + 1, zeus);

          assertEquals(2, world.registeredSince(before).size(), "and he can call another after");
        }

        @Test
        void shouldCancelTheBoltWhenTheTargetDiesDuringTheWindup() {
          Entity zeus = newZeus();
          Entity target = world.newTarget(6f, 0f, TARGET_HEALTH);
          List<Entity> cancelled = AttackTestWorld.record(zeus, "rangedAttackCancelled");
          int before = world.mark();

          fireBolt(zeus, target);
          target.getComponent(CombatStatsComponent.class).setHealth(0);
          world.step(AttackTestWorld.framesFor(BOLT_WINDUP) + 1, zeus);

          assertEquals(1, cancelled.size());
          assertEquals(0, world.registeredSince(before).size(), "no bolt for a dead target");
        }

        @Test
        void shouldNotThrowForANullBoltTarget() {
          Entity zeus = newZeus();

          assertDoesNotThrow(
              () -> {
                zeus.getEvents().trigger("rangedAttack", null, ProjectileType.LIGHTNING);
                world.step(AttackTestWorld.framesFor(BOLT_WINDUP) + 1, zeus);
              });
        }
      }

      @Nested
      @DisplayName("both together")
      class Together {

        @Test
        void shouldNotStartTheBoltWhenOnlyTheSwordIsTriggered() {
          Entity zeus = newZeus();
          Entity target = world.newTarget(2f, 0f, TARGET_HEALTH);
          List<Entity> boltWindups = AttackTestWorld.record(zeus, "rangedAttackWindup");

          zeus.getEvents().trigger("meleeAttack", target);

          assertTrue(boltWindups.isEmpty());
        }

        @Test
        void shouldNotStartTheSwordWhenOnlyTheBoltIsTriggered() {
          Entity zeus = newZeus();
          Entity target = world.newTarget(6f, 0f, TARGET_HEALTH);
          List<Entity> swordWindups = AttackTestWorld.record(zeus, "meleeAttackWindup");

          fireBolt(zeus, target);

          assertTrue(swordWindups.isEmpty());
        }

        @Test
        void shouldKeepTheBoltCooldownAndTheSwordCooldownSeparate() {
          Entity zeus = newZeus();
          Entity target = world.newTarget(2f, 0f, TARGET_HEALTH);
          List<Entity> boltWindups = AttackTestWorld.record(zeus, "rangedAttackWindup");

          zeus.getEvents().trigger("meleeAttack", target);
          world.step(AttackTestWorld.framesFor(MELEE_WINDUP) + 1, zeus);
          fireBolt(zeus, target);

          assertEquals(1, boltWindups.size(), "a sword swing does not spend the bolt's cooldown");
        }
      }
    }
  }

  // =====================================================================================
  // HARPIES
  // =====================================================================================

  @Nested
  @DisplayName("harpies")
  class Harpies {

    @Test
    void shouldGiveTheHarpyAFlightComponentAndASwordFromItsConfig() {
      Entity harpy = NPCFactory.createHarpy(player);

      FlightComponent flight = harpy.getComponent(FlightComponent.class);
      assertNotNull(flight, "harpies hover");
      assertEquals(configs.harpy.flightDamping, flight.getLinearDamping(), 1e-6f);
      WeaponItem sword = (WeaponItem) harpy.getComponent(InventoryComponent.class).getItem(1);
      assertEquals(WeaponType.SWORD, sword.getWeaponType());
      MeleeAttackComponent melee = harpy.getComponent(MeleeAttackComponent.class);
      assertEquals(configs.harpy.melee.range, melee.getRange(), 1e-6f);
      assertEquals(configs.harpy.melee.cooldown, melee.getCooldown(), 1e-6f);
      assertNull(harpy.getComponent(RangedAttackComponent.class));
    }

    @Test
    void shouldGiveTheRangedHarpyAFlightComponentAndAnAimedBowFromItsConfig() {
      Entity harpy = NPCFactory.createRangedHarpy(player);

      FlightComponent flight = harpy.getComponent(FlightComponent.class);
      assertNotNull(flight, "harpies hover");
      assertEquals(configs.rangedHarpy.flightDamping, flight.getLinearDamping(), 1e-6f);
      WeaponItem bow = (WeaponItem) harpy.getComponent(InventoryComponent.class).getItem(1);
      assertEquals(WeaponType.BOW, bow.getWeaponType());
      RangedAttackComponent ranged = harpy.getComponent(RangedAttackComponent.class);
      assertEquals(configs.rangedHarpy.ranged.range, ranged.getRange(), 1e-6f);
      assertEquals(configs.rangedHarpy.ranged.cooldown, ranged.getCooldown(), 1e-6f);
      assertEquals(configs.rangedHarpy.ranged.projectileSpeed, ranged.getProjectileSpeed(), 1e-6f);
      assertEquals(configs.rangedHarpy.ranged.aimed, ranged.isAimed());
      assertTrue(ranged.isAimed(), "a flying shooter needs aimed arrows to hit up and down");
      assertNull(harpy.getComponent(MeleeAttackComponent.class));
    }

    @Test
    void shouldNotGiveGroundEnemiesAFlightComponent() {
      assertNull(NPCFactory.createSkeleton(player).getComponent(FlightComponent.class));
      assertNull(NPCFactory.createMinotaur(player).getComponent(FlightComponent.class));
      assertNull(NPCFactory.createMedusa(player).getComponent(FlightComponent.class));
    }

    @Test
    void shouldGiveBothHarpiesTheHarpyAnimationControllerAndBothFacings() {
      Entity harpy = NPCFactory.createHarpy(player);
      Entity rangedHarpy = NPCFactory.createRangedHarpy(player);

      for (Entity enemy : new Entity[] {harpy, rangedHarpy}) {
        assertNotNull(enemy.getComponent(HarpyAnimationController.class));
        AnimationRenderComponent animator = enemy.getComponent(AnimationRenderComponent.class);
        assertTrue(animator.hasAnimation("harpy_y_l"), "left-facing animation");
        assertTrue(animator.hasAnimation("harpy_y_r"), "right-facing animation");
      }
    }
  }

  // =====================================================================================
  // SPAWN REGISTRY
  // =====================================================================================

  @Nested
  @DisplayName("spawn registry")
  class SpawnRegistry {

    @Test
    void shouldRegisterAllNpcSpawns() {
      NPCFactory.registerNpcSpawns();

      assertTrue(EntitySpawnRegistry.isRegistered("npc:shop"));
      assertTrue(EntitySpawnRegistry.isRegistered("npc:wizard"));
      assertTrue(EntitySpawnRegistry.isRegistered("npc:philosopher"));
      assertTrue(EntitySpawnRegistry.isRegistered("npc:satyr"));
    }
  }
}
