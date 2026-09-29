package com.csse3200.game.components.pet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
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
  void shouldPurchaseReplaceAndMoveAllPetsWithShopDisplay() {
    String[] prefixes = {"bird", "bat", "spirit"};
    Entity previousPet = null;

    for (int slot = 1; slot <= prefixes.length; slot++) {
      owner.setPosition(10f, 5f);
      // Both ShopDisplay and PetManagerComponent must accept the purchased pet argument.
      assertTrue(shop.buyPet(slot));

      Entity pet = manager.getActivePet();
      assertNotNull(pet);
      assertNotSame(previousPet, pet);
      assertSame(owner, pet.getComponent(PetComponent.class).getOwner());
      assertEquals(1, ServiceLocator.getPhysicsService().getPhysics().getWorld().getBodyCount());
      AnimationRenderComponent animator = pet.getComponent(AnimationRenderComponent.class);
      String prefix = prefixes[slot - 1];
      assertEquals(prefix + "_right", animator.getCurrentAnimation());
      assertTrue(pet.getPosition().x < owner.getPosition().x);
      assertTrue(pet.getPosition().y > owner.getPosition().y + owner.getScale().y);

      Vector2 start = pet.getPosition();
      owner.setPosition(11f, 6f);
      updateAndRender();
      assertTrue(pet.getPosition().x > start.x && pet.getPosition().x < start.x + 1f);
      assertTrue(pet.getPosition().y > start.y && pet.getPosition().y < start.y + 1f);

      owner.setPosition(10f, 6f);
      updateAndRender();
      assertEquals(prefix + "_left", animator.getCurrentAnimation());

      owner.setPosition(30f, 20f);
      updateAndRender();
      assertEquals(prefix + "_right", animator.getCurrentAnimation());
      assertEquals(30f - pet.getScale().x - 0.25f, pet.getPosition().x, 0.001f);
      assertEquals(23.25f, pet.getPosition().y, 0.001f);
      assertTrue(
          pet.getPosition()
              .epsilonEquals(pet.getComponent(PhysicsComponent.class).getBody().getPosition()));
      previousPet = pet;
    }

    assertEquals(10, owner.getComponent(InventoryComponent.class).getGold());
    owner.dispose();
    assertFalse(manager.hasActivePet());
    assertEquals(0, ServiceLocator.getPhysicsService().getPhysics().getWorld().getBodyCount());
    updateAndRender();
  }

  private void updateAndRender() {
    ServiceLocator.getPhysicsService().getPhysics().update();
    ServiceLocator.getEntityService().update();
    ServiceLocator.getRenderService().render(batch);
  }
}
