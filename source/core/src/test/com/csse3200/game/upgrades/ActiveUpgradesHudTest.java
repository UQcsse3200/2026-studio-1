package com.csse3200.game.upgrades;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.SnapshotArray;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import com.csse3200.game.ui.UIComponent;
import java.lang.reflect.Field;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Verifies the HUD reads UpgradesDisplay's upgrade lists live: a node appearing once active,
 * showing the right tier/remaining text, updating when tier advances, and disappearing once it
 * expires - and that it is drawn on the shop toast's charcoal panel (root -> panel -> labels), with
 * white text, no runtime tinting, and no empty panel when nothing is active.
 */
@ExtendWith(GameExtension.class)
class ActiveUpgradesHudTest {
  private UpgradesDisplay upgradesDisplay;
  private ActiveUpgradesHud hud;

  @BeforeEach
  void beforeEach() {
    RenderService renderService = new RenderService();
    renderService.setStage(mock(Stage.class));
    ServiceLocator.registerRenderService(renderService);
    ServiceLocator.registerResourceService(mock(ResourceService.class));

    upgradesDisplay = new UpgradesDisplay();
    hud = new ActiveUpgradesHud();
    new Entity()
        .addComponent(new UpgradesMenuComponent())
        .addComponent(upgradesDisplay)
        .addComponent(hud)
        .create();
  }

  @SuppressWarnings("unchecked")
  private List<UpgradeNode> getField(String name) throws Exception {
    Field field = UpgradesDisplay.class.getDeclaredField(name);
    field.setAccessible(true);
    return (List<UpgradeNode>) field.get(upgradesDisplay);
  }

  private Table getHudRoot() throws Exception {
    Field field = ActiveUpgradesHud.class.getDeclaredField("root");
    field.setAccessible(true);
    return (Table) field.get(hud);
  }

  /** The charcoal panel the labels live in (a child of root). */
  private Table getHudPanel() throws Exception {
    Field field = ActiveUpgradesHud.class.getDeclaredField("panel");
    field.setAccessible(true);
    return (Table) field.get(hud);
  }

  /** UIComponent.skin is protected static, so read it reflectively from this test's package. */
  private Skin getSkin() throws Exception {
    Field field = UIComponent.class.getDeclaredField("skin");
    field.setAccessible(true);
    return (Skin) field.get(null);
  }

  @Test
  void hudShowsNothingWhenNoUpgradesActive() throws Exception {
    hud.draw(null);

    assertEquals(0, getHudPanel().getChildren().size);
  }

  @Test
  void hudShowsActiveUpgradeWithNameTierAndRemainingText() throws Exception {
    UpgradeNode swordDamage = getField("actionUpgrades").get(0); // "sword_damage"
    swordDamage.purchaseNextTier(); // Tier 1, 2 kills (tierKillCounts = {2, 5, 8})

    hud.draw(null);

    SnapshotArray<Actor> children = getHudPanel().getChildren();
    assertEquals(1, children.size);
    assertEquals(
        "Sword Damage - Tier 1 - 2 kills left", ((Label) children.get(0)).getText().toString());
  }

  @Test
  void hudUpdatesImmediatelyWhenAFurtherPurchaseAdvancesTier() throws Exception {
    UpgradeNode swordDamage = getField("actionUpgrades").get(0);
    swordDamage.purchaseNextTier(); // Tier 1, 2 kills
    hud.draw(null);
    assertEquals(
        "Sword Damage - Tier 1 - 2 kills left",
        ((Label) getHudPanel().getChildren().get(0)).getText().toString());

    swordDamage.purchaseNextTier(); // Tier 2, 5 kills - buying again before it expires
    hud.draw(null);

    assertEquals(
        "Sword Damage - Tier 2 - 5 kills left",
        ((Label) getHudPanel().getChildren().get(0)).getText().toString());
  }

  @Test
  void hudShowsTheUpdatedKillsLeftTextAfterARealKillAndDropsTheUpgradeOnceKillsRunOut()
      throws Exception {
    Entity player = new Entity();
    upgradesDisplay.setPlayer(player); // registers UpgradesDisplay's "enemyKilled" listener
    UpgradeNode swordDamage = getField("actionUpgrades").get(0); // "sword_damage"
    swordDamage.purchaseNextTier(); // Tier 1, 2 kills (tierKillCounts = {2, 5, 8})
    hud.draw(null);
    assertEquals(
        "Sword Damage - Tier 1 - 2 kills left",
        ((Label) getHudPanel().getChildren().get(0)).getText().toString());

    player.getEvents().trigger("enemyKilled"); // the same event CombatStatsComponent.hit() fires
    hud.draw(null);

    assertEquals(
        "Sword Damage - Tier 1 - 1 kills left",
        ((Label) getHudPanel().getChildren().get(0)).getText().toString());

    player.getEvents().trigger("enemyKilled"); // last kill - upgrade expires
    hud.draw(null);

    assertEquals(0, getHudPanel().getChildren().size);
  }

  @Test
  void hudRemovesUpgradeOnceItFullyExpires() throws Exception {
    UpgradeNode playerSpeed = getField("movementUpgrades").get(0); // "player_speed"
    playerSpeed.purchaseNextTier(); // Tier 1: 0 + 10s increment = 10s remaining

    hud.draw(null);
    assertEquals(1, getHudPanel().getChildren().size);

    playerSpeed.tickTime(21f); // more than enough to fully expire
    hud.draw(null);

    assertEquals(0, getHudPanel().getChildren().size);
  }

  // --- Shield hits indicator ---
  //
  // Shield hits live on CombatStatsComponent, not UpgradeNode, so they need the sibling
  // UpgradesDisplay.getShieldHitsRemaining() rather than node.getRemainingText().

  @Test
  void hudShowsShieldHitsRemainingAfterSomeAreConsumed() throws Exception {
    Entity player = new Entity().addComponent(new CombatStatsComponent(100, 10));
    upgradesDisplay.setPlayer(player);
    CombatStatsComponent stats = player.getComponent(CombatStatsComponent.class);
    CombatStatsComponent attacker = new CombatStatsComponent(1, 20);

    UpgradeNode shield = getField("defenceUpgrades").get(0); // "shield_durability"
    shield.purchaseNextTier(); // Tier 1: 3 shield hits

    stats.hit(attacker); // 1 hit consumed, 2 left

    hud.draw(null);

    assertEquals(
        "Shield Durability - Tier 1 - " + shield.getRemainingText() + " - 2 hits left",
        ((Label) getHudPanel().getChildren().get(0)).getText().toString());
  }

  @Test
  void hudUpdatesShieldHitsRemainingLiveAsHitsAreAbsorbed() throws Exception {
    Entity player = new Entity().addComponent(new CombatStatsComponent(100, 10));
    upgradesDisplay.setPlayer(player);
    CombatStatsComponent stats = player.getComponent(CombatStatsComponent.class);
    CombatStatsComponent attacker = new CombatStatsComponent(1, 20);

    UpgradeNode shield = getField("defenceUpgrades").get(0);
    shield.purchaseNextTier(); // Tier 1: 3 shield hits

    hud.draw(null);
    assertEquals(
        "Shield Durability - Tier 1 - " + shield.getRemainingText() + " - 3 hits left",
        ((Label) getHudPanel().getChildren().get(0)).getText().toString());

    stats.hit(attacker);
    hud.draw(null);

    assertEquals(
        "Shield Durability - Tier 1 - " + shield.getRemainingText() + " - 2 hits left",
        ((Label) getHudPanel().getChildren().get(0)).getText().toString());
  }

  // --- Charcoal panel ---

  @Test
  void hudIsRootThenPanelThenLabels() throws Exception {
    UpgradeNode playerSpeed = getField("movementUpgrades").get(0);
    playerSpeed.purchaseNextTier();
    hud.draw(null);

    assertEquals(1, getHudRoot().getChildren().size); // root holds only the panel
    assertSame(getHudPanel(), getHudRoot().getChildren().get(0));
    assertSame(getHudRoot(), getHudPanel().getParent());
    assertSame(getHudPanel(), getHudPanel().getChildren().get(0).getParent()); // label in panel
  }

  @Test
  void panelIsHiddenWithNoActiveUpgradesAndVisibleWithOne() throws Exception {
    hud.draw(null);
    assertFalse(getHudPanel().isVisible()); // no empty charcoal box when nothing is active

    UpgradeNode playerSpeed = getField("movementUpgrades").get(0); // "player_speed"
    playerSpeed.purchaseNextTier();
    hud.draw(null);
    assertTrue(getHudPanel().isVisible());

    playerSpeed.tickTime(21f); // fully expires
    hud.draw(null);
    assertFalse(getHudPanel().isVisible()); // hides again once the last upgrade is gone
  }

  @Test
  void panelBackgroundIsTheToastCharcoalDrawable() throws Exception {
    Object expected = getSkin().getDrawable("toast-charcoal");

    assertNotNull(getHudPanel().getBackground());
    assertSame(expected, getHudPanel().getBackground()); // the exact baked drawable the toast uses
  }

  @Test
  void labelStyleUsesWhiteTextButTheSkinsSmallFontSoTextSizeIsUnchanged() throws Exception {
    getField("movementUpgrades").get(0).purchaseNextTier();
    hud.draw(null);

    Label label = (Label) getHudPanel().getChildren().get(0);
    Label.LabelStyle skinSmall = getSkin().get("small", Label.LabelStyle.class);

    assertEquals(Color.WHITE, label.getStyle().fontColor); // readable on charcoal (skin is black)
    assertSame(skinSmall.font, label.getStyle().font); // same font face and size as before
  }

  @Test
  void noRuntimeTintIsAppliedToRootPanelOrLabels() throws Exception {
    getField("movementUpgrades").get(0).purchaseNextTier();
    hud.draw(null);

    // Actor colours (which Table backgrounds and Images push into the shared SpriteBatch) must
    // stay the default white - the charcoal and the white text are both baked in elsewhere.
    assertEquals(Color.WHITE, getHudRoot().getColor());
    assertEquals(Color.WHITE, getHudPanel().getColor());
    assertEquals(Color.WHITE, getHudPanel().getChildren().get(0).getColor());
  }
}
