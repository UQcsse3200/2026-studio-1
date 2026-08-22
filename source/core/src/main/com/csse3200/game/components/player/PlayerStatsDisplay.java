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
 * <p>Each full heart represents 10 health points.
 * A half heart represents 5 health points.
 */
public class PlayerStatsDisplay extends UIComponent {

    private static final int HEALTH_PER_HEART = 10;
    private static final int HALF_HEART_VALUE = 5;
    private static final int MAX_HEARTS = 10;
    private static final float HEART_SIZE = 30f;

    private Table table;
    private Label healthLabel;

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

        entity.getEvents().addListener(
                "updateHealth",
                this::updatePlayerHealthUI
        );
    }

    /**
     * Creates the heart images and positions them in the top-left corner.
     */
    private void addActors() {
        table = new Table();
        table.top().left();
        table.setFillParent(true);
        table.padTop(45f).padLeft(5f);

        // Full heart textures
        greenHeartTexture =
                ServiceLocator.getResourceService()
                        .getAsset("images/heart-green.png", Texture.class);

        yellowHeartTexture =
                ServiceLocator.getResourceService()
                        .getAsset("images/heart-yellow.png", Texture.class);

        redHeartTexture =
                ServiceLocator.getResourceService()
                        .getAsset("images/heart.png", Texture.class);

        emptyHeartTexture =
                ServiceLocator.getResourceService()
                        .getAsset("images/heart-empty.png", Texture.class);

        // Half heart textures
        greenHalfHeartTexture =
                ServiceLocator.getResourceService()
                        .getAsset("images/heart-green-half.png", Texture.class);

        yellowHalfHeartTexture =
                ServiceLocator.getResourceService()
                        .getAsset("images/heart-yellow-half.png", Texture.class);

        redHalfHeartTexture =
                ServiceLocator.getResourceService()
                        .getAsset("images/heart-red-half.png", Texture.class);

        // Create 10 heart slots
        for (int i = 0; i < MAX_HEARTS; i++) {
            Image heart = new HealthHeartImage(greenHeartTexture);

            heartImages.add(heart);

            table.add(heart)
                    .size(HEART_SIZE)
                    .pad(2f);
        }

        // Move to the next row so the health number appears under the hearts
        table.row();

        healthLabel = new Label(
                "Health = 100",
                skin,
                "large"
        );

        table.add(healthLabel)
                .colspan(MAX_HEARTS)
                .padTop(5f)
                .left();

        stage.addActor(table);

        // Make sure the hearts and text match the player's current health
        int currentHealth =
                entity.getComponent(CombatStatsComponent.class).getHealth();

        updatePlayerHealthUI(currentHealth);
    }

    @Override
    public void draw(SpriteBatch batch) {
        // Drawing is handled by the stage
    }

    /**
     * Updates the heart display and health number.
     *
     * <p>Every full heart represents 10 HP.
     * A half heart represents 5 HP.
     *
     * @param health player's current health
     */
    public void updatePlayerHealthUI(int health) {

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
                heart.setDrawable(
                        new TextureRegionDrawable(
                                new TextureRegion(fullHeartTexture)
                        )
                );

            } else if (i == fullHearts && hasHalfHeart) {
                // Half heart
                heart.setDrawable(
                        new TextureRegionDrawable(
                                new TextureRegion(halfHeartTexture)
                        )
                );

            } else {
                // Empty heart
                heart.setDrawable(
                        new TextureRegionDrawable(
                                new TextureRegion(emptyHeartTexture)
                        )
                );
            }

            // Do not tint the PNGs
            heart.setColor(Color.WHITE);
        }

        // Update exact health number
        healthLabel.setText("Health = " + health);
    }

    /**
     * Custom heart image.
     *
     * <p>Resets the SpriteBatch colour after drawing so that heart
     * rendering cannot affect other objects in the game.
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