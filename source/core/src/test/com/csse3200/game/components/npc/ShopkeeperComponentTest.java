package com.csse3200.game.components.npc;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.csse3200.game.components.loot.ConsumableGenerator;
import com.csse3200.game.components.loot.WeaponGenerator;
import com.csse3200.game.components.player.InventoryComponent;
import com.csse3200.game.components.player.ShopComponent;
import com.csse3200.game.components.player.ShopDisplay;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.factories.PlayerFactory;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class ShopkeeperComponentTest {
  private Input previousInput;
  private Input input;
  private Entity player;
  private ShopDisplay shopDisplay;
  private Entity shopkeeper;

  @BeforeEach
  void setUp() {
    previousInput = Gdx.input;
    input = mock(Input.class);
    Gdx.input = input;

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
    player =
        new Entity()
            .addComponent(
                PlayerFactory.installDefaultCatalog(
                    new ShopComponent(), new WeaponGenerator(), new ConsumableGenerator()))
            .addComponent(new InventoryComponent(1000))
            .addComponent(shopDisplay);
    player.create();
    player.setPosition(1f, 0f);

    shopkeeper =
        new Entity()
            .addComponent(new DialogueProximityComponent(player, 2f))
            .addComponent(new ShopkeeperComponent(player));
    shopkeeper.create();
    shopkeeper.setPosition(0f, 0f);
  }

  @AfterEach
  void tearDown() {
    Gdx.input = previousInput;
  }

  @Test
  void shouldOpenShopWhenShopKeyPressedInRange() {
    shopkeeper.update();

    pressShopKey();
    shopkeeper.update();

    assertTrue(shopDisplay.isShopOpen());
  }

  @Test
  void shouldNotOpenShopOutOfRange() {
    player.setPosition(10f, 0f);
    shopkeeper.update();

    pressShopKey();
    shopkeeper.update();
    shopkeeper.update();

    assertFalse(shopDisplay.isShopOpen());
  }

  @Test
  void shouldNotOpenShopWithoutShopKey() {
    shopkeeper.update();
    shopkeeper.update();

    assertFalse(shopDisplay.isShopOpen());
  }

  @Test
  void shouldCloseShopWhenPlayerLeavesRange() {
    openShopInRange();
    assertTrue(shopDisplay.isShopOpen(), "the shop should start open");

    player.setPosition(10f, 0f);
    shopkeeper.update();
    shopkeeper.update();

    assertFalse(shopDisplay.isShopOpen());
  }

  @Test
  void shouldCloseShopWhenShopKeyPressedAgain() {
    openShopInRange();
    assertTrue(shopDisplay.isShopOpen(), "the shop should start open");

    pressShopKey();
    shopkeeper.update();

    assertFalse(shopDisplay.isShopOpen());
  }

  /** Makes the shop key report as just pressed. */
  private void pressShopKey() {
    when(input.isKeyJustPressed(ShopkeeperComponent.SHOP_KEY)).thenReturn(true);
  }

  /** Makes the shop key report as not pressed. */
  private void releaseShopKey() {
    when(input.isKeyJustPressed(ShopkeeperComponent.SHOP_KEY)).thenReturn(false);
  }

  /** Opens the shop with the shop key while in range, then releases the key. */
  private void openShopInRange() {
    shopkeeper.update();
    pressShopKey();
    shopkeeper.update();
    releaseShopKey();
  }
}
