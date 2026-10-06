package com.csse3200.game.win;

import com.badlogic.gdx.math.GridPoint2;
import com.csse3200.game.entities.spawn.EnemyId;
import java.util.HashSet;
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
 * FINAL BOSS   Zeus                     Level 3 - Zeus's Palace : 26,4
 * required     Cerberus                 Hound's Den             : 48,2
 * required     Medusa                   Gorgon's Gallery        : 37,11
 * required     Minotaur                 Minotaur's Labyrinth    : 24,35
 * required     Minotaur                 Shades' Barracks        : 40,17
 * required     Cyclops                  Cyclops Forge           : 30,15
 * required     Centaur (upper)          Centaur Pavilion        : 44,12
 * required     Medusa                   Zeus's Outer Guard      : 24,16
 * required     Medusa                   Zeus's Thunder Hall     : 26,12
 * escort       Centaur                  Minotaur's Labyrinth    : 28,17
 * escort       Centaur (lower)          Centaur Pavilion        : 40,5
 * escort       Cyclops                  Zeus's Outer Guard      : 32,9
 * </pre>
 *
 * <p>"Required" means the boss that guards the end of the room: the one on its highest storey,
 * beside the exit. An "escort" is another boss-type enemy in the same room. Escorts are listed so
 * the win screen can show them, but they are NOT needed for {@link WinTier#GLORY}.
 *
 * <p>The final boss is not in the list of mini bosses. There is only one Zeus in the game, in Level
 * 3, and only his death ends it.
 *
 * <p><b>Limitations:</b> an id is a map name plus a tile, so renaming a map or moving a boss marker
 * silently breaks the roster for existing saves. {@code BossRosterMapsTest} reads the real map
 * files and fails the moment the roster and the maps disagree.
 *
 * <p><b>Style reference:</b> {@code EnemyId} (a small utility class with a private constructor).
 */
public final class BossRoster {

  /** The id of the final boss: Zeus in Level 3. */
  public static final String FINAL_BOSS_ID = "Level 3 - Zeus's Palace:26,4";

  /** Every mini boss: the eight required ones first, then the three escorts. */
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
        "medusa",
        true),
      new MiniBoss(
        EnemyId.of("Minotaur's Labyrinth", new GridPoint2(24, 35)),
        "Minotaur of the Labyrinth",
        "Minotaur's Labyrinth",
        "minotaur",
        true),
      new MiniBoss(
        EnemyId.of("Shades' Barracks", new GridPoint2(40, 17)),
        "Minotaur of the Shades' Barracks",
        "Shades' Barracks",
        "minotaur",
        true),
      new MiniBoss(
        EnemyId.of("Cyclops Forge", new GridPoint2(30, 15)),
        "Cyclops of the Forge",
        "Cyclops Forge",
        "cyclops",
        true),
      new MiniBoss(
        EnemyId.of("Centaur Pavilion", new GridPoint2(44, 12)),
        "Centaur of the Pavilion heights",
        "Centaur Pavilion",
        "centaur",
        true),
      new MiniBoss(
        EnemyId.of("Zeus's Outer Guard", new GridPoint2(24, 16)),
        "Medusa of the Outer Guard",
        "Zeus's Outer Guard",
        "medusa",
        true),
      new MiniBoss(
        EnemyId.of("Zeus's Thunder Hall", new GridPoint2(26, 12)),
        "Medusa of the Thunder Hall",
        "Zeus's Thunder Hall",
        "medusa",
        true),
      new MiniBoss(
        EnemyId.of("Minotaur's Labyrinth", new GridPoint2(28, 17)),
        "Centaur of the Labyrinth",
        "Minotaur's Labyrinth",
        "centaur",
        false),
      new MiniBoss(
        EnemyId.of("Centaur Pavilion", new GridPoint2(40, 5)),
        "Centaur of the Pavilion floor",
        "Centaur Pavilion",
        "centaur",
        false),
      new MiniBoss(
        EnemyId.of("Zeus's Outer Guard", new GridPoint2(32, 9)),
        "Cyclops of the Outer Guard",
        "Zeus's Outer Guard",
        "cyclops",
        false));

  /** The ids of the required mini bosses, worked out once from the list above. */
  private static final Set<String> REQUIRED_IDS = collectRequiredIds();

  private BossRoster() {
    throw new IllegalStateException("Utility class");
  }

  /**
   * Collects the id of every mini boss that is required for Glory.
   *
   * @return the required ids, as a set that cannot be changed
   */
  private static Set<String> collectRequiredIds() {
    Set<String> ids = new HashSet<>();
    for (MiniBoss miniBoss : MINI_BOSSES) {
      if (miniBoss.isRequired()) {
        ids.add(miniBoss.getId());
      }
    }
    return Set.copyOf(ids);
  }

  /** One mini boss in the roster. Each entry holds its own values, which never change. */
  public static final class MiniBoss {
    private final String id;
    private final String label;
    private final String roomName;
    private final String enemyType;
    private final boolean required;

    /**
     * Creates one entry of the roster, describing a single mini boss.
     *
     * @param id the kill id, in the {@code <map name>:<x>,<y>} form; not blank
     * @param label what the win screen calls it, for example "Cerberus of the Hound's Den"; not
     *     blank
     * @param roomName the room it guards; not blank
     * @param enemyType the enemy type name as written in the map file, for example "cerberus"
     * @param required true if this boss is needed for {@link WinTier#GLORY}
     * @throws IllegalArgumentException if the id, label, room name or type is null or blank
     */
    public MiniBoss(String id, String label, String roomName, String enemyType, boolean required) {
      this.id = requireText(id, "id");
      this.label = requireText(label, "label");
      this.roomName = requireText(roomName, "roomName");
      this.enemyType = requireText(enemyType, "enemyType");
      this.required = required;
    }

    /**
     * Checks that a piece of text was really given.
     *
     * @param value the text to check
     * @param name what the text is called, for the error message
     * @return the same text, if it is neither null nor blank
     * @throws IllegalArgumentException if the text is null or blank
     */
    private static String requireText(String value, String name) {
      if (value == null || value.isBlank()) {
        throw new IllegalArgumentException(name + " must not be blank");
      }
      return value;
    }

    /**
     * Gets the id this boss's death is saved under.
     *
     * @return the kill id
     */
    public String getId() {
      return id;
    }

    /**
     * Gets what the win screen calls this boss.
     *
     * @return the name shown on the win screen
     */
    public String getLabel() {
      return label;
    }

    /**
     * Gets the room this boss is found in.
     *
     * @return the name of the room it guards
     */
    public String getRoomName() {
      return roomName;
    }

    /**
     * Gets what kind of enemy this boss is.
     *
     * @return the enemy type as written in the map file
     */
    public String getEnemyType() {
      return enemyType;
    }

    /**
     * Says whether this boss counts towards the Glory tier.
     *
     * @return true if this boss is needed for Glory
     */
    public boolean isRequired() {
      return required;
    }
  }

  /**
   * Gives the whole roster: the eight required bosses first, then the three escorts.
   *
   * @return every mini boss in a fixed order; the list cannot be changed by the caller
   */
  public static List<MiniBoss> getMiniBosses() {
    return MINI_BOSSES;
  }

  /**
   * Gives the ids of the bosses that must be defeated for Glory.
   *
   * @return the ids of the required mini bosses only; eight of them; unmodifiable
   */
  public static Set<String> getRequiredIds() {
    return REQUIRED_IDS;
  }

  /**
   * Counts the bosses that must be defeated for Glory.
   *
   * @return how many mini bosses are required for Glory
   */
  public static int getRequiredCount() {
    return REQUIRED_IDS.size();
  }

  /**
   * Checks whether a kill id belongs to the final boss of the game.
   *
   * @param id any kill id, or null
   * @return true only for {@link #FINAL_BOSS_ID}
   */
  public static boolean isFinalBoss(String id) {
    return FINAL_BOSS_ID.equals(id);
  }
}
