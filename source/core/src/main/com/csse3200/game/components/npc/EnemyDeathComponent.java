package com.csse3200.game.components.npc;

import com.csse3200.game.components.Component;
import com.csse3200.game.components.player.ItemDropComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EnemyDeathComponent extends Component {
  private static final Logger logger = LoggerFactory.getLogger(EnemyDeathComponent.class);

  @Override
  public void create() {
    entity.getEvents().addListener("death", this::onDeath);
  }

  private void onDeath() {
    ItemDropComponent dropper = entity.getComponent(ItemDropComponent.class);
    if (dropper != null) {
      // Drop gold
      if (dropper.dropGold()) {
        logger.info("Enemy {} dropped gold", entity);
      }
      // Drop weapons and consumables
      while (dropper.dropFirstStack()) {
        logger.info("Enemy {} dropped item", entity);
      }
    }

    entity.dispose();
  }
}
