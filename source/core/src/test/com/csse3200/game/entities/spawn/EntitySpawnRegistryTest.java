package com.csse3200.game.entities.spawn;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.attacks.MeleeAttackComponent;
import com.csse3200.game.components.loot.WeaponItem;
import com.csse3200.game.components.loot.WeaponType;
import com.csse3200.game.components.player.InventoryComponent;
import com.csse3200.game.difficulty.Difficulty;
import com.csse3200.game.difficulty.DifficultyService;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class EntitySpawnRegistryTest {
  @BeforeEach
  @AfterEach
  void resetRegistry() {
    EntitySpawnRegistry.clear();
    DefaultEntitySpawns.reset();
    DifficultyService.setCurrent(Difficulty.NORMAL);
  }

  @Test
  void buildsTheEntityRegisteredUnderAName() {
    Entity expected = new Entity();
    EntitySpawnRegistry.register("skeleton", player -> expected);

    assertTrue(EntitySpawnRegistry.isRegistered("skeleton"));
    assertSame(expected, EntitySpawnRegistry.create("skeleton", null));
  }

  @Test
  void passesThePlayerToTheFactory() {
    Entity player = new Entity();
    Entity[] seen = new Entity[1];
    EntitySpawnRegistry.register(
        "skeleton",
        target -> {
          seen[0] = target;
          return new Entity();
        });

    EntitySpawnRegistry.create("skeleton", player);

    assertSame(player, seen[0]);
  }

  @Test
  void matchesNamesIgnoringCaseAndSurroundingSpace() {
    EntitySpawnRegistry.register("Ranged-Skeleton", player -> new Entity());

    assertTrue(EntitySpawnRegistry.isRegistered("ranged-skeleton"));
    assertNotNull(EntitySpawnRegistry.create("  RANGED-SKELETON  ", null));
  }

  @Test
  void skipsAnUnknownNameInsteadOfFailing() {
    assertNull(EntitySpawnRegistry.create("centaur", null));
    assertFalse(EntitySpawnRegistry.isRegistered("centaur"));
  }

  @Test
  void createAppliesDifficultyScalingToTheBuiltEntity() {
    // Fails if DifficultyScaler.apply() is ever removed from create() - a fresh entity with these
    // starting values would otherwise come back completely unscaled on HARD.
    WeaponItem sword = new WeaponItem("Sword", WeaponType.SWORD, 5, 1, 1, 0f);
    EntitySpawnRegistry.register(
        "hard-skeleton",
        player ->
            new Entity()
                .addComponent(new CombatStatsComponent(30, 5))
                .addComponent(new MeleeAttackComponent(1f, 1f, 0f, sword))
                .addComponent(new InventoryComponent(3)));
    DifficultyService.setCurrent(Difficulty.HARD);

    Entity enemy = EntitySpawnRegistry.create("hard-skeleton", null);

    CombatStatsComponent stats = enemy.getComponent(CombatStatsComponent.class);
    InventoryComponent inventory = enemy.getComponent(InventoryComponent.class);
    assertEquals(42, stats.getHealth());
    assertEquals(8, stats.getBaseAttack());
    assertEquals(5, inventory.getGold());
  }

  @Test
  void createDoesNotThrowWhenNoFactoryIsRegisteredForAName() {
    // create() now runs every built entity through DifficultyScaler.apply() before returning it -
    // this confirms the "no factory registered" path still short-circuits to null before ever
    // reaching that call, rather than passing null into it and relying on DifficultyScaler's own
    // null-check.
    assertDoesNotThrow(() -> assertNull(EntitySpawnRegistry.create("no-such-spawn", null)));
  }

  @Test
  void skipsNullAndBlankNames() {
    assertNull(EntitySpawnRegistry.create(null, null));
    assertNull(EntitySpawnRegistry.create("  ", null));
    assertFalse(EntitySpawnRegistry.isRegistered(null));
  }

  @Test
  void ignoresRegistrationsWithNoNameOrFactory() {
    EntitySpawnRegistry.register(null, player -> new Entity());
    EntitySpawnRegistry.register("orphan", null);

    assertTrue(EntitySpawnRegistry.registeredNames().isEmpty());
  }

  @Test
  void aLaterRegistrationReplacesAnEarlierOne() {
    Entity second = new Entity();
    EntitySpawnRegistry.register("skeleton", player -> new Entity());
    EntitySpawnRegistry.register("skeleton", player -> second);

    assertSame(second, EntitySpawnRegistry.create("skeleton", null));
    assertEquals(1, EntitySpawnRegistry.registeredNames().size());
  }

  @Test
  void registerIfAbsentLeavesAnExistingNameAlone() {
    Entity mine = new Entity();
    EntitySpawnRegistry.register("skeleton", player -> mine);

    assertFalse(EntitySpawnRegistry.registerIfAbsent("skeleton", player -> new Entity()));
    assertSame(mine, EntitySpawnRegistry.create("skeleton", null));
  }

  @Test
  void shippedNamesAreRegisteredByDefault() {
    DefaultEntitySpawns.registerAll();

    assertTrue(EntitySpawnRegistry.isRegistered("skeleton"));
    assertTrue(EntitySpawnRegistry.isRegistered("ranged-skeleton"));
    assertTrue(EntitySpawnRegistry.isRegistered("centaur"));
  }

  @Test
  void defaultsDoNotOverwriteATeamsOwnRegistration() {
    Entity mine = new Entity();
    EntitySpawnRegistry.register("skeleton", player -> mine);

    DefaultEntitySpawns.registerAll();
    DefaultEntitySpawns.registerAll();

    assertSame(mine, EntitySpawnRegistry.create("skeleton", null));
  }
}
