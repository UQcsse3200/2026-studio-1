package com.csse3200.game.components.story;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.csse3200.game.ui.UIComponent;

/**
 * Displays the current story cutscene scene.
 *
 * <p>The display presents a dark story panel over the game screen and allows the player to move
 * through the story using Space or Enter.
 */
public class StoryCutsceneDisplay extends UIComponent {

  private static final float PANEL_WIDTH = 900f;
  private static final float PANEL_HEIGHT = 300f;
  private static final float PANEL_PADDING = 30f;

  private final StoryCutscene cutscene;

  private Table rootTable;
  private Image background;
  private Table panel;
  private Label titleLabel;
  private Label textLabel;
  private Label continueLabel;

  /**
   * Creates a story cutscene display.
   *
   * @param cutscene cutscene whose current scene should be displayed
   */
  public StoryCutsceneDisplay(StoryCutscene cutscene) {
    this.cutscene = cutscene;
  }

  @Override
  public void create() {
    super.create();

    entity.getEvents().addListener("advanceStory", this::handleAdvance);

    createRootTable();
    createStoryPanel();

    updateScene();

    rootTable.setVisible(true);
  }

  /** Creates the root actor used for the story overlay. */
  private void createRootTable() {
    rootTable = new Table();
    rootTable.setFillParent(true);
    rootTable.setTouchable(Touchable.disabled);

    stage.addActor(rootTable);
  }

  /** Creates the visible story panel. */
  private void createStoryPanel() {
    background = new Image(skin.newDrawable("white", new Color(0f, 0f, 0f, 0.75f)));

    background.setTouchable(Touchable.disabled);

    rootTable.addActor(background);

    panel = new Table();
    panel.setBackground(skin.newDrawable("white", new Color(0f, 0f, 0f, 0.95f)));
    panel.pad(PANEL_PADDING);

    titleLabel = new Label("", skin, "large");
    titleLabel.setColor(Color.GOLD);

    textLabel = new Label("", skin, "small");
    textLabel.setWrap(true);

    continueLabel = new Label("PRESS SPACE OR ENTER TO CONTINUE", skin, "small");
    continueLabel.setColor(Color.LIGHT_GRAY);

    panel.add(titleLabel).left().growX();

    panel.row();

    panel.add(textLabel).left().growX().padTop(20f);

    panel.row();

    panel.add(continueLabel).right().growX().padTop(25f);

    rootTable.add(panel).width(PANEL_WIDTH).height(PANEL_HEIGHT).center();
  }

  /** Handles the event triggered when Space or Enter is pressed. */
  private void handleAdvance() {
    boolean finished = advance();

    if (finished) {
      rootTable.setVisible(false);
      entity.getEvents().trigger("storyFinished");
    }
  }

  /** Updates the displayed story text. */
  public void updateScene() {
    StoryScene scene = cutscene.getCurrentScene();

    titleLabel.setText(scene.getTitle());
    textLabel.setText(scene.getText());

    titleLabel.pack();
    textLabel.pack();
    continueLabel.pack();
  }

  /**
   * Advances the story to the next scene.
   *
   * @return true when the final scene has been reached
   */
  public boolean advance() {
    boolean finished = cutscene.advance();

    if (!finished) {
      updateScene();
    }

    return finished;
  }

  @Override
  public void update() {
    // Scene2D handles the story display.
  }

  @Override
  public void draw(SpriteBatch batch) {
    /*
     * Position the background to cover the complete screen.
     * The story panel itself is positioned by the root table.
     */
    if (background != null) {
      float width = stage.getViewport().getWorldWidth();
      float height = stage.getViewport().getWorldHeight();

      background.setSize(width, height);
      background.setPosition(0f, 0f);
    }
  }

  @Override
  public void dispose() {
    if (rootTable != null) {
      rootTable.remove();
      rootTable = null;
    }

    super.dispose();
  }
}
