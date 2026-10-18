package com.csse3200.game.perks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * PerkService is all-static, so the registry is cleared by reflection before each test to keep
 * tests independent. Preferences are null when Gdx.app isn't available, so persistence is skipped.
 */
class PerkServiceTest {
  private Perk dash;
  private Perk shield;
  private Perk jump;

  @BeforeEach
  @SuppressWarnings("unchecked")
  void setUp() throws Exception {
    Field f = PerkService.class.getDeclaredField("perksById");
    f.setAccessible(true);
    ((Map<String, Perk>) f.get(null)).clear();

    dash = new Perk("test_dash", "Dash", "d", "dashUsed", 5);
    shield = new Perk("test_shield", "Shield", "s", "shieldUsed", 2);
    jump = new Perk("test_jump", "Jump", "j", "dashUsed", 3); // shares an event key with dash
    PerkService.register(dash);
    PerkService.register(shield);
    PerkService.register(jump);

    // register() reloads any saved state from Preferences, so force a clean slate in memory
    for (Perk perk : List.of(dash, shield, jump)) {
      perk.restoreState(0, false, false);
    }
  }

  private void unlock(Perk perk) {
    perk.restoreState(perk.getThreshold(), true, false);
  }

  @Test
  void registerMakesPerkRetrievableById() {
    assertSame(dash, PerkService.getPerk("test_dash"));
  }

  @Test
  void getPerkReturnsNullForUnknownId() {
    assertNull(PerkService.getPerk("nope"));
  }

  @Test
  void registerSameIdTwiceDoesNotDuplicate() {
    PerkService.register(dash);
    assertEquals(3, PerkService.getAllPerks().size());
  }

  @Test
  void getAllPerksPreservesRegistrationOrder() {
    List<Perk> all = PerkService.getAllPerks();
    assertEquals(List.of(dash, shield, jump), all);
  }

  @Test
  void getAllPerksReturnsACopy() {
    List<Perk> all = PerkService.getAllPerks();
    all.clear();
    assertEquals(3, PerkService.getAllPerks().size());
    assertNotSame(all, PerkService.getAllPerks());
  }

  @Test
  void recordEventUpdatesEveryPerkListeningForTheKey() {
    PerkService.recordEvent("dashUsed", 2);
    assertEquals(2, dash.getProgress());
    assertEquals(2, jump.getProgress());
    assertEquals(0, shield.getProgress());
  }

  @Test
  void recordEventUnlocksPerkAtThreshold() {
    PerkService.recordEvent("shieldUsed", 2);
    assertTrue(shield.isUnlocked());
  }

  @Test
  void recordEventWithUnknownKeyIsNoOp() {
    PerkService.recordEvent("unknownEvent", 100);
    assertEquals(0, dash.getProgress());
    assertEquals(0, shield.getProgress());
    assertEquals(0, jump.getProgress());
  }

  @Test
  void activateFailsForUnknownId() {
    assertFalse(PerkService.activate("nope"));
  }

  @Test
  void activateFailsForLockedPerk() {
    assertFalse(PerkService.activate("test_dash"));
    assertFalse(dash.isActive());
  }

  @Test
  void activateSucceedsForUnlockedPerk() {
    unlock(dash);
    assertTrue(PerkService.activate("test_dash"));
    assertTrue(dash.isActive());
    assertEquals(1, PerkService.getActiveCount());
  }

  @Test
  void activateFailsIfAlreadyActive() {
    unlock(dash);
    PerkService.activate("test_dash");
    assertFalse(PerkService.activate("test_dash"));
    assertEquals(1, PerkService.getActiveCount());
  }

  @Test
  void activateFailsWhenMaxActivePerksReached() {
    unlock(dash);
    unlock(shield);
    unlock(jump);
    assertTrue(PerkService.activate("test_dash"));
    assertTrue(PerkService.activate("test_shield"));
    assertEquals(PerkService.MAX_ACTIVE_PERKS, PerkService.getActiveCount());

    assertFalse(PerkService.activate("test_jump"));
    assertFalse(jump.isActive());
  }

  @Test
  void activateRunsOnActivatedCallbackOnlyOnSuccess() {
    AtomicInteger calls = new AtomicInteger();
    dash.setOnActivated(calls::incrementAndGet);

    PerkService.activate("test_dash"); // locked -> fails
    assertEquals(0, calls.get());

    unlock(dash);
    PerkService.activate("test_dash");
    assertEquals(1, calls.get());

    PerkService.activate("test_dash"); // already active -> fails
    assertEquals(1, calls.get());
  }

  @Test
  void deactivateMakesPerkInactiveAndRunsCallback() {
    AtomicInteger calls = new AtomicInteger();
    dash.setOnDeactivated(calls::incrementAndGet);
    unlock(dash);
    PerkService.activate("test_dash");

    PerkService.deactivate("test_dash");

    assertFalse(dash.isActive());
    assertEquals(0, PerkService.getActiveCount());
    assertEquals(1, calls.get());
  }

  @Test
  void deactivateFreesASlot() {
    unlock(dash);
    unlock(shield);
    unlock(jump);
    PerkService.activate("test_dash");
    PerkService.activate("test_shield");
    PerkService.deactivate("test_dash");

    assertTrue(PerkService.activate("test_jump"));
  }

  @Test
  void deactivateOnInactiveOrUnknownPerkIsNoOp() {
    AtomicInteger calls = new AtomicInteger();
    dash.setOnDeactivated(calls::incrementAndGet);

    PerkService.deactivate("test_dash");
    PerkService.deactivate("nope");

    assertEquals(0, calls.get());
  }

  @Test
  void getActiveCountIsZeroInitially() {
    assertEquals(0, PerkService.getActiveCount());
  }

  @Test
  void resetAllClearsProgressUnlockedAndActive() {
    unlock(dash);
    PerkService.activate("test_dash");
    PerkService.recordEvent("shieldUsed", 1);

    PerkService.resetAll();

    for (Perk perk : PerkService.getAllPerks()) {
      assertEquals(0, perk.getProgress());
      assertFalse(perk.isUnlocked());
      assertFalse(perk.isActive());
    }
    assertEquals(0, PerkService.getActiveCount());
  }

  @Test
  void resetAllKeepsPerksRegistered() {
    PerkService.resetAll();
    assertEquals(3, PerkService.getAllPerks().size());
  }

  @Test
  void resetPerkOnlyAffectsThatPerk() {
    unlock(dash);
    unlock(shield);
    PerkService.activate("test_dash");

    PerkService.resetPerk("test_dash");

    assertEquals(0, dash.getProgress());
    assertFalse(dash.isUnlocked());
    assertFalse(dash.isActive());
    assertTrue(shield.isUnlocked());
  }

  @Test
  void resetPerkWithUnknownIdIsNoOp() {
    unlock(dash);
    PerkService.resetPerk("nope");
    assertTrue(dash.isUnlocked());
  }
}
