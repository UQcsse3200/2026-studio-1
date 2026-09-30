package com.csse3200.game.components.player;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Align;
import com.csse3200.game.ui.UIComponent;

/** UI component for displaying the basic player controls. */
public class Tutorial extends UIComponent {

  private Table rootTable;

  @Override
  public void create() {
    super.create();

    addActors();

    entity
        .getEvents()
        .addListener(
            "toggleTutorial",
            () -> {
              rootTable.setVisible(!rootTable.isVisible());
            });
  }

  /** Creates and positions the tutorial box. */
  private void addActors() {
    rootTable = new Table();
    rootTable.setFillParent(true);
    rootTable.align(Align.bottom);
    rootTable.padBottom(20f);

    Table tutorialTable = new Table();
    tutorialTable.setBackground(skin.getDrawable("window-w"));
    tutorialTable.pad(14f);

    Label title = new Label("TUTORIAL", skin, "large");
    title.setAlignment(Align.center);
    tutorialTable.add(title).expandX().fillX().padBottom(10f).row();

    Label movement = new Label("W / A / S / D - Move", skin);
    tutorialTable.add(movement).expandX().fillX().left().row();

    Label jump = new Label("W - Jump", skin);
    tutorialTable.add(jump).expandX().fillX().left().row();

    Label attack = new Label("SPACE - Attack", skin);
    tutorialTable.add(attack).expandX().fillX().left().row();

    Label dash = new Label("L - Dash", skin);
    tutorialTable.add(dash).expandX().fillX().left().row();

    Label shield = new Label("B - Shield", skin);
    tutorialTable.add(shield).expandX().fillX().left().row();

    Label interact = new Label("E - Interact", skin);
    tutorialTable.add(interact).expandX().fillX().left().row();

    Label inventory = new Label("1 - 5 - Inventory", skin);
    tutorialTable.add(inventory).expandX().fillX().left().row();

    Label quest = new Label("J - Quest Menu", skin);
    tutorialTable.add(quest).expandX().fillX().left().row();

    rootTable.add(tutorialTable).width(400f);

    stage.addActor(rootTable);
  }

  @Override
  public void update() {
    // Tutorial display does not require regular updates.
  }

  @Override
  public void draw(SpriteBatch batch) {
    // Drawing is handled by the stage.
  }

  @Override
  public void dispose() {
    super.dispose();

    if (rootTable != null) {
      rootTable.remove();
    }
  }
}
