package com.csse3200.game.components.player;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Dialog;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;
import com.csse3200.game.components.loot.Item;
import com.csse3200.game.ui.UIComponent;
import com.csse3200.game.upgrades.UpgradeNode;
import com.csse3200.game.upgrades.UpgradesDisplay;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
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
 *
 * <p>Item / upgrade / tab / coin icons are PNGs in {@code source/core/assets/images/icons/}, loaded
 * by {@link #loadIcon(String)}. When a PNG is missing the original letter badge is shown.
 */
public class ShopDisplay extends UIComponent {

  private static final float SHOP_WIDTH = 650f;
  private static final float SHOP_HEIGHT = 600f;

  private static final float CARD_WIDTH = 175f;
  private static final float CARD_HEIGHT = 110f;

  private static final float PANEL_PADDING = 14f;
  private static final float CARD_GAP = 8f;

  private static final float UPGRADE_POPUP_WIDTH = 280f;
  private static final float UPGRADE_POPUP_GAP_ABOVE_SHOP = 16f;

  private static final float PURCHASE_TOAST_TOP_MARGIN = 24f;
  private static final float PURCHASE_TOAST_VISIBLE_SECONDS = 1.75f;

  private static final int ITEM_COLUMNS = 3;
  private static final int ITEM_SLOT_COUNT = 10;
  private static final int SELL_SLOT_COUNT = 5;

  private static final String LABEL_STYLE = "small";
  private static final String WINDOW_BACKGROUND = "window";
  private static final String EMPTY_TEXT = "Empty";

  // Used only by the Upgrades-tab popup and purchase toast, to visually match the shop's dark
  // green look - "window-c" is a Skin$TintedDrawable (name: window, color: color) with its green
  // baked in at skin-load time, the same safe mechanism as the skin's "black" drawable, NOT a
  // live Actor.setColor() tint. Kept distinct from WINDOW_BACKGROUND ("window-w", baked orange)
  // since shopTable itself is unaffected by this change.
  private static final String ACCENT_PANEL_BACKGROUND = "stone";

  // Used only by the purchase-confirmation toast - "toast-charcoal" is a Skin$TintedDrawable
  // (name: white, color: toast-charcoal-color) added to flat-earth-ui.json using the identical
  // config-time-baked mechanism as the skin's existing "black" entry, just with CARD_TINT's exact
  // RGB (0.12, 0.13, 0.18) baked in instead, so the toast matches the shop's own card color
  // precisely. No live setColor() call involved either way.
  private static final String TOAST_BACKGROUND = "shadow";
  private static final String BUTTON_BACKGROUND = "button";
  private static final String FLAT_BACKGROUND = "white";

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

  // --- Icon PNGs: source/core/assets/images/icons/ (libGDX paths are relative to assets/) ----
  private static final String ICON_POTION_RED = "images/icons/potion_red.png";
  private static final String ICON_POTION_BLUE = "images/icons/potion_blue.png";
  private static final String ICON_POTION_BROWN = "images/icons/potion_brown.png";
  private static final String ICON_SWORD = "images/icons/sword.png";
  private static final String ICON_BOW = "images/icons/bow.png";
  private static final String ICON_UPGRADE_HEALTH = "images/icons/upgrade_health.png";
  private static final String ICON_UPGRADE_SPEED = "images/icons/upgrade_speed.png";
  private static final String ICON_UPGRADE_SHIELD = "images/icons/upgrade_shield.png";
  private static final String ICON_TAB_ITEMS = "images/icons/tab_items.png";
  private static final String ICON_TAB_UPGRADES = "images/icons/tab_upgrades.png";
  private static final String ICON_TAB_GAMBLING = "images/icons/tab_gambling.png";
  private static final String ICON_COIN = "images/icons/coin.png";
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
    ICON_RULES.put("health", ICON_UPGRADE_HEALTH);
    ICON_RULES.put("heart", ICON_UPGRADE_HEALTH);
    ICON_RULES.put("speed", ICON_UPGRADE_SPEED);
    ICON_RULES.put("boot", ICON_UPGRADE_SPEED);
    ICON_RULES.put("shield", ICON_UPGRADE_SHIELD);
    ICON_RULES.put("armor", ICON_UPGRADE_SHIELD);
    ICON_RULES.put("defen", ICON_UPGRADE_SHIELD);
  }

  private final Map<String, Drawable> iconCache = new HashMap<>();
  private final List<Texture> iconTextures = new ArrayList<>();

  private Actor batchColorReset;

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
  private ScrollPane contentScroll;
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
  private Image detailIconImage;
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
    // Lets other components (e.g. the shopkeeper NPC) open the shop.
    entity.getEvents().addListener("openShop", this::openShop);

    petAtlas = new TextureAtlas(Gdx.files.internal("images/pet.atlas"));

    createShop();
    createShopIcon();
    createBatchColorReset();
  }

  /** Adds an invisible actor that resets the batch color after all other shop actors are drawn. */
  private void createBatchColorReset() {
    batchColorReset =
        new Actor() {
          @Override
          public void draw(Batch batch, float parentAlpha) {
            batch.setColor(Color.WHITE);
          }
        };
    batchColorReset.setTouchable(Touchable.disabled);
    stage.addActor(batchColorReset);
  }

  /** Keeps the color-reset actor on top so it is always drawn after every other shop actor. */
  private void keepBatchColorResetLast() {
    if (batchColorReset != null) {
      batchColorReset.toFront();
    }
  }

  /** Creates the persistent floating icon used to open the shop. */
  private void createShopIcon() {
    shopIconButton = new TextButton("Shop", skin);
    shopIconButton.setSize(120f, 52f);

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
        screenWidth - iconWidth - ICON_MARGIN,
        screenHeight - shopIconButton.getHeight() - ICON_MARGIN);
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

  /**
   * Updates pricing for an individual upgrade catalog slot.
   *
   * <p>Skips the write entirely when the slot already holds this same name and price.
   * ShopComponent.setListing() fires {@code "shopChanged"} for any listing replaced with a
   * different object reference - and syncUpgradeCatalog() rebuilds a brand-new {@code ShopListing}
   * every time regardless of whether anything changed - so an unconditional write here would
   * re-trigger that event on every single sync, including ones a "shopChanged" listener itself
   * causes. See the warning on refreshContent() for why that matters.
   */
  private void syncUpgradeListing(ShopComponent shop, int catalogSlot, UpgradeNode node) {
    int cost = node.isMaxTier() ? 0 : node.getNextTierCost();
    ShopComponent.ShopListing<ShopComponent.Upgrade> existing = shop.getUpgradeListing(catalogSlot);
    if (existing != null
        && existing.getBuyPrice() == cost
        && existing.getProduct().getName().equals(node.getName())) {
      return;
    }
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
    shopTable.getColor().set(PANEL_TINT);
    shopTable.pad(PANEL_PADDING);
    shopTable.setSize(SHOP_WIDTH, SHOP_HEIGHT);

    whiteLabelStyle = new Label.LabelStyle(skin.get(LABEL_STYLE, Label.LabelStyle.class));
    whiteLabelStyle.fontColor = new Color(Color.WHITE);

    createHeader();
    addDivider();
    createTabs();

    contentTable = new Table();
    shopTable.add(contentTable).grow().top().left().minHeight(0f);
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
    headerTable.add(createGoldPill()).center().padRight(12f);
    headerTable.add(closeButton).size(40f, 32f).right();

    shopTable.add(headerTable).growX().height(40f).top();
    shopTable.row();
  }

  /** Builds the gold counter pill (coin icon + amount) shown in the header. */
  private Table createGoldPill() {
    Table goldPill = new Table();
    goldPill.setBackground(skin.getDrawable(FLAT_BACKGROUND));
    goldPill.getColor().set(GOLD_PILL_TINT);
    goldPill.pad(4f, 12f, 4f, 12f);

    // images/icons/coin.png
    Drawable coin = loadIcon(ICON_COIN);
    if (coin != null) {
      Image coinImage = new Image(coin);
      coinImage.setScaling(Scaling.fit);
      goldPill.add(coinImage).size(20f).padRight(6f);
    }

    goldLabel = new Label(getGoldText(), whiteLabelStyle);
    goldLabel.setColor(GOLD_COLOR);
    goldPill.add(goldLabel);

    return goldPill;
  }

  /** Adds a decorative golden divider line separating the header from the category tabs. */
  private void addDivider() {
    Image divider = new Image(skin.getDrawable(FLAT_BACKGROUND));
    divider.setColor(DIVIDER_TINT);

    shopTable.add(divider).growX().height(2f).padTop(4f).padBottom(8f);
    shopTable.row();
  }

  /** Creates the top-level category navigation tab bar. */
  private void createTabs() {
    Table tabTable = new Table();

    // Tab icons: images/icons/tab_items.png, tab_upgrades.png, tab_gambling.png (PETS has none).
    addTabButton(tabTable, "ITEMS", ShopTab.ITEMS, ICON_TAB_ITEMS);
    addTabButton(tabTable, "UPGRADES", ShopTab.UPGRADES, ICON_TAB_UPGRADES);
    addTabButton(tabTable, "PETS", ShopTab.PETS, null);
    addTabButton(tabTable, "GAMBLING", ShopTab.GAMBLING, ICON_TAB_GAMBLING);

    shopTable.add(tabTable).growX().height(48f).padBottom(8f);
    shopTable.row();

    updateTabHighlights();
  }

  /**
   * Adds an individual category tab button paired with an underline accent.
   *
   * @param iconPath internal path of the tab icon, or null for a text-only tab
   */
  private void addTabButton(Table tabTable, String text, ShopTab tab, String iconPath) {
    TextButton button = new TextButton(text, skin, "shop");
    applyTabIcon(button, iconPath);

    button.addListener(
        new ClickListener() {
          @Override
          public void clicked(InputEvent event, float x, float y) {
            onTabClicked(tab);
          }
        });

    Image underline = new Image(skin.getDrawable(FLAT_BACKGROUND));
    underline.setColor(TAB_UNDERLINE_INACTIVE);

    Table tabWrap = new Table();
    tabWrap.add(button).width(135f).height(36f);
    tabWrap.row();
    tabWrap.add(underline).growX().height(TAB_UNDERLINE_HEIGHT).padTop(3f);

    tabButtons.put(tab, button);
    tabUnderlines.put(tab, underline);

    tabTable.add(tabWrap).padRight(6f);
  }

  /** Puts an icon in front of a tab button's label when the PNG exists. */
  private void applyTabIcon(TextButton button, String iconPath) {
    Drawable tabIcon = iconPath == null ? null : loadIcon(iconPath);
    if (tabIcon == null) {
      return;
    }

    Label label = button.getLabel();
    button.clearChildren();
    Image tabImage = new Image(tabIcon);
    tabImage.setScaling(Scaling.fit);
    button.add(tabImage).size(22f).padRight(6f);
    button.add(label);
  }

  /** Switches to the clicked category tab and rebuilds the content. */
  private void onTabClicked(ShopTab tab) {
    currentTab = tab;
    if (tab == ShopTab.ITEMS) {
      sellMode = false;
    }
    if (tab == ShopTab.UPGRADES) {
      // Resync before the rebuild, not from inside refreshContent() - see its warning.
      syncUpgradeCatalog();
    }
    refreshContent();
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

    // IMPORTANT: refreshContent() must never trigger a catalog write. syncUpgradeCatalog() ->
    // syncUpgradeListing() -> ShopComponent.setUpgradeListing() can fire "shopChanged" (see its
    // no-op guard below), and ShopDisplay listens to "shopChanged" via refreshShop(), which calls
    // this method again - sync from here and a changed listing recurses forever. Callers that
    // need fresh Upgrade prices (switching to the tab, opening the shop, buying) resync
    // themselves BEFORE calling this.
    hideUpgradePopup();
    contentTable.clearChildren();
    disposeGamblingWheel();

    updateGold();
    updateTabHighlights();
    clearSelection();

    if (currentTab == ShopTab.ITEMS) {
      createItemSubTabs();
    }

    buildContentScroll();
    populateCurrentTab();
  }

  /** Disposes the gambling animation, if one is currently shown. */
  private void disposeGamblingWheel() {
    if (gamblingWheel != null) {
      gamblingWheel.dispose();
      gamblingWheel = null;
    }
  }

  /** Creates the scrollable grid that holds the current tab's cards. */
  private void buildContentScroll() {
    activeGrid = new Table();
    activeGrid.top();

    contentScroll = new ScrollPane(activeGrid, skin);
    contentScroll.setScrollingDisabled(true, false);
    contentScroll.setFadeScrollBars(false);
    contentScroll.setOverscroll(false, false);
    contentScroll.setFlickScroll(false);
    final ScrollPane pane = contentScroll;
    contentScroll.addListener(
        new InputListener() {
          @Override
          public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
            stage.setScrollFocus(pane);
          }
        });

    contentTable.add(contentScroll).grow().minHeight(0f).top().left();
  }

  /** Fills the grid for whichever category tab is active. */
  private void populateCurrentTab() {
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

    buySubButton = new TextButton("BUY", skin, "shop");
    sellSubButton = new TextButton("SELL", skin, "shop");

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

    for (int slot = 1; slot <= SELL_SLOT_COUNT; slot++) {
      Item item = inventory.getItem(slot);
      addSellCard(shop, item, slot);
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
      addCardIcon(card, name, rarity);
      addNameLabel(card, name);
      addPriceLabel(card, "Gold: " + sellPrice, GOLD_COLOR);

      card.addListener(
          new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
              selectCard(
                  card,
                  new CardSelection(
                      name, sellPrice, rarity, () -> sellItemAt(slot), "SELL", false));
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

    for (int slotNumber = 1; slotNumber <= ITEM_SLOT_COUNT; slotNumber++) {
      ShopComponent.ShopListing<T> listing = catalog.get(slotNumber);
      addCatalogCard(listing, slotNumber, nameExtractor, buyAction);
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
      populateCatalogCard(card, listing, catalogSlot, nameExtractor, buyAction);
    }

    addCardToContent(card);
  }

  /** Fills an occupied catalog card with icon, name, price and its click behaviour. */
  private <T> void populateCatalogCard(
      Table card,
      ShopComponent.ShopListing<T> listing,
      int catalogSlot,
      Function<T, String> nameExtractor,
      IntConsumer buyAction) {

    String name = nameExtractor.apply(listing.getProduct());
    int price = listing.getBuyPrice();
    Rarity rarity = getRarityForPrice(price);

    // An Upgrade card at max tier has no next-tier price (synced to 0) - show "MAX" instead of
    // the misleading "Gold: 0" and keep the card from offering a purchase.
    UpgradeNode cardUpgradeNode =
        currentTab == ShopTab.UPGRADES ? findUpgradeNodeByName(name) : null;
    boolean cardMaxTier = cardUpgradeNode != null && cardUpgradeNode.isMaxTier();

    addAccentStrip(card, rarity.color);
    addCardIcon(card, name, rarity);
    addNameLabel(card, name);
    addPriceLabel(card, cardMaxTier ? "MAX" : "Gold: " + price, priceColor(cardMaxTier, price));

    card.addListener(
        new ClickListener() {
          @Override
          public void clicked(InputEvent event, float x, float y) {
            onCatalogCardClicked(card, name, price, rarity, catalogSlot, buyAction);
          }
        });
  }

  /** Selects a clicked catalog card, using the Upgrade-specific flow on the Upgrades tab. */
  private void onCatalogCardClicked(
      Table card, String name, int price, Rarity rarity, int catalogSlot, IntConsumer buyAction) {
    if (currentTab == ShopTab.UPGRADES) {
      selectUpgradeCard(card, name, price, rarity, catalogSlot);
      return;
    }

    selectCard(
        card,
        new CardSelection(name, price, rarity, () -> buyAction.accept(catalogSlot), "BUY", true));
  }

  /** Selects an Upgrade card: shows its popup and wires the tiered purchase. */
  private void selectUpgradeCard(
      Table card, String name, int price, Rarity rarity, int catalogSlot) {
    UpgradeNode node = findUpgradeNodeByName(name);
    if (node != null) {
      showUpgradePopup(node);
    }
    boolean maxTier = node != null && node.isMaxTier();
    selectCard(
        card,
        new CardSelection(
            upgradeDetailName(name, node),
            price,
            rarity,
            () -> attemptUpgradePurchase(catalogSlot, node),
            "BUY",
            true,
            maxTier));
  }

  /** Adds the card's icon badge (pet sprite on the Pets tab, item/upgrade icon elsewhere). */
  private void addCardIcon(Table card, String name, Rarity rarity) {
    Stack icon =
        currentTab == ShopTab.PETS
            ? createPetIconStack(rarity, name)
            : createIconStack(name, rarity);
    card.add(icon).size(38f, 38f).padBottom(4f);
    card.row();
  }

  /** Adds the centred, ellipsised product name to a card. */
  private void addNameLabel(Table card, String name) {
    Label nameLabel = new Label(name, whiteLabelStyle);
    nameLabel.setColor(TEXT_PRIMARY);
    nameLabel.setAlignment(Align.center);
    nameLabel.setEllipsis(true);
    card.add(nameLabel).growX().center();
    card.row();
  }

  /** Adds the price line to a card in the given color. */
  private void addPriceLabel(Table card, String text, Color color) {
    Label priceLabel = new Label(text, whiteLabelStyle);
    priceLabel.setColor(color);
    card.add(priceLabel).padTop(2f).center();
  }

  /** Appends a colored top accent strip indicating rarity. */
  private void addAccentStrip(Table card, Color color) {
    Image strip = new Image(skin.getDrawable(FLAT_BACKGROUND));
    strip.setColor(color);
    card.add(strip).growX().height(ACCENT_STRIP_HEIGHT).padBottom(5f);
    card.row();
  }

  /**
   * Builds an icon badge for a product: the PNG from {@code images/icons/} (see {@link
   * #iconForName(String)}) when one matches the name, otherwise the product's initial letter.
   */
  private Stack createIconStack(String name, Rarity rarity) {
    Stack iconStack = new Stack();

    Image iconBackground = new Image(skin.getDrawable(FLAT_BACKGROUND));
    iconBackground.setColor(rarity.color);
    iconStack.add(iconBackground);

    Drawable icon = iconForName(name);
    if (icon != null) {
      Image iconImage = new Image(icon);
      iconImage.setScaling(Scaling.fit);
      iconStack.add(iconImage);
      return iconStack;
    }

    String initial = (name == null || name.isEmpty()) ? "?" : name.substring(0, 1).toUpperCase();

    Label iconLabel = new Label(initial, whiteLabelStyle);
    iconLabel.setColor(Color.WHITE);
    iconLabel.setAlignment(Align.center);
    iconStack.add(iconLabel);

    return iconStack;
  }

  /** Creates the pet icon from the pet texture atlas. */
  private Stack createPetIconStack(Rarity rarity, String petName) {
    Stack iconStack = new Stack();

    Image iconBackground = new Image(skin.getDrawable(FLAT_BACKGROUND));
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
  private Table createCard() {
    Table card = new Table();
    card.setBackground(skin.getDrawable(BUTTON_BACKGROUND));
    card.getColor().set(CARD_TINT);
    card.pad(8f);
    card.setTransform(true);
    card.setOrigin(CARD_WIDTH / 2f, CARD_HEIGHT / 2f);
    return card;
  }

  /** Populates a card representing an empty/locked slot. */
  private void addEmptyCardContent(Table card, int slot) {
    card.getColor().set(EMPTY_CARD_TINT);
    addAccentStrip(card, EMPTY_STRIP_TINT);

    Label slotLabel = new Label(EMPTY_TEXT, whiteLabelStyle);
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

    detailNameLabel = new Label("Select an item to view details", whiteLabelStyle);
    detailNameLabel.setColor(TEXT_MUTED);
    detailNameLabel.setEllipsis(true);

    detailPriceLabel = new Label("", whiteLabelStyle);
    detailPriceLabel.setColor(GOLD_COLOR);

    createDetailActionButton();

    Table textColumn = new Table();
    textColumn.add(detailNameLabel).growX().left().row();
    textColumn.add(detailPriceLabel).left().padTop(2f);

    detailPanel.add(createDetailIconStack()).size(40f, 40f).padRight(10f);
    detailPanel.add(textColumn).growX().left();
    detailPanel.add(detailActionButton).size(85f, 36f).right();
  }

  /** Builds the icon stack (background, letter fallback, PNG) of the detail panel. */
  private Stack createDetailIconStack() {
    detailIconBg = new Image(skin.getDrawable(FLAT_BACKGROUND));
    detailIconLabel = new Label("", whiteLabelStyle);
    detailIconLabel.setColor(Color.WHITE);
    detailIconLabel.setAlignment(Align.center);

    // Shows the PNG icon of the selected product (empty until something is selected).
    detailIconImage = new Image();
    detailIconImage.setScaling(Scaling.fit);

    Stack detailIconStack = new Stack();
    detailIconStack.add(detailIconBg);
    detailIconStack.add(detailIconLabel);
    detailIconStack.add(detailIconImage);
    return detailIconStack;
  }

  /** Builds the BUY/SELL button of the detail panel (disabled until a card is selected). */
  private void createDetailActionButton() {
    detailActionButton = new TextButton("BUY", skin, "shop");
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
  }

  /**
   * The per-card values needed to select it: what it's called, what it costs, how it's styled, and
   * what happens if the player confirms it. Bundled into a record so {@link #selectCard} doesn't
   * need 8 separate parameters (java:S107).
   */
  private record CardSelection(
      String name,
      int price,
      Rarity rarity,
      Runnable action,
      String actionLabel,
      boolean requiresAffordability,
      boolean maxTier) {

    /** Same as the full constructor, with {@code maxTier} defaulted to {@code false}. */
    CardSelection(
        String name,
        int price,
        Rarity rarity,
        Runnable action,
        String actionLabel,
        boolean requiresAffordability) {
      this(name, price, rarity, action, actionLabel, requiresAffordability, false);
    }
  }

  /**
   * Updates selection highlights, applies pulsing scale, and attaches a bobbing arrow indicator.
   */
  private void selectCard(Table card, CardSelection selection) {
    highlightSelectedCard(card);

    boolean canPerformAction =
        !selection.maxTier()
            && (!selection.requiresAffordability() || canAfford(selection.price()));
    pendingAction = canPerformAction ? selection.action() : null;

    updateDetailIcon(selection);
    updateDetailText(selection, canPerformAction);
    updateDetailButton(selection.actionLabel(), canPerformAction);
  }

  /** Resets the previous selection, then highlights, pulses and marks the new card. */
  private void highlightSelectedCard(Table card) {
    // Reset previously selected card
    if (selectedCard != null) {
      selectedCard.clearActions();
      selectedCard.setScale(1f);
      selectedCard.getColor().set(CARD_TINT);
    }

    selectedCard = card;
    selectedCard.getColor().set(CARD_TINT_SELECTED);

    // Pulse / Breathing animation on selected card
    selectedCard.clearActions();
    selectedCard.addAction(
        Actions.forever(
            Actions.sequence(
                Actions.scaleTo(1.035f, 1.035f, 0.35f), Actions.scaleTo(1.0f, 1.0f, 0.35f))));

    // Attach / Position bobbing indicator arrow
    attachSelectionArrow(card);
  }

  /** Shows the selected product's icon (PNG when available, otherwise its initial). */
  private void updateDetailIcon(CardSelection selection) {
    String name = selection.name();

    detailIconBg.setColor(selection.rarity().color);
    detailIconLabel.setText(
        (name == null || name.isEmpty()) ? "?" : name.substring(0, 1).toUpperCase());

    // PNG icon in the detail panel (pets keep the letter badge).
    Drawable detailIcon = currentTab == ShopTab.PETS ? null : iconForName(name);
    detailIconImage.setDrawable(detailIcon);
    detailIconLabel.setVisible(detailIcon == null);
  }

  /** Shows the selected product's name and price in the detail panel. */
  private void updateDetailText(CardSelection selection, boolean canPerformAction) {
    detailNameLabel.setText(selection.name());
    detailNameLabel.setColor(TEXT_PRIMARY);

    detailPriceLabel.setText(selection.maxTier() ? "MAX" : "Gold: " + selection.price());
    if (selection.maxTier()) {
      detailPriceLabel.setColor(TEXT_MUTED);
    } else if (selection.requiresAffordability() && !canPerformAction) {
      detailPriceLabel.setColor(INSUFFICIENT_FUNDS_COLOR);
    } else {
      detailPriceLabel.setColor(GOLD_COLOR);
    }
  }

  /** Configures the detail panel's BUY/SELL button for the current selection. */
  private void updateDetailButton(String actionLabel, boolean canPerformAction) {
    detailActionButton.setText(actionLabel);
    detailActionButton.setColor("BUY".equals(actionLabel) ? BUY_MODE_TINT : SELL_MODE_TINT);
    detailActionButton.setDisabled(!canPerformAction);
    detailActionButton.setTouchable(canPerformAction ? Touchable.enabled : Touchable.disabled);
  }

  /**
   * Resolves the price label's color: muted at max tier, gold if affordable, red otherwise.
   * Extracted from a nested ternary (java:S3358).
   */
  private Color priceColor(boolean maxTier, int price) {
    if (maxTier) {
      return TEXT_MUTED;
    }
    if (canAfford(price)) {
      return GOLD_COLOR;
    }
    return INSUFFICIENT_FUNDS_COLOR;
  }

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
      selectionArrow.remove();
    }

    pendingAction = null;
    if (detailIconBg == null) return;

    detailIconBg.setColor(Color.CLEAR);
    detailIconLabel.setText("");
    detailIconLabel.setVisible(true);
    detailIconImage.setDrawable(null);

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

    // No node to upgrade, or it's already at max tier (next-tier cost is undefined) - bail before
    // spending any gold or showing the purchase toast.
    if (node == null || node.isMaxTier()) {
      return;
    }

    // The catalog listing's price can be stale (e.g. the upgrade expired, or a save restored it,
    // since neither resyncs the listing on its own) - resync it right before charging so the
    // price taken is always the live one.
    syncUpgradeListing(shop, catalogSlot, node);

    boolean purchased = shop.buyUpgrade(catalogSlot);
    if (purchased) {
      node.purchaseNextTier();
      syncUpgradeListing(shop, catalogSlot, node);
      showPurchaseToast(node);
      refreshContent();
    }
  }

  private void showUpgradePopup(UpgradeNode node) {
    ensureUpgradePopupCreated();
    populateUpgradePopup(node);
    upgradePopup.pack();
    positionUpgradePopupAboveShop();
    upgradePopup.setVisible(true);
    upgradePopup.toFront();
    keepBatchColorResetLast();
  }

  private void ensureUpgradePopupCreated() {
    if (upgradePopup != null) return;

    upgradePopup = new Table();
    upgradePopup.setBackground(skin.getDrawable(ACCENT_PANEL_BACKGROUND));
    upgradePopup.pad(PANEL_PADDING);

    upgradePopupCloseButton = new TextButton("X", skin);
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

    layoutUpgradePopup();

    stage.addActor(upgradePopup);
    upgradePopup.setVisible(false);
  }

  /** Arranges the popup's close button and labels. */
  private void layoutUpgradePopup() {
    upgradePopup.add(upgradePopupCloseButton).size(22f, 22f).right().padBottom(6f);
    upgradePopup.row();
    upgradePopup.add(upgradePopupNameLabel).left().row();
    upgradePopup
        .add(upgradePopupDescriptionLabel)
        .width(UPGRADE_POPUP_WIDTH - 40f)
        .left()
        .padTop(8f)
        .row();
    upgradePopup.add(upgradePopupTierLabel).left().padTop(8f);
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
    float shopX = (screenWidth - SHOP_WIDTH) / 2f;
    float shopY = (screenHeight - SHOP_HEIGHT) / 2f;
    float popupWidth = upgradePopup.getWidth();
    float popupHeight = upgradePopup.getHeight();

    float x = (screenWidth - popupWidth) / 2f;
    float y = shopY + SHOP_HEIGHT + UPGRADE_POPUP_GAP_ABOVE_SHOP;

    if (y + popupHeight > screenHeight) {
      y = shopY + SHOP_HEIGHT - popupHeight;
      x = shopX + SHOP_WIDTH + UPGRADE_POPUP_GAP_ABOVE_SHOP;
      if (x + popupWidth > screenWidth) {
        x = shopX - UPGRADE_POPUP_GAP_ABOVE_SHOP - popupWidth;
      }
    }

    x = MathUtils.clamp(x, 0f, Math.max(0f, screenWidth - popupWidth));
    y = MathUtils.clamp(y, 0f, Math.max(0f, screenHeight - popupHeight));
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
    purchaseToast.toFront();
    keepBatchColorResetLast();
    purchaseToast.addAction(
        Actions.sequence(Actions.delay(PURCHASE_TOAST_VISIBLE_SECONDS), Actions.visible(false)));
  }

  private void ensurePurchaseToastCreated() {
    if (purchaseToast != null) return;

    purchaseToast = new Table();
    purchaseToast.setBackground(skin.getDrawable(TOAST_BACKGROUND));
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

    // refreshContent() never syncs on its own (see its warning) - resync here so a reopened shop
    // never shows an Upgrades card that went stale while it was closed.
    syncUpgradeCatalog();
    refreshContent();
    positionShop();

    shopTable.setVisible(true);
    shopTable.toFront();
    keepBatchColorResetLast();
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
  public void draw(SpriteBatch batch) {
    // Shop UI is rendered by Scene2D Stage.
    batch.setColor(Color.WHITE);
  }

  @Override
  public void dispose() {
    removeShopActors();
    disposeGamblingWheel();

    // Textures loaded from images/icons/ are tracked separately.
    disposeIcons();

    contentTable = null;
    activeGrid = null;
    contentScroll = null;
    detailPanel = null;
    detailIconImage = null;
    goldLabel = null;
    selectedCard = null;
    pendingAction = null;

    super.dispose();
  }

  /** Removes every actor this component added to the stage and releases the pet atlas. */
  private void removeShopActors() {
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
    if (batchColorReset != null) {
      batchColorReset.remove();
      batchColorReset = null;
    }
  }

  // ===========================================================================================
  // Gambling tab
  // ===========================================================================================

  /** Builds the gambling wheel tab with mode selections. */
  private void createGamblingTab() {
    ShopComponent shop = entity.getComponent(ShopComponent.class);
    if (shop == null) {
      return;
    }

    if (shop.getGamblingCatalogs() == null) {
      addGamblingUnavailable();
      return;
    }

    // Root container for the gambling tab; every element is centred relative to its middle.
    Table gamblingRoot = new Table();
    gamblingRoot.setFillParent(false);
    gamblingRoot.defaults().pad(2f);
    gamblingRoot.center();

    gamblingRoot.add(createGamblingModeRow()).center().padBottom(4f).row();

    // GamblingWheel is still the existing class/API; it shows the square lottery animation.
    gamblingWheel = new GamblingWheel(whiteLabelStyle);
    gamblingRoot.add(gamblingWheel).size(380f, 248f).center().padTop(2f).padBottom(4f).row();

    gamblingRoot.add(createGamblingSpinRow()).center().padTop(2f).padBottom(4f).row();
    gamblingRoot.add(createGamblingResultLabel()).center().padTop(2f).row();

    // grow() lets the root fill the tab area while center() keeps its contents centred.
    activeGrid.add(gamblingRoot).grow().center();

    // Load the currently selected catalogue.
    refreshGamblingWheel();
  }

  /** Shows a placeholder message when no gambling catalogs are installed. */
  private void addGamblingUnavailable() {
    Label unavailable = new Label("Gambling is not available.", whiteLabelStyle);
    unavailable.setColor(TEXT_MUTED);
    unavailable.setAlignment(Align.center);
    activeGrid.add(unavailable).grow().center();
  }

  /** Builds the STANDARD / PREMIUM catalogue switch buttons. */
  private Table createGamblingModeRow() {
    Table modeRow = new Table();
    modeRow.defaults().pad(0f);

    standardGamblingButton = new TextButton("STANDARD", skin, "shop");
    premiumGamblingButton = new TextButton("PREMIUM", skin, "shop");

    standardGamblingButton.addListener(
        createCatalogSwitchListener(GamblingCatalogs.CatalogId.STANDARD));
    premiumGamblingButton.addListener(
        createCatalogSwitchListener(GamblingCatalogs.CatalogId.PREMIUM));

    modeRow.add(standardGamblingButton).width(120f).height(32f).padRight(8f);
    modeRow.add(premiumGamblingButton).width(120f).height(32f);
    return modeRow;
  }

  /** Creates a click listener that switches to the given catalogue (ignored while spinning). */
  private ClickListener createCatalogSwitchListener(GamblingCatalogs.CatalogId catalogId) {
    return new ClickListener() {
      @Override
      public void clicked(InputEvent event, float x, float y) {
        if (gamblingWheel != null && gamblingWheel.isSpinning()) {
          return;
        }

        currentGamblingCatalog = catalogId;
        refreshGamblingWheel();
      }
    };
  }

  /** Builds the row holding the spin price and the SPIN button. */
  private Table createGamblingSpinRow() {
    gamblingPriceLabel = new Label("", whiteLabelStyle);
    gamblingPriceLabel.setColor(GOLD_COLOR);
    gamblingPriceLabel.setAlignment(Align.center);

    spinButton = new TextButton("SPIN", skin, "shop");
    spinButton.setColor(SPIN_BUTTON_TINT);
    spinButton.addListener(
        new ClickListener() {
          @Override
          public void clicked(InputEvent event, float x, float y) {
            spinGamblingWheel();
          }
        });

    Table spinRow = new Table();
    spinRow.add(gamblingPriceLabel).center().padRight(16f);
    spinRow.add(spinButton).width(120f).height(32f).center();
    return spinRow;
  }

  /** Builds the label that reports the spin result. */
  private Label createGamblingResultLabel() {
    gamblingResultLabel = new Label("Choose a catalogue and spin!", whiteLabelStyle);
    gamblingResultLabel.setColor(TEXT_PRIMARY);
    gamblingResultLabel.setAlignment(Align.center);
    return gamblingResultLabel;
  }

  /** Returns the currently selected spin catalogue, or null when unavailable. */
  private GamblingCatalogs.SpinCatalog currentSpinCatalog(ShopComponent shop) {
    GamblingCatalogs catalogs = shop.getGamblingCatalogs();
    if (catalogs == null) {
      return null;
    }
    return catalogs.get(currentGamblingCatalog);
  }

  private void refreshGamblingWheel() {
    ShopComponent shop = entity.getComponent(ShopComponent.class);
    if (shop == null || gamblingWheel == null) return;

    GamblingCatalogs.SpinCatalog catalog = currentSpinCatalog(shop);
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

    GamblingCatalogs.SpinCatalog catalog = currentSpinCatalog(shop);
    if (catalog == null) return;

    if (!canAfford(catalog.getSpinPrice())) {
      showGamblingMessage("Not enough gold.", INSUFFICIENT_FUNDS_COLOR);
      return;
    }

    GamblingCatalogs.PrizeEntry<GamblingCatalogs.Prize> result =
        shop.buySpin(currentGamblingCatalog);
    if (result == null) {
      showGamblingMessage("Spin failed.", INSUFFICIENT_FUNDS_COLOR);
      refreshGamblingWheel();
      return;
    }

    applyUpgradePrize(result);

    int winningSlot = findPrizeSlot(catalog, result);
    if (winningSlot < 1) {
      showGamblingMessage("Prize received.", GOLD_COLOR);
      refreshGamblingWheel();
      return;
    }

    animateSpin(shop, winningSlot, result);
  }

  /** Sets the gambling result label's text and color. */
  private void showGamblingMessage(String text, Color color) {
    gamblingResultLabel.setText(text);
    gamblingResultLabel.setColor(color);
  }

  /** Activates the next tier of an Upgrade that was won from the wheel. */
  private void applyUpgradePrize(GamblingCatalogs.PrizeEntry<GamblingCatalogs.Prize> result) {
    if (!(result.product() instanceof ShopComponent.Upgrade upgrade)) {
      return;
    }

    UpgradeNode node = findUpgradeNodeByName(upgrade.getName());
    if (node != null && !node.isMaxTier()) {
      node.purchaseNextTier();
      showPurchaseToast(node);
    }
  }

  /** Locks the SPIN button, runs the wheel animation, then reveals the prize. */
  private void animateSpin(
      ShopComponent shop,
      int winningSlot,
      GamblingCatalogs.PrizeEntry<GamblingCatalogs.Prize> result) {
    spinButton.setDisabled(true);
    spinButton.setTouchable(Touchable.disabled);

    gamblingWheel.spinToSlot(
        winningSlot,
        () -> {
          showGamblingMessage("You won: " + getPrizeName(result), GOLD_COLOR);
          refreshGamblingWheel();

          if (shop.getPendingPetReplacement() != null) {
            showPetReplacementDialog(shop);
          }
        });
  }

  private int findPrizeSlot(
      GamblingCatalogs.SpinCatalog catalog,
      GamblingCatalogs.PrizeEntry<GamblingCatalogs.Prize> result) {
    for (int slot = 1; slot <= GamblingCatalogs.SpinCatalog.PRIZE_SLOT_COUNT; slot++) {
      GamblingCatalogs.PrizeEntry<GamblingCatalogs.Prize> prize = catalog.getPrize(slot);
      if (prize == result) {
        return slot;
      }
    }
    return -1;
  }

  private String getPrizeName(GamblingCatalogs.PrizeEntry<GamblingCatalogs.Prize> prize) {
    if (prize == null) return "?";

    Object product = prize.product();
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
      selectionArrow.setFontScale(1.2f);
      selectionArrow.setTouchable(Touchable.disabled);
    }

    selectionArrow.remove();
    selectionArrow.clearActions();
    selectionArrow.pack(); // Ensure size is calculated before positioning
    selectionArrow.setVisible(true);

    targetCard.addActor(selectionArrow);

    float x = targetCard.getWidth() - selectionArrow.getWidth() - 6f;
    float y = targetCard.getHeight() - selectionArrow.getHeight() - 10f;
    selectionArrow.setPosition(x, y);

    // Continuous bobbing animation (moves up & down by 5px)
    selectionArrow.addAction(
        Actions.forever(
            Actions.sequence(Actions.moveBy(0f, 5f, 0.25f), Actions.moveBy(0f, -5f, 0.25f))));
  }

  private void showPetReplacementDialog(ShopComponent shop) {
    ShopComponent.Pet pendingPet = shop.getPendingPetReplacement();
    if (pendingPet == null) {
      return;
    }

    InventoryComponent inventory = entity.getComponent(InventoryComponent.class);

    if (inventory == null) {
      shop.cancelPendingPetReplacement();
      return;
    }

    Dialog dialog = new Dialog("Pet Inventory Full", skin, "default");

    dialog.text(
        "You won "
            + pendingPet.getName()
            + ", but both pet slots are full.\n"
            + "Choose a pet to replace, or cancel.");

    addPetReplacementButtons(dialog, shop, inventory);

    dialog.show(stage);
  }

  /** Adds "Replace 6", "Replace 7" and "Cancel" buttons to the pet replacement dialog. */
  private void addPetReplacementButtons(
      Dialog dialog, ShopComponent shop, InventoryComponent inventory) {
    ShopComponent.Pet slot1 = inventory.getPet(1);
    ShopComponent.Pet slot2 = inventory.getPet(2);

    dialog
        .getButtonTable()
        .add(
            createDialogButton(
                dialog,
                "Replace 6. " + (slot1 == null ? EMPTY_TEXT : slot1.getName()),
                () -> shop.replacePetWithPendingPrize(1)));
    dialog
        .getButtonTable()
        .add(
            createDialogButton(
                dialog,
                "Replace 7. " + (slot2 == null ? EMPTY_TEXT : slot2.getName()),
                () -> shop.replacePetWithPendingPrize(2)));
    dialog
        .getButtonTable()
        .add(createDialogButton(dialog, "Cancel", shop::cancelPendingPetReplacement));
  }

  /** Creates a dialog button that runs an action, closes the dialog and refreshes the wheel. */
  private TextButton createDialogButton(Dialog dialog, String label, Runnable action) {
    TextButton button = new TextButton(label, skin);
    button.addListener(
        new ChangeListener() {
          @Override
          public void changed(ChangeEvent event, Actor actor) {
            action.run();
            dialog.hide();
            refreshGamblingWheel();
          }
        });
    return button;
  }

  // ===========================================================================================
  // PNG icons (source/core/assets/images/icons/)
  // ===========================================================================================

  /**
   * Returns the icon for an item/upgrade name using {@link #ICON_RULES}, e.g. a name containing
   * "bow" gives {@code images/icons/bow.png}.
   *
   * @param name item or upgrade name
   * @return the icon, or null if no rule matches or the file is missing
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
   * Loads (once) the PNG at an internal path such as {@link #ICON_BOW}.
   *
   * @param path path relative to the assets folder
   * @return the icon, or null if the file doesn't exist
   */
  private Drawable loadIcon(String path) {
    if (iconCache.containsKey(path)) {
      return iconCache.get(path);
    }

    Drawable drawable = null;
    if (Gdx.files.internal(path).exists()) {
      // Mipmaps keep the large PNGs smooth when they are drawn at 38-80 px.
      Texture texture = new Texture(Gdx.files.internal(path), true);
      texture.setFilter(Texture.TextureFilter.MipMapLinearLinear, Texture.TextureFilter.Linear);
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
}
