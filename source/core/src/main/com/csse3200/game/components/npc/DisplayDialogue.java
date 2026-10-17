package com.csse3200.game.components.npc;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.ui.Cell;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.csse3200.game.ui.UIComponent;

/** Displays an NPC's speech in a box anchored to the top centre of the screen. */
public class DisplayDialogue extends UIComponent {

  // Caps the box at 750px on wide screens, but shrinks with the viewport on narrow ones so it
  // never overlaps the bottom-centre inventory hotbar or runs off a small window.
  private static final float MAX_DIALOGUE_WIDTH = 750f;
  private static final float DIALOGUE_WIDTH_VIEWPORT_FRACTION = 0.55f;
  // Must match the left/right args passed to dialogueTable.pad() below - subtracted from the
  // table width so the wrapped label never touches the background's edges.
  private static final float DIALOGUE_SIDE_PADDING = 30f;

  private final String speakerName;

  private Table rootTable;
  private Table dialogueTable;

  private Label speakerLabel;
  private Label dialogueLabel;
  private Label continueLabel;

  private Cell<Table> dialogueCell;
  private Cell<Label> dialogueLabelCell;

  private Texture backgroundTexture;

  /**
   * @param speakerName name shown above the dialogue text, fixed for this NPC instance.
   */
  public DisplayDialogue(String speakerName) {
    this.speakerName = speakerName;
  }

  /** Builds the dialogue UI and wires it up to this NPC's showDialogue/hideDialogue events. */
  @Override
  public void create() {
    super.create();

    createDialogueUI();

    entity.getEvents().addListener("showDialogue", this::showDialogue);
    entity.getEvents().addListener("hideDialogue", this::hideDialogue);
  }

  /**
   * Builds the dialogue box, anchored to the top centre of the screen so it never overlaps the
   * bottom-centre inventory hotbar. The box starts hidden; its width is set by
   * updateDialogueWidth() rather than here, since that must also re-run on every show.
   */
  private void createDialogueUI() {

    rootTable = new Table();
    rootTable.setFillParent(true);
    rootTable.top();

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

    dialogueLabelCell = dialogueTable.add(dialogueLabel).left().padBottom(15f);
    dialogueTable.row();

    dialogueTable.add(continueLabel).right().expandX();

    dialogueTable.pad(25f, DIALOGUE_SIDE_PADDING, 20f, DIALOGUE_SIDE_PADDING);
    dialogueCell = rootTable.add(dialogueTable).padTop(30f);
    rootTable.setVisible(false);
    stage.addActor(rootTable);

    updateDialogueWidth();
  }

  /**
   * Resizes the dialogue box and its wrapped label to fit the current viewport - capped at
   * MAX_DIALOGUE_WIDTH, but never wider than DIALOGUE_WIDTH_VIEWPORT_FRACTION of it - so a window
   * resized between (or during) dialogues still gets a correctly sized box.
   */
  private void updateDialogueWidth() {
    float width =
        Math.min(
            MAX_DIALOGUE_WIDTH,
            stage.getViewport().getWorldWidth() * DIALOGUE_WIDTH_VIEWPORT_FRACTION);

    dialogueCell.width(width);
    dialogueLabelCell.width(width - 2 * DIALOGUE_SIDE_PADDING);
    dialogueTable.invalidateHierarchy();
  }

  /** Shows this NPC's dialogue text, resizing the box first in case the window was resized. */
  private void showDialogue(String text) {
    updateDialogueWidth();
    dialogueLabel.setText(text);
    rootTable.setVisible(true);
  }

  /** Hides the dialogue box without clearing its text, so reshowing it is instant. */
  private void hideDialogue() {
    rootTable.setVisible(false);
  }

  @Override
  protected void draw(SpriteBatch batch) {}

  /** Removes the dialogue box from the stage and frees its background texture. */
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
