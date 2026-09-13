package com.csse3200.game.perks;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.csse3200.game.ui.UIComponent;
import java.util.HashMap;
import java.util.Map;

/**
 * Small always-on HUD, in the top-right corner, listing every currently-active upgrade (name, tier,
 * remaining time/kills). Unlike UpgradesDisplay's own root table, this is NOT gated by
 * UpgradesMenuComponent.isOpen() - it stays visible during normal gameplay so the player can see
 * what's currently buffed without opening the Upgrades screen.
 *
 * <p>Reads the sibling UpgradesDisplay component's upgrade lists directly (same entity, same
 * pattern PauseMenuInputComponent uses to reach PauseMenuComponent) rather than owning any upgrade
 * state itself.
 */
public class ActiveUpgradesHud extends UIComponent {
  private Table root;
  private UpgradesDisplay upgradesDisplay;

  // One Label reused per node across frames (rather than allocating new Labels every frame) -
  // created lazily the first time a node is seen active, and left in this map (just re-parented
  // into/out of the table) for the rest of the upgrade's lifetime.
  private final Map<UpgradeNode, Label> labelsByNode = new HashMap<>();

  @Override
  public void create() {
    super.create();
    upgradesDisplay = entity.getComponent(UpgradesDisplay.class);

    root = new Table();
    root.setFillParent(true);
    root.top().right().pad(10f); // top-right - clear of PerformanceDisplay, which sits top-left
    stage.addActor(root);
  }

  @Override
  public void draw(SpriteBatch batch) {
    root.clearChildren(); // cheap - re-parents existing Label actors, doesn't recreate them

    if (upgradesDisplay == null) {
      return;
    }

    for (UpgradeNode node : upgradesDisplay.getAllUpgrades()) {
      if (!node.isActive()) {
        continue;
      }

      Label label = labelsByNode.get(node);
      if (label == null) {
        label = new Label("", skin, "small");
        labelsByNode.put(node, label);
      }
      label.setText(describe(node));
      root.add(label).right().row();
    }
  }

  private String describe(UpgradeNode node) {
    return node.getName() + " - Tier " + node.getCurrentTier() + " - " + node.getRemainingText();
  }

  @Override
  public void dispose() {
    root.clear();
    super.dispose();
  }
}
