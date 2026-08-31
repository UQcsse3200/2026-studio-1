package com.csse3200.game.areas.terrain.map;

import com.badlogic.gdx.math.GridPoint2;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * All spawn data defined by a level map: the player's start position plus any enemy and loot spawn
 * points. Exposed to other systems (spawning, respawn/loot recovery) so they don't hard-code
 * positions.
 */
public class MapSpawns {
  private GridPoint2 player;
  private final List<SpawnPoint> enemies = new ArrayList<>();
  private final List<SpawnPoint> loot = new ArrayList<>();

  /**
   * @return the player's start tile, or null if the map did not define one
   */
  public GridPoint2 getPlayer() {
    return player;
  }

  public void setPlayer(GridPoint2 player) {
    this.player = player;
  }

  public void addEnemy(SpawnPoint spawn) {
    enemies.add(spawn);
  }

  public void addLoot(SpawnPoint spawn) {
    loot.add(spawn);
  }

  /**
   * @return an unmodifiable view of the enemy spawn points
   */
  public List<SpawnPoint> getEnemies() {
    return Collections.unmodifiableList(enemies);
  }

  /**
   * @return an unmodifiable view of the loot spawn points
   */
  public List<SpawnPoint> getLoot() {
    return Collections.unmodifiableList(loot);
  }
}
