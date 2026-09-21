package com.csse3200.game.upgrades;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.csse3200.game.ui.UIComponent;
import java.util.HashMap;
import java.util.Map;

/**
 * Small always-on HUD, in the bottom-right corner, listing every currently-active upgrade (name,
 * tier, remaining time/kills) on a dark charcoal panel. Unlike UpgradesDisplay's own root table,
 * this is NOT gated by UpgradesMenuComponent.isOpen() - it stays visible during normal gameplay so
 * the player can see what's currently buffed without opening the Upgrades screen.
 *
 * <p>Reads the sibling UpgradesDisplay component's upgrade lists directly (same entity, same
 * pattern PauseMenuInputComponent uses to reach PauseMenuComponent) rather than owning any upgrade
 * state itself.
 *
 * <p>Layout is root (fill-parent, anchored bottom-right) -> panel (charcoal background, hidden
 * whenever nothing is active so no empty box shows) -> one Label per active upgrade.
 *
 * <p>Rendering-safety: nothing here ever calls setColor() on the panel, root or any Label, and
 * there is no alpha/fade or toFront(). The panel's colour comes from a drawable whose tint is baked
 * in at skin-load time, and the label text colour is set on the Label STYLE (fontColor), not on the
 * actor - the same approach ShopDisplay's toast uses, so nothing can leave a stray tint in the
 * shared SpriteBatch.
 */
public class ActiveUpgradesHud extends UIComponent {
  // Reuses the shop purchase toast's baked charcoal drawable (ShopDisplay.TOAST_BACKGROUND): a
  // Skin$TintedDrawable defined in flat-earth-ui.json, so the colour is fixed at skin-load time
  // rather than applied at runtime via setColor().
  private static final String PANEL_BACKGROUND = "toast-charcoal";
  private static final float PANEL_PADDING = 8f;
  private static final String LABEL_STYLE = "small";

  private Table root;
  private Table panel;
  private UpgradesDisplay upgradesDisplay;

  // The skin's "small" style with ONLY fontColor changed to white (same font, so text size is
  // unchanged) - the skin's own style is black, which would be unreadable on the charcoal panel.
  // Same pattern as ShopDisplay's whiteLabelStyle.
  private Label.LabelStyle whiteLabelStyle;

  // One Label reused per node across frames (rather than allocating new Labels every frame) -
  // created lazily the first time a node is seen active, and left in this map (just re-parented
  // into/out of the panel) for the rest of the upgrade's lifetime.
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
    return node.getName() + " - Tier " + node.getCurrentTier() + " - " + node.getRemainingText();
  }

  @Override
  public void dispose() {
    root.clear();
    super.dispose();
  }
}
