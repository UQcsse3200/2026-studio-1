package com.csse3200.game.perks;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Central registry and progress tracker for every Perk in the game. Any gameplay system - existing
 * or future - reports progress toward a milestone with a single call:
 *
 * <p>{@code PerkService.recordEvent("dashUsed", 1);}
 *
 * <p>Adding a new perk tied to an EXISTING event key needs zero changes anywhere else - just a new
 * entry in {@link PerkDefinitions}. Adding a perk for a brand-new milestone needs one new
 * recordEvent(...) call at the point that milestone happens (e.g. inside the wall-jump code, once
 * it exists), plus a new PerkDefinitions entry - nothing about this class changes either way.
 *
 * <p>Unlocked/in-progress state persists forever via Preferences (achievement-style), the same
 * pattern PauseMenuDisplay already uses for audio settings.
 */
public final class PerkService {
  private static final Logger logger = LoggerFactory.getLogger(PerkService.class);
  private static final String PREFS_NAME = "perks";

  private static final Map<String, Perk> perksById = new LinkedHashMap<>();
  private static final Preferences prefs =
      (Gdx.app != null) ? Gdx.app.getPreferences(PREFS_NAME) : null;

  private PerkService() {}

  /**
   * Registers a perk definition and loads any previously-saved progress for it. Call once per perk
   * (see {@link PerkDefinitions#registerAll()}) before any recordEvent(...) calls for it will have
   * any effect. Safe to call again for the same id - re-registering just reloads saved state, it
   * doesn't duplicate the perk.
   */
  public static void register(Perk perk) {
    perksById.put(perk.getId(), perk);
    loadState(perk);
  }

  /**
   * Reports progress toward every registered perk listening for this event key. Safe to call for a
   * key with no matching perks - it's simply a no-op, so gameplay code never needs to check whether
   * a perk exists before calling this.
   */
  public static void recordEvent(String eventKey, int amount) {
    for (Perk perk : perksById.values()) {
      if (perk.getEventKey().equals(eventKey)) {
        boolean justUnlocked = perk.recordProgress(amount);
        saveState(perk);
        if (justUnlocked) {
          logger.info("Perk unlocked: {}", perk.getName());
        }
      }
    }
  }

  public static List<Perk> getAllPerks() {
    return new ArrayList<>(perksById.values());
  }

  public static Perk getPerk(String id) {
    return perksById.get(id);
  }

  private static void loadState(Perk perk) {
    if (prefs == null) {
      return;
    }
    int progress = prefs.getInteger(perk.getId() + ".progress", 0);
    boolean unlocked = prefs.getBoolean(perk.getId() + ".unlocked", false);
    perk.restoreState(progress, unlocked);
  }

  private static void saveState(Perk perk) {
    if (prefs == null) {
      return;
    }
    prefs.putInteger(perk.getId() + ".progress", perk.getProgress());
    prefs.putBoolean(perk.getId() + ".unlocked", perk.isUnlocked());
    prefs.flush();
  }

  /** Debug/testing hook - clears every registered perk's progress, in memory and on disk. */
  public static void resetAll() {
    for (Perk perk : perksById.values()) {
      perk.restoreState(0, false);
      if (prefs != null) {
        prefs.remove(perk.getId() + ".progress");
        prefs.remove(perk.getId() + ".unlocked");
      }
    }
    if (prefs != null) {
      prefs.flush();
    }
  }
}
