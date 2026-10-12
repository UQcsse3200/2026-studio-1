package com.csse3200.game.entities.factories;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.AttackTestWorld;
import com.csse3200.game.ai.tasks.AITaskComponent;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.enemies.*;
import com.csse3200.game.components.enemies.animation.CerberusAnimationController;
import com.csse3200.game.components.enemies.animation.HarpyAnimationController;
import com.csse3200.game.components.enemies.attacks.*;
import com.csse3200.game.components.loot.WeaponItem;
import com.csse3200.game.components.loot.WeaponType;
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
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.rendering.AnimationRenderComponent;
import com.csse3200.game.rendering.DebugRenderer;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

@ExtendWith(GameExtension.class)
public class EnemyFactoryTest {
  // Nested classes cannot name a static method of this class by its short name in @MethodSource, so
  // the sources are given by full name here.
  private static final String EVERY_ENEMY =
      "com.csse3200.game.entities.factories.EnemyFactoryTest#everyEnemy";
  private static final String NON_CHARGING_ENEMIES =
      "com.csse3200.game.entities.factories.EnemyFactoryTest#nonChargingEnemies";
  private static final String ENEMIES_THAT_REJECT_NULL =
      "com.csse3200.game.entities.factories.EnemyFactoryTest#newEnemiesAndSkeleton";
  private static final String MID_RANGE_SHOOTERS =
      "com.csse3200.game.entities.factories.EnemyFactoryTest#midRangeShooters";

  private Entity player;
  private static NPCConfigs configs;

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

  /**
   * One enemy as every parameterised test in this file sees it: its creator, its type label, and
   * the health and base attack NPCs.json gives it. A single object, so each test reads only the
   * parts it needs.
   */
  private record EnemyCase(
      String name, Function<Entity, Entity> factory, String label, int health, int baseAttack) {
    @Override
    public String toString() {
      return name;
    }
  }

  static Stream<EnemyCase> everyEnemy() {
    return Stream.of(
        new EnemyCase("skeleton", EnemyFactory::createSkeleton, "Skeleton", 30, 5),
        new EnemyCase(
            "ranged skeleton", EnemyFactory::createRangedSkeleton, "Ranged Skeleton", 20, 5),
        new EnemyCase("minotaur", EnemyFactory::createMinotaur, "Minotaur", 55, 5),
        new EnemyCase("centaur", EnemyFactory::createCentaur, "Centaur", 40, 7),
        new EnemyCase("cyclops", EnemyFactory::createCyclops, "Cyclops", 70, 10),
        new EnemyCase("medusa", EnemyFactory::createMedusa, "Medusa", 150, 8),
        new EnemyCase("cerberus", EnemyFactory::createCerberus, "Cerberus", 250, 12),
        new EnemyCase("zeus", EnemyFactory::createZeus, "Zeus", 300, 10),
        new EnemyCase("harpy", EnemyFactory::createHarpy, "Harpy", 30, 6),
        new EnemyCase("ranged harpy", EnemyFactory::createRangedHarpy, "Ranged Harpy", 40, 5));
  }

  /** Every enemy whose name is in the given list, in the order of everyEnemy(). */
  private static Stream<EnemyCase> enemiesNamed(String... names) {
    List<String> wanted = List.of(names);
    return everyEnemy().filter(enemy -> wanted.contains(enemy.name()));
  }

  static Stream<EnemyCase> nonChargingEnemies() {
    return everyEnemy().filter(enemy -> !List.of("minotaur", "centaur").contains(enemy.name()));
  }

  static Stream<EnemyCase> newEnemiesAndSkeleton() {
    return enemiesNamed("skeleton", "medusa", "cerberus", "zeus", "harpy", "ranged harpy");
  }

  /** The enemies that fire something at a target standing at mid range. */
  static Stream<EnemyCase> midRangeShooters() {
    return enemiesNamed("ranged skeleton", "centaur", "cyclops", "medusa", "zeus", "ranged harpy");
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
    void shouldBuildEveryEnemyWithoutThrowing(EnemyCase expected) {
      Entity enemy = expected.factory().apply(player);

      assertNotNull(enemy, expected + " must be built");
    }

    @ParameterizedTest(name = "{0} has the components every enemy needs")
    @MethodSource(EVERY_ENEMY)
    void shouldWireStatsTypeLootDeathAnimationAndAiOnEveryEnemy(EnemyCase expected) {
      Entity enemy = expected.factory().apply(player);

      assertNotNull(enemy.getComponent(CombatStatsComponent.class), expected + " combat stats");
      assertNotNull(
          enemy.getComponent(EnemyTypeComponent.class),
          expected + " needs an enemy type: the death handler reads its label");
      assertNotNull(enemy.getComponent(InventoryComponent.class), expected + " inventory");
      assertNotNull(enemy.getComponent(ItemDropComponent.class), expected + " item drop");
      assertNotNull(enemy.getComponent(EnemyDeathComponent.class), expected + " death handler");
      assertNotNull(enemy.getComponent(AnimationRenderComponent.class), expected + " animator");
      assertNotNull(enemy.getComponent(AITaskComponent.class), expected + " AI");
    }

    @ParameterizedTest(name = "{0} carries its type label")
    @MethodSource(EVERY_ENEMY)
    void shouldLabelEveryEnemyWithItsType(EnemyCase expected) {
      Entity enemy = expected.factory().apply(player);

      assertEquals(expected.label(), enemy.getComponent(EnemyTypeComponent.class).getEnemyLabel());
    }

    @ParameterizedTest(name = "{0} takes its health and attack from the config")
    @MethodSource(EVERY_ENEMY)
    void shouldTakeHealthAndAttackFromTheConfig(EnemyCase expected) {
      Entity enemy = expected.factory().apply(player);
      BaseEntityConfig config = configFor(expected.name());

      CombatStatsComponent stats = enemy.getComponent(CombatStatsComponent.class);
      assertEquals(config.health, stats.getHealth(), expected + " health");
      assertEquals(config.baseAttack, stats.getBaseAttack(), expected + " base attack");
    }

    @ParameterizedTest(name = "{0} has a positive size")
    @MethodSource(EVERY_ENEMY)
    void shouldGiveEveryEnemyAPositiveScale(EnemyCase expected) {
      Entity enemy = expected.factory().apply(player);

      assertTrue(enemy.getScale().x > 0f, expected + " width");
      assertTrue(enemy.getScale().y > 0f, expected + " height");
    }

    @ParameterizedTest(name = "{0} can attack")
    @MethodSource(EVERY_ENEMY)
    void shouldGiveEveryEnemyAtLeastOneAttack(EnemyCase expected) {
      Entity enemy = expected.factory().apply(player);

      assertTrue(
          enemy.getComponent(MeleeAttackComponent.class) != null
              || enemy.getComponent(RangedAttackComponent.class) != null
              || enemy.getComponent(RockAttackComponent.class) != null
              || enemy.getComponent(LaserAttackComponent.class) != null,
          expected + " needs a melee or a ranged attack");
    }

    @ParameterizedTest(name = "{0} winds up for less time than its cooldown")
    @MethodSource(EVERY_ENEMY)
    void shouldKeepEveryWindupShorterThanItsCooldown(EnemyCase expected) {
      Entity enemy = expected.factory().apply(player);
      MeleeAttackComponent melee = enemy.getComponent(MeleeAttackComponent.class);
      RangedAttackComponent ranged = enemy.getComponent(RangedAttackComponent.class);
      RockAttackComponent rock = enemy.getComponent(RockAttackComponent.class);
      LaserAttackComponent laser = enemy.getComponent(LaserAttackComponent.class);

      if (melee != null) {
        assertTrue(melee.getWindupDuration() < melee.getCooldown(), expected + " melee windup");
      }
      if (ranged != null) {
        assertTrue(ranged.getWindupDuration() < ranged.getCooldown(), expected + " ranged windup");
      }
      if (rock != null) {
        assertTrue(rock.getWindupDuration() < rock.getCooldown(), expected + " rock windup");
      }
      if (laser != null) {
        assertTrue(laser.getWindupDuration() < laser.getCooldown(), expected + " laser windup");
      }
    }

    @ParameterizedTest(name = "{0}: two builds are independent entities")
    @MethodSource(EVERY_ENEMY)
    void shouldBuildAFreshEntityEveryTime(EnemyCase expected) {
      Entity first = expected.factory().apply(player);
      Entity second = expected.factory().apply(player);

      assertNotSame(first, second, expected.name());
      assertNotSame(
          first.getComponent(InventoryComponent.class),
          second.getComponent(InventoryComponent.class),
          expected + " must not share an inventory");
      assertNotSame(
          first.getComponent(AnimationRenderComponent.class),
          second.getComponent(AnimationRenderComponent.class),
          expected + " must not share an animator");
    }

    @ParameterizedTest(name = "{0} rejects a null target")
    @MethodSource(ENEMIES_THAT_REJECT_NULL)
    void shouldRejectANullTarget(EnemyCase expected) {
      assertThrows(
          IllegalArgumentException.class, () -> expected.factory().apply(null), expected.name());
    }

    @ParameterizedTest(name = "{0} builds even when its target has no hitbox")
    @MethodSource(EVERY_ENEMY)
    void shouldBuildEveryEnemyForATargetWithNoHitbox(EnemyCase expected) {
      // A bare target (stats only) must not crash a factory: the layer the touch attack hits is
      // the player layer constant, not something read from the target.
      Entity bareTarget = new Entity().addComponent(new CombatStatsComponent(100, 0));

      assertNotNull(expected.factory().apply(bareTarget), expected.name());
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
    void shouldNotGiveANonChargingEnemyTouchDamage(EnemyCase expected) {
      Entity enemy = expected.factory().apply(player);

      assertNull(enemy.getComponent(TouchAttackComponent.class), expected.name());
    }

    @Test
    void shouldGiveTheChargingEnemiesATouchAttack() {
      // The charge deals its damage through the touch attack; without one a charge hurts nobody.
      assertNotNull(
          EnemyFactory.createMinotaur(player).getComponent(TouchAttackComponent.class), "minotaur");
      assertNotNull(
          EnemyFactory.createCentaur(player).getComponent(TouchAttackComponent.class), "centaur");
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
      Entity skeleton = EnemyFactory.createSkeleton(player);

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
      Entity archer = EnemyFactory.createRangedSkeleton(player);

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
      Entity minotaur = EnemyFactory.createMinotaur(player);

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
      Entity minotaur = EnemyFactory.createMinotaur(player);

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
      Entity minotaur = EnemyFactory.createMinotaur(player);

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
      Entity centaur = EnemyFactory.createCentaur(player);

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
      Entity centaur = EnemyFactory.createCentaur(player);

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
      Entity cyclops = EnemyFactory.createCyclops(player);

      MeleeAttackComponent melee = cyclops.getComponent(MeleeAttackComponent.class);
      assertNotNull(melee);
      assertEquals(configs.cyclops.melee.range, melee.getRange(), 1e-6f);
      assertEquals(configs.cyclops.melee.cooldown, melee.getCooldown(), 1e-6f);
      assertEquals(configs.cyclops.melee.knockback, melee.getKnockback(), 1e-6f);
      assertEquals(configs.cyclops.melee.windup, melee.getWindupDuration(), 1e-6f);
    }

    @Test
    void shouldGiveTheCyclopsARockAttackFromItsConfig() {
      Entity cyclops = EnemyFactory.createCyclops(player);

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
      Entity cyclops = EnemyFactory.createCyclops(player);

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
      Entity cyclops = EnemyFactory.createCyclops(player);

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
      Entity cyclops = EnemyFactory.createCyclops(player);

      float stomp = cyclops.getComponent(MeleeAttackComponent.class).getRange();
      float laser = cyclops.getComponent(LaserAttackComponent.class).getRange();
      float rock = cyclops.getComponent(RockAttackComponent.class).getRange();
      assertTrue(stomp < laser, "stomp range below laser range");
      assertTrue(laser < rock, "laser range below rock range");
    }

    @Test
    void shouldGiveTheCyclopsGoldOnlyBecauseNaturalWeaponsMustNeverDrop() {
      Entity cyclops = EnemyFactory.createCyclops(player);

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
      Entity medusa = EnemyFactory.createMedusa(player);

      MeleeAttackComponent melee = medusa.getComponent(MeleeAttackComponent.class);
      RangedAttackComponent ranged = medusa.getComponent(RangedAttackComponent.class);
      assertNotNull(melee, "bite");
      assertNotNull(ranged, "gaze");
      assertTrue(melee.getRange() < ranged.getRange());
    }

    @Test
    void shouldKeepMedusasGazeCooldownAtLeastTwiceThePetrifyDuration() {
      Entity medusa = EnemyFactory.createMedusa(player);

      RangedAttackComponent ranged = medusa.getComponent(RangedAttackComponent.class);
      assertTrue(ranged.getCooldown() >= 2f * configs.medusa.petrify.duration);
    }

    @Test
    void shouldTakeMedusasAttackNumbersFromTheConfig() {
      Entity medusa = EnemyFactory.createMedusa(player);

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
      Entity medusa = EnemyFactory.createMedusa(player);

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
        Entity cerberus = EnemyFactory.createCerberus(player);

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
            EnemyFactory.createCerberus(player).getComponent(MeleeAttackComponent.class);

        assertEquals(configs.cerberus.melee.windup, bite.getWindupDuration(), 1e-6f);
      }

      @Test
      void shouldPayOutGoldAndKeepTheBiteOutOfTheInventory() {
        InventoryComponent inventory =
            EnemyFactory.createCerberus(player).getComponent(InventoryComponent.class);

        assertTrue(inventory.getGold() > 0, "the Cerberus should drop some gold");
        assertNull(inventory.getItem(1), "his bite is a natural weapon and must never drop");
      }

      @Test
      void shouldTakeHazardDamage() {
        assertNotNull(
            EnemyFactory.createCerberus(player).getComponent(HazardContactDamageComponent.class),
            "every enemy takes hazard damage");
      }

      @Test
      void shouldNotFlyOrCharge() {
        Entity cerberus = EnemyFactory.createCerberus(player);

        assertNull(cerberus.getComponent(FlightComponent.class));
        assertNull(cerberus.getComponent(ChargeComponent.class));
      }

      @Test
      void shouldGiveCerberusItsOwnAnimationControllerAndBothFacings() {
        Entity cerberus = EnemyFactory.createCerberus(player);

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
      private static final float RANGE = configs.cerberus.melee.range;
      private static final float COOLDOWN = configs.cerberus.melee.cooldown;
      private static final float KNOCKBACK = configs.cerberus.melee.knockback;
      private static final float WINDUP = configs.cerberus.melee.windup;
      private static final int BITE_DAMAGE = configs.cerberus.baseAttack;
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
        Entity zeus = EnemyFactory.createZeus(player);

        MeleeAttackComponent melee = zeus.getComponent(MeleeAttackComponent.class);
        RangedAttackComponent ranged = zeus.getComponent(RangedAttackComponent.class);
        assertNotNull(melee);
        assertNotNull(ranged);
        assertTrue(melee.getRange() < ranged.getRange());
      }

      @Test
      void shouldTakeTheSwordNumbersFromTheConfig() {
        MeleeAttackComponent melee =
            EnemyFactory.createZeus(player).getComponent(MeleeAttackComponent.class);

        assertEquals(configs.zeus.melee.range, melee.getRange(), 1e-6f);
        assertEquals(configs.zeus.melee.cooldown, melee.getCooldown(), 1e-6f);
        assertEquals(configs.zeus.melee.knockback, melee.getKnockback(), 1e-6f);
      }

      @Test
      void shouldTakeTheLightningNumbersFromTheConfig() {
        RangedAttackComponent bolt =
            EnemyFactory.createZeus(player).getComponent(RangedAttackComponent.class);

        assertEquals(configs.zeus.ranged.range, bolt.getRange(), 1e-6f);
        assertEquals(configs.zeus.ranged.cooldown, bolt.getCooldown(), 1e-6f);
        assertEquals(configs.zeus.ranged.knockback, bolt.getKnockback(), 1e-6f);
        assertEquals(configs.zeus.ranged.projectileSpeed, bolt.getProjectileSpeed(), 1e-6f);
        assertEquals(configs.zeus.ranged.windup, bolt.getWindupDuration(), 1e-6f);
      }

      @Test
      void shouldAimTheLightning() {
        RangedAttackComponent bolt =
            EnemyFactory.createZeus(player).getComponent(RangedAttackComponent.class);

        assertEquals(configs.zeus.ranged.aimed, bolt.isAimed());
        assertTrue(bolt.isAimed(), "lightning must be aimed to reach a player above or below");
      }

      @Test
      void shouldCarryASwordItCanDrop() {
        Entity zeus = EnemyFactory.createZeus(player);

        WeaponItem sword = (WeaponItem) zeus.getComponent(InventoryComponent.class).getItem(1);
        assertEquals(WeaponType.SWORD, sword.getWeaponType());
        assertEquals(sword.getDamage(), zeus.getComponent(MeleeAttackComponent.class).getDamage());
      }

      @Test
      void shouldKeepTheLightningOutOfTheInventory() {
        InventoryComponent inventory =
            EnemyFactory.createZeus(player).getComponent(InventoryComponent.class);

        assertNull(inventory.getItem(2), "his lightning is a natural weapon and must never drop");
      }

      @Test
      void shouldPayOutGold() {
        InventoryComponent inventory =
            EnemyFactory.createZeus(player).getComponent(InventoryComponent.class);

        assertTrue(inventory.getGold() > 0, "Zeus should drop some gold");
      }

      @Test
      void shouldTakeHazardDamageAndAvoidHazards() {
        Entity zeus = EnemyFactory.createZeus(player);

        assertNotNull(
            zeus.getComponent(HazardContactDamageComponent.class),
            "every enemy takes hazard damage");
        assertNotNull(
            zeus.getComponent(HazardAvoidanceComponent.class), "he walks, so he avoids hazards");
      }

      @Test
      void shouldNotFlyOrCharge() {
        Entity zeus = EnemyFactory.createZeus(player);

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
      Entity harpy = EnemyFactory.createHarpy(player);

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
      Entity harpy = EnemyFactory.createRangedHarpy(player);

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
      assertNull(EnemyFactory.createSkeleton(player).getComponent(FlightComponent.class));
      assertNull(EnemyFactory.createMinotaur(player).getComponent(FlightComponent.class));
      assertNull(EnemyFactory.createMedusa(player).getComponent(FlightComponent.class));
    }

    @Test
    void shouldGiveBothHarpiesTheHarpyAnimationControllerAndBothFacings() {
      Entity harpy = EnemyFactory.createHarpy(player);
      Entity rangedHarpy = EnemyFactory.createRangedHarpy(player);

      for (Entity enemy : new Entity[] {harpy, rangedHarpy}) {
        assertNotNull(enemy.getComponent(HarpyAnimationController.class));
        AnimationRenderComponent animator = enemy.getComponent(AnimationRenderComponent.class);
        assertTrue(animator.hasAnimation("harpy_y_l"), "left-facing animation");
        assertTrue(animator.hasAnimation("harpy_y_r"), "right-facing animation");
      }
    }
  }

  // =====================================================================================
  // EVERY ENEMY IN A LIVE WORLD
  // =====================================================================================

  /**
   * Runs every enemy the way the game does: built by {@link EnemyFactory}, created, placed in a
   * physics world beside a target, and updated frame by frame so its own AI decides when to attack.
   * Nothing here triggers an attack event by hand. The expected numbers are the ones in {@code
   * configs/NPCs.json}.
   */
  @Nested
  @DisplayName("in a live world")
  class InALiveWorld {
    private static final int TARGET_HEALTH = 1000;

    /** Centre-to-centre distance that is inside every enemy's shortest attack range. */
    private static final float IN_REACH = 1f;

    /** Beyond every melee range in the config, inside every ranged range. */
    private static final float MID_RANGE = 5f;

    /** Far outside every attack range and every charge aggro radius. */
    private static final float OUT_OF_REACH = 100f;

    /** Long enough for the slowest attack: a 5 second cooldown plus its windup. */
    private static final float LONG_ENOUGH_SECONDS = 8f;

    private AttackTestWorld world;

    @BeforeEach
    void createWorld() {
      world = AttackTestWorld.create();
      // The wander and chase tasks draw debug lines, so the render service must hand one back.
      RenderService renderService = mock(RenderService.class);
      when(renderService.getDebug()).thenReturn(mock(DebugRenderer.class));
      ServiceLocator.registerRenderService(renderService);
    }

    // ---------- the scene every test builds ----------

    /** A factory-built enemy at the origin and its target, both live in the test world. */
    private final class Scene {
      final Entity target;
      final Entity enemy;

      /**
       * @param factory the creator under test
       * @param distance centre-to-centre distance from the enemy to the target, to its right
       */
      Scene(Function<Entity, Entity> factory, float distance) {
        target = world.newTarget(0f, 0f, TARGET_HEALTH);
        enemy = factory.apply(target);
        // Registering runs create() on every component, exactly as spawning in a level does.
        world.register(enemy);
        // There is no floor in the test world, so switch gravity off instead of letting it fall.
        enemy.getComponent(PhysicsComponent.class).getBody().setGravityScale(0f);
        enemy.setPosition(0f, 0f);
        placeTargetAt(distance);
      }

      void placeTargetAt(float distance) {
        Vector2 enemyCentre = enemy.getCenterPosition();
        Vector2 half = target.getScale().cpy().scl(0.5f);
        target.setPosition(enemyCentre.x + distance - half.x, enemyCentre.y - half.y);
      }

      int targetHealth() {
        return target.getComponent(CombatStatsComponent.class).getHealth();
      }

      int damageDealt() {
        return TARGET_HEALTH - targetHealth();
      }

      void run(float seconds) {
        world.step(AttackTestWorld.framesFor(seconds));
      }
    }

    // =====================================================================================
    // EVERY ENEMY
    // =====================================================================================

    @Nested
    @DisplayName("every enemy, built by the factory and run in a world")
    class EveryEnemyCase {

      @ParameterizedTest(name = "{0} spawns with the health and attack in the config")
      @MethodSource(EVERY_ENEMY)
      void shouldSpawnWithTheHealthAndAttackInTheConfig(EnemyCase expected) {
        Scene scene = new Scene(expected.factory(), OUT_OF_REACH);

        CombatStatsComponent stats = scene.enemy.getComponent(CombatStatsComponent.class);
        assertEquals(expected.health(), stats.getHealth(), expected + " health");
        assertEquals(expected.baseAttack(), stats.getBaseAttack(), expected + " base attack");
      }

      @ParameterizedTest(name = "{0} keeps its type label once created")
      @MethodSource(EVERY_ENEMY)
      void shouldKeepItsTypeLabelOnceCreated(EnemyCase expected) {
        Scene scene = new Scene(expected.factory(), OUT_OF_REACH);

        EnemyTypeComponent type = scene.enemy.getComponent(EnemyTypeComponent.class);
        assertNotNull(type, expected + " needs an enemy type");
        assertEquals(expected.label(), type.getEnemyLabel());
      }

      @ParameterizedTest(name = "{0} runs for two seconds without throwing")
      @MethodSource(EVERY_ENEMY)
      void shouldRunItsOwnUpdateLoopWithoutThrowing(EnemyCase expected) {
        Scene scene = new Scene(expected.factory(), IN_REACH);

        assertDoesNotThrow(() -> scene.run(2f), expected.name());
      }

      @ParameterizedTest(name = "{0} hurts a target in reach by itself")
      @MethodSource(EVERY_ENEMY)
      void shouldHurtATargetInReachThroughItsOwnAi(EnemyCase expected) {
        Scene scene = new Scene(expected.factory(), IN_REACH);

        scene.run(LONG_ENOUGH_SECONDS);

        assertTrue(scene.damageDealt() > 0, expected + " never hurt a target standing beside it");
      }

      @ParameterizedTest(name = "{0} leaves a distant target alone")
      @MethodSource(EVERY_ENEMY)
      void shouldLeaveATargetFarOutOfReachAlone(EnemyCase expected) {
        Scene scene = new Scene(expected.factory(), OUT_OF_REACH);
        int before = world.mark();

        scene.run(LONG_ENOUGH_SECONDS);

        assertEquals(0, scene.damageDealt(), expected + " hurt a target 100 units away");
        assertTrue(world.registeredSince(before).isEmpty(), expected + " fired at nothing");
      }

      @ParameterizedTest(name = "{0} survives its own attacks")
      @MethodSource(EVERY_ENEMY)
      void shouldNotHurtItselfWhileAttacking(EnemyCase expected) {
        Scene scene = new Scene(expected.factory(), IN_REACH);

        scene.run(LONG_ENOUGH_SECONDS);

        assertEquals(
            expected.health(),
            scene.enemy.getComponent(CombatStatsComponent.class).getHealth(),
            expected.name());
      }

      @ParameterizedTest(name = "{0} fires at a target at mid range")
      @MethodSource(MID_RANGE_SHOOTERS)
      void shouldFireAProjectileAtATargetAtMidRange(EnemyCase expected) {
        Scene scene = new Scene(expected.factory(), MID_RANGE);
        int before = world.mark();

        scene.run(LONG_ENOUGH_SECONDS);

        assertFalse(
            world.registeredSince(before).isEmpty(), expected + " fired nothing in 8 seconds");
      }
    }

    // =====================================================================================
    // CERBERUS: range 4, cooldown 1.5, knockback 2, windup 0.5, bite 12
    // =====================================================================================

    @Nested
    @DisplayName("cerberus, live")
    class LiveCerberus {
      private static final float RANGE = 4f;
      private static final float COOLDOWN = 1.5f;
      private static final float WINDUP = 0.5f;
      private static final int BITE_DAMAGE = 12;

      /** A few frames of slack either side of a timing boundary. */
      private static final float SLACK = 0.1f;

      private Scene cerberusWithTargetAt(float distance) {
        return new Scene(EnemyFactory::createCerberus, distance);
      }

      @Test
      void shouldNotBiteBeforeItsWindupHasPassed() {
        Scene scene = cerberusWithTargetAt(IN_REACH);

        scene.run(WINDUP - SLACK);

        assertEquals(0, scene.damageDealt());
      }

      @Test
      void shouldBiteOnceForItsConfiguredDamageAfterTheWindup() {
        Scene scene = cerberusWithTargetAt(IN_REACH);

        scene.run(WINDUP + SLACK);

        assertEquals(BITE_DAMAGE, scene.damageDealt());
      }

      @Test
      void shouldNotBiteAgainUntilTheCooldownHasPassed() {
        Scene scene = cerberusWithTargetAt(IN_REACH);

        scene.run(WINDUP + SLACK);
        scene.run(COOLDOWN - 2 * SLACK);

        assertEquals(BITE_DAMAGE, scene.damageDealt(), "still one bite inside the cooldown");
      }

      @Test
      void shouldBiteASecondTimeAfterTheCooldownAndAnotherWindup() {
        Scene scene = cerberusWithTargetAt(IN_REACH);

        scene.run(WINDUP + SLACK);
        scene.run(COOLDOWN + WINDUP + SLACK);

        assertEquals(2 * BITE_DAMAGE, scene.damageDealt());
      }

      @Test
      void shouldKeepBitingAtTheSameRate() {
        Scene scene = cerberusWithTargetAt(IN_REACH);

        scene.run(LONG_ENOUGH_SECONDS);

        // One bite every cooldown plus windup (2 seconds): 4 in 8 seconds.
        assertEquals(4 * BITE_DAMAGE, scene.damageDealt());
      }

      @Test
      void shouldReachATargetJustInsideItsRange() {
        Scene scene = cerberusWithTargetAt(RANGE - 0.5f);

        scene.run(WINDUP + SLACK);

        assertEquals(BITE_DAMAGE, scene.damageDealt());
      }

      @Test
      void shouldNotReachATargetJustOutsideItsRange() {
        Scene scene = cerberusWithTargetAt(RANGE + 0.5f);

        scene.run(LONG_ENOUGH_SECONDS);

        assertEquals(0, scene.damageDealt());
      }

      @Test
      void shouldStartBitingWhenTheTargetWalksIntoRange() {
        Scene scene = cerberusWithTargetAt(RANGE + 2f);
        scene.run(1f);
        assertEquals(0, scene.damageDealt(), "nothing while the target is out of range");

        scene.placeTargetAt(IN_REACH);
        scene.run(WINDUP + SLACK);

        assertEquals(BITE_DAMAGE, scene.damageDealt());
      }

      @Test
      void shouldMissWhenTheTargetLeavesDuringTheWindup() {
        Scene scene = cerberusWithTargetAt(IN_REACH);

        scene.run(WINDUP / 2f);
        scene.placeTargetAt(OUT_OF_REACH);
        scene.run(WINDUP);

        assertEquals(0, scene.damageDealt());
      }

      @Test
      void shouldKnockTheTargetAwayFromItself() {
        Scene scene = cerberusWithTargetAt(IN_REACH);

        scene.run(WINDUP + 0.04f);

        assertTrue(
            AttackTestWorld.velocityOf(scene.target).x > 0f, "the target is pushed to the right");
      }

      @Test
      void shouldStayWhereItIsBecauseItGuardsItsRoom() {
        Scene scene = cerberusWithTargetAt(IN_REACH);

        scene.run(LONG_ENOUGH_SECONDS);

        assertEquals(0f, scene.enemy.getPosition().x, 1e-3f);
        assertEquals(0f, scene.enemy.getPosition().y, 1e-3f);
      }

      @Test
      void shouldFireNoProjectilesBecauseItOnlyBites() {
        Scene scene = cerberusWithTargetAt(IN_REACH);
        int before = world.mark();

        scene.run(LONG_ENOUGH_SECONDS);

        assertTrue(world.registeredSince(before).isEmpty());
      }

      @Test
      void shouldStopBitingOnceTheTargetIsDead() {
        Scene scene = cerberusWithTargetAt(IN_REACH);
        scene.target.getComponent(CombatStatsComponent.class).setHealth(0);

        scene.run(LONG_ENOUGH_SECONDS);

        assertEquals(0, scene.targetHealth(), "a dead target is not bitten below zero");
      }
    }
  }
}
