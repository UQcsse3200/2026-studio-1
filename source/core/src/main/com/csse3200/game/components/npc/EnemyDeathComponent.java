package com.csse3200.game.components.npc;

import com.badlogic.gdx.Gdx;
import com.csse3200.game.components.Component;
import com.csse3200.game.components.player.InventoryComponent;
import com.csse3200.game.components.player.ItemDropComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EnemyDeathComponent extends Component {
  private static final Logger logger = LoggerFactory.getLogger(EnemyDeathComponent.class);

  @Override
  public void create() {
    entity.getEvents().addListener("death", this::onDeathWrapper);
  }

  private void onDeathWrapper() {
    Gdx.app.postRunnable(this::onDeath);
  }

  private void onDeath() {
    if (entity.isDisposed()) {
      logger.info(
          "Enemy: {} already died.", entity.getComponent(EnemyTypeComponent.class).getEnemyLabel());
      return;
    }
    ItemDropComponent dropper = entity.getComponent(ItemDropComponent.class);
    if (dropper != null) {
      int goldNum;
      InventoryComponent inventory = entity.getComponent(InventoryComponent.class);
      if (inventory != null) {
        goldNum = entity.getComponent(InventoryComponent.class).getGold();
      } else {
        goldNum = 0;
      }
      // Drop gold
      if (dropper.dropGold()) {
        logger.info(
            "Enemy {} dropped {} gold",
            entity.getComponent(EnemyTypeComponent.class).getEnemyLabel(),
            goldNum);
      }

      // Drop weapons and consumables
      while (dropper.dropFirstStack()) {
        logger.info("Enemy {} dropped item",
            entity.getComponent(EnemyTypeComponent.class).getEnemyLabel());
      }
    }

    logger.info(
        "Enemy {} died at x:{} y:{}",
        entity.getComponent(EnemyTypeComponent.class).getEnemyLabel(),
        entity.getCenterPosition().x,
        entity.getCenterPosition().y);
    entity.dispose();
  }
}
