package com.csse3200.game.components.player;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.components.loot.Item;
import com.csse3200.game.components.loot.WeaponItem;
import com.csse3200.game.components.loot.WeaponType;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.factories.DaggerFactory;
import com.csse3200.game.rendering.RenderComponent;
import com.csse3200.game.services.ServiceLocator;

/**
 * Renders the equipped weapon beside the player and handles weapon-specific visual animations such
 * as sword swings, bow drawing and dagger throwing.
 *
 * <p>The renderer keeps track of both the current weapon type and tier so upgraded weapon visuals
 * can be introduced without changing the rendering flow itself.
 */
public class WeaponRenderComponent extends RenderComponent {

  // Base weapon dimensions.
  private static final float SWORD_WIDTH = 0.3f;
  private static final float SWORD_HEIGHT = 0.7f;

  private static final float BOW_WIDTH = 0.5f;
  private static final float BOW_HEIGHT = 0.75f;

  private static final float DAGGER_WIDTH = 0.25f;
  private static final float DAGGER_HEIGHT = 0.5f;

  private static final float AXE_WIDTH = 0.25f;
  private static final float AXE_HEIGHT = 0.5f;

  // Animation durations.
  private static final float SWING_DURATION = 0.3f;
  private static final float BOW_DRAW_DURATION = 0.3f;
  private static final float DAGGER_THROW_DURATION = 1.5f;

  // Walking animation.
  private static final float WALK_BOB_SPEED = 10f;
  private static final float WALK_BOB_AMOUNT = 0.025f;
  private static final float WALK_SWAY_AMOUNT = 5f;

  // Bow animation.
  private static final float BOW_DRAW_DISTANCE = 0.12f;

  // Weapon hand positioning.
  private static final float RIGHT_HAND_OFFSET_X = 0.75f;
  private static final float LEFT_HAND_OFFSET_X = 0.15f;
  private static final float HAND_OFFSET_Y = 0.75f;

  private Texture texture;

  /** Currently equipped weapon type. */
  private WeaponType currentWeaponType;

  /** Currently equipped weapon tier. */
  private int currentWeaponTier;

  private final Vector2 handAnchor = new Vector2();
  private final Vector2 aimDirection = new Vector2(1f, 0f);

  private boolean facingRight = true;

  // Sword attack animation.
  private boolean swinging;
  private float swingTime;

  // Walking animation.
  private float walkAnimationTime;

  // Dagger throw animation.
  private boolean daggerThrown;
  private float daggerThrowTime;

  // Bow draw animation.
  private boolean drawingBow;
  private float bowDrawTime;

  private final Vector2 previousPosition = new Vector2();

  public WeaponRenderComponent() {
    texture = null;
    currentWeaponType = null;
    currentWeaponTier = 1;
  }

  @Override
  public void create() {
    super.create();

    previousPosition.set(entity.getPosition());

    entity.getEvents().addListener("swordAttack", this::startSwing);
    entity.getEvents().addListener("weaponAttack", this::handleWeaponAttack);
    entity.getEvents().addListener("walk", this::updateFacing);
    entity.getEvents().addListener("activeSlotChanged", this::updateWeapon);
    entity
        .getEvents()
        .addListener(
            "inventoryChanged",
            () -> updateWeapon(entity.getComponent(InventoryComponent.class).getActiveSlot()));

    updateWeapon(entity.getComponent(InventoryComponent.class).getActiveSlot());
  }

  /**
   * Updates the currently rendered weapon from the active inventory slot.
   *
   * <p>The weapon type and tier are stored separately from the texture so upgraded weapon visuals
   * can depend on tier.
   */
  private void updateWeapon(int activeSlot) {
    InventoryComponent inventory = entity.getComponent(InventoryComponent.class);
    Item item = inventory.getItem(activeSlot);

    resetAnimationState();

    if (!(item instanceof WeaponItem weaponItem)) {
      texture = null;
      currentWeaponType = null;
      currentWeaponTier = 1;
      return;
    }

    currentWeaponType = weaponItem.getWeaponType();
    currentWeaponTier = weaponItem.getTier();

    texture = getWeaponTexture(currentWeaponType, currentWeaponTier);
  }

  /**
   * Returns the texture associated with the supplied weapon type and tier.
   *
   * <p>Swords use different textures for each upgrade tier, while bows and daggers continue to use
   * their existing textures.
   */
  private Texture getWeaponTexture(WeaponType weaponType, int tier) {
    if (weaponType == null) {
      return null;
    }

    return switch (weaponType) {
      case BOW ->
          ServiceLocator.getResourceService().getAsset("images/items/bow.png", Texture.class);

      case DAGGER ->
          ServiceLocator.getResourceService().getAsset("images/dagger.png", Texture.class);

      case SWORD -> getSwordTexture(tier);

      case AXE ->
          ServiceLocator.getResourceService().getAsset("images/items/axe.png", Texture.class);
    };
  }

  /**
   * Returns the sword texture for the current tier.
   *
   * <p>Tier 1 keeps the original sword. Tier 2 and Tier 3 use the new upgraded sword assets.
   */
  private Texture getSwordTexture(int tier) {
    String swordPath =
        switch (tier) {
          case 2 -> "images/items/sword_t2.png";
          case 3 -> "images/items/sword_t3.png";
          default -> "images/items/sword.png";
        };

    return ServiceLocator.getResourceService().getAsset(swordPath, Texture.class);
  }

  /**
   * Resets active weapon animations when switching weapons.
   *
   * <p>This prevents an animation from the previous weapon continuing after the player changes
   * weapon.
   */
  private void resetAnimationState() {
    swinging = false;
    swingTime = 0f;

    drawingBow = false;
    bowDrawTime = 0f;

    daggerThrown = false;
    daggerThrowTime = 0f;
  }

  private void startSwing(int damage) {
    if (!isCurrentWeapon(WeaponType.SWORD)) {
      return;
    }

    swinging = true;
    swingTime = 0f;
  }

  private void handleWeaponAttack() {
    InventoryComponent inventory = entity.getComponent(InventoryComponent.class);
    Item item = inventory.getActiveItem();

    if (!(item instanceof WeaponItem weaponItem)) {
      return;
    }

    switch (weaponItem.getWeaponType()) {
      case BOW:
        drawingBow = true;
        bowDrawTime = 0f;
        break;

      case DAGGER:
        if (weaponItem.getQuantity() <= 0) {
          return;
        }

        daggerThrown = true;
        daggerThrowTime = 0f;
        break;

      case SWORD:
        // Sword animation is triggered through the swordAttack event.
        break;
    }
  }

  private void throwDagger() {
    Vector2 playerPosition = entity.getPosition();
    Vector2 playerScale = entity.getScale();

    float handOffsetX = getHandOffsetX();

    handAnchor.set(
        playerPosition.x + playerScale.x * handOffsetX,
        playerPosition.y + playerScale.y * HAND_OFFSET_Y);

    Entity dagger = DaggerFactory.createDagger(handAnchor, aimDirection);

    ServiceLocator.getEntityService().register(dagger);
  }

  private void updateFacing(Vector2 direction) {
    if (direction == null || direction.isZero()) {
      return;
    }

    aimDirection.set(direction).nor();

    if (direction.x != 0) {
      facingRight = direction.x > 0;
    }
  }

  @Override
  public void update() {
    float deltaTime = ServiceLocator.getTimeSource().getDeltaTime();

    Vector2 currentPosition = entity.getPosition();
    boolean moving = !currentPosition.epsilonEquals(previousPosition, 0.001f);

    if (moving && !isCurrentWeapon(WeaponType.BOW)) {
      walkAnimationTime += deltaTime * WALK_BOB_SPEED;
    } else if (!moving) {
      walkAnimationTime = 0f;
    }

    previousPosition.set(currentPosition);

    if (swinging) {
      swingTime += deltaTime;

      if (swingTime >= SWING_DURATION) {
        swinging = false;
        swingTime = 0f;
      }
    }

    if (drawingBow) {
      bowDrawTime += deltaTime;

      if (bowDrawTime >= BOW_DRAW_DURATION) {
        drawingBow = false;
        bowDrawTime = 0f;
      }
    }

    if (daggerThrown) {
      daggerThrowTime += deltaTime;

      if (daggerThrowTime >= DAGGER_THROW_DURATION) {
        daggerThrown = false;
        daggerThrowTime = 0f;
      }
    }
  }

  @Override
  protected void draw(SpriteBatch batch) {
    if (texture == null || daggerThrown || currentWeaponType == null) {
      return;
    }

    WeaponVisualConfig visualConfig = getVisualConfig();

    updateHandAnchor(visualConfig);

    float weaponWidth = visualConfig.width;
    float weaponHeight = visualConfig.height;

    float weaponX = handAnchor.x - weaponWidth / 2f;
    float weaponY = handAnchor.y - weaponHeight / 2f;

    if (isCurrentWeapon(WeaponType.BOW) && drawingBow) {
      float progress = bowDrawTime / BOW_DRAW_DURATION;
      float pullProgress = getBowPullProgress(progress);

      weaponX -= aimDirection.x * BOW_DRAW_DISTANCE * pullProgress;
      weaponY -= aimDirection.y * BOW_DRAW_DISTANCE * pullProgress;
    }

    float rotation = getWeaponRotation();

    if (!isCurrentWeapon(WeaponType.BOW)) {
      float bobOffset = (float) Math.sin(walkAnimationTime) * WALK_BOB_AMOUNT;
      weaponY += bobOffset;
    }

    if (swinging) {
      float progress = swingTime / SWING_DURATION;
      rotation = -45f + (90f * progress);
    }

    batch.draw(
        texture,
        weaponX,
        weaponY,
        weaponWidth / 2f,
        weaponHeight / 2f,
        weaponWidth,
        weaponHeight,
        visualConfig.scaleX,
        visualConfig.scaleY,
        rotation,
        0,
        0,
        texture.getWidth(),
        texture.getHeight(),
        visualConfig.flipX && !facingRight,
        false);
  }

  /**
   * Returns the visual configuration for the currently equipped weapon.
   *
   * <p>Tier is used to provide different visual sizes for upgraded weapons.
   */
  private WeaponVisualConfig getVisualConfig() {
    if (currentWeaponType == null) {
      return new WeaponVisualConfig(0f, 0f, 1f, 1f, false);
    }

    return switch (currentWeaponType) {
      case SWORD -> getSwordVisualConfig(currentWeaponTier);
      case BOW -> getBowVisualConfig(currentWeaponTier);
      case DAGGER -> getDaggerVisualConfig(currentWeaponTier);
      case AXE -> getAxeVisualConfig(currentWeaponTier);
    };
  }

  private WeaponVisualConfig getSwordVisualConfig(int tier) {
    float scale =
        switch (tier) {
          case 2 -> 1.20f;
          case 3 -> 1.45f;
          default -> 1.00f;
        };

    return new WeaponVisualConfig(SWORD_WIDTH, SWORD_HEIGHT, scale, scale, true);
  }

  private WeaponVisualConfig getBowVisualConfig(int tier) {
    float scale =
        switch (tier) {
          case 2 -> 1.10f;
          case 3 -> 1.20f;
          default -> 1.00f;
        };

    return new WeaponVisualConfig(BOW_WIDTH, BOW_HEIGHT, scale, scale, false);
  }

  private WeaponVisualConfig getDaggerVisualConfig(int tier) {
    float scale =
        switch (tier) {
          case 2 -> 1.15f;
          case 3 -> 1.30f;
          default -> 1.00f;
        };

    return new WeaponVisualConfig(DAGGER_WIDTH, DAGGER_HEIGHT, scale, scale, true);
  }

  private WeaponVisualConfig getAxeVisualConfig(int tier) {
    float scale =
        switch (tier) {
          case 2 -> 1.15f;
          case 3 -> 1.30f;
          default -> 1.00f;
        };

    return new WeaponVisualConfig(AXE_WIDTH, AXE_HEIGHT, scale, scale, true);
  }

  private void updateHandAnchor(WeaponVisualConfig visualConfig) {
    Vector2 playerPosition = entity.getPosition();
    Vector2 playerScale = entity.getScale();

    float handOffsetX = getHandOffsetX();

    handAnchor.set(
        playerPosition.x + playerScale.x * handOffsetX,
        playerPosition.y + playerScale.y * HAND_OFFSET_Y);
  }

  private float getHandOffsetX() {
    if (facingRight) {
      return RIGHT_HAND_OFFSET_X;
    }

    return LEFT_HAND_OFFSET_X;
  }

  private float getBowPullProgress(float progress) {
    if (progress < 0.5f) {
      return progress * 2f;
    }

    return (1f - progress) * 2f;
  }

  private float getWeaponRotation() {
    if (isCurrentWeapon(WeaponType.BOW)) {
      return MathUtils.atan2(aimDirection.y, aimDirection.x) * MathUtils.radiansToDegrees - 90f;
    }

    return (float) Math.sin(walkAnimationTime) * WALK_SWAY_AMOUNT;
  }

  private boolean isCurrentWeapon(WeaponType weaponType) {
    return currentWeaponType == weaponType;
  }

  public Vector2 getHandAnchor() {
    return handAnchor.cpy();
  }

  public boolean isFacingRight() {
    return facingRight;
  }

  public Vector2 getAimDirection() {
    return aimDirection.cpy();
  }

  /**
   * Returns the currently rendered weapon tier.
   *
   * @return current weapon tier, or 1 when no weapon is equipped
   */
  public int getCurrentWeaponTier() {
    return currentWeaponTier;
  }

  /**
   * Returns the currently rendered weapon type.
   *
   * @return current weapon type, or null when no weapon is equipped
   */
  public WeaponType getCurrentWeaponType() {
    return currentWeaponType;
  }

  @Override
  public float getZIndex() {
    return -entity.getPosition().y + 0.01f;
  }

  /** Stores the visual properties needed to render a weapon. */
  private static class WeaponVisualConfig {
    private final float width;
    private final float height;
    private final float scaleX;
    private final float scaleY;
    private final boolean flipX;

    private WeaponVisualConfig(
        float width, float height, float scaleX, float scaleY, boolean flipX) {
      this.width = width;
      this.height = height;
      this.scaleX = scaleX;
      this.scaleY = scaleY;
      this.flipX = flipX;
    }
  }
}
