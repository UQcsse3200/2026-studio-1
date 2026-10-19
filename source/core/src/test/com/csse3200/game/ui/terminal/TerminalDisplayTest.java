package com.csse3200.game.ui.terminal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.services.ServiceLocator;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

@ExtendWith(GameExtension.class)
class TerminalDisplayTest {
  private RenderService renderService;
  private Terminal terminal;
  private TerminalDisplay display;
  private Label inputLabel;
  private Label outputLabel;

  @BeforeEach
  void setUp() {
    Stage stage = mock(Stage.class);
    renderService = mock(RenderService.class);
    when(renderService.getStage()).thenReturn(stage);
    ServiceLocator.registerRenderService(renderService);

    terminal = new Terminal();
    display = new TerminalDisplay();
    new Entity().addComponent(terminal).addComponent(display).create();

    ArgumentCaptor<Actor> addedActors = ArgumentCaptor.forClass(Actor.class);
    verify(stage, times(2)).addActor(addedActors.capture());
    List<Actor> actors = addedActors.getAllValues();
    inputLabel = (Label) actors.get(0);
    outputLabel = (Label) actors.get(1);
  }

  @Test
  void showsEnteredTextOnlyWhileTerminalIsOpen() {
    display.draw(mock(SpriteBatch.class));
    assertFalse(inputLabel.isVisible());
    assertFalse(outputLabel.isVisible());

    terminal.setOpen();
    terminal.setEnteredMessage("where");
    display.draw(mock(SpriteBatch.class));
    assertTrue(inputLabel.isVisible());
    assertEquals("> where", inputLabel.getText().toString());

    terminal.setClosed();
    display.draw(mock(SpriteBatch.class));
    assertFalse(inputLabel.isVisible());
  }

  @Test
  void showsCommandOutputEvenWhenTerminalIsClosed() {
    terminal.showOutputMessage("Room: Dungeon");

    display.draw(mock(SpriteBatch.class));
    assertTrue(outputLabel.isVisible());
    assertEquals("Room: Dungeon", outputLabel.getText().toString());
    assertTrue(outputLabel.getWidth() > 0f);

    terminal.setOpen();
    display.draw(mock(SpriteBatch.class));
    assertFalse(outputLabel.isVisible());
  }

  @Test
  void usesTerminalLayerAndUnregistersWhenDisposed() {
    assertEquals(10f, display.getZIndex());

    display.dispose();

    verify(renderService).unregister(display);
  }
}
