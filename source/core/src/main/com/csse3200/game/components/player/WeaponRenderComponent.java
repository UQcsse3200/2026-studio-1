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

// Renders the equipped weapon beside the player and provides a basic sword swing animation.
public class WeaponRenderComponent extends RenderComponent {
  private static final float SWORD_WIDTH = 0.3f;
  private static final float SWORD_HEIGHT = 0.7f;

  private static final float BOW_WIDTH = 0.5f;
  private static final float BOW_HEIGHT = 0.75f;

  private static final float DAGGER_WIDTH = 0.25f;
  private static final float DAGGER_HEIGHT = 0.5f;

  private static final float SWING_DURATION = 0.3f;

  private static final float WALK_BOB_SPEED = 10f;
  private static final float WALK_BOB_AMOUNT = 0.025f;
  private static final float WALK_SWAY_AMOUNT = 5f;

  private static final float BOW_DRAW_DURATION = 0.3f;
  private static final float BOW_DRAW_DISTANCE = 0.12f;

  private static final float RIGHT_HAND_OFFSET_X = 0.75f;
  private static final float LEFT_HAND_OFFSET_X = 0.15f;
  private static final float HAND_OFFSET_Y = 0.35f;

  private static final float DAGGER_THROW_DURATION = 1.5f;

  private Texture texture;
  private boolean isBow;
  private final Vector2 handAnchor = new Vector2();
  private final Vector2 aimDirection = new Vector2(1f, 0f);

  private boolean facingRight = true;
  private boolean swinging;
  private float swingTime;
  private float walkAnimationTime;

  private boolean daggerThrown;
  private float daggerThrowTime;

  private boolean drawingBow;
  private float bowDrawTime;

  private final Vector2 previousPosition = new Vector2();

  public WeaponRenderComponent() {
    texture = null;
    isBow = false;
  }

  @Override
  public void create() {
    super.create();

    previousPosition.set(entity.getPosition());

    entity.getEvents().addListener("swordAttack", this::startSwing);
    entity.getEvents().addListener("weaponAttack", this::handleWeaponAttack);
    entity.getEvents().addListener("walk", this::updateFacing);
    entity.getEvents().addListener("activeSlotChanged", this::updateWeapon);

    updateWeapon(entity.getComponent(InventoryComponent.class).getActiveSlot());
  }

  private void updateWeapon(int activeSlot) {
    InventoryComponent inventory = entity.getComponent(InventoryComponent.class);
    Item item = inventory.getItem(activeSlot);

    if (!(item instanceof WeaponItem weaponItem)) {
      texture = null;
      return;
    }

    if (weaponItem.getWeaponType() == WeaponType.BOW) {
      texture = ServiceLocator.getResourceService().getAsset("images/bow.png", Texture.class);
      isBow = true;
    } else if (weaponItem.getWeaponType() == WeaponType.DAGGER) {
      texture = ServiceLocator.getResourceService().getAsset("images/dagger.png", Texture.class);
      isBow = false;
    } else {
      texture = ServiceLocator.getResourceService().getAsset("images/sword.png", Texture.class);
      isBow = false;
    }
  }

  private void startSwing(int damage) {
    if (isBow) {
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

    if (weaponItem.getWeaponType() == WeaponType.BOW) {
      drawingBow = true;
      bowDrawTime = 0f;
    } else if (weaponItem.getWeaponType() == WeaponType.DAGGER) {
      if (weaponItem.getQuantity() <= 0) {
        return;
      }

      daggerThrown = true;
      daggerThrowTime = 0f;
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

    if (moving && !isBow) {
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
    if (texture == null || daggerThrown) {
      return;
    }

    updateHandAnchor();

    Item activeItem = entity.getComponent(InventoryComponent.class).getActiveItem();
    boolean isDagger = isDaggerWeapon(activeItem);

    float weaponWidth = getWeaponWidth(isDagger);
    float weaponHeight = getWeaponHeight(isDagger);

    float weaponX = handAnchor.x - weaponWidth / 2f;
    float weaponY = handAnchor.y - weaponHeight / 2f;

    if (isBow && drawingBow) {
      float progress = bowDrawTime / BOW_DRAW_DURATION;
      float pullProgress = getBowPullProgress(progress);

      weaponX -= aimDirection.x * BOW_DRAW_DISTANCE * pullProgress;
      weaponY -= aimDirection.y * BOW_DRAW_DISTANCE * pullProgress;
    }

    float rotation = getWeaponRotation();

    if (!isBow) {
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
        1f,
        1f,
        rotation,
        0,
        0,
        texture.getWidth(),
        texture.getHeight(),
        !facingRight && !isBow,
        false);
  }

  private void updateHandAnchor() {
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

  private boolean isDaggerWeapon(Item activeItem) {
    if (activeItem instanceof WeaponItem weaponItem) {
      return weaponItem.getWeaponType() == WeaponType.DAGGER;
    }

    return false;
  }

  private float getWeaponWidth(boolean isDagger) {
    if (isBow) {
      return BOW_WIDTH;
    }

    if (isDagger) {
      return DAGGER_WIDTH;
    }

    return SWORD_WIDTH;
  }

  private float getWeaponHeight(boolean isDagger) {
    if (isBow) {
      return BOW_HEIGHT;
    }

    if (isDagger) {
      return DAGGER_HEIGHT;
    }

    return SWORD_HEIGHT;
  }

  private float getBowPullProgress(float progress) {
    if (progress < 0.5f) {
      return progress * 2f;
    }

    return (1f - progress) * 2f;
  }

  private float getWeaponRotation() {
    if (isBow) {
      return MathUtils.atan2(aimDirection.y, aimDirection.x) * MathUtils.radiansToDegrees - 90f;
    }

    return (float) Math.sin(walkAnimationTime) * WALK_SWAY_AMOUNT;
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

  @Override
  public float getZIndex() {
    return -entity.getPosition().y + 0.01f;
  }
}
