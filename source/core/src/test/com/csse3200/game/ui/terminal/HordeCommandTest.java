package com.csse3200.game.ui.terminal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.areas.LevelGameArea;
import com.csse3200.game.areas.terrain.map.LevelView;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.ui.terminal.commands.HordeCommand;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class HordeCommandTest {
  private final LevelGameArea area = mock(LevelGameArea.class);
  private final LevelView level = mock(LevelView.class);
  private final Entity player = mock(Entity.class);
  private final AtomicReference<String> output = new AtomicReference<>();
  private HordeCommand command;

  @BeforeEach
  void setUp() {
    command = new HordeCommand(() -> area, output::set);
    when(area.getPlayer()).thenReturn(player);
    when(area.getLevel()).thenReturn(level);
    when(player.getCenterPosition()).thenReturn(new Vector2(10.5f, 5.5f));
    when(level.tileSize()).thenReturn(1f);
    when(level.inBounds(anyInt(), anyInt())).thenReturn(true);
    when(level.isWalkable(anyInt(), anyInt())).thenReturn(true);
    when(level.groundTiles())
        .thenReturn(
            List.of(
                new GridPoint2(8, 5),
                new GridPoint2(12, 5),
                new GridPoint2(7, 5),
                new GridPoint2(13, 5),
                new GridPoint2(6, 5)));
    when(area.spawnEnemy(eq("skeleton"), any(Vector2.class))).thenReturn(true);
  }

  @Test
  void spawnsRequestedNumberOfSkeletons() {
    assertTrue(command.action(new ArrayList<>(List.of("5"))));
    verify(area, times(5)).spawnEnemy(eq("skeleton"), any(Vector2.class));
    assertEquals("Spawned 5 of 5 skeletons.", output.get());
  }

  @Test
  void rejectsInvalidCountsWithoutSpawning() {
    for (List<String> args :
        List.of(
            List.<String>of(),
            List.of("five"),
            List.of("0"),
            List.of("-1"),
            List.of("21"),
            List.of("5", "extra"))) {
      assertFalse(command.action(new ArrayList<>(args)));
    }
    verify(area, never()).spawnEnemy(anyString(), any(Vector2.class));
  }

  @Test
  void doesNotPartiallySpawnWhenThereIsTooLittleRoom() {
    when(level.groundTiles()).thenReturn(List.of(new GridPoint2(8, 5)));

    assertFalse(command.action(new ArrayList<>(List.of("5"))));
    verify(area, never()).spawnEnemy(anyString(), any(Vector2.class));
    assertEquals("Not enough nearby ground to spawn 5 enemies.", output.get());
  }

  @Test
  void usesTheCurrentAreaAfterAChangeOfRoom() {
    AtomicReference<LevelGameArea> currentArea = new AtomicReference<>(area);
    command = new HordeCommand(currentArea::get, output::set);
    LevelGameArea nextArea = mock(LevelGameArea.class);
    when(nextArea.getPlayer()).thenReturn(player);
    when(nextArea.getLevel()).thenReturn(level);
    when(nextArea.spawnEnemy(eq("skeleton"), any(Vector2.class))).thenReturn(true);
    currentArea.set(nextArea);

    assertTrue(command.action(new ArrayList<>(List.of("1"))));
    verify(nextArea).spawnEnemy(eq("skeleton"), any(Vector2.class));
    verify(area, never()).spawnEnemy(anyString(), any(Vector2.class));
  }
}
