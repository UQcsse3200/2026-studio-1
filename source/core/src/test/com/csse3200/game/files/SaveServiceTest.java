package com.csse3200.game.files;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.Files;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.csse3200.game.extensions.GameExtension;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests for {@link SaveService}. Gdx.files is swapped for a mock that points external files at a
 * temporary folder, so these tests never touch the real save file in the user's home directory.
 */
@ExtendWith(GameExtension.class)
class SaveServiceTest {
  private static final String SAVE_PATH = "AscentOfTheMortal" + File.separator + "save.json";

  @TempDir Path tempDir;

  private Files originalFiles;
  private Files mockFiles;

  @BeforeEach
  void setUp() {
    originalFiles = Gdx.files;
    mockFiles = mock(Files.class);
    when(mockFiles.external(anyString()))
        .thenAnswer(invocation -> fileIn(tempDir, invocation.getArgument(0)));
    Gdx.files = mockFiles;
  }

  @AfterEach
  void tearDown() {
    Gdx.files = originalFiles;
  }

  private static FileHandle fileIn(Path folder, String path) {
    return new FileHandle(folder.resolve(path).toFile());
  }

  private File saveFile() {
    return tempDir.resolve(SAVE_PATH).toFile();
  }

  @Test
  void hasSaveIsFalseWhenNoSaveFileExists() {
    assertFalse(SaveService.hasSave());
  }

  @Test
  void hasSaveIsFalseWhenFilesAreNotAvailable() {
    Gdx.files = null;

    assertFalse(SaveService.hasSave());
  }

  @Test
  void saveWritesTheFileToTheSaveFolder() {
    SaveService.save(new GameSaveData());

    assertTrue(saveFile().exists());
    assertTrue(SaveService.hasSave());
    verify(mockFiles, atLeastOnce()).external(SAVE_PATH);
  }

  @Test
  void saveThenLoadKeepsPlayerData() {
    GameSaveData data = new GameSaveData();
    data.health = 45;
    data.gold = 60;
    data.stamina = 80f;
    data.activeSlot = 2;
    data.posX = 14.5f;
    data.posY = 6f;
    data.level = "maps/level2.json";
    data.difficulty = "HARD";

    SaveService.save(data);
    GameSaveData loaded = SaveService.load();

    assertEquals(45, loaded.health);
    assertEquals(60, loaded.gold);
    assertEquals(80f, loaded.stamina);
    assertEquals(2, loaded.activeSlot);
    assertEquals(14.5f, loaded.posX);
    assertEquals(6f, loaded.posY);
    assertEquals("maps/level2.json", loaded.level);
    assertEquals("HARD", loaded.difficulty);
  }

  @Test
  void saveThenLoadKeepsListsAndNestedObjects() {
    GameSaveData data = new GameSaveData();
    SavedItem sword = new SavedItem();
    sword.slot = 1;
    sword.name = "Sword";
    sword.itemType = "WEAPON";
    sword.quantity = 1;
    sword.weaponTier = 2;
    data.items.add(sword);
    data.killedEnemyIds.add("Level 1 - Out of the Underworld:20,5");
    data.collectedLootIds.add("Level 1 - Out of the Underworld:12,7");
    data.ownedPetNames.add("Bird");

    SavedPerk perk = new SavedPerk();
    perk.id = "thickSkin";
    perk.progress = 1;
    perk.unlocked = true;
    perk.active = true;
    data.perks = new ArrayList<>();
    data.perks.add(perk);

    SaveService.save(data);
    GameSaveData loaded = SaveService.load();

    assertEquals(1, loaded.items.size());
    SavedItem loadedSword = loaded.items.get(0);
    assertEquals("Sword", loadedSword.name);
    assertEquals(1, loadedSword.slot);
    assertEquals(Integer.valueOf(2), loadedSword.weaponTier);

    assertEquals(1, loaded.killedEnemyIds.size());
    assertEquals("Level 1 - Out of the Underworld:20,5", loaded.killedEnemyIds.get(0));
    assertEquals("Level 1 - Out of the Underworld:12,7", loaded.collectedLootIds.get(0));
    assertEquals("Bird", loaded.ownedPetNames.get(0));

    assertNotNull(loaded.perks);
    assertEquals("thickSkin", loaded.perks.get(0).id);
    assertTrue(loaded.perks.get(0).unlocked);
    assertTrue(loaded.perks.get(0).active);
  }

  @Test
  void saveReplacesThePreviousSave() {
    GameSaveData first = new GameSaveData();
    first.gold = 10;
    SaveService.save(first);

    GameSaveData second = new GameSaveData();
    second.gold = 99;
    SaveService.save(second);

    assertEquals(99, SaveService.load().gold);
  }

  @Test
  void loadWithNoSaveFileReturnsEmptyData() {
    GameSaveData loaded = SaveService.load();

    assertNotNull(loaded);
    assertEquals(0, loaded.gold);
    assertNull(loaded.level);
    assertTrue(loaded.items.isEmpty());
  }

  @Test
  void loadWithBrokenSaveFileReturnsEmptyData() {
    File file = saveFile();
    file.getParentFile().mkdirs();
    new FileHandle(file).writeString("{\"health\": ", false);

    GameSaveData loaded = SaveService.load();

    assertNotNull(loaded);
    assertEquals(0, loaded.gold);
    assertTrue(loaded.items.isEmpty());
  }
}
