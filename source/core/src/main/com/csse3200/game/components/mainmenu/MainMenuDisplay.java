package com.csse3200.game.components.mainmenu;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.csse3200.game.services.ServiceLocator;
import com.csse3200.game.ui.UIComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** A ui component for displaying the Main menu. */
public class MainMenuDisplay extends UIComponent {
  private static final Logger logger = LoggerFactory.getLogger(MainMenuDisplay.class);
  private static final float Z_INDEX = 2f;
  private Table table;
  private TextButton[] buttons;
  private int selectedIndex = 0;

  // Whether keyboard nav currently owns button highlighting. True = keyboard nav controls
  // colors; false = mouse hover controls colors. Only one of the two is ever allowed to set
  // a button's color at a time.
  private boolean usingKeyboardNav = true;

  @Override
  public void create() {
    super.create();
    addActors();
    registerEventListeners();
  }

  private void addActors() {
    table = new Table();
    table.setFillParent(true);
    Image title =
        new Image(
            ServiceLocator.getResourceService()
                .getAsset("images/box_boy_title.png", Texture.class));

    TextButton startBtn = new TextButton("Start", skin);
    TextButton loadBtn = new TextButton("Load", skin);
    TextButton settingsBtn = new TextButton("Settings", skin);
    TextButton exitBtn = new TextButton("Exit", skin);

    buttons = new TextButton[] {startBtn, loadBtn, settingsBtn, exitBtn};

    // Triggers an event when the button is pressed
    startBtn.addListener(
        new ChangeListener() {
          @Override
          public void changed(ChangeEvent changeEvent, Actor actor) {
            logger.debug("Start button clicked");
            entity.getEvents().trigger("start");
          }
        });

    loadBtn.addListener(
        new ChangeListener() {
          @Override
          public void changed(ChangeEvent changeEvent, Actor actor) {
            logger.debug("Load button clicked");
            entity.getEvents().trigger("load");
          }
        });

    settingsBtn.addListener(
        new ChangeListener() {
          @Override
          public void changed(ChangeEvent changeEvent, Actor actor) {
            logger.debug("Settings button clicked");
            entity.getEvents().trigger("settings");
          }
        });

    exitBtn.addListener(
        new ChangeListener() {
          @Override
          public void changed(ChangeEvent changeEvent, Actor actor) {

            logger.debug("Exit button clicked");
            entity.getEvents().trigger("exit");
          }
        });

    table.add(title);
    table.row();
    table.add(startBtn).padTop(30f);
    table.row();
    table.add(loadBtn).padTop(15f);
    table.row();
    table.add(settingsBtn).padTop(15f);
    table.row();
    table.add(exitBtn).padTop(15f);

    // Moving the mouse at all hands color control over to hover: drop keyboard-nav
    // ownership and clear every button back to the default color so no stale
    // keyboard highlight is left behind.
    table.addListener(
        new InputListener() {
          @Override
          public boolean mouseMoved(InputEvent event, float x, float y) {
            usingKeyboardNav = false;
            for (TextButton button : buttons) {
              button.setColor(Color.WHITE);
            }
            return false;
          }
        });

    // Hover highlighting per-button - only takes effect once the mouse owns color control.
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

    stage.addActor(table);
    updateHighlight();
  }

  /** Listens for the keyboard-navigation events fired by MainMenuInputComponent. */
  private void registerEventListeners() {
    entity.getEvents().addListener("navigateUp", this::navigateUp);
    entity.getEvents().addListener("navigateDown", this::navigateDown);
    entity.getEvents().addListener("confirmSelection", this::confirmSelection);
  }

  private void navigateUp() {
    usingKeyboardNav = true;
    selectedIndex = (selectedIndex - 1 + buttons.length) % buttons.length;
    updateHighlight();
  }

  private void navigateDown() {
    usingKeyboardNav = true;
    selectedIndex = (selectedIndex + 1) % buttons.length;
    updateHighlight();
  }

  /**
   * Highlights whichever button is currently selected via keyboard navigation. Only applies color
   * when keyboard nav owns highlighting, so it can never fight with the hover listeners.
   */
  private void updateHighlight() {
    if (!usingKeyboardNav) {
      return;
    }
    for (int i = 0; i < buttons.length; i++) {
      buttons[i].setColor(i == selectedIndex ? Color.YELLOW : Color.WHITE);
    }
  }

  /** Enter/Space was pressed - trigger whatever the currently highlighted button does. */
  private void confirmSelection() {
    usingKeyboardNav = true;
    updateHighlight();
    switch (selectedIndex) {
      case 0 -> entity.getEvents().trigger("start");
      case 1 -> entity.getEvents().trigger("load");
      case 2 -> entity.getEvents().trigger("settings");
      case 3 -> entity.getEvents().trigger("exit");
      default -> {}
    }
  }

  @Override
  public void draw(SpriteBatch batch) {
    // draw is handled by the stage
  }

  @Override
  public float getZIndex() {
    return Z_INDEX;
  }

  @Override
  public void dispose() {
    table.clear();
    super.dispose();
  }
}
