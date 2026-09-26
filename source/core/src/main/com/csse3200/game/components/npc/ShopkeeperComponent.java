package com.csse3200.game.components.npc;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.csse3200.game.components.Component;
import com.csse3200.game.components.player.ShopDisplay;
import com.csse3200.game.entities.Entity;

// Opens the shop with F while the player is in dialogue range, and closes it when they leave.
public class ShopkeeperComponent extends Component {
  private final Entity player;
  private boolean wasInRange = false;

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
    if (inRange && Gdx.input.isKeyJustPressed(Input.Keys.F)) {
      player.getEvents().trigger("openShop");
    } else if (wasInRange && !inRange) {
      ShopDisplay shop = player.getComponent(ShopDisplay.class);
      if (shop != null) {
        shop.closeShop();
      }
    }
    wasInRange = inRange;
  }
}
