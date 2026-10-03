package com.csse3200.game.components.gamearea;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.csse3200.game.components.player.LadderComponent;
import com.csse3200.game.components.player.SubLevelTravelComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.ui.UIComponent;

/** Displays the Level 1 lift prompt or an ordinary ladder's automatic ascent prompt. */
public class SubLevelTravelPromptDisplay extends UIComponent {
  static final String PROMPT_TEXT = "Press \"E\" To go to the Nether";
  static final String LADDER_PROMPT_TEXT = "Press \"E\" to go up";
  private static final float WORLD_VERTICAL_OFFSET = 0.35f;

  private final Entity player;
  private final Camera worldCamera;
  private Label prompt;

  public SubLevelTravelPromptDisplay(Entity player, Camera worldCamera) {
    this.player = player;
    this.worldCamera = worldCamera;
  }

  @Override
  public void create() {
    super.create();
    Label.LabelStyle style = new Label.LabelStyle(skin.get("subtitle", Label.LabelStyle.class));
    style.fontColor = Color.WHITE;
    prompt = new Label(PROMPT_TEXT, style);
    prompt.setTouchable(Touchable.disabled);
    prompt.setVisible(false);
    prompt.pack();
    stage.addActor(prompt);
    prompt.toBack();
  }

  @Override
  public void update() {
    SubLevelTravelComponent travel = player.getComponent(SubLevelTravelComponent.class);
    LadderComponent ladder = player.getComponent(LadderComponent.class);
    boolean showNether = travel != null && travel.canTravelToNether();
    boolean showLadder = !showNether && ladder != null && ladder.canAutoClimb();
    if (showNether || showLadder) {
      String text = showNether ? PROMPT_TEXT : LADDER_PROMPT_TEXT;
      if (!prompt.getText().toString().equals(text)) {
        prompt.setText(text);
        prompt.pack();
      }
    }
    prompt.setVisible(showNether || showLadder);
  }

  @Override
  public void draw(SpriteBatch batch) {
    if (!prompt.isVisible()) {
      return;
    }

    Vector2 playerCentre = player.getCenterPosition();
    Vector3 screenPosition =
        worldCamera.project(
            new Vector3(
                playerCentre.x,
                player.getPosition().y + player.getScale().y + WORLD_VERTICAL_OFFSET,
                0f));
    Vector2 stagePosition =
        stage
            .getViewport()
            .unproject(new Vector2(screenPosition.x, Gdx.graphics.getHeight() - screenPosition.y));
    prompt.setPosition(stagePosition.x - prompt.getWidth() / 2f, stagePosition.y);
  }

  @Override
  public void dispose() {
    super.dispose();
    prompt.remove();
  }
}
