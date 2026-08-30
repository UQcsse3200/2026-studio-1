package com.csse3200.game.components.player;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.csse3200.game.components.loot.Item;
import com.csse3200.game.ui.UIComponent;

/** A HUD-style UI component for displaying the player's inventory. */
public class InventoryDisplay extends UIComponent {

  private static final float SLOT_WIDTH = 130f;
  private static final float SLOT_HEIGHT = 36f;
  private static final float SLOT_GAP = 4f;
  private static final float PANEL_PADDING = 10f;
  private static final float RIGHT_MARGIN = 16f;
  private static final String LABEL_STYLE = "small";

  private static final Color EMPTY_TEXT_COLOR = new Color(1f, 1f, 1f, 0.55f);
  private static final Color FILLED_TEXT_COLOR = Color.WHITE;
  private static final Color ACTIVE_SLOT_COLOR = new Color(1f, 0.8f, 0.3f, 1f);

  private Table inventoryTable;
  private Label goldLabel;

  /** Creates the inventory UI and adds it to the stage. */
  @Override
  public void create() {
    super.create();

    entity.getEvents().addListener("inventoryChanged", this::refreshInventory);
    entity.getEvents().addListener("activeSlotChanged", this::refreshActiveSlot);

    createInventory();
  }

  /** Creates the inventory panel. */
  private void createInventory() {
    InventoryComponent inventory = entity.getComponent(InventoryComponent.class);

    inventoryTable = new Table();

    inventoryTable.top().center();
    inventoryTable.pad(PANEL_PADDING);
    inventoryTable.setBackground(skin.getDrawable("window-w"));

    goldLabel = new Label("Gold: " + inventory.getGold(), skin, LABEL_STYLE);

    inventoryTable.add(goldLabel).left().growX().padBottom(6f);

    inventoryTable.row();

    for (int slotNumber = 1; slotNumber <= inventory.getMaxSlots(); slotNumber++) {
      addSlot(inventory.getItem(slotNumber), slotNumber);
      inventoryTable.row();
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

    InventoryComponent inventory = entity.getComponent(InventoryComponent.class);

    boolean isActive = inventory.getActiveSlot() == slotNumber;

    slot.setBackground(skin.getDrawable("button-c"));

    if (isActive) {
      slot.setBackground(skin.newDrawable("button-c", ACTIVE_SLOT_COLOR));
    } else {
      slot.setBackground(skin.getDrawable("button-c"));
    }

    slot.pad(4f, 8f, 4f, 8f);

    Label slotNumberLabel = new Label(slotNumber + ".", skin, LABEL_STYLE);
    slotNumberLabel.setColor(EMPTY_TEXT_COLOR);

    boolean isEmpty = item == null;
    String itemName = isEmpty ? "Empty" : item.getName();

    Label itemLabel = new Label(itemName, skin, LABEL_STYLE);
    itemLabel.setEllipsis(true);
    itemLabel.setColor(isEmpty ? EMPTY_TEXT_COLOR : FILLED_TEXT_COLOR);

    String quantity = isEmpty ? "" : "x" + item.getQuantity();

    Label quantityLabel = new Label(quantity, skin, LABEL_STYLE);
    quantityLabel.setColor(FILLED_TEXT_COLOR);

    slot.add(slotNumberLabel).left().padRight(6f).width(14f);
    slot.add(itemLabel).left().expandX().fillX();
    slot.add(quantityLabel).right().padLeft(6f);

    inventoryTable.add(slot).size(SLOT_WIDTH, SLOT_HEIGHT).pad(SLOT_GAP);
  }

  /**
   * Refreshes the inventory UI when the inventory changes.
   *
   * <p>This updates both gold and item quantities.
   */
  private void refreshInventory() {
    if (inventoryTable != null) {
      inventoryTable.remove();
      inventoryTable = null;
    }

    goldLabel = null;

    createInventory();
  }

  /** Positions the inventory on the right side of the screen. */
  private void positionInventory() {
    float screenWidth = stage.getViewport().getWorldWidth();
    float screenHeight = stage.getViewport().getWorldHeight();

    float x = screenWidth - inventoryTable.getWidth() - RIGHT_MARGIN;
    float y = (screenHeight - inventoryTable.getHeight()) / 2f;

    inventoryTable.setPosition(x, y);
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

    goldLabel = null;

    super.dispose();
  }

  private void refreshActiveSlot(int activeSlot) {
    refreshInventory();
  }
}
