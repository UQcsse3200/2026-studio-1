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
import com.csse3200.game.components.npc.EnemyDeathComponent;
import com.csse3200.game.components.npc.EnemyTypeComponent;
import com.csse3200.game.components.player.InventoryComponent;
import com.csse3200.game.components.player.ItemDropComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.physics.PhysicsService;
import com.csse3200.game.rendering.AnimationRenderComponent;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import java.util.function.Function;
import java.util.stream.Stream;
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
 * <p>Fixture note: {@code NPCFactory} loads each enemy's texture atlas straight from the asset
 * folder (the tests run with the working directory set to {@code core/assets}), and the {@link
 * GameExtension} supplies headless libGDX with a mocked GL, so a real atlas loads without a window.
 * The numbers below come from {@code configs/NPCs.json} and the weapon generator. If you change a
 * number there, update it here.
 *
 * <p>The Cyclops tests describe today's Cyclops (stomp plus one thrown rock, both natural weapons).
 * When the Cyclops is redone with a laser, update that group.
 */
@ExtendWith(GameExtension.class)
class NPCFactoryTest {
  private Entity player;

  @BeforeEach
  void setUp() {
    ServiceLocator.registerPhysicsService(new PhysicsService());
    ServiceLocator.registerRenderService(mock(RenderService.class));
    ServiceLocator.registerResourceService(mock(ResourceService.class));
    ServiceLocator.registerTimeSource(mock(GameTime.class));
    player = new Entity().addComponent(new CombatStatsComponent(100, 0));
  }

  static Stream<Arguments> everyEnemy() {
    return Stream.of(
        Arguments.of("skeleton", (Function<Entity, Entity>) NPCFactory::createSkeleton),
        Arguments.of(
            "ranged skeleton", (Function<Entity, Entity>) NPCFactory::createRangedSkeleton),
        Arguments.of("minotaur", (Function<Entity, Entity>) NPCFactory::createMinotaur),
        Arguments.of("centaur", (Function<Entity, Entity>) NPCFactory::createCentaur),
        Arguments.of("cyclops", (Function<Entity, Entity>) NPCFactory::createCyclops));
  }

  // ---------- every enemy ----------

  @ParameterizedTest(name = "{0} builds")
  @MethodSource("everyEnemy")
  void shouldBuildEveryEnemyWithoutThrowing(String name, Function<Entity, Entity> factory) {
    Entity enemy = factory.apply(player);

    assertNotNull(enemy, name + " must be built");
  }

  @ParameterizedTest(name = "{0} has the components every enemy needs")
  @MethodSource("everyEnemy")
  void shouldWireStatsTypeLootDeathAnimationAndAiOnEveryEnemy(
      String name, Function<Entity, Entity> factory) {
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

  @ParameterizedTest(name = "{0} has a positive size")
  @MethodSource("everyEnemy")
  void shouldGiveEveryEnemyAPositiveScale(String name, Function<Entity, Entity> factory) {
    Entity enemy = factory.apply(player);

    assertTrue(enemy.getScale().x > 0f, name + " width");
    assertTrue(enemy.getScale().y > 0f, name + " height");
  }

  @ParameterizedTest(name = "{0} attacks through an attack component, not touch damage")
  @MethodSource("everyEnemy")
  void shouldNotGiveAnyPlatformerEnemyTouchDamage(String name, Function<Entity, Entity> factory) {
    Entity enemy = factory.apply(player);

    assertNull(enemy.getComponent(TouchAttackComponent.class), name);
  }

  @ParameterizedTest(name = "{0}: two builds are independent entities")
  @MethodSource("everyEnemy")
  void shouldBuildAFreshEntityEveryTime(String name, Function<Entity, Entity> factory) {
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

  // ---------- skeleton ----------

  @Test
  void shouldGiveTheSkeletonItsConfiguredStats() {
    Entity skeleton = NPCFactory.createSkeleton(player);

    CombatStatsComponent stats = skeleton.getComponent(CombatStatsComponent.class);
    assertEquals(30, stats.getHealth());
    assertEquals(5, stats.getBaseAttack());
    assertEquals("Skeleton", skeleton.getComponent(EnemyTypeComponent.class).getEnemyLabel());
  }

  @Test
  void shouldArmTheSkeletonWithATierOneSwordItCanDrop() {
    Entity skeleton = NPCFactory.createSkeleton(player);

    InventoryComponent inventory = skeleton.getComponent(InventoryComponent.class);
    assertEquals(3, inventory.getGold());
    WeaponItem sword = (WeaponItem) inventory.getItem(1);
    assertEquals(WeaponType.SWORD, sword.getWeaponType());
    assertEquals(1, sword.getTier());
    MeleeAttackComponent melee = skeleton.getComponent(MeleeAttackComponent.class);
    assertEquals(2f, melee.getRange(), 1e-6f);
    assertEquals(5f, melee.getCooldown(), 1e-6f);
    assertEquals(sword.getDamage(), melee.getDamage());
    assertNull(skeleton.getComponent(RangedAttackComponent.class));
  }

  // ---------- ranged skeleton ----------

  @Test
  void shouldGiveTheRangedSkeletonItsConfiguredStatsAndLabel() {
    Entity archer = NPCFactory.createRangedSkeleton(player);

    CombatStatsComponent stats = archer.getComponent(CombatStatsComponent.class);
    assertEquals(20, stats.getHealth());
    assertEquals(5, stats.getBaseAttack());
    assertEquals("Ranged Skeleton", archer.getComponent(EnemyTypeComponent.class).getEnemyLabel());
  }

  @Test
  void shouldArmTheRangedSkeletonWithATierOneBowAndApplyTheProjectileSpeed() {
    Entity archer = NPCFactory.createRangedSkeleton(player);

    WeaponItem bow = (WeaponItem) archer.getComponent(InventoryComponent.class).getItem(1);
    assertEquals(WeaponType.BOW, bow.getWeaponType());
    RangedAttackComponent ranged = archer.getComponent(RangedAttackComponent.class);
    assertEquals(8f, ranged.getRange(), 1e-6f);
    assertEquals(5f, ranged.getCooldown(), 1e-6f);
    assertEquals(8f, ranged.getProjectileSpeed(), 1e-6f);
    assertEquals(bow.getDamage(), ranged.getDamage());
    assertEquals(bow.getWindupDuration(), ranged.getWindupDuration(), 1e-6f);
    assertNull(archer.getComponent(MeleeAttackComponent.class));
  }

  // ---------- minotaur ----------

  @Test
  void shouldGiveTheMinotaurItsConfiguredStatsAndLabel() {
    Entity minotaur = NPCFactory.createMinotaur(player);

    CombatStatsComponent stats = minotaur.getComponent(CombatStatsComponent.class);
    assertEquals(40, stats.getHealth());
    assertEquals(5, stats.getBaseAttack());
    assertEquals("Minotaur", minotaur.getComponent(EnemyTypeComponent.class).getEnemyLabel());
  }

  @Test
  void shouldArmTheMinotaurWithATierTwoAxeWithZeroWindupAndDropIt() {
    Entity minotaur = NPCFactory.createMinotaur(player);

    WeaponItem axe = (WeaponItem) minotaur.getComponent(InventoryComponent.class).getItem(1);
    assertEquals(WeaponType.AXE, axe.getWeaponType());
    assertEquals(2, axe.getTier());
    assertEquals(0f, axe.getWindupDuration(), 1e-6f, "the factory removes the axe's windup");
    MeleeAttackComponent melee = minotaur.getComponent(MeleeAttackComponent.class);
    assertEquals(2.5f, melee.getRange(), 1e-6f);
    assertEquals(5f, melee.getCooldown(), 1e-6f);
    assertEquals(2f, melee.getKnockback(), 1e-6f);
    assertEquals(23, melee.getDamage(), "tier-2 axe damage");
    assertEquals(0f, melee.getWindupDuration(), 1e-6f);
  }

  @Test
  void shouldGiveTheMinotaurAChargeFromItsConfig() {
    Entity minotaur = NPCFactory.createMinotaur(player);

    ChargeComponent charge = minotaur.getComponent(ChargeComponent.class);
    assertNotNull(charge);
    assertEquals(4f, charge.getChargeDuration(), 1e-6f);
    assertEquals(2.5f, charge.getWindupDuration(), 1e-6f);
    assertEquals(
        1.0f,
        charge.getDamageMultiplier(),
        1e-6f,
        "the multiplier only applies while charging, so it reads 1 at rest");
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
  void shouldGiveTheCentaurItsConfiguredStatsAndLabel() {
    Entity centaur = NPCFactory.createCentaur(player);

    CombatStatsComponent stats = centaur.getComponent(CombatStatsComponent.class);
    assertEquals(40, stats.getHealth());
    assertEquals(7, stats.getBaseAttack());
    assertEquals("Centaur", centaur.getComponent(EnemyTypeComponent.class).getEnemyLabel());
  }

  @Test
  void shouldArmTheCentaurWithABowAndApplyItsRangedConfig() {
    Entity centaur = NPCFactory.createCentaur(player);

    WeaponItem bow = (WeaponItem) centaur.getComponent(InventoryComponent.class).getItem(1);
    assertEquals(WeaponType.BOW, bow.getWeaponType(), "a sword here would be rejected");
    RangedAttackComponent ranged = centaur.getComponent(RangedAttackComponent.class);
    assertEquals(8f, ranged.getRange(), 1e-6f);
    assertEquals(5f, ranged.getCooldown(), 1e-6f);
    assertEquals(2f, ranged.getKnockback(), 1e-6f);
    assertEquals(8f, ranged.getProjectileSpeed(), 1e-6f);
    assertEquals(bow.getDamage(), ranged.getDamage());
  }

  @Test
  void shouldGiveTheCentaurAChargeFromItsConfig() {
    Entity centaur = NPCFactory.createCentaur(player);

    ChargeComponent charge = centaur.getComponent(ChargeComponent.class);
    assertNotNull(charge);
    assertEquals(4f, charge.getChargeDuration(), 1e-6f);
    assertEquals(0f, charge.getWindupDuration(), 1e-6f, "the Centaur config has no windup");
    assertNull(centaur.getComponent(MeleeAttackComponent.class));
  }

  // ---------- cyclops (today's version) ----------

  @Test
  void shouldGiveTheCyclopsItsConfiguredStatsAndLabel() {
    Entity cyclops = NPCFactory.createCyclops(player);

    CombatStatsComponent stats = cyclops.getComponent(CombatStatsComponent.class);
    assertEquals(50, stats.getHealth());
    assertEquals(10, stats.getBaseAttack());
    assertEquals("Cyclops", cyclops.getComponent(EnemyTypeComponent.class).getEnemyLabel());
  }

  @Test
  void shouldGiveTheCyclopsANaturalMeleeAndANaturalRangedAttack() {
    Entity cyclops = NPCFactory.createCyclops(player);

    MeleeAttackComponent melee = cyclops.getComponent(MeleeAttackComponent.class);
    assertEquals(3f, melee.getRange(), 1e-6f);
    assertEquals(4f, melee.getCooldown(), 1e-6f);
    assertEquals(2.5f, melee.getKnockback(), 1e-6f);
    assertEquals(10, melee.getDamage(), "natural fists use the Cyclops's base attack");
    assertEquals(3f, melee.getWindupDuration(), 1e-6f, "melee cooldown 4 minus 1");

    RangedAttackComponent ranged = cyclops.getComponent(RangedAttackComponent.class);
    assertEquals(7f, ranged.getRange(), 1e-6f);
    assertEquals(3f, ranged.getCooldown(), 1e-6f);
    assertEquals(8f, ranged.getProjectileSpeed(), 1e-6f);
    assertEquals(10, ranged.getDamage());
    assertEquals(
        2f, ranged.getWindupDuration(), 1e-6f, "the rock's windup (cooldown 3 minus 1) is stored");
  }

  @Test
  void shouldGiveTheCyclopsGoldOnlyBecauseNaturalWeaponsMustNeverDrop() {
    Entity cyclops = NPCFactory.createCyclops(player);

    InventoryComponent inventory = cyclops.getComponent(InventoryComponent.class);
    assertEquals(12, inventory.getGold());
    assertNull(inventory.getItem(1), "natural weapons live in the attack components");
  }

  // ---------- targets ----------

  @Test
  void shouldRejectANullTargetBecauseTheMeleeTaskDoes() {
    assertThrows(IllegalArgumentException.class, () -> NPCFactory.createSkeleton(null));
  }
}
