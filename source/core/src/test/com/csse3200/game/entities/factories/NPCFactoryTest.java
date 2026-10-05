package com.csse3200.game.entities.factories;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import com.csse3200.game.ai.tasks.AITaskComponent;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.attacks.ChargeComponent;
import com.csse3200.game.components.attacks.MeleeAttackComponent;
import com.csse3200.game.components.attacks.RangedAttackComponent;
import com.csse3200.game.components.attacks.TouchAttackComponent;
import com.csse3200.game.components.loot.WeaponItem;
import com.csse3200.game.components.loot.WeaponType;
import com.csse3200.game.components.npc.CerberusAnimationController;
import com.csse3200.game.components.npc.EnemyDeathComponent;
import com.csse3200.game.components.npc.EnemyTypeComponent;
import com.csse3200.game.components.npc.FlightComponent;
import com.csse3200.game.components.npc.HarpyAnimationController;
import com.csse3200.game.components.player.InventoryComponent;
import com.csse3200.game.components.player.ItemDropComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.configs.BaseEntityConfig;
import com.csse3200.game.entities.configs.NPCConfigs;
import com.csse3200.game.entities.spawn.DefaultEntitySpawns;
import com.csse3200.game.entities.spawn.EntitySpawnRegistry;
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
import java.util.function.Function;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Tests that each NPC factory method wires the components, weapons and values its design needs.
 * "Wired" tests check what the built entity HAS; the entities are not created or run, so no physics
 * stepping happens here.
 *
 * <p>Every number is read from {@code configs/NPCs.json} through the config classes rather than
 * written out here, so changing a value in the JSON does not break these tests, and a value that
 * the factory forgets to read does.
 *
 * <p>The target is a stand-in for the player: it has combat stats and a hitbox on the player layer,
 * because the charging enemies' touch attack is built to hit the player layer.
 *
 * <p>Changes since the first version of this class:
 *
 * <ul>
 *   <li>The five new enemies (Medusa, Cerberus, Zeus, Harpy, Ranged Harpy) are in every
 *       parameterized test.
 *   <li>The Minotaur and Centaur now carry a {@link TouchAttackComponent} (the charge deals its
 *       damage through it), so the old "no enemy has touch damage" test is split: only the
 *       non-charging enemies are checked for none, and the charging ones are checked for one.
 *       {@code shouldGiveTheChargingEnemiesATouchAttack} fails until that wiring is in the factory.
 *   <li>Both Harpies carry a {@link FlightComponent} with the configured damping.
 *   <li>The Cyclops, Zeus and Medusa tests only check invariants (config values, both attacks
 *       present, melee shorter than ranged, windup below cooldown), because those enemies' attack
 *       logic is being reworked.
 * </ul>
 */
@ExtendWith(GameExtension.class)
class NPCFactoryTest {
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

  // ---------- every enemy ----------

  @ParameterizedTest(name = "{0} builds")
  @MethodSource("everyEnemy")
  void shouldBuildEveryEnemyWithoutThrowing(
      String name, Function<Entity, Entity> factory, String label) {
    Entity enemy = factory.apply(player);

    assertNotNull(enemy, name + " must be built");
  }

  @ParameterizedTest(name = "{0} has the components every enemy needs")
  @MethodSource("everyEnemy")
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
  @MethodSource("everyEnemy")
  void shouldLabelEveryEnemyWithItsType(
      String name, Function<Entity, Entity> factory, String label) {
    Entity enemy = factory.apply(player);

    assertEquals(label, enemy.getComponent(EnemyTypeComponent.class).getEnemyLabel());
  }

  @ParameterizedTest(name = "{0} takes its health and attack from the config")
  @MethodSource("everyEnemy")
  void shouldTakeHealthAndAttackFromTheConfig(
      String name, Function<Entity, Entity> factory, String label) {
    Entity enemy = factory.apply(player);
    BaseEntityConfig config = configFor(name);

    CombatStatsComponent stats = enemy.getComponent(CombatStatsComponent.class);
    assertEquals(config.health, stats.getHealth(), name + " health");
    assertEquals(config.baseAttack, stats.getBaseAttack(), name + " base attack");
  }

  @ParameterizedTest(name = "{0} has a positive size")
  @MethodSource("everyEnemy")
  void shouldGiveEveryEnemyAPositiveScale(
      String name, Function<Entity, Entity> factory, String label) {
    Entity enemy = factory.apply(player);

    assertTrue(enemy.getScale().x > 0f, name + " width");
    assertTrue(enemy.getScale().y > 0f, name + " height");
  }

  @ParameterizedTest(name = "{0} can attack")
  @MethodSource("everyEnemy")
  void shouldGiveEveryEnemyAtLeastOneAttack(
      String name, Function<Entity, Entity> factory, String label) {
    Entity enemy = factory.apply(player);

    assertTrue(
        enemy.getComponent(MeleeAttackComponent.class) != null
            || enemy.getComponent(RangedAttackComponent.class) != null,
        name + " needs a melee or a ranged attack");
  }

  @ParameterizedTest(name = "{0} winds up for less time than its cooldown")
  @MethodSource("everyEnemy")
  void shouldKeepEveryWindupShorterThanItsCooldown(
      String name, Function<Entity, Entity> factory, String label) {
    Entity enemy = factory.apply(player);
    MeleeAttackComponent melee = enemy.getComponent(MeleeAttackComponent.class);
    RangedAttackComponent ranged = enemy.getComponent(RangedAttackComponent.class);

    if (melee != null) {
      assertTrue(melee.getWindupDuration() < melee.getCooldown(), name + " melee windup");
    }
    if (ranged != null) {
      assertTrue(ranged.getWindupDuration() < ranged.getCooldown(), name + " ranged windup");
    }
  }

  @ParameterizedTest(name = "{0}: two builds are independent entities")
  @MethodSource("everyEnemy")
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

  // ---------- touch damage: only the charging enemies ----------

  @ParameterizedTest(name = "{0} attacks through an attack component, not touch damage")
  @MethodSource("nonChargingEnemies")
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

  // ---------- null target ----------

  @ParameterizedTest(name = "{0} rejects a null target")
  @MethodSource("newEnemiesAndSkeleton")
  void shouldRejectANullTarget(String name, Function<Entity, Entity> factory, String label) {
    assertThrows(IllegalArgumentException.class, () -> factory.apply(null), name);
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

  // ---------- skeleton ----------

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

  // ---------- ranged skeleton ----------

  @Test
  void shouldArmTheRangedSkeletonWithATierOneBowAndApplyTheProjectileSpeed() {
    Entity archer = NPCFactory.createRangedSkeleton(player);

    WeaponItem bow = (WeaponItem) archer.getComponent(InventoryComponent.class).getItem(1);
    assertEquals(WeaponType.BOW, bow.getWeaponType());
    RangedAttackComponent ranged = archer.getComponent(RangedAttackComponent.class);
    assertEquals(configs.rangedSkeleton.ranged.range, ranged.getRange(), 1e-6f);
    assertEquals(configs.rangedSkeleton.ranged.cooldown, ranged.getCooldown(), 1e-6f);
    assertEquals(configs.rangedSkeleton.ranged.projectileSpeed, ranged.getProjectileSpeed(), 1e-6f);
    assertEquals(bow.getDamage(), ranged.getDamage());
    assertEquals(bow.getWindupDuration(), ranged.getWindupDuration(), 1e-6f);
    assertNull(archer.getComponent(MeleeAttackComponent.class));
  }

  // ---------- minotaur ----------

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

  // ---------- centaur ----------

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

  // ---------- cyclops (invariants only: the attack set is being reworked) ----------

  @Test
  void shouldGiveTheCyclopsANaturalMeleeAndANaturalRangedAttackFromItsConfig() {
    Entity cyclops = NPCFactory.createCyclops(player);

    MeleeAttackComponent melee = cyclops.getComponent(MeleeAttackComponent.class);
    assertEquals(configs.cyclops.melee.range, melee.getRange(), 1e-6f);
    assertEquals(configs.cyclops.melee.cooldown, melee.getCooldown(), 1e-6f);
    assertEquals(configs.cyclops.melee.knockback, melee.getKnockback(), 1e-6f);

    RangedAttackComponent ranged = cyclops.getComponent(RangedAttackComponent.class);
    assertEquals(configs.cyclops.ranged.range, ranged.getRange(), 1e-6f);
    assertEquals(configs.cyclops.ranged.cooldown, ranged.getCooldown(), 1e-6f);
    assertEquals(configs.cyclops.ranged.projectileSpeed, ranged.getProjectileSpeed(), 1e-6f);
  }

  @Test
  void shouldGiveTheCyclopsGoldOnlyBecauseNaturalWeaponsMustNeverDrop() {
    Entity cyclops = NPCFactory.createCyclops(player);

    InventoryComponent inventory = cyclops.getComponent(InventoryComponent.class);
    assertTrue(inventory.getGold() > 0, "the Cyclops pays out in gold");
    assertNull(inventory.getItem(1), "natural weapons live in the attack components");
  }

  // ---------- medusa ----------

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

  // ---------- cerberus ----------

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

  // ---------- zeus (invariants only: his logic is being written separately) ----------

  @Test
  void shouldGiveZeusBothAttacksWithMeleeShorterThanRanged() {
    Entity zeus = NPCFactory.createZeus(player);

    MeleeAttackComponent melee = zeus.getComponent(MeleeAttackComponent.class);
    RangedAttackComponent ranged = zeus.getComponent(RangedAttackComponent.class);
    assertNotNull(melee);
    assertNotNull(ranged);
    assertTrue(melee.getRange() < ranged.getRange());
  }

  // ---------- harpies ----------

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

  // ---------- robustness: a target without a hitbox ----------

  @ParameterizedTest(name = "{0} builds even when its target has no hitbox")
  @MethodSource("everyEnemy")
  void shouldBuildEveryEnemyForATargetWithNoHitbox(
      String name, Function<Entity, Entity> factory, String label) {
    // A bare target (stats only) must not crash a factory: the layer the touch attack hits is the
    // player layer constant, not something read from the target.
    Entity bareTarget = new Entity().addComponent(new CombatStatsComponent(100, 0));

    assertNotNull(factory.apply(bareTarget), name);
  }

  // ---------- current art and animation wiring (owned by the sprite author) ----------

  @Test
  void shouldGiveCerberusItsOwnAnimationControllerAndBothFacings() {
    Entity cerberus = NPCFactory.createCerberus(player);

    assertNotNull(cerberus.getComponent(CerberusAnimationController.class));
    AnimationRenderComponent animator = cerberus.getComponent(AnimationRenderComponent.class);
    assertTrue(animator.hasAnimation("cerberus_l"), "left-facing animation");
    assertTrue(animator.hasAnimation("cerberus_r"), "right-facing animation");
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
  
  @Test
  void shouldRegisterAllNpcSpawns() {
    NPCFactory.registerNpcSpawns();
    
    assertTrue(EntitySpawnRegistry.isRegistered("npc:shop"));
    assertTrue(EntitySpawnRegistry.isRegistered("npc:wizard"));
    assertTrue(EntitySpawnRegistry.isRegistered("npc:philosopher"));
    assertTrue(EntitySpawnRegistry.isRegistered("npc:satyr"));
  }
}
