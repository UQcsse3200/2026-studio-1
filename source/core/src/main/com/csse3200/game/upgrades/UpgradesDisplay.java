package com.csse3200.game.upgrades;

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
import com.csse3200.game.components.player.StaminaComponent;
import com.csse3200.game.difficulty.DifficultyService;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.pausemenu.PauseMenuComponent;
import com.csse3200.game.services.ServiceLocator;
import com.csse3200.game.ui.UIComponent;
import java.util.ArrayList;
import java.util.List;

/**
 * Displays the Upgrades screen: category tabs (Action / Defence), a row of upgrade nodes per
 * category, and a detail panel for the selected node's name, description, current tier and
 * remaining time/kills.
 *
 * <p>Every upgrade is temporary: Action upgrades expire after a number of kills, Defence upgrades
 * after a duration. Buying an active upgrade again advances its tier and extends the expiry window;
 * once fully expired it resets to Tier 0.
 */
public class UpgradesDisplay extends UIComponent {

  // Tier -> effect magnitude for each upgrade's gameplay effect. Index 0 = Tier 1, etc.
  private static final float[] PLAYER_SPEED_MULTIPLIER_PER_TIER = {1.15f, 1.3f, 1.5f};
  private static final int[] SWORD_DAMAGE_BONUS_PER_TIER = {5, 10, 15};
  private static final float[] ATTACK_SPEED_COOLDOWN_MULTIPLIER_PER_TIER = {0.8f, 0.6f, 0.4f};
  private static final int[] REGEN_HEAL_PER_KILL_PER_TIER = {5, 10};
  private static final float[] ENDURANCE_REGEN_MULTIPLIER_PER_TIER = {1.6f, 2.0f, 2.5f};
  // Fired on the player whenever Sword Damage's bonus changes (including to 0 on expiry), so
  // WeaponDisplay etc. can react without a direct reference to this class.
  private static final String SWORD_DAMAGE_BONUS_EVENT = "swordDamageBonusChanged";
  // Fired on the player whenever any upgrade activates (purchase or tier-up), so
  // UpgradeActivationFlashComponent can flash the player sprite without a direct reference here.
  private static final String UPGRADE_ACTIVATED_EVENT = "upgradeActivated";

  private Table root;
  private Table nodeRow;
  private Label categoryHeaderLabel;
  private Label detailNameLabel;
  private Label detailDescriptionLabel;
  private Label detailStatusLabel;

  private String activeCategory = null; // nothing selected until the player picks a tab
  private UpgradeNode selectedNode;
  private UpgradesMenuComponent upgradesMenu;

  // Sibling component on the shared "ui" entity, same pattern PauseMenuInputComponent uses.
  private PauseMenuComponent pauseMenu;

  // Wired in later via setPlayer() - not available at create() time.
  private Entity player;

  // Sword Damage's baseAttack before any bonus was applied; null means no bonus is active.
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
   * Supplies the player entity once it's spawned (after this UI entity already exists), so effect
   * code below must not assume player is non-null before this runs.
   *
   * <p>If a player was already set and {@code newPlayer} is a different entity (e.g. the player
   * died and was replaced by a brand-new entity on revival), every upgrade is cleared first via
   * {@link #clearAllUpgrades()} - their effects were applied to the old, now-disposed player, so
   * leaving them "active" in the HUD would be stale and nothing would ever remove them from the new
   * player. The very first call at game start must not clear anything, since there is no prior
   * player to have earned anything from; passing the same entity again is also a no-op.
   */
  public void setPlayer(Entity newPlayer) {
    if (this.player == newPlayer) {
      return; // true no-op, as documented above - already listening, nothing to clear
    }
    if (this.player != null) {
      clearAllUpgrades();
    }
    this.player = newPlayer;
    newPlayer.getEvents().addListener("enemyKilled", this::onEnemyKilled);
  }

  /**
   * Forces every upgrade node back to Tier 0 (inactive) without firing their tier-changed/expired
   * callbacks - those callbacks apply effects to {@link #player}, which by the time this is called
   * may already be a different (or disposed) entity than the one the upgrade was earned on, so
   * running them would apply to the wrong player or be meaningless. Also clears
   * swordDamageBaselineAttack, the one piece of state captured from a player outside UpgradeNode
   * itself. Does not refund any gold spent.
   */
  private void clearAllUpgrades() {
    for (UpgradeNode node : getAllUpgrades()) {
      node.reset();
    }
    swordDamageBaselineAttack = null;
  }

  private void onEnemyKilled() {
    if (player == null) {
      return;
    }

    // Counts this kill against every kill-count upgrade. Must run before Regen's early return
    // below, or it would be skipped whenever Regen on Kill isn't active.
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

    int healAmount =
        Math.round(
            REGEN_HEAL_PER_KILL_PER_TIER[regenOnKill.getCurrentTier() - 1]
                * DifficultyService.getCurrent().getRegenHealMultiplier());

    CombatStatsComponent combatStats = player.getComponent(CombatStatsComponent.class);

    if (combatStats == null) {
      return;
    }

    // Nothing enforces that the player has a ConsumableUseComponent, so heal uncapped if it's
    // missing rather than throwing or skipping the heal.
    com.csse3200.game.components.player.ConsumableUseComponent consumableUse =
        player.getComponent(com.csse3200.game.components.player.ConsumableUseComponent.class);

    int newHealth = combatStats.getHealth() + healAmount;
    if (consumableUse != null) {
      newHealth = Math.min(newHealth, consumableUse.getMaxHealth());
    }

    combatStats.setHealth(newHealth);
  }

  /**
   * @return a fresh combined list of every upgrade across all categories, so callers (e.g. {@link
   *     ActiveUpgradesHud}) can't mutate the internal per-category lists.
   */
  public List<UpgradeNode> getAllUpgrades() {
    List<UpgradeNode> all =
        new ArrayList<>(actionUpgrades.size() + defenceUpgrades.size() + movementUpgrades.size());
    all.addAll(actionUpgrades);
    all.addAll(defenceUpgrades);
    all.addAll(movementUpgrades);
    return all;
  }

  /**
   * @return the player's current shield hits remaining, or 0 if the player isn't set or has no
   *     CombatStatsComponent - shield hits live on CombatStatsComponent rather than UpgradeNode, so
   *     {@link ActiveUpgradesHud} needs this to show Shield Durability's remaining hits.
   */
  public int getShieldHitsRemaining() {
    CombatStatsComponent combatStats = getCombatStats();
    return combatStats == null ? 0 : combatStats.getShieldHits();
  }

  private void buildUpgradeData() {
    // Action upgrades: expire after a number of kills while active.
    UpgradeNode swordDamage =
        UpgradeNode.killCountBased(
            "sword_damage",
            "Sword Damage",
            "Increases melee damage. Stacking tiers also raises the kill threshold.",
            new int[] {20, 18, 15},
            new int[] {2, 5, 8});
    swordDamage.setOnTierChanged(() -> applySwordDamageEffect(swordDamage));
    swordDamage.setOnExpired(this::removeSwordDamageEffect);
    actionUpgrades.add(swordDamage);

    UpgradeNode attackSpeed =
        UpgradeNode.killCountBased(
            "attack_speed",
            "Attack Speed",
            "Reduces the delay between attacks. Stacking tiers also raises the kill threshold.",
            new int[] {25, 22, 20},
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
            new int[] {18, 15, 15},
            new float[] {20f, 35f, 55f});

    shieldDurability.setOnTierChanged(() -> applyShieldEffect(shieldDurability));
    shieldDurability.setOnExpired(this::removeShieldEffect);

    defenceUpgrades.add(shieldDurability);

    UpgradeNode regenOnKill =
        UpgradeNode.timeBased(
            "regen_on_kill",
            "Regen on Kill",
            "Heals you when you defeat an enemy. Stacking tiers also extends the duration.",
            new int[] {30, 25},
            new float[] {15f, 30f});

    regenOnKill.setOnTierChanged(() -> applyRegenEffect(regenOnKill));
    regenOnKill.setOnExpired(this::removeRegenEffect);

    defenceUpgrades.add(regenOnKill);

    // Movement upgrades: expire after a fixed duration, same as Defence. Each tier adds its
    // increment on top of whatever time is currently remaining, rather than resetting it.
    UpgradeNode playerSpeed =
        UpgradeNode.timeBased(
            "player_speed",
            "Player Speed+",
            "Increases movement speed. Stacking tiers also adds 10s to the remaining duration.",
            new int[] {15, 12, 10},
            new float[] {10f, 10f, 10f});
    playerSpeed.setOnTierChanged(() -> applyPlayerSpeedEffect(playerSpeed));
    playerSpeed.setOnExpired(() -> removePlayerSpeedEffect(playerSpeed));
    movementUpgrades.add(playerSpeed);

    UpgradeNode endurance =
        UpgradeNode.timeBased(
            "endurance",
            "Endurance",
            "Stamina refills faster. Stacking tiers also adds 10s to the remaining duration.",
            new int[] {15, 12, 10},
            new float[] {10f, 10f, 10f});
    endurance.setOnTierChanged(() -> applyEnduranceEffect(endurance));
    endurance.setOnExpired(this::removeEnduranceEffect);
    movementUpgrades.add(endurance);
  }

  private void applyEnduranceEffect(UpgradeNode node) {
    triggerUpgradeActivatedFlash(node);

    StaminaComponent stamina = getStamina();
    if (stamina == null) {
      return;
    }
    stamina.setRegenMultiplier(ENDURANCE_REGEN_MULTIPLIER_PER_TIER[node.getCurrentTier() - 1]);
  }

  private void removeEnduranceEffect() {
    StaminaComponent stamina = getStamina();
    if (stamina != null) {
      stamina.setRegenMultiplier(1f);
    }
  }

  /**
   * Applies the Player Speed+ effect. The node is used as the modifier key so removeSpeedModifier()
   * only removes this upgrade's contribution.
   */
  private void applyPlayerSpeedEffect(UpgradeNode node) {
    triggerUpgradeActivatedFlash(node);

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
    triggerUpgradeActivatedFlash(node);

    CombatStatsComponent combatStats = getCombatStats();
    if (combatStats == null) {
      return;
    }

    int[] shieldHitsPerTier = {3, 5, 8};
    int shieldHits = shieldHitsPerTier[node.getCurrentTier() - 1];

    combatStats.setShieldHits(shieldHits);
  }

  private void removeShieldEffect() {
    CombatStatsComponent combatStats = getCombatStats();
    if (combatStats != null) {
      combatStats.setShieldHits(0);
    }
  }

  private void applyRegenEffect(UpgradeNode node) {
    // Regen's heal itself fires from onEnemyKilled(), not from here - but the flash still needs
    // to fire on activation, same as every other upgrade.
    triggerUpgradeActivatedFlash(node);
  }

  private void removeRegenEffect() {
    // No-op: Regen has no persistent stat to remove.
  }

  private void applyAttackSpeedEffect(UpgradeNode node) {
    triggerUpgradeActivatedFlash(node);

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
   * Applies the Sword Damage effect, recomputed from the stored baseline plus the current tier's
   * bonus each time - so a later tier replaces the bonus rather than stacking on top of it.
   */
  private void applySwordDamageEffect(UpgradeNode node) {
    triggerUpgradeActivatedFlash(node);

    CombatStatsComponent combatStats = getCombatStats();
    if (combatStats == null) {
      return;
    }
    if (swordDamageBaselineAttack == null) {
      swordDamageBaselineAttack = combatStats.getBaseAttack();
    }
    int bonus = SWORD_DAMAGE_BONUS_PER_TIER[node.getCurrentTier() - 1];
    combatStats.setBaseAttack(swordDamageBaselineAttack + bonus);
    // combatStats is non-null only if player is too, so this is safe.
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

  private StaminaComponent getStamina() {
    return player == null ? null : player.getComponent(StaminaComponent.class);
  }

  private CombatStatsComponent getCombatStats() {
    return player == null ? null : player.getComponent(CombatStatsComponent.class);
  }

  private DeathStateComponent getDeathState() {
    return player == null ? null : player.getComponent(DeathStateComponent.class);
  }

  /**
   * Fires {@code UPGRADE_ACTIVATED_EVENT} on the player so {@code UpgradeActivationFlashComponent}
   * can flash the sprite on activation - every apply*Effect() method above calls this instead of
   * triggering the event itself.
   *
   * <p>Skipped while {@code node} is being restored from a save (see {@link
   * UpgradeNode#isRestoring()}): {@link UpgradeNode#restore(int, float, int)} re-runs the same
   * tier-changed callback so the gameplay effect (stat bonus, shield hits, etc.) is still applied,
   * but a restore isn't a fresh purchase and shouldn't flash the player as if it were one. Also a
   * no-op if {@link #player} hasn't been set yet.
   */
  private void triggerUpgradeActivatedFlash(UpgradeNode node) {
    if (player == null || node.isRestoring()) {
      return;
    }
    player.getEvents().trigger(UPGRADE_ACTIVATED_EVENT);
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
    detailNameLabel.setText(""); // categoryHeaderLabel shows the "Select an upgrade" prompt instead
    detailDescriptionLabel.setText("");
    detailStatusLabel.setText("");
  }

  @Override
  public void draw(SpriteBatch batch) {
    // Upgrades keep ticking even while this screen is closed (visibility is gated separately
    // below), but not while paused or the player is dead - matching MainGameScreen's own freeze
    // of physics/entity updates. No resume logic is needed: tickTime() just picks up again once
    // both conditions clear.
    boolean paused = pauseMenu != null && pauseMenu.isPaused();
    DeathStateComponent deathState = getDeathState();
    boolean playerDead = deathState != null && deathState.isDead();

    if (!paused && !playerDead) {
      // The game clock, not Gdx.graphics.getDeltaTime() directly - this is the same source every
      // other time-scaled system reads, so these timers honour timeScale consistently with the
      // rest of the game instead of only through the paused/playerDead gates above.
      float delta = ServiceLocator.getTimeSource().getDeltaTime();
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
