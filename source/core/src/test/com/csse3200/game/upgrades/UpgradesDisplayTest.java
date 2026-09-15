package com.csse3200.game.upgrades;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.csse3200.game.components.CombatStatsComponent;
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
}
