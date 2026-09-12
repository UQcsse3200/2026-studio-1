package com.csse3200.game.perks;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;

import com.badlogic.gdx.scenes.scene2d.Stage;
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
 * attemptPurchase()/selectedNode are private, so this drives them the same way
 * PauseMenuDisplayTest drives PauseMenuDisplay's package-private navigation methods - the only
 * difference is reflection is needed here since these particular members are private, not a
 * different code path from what a real Buy click triggers.
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

  private void selectNode(UpgradeNode node) throws Exception {
    Field field = UpgradesDisplay.class.getDeclaredField("selectedNode");
    field.setAccessible(true);
    field.set(display, node);
  }

  private UpgradeNode getSelectedNode() throws Exception {
    Field field = UpgradesDisplay.class.getDeclaredField("selectedNode");
    field.setAccessible(true);
    return (UpgradeNode) field.get(display);
  }

  private void attemptPurchase() throws Exception {
    Method method = UpgradesDisplay.class.getDeclaredMethod("attemptPurchase");
    method.setAccessible(true);
    method.invoke(display);
  }

  /**
   * Regression test for a crash where clicking Buy threw a NullPointerException: attemptPurchase()
   * called refreshNodeRow() (which resets selectedNode to null as a side effect, needed for the
   * tab-switch case) and then immediately called showDetail(selectedNode) with that now-null
   * reference.
   */
  @Test
  void purchasingAnUpgradeShouldNotThrow() throws Exception {
    UpgradeNode swordDamage = getActionUpgrades().get(0);
    selectNode(swordDamage);

    assertDoesNotThrow(this::attemptPurchase);
  }

  @Test
  void purchasingShouldAdvanceTierAndKeepTheNodeSelected() throws Exception {
    UpgradeNode swordDamage = getActionUpgrades().get(0);
    selectNode(swordDamage);

    attemptPurchase();

    assertEquals(1, swordDamage.getCurrentTier());
    assertSame(swordDamage, getSelectedNode());
  }

  @Test
  void purchasingASecondTierShouldAdvanceAgain() throws Exception {
    UpgradeNode swordDamage = getActionUpgrades().get(0);
    selectNode(swordDamage);

    attemptPurchase();
    attemptPurchase();

    assertEquals(2, swordDamage.getCurrentTier());
    assertSame(swordDamage, getSelectedNode());
  }

  private Entity newPlayerEntity() {
    return new Entity()
        .addComponent(new CombatStatsComponent(100, 10))
        .addComponent(new PlayerActions());
  }

  @Test
  void swordDamagePurchaseShouldAddTierBonusOnTopOfBaseAttack() throws Exception {
    Entity player = newPlayerEntity();
    display.setPlayer(player);

    UpgradeNode swordDamage = getActionUpgrades().get(0);
    selectNode(swordDamage);
    attemptPurchase(); // Tier 1: base 10 + 5

    assertEquals(15, player.getComponent(CombatStatsComponent.class).getBaseAttack());
  }

  @Test
  void swordDamageSecondTierShouldReplaceNotStackTheBonus() throws Exception {
    Entity player = newPlayerEntity();
    display.setPlayer(player);

    UpgradeNode swordDamage = getActionUpgrades().get(0);
    selectNode(swordDamage);
    attemptPurchase(); // Tier 1: 10 + 5 = 15
    selectNode(swordDamage);
    attemptPurchase(); // Tier 2: 10 + 10 = 20, not 15 + 10 = 25

    assertEquals(20, player.getComponent(CombatStatsComponent.class).getBaseAttack());
  }

  @Test
  void swordDamageExpiryShouldRestoreTheOriginalBaseAttack() throws Exception {
    Entity player = newPlayerEntity();
    display.setPlayer(player);

    UpgradeNode swordDamage = getActionUpgrades().get(0);
    selectNode(swordDamage);
    attemptPurchase(); // Tier 1: 10 + 5 = 15, 2 kills remaining (tierKillCounts = {2, 5, 8})

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

    Field field = UpgradesDisplay.class.getDeclaredField("movementUpgrades");
    field.setAccessible(true);
    @SuppressWarnings("unchecked")
    UpgradeNode playerSpeed = ((List<UpgradeNode>) field.get(display)).get(0); // "player_speed"

    selectNode(playerSpeed);
    attemptPurchase(); // Tier 1: 1.15x, 0 + 10s increment = 10s remaining

    assertEquals(1.15f, playerActions.getEffectiveSpeedMultiplier(), 0.0001f);

    playerSpeed.tickTime(21f); // more than enough to fully expire

    assertEquals(1f, playerActions.getEffectiveSpeedMultiplier(), 0.0001f);
  }
}
