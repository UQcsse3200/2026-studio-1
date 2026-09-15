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
 * Displays the player's shop interface using a dark, MOBA-style layout.
 *
 * <p>The shop provides three categories:
 *
 * <ul>
 *   <li>Items - BUY and SELL modes
 *   <li>Upgrades
 *   <li>Pets
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

  private static final float PANEL_PADDING = 12f;
  private static final float CARD_GAP = 6f;

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

  // Used only by the Upgrades-tab popup and purchase toast, to visually match the shop's dark
  // green look - "window-c" is a Skin$TintedDrawable (name: window, color: color) with its green
  // baked in at skin-load time, the same safe mechanism as the skin's "black" drawable, NOT a
  // live Actor.setColor() tint. Kept distinct from WINDOW_BACKGROUND ("window-w", baked orange)
  // since shopTable itself is unaffected by this change.
  private static final String ACCENT_PANEL_BACKGROUND = "window-c";

  // Used only by the purchase-confirmation toast - "toast-charcoal" is a Skin$TintedDrawable
  // (name: white, color: toast-charcoal-color) added to flat-earth-ui.json using the identical
  // config-time-baked mechanism as the skin's existing "black" entry, just with CARD_TINT's exact
  // RGB (0.12, 0.13, 0.18) baked in instead, so the toast matches the shop's own card color
  // precisely. No live setColor() call involved either way.
  private static final String TOAST_BACKGROUND = "toast-charcoal";
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
  private static final Color INSUFFICIENT_FUNDS_COLOR = new Color(0.90f, 0.25f, 0.25f, 1f);
  private static final Color TAB_TEXT_ACTIVE = new Color(0.91f, 0.77f, 0.42f, 1f);

  private static final Color GOLD_PILL_TINT = new Color(0.22f, 0.18f, 0.09f, 1f);
  private static final Color DIVIDER_TINT = new Color(0.91f, 0.77f, 0.42f, 0.30f);
  private static final Color EMPTY_STRIP_TINT = new Color(0.42f, 0.45f, 0.52f, 0.25f);

  private static final Color BUY_MODE_TINT = new Color(0.20f, 0.22f, 0.28f, 1f);
  private static final Color SELL_MODE_TINT = new Color(0.32f, 0.15f, 0.15f, 1f);

  private static final Color RARITY_COMMON = new Color(0.42f, 0.45f, 0.52f, 1f);
  private static final Color RARITY_RARE = new Color(0.24f, 0.55f, 0.85f, 1f);
  private static final Color RARITY_EPIC = new Color(0.64f, 0.35f, 0.85f, 1f);
  private static final Color RARITY_LEGENDARY = new Color(0.91f, 0.77f, 0.42f, 1f);

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
    PETS
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

  // Items tab: BUY vs SELL.
  private boolean sellMode = false;
  private TextButton buySubButton;
  private TextButton sellSubButton;

  // Currently selected card.
  private Table selectedCard;
  private Runnable pendingAction;

  // Detail panel.
  private Image detailIconBg;
  private Label detailIconLabel;
  private Label detailNameLabel;
  private Label detailPriceLabel;
  private TextButton detailActionButton;

  // Real upgrade data, supplied late (once the player entity + UpgradesDisplay both exist) via
  // setUpgradesDisplay() - same late-binding pattern UpgradesDisplay itself uses for setPlayer().
  private UpgradesDisplay upgradesDisplay;

  // Upgrades-tab-only informational popup shown ALONGSIDE the normal selectCard()/detail-panel
  // flow: a single reusable Table positioned in the empty space above the shop window, hidden by
  // default and repopulated per upgrade on each click. Purely informational - name, description,
  // tier/status, and a close (X) button. No purchase action lives here.
  //
  // Rebuilt after root-causing a rendering bug in an earlier version of this popup: its
  // background Table was tinted via setColor(customColor) and brought to the front via
  // toFront() every time it was shown. Table backgrounds are drawn through
  // batch.setColor(tableColor) + batch.draw(region,...) - the standard region-based path that
  // DOES persist in the shared SpriteBatch's color state after drawing, unlike Label text
  // (drawn via BitmapFontCache's vertex-array batch.draw() overload, which never touches the
  // batch's color at all). Since nothing after this popup in a frame necessarily resets that
  // color back to white, and since toFront() guaranteed it was among the last things drawn, its
  // tint leaked into every world sprite drawn on the next frame until the popup stopped being
  // last-drawn. This rebuild never tints the Table itself and never reorders it - see
  // ensureUpgradePopupCreated()'s comments for exactly how.
  private Table upgradePopup;
  private Label upgradePopupNameLabel;
  private Label upgradePopupDescriptionLabel;
  private Label upgradePopupTierLabel;
  private TextButton upgradePopupCloseButton;

  // Purchase-confirmation toast: a small top-center "<Name> activated - Tier <N>" notification
  // shown briefly right after a successful upgrade purchase. Built with the same two safety
  // rules confirmed by the two rendering bugs already root-caused in this file: (1) no fade -
  // Actions.visible(false) is a scheduled flag flip, never an alpha/color transition, so the
  // Table is always either fully opaque or fully invisible, never partially so; (2) the Table's
  // background is the skin's own untinted window drawable - no setColor() call on it or on any
  // non-text element - so its background always draws with the Table's default white Actor
  // color, exactly like the informational popup this mirrors. Never toFront()'d either.
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
   * Supplies the real upgrade system so the Upgrades tab can show real upgrades (name + current
   * next-tier cost) instead of ShopComponent's placeholder stubs, and so its popup can look up the
   * matching UpgradeNode to actually apply a purchase. Called once both this entity's ShopDisplay
   * and the UI entity's UpgradesDisplay exist - see MainGameScreen, which wires this up the same
   * way it calls upgradesDisplay.setPlayer(...).
   */
  public void setUpgradesDisplay(UpgradesDisplay upgradesDisplay) {
    this.upgradesDisplay = upgradesDisplay;
    syncUpgradeCatalog();
    refreshContent();
  }

  /**
   * Replaces ShopComponent's Upgrade catalog listings with one real entry per UpgradeNode (name +
   * current next-tier cost), in {@link UpgradesDisplay#getAllUpgrades()} order starting at slot 1.
   * ShopComponent stays fully decoupled from the perks package - this only ever calls its existing
   * public setUpgradeListing(), it never gains any UpgradeNode-specific logic of its own.
   */
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

  /**
   * Writes/refreshes one catalog slot's listing so its price matches {@code node}'s current
   * next-tier cost.
   */
  private void syncUpgradeListing(ShopComponent shop, int catalogSlot, UpgradeNode node) {
    int cost = node.isMaxTier() ? 0 : node.getNextTierCost();

    shop.setUpgradeListing(
        catalogSlot,
        new ShopComponent.ShopListing<>(new ShopComponent.Upgrade(node.getName()), cost));
  }

  /** Toggles the shop window open/closed. */
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

    shopTable.setVisible(false);
  }

  /** Creates the shop header. */
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

  /** Adds a divider below the shop header. */
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

  /** Adds one tab button to the tab bar. */
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

  /** Updates the active tab highlight. */
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

  /** Refreshes the currently selected tab. */
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

      default:
        break;
    }
  }

  /** Creates the BUY/SELL buttons for the Items tab. */
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

  /** Updates the BUY/SELL sub-tab highlights. */
  private void updateItemSubTabHighlights() {
    if (buySubButton == null || sellSubButton == null) {
      return;
    }

    buySubButton.setColor(!sellMode ? BUY_MODE_TINT : TAB_TINT_INACTIVE);
    buySubButton.getLabel().setColor(Color.WHITE);

    sellSubButton.setColor(sellMode ? SELL_MODE_TINT : TAB_TINT_INACTIVE);
    sellSubButton.getLabel().setColor(Color.WHITE);
  }

  /** Creates the Items BUY grid. */
  private void createItemsTab() {
    ShopComponent shop = entity.getComponent(ShopComponent.class);

    if (shop == null) {
      return;
    }

    createCatalogTab(shop.getItemCatalog(), Item::getName, this::buyItem);
  }

  /** Creates the Items SELL grid. */
  private void createSellGrid() {
    ShopComponent shop = entity.getComponent(ShopComponent.class);

    InventoryComponent inventory = entity.getComponent(InventoryComponent.class);

    if (shop == null || inventory == null) {
      return;
    }

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

  /** Creates one SELL card. */
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

              selectCard(card, name, sellPrice, rarity, () -> sellItemAt(slot), "SELL", false);
            }
          });
    }

    addCardToContent(card);
  }

  /** Gets the item's sell price. */
  private int getSellPriceFor(ShopComponent shop, Item item) {

    return shop.getSellPrice(item);
  }

  /** Creates the Upgrades tab. */
  private void createUpgradesTab() {
    ShopComponent shop = entity.getComponent(ShopComponent.class);

    if (shop == null) {
      return;
    }

    createCatalogTab(shop.getUpgradeCatalog(), ShopComponent.Upgrade::getName, this::buyUpgrade);
  }

  /** Creates the Pets tab. */
  private void createPetsTab() {
    ShopComponent shop = entity.getComponent(ShopComponent.class);

    if (shop == null) {
      return;
    }

    createCatalogTab(shop.getPetCatalog(), ShopComponent.Pet::getName, this::buyPet);
  }

  /** Creates a catalog grid for Items, Upgrades or Pets. */
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

  /** Creates one catalog card. */
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

      /*
       * PETS:
       * Use the real pet sprite from pet.atlas.
       *
       * ITEMS / UPGRADES:
       * Continue using the placeholder icon.
       */
      if (currentTab == ShopTab.PETS) {

        card.add(createPetIconStack(rarity, catalogSlot)).size(40f, 40f).padBottom(4f);

      } else {

        card.add(createIconStack(name, rarity)).size(40f, 40f).padBottom(4f);
      }

      card.row();

      Label nameLabel = new Label(name, whiteLabelStyle);

      nameLabel.setColor(Color.WHITE);
      nameLabel.setAlignment(Align.center);

      card.add(nameLabel).growX().center();

      card.row();

      Label priceLabel = new Label("Gold: " + price, whiteLabelStyle);

      /*
       * Show unaffordable purchase prices in red.
       */
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

  /** Adds a rarity-colored strip to the top of a card. */
  private void addAccentStrip(Table card, Color color) {

    Image strip = new Image(skin.getDrawable(BUTTON_BACKGROUND));

    strip.setColor(color);

    card.add(strip).growX().height(ACCENT_STRIP_HEIGHT).padBottom(6f);

    card.row();
  }

  /** Creates the placeholder icon used by Items and Upgrades. */
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
   * Creates the pet icon corresponding to the pet's catalog slot.
   *
   * <p>The catalog slot is converted to an atlas frame index:
   *
   * <pre>
   * catalog slot 1 -> idle_right index 0
   * catalog slot 2 -> idle_right index 1
   * catalog slot 3 -> idle_right index 2
   * catalog slot 4 -> idle_right index 3
   * ...
   * </pre>
   *
   * <p>If the atlas does not contain a matching frame, a fallback "P" icon is displayed.
   */
  private Stack createPetIconStack(Rarity rarity, int catalogSlot) {

    Stack iconStack = new Stack();

    Image iconBackground = new Image(skin.getDrawable(BUTTON_BACKGROUND));

    iconBackground.setColor(rarity.color);

    iconStack.add(iconBackground);

    if (petAtlas != null && catalogSlot > 0) {

      int frameIndex = catalogSlot - 1;

      AtlasRegion petRegion = petAtlas.findRegion("idle_right", frameIndex);

      if (petRegion != null) {

        Image petImage = new Image(petRegion);

        petImage.setScaling(Scaling.fit);

        iconStack.add(petImage);

        return iconStack;
      }
    }

    /*
     * Fallback if:
     * - the atlas is not loaded, or
     * - the requested pet frame does not exist.
     */
    Label fallbackLabel = new Label("P", whiteLabelStyle);

    fallbackLabel.setColor(Color.WHITE);
    fallbackLabel.setAlignment(Align.center);

    iconStack.add(fallbackLabel);

    return iconStack;
  }

  /** Converts price into a simple display rarity. */
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

  /** Creates an empty dark shop card. */
  private Table createCard() {

    Table card = new Table();

    card.setBackground(skin.getDrawable(BUTTON_BACKGROUND));

    card.setColor(CARD_TINT);

    card.pad(8f);

    return card;
  }

  /** Creates an empty/locked slot. */
  private void addEmptyCardContent(Table card, int slot) {

    card.setColor(EMPTY_CARD_TINT);

    addAccentStrip(card, EMPTY_STRIP_TINT);

    Label slotLabel = new Label("Empty", whiteLabelStyle);

    slotLabel.setColor(TEXT_MUTED);

    card.add(slotLabel).center().expand();
  }

  /** Adds a card to the active grid. */
  private void addCardToContent(Table card) {

    activeGrid.add(card).size(CARD_WIDTH, CARD_HEIGHT).pad(CARD_GAP);

    int children = activeGrid.getChildren().size;

    if (children % ITEM_COLUMNS == 0) {
      activeGrid.row();
    }
  }

  /** Creates the bottom detail/action panel. */
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
   * Selects a card and updates the detail panel.
   *
   * @param requiresAffordability true for BUY actions, false for SELL actions
   */
  private void selectCard(
      Table card,
      String name,
      int price,
      Rarity rarity,
      Runnable action,
      String actionLabel,
      boolean requiresAffordability) {

    if (selectedCard != null) {
      selectedCard.setColor(CARD_TINT);
    }

    selectedCard = card;
    selectedCard.setColor(CARD_TINT_SELECTED);

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
    detailActionButton.setDisabled(!canPerformAction);
    detailActionButton.setTouchable(canPerformAction ? Touchable.enabled : Touchable.disabled);
  }

  /** Clears the current selection. */
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
    detailPriceLabel.setColor(GOLD_COLOR);

    detailActionButton.setText("BUY");
    detailActionButton.setDisabled(true);
    detailActionButton.setTouchable(Touchable.disabled);
  }

  /**
   * Finds the UpgradeNode whose name matches {@code name}, or null if none/not yet wired up.
   * ShopComponent's Upgrade stub only carries a name, so name is the join key between the two
   * systems.
   */
  private UpgradeNode findUpgradeNodeByName(String name) {
    if (upgradesDisplay == null) {
      return null;
    }

    for (UpgradeNode node : upgradesDisplay.getAllUpgrades()) {
      if (node.getName().equals(name)) {
        return node;
      }
    }

    return null;
  }

  /**
   * "Tier X/Y" when {@code node} is currently active, or "Not active" otherwise - the single source
   * of tier-status text shared by both the bottom detail panel (via upgradeDetailName()) and the
   * informational popup (via populateUpgradePopup()), so the two never drift apart.
   */
  private String tierStatusText(UpgradeNode node) {
    return node.isActive()
        ? "Tier " + node.getCurrentTier() + "/" + node.getMaxTier()
        : "Not active";
  }

  /**
   * Builds the name shown in the bottom detail panel for an Upgrades-tab card: the plain name, plus
   * " (Tier X/Y)" when the upgrade is currently active - Items/Pets never call this, they pass
   * their plain name into selectCard() exactly as before.
   */
  private String upgradeDetailName(String name, UpgradeNode node) {
    if (node == null || !node.isActive()) {
      return name;
    }

    return name + " (" + tierStatusText(node) + ")";
  }

  /**
   * Runs the bottom detail panel's BUY action for an Upgrades-tab card: deducts gold and records
   * the purchase via {@link ShopComponent#buyUpgrade(int)}, and ONLY if that actually succeeded,
   * applies the real gameplay effect via {@link UpgradeNode#purchaseNextTier()} - so a failed (e.g.
   * unaffordable) purchase never advances the upgrade's tier. On success, also refreshes this
   * catalog slot's listing to the new next-tier cost and rebuilds the shop content so the grid
   * immediately reflects the updated price/tier/gold.
   *
   * @param node the matching UpgradeNode, or null if none was found (defensive - shouldn't happen
   *     once wired via setUpgradesDisplay(); the ShopComponent-side purchase still runs, just
   *     without applying a gameplay effect)
   */
  private void attemptUpgradePurchase(int catalogSlot, UpgradeNode node) {
    ShopComponent shop = entity.getComponent(ShopComponent.class);

    if (shop == null) {
      return;
    }

    boolean purchased = shop.buyUpgrade(catalogSlot);

    if (purchased) {
      if (node != null) {
        node.purchaseNextTier();
        syncUpgradeListing(shop, catalogSlot, node);
        showPurchaseToast(node); // after purchaseNextTier() so the tier shown is the new one
      }
      refreshContent(); // also hides the informational popup - see hideUpgradePopup()
    }
  }

  /**
   * Shows the Upgrades-tab-only informational popup for {@code node}, in the empty space above the
   * shop window. Purely informational: name, description, current tier/status, and a close (X)
   * button - no purchase action here.
   */
  private void showUpgradePopup(UpgradeNode node) {
    ensureUpgradePopupCreated();
    populateUpgradePopup(node);
    positionUpgradePopupAboveShop();

    upgradePopup.setVisible(true);
    // Deliberately NOT calling toFront() - see the field-level comment on upgradePopup for why.
    // It's added to the stage after shopTable/shopIconButton, so it already renders above them
    // through plain insertion order alone; forcing it further to the very end of the whole
    // stage's draw order is exactly what caused the original bug and isn't needed here.
  }

  /**
   * Builds the (initially hidden) upgrade popup once, reused for every subsequent click.
   *
   * <p>Safety, per the confirmed root cause: the Table itself is never given a custom background
   * tint - only the skin's own untinted window drawable, so its background draws with
   * batch.setColor(1,1,1,1) (the Table's own default Actor color), never anything else. The close
   * button is likewise never tinted (default white). Neither is ever the source of a lingering
   * non-white batch color, regardless of draw order. The three Labels DO have non-white colors
   * (TEXT_PRIMARY/TEXT_MUTED/GOLD_COLOR, matching the rest of this UI) - that's safe specifically
   * because Label text is drawn via BitmapFontCache's vertex-array batch.draw() overload, which
   * (verified against the libGDX source) never reads or writes the batch's color state at all, so
   * no Label anywhere in this codebase can cause this bug.
   */
  private void ensureUpgradePopupCreated() {
    if (upgradePopup != null) {
      return;
    }

    upgradePopup = new Table();
    // untinted - no setColor() call; "window-c"'s green is baked into the drawable itself
    upgradePopup.setBackground(skin.getDrawable(ACCENT_PANEL_BACKGROUND));
    upgradePopup.pad(PANEL_PADDING);
    upgradePopup.setSize(UPGRADE_POPUP_WIDTH, UPGRADE_POPUP_HEIGHT);

    upgradePopupCloseButton = new TextButton("X", skin); // untinted - stays default white
    upgradePopupCloseButton.addListener(
        new ClickListener() {
          @Override
          public void clicked(InputEvent event, float x, float y) {
            hideUpgradePopup();
          }
        });

    upgradePopupNameLabel = new Label("", whiteLabelStyle);
    upgradePopupNameLabel.setColor(TEXT_PRIMARY);

    upgradePopupDescriptionLabel = new Label("", whiteLabelStyle);
    upgradePopupDescriptionLabel.setWrap(true);
    upgradePopupDescriptionLabel.setColor(TEXT_MUTED);

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

  /**
   * Fills the popup with {@code node}'s current name/description/tier - purely display, no action.
   * Reuses tierStatusText() rather than recomputing tier text separately, the same helper
   * upgradeDetailName() uses for the bottom detail panel.
   */
  private void populateUpgradePopup(UpgradeNode node) {
    upgradePopupNameLabel.setText(node.getName());
    upgradePopupDescriptionLabel.setText(node.getDescription());
    upgradePopupTierLabel.setText(tierStatusText(node));
  }

  /**
   * Positions the upgrade popup in the empty space ABOVE the shop window (where the player sprite
   * and open world are visible), rather than centered over the card grid - so it never blocks
   * access to other cards while open. Anchored to the shop's own top edge (not an absolute screen
   * position) so it can never overlap the shop regardless of screen size.
   */
  private void positionUpgradePopupAboveShop() {
    if (upgradePopup == null) {
      return;
    }

    float screenWidth = stage.getViewport().getWorldWidth();
    float screenHeight = stage.getViewport().getWorldHeight();

    float shopTop = (screenHeight - SHOP_HEIGHT) / 2f + SHOP_HEIGHT;

    float x = (screenWidth - upgradePopup.getWidth()) / 2f;
    float y = shopTop + UPGRADE_POPUP_GAP_ABOVE_SHOP;

    upgradePopup.setPosition(x, y);
  }

  /**
   * Hides the upgrade popup, if it exists. Called whenever the shop's content is rebuilt/closed,
   * and by the popup's own close (X) button.
   */
  private void hideUpgradePopup() {
    if (upgradePopup != null) {
      upgradePopup.setVisible(false);
    }
  }

  /**
   * Shows a brief "<Name> activated - Tier <N>" confirmation toast, top-center of the screen, after
   * a successful upgrade purchase. Auto-hides itself after {@link #PURCHASE_TOAST_VISIBLE_SECONDS}
   * - see {@link #ensurePurchaseToastCreated()} for exactly why this is safe against both rendering
   * bugs already root-caused in this file.
   */
  private void showPurchaseToast(UpgradeNode node) {
    ensurePurchaseToastCreated();

    purchaseToastLabel.setText(node.getName() + " activated - Tier " + node.getCurrentTier());
    positionPurchaseToastTopCenter();

    // clearActions() first so repeated purchases in quick succession restart the visible window
    // from full length, rather than queuing up multiple hide actions.
    purchaseToast.clearActions();
    purchaseToast.setVisible(true);
    // Binary visibility only - a scheduled flag flip, never an alpha/color transition. No
    // Actions.fadeOut() or any other tween: this was the exact cause of the first darkening bug.
    purchaseToast.addAction(
        Actions.sequence(Actions.delay(PURCHASE_TOAST_VISIBLE_SECONDS), Actions.visible(false)));
    // Deliberately NOT calling toFront() - same reasoning as showUpgradePopup().
  }

  /**
   * Builds the (initially hidden) purchase toast once, reused for every subsequent purchase.
   *
   * <p>Safety, per both rendering bugs already root-caused in this file: the Table's background is
   * "toast-charcoal" (Skin$TintedDrawable: name "white", color "toast-charcoal-color", added to
   * flat-earth-ui.json using the identical pattern as the skin's own "black" entry) - its dark tone
   * is baked in at skin-load time, never a custom setColor() tint applied to the Table itself,
   * exactly the fix that resolved the informational popup's darkening bug. The Label uses
   * whiteLabelStyle as-is (plain white text, no further setColor() call), which is safe regardless:
   * Label text draws via BitmapFontCache's vertex-array batch.draw() overload, which never touches
   * the shared SpriteBatch's color state. Never toFront()'d, and see showPurchaseToast() for why
   * hiding it uses a plain scheduled setVisible(false) rather than any fade.
   */
  private void ensurePurchaseToastCreated() {
    if (purchaseToast != null) {
      return;
    }

    purchaseToast = new Table();
    // untinted - no setColor() call; the dark tone is baked into the drawable itself
    purchaseToast.setBackground(skin.getDrawable(TOAST_BACKGROUND));
    purchaseToast.pad(PANEL_PADDING);

    purchaseToastLabel = new Label("", whiteLabelStyle); // whiteLabelStyle is already white

    purchaseToast.add(purchaseToastLabel);
    purchaseToast.pack();

    stage.addActor(purchaseToast);

    purchaseToast.setVisible(false);
  }

  /**
   * Positions the purchase toast centered horizontally, near the top of the screen - distinct from
   * the bottom detail panel and the above-shop informational popup.
   */
  private void positionPurchaseToastTopCenter() {
    purchaseToast.pack(); // resize to fit the current text before positioning

    float screenWidth = stage.getViewport().getWorldWidth();
    float screenHeight = stage.getViewport().getWorldHeight();

    float x = (screenWidth - purchaseToast.getWidth()) / 2f;
    float y = screenHeight - PURCHASE_TOAST_TOP_MARGIN - purchaseToast.getHeight();

    purchaseToast.setPosition(x, y);
  }

  /** Attempts to purchase an item. */
  private void buyItem(int catalogSlot) {

    ShopComponent shop = entity.getComponent(ShopComponent.class);

    if (shop == null) {
      return;
    }

    shop.buyItem(catalogSlot);
  }

  /** Attempts to sell an item. */
  private void sellItemAt(int playerSlot) {

    ShopComponent shop = entity.getComponent(ShopComponent.class);

    if (shop == null) {
      return;
    }

    shop.sellItem(playerSlot);
  }

  /** Attempts to purchase an upgrade. */
  private void buyUpgrade(int catalogSlot) {

    ShopComponent shop = entity.getComponent(ShopComponent.class);

    if (shop == null) {
      return;
    }

    shop.buyUpgrade(catalogSlot);
  }

  /** Attempts to purchase a pet. */
  private void buyPet(int catalogSlot) {

    ShopComponent shop = entity.getComponent(ShopComponent.class);

    if (shop == null) {
      return;
    }

    shop.buyPet(catalogSlot);
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

  /** Gets the player's current gold. */
  private String getGoldText() {

    InventoryComponent inventory = entity.getComponent(InventoryComponent.class);

    if (inventory == null) {
      return "Gold: 0";
    }

    return "Gold: " + inventory.getGold();
  }

  /** Checks whether the player has enough gold to purchase the given price. */
  private boolean canAfford(int price) {

    InventoryComponent inventory = entity.getComponent(InventoryComponent.class);

    if (inventory == null) {
      return false;
    }

    return inventory.getGold() >= price;
  }

  /** Opens the shop. */
  private void openShop() {

    if (shopTable == null) {
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
    hideUpgradePopup();
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

  @Override
  public void draw(SpriteBatch batch) {
    // Scene2D stage handles shop rendering.
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

    contentTable = null;
    activeGrid = null;
    detailPanel = null;
    goldLabel = null;
    selectedCard = null;
    pendingAction = null;

    super.dispose();
  }
}
