
package com.csse3200.game.screens;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.csse3200.game.GdxGame;
import org.junit.jupiter.api.Test;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;


class DeathScreenTest {

    @Test
    void constructorCreatesDeathScreen() {
        GdxGame game = org.mockito.Mockito.mock(GdxGame.class);

        DeathScreen deathScreen = new DeathScreen(game);

        assertNotNull(deathScreen);
    }

    @Test
    void showDoesNotThrow() {
        GdxGame game = org.mockito.Mockito.mock(GdxGame.class);
        DeathScreen deathScreen = new DeathScreen(game);

        assertDoesNotThrow(deathScreen::show);
    }

    @Test
    void disposeDoesNotThrow() {
        GdxGame game = org.mockito.Mockito.mock(GdxGame.class);
        DeathScreen deathScreen = new DeathScreen(game);

        assertDoesNotThrow(deathScreen::dispose);
    }

    @Test
    void renderDoesNotThrow() {
        GdxGame game = org.mockito.Mockito.mock(GdxGame.class);
        DeathScreen deathScreen = new DeathScreen(game);

        Gdx.gl = org.mockito.Mockito.mock(com.badlogic.gdx.graphics.GL20.class);

        assertDoesNotThrow(() -> deathScreen.render(0.016f));
    }
}
