package com.csse3200.game.files;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.csse3200.game.perks.Perk;
import com.csse3200.game.perks.PerkService;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link LoadService#restorePerks(List)}, which puts every perk back from the save file
 * when a game is loaded.
 *
 * <p>PerkService is static, so the real perks are set aside before each test and put back after it,
 * and only test perks are registered while the tests run.
 */
class LoadServicePerkRestoreTest {
  private Map<String, Perk> registeredPerks;
  private Map<String, Perk> originalPerks;

  private Perk dash;
  private Perk shield;
  private Perk jump;

  @BeforeEach
  @SuppressWarnings("unchecked")
  void setUp() throws Exception {
    Field field = PerkService.class.getDeclaredField("perksById");
    field.setAccessible(true);
    registeredPerks = (Map<String, Perk>) field.get(null);
    originalPerks = new LinkedHashMap<>(registeredPerks);
    registeredPerks.clear();

    dash = new Perk("test_restore_dash", "Dash", "d", "dashUsed", 5);
    shield = new Perk("test_restore_shield", "Shield", "s", "shieldUsed", 2);
    jump = new Perk("test_restore_jump", "Jump", "j", "jumpUsed", 3);
    PerkService.register(dash);
    PerkService.register(shield);
    PerkService.register(jump);
    PerkService.resetAll();
  }

  @AfterEach
  void tearDown() {
    PerkService.resetAll();
    registeredPerks.clear();
    registeredPerks.putAll(originalPerks);
  }

  private static SavedPerk saved(String id, int progress, boolean unlocked, boolean active) {
    SavedPerk perk = new SavedPerk();
    perk.id = id;
    perk.progress = progress;
    perk.unlocked = unlocked;
    perk.active = active;
    return perk;
  }

  @Test
  void restoresProgressUnlockedAndActive() {
    LoadService.restorePerks(
        List.of(saved(dash.getId(), 5, true, true), saved(shield.getId(), 1, false, false)));

    assertEquals(5, dash.getProgress());
    assertTrue(dash.isUnlocked());
    assertTrue(dash.isActive());

    assertEquals(1, shield.getProgress());
    assertFalse(shield.isUnlocked());
    assertFalse(shield.isActive());
  }

  @Test
  void perksMissingFromTheSaveAreReset() {
    PerkService.restorePerk(jump.getId(), 3, true, true);

    LoadService.restorePerks(List.of(saved(dash.getId(), 2, false, false)));

    assertEquals(0, jump.getProgress());
    assertFalse(jump.isUnlocked());
    assertFalse(jump.isActive());
  }

  @Test
  void nullListFromAnOlderSaveLeavesPerksUnchanged() {
    PerkService.restorePerk(jump.getId(), 3, true, true);

    LoadService.restorePerks(null);

    assertTrue(jump.isUnlocked());
    assertTrue(jump.isActive());
  }

  @Test
  void emptyListResetsEveryPerk() {
    PerkService.restorePerk(dash.getId(), 5, true, true);

    LoadService.restorePerks(new ArrayList<>());

    assertFalse(dash.isUnlocked());
    assertFalse(dash.isActive());
    assertEquals(0, PerkService.getActiveCount());
  }

  @Test
  void neverRestoresMoreThanTheActivePerkLimit() {
    LoadService.restorePerks(
        List.of(
            saved(dash.getId(), 5, true, true),
            saved(shield.getId(), 2, true, true),
            saved(jump.getId(), 3, true, true)));

    assertEquals(PerkService.MAX_ACTIVE_PERKS, PerkService.getActiveCount());
    assertTrue(jump.isUnlocked(), "a perk over the limit stays unlocked, just not active");
  }

  @Test
  void lockedPerkIsNeverRestoredAsActive() {
    LoadService.restorePerks(List.of(saved(dash.getId(), 1, false, true)));

    assertFalse(dash.isActive());
  }

  @Test
  void progressIsKeptWithinZeroAndTheThreshold() {
    LoadService.restorePerks(
        List.of(saved(dash.getId(), -4, false, false), saved(shield.getId(), 99, true, false)));

    assertEquals(0, dash.getProgress());
    assertEquals(shield.getThreshold(), shield.getProgress());
  }

  @Test
  void unknownIdsAndBadEntriesAreIgnored() {
    List<SavedPerk> savedPerks =
        Arrays.asList(
            null,
            saved(null, 1, true, true),
            saved("no_such_perk", 1, true, true),
            saved(dash.getId(), 5, true, false));

    assertDoesNotThrow(() -> LoadService.restorePerks(savedPerks));
    assertTrue(dash.isUnlocked());
    assertEquals(0, PerkService.getActiveCount());
  }

  @Test
  void restoringAnActivePerkDoesNotRunItsActivateCallback() {
    int[] calls = {0};
    dash.setOnActivated(() -> calls[0]++);

    LoadService.restorePerks(List.of(saved(dash.getId(), 5, true, true)));

    assertTrue(dash.isActive());
    assertEquals(0, calls[0]);
  }
}
