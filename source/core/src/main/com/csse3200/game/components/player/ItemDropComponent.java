package com.csse3200.game.components.player;

import com.csse3200.game.components.Component;
import com.csse3200.game.components.loot.Item;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.factories.LootFactory;
import com.csse3200.game.services.ServiceLocator;

import java.util.function.BiFunction;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles dropping inventory items from the owning entity into the game world.
 */
public class ItemDropComponent extends Component {
    private static final Logger logger = LoggerFactory.getLogger(ItemDropComponent.class);
    private static final float HORIZONTAL_DROP_GAP = 0.25f;

    private final BiFunction<Item, Entity, Entity> lootFactory;
    private final Consumer<Entity> lootSpawner;

    /**
     * Creates a drop handler that spawns loot through the global entity service.
     */
    public ItemDropComponent() {
        this(LootFactory::createDroppedLoot, loot -> ServiceLocator.getEntityService().register(loot));
    }

    /**
     * Creates a drop handler with injectable world dependencies.
     *
     * <p>Package-private for unit tests.
     *
     * @param lootFactory creates a loot entity for an item and its dropping owner
     * @param lootSpawner adds the created loot entity to the world
     */
    ItemDropComponent(BiFunction<Item, Entity, Entity> lootFactory, Consumer<Entity> lootSpawner) {
        if (lootFactory == null || lootSpawner == null) {
            throw new IllegalArgumentException("Loot factory and spawner must not be null.");
        }
        this.lootFactory = lootFactory;
        this.lootSpawner = lootSpawner;
    }

    /**
     * Registers the temporary {@code Q}-drop event handler.
     */
    @Override
    public void create() {
        entity.getEvents().addListener("dropItem", this::dropFirstStack);
    }

    /**
     * Drops the whole stack from the lowest-numbered occupied inventory slot.
     *
     * <p>This is intentionally a small temporary selection rule. It can be replaced with an explicit
     * selected-slot event without changing loot creation or spawning.
     *
     * @return {@code true} when a stack was removed and spawned
     */
    public boolean dropFirstStack() {
        InventoryComponent inventory = entity.getComponent(InventoryComponent.class);
        if (inventory == null) {
            logger.debug("Cannot drop an item, entity has no inventory");
            return false;
        }

        int slot = findFirstOccupiedSlot(inventory);
        if (slot == -1) {
            logger.debug("Cannot drop an item, inventory is empty");
            return false;
        }

        Item item = inventory.getItem(slot);
        Entity loot = lootFactory.apply(item, entity);
        if (loot == null) {
            logger.warn("Loot factory returned null for item {}", item.getName());
            return false;
        }

        Item removed = inventory.removeItem(slot);
        if (removed == null) {
            return false;
        }

        float dropX = entity.getPosition().x + entity.getScale().x + HORIZONTAL_DROP_GAP;
        loot.setPosition(dropX, entity.getPosition().y);

        try {
            lootSpawner.accept(loot);
        } catch (RuntimeException exception) {
            inventory.addItem(removed);
            throw exception;
        }

        logger.info("Dropped {} x{} from slot {}", removed.getName(), removed.getQuantity(), slot);
        entity.getEvents().trigger("itemDropped", removed, loot);
        return true;
    }

    private int findFirstOccupiedSlot(InventoryComponent inventory) {
        for (int slot = 1; slot <= inventory.getMaxSlots(); slot++) {
            if (inventory.containsItem(slot)) {
                return slot;
            }
        }
        return -1;
    }
}
