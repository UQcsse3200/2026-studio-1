package com.csse3200.game.entities.factories;

import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.ai.tasks.AITaskComponent;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.MeleeAttackComponent;
import com.csse3200.game.components.RangedAttackComponent;
import com.csse3200.game.components.TouchAttackComponent;
import com.csse3200.game.components.npc.DialogueComponent;
import com.csse3200.game.components.npc.DialogueProximityComponent;
import com.csse3200.game.components.npc.DisplayDialogue;
import com.csse3200.game.components.npc.GhostAnimationController;
import com.csse3200.game.components.npc.SkeletonAnimationController;
import com.csse3200.game.components.tasks.ChaseTask;
import com.csse3200.game.components.tasks.MeleeAttackTask;
import com.csse3200.game.components.tasks.PlatformWanderTask;
import com.csse3200.game.components.tasks.RangedAttackTask;
import com.csse3200.game.components.tasks.WanderTask;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.configs.BaseEntityConfig;
import com.csse3200.game.entities.configs.GhostKingConfig;
import com.csse3200.game.entities.configs.NPCConfigs;
import com.csse3200.game.entities.configs.RangedSkeletonConfig;
import com.csse3200.game.entities.configs.SkeletonConfig;
import com.csse3200.game.files.FileLoader;
import com.csse3200.game.physics.PhysicsLayer;
import com.csse3200.game.physics.PhysicsUtils;
import com.csse3200.game.physics.components.ColliderComponent;
import com.csse3200.game.physics.components.HitboxComponent;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.physics.components.PhysicsMovementComponent;
import com.csse3200.game.rendering.AnimationRenderComponent;
import com.csse3200.game.rendering.TextureRenderComponent;
import com.csse3200.game.services.ServiceLocator;

/**
 * Factory to create non-playable character (NPC) entities with predefined components.
 *
 * <p>Each NPC entity type should have a creation method that returns a corresponding entity.
 * Predefined entity properties can be loaded from configs stored as json files which are defined in
 * "NPCConfigs".
 *
 * <p>If needed, this factory can be separated into more specific factories for entities with
 * similar characteristics.
 */
public class NPCFactory {
  private static final NPCConfigs configs =
      FileLoader.readClass(NPCConfigs.class, "configs/NPCs.json");

  /**
   * Creates a ghost entity.
   *
   * @param target entity to chase
   * @return entity
   */
  public static Entity createGhost(Entity target) {
    Entity ghost = createBaseNPC(target);
    BaseEntityConfig config = configs.ghost;

    AnimationRenderComponent animator =
        new AnimationRenderComponent(
            ServiceLocator.getResourceService()
                .getAsset("images/enemies/ghost.atlas", TextureAtlas.class));
    animator.addAnimation("angry_float", 0.1f, Animation.PlayMode.LOOP);
    animator.addAnimation("float", 0.1f, Animation.PlayMode.LOOP);

    ghost
        .addComponent(new CombatStatsComponent(config.health, config.baseAttack))
        .addComponent(animator)
        .addComponent(new GhostAnimationController());

    ghost.getComponent(AnimationRenderComponent.class).scaleEntity();

    return ghost;
  }

  /**
   * Creates a ghost king entity.
   *
   * @param target entity to chase
   * @return entity
   */
  public static Entity createGhostKing(Entity target) {
    Entity ghostKing = createBaseNPC(target);
    GhostKingConfig config = configs.ghostKing;

    AnimationRenderComponent animator =
        new AnimationRenderComponent(
            ServiceLocator.getResourceService()
                .getAsset("images/enemies/ghostKing.atlas", TextureAtlas.class));
    animator.addAnimation("float", 0.1f, Animation.PlayMode.LOOP);
    animator.addAnimation("angry_float", 0.1f, Animation.PlayMode.LOOP);

    ghostKing
        .addComponent(new CombatStatsComponent(config.health, config.baseAttack))
        .addComponent(animator)
        .addComponent(new GhostAnimationController());

    ghostKing.getComponent(AnimationRenderComponent.class).scaleEntity();
    return ghostKing;
  }

  /**
   * Creates a skeleton entity.
   *
   * @param target entity to chase
   * @return entity
   */
  public static Entity createSkeleton(Entity target) {
    float scale = 1.0f;
    Vector2 collisionScale = new Vector2(0.45f, 0.6f);
    Entity skeleton = createBasePlatformerNPC(target, (scale * collisionScale.x) / 2);
    SkeletonConfig config = configs.skeleton;

    AnimationRenderComponent animator =
        new AnimationRenderComponent(
            ServiceLocator.getResourceService()
                .getAsset("images/enemies/skeleton.atlas", TextureAtlas.class));
    animator.addAnimation("idlel", 0.1f, Animation.PlayMode.LOOP);
    animator.addAnimation("idler", 0.1f, Animation.PlayMode.LOOP);
    animator.addAnimation("walkl", 0.1f, Animation.PlayMode.LOOP);
    animator.addAnimation("walkr", 0.1f, Animation.PlayMode.LOOP);

    skeleton
        .addComponent(new CombatStatsComponent(config.health, config.baseAttack))
        .addComponent(
            new MeleeAttackComponent(
                config.melee.range, config.melee.cooldown, config.melee.knockback))
        .addComponent(animator)
        .addComponent(new SkeletonAnimationController());

    skeleton.getComponent(AnimationRenderComponent.class).scaleEntity();
    skeleton.setScale(scale, scale);
    PhysicsUtils.setScaledCollider(skeleton, collisionScale.x, collisionScale.y);
    return skeleton;
  }

  /**
   * Creates a ranged skeleton entity (e.g. an archer-type enemy) that attacks from a distance
   * instead of approaching all the way up to its target.
   *
   * @param target entity to chase
   * @return entity
   */
  public static Entity createRangedSkeleton(Entity target) {
    float scale = 1.0f;
    Vector2 collisionScale = new Vector2(0.45f, 0.6f);
    Entity rangedSkeleton = createBasePlatformerNPC(target, (scale * collisionScale.x) / 2);
    RangedSkeletonConfig config = configs.rangedSkeleton;

    AnimationRenderComponent animator =
        new AnimationRenderComponent(
            ServiceLocator.getResourceService()
                .getAsset("images/enemies/skeleton.atlas", TextureAtlas.class));
    animator.addAnimation("idlel", 0.1f, Animation.PlayMode.LOOP);
    animator.addAnimation("idler", 0.1f, Animation.PlayMode.LOOP);
    animator.addAnimation("walkl", 0.1f, Animation.PlayMode.LOOP);
    animator.addAnimation("walkr", 0.1f, Animation.PlayMode.LOOP);

    rangedSkeleton
        .addComponent(new CombatStatsComponent(config.health, config.baseAttack))
        .addComponent(
            new RangedAttackComponent(
                config.ranged.range, config.ranged.cooldown, config.ranged.knockback))
        .addComponent(animator)
        .addComponent(new SkeletonAnimationController());

    rangedSkeleton.getComponent(AnimationRenderComponent.class).scaleEntity();

    // Attack from range instead of flying/chasing all the way onto the target - see
    // RangedAttackTask's javadoc for why a higher priority than ChaseTask is what achieves this.
    rangedSkeleton
        .getComponent(AITaskComponent.class)
        .addTask(new RangedAttackTask(target, 15, config.ranged.range));

    rangedSkeleton.setScale(scale, scale);
    PhysicsUtils.setScaledCollider(rangedSkeleton, collisionScale.x, collisionScale.y);
    return rangedSkeleton;
  }

  /**
   * Creates a generic NPC to be used as a base entity by more specific NPC creation methods.
   *
   * @return entity
   */
  private static Entity createBaseNPC(Entity target) {
    AITaskComponent aiComponent =
        new AITaskComponent()
            .addTask(new WanderTask(new Vector2(2f, 2f), 2f))
            .addTask(new ChaseTask(target, 10, 3f, 4f));
    Entity npc =
        new Entity()
            .addComponent(new PhysicsComponent())
            .addComponent(new PhysicsMovementComponent())
            .addComponent(new ColliderComponent())
            .addComponent(new HitboxComponent().setLayer(PhysicsLayer.NPC))
            .addComponent(new TouchAttackComponent(PhysicsLayer.PLAYER, 1.5f))
            .addComponent(aiComponent);

    PhysicsUtils.setScaledCollider(npc, 0.9f, 0.4f);
    // Let gravity pull the NPC down instead of the wander/chase AI flying it directly toward
    npc.getComponent(PhysicsMovementComponent.class).setGroundedMovement(true);
    return npc;
  }

  /**
   * Creates a generic NPC that falls with gravity to be used as a base entity by more specific
   * Platformer NPC creation methods.
   *
   * <p>Structure inspired by createBaseNPC function, but removes touch damage in favour of using
   * melee attacks
   *
   * @return entity
   */
  private static Entity createBasePlatformerNPC(Entity target, final float floorCollisionScale) {
    AITaskComponent aiComponent =
        new AITaskComponent()
            .addTask(new PlatformWanderTask(new Vector2(2f, 2f), 2f, floorCollisionScale))
            .addTask(new ChaseTask(target, 10, 3f, 4f))
            .addTask(new MeleeAttackTask(target, 15, 1f));
    Entity npc =
        new Entity()
            .addComponent(new PhysicsComponent())
            .addComponent(new PhysicsMovementComponent())
            .addComponent(new ColliderComponent())
            .addComponent(new HitboxComponent().setLayer(PhysicsLayer.NPC))
            .addComponent(aiComponent);

    PhysicsUtils.setScaledCollider(npc, 0.9f, 0.7f);
    return npc;
  }

  /**
   * Creates a passive traveler NPC that wanders but cannot attack the player.
   *
   * @return entity
   */
  public static Entity createTravelerNPC(Entity player) {
    final float colliderWidthFraction = 0.9f;
    // Matches the player's rendered height: PlayerFactory scales box_boy_leaf.png (792x1000) to
    // width 1, height 1000/792 via TextureRenderComponent.scaleEntity().
    final float playerHeight = 1000f / 792f;
    String[] dialoguetext = {"Hello", "Good luck"};
    Entity npc =
        new Entity()
            .addComponent(new PhysicsComponent())
            .addComponent(new PhysicsMovementComponent())
            .addComponent(new ColliderComponent())
            .addComponent(new HitboxComponent().setLayer(PhysicsLayer.NPC))
            .addComponent(new CombatStatsComponent(50, 0))
            .addComponent(new TextureRenderComponent("images/enemies/npc_traveler.png"))
            // npc dialogue
            .addComponent(new DialogueComponent(dialoguetext))
            .addComponent(new DisplayDialogue("Traveler"))
            .addComponent(new DialogueProximityComponent(player, 2f));

    npc.getComponent(TextureRenderComponent.class).scaleEntity();
    npc.scaleHeight(playerHeight);

    // rayCastPositionScale is a fraction of the entity's own width (0 = sprite edge, 0.5 =
    // centre), so the fraction that lines the raycast up with the collider's edge is
    // 0.5 - colliderWidthFraction / 2, independent of the entity's absolute scale.
    float floorCollisionScale = 0.5f - colliderWidthFraction / 2f;
    // Wander range is a hard guarantee, not just a low-probability-of-falling value: at spawn
    // TRAVELER_NPC_SPAWN=(4, 13) in level1-greek.json, the entity's left edge (position.x) is
    // 1.782, on a floor patch spanning world x=[1.0, 3.0] (wall to the left, a non-solid ladder
    // tile from x=3.0). With this entity's width (0.937) and 0.9-fraction collider (0.843 wide,
    // 0.047 margin each side of the sprite), the tightest constraint is the ladder side: the
    // largest half-range that still keeps the collider's right edge >=0.05 inside the floor at
    // the worst-case wander target is (3.0 - 0.05 - 1.782 - 0.937 + 0.047) = 0.279. Using 0.25
    // (half-range) keeps a comfortable ~0.08 margin from the ladder edge, and an even larger
    // ~0.58 margin from the wall on the left, at the furthest wander position on either side.
    npc.addComponent(
        new AITaskComponent()
            .addTask(new PlatformWanderTask(new Vector2(0.5f, 0.5f), 2f, floorCollisionScale)));

    PhysicsUtils.setScaledCollider(npc, colliderWidthFraction, 0.7f);
    return npc;
  }

  private NPCFactory() {
    throw new IllegalStateException("Instantiating static util class");
  }
}
