package com.csse3200.game.entities.factories;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.graphics.Texture;
import com.csse3200.game.components.loot.ConsumableGenerator;
import com.csse3200.game.components.loot.ConsumableItem;
import com.csse3200.game.components.loot.ConsumableType;
import com.csse3200.game.components.loot.LootPickupComponent;
import com.csse3200.game.components.loot.WeaponGenerator;
import com.csse3200.game.components.loot.WeaponItem;
import com.csse3200.game.components.loot.WeaponType;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.physics.PhysicsService;
import com.csse3200.game.rendering.BobbingTextureRenderComponent;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.rendering.TextureRenderComponent;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class LootFactoryTest {

  @BeforeEach
  void setUp() {
    Texture texture = mock(Texture.class);
    when(texture.getWidth()).thenReturn(16);
    when(texture.getHeight()).thenReturn(16);

    ResourceService resourceService = mock(ResourceService.class);
    when(resourceService.getAsset(anyString(), eq(Texture.class))).thenReturn(texture);

    ServiceLocator.registerResourceService(resourceService);
    ServiceLocator.registerRenderService(mock(RenderService.class));
    ServiceLocator.registerTimeSource(mock(GameTime.class));
    ServiceLocator.registerPhysicsService(new PhysicsService());
  }

  /** Consumables get their own sprite and the bobbing motion, not the currency animation. */
  @Test
  void shouldRenderEveryConsumableWithItsOwnBobbingSprite() {
    ConsumableGenerator generator = new ConsumableGenerator();

    for (ConsumableType type : ConsumableType.values()) {
      ConsumableItem item = generator.generateConsumable(type, 1);
      Entity loot = LootFactory.createLoot(item);

      assertNotNull(
          loot.getComponent(BobbingTextureRenderComponent.class),
          type + " should be drawn with a bobbing sprite");
      assertNull(
          loot.getComponent(TextureRenderComponent.class),
          type + " should not fall through to the static weapon sprite");
    }
  }

  /** A consumable dropped as loot can be walked over and collected, the same as a weapon. */
  @Test
  void shouldMakeConsumableLootPickupable() {
    ConsumableItem item =
        new ConsumableGenerator().generateConsumable(ConsumableType.HEALTH_POTION, 1);

    Entity loot = LootFactory.createLoot(item);

    assertNotNull(loot.getComponent(LootPickupComponent.class));
  }

  /** Adding the consumable branch must not change how weapons are built. */
  @Test
  void shouldStillRenderWeaponsWithAStaticSprite() {
    WeaponItem weapon = new WeaponGenerator().generateWeapon(WeaponType.SWORD, 1);

    Entity loot = LootFactory.createLoot(weapon);

    assertNotNull(loot.getComponent(TextureRenderComponent.class));
    assertNull(loot.getComponent(BobbingTextureRenderComponent.class));
    assertNotNull(loot.getComponent(LootPickupComponent.class));
  }
}
