package com.csse3200.game.components.npc;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.ui.UIComponent;

/** Displays an NPC's generated name above the NPC in the game world. */
public class NameDisplay extends UIComponent {
  private static final float WORLD_VERTICAL_OFFSET = 0.35f;

  private final Entity npc;
  private final com.badlogic.gdx.graphics.Camera worldCamera;
  private Label nameLabel;

  public NameDisplay(Entity npc, com.badlogic.gdx.graphics.Camera worldCamera) {
    this.npc = npc;
    this.worldCamera = worldCamera;
  }

  @Override
  public void create() {
    super.create();

    Label.LabelStyle style = new Label.LabelStyle(skin.get("small", Label.LabelStyle.class));
    style.fontColor = Color.WHITE;

    nameLabel = new Label(npc.getComponent(NameComponent.class).getName(), style);
    nameLabel.setTouchable(Touchable.disabled);
    nameLabel.pack();

    stage.addActor(nameLabel);
  }

  @Override
  public void draw(SpriteBatch batch) {
    Vector2 npcCentre = npc.getCenterPosition();

    Vector3 screenPosition =
        worldCamera.project(
            new Vector3(
                npcCentre.x, npc.getPosition().y + npc.getScale().y + WORLD_VERTICAL_OFFSET, 0f));

    Vector2 stagePosition =
        stage
            .getViewport()
            .unproject(new Vector2(screenPosition.x, Gdx.graphics.getHeight() - screenPosition.y));

    nameLabel.setPosition(stagePosition.x - nameLabel.getWidth() / 2f, stagePosition.y);
  }

  @Override
  public void dispose() {
    if (nameLabel != null) {
      nameLabel.remove();
    }
    super.dispose();
  }
}
