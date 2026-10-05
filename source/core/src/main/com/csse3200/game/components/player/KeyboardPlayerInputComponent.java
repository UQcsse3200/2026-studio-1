package com.csse3200.game.components.player;

import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.components.loot.WeaponGenerator;
import com.csse3200.game.components.loot.WeaponItem;
import com.csse3200.game.components.loot.WeaponType;
import com.csse3200.game.input.InputComponent;
import com.csse3200.game.utils.math.Vector2Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Input handler for the player for keyboard and touch (mouse) input. This input handler only uses
 * keyboard input.
 */
public class KeyboardPlayerInputComponent extends InputComponent {
  private static final Logger logger = LoggerFactory.getLogger(KeyboardPlayerInputComponent.class);

  private final Vector2 walkDirection = Vector2.Zero.cpy();
  private final Vector2 jumpDirection = Vector2.Zero.cpy();
  private final Vector2 dashDirection = Vector2.Zero.cpy();

  public KeyboardPlayerInputComponent() {
    super(5);
  }

  private boolean jumped = false;
  private boolean dashed = false;
  private boolean crouch = false;
  private boolean walkingLeft = false;
  private boolean walkingRight = false;
  private boolean walkingDown = false;
  private String direction = "Right";

  public String getDirection() {
    return this.direction;
  }

  /**
   * Triggers player events on specific keycodes.
   *
   * @return whether the input was processed
   * @see InputProcessor#keyDown(int)
   */
  @Override
  public boolean keyDown(int keycode) {
    entity.getEvents().trigger("idle", direction);

    SubLevelTravelComponent travel = entity.getComponent(SubLevelTravelComponent.class);
    if (travel != null && travel.isControlLocked()) {
      return true;
    }
    LadderComponent ladder = entity.getComponent(LadderComponent.class);
    if (ladder != null && ladder.isAutoClimbing()) {
      return true;
    }

    switch (keycode) {
      case Keys.E:
        if (travel != null && travel.beginTravel()) {
          return true;
        }
        if (ladder != null && ladder.beginAutoClimb()) {
          walkDirection.setZero();
          walkingLeft = false;
          walkingRight = false;
          walkingDown = false;
          entity.getEvents().trigger("walkStop");
          return true;
        }
        return false;

      case Keys.R:
        /*
         * R attempts to bribe the enemy that most recently damaged
         * the player.
         */
        PlayerActions playerActions = entity.getComponent(PlayerActions.class);

        if (playerActions != null && playerActions.bribeLastAttacker()) {
          logger.info("R key pressed - enemy bribe successful");
          return true;
        }

        return true;

      case Keys.W:
        LadderComponent ladderUp = entity.getComponent(LadderComponent.class);
        if (ladderUp != null && ladderUp.beginClimb(1f)) {
          entity.getEvents().trigger("climb");
          return true;
        }
        stopClimbing();
        jumpDirection.add(Vector2Utils.UP);
        triggerJumpEvent();
        entity.getEvents().trigger("jumping", direction);
        jumped = true;
        return true;

      case Keys.L:
        dashing();
        return true;

      case Keys.A:
        stopClimbing();
        walking('a');
        return true;

      case Keys.S:
        LadderComponent ladderDown = entity.getComponent(LadderComponent.class);
        if (ladderDown != null && ladderDown.beginClimb(-1f)) {
          return true;
        }
        stopClimbing();
        if (!walkingDown) {
          walkDirection.add(Vector2Utils.DOWN);
          walkingDown = true;
        }
        triggerWalkEvent();

        return true;

      case Keys.D:
        stopClimbing();
        walking('d');
        return true;

      case Keys.SPACE:
        entity.getEvents().trigger("attack");
        entity.getEvents().trigger("attacking", direction);
        return true;

      case Keys.F:
        entity.getEvents().trigger("specialAttack");
        return true;

      case Keys.G:
        entity.getEvents().trigger("areaAttack");
        return true;

      case Keys.Q:
        entity.getEvents().trigger("dropItem");
        return true;

      case Keys.B:
        activateAvailableShield();
        return true;

      case Keys.C:
        logger.info("C key pressed - triggering activateArmour");
        entity.getEvents().trigger("activateArmour");
        return true;

      case Keys.J:
        entity.getEvents().trigger("toggleQuestMenu");
        return true;

      case Keys.T:
        entity.getEvents().trigger("toggleTutorial");
        return true;

      case Keys.CONTROL_LEFT:
        entity.getEvents().trigger("ctrlChanged", true);
        entity.getEvents().trigger("crouchidle", direction);
        crouch = true;
        return true;

      case Keys.SHIFT_LEFT:
        entity.getEvents().trigger("slide", true);
        return true;

      case Keys.NUM_1:
        handleInventorySlot(1);
        return true;

      case Keys.NUM_2:
        handleInventorySlot(2);
        return true;

      case Keys.NUM_3:
        handleInventorySlot(3);
        return true;

      case Keys.NUM_4:
        handleInventorySlot(4);
        return true;

      case Keys.NUM_5:
        handleInventorySlot(5);
        return true;

      case Keys.F6:
        grantTestBow(2);
        return true;

      case Keys.F7:
        grantTestBow(3);
        return true;

      default:
        return false;
    }
  }

  /**
   * Activates the Ballistic Shield if the player is holding one.
   *
   * <p>If the player does not have a Ballistic Shield, the normal Tier 1 Shield is activated
   * instead. This keeps the existing B-key behaviour for the normal Shield.
   */
  private void activateAvailableShield() {
    BallisticShieldComponent ballisticShield = entity.getComponent(BallisticShieldComponent.class);

    if (ballisticShield != null && ballisticShield.hasShield()) {
      logger.info("B key pressed - activating Ballistic Shield");
      ballisticShield.activateBallisticShield();
      return;
    }

    logger.info("B key pressed - activating normal Shield");

    entity.getEvents().trigger("activateShield");
  }

  private void grantTestBow(int tier) {
    InventoryComponent inventory = entity.getComponent(InventoryComponent.class);
    if (inventory == null) {
      logger.warn("Cannot grant a tier {} test bow: player has no inventory", tier);
      return;
    }

    WeaponItem bow = new WeaponGenerator().generateWeapon(WeaponType.BOW, tier);

    if (inventory.addItem(bow) > 0) {
      logger.warn("Cannot grant a tier {} test bow: inventory is full", tier);
      return;
    }

    for (var entry : inventory.getInventorySlots().entrySet()) {
      if (entry.getValue() instanceof WeaponItem inventoryWeapon
          && inventoryWeapon.getWeaponType() == WeaponType.BOW
          && inventoryWeapon.getTier() == tier) {
        inventory.setActiveSlot(entry.getKey());
        logger.info("Granted and equipped tier {} test bow in slot {}", tier, entry.getKey());
        return;
      }
    }

    throw new IllegalStateException("Granted tier {} bow was not found in inventory.");
  }

  /**
   * Selects an inventory slot and attempts to use the item in that slot.
   *
   * @param slot inventory slot to select
   */
  private void handleInventorySlot(int slot) {
    InventoryComponent inventory = entity.getComponent(InventoryComponent.class);

    if (inventory == null) {
      return;
    }

    inventory.setActiveSlot(slot);

    entity.getEvents().trigger("useItem", slot);
  }

  /**
   * Triggers player events on specific keycodes.
   *
   * @return whether the input was processed
   * @see InputProcessor#keyUp(int)
   */
  @Override
  public boolean keyUp(int keycode) {
    LadderComponent ladder = entity.getComponent(LadderComponent.class);
    if (ladder != null && ladder.isAutoClimbing() && keycode != Keys.CONTROL_LEFT) {
      return true;
    }
    switch (keycode) {
      case Keys.W:
        stopClimbing();
        if (walkDirection.isZero()) {
          entity.getEvents().trigger("idle", direction);
        }
        triggerWalkEvent();
        return true;

      case Keys.A:
        if (walkingLeft) {
          walkDirection.sub(Vector2Utils.LEFT);
          walkingLeft = false;
          if (walkDirection.isZero()) {
            entity.getEvents().trigger("idle", direction);
          }
          triggerWalkEvent();
        }
        return true;

      case Keys.S:
        stopClimbing();
        // Climbing does not add DOWN to walkDirection, so only remove it after ordinary walking.
        if (walkingDown) {
          walkDirection.sub(Vector2Utils.DOWN);
          walkingDown = false;
          triggerWalkEvent();
        }
        return true;

      case Keys.D:
        if (walkingRight) {
          walkDirection.sub(Vector2Utils.RIGHT);
          walkingRight = false;
          if (walkDirection.isZero()) {
            entity.getEvents().trigger("idle", direction);
          }
          triggerWalkEvent();
        }
        return true;

      case Keys.CONTROL_LEFT:
        entity.getEvents().trigger("ctrlChanged", false);
        entity.getEvents().trigger("idle", direction);
        crouch = false;
        return true;

      case Keys.SHIFT_LEFT:
        return true;

      default:
        return false;
    }
  }

  private void stopClimbing() {
    LadderComponent ladder = entity.getComponent(LadderComponent.class);
    if (ladder != null) {
      ladder.stopClimbing();
    }
  }

  private void walking(char key) {
    if (key == 'd') {
      direction = "Right";
      if (!walkingRight) {
        walkDirection.add(Vector2Utils.RIGHT);
        walkingRight = true;
      }
    } else if (key == 'a') {
      direction = "Left";
      if (!walkingLeft) {
        walkDirection.add(Vector2Utils.LEFT);
        walkingLeft = true;
      }
    }

    if (crouch) {
      entity.getEvents().trigger("crouchidle", direction);
    } else {
      entity.getEvents().trigger("run", direction);
    }

    triggerWalkEvent();
  }

  private void dashing() {
    if (direction.equals("Left")) {
      dashDirection.add(Vector2Utils.LEFT);
    } else {
      dashDirection.add(Vector2Utils.RIGHT);
    }

    triggerDashEvent();
    entity.getEvents().trigger("rolling", direction);
    dashed = true;
  }

  private void triggerWalkEvent() {
    if (walkDirection.epsilonEquals(Vector2.Zero)) {
      entity.getEvents().trigger("walkStop");
    } else {
      entity.getEvents().trigger("walk", walkDirection);
    }
  }

  private void triggerJumpEvent() {
    entity.getEvents().trigger("jump", jumpDirection);
    jumpDirection.y = 0;
    jumped = false;
  }

  private void triggerDashEvent() {
    entity.getEvents().trigger("dash", dashDirection);
    dashDirection.x = 0;
    dashed = false;
  }
}
