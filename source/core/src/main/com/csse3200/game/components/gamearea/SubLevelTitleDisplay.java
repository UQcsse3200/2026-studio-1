package com.csse3200.game.components.gamearea;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.ui.UIComponent;

/** Brief themed title shown when the player enters a dungeon or Nether sub-level. */
public class SubLevelTitleDisplay extends UIComponent {
  private static final float DISPLAY_SECONDS = 4f;
  private final Entity player;
  private Label title;
  private float remaining = DISPLAY_SECONDS;

  public SubLevelTitleDisplay(Entity player) {
    this.player = player;
  }

  @Override
  public void create() {
    super.create();
    Label.LabelStyle style = new Label.LabelStyle(skin.get("large", Label.LabelStyle.class));
    style.fontColor = Color.GOLD;
    title = new Label("DUNGEON", style);
    title.setFontScale(1.5f);
    title.pack();
    stage.addActor(title);
    player.getEvents().addListener("subLevelEntered", this::showTitle);
  }

  @Override
  public void update() {
    remaining = Math.max(0f, remaining - Gdx.graphics.getDeltaTime());
    title.setVisible(remaining > 0f);
  }

  @Override
  public void draw(SpriteBatch batch) {
    title.setPosition(
        (Gdx.graphics.getWidth() - title.getWidth()) / 2f, Gdx.graphics.getHeight() - 100f);
  }

  @Override
  public void dispose() {
    super.dispose();
    title.remove();
  }

  private void showTitle(String name) {
    title.setText(name);
    title.pack();
    remaining = DISPLAY_SECONDS;
  }
}
