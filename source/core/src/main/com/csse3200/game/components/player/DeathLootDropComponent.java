package com.csse3200.game.components.player;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.components.Component;
import com.csse3200.game.components.loot.Item;
import com.csse3200.game.components.loot.ItemType;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.factories.LootFactory;
import com.csse3200.game.services.ServiceLocator;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Drops everything the hero is carrying at the spot where the hero dies.
 *
 * <p>This is the loot half of Hero Revival: when the hero dies, every inventory slot and all of the
 * hero's gold become loot on the ground at the death spot, and the inventory is emptied so nothing
 * exists both on the ground and in an inventory. Loot entities never despawn, so the next hero can
 * travel back and collect it.
 *
 * <p>The "death" event can fire more than once for a single death ({@code CombatStatsComponent}
 * fires it on every hit at zero health, and {@link DeathStateComponent} fires it again), so the
 * drop only ever happens once per hero.
 *
 * <p>The drop is not done straight away. Death can be triggered from inside a Box2D collision
 * callback (an enemy's touch attack, for example), and the physics world is locked during that
 * callback, so creating the loot's physics bodies there would fail. Instead the drop is scheduled
 * to run after the current frame, once the physics step has finished.
 *
 * <p>Note for the revival flow: the dropped loot overlaps the dead hero's body. The dead hero must
 * be removed (or disposed) before physics starts again, otherwise it could collect its own loot.
 */
public class DeathLootDropComponent extends Component {
  private static final Logger logger = LoggerFactory.getLogger(DeathLootDropComponent.class);

  /** Horizontal gap between loot piles, in world units, so they do not all sit on one spot. */
  private static final float PILE_GAP = 0.4f;

  private final BiFunction<Item, Entity, Entity> lootFactory;
  private final Consumer<Entity> lootSpawner;
  private final Consumer<Runnable> scheduler;
  private boolean dropScheduled = false;
  private boolean hasDropped = false;

  /**
   * Creates a component that drops loot through {@link LootFactory} into the entity service, after
   * the current frame has finished.
   */
  public DeathLootDropComponent() {
    this(
        LootFactory::createDroppedLoot,
        loot -> ServiceLocator.getEntityService().register(loot),
        runnable -> Gdx.app.postRunnable(runnable));
  }

  /**
   * Creates a component with a custom loot factory and spawner, so tests can check what is dropped
   * without needing textures or physics.
   *
   * @param lootFactory creates a loot entity from an item and the entity dropping it
   * @param lootSpawner adds a loot entity to the game world
   * @param scheduler runs the drop later; the game passes {@code Gdx.app.postRunnable}, tests can
   *     pass {@code Runnable::run} to drop straight away
   */
  DeathLootDropComponent(
      BiFunction<Item, Entity, Entity> lootFactory,
      Consumer<Entity> lootSpawner,
      Consumer<Runnable> scheduler) {
    if (lootFactory == null || lootSpawner == null || scheduler == null) {
      throw new IllegalArgumentException("Loot factory, spawner and scheduler must not be null.");
    }
    this.lootFactory = lootFactory;
    this.lootSpawner = lootSpawner;
    this.scheduler = scheduler;
  }

  @Override
  public void create() {
    entity.getEvents().addListener("death", this::onDeath);
  }

  /**
   * Schedules the drop for after the current frame. Repeated death events are ignored once a drop
   * is scheduled.
   */
  private void onDeath() {
    if (dropScheduled) {
      return;
    }
    dropScheduled = true;
    scheduler.accept(this::dropEverything);
  }

  /**
   * Drops every item and all gold at the hero's current position and empties the inventory.
   *
   * <p>Only the first call does anything. Later calls return 0, so a repeated death event cannot
   * drop the same items twice.
   *
   * @return the number of loot piles dropped
   */
  public int dropEverything() {
    if (hasDropped) {
      return 0;
    }
    hasDropped = true;

    InventoryComponent inventory = entity.getComponent(InventoryComponent.class);
    if (inventory == null) {
      logger.debug("Hero died without an inventory, nothing to drop");
      return 0;
    }

    List<Item> itemsToDrop = takeEverything(inventory);

    // Lay the piles out in a short row centred on the death spot, so they are easy to see and all
    // still sit where the hero died.
    Vector2 deathSpot = entity.getPosition();
    float firstX = deathSpot.x - (itemsToDrop.size() - 1) * PILE_GAP / 2f;

    for (int i = 0; i < itemsToDrop.size(); i++) {
      Entity loot = lootFactory.apply(itemsToDrop.get(i), entity);
      loot.setPosition(firstX + i * PILE_GAP, deathSpot.y);
      lootSpawner.accept(loot);
    }

    logger.info(
        "Hero died and dropped {} loot piles at ({}, {})",
        itemsToDrop.size(),
        deathSpot.x,
        deathSpot.y);
    entity.getEvents().trigger("deathLootDropped", itemsToDrop.size());
    return itemsToDrop.size();
  }

  /**
   * Removes every item and all gold from the inventory and returns them as items to drop.
   *
   * @param inventory the dead hero's inventory
   * @return the items that were removed, with gold (if any) as the last item
   */
  private List<Item> takeEverything(InventoryComponent inventory) {
    List<Item> items = new ArrayList<>();

    for (int slot = 1; slot <= inventory.getMaxSlots(); slot++) {
      Item item = inventory.removeItem(slot);
      if (item != null) {
        items.add(item);
      }
    }

    int gold = inventory.getGold();
    if (gold > 0) {
      // The max stack size is the gold amount itself. A fixed cap (such as 99) would silently cut a
      // large purse down, and the extra gold would be lost.
      items.add(new Item("Gold", ItemType.CURRENCY, gold, gold));
      inventory.setGold(0);
    }

    return items;
  }
}
