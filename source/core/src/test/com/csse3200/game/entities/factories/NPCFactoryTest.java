package com.csse3200.game.entities.factories;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.csse3200.game.entities.spawn.DefaultEntitySpawns;
import com.csse3200.game.entities.spawn.EntitySpawnRegistry;
import com.csse3200.game.extensions.GameExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class NPCFactoryTest {
  @BeforeEach
  @AfterEach
  void resetRegistry() {
    EntitySpawnRegistry.clear();
    DefaultEntitySpawns.reset();
  }

  @Test
  void shouldRegisterAllNpcSpawns() {
    NPCFactory.registerNpcSpawns();

    assertTrue(EntitySpawnRegistry.isRegistered("npc:shop"));
    assertTrue(EntitySpawnRegistry.isRegistered("npc:wizard"));
    assertTrue(EntitySpawnRegistry.isRegistered("npc:philosopher"));
    assertTrue(EntitySpawnRegistry.isRegistered("npc:satyr"));
  }
}
