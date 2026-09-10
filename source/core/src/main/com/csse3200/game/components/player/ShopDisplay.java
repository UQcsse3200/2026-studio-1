package com.csse3200.game.components.player;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.csse3200.game.components.loot.Item;
import com.csse3200.game.pausemenu.PauseMenuComponent;
import com.csse3200.game.ui.UIComponent;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.IntConsumer;

/**
 * Displays the player's shop interface using a dark, MOBA-style layout (inspired by games such as
 * Arena of Valor / League of Legends).
 *
 * <p>The shop is opened via a persistent shop icon/button (disabled while the game is paused via
 * {@link PauseMenuComponent}). Once opened it provides three categories:
 *
 * <ul>
 *   <li>Items — has BUY and SELL sub-tabs. BUY shows the shop's item catalog; SELL shows the
 *       player's inventory so they can sell items back for gold (via {@link
 *       ShopComponent#sellItem(int)}).
 *   <li>Upgrades
 *   <li>Pets
 * </ul>
 *
 * <p>Each grid always displays {@value #ITEM_SLOT_COUNT} slots. Cards carry a thin top accent strip
 * color-coded by rarity (derived from price, see {@link #getRarityForPrice(int)}), matching the
 * icon and the detail panel's side accent bar. Clicking a card selects it and shows its details in
 * a shared bottom panel with a single action button (BUY or SELL depending on context), rather than
 * each card carrying its own button.
 */
public class ShopDisplay extends UIComponent {

  private static final float SHOP_WIDTH = 650f;
  private static final float SHOP_HEIGHT = 560f;

  private static final float CARD_WIDTH = 185f;
  private static final float CARD_HEIGHT = 110f;

  private static final float PANEL_PADDING = 12f;
  private static final float CARD_GAP = 6f;

  private static final int ITEM_COLUMNS = 3;

  // Also used as the assumed player inventory size when listing items to sell. If your
  // InventoryComponent exposes its own capacity (e.g. getCapacity()/getSize()), swap the loop in
  // createSellGrid() to use that instead.
  private static final int ITEM_SLOT_COUNT = 10;

  private static final String LABEL_STYLE = "small";

  private static final String WINDOW_BACKGROUND = "window-w";
  private static final String BUTTON_BACKGROUND = "button-c";

  private static final float ICON_SIZE = 48f;
  private static final float ICON_MARGIN = 10f;

  private static final float ACCENT_STRIP_HEIGHT = 3f;
  private static final float TAB_UNDERLINE_HEIGHT = 3f;

  // --- Dark / MOBA-style palette -------------------------------------------------------------
  private static final Color PANEL_TINT = new Color(0.07f, 0.08f, 0.11f, 1f);
  private static final Color CARD_TINT = new Color(0.12f, 0.13f, 0.18f, 1f);
  private static final Color CARD_TINT_SELECTED = new Color(0.20f, 0.21f, 0.27f, 1f);
  private static final Color EMPTY_CARD_TINT = new Color(0.09f, 0.10f, 0.13f, 1f);
  private static final Color TAB_TINT_ACTIVE = new Color(0.22f, 0.24f, 0.30f, 1f);
  private static final Color TAB_TINT_INACTIVE = new Color(0.13f, 0.14f, 0.18f, 1f);
  private static final Color TAB_UNDERLINE_INACTIVE = new Color(0f, 0f, 0f, 0f);

  private static final Color TEXT_PRIMARY = new Color(0.91f, 0.91f, 0.93f, 1f);
  private static final Color TEXT_MUTED = new Color(0.42f, 0.45f, 0.52f, 1f);
  private static final Color GOLD_COLOR = new Color(0.91f, 0.77f, 0.42f, 1f);
  private static final Color TAB_TEXT_ACTIVE = new Color(0.91f, 0.77f, 0.42f, 1f);

  private static final Color GOLD_PILL_TINT = new Color(0.22f, 0.18f, 0.09f, 1f);
  private static final Color DIVIDER_TINT = new Color(0.91f, 0.77f, 0.42f, 0.30f);
  private static final Color EMPTY_STRIP_TINT = new Color(0.42f, 0.45f, 0.52f, 0.25f);

  // Single BUY/SELL toggle button colors for the Items tab.
  private static final Color BUY_MODE_TINT = new Color(0.20f, 0.22f, 0.28f, 1f);
  private static final Color SELL_MODE_TINT = new Color(0.32f, 0.15f, 0.15f, 1f);
  private static final Color BUY_MODE_TEXT = new Color(0.91f, 0.77f, 0.42f, 1f);
  private static final Color SELL_MODE_TEXT = new Color(0.87f, 0.40f, 0.40f, 1f);

  private static final Color RARITY_COMMON = new Color(0.42f, 0.45f, 0.52f, 1f);
  private static final Color RARITY_RARE = new Color(0.24f, 0.55f, 0.85f, 1f);
  private static final Color RARITY_EPIC = new Color(0.64f, 0.35f, 0.85f, 1f);
  private static final Color RARITY_LEGENDARY = new Color(0.91f, 0.77f, 0.42f, 1f);

  /** Rarity tier used purely for card/icon/accent coloring. */
  private enum Rarity {
    COMMON(RARITY_COMMON),
    RARE(RARITY_RARE),
    EPIC(RARITY_EPIC),
    LEGENDARY(RARITY_LEGENDARY);

    private final Color color;

    Rarity(Color color) {
      this.color = color;
    }
  }

  private enum ShopTab {
    ITEMS,
    UPGRADES,
    PETS
  }

  private Table shopTable;
  private Table contentTable;
  private Table activeGrid;
  private Table detailPanel;
  private Label goldLabel;
  private TextButton shopIconButton;

  // Plain Labels in this skin default to black fontColor, which makes setColor(...) tinting
  // always render black (anyColor * black = black). We build our own LabelStyle with a WHITE
  // base fontColor instead, so every setColor(...) call below multiplies correctly and shows
  // the intended color.
  private Label.LabelStyle whiteLabelStyle;

  private final Map<ShopTab, TextButton> tabButtons = new EnumMap<>(ShopTab.class);
  private final Map<ShopTab, Image> tabUnderlines = new EnumMap<>(ShopTab.class);

  private ShopTab currentTab = ShopTab.ITEMS;

  // Items tab sub-mode: BUY (shop catalog) vs SELL (player inventory).
  private boolean sellMode = false;
  private TextButton buySubButton;
  private TextButton sellSubButton;

  // Currently selected card, shown in the bottom detail panel.
  private Table selectedCard;
  private Runnable pendingAction;

  private Image detailIconBg;
  private Label detailIconLabel;
  private Label detailNameLabel;
  private Label detailPriceLabel;
  private TextButton detailActionButton;

  @Override
  public void create() {
    super.create();

    entity.getEvents().addListener("inventoryChanged", this::refreshShop);
    entity.getEvents().addListener("upgradesPurchased", this::refreshShop);
    entity.getEvents().addListener("petPurchased", this::refreshShop);

    createShop();
    createShopIcon();
  }

  /** Creates the persistent icon/button used to open the shop. */
  private void createShopIcon() {
    shopIconButton = new TextButton("Shop", skin);
    shopIconButton.setSize(ICON_SIZE * 1.6f, ICON_SIZE);

    positionShopIcon();

    shopIconButton.addListener(
        new ClickListener() {
          @Override
          public void clicked(InputEvent event, float x, float y) {
            toggleShop();
          }
        });

    stage.addActor(shopIconButton);
  }

  /** Positions the shop icon in a fixed corner of the screen. */
  private void positionShopIcon() {
    if (shopIconButton == null) {
      return;
    }

    float screenWidth = stage.getViewport().getWorldWidth();
    float screenHeight = stage.getViewport().getWorldHeight();

    float iconWidth = shopIconButton.getWidth();

    shopIconButton.setPosition(
        screenWidth - iconWidth - ICON_MARGIN, screenHeight - ICON_SIZE - ICON_MARGIN);
  }

  /** Toggles the shop window open/closed when the icon is clicked. */
  private void toggleShop() {
    if (shopTable == null || isGamePaused()) {
      return;
    }

    if (shopTable.isVisible()) {
      closeShop();
    } else {
      openShop();
    }
  }

  /** Creates the main shop window. */
  private void createShop() {
    ShopComponent shop = entity.getComponent(ShopComponent.class);
    InventoryComponent inventory = entity.getComponent(InventoryComponent.class);

    if (shop == null || inventory == null) {
      return;
    }

    shopTable = new Table();

    shopTable.setBackground(skin.getDrawable(WINDOW_BACKGROUND));
    shopTable.setColor(PANEL_TINT);
    shopTable.pad(PANEL_PADDING);
    shopTable.setSize(SHOP_WIDTH, SHOP_HEIGHT);

    whiteLabelStyle = new Label.LabelStyle(skin.get(LABEL_STYLE, Label.LabelStyle.class));
    whiteLabelStyle.fontColor = Color.WHITE;

    createHeader();
    addDivider();
    createTabs();

    contentTable = new Table();
    shopTable.add(contentTable).grow().top().left();

    shopTable.row();

    createDetailPanel();
    shopTable.add(detailPanel).growX().height(72f).padTop(8f);

    refreshContent();

    positionShop();

    stage.addActor(shopTable);

    // Shop stays hidden until the shop icon is clicked (see openShop()/toggleShop()).
    shopTable.setVisible(false);
  }

  /** Creates the shop title, a gold "pill" display and close button. */
  private void createHeader() {
    Table headerTable = new Table();

    Label titleLabel = new Label("SHOP", whiteLabelStyle);
    titleLabel.setColor(TEXT_PRIMARY);

    Table goldPill = new Table();
    goldPill.setBackground(skin.getDrawable(BUTTON_BACKGROUND));
    goldPill.setColor(GOLD_PILL_TINT);
    goldPill.pad(4f, 10f, 4f, 10f);

    goldLabel = new Label(getGoldText(), whiteLabelStyle);
    goldLabel.setColor(GOLD_COLOR);
    goldPill.add(goldLabel);

    TextButton closeButton = new TextButton("X", skin);

    closeButton.addListener(
        new ClickListener() {
          @Override
          public void clicked(InputEvent event, float x, float y) {
            closeShop();
          }
        });

    headerTable.add(titleLabel).left().expandX();
    headerTable.add(goldPill).center().padRight(12f);
    headerTable.add(closeButton).size(40f, 32f).right();

    shopTable.add(headerTable).growX().height(40f).top();

    shopTable.row();
  }

  /** Adds a thin gold divider line separating the header from the tab bar. */
  private void addDivider() {
    Image divider = new Image(skin.getDrawable(BUTTON_BACKGROUND));
    divider.setColor(DIVIDER_TINT);

    shopTable.add(divider).growX().height(2f).padTop(4f).padBottom(8f);

    shopTable.row();
  }

  /** Creates the category tab bar. */
  private void createTabs() {
    Table tabTable = new Table();

    addTabButton(tabTable, "ITEMS", ShopTab.ITEMS);
    addTabButton(tabTable, "UPGRADES", ShopTab.UPGRADES);
    addTabButton(tabTable, "PETS", ShopTab.PETS);

    shopTable.add(tabTable).growX().height(48f).padBottom(8f);

    shopTable.row();

    updateTabHighlights();
  }

  /**
   * Adds one tab button (with an underline indicator beneath it) to the tab bar.
   *
   * @param tabTable table containing the tab buttons
   * @param text button text
   * @param tab tab represented by the button
   */
  private void addTabButton(Table tabTable, String text, ShopTab tab) {

    TextButton button = new TextButton(text, skin);

    button.addListener(
        new ClickListener() {
          @Override
          public void clicked(InputEvent event, float x, float y) {
            currentTab = tab;

            if (tab == ShopTab.ITEMS) {
              sellMode = false;
            }

            refreshContent();
          }
        });

    Image underline = new Image(skin.getDrawable(BUTTON_BACKGROUND));
    underline.setColor(TAB_UNDERLINE_INACTIVE);

    Table tabWrap = new Table();
    tabWrap.add(button).width(180f).height(36f);
    tabWrap.row();
    tabWrap.add(underline).growX().height(TAB_UNDERLINE_HEIGHT).padTop(3f);

    tabButtons.put(tab, button);
    tabUnderlines.put(tab, underline);

    tabTable.add(tabWrap).padRight(6f);
  }

  /** Highlights the currently active tab (background, text and underline) and dims the others. */
  private void updateTabHighlights() {
    for (Map.Entry<ShopTab, TextButton> entry : tabButtons.entrySet()) {
      ShopTab tab = entry.getKey();
      TextButton button = entry.getValue();
      boolean active = tab == currentTab;

      button.setColor(active ? TAB_TINT_ACTIVE : TAB_TINT_INACTIVE);
      button.getLabel().setColor(active ? TAB_TEXT_ACTIVE : TEXT_MUTED);

      Image underline = tabUnderlines.get(tab);
      if (underline != null) {
        underline.setColor(active ? TAB_TEXT_ACTIVE : TAB_UNDERLINE_INACTIVE);
      }
    }
  }

  /** Refreshes the contents of the currently selected tab. */
  private void refreshContent() {
    if (contentTable == null) {
      return;
    }

    contentTable.clearChildren();

    updateGold();
    updateTabHighlights();
    clearSelection();

    if (currentTab == ShopTab.ITEMS) {
      createItemSubTabs();
    }

    activeGrid = new Table();
    contentTable.add(activeGrid).grow().top().left();

    switch (currentTab) {
      case ITEMS:
        if (sellMode) {
          createSellGrid();
        } else {
          createItemsTab();
        }
        break;

      case UPGRADES:
        createUpgradesTab();
        break;

      case PETS:
        createPetsTab();
        break;

      default:
        break;
    }
  }

  /** Creates the BUY/SELL sub-tab row shown above the Items grid, two buttons side by side. */
  private void createItemSubTabs() {
    Table subTabRow = new Table();

    buySubButton = new TextButton("BUY", skin);
    sellSubButton = new TextButton("SELL", skin);

    buySubButton.addListener(
        new ClickListener() {
          @Override
          public void clicked(InputEvent event, float x, float y) {
            sellMode = false;
            refreshContent();
          }
        });

    sellSubButton.addListener(
        new ClickListener() {
          @Override
          public void clicked(InputEvent event, float x, float y) {
            sellMode = true;
            refreshContent();
          }
        });

    subTabRow.add(buySubButton).width(90f).height(32f).padRight(6f);
    subTabRow.add(sellSubButton).width(90f).height(32f);

    contentTable.add(subTabRow).left().padBottom(8f);
    contentTable.row();

    updateItemSubTabHighlights();
  }

  /** Highlights whichever of BUY/SELL is currently active and dims the other. */
  private void updateItemSubTabHighlights() {
    if (buySubButton == null || sellSubButton == null) {
      return;
    }

    buySubButton.setColor(!sellMode ? BUY_MODE_TINT : TAB_TINT_INACTIVE);
    buySubButton.getLabel().setColor(Color.WHITE);

    sellSubButton.setColor(sellMode ? SELL_MODE_TINT : TAB_TINT_INACTIVE);
    sellSubButton.getLabel().setColor(Color.WHITE);
  }

  /** Creates the Items (BUY) grid. Always shows {@value #ITEM_SLOT_COUNT} slots. */
  private void createItemsTab() {
    ShopComponent shop = entity.getComponent(ShopComponent.class);

    if (shop == null) {
      return;
    }

    createCatalogTab(shop.getItemCatalog(), Item::getName, this::buyItem);
  }

  /**
   * Creates the Items (SELL) grid: lists the player's inventory items instead of the shop catalog,
   * using {@link ShopComponent#sellItem(int)} to sell.
   */
  private void createSellGrid() {
    ShopComponent shop = entity.getComponent(ShopComponent.class);
    InventoryComponent inventory = entity.getComponent(InventoryComponent.class);

    if (shop == null || inventory == null) {
      return;
    }

    int displayedSlots = 0;

    for (int slot = 1; slot <= ITEM_SLOT_COUNT; slot++) {
      Item item = inventory.getItem(slot);

      addSellCard(shop, item, slot);

      displayedSlots++;

      if (displayedSlots % ITEM_COLUMNS == 0) {
        activeGrid.row();
      }
    }
  }

  /**
   * Creates one sell card for a player inventory slot.
   *
   * @param shop shop component, used to look up the sell price for the item
   * @param item item currently in this inventory slot, or {@code null} if empty
   * @param slot player inventory slot
   */
  private void addSellCard(ShopComponent shop, Item item, int slot) {
    Table card = createCard();

    if (item == null) {
      addEmptyCardContent(card, slot);
    } else {
      String name = item.getName();
      int sellPrice = getSellPriceFor(shop, item);
      Rarity rarity = getRarityForPrice(sellPrice);

      addAccentStrip(card, rarity.color);

      card.add(createIconStack(name, rarity)).size(40f, 40f).padBottom(4f);
      card.row();

      Label nameLabel = new Label(name, whiteLabelStyle);
      nameLabel.setColor(Color.WHITE);
      nameLabel.setAlignment(Align.center);
      card.add(nameLabel).growX().center();

      card.row();

      Label priceLabel = new Label("Gold: " + sellPrice, whiteLabelStyle);
      priceLabel.setColor(GOLD_COLOR);
      card.add(priceLabel).padTop(2f).center();

      card.addListener(
          new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
              selectCard(card, name, sellPrice, rarity, () -> sellItemAt(slot), "SELL");
            }
          });
    }

    addCardToContent(card);
  }

  /**
   * Looks up the sell price for an inventory item by matching it against the shop's item catalog by
   * name and item type (mirrors {@code ShopComponent.findItemListing}).
   *
   * @param shop shop component whose catalog is searched
   * @param item inventory item to price
   * @return sell price in gold, or {@code 0} if no matching catalog listing is found
   */
  private int getSellPriceFor(ShopComponent shop, Item item) {
    for (ShopComponent.ShopListing<Item> listing : shop.getItemCatalog().values()) {
      Item product = listing.getProduct();

      if (product.getName().equals(item.getName()) && product.getItemType() == item.getItemType()) {
        return listing.getSellPrice();
      }
    }

    return 0;
  }

  /** Creates the Upgrades tab. Always shows {@value #ITEM_SLOT_COUNT} slots. */
  private void createUpgradesTab() {
    ShopComponent shop = entity.getComponent(ShopComponent.class);

    if (shop == null) {
      return;
    }

    createCatalogTab(shop.getUpgradeCatalog(), ShopComponent.Upgrade::getName, this::buyUpgrade);
  }

  /** Creates the Pets tab. Always shows {@value #ITEM_SLOT_COUNT} slots. */
  private void createPetsTab() {
    ShopComponent shop = entity.getComponent(ShopComponent.class);

    if (shop == null) {
      return;
    }

    createCatalogTab(shop.getPetCatalog(), ShopComponent.Pet::getName, this::buyPet);
  }

  /**
   * Renders a shop tab with a fixed grid of {@value #ITEM_SLOT_COUNT} slots, regardless of the type
   * of product being sold. Any slot without a listing is rendered as a locked/empty slot.
   *
   * @param catalog the catalog of listings for this tab, keyed by slot number
   * @param nameExtractor function that extracts a display name from a product
   * @param buyAction action invoked with the slot number when the detail panel's BUY is clicked
   * @param <T> product type sold in this tab (Item, Upgrade, Pet, ...)
   */
  private <T> void createCatalogTab(
      Map<Integer, ShopComponent.ShopListing<T>> catalog,
      Function<T, String> nameExtractor,
      IntConsumer buyAction) {

    int displayedSlots = 0;

    for (int slotNumber = 1; slotNumber <= ITEM_SLOT_COUNT; slotNumber++) {

      ShopComponent.ShopListing<T> listing = catalog.get(slotNumber);

      addCatalogCard(listing, slotNumber, nameExtractor, buyAction);

      displayedSlots++;

      if (displayedSlots % ITEM_COLUMNS == 0) {
        activeGrid.row();
      }
    }
  }

  /**
   * Creates one catalog card (item, upgrade or pet) and adds it to the content grid. The whole card
   * is clickable and selects the product, showing it in the bottom detail panel.
   *
   * @param listing shop listing for this slot, or null/empty if the slot is empty
   * @param catalogSlot shop catalog slot number
   * @param nameExtractor function that extracts a display name from a product
   * @param buyAction action invoked with the slot number when the detail panel's BUY is clicked
   * @param <T> product type sold in this tab
   */
  private <T> void addCatalogCard(
      ShopComponent.ShopListing<T> listing,
      int catalogSlot,
      Function<T, String> nameExtractor,
      IntConsumer buyAction) {

    Table card = createCard();

    if (listing == null || listing.getProduct() == null) {
      addEmptyCardContent(card, catalogSlot);
    } else {
      T product = listing.getProduct();
      String name = nameExtractor.apply(product);
      int price = listing.getBuyPrice();
      Rarity rarity = getRarityForPrice(price);

      addAccentStrip(card, rarity.color);

      card.add(createIconStack(name, rarity)).size(40f, 40f).padBottom(4f);
      card.row();

      Label nameLabel = new Label(name, whiteLabelStyle);
      nameLabel.setColor(Color.WHITE);
      nameLabel.setAlignment(Align.center);
      card.add(nameLabel).growX().center();

      card.row();

      Label priceLabel = new Label("Gold: " + price, whiteLabelStyle);
      priceLabel.setColor(GOLD_COLOR);
      card.add(priceLabel).padTop(2f).center();

      card.addListener(
          new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
              selectCard(card, name, price, rarity, () -> buyAction.accept(catalogSlot), "BUY");
            }
          });
    }

    addCardToContent(card);
  }

  /**
   * Adds a thin rarity-colored accent strip as the first row of a card, giving each card a visible
   * top "border" that hints at its rarity before the player even reads the price.
   *
   * @param card card to add the strip to
   * @param color strip color
   */
  private void addAccentStrip(Table card, Color color) {
    Image strip = new Image(skin.getDrawable(BUTTON_BACKGROUND));
    strip.setColor(color);

    card.add(strip).growX().height(ACCENT_STRIP_HEIGHT).padBottom(6f);
    card.row();
  }

  /**
   * Builds a placeholder "icon" for a product: a colored square (by rarity) with the product's
   * first letter centered on top. Swap this for a real {@link Image} using your own item textures
   * once icon assets are available.
   *
   * @param name product name, used to derive the initial shown on the icon
   * @param rarity rarity used to color the icon background
   * @return a stack containing the icon background and initial letter
   */
  private Stack createIconStack(String name, Rarity rarity) {
    Stack iconStack = new Stack();

    Image iconBackground = new Image(skin.getDrawable(BUTTON_BACKGROUND));
    iconBackground.setColor(rarity.color);

    String initial = (name == null || name.isEmpty()) ? "?" : name.substring(0, 1).toUpperCase();

    Label iconLabel = new Label(initial, whiteLabelStyle);
    iconLabel.setColor(Color.WHITE);
    iconLabel.setAlignment(Align.center);

    iconStack.add(iconBackground);
    iconStack.add(iconLabel);

    return iconStack;
  }

  /**
   * Derives a display rarity from an item's price. This is a simple stand-in until
   * items/upgrades/pets carry their own rarity field — swap this out for {@code
   * product.getRarity()} if/when that exists.
   *
   * @param price buy or sell price of the listing
   * @return rarity tier used for card/icon/accent coloring
   */
  private Rarity getRarityForPrice(int price) {
    if (price >= 2000) {
      return Rarity.LEGENDARY;
    }
    if (price >= 1000) {
      return Rarity.EPIC;
    }
    if (price >= 300) {
      return Rarity.RARE;
    }
    return Rarity.COMMON;
  }

  /**
   * Creates a generic, dark-themed shop card shell (no content).
   *
   * @return empty shop card
   */
  private Table createCard() {
    Table card = new Table();

    card.setBackground(skin.getDrawable(BUTTON_BACKGROUND));
    card.setColor(CARD_TINT);
    card.pad(8f);

    return card;
  }

  /**
   * Renders an empty/locked slot, including a faint strip so its layout still lines up with filled
   * cards.
   *
   * @param card shop card
   * @param slot slot number
   */
  private void addEmptyCardContent(Table card, int slot) {
    card.setColor(EMPTY_CARD_TINT);

    addAccentStrip(card, EMPTY_STRIP_TINT);

    Label slotLabel = new Label("Empty", whiteLabelStyle);
    slotLabel.setColor(TEXT_MUTED);

    card.add(slotLabel).center().expand();
  }

  /**
   * Adds a card to the active grid.
   *
   * @param card card to add
   */
  private void addCardToContent(Table card) {
    activeGrid.add(card).size(CARD_WIDTH, CARD_HEIGHT).pad(CARD_GAP);

    int children = activeGrid.getChildren().size;

    if (children % ITEM_COLUMNS == 0) {
      activeGrid.row();
    }
  }

  /** Creates the shared bottom detail/action panel (initially empty). */
  private void createDetailPanel() {
    detailPanel = new Table();
    detailPanel.setBackground(skin.getDrawable(BUTTON_BACKGROUND));
    detailPanel.setColor(CARD_TINT);
    detailPanel.pad(10f);

    detailIconBg = new Image(skin.getDrawable(BUTTON_BACKGROUND));
    detailIconLabel = new Label("", whiteLabelStyle);
    detailIconLabel.setColor(Color.WHITE);
    detailIconLabel.setAlignment(Align.center);

    Stack detailIconStack = new Stack();
    detailIconStack.add(detailIconBg);
    detailIconStack.add(detailIconLabel);

    detailNameLabel = new Label("Select an item to view details", whiteLabelStyle);
    detailNameLabel.setColor(TEXT_MUTED);

    detailPriceLabel = new Label("", whiteLabelStyle);
    detailPriceLabel.setColor(GOLD_COLOR);

    detailActionButton = new TextButton("BUY", skin);
    detailActionButton.setDisabled(true);
    detailActionButton.setTouchable(Touchable.disabled);

    detailActionButton.addListener(
        new ClickListener() {
          @Override
          public void clicked(InputEvent event, float x, float y) {
            if (pendingAction != null) {
              pendingAction.run();
            }
          }
        });

    Table textColumn = new Table();
    textColumn.add(detailNameLabel).left().row();
    textColumn.add(detailPriceLabel).left().padTop(2f);

    detailPanel.add(detailIconStack).size(40f, 40f).padRight(10f);
    detailPanel.add(textColumn).growX().left();
    detailPanel.add(detailActionButton).size(80f, 34f).right();
  }

  /**
   * Marks a card as selected and populates the bottom detail panel with its info and a working
   * action. Any previously selected card is un-highlighted.
   *
   * @param card the card actor that was clicked
   * @param name product/item display name
   * @param price price shown (buy price or sell price depending on context)
   * @param rarity rarity used to drive icon/detail color
   * @param action action to run when the detail panel's action button is pressed
   * @param actionLabel label for the action button, e.g. "BUY" or "SELL"
   */
  private void selectCard(
      Table card, String name, int price, Rarity rarity, Runnable action, String actionLabel) {
    if (selectedCard != null) {
      selectedCard.setColor(CARD_TINT);
    }

    selectedCard = card;
    selectedCard.setColor(CARD_TINT_SELECTED);

    pendingAction = action;

    detailIconBg.setColor(rarity.color);
    detailIconLabel.setText(
        (name == null || name.isEmpty()) ? "?" : name.substring(0, 1).toUpperCase());

    detailNameLabel.setText(name);
    detailNameLabel.setColor(TEXT_PRIMARY);

    detailPriceLabel.setText("Gold: " + price);

    detailActionButton.setText(actionLabel);
    detailActionButton.setDisabled(false);
    detailActionButton.setTouchable(Touchable.enabled);
  }

  /** Clears the current selection and resets the detail panel to its placeholder state. */
  private void clearSelection() {
    if (selectedCard != null) {
      selectedCard.setColor(CARD_TINT);
      selectedCard = null;
    }

    pendingAction = null;

    if (detailIconBg == null) {
      return;
    }

    detailIconBg.setColor(EMPTY_CARD_TINT);
    detailIconLabel.setText("");

    detailNameLabel.setText("Select an item to view details");
    detailNameLabel.setColor(TEXT_MUTED);

    detailPriceLabel.setText("");

    detailActionButton.setText("BUY");
    detailActionButton.setDisabled(true);
    detailActionButton.setTouchable(Touchable.disabled);
  }

  /**
   * Attempts to purchase an item.
   *
   * @param catalogSlot shop catalog slot
   */
  private void buyItem(int catalogSlot) {
    ShopComponent shop = entity.getComponent(ShopComponent.class);

    if (shop == null || isGamePaused()) {
      return;
    }

    shop.buyItem(catalogSlot);
    refreshShop();
  }

  /**
   * Attempts to sell an item from the player's inventory.
   *
   * @param playerSlot inventory slot on the sibling {@link InventoryComponent}
   */
  private void sellItemAt(int playerSlot) {
    ShopComponent shop = entity.getComponent(ShopComponent.class);

    if (shop == null || isGamePaused()) {
      return;
    }

    shop.sellItem(playerSlot);
    refreshShop();
  }

  /**
   * Attempts to purchase an upgrade.
   *
   * @param catalogSlot shop catalog slot
   */
  private void buyUpgrade(int catalogSlot) {
    ShopComponent shop = entity.getComponent(ShopComponent.class);

    if (shop == null || isGamePaused()) {
      return;
    }

    shop.buyUpgrade(catalogSlot);
    refreshShop();
  }

  /**
   * Attempts to purchase a pet.
   *
   * @param catalogSlot shop catalog slot
   */
  private void buyPet(int catalogSlot) {
    ShopComponent shop = entity.getComponent(ShopComponent.class);

    if (shop == null || isGamePaused()) {
      return;
    }

    shop.buyPet(catalogSlot);
    refreshShop();
  }

  /** Refreshes the complete shop interface. */
  private void refreshShop() {
    if (shopTable == null) {
      return;
    }

    updateGold();
    refreshContent();
  }

  /** Updates the gold label. */
  private void updateGold() {
    if (goldLabel != null) {
      goldLabel.setText(getGoldText());
    }
  }

  /**
   * Gets the current player's gold as display text.
   *
   * @return formatted gold text
   */
  private String getGoldText() {
    InventoryComponent inventory = entity.getComponent(InventoryComponent.class);

    if (inventory == null) {
      return "Gold: 0";
    }

    return "Gold: " + inventory.getGold();
  }

  /** Opens the shop. */
  public void openShop() {
    if (shopTable == null || isGamePaused()) {
      return;
    }

    currentTab = ShopTab.ITEMS;
    sellMode = false;
    refreshContent();

    positionShop();

    shopTable.setVisible(true);
    shopTable.toFront();
  }

  /** Closes the shop. */
  public void closeShop() {
    if (shopTable == null) {
      return;
    }

    shopTable.setVisible(false);
  }

  /** Positions the shop in the centre of the screen. */
  private void positionShop() {
    if (shopTable == null) {
      return;
    }

    float screenWidth = stage.getViewport().getWorldWidth();
    float screenHeight = stage.getViewport().getWorldHeight();

    float x = (screenWidth - SHOP_WIDTH) / 2f;
    float y = (screenHeight - SHOP_HEIGHT) / 2f;

    shopTable.setPosition(x, y);
  }

  /**
   * Runs every frame. If the shop is open and the game becomes paused (regardless of how the pause
   * was triggered), the shop is force-closed so it can't be used while paused.
   */
  @Override
  public void update() {
    if (shopTable != null && shopTable.isVisible() && isGamePaused()) {
      closeShop();
    }
  }

  @Override
  public void draw(SpriteBatch batch) {
    // Scene2D stage handles the shop rendering.
  }

  @Override
  public void dispose() {
    if (shopTable != null) {
      shopTable.remove();
      shopTable = null;
    }

    if (shopIconButton != null) {
      shopIconButton.remove();
      shopIconButton = null;
    }

    contentTable = null;
    activeGrid = null;
    detailPanel = null;
    goldLabel = null;
    selectedCard = null;
    pendingAction = null;

    super.dispose();
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
}
