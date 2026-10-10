package com.csse3200.game.perks;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.csse3200.game.components.Component;
import com.csse3200.game.physics.PhysicsLayer;
import com.csse3200.game.physics.components.HitboxComponent;
import com.csse3200.game.win.TortoiseLedger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A one-off hidden collectible for the "Time Freeze" perk ({@code timeLord} in {@link
 * PerkDefinitions}). When the player touches this entity, it marks itself found (forever, via
 * {@link TortoiseRegistry}) and reports one unit of progress toward that perk via {@link
 * PerkService#recordEvent}, then removes itself.
 *
 * <p>Deliberately does not use the loot/inventory system at all ({@code LootPickupComponent},
 * {@code Item}, etc.) - a tortoise isn't an item the player carries, it's purely a perk milestone
 * trigger, so this stays self-contained to the perks package rather than pulling in that system.
 */
public class TortoiseComponent extends Component {
  private static final Logger logger = LoggerFactory.getLogger(TortoiseComponent.class);
  private static final String EVENT_KEY = "Tortoise";

  private final String tortoiseId;
  private HitboxComponent hitboxComponent;
  private boolean collected = false;

  /**
   * @param tortoiseId stable id for this specific tortoise (e.g. {@code "level1_a"}), used so it
   *     stays found forever once collected - see {@link TortoiseRegistry}
   */
  public TortoiseComponent(String tortoiseId) {
    this.tortoiseId = tortoiseId;
  }

  @Override
  public void create() {
    hitboxComponent = entity.getComponent(HitboxComponent.class);
    entity.getEvents().addListener("collisionStart", this::onCollisionStart);
  }

  private void onCollisionStart(Fixture me, Fixture other) {
    if (collected || !isOwnHitbox(me) || !isPlayerFixture(other)) {
      return;
    }

    collected = true;
    TortoiseRegistry.markFound(tortoiseId);
    TortoiseLedger.recordFound(tortoiseId);
    logger.info("Tortoise \"{}\" found!", tortoiseId);
    PerkService.recordEvent(EVENT_KEY, 1);
    Gdx.app.postRunnable(entity::dispose);
  }

  private boolean isOwnHitbox(Fixture me) {
    return hitboxComponent != null && hitboxComponent.getFixture() == me;
  }

  private boolean isPlayerFixture(Fixture other) {
    return PhysicsLayer.contains(PhysicsLayer.PLAYER, other.getFilterData().categoryBits);
  }
}
