package com.csse3200.game.entities.spawn;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
        "ghost",
        target -> {
          seen[0] = target;
          return new Entity();
        });

    EntitySpawnRegistry.create("ghost", player);

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
    EntitySpawnRegistry.register("ghost", player -> new Entity());
    EntitySpawnRegistry.register("ghost", player -> second);

    assertSame(second, EntitySpawnRegistry.create("ghost", null));
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
    assertTrue(EntitySpawnRegistry.isRegistered("ghostking"));
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
