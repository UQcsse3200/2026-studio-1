package com.csse3200.game.components.player;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;
import com.csse3200.game.components.loot.Item;
import com.csse3200.game.ui.UIComponent;
import com.csse3200.game.upgrades.UpgradeNode;
import com.csse3200.game.upgrades.UpgradesDisplay;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.IntConsumer;

/**
 * Displays the player's shop interface using a warm fantasy RPG palette with golden bronze accents.
 *
 * <p>The shop provides four categories:
 *
 * <ul>
 *   <li>Items - BUY and SELL modes
 *   <li>Upgrades
 *   <li>Pets
 *   <li>Gambling
 * </ul>
 *
 * <p>The Pets tab uses the pet texture atlas located at {@code assets/images/pet.atlas}. The
 * catalog slot determines which {@code idle_right} frame is used as the pet icon.
 */
public class ShopDisplay extends UIComponent {

  private static final float SHOP_WIDTH = 650f;
  private static final float SHOP_HEIGHT = 560f;

  private static final float CARD_WIDTH = 185f;
  private static final float CARD_HEIGHT = 110f;

  private static final float PANEL_PADDING = 14f;
  private static final float CARD_GAP = 8f;

  private static final float UPGRADE_POPUP_WIDTH = 320f;
  private static final float UPGRADE_POPUP_HEIGHT = 150f;
  private static final float UPGRADE_POPUP_GAP_ABOVE_SHOP = 16f;

  private static final float PURCHASE_TOAST_TOP_MARGIN = 24f;
  private static final float PURCHASE_TOAST_VISIBLE_SECONDS = 1.75f;

  private static final int ITEM_COLUMNS = 3;
  private static final int ITEM_SLOT_COUNT = 10;
  private static final int SELL_SLOT_COUNT = 5;

  private static final String LABEL_STYLE = "small";

  private static final String WINDOW_BACKGROUND = "window-w";
  private static final String ACCENT_PANEL_BACKGROUND = "window-c";
  private static final String TOAST_BACKGROUND = "toast-charcoal";
  private static final String BUTTON_BACKGROUND = "button-c";

  private static final float ICON_SIZE = 48f;
  private static final float ICON_MARGIN = 10f;

  private static final float ACCENT_STRIP_HEIGHT = 4f;
  private static final float TAB_UNDERLINE_HEIGHT = 4f;

  // --- Warm RPG / Classic Fantasy Color Palette ---------------------------------------------
  // Deep warm wood tone for the main panel backdrop
  private static final Color PANEL_TINT = new Color(0.28f, 0.16f, 0.12f, 0.98f);

  // Deep navy cards for maximum contrast against icons and text
  private static final Color CARD_TINT = new Color(0.10f, 0.12f, 0.20f, 1f);
  private static final Color CARD_TINT_SELECTED = new Color(0.18f, 0.22f, 0.36f, 1f);
  // Muted earthy dark tone for empty/unoccupied slots
  private static final Color EMPTY_CARD_TINT = new Color(0.18f, 0.12f, 0.11f, 0.95f);

  // Tab navigation states
  private static final Color TAB_TINT_ACTIVE = new Color(0.92f, 0.60f, 0.15f, 1f);
  private static final Color TAB_TINT_INACTIVE = new Color(0.20f, 0.16f, 0.24f, 1f);
  private static final Color TAB_UNDERLINE_INACTIVE = new Color(0f, 0f, 0f, 0f);

  // Text color hierarchy
  private static final Color TEXT_PRIMARY = new Color(1f, 1f, 1f, 1f);
  private static final Color TEXT_MUTED = new Color(0.68f, 0.62f, 0.60f, 1f);
  private static final Color GOLD_COLOR = new Color(1.0f, 0.84f, 0.25f, 1f);
  private static final Color INSUFFICIENT_FUNDS_COLOR = new Color(0.95f, 0.30f, 0.30f, 1f);
  private static final Color TAB_TEXT_ACTIVE = new Color(1f, 1f, 1f, 1f);

  // Golden bronze pill and divider accents
  private static final Color GOLD_PILL_TINT = new Color(0.20f, 0.12f, 0.05f, 1f);
  private static final Color DIVIDER_TINT = new Color(0.85f, 0.62f, 0.22f, 0.80f);
  private static final Color EMPTY_STRIP_TINT = new Color(0.35f, 0.25f, 0.22f, 0.50f);

  // Action toggle buttons
  private static final Color BUY_MODE_TINT = new Color(0.11f, 0.60f, 0.55f, 1f);
  private static final Color SELL_MODE_TINT = new Color(0.72f, 0.24f, 0.18f, 1f);
  private static final Color SPIN_BUTTON_TINT = new Color(0.92f, 0.55f, 0.12f, 1f);
  private static final Color BUY_MODE_TEXT = Color.WHITE;
  private static final Color SELL_MODE_TEXT = Color.WHITE;
  // --- Selection Highlights ---
  private static final Color SELECTION_ARROW_COLOR = new Color(1.0f, 0.85f, 0.25f, 1f);

  // Floating arrow indicator for the selected card
  private Label selectionArrow;

  // Item rarity tiers
  private static final Color RARITY_COMMON = new Color(0.60f, 0.65f, 0.72f, 1f);
  private static final Color RARITY_RARE = new Color(0.18f, 0.65f, 0.95f, 1f);
  private static final Color RARITY_EPIC = new Color(0.68f, 0.28f, 0.90f, 1f);
  private static final Color RARITY_LEGENDARY = new Color(1.0f, 0.75f, 0.10f, 1f);

  /** Pet atlas containing the pet sprites. */
  private TextureAtlas petAtlas;

  /** Rarity tier used for card/icon/accent coloring. */
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
    PETS,
    GAMBLING
  }

  private Table shopTable;
  private Table contentTable;
  private Table activeGrid;
  private Table detailPanel;
  private Label goldLabel;
  private TextButton shopIconButton;

  private Label.LabelStyle whiteLabelStyle;

  private final Map<ShopTab, TextButton> tabButtons = new EnumMap<>(ShopTab.class);
  private final Map<ShopTab, Image> tabUnderlines = new EnumMap<>(ShopTab.class);

  private ShopTab currentTab = ShopTab.ITEMS;

  // Gambling tab components.
  private GamblingWheel gamblingWheel;
  private TextButton standardGamblingButton;
  private TextButton premiumGamblingButton;
  private TextButton spinButton;
  private Label gamblingPriceLabel;
  private Label gamblingResultLabel;

  private GamblingCatalogs.CatalogId currentGamblingCatalog = GamblingCatalogs.CatalogId.STANDARD;

  // Items tab sub-modes: BUY vs SELL.
  private boolean sellMode = false;
  private TextButton buySubButton;
  private TextButton sellSubButton;

  // Currently selected card.
  private Table selectedCard;
  private Runnable pendingAction;

  // Bottom detail panel components.
  private Image detailIconBg;
  private Label detailIconLabel;
  private Label detailNameLabel;
  private Label detailPriceLabel;
  private TextButton detailActionButton;

  private UpgradesDisplay upgradesDisplay;

  private Table upgradePopup;
  private Label upgradePopupNameLabel;
  private Label upgradePopupDescriptionLabel;
  private Label upgradePopupTierLabel;
  private TextButton upgradePopupCloseButton;

  private Table purchaseToast;
  private Label purchaseToastLabel;

  @Override
  public void create() {
    super.create();

    entity.getEvents().addListener("inventoryChanged", this::refreshShop);
    entity.getEvents().addListener("shopChanged", this::refreshShop);
    entity.getEvents().addListener("upgradePurchased", this::refreshShop);
    entity.getEvents().addListener("petPurchased", (ShopComponent.Pet pet) -> refreshShop());

    petAtlas = new TextureAtlas(Gdx.files.internal("images/pet.atlas"));

    createShop();
    createShopIcon();
  }

  /** Creates the persistent floating icon used to open the shop. */
  private void createShopIcon() {
    shopIconButton = new TextButton("Shop", skin);
    shopIconButton.setSize(ICON_SIZE * 1.6f, ICON_SIZE);
    shopIconButton.setColor(TAB_TINT_ACTIVE);

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

  /** Positions the shop icon in the top-right corner of the screen. */
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

  /**
   * Connects the real upgrades system to the shop interface.
   *
   * @param upgradesDisplay the active UpgradesDisplay instance
   */
  public void setUpgradesDisplay(UpgradesDisplay upgradesDisplay) {
    this.upgradesDisplay = upgradesDisplay;
    syncUpgradeCatalog();
    refreshContent();
  }

  /** Synchronizes the Upgrade catalog with real upgrade nodes. */
  private void syncUpgradeCatalog() {
    if (upgradesDisplay == null || entity == null) {
      return;
    }

    ShopComponent shop = entity.getComponent(ShopComponent.class);
    if (shop == null) {
      return;
    }

    int slot = 1;
    for (UpgradeNode node : upgradesDisplay.getAllUpgrades()) {
      if (slot > ShopComponent.MAX_CATALOG_SLOTS) {
        break;
      }
      syncUpgradeListing(shop, slot, node);
      slot++;
    }
  }

  /** Updates pricing for an individual upgrade catalog slot. */
  private void syncUpgradeListing(ShopComponent shop, int catalogSlot, UpgradeNode node) {
    int cost = node.isMaxTier() ? 0 : node.getNextTierCost();
    shop.setUpgradeListing(
        catalogSlot,
        new ShopComponent.ShopListing<>(new ShopComponent.Upgrade(node.getName()), cost));
  }

  /** Toggles the shop dialog window open or closed. */
  private void toggleShop() {
    if (shopTable == null) {
      return;
    }

    if (shopTable.isVisible()) {
      closeShop();
    } else {
      openShop();
    }
  }

  /** Builds the main shop dialog frame and layout containers. */
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
    shopTable.add(detailPanel).growX().height(74f).padTop(8f);

    refreshContent();
    positionShop();

    stage.addActor(shopTable);
    shopTable.setVisible(false);
  }

  /** Constructs the header bar with title, gold pill counter, and close button. */
  private void createHeader() {
    Table headerTable = new Table();

    Label titleLabel = new Label("SHOP", whiteLabelStyle);
    titleLabel.setColor(GOLD_COLOR);

    Table goldPill = new Table();
    goldPill.setBackground(skin.getDrawable(BUTTON_BACKGROUND));
    goldPill.setColor(GOLD_PILL_TINT);
    goldPill.pad(4f, 12f, 4f, 12f);

    goldLabel = new Label(getGoldText(), whiteLabelStyle);
    goldLabel.setColor(GOLD_COLOR);
    goldPill.add(goldLabel);

    TextButton closeButton = new TextButton("X", skin);
    closeButton.setColor(SELL_MODE_TINT);
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

  /** Adds a decorative golden divider line separating the header from the category tabs. */
  private void addDivider() {
    Image divider = new Image(skin.getDrawable(BUTTON_BACKGROUND));
    divider.setColor(DIVIDER_TINT);

    shopTable.add(divider).growX().height(2f).padTop(4f).padBottom(8f);
    shopTable.row();
  }

  /** Creates the top-level category navigation tab bar. */
  private void createTabs() {
    Table tabTable = new Table();

    addTabButton(tabTable, "ITEMS", ShopTab.ITEMS);
    addTabButton(tabTable, "UPGRADES", ShopTab.UPGRADES);
    addTabButton(tabTable, "PETS", ShopTab.PETS);
    addTabButton(tabTable, "GAMBLING", ShopTab.GAMBLING);

    shopTable.add(tabTable).growX().height(48f).padBottom(8f);
    shopTable.row();

    updateTabHighlights();
  }

  /** Adds an individual category tab button paired with an underline accent. */
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
    tabWrap.add(button).width(135f).height(36f);
    tabWrap.row();
    tabWrap.add(underline).growX().height(TAB_UNDERLINE_HEIGHT).padTop(3f);

    tabButtons.put(tab, button);
    tabUnderlines.put(tab, underline);

    tabTable.add(tabWrap).padRight(6f);
  }

  /** Updates tint and text colors across all category tabs based on selection. */
  private void updateTabHighlights() {
    for (Map.Entry<ShopTab, TextButton> entry : tabButtons.entrySet()) {
      ShopTab tab = entry.getKey();
      TextButton button = entry.getValue();
      boolean active = tab == currentTab;

      button.setColor(active ? TAB_TINT_ACTIVE : TAB_TINT_INACTIVE);
      button.getLabel().setColor(active ? TAB_TEXT_ACTIVE : TEXT_MUTED);

      Image underline = tabUnderlines.get(tab);
      if (underline != null) {
        underline.setColor(active ? GOLD_COLOR : TAB_UNDERLINE_INACTIVE);
      }
    }
  }

  /** Rebuilds the content grid according to the active tab and mode. */
  private void refreshContent() {
    if (contentTable == null) {
      return;
    }

    hideUpgradePopup();
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
      case GAMBLING:
        createGamblingTab();
        break;
      default:
        break;
    }
  }

  /** Renders BUY and SELL toggle controls within the Items tab. */
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

    subTabRow.add(buySubButton).width(95f).height(32f).padRight(8f);
    subTabRow.add(sellSubButton).width(95f).height(32f);

    contentTable.add(subTabRow).left().padBottom(8f);
    contentTable.row();

    updateItemSubTabHighlights();
  }

  /** Refreshes color highlights for BUY/SELL buttons. */
  private void updateItemSubTabHighlights() {
    if (buySubButton == null || sellSubButton == null) {
      return;
    }

    buySubButton.setColor(!sellMode ? BUY_MODE_TINT : TAB_TINT_INACTIVE);
    buySubButton.getLabel().setColor(!sellMode ? BUY_MODE_TEXT : TEXT_MUTED);

    sellSubButton.setColor(sellMode ? SELL_MODE_TINT : TAB_TINT_INACTIVE);
    sellSubButton.getLabel().setColor(sellMode ? SELL_MODE_TEXT : TEXT_MUTED);
  }

  /** Builds the item purchasing catalog grid. */
  private void createItemsTab() {
    ShopComponent shop = entity.getComponent(ShopComponent.class);
    if (shop == null) return;
    createCatalogTab(shop.getItemCatalog(), Item::getName, this::buyItem);
  }

  /** Builds the player inventory grid for selling items. */
  private void createSellGrid() {
    ShopComponent shop = entity.getComponent(ShopComponent.class);
    InventoryComponent inventory = entity.getComponent(InventoryComponent.class);

    if (shop == null || inventory == null) return;

    int displayedSlots = 0;
    for (int slot = 1; slot <= SELL_SLOT_COUNT; slot++) {
      Item item = inventory.getItem(slot);
      addSellCard(shop, item, slot);
      displayedSlots++;

      if (displayedSlots % ITEM_COLUMNS == 0) {
        activeGrid.row();
      }
    }
  }

  /** Creates an inventory item card for the sell view. */
  private void addSellCard(ShopComponent shop, Item item, int slot) {
    Table card = createCard();

    if (item == null) {
      addEmptyCardContent(card, slot);
    } else {
      String name = item.getName();
      int sellPrice = getSellPriceFor(shop, item);
      Rarity rarity = getRarityForPrice(sellPrice);

      addAccentStrip(card, rarity.color);
      card.add(createIconStack(name, rarity)).size(38f, 38f).padBottom(4f);
      card.row();

      Label nameLabel = new Label(name, whiteLabelStyle);
      nameLabel.setColor(TEXT_PRIMARY);
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
              selectCard(card, name, sellPrice, rarity, () -> sellItemAt(slot), "SELL", false);
            }
          });
    }

    addCardToContent(card);
  }

  /** Looks up resale value against catalog pricing. */
  private int getSellPriceFor(ShopComponent shop, Item item) {
    return shop.getSellPrice(item);
  }

  /** Builds the upgrade catalog grid. */
  private void createUpgradesTab() {
    ShopComponent shop = entity.getComponent(ShopComponent.class);
    if (shop == null) return;
    createCatalogTab(shop.getUpgradeCatalog(), ShopComponent.Upgrade::getName, this::buyUpgrade);
  }

  /** Builds the pet catalog grid. */
  private void createPetsTab() {
    ShopComponent shop = entity.getComponent(ShopComponent.class);
    if (shop == null) return;
    createCatalogTab(shop.getPetCatalog(), ShopComponent.Pet::getName, this::buyPet);
  }

  /** Populates a grid with listings up to {@value #ITEM_SLOT_COUNT}. */
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

  /** Creates an interactive catalog listing card. */
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

      if (currentTab == ShopTab.PETS) {
        card.add(createPetIconStack(rarity, name)).size(38f, 38f).padBottom(4f);
      } else {
        card.add(createIconStack(name, rarity)).size(38f, 38f).padBottom(4f);
      }

      card.row();

      Label nameLabel = new Label(name, whiteLabelStyle);
      nameLabel.setColor(TEXT_PRIMARY);
      nameLabel.setAlignment(Align.center);
      card.add(nameLabel).growX().center();
      card.row();

      Label priceLabel = new Label("Gold: " + price, whiteLabelStyle);
      priceLabel.setColor(canAfford(price) ? GOLD_COLOR : INSUFFICIENT_FUNDS_COLOR);
      card.add(priceLabel).padTop(2f).center();

      card.addListener(
          new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
              if (currentTab == ShopTab.UPGRADES) {
                UpgradeNode node = findUpgradeNodeByName(name);
                if (node != null) {
                  showUpgradePopup(node);
                }
                selectCard(
                    card,
                    upgradeDetailName(name, node),
                    price,
                    rarity,
                    () -> attemptUpgradePurchase(catalogSlot, node),
                    "BUY",
                    true);
              } else {
                selectCard(
                    card, name, price, rarity, () -> buyAction.accept(catalogSlot), "BUY", true);
              }
            }
          });
    }

    addCardToContent(card);
  }

  /** Appends a colored top accent strip indicating rarity. */
  private void addAccentStrip(Table card, Color color) {
    Image strip = new Image(skin.getDrawable(BUTTON_BACKGROUND));
    strip.setColor(color);
    card.add(strip).growX().height(ACCENT_STRIP_HEIGHT).padBottom(5f);
    card.row();
  }

  /** Builds an icon badge containing the product's initial letter. */
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

  /** Creates the pet icon from the pet texture atlas. */
  private Stack createPetIconStack(Rarity rarity, String petName) {
    Stack iconStack = new Stack();

    Image iconBackground = new Image(skin.getDrawable(BUTTON_BACKGROUND));
    iconBackground.setColor(rarity.color);
    iconStack.add(iconBackground);

    if (petAtlas != null && petName != null && !petName.isBlank()) {
      String regionName = petName.toLowerCase() + "_right";
      AtlasRegion petRegion = petAtlas.findRegion(regionName, 0);

      if (petRegion != null) {
        Image petImage = new Image(petRegion);
        petImage.setScaling(Scaling.fit);
        iconStack.add(petImage);
        return iconStack;
      }
    }

    Label fallbackLabel = new Label("P", whiteLabelStyle);
    fallbackLabel.setColor(Color.WHITE);
    fallbackLabel.setAlignment(Align.center);
    iconStack.add(fallbackLabel);

    return iconStack;
  }

  /** Converts price into a display rarity tier. */
  private Rarity getRarityForPrice(int price) {
    if (price >= 2000) return Rarity.LEGENDARY;
    if (price >= 1000) return Rarity.EPIC;
    if (price >= 300) return Rarity.RARE;
    return Rarity.COMMON;
  }

  /** Creates the base table shell for a slot card. */
  /** Creates the base table shell for a slot card. */
  private Table createCard() {
    Table card = new Table();
    card.setBackground(skin.getDrawable(BUTTON_BACKGROUND));
    card.setColor(CARD_TINT);
    card.pad(8f);
    card.setTransform(true);
    card.setOrigin(CARD_WIDTH / 2f, CARD_HEIGHT / 2f);
    return card;
  }

  /** Populates a card representing an empty/locked slot. */
  private void addEmptyCardContent(Table card, int slot) {
    card.setColor(EMPTY_CARD_TINT);
    addAccentStrip(card, EMPTY_STRIP_TINT);

    Label slotLabel = new Label("Empty", whiteLabelStyle);
    slotLabel.setColor(TEXT_MUTED);
    card.add(slotLabel).center().expand();
  }

  /** Appends a finished card into the content grid layout. */
  private void addCardToContent(Table card) {
    activeGrid.add(card).size(CARD_WIDTH, CARD_HEIGHT).pad(CARD_GAP);
    int children = activeGrid.getChildren().size;
    if (children % ITEM_COLUMNS == 0) {
      activeGrid.row();
    }
  }

  /** Initializes the bottom detail selection panel. */
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
    detailActionButton.setColor(BUY_MODE_TINT);
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
    detailPanel.add(detailActionButton).size(85f, 36f).right();
  }

  /** Updates selection highlights and populates the bottom detail drawer. */
  /**
   * Updates selection highlights, applies pulsing scale, and attaches a bobbing arrow indicator.
   */
  private void selectCard(
      Table card,
      String name,
      int price,
      Rarity rarity,
      Runnable action,
      String actionLabel,
      boolean requiresAffordability) {

    // Reset previously selected card
    if (selectedCard != null) {
      selectedCard.clearActions();
      selectedCard.setScale(1f);
      selectedCard.setColor(CARD_TINT);
    }

    selectedCard = card;
    selectedCard.setColor(CARD_TINT_SELECTED);

    // Pulse / Breathing animation on selected card
    selectedCard.clearActions();
    selectedCard.addAction(
        Actions.forever(
            Actions.sequence(
                Actions.scaleTo(1.035f, 1.035f, 0.35f), Actions.scaleTo(1.0f, 1.0f, 0.35f))));

    // Attach / Position bobbing indicator arrow
    attachSelectionArrow(card);

    boolean canPerformAction = !requiresAffordability || canAfford(price);
    pendingAction = canPerformAction ? action : null;

    detailIconBg.setColor(rarity.color);
    detailIconLabel.setText(
        (name == null || name.isEmpty()) ? "?" : name.substring(0, 1).toUpperCase());

    detailNameLabel.setText(name);
    detailNameLabel.setColor(TEXT_PRIMARY);

    detailPriceLabel.setText("Gold: " + price);
    if (requiresAffordability && !canPerformAction) {
      detailPriceLabel.setColor(INSUFFICIENT_FUNDS_COLOR);
    } else {
      detailPriceLabel.setColor(GOLD_COLOR);
    }

    detailActionButton.setText(actionLabel);
    detailActionButton.setColor("BUY".equals(actionLabel) ? BUY_MODE_TINT : SELL_MODE_TINT);
    detailActionButton.setDisabled(!canPerformAction);
    detailActionButton.setTouchable(canPerformAction ? Touchable.enabled : Touchable.disabled);
  }

  /** Clears selection highlights and resets detail information. */
  /** Clears selection highlights and resets detail information. */
  private void clearSelection() {
    if (selectedCard != null) {
      selectedCard.clearActions();
      selectedCard.setScale(1f);
      selectedCard.setColor(CARD_TINT);
      selectedCard = null;
    }

    if (selectionArrow != null) {
      selectionArrow.clearActions();
      selectionArrow.setVisible(false);
    }

    pendingAction = null;
    if (detailIconBg == null) return;

    detailIconBg.setColor(EMPTY_CARD_TINT);
    detailIconLabel.setText("");

    detailNameLabel.setText("Select an item to view details");
    detailNameLabel.setColor(TEXT_MUTED);

    detailPriceLabel.setText("");
    detailPriceLabel.setColor(GOLD_COLOR);

    detailActionButton.setText("BUY");
    detailActionButton.setColor(BUY_MODE_TINT);
    detailActionButton.setDisabled(true);
    detailActionButton.setTouchable(Touchable.disabled);
  }

  private UpgradeNode findUpgradeNodeByName(String name) {
    if (upgradesDisplay == null) return null;
    for (UpgradeNode node : upgradesDisplay.getAllUpgrades()) {
      if (node.getName().equals(name)) return node;
    }
    return null;
  }

  private String tierStatusText(UpgradeNode node) {
    return node.isActive()
        ? "Tier " + node.getCurrentTier() + "/" + node.getMaxTier()
        : "Not active";
  }

  private String upgradeDetailName(String name, UpgradeNode node) {
    if (node == null || !node.isActive()) return name;
    return name + " (" + tierStatusText(node) + ")";
  }

  private void attemptUpgradePurchase(int catalogSlot, UpgradeNode node) {
    ShopComponent shop = entity.getComponent(ShopComponent.class);
    if (shop == null) return;

    boolean purchased = shop.buyUpgrade(catalogSlot);
    if (purchased) {
      if (node != null) {
        node.purchaseNextTier();
        syncUpgradeListing(shop, catalogSlot, node);
        showPurchaseToast(node);
      }
      refreshContent();
    }
  }

  private void showUpgradePopup(UpgradeNode node) {
    ensureUpgradePopupCreated();
    populateUpgradePopup(node);
    positionUpgradePopupAboveShop();
    upgradePopup.setVisible(true);
  }

  private void ensureUpgradePopupCreated() {
    if (upgradePopup != null) return;

    upgradePopup = new Table();
    upgradePopup.setBackground(skin.getDrawable(ACCENT_PANEL_BACKGROUND));
    upgradePopup.setColor(PANEL_TINT);
    upgradePopup.pad(PANEL_PADDING);
    upgradePopup.setSize(UPGRADE_POPUP_WIDTH, UPGRADE_POPUP_HEIGHT);

    upgradePopupCloseButton = new TextButton("X", skin);
    upgradePopupCloseButton.setColor(SELL_MODE_TINT);
    upgradePopupCloseButton.addListener(
        new ClickListener() {
          @Override
          public void clicked(InputEvent event, float x, float y) {
            hideUpgradePopup();
          }
        });

    upgradePopupNameLabel = new Label("", whiteLabelStyle);
    upgradePopupNameLabel.setColor(GOLD_COLOR);

    upgradePopupDescriptionLabel = new Label("", whiteLabelStyle);
    upgradePopupDescriptionLabel.setWrap(true);
    upgradePopupDescriptionLabel.setColor(TEXT_PRIMARY);

    upgradePopupTierLabel = new Label("", whiteLabelStyle);
    upgradePopupTierLabel.setColor(GOLD_COLOR);

    upgradePopup.add(upgradePopupCloseButton).size(22f, 22f).right().padBottom(6f);
    upgradePopup.row();
    upgradePopup.add(upgradePopupNameLabel).left().row();
    upgradePopup.add(upgradePopupDescriptionLabel).width(280f).left().padTop(8f).row();
    upgradePopup.add(upgradePopupTierLabel).left().padTop(8f);

    stage.addActor(upgradePopup);
    upgradePopup.setVisible(false);
  }

  private void populateUpgradePopup(UpgradeNode node) {
    upgradePopupNameLabel.setText(node.getName());
    upgradePopupDescriptionLabel.setText(node.getDescription());
    upgradePopupTierLabel.setText(tierStatusText(node));
  }

  private void positionUpgradePopupAboveShop() {
    if (upgradePopup == null) return;

    float screenWidth = stage.getViewport().getWorldWidth();
    float screenHeight = stage.getViewport().getWorldHeight();
    float shopTop = (screenHeight - SHOP_HEIGHT) / 2f + SHOP_HEIGHT;

    float x = (screenWidth - upgradePopup.getWidth()) / 2f;
    float y = shopTop + UPGRADE_POPUP_GAP_ABOVE_SHOP;

    upgradePopup.setPosition(x, y);
  }

  private void hideUpgradePopup() {
    if (upgradePopup != null) {
      upgradePopup.setVisible(false);
    }
  }

  private void showPurchaseToast(UpgradeNode node) {
    ensurePurchaseToastCreated();
    purchaseToastLabel.setText(node.getName() + " activated - Tier " + node.getCurrentTier());
    positionPurchaseToastTopCenter();

    purchaseToast.clearActions();
    purchaseToast.setVisible(true);
    purchaseToast.addAction(
        Actions.sequence(Actions.delay(PURCHASE_TOAST_VISIBLE_SECONDS), Actions.visible(false)));
  }

  private void ensurePurchaseToastCreated() {
    if (purchaseToast != null) return;

    purchaseToast = new Table();
    purchaseToast.setBackground(skin.getDrawable(TOAST_BACKGROUND));
    purchaseToast.setColor(CARD_TINT);
    purchaseToast.pad(PANEL_PADDING);

    purchaseToastLabel = new Label("", whiteLabelStyle);
    purchaseToastLabel.setColor(GOLD_COLOR);

    purchaseToast.add(purchaseToastLabel);
    purchaseToast.pack();

    stage.addActor(purchaseToast);
    purchaseToast.setVisible(false);
  }

  private void positionPurchaseToastTopCenter() {
    purchaseToast.pack();
    float screenWidth = stage.getViewport().getWorldWidth();
    float screenHeight = stage.getViewport().getWorldHeight();

    float x = (screenWidth - purchaseToast.getWidth()) / 2f;
    float y = screenHeight - PURCHASE_TOAST_TOP_MARGIN - purchaseToast.getHeight();

    purchaseToast.setPosition(x, y);
  }

  private void buyItem(int catalogSlot) {
    ShopComponent shop = entity.getComponent(ShopComponent.class);
    if (shop == null) return;
    shop.buyItem(catalogSlot);
  }

  private void sellItemAt(int playerSlot) {
    ShopComponent shop = entity.getComponent(ShopComponent.class);
    if (shop == null) return;
    shop.sellItem(playerSlot);
  }

  private void buyUpgrade(int catalogSlot) {
    ShopComponent shop = entity.getComponent(ShopComponent.class);
    if (shop == null) return;
    shop.buyUpgrade(catalogSlot);
  }

  private void buyPet(int catalogSlot) {
    ShopComponent shop = entity.getComponent(ShopComponent.class);
    if (shop == null) return;
    shop.buyPet(catalogSlot);
  }

  private void refreshShop() {
    if (shopTable == null) return;
    updateGold();
    refreshContent();
  }

  private void updateGold() {
    if (goldLabel != null) {
      goldLabel.setText(getGoldText());
    }
  }

  private String getGoldText() {
    InventoryComponent inventory = entity.getComponent(InventoryComponent.class);
    if (inventory == null) return "Gold: 0";
    return "Gold: " + inventory.getGold();
  }

  private boolean canAfford(int price) {
    InventoryComponent inventory = entity.getComponent(InventoryComponent.class);
    if (inventory == null) return false;
    return inventory.getGold() >= price;
  }

  private void openShop() {
    if (shopTable == null) return;

    currentTab = ShopTab.ITEMS;
    sellMode = false;

    refreshContent();
    positionShop();

    shopTable.setVisible(true);
    shopTable.toFront();
  }

  public void closeShop() {
    if (shopTable == null) return;
    shopTable.setVisible(false);
    hideUpgradePopup();
  }

  private void positionShop() {
    if (shopTable == null) return;

    float screenWidth = stage.getViewport().getWorldWidth();
    float screenHeight = stage.getViewport().getWorldHeight();

    float x = (screenWidth - SHOP_WIDTH) / 2f;
    float y = (screenHeight - SHOP_HEIGHT) / 2f;

    shopTable.setPosition(x, y);
  }

  @Override
  public void draw(SpriteBatch batch) {}

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
    if (petAtlas != null) {
      petAtlas.dispose();
      petAtlas = null;
    }
    if (upgradePopup != null) {
      upgradePopup.remove();
      upgradePopup = null;
    }
    if (purchaseToast != null) {
      purchaseToast.remove();
      purchaseToast = null;
    }
    if (selectionArrow != null) {
      selectionArrow.remove();
      selectionArrow = null;
    }

    contentTable = null;
    activeGrid = null;
    detailPanel = null;
    goldLabel = null;
    selectedCard = null;
    pendingAction = null;

    super.dispose();
  }

  /** Builds the gambling wheel tab with mode selections. */
  private void createGamblingTab() {
    ShopComponent shop = entity.getComponent(ShopComponent.class);
    if (shop == null) return;

    GamblingCatalogs catalogs = shop.getGamblingCatalogs();
    if (catalogs == null) {
      Label unavailable = new Label("Gambling is not available.", whiteLabelStyle);
      unavailable.setColor(TEXT_MUTED);
      activeGrid.add(unavailable).expand().center();
      return;
    }

    Table gamblingRoot = new Table();
    gamblingRoot.defaults().pad(4f);

    Table modeRow = new Table();
    standardGamblingButton = new TextButton("STANDARD", skin);
    premiumGamblingButton = new TextButton("PREMIUM", skin);

    standardGamblingButton.addListener(
        new ClickListener() {
          @Override
          public void clicked(InputEvent event, float x, float y) {
            if (gamblingWheel != null && gamblingWheel.isSpinning()) return;
            currentGamblingCatalog = GamblingCatalogs.CatalogId.STANDARD;
            refreshGamblingWheel();
          }
        });

    premiumGamblingButton.addListener(
        new ClickListener() {
          @Override
          public void clicked(InputEvent event, float x, float y) {
            if (gamblingWheel != null && gamblingWheel.isSpinning()) return;
            currentGamblingCatalog = GamblingCatalogs.CatalogId.PREMIUM;
            refreshGamblingWheel();
          }
        });

    modeRow.add(standardGamblingButton).width(120f).height(32f).padRight(8f);
    modeRow.add(premiumGamblingButton).width(120f).height(32f);
    gamblingRoot.add(modeRow).center().row();

    gamblingWheel = new GamblingWheel(whiteLabelStyle);
    gamblingRoot.add(gamblingWheel).size(300f, 300f).center().padTop(4f).row();

    gamblingPriceLabel = new Label("", whiteLabelStyle);
    gamblingPriceLabel.setColor(GOLD_COLOR);
    gamblingRoot.add(gamblingPriceLabel).center().padTop(2f).row();

    spinButton = new TextButton("SPIN", skin);
    spinButton.setColor(SPIN_BUTTON_TINT);
    spinButton.addListener(
        new ClickListener() {
          @Override
          public void clicked(InputEvent event, float x, float y) {
            spinGamblingWheel();
          }
        });

    gamblingRoot.add(spinButton).width(150f).height(40f).center().padTop(4f).row();

    gamblingResultLabel = new Label("Choose a wheel and spin!", whiteLabelStyle);
    gamblingResultLabel.setColor(TEXT_PRIMARY);
    gamblingResultLabel.setAlignment(Align.center);
    gamblingRoot.add(gamblingResultLabel).center().padTop(2f);

    activeGrid.add(gamblingRoot).grow().center();
    refreshGamblingWheel();
  }

  private void refreshGamblingWheel() {
    ShopComponent shop = entity.getComponent(ShopComponent.class);
    if (shop == null || gamblingWheel == null) return;

    GamblingCatalogs catalogs = shop.getGamblingCatalogs();
    if (catalogs == null) return;

    GamblingCatalogs.SpinCatalog catalog = catalogs.get(currentGamblingCatalog);
    if (catalog == null) return;

    gamblingWheel.setCatalog(currentGamblingCatalog, catalog);
    gamblingPriceLabel.setText("Spin Cost: " + catalog.getSpinPrice() + " Gold");

    boolean canAfford = canAfford(catalog.getSpinPrice());
    spinButton.setDisabled(!canAfford);
    spinButton.setTouchable(canAfford ? Touchable.enabled : Touchable.disabled);
    gamblingPriceLabel.setColor(canAfford ? GOLD_COLOR : INSUFFICIENT_FUNDS_COLOR);

    updateGamblingModeButtons();
  }

  private void updateGamblingModeButtons() {
    if (standardGamblingButton == null || premiumGamblingButton == null) return;

    boolean standard = currentGamblingCatalog == GamblingCatalogs.CatalogId.STANDARD;
    standardGamblingButton.setColor(standard ? TAB_TINT_ACTIVE : TAB_TINT_INACTIVE);
    premiumGamblingButton.setColor(!standard ? TAB_TINT_ACTIVE : TAB_TINT_INACTIVE);

    standardGamblingButton.getLabel().setColor(standard ? TAB_TEXT_ACTIVE : TEXT_MUTED);
    premiumGamblingButton.getLabel().setColor(!standard ? TAB_TEXT_ACTIVE : TEXT_MUTED);
  }

  private void spinGamblingWheel() {
    ShopComponent shop = entity.getComponent(ShopComponent.class);
    if (shop == null || gamblingWheel == null || gamblingWheel.isSpinning()) return;

    GamblingCatalogs catalogs = shop.getGamblingCatalogs();
    if (catalogs == null) return;

    GamblingCatalogs.SpinCatalog catalog = catalogs.get(currentGamblingCatalog);
    if (catalog == null) return;

    if (!canAfford(catalog.getSpinPrice())) {
      gamblingResultLabel.setText("Not enough gold.");
      gamblingResultLabel.setColor(INSUFFICIENT_FUNDS_COLOR);
      return;
    }

    GamblingCatalogs.PrizeEntry<?> result = shop.buySpin(currentGamblingCatalog);
    if (result == null) {
      gamblingResultLabel.setText("Spin failed.");
      gamblingResultLabel.setColor(INSUFFICIENT_FUNDS_COLOR);
      refreshGamblingWheel();
      return;
    }

    int winningSlot = findPrizeSlot(catalog, result);
    if (winningSlot < 1) {
      gamblingResultLabel.setText("Prize received.");
      gamblingResultLabel.setColor(GOLD_COLOR);
      refreshGamblingWheel();
      return;
    }

    spinButton.setDisabled(true);
    spinButton.setTouchable(Touchable.disabled);

    gamblingWheel.spinToSlot(
        winningSlot,
        () -> {
          gamblingResultLabel.setText("You won: " + getPrizeName(result));
          gamblingResultLabel.setColor(GOLD_COLOR);
          refreshGamblingWheel();
        });
  }

  private int findPrizeSlot(
      GamblingCatalogs.SpinCatalog catalog, GamblingCatalogs.PrizeEntry<?> result) {
    for (int slot = 1; slot <= GamblingCatalogs.SpinCatalog.PRIZE_SLOT_COUNT; slot++) {
      GamblingCatalogs.PrizeEntry<?> prize = catalog.getPrize(slot);
      if (prize == result) {
        return slot;
      }
    }
    return -1;
  }

  private String getPrizeName(GamblingCatalogs.PrizeEntry<?> prize) {
    if (prize == null) return "?";

    Object product = prize.getProduct();
    if (product instanceof GamblingCatalogs.ItemPrize itemPrize) {
      return itemPrize.getDisplayName();
    }
    if (product instanceof GamblingCatalogs.GoldPrize goldPrize) {
      return goldPrize.getAmount() + " Gold";
    }
    if (product instanceof ShopComponent.Pet pet) {
      return pet.getName();
    }
    if (product instanceof ShopComponent.Upgrade upgrade) {
      return upgrade.getName();
    }

    return "Unknown Prize";
  }

  /** Places an animated pointer arrow right on top of the selected card. */
  private void attachSelectionArrow(Table targetCard) {
    if (selectionArrow == null) {
      selectionArrow = new Label("▼", whiteLabelStyle);
      selectionArrow.setColor(SELECTION_ARROW_COLOR);
      selectionArrow.setAlignment(Align.center);
      selectionArrow.setFontScale(1.4f);
      selectionArrow.setTouchable(Touchable.disabled);
      stage.addActor(selectionArrow);
    }

    selectionArrow.setVisible(true);
    selectionArrow.clearActions();

    // Calculate position in stage coordinates
    com.badlogic.gdx.math.Vector2 cardStagePos =
        targetCard.localToStageCoordinates(
            new com.badlogic.gdx.math.Vector2(targetCard.getWidth() / 2f, targetCard.getHeight()));

    float startX = cardStagePos.x - selectionArrow.getWidth() / 2f;
    float startY = cardStagePos.y + 4f;

    selectionArrow.setPosition(startX, startY);

    // Continuous bobbing animation (moves up & down by 5px)
    selectionArrow.addAction(
        Actions.forever(
            Actions.sequence(Actions.moveBy(0f, 6f, 0.3f), Actions.moveBy(0f, -6f, 0.3f))));
  }
}
