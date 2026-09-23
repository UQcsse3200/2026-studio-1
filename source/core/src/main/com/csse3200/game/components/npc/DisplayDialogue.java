package com.csse3200.game.components.npc;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.csse3200.game.ui.UIComponent;

public class DisplayDialogue extends UIComponent {

  private final String speakerName;

  private Table rootTable;
  private Table dialogueTable;

  private Label speakerLabel;
  private Label dialogueLabel;
  private Label continueLabel;

  private Texture backgroundTexture;

  public DisplayDialogue(String speakerName) {
    this.speakerName = speakerName;
  }

  @Override
  public void create() {
    super.create();

    createDialogueUI();

    entity.getEvents().addListener("showDialogue", this::showDialogue);
    entity.getEvents().addListener("hideDialogue", this::hideDialogue);
  }

  private void createDialogueUI() {

    rootTable = new Table();
    rootTable.setFillParent(true);
    rootTable.bottom();

    dialogueTable = new Table();

    // black background
    Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
    pixmap.setColor(0f, 0f, 0f, 0.80f);
    pixmap.fill();

    backgroundTexture = new Texture(pixmap);
    TextureRegionDrawable background =
        new TextureRegionDrawable(new TextureRegion(backgroundTexture));
    dialogueTable.setBackground(background);

    Label.LabelStyle textColor = new Label.LabelStyle(skin.get(Label.LabelStyle.class));

    textColor.fontColor = Color.WHITE; // make the words in dialogue box clear by white words

    speakerLabel = new Label(speakerName, textColor);
    dialogueLabel = new Label("", textColor);
    continueLabel = new Label("[E] Next", textColor);

    dialogueLabel.setWrap(true);

    speakerLabel.setAlignment(Align.left);
    dialogueLabel.setAlignment(Align.left);
    continueLabel.setAlignment(Align.right);

    dialogueTable.add(speakerLabel).left().expandX().padBottom(10f);
    dialogueTable.row();

    dialogueTable.add(dialogueLabel).width(650f).left().padBottom(15f);
    dialogueTable.row();

    dialogueTable.add(continueLabel).right().expandX();

    dialogueTable.pad(25f, 30f, 20f, 30f);
    rootTable.add(dialogueTable).width(750f).padBottom(40f);
    rootTable.setVisible(false);
    stage.addActor(rootTable);
  }

  private void showDialogue(String text) {
    dialogueLabel.setText(text);
    rootTable.setVisible(true);
  }

  private void hideDialogue() {
    rootTable.setVisible(false);
  }

  @Override
  protected void draw(SpriteBatch batch) {}

  @Override
  public void dispose() {
    if (rootTable != null) {
      rootTable.remove();
    }
    if (backgroundTexture != null) {
      backgroundTexture.dispose();
    }
    super.dispose();
  }
}
