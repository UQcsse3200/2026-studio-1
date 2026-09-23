package com.csse3200.game.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.csse3200.game.GdxGame;
import com.csse3200.game.components.story.StoryCutscene;
import com.csse3200.game.components.story.StoryScene;

/**
 * Full-screen cinematic story cutscene.
 *
 * <p>Displays a background image for each story scene with a dialogue panel containing the scene
 * title, story text and a continue prompt.
 */
public class StoryCutsceneScreen extends ScreenAdapter {

  private static final float DIALOGUE_WIDTH = 1000f;
  private static final float DIALOGUE_HEIGHT = 270f;

  private static final float DIALOGUE_PADDING = 30f;

  /** Time between each revealed word. */
  private static final float WORD_REVEAL_TIME = 0.10f;

  private final GdxGame game;
  private final StoryCutscene cutscene;

  private Stage stage;

  private Image backgroundImage;
  private Image darkOverlay;

  private Table dialoguePanel;
  private Label titleLabel;
  private Label textLabel;
  private Label continueLabel;

  private Texture currentTexture;
  private Texture panelTexture;

  private InputAdapter input;

  private String[] words = new String[0];
  private int revealedWords = 0;
  private float revealTimer = 0f;
  private boolean textFullyRevealed = false;

  /**
   * Creates the story cutscene screen.
   *
   * @param game game instance used to transition to the main game
   * @param cutscene story cutscene to display
   */
  public StoryCutsceneScreen(GdxGame game, StoryCutscene cutscene) {
    this.game = game;
    this.cutscene = cutscene;
  }

  @Override
  public void show() {
    stage = new Stage(new ScreenViewport());

    createBackground();
    createDarkOverlay();
    createDialoguePanel();
    createInput();

    updateScene();

    Gdx.input.setInputProcessor(input);
  }

  /** Creates the full-screen background image. */
  private void createBackground() {
    backgroundImage = new Image();
    backgroundImage.setFillParent(true);
    backgroundImage.setScaling(Scaling.fill);

    stage.addActor(backgroundImage);
  }

  /** Adds a subtle dark overlay so the dialogue remains readable. */
  private void createDarkOverlay() {
    darkOverlay = new Image();
    darkOverlay.setFillParent(true);
    darkOverlay.setColor(new Color(0f, 0f, 0f, 0.18f));

    stage.addActor(darkOverlay);
  }

  /** Creates the cinematic dialogue panel. */
  private void createDialoguePanel() {
    dialoguePanel = new Table();

    panelTexture = createPanelTexture();

    dialoguePanel.setBackground(new TextureRegionDrawable(panelTexture));

    dialoguePanel.pad(DIALOGUE_PADDING);

    titleLabel = new Label("", createTitleStyle());

    textLabel = new Label("", createTextStyle());
    textLabel.setWrap(true);
    textLabel.setAlignment(1);

    continueLabel = new Label("PRESS SPACE OR ENTER TO CONTINUE", createPromptStyle());

    dialoguePanel.add(titleLabel).center().growX();

    dialoguePanel.row();

    dialoguePanel.add(textLabel).center().growX().padTop(18f);

    dialoguePanel.row();

    dialoguePanel.add(continueLabel).right().growX().padTop(25f);

    dialoguePanel.setSize(DIALOGUE_WIDTH, DIALOGUE_HEIGHT);

    positionDialoguePanel();

    stage.addActor(dialoguePanel);
  }

  /** Creates the large gold title font. */
  private Label.LabelStyle createTitleStyle() {
    BitmapFont font = new BitmapFont();

    font.getData().setScale(1.8f);

    Label.LabelStyle style = new Label.LabelStyle();
    style.font = font;
    style.fontColor = Color.GOLD;

    return style;
  }

  /** Creates the large white story font. */
  private Label.LabelStyle createTextStyle() {
    BitmapFont font = new BitmapFont();

    font.getData().setScale(1.35f);

    Label.LabelStyle style = new Label.LabelStyle();
    style.font = font;
    style.fontColor = Color.WHITE;

    return style;
  }

  /** Creates the smaller continue prompt font. */
  private Label.LabelStyle createPromptStyle() {
    BitmapFont font = new BitmapFont();

    font.getData().setScale(0.75f);

    Label.LabelStyle style = new Label.LabelStyle();
    style.font = font;
    style.fontColor = Color.LIGHT_GRAY;

    return style;
  }

  /** Creates a dark transparent dialogue panel texture. */
  private Texture createPanelTexture() {
    com.badlogic.gdx.graphics.Pixmap pixmap =
        new com.badlogic.gdx.graphics.Pixmap(
            2, 2, com.badlogic.gdx.graphics.Pixmap.Format.RGBA8888);

    pixmap.setColor(new Color(0f, 0f, 0f, 0.84f));
    pixmap.fill();

    Texture texture = new Texture(pixmap);
    pixmap.dispose();

    return texture;
  }

  /** Updates the screen to display the current story scene. */
  private void updateScene() {
    StoryScene scene = cutscene.getCurrentScene();

    titleLabel.setText(scene.getTitle());

    words = scene.getText().trim().split("\\s+");

    revealedWords = 0;
    revealTimer = 0f;
    textFullyRevealed = false;

    textLabel.setText("");

    continueLabel.setVisible(false);

    loadBackground(scene.getImagePath());

    positionDialoguePanel();
  }

  /**
   * Loads the background image associated with the current scene.
   *
   * @param imagePath asset path of the image
   */
  private void loadBackground(String imagePath) {
    if (currentTexture != null) {
      currentTexture.dispose();
      currentTexture = null;
    }

    if (imagePath == null || imagePath.isBlank()) {
      backgroundImage.setDrawable(null);
      return;
    }

    if (!Gdx.files.internal(imagePath).exists()) {
      Gdx.app.error("StoryCutsceneScreen", "Story image not found: " + imagePath);
      return;
    }

    currentTexture = new Texture(Gdx.files.internal(imagePath));

    backgroundImage.setDrawable(new TextureRegionDrawable(currentTexture));
  }

  /** Creates keyboard input for advancing the cutscene. */
  private void createInput() {
    input =
        new InputAdapter() {

          @Override
          public boolean keyDown(int keycode) {

            if (keycode == Input.Keys.SPACE || keycode == Input.Keys.ENTER) {

              advanceRequested();

              return true;
            }

            return false;
          }
        };
  }

  /**
   * Handles Space or Enter input.
   *
   * <p>If the current sentence is still being revealed, the first press reveals the complete
   * sentence. A second press advances the scene.
   */
  private void advanceRequested() {

    if (!textFullyRevealed) {
      revealAllText();
      return;
    }

    boolean finished = cutscene.advance();

    if (finished) {
      game.setScreen(GdxGame.ScreenType.MAIN_GAME);
      return;
    }

    updateScene();
  }

  /** Reveals all remaining words immediately. */
  private void revealAllText() {
    revealedWords = words.length;
    textFullyRevealed = true;

    textLabel.setText(String.join(" ", words));

    continueLabel.setVisible(true);
  }

  /** Updates the word-by-word text reveal. */
  private void updateTextReveal(float delta) {

    if (textFullyRevealed || words.length == 0) {
      return;
    }

    revealTimer += delta;

    while (revealTimer >= WORD_REVEAL_TIME && revealedWords < words.length) {

      revealTimer -= WORD_REVEAL_TIME;
      revealedWords++;

      StringBuilder visibleText = new StringBuilder();

      for (int i = 0; i < revealedWords; i++) {

        if (i > 0) {
          visibleText.append(" ");
        }

        visibleText.append(words[i]);
      }

      textLabel.setText(visibleText.toString());
    }

    if (revealedWords >= words.length) {
      textFullyRevealed = true;
      continueLabel.setVisible(true);
    }
  }

  /** Keeps the dialogue panel centred near the bottom of the screen. */
  private void positionDialoguePanel() {
    if (dialoguePanel == null) {
      return;
    }

    dialoguePanel.setPosition((Gdx.graphics.getWidth() - DIALOGUE_WIDTH) / 2f, 25f);
  }

  @Override
  public void render(float delta) {

    Gdx.gl.glClearColor(0f, 0f, 0f, 1f);

    Gdx.gl.glClear(com.badlogic.gdx.graphics.GL20.GL_COLOR_BUFFER_BIT);

    updateTextReveal(delta);

    stage.act(delta);
    stage.draw();
  }

  @Override
  public void resize(int width, int height) {

    if (stage != null) {

      stage.getViewport().update(width, height, true);

      positionDialoguePanel();
    }
  }

  @Override
  public void hide() {
    // MainGameScreen installs its own input processor when it becomes active.
  }

  @Override
  public void dispose() {

    Gdx.input.setInputProcessor(null);

    if (currentTexture != null) {
      currentTexture.dispose();
      currentTexture = null;
    }

    if (panelTexture != null) {
      panelTexture.dispose();
      panelTexture = null;
    }

    if (stage != null) {
      stage.dispose();
      stage = null;
    }
  }
}
