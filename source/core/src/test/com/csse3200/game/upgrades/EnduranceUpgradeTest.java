package com.csse3200.game.upgrades;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import com.badlogic.gdx.scenes.scene2d.Stage;
import com.csse3200.game.components.player.StaminaComponent;
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

/** Endurance: a time-based Movement upgrade that scales the player's stamina refill rate. */
@ExtendWith(GameExtension.class)
class EnduranceUpgradeTest {
  private static final float DELTA = 0.0001f;

  private UpgradesDisplay display;

  @BeforeEach
  void beforeEach() {
    RenderService renderService = new RenderService();
    renderService.setStage(mock(Stage.class));
    ServiceLocator.registerRenderService(renderService);
    ServiceLocator.registerResourceService(mock(ResourceService.class));

    display = new UpgradesDisplay();
    new Entity().addComponent(new UpgradesMenuComponent()).addComponent(display).create();
  }

  @SuppressWarnings("unchecked")
  private List<UpgradeNode> getMovementUpgrades() throws Exception {
    Field field = UpgradesDisplay.class.getDeclaredField("movementUpgrades");
    field.setAccessible(true);
    return (List<UpgradeNode>) field.get(display);
  }

  private UpgradeNode endurance() throws Exception {
    return getMovementUpgrades().get(1); // "endurance", after "player_speed"
  }

  private Entity newPlayerWithStamina() {
    return new Entity().addComponent(new StaminaComponent());
  }

  @Test
  void buyingEachTierSetsTheRegenMultiplierForThatTier() throws Exception {
    Entity player = newPlayerWithStamina();
    display.setPlayer(player);
    StaminaComponent stamina = player.getComponent(StaminaComponent.class);

    endurance().purchaseNextTier(); // Tier 1
    assertEquals(1.6f, stamina.getRegenMultiplier(), DELTA);

    endurance().purchaseNextTier(); // Tier 2
    assertEquals(2.0f, stamina.getRegenMultiplier(), DELTA);

    endurance().purchaseNextTier(); // Tier 3
    assertEquals(2.5f, stamina.getRegenMultiplier(), DELTA);
  }

  @Test
  void expiryResetsTheRegenMultiplierToOne() throws Exception {
    Entity player = newPlayerWithStamina();
    display.setPlayer(player);
    StaminaComponent stamina = player.getComponent(StaminaComponent.class);

    endurance().purchaseNextTier(); // Tier 1: 10s
    endurance().tickTime(11f); // more than enough to fully expire

    assertEquals(0, endurance().getCurrentTier());
    assertEquals(1.0f, stamina.getRegenMultiplier(), DELTA);
  }

  @Test
  void enduranceAppearsInGetAllUpgrades() {
    boolean found =
        display.getAllUpgrades().stream()
            .anyMatch(
                node -> node.getId().equals("endurance") && node.getName().equals("Endurance"));

    assertTrue(found, "Expected Endurance to be listed by getAllUpgrades() for the shop and HUD.");
  }

  @Test
  void buyingWithoutAPlayerOrStaminaComponentDoesNotThrow() throws Exception {
    assertDoesNotThrow(() -> endurance().purchaseNextTier()); // no player set at all

    display.setPlayer(new Entity()); // player present, but no StaminaComponent
    assertDoesNotThrow(() -> endurance().purchaseNextTier());
    assertDoesNotThrow(() -> endurance().tickTime(11f));
  }
}
