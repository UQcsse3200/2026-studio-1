package com.csse3200.game.components.npc;

import com.badlogic.gdx.Gdx;
import com.csse3200.game.Quests.Quest;
import com.csse3200.game.components.Component;
import com.csse3200.game.components.player.InventoryComponent;
import com.csse3200.game.components.player.ItemDropComponent;
import com.csse3200.game.perks.BonusLootDrop;
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
      logger.info("Enemy: {} already died.", getLabel());
      return;
    }
    Quest.incrementGlobalEnemiesKilled();

    ItemDropComponent dropper = entity.getComponent(ItemDropComponent.class);
    if (dropper != null) {
      // Drop gold
      if (dropper.dropGold()) {
        InventoryComponent inventory = entity.getComponent(InventoryComponent.class);
        int goldNum = (inventory != null) ? inventory.getGold() : 0;
        logger.info("Enemy {} dropped {} gold", getLabel(), goldNum);
      }

      // Drop weapons and consumables
      while (dropper.dropFirstStack()) {
        logger.info("Enemy {} dropped item", getLabel());
      }
    }
    BonusLootDrop.tryBonusDrop(entity);

    logger.info(
            "Enemy {} died at x:{} y:{}",
            getLabel(),
            entity.getCenterPosition().x,
            entity.getCenterPosition().y);
    entity.dispose();
  }

  /** Label for log messages. Friendly NPCs have no EnemyTypeComponent, so they log as "NPC". */
  private String getLabel() {
    EnemyTypeComponent type = entity.getComponent(EnemyTypeComponent.class);
    return type == null ? "NPC" : type.getEnemyLabel();
  }
}