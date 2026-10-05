package com.csse3200.game.components.boss;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ProgressBar;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.ui.UIComponent;

/**
 * Shows a boss's name and remaining health in a bar at the top of the screen for as long as the
 * boss is alive. Requires a {@link CombatStatsComponent} on the same entity.
 */
public class BossHealthBarDisplay extends UIComponent {
  private static final float BAR_WIDTH = 420f;

  private final String bossName;
  private Table table;
  private ProgressBar bar;

  /**
   * @param bossName the name shown over the bar
   */
  public BossHealthBarDisplay(String bossName) {
    this.bossName = bossName;
  }

  @Override
  public void create() {
    super.create();
    int health = entity.getComponent(CombatStatsComponent.class).getHealth();

    bar = new ProgressBar(0f, Math.max(1, health), 1f, false, skin);
    bar.setValue(health);
    bar.setAnimateDuration(0.15f);

    table = new Table();
    table.top();
    table.setFillParent(true);
    table.padTop(12f);
    table.add(new Label(bossName, skin)).row();
    table.add(bar).width(BAR_WIDTH);
    stage.addActor(table);

    entity.getEvents().addListener("updateHealth", this::updateHealth);
  }

  private void updateHealth(int health) {
    bar.setValue(health);
    table.setVisible(health > 0);
  }

  @Override
  protected void draw(SpriteBatch batch) {
    // The stage draws the bar.
  }

  @Override
  public void dispose() {
    super.dispose();
    table.remove();
  }
}
