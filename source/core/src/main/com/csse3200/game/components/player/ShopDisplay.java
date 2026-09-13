package com.csse3200.game.components.player;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
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

  private static final int ITEM_COLUMNS = 3;
  private static final int ITEM_SLOT_COUNT = 10;
  private static final int SELL_SLOT_COUNT = 5;

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

  @Override
  public void create() {
    super.create();

    entity.getEvents().addListener("inventoryChanged", this::refreshShop);
    entity.getEvents().addListener("shopChanged", this::refreshShop);
    entity.getEvents().addListener("upgradePurchased", this::refreshShop);
    entity.getEvents().addListener("petPurchased", this::refreshShop);

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

              selectCard(
                  card, name, price, rarity, () -> buyAction.accept(catalogSlot), "BUY", true);
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

    contentTable = null;
    activeGrid = null;
    detailPanel = null;
    goldLabel = null;
    selectedCard = null;
    pendingAction = null;

    super.dispose();
  }
}
