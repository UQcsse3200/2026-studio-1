package com.csse3200.game.components.player;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
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
import com.csse3200.game.components.loot.Item;
import com.csse3200.game.components.pet.PetManagerComponent;
import com.csse3200.game.pausemenu.PauseMenuComponent;
import com.csse3200.game.ui.UIComponent;
import java.util.ArrayList;
import java.util.List;

public class InventoryDisplay extends UIComponent {

  // --- Pixel-art geometry --------------------------------------------------------------------
  // Every generated texture pixel is drawn as UI_SCALE screen pixels.
  private static final float UI_SCALE = 4f;

  private static final int SLOT_PX = 18;
  private static final int ICON_PX = 12;
  private static final int COIN_PX = 10;
  private static final int PANEL_PX = 10;
  private static final int PANEL_BORDER_PX = 3;

  private static final float SLOT_SIZE = SLOT_PX * UI_SCALE;
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

  // --- State ---------------------------------------------------------------------------------
  private Table hudTable;
  private Table tooltipTable;
  private Label.LabelStyle labelStyle;

  private final List<Texture> generatedTextures = new ArrayList<>();
  private Drawable slotDrawable;
  private Drawable petSlotDrawable;
  private Drawable selectionDrawable;
  private Drawable panelDrawable;
  private Drawable itemTileDrawable;
  private Drawable coinDrawable;
  private Drawable hoverDrawable;

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

    stage.addActor(hudTable);
  }

  /**
   * Creates the gold counter: a small beveled panel with a pixel-art coin and the amount.
   *
   * @param gold amount of gold to display
   * @return the gold counter actor
   */
  private Actor createGoldPill(int gold) {
    Table pill = new Table();
    pill.setBackground(panelDrawable);
    pill.setTouchable(Touchable.disabled);
    pill.pad(8f, 14f, 8f, 16f);

    pill.add(new Image(coinDrawable)).size(COIN_SIZE).padRight(10f);
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

    Table itemPanel = new Table();
    itemPanel.setBackground(panelDrawable);
    itemPanel.pad(PANEL_PADDING);

    for (int slotNumber = 1; slotNumber <= inventory.getMaxSlots(); slotNumber++) {
      boolean active = inventory.getActiveSlot() == slotNumber;
      itemPanel
          .add(createItemSlot(inventory.getItem(slotNumber), slotNumber, active))
          .size(SLOT_SIZE);
    }

    row.add(itemPanel).bottom();

    int petSlots = inventory.getPetSlotCount();
    if (petSlots > 0) {
      PetManagerComponent petManager = entity.getComponent(PetManagerComponent.class);

      Table petPanel = new Table();
      petPanel.setBackground(panelDrawable);
      petPanel.pad(PANEL_PADDING);

      for (int petSlot = 1; petSlot <= petSlots; petSlot++) {
        ShopComponent.Pet pet = inventory.getPet(petSlot);

        boolean active =
            pet != null
                && petManager != null
                && petManager.getActivePetType() != null
                && petManager.getActivePetType().getName().equals(pet.getName());

        petPanel.add(createPetSlot(pet, petSlot, active)).size(SLOT_SIZE);
      }

      row.add(petPanel).bottom().padLeft(GROUP_GAP);
    }

    return row;
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
   * Assembles a slot out of stacked layers: background, selection frame, icon, hotkey number and
   * stack count.
   *
   * @param background beveled slot background
   * @param hotkey text for the key number in the top-left corner
   * @param icon icon actor, or null for an empty slot
   * @param quantity stack count text for the bottom-right corner, or null to hide it
   * @param active whether to draw the pulsing selection frame
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

    Image backgroundImage = new Image(background);
    backgroundImage.setTouchable(Touchable.disabled);
    slot.add(backgroundImage);

    if (active) {
      Image selection = new Image(selectionDrawable);
      selection.setColor(selectionColor);
      selection.setTouchable(Touchable.disabled);
      selection.addAction(
          Actions.forever(Actions.sequence(Actions.alpha(0.6f, 0.7f), Actions.alpha(1f, 0.7f))));
      slot.add(selection);
    }

    if (icon != null) {
      Table iconLayer = new Table();
      iconLayer.setTouchable(Touchable.disabled);
      iconLayer.add(icon).size(ICON_SIZE);
      slot.add(iconLayer);
    }

    Table keyLayer = new Table();
    keyLayer.setTouchable(Touchable.disabled);
    keyLayer.top().left().pad(10f, 12f, 0f, 0f);
    keyLayer.add(shadowText(hotkey, active ? TEXT_WHITE : TEXT_KEY_IDLE, 0.6f));
    slot.add(keyLayer);

    if (quantity != null) {
      Table quantityLayer = new Table();
      quantityLayer.setTouchable(Touchable.disabled);
      quantityLayer.bottom().right().pad(0f, 0f, 8f, 10f);
      quantityLayer.add(shadowText(quantity, TEXT_WHITE, 0.8f));
      slot.add(quantityLayer);
    }

    if (onClick != null) {
      Image hover = new Image(hoverDrawable);
      hover.setTouchable(Touchable.disabled);
      hover.setVisible(false);
      slot.add(hover);

      slot.addListener(
          new ClickListener() {
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
          });
    }

    return slot;
  }

  /**
   * Creates the icon shown inside a slot.
   *
   * <p>This is a placeholder: a beveled tile whose colour is derived from the name, with the name's
   * initials on top. To use real PNG icons, replace the body of this method with an {@link Image}
   * built from your own {@link Drawable} for the given name.
   *
   * @param name item or pet name
   * @return the icon actor
   */
  private Actor createIconActor(String name) {
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
  // Procedurally generated pixel-art textures
  // ===========================================================================================

  /** Generates every drawable used by the HUD once, up front. */
  private void createPixelDrawables() {
    slotDrawable = buildSlotDrawable(SLOT_FILL);
    petSlotDrawable = buildSlotDrawable(PET_SLOT_FILL);
    selectionDrawable = buildSelectionDrawable();
    panelDrawable = buildPanelDrawable();
    itemTileDrawable = buildItemTileDrawable();
    coinDrawable = buildCoinDrawable();
    hoverDrawable = buildHoverDrawable();
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

    return new TextureRegionDrawable(new TextureRegion(buildTexture(pixmap)));
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

    return new TextureRegionDrawable(new TextureRegion(buildTexture(pixmap)));
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

    return new TextureRegionDrawable(new TextureRegion(buildTexture(pixmap)));
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

    return new TextureRegionDrawable(new TextureRegion(buildTexture(pixmap)));
  }

  /** Builds a small pixel-art gold coin (outline, rim, face, centre groove and a shine). */
  private Drawable buildCoinDrawable() {
    Pixmap pixmap = newPixmap(COIN_PX);

    int outline = Color.rgba8888(COIN_OUTLINE);
    int edge = Color.rgba8888(COIN_EDGE);
    int fill = Color.rgba8888(COIN_FILL);
    int groove = Color.rgba8888(COIN_GROOVE);
    int shine = Color.rgba8888(COIN_SHINE);

    float center = (COIN_PX - 1) / 2f;

    for (int y = 0; y < COIN_PX; y++) {
      for (int x = 0; x < COIN_PX; x++) {
        float dx = x - center;
        float dy = y - center;
        float distance = (float) Math.sqrt(dx * dx + dy * dy);

        if (distance > 4.7f) {
          continue;
        }

        int color;
        if (distance > 3.7f) {
          color = outline;
        } else if ((x == 4 || x == 5) && y >= 2 && y <= 7) {
          color = groove;
        } else if (distance > 2.9f) {
          color = edge;
        } else {
          color = fill;
        }

        boolean isShine = (y == 2 && (x == 2 || x == 3)) || (x == 2 && y == 3);
        if (isShine) {
          color = shine;
        }

        pixmap.drawPixel(x, y, color);
      }
    }

    return new TextureRegionDrawable(new TextureRegion(buildTexture(pixmap)));
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
    refreshInventory();

    InventoryComponent inventory = entity.getComponent(InventoryComponent.class);
    Item item = inventory == null ? null : inventory.getItem(activeSlot);

    showTooltip(item == null ? null : item.getName());
  }

  private void refreshActivePet(ShopComponent.Pet pet) {
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

    slotDrawable = null;
    petSlotDrawable = null;
    selectionDrawable = null;
    panelDrawable = null;
    itemTileDrawable = null;
    coinDrawable = null;
    hoverDrawable = null;

    super.dispose();
  }
}
