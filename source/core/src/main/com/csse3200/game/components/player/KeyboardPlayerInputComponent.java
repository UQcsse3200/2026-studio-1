package com.csse3200.game.components.player;

import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.input.InputComponent;
import com.csse3200.game.pausemenu.*;
import com.csse3200.game.utils.math.Vector2Utils;

/**
 * Input handler for the player for keyboard and touch (mouse) input. This input handler only uses
 * keyboard input.
 *
 * <p>Every action's key is looked up from {@link KeybindSettings} instead of being hardcoded, so
 * the player can rebind controls at runtime from the pause menu.
 */
public class KeyboardPlayerInputComponent extends InputComponent {
  private final Vector2 walkDirection = Vector2.Zero.cpy();
  private final Vector2 jumpDirection = Vector2.Zero.cpy();
  private final Vector2 dashDirection = Vector2.Zero.cpy();

  public KeyboardPlayerInputComponent() {
    super(5);
  }

  private boolean jumped = false;
  private boolean dashed = false;
  private boolean crouch = false;
  private String direction = "Right";
  private String SlideString = "slide";

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
    if (PauseMenuComponent.isGamePaused()) {
      return false;
    }
    entity.getEvents().trigger("idle", direction);
    SubLevelTravelComponent travel = entity.getComponent(SubLevelTravelComponent.class);
    if (travel != null && travel.isControlLocked()) {
      return true;
    }

    if (keycode == KeybindSettings.getKey("interact")) {
      return travel != null && travel.beginTravel();
    }
    if (keycode == KeybindSettings.getKey("jump")) {
      LadderComponent ladderUp = entity.getComponent(LadderComponent.class);
      if (ladderUp != null && ladderUp.beginClimb(1f)) {
        return true;
      }
      jumpDirection.add(Vector2Utils.UP); // Adds to the y vector
      triggerJumpEvent();
      entity.getEvents().trigger("jumping", direction);
      jumped = true;
      return true;
    }
    if (keycode == KeybindSettings.getKey("dash")) {
      dashing(); // makes player dash
      return true;
    }
    if (keycode == KeybindSettings.getKey("moveLeft")) {
      walking('a'); // makes player walk left
      return true;
    }
    if (keycode == KeybindSettings.getKey("moveDown")) {
      LadderComponent ladderDown = entity.getComponent(LadderComponent.class);
      if (ladderDown != null && ladderDown.beginClimb(-1f)) {
        return true;
      }
      walkDirection.add(Vector2Utils.DOWN);
      triggerWalkEvent();
      return true;
    }
    if (keycode == KeybindSettings.getKey("moveRight")) {
      walking('d'); // makes player walk right
      return true;
    }
    if (keycode == KeybindSettings.getKey("attack")) {
      entity.getEvents().trigger("attack");
      entity.getEvents().trigger("attacking", direction);
      return true;
    }
    if (keycode == KeybindSettings.getKey("dropItem")) {
      entity.getEvents().trigger("dropItem");
      return true;
    }
    if (keycode == KeybindSettings.getKey("equipShield")) {
      entity.getEvents().trigger("activateShield");
      return true;
    }
    if (keycode == KeybindSettings.getKey("toggleQuestMenu")) {
      entity.getEvents().trigger("toggleQuestMenu");
      return true;
    }
    if (keycode == KeybindSettings.getKey("crouch")) {
      entity.getEvents().trigger("ctrlChanged", true);
      entity.getEvents().trigger("crouchidle", direction);
      crouch = true;
      return true;
    }
    if (keycode == KeybindSettings.getKey(SlideString)) {
      entity.getEvents().trigger(SlideString, true);
      return true;
    }
    if (keycode == KeybindSettings.getKey("hotbarSlot1")) {
      handleInventorySlot(1);
      return true;
    }
    if (keycode == KeybindSettings.getKey("hotbarSlot2")) {
      handleInventorySlot(2);
      return true;
    }
    if (keycode == KeybindSettings.getKey("hotbarSlot3")) {
      handleInventorySlot(3);
      return true;
    }
    if (keycode == KeybindSettings.getKey("hotbarSlot4")) {
      handleInventorySlot(4);
      return true;
    }
    if (keycode == KeybindSettings.getKey("hotbarSlot5")) {
      handleInventorySlot(5);
      return true;
    }

    return false;
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
    if (PauseMenuComponent.isGamePaused()) {
      stopClimbing();
      return false;
    }

    if (keycode == KeybindSettings.getKey("jump")) {
      stopClimbing();
      return true;
    }
    if (keycode == KeybindSettings.getKey("moveLeft")) {
      walkDirection.sub(Vector2Utils.LEFT);
      if (walkDirection.isZero()) {
        entity.getEvents().trigger("idle", direction);
      }
      triggerWalkEvent();
      return true;
    }
    if (keycode == KeybindSettings.getKey("moveDown")) {
      stopClimbing();
      walkDirection.sub(Vector2Utils.DOWN);
      triggerWalkEvent();
      return true;
    }
    if (keycode == KeybindSettings.getKey("moveRight")) {
      walkDirection.sub(Vector2Utils.RIGHT);
      if (walkDirection.isZero()) {
        entity.getEvents().trigger("idle", direction);
      }
      triggerWalkEvent();
      return true;
    }
    if (keycode == KeybindSettings.getKey("crouch")) {
      entity.getEvents().trigger("ctrlChanged", false);
      entity.getEvents().trigger("idle", direction);
      crouch = false;
      return true;
    }
    if (keycode == KeybindSettings.getKey(SlideString)) {
      return true;
    }

    return false;
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
      walkDirection.add(Vector2Utils.RIGHT);
    } else if (key == 'a') {
      direction = "Left";
      walkDirection.add(Vector2Utils.LEFT);
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
      dashDirection.add(Vector2Utils.LEFT); // Adds to the x vector to the left
    } else {
      dashDirection.add(Vector2Utils.RIGHT); // Adds to the x vector to the right
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
    // Player has upwards y velocity
    entity.getEvents().trigger("jump", jumpDirection);
    jumpDirection.y = 0;
    jumped = false;
  }

  private void triggerDashEvent() {
    // Player has an x velocity in the direction they last went or are going
    entity.getEvents().trigger("dash", dashDirection);
    dashDirection.x = 0;
    dashed = false;
  }
}
