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
    String enemyType = entity.getComponent(EnemyTypeComponent.class).getEnemyLabel();
    if (entity.isDisposed()) {
      logger.info(
          "Enemy: {} already died.", entity.getComponent(EnemyTypeComponent.class).getEnemyLabel());
      return;
    }
    ItemDropComponent dropper = entity.getComponent(ItemDropComponent.class);
    if (dropper != null) {

      int goldNum = entity.getComponent(InventoryComponent.class).getGold();
      // Drop gold
      if (dropper.dropGold()) {
        logger.info(
            "Enemy {} dropped {} gold",
            entity.getComponent(EnemyTypeComponent.class).getEnemyLabel(),
            goldNum);
      }

      // Drop weapons and consumables
      while (dropper.dropFirstStack()) {
        logger.info(
            "Enemy {} dropped item", entity.getComponent(EnemyTypeComponent.class).getEnemyLabel());
      }
    }

    entity.dispose();
    logger.info(
        "Enemy {} died at x:{} y:{}",
        entity.getComponent(EnemyTypeComponent.class).getEnemyLabel(),
        entity.getCenterPosition().x,
        entity.getCenterPosition().y);
  }
}
