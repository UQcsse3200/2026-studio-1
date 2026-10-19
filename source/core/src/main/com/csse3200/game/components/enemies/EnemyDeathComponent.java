package com.csse3200.game.components.enemies;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.csse3200.game.Quests.Quest;
import com.csse3200.game.ai.tasks.AITaskComponent;
import com.csse3200.game.components.Component;
import com.csse3200.game.components.enemies.attacks.ChargeComponent;
import com.csse3200.game.components.enemies.attacks.MeleeAttackComponent;
import com.csse3200.game.components.enemies.attacks.RangedAttackComponent;
import com.csse3200.game.components.enemies.attacks.TouchAttackComponent;
import com.csse3200.game.components.player.InventoryComponent;
import com.csse3200.game.components.player.ItemDropComponent;
import com.csse3200.game.perks.BonusLootDrop;
import com.csse3200.game.physics.PhysicsLayer;
import com.csse3200.game.physics.components.ColliderComponent;
import com.csse3200.game.physics.components.HitboxComponent;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.physics.components.PhysicsMovementComponent;
import com.csse3200.game.rendering.AnimationRenderComponent;
import com.csse3200.game.rendering.EnemyWeaponAnimationComponent;
import com.csse3200.game.services.ServiceLocator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EnemyDeathComponent extends Component {
  private static final Logger logger = LoggerFactory.getLogger(EnemyDeathComponent.class);
  private static final float MAX_DEATH_ANIMATION_DURATION = 4.0f;

  private boolean dying = false;
  private float deathTimer = 0f;

  @Override
  public void create() {
    entity.getEvents().addListener("death", this::onDeathWrapper);
  }

  @Override
  public void update() {
    if (!dying) {
      return;
    }
    float delta = 0f;
    if (ServiceLocator.getTimeSource() != null) {
      delta = ServiceLocator.getTimeSource().getDeltaTime();
    } else if (Gdx.graphics != null) {
      delta = Gdx.graphics.getDeltaTime();
    }
    deathTimer += delta;

    AnimationRenderComponent animator = entity.getComponent(AnimationRenderComponent.class);
    if (animator == null || animator.isFinished() || deathTimer >= MAX_DEATH_ANIMATION_DURATION) {
      dying = false;
      disposeEntity();
    }
  }

  private void onDeathWrapper() {
    Gdx.app.postRunnable(this::onDeath);
  }

  private void onDeath() {
    if (entity.isDisposed()) {
      logger.info("Enemy: {} already died.", getLabel());
      return;
    }
    Quest.incrementGlobalEnemiesKilled();

    dropLoot();
    BonusLootDrop.tryBonusDrop(entity);

    logger.info(
        "Enemy {} died at x:{} y:{}",
        getLabel(),
        entity.getCenterPosition().x,
        entity.getCenterPosition().y);

    AnimationRenderComponent animator = entity.getComponent(AnimationRenderComponent.class);
    if (!isDeathAnimationPlaying(animator) || animator.isFinished()) {
      entity.dispose();
      return;
    }

    startDyingSequence();
  }

  private void dropLoot() {
    ItemDropComponent dropper = entity.getComponent(ItemDropComponent.class);
    if (dropper == null) {
      return;
    }
    if (dropper.dropGold()) {
      InventoryComponent inventory = entity.getComponent(InventoryComponent.class);
      int goldNum = (inventory != null) ? inventory.getGold() : 0;
      logger.info("Enemy {} dropped {} gold", getLabel(), goldNum);
    }
    while (dropper.dropFirstStack()) {
      logger.info("Enemy {} dropped item", getLabel());
    }
  }

  private boolean isDeathAnimationPlaying(AnimationRenderComponent animator) {
    if (animator == null) {
      return false;
    }
    String current = animator.getCurrentAnimation();
    return current != null && (current.contains("death") || current.contains("dead"));
  }

  private void startDyingSequence() {
    dying = true;
    deathTimer = 0f;

    PhysicsComponent physics = entity.getComponent(PhysicsComponent.class);
    if (physics != null && physics.getBody() != null) {
      Body body = physics.getBody();
      try {
        body.setLinearVelocity(0f, 0f);
        body.setType(BodyDef.BodyType.StaticBody);
      } catch (Exception e) {
        logger.debug("Could not set body type to static on death: {}", e.getMessage());
      }
    }

    ColliderComponent collider = entity.getComponent(ColliderComponent.class);
    if (collider != null) {
      collider.setSensor(true);
    }

    HitboxComponent hitbox = entity.getComponent(HitboxComponent.class);
    if (hitbox != null) {
      hitbox.setLayer(PhysicsLayer.NONE);
    }

    disableComponent(AITaskComponent.class);
    disableComponent(PhysicsMovementComponent.class);
    disableComponent(TouchAttackComponent.class);
    disableComponent(HazardContactDamageComponent.class);
    disableComponent(MeleeAttackComponent.class);
    disableComponent(RangedAttackComponent.class);
    disableComponent(ChargeComponent.class);

    EnemyWeaponAnimationComponent weaponAnim =
        entity.getComponent(EnemyWeaponAnimationComponent.class);
    if (weaponAnim != null) {
      weaponAnim.stopAnimation();
    }
  }

  private <T extends Component> void disableComponent(Class<T> type) {
    T comp = entity.getComponent(type);
    if (comp != null) {
      comp.setEnabled(false);
    }
  }

  private void disposeEntity() {
    if (Gdx.app != null) {
      Gdx.app.postRunnable(entity::dispose);
    } else {
      entity.dispose();
    }
  }

  public boolean isDying() {
    return dying;
  }

  /** Label for log messages. Friendly NPCs have no EnemyTypeComponent, so they log as "NPC". */
  private String getLabel() {
    EnemyTypeComponent type = entity.getComponent(EnemyTypeComponent.class);
    return type == null ? "NPC" : type.getEnemyLabel();
  }
}
