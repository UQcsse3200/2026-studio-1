package com.csse3200.game.components.npc;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.Component;
import com.csse3200.game.components.player.ShieldComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.physics.PhysicsLayer;
import com.csse3200.game.physics.components.HitboxComponent;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.rendering.AnimationRenderComponent;
import com.csse3200.game.rendering.SpinningTextureRenderComponent;
import com.csse3200.game.rendering.TextureRenderComponent;
import com.csse3200.game.services.ServiceLocator;

/** Throws a splash poison potion in an arc at a target entity when triggered. */
public class PotionThrowComponent extends Component {
  private static final String POTION_TEXTURE = "images/ui/Poison.png";
  private static final String SPLASH_ATLAS = "images/npcs/poison_splash.atlas";
  private static final float POTION_SIZE = 0.5f;
  private static final float POTION_SPIN_DEGREES_PER_SECOND = 720f;
  private static final float HEAD_GAP = 0.05f;
  private static final float GRAVITY = 15f;
  private static final float ARC_HEIGHT = 0.9f;
  private static final float ARC_CLEARANCE_ABOVE_TARGET = 0.5f;
  private static final float SPLASH_SIZE = 1f;
  private static final float SPLASH_FRAME_SECONDS = 0.1f;
  private static final float SPLASH_RADIUS = 1.5f;
  private static final int POISON_DAMAGE_PER_TICK = 5;
  private static final int POISON_TICKS = 5;
  private static final float POISON_TICK_SECONDS = 2f;
  private static final float POISON_INDICATOR_SIZE = 0.35f;

  private final float range;
  private PoisonEffectComponent activePoison;

  /**
   * Creates a potion throw component with a configurable range.
   *
   * @param range maximum distance between this entity and the target for a throw.
   */
  public PotionThrowComponent(float range) {
    this.range = range;
  }

  /** Registers a listener for the throw-trigger event. */
  @Override
  public void create() {
    entity.getEvents().addListener("throwPoisonPotion", this::attemptThrow);
  }

  /**
   * Throws a potion that rises from above this entity's head and lands at the target's feet.
   *
   * @param target the entity being aimed at
   */
  private void attemptThrow(Entity target) {
    // range check
    if (target == null || entity.getPosition().dst(target.getPosition()) > range) {
      return;
    }

    Vector2 spawnCentre =
        new Vector2(
            entity.getCenterPosition().x,
            entity.getPosition().y + entity.getScale().y + HEAD_GAP + POTION_SIZE / 2f);
    Vector2 landing = new Vector2(target.getCenterPosition().x, target.getPosition().y);
    float dx = landing.x - spawnCentre.x;
    float dy = landing.y - spawnCentre.y;

    float apex = Math.max(ARC_HEIGHT, dy + ARC_CLEARANCE_ABOVE_TARGET);
    float upSpeed = (float) Math.sqrt(2f * GRAVITY * apex);
    float flightSeconds = upSpeed / GRAVITY + (float) Math.sqrt(2f * (apex - dy) / GRAVITY);
    Vector2 launchVelocity = new Vector2(dx / flightSeconds, upSpeed);

    Entity potion =
        new Entity()
            .addComponent(
                new SpinningTextureRenderComponent(POTION_TEXTURE, POTION_SPIN_DEGREES_PER_SECOND))
            .addComponent(new PhysicsComponent())
            .addComponent(new HitboxComponent().setLayer(PhysicsLayer.DEFAULT))
            .addComponent(
                new SplashPotionComponent(launchVelocity, smashAt -> onSmash(smashAt, target)));
    potion.setPosition(spawnCentre.x - POTION_SIZE / 2f, spawnCentre.y - POTION_SIZE / 2f);
    potion.setScale(POTION_SIZE, POTION_SIZE);
    ServiceLocator.getEntityService().register(potion);
  }

  /** Plays the splash, then poisons the target if it is inside the splash and not shielded. */
  private void onSmash(Vector2 smashAt, Entity target) {
    spawnSplash(smashAt);

    if (target.isDisposed() || target.getCenterPosition().dst(smashAt) > SPLASH_RADIUS) {
      return;
    }
    CombatStatsComponent stats = target.getComponent(CombatStatsComponent.class);
    if (stats == null) {
      return;
    }
    if (stats.getShieldHits() > 0) {
      stats.setShieldHits(stats.getShieldHits() - 1);
      return;
    }
    ShieldComponent shield = target.getComponent(ShieldComponent.class);
    if (shield != null && shield.isActive()) {
      return;
    }
    poison(target);
  }

  /** Spawns a one-shot splash animation on the surface the potion hit. */
  private void spawnSplash(Vector2 smashAt) {
    AnimationRenderComponent animator =
        new AnimationRenderComponent(new TextureAtlas(Gdx.files.internal(SPLASH_ATLAS)), true);
    animator.addAnimation("splash", SPLASH_FRAME_SECONDS, Animation.PlayMode.NORMAL);

    Entity splash =
        new Entity().addComponent(animator).addComponent(new SplashEffectComponent("splash"));
    splash.setScale(SPLASH_SIZE, SPLASH_SIZE);
    splash.setPosition(smashAt.x - SPLASH_SIZE / 2f, smashAt.y - POTION_SIZE / 2f);
    ServiceLocator.getEntityService().register(splash);
  }

  /** Starts a poison on the target, or restarts the running one. */
  private void poison(Entity target) {
    if (activePoison != null && activePoison.isRunning()) {
      activePoison.restart();
      return;
    }
    activePoison =
        new PoisonEffectComponent(
            target, POISON_DAMAGE_PER_TICK, POISON_TICKS, POISON_TICK_SECONDS);
    Entity poisonEntity =
        new Entity()
            .addComponent(new TextureRenderComponent(POTION_TEXTURE))
            .addComponent(activePoison);
    poisonEntity.setScale(POISON_INDICATOR_SIZE, POISON_INDICATOR_SIZE);
    ServiceLocator.getEntityService().register(poisonEntity);
  }
}
