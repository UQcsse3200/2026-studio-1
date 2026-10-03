package com.csse3200.game.ui.terminal.commands;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.areas.LevelGameArea;
import com.csse3200.game.entities.Entity;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SpawnCommandTest {

  private SpawnCommand spawnCommand;
  // Arbitrary for testing
  private static final String Enemy = "skeleton";
  private final LevelGameArea mockLevelGameArea = mock(LevelGameArea.class);
  private final Entity mockPlayer = mock(Entity.class);
  // Arbitrary for testing
  private final Vector2 playerPosition = new Vector2(100f, 100f);

  @BeforeEach
  void setup() {
    spawnCommand = new SpawnCommand(mockLevelGameArea);
  }

  @AfterEach
  void tearDown() {
    // Reset levelGameArea after each test
    SpawnCommand.updateLevelGameArea(null);
  }

  /** Stubs levelGameArea.getPlayer().getPosition() for playerPosition. */
  private void stubPlayerPosition() {
    when(mockLevelGameArea.getPlayer()).thenReturn(mockPlayer);
    when(mockPlayer.getPosition()).thenReturn(playerPosition);
  }

  @Test
  void successfullySpawnsGivenValidEnemy() {
    stubPlayerPosition();
    when(mockLevelGameArea.spawnEnemy(Enemy, playerPosition)).thenReturn(true);

    assertTrue(spawnCommand.action(new ArrayList<>(List.of(Enemy))));
    verify(mockLevelGameArea).spawnEnemy(Enemy, playerPosition);
  }

  @Test
  void returnsFalseOnInvalidSpawnArg() {
    stubPlayerPosition();
    when(mockLevelGameArea.spawnEnemy("NotEnemy", playerPosition)).thenReturn(false);

    assertFalse(spawnCommand.action(new ArrayList<>(List.of("NotEnemy"))));
    verify(mockLevelGameArea).spawnEnemy("NotEnemy", playerPosition);
  }

  @Test
  void correctlyUpdatesLevelGameArea() {
    // TODO - Finish all test cases for the spawn command
  }

  @Test
  void isValidReturnsFalseOnWrongArgSize() {
    // TODO - Finish all test cases for the spawn command
  }

  @Test
  void isValidReturnsFalseOnNullLevelGameArea() {
    // TODO - Finish all test cases for the spawn command
  }

  @Test
  void isValidReturnsTrueOnValid() {
    // TODO - Finish all test cases for the spawn command
  }
}
