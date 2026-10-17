package com.csse3200.game.perks.tortoises;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.components.Component;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.services.ServiceLocator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Makes the enemy it is attached to leave a hidden tortoise behind when it dies.
 *
 * <p>A tortoise is a world pickup, not an inventory item, so it cannot go through {@code
 * ItemDropComponent}. This component listens for the enemy's own {@code "death"} event instead,
 * reads where the enemy stood, and spawns {@link TortoiseFactory#createTortoise(String)} there. It
 * does not touch {@code EnemyDeathComponent}: that component disposes the enemy on a later frame
 * (it posts a runnable), so the position is still readable when this listener runs.
 *
 * <p>A tortoise that has already been found is never dropped again, and a second {@code "death"}
 * event on the same enemy is ignored.
 */
public class TortoiseDropComponent extends Component {
  private static final Logger logger = LoggerFactory.getLogger(TortoiseDropComponent.class);

  /** The enemy event this component listens for. */
  private static final String DEATH_EVENT = "death";

  private final String tortoiseId;
  private boolean dropped = false;

  /**
   * @param tortoiseId stable id of the tortoise this enemy drops, for example {@code
   *     "zeus_tortoise"}; each tortoise in the game needs its own distinct id
   * @throws IllegalArgumentException if the id is null or blank
   */
  public TortoiseDropComponent(String tortoiseId) {
    if (tortoiseId == null || tortoiseId.isBlank()) {
      throw new IllegalArgumentException("tortoiseID must not be blank.");
    }
    this.tortoiseId = tortoiseId;
  }

  /**
   * @return the id of the tortoise this enemy drops
   */
  public String getTortoiseId() {
    return tortoiseId;
  }

  /** Listens for the enemy's {@code DEATH_EVENT}. */
  @Override
  public void create() {
    entity.getEvents().addListener(DEATH_EVENT, this::onDeath);
  }

  /** Handles the enemy's {@code "death"} event: spawns the tortoise once, if not yet found. */
  private void onDeath() {
    if (dropped) {
      return;
    }
    dropped = true;
    if (TortoiseFactory.isFound(getTortoiseId())) {
      logger.info(
          "Tortoise: {} has already been found. No dropping required.", this.getTortoiseId());
      return;
    }
    Vector2 centre = entity.getCenterPosition();
    Vector2 feetPos = new Vector2(centre.x, entity.getPosition().y);
    runOnRenderThread(() -> spawnAt(getTortoiseId(), feetPos));
  }

  /**
   * Spawns one tortoise into the running game.
   *
   * @param tortoiseId the tortoise's stable id
   * @param position world position for the tortoise's bottom-left corner
   * @return the spawned tortoise
   */
  static Entity spawnAt(String tortoiseId, Vector2 position) {
    Entity tortoise = TortoiseFactory.createTortoise(tortoiseId);
    tortoise.setPosition(position);
    ServiceLocator.getEntityService().register(tortoise);
    logger.info("{} was dropped at position {}", tortoise, tortoise.getPosition());
    return tortoise;
  }

  /**
   * Runs a task on the render thread, or straight away when there is no running game (tests).
   *
   * @param task the work to run
   */
  private static void runOnRenderThread(Runnable task) {
    if (Gdx.app != null) {
      Gdx.app.postRunnable(task);
    } else {
      task.run();
    }
  }
}
