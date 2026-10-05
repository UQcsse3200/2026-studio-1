package com.csse3200.game.components.pet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.csse3200.game.components.player.InventoryComponent;
import com.csse3200.game.components.player.ShopComponent;
import com.csse3200.game.components.player.ShopDisplay;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.physics.PhysicsService;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.rendering.AnimationRenderComponent;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/** Exercises pet purchases with the real shop listeners, pet atlas, and Box2D bodies. */
@ExtendWith(GameExtension.class)
class PetShopIntegrationTest {
  private Entity owner;
  private ShopComponent shop;
  private PetManagerComponent manager;
  private SpriteBatch batch;

  @BeforeEach
  void setUp() {
    GameTime timeSource = mock(GameTime.class);
    when(timeSource.getDeltaTime()).thenReturn(0.1f);
    ServiceLocator.registerTimeSource(timeSource);
    ServiceLocator.registerPhysicsService(new PhysicsService());
    ServiceLocator.registerEntityService(new EntityService());

    Viewport viewport = mock(Viewport.class);
    when(viewport.getWorldWidth()).thenReturn(1280f);
    when(viewport.getWorldHeight()).thenReturn(720f);
    Stage stage = mock(Stage.class);
    when(stage.getViewport()).thenReturn(viewport);
    RenderService renderService = new RenderService();
    renderService.setStage(stage);
    ServiceLocator.registerRenderService(renderService);
    batch = mock(SpriteBatch.class);

    shop = new ShopComponent().seedDefaultCatalog();
    manager = new PetManagerComponent();
    owner =
        new Entity()
            .addComponent(new InventoryComponent(100))
            .addComponent(shop)
            .addComponent(new ShopDisplay())
            .addComponent(manager);
    owner.setScale(2f, 3f);
    ServiceLocator.getEntityService().register(owner);
  }

  @AfterEach
  void tearDown() {
    ServiceLocator.getEntityService().dispose();
    ServiceLocator.getPhysicsService().getPhysics().dispose();
    ServiceLocator.getRenderService().dispose();
  }

  @Test
  void shouldPurchaseAndKeepFirstPetActiveUntilSwitched() {
    InventoryComponent inventory = owner.getComponent(InventoryComponent.class);

    // Purchase Bird - first pet should be stored and automatically activated.
    owner.setPosition(10f, 5f);
    assertTrue(shop.buyPet(1));

    Entity birdEntity = manager.getActivePet();
    assertNotNull(birdEntity);
    assertSame(shop.getPetListing(1).getProduct(), inventory.getPet(1));
    assertSame(shop.getPetListing(1).getProduct(), manager.getActivePetType());

    assertSame(owner, birdEntity.getComponent(PetComponent.class).getOwner());
    assertEquals(
            1, ServiceLocator.getPhysicsService().getPhysics().getWorld().getBodyCount());

    AnimationRenderComponent birdAnimator =
            birdEntity.getComponent(AnimationRenderComponent.class);

    assertEquals("bird_right", birdAnimator.getCurrentAnimation());
    assertTrue(birdEntity.getPosition().x < owner.getPosition().x);
    assertTrue(
            birdEntity.getPosition().y > owner.getPosition().y + owner.getScale().y);

    // Purchase Bat - it should occupy the second pet slot,
    // but Bird should remain active.
    assertTrue(shop.buyPet(2));

    assertSame(shop.getPetListing(2).getProduct(), inventory.getPet(2));
    assertTrue(inventory.isPetInventoryFull());

    assertSame(birdEntity, manager.getActivePet());
    assertSame(shop.getPetListing(1).getProduct(), manager.getActivePetType());
    assertEquals(
            1, ServiceLocator.getPhysicsService().getPhysics().getWorld().getBodyCount());

    // Purchasing Spirit should fail because both pet slots are occupied.
    int goldBeforeFailedPurchase = inventory.getGold();

    assertFalse(shop.buyPet(3));

    assertEquals(goldBeforeFailedPurchase, inventory.getGold());
    assertSame(birdEntity, manager.getActivePet());
    assertSame(shop.getPetListing(1).getProduct(), manager.getActivePetType());

    // The active Bird should still follow the player.
    Vector2 start = birdEntity.getPosition();

    owner.setPosition(11f, 6f);
    updateAndRender();

    assertTrue(
            birdEntity.getPosition().x > start.x
                    && birdEntity.getPosition().x < start.x + 1f);
    assertTrue(
            birdEntity.getPosition().y > start.y
                    && birdEntity.getPosition().y < start.y + 1f);

    // Check direction animation.
    owner.setPosition(10f, 6f);
    updateAndRender();

    assertEquals("bird_left", birdAnimator.getCurrentAnimation());

    // Moving far away should cause the active pet to catch up.
    owner.setPosition(30f, 20f);
    updateAndRender();

    assertEquals("bird_right", birdAnimator.getCurrentAnimation());
    assertEquals(
            30f - birdEntity.getScale().x - 0.25f,
            birdEntity.getPosition().x,
            0.001f);
    assertEquals(23.25f, birdEntity.getPosition().y, 0.001f);

    assertTrue(
            birdEntity
                    .getPosition()
                    .epsilonEquals(
                            birdEntity.getComponent(PhysicsComponent.class).getBody().getPosition()));

    // Bird costs 20 and Bat costs 30. Failed Spirit purchase costs nothing.
    assertEquals(50, inventory.getGold());

    // Disposing the owner should also dispose the active pet.
    owner.dispose();

    assertFalse(manager.hasActivePet());
    assertNull(manager.getActivePetType());
    assertEquals(
            0, ServiceLocator.getPhysicsService().getPhysics().getWorld().getBodyCount());

    updateAndRender();
  }

  private void updateAndRender() {
    ServiceLocator.getPhysicsService().getPhysics().update();
    ServiceLocator.getEntityService().update();
    ServiceLocator.getRenderService().render(batch);
  }
}
