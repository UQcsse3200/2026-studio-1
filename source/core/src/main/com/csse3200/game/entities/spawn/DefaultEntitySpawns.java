package com.csse3200.game.entities.spawn;

import com.csse3200.game.entities.factories.NPCFactory;

/**
 * The spawn names that ship with the game, registered in one place so existing maps keep working.
 *
 * <p>This class is transitional. Each entry belongs to the team that owns the entity, and each team
 * should move its own lines into its own start-up code, at which point this class loses that entry.
 * Nothing here is special: these are ordinary {@link EntitySpawnRegistry} registrations, and a team
 * registering elsewhere gets exactly the same result.
 *
 * <p>Do not add to this class. Register from your own code instead:
 *
 * <pre>{@code
 * EntitySpawnRegistry.register("my-enemy", MyFactory::createMyEnemy);
 * }</pre>
 */
public final class DefaultEntitySpawns {
  private static boolean registered;

  private DefaultEntitySpawns() {}

  /**
   * Registers the shipped spawn names. Safe to call repeatedly, and a name a team has already
   * registered for itself is left alone, so these defaults never override a team's own factory.
   */
  public static void registerAll() {
    if (registered) {
      return;
    }
    registered = true;

    // Enemy team.
    EntitySpawnRegistry.registerIfAbsent("skeleton", NPCFactory::createSkeleton);
    EntitySpawnRegistry.registerIfAbsent("enemy-skeleton-hoplite", NPCFactory::createSkeleton);
    EntitySpawnRegistry.registerIfAbsent("rangedskeleton", NPCFactory::createRangedSkeleton);
    EntitySpawnRegistry.registerIfAbsent("ranged-skeleton", NPCFactory::createRangedSkeleton);
    EntitySpawnRegistry.registerIfAbsent("cyclops", NPCFactory::createCyclops);
    EntitySpawnRegistry.registerIfAbsent("minotaur", NPCFactory::createMinotaur);
    EntitySpawnRegistry.registerIfAbsent("centaur", NPCFactory::createCentaur);
    EntitySpawnRegistry.registerIfAbsent("enemy-centaur", NPCFactory::createCentaur);

    // NPC team.
    EntitySpawnRegistry.registerIfAbsent("npc:traveler", NPCFactory::createTravelerNPC);
  }

  /** Allows tests to re-run {@link #registerAll()} from a known state. */
  public static void reset() {
    registered = false;
  }
}
