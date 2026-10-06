package com.csse3200.game.win;

/**
 * How well the player won. There are three levels of winning, and a fourth value for "has not won".
 * The tier is worked out once, when Zeus falls, by {@link WinEvaluator}.
 *
 * <p><b>The ladder:</b>
 *
 * <ul>
 *   <li>{@link #NONE} (level 0): the final boss has not been defeated, so there is no win
 *   <li>{@link #VICTORY} (level 1): Zeus in his palace has been defeated, nothing more
 *   <li>{@link #GLORY} (level 2): Victory, and every required mini boss in the side rooms has also
 *       been defeated
 *   <li>{@link #LEGEND} (level 3): Glory, and enough quests have also been completed
 * </ul>
 *
 * <p>Each tier includes everything below it, so a higher tier is never reached by skipping a lower
 * one. The levels are stable numbers (0 to 3) because they are shown as stars and may be saved.
 *
 * <p><b>Limitations:</b> the tier says nothing about how the player won (deaths, time, difficulty).
 * Adding a fourth level later means a new value here, a new rule in {@link WinEvaluator} and a new
 * title; nothing else needs to change.
 *
 * <p><b>Style reference:</b> {@code WeaponTier} (an enum with a numeric level and a lookup).
 */
public enum WinTier {
  NONE(0, "No victory yet", ""),
  VICTORY(1, "Victory", "Zeus has fallen."),
  GLORY(2, "Glory", "Every guardian has fallen."),
  LEGEND(3, "Legend", "Every guardian has fallen and every quest is done.");

  private final int level;
  private final String title;
  private final String subtitle;

  WinTier(int level, String title, String subtitle) {
    this.level = level;
    this.title = title;
    this.subtitle = subtitle;
  }

  /**
   * @return the numeric level, 0 for {@link #NONE} up to 3 for {@link #LEGEND}
   */
  public int getLevel() {
    return level;
  }

  /**
   * @return the short title shown on the win screen; never blank
   */
  public String getTitle() {
    return this.title;
  }

  /**
   * @return the one line explanation shown under the title; blank only for {@link #NONE}
   */
  public String getSubtitle() {
    return this.subtitle;
  }

  /**
   * @return true for every tier that counts as a win, which is every tier except {@link #NONE}
   */
  public boolean isWin() {
    return level >= 1;
  }

  /**
   * Compares two tiers by level.
   *
   * @param otherTier the tier to compare with; not null
   * @return true if this tier is the same as or higher than {@code other}
   * @throws IllegalArgumentException if {@code other} is null
   */
  public boolean isAtLeast(WinTier otherTier) {
    if (otherTier == null) {
      throw new IllegalArgumentException("The other win tier must not be null.");
    }
    return this.getLevel() >= otherTier.getLevel();
  }

  /**
   * Looks a tier up by its numeric level, for example when reading a saved value.
   *
   * @param level 0 to 3
   * @return the matching tier
   * @throws IllegalArgumentException if the level is below 0 or above 3
   */
  public static WinTier fromLevel(int level) {
    return switch (level) {
      case 0 -> NONE;
      case 1 -> VICTORY;
      case 2 -> GLORY;
      case 3 -> LEGEND;
      default -> throw new IllegalArgumentException("No win tier has level " + level);
    };
  }
}
