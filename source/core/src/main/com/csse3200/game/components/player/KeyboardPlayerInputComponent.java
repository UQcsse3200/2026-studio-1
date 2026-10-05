package com.csse3200.game.components.player;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.components.loot.WeaponGenerator;
import com.csse3200.game.components.loot.WeaponItem;
import com.csse3200.game.components.loot.WeaponType;
import com.csse3200.game.input.InputComponent;
import com.csse3200.game.pausemenu.KeybindSettings;
import com.csse3200.game.pausemenu.PauseMenuComponent;
import com.csse3200.game.utils.math.Vector2Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * \* Input handler for the player for keyboard and touch (mouse) input. This input handler only
 * uses \* keyboard input. \* \*
 *
 * <p>Every action's key is looked up from {@link KeybindSettings} instead of being hardcoded, so \*
 * the player can rebind controls at runtime from the pause menu. The one exception is the developer
 * \* shortcuts in {@link #handleDebugKeys}, which are fixed keys.
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
  private boolean wasPaused = false;
  private static final String SLIDE_STRING = "slide";
  private static final String WALK_STOP = "walkStop";
  private static final String MOVE_LEFT = "moveLeft";
  private static final String MOVE_RIGHT = "moveRight";
  private static final String MOVE_DOWN = "moveDown";
  private static final String CROUCH_STRING = "crouch";
  private static final String CTRL_CHANGED = "ctrlChanged";

  public String getDirection() {
    return this.direction;
  }

  /**
   * Per-frame upkeep: stops walking if the game was just paused, and lets go of any held direction
   * or crouch whose key was released while a menu was consuming the input (see {@link
   * #releaseWalkDirectionsNoLongerHeld()}).
   */
  @Override
  public void update() {
    boolean paused = PauseMenuComponent.isGamePaused().get();

    if (paused && !wasPaused) {
      stopMovement();
    }

    wasPaused = paused;

    if (!paused) {
      releaseWalkDirectionsNoLongerHeld();
      releaseCrouchIfNoLongerHeld();
    }
  }

  /**
   * Lets go of any held walk direction whose key is no longer down.
   *
   * <p>A key release only reaches this component if no higher-priority handler consumes it first,
   * and while the game is paused the pause menu consumes them: it takes every Left/Right release
   * (to drive its sliders) and {@code KeyboardPauseInput} takes the rest. So hold Left, pause,
   * release Left, unpause would leave {@code walkingLeft} set with no release ever coming, and the
   * player would keep walking on its own. Losing window focus mid-press does the same. Checking the
   * real keyboard each frame repairs it however the release got lost.
   */
  private void releaseWalkDirectionsNoLongerHeld() {
    boolean released = false;
    if (walkingLeft && !isKeyHeld(MOVE_LEFT)) {
      walkDirection.sub(Vector2Utils.LEFT);
      walkingLeft = false;
      released = true;
    }
    if (walkingRight && !isKeyHeld(MOVE_RIGHT)) {
      walkDirection.sub(Vector2Utils.RIGHT);
      walkingRight = false;
      released = true;
    }
    if (walkingDown && !isKeyHeld(MOVE_DOWN)) {
      walkDirection.sub(Vector2Utils.DOWN);
      walkingDown = false;
      released = true;
    }
    if (released) {
      if (walkDirection.isZero()) {
        entity.getEvents().trigger("idle", direction);
      }
      triggerWalkEvent();
    }
  }

  /**
   * Same repair as {@link #releaseWalkDirectionsNoLongerHeld()}, for a crouch whose key-up was
   * lost.
   */
  private void releaseCrouchIfNoLongerHeld() {
    if (crouch && !isKeyHeld(CROUCH_STRING)) {
      entity.getEvents().trigger(CTRL_CHANGED, false);
      entity.getEvents().trigger("idle", direction);
      crouch = false;
    }
  }

  /**
   * @param action the action name, as used with {@link KeybindSettings#getKey(String)}
   * @return whether the key currently bound to that action is physically held down
   */
  boolean isKeyHeld(String action) {
    int key = KeybindSettings.getKey(action);
    return key >= 0 && Gdx.input != null && Gdx.input.isKeyPressed(key);
  }

  /**
   * Triggers player events on specific keycodes.
   *
   * @return whether the input was processed
   * @see InputProcessor#keyDown(int)
   */
  @Override
  public boolean keyDown(int keycode) {
    if (PauseMenuComponent.isGamePaused().get()) {
      stopMovement();
      wasPaused = true;
      return false;
    }

    wasPaused = false;
    entity.getEvents().trigger("idle", direction);
    SubLevelTravelComponent travel = entity.getComponent(SubLevelTravelComponent.class);
    if (travel != null && travel.isControlLocked()) {
      return true;
    }
    LadderComponent ladder = entity.getComponent(LadderComponent.class);
    if (ladder != null && ladder.isAutoClimbing()) {
      return true;
    }
    if (keycode == KeybindSettings.getKey("interact")) {
      if (travel != null && travel.beginTravel()) {
        return true;
      }
      if (ladder != null && ladder.beginAutoClimb()) {
        walkDirection.setZero();
        walkingLeft = false;
        walkingRight = false;
        walkingDown = false;
        entity.getEvents().trigger(WALK_STOP);
        return true;
      }
      return false;
    }
    return handleJumpKey(keycode)
        || handleMovementKeys(keycode)
        || handleCombatAndItemKeys(keycode)
        || handleHotbarKeys(keycode)
        || handleDebugKeys(keycode);
  }

  private void stopMovement() {
    walkDirection.setZero();
    walkingLeft = false;
    walkingRight = false;
    walkingDown = false;
    entity.getEvents().trigger(WALK_STOP);
  }

  /** Handles the jump key, starting a ladder climb instead when one is available. */
  private boolean handleJumpKey(int keycode) {
    if (keycode != KeybindSettings.getKey("jump")) {
      return false;
    }
    LadderComponent ladderUp = entity.getComponent(LadderComponent.class);
    if (ladderUp != null && ladderUp.beginClimb(1f)) {
      return true;
    }
    stopClimbing();
    jumpDirection.add(Vector2Utils.UP); // Adds to the y vector
    triggerJumpEvent();
    entity.getEvents().trigger("jumping", direction);
    jumped = true;
    return true;
  }

  /** Handles dash and the walk / ladder-down directional keys. */
  private boolean handleMovementKeys(int keycode) {
    if (keycode == KeybindSettings.getKey("dash")) {
      dashing(); // makes player dash
      return true;
    }
    if (keycode == KeybindSettings.getKey(MOVE_LEFT)) {
      stopClimbing();
      walking('a'); // makes player walk left
      return true;
    }
    if (keycode == KeybindSettings.getKey(MOVE_DOWN)) {
      LadderComponent ladderDown = entity.getComponent(LadderComponent.class);
      if (ladderDown != null && ladderDown.beginClimb(-1f)) {
        return true;
      }
      stopClimbing();
      if (!walkingDown) {
        walkDirection.add(Vector2Utils.DOWN);
        walkingDown = true;
        triggerWalkEvent();
      }
      return true;
    }
    if (keycode == KeybindSettings.getKey(MOVE_RIGHT)) {
      stopClimbing();
      walking('d'); // makes player walk right
      return true;
    }
    return false;
  }

  /** Handles attacks, item/shield use, the quest and tutorial toggles, crouch and slide. */
  private boolean handleCombatAndItemKeys(int keycode) {
    if (keycode == KeybindSettings.getKey("attack")) {
      entity.getEvents().trigger("attack");
      entity.getEvents().trigger("attacking", direction);
      return true;
    }
    if (keycode == KeybindSettings.getKey("specialAttack")) {
      entity.getEvents().trigger("specialAttack");
      return true;
    }
    if (keycode == KeybindSettings.getKey("areaAttack")) {
      entity.getEvents().trigger("areaAttack");
      return true;
    }
    if (keycode == KeybindSettings.getKey("timeFreeze")) {
      entity.getEvents().trigger("activateTimeFreeze");
      return true;
    }
    if (keycode == KeybindSettings.getKey("dropItem")) {
      entity.getEvents().trigger("dropItem");
      return true;
    }
    if (keycode == KeybindSettings.getKey("equipShield")) {
      // Ballistic shield first if one is held, otherwise the normal shield (see the method doc).
      activateAvailableShield();
      return true;
    }
    if (keycode == KeybindSettings.getKey("bribe")) {
      tryBribe();
      return true;
    }
    if (keycode == KeybindSettings.getKey("toggleQuestMenu")) {
      entity.getEvents().trigger("toggleQuestMenu");
      return true;
    }
    if (keycode == KeybindSettings.getKey("toggleTutorial")) {
      entity.getEvents().trigger("toggleTutorial");
      return true;
    }
    if (keycode == KeybindSettings.getKey(CROUCH_STRING)) {
      entity.getEvents().trigger(CTRL_CHANGED, true);
      entity.getEvents().trigger("crouchidle", direction);
      crouch = true;
      return true;
    }
    if (keycode == KeybindSettings.getKey(SLIDE_STRING)) {
      entity.getEvents().trigger(SLIDE_STRING, true);
      return true;
    }
    return false;
  }

  /** Handles the five hotbar slot keys. */
  private boolean handleHotbarKeys(int keycode) {
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
   * \* Developer shortcuts that grant a test bow. Deliberately fixed keys, not rebindable (no row
   * in \* the pause menu's Keybinds panel), and checked after every rebindable action so a player's
   * own \* binding always wins over them.
   */
  private boolean handleDebugKeys(int keycode) {
    if (keycode == Keys.F6) {
      grantTestBow(2);
      return true;
    }
    if (keycode == Keys.F7) {
      grantTestBow(3);
      return true;
    }
    return false;
  }

  /**
   * \* Activates the Ballistic Shield if the player is holding one. \* \*
   *
   * <p>If the player does not have a Ballistic Shield, the normal Tier 1 Shield is activated \*
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
   * \* Selects an inventory slot and attempts to use the item in that slot. \* \* @param slot
   * inventory slot to select
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
   * \* Triggers player events on specific keycodes. \* \* @return whether the input was processed
   * \* @see InputProcessor#keyUp(int)
   */
  @Override
  public boolean keyUp(int keycode) {
    if (PauseMenuComponent.isGamePaused().get()) {
      stopMovement();
      return false;
    }
    LadderComponent ladder = entity.getComponent(LadderComponent.class);
    if (ladder != null
        && ladder.isAutoClimbing()
        && keycode != KeybindSettings.getKey(CROUCH_STRING)) {
      return true;
    }
    if (keycode == KeybindSettings.getKey("jump")) {
      stopClimbing();
      return true;
    }
    if (keycode == KeybindSettings.getKey(MOVE_LEFT)) {
      if (walkingLeft) {
        walkDirection.sub(Vector2Utils.LEFT);
        walkingLeft = false;
      }
      if (walkDirection.isZero()) {
        entity.getEvents().trigger("idle", direction);
      }
      triggerWalkEvent();
      return true;
    }
    if (keycode == KeybindSettings.getKey(MOVE_DOWN)) {
      stopClimbing();
      if (walkingDown) {
        walkDirection.sub(Vector2Utils.DOWN);
        walkingDown = false;
      }
      triggerWalkEvent();
      return true;
    }
    if (keycode == KeybindSettings.getKey(MOVE_RIGHT)) {
      if (walkingRight) {
        walkDirection.sub(Vector2Utils.RIGHT);
        walkingRight = false;
      }
      if (walkDirection.isZero()) {
        entity.getEvents().trigger("idle", direction);
      }
      triggerWalkEvent();
      return true;
    }
    if (keycode == KeybindSettings.getKey(CROUCH_STRING)) {
      entity.getEvents().trigger(CTRL_CHANGED, false);
      entity.getEvents().trigger("idle", direction);
      crouch = false;
      return true;
    }
    if (keycode == KeybindSettings.getKey(SLIDE_STRING)) {
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
      entity.getEvents().trigger(WALK_STOP);
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
