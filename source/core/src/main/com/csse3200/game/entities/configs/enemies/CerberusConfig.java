package com.csse3200.game.entities.configs.enemies;

import com.csse3200.game.entities.configs.BaseEntityConfig;
import com.csse3200.game.entities.configs.attacks.MeleeAttackConfig;

/**
 * Configuration for Cerberus: a stationary mini-boss who bites very often (three heads, modelled
 * here as a single melee attack with a deliberately short cooldown rather than three separate
 * attack components - see {@link com.csse3200.game.entities.factories.NPCFactory#createCerberus}).
 * Carries no weapon - his bite flows through {@link
 * com.csse3200.game.components.loot.WeaponItem#natural}, same as every other natural-weapon mini-
 * boss.
 *
 * <p>Satisfies the team's "boss health near 100" design rule. The "melee range below ranged range"
 * rule does not apply - Cerberus has no ranged attack.
 */
public class CerberusConfig extends BaseEntityConfig {
  public MeleeAttackConfig melee;
}
