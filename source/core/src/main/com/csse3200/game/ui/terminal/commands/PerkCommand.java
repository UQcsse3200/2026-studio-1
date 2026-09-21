package com.csse3200.game.ui.terminal.commands;

// NEW FILE: debug terminal command for testing perk rewards (Iron Skin, Shield Master, etc.)
// without grinding the real milestone or waiting on the quest-tracker UI integration.

import com.csse3200.game.perks.Perk;
import com.csse3200.game.perks.PerkService;
import java.util.ArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Debug terminal command to instantly unlock a perk by id, or lock it back to zero progress.
 *
 * <p>Unlocking is not a shortcut around the perk system - it drives the perk's real progress
 * straight to its threshold via {@link PerkService#recordEvent}, so it triggers exactly the same
 * {@code onUnlocked} path a real playthrough would.
 *
 * <p>Usage in the debug terminal:
 *
 * <ul>
 *   <li>{@code "perk <id>"} - force-unlocks that perk, e.g. {@code "perk iron_skin"}
 *   <li>{@code "perk <id> reset"} - locks that perk back to zero progress, e.g. {@code "perk
 *       shield_master reset"}
 * </ul>
 */
public class PerkCommand implements Command {
  private static final Logger logger = LoggerFactory.getLogger(PerkCommand.class);
  private static final String RESET_ARG = "reset";

  @Override
  public boolean action(ArrayList<String> args) {
    if (args.isEmpty()) {
      logger.error("Usage: perk <id> [reset], e.g. \"perk iron_skin\" or \"perk iron_skin reset\"");
      return false;
    }

    String id = args.get(0);

    if (args.size() > 1 && RESET_ARG.equalsIgnoreCase(args.get(1))) {
      return resetPerk(id);
    }

    return unlockPerk(id);
  }

  private boolean unlockPerk(String id) {
    Perk perk = PerkService.getPerk(id);
    if (perk == null) {
      logger.error("No perk registered with id \"{}\"", id);
      return false;
    }

    if (perk.isUnlocked()) {
      logger.info("Perk \"{}\" is already unlocked", id);
      return true;
    }

    int remaining = perk.getThreshold() - perk.getProgress();
    PerkService.recordEvent(perk.getEventKey(), remaining); // drives real progress to threshold
    logger.info("Forced perk \"{}\" to unlock", id);
    return true;
  }

  private boolean resetPerk(String id) {
    if (PerkService.getPerk(id) == null) {
      logger.error("No perk registered with id \"{}\"", id);
      return false;
    }

    PerkService.resetPerk(id);
    logger.info("Perk \"{}\" reset to zero progress", id);
    return true;
  }
}
