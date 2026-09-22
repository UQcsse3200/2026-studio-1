package com.csse3200.game.areas.terrain.map;

import com.badlogic.gdx.math.GridPoint2;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * All spawn data defined by a level map: the player's start position, any enemy and loot spawn
 * points, and any {@link Marker}s the author placed. Exposed to other systems (spawning,
 * respawn/loot recovery) so they don't hard-code positions.
 *
 * <p>Player, enemy and loot have their own accessors because the level area spawns them itself.
 * Everything else is a marker, looked up by kind, so a team can place content on a map without the
 * map package knowing what that content is.
 */
public class MapSpawns {
  private GridPoint2 player;
  private final List<SpawnPoint> enemies = new ArrayList<>();
  private final List<SpawnPoint> loot = new ArrayList<>();
  private final Map<String, List<Marker>> markers = new LinkedHashMap<>();

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

  /**
   * Records a marker under its kind.
   *
   * @param marker the marker to add
   */
  public void addMarker(Marker marker) {
    markers.computeIfAbsent(marker.kind(), kind -> new ArrayList<>()).add(marker);
  }

  /**
   * All markers of one kind, in the order they were read from the map.
   *
   * @param kind the marker kind, matched ignoring case (e.g. "npc", "light", "checkpoint")
   * @return an unmodifiable view, empty if the map placed none of that kind
   */
  public List<Marker> getMarkers(String kind) {
    if (kind == null) {
      return Collections.emptyList();
    }
    List<Marker> found = markers.get(kind.trim().toLowerCase(Locale.ROOT));
    return found == null ? Collections.emptyList() : Collections.unmodifiableList(found);
  }

  /**
   * @return every marker kind this map defines, for diagnostics and validation
   */
  public Set<String> getMarkerKinds() {
    return Collections.unmodifiableSet(markers.keySet());
  }

  /**
   * @return every marker on this map, across all kinds
   */
  public List<Marker> getAllMarkers() {
    List<Marker> all = new ArrayList<>();
    markers.values().forEach(all::addAll);
    return Collections.unmodifiableList(all);
  }
}
