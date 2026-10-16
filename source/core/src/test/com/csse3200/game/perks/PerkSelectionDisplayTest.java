package com.csse3200.game.perks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tests the display's selection / navigation / toggle logic without building the real Scene2D
 * widgets: the private UI fields (perks, cardFrames, continueButton, rootTable) are injected by
 * reflection, so no render service or real skin textures are needed.
 *
 * <p>NOTE: if your UIComponent declares {@code skin} as a static final field, the skin injection
 * below is skipped and the real skin is used instead (it must contain a "white" drawable).
 */
@ExtendWith(GameExtension.class)
class PerkSelectionDisplayTest {
  private PerkSelectionDisplay display;
  private AtomicInteger continueCalls;
  private Perk unlocked;
  private Perk locked;
  private Perk other;
  private Table[] frames;
  private TextButton continueButton;

  @BeforeEach
  @SuppressWarnings("unchecked")
  void setUp() throws Exception {
    // PerkService is all-static: wipe its registry between tests
    Field registry = PerkService.class.getDeclaredField("perksById");
    registry.setAccessible(true);
    ((Map<String, Perk>) registry.get(null)).clear();

    unlocked = new Perk("a", "Alpha", "", "evA", 1);
    locked = new Perk("b", "Beta", "", "evB", 5);
    other = new Perk("c", "Gamma", "", "evC", 1);
    PerkService.register(unlocked);
    PerkService.register(locked);
    PerkService.register(other);
    // register() reloads saved state from Preferences, so set the state we want afterwards
    unlocked.restoreState(1, true, false);
    locked.restoreState(0, false, false);
    other.restoreState(1, true, false);

    continueCalls = new AtomicInteger();
    display = new PerkSelectionDisplay(continueCalls::incrementAndGet);
    new Entity().addComponent(display); // gives the display an entity for event handling

    List<Perk> perks = PerkService.getAllPerks();
    frames = new Table[perks.size()];
    for (int i = 0; i < frames.length; i++) {
      Table card = new Table();
      frames[i] = new Table();
      frames[i].add(card);
    }
    continueButton = mock(TextButton.class);

    set("perks", perks);
    set("cardFrames", frames);
    set("continueButton", continueButton);
    Table root = new Table();
    root.setVisible(false); // create() hides the root until show() is called
    set("rootTable", root);
    injectSkinIfPossible();

    Method register = PerkSelectionDisplay.class.getDeclaredMethod("registerEventListeners");
    register.setAccessible(true);
    register.invoke(display);
  }

  private void set(String name, Object value) throws Exception {
    Field f = PerkSelectionDisplay.class.getDeclaredField(name);
    f.setAccessible(true);
    f.set(display, value);
  }

  private int selectedIndex() throws Exception {
    Field f = PerkSelectionDisplay.class.getDeclaredField("selectedIndex");
    f.setAccessible(true);
    return f.getInt(display);
  }

  private void injectSkinIfPossible() throws Exception {
    Class<?> c = display.getClass();
    while (c != null) {
      try {
        Field f = c.getDeclaredField("skin");
        if (!Modifier.isStatic(f.getModifiers())) {
          f.setAccessible(true);
          f.set(display, mock(Skin.class));
        }
        return;
      } catch (NoSuchFieldException e) {
        c = c.getSuperclass();
      }
    }
  }

  private void fire(String event) {
    display.getEntity().getEvents().trigger(event);
  }

  @Test
  void isVisibleFalseBeforeShown() {
    assertFalse(display.isVisible());
  }

  @Test
  void isVisibleFalseBeforeCreateRuns() {
    PerkSelectionDisplay fresh = new PerkSelectionDisplay(null);
    assertFalse(fresh.isVisible());
  }

  @Test
  void showBeforeCreateIsSafeNoOp() {
    new PerkSelectionDisplay(null).show();
  }

  @Test
  void showMakesDisplayVisibleAndResetsSelection() throws Exception {
    fire("perkSelectNavigateRight");
    display.show();

    assertTrue(display.isVisible());
    assertEquals(0, selectedIndex());
  }

  @Test
  void navigateRightWrapsAround() throws Exception {
    display.navigateRight();
    assertEquals(1, selectedIndex());
    display.navigateRight();
    display.navigateRight();
    assertEquals(0, selectedIndex());
  }

  @Test
  void navigateLeftWrapsAround() throws Exception {
    display.navigateLeft();
    assertEquals(2, selectedIndex());
    display.navigateLeft();
    assertEquals(1, selectedIndex());
  }

  @Test
  void navigateDownMovesFocusToContinueButton() throws Exception {
    display.navigateToContinue();
    assertEquals(3, selectedIndex());
  }

  @Test
  void leftRightIgnoredWhileContinueIsFocused() throws Exception {
    display.navigateToContinue();
    display.navigateLeft();
    display.navigateRight();
    assertEquals(3, selectedIndex());
  }

  @Test
  void navigateUpReturnsFromContinueToFirstCard() throws Exception {
    display.navigateToContinue();
    display.navigateToCards();
    assertEquals(0, selectedIndex());
  }

  @Test
  void navigateUpIsNoOpWhenAlreadyOnACard() throws Exception {
    display.navigateRight();
    display.navigateToCards();
    assertEquals(1, selectedIndex());
  }

  @Test
  void navigationEventsAreWiredToHandlers() throws Exception {
    fire("perkSelectNavigateRight");
    assertEquals(1, selectedIndex());
    fire("perkSelectNavigateLeft");
    assertEquals(0, selectedIndex());
    fire("perkSelectNavigateDown");
    assertEquals(3, selectedIndex());
    fire("perkSelectNavigateUp");
    assertEquals(0, selectedIndex());
  }

  @Test
  void confirmActivatesUnlockedInactivePerk() {
    fire("perkSelectConfirm"); // index 0 = unlocked
    assertTrue(unlocked.isActive());
  }

  @Test
  void confirmDeactivatesActivePerk() {
    fire("perkSelectConfirm");
    fire("perkSelectConfirm");
    assertFalse(unlocked.isActive());
  }

  @Test
  void confirmOnLockedPerkDoesNothing() {
    fire("perkSelectNavigateRight"); // index 1 = locked
    fire("perkSelectConfirm");
    assertFalse(locked.isActive());
  }

  @Test
  void confirmRespectsMaxActivePerks() {
    locked.restoreState(5, true, false); // card 1 is now unlocked too
    fire("perkSelectConfirm"); // card 0 -> active
    fire("perkSelectNavigateRight");
    fire("perkSelectNavigateRight");
    fire("perkSelectConfirm"); // card 2 -> active (2 of 2)
    fire("perkSelectNavigateRight"); // wraps to card 0
    fire("perkSelectNavigateRight"); // card 1
    fire("perkSelectConfirm"); // no room

    assertTrue(unlocked.isActive());
    assertTrue(other.isActive());
    assertFalse(locked.isActive());
    assertEquals(PerkService.MAX_ACTIVE_PERKS, PerkService.getActiveCount());
  }

  @Test
  void confirmOnContinueRunsCallbackAndHidesDisplay() {
    display.show();
    fire("perkSelectNavigateDown");
    fire("perkSelectConfirm");

    assertEquals(1, continueCalls.get());
    assertFalse(display.isVisible());
  }

  @Test
  void closeWithNullCallbackDoesNotThrow() throws Exception {
    PerkSelectionDisplay noCallback = new PerkSelectionDisplay(null);
    new Entity().addComponent(noCallback);
    Field rootTable = PerkSelectionDisplay.class.getDeclaredField("rootTable");
    rootTable.setAccessible(true);
    rootTable.set(noCallback, new Table());
    Field perks = PerkSelectionDisplay.class.getDeclaredField("perks");
    perks.setAccessible(true);
    perks.set(noCallback, new ArrayList<Perk>());

    // With zero perks, index 0 == perks.size() == the Continue button
    Method onConfirm = PerkSelectionDisplay.class.getDeclaredMethod("onConfirm");
    onConfirm.setAccessible(true);
    onConfirm.invoke(noCallback);
  }

  @Test
  void focusedCardAndContinueButtonColorFollowSelection() {
    display.navigateToContinue();
    org.mockito.Mockito.verify(continueButton).setColor(Color.CYAN);

    display.navigateToCards();
    org.mockito.Mockito.verify(continueButton).setColor(Color.WHITE);
  }
}
