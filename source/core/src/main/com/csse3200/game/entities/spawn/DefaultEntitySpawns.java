package com.csse3200.game.entities.spawn;

import com.csse3200.game.entities.factories.EnemyFactory;

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
    EntitySpawnRegistry.registerIfAbsent("skeleton", EnemyFactory::createSkeleton);
    EntitySpawnRegistry.registerIfAbsent("enemy-skeleton-hoplite", EnemyFactory::createSkeleton);
    EntitySpawnRegistry.registerIfAbsent("ranged-skeleton", EnemyFactory::createRangedSkeleton);
    EntitySpawnRegistry.registerIfAbsent("cyclops", EnemyFactory::createCyclops);
    EntitySpawnRegistry.registerIfAbsent("minotaur", EnemyFactory::createMinotaur);
    EntitySpawnRegistry.registerIfAbsent("centaur", EnemyFactory::createCentaur);
    EntitySpawnRegistry.registerIfAbsent("enemy-centaur", EnemyFactory::createCentaur);
    EntitySpawnRegistry.registerIfAbsent("cerberus", EnemyFactory::createCerberus);
    EntitySpawnRegistry.registerIfAbsent("enemy-cerberus", EnemyFactory::createCerberus);
    EntitySpawnRegistry.registerIfAbsent("medusa", EnemyFactory::createMedusa);
    EntitySpawnRegistry.registerIfAbsent("enemy-medusa", EnemyFactory::createMedusa);
    EntitySpawnRegistry.registerIfAbsent("zeus", EnemyFactory::createZeus);
    EntitySpawnRegistry.registerIfAbsent("enemy-zeus", EnemyFactory::createZeus);
    EntitySpawnRegistry.registerIfAbsent("harpy", EnemyFactory::createHarpy);
    EntitySpawnRegistry.registerIfAbsent("ranged-harpy", EnemyFactory::createRangedHarpy);
  }

  /** Allows tests to re-run {@link #registerAll()} from a known state. */
  public static void reset() {
    registered = false;
  }
}
