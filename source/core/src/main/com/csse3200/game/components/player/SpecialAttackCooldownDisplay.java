package com.csse3200.game.components.player;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.csse3200.game.ui.UIComponent;

/** Displays the remaining cooldowns for the player's special attacks. */
public class SpecialAttackCooldownDisplay extends UIComponent {
  private static final Color READY_COLOR = new Color(0.55f, 1f, 0.55f, 1f);
  private static final Color COOLDOWN_COLOR = new Color(1f, 0.82f, 0.45f, 1f);

  private Table table;
  private Label specialAttackLabel;
  private Label areaAttackLabel;
  private PlayerActions playerActions;

  @Override
  public void create() {
    super.create();
    playerActions = entity.getComponent(PlayerActions.class);
    if (playerActions == null) {
      throw new IllegalStateException("Special attack cooldown display requires PlayerActions.");
    }

    table = new Table();
    table.top().left();
    table.setFillParent(true);
    table.padTop(275f).padLeft(8f);

    specialAttackLabel = new Label("", skin, "default");
    areaAttackLabel = new Label("", skin, "default");
    table.add(specialAttackLabel).left().row();
    table.add(areaAttackLabel).left();
    stage.addActor(table);

    updateLabels();
  }

  @Override
  public void update() {
    updateLabels();
  }

  private void updateLabels() {
    setCooldownLabel(
        specialAttackLabel, "F Special", playerActions.getSpecialAttackCooldownRemaining());
    setCooldownLabel(areaAttackLabel, "G AoE", playerActions.getAreaAttackCooldownRemaining());
  }

  private void setCooldownLabel(Label label, String ability, float remaining) {
    if (remaining <= 0f) {
      label.setText(ability + ": READY");
      label.setColor(READY_COLOR);
      return;
    }

    float displayedSeconds = (float) Math.ceil(remaining * 10f) / 10f;
    label.setText(String.format("%s: %.1fs", ability, displayedSeconds));
    label.setColor(COOLDOWN_COLOR);
  }

  @Override
  public void draw(SpriteBatch batch) {}

  @Override
  public void dispose() {
    super.dispose();
    table.remove();
  }
}
