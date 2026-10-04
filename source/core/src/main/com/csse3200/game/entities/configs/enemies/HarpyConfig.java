package com.csse3200.game.entities.configs.enemies;

<<<<<<< HEAD
import com.csse3200.game.entities.configs.BaseEntityConfig;
import com.csse3200.game.entities.configs.attacks.MeleeAttackConfig;

/**
 * Configuration for a Harpy: a flying melee enemy mirroring the (ground) Skeleton's sword attack
 * and stats - see {@link com.csse3200.game.entities.factories.NPCFactory#createHarpy}. Not a mini-
 * boss, so health/stats match Skeleton's rather than the "near 100" boss rule.
 */
public class HarpyConfig extends BaseEntityConfig {
=======
import com.csse3200.game.entities.configs.attacks.MeleeAttackConfig;

/** Melee Harpy: flying, sword. */
public class HarpyConfig extends FlyingEnemyConfig {
>>>>>>> origin/enemies
  public MeleeAttackConfig melee;
}
