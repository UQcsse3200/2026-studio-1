package com.csse3200.game.components.loot;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.csse3200.game.components.Component;
import com.csse3200.game.components.player.InventoryComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.physics.BodyUserData;
import com.csse3200.game.physics.PhysicsLayer;
import com.csse3200.game.physics.components.HitboxComponent;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Allows a loot entity to be picked up by the player.
 *
 * <p>When the player collides with this entity, the loot item is added to the player's inventory.
 * The loot entity is removed after the item is successfully added.
 */
public class LootPickupComponent extends Component {

  private static final Logger logger = LoggerFactory.getLogger(LootPickupComponent.class);

  private final Item item;
  private final Entity pickupBlockedPlayer;
  private final long pickupDelayMillis;
  private HitboxComponent hitboxComponent;
  private GameTime timeSource;
  private long pickupBlockedUntil;
  private boolean collected = false;

  /**
   * Creates a loot pickup component.
   *
   * @param item item that will be added to the player's inventory
   */
  public LootPickupComponent(Item item) {
    this(item, null, 0L);
  }

  /**
   * Creates loot with a temporary pickup block for the entity that dropped it.
   *
   * @param item item that will be added to the player's inventory
   * @param pickupBlockedPlayer player temporarily prevented from collecting the item
   * @param pickupDelayMillis duration of the pickup block in milliseconds
   */
  public LootPickupComponent(Item item, Entity pickupBlockedPlayer, long pickupDelayMillis) {
    this.item = item;
    this.pickupBlockedPlayer = pickupBlockedPlayer;
    this.pickupDelayMillis = Math.max(0L, pickupDelayMillis);
  }

  @Override
  public void create() {
    hitboxComponent = entity.getComponent(HitboxComponent.class);
    timeSource = ServiceLocator.getTimeSource();
    if (pickupBlockedPlayer != null && timeSource != null) {
      pickupBlockedUntil = timeSource.getTime() + pickupDelayMillis;
    }
    entity.getEvents().addListener("collisionStart", this::onCollisionStart);
  }

  private void onCollisionStart(Fixture me, Fixture other) {
    if (collected) {
      return;
    }
    // Only react when this entity's hitbox is involved.
    if (hitboxComponent == null || hitboxComponent.getFixture() != me) {
      return;
    }

    // Only react to collisions with the player.
    if (!PhysicsLayer.contains(PhysicsLayer.PLAYER, other.getFilterData().categoryBits)) {
      return;
    }

    // Get the player entity from the collision fixture.
    BodyUserData userData = (BodyUserData) other.getBody().getUserData();
    if (userData == null || userData.entity == null) {
      return;
    }

    Entity player = userData.entity;
    if (isPickupBlockedFor(player)) {
      return;
    }

    InventoryComponent inventory = player.getComponent(InventoryComponent.class);

    if (inventory == null || item == null) {
      return;
    }

    if (item.getItemType() == ItemType.CURRENCY) {
      int amount = item.getQuantity();
      if (amount <= 0) {
        return;
      }
      inventory.addGold(amount);
      collected = true;
      logger.info("Picked up {}. Gold: {}", item.getName(), inventory.getGold());
      Gdx.app.postRunnable(entity::dispose);
      return;
    }

    // Try to add the item to the player's inventory.
    int remaining = inventory.addItem(item);

    // Only remove the loot if the entire item was added.
    if (remaining == 0) {
      collected = true;

      logger.info(
          "Picked up {}. Inventory quantity: {}", item.getName(), inventory.getTotalQuantity(item));

      Gdx.app.postRunnable(entity::dispose);
    } else {
      // Keep only the quantity that did not fit in the world entity. Without this, leaving and
      // re-entering the pickup would add the already-collected portion again.
      item.setQuantity(remaining);
    }
  }

  private boolean isPickupBlockedFor(Entity player) {
    return player == pickupBlockedPlayer
        && timeSource != null
        && timeSource.getTime() < pickupBlockedUntil;
  }
}
