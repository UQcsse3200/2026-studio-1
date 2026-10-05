package com.csse3200.game.components;

import com.csse3200.game.components.player.BallisticShieldComponent;
import com.csse3200.game.entities.Entity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CombatStatsComponent extends Component {
  private static final Logger logger = LoggerFactory.getLogger(CombatStatsComponent.class);

  private int health;
  private int baseAttack;
  private int shieldHits;

  public CombatStatsComponent(int health, int baseAttack) {
    setHealth(health);
    setBaseAttack(baseAttack);
  }

  public Boolean isDead() {
    return health == 0;
  }

  public int getHealth() {
    return health;
  }

  public void setHealth(int health) {
    if (health >= 0) {
      this.health = health;
    } else {
      this.health = 0;
    }

    if (entity != null) {
      entity.getEvents().trigger("updateHealth", this.health);
      if (this.health <= 0) {
        entity.getEvents().trigger("death");
      }
    }
  }

  public void addHealth(int health) {
    if (this.health + health >= 0) {
      this.health += health;
    } else {
      this.health = 0;
    }

    if (entity != null) {
      entity.getEvents().trigger("updateHealth", this.health);
      if (this.health <= 0) {
        entity.getEvents().trigger("death");
      }
    }
  }

  public int getBaseAttack() {
    return baseAttack;
  }

  public int getShieldHits() {
    return shieldHits;
  }

  public void setShieldHits(int shieldHits) {
    this.shieldHits = Math.max(0, shieldHits);
  }

  public void setBaseAttack(int attack) {
    if (attack >= 0) {
      this.baseAttack = attack;
    } else {
      logger.error("Can not set base attack to a negative attack value");
    }
  }

  public void hit(CombatStatsComponent attacker) {
    hit(attacker, attacker.getBaseAttack());
  }

  public void hit(CombatStatsComponent attacker, int damage) {
    if (damage < 0) {
      throw new IllegalArgumentException("Damage must not be negative.");
    }

    if (shieldHits > 0) {
      shieldHits--;
      return;
    }

    BallisticShieldComponent ballisticShield =
        entity == null ? null : entity.getComponent(BallisticShieldComponent.class);

    if (ballisticShield != null && ballisticShield.isActive()) {
      ballisticShield.blockDamage(attacker, damage);
      return;
    }

    int actualDamage = damage;

    boolean wasAlive = !isDead();

    int newHealth = getHealth() - actualDamage;
    setHealth(newHealth);

    if (actualDamage > 0 && entity != null && attacker != null) {
      Entity attackerEntity = attacker.getEntity();

      if (attackerEntity != null) {
        entity.getEvents().trigger("damagedBy", attackerEntity);
      }
    }

    if (wasAlive && isDead() && attacker != null && attacker.getEntity() != null) {
      attacker.getEntity().getEvents().trigger("enemyKilled");
    }
  }
}
