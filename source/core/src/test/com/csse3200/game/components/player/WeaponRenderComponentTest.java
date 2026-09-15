package com.csse3200.game.components.player;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.csse3200.game.components.loot.WeaponItem;
import com.csse3200.game.components.loot.WeaponType;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class WeaponRenderComponentTest {
  @Test
  void shouldStopRenderingWeaponAfterDroppingSelectedStack() {
    Texture swordTexture = mock(Texture.class);
    when(swordTexture.getWidth()).thenReturn(32);
    when(swordTexture.getHeight()).thenReturn(32);
    ResourceService resources = mock(ResourceService.class);
    when(resources.getAsset("images/items/sword.png", Texture.class)).thenReturn(swordTexture);
    ServiceLocator.registerResourceService(resources);
    ServiceLocator.registerRenderService(new RenderService());

    InventoryComponent inventory = new InventoryComponent(0);
    inventory.addItem(new WeaponItem("Sword", WeaponType.SWORD, 10, 1, 1));
    WeaponRenderComponent renderer = new WeaponRenderComponent();
    ItemDropComponent drop = new ItemDropComponent((item, owner) -> new Entity(), loot -> {});
    new Entity().addComponent(inventory).addComponent(renderer).addComponent(drop).create();
    SpriteBatch batch = mock(SpriteBatch.class);

    renderer.render(batch);
    verify(batch)
        .draw(
            eq(swordTexture),
            anyFloat(),
            anyFloat(),
            anyFloat(),
            anyFloat(),
            anyFloat(),
            anyFloat(),
            anyFloat(),
            anyFloat(),
            anyFloat(),
            anyInt(),
            anyInt(),
            anyInt(),
            anyInt(),
            anyBoolean(),
            anyBoolean());
    clearInvocations(batch);

    assertTrue(drop.dropActiveStack());
    assertEquals(1, inventory.getActiveSlot());
    renderer.render(batch);

    verifyNoInteractions(batch);
  }
}
