package com.csse3200.game.components.player;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.csse3200.game.components.loot.Item;
import com.csse3200.game.components.pet.PetManagerComponent;
import com.csse3200.game.ui.UIComponent;

/** A HUD-style UI component for displaying the player's inventory. */
public class InventoryDisplay extends UIComponent {

  private static final float SLOT_WIDTH = 220f;
  private static final float SLOT_HEIGHT = 36f;
  private static final float SLOT_GAP = 4f;
  private static final float PANEL_PADDING = 10f;
  private static final float RIGHT_MARGIN = 16f;
  private static final String LABEL_STYLE = "default";
  private static final String SLOT_BACKGROUND = "button";

  private static final Color EMPTY_TEXT_COLOR = new Color(1f, 1f, 1f, 0.55f);
  private static final Color FILLED_TEXT_COLOR = Color.WHITE;
  private static final Color ACTIVE_SLOT_COLOR = new Color(1f, 0.8f, 0.3f, 1f);

  private Table inventoryTable;
  private Label goldLabel;

  private boolean hidden = false;

  /** Creates the inventory UI and adds it to the stage. */
  @Override
  public void create() {
    super.create();

    entity.getEvents().addListener("inventoryChanged", this::refreshInventory);
    entity.getEvents().addListener("activeSlotChanged", this::refreshActiveSlot);
    entity.getEvents().addListener("activePetChanged", this::refreshActivePet);

    createInventory();
  }

  /** Creates the inventory panel. */
  private void createInventory() {
    InventoryComponent inventory = entity.getComponent(InventoryComponent.class);

    inventoryTable = new Table();

    inventoryTable.top().center();
    inventoryTable.pad(PANEL_PADDING);
    inventoryTable.setBackground(skin.getDrawable("window"));

    goldLabel = new Label("Gold: " + inventory.getGold(), skin, LABEL_STYLE);

    inventoryTable.add(goldLabel).left().growX().padBottom(8f);
    inventoryTable.row();

    // Item inventory
    Label itemsLabel = new Label("Items", skin, LABEL_STYLE);
    itemsLabel.setColor(FILLED_TEXT_COLOR);

    inventoryTable.add(itemsLabel).left().padBottom(4f);
    inventoryTable.row();

    for (int slotNumber = 1; slotNumber <= inventory.getMaxSlots(); slotNumber++) {
      addSlot(inventory.getItem(slotNumber), slotNumber);
      inventoryTable.row();
    }

    // Separate pet inventory from normal item slots.
    Label petsLabel = new Label("Pets", skin, LABEL_STYLE);
    petsLabel.setColor(FILLED_TEXT_COLOR);

    inventoryTable.add(petsLabel).left().padTop(12f).padBottom(4f);
    inventoryTable.row();

    for (int petSlot = 1; petSlot <= inventory.getPetSlotCount(); petSlot++) {
      addPetSlot(inventory.getPet(petSlot), petSlot);
      inventoryTable.row();
    }

    inventoryTable.pack();

    positionInventory();
    inventoryTable.setVisible(!hidden);
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

    if (isActive) {
      slot.setBackground(skin.newDrawable(SLOT_BACKGROUND, ACTIVE_SLOT_COLOR));
    } else {
      slot.setBackground(skin.getDrawable(SLOT_BACKGROUND));
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

    slot.add(slotNumberLabel).left().padRight(4f).width(14f);
    slot.add(itemLabel).left().width(125f);
    slot.add(quantityLabel).right().padLeft(4f).width(28f);

    inventoryTable.add(slot).size(SLOT_WIDTH, SLOT_HEIGHT).pad(SLOT_GAP);
  }

  /**
   * Adds one pet inventory slot.
   *
   * <p>Pet slots are displayed as keyboard slots 6 and 7 and are independent from the normal item
   * inventory slots.
   *
   * @param pet pet contained in the slot, or null if empty
   * @param petSlot pet inventory slot number
   */
  private void addPetSlot(ShopComponent.Pet pet, int petSlot) {
    Table slot = new Table();

    PetManagerComponent petManager = entity.getComponent(PetManagerComponent.class);

    boolean isActive =
        pet != null
            && petManager != null
            && petManager.getActivePetType() != null
            && petManager.getActivePetType().getName().equals(pet.getName());

    if (isActive) {
      slot.setBackground(skin.newDrawable(SLOT_BACKGROUND, ACTIVE_SLOT_COLOR));
    } else {
      slot.setBackground(skin.getDrawable(SLOT_BACKGROUND));
    }

    slot.pad(4f, 8f, 4f, 8f);

    // Pet slots 1 and 2 correspond to keyboard keys 6 and 7.
    int keyNumber = petSlot + 5;

    Label slotNumberLabel = new Label(keyNumber + ".", skin, LABEL_STYLE);
    slotNumberLabel.setColor(EMPTY_TEXT_COLOR);

    boolean isEmpty = pet == null;
    String petName = isEmpty ? "Empty" : pet.getName();

    Label petLabel = new Label(petName, skin, LABEL_STYLE);
    petLabel.setEllipsis(true);
    petLabel.setColor(isEmpty ? EMPTY_TEXT_COLOR : FILLED_TEXT_COLOR);

    slot.add(slotNumberLabel).left().padRight(4f).width(14f);
    slot.add(petLabel).left().growX();

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

  private void refreshActivePet(ShopComponent.Pet pet) {
    refreshInventory();
  }

  /**
   * Sets inventory to be visible or not. Used by terminal command {@link
   * com.csse3200.game.ui.terminal.commands.InventoryCommand}
   *
   * @param shouldHide boolean on whether to hide the inventory or not.
   */
  public void setHidden(boolean shouldHide) {
    hidden = shouldHide;
    if (inventoryTable != null) {
      inventoryTable.setVisible(!hidden);
      System.out.println("InventoryTable is hidden: " + inventoryTable.isVisible());
    }
  }
}
