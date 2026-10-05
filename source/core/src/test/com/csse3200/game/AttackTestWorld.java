package com.csse3200.game;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.loot.WeaponItem;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.events.listeners.EventListener1;
import com.csse3200.game.physics.PhysicsLayer;
import com.csse3200.game.physics.PhysicsService;
import com.csse3200.game.physics.components.ColliderComponent;
import com.csse3200.game.physics.components.HitboxComponent;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import java.util.ArrayList;
import java.util.List;

/**
 * A small, reusable fake game for attack and projectile tests. It does the three jobs every one of
 * these tests repeats, so a test class can stay focused on the behaviour it checks:
 *
 * <ol>
 *   <li><b>Set up the services.</b> {@link #create()} registers a mocked clock (fixed 0.02 s per
 *       frame), a resource service that hands back a 16 by 16 texture for any request, a mocked
 *       render service, a real physics world and an entity service that records everything
 *       registered with it. Call it from {@code @BeforeEach}.
 *   <li><b>Build the usual entities.</b> {@link #newTarget}, {@link #newAttacker} and the weapon
 *       helpers create the shapes these tests keep needing.
 *   <li><b>Run time forward.</b> {@link #step(int, Entity...)} advances the physics world and
 *       updates every registered entity, exactly as the game loop does.
 * </ol>
 *
 * <p>Use one instance per test. Nothing here is static state, so tests cannot leak into each other,
 * apart from the {@link ServiceLocator}, which {@link #create()} overwrites every time.
 *
 * <p>Where it lives: {@code core/src/test/com/csse3200/game/testing/AttackTestWorld.java}.
 */
public final class AttackTestWorld {
  /** Seconds the mocked clock reports for every frame. */
  public static final float DELTA = 0.02f;

  private final List<Entity> registered = new ArrayList<>();

  private AttackTestWorld() {}

  /**
   * Registers fresh services and returns the world that tracks them.
   *
   * @return a new, empty world
   */
  public static AttackTestWorld create() {
    AttackTestWorld world = new AttackTestWorld();

    Texture texture = mock(Texture.class);
    when(texture.getWidth()).thenReturn(16);
    when(texture.getHeight()).thenReturn(16);
    ResourceService resourceService = mock(ResourceService.class);
    when(resourceService.getAsset(anyString(), eq(Texture.class))).thenReturn(texture);

    GameTime gameTime = mock(GameTime.class);
    when(gameTime.getDeltaTime()).thenReturn(DELTA);

    ServiceLocator.registerTimeSource(gameTime);
    ServiceLocator.registerResourceService(resourceService);
    ServiceLocator.registerRenderService(mock(RenderService.class));
    ServiceLocator.registerPhysicsService(new PhysicsService());
    ServiceLocator.registerEntityService(new RecordingEntityService(world.registered));
    return world;
  }

  // ---------- entities ----------

  /**
   * Creates a stationary stand-in for the player: a dynamic body with gravity switched off, a
   * hitbox on the player layer and some health, registered with the world.
   *
   * @param x horizontal position
   * @param y vertical position
   * @param health starting health
   * @return the registered target
   */
  public Entity newTarget(float x, float y, int health) {
    Entity target =
        new Entity()
            .addComponent(new PhysicsComponent())
            .addComponent(new ColliderComponent())
            .addComponent(new HitboxComponent().setLayer(PhysicsLayer.PLAYER))
            .addComponent(new CombatStatsComponent(health, 0));
    register(target);
    target.setPosition(x, y);
    target.getComponent(PhysicsComponent.class).getBody().setGravityScale(0f);
    return target;
  }

  /**
   * Creates an attacker that is NOT registered, so a test controls when it updates. It has the
   * given components plus 20 health and 5 base attack, and sits at the origin with a 1 by 2 body
   * (so "height" means something when a test places a projectile relative to the owner).
   *
   * @param components the attack components under test
   * @return the created attacker
   */
  public Entity newAttacker(com.csse3200.game.components.Component... components) {
    Entity attacker = new Entity();
    for (com.csse3200.game.components.Component component : components) {
      attacker.addComponent(component);
    }
    attacker.addComponent(new CombatStatsComponent(20, 5));
    attacker.setScale(1f, 2f);
    attacker.create();
    attacker.setPosition(0f, 0f);
    return attacker;
  }

  /**
   * Registers an entity with the entity service (which creates it) and remembers it so {@link
   * #step(int, Entity...)} will update it.
   *
   * @param entity the entity to register
   * @return the same entity
   */
  public Entity register(Entity entity) {
    ServiceLocator.getEntityService().register(entity);
    return entity;
  }

  /**
   * @return how many entities have been registered so far. Take this before an action, then call
   *     {@link #registeredSince(int)} to see what the action created, for example a projectile.
   */
  public int mark() {
    return registered.size();
  }

  /**
   * @param mark a value returned earlier by {@link #mark()}
   * @return the entities registered after that point, oldest first
   */
  public List<Entity> registeredSince(int mark) {
    return new ArrayList<>(registered.subList(mark, registered.size()));
  }

  // ---------- weapons ----------

  /**
   * @param damage damage the weapon deals
   * @param windup seconds of windup (the test weapon's own windup)
   * @return a natural weapon, the kind enemies carry
   */
  public static WeaponItem naturalWeapon(int damage, float windup) {
    return WeaponItem.natural("Test Natural Weapon", damage, windup);
  }

  // ---------- events ----------

  /**
   * Records every entity an event carries, in order. Use it for the attack events, which all pass
   * the target (for example {@code "rockAttackFired"}).
   *
   * @param source the entity that announces the event
   * @param eventName the event to listen for
   * @return a live list that fills as the event fires
   */
  public static List<Entity> record(Entity source, String eventName) {
    List<Entity> seen = new ArrayList<>();
    source.getEvents().addListener(eventName, (EventListener1<Entity>) seen::add);
    return seen;
  }

  // ---------- time ----------

  /**
   * Runs the game forward. Each frame advances the physics world once, then runs the early update
   * and the update of every registered entity and of every extra entity given.
   *
   * @param frames number of frames (each {@link #DELTA} seconds)
   * @param extras entities that are not registered but must still update, such as an attacker
   */
  public void step(int frames, Entity... extras) {
    for (int i = 0; i < frames; i++) {
      ServiceLocator.getPhysicsService().getPhysics().update();
      for (Entity entity : new ArrayList<>(registered)) {
        entity.earlyUpdate();
        entity.update();
      }
      for (Entity extra : extras) {
        extra.earlyUpdate();
        extra.update();
      }
    }
  }

  /**
   * @param seconds time to advance
   * @return the number of frames that is at least that long
   */
  public static int framesFor(float seconds) {
    return (int) Math.ceil(seconds / DELTA);
  }

  /**
   * @param entity any entity with a physics body
   * @return a copy of the body's linear velocity
   */
  public static Vector2 velocityOf(Entity entity) {
    return entity.getComponent(PhysicsComponent.class).getBody().getLinearVelocity().cpy();
  }

  /** An entity service that remembers what is registered, so tests can find spawned projectiles. */
  private static final class RecordingEntityService extends EntityService {
    private final List<Entity> sink;

    RecordingEntityService(List<Entity> sink) {
      this.sink = sink;
    }

    @Override
    public void register(Entity entity) {
      sink.add(entity);
      super.register(entity);
    }
  }
}
