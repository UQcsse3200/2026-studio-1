package com.csse3200.game.perks;

import com.csse3200.game.components.loot.Item;
import com.csse3200.game.components.loot.ItemType;
import com.csse3200.game.components.loot.LootTable;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.factories.LootFactory;
import com.csse3200.game.services.ServiceLocator;
import java.util.Random;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The effect of the {@code snatcher} ("Loot Drop") perk: a chance for a killed enemy to drop one
 * extra item.
 *
 * <p>Enemies always drop exactly what they carry (see {@code ItemDropComponent}); there is no
 * drop-chance roll in the game to boost. So this adds its own independent roll on top: while the
 * perk is active, each enemy death has a {@link #BONUS_DROP_CHANCE} chance of also dropping one
 * item rolled from the standard weighted loot table.
 *
 * <p>Stateless - the perk only has to be active at the moment the enemy dies, so there is nothing
 * to apply or revert when it is toggled. Called from {@code EnemyDeathComponent}.
 */
public final class BonusLootDrop {
  private static final Logger logger = LoggerFactory.getLogger(BonusLootDrop.class);
  private static final String PERK_ID = "snatcher";

  /** Chance (0-1) that a kill drops one extra item while the perk is active. */
  private static final float BONUS_DROP_CHANCE = 1f;

  private static final float GOLD_SHARE = 0.4f;

  /** Gold per bonus coin drop, inclusive. Enemies carry 3, so this is about one extra enemy. */
  private static final int MIN_BONUS_GOLD = 2;

  private static final int MAX_BONUS_GOLD = 6;
  private static final float DROP_GAP = 0.25f;
  private static final Random random = new Random();

  private BonusLootDrop() {}

  /**
   * Rolls for a bonus drop where {@code deadEnemy} died. Does nothing unless the Loot Drop perk is
   * active, so it is safe to call on every enemy death.
   *
   * @param deadEnemy the enemy that just died; must not be disposed yet (its position is read)
   */
  public static void tryBonusDrop(Entity deadEnemy) {
    Perk snatcherPerk = PerkService.getPerk(PERK_ID);
    if (snatcherPerk == null || !snatcherPerk.isActive()) {
      return;
    }
    if (random.nextFloat() >= BONUS_DROP_CHANCE) {
      return;
    }

    Item item = random.nextFloat() < GOLD_SHARE ? rollGold() : rollTableItem();
    Entity loot = LootFactory.createDroppedLoot(item, deadEnemy);

    // Land past the spot the enemy's own drops use, so the bonus doesn't sit exactly on top of
    // them.
    float dropX = deadEnemy.getPosition().x + deadEnemy.getScale().x + 2 * DROP_GAP;
    loot.setPosition(dropX, deadEnemy.getPosition().y);
    ServiceLocator.getEntityService().register(loot);

    logger.info("Loot Drop perk: bonus {} x{} dropped", item.getName(), item.getQuantity());
  }

  private static Item rollGold() {
    int amount = MIN_BONUS_GOLD + random.nextInt(MAX_BONUS_GOLD - MIN_BONUS_GOLD + 1);
    return new Item("Gold", ItemType.CURRENCY, amount, 99);
  }

  private static Item rollTableItem() {
    return LootTable.createDefault(random.nextLong()).rollItem();
  }
}
