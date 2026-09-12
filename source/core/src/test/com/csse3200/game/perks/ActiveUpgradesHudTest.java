package com.csse3200.game.perks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.SnapshotArray;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import java.lang.reflect.Field;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Verifies the HUD reads UpgradesDisplay's upgrade lists live: a node appearing once active,
 * showing the right tier/remaining text, updating when tier advances, and disappearing once it
 * expires.
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

  @Test
  void hudShowsNothingWhenNoUpgradesActive() throws Exception {
    hud.draw(null);

    assertEquals(0, getHudRoot().getChildren().size);
  }

  @Test
  void hudShowsActiveUpgradeWithNameTierAndRemainingText() throws Exception {
    UpgradeNode swordDamage = getField("actionUpgrades").get(0); // "sword_damage"
    swordDamage.purchaseNextTier(); // Tier 1, 2 kills (tierKillCounts = {2, 5, 8})

    hud.draw(null);

    SnapshotArray<Actor> children = getHudRoot().getChildren();
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
        ((Label) getHudRoot().getChildren().get(0)).getText().toString());

    swordDamage.purchaseNextTier(); // Tier 2, 5 kills - buying again before it expires
    hud.draw(null);

    assertEquals(
        "Sword Damage - Tier 2 - 5 kills left",
        ((Label) getHudRoot().getChildren().get(0)).getText().toString());
  }

  @Test
  void hudRemovesUpgradeOnceItFullyExpires() throws Exception {
    UpgradeNode playerSpeed = getField("movementUpgrades").get(0); // "player_speed"
    playerSpeed.purchaseNextTier(); // Tier 1: 0 + 10s increment = 10s remaining

    hud.draw(null);
    assertEquals(1, getHudRoot().getChildren().size);

    playerSpeed.tickTime(21f); // more than enough to fully expire
    hud.draw(null);

    assertEquals(0, getHudRoot().getChildren().size);
  }
}
