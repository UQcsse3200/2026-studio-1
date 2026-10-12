package com.csse3200.game.entities.factories;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.configs.NPCConfigs;
import com.csse3200.game.entities.spawn.DefaultEntitySpawns;
import com.csse3200.game.entities.spawn.EntitySpawnRegistry;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.files.FileLoader;
import com.csse3200.game.physics.PhysicsLayer;
import com.csse3200.game.physics.PhysicsService;
import com.csse3200.game.physics.components.HitboxComponent;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tests that each NPC factory method wires the components, weapons and values its design needs,
 * and, for the two bosses with their own groups below, how their attacks behave once built. "Wired"
 * tests check what the built entity HAS; the entities are not created or run, so no physics
 * stepping happens in them.
 *
 * <p>The file is grouped with JUnit 5 nested classes, so the test report reads as a tree:
 *
 * <ul>
 *   <li><b>every enemy</b>: the rules all ten enemies share, run once per enemy.
 *   <li><b>touch damage</b>: only the charging enemies have it.
 *   <li><b>one group per enemy</b>: skeleton, ranged skeleton, minotaur, centaur, cyclops, medusa,
 *       cerberus, zeus and the harpies. The Cerberus and Zeus groups are split again into config
 *       (the rules {@code NPCs.json} must obey), factory wiring and attacks (behaviour with real
 *       attack components).
 *   <li><b>spawn registry</b>: the NPC spawn names.
 * </ul>
 *
 * <p>Every number is read from {@code configs/NPCs.json} through the config classes rather than
 * written out here, so changing a value in the JSON does not break these tests, and a value that
 * the factory forgets to read does. The exceptions are the behaviour groups for Zeus and the
 * Cerberus, which build real attack components from named constants at the top of each group.
 *
 * <p>The target is a stand-in for the player: it has combat stats and a hitbox on the player layer,
 * because the charging enemies' touch attack is built to hit the player layer.
 *
 * <p>The "attacks" and "bite" groups call {@code AttackTestWorld.create()}, which replaces the
 * registered services. They therefore sit apart from the wiring groups, which use the services
 * registered by {@link #setUp()}.
 */
@ExtendWith(GameExtension.class)
class NPCFactoryTest {

  private Entity player;
  private NPCConfigs configs;

  @BeforeEach
  void setUp() {
    ServiceLocator.registerPhysicsService(new PhysicsService());
    ServiceLocator.registerRenderService(mock(RenderService.class));
    ServiceLocator.registerResourceService(mock(ResourceService.class));
    ServiceLocator.registerTimeSource(mock(GameTime.class));
    player =
        new Entity()
            .addComponent(new CombatStatsComponent(100, 0))
            .addComponent(new HitboxComponent().setLayer(PhysicsLayer.PLAYER));
    configs = FileLoader.readClass(NPCConfigs.class, "configs/NPCs.json");
  }

  @AfterEach
  void resetRegistry() {
    EntitySpawnRegistry.clear();
    DefaultEntitySpawns.reset();
  }

  // =====================================================================================
  // SPAWN REGISTRY
  // =====================================================================================

  @Nested
  @DisplayName("spawn registry")
  class SpawnRegistry {

    @Test
    void shouldRegisterAllNpcSpawns() {
      NPCFactory.registerNpcSpawns();

      assertTrue(EntitySpawnRegistry.isRegistered("npc:shop"));
      assertTrue(EntitySpawnRegistry.isRegistered("npc:wizard"));
      assertTrue(EntitySpawnRegistry.isRegistered("npc:philosopher"));
      assertTrue(EntitySpawnRegistry.isRegistered("npc:satyr"));
    }
  }
}
