package com.csse3200.game.components.player;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Array;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.services.ServiceLocator;
import com.csse3200.game.ui.UIComponent;

/**
 * UI component for displaying the player's health as hearts.
 *
 * <p>Each heart represents 10 health points. A player with 100 health therefore has 10 hearts.
 */
public class PlayerStatsDisplay extends UIComponent {

  private static final int HEALTH_PER_HEART = 10;
  private static final int MAX_HEARTS = 10;
  private static final float HEART_SIZE = 30f;

  private Table table;
  private Label healthLabel;

  private final Array<Image> heartImages = new Array<>();

  private Texture greenHeartTexture;
  private Texture yellowHeartTexture;
  private Texture redHeartTexture;
  private Texture emptyHeartTexture;

  /** Creates the health UI and listens for health changes. */
  @Override
  public void create() {
    super.create();

    addActors();

    entity.getEvents().addListener("updateHealth", this::updatePlayerHealthUI);
  }

  /** Creates the heart images and positions them in the top-left corner. */
  private void addActors() {
    table = new Table();
    table.top().left();
    table.setFillParent(true);
    table.padTop(45f).padLeft(5f);

    // Load the different heart textures
    greenHeartTexture =
        ServiceLocator.getResourceService().getAsset("images/heart-green-512px.png", Texture.class);

    yellowHeartTexture =
        ServiceLocator.getResourceService()
            .getAsset("images/heart-yellow-512px.png", Texture.class);

    // Existing heart.png is used as the red heart
    redHeartTexture =
        ServiceLocator.getResourceService().getAsset("images/heart-red-512px.png", Texture.class);

    emptyHeartTexture =
        ServiceLocator.getResourceService().getAsset("images/heart-empty-512px.png", Texture.class);

    // Create 10 heart slots.
    // They start as green because the player begins at full health.
    for (int i = 0; i < MAX_HEARTS; i++) {
      Image heart = new HealthHeartImage(greenHeartTexture);

      heartImages.add(heart);

      table.add(heart).size(HEART_SIZE).pad(2f);
    }

    // Move to the next row so the health number appears under the hearts
    table.row();

    healthLabel = new Label("Health = 100", skin, "large");

    table.add(healthLabel).colspan(MAX_HEARTS).padTop(5f).left();

    stage.addActor(table);

    // Make sure the hearts and text match the player's current health
    int currentHealth = entity.getComponent(CombatStatsComponent.class).getHealth();

    updatePlayerHealthUI(currentHealth);
  }

  @Override
  public void draw(SpriteBatch batch) {
    // Drawing is handled by the stage
  }

  /**
   * Updates the heart display and health number.
   *
   * <p>Every 10 health points represents one filled heart. Lost health is represented by empty
   * heart outlines.
   *
   * @param health player's current health
   */
  public void updatePlayerHealthUI(int health) {

    int visibleHearts = (int) Math.ceil((double) health / HEALTH_PER_HEART);

    visibleHearts = Math.max(0, Math.min(visibleHearts, MAX_HEARTS));

    Texture currentHeartTexture;

    // Choose which filled-heart image to use
    if (visibleHearts <= 3) {
      // 1-3 hearts = red
      currentHeartTexture = redHeartTexture;

    } else if (visibleHearts <= 6) {
      // 4-6 hearts = yellow
      currentHeartTexture = yellowHeartTexture;

    } else {
      // 7-10 hearts = green
      currentHeartTexture = greenHeartTexture;
    }

    // Update every heart slot
    for (int i = 0; i < heartImages.size; i++) {
      Image heart = heartImages.get(i);

      if (i < visibleHearts) {
        // Filled heart
        heart.setDrawable(new TextureRegionDrawable(new TextureRegion(currentHeartTexture)));

      } else {
        // Empty heart outline
        heart.setDrawable(new TextureRegionDrawable(new TextureRegion(emptyHeartTexture)));
      }

      /*
       * The PNG already contains the desired colour,
       * so do not tint it.
       */
      heart.setColor(Color.WHITE);
    }

    // Update exact health number
    healthLabel.setText("Health = " + health);
  }

  /**
   * Custom heart image.
   *
   * <p>Resets the SpriteBatch colour after drawing so that the heart rendering cannot affect other
   * objects in the game.
   */
  private static class HealthHeartImage extends Image {

    public HealthHeartImage(Texture texture) {
      super(texture);
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
      super.draw(batch, parentAlpha);

      batch.setColor(Color.WHITE);
    }
  }

  /** Removes UI actors when this component is destroyed. */
  @Override
  public void dispose() {
    super.dispose();

    table.remove();
    heartImages.clear();
  }
}
