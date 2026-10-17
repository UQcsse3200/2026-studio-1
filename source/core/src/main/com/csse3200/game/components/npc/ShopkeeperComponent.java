package com.csse3200.game.components.npc;

import com.badlogic.gdx.Gdx;
import com.csse3200.game.components.Component;
import com.csse3200.game.components.player.ShopDisplay;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.pausemenu.KeybindSettings;

/**
 * Toggles the shop open/closed with the rebindable "shop" key while the player is in dialogue
 * range, and closes it when they leave. The key used to be hard-coded to F, which collides with the
 * default "specialAttack" binding and only ever opened the shop (pressing it again just reset the
 * shop back to the Items tab instead of closing it).
 */
public class ShopkeeperComponent extends Component {
  private final Entity player;
  private boolean playerInRange = false;

  /**
   * @param player the player entity whose shop this NPC toggles.
   */
  public ShopkeeperComponent(Entity player) {
    this.player = player;
  }

  /**
   * Toggles the shop when the bound "shop" key is pressed while the player is in range, and closes
   * it if the player walks out of range. Does nothing if "shop" is currently unbound.
   */
  @Override
  public void update() {
    DialogueProximityComponent proximity = entity.getComponent(DialogueProximityComponent.class);
    if (proximity == null || player == null) {
      return;
    }

    boolean inRange = proximity.isPlayerInRange();
    int shopKey = KeybindSettings.getKey("shop");
    boolean shopKeyPressed =
        shopKey != KeybindSettings.UNBOUND && Gdx.input.isKeyJustPressed(shopKey);

    if (inRange && shopKeyPressed) {
      toggleShop();
    } else if (playerInRange && !inRange) {
      closeShop();
    }
    playerInRange = inRange;
  }

  /** Opens the shop if it's currently closed, or closes it if it's already open. */
  private void toggleShop() {
    ShopDisplay shop = player.getComponent(ShopDisplay.class);
    if (shop == null) {
      return;
    }
    if (shop.isShopOpen()) {
      shop.closeShop();
    } else {
      player.getEvents().trigger("openShop");
    }
  }

  /** Closes the shop if it's open - used when the player walks out of dialogue range. */
  private void closeShop() {
    ShopDisplay shop = player.getComponent(ShopDisplay.class);
    if (shop != null) {
      shop.closeShop();
    }
  }
}
