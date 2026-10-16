package com.csse3200.game.entities.factories;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.csse3200.game.ai.tasks.AITaskComponent;
import com.csse3200.game.ai.tasks.PriorityTask;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.QuestGiverComponent;
import com.csse3200.game.components.enemies.EnemyDeathComponent;
import com.csse3200.game.components.enemies.EnemyTypeComponent;
import com.csse3200.game.components.enemies.animation.SkeletonAnimationController;
import com.csse3200.game.components.enemies.attacks.MeleeAttackComponent;
import com.csse3200.game.components.enemies.attacks.RangedAttackComponent;
import com.csse3200.game.components.enemies.attacks.TouchAttackComponent;
import com.csse3200.game.components.enemies.tasks.PlatformWanderTask;
import com.csse3200.game.components.loot.ConsumableItem;
import com.csse3200.game.components.loot.ConsumableType;
import com.csse3200.game.components.loot.Item;
import com.csse3200.game.components.loot.ItemType;
import com.csse3200.game.components.loot.WeaponItem;
import com.csse3200.game.components.loot.WeaponType;
import com.csse3200.game.components.npc.DialogueComponent;
import com.csse3200.game.components.npc.DialogueProximityComponent;
import com.csse3200.game.components.npc.DisplayDialogue;
import com.csse3200.game.components.npc.HeadbuttAttackComponent;
import com.csse3200.game.components.npc.PotionThrowComponent;
import com.csse3200.game.components.npc.ProvokedComponent;
import com.csse3200.game.components.npc.ShopkeeperComponent;
import com.csse3200.game.components.player.InventoryComponent;
import com.csse3200.game.components.player.ItemDropComponent;
import com.csse3200.game.components.tasks.ChaseTask;
import com.csse3200.game.components.tasks.RetaliateTask;
import com.csse3200.game.components.tasks.StandStillTask;
import com.csse3200.game.components.tasks.WanderTask;
import com.csse3200.game.difficulty.Difficulty;
import com.csse3200.game.difficulty.DifficultyService;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.spawn.EntitySpawnRegistry;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.physics.PhysicsLayer;
import com.csse3200.game.physics.PhysicsService;
import com.csse3200.game.physics.components.ColliderComponent;
import com.csse3200.game.physics.components.HitboxComponent;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.physics.components.PhysicsMovementComponent;
import com.csse3200.game.rendering.AnimationRenderComponent;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Tests that each {@link NPCFactory} creator wires the friendly NPC's components, quest, dialogue
 * and animations, that the private helpers behind them do what their Javadoc promises, and that the
 * NPC spawn names build the right NPC. "Wired" tests check what the built entity HAS; the entities
 * are not created or run, so no physics stepping happens in them.
 *
 * <p>The file is grouped with JUnit 5 nested classes, so the test report reads as a tree:
 *
 * <ul>
 *   <li><b>every NPC</b>: the rules all four friendly NPCs share, run once per NPC.
 *   <li><b>killable NPCs</b>: the wizard, philosopher and satyr can be killed; the shopkeeper
 *       cannot.
 *   <li><b>one group per NPC</b>: shop, wizard, philosopher and satyr.
 *   <li><b>helpers</b>: the private static helpers, reached by reflection, one group per helper.
 *   <li><b>spawn registry</b>: the NPC spawn names.
 *   <li><b>structure</b>: enemy creators stay out of this factory.
 * </ul>
 *
 * <p>The target is a stand-in for the player: it has combat stats and a hitbox on the player layer.
 * The numbers (50 health, 8 unit provoke range, 100 gold and so on) are the ones in {@code
 * NPCFactory}, so the cases read the same as the Java.
 *
 * <p>Nearly every NPC component keeps its settings in private fields with no getter, so the tests
 * read them by reflection through {@link #read}. The two tasks that hold a priority ({@code
 * StandStillTask}, {@code RetaliateTask}) are read the same way, because their {@code
 * getPriority()} depends on a running owner.
 */
@ExtendWith(GameExtension.class)
class NPCFactoryTest {
  private static final String EVERY_NPC =
      "com.csse3200.game.entities.factories.NPCFactoryTest#everyNpc";

  private static final String SHOP_NPC_ATLAS_PATH = "images/npcs/npc_shop.atlas";
  private static final float FRIENDLY_NPC_HEIGHT = 47f / 42f;
  private static final float FRIENDLY_NPC_COLLIDER_WIDTH = 0.9f;
  private static final float FRIENDLY_NPC_WANDER_RANGE = 1.5f;
  private static final int FRIENDLY_NPC_HEALTH = 50;
  private static final int FRIENDLY_NPC_STAND_PRIORITY = 5;
  private static final float TOLERANCE = 1e-4f;

  private Entity player;

  @BeforeEach
  void setUp() {
    ServiceLocator.registerPhysicsService(new PhysicsService());
    ServiceLocator.registerRenderService(mock(RenderService.class));
    ServiceLocator.registerResourceService(mock(ResourceService.class));
    ServiceLocator.registerTimeSource(mock(GameTime.class));
    DifficultyService.setCurrent(Difficulty.NORMAL);
    player =
        new Entity()
            .addComponent(new CombatStatsComponent(100, 0))
            .addComponent(new HitboxComponent().setLayer(PhysicsLayer.PLAYER));
  }

  @AfterEach
  void resetRegistry() {
    EntitySpawnRegistry.clear();
  }

  /** One friendly NPC as every parameterised test sees it: its name, creator and speaker. */
  private record NpcCase(String name, Function<Entity, Entity> factory, String speaker) {
    @Override
    public String toString() {
      return name;
    }
  }

  static Stream<NpcCase> everyNpc() {
    return Stream.of(
        new NpcCase("shop", NPCFactory::createShopNPC, "Hermes"),
        new NpcCase("wizard", NPCFactory::createWizardNPC, "Wizard"),
        new NpcCase("philosopher", NPCFactory::createPhilosopherNPC, "Philosopher"),
        new NpcCase("satyr", NPCFactory::createSatyrNPC, "Satyr"));
  }

  /** Reads a private field of {@code owner} that is declared in {@code declaring}. */
  private static Object read(Object owner, Class<?> declaring, String name)
      throws ReflectiveOperationException {
    Field field = declaring.getDeclaredField(name);
    field.setAccessible(true);
    return field.get(owner);
  }

  /** Reads the tasks an AITaskComponent holds; it keeps them in a private list with no getter. */
  @SuppressWarnings("unchecked")
  private static List<PriorityTask> tasksOf(Entity entity) throws ReflectiveOperationException {
    return (List<PriorityTask>)
        read(entity.getComponent(AITaskComponent.class), AITaskComponent.class, "priorityTasks");
  }

  /** Finds the one task of a type among an entity's tasks, or null if there is none. */
  private static <T extends PriorityTask> T taskOf(Entity entity, Class<T> type)
      throws ReflectiveOperationException {
    for (PriorityTask task : tasksOf(entity)) {
      if (type.isInstance(task)) {
        return type.cast(task);
      }
    }
    return null;
  }

  /** Counts the tasks of a type among an entity's tasks. */
  private static long countTasks(Entity entity, Class<? extends PriorityTask> type)
      throws ReflectiveOperationException {
    return tasksOf(entity).stream().filter(type::isInstance).count();
  }

  /** Calls a private static helper of NPCFactory, unwrapping the exception it throws. */
  private static Object call(String name, Class<?>[] parameterTypes, Object... args)
      throws Throwable {
    Method method = NPCFactory.class.getDeclaredMethod(name, parameterTypes);
    method.setAccessible(true);
    try {
      return method.invoke(null, args);
    } catch (InvocationTargetException e) {
      throw e.getCause();
    }
  }

  /** The width and height of the box shape in an entity's collider. */
  private static Vector2 colliderSize(Entity entity) throws ReflectiveOperationException {
    FixtureDef definition =
        (FixtureDef)
            read(
                entity.getComponent(ColliderComponent.class),
                ColliderComponent.class,
                "fixtureDef");
    PolygonShape shape = (PolygonShape) definition.shape;
    Vector2 vertex = new Vector2();
    float minX = Float.MAX_VALUE;
    float maxX = -Float.MAX_VALUE;
    float minY = Float.MAX_VALUE;
    float maxY = -Float.MAX_VALUE;
    for (int i = 0; i < shape.getVertexCount(); i++) {
      shape.getVertex(i, vertex);
      minX = Math.min(minX, vertex.x);
      maxX = Math.max(maxX, vertex.x);
      minY = Math.min(minY, vertex.y);
      maxY = Math.max(maxY, vertex.y);
    }
    return new Vector2(maxX - minX, maxY - minY);
  }

  /** The animations registered on an animator, by name. */
  @SuppressWarnings("unchecked")
  private static Map<String, Animation<TextureRegion>> animationsOf(
      AnimationRenderComponent animator) throws ReflectiveOperationException {
    return (Map<String, Animation<TextureRegion>>)
        read(animator, AnimationRenderComponent.class, "animations");
  }

  /** The speaker name a DisplayDialogue was built with. */
  private static String speakerOf(Entity npc) throws ReflectiveOperationException {
    return (String)
        read(npc.getComponent(DisplayDialogue.class), DisplayDialogue.class, "speakerName");
  }

  /** The amount a DialogueComponent asks the player to do. */
  private static int amountToDoOf(Entity npc) throws ReflectiveOperationException {
    return (int)
        read(npc.getComponent(DialogueComponent.class), DialogueComponent.class, "amountXToDo");
  }

  /** The priority a StandStillTask or RetaliateTask was built with. */
  private static int priorityOf(PriorityTask task) throws ReflectiveOperationException {
    return (int) read(task, task.getClass(), "priority");
  }

  @Nested
  @DisplayName("every NPC")
  class EveryNpc {

    /** Should build every npc without throwing. */
    @ParameterizedTest
    @MethodSource(EVERY_NPC)
    void shouldBuildEveryNpcWithoutThrowing(NpcCase npc) {
      assertNotNull(npc.factory().apply(player));
    }

    /** Should wire the friendly parts on every npc. */
    @ParameterizedTest
    @MethodSource(EVERY_NPC)
    void shouldWireTheFriendlyPartsOnEveryNpc(NpcCase npc) {
      Entity built = npc.factory().apply(player);

      assertNotNull(built.getComponent(PhysicsComponent.class));
      assertNotNull(built.getComponent(PhysicsMovementComponent.class));
      assertNotNull(built.getComponent(ColliderComponent.class));
      assertNotNull(built.getComponent(QuestGiverComponent.class));
      assertNotNull(built.getComponent(DialogueComponent.class));
      assertNotNull(built.getComponent(DisplayDialogue.class));
      assertNotNull(built.getComponent(DialogueProximityComponent.class));
      assertNotNull(built.getComponent(AITaskComponent.class));
      assertNotNull(built.getComponent(AnimationRenderComponent.class));
    }

    /** Should give every npc five dialogue lines and the jump quest to start with. */
    @ParameterizedTest
    @MethodSource(EVERY_NPC)
    void shouldGiveEveryNpcFiveDialogueLinesAndTheJumpQuestToStartWith(NpcCase npc)
        throws ReflectiveOperationException {
      DialogueComponent dialogue =
          npc.factory().apply(player).getComponent(DialogueComponent.class);
      String[] lines = (String[]) read(dialogue, DialogueComponent.class, "dialogue");

      assertEquals(5, dialogue.getLineCount());
      assertEquals("Hello, here's a quest!", lines[0]);
    }

    /** Should name every speaker. */
    @ParameterizedTest
    @MethodSource(EVERY_NPC)
    void shouldNameEverySpeaker(NpcCase npc) throws ReflectiveOperationException {
      assertEquals(npc.speaker(), speakerOf(npc.factory().apply(player)));
    }

    /** Should give every npc the four walk and idle animations. */
    @ParameterizedTest
    @MethodSource(EVERY_NPC)
    void shouldGiveEveryNpcTheFourWalkAndIdleAnimations(NpcCase npc) {
      AnimationRenderComponent animator =
          npc.factory().apply(player).getComponent(AnimationRenderComponent.class);

      for (String name : new String[] {"walkr", "walkl", "idler", "idlel"}) {
        assertTrue(animator.hasAnimation(name), name);
      }
    }

    /**
     * Should hold exactly a platform wander and a stand still task, plus one retaliate task for the
     * wizard and the satyr. RetaliateTask extends StandStillTask, so tasks are compared by their
     * exact class and a failure prints the whole list.
     */
    @ParameterizedTest
    @MethodSource(EVERY_NPC)
    void shouldHoldExactlyAWanderAndAStandStillTask(NpcCase npc)
        throws ReflectiveOperationException {
      Entity built = npc.factory().apply(player);

      List<String> expected = new ArrayList<>(List.of("PlatformWanderTask", "StandStillTask"));
      if (npc.name().equals("wizard") || npc.name().equals("satyr")) {
        expected.add("RetaliateTask");
      }
      List<String> actual = new ArrayList<>();
      for (PriorityTask task : tasksOf(built)) {
        actual.add(task.getClass().getSimpleName());
      }
      expected.sort(null);
      actual.sort(null);

      assertEquals(expected, actual, npc.name() + " tasks");
    }

    /** Should keep the stand still task at the friendly NPC priority. */
    @ParameterizedTest
    @MethodSource(EVERY_NPC)
    void shouldKeepTheStandStillTaskAtTheFriendlyPriority(NpcCase npc)
        throws ReflectiveOperationException {
      Entity built = npc.factory().apply(player);
      PriorityTask standStill = null;
      for (PriorityTask task : tasksOf(built)) {
        if (task.getClass() == StandStillTask.class) {
          standStill = task;
        }
      }

      assertNotNull(standStill, npc.name() + " needs a plain stand still task");
      assertEquals(FRIENDLY_NPC_STAND_PRIORITY, priorityOf(standStill));
    }

    /** Should move on the ground. */
    @ParameterizedTest
    @MethodSource(EVERY_NPC)
    void shouldMoveOnTheGround(NpcCase npc) {
      assertTrue(
          npc.factory()
              .apply(player)
              .getComponent(PhysicsMovementComponent.class)
              .isGroundedMovement());
    }

    /** Should not have an enemy type or enemy attack. */
    @ParameterizedTest
    @MethodSource(EVERY_NPC)
    void shouldNotHaveAnEnemyTypeOrEnemyAttack(NpcCase npc) {
      Entity built = npc.factory().apply(player);

      assertNull(built.getComponent(EnemyTypeComponent.class));
      assertNull(built.getComponent(MeleeAttackComponent.class));
      assertNull(built.getComponent(RangedAttackComponent.class));
      assertNull(built.getComponent(TouchAttackComponent.class));
    }

    /** Should give every npc a positive scale. */
    @ParameterizedTest
    @MethodSource(EVERY_NPC)
    void shouldGiveEveryNpcAPositiveScale(NpcCase npc) {
      Entity built = npc.factory().apply(player);

      assertTrue(built.getScale().x > 0);
      assertTrue(built.getScale().y > 0);
    }

    /**
     * Should reject a null player. The quest giver refuses it, so every creator fails at
     * construction with an IllegalArgumentException (not a NullPointerException) that says why.
     */
    @ParameterizedTest
    @MethodSource(EVERY_NPC)
    void shouldRejectANullPlayerBecauseTheQuestGiverNeedsOne(NpcCase npc) {
      IllegalArgumentException thrown =
          assertThrows(IllegalArgumentException.class, () -> npc.factory().apply(null));

      assertTrue(thrown.getMessage().contains("null for a player"));
    }
  }

  @Nested
  @DisplayName("killable NPCs")
  class KillableNpcs {

    /** Should make the killable npcs killable. */
    @Test
    void shouldMakeTheKillableNpcsKillable() {
      List<Function<Entity, Entity>> creators =
          List.of(
              NPCFactory::createWizardNPC,
              NPCFactory::createPhilosopherNPC,
              NPCFactory::createSatyrNPC);

      for (Function<Entity, Entity> creator : creators) {
        Entity npc = creator.apply(player);

        assertNotNull(npc.getComponent(CombatStatsComponent.class));
        assertNotNull(npc.getComponent(EnemyDeathComponent.class));
        assertEquals(PhysicsLayer.NPC, npc.getComponent(HitboxComponent.class).getLayer());
        assertEquals(FRIENDLY_NPC_HEALTH, npc.getComponent(CombatStatsComponent.class).getHealth());
        assertEquals(0, npc.getComponent(CombatStatsComponent.class).getBaseAttack());
      }
    }

    /** Should not make the shopkeeper killable. */
    @Test
    void shouldNotMakeTheShopkeeperKillable() {
      Entity npc = NPCFactory.createShopNPC(player);

      assertNull(npc.getComponent(CombatStatsComponent.class));
      assertNull(npc.getComponent(EnemyDeathComponent.class));
    }
  }

  @Nested
  @DisplayName("shop")
  class Shop {

    /** Should add a shopkeeper for the player. */
    @Test
    void shouldAddAShopkeeperForThePlayer() {
      assertNotNull(NPCFactory.createShopNPC(player).getComponent(ShopkeeperComponent.class));
    }

    /** Should ask for shields collected. */
    @Test
    void shouldAskForShieldsCollected() {
      Entity npc = NPCFactory.createShopNPC(player);

      assertEquals(
          "shieldscollectedquest", npc.getComponent(DialogueComponent.class).getQuestType());
    }

    /** Should ask for two. */
    @Test
    void shouldAskForTwo() throws ReflectiveOperationException {
      assertEquals(2, amountToDoOf(NPCFactory.createShopNPC(player)));
    }

    /** Should reward a tier two bow. */
    @Test
    void shouldRewardATierTwoBow() {
      Item item =
          NPCFactory.createShopNPC(player).getComponent(QuestGiverComponent.class).itemToGive;

      WeaponItem weapon = assertInstanceOf(WeaponItem.class, item);
      assertEquals(WeaponType.BOW, weapon.getWeaponType());
      assertEquals(2, weapon.getTier());
    }

    /** Should not give gold. */
    @Test
    void shouldNotGiveGold() {
      assertEquals(
          0, NPCFactory.createShopNPC(player).getComponent(QuestGiverComponent.class).goldToGive);
    }

    /** Should not drop loot. */
    @Test
    void shouldNotDropLoot() {
      Entity npc = NPCFactory.createShopNPC(player);

      assertNull(npc.getComponent(ItemDropComponent.class));
      assertNull(npc.getComponent(InventoryComponent.class));
    }

    /** Should be called hermes. */
    @Test
    void shouldBeCalledHermes() throws ReflectiveOperationException {
      assertEquals("Hermes", speakerOf(NPCFactory.createShopNPC(player)));
    }
  }

  @Nested
  @DisplayName("wizard")
  class Wizard {

    /** Should ask for enemies killed. */
    @Test
    void shouldAskForEnemiesKilled() {
      Entity wizard = NPCFactory.createWizardNPC(player);

      assertEquals("enemiesquest", wizard.getComponent(DialogueComponent.class).getQuestType());
    }

    /** Should ask for three. */
    @Test
    void shouldAskForThree() throws ReflectiveOperationException {
      assertEquals(3, amountToDoOf(NPCFactory.createWizardNPC(player)));
    }

    /** Should reward a tier three sword. */
    @Test
    void shouldRewardATierThreeSword() {
      Item item =
          NPCFactory.createWizardNPC(player).getComponent(QuestGiverComponent.class).itemToGive;

      WeaponItem weapon = assertInstanceOf(WeaponItem.class, item);
      assertEquals(WeaponType.SWORD, weapon.getWeaponType());
      assertEquals(3, weapon.getTier());
    }

    /** Should be provoked within eight units. */
    @Test
    void shouldBeProvokedWithinEightUnits() throws ReflectiveOperationException {
      Entity wizard = NPCFactory.createWizardNPC(player);

      assertEquals(
          8f,
          read(
              wizard.getComponent(ProvokedComponent.class),
              ProvokedComponent.class,
              "calmDownSeconds"));
    }

    /** Should throw potions from six units. */
    @Test
    void shouldThrowPotionsFromSixUnits() throws ReflectiveOperationException {
      Entity wizard = NPCFactory.createWizardNPC(player);
      RetaliateTask retaliate = taskOf(wizard, RetaliateTask.class);

      assertEquals(
          6f,
          read(
              wizard.getComponent(PotionThrowComponent.class),
              PotionThrowComponent.class,
              "range"));
      assertEquals(10, priorityOf(retaliate));
      assertEquals(6f, read(retaliate, RetaliateTask.class, "range"));
      assertEquals("throwPoisonPotion", read(retaliate, RetaliateTask.class, "attackEvent"));
    }

    /** Should drop one gold and one regeneration potion. */
    @Test
    void shouldDropOneGoldAndOneRegenerationPotion() {
      Entity wizard = NPCFactory.createWizardNPC(player);
      InventoryComponent inventory = wizard.getComponent(InventoryComponent.class);

      assertEquals(1, inventory.getGold());
      long potions =
          inventory.getInventorySlots().values().stream()
              .filter(item -> item.getItemType() == ItemType.CONSUMABLE)
              .map(ConsumableItem.class::cast)
              .filter(item -> item.getConsumableType() == ConsumableType.REGENERATION)
              .filter(item -> item.getQuantity() == 1)
              .count();
      assertEquals(1, potions);
      assertNotNull(wizard.getComponent(ItemDropComponent.class));
    }

    /** Should keep the potion retaliation above platform wander and stand still. */
    @Test
    void shouldKeepThePotionRetaliationAbovePlatformWanderAndStandStill()
        throws ReflectiveOperationException {
      Entity wizard = NPCFactory.createWizardNPC(player);

      assertTrue(
          priorityOf(taskOf(wizard, RetaliateTask.class))
              > priorityOf(taskOf(wizard, StandStillTask.class)));
      assertTrue(
          priorityOf(taskOf(wizard, RetaliateTask.class))
              > taskOf(wizard, PlatformWanderTask.class).getPriority());
    }
  }

  @Nested
  @DisplayName("philosopher")
  class Philosopher {

    /** Should give one hundred gold. */
    @Test
    void shouldGiveOneHundredGold() {
      assertEquals(
          100,
          NPCFactory.createPhilosopherNPC(player)
              .getComponent(QuestGiverComponent.class)
              .goldToGive);
    }

    /** Should keep the default quest. */
    @Test
    void shouldKeepTheDefaultQuest() throws ReflectiveOperationException {
      Entity npc = NPCFactory.createPhilosopherNPC(player);

      assertEquals("jumpquest", npc.getComponent(DialogueComponent.class).getQuestType());
      assertEquals(10, amountToDoOf(npc));
    }

    /** Should not attack. */
    @Test
    void shouldNotAttack() throws ReflectiveOperationException {
      Entity npc = NPCFactory.createPhilosopherNPC(player);

      assertNull(npc.getComponent(ProvokedComponent.class));
      assertEquals(0, countTasks(npc, RetaliateTask.class));
    }

    /** Should not drop loot. */
    @Test
    void shouldNotDropLoot() {
      Entity npc = NPCFactory.createPhilosopherNPC(player);

      assertNull(npc.getComponent(ItemDropComponent.class));
      assertNull(npc.getComponent(InventoryComponent.class));
    }
  }

  @Nested
  @DisplayName("satyr")
  class Satyr {
    /** One headbutt animation: its name, seconds per frame and play mode. */
    private record Row(String name, float frameTime, Animation.PlayMode mode) {}

    /** Should register the six headbutt animations. */
    @Test
    void shouldRegisterTheSixHeadbuttAnimations() throws ReflectiveOperationException {
      Map<String, Animation<TextureRegion>> animations =
          animationsOf(
              NPCFactory.createSatyrNPC(player).getComponent(AnimationRenderComponent.class));
      List<Row> rows =
          List.of(
              new Row("windupr", 0.25f, Animation.PlayMode.NORMAL),
              new Row("windupl", 0.25f, Animation.PlayMode.NORMAL),
              new Row("charger", 0.1f, Animation.PlayMode.LOOP),
              new Row("chargel", 0.1f, Animation.PlayMode.LOOP),
              new Row("headbuttr", 0.1f, Animation.PlayMode.NORMAL),
              new Row("headbuttl", 0.1f, Animation.PlayMode.NORMAL));

      for (Row row : rows) {
        Animation<TextureRegion> animation = animations.get(row.name());
        assertNotNull(animation, row.name());
        assertEquals(row.frameTime(), animation.getFrameDuration(), TOLERANCE, row.name());
        assertEquals(row.mode(), animation.getPlayMode(), row.name());
      }
    }

    /** Should be provoked within eight units and forget after one point five. */
    @Test
    void shouldBeProvokedWithinEightUnitsAndForgetAfterOnePointFive()
        throws ReflectiveOperationException {
      ProvokedComponent provoked =
          NPCFactory.createSatyrNPC(player).getComponent(ProvokedComponent.class);

      assertEquals(8f, read(provoked, ProvokedComponent.class, "calmDownSeconds"));
      assertEquals(1.5f, read(provoked, ProvokedComponent.class, "repeatCooldownSeconds"));
    }

    /** Should headbutt the player. */
    @Test
    void shouldHeadbuttThePlayer() throws ReflectiveOperationException {
      HeadbuttAttackComponent headbutt =
          NPCFactory.createSatyrNPC(player).getComponent(HeadbuttAttackComponent.class);

      assertNotNull(headbutt);
      assertSame(player, read(headbutt, HeadbuttAttackComponent.class, "player"));
    }

    /** Should retaliate from four units. */
    @Test
    void shouldRetaliateFromFourUnits() throws ReflectiveOperationException {
      RetaliateTask retaliate = taskOf(NPCFactory.createSatyrNPC(player), RetaliateTask.class);

      assertEquals(10, priorityOf(retaliate));
      assertEquals(4f, read(retaliate, RetaliateTask.class, "range"));
      assertEquals("headbutt", read(retaliate, RetaliateTask.class, "attackEvent"));
    }

    /** Should ask for gold spent. */
    @Test
    void shouldAskForGoldSpent() throws ReflectiveOperationException {
      Entity satyr = NPCFactory.createSatyrNPC(player);

      assertEquals("goldspentquest", satyr.getComponent(DialogueComponent.class).getQuestType());
      assertEquals(50, amountToDoOf(satyr));
    }

    /** Should give one gold. */
    @Test
    void shouldGiveOneGold() {
      assertEquals(
          1, NPCFactory.createSatyrNPC(player).getComponent(QuestGiverComponent.class).goldToGive);
    }
  }

  /** The private static helpers, reached by reflection. One nested class per helper. */
  @Nested
  @DisplayName("helpers")
  class Helpers {

    @Nested
    @DisplayName("createBaseNPC")
    class CreateBaseNPC {

      private Entity base() throws Throwable {
        return (Entity) call("createBaseNPC", new Class<?>[] {Entity.class}, player);
      }

      /** Should add the five body components. */
      @Test
      void shouldAddTheFiveBodyComponents() throws Throwable {
        Entity base = base();

        assertNotNull(base.getComponent(PhysicsComponent.class));
        assertNotNull(base.getComponent(PhysicsMovementComponent.class));
        assertNotNull(base.getComponent(ColliderComponent.class));
        assertNotNull(base.getComponent(HitboxComponent.class));
        assertNotNull(base.getComponent(AITaskComponent.class));
      }

      /** Should put the hitbox on the npc layer. */
      @Test
      void shouldPutTheHitboxOnTheNpcLayer() throws Throwable {
        assertEquals(PhysicsLayer.NPC, base().getComponent(HitboxComponent.class).getLayer());
      }

      /** Should wander and chase the target. */
      @Test
      void shouldWanderAndChaseTheTarget() throws Throwable {
        Entity base = base();

        assertEquals(1, countTasks(base, WanderTask.class));
        assertEquals(1, countTasks(base, ChaseTask.class));
        // ChaseTask.getPriority() depends on a running owner, so read the priority it was built
        // with.
        assertEquals(10, priorityOf(taskOf(base, ChaseTask.class)));
      }

      /** Should move on the ground. */
      @Test
      void shouldMoveOnTheGround() throws Throwable {
        assertTrue(base().getComponent(PhysicsMovementComponent.class).isGroundedMovement());
      }

      /** Should scale the collider to ninety by forty. */
      @Test
      void shouldScaleTheColliderToNinetyByForty() throws Throwable {
        Entity base = base();
        Vector2 size = colliderSize(base);

        assertEquals(base.getScale().x * 0.9f, size.x, TOLERANCE);
        assertEquals(base.getScale().y * 0.4f, size.y, TOLERANCE);
      }
    }

    @Nested
    @DisplayName("createAnimatedNPC")
    class CreateAnimatedNPC {
      private final Class<?>[] types = {Entity.class, String.class, String.class};

      private Entity animated(String name) throws Throwable {
        return (Entity) call("createAnimatedNPC", types, player, name, SHOP_NPC_ATLAS_PATH);
      }

      /** Should load an independent atlas per npc. */
      @Test
      void shouldLoadAnIndependentAtlasPerNpc() throws Throwable {
        AnimationRenderComponent a = animated("A").getComponent(AnimationRenderComponent.class);
        AnimationRenderComponent b = animated("B").getComponent(AnimationRenderComponent.class);

        assertNotSame(
            read(a, AnimationRenderComponent.class, "atlas"),
            read(b, AnimationRenderComponent.class, "atlas"));
      }

      /** Should own its atlas. */
      @Test
      void shouldOwnItsAtlas() throws Throwable {
        AnimationRenderComponent animator =
            animated("A").getComponent(AnimationRenderComponent.class);

        assertEquals(true, read(animator, AnimationRenderComponent.class, "ownsAtlas"));
      }

      /** Should register four looping animations. */
      @Test
      void shouldRegisterFourLoopingAnimations() throws Throwable {
        Map<String, Animation<TextureRegion>> animations =
            animationsOf(animated("A").getComponent(AnimationRenderComponent.class));

        for (String name : new String[] {"walkr", "walkl", "idler", "idlel"}) {
          assertEquals(0.15f, animations.get(name).getFrameDuration(), TOLERANCE, name);
          assertEquals(Animation.PlayMode.LOOP, animations.get(name).getPlayMode(), name);
        }
      }

      /** Should add the skeleton animation controller. */
      @Test
      void shouldAddTheSkeletonAnimationController() throws Throwable {
        assertNotNull(animated("A").getComponent(SkeletonAnimationController.class));
      }

      /** Should pass the speaker name through. */
      @Test
      void shouldPassTheSpeakerNameThrough() throws Throwable {
        assertEquals("Persephone", speakerOf(animated("Persephone")));
      }
    }

    @Nested
    @DisplayName("loadIndependentAtlas")
    class LoadIndependentAtlas {
      private final Class<?>[] types = {String.class};

      /** Should return a different atlas each call. */
      @Test
      void shouldReturnADifferentAtlasEachCall() throws Throwable {
        TextureAtlas a = (TextureAtlas) call("loadIndependentAtlas", types, SHOP_NPC_ATLAS_PATH);
        TextureAtlas b = (TextureAtlas) call("loadIndependentAtlas", types, SHOP_NPC_ATLAS_PATH);

        assertNotSame(a, b);
      }

      /** Should throw for a missing file. */
      @Test
      void shouldThrowForAMissingFile() {
        assertThrows(
            GdxRuntimeException.class,
            () -> call("loadIndependentAtlas", types, "images/npcs/missing.atlas"));
      }
    }

    @Nested
    @DisplayName("addFriendlyNpcParts")
    class AddFriendlyNpcParts {
      private final Class<?>[] types = {Entity.class, Entity.class, String.class};

      private Entity parts() throws Throwable {
        return (Entity) call("addFriendlyNpcParts", types, new Entity(), player, "Name");
      }

      /** Should add the quest and dialogue parts. */
      @Test
      void shouldAddTheQuestAndDialogueParts() throws Throwable {
        Entity npc = parts();

        assertNotNull(npc.getComponent(QuestGiverComponent.class));
        assertNotNull(npc.getComponent(DialogueComponent.class));
        assertNotNull(npc.getComponent(DisplayDialogue.class));
        assertNotNull(npc.getComponent(DialogueProximityComponent.class));
      }

      /** Should start on the jump quest with ten to do. */
      @Test
      void shouldStartOnTheJumpQuestWithTenToDo() throws Throwable {
        Entity npc = parts();

        assertEquals("jumpquest", npc.getComponent(DialogueComponent.class).getQuestType());
        assertEquals(10, amountToDoOf(npc));
      }

      /** Should watch the player from two units. */
      @Test
      void shouldWatchThePlayerFromTwoUnits() throws Throwable {
        assertEquals(
            2f,
            read(
                parts().getComponent(DialogueProximityComponent.class),
                DialogueProximityComponent.class,
                "dialogueDistance"));
      }

      /** Should scale the height. */
      @Test
      void shouldScaleTheHeight() throws Throwable {
        assertEquals(FRIENDLY_NPC_HEIGHT, parts().getScale().y, TOLERANCE);
      }

      /** Should wander around one and a half units. */
      @Test
      void shouldWanderAroundOneAndAHalfUnits() throws Throwable {
        PlatformWanderTask wander = taskOf(parts(), PlatformWanderTask.class);
        Vector2 range = (Vector2) read(wander, WanderTask.class, "wanderRange");

        assertEquals(FRIENDLY_NPC_WANDER_RANGE, range.x, TOLERANCE);
        assertEquals(FRIENDLY_NPC_WANDER_RANGE, range.y, TOLERANCE);
      }

      /** Should keep the floor probe inside the collider. */
      @Test
      void shouldKeepTheFloorProbeInsideTheCollider() throws Throwable {
        PlatformWanderTask wander = taskOf(parts(), PlatformWanderTask.class);
        float probe = (float) read(wander, PlatformWanderTask.class, "rayCastPositionScale");

        assertEquals(0.5f - FRIENDLY_NPC_COLLIDER_WIDTH / 2f, probe, TOLERANCE);
      }

      /** Should stand still near the player. */
      @Test
      void shouldStandStillNearThePlayer() throws Throwable {
        StandStillTask stand = taskOf(parts(), StandStillTask.class);

        assertSame(player, read(stand, StandStillTask.class, "player"));
        assertEquals(FRIENDLY_NPC_STAND_PRIORITY, priorityOf(stand));
      }

      /** Should scale the collider to ninety by seventy. */
      @Test
      void shouldScaleTheColliderToNinetyBySeventy() throws Throwable {
        Entity npc = parts();
        Vector2 size = colliderSize(npc);

        assertEquals(npc.getScale().x * 0.9f, size.x, TOLERANCE);
        assertEquals(npc.getScale().y * 0.7f, size.y, TOLERANCE);
      }

      /** Should move on the ground. */
      @Test
      void shouldMoveOnTheGround() throws Throwable {
        assertTrue(parts().getComponent(PhysicsMovementComponent.class).isGroundedMovement());
      }
    }

    @Nested
    @DisplayName("makeKillable")
    class MakeKillable {
      private final Class<?>[] types = {Entity.class};

      /** Should add hitbox health and death. */
      @Test
      void shouldAddHitboxHealthAndDeath() throws Throwable {
        Entity npc = (Entity) call("makeKillable", types, new Entity());

        assertEquals(PhysicsLayer.NPC, npc.getComponent(HitboxComponent.class).getLayer());
        assertEquals(FRIENDLY_NPC_HEALTH, npc.getComponent(CombatStatsComponent.class).getHealth());
        assertEquals(0, npc.getComponent(CombatStatsComponent.class).getBaseAttack());
        assertNotNull(npc.getComponent(EnemyDeathComponent.class));
      }

      /** Should return the same entity. */
      @Test
      void shouldReturnTheSameEntity() throws Throwable {
        Entity npc = new Entity();

        assertSame(npc, call("makeKillable", types, npc));
      }
    }
  }

  @Nested
  @DisplayName("spawn registry")
  class SpawnRegistry {

    /** Should register all npc spawns. */
    @Test
    void shouldRegisterAllNpcSpawns() {
      NPCFactory.registerNpcSpawns();

      for (String name : new String[] {"npc:shop", "npc:wizard", "npc:philosopher", "npc:satyr"}) {
        assertTrue(EntitySpawnRegistry.isRegistered(name), name);
      }
    }

    /** Should build the right npc for each name. */
    @Test
    void shouldBuildTheRightNpcForEachName() throws ReflectiveOperationException {
      NPCFactory.registerNpcSpawns();
      Map<String, String> speakers =
          Map.of(
              "npc:shop", "Hermes",
              "npc:wizard", "Wizard",
              "npc:philosopher", "Philosopher",
              "npc:satyr", "Satyr");

      for (Map.Entry<String, String> entry : speakers.entrySet()) {
        Entity npc = EntitySpawnRegistry.create(entry.getKey(), player);

        assertNotNull(npc, entry.getKey());
        assertEquals(entry.getValue(), speakerOf(npc), entry.getKey());
      }
    }

    /** Should register exactly four names. */
    @Test
    void shouldRegisterExactlyFourNames() {
      NPCFactory.registerNpcSpawns();

      assertEquals(4, npcNames().size());
    }

    /** Should register no enemy name. */
    @Test
    void shouldRegisterNoEnemyName() {
      NPCFactory.registerNpcSpawns();

      for (String name : new String[] {"skeleton", "zeus", "harpy", "medusa"}) {
        assertFalse(EntitySpawnRegistry.isRegistered(name), name);
      }
    }

    /** Should be safe to call twice. */
    @Test
    void shouldBeSafeToCallTwice() {
      assertDoesNotThrow(
          () -> {
            NPCFactory.registerNpcSpawns();
            NPCFactory.registerNpcSpawns();
          });

      assertEquals(4, npcNames().size());
    }

    /** The registered names that start with "npc:". */
    private Set<String> npcNames() {
      return EntitySpawnRegistry.registeredNames().stream()
          .filter(name -> name.startsWith("npc:"))
          .collect(java.util.stream.Collectors.toSet());
    }
  }

  @Nested
  @DisplayName("structure")
  class Structure {

    /** Should keep enemy creators out of the NPC factory. */
    @Test
    void shouldKeepEnemyCreatorsOutOfNPCFactory() {
      Set<String> enemyCreators =
          Set.of(
              "createSkeleton",
              "createRangedSkeleton",
              "createMinotaur",
              "createCentaur",
              "createCyclops",
              "createMedusa",
              "createCerberus",
              "createZeus",
              "createHarpy",
              "createRangedHarpy");

      for (Method method : NPCFactory.class.getDeclaredMethods()) {
        assertFalse(
            Modifier.isPublic(method.getModifiers()) && enemyCreators.contains(method.getName()),
            method.getName());
      }
    }

    /** Should not be instantiable. */
    @Test
    void shouldNotBeInstantiable() throws ReflectiveOperationException {
      Constructor<NPCFactory> constructor = NPCFactory.class.getDeclaredConstructor();
      constructor.setAccessible(true);

      InvocationTargetException thrown =
          assertThrows(InvocationTargetException.class, constructor::newInstance);

      assertInstanceOf(IllegalStateException.class, thrown.getCause());
    }
  }
}
