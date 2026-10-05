package com.csse3200.game.perks;

import com.csse3200.game.ai.tasks.AITaskComponent;
import com.csse3200.game.components.Component;
import com.csse3200.game.components.attacks.MeleeAttackComponent;
import com.csse3200.game.components.attacks.RangedAttackComponent;
import com.csse3200.game.components.attacks.TouchAttackComponent;
import com.csse3200.game.components.npc.EnemyDeathComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.physics.components.PhysicsMovementComponent;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The "Time Freeze" ability, granted by the {@code timeLord} perk. Pressing the Time Freeze key
 * (which fires {@code "activateTimeFreeze"} on the player) stops every enemy for a few seconds
 * while the player keeps moving and fighting normally - frozen enemies can still be hit and killed.
 *
 * <p>This is an ability, not a stat bonus, so unlike {@code ShieldComponent} there is nothing to
 * apply or revert when the perk is toggled: the perk just has to be <em>active</em> at the moment
 * the key is pressed. Deactivating it mid-freeze ends the freeze early.
 *
 * <p>An enemy is "frozen" by disabling the components that make it move or hurt the player (see
 * {@link #FROZEN_BEHAVIOURS}). Its stats, collider, hitbox, drops and rendering are never touched,
 * which is what keeps it killable. Timing uses {@code GameTime.getDeltaTime()}, which is scaled by
 * the game's time scale, so the freeze does not tick down while the game is paused or the death
 * screen is up.
 *
 * <p>Known limits: projectiles an enemy had already fired keep flying, and enemy animations keep
 * playing (they just stay in place).
 */
public class TimeFreezeComponent extends Component {
  private static final Logger logger = LoggerFactory.getLogger(TimeFreezeComponent.class);
  private static final String PERK_ID = "timeLord";

  /** How long enemies stay frozen, in seconds of game time. */
  private static final float FREEZE_DURATION_SECONDS = 5f;

  /**
   * Recharge time after a freeze ends, in seconds. Without one, pressing the key every 5 seconds
   * would keep every enemy frozen forever. Set to 0 to remove the limit.
   */
  private static final float COOLDOWN_SECONDS = 20f;

  /**
   * The components that make an enemy act: think (AI), walk, melee, shoot, and hurt on contact.
   * Disabled while frozen, re-enabled afterwards.
   */
  private static final List<Class<? extends Component>> FROZEN_BEHAVIOURS =
      List.of(
          AITaskComponent.class,
          PhysicsMovementComponent.class,
          MeleeAttackComponent.class,
          RangedAttackComponent.class,
          TouchAttackComponent.class);

  private final Set<Entity> frozenEnemies = Collections.newSetFromMap(new IdentityHashMap<>());
  private boolean freezing = false;
  private float freezeRemaining;
  private float cooldownRemaining;

  @Override
  public void create() {
    entity.getEvents().addListener("activateTimeFreeze", this::tryActivate);

    Perk timeLordPerk = PerkService.getPerk(PERK_ID);
    if (timeLordPerk != null) {
      timeLordPerk.setOnDeactivated(this::endFreeze);
    }
  }

  @Override
  public void update() {
    GameTime timeSource = ServiceLocator.getTimeSource();
    if (timeSource == null) {
      return;
    }
    float delta = timeSource.getDeltaTime();

    if (freezing) {
      // Re-sweep each frame so an enemy that appears mid-freeze (e.g. after a room transition) is
      // frozen too; already-frozen enemies are skipped.
      freezeAllEnemies();
      freezeRemaining -= delta;
      if (freezeRemaining <= 0f) {
        endFreeze();
      }
    } else if (cooldownRemaining > 0f) {
      cooldownRemaining -= delta;
    }
  }

  private void tryActivate() {
    Perk timeLordPerk = PerkService.getPerk(PERK_ID);
    if (timeLordPerk == null || !timeLordPerk.isActive()) {
      logger.debug("Time Freeze pressed but the Time Freeze perk isn't active, ignoring");
      return;
    }
    if (freezing) {
      return;
    }
    if (cooldownRemaining > 0f) {
      logger.info("Time Freeze recharging, {}s left", Math.ceil(cooldownRemaining));
      return;
    }

    freezing = true;
    freezeRemaining = FREEZE_DURATION_SECONDS;
    freezeAllEnemies();
    logger.info("Time Freeze activated for {}s", FREEZE_DURATION_SECONDS);
    entity.getEvents().trigger("timeFreezeActivated", FREEZE_DURATION_SECONDS);
  }

  private void freezeAllEnemies() {
    List<Entity> enemies =
        ServiceLocator.getEntityService().getEntitiesWithComponent(EnemyDeathComponent.class);
    for (Entity enemy : enemies) {
      if (enemy.isDisposed() || !frozenEnemies.add(enemy)) {
        continue;
      }
      setBehavioursEnabled(enemy, false);

      // The movement component pushes an enemy every frame, so also kill any momentum it already
      // has - otherwise an enemy mid-lunge would keep sliding. Gravity still applies.
      PhysicsComponent physics = enemy.getComponent(PhysicsComponent.class);
      if (physics != null && physics.getBody() != null) {
        physics.getBody().setLinearVelocity(0f, 0f);
      }
    }
  }

  /** Ends the freeze, thaws every enemy and starts the cooldown. */
  private void endFreeze() {
    if (!freezing) {
      return;
    }
    freezing = false;
    releaseEnemies();
    cooldownRemaining = COOLDOWN_SECONDS;
    logger.info("Time Freeze ended");
    entity.getEvents().trigger("timeFreezeEnded");
  }

  private void releaseEnemies() {
    for (Entity enemy : frozenEnemies) {
      if (!enemy.isDisposed()) {
        setBehavioursEnabled(enemy, true);
      }
    }
    frozenEnemies.clear();
  }

  private static void setBehavioursEnabled(Entity enemy, boolean enabled) {
    for (Class<? extends Component> type : FROZEN_BEHAVIOURS) {
      Component behaviour = enemy.getComponent(type);
      if (behaviour != null) {
        behaviour.setEnabled(enabled);
      }
    }
  }

  /**
   * If the player dies mid-freeze this component is disposed with them, so it must thaw the enemies
   * itself - nothing else would ever re-enable them.
   */
  @Override
  public void dispose() {
    releaseEnemies();
    super.dispose();
  }
}
