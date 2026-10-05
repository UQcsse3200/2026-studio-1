package com.csse3200.game.pausemenu;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Preferences;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Central store for every rebindable action's key. Any input component reads the current key for an
 * action here instead of hardcoding it, so the pause menu's Keybinds panel can change it at
 * runtime.
 *
 * <p>Binding the same key to two actions is not allowed: setKey(...) automatically unbinds
 * whichever other action previously held that key (see UNBOUND).
 */
public final class KeybindSettings {
  private static final String PREFS_NAME = "keybind_settings";
  private static final String PREFS_KEY_PREFIX = "key.";

  /** Sentinel used for an action with no key currently bound. */
  public static final int UNBOUND = -1;

  private static final Map<String, Integer> defaultKeys = new LinkedHashMap<>();
  private static final Map<String, Integer> currentKeys = new LinkedHashMap<>();
  private static final Preferences prefs =
      (Gdx.app != null) ? Gdx.app.getPreferences(PREFS_NAME) : null;

  static {
    defaultKeys.put("moveLeft", Input.Keys.A);
    defaultKeys.put("moveRight", Input.Keys.D);
    defaultKeys.put("moveDown", Input.Keys.S);
    defaultKeys.put("jump", Input.Keys.W);
    defaultKeys.put("dash", Input.Keys.L);
    defaultKeys.put("slide", Input.Keys.SHIFT_LEFT);
    defaultKeys.put("attack", Input.Keys.SPACE);
    defaultKeys.put("bribe", Input.Keys.R);
    defaultKeys.put("dropItem", Input.Keys.Q);
    defaultKeys.put("equipShield", Input.Keys.B);
    defaultKeys.put("interact", Input.Keys.E);
    defaultKeys.put("toggleQuestMenu", Input.Keys.J);
    defaultKeys.put("crouch", Input.Keys.CONTROL_LEFT);
    defaultKeys.put("TimeFreeze", Input.Keys.R);
    defaultKeys.put("pause", Input.Keys.ESCAPE);
    defaultKeys.put("hotbarSlot1", Input.Keys.NUM_1);
    defaultKeys.put("hotbarSlot2", Input.Keys.NUM_2);
    defaultKeys.put("hotbarSlot3", Input.Keys.NUM_3);
    defaultKeys.put("hotbarSlot4", Input.Keys.NUM_4);
    defaultKeys.put("hotbarSlot5", Input.Keys.NUM_5);

    for (Map.Entry<String, Integer> entry : defaultKeys.entrySet()) {
      currentKeys.put(entry.getKey(), loadKey(entry.getKey(), entry.getValue()));
    }
  }

  private KeybindSettings() {}

  /**
   * @param action the action name, e.g. "jump"
   * @return the keycode currently bound to it, or UNBOUND if the action doesn't exist or has no key
   *     bound
   */
  public static int getKey(String action) {
    Integer keycode = currentKeys.get(action);
    return keycode != null ? keycode : UNBOUND;
  }

  /*
   Binds action to keycode. If any other action currently holds keycode, that action is
   unbound first (set to UNBOUND), so no two actions can ever share the same key.
  */
  public static void setKey(String action, int keycode) {
    if (!currentKeys.containsKey(action)) {
      return;
    }

    for (Map.Entry<String, Integer> entry : currentKeys.entrySet()) {
      String otherAction = entry.getKey();
      Integer otherKeycode = entry.getValue();
      if (!otherAction.equals(action) && otherKeycode != null && otherKeycode == keycode) {
        currentKeys.put(otherAction, UNBOUND);
        saveKey(otherAction, UNBOUND);
      }
    }

    currentKeys.put(action, keycode);
    saveKey(action, keycode);
  }

  // Resets every action back to its default key.
  public static void resetToDefaults() {
    for (Map.Entry<String, Integer> entry : defaultKeys.entrySet()) {
      currentKeys.put(entry.getKey(), entry.getValue());
      saveKey(entry.getKey(), entry.getValue());
    }
  }

  private static int loadKey(String action, int defaultValue) {
    if (prefs == null) {
      return defaultValue;
    }
    return prefs.getInteger(PREFS_KEY_PREFIX + action, defaultValue);
  }

  private static void saveKey(String action, int keycode) {
    if (prefs == null) {
      return;
    }
    prefs.putInteger(PREFS_KEY_PREFIX + action, keycode);
    prefs.flush();
  }
}
