package com.csse3200.game.components.npc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.csse3200.game.components.player.InventoryComponent;
import com.csse3200.game.components.player.ShopComponent;
import com.csse3200.game.components.player.ShopDisplay;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.pausemenu.KeybindSettings;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import java.lang.reflect.Field;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class ShopkeeperComponentTest {
  private Input previousInput;
  private Input input;
  private Entity player;
  private Entity shopkeeper;
  private int shopOpenRequests;

  @BeforeEach
  void setUp() {
    previousInput = Gdx.input;
    input = mock(Input.class);
    Gdx.input = input;

    player = new Entity();
    player.setPosition(1f, 0f);
    player.getEvents().addListener("openShop", () -> shopOpenRequests++);
    shopOpenRequests = 0;

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

    assertEquals(1, shopOpenRequests);
  }

  @Test
  void shouldNotOpenShopOutOfRange() {
    player.setPosition(10f, 0f);
    shopkeeper.update();

    pressShopKey();
    shopkeeper.update();

    assertEquals(0, shopOpenRequests);
  }

  @Test
  void shouldNotOpenShopWithoutShopKey() {
    shopkeeper.update();
    shopkeeper.update();

    assertEquals(0, shopOpenRequests);
  }

  @Test
  void shouldCloseShopWhenPlayerLeavesRange() throws Exception {
    ShopDisplay shopDisplay = addShopToPlayer();
    player.getEvents().trigger("openShop");
    assertTrue(shopTable(shopDisplay).isVisible(), "the shop should start open");

    shopkeeper.update();
    shopkeeper.update();
    player.setPosition(10f, 0f);
    shopkeeper.update();
    shopkeeper.update();

    assertFalse(shopTable(shopDisplay).isVisible());
  }

  /** Makes the shop keybind report as just pressed. */
  private void pressShopKey() {
    when(input.isKeyJustPressed(KeybindSettings.getKey("shop"))).thenReturn(true);
  }

  /** Gives the player a real shop display backed by a mocked stage. */
  private ShopDisplay addShopToPlayer() {
    Viewport viewport = mock(Viewport.class);
    when(viewport.getWorldWidth()).thenReturn(1280f);
    when(viewport.getWorldHeight()).thenReturn(720f);
    Stage stage = mock(Stage.class);
    when(stage.getViewport()).thenReturn(viewport);

    RenderService renderService = new RenderService();
    renderService.setStage(stage);
    ServiceLocator.registerRenderService(renderService);
    ServiceLocator.registerResourceService(mock(ResourceService.class));

    ShopDisplay shopDisplay = new ShopDisplay();
    player =
        new Entity()
            .addComponent(new ShopComponent().seedDefaultCatalog())
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
    return shopDisplay;
  }

  /** Reads the shop window, which ShopDisplay keeps private. */
  private Table shopTable(ShopDisplay shopDisplay) throws Exception {
    Field field = ShopDisplay.class.getDeclaredField("shopTable");
    field.setAccessible(true);
    return (Table) field.get(shopDisplay);
  }
}
