package com.csse3200.game.pausemenu;

import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Event;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Slider;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.csse3200.game.services.ServiceLocator;
import com.csse3200.game.ui.UIComponent;

public class PauseMenuDisplay extends UIComponent {

  private static float Music_vol = 0.5f;
  private static final float MUSIC_STEP = 0.05f;
  private Table table;
  private Table pauseOverlay;
  private Table pausePanel;
  private PauseMenuComponent pauseMenu;
  private TextButton[] buttons;
  private Slider musicSlider;
  private Label musicLabel;
  private int selectedIndex = 0;
  private boolean wasPaused = false;

  private boolean usingKeyboardNav = true;

  @Override
  public void create() {
    super.create();
    pauseMenu = entity.getComponent(PauseMenuComponent.class);
    addActors();
    registerEventListeners();
  }

  private void addActors() {
    table = new Table();
    table.setFillParent(true);
    pauseOverlay = new Table();
    pauseOverlay.setFillParent(true);
    pauseOverlay.setBackground(skin.newDrawable("white", new Color(0, 0, 0, 0.5f)));
    pauseOverlay.setVisible(false);

    stage.addActor(pauseOverlay);

    pausePanel = new Table();
    pausePanel.setBackground(skin.newDrawable("white", new Color(0.05f, 0.08f, 0.05f, 0.85f)));
    pausePanel.pad(35f);

    Label title = new Label("PAUSED", skin);
    title.getStyle().fontColor = Color.WHITE;

    TextButton resumeBtn = new TextButton("Resume", skin);
    TextButton restartBtn = new TextButton("Restart", skin);
    TextButton mainMenuBtn = new TextButton("Main Menu", skin);

    buttons = new TextButton[] {resumeBtn, restartBtn, mainMenuBtn};

    resumeBtn.addListener(
        new ChangeListener() {
          @Override
          public void changed(ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) {
            entity.getEvents().trigger("resumeClicked");
          }
        });
    restartBtn.addListener(
        new ChangeListener() {
          @Override
          public void changed(ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) {
            entity.getEvents().trigger("restartClicked");
          }
        });
    mainMenuBtn.addListener(
        new ChangeListener() {
          @Override
          public void changed(ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) {
            entity.getEvents().trigger("mainMenuClicked");
          }
        });

    pausePanel.add(title);
    pausePanel.row();

    pausePanel.add(resumeBtn).padTop(20f);
    pausePanel.row();

    pausePanel.add(restartBtn).padTop(15f);
    pausePanel.row();

    pausePanel.add(mainMenuBtn).padTop(15f);
    pausePanel.row();

    pausePanel.add(MusicSlider()).padTop(25f);

    table.add(pausePanel).width(350f).height(350f);

    table.addListener(
        new InputListener() {
          @Override
          public boolean mouseMoved(InputEvent event, float x, float y) {
            usingKeyboardNav = false;
            for (TextButton button : buttons) {
              button.setColor(Color.WHITE);
            }
            musicLabel.setColor(Color.WHITE);
            return false;
          }
        });

    for (TextButton button : buttons) {
      button.addListener(
          new InputListener() {
            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
              if (!usingKeyboardNav) {
                button.setColor(Color.YELLOW);
              }
            }

            @Override
            public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
              button.setColor(Color.WHITE);
            }
          });
    }

    table.setVisible(false);
    stage.addActor(table);
  }

  private Table MusicSlider() {
    musicLabel = new Label("Music Volume", skin);
    musicSlider = new Slider(0f, 1f, 0.01f, false, skin);
    musicSlider.setValue(Music_vol);
    Label musicValueLabel = new Label(String.format("%.2f", Music_vol), skin);
    musicSlider.addListener(
        (Event event) -> {
          Music_vol = musicSlider.getValue();
          musicValueLabel.setText(String.format("%.0f%%", Music_vol * 100));
          Music music =
              ServiceLocator.getResourceService()
                  .getAsset(PauseMenuComponent.BACKGROUND_MUSIC, Music.class);
          if (music != null) {
            music.setVolume(Music_vol);
          }
          return true;
        });
    Table row = new Table();
    row.add(musicLabel).padRight(10f);
    row.add(musicSlider).width(200f);
    row.add(musicValueLabel).padLeft(10f);
    return row;
  }

  private void registerEventListeners() {
    entity.getEvents().addListener("navigateUp", this::navigateUp);
    entity.getEvents().addListener("navigateDown", this::navigateDown);
    entity.getEvents().addListener("navigateLeft", this::navigateLeft);
    entity.getEvents().addListener("navigateRight", this::navigateRight);
    entity.getEvents().addListener("confirmSelection", this::confirmSelection);
  }

  private void navigateUp() {
    usingKeyboardNav = true;
    int itemCount = buttons.length + 1;
    selectedIndex = (selectedIndex - 1 + itemCount) % itemCount;
    updateHighlight();
  }

  private void navigateDown() {
    usingKeyboardNav = true;
    int itemCount = buttons.length + 1;
    selectedIndex = (selectedIndex + 1) % itemCount;
    updateHighlight();
  }

  /** Left/Right only affect the music slider, and only while it's the selected item. */
  private void navigateLeft() {
    if (selectedIndex != buttons.length) {
      return;
    }
    float newValue = Math.max(0f, musicSlider.getValue() - MUSIC_STEP);
    musicSlider.setValue(newValue);
  }

  private void navigateRight() {
    if (selectedIndex != buttons.length) {
      return;
    }
    float newValue = Math.min(1f, musicSlider.getValue() + MUSIC_STEP);
    musicSlider.setValue(newValue);
  }

  private void updateHighlight() {
    if (!usingKeyboardNav) {
      return;
    }
    for (int i = 0; i < buttons.length; i++) {
      buttons[i].setColor(i == selectedIndex ? Color.YELLOW : Color.WHITE);
    }
    musicLabel.setColor(selectedIndex == buttons.length ? Color.YELLOW : Color.WHITE);
  }

  private void confirmSelection() {
    usingKeyboardNav = true;
    updateHighlight();
    switch (selectedIndex) {
      case 0 -> entity.getEvents().trigger("resumeClicked");
      case 1 -> entity.getEvents().trigger("restartClicked");
      case 2 -> entity.getEvents().trigger("mainMenuClicked");
      default -> {}
    }
  }

  @Override
  public void draw(SpriteBatch batch) {
    boolean isPaused = pauseMenu.isPaused();
    pauseOverlay.setVisible(isPaused);
    table.setVisible(isPaused);

    if (isPaused && !wasPaused) {
      selectedIndex = 0;
      usingKeyboardNav = true;
      updateHighlight();
    }
    wasPaused = isPaused;
  }

  @Override
  public void dispose() {
    table.clear();
    super.dispose();
  }
}
