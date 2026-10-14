package com.csse3200.game.upgrades;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import java.lang.reflect.Field;
import java.util.List;
import java.util.function.Predicate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Drives the real category-tab and node-button TextButtons built by addActors()/refreshNodeRow() -
 * found by walking the actual actor tree, then firing a real ChangeEvent on them - rather than
 * reflectively setting activeCategory/selectedNode directly. This exercises the ChangeListeners
 * themselves (and the switch/loop/ternary logic inside refreshNodeRow()), not just the state they
 * end up setting.
 */
@ExtendWith(GameExtension.class)
class UpgradesDisplayTabNavigationTest {
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

  private Table getRoot() throws Exception {
    Field field = UpgradesDisplay.class.getDeclaredField("root");
    field.setAccessible(true);
    return (Table) field.get(display);
  }

  private Actor findActor(Actor root, Predicate<Actor> predicate) {
    if (predicate.test(root)) {
      return root;
    }
    if (root instanceof Group) {
      for (Actor child : ((Group) root).getChildren()) {
        Actor found = findActor(child, predicate);
        if (found != null) {
          return found;
        }
      }
    }
    return null;
  }

  private TextButton findButtonWithExactText(String text) throws Exception {
    Actor found =
        findActor(
            getRoot(),
            actor ->
                actor instanceof TextButton
                    && ((TextButton) actor).getText().toString().equals(text));
    return (TextButton) found;
  }

  private TextButton findButtonStartingWith(String prefix) throws Exception {
    Actor found =
        findActor(
            getRoot(),
            actor ->
                actor instanceof TextButton
                    && ((TextButton) actor).getText().toString().startsWith(prefix));
    return (TextButton) found;
  }

  private void click(TextButton button) {
    button.fire(new ChangeListener.ChangeEvent());
  }

  private String labelText(String fieldName) throws Exception {
    Field field = UpgradesDisplay.class.getDeclaredField(fieldName);
    field.setAccessible(true);
    return ((Label) field.get(display)).getText().toString();
  }

  @SuppressWarnings("unchecked")
  private List<UpgradeNode> getActionUpgrades() throws Exception {
    Field field = UpgradesDisplay.class.getDeclaredField("actionUpgrades");
    field.setAccessible(true);
    return (List<UpgradeNode>) field.get(display);
  }

  private Table getNodeRow() throws Exception {
    Field field = UpgradesDisplay.class.getDeclaredField("nodeRow");
    field.setAccessible(true);
    return (Table) field.get(display);
  }

  /** Clicking the Action tab shows its prompt and one node button per action upgrade. */
  @Test
  void clickingTheActionTabShowsItsUpgradesAndThePrompt() throws Exception {
    click(findButtonWithExactText("Action"));

    assertEquals("Select an upgrade", labelText("categoryHeaderLabel"));
    assertEquals(getActionUpgrades().size(), getNodeRow().getChildren().size);
  }

  /** Clicking the Defence tab switches the row over to the Defence upgrades. */
  @Test
  void clickingTheDefenceTabShowsDefenceUpgrades() throws Exception {
    click(findButtonWithExactText("Defence"));

    assertEquals("Select an upgrade", labelText("categoryHeaderLabel"));
    assertNotNull(findButtonStartingWith("Shield Durability"));
  }

  /** Clicking the Movement tab exercises the switch statement's default branch. */
  @Test
  void clickingTheMovementTabShowsMovementUpgrades() throws Exception {
    click(findButtonWithExactText("Movement"));

    assertEquals("Select an upgrade", labelText("categoryHeaderLabel"));
    assertNotNull(findButtonStartingWith("Player Speed+"));
  }

  /** Clicking a node button selects it and populates the detail panel via showDetail(). */
  @Test
  void clickingANodeButtonShowsItsDetail() throws Exception {
    click(findButtonWithExactText("Action"));

    click(findButtonStartingWith("Sword Damage"));

    assertEquals("Sword Damage", labelText("detailNameLabel"));
    assertEquals("Not active", labelText("detailStatusLabel"));
  }

  /** An active upgrade's node button is coloured green and shows its current tier. */
  @Test
  void anActiveUpgradesNodeButtonIsGreenAndShowsItsTier() throws Exception {
    getActionUpgrades().get(0).purchaseNextTier(); // "sword_damage" Tier 1

    click(findButtonWithExactText("Action"));
    TextButton swordDamageButton = findButtonStartingWith("Sword Damage");

    assertEquals("Sword Damage (T1)", swordDamageButton.getText().toString());
    assertEquals(Color.GREEN, swordDamageButton.getColor());
  }
}
