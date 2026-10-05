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
 * <p>Unlocked/progress/active state persists via Preferences, the same pattern PauseMenuDisplay
 * already uses for audio settings - but only within one playthrough: it survives death/revive (that
 * never touches this class at all), but {@link #resetAll()} is called on every fresh game start
 * (Start or Restart, not Load) - see {@code MainGameScreen}'s constructor.
 *
 * <p>Unlocking a perk (permanent, once earned) is separate from making it active (up to {@value
 * #MAX_ACTIVE_PERKS} at a time, freely toggled via {@link #activate}/{@link #deactivate} - see the
 * perk-selection screen shown after death). Only active perks' rewards actually apply - see e.g.
 * ShieldComponent, which checks {@link Perk#isActive()} when a new player entity is created.
 */
public final class PerkService {
  private static final Logger logger = LoggerFactory.getLogger(PerkService.class);
  private static final String PREFS_NAME = "perks";
  private static final String PREFS_PROGRESS_SUFFIX = ".progress";
  private static final String PREFS_UNLOCKED_SUFFIX = ".unlocked";
  private static final String PREFS_ACTIVE_SUFFIX = ".active";

  /** How many perks the player can have active (equipped) at once. */
  public static final int MAX_ACTIVE_PERKS = 2;

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

  /**
   * @return how many perks are currently active, out of {@value #MAX_ACTIVE_PERKS}
   */
  public static int getActiveCount() {
    int count = 0;
    for (Perk perk : perksById.values()) {
      if (perk.isActive()) {
        count++;
      }
    }
    return count;
  }

  /**
   * Makes an unlocked perk active, if there's room. This is the only place a perk's active state is
   * changed, so the {@value #MAX_ACTIVE_PERKS}-active rule only needs to live here.
   *
   * @param id the perk to activate
   * @return {@code true} if it's now active - {@code false} if the id is unknown, the perk isn't
   *     unlocked yet, it was already active, or {@value #MAX_ACTIVE_PERKS} perks are already active
   */
  public static boolean activate(String id) {
    Perk perk = perksById.get(id);
    if (perk == null || !perk.isUnlocked() || perk.isActive()) {
      return false;
    }
    if (getActiveCount() >= MAX_ACTIVE_PERKS) {
      return false;
    }
    perk.setActive(true);
    saveState(perk);
    perk.notifyActivated();
    return true;
  }

  /**
   * Makes a perk inactive. Safe to call on an already-inactive or unregistered id - a no-op either
   * way.
   *
   * @param id the perk to deactivate
   */
  public static void deactivate(String id) {
    Perk perk = perksById.get(id);
    if (perk == null || !perk.isActive()) {
      return;
    }
    perk.setActive(false);
    saveState(perk);
    perk.notifyDeactivated();
  }

  private static void loadState(Perk perk) {
    if (prefs == null) {
      return;
    }
    int progress = prefs.getInteger(perk.getId() + PREFS_PROGRESS_SUFFIX, 0);
    boolean unlocked = prefs.getBoolean(perk.getId() + PREFS_UNLOCKED_SUFFIX, false);
    boolean active = prefs.getBoolean(perk.getId() + PREFS_ACTIVE_SUFFIX, false);
    perk.restoreState(progress, unlocked, active);
  }

  private static void saveState(Perk perk) {
    if (prefs == null) {
      return;
    }
    prefs.putInteger(perk.getId() + PREFS_PROGRESS_SUFFIX, perk.getProgress());
    prefs.putBoolean(perk.getId() + PREFS_UNLOCKED_SUFFIX, perk.isUnlocked());
    prefs.putBoolean(perk.getId() + PREFS_ACTIVE_SUFFIX, perk.isActive());
    prefs.flush();
  }

  /**
   * Clears every registered perk's progress/unlocked/active state, in memory and on disk. Called on
   * every fresh game start (Start or Restart, not Load) - see {@code MainGameScreen}'s constructor
   * - and usable as a debug/testing hook too.
   */
  public static void resetAll() {
    for (Perk perk : perksById.values()) {
      perk.restoreState(0, false, false);
      if (prefs != null) {
        prefs.remove(perk.getId() + PREFS_PROGRESS_SUFFIX);
        prefs.remove(perk.getId() + PREFS_UNLOCKED_SUFFIX);
        prefs.remove(perk.getId() + PREFS_ACTIVE_SUFFIX);
      }
    }
    if (prefs != null) {
      prefs.flush();
    }
  }

  /**
   * Debug/testing hook - locks a single perk back to zero progress (and inactive), in memory and on
   * disk. Safe to call for an unregistered id - it's simply a no-op.
   *
   * <p>Note this only resets the perk's own tracked state. It does not revert any gameplay reward
   * already applied to a live entity this session (e.g. a shield's extended duration) - that reward
   * was applied directly to that component's state when the perk was active at entity creation, so
   * a fresh player entity (e.g. after a respawn) is needed to see the change reflected.
   *
   * @param id the perk's id, as passed to its {@link Perk} constructor
   */
  public static void resetPerk(String id) {
    Perk perk = perksById.get(id);
    if (perk == null) {
      return;
    }
    perk.restoreState(0, false, false);
    if (prefs != null) {
      prefs.remove(id + PREFS_PROGRESS_SUFFIX);
      prefs.remove(id + PREFS_UNLOCKED_SUFFIX);
      prefs.remove(id + PREFS_ACTIVE_SUFFIX);
      prefs.flush();
    }
  }
}
