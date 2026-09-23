package com.csse3200.game.upgrades;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.csse3200.game.ui.UIComponent;
import java.util.HashMap;
import java.util.Map;

/**
 * Small always-on HUD, bottom-right, listing every active upgrade (name, tier, remaining
 * time/kills, plus hits left for Shield Durability) on a dark charcoal panel. Unlike
 * UpgradesDisplay's own root table, this is NOT gated by UpgradesMenuComponent.isOpen() - it stays
 * visible during normal gameplay.
 *
 * <p>Reads the sibling UpgradesDisplay component directly, rather than owning any upgrade state.
 *
 * <p>Layout: root (fill-parent, bottom-right) -> panel (charcoal background, hidden when nothing is
 * active) -> one Label per active upgrade.
 *
 * <p>Rendering-safety: no setColor() on the panel, root or any Label, no alpha/fade or toFront().
 * The panel's tint is baked into its drawable at skin-load time, and label text colour is set on
 * the Label STYLE, not the actor - the same approach ShopDisplay's toast uses - so nothing here can
 * leave a stray tint in the shared SpriteBatch.
 */
public class ActiveUpgradesHud extends UIComponent {
  // Reuses the shop toast's baked charcoal drawable (ShopDisplay.TOAST_BACKGROUND) - tint is
  // fixed at skin-load time, not applied via setColor().
  private static final String PANEL_BACKGROUND = "toast-charcoal";
  private static final float PANEL_PADDING = 8f;
  private static final String LABEL_STYLE = "small";
  private static final String SHIELD_DURABILITY_ID = "shield_durability";

  private Table root;
  private Table panel;
  private UpgradesDisplay upgradesDisplay;

  // Skin's "small" style with only fontColor changed to white - same font/size, but the skin's
  // own black text would be unreadable on the charcoal panel.
  private Label.LabelStyle whiteLabelStyle;

  // One Label reused per node across frames - created lazily, then just re-parented in/out of
  // the panel rather than recreated.
  private final Map<UpgradeNode, Label> labelsByNode = new HashMap<>();

  @Override
  public void create() {
    super.create();
    upgradesDisplay = entity.getComponent(UpgradesDisplay.class);

    whiteLabelStyle = new Label.LabelStyle(skin.get(LABEL_STYLE, Label.LabelStyle.class));
    whiteLabelStyle.fontColor = Color.WHITE;

    root = new Table();
    root.setFillParent(true);
    root.bottom()
        .right()
        .pad(10f); // bottom-right - clear of everything else, which is top-anchored

    panel = new Table();
    panel.setBackground(skin.getDrawable(PANEL_BACKGROUND)); // baked tint - no setColor()
    panel.pad(PANEL_PADDING);
    panel.setVisible(false); // shown only while at least one upgrade is active - see draw()
    root.add(panel);

    stage.addActor(root);
  }

  @Override
  public void draw(SpriteBatch batch) {
    panel.clearChildren(); // cheap - re-parents existing Label actors, doesn't recreate them

    boolean anyActive = false;

    if (upgradesDisplay != null) {
      for (UpgradeNode node : upgradesDisplay.getAllUpgrades()) {
        if (!node.isActive()) {
          continue;
        }

        Label label = labelsByNode.get(node);
        if (label == null) {
          label = new Label("", whiteLabelStyle);
          labelsByNode.put(node, label);
        }
        label.setText(describe(node));
        panel.add(label).right().row();
        anyActive = true;
      }
    }

    panel.setVisible(anyActive); // no empty charcoal box when nothing is active
  }

  private String describe(UpgradeNode node) {
    String text =
        node.getName() + " - Tier " + node.getCurrentTier() + " - " + node.getRemainingText();

    if (node.getId().equals(SHIELD_DURABILITY_ID)) {
      // Shield hits live on CombatStatsComponent, not UpgradeNode, so getRemainingText() (the
      // shield's time-based duration) doesn't include them - append separately.
      text += " - " + upgradesDisplay.getShieldHitsRemaining() + " hits left";
    }

    return text;
  }

  @Override
  public void dispose() {
    root.clear();
    super.dispose();
  }
}
