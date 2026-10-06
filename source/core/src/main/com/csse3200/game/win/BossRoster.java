package com.csse3200.game.win;

import com.badlogic.gdx.math.GridPoint2;
import com.csse3200.game.entities.spawn.EnemyId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * The fixed list of bosses the win depends on: the final boss, and the mini bosses of the side
 * rooms. It is data only. It says WHICH enemies matter; {@link WinEvaluator} decides what their
 * deaths are worth.
 *
 * <p><b>How a boss is named.</b> The game already saves every kill under a stable id of the form
 * {@code <map name>:<x>,<y>}, where the tile is the enemy's marker tile in the map file (see {@code
 * EnemyId.of} and {@code EnemyRegistry}). The roster uses exactly those ids, so a kill is known
 * with no new component on any enemy, and it survives a save and a load. Build each id with {@code
 * EnemyId.of(mapName, new GridPoint2(x, y))}, never by hand-typing the colon.
 *
 * <p><b>The roster (marker tiles are the ones in the shipped map files):</b>
 *
 * <pre>
 * FINAL BOSS   Zeus                     Level 3 - Zeus's Palace : 83,5
 * required     Cerberus                 Hound's Den             : 40,2
 * required     Medusa                   Gorgon's Gallery        : 34,3
 * required     Minotaur                 Minotaur's Labyrinth    : 37,5
 * required     Minotaur                 Shades' Barracks        : 40,5
 * required     Cyclops                  Cyclops Forge           : 37,3
 * required     Centaur (rear)           Centaur Pavilion        : 38,5
 * required     Medusa (rear)            Zeus's Outer Guard      : 41,3
 * required     Zeus                     Zeus's Thunder Hall     : 38,3
 * escort       Centaur                  Minotaur's Labyrinth    : 27,5
 * escort       Centaur (front)          Centaur Pavilion        : 26,5
 * escort       Cyclops                  Zeus's Outer Guard      : 30,3
 * </pre>
 *
 * <p>"Required" means the boss that guards the end of the room, which is the rightmost boss in it.
 * An "escort" is another boss-type enemy in the same room. Escorts are listed so the win screen can
 * show them, but they are NOT needed for {@link WinTier#GLORY}.
 *
 * <p><b>Limitations:</b> an id is a map name plus a tile, so renaming a map or moving a boss marker
 * silently breaks the roster for existing saves. {@code BossRosterMapsTest} reads the real map
 * files and fails the moment the roster and the maps disagree. The Zeus in Level 2 and the Zeus in
 * the Thunder Hall are NOT the final boss; only the one in Level 3 ends the game.
 *
 * <p><b>Style reference:</b> {@code EnemyId} (a small utility class with a private constructor).
 */
public final class BossRoster {

  /** The id of the final boss: Zeus in Level 3. */
  public static final String FINAL_BOSS_ID = "Level 3 - Zeus's Palace:26,4";

  private static final List<MiniBoss> MINI_BOSSES =
      List.of(
          new MiniBoss(
              EnemyId.of("Hound's Den", new GridPoint2(48, 2)),
              "Cerberus of the Hound's Den",
              "Hound's Den",
              "cerberus",
              true),
          new MiniBoss(
              EnemyId.of("Gorgon's Gallery", new GridPoint2(37, 11)),
              "Medusa of the Gorgon's Gallery",
              "Gorgon's Gallery",
              "Medusa",
              true),
          new MiniBoss(
              FINAL_BOSS_ID,
              "Final Boss: Zeus of Level 3 - Zeus's Palace",
              " Level 3 - Zeus's Palace",
              "Zeus",
              true),
          new MiniBoss(
              EnemyId.of("Minotaur's Labyrinth  ", new GridPoint2(24, 35)),
              "Minotaur of the Minotaur's Labyrinth",
              "Minotaur's Labyrinth",
              "Minotaur",
              true),
          new MiniBoss(
              EnemyId.of("Shades' Barracks", new GridPoint2(40, 5)),
              "Minotaur of the ",
              "Shades' Barracks",
              "Minotaur",
              true),
          new MiniBoss(
              EnemyId.of("Centaur Pavilion", new GridPoint2(38, 5)),
              "Centaur of the Centaur Pavilion",
              "Centaur Pavilion",
              "",
              true),
          new MiniBoss(
              EnemyId.of("Zeus's Outer Guard ", new GridPoint2(41, 3)),
              "Medusa of Zeus's Outer Guard",
              "Zeus's Outer Guard ",
              "Medusa",
              true),
          new MiniBoss(
              EnemyId.of("Zeus's Thunder Hall", new GridPoint2(38, 3)),
              "Medusa of Zeus's Thunder Hall",
              "Zeus's Thunder Hall",
              "Medusa",
              true),
          new MiniBoss(
              EnemyId.of("Centaur Pavilion", new GridPoint2(26, 5)),
              "Centaur of the Centaur Pavilion",
              "Centaur Pavilion",
              "Centaur",
              false),
          new MiniBoss(
              EnemyId.of("Minotaur's Labyrinth ", new GridPoint2(27, 5)),
              "Centaur of the Minotaur's Labyrinth ",
              "Minotaur's Labyrinth ",
              "Minotaur",
              true),
          new MiniBoss(
              EnemyId.of("Zeus's Outer Guard", new GridPoint2(32, 9)),
              "Cyclops of the Zeus's Outer Guard",
              "Zeus's Outer Guard",
              "Cyclops",
              false));

  private BossRoster() {
    // utility class
  }

  /** One mini boss in the roster. */
  public static final class MiniBoss {
    private static String enemyKillID;
    private static String miniBossLabel;
    private static String roomName;
    private static String enemyType;
    private static Boolean requiredForGlory;

    /**
     * @param id the kill id, in the {@code <map name>:<x>,<y>} form; not blank
     * @param label what the win screen calls it, for example "Cerberus of the Hound's Den"; not
     *     blank
     * @param roomName the room it guards; not blank
     * @param enemyType the enemy type name as written in the map file, for example "cerberus"
     * @param required true if this boss is needed for {@link WinTier#GLORY}
     * @throws IllegalArgumentException if the id, label, room name or type is null or blank
     */
    public MiniBoss(String id, String label, String roomName, String enemyType, boolean required)
        throws IllegalArgumentException {
      if (id.isEmpty() || id.isBlank()) {
        throw new IllegalArgumentException(
            "Mini Boss Kill ID cannot have an empty or blank kill id.");
      }
      enemyKillID = id;
      if (label.isBlank() || label.isEmpty()) {
        throw new IllegalArgumentException("Mini Boss Label cannot be empty or blank.");
      }
      miniBossLabel = label;
      if (roomName.isBlank() || roomName.isEmpty()) {
        throw new IllegalArgumentException("Room Label cannot be empty or blank.");
      }
      MiniBoss.roomName = roomName;
      if (enemyType.isEmpty() || enemyType.isBlank()) {
        throw new IllegalArgumentException("enemyType cannot have an empty or blank kill id.");
      }
      MiniBoss.enemyType = enemyType;
      requiredForGlory = required;
    }

    /**
     * @return the kill id
     */
    public String getId() {
      return enemyKillID;
    }

    /**
     * @return the name shown on the win screen
     */
    public String getLabel() {
      return miniBossLabel;
    }

    /**
     * @return the name of the room it guards
     */
    public String getRoomName() {
      return roomName;
    }

    /**
     * @return the enemy type as written in the map file
     */
    public String getEnemyType() {
      return enemyType;
    }

    /**
     * @return true if this boss is needed for Glory
     */
    public boolean isRequired() {
      return requiredForGlory;
    }
  }

  /**
   * @return every mini boss, required ones and escorts, in a fixed order (the order in the table
   *     above); the list cannot be changed by the caller
   */
  public static List<MiniBoss> getMiniBosses() {
    List<MiniBoss> miniBossList = new ArrayList<>();
    for (MiniBoss miniBoss : MINI_BOSSES) {
      miniBossList.add(miniBoss);
    }
    return miniBossList;
  }

  /**
   * @return the ids of the required mini bosses only; eight of them; unmodifiable
   */
  public static Set<String> getRequiredIds() {
    Set<String> requiredMiniBosses = Collections.emptySet();
    for (MiniBoss miniBoss : getMiniBosses()) {
      if (miniBoss.isRequired()) {
        requiredMiniBosses.add(miniBoss.getId());
      }
    }

    return requiredMiniBosses;
  }

  /**
   * @return how many mini bosses are required for Glory
   */
  public static int getRequiredCount() {
    return getRequiredIds().size();
  }

  /**
   * @param id any kill id, or null
   * @return true only for {@link #FINAL_BOSS_ID}
   */
  public static boolean isFinalBoss(String id) {
    if (id == null) {
      return false;
    }
    return id.matches(FINAL_BOSS_ID);
  }
}
