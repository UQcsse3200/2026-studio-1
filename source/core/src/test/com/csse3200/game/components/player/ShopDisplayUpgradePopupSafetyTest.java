package com.csse3200.game.components.player;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Regression test for a rendering bug: an earlier version of the Upgrades-tab informational popup
 * left the shared SpriteBatch's color tinted after drawing (a non-white Table background +
 * toFront() + text-only children), darkening every world sprite drawn on the next frame until the
 * popup stopped being last-drawn.
 *
 * <p>A headless test can't drive an actual render pass, so this directly asserts the specific
 * mechanism that caused the bug: the popup's background Table and close button must never carry any
 * color other than default white, across repeated open/close/purchase cycles - exactly the scenario
 * that originally triggered it. (toFront() is verified by inspection instead - see
 * ShopDisplay.showUpgradePopup(), which contains no such call.)
 */
@ExtendWith(GameExtension.class)
class ShopDisplayUpgradePopupSafetyTest {
  private ShopDisplay shopDisplay;
  private ShopComponent shopComponent;

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
    new Entity()
        .addComponent(shopComponent)
        .addComponent(new InventoryComponent(1000))
        .addComponent(shopDisplay)
        .create();

    UpgradesDisplay upgradesDisplay = new UpgradesDisplay();
    new Entity().addComponent(new UpgradesMenuComponent()).addComponent(upgradesDisplay).create();

    shopDisplay.setUpgradesDisplay(upgradesDisplay);
  }

  /**
   * Regression test for the actual reported bug: the Upgrades tab showed only
   * ShopComponent.seedDefaultCatalog()'s hardcoded "Health Upgrade" placeholder in slot 1, with
   * slots 2+ empty, because ShopDisplay.setUpgradesDisplay()/syncUpgradeCatalog() didn't exist on
   * this branch at all (confirmed via git history - never a lifecycle timing race). Now that
   * setUpgradesDisplay() is called (see beforeEach()), the catalog must be fully replaced with the
   * 5 real UpgradeNodes, in UpgradesDisplay.getAllUpgrades() order starting at slot 1.
   */
  @Test
  void settingUpgradesDisplayReplacesThePlaceholderCatalogWithRealUpgrades() {
    assertEquals(5, shopComponent.getUpgradeCatalog().size());
    assertEquals("Sword Damage", shopComponent.getUpgradeListing(1).getProduct().getName());
    assertEquals("Attack Speed", shopComponent.getUpgradeListing(2).getProduct().getName());
    assertEquals("Shield Durability", shopComponent.getUpgradeListing(3).getProduct().getName());
    assertEquals("Regen on Kill", shopComponent.getUpgradeListing(4).getProduct().getName());
    assertEquals("Player Speed+", shopComponent.getUpgradeListing(5).getProduct().getName());
  }

  private UpgradeNode firstUpgrade() throws Exception {
    Field field = ShopDisplay.class.getDeclaredField("upgradesDisplay");
    field.setAccessible(true);
    UpgradesDisplay upgradesDisplay = (UpgradesDisplay) field.get(shopDisplay);
    return upgradesDisplay.getAllUpgrades().get(0); // "sword_damage", synced to catalog slot 1
  }

  private void showPopup(UpgradeNode node) throws Exception {
    Method method = ShopDisplay.class.getDeclaredMethod("showUpgradePopup", UpgradeNode.class);
    method.setAccessible(true);
    method.invoke(shopDisplay, node);
  }

  private void hidePopup() throws Exception {
    Method method = ShopDisplay.class.getDeclaredMethod("hideUpgradePopup");
    method.setAccessible(true);
    method.invoke(shopDisplay);
  }

  private void buyUpgrade(UpgradeNode node) throws Exception {
    Method method =
        ShopDisplay.class.getDeclaredMethod("attemptUpgradePurchase", int.class, UpgradeNode.class);
    method.setAccessible(true);
    method.invoke(shopDisplay, 1, node);
  }

  private Table popupTable() throws Exception {
    Field field = ShopDisplay.class.getDeclaredField("upgradePopup");
    field.setAccessible(true);
    return (Table) field.get(shopDisplay);
  }

  private TextButton popupCloseButton() throws Exception {
    Field field = ShopDisplay.class.getDeclaredField("upgradePopupCloseButton");
    field.setAccessible(true);
    return (TextButton) field.get(shopDisplay);
  }

  private Table purchaseToast() throws Exception {
    Field field = ShopDisplay.class.getDeclaredField("purchaseToast");
    field.setAccessible(true);
    return (Table) field.get(shopDisplay);
  }

  @Test
  void popupTableAndCloseButtonStayDefaultWhiteAcrossRepeatedOpenCloseAndPurchaseCycles()
      throws Exception {
    UpgradeNode node = firstUpgrade();

    for (int i = 0; i < 5; i++) {
      showPopup(node);
      assertEquals(Color.WHITE, popupTable().getColor());
      assertEquals(Color.WHITE, popupCloseButton().getColor());

      hidePopup();
      assertEquals(Color.WHITE, popupTable().getColor());
      assertEquals(Color.WHITE, popupCloseButton().getColor());

      showPopup(node);
      buyUpgrade(node); // buyUpgrade -> refreshContent() -> hideUpgradePopup()
      assertEquals(Color.WHITE, popupTable().getColor());
      assertEquals(Color.WHITE, popupCloseButton().getColor());
    }
  }

  /**
   * Same kind of direct proof as the popup test above, applied to the purchase-confirmation toast:
   * its background Table must never carry any color other than default white, across several
   * purchases in a row - the toast is (re)shown on every successful purchase, so this exercises
   * ensurePurchaseToastCreated()/showPurchaseToast() repeatedly rather than just once.
   */
  @Test
  void purchaseToastStaysDefaultWhiteAcrossRepeatedPurchases() throws Exception {
    UpgradeNode node = firstUpgrade();

    for (int i = 0; i < 5; i++) {
      buyUpgrade(node); // -> attemptUpgradePurchase() -> showPurchaseToast() on success
      assertEquals(Color.WHITE, purchaseToast().getColor());
    }
  }
}
