package com.csse3200.game.components.npc;

import com.badlogic.gdx.Gdx;
import com.csse3200.game.components.Component;
import com.csse3200.game.components.player.ShopDisplay;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.pausemenu.KeybindSettings;

/** Opens the shop with the shop key while the player is in range, and closes it on leaving. */
public class ShopkeeperComponent extends Component {
  private static final String SHOP_ACTION = "shop";

  private final Entity player;
  private boolean playerInRange = false;

  public ShopkeeperComponent(Entity player) {
    this.player = player;
  }

  @Override
  public void update() {
    DialogueProximityComponent proximity = entity.getComponent(DialogueProximityComponent.class);
    if (proximity == null || player == null) {
      return;
    }

    boolean inRange = proximity.isPlayerInRange();
    if (inRange && isShopKeyJustPressed()) {
      player.getEvents().trigger("openShop");
    } else if (playerInRange && !inRange) {
      ShopDisplay shop = player.getComponent(ShopDisplay.class);
      if (shop != null) {
        shop.closeShop();
      }
    }
    playerInRange = inRange;
  }

  /** Checks the shop keybind, ignoring it while unbound so it never matches any key. */
  private boolean isShopKeyJustPressed() {
    int key = KeybindSettings.getKey(SHOP_ACTION);
    return key != KeybindSettings.UNBOUND && Gdx.input.isKeyJustPressed(key);
  }
}
