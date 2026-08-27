package com.csse3200.game.components.player;

import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.components.Component;
import com.csse3200.game.components.loot.WeaponItem;
import com.csse3200.game.components.loot.WeaponType;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.factories.ArrowFactory;
import com.csse3200.game.services.ServiceLocator;

/** Handles player attacks based on the currently equipped weapon. */
public class WeaponAttackComponent extends Component {
  private final WeaponItem weapon;
  private Vector2 attackDirection = new Vector2(1f, 0f);

  public WeaponAttackComponent(WeaponItem weapon) {
    if (weapon == null) {
      throw new IllegalArgumentException("Weapon must not be null.");
    }

    this.weapon = weapon;
  }

  @Override
  public void create() {
    entity.getEvents().addListener("weaponAttack", this::attack);
    entity.getEvents().addListener("walk", this::updateAttackDirection);
  }

  private void updateAttackDirection(Vector2 direction) {
    if (direction != null && !direction.isZero()) {
      attackDirection = direction.cpy().nor();
    }
  }

  /** Performs an attack using the equipped weapon. */
  void attack() {
    WeaponType type = weapon.getWeaponType();

    switch (type) {
      case SWORD:
        swordAttack();
        break;
      case BOW:
        bowAttack();
        break;
      default:
        throw new IllegalStateException("Unsupported weapon type: " + type);
    }
  }

  private void swordAttack() {
    entity.getEvents().trigger("swordAttack", weapon.getDamage());
  }

  private void bowAttack() {
    entity.getEvents().trigger("bowAttack", weapon.getDamage());

    Vector2 spawnPosition = entity.getCenterPosition();

    Entity arrow = ArrowFactory.createArrow(spawnPosition, attackDirection);
    ServiceLocator.getEntityService().register(arrow);
  }

  public WeaponItem getWeapon() {
    return weapon;
  }
}
