package com.csse3200.game.entities.spawn;

import com.csse3200.game.entities.Entity;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Maps a spawn name used in a map file to the factory that builds that entity.
 *
 * <p>This is how a team puts its own entities into levels without editing the map system. A map
 * says <em>what</em> to spawn and where; this registry says <em>how</em> to build it, and the two
 * sides never need to know about each other.
 *
 * <p>Register once at start-up, from your own team's code:
 *
 * <pre>{@code
 * EntitySpawnRegistry.register("skeleton", NPCFactory::createSkeleton);
 * EntitySpawnRegistry.register("pet:companion", PetFactory::createCompanion);
 * }</pre>
 *
 * <p>then place it in a map's entity legend, with no further code anywhere:
 *
 * <pre>{@code
 * "S": { "type": "ENEMY", "enemyType": "skeleton" }
 * }</pre>
 *
 * <p>Names are matched ignoring case and surrounding space. A name nobody has registered is
 * reported once and skipped, so a map may place an entity before the code to build it exists, and
 * the level still loads.
 */
public final class EntitySpawnRegistry {
  private static final Logger logger = LoggerFactory.getLogger(EntitySpawnRegistry.class);

  private static final Map<String, SpawnFactory> factories = new LinkedHashMap<>();

  private EntitySpawnRegistry() {}

  /** Builds one entity for a spawn point. */
  @FunctionalInterface
  public interface SpawnFactory {
    /**
     * @param player the player entity, which most enemies need as a target
     * @return the new entity, not yet registered or positioned
     */
    Entity create(Entity player);
  }

  /**
   * Registers the factory for a spawn name, replacing any previous factory for that name.
   *
   * @param name the name map files use, matched ignoring case (e.g. "skeleton", "npc:traveler")
   * @param factory builds the entity
   */
  public static void register(String name, SpawnFactory factory) {
    if (name == null || name.isBlank() || factory == null) {
      logger.warn("Ignoring spawn registration with no name or no factory");
      return;
    }
    factories.put(key(name), factory);
  }

  /**
   * Registers a factory only if the name is free, so a team's own registration is never replaced by
   * a later default.
   *
   * @param name the name map files use
   * @param factory builds the entity
   * @return true if this call registered the factory
   */
  public static boolean registerIfAbsent(String name, SpawnFactory factory) {
    if (name == null || name.isBlank() || factory == null || isRegistered(name)) {
      return false;
    }
    factories.put(key(name), factory);
    return true;
  }

  /**
   * Builds the entity registered under a name.
   *
   * @param name the spawn name from the map, may be null
   * @param player the player entity to pass to the factory
   * @return the new entity, or null if the name is unknown or the factory returned null
   */
  public static Entity create(String name, Entity player) {
    if (name == null || name.isBlank()) {
      return null;
    }
    SpawnFactory factory = factories.get(key(name));
    if (factory == null) {
      logger.warn(
          "No factory registered for spawn '{}' - skipped. Registered names: {}",
          name,
          factories.keySet());
      return null;
    }
    return factory.create(player);
  }

  /**
   * @param name the spawn name to look for
   * @return true if a factory is registered under that name
   */
  public static boolean isRegistered(String name) {
    return name != null && factories.containsKey(key(name));
  }

  /**
   * @return every registered spawn name, for diagnostics and validation
   */
  public static Set<String> registeredNames() {
    return Collections.unmodifiableSet(factories.keySet());
  }

  /** Removes every registration. Intended for tests that need a known starting state. */
  public static void clear() {
    factories.clear();
  }

  private static String key(String name) {
    return name.trim().toLowerCase(Locale.ROOT);
  }
}
