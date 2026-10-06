package com.csse3200.game.win;

import com.csse3200.game.components.Component;
import com.csse3200.game.entities.Entity;

/**
 * Attached to Zeus in Level 3 and nothing else. When he dies it tells the player entity, once, that
 * the final boss has fallen. The main screen listens for that and shows the win.
 *
 * <p><b>Why it announces on the player.</b> The player is the one entity that exists in every room,
 * and the main screen already holds it, so no screen reference has to be passed to an enemy.
 *
 * <p><b>Why there is no delay in here.</b> {@code EnemyDeathComponent} disposes the enemy straight
 * after its death handling, so a timer on the boss would never finish. A pause before the win
 * screen is the main screen's job (see {@code 06_wiring.md}).
 *
 * <p><b>Style reference:</b> {@code PersistentEnemyIdComponent} (a tiny component attached to an
 * enemy for one job).
 */
public class BossDefeatedWinComponent extends Component {

  /** The event announced on the player when the final boss dies. */
  public static final String FINAL_BOSS_DEFEATED_EVENT = "finalBossDefeated";

  private final Entity player;
  private boolean announced;

  /**
   * @param player the entity to announce on; not null
   * @throws IllegalArgumentException if the player is null
   */
  public BossDefeatedWinComponent(Entity player) {
    if (player == null) {
      throw new IllegalArgumentException("Player must not be null");
    }
    this.player = player;
    announced = false;
  }

  /** Starts listening for this entity's own death. */
  @Override
  public void create() {
    this.getEntity().getEvents().addListener("death", this::onDeath);
  }

  /** Announces the final boss's death on the player, the first time only. */
  private void onDeath() {
    if (hasAnnounced()) {
      return;
    }
    this.announced = true;
    this.player.getEvents().trigger(FINAL_BOSS_DEFEATED_EVENT);
  }

  /**
   * @return true once the death has been announced
   */
  public boolean hasAnnounced() {
    return this.announced;
  }
}
