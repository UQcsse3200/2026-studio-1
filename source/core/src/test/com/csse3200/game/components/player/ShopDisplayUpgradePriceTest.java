package com.csse3200.game.components.player;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import com.csse3200.game.upgrades.UpgradeNode;
import com.csse3200.game.upgrades.UpgradesDisplay;
import com.csse3200.game.upgrades.UpgradesMenuComponent;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Regression test for stale shop upgrade prices: {@code ShopDisplay} only resynced its Upgrades
 * catalog listing from the live {@code UpgradeNode} on a purchase and on {@code
 * setUpgradesDisplay()}. Nothing resynced it when an upgrade expired back to Tier 0 or after {@code
 * LoadService} restored a saved upgrade, so the catalog kept charging whatever price was last
 * synced - 0 once an upgrade had been maxed out, or an earlier tier's price after a load.
 *
 * <p>Uses "Sword Damage" (the first upgrade returned by {@code UpgradesDisplay.getAllUpgrades()},
 * synced to catalog slot 1 - see {@code ShopDisplayUpgradePopupSafetyTest.firstUpgrade()}), a
 * KILL_COUNT upgrade with tier costs {@code {20, 18, 15}} and kill thresholds {@code {2, 5, 8}}.
 *
 * <p>Also covers a second, since-fixed bug in the same code path: {@code refreshContent()} used to
 * call {@code syncUpgradeCatalog()} itself whenever the Upgrades tab was showing.
 * syncUpgradeCatalog() always wrote a brand-new {@code ShopListing}, and {@code
 * ShopComponent.setListing()} fires {@code "shopChanged"} for any such write - which {@code
 * ShopDisplay} listens for via {@code refreshShop()}, which calls {@code refreshContent()} again -
 * so opening the Upgrades tab recursed forever and overflowed the stack. {@code shopComponent} and
 * {@code shopDisplay} below are real components on a real {@code Entity} (not mocks), so that
 * "shopChanged" listener is genuinely registered and this recursion would actually fire.
 */
@ExtendWith(GameExtension.class)
class ShopDisplayUpgradePriceTest {
  private Entity playerEntity;
  private ShopDisplay shopDisplay;
  private ShopComponent shopComponent;
  private InventoryComponent inventory;

  @BeforeEach
  void beforeEach() {
    Viewport viewport = mock(Viewport.class);
    when(viewport.getWorldWidth()).thenReturn(1280f);
    when(viewport.getWorldHeight()).thenReturn(720f);

    Stage stage = mock(Stage.class);
    when(stage.getViewport()).thenReturn(viewport);

    RenderService renderService = new RenderService();
    renderService.setStage(stage);
    ServiceLocator.registerRenderService(renderService);
    ServiceLocator.registerResourceService(mock(ResourceService.class));

    shopDisplay = new ShopDisplay();
    shopComponent = new ShopComponent().seedDefaultCatalog();
    inventory = new InventoryComponent(1000);
    playerEntity =
        new Entity().addComponent(shopComponent).addComponent(inventory).addComponent(shopDisplay);
    playerEntity.create();

    UpgradesDisplay upgradesDisplay = new UpgradesDisplay();
    new Entity().addComponent(new UpgradesMenuComponent()).addComponent(upgradesDisplay).create();

    shopDisplay.setUpgradesDisplay(upgradesDisplay);
  }

  private UpgradeNode firstUpgrade() throws Exception {
    Field field = ShopDisplay.class.getDeclaredField("upgradesDisplay");
    field.setAccessible(true);
    UpgradesDisplay upgradesDisplay = (UpgradesDisplay) field.get(shopDisplay);
    return upgradesDisplay.getAllUpgrades().get(0); // "sword_damage", synced to catalog slot 1
  }

  /** Reopens the shop, exercising the same {@code syncUpgradeCatalog()} call a player triggers. */
  private void openShop() throws Exception {
    Method method = ShopDisplay.class.getDeclaredMethod("openShop");
    method.setAccessible(true);
    method.invoke(shopDisplay);
  }

  private void buyUpgrade(UpgradeNode node) throws Exception {
    Method method =
        ShopDisplay.class.getDeclaredMethod("attemptUpgradePurchase", int.class, UpgradeNode.class);
    method.setAccessible(true);
    method.invoke(shopDisplay, 1, node);
  }

  private int catalogSlotOnePrice() {
    return shopComponent.getUpgradeListing(1).getBuyPrice();
  }

  private Color priceColor(boolean maxTier, int price) throws Exception {
    Method method = ShopDisplay.class.getDeclaredMethod("priceColor", boolean.class, int.class);
    method.setAccessible(true);
    return (Color) method.invoke(shopDisplay, maxTier, price);
  }

  private Color colorConstant(String fieldName) throws Exception {
    Field field = ShopDisplay.class.getDeclaredField(fieldName);
    field.setAccessible(true);
    return (Color) field.get(null);
  }

  private void syncUpgradeCatalog() throws Exception {
    Method method = ShopDisplay.class.getDeclaredMethod("syncUpgradeCatalog");
    method.setAccessible(true);
    method.invoke(shopDisplay);
  }

  private void refreshContent() throws Exception {
    Method method = ShopDisplay.class.getDeclaredMethod("refreshContent");
    method.setAccessible(true);
    method.invoke(shopDisplay);
  }

  /**
   * Reproduces exactly what a category tab's {@code ClickListener} does (see {@code
   * addTabButton()}): sets {@code currentTab}, resets {@code sellMode} for ITEMS, syncs the Upgrade
   * catalog for UPGRADES, then rebuilds. Done via reflection rather than firing a real Scene2D
   * touch event, consistent with the rest of this file's private-method access.
   */
  @SuppressWarnings({"unchecked", "rawtypes"})
  private void clickTab(String tabName) throws Exception {
    Field tabField = ShopDisplay.class.getDeclaredField("currentTab");
    tabField.setAccessible(true);
    Enum<?> tab = Enum.valueOf((Class<Enum>) tabField.getType(), tabName);
    tabField.set(shopDisplay, tab);

    if ("ITEMS".equals(tabName)) {
      Field sellModeField = ShopDisplay.class.getDeclaredField("sellMode");
      sellModeField.setAccessible(true);
      sellModeField.setBoolean(shopDisplay, false);
    }
    if ("UPGRADES".equals(tabName)) {
      syncUpgradeCatalog();
    }
    refreshContent();
  }

  @Test
  void priceResyncsToTierOneCostAfterAnUpgradeExpiresFromMaxTier() throws Exception {
    UpgradeNode node = firstUpgrade();

    // Drive it straight to max tier (3) via the node's own API, bypassing the shop entirely -
    // mirrors what repeated purchases would leave behind.
    node.purchaseNextTier();
    node.purchaseNextTier();
    node.purchaseNextTier();
    assertTrue(node.isMaxTier());

    openShop();
    assertEquals(0, catalogSlotOnePrice());

    // Run out its last tier's kill threshold (8) so it fully expires back to Tier 0.
    for (int i = 0; i < 8; i++) {
      node.onEnemyKilled();
    }
    assertEquals(0, node.getCurrentTier());

    openShop();
    assertEquals(20, catalogSlotOnePrice()); // Tier 1 cost, not the stale maxed-out 0
  }

  @Test
  void priceResyncsAfterUpgradeNodeRestoreSimulatingALoad() throws Exception {
    UpgradeNode node = firstUpgrade();
    assertEquals(20, catalogSlotOnePrice()); // Tier 1 cost, synced by setUpgradesDisplay()

    // Simulate LoadService restoring a save where this upgrade was already at Tier 2 - nothing
    // tells ShopDisplay about it, so the listing stays at the old price until something resyncs.
    assertTrue(node.restore(2, 0f, 5));
    assertEquals(20, catalogSlotOnePrice()); // still stale, not yet resynced

    openShop();
    assertEquals(node.getNextTierCost(), catalogSlotOnePrice()); // Tier 3 cost (15)
  }

  @Test
  void buyingAtMaxTierSpendsNoGoldAndDoesNotAdvanceTheTier() throws Exception {
    UpgradeNode node = firstUpgrade();

    buyUpgrade(node); // Tier 1, cost 20
    buyUpgrade(node); // Tier 2, cost 18
    buyUpgrade(node); // Tier 3, cost 15
    assertTrue(node.isMaxTier());

    int goldAtMaxTier = inventory.getGold();
    int tierAtMaxTier = node.getCurrentTier();

    buyUpgrade(node); // Should be a no-op: no listing, no gold spent, no tier change

    assertEquals(goldAtMaxTier, inventory.getGold());
    assertEquals(tierAtMaxTier, node.getCurrentTier());
  }

  @Test
  void normalPurchaseDeductsTheCorrectCostAndAdvancesTheTier() throws Exception {
    UpgradeNode node = firstUpgrade();
    int goldBefore = inventory.getGold();

    buyUpgrade(node); // Tier 0 -> Tier 1, cost 20

    assertEquals(1, node.getCurrentTier());
    assertEquals(goldBefore - 20, inventory.getGold());
  }

  /**
   * Regression test for the StackOverflowError: switching to the Upgrades tab syncs the catalog and
   * rebuilds the content without recursing. Advancing the node first means the sync below actually
   * changes slot 1's price (20 -&gt; 18), so it is guaranteed to write and fire {@code
   * "shopChanged"} once - if {@code refreshContent()} still called {@code syncUpgradeCatalog()}
   * itself (the line this fix removed), that write would have looped back into another
   * refreshContent() call and kept firing "shopChanged" without end.
   */
  @Test
  void switchingToUpgradesTabCompletesWithoutStackOverflowAndSyncsCatalogAtMostOnce()
      throws Exception {
    UpgradeNode node = firstUpgrade();
    node.purchaseNextTier(); // Tier 1 - slot 1's synced price is now stale (still 20, should be 18)

    AtomicInteger shopChangedCount = new AtomicInteger();
    playerEntity.getEvents().addListener("shopChanged", shopChangedCount::incrementAndGet);

    assertDoesNotThrow(() -> clickTab("UPGRADES"));

    assertEquals(18, catalogSlotOnePrice());
    assertEquals(1, shopChangedCount.get());
  }

  /**
   * Exercises the real event wiring end-to-end: opening the shop, switching tabs back and forth
   * several times, and buying upgrades, all on the real {@code ShopComponent}/{@code ShopDisplay}
   * pair from {@code beforeEach()} (not mocks) - so every "shopChanged" trigger actually reaches
   * {@code ShopDisplay.refreshShop()}. None of this should throw, hang, or miscount gold/tier.
   */
  @Test
  void openingShopSwitchingTabsRepeatedlyAndBuyingUpgradesWorkWithRealEventWiring()
      throws Exception {
    UpgradeNode node = firstUpgrade();

    for (int i = 0; i < 5; i++) {
      openShop();
      assertDoesNotThrow(() -> clickTab("UPGRADES"));
      assertDoesNotThrow(() -> clickTab("ITEMS"));
      assertDoesNotThrow(() -> clickTab("UPGRADES"));
      buyUpgrade(node); // no-ops once max tier is reached (iterations 4 and 5)
    }

    assertTrue(node.isMaxTier());
    assertEquals(3, node.getCurrentTier());
    assertEquals(1000 - 20 - 18 - 15, inventory.getGold()); // only the 3 real tiers were charged
  }

  /**
   * Direct unit test of the no-op guard added to {@code syncUpgradeListing()}: re-syncing a slot
   * that already holds the same upgrade name and price must not write, and therefore must not fire
   * {@code "shopChanged"} - that unconditional write (regardless of whether anything changed) was
   * what made the recursion in the test above possible in the first place.
   */
  /**
   * Covers the three branches extracted from the nested ternary this fixes (java:S3358): max-tier
   * wins regardless of gold, then affordable vs. not - against the real InventoryComponent(1000)
   * from beforeEach().
   */
  @Test
  void priceColorReturnsTheCorrectColorForMaxTierAffordableAndUnaffordable() throws Exception {
    // inventory holds 1000 gold (see beforeEach())
    assertEquals(colorConstant("TEXT_MUTED"), priceColor(true, 1_000_000));
    assertEquals(colorConstant("GOLD_COLOR"), priceColor(false, 500));
    assertEquals(colorConstant("INSUFFICIENT_FUNDS_COLOR"), priceColor(false, 1_000_000));
  }

  @Test
  void syncUpgradeListingDoesNotFireShopChangedWhenNameAndPriceAreUnchanged() throws Exception {
    UpgradeNode node = firstUpgrade(); // already synced to slot 1 by setUpgradesDisplay()

    AtomicInteger shopChangedCount = new AtomicInteger();
    playerEntity.getEvents().addListener("shopChanged", shopChangedCount::incrementAndGet);

    Method method =
        ShopDisplay.class.getDeclaredMethod(
            "syncUpgradeListing", ShopComponent.class, int.class, UpgradeNode.class);
    method.setAccessible(true);
    method.invoke(shopDisplay, shopComponent, 1, node); // same name/price already stored - no-op

    assertEquals(0, shopChangedCount.get());
  }
}
