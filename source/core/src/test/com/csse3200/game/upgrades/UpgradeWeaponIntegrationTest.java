package com.csse3200.game.upgrades;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.player.ConsumableUseComponent;
import com.csse3200.game.components.player.PlayerActions;
import com.csse3200.game.components.player.PlayerProjectileHitComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.physics.PhysicsService;
import com.csse3200.game.physics.components.HitboxComponent;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Proves the upgrades system reacts identically to a kill no matter which weapon lands it. Sword
 * kills go through CombatStatsComponent.hit() (melee's existing path); bow/dagger kills go through
 * a real PlayerProjectileHitComponent, driven through an actual collision - the exact code path
 * fixed in this change, not a direct call into UpgradeNode/CombatStatsComponent.
 *
 * <p>Both weapons deal exactly WEAPON_DAMAGE, so a target seeded with that much health dies in one
 * hit (newLethalTarget()) and a target seeded with far more survives it (newSurvivingTarget()).
 *
 * <p>Follows UpgradesDisplayTest's setup style (newPlayerEntity(), reflection helpers for
 * getActionUpgrades()/getDefenceUpgrades()).
 */
@ExtendWith(GameExtension.class)
class UpgradeWeaponIntegrationTest {
  private static final int WEAPON_DAMAGE = 10;

  private UpgradesDisplay display;

  @BeforeEach
  void beforeEach() {
    RenderService renderService = new RenderService();
    renderService.setStage(mock(Stage.class));
    ServiceLocator.registerRenderService(renderService);
    ServiceLocator.registerResourceService(mock(ResourceService.class));
    ServiceLocator.registerPhysicsService(new PhysicsService());

    display = new UpgradesDisplay();
    new Entity().addComponent(new UpgradesMenuComponent()).addComponent(display).create();
  }

  @SuppressWarnings("unchecked")
  private List<UpgradeNode> getActionUpgrades() throws Exception {
    Field field = UpgradesDisplay.class.getDeclaredField("actionUpgrades");
    field.setAccessible(true);
    return (List<UpgradeNode>) field.get(display);
  }

  @SuppressWarnings("unchecked")
  private List<UpgradeNode> getDefenceUpgrades() throws Exception {
    Field field = UpgradesDisplay.class.getDeclaredField("defenceUpgrades");
    field.setAccessible(true);
    return (List<UpgradeNode>) field.get(display);
  }

  private Entity newPlayerEntity() {
    return new Entity()
        .addComponent(new CombatStatsComponent(100, WEAPON_DAMAGE))
        .addComponent(new PlayerActions());
  }

  /** For Regen tests - adds ConsumableUseComponent so the heal has a real cap to check against. */
  private Entity newPlayerEntityWithMaxHealth(int currentHealth, int maxHealth) {
    return new Entity()
        .addComponent(new CombatStatsComponent(currentHealth, WEAPON_DAMAGE))
        .addComponent(new PlayerActions())
        .addComponent(new ConsumableUseComponent(maxHealth));
  }

  /** Exactly WEAPON_DAMAGE health - either weapon kills it in one hit. */
  private Entity newLethalTarget() {
    return newTarget(WEAPON_DAMAGE);
  }

  /** Far more health than WEAPON_DAMAGE - neither weapon kills it in one hit. */
  private Entity newSurvivingTarget() {
    return newTarget(999);
  }

  private Entity newTarget(int health) {
    Entity target =
        new Entity()
            .addComponent(new CombatStatsComponent(health, 0))
            .addComponent(new PhysicsComponent())
            .addComponent(new HitboxComponent());
    target.create();
    return target;
  }

  /** Sword: CombatStatsComponent.hit(), the same path melee combat already uses. */
  private static void swordHit(Entity player, Entity target) {
    target
        .getComponent(CombatStatsComponent.class)
        .hit(player.getComponent(CombatStatsComponent.class));
  }

  /** Bow/dagger: a real PlayerProjectileHitComponent, driven through an actual collision. */
  private static void projectileHit(Entity player, Entity target) {
    Entity projectile =
        new Entity()
            .addComponent(new PlayerProjectileHitComponent(WEAPON_DAMAGE, player))
            .addComponent(new PhysicsComponent())
            .addComponent(new HitboxComponent());
    projectile.create();

    Fixture projectileFixture = projectile.getComponent(HitboxComponent.class).getFixture();
    Fixture targetFixture = target.getComponent(HitboxComponent.class).getFixture();
    projectile.getEvents().trigger("collisionStart", projectileFixture, targetFixture);
  }

  private static Stream<Arguments> weapons() {
    return Stream.of(
        Arguments.of("sword", (BiConsumer<Entity, Entity>) UpgradeWeaponIntegrationTest::swordHit),
        Arguments.of(
            "bow/dagger",
            (BiConsumer<Entity, Entity>) UpgradeWeaponIntegrationTest::projectileHit));
  }

  @ParameterizedTest(name = "{0} kill decrements Sword Damage''s remaining kill count")
  @MethodSource("weapons")
  void killDecrementsSwordDamagesRemainingKillCount(
      String weaponName, BiConsumer<Entity, Entity> weaponHit) throws Exception {
    Entity player = newPlayerEntity();
    display.setPlayer(player);
    Entity target = newLethalTarget();

    UpgradeNode swordDamage = getActionUpgrades().get(0); // "sword_damage"
    swordDamage.purchaseNextTier(); // Tier 1: 2 kills remaining

    weaponHit.accept(player, target);

    assertEquals(1, swordDamage.getCurrentTier()); // still active
    assertEquals("1 kills left", swordDamage.getRemainingText());
  }

  @ParameterizedTest(name = "{0} kill decrements Attack Speed''s remaining kill count")
  @MethodSource("weapons")
  void killDecrementsAttackSpeedsRemainingKillCount(
      String weaponName, BiConsumer<Entity, Entity> weaponHit) throws Exception {
    Entity player = newPlayerEntity();
    display.setPlayer(player);
    Entity target = newLethalTarget();

    UpgradeNode attackSpeed = getActionUpgrades().get(1); // "attack_speed"
    attackSpeed.purchaseNextTier(); // Tier 1: 5 kills remaining

    weaponHit.accept(player, target);

    assertEquals(1, attackSpeed.getCurrentTier()); // still active
    assertEquals("4 kills left", attackSpeed.getRemainingText());
  }

  @ParameterizedTest(name = "{0} kill triggers Regen on Kill''s heal")
  @MethodSource("weapons")
  void killTriggersRegenOnKillsHeal(String weaponName, BiConsumer<Entity, Entity> weaponHit)
      throws Exception {
    Entity player = newPlayerEntityWithMaxHealth(50, 100); // damaged below max
    display.setPlayer(player);
    Entity target = newLethalTarget();

    getDefenceUpgrades().get(1).purchaseNextTier(); // "regen_on_kill" Tier 1: 5 HP/kill

    weaponHit.accept(player, target);

    assertEquals(55, player.getComponent(CombatStatsComponent.class).getHealth());
  }

  @ParameterizedTest(name = "a non-lethal {0} hit does not fire enemyKilled or affect any upgrade")
  @MethodSource("weapons")
  void nonLethalHitDoesNotFireEnemyKilledOrAffectAnyUpgrade(
      String weaponName, BiConsumer<Entity, Entity> weaponHit) throws Exception {
    Entity player = newPlayerEntityWithMaxHealth(50, 100);
    display.setPlayer(player);
    Entity target = newSurvivingTarget(); // far more health than WEAPON_DAMAGE - survives the hit

    UpgradeNode swordDamage = getActionUpgrades().get(0);
    UpgradeNode attackSpeed = getActionUpgrades().get(1);
    UpgradeNode regenOnKill = getDefenceUpgrades().get(1);
    swordDamage.purchaseNextTier(); // 2 kills remaining
    attackSpeed.purchaseNextTier(); // 5 kills remaining
    regenOnKill.purchaseNextTier(); // active, 5 HP/kill

    List<Object> kills = new ArrayList<>();
    player.getEvents().addListener("enemyKilled", () -> kills.add(new Object()));

    weaponHit.accept(player, target);

    assertTrue(kills.isEmpty(), "enemyKilled should not fire on a non-lethal hit");
    assertEquals("2 kills left", swordDamage.getRemainingText()); // untouched
    assertEquals("5 kills left", attackSpeed.getRemainingText()); // untouched
    assertEquals(50, player.getComponent(CombatStatsComponent.class).getHealth()); // no heal
  }
}
