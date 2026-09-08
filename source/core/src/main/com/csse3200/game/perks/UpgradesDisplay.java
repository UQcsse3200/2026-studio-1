package com.csse3200.game.perks;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.csse3200.game.perks.UpgradeNode.ExpiryType;
import com.csse3200.game.ui.UIComponent;
import java.util.ArrayList;
import java.util.List;

/**
 * Displays the Upgrades screen: category tabs (Action / Defence), a row of
 * upgrade nodes per category, and a detail panel for the selected node.
 *
 * Every upgrade is temporary and repurchasable - Action upgrades expire
 * after a number of kills, Defence upgrades expire after a duration.
 * Buying the same upgrade again while active advances its tier and grants
 * a longer expiry window. Once fully expired, it resets to Tier 0 and can
 * be bought again from Tier 1.
 *
 * Currency is currently mocked (MOCK_CURRENCY_START) until the real
 * Currency System exists.
 */
public class UpgradesDisplay extends UIComponent {

  private static final int MOCK_CURRENCY_START = 200; // TODO: replace with real currency system

  private Table root;
  private Table nodeRow;
  private Label currencyLabel;
  private Label categoryHeaderLabel;
  private Label detailNameLabel;
  private Label detailDescriptionLabel;
  private Label detailCostLabel;
  private Label detailStatusLabel;
  private TextButton buyButton;

  private int currency = MOCK_CURRENCY_START;
  private String activeCategory = null; // nothing selected until the player picks a tab
  private UpgradeNode selectedNode;
  private UpgradesMenuComponent upgradesMenu;

  private final List<UpgradeNode> actionUpgrades = new ArrayList<>();
  private final List<UpgradeNode> defenceUpgrades = new ArrayList<>();
  private final List<UpgradeNode> movementUpgrades = new ArrayList<>();

  @Override
  public void create() {
    super.create();
    upgradesMenu = entity.getComponent(UpgradesMenuComponent.class);
    buildUpgradeData();
    addActors();
    root.setVisible(false); // hidden until upgradesMenu.isOpen() is true
  }

  private void buildUpgradeData() {
    // Action upgrades: expire after a number of kills while active.
    actionUpgrades.add(UpgradeNode.killCountBased(
        "sword_damage", "Sword Damage",
        "Increases melee damage. Stacking tiers also raises the kill threshold.",
        new int[] {40, 35, 30},
        new int[] {5, 8, 12}));

    actionUpgrades.add(UpgradeNode.killCountBased(
        "attack_speed", "Attack Speed",
        "Reduces the delay between attacks. Stacking tiers also raises the kill threshold.",
        new int[] {50, 45, 40},
        new int[] {5, 8, 12}));

    // Defence upgrades: expire after a fixed duration.
    defenceUpgrades.add(UpgradeNode.timeBased(
        "shield_durability", "Shield Durability",
        "Reduces damage taken while your shield is active. Stacking tiers also extends the duration.",
        new int[] {30, 25, 25},
        new float[] {20f, 35f, 55f}));

    defenceUpgrades.add(UpgradeNode.timeBased(
        "regen_on_kill", "Regen on Kill",
        "Damaging enemies heals you. Stacking tiers also extends the duration.",
        new int[] {60, 50},
        new float[] {15f, 30f}));

    // Movement upgrades: expire after a fixed duration, same pattern as Defence.
    // TODO: confirm with the team whether Movement should be time-based like this,
    // or kill-count-based like Action - defaulted to time-based since a speed
    // boost reads more naturally as "lasts N seconds" than "lasts N kills".
    movementUpgrades.add(UpgradeNode.timeBased(
        "player_speed", "Player Speed+",
        "Increases movement speed. Stacking tiers also extends the duration.",
        new int[] {35, 30, 30},
        new float[] {20f, 35f, 55f}));
  }

  private void addActors() {
    root = new Table();
    root.setFillParent(true);
    root.top().pad(30f);

    // --- Top bar: title + currency ---
    Table topBar = new Table();
    Label title = new Label("UPGRADES", skin);
    currencyLabel = new Label("Currency: " + currency, skin);
    topBar.add(title).left().expandX();
    topBar.add(currencyLabel).right();
    root.add(topBar).growX();
    root.row().padTop(20f);

    // --- Category tabs, centered ---
    Table tabs = new Table();
    TextButton actionTab = new TextButton("Action", skin);
    TextButton defenceTab = new TextButton("Defence", skin);
    TextButton movementTab = new TextButton("Movement", skin);

    actionTab.addListener(new ChangeListener() {
      @Override
      public void changed(ChangeEvent event, Actor actor) {
        activeCategory = "Action";
        refreshNodeRow();
      }
    });
    defenceTab.addListener(new ChangeListener() {
      @Override
      public void changed(ChangeEvent event, Actor actor) {
        activeCategory = "Defence";
        refreshNodeRow();
      }
    });
    movementTab.addListener(new ChangeListener() {
      @Override
      public void changed(ChangeEvent event, Actor actor) {
        activeCategory = "Movement";
        refreshNodeRow();
      }
    });

    tabs.add(actionTab).padRight(10f);
    tabs.add(defenceTab).padRight(10f);
    tabs.add(movementTab);
    root.add(tabs).center();
    root.row().padTop(20f);

    // --- Category header ("Select an upgrade"), centered above the node row ---
    categoryHeaderLabel = new Label("", skin);
    root.add(categoryHeaderLabel).center();
    root.row().padTop(10f);

    // --- Node row (populated by refreshNodeRow) ---
    nodeRow = new Table();
    root.add(nodeRow).center();
    root.row().padTop(30f);

    // --- Detail panel ---
    Table detailPanel = new Table();
    detailNameLabel = new Label("", skin);
    detailDescriptionLabel = new Label("", skin);
    detailDescriptionLabel.setWrap(true);
    detailCostLabel = new Label("", skin);
    detailStatusLabel = new Label("", skin);
    buyButton = new TextButton("Buy", skin);

    buyButton.addListener(new ChangeListener() {
      @Override
      public void changed(ChangeEvent event, Actor actor) {
        attemptPurchase();
      }
    });

    detailPanel.add(detailNameLabel).left().row();
    detailPanel.add(detailDescriptionLabel).width(400f).left().padTop(8f).row();
    detailPanel.add(detailCostLabel).left().padTop(8f).row();
    detailPanel.add(detailStatusLabel).left().padTop(4f).row();
    detailPanel.add(buyButton).left().padTop(12f);

    root.add(detailPanel).growX();

    refreshNodeRow();
    stage.addActor(root);
  }

  /** Rebuilds the row of upgrade nodes for whichever category is currently active. */
  private void refreshNodeRow() {
    nodeRow.clear();

    if (activeCategory == null) {
      categoryHeaderLabel.setText("");
      selectedNode = null;
      clearDetail();
      return;
    }

    categoryHeaderLabel.setText("Select an upgrade");
    List<UpgradeNode> upgrades;
    switch (activeCategory) {
      case "Action":
        upgrades = actionUpgrades;
        break;
      case "Defence":
        upgrades = defenceUpgrades;
        break;
      default:
        upgrades = movementUpgrades;
        break;
    }

    for (UpgradeNode node : upgrades) {
      TextButton nodeButton = new TextButton(
          node.getName() + (node.isActive() ? " (T" + node.getCurrentTier() + ")" : ""), skin);
      nodeButton.setColor(node.isActive() ? Color.GREEN : Color.WHITE);

      nodeButton.addListener(new ChangeListener() {
        @Override
        public void changed(ChangeEvent event, Actor actor) {
          selectedNode = node;
          showDetail(node);
        }
      });

      nodeRow.add(nodeButton).padRight(20f);
    }

    selectedNode = null;
    clearDetail();
  }

  private void showDetail(UpgradeNode node) {
    detailNameLabel.setText(node.getName()
        + (node.isActive() ? " (Tier " + node.getCurrentTier() + "/" + node.getMaxTier() + ")" : ""));
    detailDescriptionLabel.setText(node.getDescription());

    if (node.isMaxTier()) {
      detailCostLabel.setText("Max tier reached");
      buyButton.setVisible(false);
    } else {
      detailCostLabel.setText("Cost to " + (node.isActive() ? "advance tier" : "activate")
          + ": " + node.getNextTierCost());
      buyButton.setVisible(true);
      buyButton.setText(node.isActive() ? "Upgrade Tier" : "Buy");
    }

    detailStatusLabel.setText(node.isActive() ? "Active - " + node.getRemainingText() : "Not active");
  }

  private void clearDetail() {
    detailNameLabel.setText(""); // the centered categoryHeaderLabel above the node row
                                   // handles the "Select an upgrade" prompt instead
    detailDescriptionLabel.setText("");
    detailCostLabel.setText("");
    detailStatusLabel.setText("");
    buyButton.setVisible(false);
  }

  private void attemptPurchase() {
    if (selectedNode == null || selectedNode.isMaxTier()) {
      return;
    }
    int cost = selectedNode.getNextTierCost();
    if (currency < cost) {
      // TODO: show a "can't afford" message instead of silently failing
      return;
    }

    currency -= cost;
    selectedNode.purchaseNextTier();
    currencyLabel.setText("Currency: " + currency);

    // TODO: call into the Core Upgrade System's apply logic here, e.g.
    // upgradeManager.apply(selectedNode.getId(), selectedNode.getCurrentTier());
    // and make sure enemy-kill events call selectedNode.onEnemyKilled()
    // on every currently-active kill-count-based upgrade, not just this one.

    refreshNodeRow();
    showDetail(selectedNode);
  }

  @Override
  public void draw(SpriteBatch batch) {
    // Active upgrades keep counting down in the background even while this
    // screen is closed - only visibility is gated on isOpen(), not ticking.
    float delta = Gdx.graphics.getDeltaTime();
    for (UpgradeNode node : actionUpgrades) {
      node.tickTime(delta);
    }
    for (UpgradeNode node : defenceUpgrades) {
      node.tickTime(delta);
    }

    boolean isOpen = upgradesMenu != null && upgradesMenu.isOpen();
    root.setVisible(isOpen);

    if (isOpen && selectedNode != null) {
      showDetail(selectedNode); // keep the countdown text live while a node is selected
    }
  }

  @Override
  public void dispose() {
    root.clear();
    super.dispose();
  }
}