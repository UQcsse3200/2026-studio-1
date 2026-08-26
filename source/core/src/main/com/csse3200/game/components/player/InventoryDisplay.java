package com.csse3200.game.components.player;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.csse3200.game.components.loot.Item;
import com.csse3200.game.ui.UIComponent;

/** A modern HUD-style UI component for displaying the player's inventory. */
public class InventoryDisplay extends UIComponent {
  private static final int COLUMNS = 5;

  private static final float SLOT_SIZE = 92f;
  private static final float SLOT_GAP = 6f;

  private static final float PANEL_PADDING = 16f;
  private static final float HEADER_PADDING = 8f;

  private Table inventoryTable;

  /** Creates the inventory UI and adds it to the stage. */
  @Override
  public void create() {
    super.create();
    createInventory();
  }

  /** Creates the inventory panel. */
  private void createInventory() {
    InventoryComponent inventory = entity.getComponent(InventoryComponent.class);

    inventoryTable = new Table();
    inventoryTable.top().center();

    // Main inventory panel.
    inventoryTable.setBackground(skin.getDrawable("window-w"));

    //    // Header.
    //    Label title = new Label("INVENTORY", skin, "title");

    //
    // inventoryTable.add(title).colspan(COLUMNS).padTop(PANEL_PADDING).padBottom(HEADER_PADDING);

    inventoryTable.row();

    // Inventory slots.
    for (int slot = 1; slot <= inventory.getMaxSlots(); slot++) {
      addSlot(inventory.getItem(slot), slot);

      if (slot % COLUMNS == 0) {
        inventoryTable.row();
      }
    }

    inventoryTable.pack();

    positionInventory();

    stage.addActor(inventoryTable);
  }

  /**
   * Adds one inventory slot.
   *
   * @param item item contained in the slot, or null if empty
   * @param slotNumber slot number
   */
  private void addSlot(Item item, int slotNumber) {
    Table slot = new Table();

    // Slot background.
    slot.setBackground(skin.getDrawable("button-c"));

    /*
     * Slot number.
     *
     * Example:
     *
     * ┌──────────┐
     * │ 1        │
     * │          │
     * │   Empty  │
     * │       x1 │
     * └──────────┘
     */
    Label slotNumberLabel = new Label(String.valueOf(slotNumber), skin, "small");

    String itemName = item == null ? "EMPTY" : item.getName();
    Label itemLabel = new Label(itemName, skin, "small");

    String quantity = item == null ? "" : "x" + item.getQuantity();
    Label quantityLabel = new Label(quantity, skin, "small");

    // Top-left: slot number.
    slot.add(slotNumberLabel).top().left().padTop(6f).padLeft(7f);

    // Make the remaining width available.
    slot.add().expandX();

    slot.row();

    // Center: item placeholder.
    slot.add(itemLabel).colspan(2).center().expand().pad(4f);

    slot.row();

    // Bottom-right: quantity.
    slot.add().expandX();

    slot.add(quantityLabel).bottom().right().padBottom(6f).padRight(7f);

    inventoryTable.add(slot).size(SLOT_SIZE).pad(SLOT_GAP);
  }

  /** Positions the inventory in the centre of the screen. */
  private void positionInventory() {
    float screenWidth = stage.getViewport().getWorldWidth();
    float screenHeight = stage.getViewport().getWorldHeight();

    inventoryTable.setPosition(
        (screenWidth - inventoryTable.getWidth()) / 2f, (inventoryTable.getHeight()) / 4f);
  }

  @Override
  public void draw(SpriteBatch batch) {
    // Drawing is handled by the Scene2D stage.
  }

  @Override
  public void dispose() {
    if (inventoryTable != null) {
      inventoryTable.remove();
      inventoryTable = null;
    }

    super.dispose();
  }
}
