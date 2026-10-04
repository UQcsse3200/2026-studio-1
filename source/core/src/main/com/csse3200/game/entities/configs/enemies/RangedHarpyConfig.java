package com.csse3200.game.entities.configs.enemies;

<<<<<<< HEAD
import com.csse3200.game.entities.configs.BaseEntityConfig;
import com.csse3200.game.entities.configs.attacks.RangedAttackConfig;

/**
 * Configuration for a Ranged Harpy: a flying ranged enemy mirroring the (ground) Ranged Skeleton's
 * bow attack and stats - see {@link
 * com.csse3200.game.entities.factories.NPCFactory#createRangedHarpy}. Not a mini-boss, so
 * health/stats match Ranged Skeleton's rather than the "near 100" boss rule.
 */
public class RangedHarpyConfig extends BaseEntityConfig {
=======
import com.csse3200.game.entities.configs.attacks.RangedAttackConfig;

/** Ranged Harpy: flying, bow. */
public class RangedHarpyConfig extends FlyingEnemyConfig {
>>>>>>> origin/enemies
  public RangedAttackConfig ranged;
}
