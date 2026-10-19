package com.csse3200.game.components.player;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Scaling;
import com.csse3200.game.components.loot.Item;
import com.csse3200.game.components.pet.PetManagerComponent;
import com.csse3200.game.pausemenu.PauseMenuComponent;
import com.csse3200.game.ui.UIComponent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A Minecraft-style HUD for the player's inventory.
 *
 * <p>The inventory is drawn as a hotbar docked at the bottom centre of the screen:
 *
 * <ul>
 *   <li>A row of item slots (keys 1..N) with a selection frame on the active slot.
 *   <li>A separate pet panel (keys 6 and 7) with purple-tinted slots; the active pet gets a gold
 *       selection frame.
 *   <li>A gold counter with a coin icon above the hotbar.
 *   <li>Stack counts in the bottom-right corner of a slot (hidden for single items, exactly like
 *       Minecraft) and a fading item-name tooltip above the hotbar when the active slot changes.
 * </ul>
 *
 * <p>Slots are clickable: clicking an item slot selects it as the active slot, and clicking an
 * owned pet slot switches the active pet. Clickable slots light up while hovered. Clicks are
 * ignored while the game is paused.
 *
 * <p>Artwork is loaded from {@code source/core/assets/images/icons/} through {@link
 * #loadIcon(String)} (item icons, coin, empty/normal/selected slot frames). Anything whose PNG is
 * missing falls back to a texture generated at runtime as tiny pixel art, so the HUD always
 * renders.
 */
public class InventoryDisplay extends UIComponent {
  // --- Pixel-art geometry --------------------------------------------------------------------
  // Every generated texture pixel is drawn as UI_SCALE screen pixels.
  private static final float UI_SCALE = 4f;

  private static final int SLOT_PX = 18;
  private static final int ICON_PX = 12;
  private static final int COIN_PX = 10;
  private static final int PANEL_PX = 10;
  private static final int PANEL_BORDER_PX = 3;

  // Legacy gold slot frame (source/core/assets/images/hotbar_frame_40.png). Only used as a
  // fallback when the new slot PNGs in images/icons/ are missing.
  private static final String FRAME_PATH = "images/hotbar_frame_40.png";
  private static final int FRAME_PX = 40;
  private static final float FRAME_SCALE = 2f;
  private static final float SLOT_SIZE = FRAME_PX * FRAME_SCALE;
  private static final float ICON_SIZE = ICON_PX * UI_SCALE;
  private static final float COIN_SIZE = COIN_PX * UI_SCALE * 0.7f;

  // --- Layout --------------------------------------------------------------------------------
  private static final float PANEL_PADDING = 8f;
  private static final float GROUP_GAP = 24f;
  private static final float BOTTOM_MARGIN = 24f;
  private static final float TOOLTIP_GAP = 10f;
  private static final float SHADOW_OFFSET = 2f;
  private static final int PET_KEY_OFFSET = 5;

  // --- Tooltip timing ------------------------------------------------------------------------
  private static final float TOOLTIP_VISIBLE_SECONDS = 1.8f;
  private static final float TOOLTIP_FADE_SECONDS = 0.5f;

  private static final String LABEL_STYLE = "default";

  // --- Palette -------------------------------------------------------------------------------
  private static final Color PANEL_OUTLINE = new Color(0f, 0f, 0f, 1f);
  private static final Color PANEL_HILITE = new Color(0.50f, 0.50f, 0.54f, 1f);
  private static final Color PANEL_SHADE = new Color(0.07f, 0.07f, 0.08f, 1f);
  private static final Color PANEL_FILL = new Color(0.14f, 0.14f, 0.16f, 0.90f);

  private static final Color SLOT_FILL = new Color(0.24f, 0.24f, 0.26f, 0.92f);
  private static final Color PET_SLOT_FILL = new Color(0.27f, 0.22f, 0.34f, 0.92f);
  private static final Color SLOT_EDGE_DARK = new Color(0.07f, 0.07f, 0.08f, 1f);
  private static final Color SLOT_EDGE_LIGHT = new Color(0.52f, 0.52f, 0.56f, 1f);
  private static final Color SLOT_EDGE_MID = new Color(0.30f, 0.30f, 0.32f, 1f);

  private static final Color COIN_OUTLINE = new Color(0.40f, 0.25f, 0.02f, 1f);
  private static final Color COIN_EDGE = new Color(0.90f, 0.65f, 0.08f, 1f);
  private static final Color COIN_FILL = new Color(1.00f, 0.82f, 0.20f, 1f);
  private static final Color COIN_GROOVE = new Color(0.80f, 0.55f, 0.05f, 1f);
  private static final Color COIN_SHINE = new Color(1.00f, 0.97f, 0.70f, 1f);

  private static final Color TEXT_WHITE = Color.WHITE;
  private static final Color TEXT_KEY_IDLE = new Color(0.78f, 0.78f, 0.82f, 0.85f);
  private static final Color TEXT_GOLD = new Color(1.00f, 0.85f, 0.30f, 1f);
  private static final Color TEXT_SHADOW = new Color(0f, 0f, 0f, 0.65f);

  private static final Color SELECTION_ITEM_COLOR = Color.WHITE;
  private static final Color SELECTION_PET_COLOR = new Color(1f, 0.8f, 0.3f, 1f);
  private static final Color FRAME_IDLE_TINT = new Color(0.62f, 0.62f, 0.66f, 1f);

  // --- Icon PNGs: source/core/assets/images/icons/ (libGDX paths are relative to assets/) ----
  private static final String ICON_POTION_RED = "images/icons/potion_red.png";
  private static final String ICON_POTION_BLUE = "images/icons/potion_blue.png";
  private static final String ICON_POTION_BROWN = "images/icons/potion_brown.png";
  private static final String ICON_SWORD = "images/icons/sword.png";
  private static final String ICON_BOW = "images/icons/bow.png";
  private static final String ICON_COIN = "images/icons/coin.png";
  private static final String ICON_SLOT_EMPTY = "images/icons/slot_empty.png";
  private static final String ICON_SLOT_NORMAL = "images/icons/slot_normal.png";
  private static final String ICON_SLOT_SELECTED = "images/icons/slot_selected.png";
  private static final String ICON_DAGGER = "images/icons/dagger.png";
  private static final String ICON_POTION_GREEN = "images/icons/potion_green.png";

  // Icon rules: keyword in the lower-case item/upgrade name -> PNG. First match wins, so the
  // more specific keywords come first. Names come from ShopComponent / the loot generators.
  private static final Map<String, String> ICON_RULES = new LinkedHashMap<>();

  static {
    ICON_RULES.put("health potion", ICON_POTION_RED);
    ICON_RULES.put("speed potion", ICON_POTION_BLUE);
    ICON_RULES.put("mana", ICON_POTION_BLUE);
    ICON_RULES.put("regen", ICON_POTION_BROWN);
    ICON_RULES.put("endur", ICON_POTION_GREEN);
    ICON_RULES.put("dagger", ICON_DAGGER);
    ICON_RULES.put("potion", ICON_POTION_RED);
    ICON_RULES.put("sword", ICON_SWORD);
    ICON_RULES.put("bow", ICON_BOW);
  }

  // --- State ---------------------------------------------------------------------------------
  private Table hudTable;
  private Table tooltipTable;
  private Label.LabelStyle labelStyle;
  private boolean hidden = false;

  private final Map<String, Drawable> iconCache = new HashMap<>();
  private final List<Texture> iconTextures = new ArrayList<>();
  private final List<Texture> generatedTextures = new ArrayList<>();

  private Drawable slotDrawable;
  private Drawable petSlotDrawable;
  private Drawable selectionDrawable;
  private Drawable panelDrawable;
  private Drawable itemTileDrawable;
  private Drawable coinDrawable;
  private Drawable hoverDrawable;
  private Drawable frameDrawable;
  private Drawable glowDrawable;

  // New artwork frames (null when the PNG is missing).
  private Drawable emptyFrameDrawable;
  private Drawable normalFrameDrawable;
  private Drawable selectedFrameDrawable;

  // Hotkey of the one slot that should bounce on the next rebuild (null = none).
  private String popHotkey;

  /** Creates the inventory UI and adds it to the stage. */
  @Override
  public void create() {
    super.create();

    // Plain Labels in this skin can default to a dark fontColor, which would tint every
    // setColor(...) call to black. Use a WHITE-based style so tints behave as written.
    labelStyle = new Label.LabelStyle(skin.get(LABEL_STYLE, Label.LabelStyle.class));
    labelStyle.fontColor = Color.WHITE;

    createPixelDrawables();
    createTooltip();

    entity.getEvents().addListener("inventoryChanged", this::refreshInventory);
    entity.getEvents().addListener("activeSlotChanged", this::refreshActiveSlot);
    entity.getEvents().addListener("activePetChanged", this::refreshActivePet);

    createInventory();
  }

  // ===========================================================================================
  // Building the HUD
  // ===========================================================================================

  /** Builds the whole HUD: gold counter on top, hotbar row (items + pets) underneath. */
  private void createInventory() {
    InventoryComponent inventory = entity.getComponent(InventoryComponent.class);

    hudTable = new Table();

    hudTable.add(createGoldPill(inventory.getGold())).left().padBottom(8f);
    hudTable.row();
    hudTable.add(createHotbarRow(inventory));

    hudTable.pack();

    positionHud();

    hudTable.setVisible(!hidden);
    stage.addActor(hudTable);

    popHotkey = null;
  }

  /**
   * Creates the gold counter: a small beveled panel with a coin icon and the amount.
   *
   * @param gold amount of gold to display
   * @return the gold counter actor
   */
  private Actor createGoldPill(int gold) {
    Table pill = new Table();
    pill.setBackground(panelDrawable);
    pill.setTouchable(Touchable.disabled);
    pill.pad(8f, 14f, 8f, 16f);

    Image coin = new Image(coinDrawable);
    coin.setScaling(Scaling.fit);
    pill.add(coin).size(COIN_SIZE).padRight(10f);
    pill.add(shadowText(String.valueOf(gold), TEXT_GOLD, 1.1f));

    return pill;
  }

  /**
   * Creates the bottom row: the item hotbar on the left and the pet panel on the right.
   *
   * @param inventory the player's inventory
   * @return the hotbar row actor
   */
  private Actor createHotbarRow(InventoryComponent inventory) {
    Table row = new Table();
    row.add(createItemPanel(inventory)).bottom();

    if (inventory.getPetSlotCount() > 0) {
      row.add(createPetPanel(inventory)).bottom().padLeft(GROUP_GAP);
    }

    return row;
  }

  /** Creates a beveled panel that holds a row of slots. */
  private Table newSlotPanel() {
    Table panel = new Table();
    panel.setBackground(panelDrawable);
    panel.pad(PANEL_PADDING);
    return panel;
  }

  /** Creates the panel holding the item slots. */
  private Table createItemPanel(InventoryComponent inventory) {
    Table itemPanel = newSlotPanel();

    for (int slotNumber = 1; slotNumber <= inventory.getMaxSlots(); slotNumber++) {
      boolean active = inventory.getActiveSlot() == slotNumber;
      itemPanel
          .add(createItemSlot(inventory.getItem(slotNumber), slotNumber, active))
          .size(SLOT_SIZE);
    }

    return itemPanel;
  }

  /** Creates the panel holding the pet slots. */
  private Table createPetPanel(InventoryComponent inventory) {
    PetManagerComponent petManager = entity.getComponent(PetManagerComponent.class);
    Table petPanel = newSlotPanel();

    for (int petSlot = 1; petSlot <= inventory.getPetSlotCount(); petSlot++) {
      ShopComponent.Pet pet = inventory.getPet(petSlot);
      boolean active = isActivePet(pet, petManager);
      petPanel.add(createPetSlot(pet, petSlot, active)).size(SLOT_SIZE);
    }

    return petPanel;
  }

  /** Returns whether the given pet is the player's currently active companion. */
  private static boolean isActivePet(ShopComponent.Pet pet, PetManagerComponent petManager) {
    return pet != null
        && petManager != null
        && petManager.getActivePetType() != null
        && petManager.getActivePetType().getName().equals(pet.getName());
  }

  /**
   * Creates one item slot.
   *
   * @param item item contained in the slot, or null if empty
   * @param slotNumber slot number (also the keyboard key)
   * @param active whether this is the currently selected slot
   * @return the slot actor
   */
  private Actor createItemSlot(Item item, int slotNumber, boolean active) {
    Actor icon = item == null ? null : createIconActor(item.getName());

    String quantity = null;
    if (item != null && item.getQuantity() > 1) {
      quantity = String.valueOf(item.getQuantity());
    }

    return buildSlot(
        slotDrawable,
        String.valueOf(slotNumber),
        icon,
        quantity,
        active,
        SELECTION_ITEM_COLOR,
        () -> selectItemSlot(slotNumber));
  }

  /**
   * Creates one pet slot. Pet slots are displayed as keyboard slots 6 and 7 and are independent
   * from the normal item inventory slots.
   *
   * @param pet pet contained in the slot, or null if empty
   * @param petSlot pet inventory slot number
   * @param active whether this pet is currently active
   * @return the slot actor
   */
  private Actor createPetSlot(ShopComponent.Pet pet, int petSlot, boolean active) {
    Actor icon = pet == null ? null : createIconActor(pet.getName());

    // Empty pet slots have nothing to switch to, so they are not clickable.
    Runnable onClick = pet == null ? null : () -> selectPet(pet);

    return buildSlot(
        petSlotDrawable,
        String.valueOf(petSlot + PET_KEY_OFFSET),
        icon,
        null,
        active,
        SELECTION_PET_COLOR,
        onClick);
  }

  /**
   * Assembles a slot out of stacked layers: background, glow, icon, frame, hotkey number, stack
   * count and hover overlay.
   *
   * @param background beveled slot background
   * @param hotkey text for the key number in the top-left corner
   * @param icon icon actor, or null for an empty slot
   * @param quantity stack count text for the bottom-right corner, or null to hide it
   * @param active whether to draw the selection frame
   * @param selectionColor tint of the selection frame
   * @param onClick action to run when the slot is clicked, or null if the slot is not clickable
   * @return the assembled slot
   */
  private Actor buildSlot(
      Drawable background,
      String hotkey,
      Actor icon,
      String quantity,
      boolean active,
      Color selectionColor,
      Runnable onClick) {

    Stack slot = new Stack();
    // The slot itself receives clicks; every layer inside it lets them fall through.
    slot.setTouchable(Touchable.enabled);

    slot.add(passive(new Image(background)));
    addGlowLayer(slot, active, selectionColor);
    addIconLayer(slot, icon);
    addFrameLayer(slot, active, icon == null, selectionColor);
    addKeyLayer(slot, hotkey, active);
    addQuantityLayer(slot, quantity);
    addHoverAndClick(slot, onClick);
    popIfSelected(slot, active, hotkey);

    return slot;
  }

  /** Makes an image ignore touches so clicks fall through to the slot. */
  private static Image passive(Image image) {
    image.setTouchable(Touchable.disabled);
    return image;
  }

  /** Creates an empty layer table that ignores touches. */
  private static Table passiveLayer() {
    Table layer = new Table();
    layer.setTouchable(Touchable.disabled);
    return layer;
  }

  /** Adds the pulsing glow behind the icon (only when the selected PNG has no glow of its own). */
  private void addGlowLayer(Stack slot, boolean active, Color selectionColor) {
    if (!active || selectedFrameDrawable != null) {
      return;
    }

    Image glow = passive(new Image(glowDrawable));
    glow.setColor(selectionColor);
    glow.addAction(
        Actions.forever(Actions.sequence(Actions.alpha(0.45f, 0.8f), Actions.alpha(1f, 0.8f))));
    slot.add(glow);
  }

  /** Adds the item/pet icon layer, if the slot holds something. */
  private void addIconLayer(Stack slot, Actor icon) {
    if (icon == null) {
      return;
    }

    Table iconLayer = passiveLayer();
    iconLayer.add(icon).size(ICON_SIZE);
    slot.add(iconLayer);
  }

  /** Picks the frame: selected PNG > empty PNG > normal PNG > legacy frame (may be null). */
  private Drawable pickFrame(boolean active, boolean empty) {
    if (active && selectedFrameDrawable != null) {
      return selectedFrameDrawable;
    }
    if (!active && empty && emptyFrameDrawable != null) {
      return emptyFrameDrawable;
    }
    if (normalFrameDrawable != null) {
      return normalFrameDrawable;
    }
    return frameDrawable;
  }

  /** Adds the frame layer, or the generated selection ring when there is no frame image. */
  private void addFrameLayer(Stack slot, boolean active, boolean empty, Color selectionColor) {
    Drawable chosen = pickFrame(active, empty);

    if (chosen != null) {
      // New artwork carries its own colours; only the legacy frame is dimmed when idle.
      boolean customFrame = chosen != frameDrawable;
      Image frame = passive(new Image(chosen));
      frame.setColor(active || customFrame ? Color.WHITE : FRAME_IDLE_TINT);
      slot.add(frame);
    } else if (active) {
      Image selection = passive(new Image(selectionDrawable));
      selection.setColor(selectionColor);
      slot.add(selection);
    }
  }

  /** Adds the hotkey number in the top-left corner. */
  private void addKeyLayer(Stack slot, String hotkey, boolean active) {
    Table keyLayer = passiveLayer();
    keyLayer.top().left().pad(14f, 18f, 0f, 0f);
    keyLayer.add(shadowText(hotkey, active ? TEXT_WHITE : TEXT_KEY_IDLE, 0.6f));
    slot.add(keyLayer);
  }

  /** Adds the stack count in the bottom-right corner, if there is one. */
  private void addQuantityLayer(Stack slot, String quantity) {
    if (quantity == null) {
      return;
    }

    Table quantityLayer = passiveLayer();
    quantityLayer.bottom().right().pad(0f, 0f, 14f, 16f);
    quantityLayer.add(shadowText(quantity, TEXT_WHITE, 0.8f));
    slot.add(quantityLayer);
  }

  /** Adds the hover overlay and click handling for clickable slots. */
  private void addHoverAndClick(Stack slot, Runnable onClick) {
    if (onClick == null) {
      return;
    }

    Image hover = passive(new Image(hoverDrawable));
    hover.setVisible(false);
    slot.add(hover);

    slot.addListener(new HoverClickListener(hover, onClick));
  }

  /** Bounces the slot in when it was freshly selected. */
  private void popIfSelected(Stack slot, boolean active, String hotkey) {
    if (!active || !hotkey.equals(popHotkey)) {
      return;
    }

    slot.setTransform(true);
    slot.setOrigin(SLOT_SIZE / 2f, SLOT_SIZE / 2f);
    slot.setScale(0.8f);
    slot.addAction(Actions.scaleTo(1f, 1f, 0.35f, Interpolation.swingOut));
  }

  /** Shows a hover overlay while the mouse is over a slot and runs an action on click. */
  private static class HoverClickListener extends ClickListener {
    private final Image hover;
    private final Runnable onClick;

    HoverClickListener(Image hover, Runnable onClick) {
      this.hover = hover;
      this.onClick = onClick;
    }

    @Override
    public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
      super.enter(event, x, y, pointer, fromActor);
      if (pointer == -1) {
        hover.setVisible(true);
      }
    }

    @Override
    public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
      super.exit(event, x, y, pointer, toActor);
      if (pointer == -1) {
        hover.setVisible(false);
      }
    }

    @Override
    public void clicked(InputEvent event, float x, float y) {
      onClick.run();
    }
  }

  /**
   * Creates the icon shown inside a slot.
   *
   * <p>Uses the PNG from {@code images/icons/} when {@link #iconForName(String)} finds one (e.g. a
   * name containing "bow" gives {@code images/icons/bow.png}). Otherwise falls back to a beveled
   * tile whose colour is derived from the name, with the name's initials on top.
   *
   * @param name item or pet name
   * @return the icon actor
   */
  private Actor createIconActor(String name) {
    Drawable png = iconForName(name);
    if (png != null) {
      Image image = new Image(png);
      image.setScaling(Scaling.fit);
      return image;
    }

    Image tile = new Image(itemTileDrawable);
    tile.setColor(colorFor(name));

    Table text = new Table();
    text.add(shadowText(initialsOf(name), TEXT_WHITE, 0.75f));

    Stack icon = new Stack();
    icon.add(tile);
    icon.add(text);

    return icon;
  }

  /** Positions the HUD at the bottom centre of the screen. */
  private void positionHud() {
    float screenWidth = stage.getViewport().getWorldWidth();

    float x = (screenWidth - hudTable.getWidth()) / 2f;
    float y = BOTTOM_MARGIN;

    hudTable.setPosition(x, y);
  }

  // ===========================================================================================
  // Text helpers
  // ===========================================================================================

  /**
   * Creates Minecraft-style text: the text drawn twice, with a dark copy offset down-right to act
   * as a drop shadow.
   *
   * @param text text to display
   * @param color colour of the main text
   * @param scale font scale
   * @return an actor containing the shadowed text
   */
  private Actor shadowText(String text, Color color, float scale) {
    Label shadow = new Label(text, labelStyle);
    shadow.setFontScale(scale);
    shadow.setColor(TEXT_SHADOW);

    Label main = new Label(text, labelStyle);
    main.setFontScale(scale);
    main.setColor(color);

    Table shadowLayer = new Table();
    shadowLayer.add(shadow).padTop(SHADOW_OFFSET).padLeft(SHADOW_OFFSET);

    Table mainLayer = new Table();
    mainLayer.add(main).padBottom(SHADOW_OFFSET).padRight(SHADOW_OFFSET);

    Stack stack = new Stack();
    stack.add(shadowLayer);
    stack.add(mainLayer);

    return stack;
  }

  /**
   * Builds short initials for placeholder icons, e.g. "Basic Sword" becomes "BS" and "Wolf" becomes
   * "Wo".
   *
   * @param name item or pet name
   * @return one to two characters
   */
  private static String initialsOf(String name) {
    if (name == null || name.isBlank()) {
      return "?";
    }

    String[] words = name.trim().split("\\s+");

    if (words.length >= 2) {
      return ("" + words[0].charAt(0) + words[1].charAt(0)).toUpperCase();
    }

    String word = words[0];
    if (word.length() == 1) {
      return word.toUpperCase();
    }

    return Character.toUpperCase(word.charAt(0)) + word.substring(1, 2).toLowerCase();
  }

  /**
   * Derives a stable, pleasant colour from a name so different items get different tiles.
   *
   * @param name item or pet name
   * @return an opaque colour
   */
  private static Color colorFor(String name) {
    int hash = name == null ? 0 : name.hashCode();
    float hue = Math.floorMod(hash, 360);

    Color color = new Color();
    color.fromHsv(hue, 0.55f, 0.90f);
    color.a = 1f;

    return color;
  }

  // ===========================================================================================
  // Tooltip (item name above the hotbar, fades out like Minecraft)
  // ===========================================================================================

  /** Creates the persistent, initially invisible tooltip actor. */
  private void createTooltip() {
    tooltipTable = new Table();
    tooltipTable.setTouchable(Touchable.disabled);
    tooltipTable.getColor().a = 0f;

    stage.addActor(tooltipTable);
  }

  /**
   * Shows a name above the hotbar that stays for a moment and then fades away.
   *
   * @param text text to show; null or empty hides the tooltip
   */
  private void showTooltip(String text) {
    if (tooltipTable == null || hudTable == null) {
      return;
    }

    tooltipTable.clearActions();
    tooltipTable.clearChildren();

    if (text == null || text.isEmpty()) {
      tooltipTable.getColor().a = 0f;
      return;
    }

    tooltipTable.add(shadowText(text, TEXT_WHITE, 1.1f));
    tooltipTable.pack();

    float screenWidth = stage.getViewport().getWorldWidth();
    float x = (screenWidth - tooltipTable.getWidth()) / 2f;
    float y = hudTable.getY() + hudTable.getHeight() + TOOLTIP_GAP;
    tooltipTable.setPosition(x, y);

    tooltipTable.getColor().a = 1f;
    tooltipTable.addAction(
        Actions.sequence(
            Actions.delay(TOOLTIP_VISIBLE_SECONDS), Actions.fadeOut(TOOLTIP_FADE_SECONDS)));
  }

  // ===========================================================================================
  // Textures: PNG artwork + procedurally generated fallbacks
  // ===========================================================================================

  /** Loads / generates every drawable used by the HUD once, up front. */
  private void createPixelDrawables() {
    slotDrawable = buildSlotDrawable(SLOT_FILL);
    petSlotDrawable = buildSlotDrawable(PET_SLOT_FILL);
    selectionDrawable = buildSelectionDrawable();
    panelDrawable = buildPanelDrawable();
    itemTileDrawable = buildItemTileDrawable();
    coinDrawable = buildCoinDrawable();
    hoverDrawable = buildHoverDrawable();
    frameDrawable = loadFrameDrawable();
    glowDrawable = buildGlowDrawable();

    // New artwork from source/core/assets/images/icons/ (falls back to the above when missing).
    Drawable coinPng = loadIcon(ICON_COIN);
    if (coinPng != null) {
      coinDrawable = coinPng;
    }
    emptyFrameDrawable = loadIcon(ICON_SLOT_EMPTY);
    normalFrameDrawable = loadIcon(ICON_SLOT_NORMAL);
    selectedFrameDrawable = loadIcon(ICON_SLOT_SELECTED);
  }

  /**
   * Uploads a pixmap as a crisp (nearest-filtered) texture and remembers it for disposal.
   *
   * @param pixmap finished pixmap; it is disposed here
   * @return the created texture
   */
  private Texture buildTexture(Pixmap pixmap) {
    Texture texture = new Texture(pixmap);
    texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
    pixmap.dispose();

    generatedTextures.add(texture);
    return texture;
  }

  /** Wraps a finished pixmap as a drawable (the pixmap is disposed). */
  private Drawable toDrawable(Pixmap pixmap) {
    return new TextureRegionDrawable(new TextureRegion(buildTexture(pixmap)));
  }

  /** Creates a pixmap that writes colours exactly as given (no alpha blending). */
  private static Pixmap newPixmap(int size) {
    Pixmap pixmap = new Pixmap(size, size, Pixmap.Format.RGBA8888);
    pixmap.setBlending(Pixmap.Blending.None);
    return pixmap;
  }

  /**
   * Builds an inset slot: dark top/left edges and light bottom/right edges, like Minecraft's
   * inventory cells.
   */
  private Drawable buildSlotDrawable(Color fill) {
    Pixmap pixmap = newPixmap(SLOT_PX);

    int fillColor = Color.rgba8888(fill);
    int dark = Color.rgba8888(SLOT_EDGE_DARK);
    int light = Color.rgba8888(SLOT_EDGE_LIGHT);
    int mid = Color.rgba8888(SLOT_EDGE_MID);

    int last = SLOT_PX - 1;

    for (int y = 0; y < SLOT_PX; y++) {
      for (int x = 0; x < SLOT_PX; x++) {
        boolean top = y == 0;
        boolean left = x == 0;
        boolean bottom = y == last;
        boolean right = x == last;

        int color;
        if ((top && right) || (bottom && left)) {
          color = mid;
        } else if (top || left) {
          color = dark;
        } else if (bottom || right) {
          color = light;
        } else {
          color = fillColor;
        }

        pixmap.drawPixel(x, y, color);
      }
    }

    return toDrawable(pixmap);
  }

  /** Builds the selection frame: a black outline with a white ring inside, centre transparent. */
  private Drawable buildSelectionDrawable() {
    Pixmap pixmap = newPixmap(SLOT_PX);

    int black = Color.rgba8888(Color.BLACK);
    int white = Color.rgba8888(Color.WHITE);

    int last = SLOT_PX - 1;

    for (int y = 0; y < SLOT_PX; y++) {
      for (int x = 0; x < SLOT_PX; x++) {
        int ring = Math.min(Math.min(x, y), Math.min(last - x, last - y));

        if (ring == 0) {
          pixmap.drawPixel(x, y, black);
        } else if (ring == 1) {
          pixmap.drawPixel(x, y, white);
        }
      }
    }

    return toDrawable(pixmap);
  }

  /** Builds the beveled, scalable nine-patch panel used behind the hotbar and gold counter. */
  private Drawable buildPanelDrawable() {
    Pixmap pixmap = newPixmap(PANEL_PX);

    int outline = Color.rgba8888(PANEL_OUTLINE);
    int hilite = Color.rgba8888(PANEL_HILITE);
    int shade = Color.rgba8888(PANEL_SHADE);
    int fill = Color.rgba8888(PANEL_FILL);

    int last = PANEL_PX - 1;

    for (int y = 0; y < PANEL_PX; y++) {
      for (int x = 0; x < PANEL_PX; x++) {
        int ring = Math.min(Math.min(x, y), Math.min(last - x, last - y));

        int color;
        if (ring == 0) {
          color = outline;
        } else if (ring == 1) {
          boolean topOrLeft = (y == ring && x <= last - ring) || (x == ring && y <= last - ring);
          color = topOrLeft ? hilite : shade;
        } else {
          color = fill;
        }

        pixmap.drawPixel(x, y, color);
      }
    }

    NinePatch patch =
        new NinePatch(
            buildTexture(pixmap),
            PANEL_BORDER_PX,
            PANEL_BORDER_PX,
            PANEL_BORDER_PX,
            PANEL_BORDER_PX);
    patch.scale(UI_SCALE, UI_SCALE);

    return new NinePatchDrawable(patch);
  }

  /**
   * Builds a neutral, beveled item tile (outlined, highlight top/left, shade bottom/right). It is
   * drawn mostly white-grey so tinting with {@code setColor} gives each item its own colour.
   */
  private Drawable buildItemTileDrawable() {
    Pixmap pixmap = newPixmap(ICON_PX);

    int outline = Color.rgba8888(new Color(0.10f, 0.10f, 0.10f, 1f));
    int highlight = Color.rgba8888(new Color(1f, 1f, 1f, 1f));
    int shade = Color.rgba8888(new Color(0.55f, 0.55f, 0.55f, 1f));
    int body = Color.rgba8888(new Color(0.85f, 0.85f, 0.85f, 1f));

    int last = ICON_PX - 1;

    for (int y = 0; y < ICON_PX; y++) {
      for (int x = 0; x < ICON_PX; x++) {
        boolean cornerPixel = (x == 0 || x == last) && (y == 0 || y == last);
        if (cornerPixel) {
          continue;
        }

        int ring = Math.min(Math.min(x, y), Math.min(last - x, last - y));

        int color;
        if (ring == 0) {
          color = outline;
        } else if (y == 1 || x == 1) {
          color = highlight;
        } else if (y == last - 1 || x == last - 1) {
          color = shade;
        } else {
          color = body;
        }

        pixmap.drawPixel(x, y, color);
      }
    }

    return toDrawable(pixmap);
  }

  /**
   * Loads the legacy slot frame PNG from the assets folder.
   *
   * @return the frame drawable, or null if the file is missing (a generated ring is used instead)
   */
  private Drawable loadFrameDrawable() {
    if (!Gdx.files.internal(FRAME_PATH).exists()) {
      return null;
    }

    Texture texture = new Texture(Gdx.files.internal(FRAME_PATH));
    texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
    generatedTextures.add(texture);

    return new TextureRegionDrawable(new TextureRegion(texture));
  }

  /** Builds a blocky radial glow: white, brightest at the centre, fading to transparent. */
  private Drawable buildGlowDrawable() {
    Pixmap pixmap = newPixmap(SLOT_PX);

    float center = (SLOT_PX - 1) / 2f;

    for (int y = 0; y < SLOT_PX; y++) {
      for (int x = 0; x < SLOT_PX; x++) {
        float dx = x - center;
        float dy = y - center;
        float distance = (float) Math.sqrt(dx * dx + dy * dy) / center;
        float falloff = Math.max(0f, 1f - distance);

        pixmap.drawPixel(x, y, Color.rgba8888(1f, 1f, 1f, falloff * falloff * 0.6f));
      }
    }

    return toDrawable(pixmap);
  }

  /** Builds the translucent white overlay shown over a hovered, clickable slot. */
  private Drawable buildHoverDrawable() {
    Pixmap pixmap = newPixmap(SLOT_PX);

    int overlay = Color.rgba8888(new Color(1f, 1f, 1f, 0.22f));

    for (int y = 0; y < SLOT_PX; y++) {
      for (int x = 0; x < SLOT_PX; x++) {
        pixmap.drawPixel(x, y, overlay);
      }
    }

    return toDrawable(pixmap);
  }

  /** Builds a small pixel-art gold coin (outline, rim, face, centre groove and a shine). */
  private Drawable buildCoinDrawable() {
    Pixmap pixmap = newPixmap(COIN_PX);

    float center = (COIN_PX - 1) / 2f;

    for (int y = 0; y < COIN_PX; y++) {
      for (int x = 0; x < COIN_PX; x++) {
        float dx = x - center;
        float dy = y - center;
        float distance = (float) Math.sqrt(dx * dx + dy * dy);

        if (distance <= 4.7f) {
          pixmap.drawPixel(x, y, Color.rgba8888(coinColor(x, y, distance)));
        }
      }
    }

    return toDrawable(pixmap);
  }

  /** Picks the colour of one coin pixel from its position and distance to the centre. */
  private static Color coinColor(int x, int y, float distance) {
    boolean isShine = (y == 2 && (x == 2 || x == 3)) || (x == 2 && y == 3);

    if (isShine) {
      return COIN_SHINE;
    }
    if (distance > 3.7f) {
      return COIN_OUTLINE;
    }
    if ((x == 4 || x == 5) && y >= 2 && y <= 7) {
      return COIN_GROOVE;
    }
    if (distance > 2.9f) {
      return COIN_EDGE;
    }
    return COIN_FILL;
  }

  // ===========================================================================================
  // Event handlers
  // ===========================================================================================

  /**
   * Selects an item slot as the active slot (same as pressing its number key).
   *
   * @param slotNumber slot to select
   */
  private void selectItemSlot(int slotNumber) {
    if (isGamePaused()) {
      return;
    }

    InventoryComponent inventory = entity.getComponent(InventoryComponent.class);

    if (inventory != null) {
      inventory.setActiveSlot(slotNumber);
    }
  }

  /**
   * Switches the active companion to the clicked pet (same as pressing its number key).
   *
   * @param pet pet in the clicked slot
   */
  private void selectPet(ShopComponent.Pet pet) {
    if (isGamePaused()) {
      return;
    }

    PetManagerComponent petManager = entity.getComponent(PetManagerComponent.class);

    if (petManager != null) {
      petManager.switchActivePet(pet);
    }
  }

  /**
   * Checks whether the game is currently paused via the sibling {@link PauseMenuComponent}.
   *
   * @return {@code true} if a pause menu component is present and reports paused
   */
  private boolean isGamePaused() {
    PauseMenuComponent pauseComponent = entity.getComponent(PauseMenuComponent.class);

    return pauseComponent != null && pauseComponent.isPaused();
  }

  /**
   * Refreshes the inventory UI when the inventory changes.
   *
   * <p>This updates both gold and item quantities.
   */
  private void refreshInventory() {
    if (hudTable != null) {
      hudTable.remove();
      hudTable = null;
    }

    createInventory();
  }

  private void refreshActiveSlot(int activeSlot) {
    popHotkey = String.valueOf(activeSlot);
    refreshInventory();

    InventoryComponent inventory = entity.getComponent(InventoryComponent.class);
    Item item = inventory == null ? null : inventory.getItem(activeSlot);

    showTooltip(item == null ? null : item.getName());
  }

  /** Returns the hotkey shown on the slot that holds the given pet, or null if not found. */
  private String petHotkey(ShopComponent.Pet pet) {
    InventoryComponent inventory = entity.getComponent(InventoryComponent.class);

    if (pet == null || inventory == null) {
      return null;
    }

    for (int petSlot = 1; petSlot <= inventory.getPetSlotCount(); petSlot++) {
      ShopComponent.Pet owned = inventory.getPet(petSlot);

      if (owned != null && owned.getName().equals(pet.getName())) {
        return String.valueOf(petSlot + PET_KEY_OFFSET);
      }
    }

    return null;
  }

  private void refreshActivePet(ShopComponent.Pet pet) {
    popHotkey = petHotkey(pet);
    refreshInventory();

    showTooltip(pet == null ? null : pet.getName());
  }

  @Override
  public void draw(SpriteBatch batch) {
    // Drawing is handled by the Scene2D stage.
  }

  @Override
  public void dispose() {
    if (hudTable != null) {
      hudTable.remove();
      hudTable = null;
    }

    if (tooltipTable != null) {
      tooltipTable.remove();
      tooltipTable = null;
    }

    for (Texture texture : generatedTextures) {
      texture.dispose();
    }
    generatedTextures.clear();

    // Textures loaded from images/icons/ are tracked separately.
    disposeIcons();
    clearDrawables();

    super.dispose();
  }

  /** Drops every drawable reference so nothing points at a disposed texture. */
  private void clearDrawables() {
    slotDrawable = null;
    petSlotDrawable = null;
    selectionDrawable = null;
    panelDrawable = null;
    itemTileDrawable = null;
    coinDrawable = null;
    hoverDrawable = null;
    frameDrawable = null;
    glowDrawable = null;
    emptyFrameDrawable = null;
    normalFrameDrawable = null;
    selectedFrameDrawable = null;
  }

  /**
   * Returns the icon for an item or pet name using the configured icon rules.
   *
   * @param name item or pet name
   * @return the icon, or null if no matching icon is available
   */
  private Drawable iconForName(String name) {
    if (name == null) {
      return null;
    }

    String lower = name.toLowerCase();

    for (Map.Entry<String, String> rule : ICON_RULES.entrySet()) {
      if (lower.contains(rule.getKey())) {
        return loadIcon(rule.getValue());
      }
    }

    return null;
  }

  /**
   * Loads a PNG icon once and caches its drawable.
   *
   * @param path path relative to the assets folder
   * @return the drawable, or null if the file does not exist
   */
  private Drawable loadIcon(String path) {
    if (iconCache.containsKey(path)) {
      return iconCache.get(path);
    }

    Drawable drawable = null;

    if (Gdx.files.internal(path).exists()) {
      Texture texture = new Texture(Gdx.files.internal(path));
      texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);

      iconTextures.add(texture);
      drawable = new TextureRegionDrawable(new TextureRegion(texture));
    }

    iconCache.put(path, drawable);
    return drawable;
  }

  /** Frees every texture loaded by {@link #loadIcon(String)}. */
  private void disposeIcons() {
    for (Texture texture : iconTextures) {
      texture.dispose();
    }
    iconTextures.clear();
    iconCache.clear();
  }

  /**
   * Returns the x position of the inventory HUD (bottom-left corner) in stage coordinates.
   *
   * @return HUD x position, or 0 if the HUD has not been built yet
   */
  public float getHudX() {
    return hudTable == null ? 0f : hudTable.getX();
  }

  /**
   * Returns the y position of the inventory HUD (bottom-left corner) in stage coordinates.
   *
   * @return HUD y position, or 0 if the HUD has not been built yet
   */
  public float getHudY() {
    return hudTable == null ? 0f : hudTable.getY();
  }

  /**
   * Returns the width of the whole inventory HUD (gold counter, hotbar and pet panel).
   *
   * @return HUD width, or 0 if the HUD has not been built yet
   */
  public float getHudWidth() {
    return hudTable == null ? 0f : hudTable.getWidth();
  }

  /**
   * Returns the height of the whole inventory HUD (gold counter plus hotbar row).
   *
   * @return HUD height, or 0 if the HUD has not been built yet
   */
  public float getHudHeight() {
    return hudTable == null ? 0f : hudTable.getHeight();
  }

  /**
   * Sets whether the inventory is visible.
   *
   * @param shouldHide whether to hide the inventory
   */
  public void setHidden(boolean shouldHide) {
    hidden = shouldHide;
    if (hudTable != null) {
      hudTable.setVisible(!hidden);
      System.out.println("InventoryTable is hidden: " + hudTable.isVisible());
    }
  }
}
