package com.csse3200.game.upgrades;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.player.DeathStateComponent;
import com.csse3200.game.components.player.PlayerActions;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.pausemenu.PauseMenuComponent;
import com.csse3200.game.ui.UIComponent;
import java.util.ArrayList;
import java.util.List;

/**
 * Displays the Upgrades screen: category tabs (Action / Defence), a row of upgrade nodes per
 * category, and a detail panel for the selected node's name, description, current tier and
 * remaining time/kills.
 *
 * <p>View-only - there is no purchase action anywhere on this screen. It never calls {@link
 * UpgradeNode#purchaseNextTier()} itself; the only way to actually buy an upgrade (deduct gold,
 * advance its tier) is through {@code ShopDisplay}'s real Upgrades-tab buy flow, which applies the
 * purchase to the exact same UpgradeNode instances this screen reads from. This screen used to have
 * its own Buy button that spent real gold directly, bypassing the shop entirely - that's been
 * removed so there is exactly one way to purchase an upgrade.
 *
 * <p>Every upgrade is temporary - Action upgrades expire after a number of kills, Defence upgrades
 * expire after a duration. Buying the same upgrade again (via the shop) while active advances its
 * tier and grants a longer expiry window. Once fully expired, it resets to Tier 0 and can be bought
 * again from Tier 1.
 */
public class UpgradesDisplay extends UIComponent {

  // Tier -> effect magnitude for the two upgrades with real gameplay effects wired up so far.
  // Index 0 = Tier 1, etc. Attack Speed / Shield Durability / Regen on Kill have no effect wired
  // yet - they need infrastructure (attack-speed hook, shield component, on-kill heal) that
  // doesn't exist in the codebase yet.
  private static final float[] PLAYER_SPEED_MULTIPLIER_PER_TIER = {1.15f, 1.3f, 1.5f};
  private static final int[] SWORD_DAMAGE_BONUS_PER_TIER = {5, 10, 15};
  private static final float[] ATTACK_SPEED_COOLDOWN_MULTIPLIER_PER_TIER = {0.8f, 0.6f, 0.4f};
  private static final int[] REGEN_HEAL_PER_KILL_PER_TIER = {5, 10};
  // Fired on the player entity whenever Sword Damage's bonus changes (including back to 0 on
  // expiry), so anything else on the player (e.g. WeaponDisplay) can reflect it without needing
  // a direct reference to this class - see applySwordDamageEffect()/removeSwordDamageEffect().
  private static final String SWORD_DAMAGE_BONUS_EVENT = "swordDamageBonusChanged";

  private Table root;
  private Table nodeRow;
  private Label categoryHeaderLabel;
  private Label detailNameLabel;
  private Label detailDescriptionLabel;
  private Label detailStatusLabel;

  private String activeCategory = null; // nothing selected until the player picks a tab
  private UpgradeNode selectedNode;
  private UpgradesMenuComponent upgradesMenu;

  // Fetched off this same entity, the same way PauseMenuInputComponent/PauseMenuDisplay already
  // do - PauseMenuComponent is a sibling component on the shared "ui" entity in MainGameScreen,
  // not separately tracked state.
  private PauseMenuComponent pauseMenu;

  // The player entity, so activated upgrades can reach PlayerActions/CombatStatsComponent to
  // apply real gameplay effects. Not available at create() time - MainGameScreen constructs the
  // player (via LevelGameArea) after the UI entity is registered - so this is wired in later via
  // setPlayer() once the player actually exists, the same way MainGameScreen already holds onto
  // sibling display components (e.g. deathScreenDisplay) to call into them once both sides are
  // ready.
  private Entity player;

  // Sword Damage's baseAttack before any bonus from this upgrade was applied. Captured once on
  // first activation so later tier changes/expiry always compute off the true original value,
  // not whatever the previous tier's bonus already modified it to. Null means "no bonus applied
  // right now".
  private Integer swordDamageBaselineAttack;

  private final List<UpgradeNode> actionUpgrades = new ArrayList<>();
  private final List<UpgradeNode> defenceUpgrades = new ArrayList<>();
  private final List<UpgradeNode> movementUpgrades = new ArrayList<>();

  @Override
  public void create() {
    super.create();
    upgradesMenu = entity.getComponent(UpgradesMenuComponent.class);
    pauseMenu = entity.getComponent(PauseMenuComponent.class);
    buildUpgradeData();
    addActors();
    root.setVisible(false); // hidden until upgradesMenu.isOpen() is true
  }

  /**
   * Supplies the player entity so activated upgrades can reach its PlayerActions/
   * CombatStatsComponent. Called by MainGameScreen once the player has actually been spawned (after
   * this UI entity is already registered), so effect application/removal below must not assume
   * player is non-null at any point before this runs.
   */
  public void setPlayer(Entity player) {
    this.player = player;
    player.getEvents().addListener("enemyKilled", this::onEnemyKilled);
  }

  private void onEnemyKilled() {
    if (player == null) {
      return;
    }

    // Count this kill against every kill-count upgrade (Sword Damage, Attack Speed). Must run
    // before the Regen logic below, which returns early whenever Regen on Kill isn't active - that
    // would otherwise skip this loop for the common case. UpgradeNode.onEnemyKilled() is already a
    // no-op for inactive and time-based nodes, so no filtering is needed here; it also expires the
    // upgrade (firing its onExpired effect removal) when the last kill is used up.
    for (UpgradeNode node : getAllUpgrades()) {
      node.onEnemyKilled();
    }

    UpgradeNode regenOnKill = null;
    for (UpgradeNode node : defenceUpgrades) {
      if (node.getId().equals("regen_on_kill")) {
        regenOnKill = node;
        break;
      }
    }

    if (regenOnKill == null || regenOnKill.getCurrentTier() == 0) {
      return;
    }

    int healAmount = REGEN_HEAL_PER_KILL_PER_TIER[regenOnKill.getCurrentTier() - 1];

    CombatStatsComponent combatStats = player.getComponent(CombatStatsComponent.class);

    if (combatStats == null) {
      return;
    }

    int maxHealth =
        player
            .getComponent(com.csse3200.game.components.player.ConsumableUseComponent.class)
            .getMaxHealth();

    combatStats.setHealth(Math.min(combatStats.getHealth() + healAmount, maxHealth));
  }

  /**
   * @return a fresh combined list of every upgrade node across all categories (action, defence,
   *     movement), for a sibling component (e.g. {@link ActiveUpgradesHud}) to iterate over.
   *     Returned as a new list each call so callers can't mutate the internal per-category lists.
   */
  public List<UpgradeNode> getAllUpgrades() {
    List<UpgradeNode> all =
        new ArrayList<>(actionUpgrades.size() + defenceUpgrades.size() + movementUpgrades.size());
    all.addAll(actionUpgrades);
    all.addAll(defenceUpgrades);
    all.addAll(movementUpgrades);
    return all;
  }

  private void buildUpgradeData() {
    // Action upgrades: expire after a number of kills while active.
    UpgradeNode swordDamage =
        UpgradeNode.killCountBased(
            "sword_damage",
            "Sword Damage",
            "Increases melee damage. Stacking tiers also raises the kill threshold.",
            new int[] {40, 35, 30},
            new int[] {2, 5, 8});
    swordDamage.setOnTierChanged(() -> applySwordDamageEffect(swordDamage));
    swordDamage.setOnExpired(this::removeSwordDamageEffect);
    actionUpgrades.add(swordDamage);

    UpgradeNode attackSpeed =
        UpgradeNode.killCountBased(
            "attack_speed",
            "Attack Speed",
            "Reduces the delay between attacks. Stacking tiers also raises the kill threshold.",
            new int[] {50, 45, 40},
            new int[] {5, 8, 12});

    attackSpeed.setOnTierChanged(() -> applyAttackSpeedEffect(attackSpeed));
    attackSpeed.setOnExpired(this::removeAttackSpeedEffect);

    actionUpgrades.add(attackSpeed);

    // Defence upgrades: expire after a fixed duration.
    UpgradeNode shieldDurability =
        UpgradeNode.timeBased(
            "shield_durability",
            "Shield Durability",
            "Absorbs a limited number of attacks. Stacking tiers increases shield durability.",
            new int[] {30, 25, 25},
            new float[] {20f, 35f, 55f});

    shieldDurability.setOnTierChanged(() -> applyShieldEffect(shieldDurability));
    shieldDurability.setOnExpired(this::removeShieldEffect);

    defenceUpgrades.add(shieldDurability);

    UpgradeNode regenOnKill =
        UpgradeNode.timeBased(
            "regen_on_kill",
            "Regen on Kill",
            "Heals you when you defeat an enemy. Stacking tiers also extends the duration.",
            new int[] {60, 50},
            new float[] {15f, 30f});

    regenOnKill.setOnTierChanged(() -> applyRegenEffect(regenOnKill));
    regenOnKill.setOnExpired(this::removeRegenEffect);

    defenceUpgrades.add(regenOnKill);

    // Movement upgrades: expire after a fixed duration, same pattern as Defence.
    // TODO: confirm with the team whether Movement should be time-based like this,
    // or kill-count-based like Action - defaulted to time-based since a speed
    // boost reads more naturally as "lasts N seconds" than "lasts N kills".
    //
    // Every tier adds a flat +10s on top of whatever time is currently remaining (see
    // UpgradeNode.purchaseNextTier()'s TIME branch) - buying again before it expires always
    // extends the timer further rather than resetting it to a bigger flat total.
    UpgradeNode playerSpeed =
        UpgradeNode.timeBased(
            "player_speed",
            "Player Speed+",
            "Increases movement speed. Stacking tiers also adds 10s to the remaining duration.",
            new int[] {35, 30, 25},
            new float[] {10f, 10f, 10f});
    playerSpeed.setOnTierChanged(() -> applyPlayerSpeedEffect(playerSpeed));
    playerSpeed.setOnExpired(() -> removePlayerSpeedEffect(playerSpeed));
    movementUpgrades.add(playerSpeed);
  }

  /**
   * Applies/refreshes the Player Speed+ effect on PlayerActions. The node itself is used as the
   * modifier key so a later removeSpeedModifier() call can target exactly this upgrade's
   * contribution without touching any other active speed modifier.
   */
  private void applyPlayerSpeedEffect(UpgradeNode node) {
    PlayerActions playerActions = getPlayerActions();
    if (playerActions == null) {
      return;
    }
    float multiplier = PLAYER_SPEED_MULTIPLIER_PER_TIER[node.getCurrentTier() - 1];
    playerActions.addSpeedModifier(node, multiplier);
  }

  private void removePlayerSpeedEffect(UpgradeNode node) {
    PlayerActions playerActions = getPlayerActions();
    if (playerActions != null) {
      playerActions.removeSpeedModifier(node);
    }
  }

  private void applyShieldEffect(UpgradeNode node) {
    CombatStatsComponent combatStats = player.getComponent(CombatStatsComponent.class);
    if (combatStats == null) {
      return;
    }

    int[] shieldHitsPerTier = {3, 5, 8};
    int shieldHits = shieldHitsPerTier[node.getCurrentTier() - 1];

    combatStats.setShieldHits(shieldHits);
  }

  private void removeShieldEffect() {
    CombatStatsComponent combatStats = player.getComponent(CombatStatsComponent.class);
    if (combatStats != null) {
      combatStats.setShieldHits(0);
    }
  }

  private void applyRegenEffect(UpgradeNode node) {
    // Regen is triggered when an enemy is killed, so there is no
    // continuous effect to apply here.
  }

  private void removeRegenEffect() {
    // Regen has no persistent player stat to remove.
  }

  private void applyAttackSpeedEffect(UpgradeNode node) {
    PlayerActions playerActions = getPlayerActions();
    if (playerActions == null) {
      return;
    }

    float multiplier = ATTACK_SPEED_COOLDOWN_MULTIPLIER_PER_TIER[node.getCurrentTier() - 1];

    playerActions.setAttackSpeedMultiplier(multiplier);
  }

  private void removeAttackSpeedEffect() {
    PlayerActions playerActions = getPlayerActions();
    if (playerActions != null) {
      playerActions.setAttackSpeedMultiplier(1f);
    }
  }

  /**
   * Applies/refreshes the Sword Damage effect on CombatStatsComponent. Recomputes from the stored
   * baseline (captured on first activation) plus the current tier's bonus every time, rather than
   * adding on top of the already-modified value - so buying tier 2 after tier 1 replaces the bonus
   * instead of stacking it twice.
   */
  private void applySwordDamageEffect(UpgradeNode node) {
    CombatStatsComponent combatStats = getCombatStats();
    if (combatStats == null) {
      return;
    }
    if (swordDamageBaselineAttack == null) {
      swordDamageBaselineAttack = combatStats.getBaseAttack();
    }
    int bonus = SWORD_DAMAGE_BONUS_PER_TIER[node.getCurrentTier() - 1];
    combatStats.setBaseAttack(swordDamageBaselineAttack + bonus);
    // combatStats being non-null (from getCombatStats()) means player is non-null too.
    player.getEvents().trigger(SWORD_DAMAGE_BONUS_EVENT, bonus);
  }

  /** Restores baseAttack to whatever it was before Sword Damage first applied a bonus. */
  private void removeSwordDamageEffect() {
    if (swordDamageBaselineAttack == null) {
      return; // no bonus was ever applied (e.g. player wasn't set yet) - nothing to restore
    }
    CombatStatsComponent combatStats = getCombatStats();
    if (combatStats != null) {
      combatStats.setBaseAttack(swordDamageBaselineAttack);
    }
    swordDamageBaselineAttack = null;
    if (player != null) {
      player.getEvents().trigger(SWORD_DAMAGE_BONUS_EVENT, 0);
    }
  }

  private PlayerActions getPlayerActions() {
    return player == null ? null : player.getComponent(PlayerActions.class);
  }

  private CombatStatsComponent getCombatStats() {
    return player == null ? null : player.getComponent(CombatStatsComponent.class);
  }

  private DeathStateComponent getDeathState() {
    return player == null ? null : player.getComponent(DeathStateComponent.class);
  }

  private void addActors() {
    root = new Table();
    root.setFillParent(true);
    root.top().pad(30f);

    // --- Title ---
    Label title = new Label("UPGRADES", skin);
    root.add(title).left();
    root.row().padTop(20f);

    // --- Category tabs, centered ---
    Table tabs = new Table();
    TextButton actionTab = new TextButton("Action", skin);
    TextButton defenceTab = new TextButton("Defence", skin);
    TextButton movementTab = new TextButton("Movement", skin);

    actionTab.addListener(
        new ChangeListener() {
          @Override
          public void changed(ChangeEvent event, Actor actor) {
            activeCategory = "Action";
            refreshNodeRow();
          }
        });
    defenceTab.addListener(
        new ChangeListener() {
          @Override
          public void changed(ChangeEvent event, Actor actor) {
            activeCategory = "Defence";
            refreshNodeRow();
          }
        });
    movementTab.addListener(
        new ChangeListener() {
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

    // --- Detail panel (view-only: name, description, tier/status) ---
    Table detailPanel = new Table();
    detailNameLabel = new Label("", skin);
    detailDescriptionLabel = new Label("", skin);
    detailDescriptionLabel.setWrap(true);
    detailStatusLabel = new Label("", skin);

    detailPanel.add(detailNameLabel).left().row();
    detailPanel.add(detailDescriptionLabel).width(400f).left().padTop(8f).row();
    detailPanel.add(detailStatusLabel).left().padTop(4f);

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
      TextButton nodeButton =
          new TextButton(
              node.getName() + (node.isActive() ? " (T" + node.getCurrentTier() + ")" : ""), skin);
      nodeButton.setColor(node.isActive() ? Color.GREEN : Color.WHITE);

      nodeButton.addListener(
          new ChangeListener() {
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
    detailNameLabel.setText(
        node.getName()
            + (node.isActive()
                ? " (Tier " + node.getCurrentTier() + "/" + node.getMaxTier() + ")"
                : ""));
    detailDescriptionLabel.setText(node.getDescription());
    detailStatusLabel.setText(
        node.isActive() ? "Active - " + node.getRemainingText() : "Not active");
  }

  private void clearDetail() {
    detailNameLabel.setText(""); // the centered categoryHeaderLabel above the node row
    // handles the "Select an upgrade" prompt instead
    detailDescriptionLabel.setText("");
    detailStatusLabel.setText("");
  }

  @Override
  public void draw(SpriteBatch batch) {
    // Active upgrades keep counting down in the background even while this
    // screen is closed - only visibility is gated on isOpen(), not ticking.
    //
    // ...but not while the game is paused or the player is dead - in both cases the game world
    // itself is frozen (see MainGameScreen.render()'s own pauseMenu.isPaused()/isPlayerDead()
    // guards around physics/entity updates), so an upgrade's countdown freezing right along with
    // it is just "don't tick this frame" - no separate resume logic needed, since remainingSeconds/
    // remainingKills are never touched while frozen, tickTime() picks up from the exact value it
    // left off at once both conditions are false again. Tier level (currentTier) is never touched
    // here either way - only the countdown ticking is gated.
    boolean paused = pauseMenu != null && pauseMenu.isPaused();
    DeathStateComponent deathState = getDeathState();
    boolean playerDead = deathState != null && deathState.isDead();

    if (!paused && !playerDead) {
      float delta = Gdx.graphics.getDeltaTime();
      for (UpgradeNode node : actionUpgrades) {
        node.tickTime(delta);
      }
      for (UpgradeNode node : defenceUpgrades) {
        node.tickTime(delta);
      }
      for (UpgradeNode node : movementUpgrades) {
        node.tickTime(delta);
      }
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
