package com.csse3200.game.components.loot;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.csse3200.game.components.Component;
import com.csse3200.game.components.player.InventoryComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.physics.BodyUserData;
import com.csse3200.game.physics.PhysicsLayer;
import com.csse3200.game.physics.components.HitboxComponent;
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
  private HitboxComponent hitboxComponent;
  private boolean collected = false;

  /**
   * Creates a loot pickup component.
   *
   * @param item item that will be added to the player's inventory
   */
  public LootPickupComponent(Item item) {
    this.item = item;
  }

  @Override
  public void create() {
    hitboxComponent = entity.getComponent(HitboxComponent.class);
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
    }
  }
}
