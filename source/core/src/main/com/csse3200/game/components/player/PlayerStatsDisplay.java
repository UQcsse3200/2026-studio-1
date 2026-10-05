package com.csse3200.game.components.player;

import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ProgressBar;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Array;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.pausemenu.AudioSettings;
import com.csse3200.game.services.ServiceLocator;
import com.csse3200.game.ui.UIComponent;

/**
 * UI component for displaying the player's health as hearts.
 *
 * <p>Each full heart represents 10 health points. A half heart represents 5 health points.
 */
public class PlayerStatsDisplay extends UIComponent {

  private static final int HEALTH_PER_HEART = 10;
  private static final int HALF_HEART_VALUE = 5;
  private static final int MAX_HEARTS = 10;
  private static final float HEART_SIZE = 30f;

  private static final float BRIBE_MESSAGE_DURATION = 3f;

  private Table table;
  private Label healthLabel;
  private int previousHealth;
  private ProgressBar staminaBar;
  private Table staminaTable;
  private StaminaComponent staminaComponent;

  private Label bribeLabel;
  private Table bribeMessageTable;
  private Table bribeContainerTable;
  private float bribeMessageTimer = 0f;

  private final Array<Image> heartImages = new Array<>();

  private Texture greenHeartTexture;
  private Texture yellowHeartTexture;
  private Texture redHeartTexture;
  private Texture emptyHeartTexture;

  private Texture greenHalfHeartTexture;
  private Texture yellowHalfHeartTexture;
  private Texture redHalfHeartTexture;

  /** Creates the health UI and listens for health changes. */
  @Override
  public void create() {
    super.create();

    addActors();

    entity.getEvents().addListener("updateHealth", this::updatePlayerHealthUI);
    entity.getEvents().addListener("updateStamina", this::updateStaminaUI);

    // Bribe notification events
    entity.getEvents().addListener("bribeSuccess", this::showBribeSuccess);
    entity.getEvents().addListener("bribeNoGold", this::showBribeNoGold);
    entity.getEvents().addListener("bribeNoTarget", this::showBribeNoTarget);
  }

  /** Creates the health UI and positions it in the top-left corner. */
  private void addActors() {
    table = new Table();
    table.top().left();
    table.setFillParent(true);
    table.padTop(45f).padLeft(5f);

    // Full heart textures
    greenHeartTexture =
        ServiceLocator.getResourceService().getAsset("images/ui/heart-green.png", Texture.class);

    yellowHeartTexture =
        ServiceLocator.getResourceService().getAsset("images/ui/heart-yellow.png", Texture.class);

    redHeartTexture =
        ServiceLocator.getResourceService().getAsset("images/ui/heart.png", Texture.class);

    emptyHeartTexture =
        ServiceLocator.getResourceService().getAsset("images/ui/heart-empty.png", Texture.class);

    // Half heart textures
    greenHalfHeartTexture =
        ServiceLocator.getResourceService()
            .getAsset("images/ui/heart-green-half.png", Texture.class);

    yellowHalfHeartTexture =
        ServiceLocator.getResourceService()
            .getAsset("images/ui/heart-yellow-half.png", Texture.class);

    redHalfHeartTexture =
        ServiceLocator.getResourceService().getAsset("images/ui/heart-red-half.png", Texture.class);

    // Create 10 heart slots
    for (int i = 0; i < MAX_HEARTS; i++) {
      Image heart = new HealthHeartImage(greenHeartTexture);

      heartImages.add(heart);

      table.add(heart).size(HEART_SIZE).pad(2f);
    }

    // Move to the next row so the health number appears under the hearts
    table.row();

    healthLabel = new Label("Health = 100", skin, "title");
    healthLabel.setFontScale(0.35f);

    table.add(healthLabel).colspan(MAX_HEARTS).padTop(0f).padBottom(4f).left();

    /*
     * Bribe notification.
     *
     * The container fills the screen only to allow the message box
     * to be positioned at the top centre.
     *
     * The actual bribeMessageTable remains a small box.
     */
    bribeContainerTable = new Table();
    bribeContainerTable.setFillParent(true);
    bribeContainerTable.top().center();
    bribeContainerTable.padTop(20f);

    bribeMessageTable = new Table();
    bribeMessageTable.setBackground(createBribeBoxBackground());
    bribeMessageTable.pad(7f, 10f, 7f, 10f);
    bribeMessageTable.setVisible(false);

    bribeLabel = new Label("", skin, "title");
    bribeLabel.setFontScale(0.24f);
    bribeLabel.setColor(Color.WHITE);
    bribeLabel.setWrap(true);

    bribeMessageTable.add(bribeLabel).width(360f).center();

    // Put the small message box inside the full-screen positioning container.
    bribeContainerTable.add(bribeMessageTable).center();

    // Stamina bar
    staminaTable = new Table();
    staminaTable.top().left();
    staminaTable.setFillParent(true);
    staminaTable.padTop(240f).padLeft(5f);

    staminaBar = new ProgressBar(0f, 100f, 1f, false, skin);

    staminaBar.setValue(100f);

    staminaTable.add(staminaBar).width(250f).height(20f).left();

    stage.addActor(staminaTable);
    stage.addActor(table);
    stage.addActor(bribeContainerTable);

    // Make sure the hearts and text match the player's current health
    int currentHealth = entity.getComponent(CombatStatsComponent.class).getHealth();
    staminaComponent = entity.getComponent(StaminaComponent.class);
    previousHealth = currentHealth;

    updatePlayerHealthUI(currentHealth);
  }

  /**
   * Creates the background for the bribe notification.
   *
   * <p>A simple white box with a dark border makes the notification readable against the game
   * world.
   */
  private TextureRegionDrawable createBribeBoxBackground() {
    int width = 2;
    int height = 2;

    com.badlogic.gdx.graphics.Pixmap pixmap =
        new com.badlogic.gdx.graphics.Pixmap(
            width, height, com.badlogic.gdx.graphics.Pixmap.Format.RGBA8888);

    pixmap.setColor(Color.WHITE);
    pixmap.fill();

    pixmap.setColor(Color.DARK_GRAY);
    pixmap.drawRectangle(0, 0, width, height);

    Texture texture = new Texture(pixmap);
    pixmap.dispose();

    return new TextureRegionDrawable(new TextureRegion(texture));
  }

  @Override
  public void update() {
    super.update();

    if (bribeMessageTimer > 0f) {
      bribeMessageTimer -= ServiceLocator.getTimeSource().getDeltaTime();

      if (bribeMessageTimer <= 0f) {
        bribeMessageTimer = 0f;

        if (bribeMessageTable != null) {
          bribeMessageTable.setVisible(false);
        }

        if (bribeLabel != null) {
          bribeLabel.setText("");
        }
      }
    }
  }

  @Override
  public void draw(SpriteBatch batch) {
    // Drawing is handled by the stage
  }

  /**
   * Displays a temporary bribe notification.
   *
   * @param message message to display
   */
  private void showBribeMessage(String message) {
    if (bribeLabel == null || bribeMessageTable == null) {
      return;
    }

    bribeLabel.setText(message);
    bribeMessageTable.setVisible(true);
    bribeMessageTimer = BRIBE_MESSAGE_DURATION;
  }

  /** Displays the successful bribe message. */
  private void showBribeSuccess() {
    showBribeMessage("ENEMY BRIBED!  -50 GOLD  |  IGNORING YOU FOR 20s");
  }

  /** Displays the insufficient gold message. */
  private void showBribeNoGold() {
    showBribeMessage("NOT ENOUGH GOLD  |  YOU NEED 50 GOLD TO BRIBE");
  }

  /** Displays the no-target message. */
  private void showBribeNoTarget() {
    showBribeMessage("NO BRIBE TARGET  |  AN ENEMY MUST DAMAGE YOU FIRST");
  }

  /**
   * Updates the heart display and health number.
   *
   * <p>Every full heart represents 10 health points. A half heart represents 5 health points.
   *
   * @param health player's current health
   */
  public void updatePlayerHealthUI(int health) {

    // Calculate how much health the player lost
    int damageTaken = previousHealth - health;
    KeyboardPlayerInputComponent input = entity.getComponent(KeyboardPlayerInputComponent.class);
    // Play different sounds depending on the amount of damage
    if (damageTaken == 25) {
      // Crowned ghost hit
      Sound crownHitSound =
          ServiceLocator.getResourceService().getAsset("sounds/player-hit-crown.ogg", Sound.class);

      crownHitSound.play(AudioSettings.getEffectiveEffectsVolume());
      if (input != null) {
        entity.getEvents().trigger("hurt", input.getDirection());
      }
    } else if (damageTaken > 0) {
      // Regular damage
      Sound hitSound =
          ServiceLocator.getResourceService().getAsset("sounds/player-hit.ogg", Sound.class);

      hitSound.play(AudioSettings.getEffectiveEffectsVolume());
      if (input != null) {
        entity.getEvents().trigger("hurt", input.getDirection());
      }
    }

    previousHealth = health;

    int fullHearts = health / HEALTH_PER_HEART;
    int remainder = health % HEALTH_PER_HEART;

    boolean hasHalfHeart = remainder >= HALF_HEART_VALUE;

    fullHearts = Math.max(0, Math.min(fullHearts, MAX_HEARTS));

    Texture fullHeartTexture;
    Texture halfHeartTexture;

    // Choose the correct heart colour set based on current health
    if (health <= 30) {
      fullHeartTexture = redHeartTexture;
      halfHeartTexture = redHalfHeartTexture;

    } else if (health <= 60) {
      fullHeartTexture = yellowHeartTexture;
      halfHeartTexture = yellowHalfHeartTexture;

    } else {
      fullHeartTexture = greenHeartTexture;
      halfHeartTexture = greenHalfHeartTexture;
    }

    // Update all heart slots
    for (int i = 0; i < heartImages.size; i++) {
      Image heart = heartImages.get(i);

      if (i < fullHearts) {
        // Full heart
        heart.setDrawable(new TextureRegionDrawable(new TextureRegion(fullHeartTexture)));

      } else if (i == fullHearts && hasHalfHeart) {
        // Half heart
        heart.setDrawable(new TextureRegionDrawable(new TextureRegion(halfHeartTexture)));

      } else {
        // Empty heart
        heart.setDrawable(new TextureRegionDrawable(new TextureRegion(emptyHeartTexture)));
      }

      // Do not tint the PNGs
      heart.setColor(Color.WHITE);
    }

    // Update exact health number
    healthLabel.setText("Health = " + health);
  }

  /** Updates the stamina display. */
  public void updateStaminaUI(float stamina) {
    if (staminaBar != null) {
      staminaBar.setValue(stamina);
    }
  }

  /**
   * Custom heart image.
   *
   * <p>Resets the SpriteBatch colour after drawing so that heart rendering cannot affect other
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

    if (table != null) {
      table.remove();
    }

    if (bribeMessageTable != null) {
      bribeMessageTable.remove();
    }

    if (bribeContainerTable != null) {
      bribeContainerTable.remove();
    }

    if (staminaTable != null) {
      staminaTable.remove();
    }

    heartImages.clear();
  }
}
