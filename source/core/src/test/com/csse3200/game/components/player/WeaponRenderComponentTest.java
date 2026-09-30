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
import com.csse3200.game.components.loot.WeaponTier;
import com.csse3200.game.components.loot.WeaponType;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

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

  @Test
  void shouldRenderTierOneWeaponAtNormalScale() {
    Texture swordTexture = mock(Texture.class);
    when(swordTexture.getWidth()).thenReturn(32);
    when(swordTexture.getHeight()).thenReturn(32);

    ResourceService resources = mock(ResourceService.class);
    when(resources.getAsset("images/items/sword.png", Texture.class)).thenReturn(swordTexture);

    ServiceLocator.registerResourceService(resources);
    ServiceLocator.registerRenderService(new RenderService());

    InventoryComponent inventory = new InventoryComponent(0);

    inventory.addItem(
        new WeaponItem("Tier 1 Sword", WeaponType.SWORD, WeaponTier.TIER_1, 1, 1, 0f));

    WeaponRenderComponent renderer = new WeaponRenderComponent();

    new Entity().addComponent(inventory).addComponent(renderer).create();

    SpriteBatch batch = mock(SpriteBatch.class);

    renderer.render(batch);

    ArgumentCaptor<Float> scaleX = ArgumentCaptor.forClass(Float.class);
    ArgumentCaptor<Float> scaleY = ArgumentCaptor.forClass(Float.class);

    verify(batch)
        .draw(
            eq(swordTexture),
            anyFloat(),
            anyFloat(),
            anyFloat(),
            anyFloat(),
            anyFloat(),
            anyFloat(),
            scaleX.capture(),
            scaleY.capture(),
            anyFloat(),
            anyInt(),
            anyInt(),
            anyInt(),
            anyInt(),
            anyBoolean(),
            anyBoolean());

    assertEquals(1.00f, scaleX.getValue(), 0.001f);
    assertEquals(1.00f, scaleY.getValue(), 0.001f);
  }

  @Test
  void shouldRenderTierTwoSwordWithUpgradedTextureAndScale() {
    Texture tierTwoSwordTexture = mock(Texture.class);
    when(tierTwoSwordTexture.getWidth()).thenReturn(32);
    when(tierTwoSwordTexture.getHeight()).thenReturn(32);

    ResourceService resources = mock(ResourceService.class);
    when(resources.getAsset("images/items/sword_t2.png", Texture.class))
        .thenReturn(tierTwoSwordTexture);

    ServiceLocator.registerResourceService(resources);
    ServiceLocator.registerRenderService(new RenderService());

    InventoryComponent inventory = new InventoryComponent(0);

    inventory.addItem(
        new WeaponItem("Tier 2 Sword", WeaponType.SWORD, WeaponTier.TIER_2, 1, 1, 0f));

    WeaponRenderComponent renderer = new WeaponRenderComponent();

    new Entity().addComponent(inventory).addComponent(renderer).create();

    SpriteBatch batch = mock(SpriteBatch.class);

    renderer.render(batch);

    ArgumentCaptor<Float> scaleX = ArgumentCaptor.forClass(Float.class);
    ArgumentCaptor<Float> scaleY = ArgumentCaptor.forClass(Float.class);

    verify(batch)
        .draw(
            eq(tierTwoSwordTexture),
            anyFloat(),
            anyFloat(),
            anyFloat(),
            anyFloat(),
            anyFloat(),
            anyFloat(),
            scaleX.capture(),
            scaleY.capture(),
            anyFloat(),
            anyInt(),
            anyInt(),
            anyInt(),
            anyInt(),
            anyBoolean(),
            anyBoolean());

    assertEquals(1.20f, scaleX.getValue(), 0.001f);
    assertEquals(1.20f, scaleY.getValue(), 0.001f);
  }

  @Test
  void shouldRenderTierThreeSwordWithUpgradedTextureAndScale() {
    Texture tierThreeSwordTexture = mock(Texture.class);
    when(tierThreeSwordTexture.getWidth()).thenReturn(32);
    when(tierThreeSwordTexture.getHeight()).thenReturn(32);

    ResourceService resources = mock(ResourceService.class);
    when(resources.getAsset("images/items/sword_t3.png", Texture.class))
        .thenReturn(tierThreeSwordTexture);

    ServiceLocator.registerResourceService(resources);
    ServiceLocator.registerRenderService(new RenderService());

    InventoryComponent inventory = new InventoryComponent(0);

    inventory.addItem(
        new WeaponItem("Tier 3 Sword", WeaponType.SWORD, WeaponTier.TIER_3, 1, 1, 0f));

    WeaponRenderComponent renderer = new WeaponRenderComponent();

    new Entity().addComponent(inventory).addComponent(renderer).create();

    SpriteBatch batch = mock(SpriteBatch.class);

    renderer.render(batch);

    ArgumentCaptor<Float> scaleX = ArgumentCaptor.forClass(Float.class);
    ArgumentCaptor<Float> scaleY = ArgumentCaptor.forClass(Float.class);

    verify(batch)
        .draw(
            eq(tierThreeSwordTexture),
            anyFloat(),
            anyFloat(),
            anyFloat(),
            anyFloat(),
            anyFloat(),
            anyFloat(),
            scaleX.capture(),
            scaleY.capture(),
            anyFloat(),
            anyInt(),
            anyInt(),
            anyInt(),
            anyInt(),
            anyBoolean(),
            anyBoolean());

    assertEquals(1.45f, scaleX.getValue(), 0.001f);
    assertEquals(1.45f, scaleY.getValue(), 0.001f);
  }
}
