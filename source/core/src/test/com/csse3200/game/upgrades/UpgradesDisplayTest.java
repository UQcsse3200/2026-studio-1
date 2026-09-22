package com.csse3200.game.upgrades;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.player.ConsumableUseComponent;
import com.csse3200.game.components.player.PlayerActions;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * This screen is view-only - there is no Buy button/attemptPurchase() anymore, so these tests cover
 * two things instead: (1) showDetail()'s displayed text (name/tier, description, status/remaining)
 * is correct for both an inactive and an active upgrade, and (2) the gameplay-effect wiring
 * (applySwordDamageEffect(), applyPlayerSpeedEffect(), etc., registered via
 * UpgradeNode.setOnTierChanged()/setOnExpired()) still fires correctly when something else (in
 * reality, ShopDisplay's real buy flow) calls UpgradeNode.purchaseNextTier() directly - simulated
 * here the same way, since that effect wiring doesn't care who triggered the purchase.
 *
 * <p>showDetail()/selectedNode are private, so reading/driving them still needs reflection, the
 * same way PauseMenuDisplayTest drives PauseMenuDisplay's package-private navigation methods.
 */
@ExtendWith(GameExtension.class)
class UpgradesDisplayTest {
  private UpgradesDisplay display;

  @BeforeEach
  void beforeEach() {
    RenderService renderService = new RenderService();
    renderService.setStage(mock(Stage.class));
    ServiceLocator.registerRenderService(renderService);
    ServiceLocator.registerResourceService(mock(ResourceService.class));

    display = new UpgradesDisplay();
    Entity entity = new Entity().addComponent(new UpgradesMenuComponent()).addComponent(display);
    entity.create();
  }

  @SuppressWarnings("unchecked")
  private List<UpgradeNode> getActionUpgrades() throws Exception {
    Field field = UpgradesDisplay.class.getDeclaredField("actionUpgrades");
    field.setAccessible(true);
    return (List<UpgradeNode>) field.get(display);
  }

  @SuppressWarnings("unchecked")
  private List<UpgradeNode> getMovementUpgrades() throws Exception {
    Field field = UpgradesDisplay.class.getDeclaredField("movementUpgrades");
    field.setAccessible(true);
    return (List<UpgradeNode>) field.get(display);
  }

  @SuppressWarnings("unchecked")
  private List<UpgradeNode> getDefenceUpgrades() throws Exception {
    Field field = UpgradesDisplay.class.getDeclaredField("defenceUpgrades");
    field.setAccessible(true);
    return (List<UpgradeNode>) field.get(display);
  }

  private void showDetail(UpgradeNode node) throws Exception {
    Method method = UpgradesDisplay.class.getDeclaredMethod("showDetail", UpgradeNode.class);
    method.setAccessible(true);
    method.invoke(display, node);
  }

  private String labelText(String fieldName) throws Exception {
    Field field = UpgradesDisplay.class.getDeclaredField(fieldName);
    field.setAccessible(true);
    return ((Label) field.get(display)).getText().toString();
  }

  private Entity newPlayerEntity() {
    return new Entity()
        .addComponent(new CombatStatsComponent(100, 10))
        .addComponent(new PlayerActions());
  }

  /** Only for Regen on Kill tests below - adds ConsumableUseComponent, unlike newPlayerEntity(). */
  private Entity newPlayerEntityWithMaxHealth(int currentHealth, int maxHealth) {
    return new Entity()
        .addComponent(new CombatStatsComponent(currentHealth, 10))
        .addComponent(new PlayerActions())
        .addComponent(new ConsumableUseComponent(maxHealth));
  }

  @Test
  void showDetailDisplaysNameDescriptionAndNotActiveStatusForAnUnpurchasedUpgrade()
      throws Exception {
    UpgradeNode swordDamage = getActionUpgrades().get(0);

    showDetail(swordDamage);

    assertEquals("Sword Damage", labelText("detailNameLabel")); // no tier suffix - not active yet
    assertEquals(swordDamage.getDescription(), labelText("detailDescriptionLabel"));
    assertEquals("Not active", labelText("detailStatusLabel"));
  }

  @Test
  void showDetailDisplaysTierAndRemainingCountdownForAnActiveUpgrade() throws Exception {
    UpgradeNode swordDamage = getActionUpgrades().get(0);
    swordDamage.purchaseNextTier(); // Tier 1, 2 kills remaining (tierKillCounts = {2, 5, 8})

    showDetail(swordDamage);

    assertEquals("Sword Damage (Tier 1/3)", labelText("detailNameLabel"));
    assertEquals("Active - " + swordDamage.getRemainingText(), labelText("detailStatusLabel"));
  }

  @Test
  void swordDamagePurchaseShouldAddTierBonusOnTopOfBaseAttack() throws Exception {
    Entity player = newPlayerEntity();
    display.setPlayer(player);

    UpgradeNode swordDamage = getActionUpgrades().get(0);
    swordDamage.purchaseNextTier(); // Tier 1: base 10 + 5

    assertEquals(15, player.getComponent(CombatStatsComponent.class).getBaseAttack());
  }

  @Test
  void swordDamageSecondTierShouldReplaceNotStackTheBonus() throws Exception {
    Entity player = newPlayerEntity();
    display.setPlayer(player);

    UpgradeNode swordDamage = getActionUpgrades().get(0);
    swordDamage.purchaseNextTier(); // Tier 1: 10 + 5 = 15
    swordDamage.purchaseNextTier(); // Tier 2: 10 + 10 = 20, not 15 + 10 = 25

    assertEquals(20, player.getComponent(CombatStatsComponent.class).getBaseAttack());
  }

  @Test
  void swordDamageExpiryShouldRestoreTheOriginalBaseAttack() throws Exception {
    Entity player = newPlayerEntity();
    display.setPlayer(player);

    UpgradeNode swordDamage = getActionUpgrades().get(0);
    swordDamage.purchaseNextTier(); // Tier 1: 10 + 5 = 15, 2 kills remaining

    for (int i = 0; i < 5; i++) {
      swordDamage.onEnemyKilled(); // deliberately overshoots the 2-kill threshold - onEnemyKilled()
      // no-ops once inactive, so this still ends up fully expired
    }

    assertEquals(0, swordDamage.getCurrentTier());
    assertEquals(10, player.getComponent(CombatStatsComponent.class).getBaseAttack());
  }

  @Test
  void playerSpeedPurchaseShouldAddSpeedModifierAndExpiryShouldRemoveIt() throws Exception {
    Entity player = newPlayerEntity();
    display.setPlayer(player);
    PlayerActions playerActions = player.getComponent(PlayerActions.class);

    UpgradeNode playerSpeed = getMovementUpgrades().get(0); // "player_speed"
    playerSpeed.purchaseNextTier(); // Tier 1: 1.15x, 0 + 10s increment = 10s remaining

    assertEquals(1.15f, playerActions.getEffectiveSpeedMultiplier(), 0.0001f);

    playerSpeed.tickTime(21f); // more than enough to fully expire

    assertEquals(1f, playerActions.getEffectiveSpeedMultiplier(), 0.0001f);
  }

  // --- Kill-count countdown, driven by the player's real "enemyKilled" event ---
  //
  // Regression tests for a bug where kill-count upgrades (Sword Damage, Attack Speed) never
  // counted down: CombatStatsComponent.hit() fires "enemyKilled" on the attacker, but
  // UpgradesDisplay's listener only handled Regen on Kill and never called
  // UpgradeNode.onEnemyKilled(), so remainingKills never dropped and the upgrade never expired.
  // These fire the real event on the player's EventHandler instead of calling
  // node.onEnemyKilled() directly, so they exercise the listener wiring itself.

  private void killEnemies(Entity player, int times) {
    for (int i = 0; i < times; i++) {
      player.getEvents().trigger("enemyKilled");
    }
  }

  @Test
  void killsCountDownASwordDamageUpgradeAndItExpiresAtZero() throws Exception {
    Entity player = newPlayerEntity();
    display.setPlayer(player);
    CombatStatsComponent stats = player.getComponent(CombatStatsComponent.class);

    UpgradeNode swordDamage = getActionUpgrades().get(0);
    swordDamage.purchaseNextTier(); // Tier 1: 2 kills, base 10 + 5
    swordDamage.purchaseNextTier(); // Tier 2: 5 kills (overwrites, not adds), base 10 + 10
    assertEquals("5 kills left", swordDamage.getRemainingText());

    killEnemies(player, 3); // N = 3

    assertEquals("2 kills left", swordDamage.getRemainingText()); // dropped by exactly N
    assertEquals(2, swordDamage.getCurrentTier()); // still active - tier untouched mid-countdown
    assertEquals(20, stats.getBaseAttack()); // effect still applied

    killEnemies(player, 2); // reaches 0

    assertEquals(0, swordDamage.getCurrentTier()); // expired
    assertEquals(10, stats.getBaseAttack()); // effect removed, original attack restored
  }

  @Test
  void killsCountDownAnAttackSpeedUpgradeAndItExpiresAtZero() throws Exception {
    Entity player = newPlayerEntity();
    display.setPlayer(player);

    UpgradeNode attackSpeed = getActionUpgrades().get(1); // "attack_speed"
    attackSpeed.purchaseNextTier(); // Tier 1: 5 kills (tierKillCounts = {5, 8, 12})
    assertEquals("5 kills left", attackSpeed.getRemainingText());

    killEnemies(player, 4);

    assertEquals("1 kills left", attackSpeed.getRemainingText());
    assertEquals(1, attackSpeed.getCurrentTier());

    killEnemies(player, 1);

    assertEquals(0, attackSpeed.getCurrentTier());
  }

  @Test
  void killsBeforeAKillCountUpgradeIsBoughtDoNotUseUpItsKills() throws Exception {
    Entity player = newPlayerEntity();
    display.setPlayer(player);
    UpgradeNode swordDamage = getActionUpgrades().get(0);

    // Nothing active, and Regen on Kill also inactive - this is the path through the handler's
    // early return, so it also guards that the countdown runs BEFORE that return.
    killEnemies(player, 3);
    swordDamage.purchaseNextTier();

    assertEquals("2 kills left", swordDamage.getRemainingText()); // full Tier 1 allowance intact
  }

  @Test
  void killsDoNotAffectATimeBasedUpgradesTimer() throws Exception {
    Entity player = newPlayerEntity();
    display.setPlayer(player);
    PlayerActions playerActions = player.getComponent(PlayerActions.class);

    UpgradeNode playerSpeed = getMovementUpgrades().get(0); // "player_speed" - TIME based
    playerSpeed.purchaseNextTier(); // Tier 1: 10s remaining
    assertEquals("10s left", playerSpeed.getRemainingText());

    killEnemies(player, 5);

    assertEquals("10s left", playerSpeed.getRemainingText()); // timer untouched by kills
    assertEquals(1, playerSpeed.getCurrentTier());
    assertEquals(1.15f, playerActions.getEffectiveSpeedMultiplier(), 0.0001f); // still active
  }

  // --- Shield Durability (defenceUpgrades.get(0), "shield_durability") ---
  //
  // TIME-based, so it expires via tickTime() like Player Speed+, but its effect is a shield-hit
  // counter on CombatStatsComponent rather than a continuous stat modifier.

  @Test
  void shieldDurabilityEachTierSetsTheCorrespondingShieldHitCount() throws Exception {
    Entity player = newPlayerEntity();
    display.setPlayer(player);
    CombatStatsComponent stats = player.getComponent(CombatStatsComponent.class);

    UpgradeNode shield = getDefenceUpgrades().get(0); // "shield_durability"

    shield.purchaseNextTier(); // Tier 1
    assertEquals(3, stats.getShieldHits());

    shield.purchaseNextTier(); // Tier 2 - replaces, not adds on top of the 3 already there
    assertEquals(5, stats.getShieldHits());

    shield.purchaseNextTier(); // Tier 3
    assertEquals(8, stats.getShieldHits());
  }

  @Test
  void shieldAbsorbsHitsWithoutLosingHealthUntilItsHitsAreUsedUpThenDamageAppliesNormally()
      throws Exception {
    Entity player = newPlayerEntity();
    display.setPlayer(player);
    CombatStatsComponent stats = player.getComponent(CombatStatsComponent.class);
    CombatStatsComponent attacker = new CombatStatsComponent(1, 20); // deals 20 per hit

    UpgradeNode shield = getDefenceUpgrades().get(0);
    shield.purchaseNextTier(); // Tier 1: 3 shield hits

    for (int i = 0; i < 3; i++) {
      stats.hit(attacker);
      assertEquals(100, stats.getHealth()); // absorbed - health untouched
    }
    assertEquals(0, stats.getShieldHits()); // all 3 shield hits consumed

    stats.hit(attacker); // shield exhausted - this hit lands for real

    assertEquals(80, stats.getHealth()); // 100 - 20
  }

  @Test
  void shieldExpiryResetsShieldHitsToZeroEvenIfSomeWereUnused() throws Exception {
    Entity player = newPlayerEntity();
    display.setPlayer(player);
    CombatStatsComponent stats = player.getComponent(CombatStatsComponent.class);
    CombatStatsComponent attacker = new CombatStatsComponent(1, 20);

    UpgradeNode shield = getDefenceUpgrades().get(0);
    shield.purchaseNextTier(); // Tier 1: 3 shield hits, 20s duration

    stats.hit(attacker); // 1 hit used, 2 left
    assertEquals(2, stats.getShieldHits());

    shield.tickTime(21f); // more than enough to fully expire

    assertEquals(0, shield.getCurrentTier());
    assertEquals(0, stats.getShieldHits()); // reset to 0, not left at the 2 that were unused
  }

  // --- Regen on Kill (defenceUpgrades.get(1), "regen_on_kill") ---
  //
  // TIME-based like Shield Durability, but its effect fires from onEnemyKilled() rather than from
  // setOnTierChanged()/tickTime(). Uses newPlayerEntityWithMaxHealth() so the heal has a real cap
  // to test against - except the last test below, which deliberately uses plain newPlayerEntity().

  @Test
  void regenOnKillTier1HealsExactlyFiveHpOnAKill() throws Exception {
    Entity player = newPlayerEntityWithMaxHealth(50, 100); // damaged below max
    display.setPlayer(player);
    CombatStatsComponent stats = player.getComponent(CombatStatsComponent.class);

    getDefenceUpgrades().get(1).purchaseNextTier(); // "regen_on_kill" Tier 1: 5 HP/kill

    killEnemies(player, 1);

    assertEquals(55, stats.getHealth());
  }

  @Test
  void regenOnKillTier2HealsExactlyTenHpOnAKill() throws Exception {
    Entity player = newPlayerEntityWithMaxHealth(50, 100);
    display.setPlayer(player);
    CombatStatsComponent stats = player.getComponent(CombatStatsComponent.class);

    UpgradeNode regen = getDefenceUpgrades().get(1);
    regen.purchaseNextTier(); // Tier 1: 5 HP/kill
    regen.purchaseNextTier(); // Tier 2: 10 HP/kill - replaces, not adds to, the Tier 1 amount

    killEnemies(player, 1);

    assertEquals(60, stats.getHealth());
  }

  @Test
  void regenOnKillHealingIsCappedAtMaxHealth() throws Exception {
    Entity player = newPlayerEntityWithMaxHealth(98, 100); // near-full health
    display.setPlayer(player);
    CombatStatsComponent stats = player.getComponent(CombatStatsComponent.class);

    getDefenceUpgrades().get(1).purchaseNextTier(); // Tier 1: 5 HP/kill - 98 + 5 would be 103

    killEnemies(player, 1);

    assertEquals(100, stats.getHealth()); // capped at maxHealth, not 103
  }

  @Test
  void regenOnKillAtTierZeroDoesNotChangeHealthOnAKill() throws Exception {
    Entity player = newPlayerEntityWithMaxHealth(50, 100);
    display.setPlayer(player);
    CombatStatsComponent stats = player.getComponent(CombatStatsComponent.class);

    // "regen_on_kill" deliberately never purchased - stays at Tier 0/inactive.
    killEnemies(player, 1);

    assertEquals(50, stats.getHealth()); // unchanged
  }

  @Test
  void regenOnKillWithoutConsumableUseComponentHealsUncappedInsteadOfThrowing() throws Exception {
    // Regression test for the null-check fix in UpgradesDisplay.onEnemyKilled(): plain
    // newPlayerEntity() (no ConsumableUseComponent) plus an active Regen on Kill upgrade used to
    // throw an NPE the moment a kill fired. Now it should still heal, just without a max-health
    // cap to check against.
    Entity player = newPlayerEntity();
    display.setPlayer(player);
    CombatStatsComponent stats = player.getComponent(CombatStatsComponent.class);

    getDefenceUpgrades().get(1).purchaseNextTier(); // "regen_on_kill" Tier 1: 5 HP/kill

    killEnemies(player, 1);

    assertEquals(105, stats.getHealth()); // 100 + 5, uncapped since there's no max to check
  }

  // --- Attack Speed effect (actionUpgrades.get(1), "attack_speed") ---
  //
  // KILL_COUNT-based, same as Sword Damage - only its kill-countdown was covered before; these
  // cover the actual cooldown-multiplier effect on PlayerActions.

  @Test
  void attackSpeedEachTierSetsTheCorrespondingCooldownMultiplier() throws Exception {
    Entity player = newPlayerEntity();
    display.setPlayer(player);
    PlayerActions playerActions = player.getComponent(PlayerActions.class);

    UpgradeNode attackSpeed = getActionUpgrades().get(1); // "attack_speed"

    attackSpeed.purchaseNextTier(); // Tier 1
    assertEquals(0.8f, playerActions.getAttackSpeedMultiplier(), 0.0001f);

    attackSpeed.purchaseNextTier(); // Tier 2 - replaces, not stacks on top of, Tier 1's multiplier
    assertEquals(0.6f, playerActions.getAttackSpeedMultiplier(), 0.0001f);

    attackSpeed.purchaseNextTier(); // Tier 3
    assertEquals(0.4f, playerActions.getAttackSpeedMultiplier(), 0.0001f);
  }

  @Test
  void attackSpeedExpiryOnceKillsAreExhaustedResetsTheMultiplierToOne() throws Exception {
    Entity player = newPlayerEntity();
    display.setPlayer(player);
    PlayerActions playerActions = player.getComponent(PlayerActions.class);

    UpgradeNode attackSpeed = getActionUpgrades().get(1);
    attackSpeed.purchaseNextTier(); // Tier 1: 5 kills (tierKillCounts = {5, 8, 12})
    assertEquals(0.8f, playerActions.getAttackSpeedMultiplier(), 0.0001f);

    // KILL_COUNT-based (unlike Shield Durability/Player Speed+), so it expires by exhausting kills
    // rather than via tickTime() - tickTime() is a documented no-op for kill-count upgrades.
    killEnemies(player, 5);

    assertEquals(0, attackSpeed.getCurrentTier());
    assertEquals(1f, playerActions.getAttackSpeedMultiplier(), 0.0001f);
  }

  // --- Two kill-count upgrades running at once ---

  @Test
  void oneKillCountUpgradeExpiringDoesNotAffectAnotherRunningAtTheSameTime() throws Exception {
    Entity player = newPlayerEntity();
    display.setPlayer(player);
    CombatStatsComponent stats = player.getComponent(CombatStatsComponent.class);
    PlayerActions playerActions = player.getComponent(PlayerActions.class);

    UpgradeNode swordDamage = getActionUpgrades().get(0);
    UpgradeNode attackSpeed = getActionUpgrades().get(1);
    swordDamage.purchaseNextTier(); // Tier 1: 2 kills, base 10 + 5
    attackSpeed.purchaseNextTier(); // Tier 1: 5 kills, 0.8x cooldown

    killEnemies(player, 2); // exhausts Sword Damage's 2 kills, well short of Attack Speed's 5

    assertEquals(0, swordDamage.getCurrentTier()); // expired
    assertEquals(10, stats.getBaseAttack()); // effect removed, original attack restored

    assertEquals(1, attackSpeed.getCurrentTier()); // still active
    assertEquals("3 kills left", attackSpeed.getRemainingText()); // 5 - 2, untouched by the other
    assertEquals(0.8f, playerActions.getAttackSpeedMultiplier(), 0.0001f); // effect still applied
  }
}
