package com.csse3200.game.components.player;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.components.loot.Item;
import com.csse3200.game.components.loot.WeaponItem;
import com.csse3200.game.components.loot.WeaponType;
import com.csse3200.game.rendering.RenderComponent;
import com.csse3200.game.services.ServiceLocator;

// Renders the equipped weapon beside the player and provides a basic sword swing animation.
public class WeaponRenderComponent extends RenderComponent {
  private static final float SWORD_WIDTH = 0.3f;
  private static final float SWORD_HEIGHT = 0.7f;

  private static final float BOW_WIDTH = 0.5f;
  private static final float BOW_HEIGHT = 0.75f;

  private static final float SWING_DURATION = 0.3f;

  private static final float WALK_BOB_SPEED = 10f;
  private static final float WALK_BOB_AMOUNT = 0.025f;
  private static final float WALK_SWAY_AMOUNT = 5f;

  private static final float RIGHT_HAND_OFFSET_X = 0.75f;
  private static final float LEFT_HAND_OFFSET_X = 0.15f;
  private static final float HAND_OFFSET_Y = 0.35f;

  private Texture texture;
  private boolean isBow;
  private final Vector2 handAnchor = new Vector2();

  private boolean facingRight = true;
  private boolean swinging;
  private float swingTime;
  private float walkAnimationTime;
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

  private void updateFacing(Vector2 direction) {
    if (direction == null || direction.isZero()) {
      return;
    }

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

    if (!swinging) {
      return;
    }

    swingTime += deltaTime;

    if (swingTime >= SWING_DURATION) {
      swinging = false;
      swingTime = 0f;
    }
  }

  @Override
  protected void draw(SpriteBatch batch) {
    if (texture == null) {
      return;
    }

    Vector2 playerPosition = entity.getPosition();
    Vector2 playerScale = entity.getScale();

    float handOffsetX = facingRight ? RIGHT_HAND_OFFSET_X : LEFT_HAND_OFFSET_X;

    handAnchor.set(
        playerPosition.x + playerScale.x * handOffsetX,
        playerPosition.y + playerScale.y * HAND_OFFSET_Y);

    float weaponWidth = isBow ? BOW_WIDTH : SWORD_WIDTH;
    float weaponHeight = isBow ? BOW_HEIGHT : SWORD_HEIGHT;

    float weaponX = handAnchor.x - weaponWidth / 2f;
    float weaponY = handAnchor.y - weaponHeight / 2f;

    float rotation = 0f;

    if (!isBow) {
      float bobOffset = (float) Math.sin(walkAnimationTime) * WALK_BOB_AMOUNT;
      float sway = (float) Math.sin(walkAnimationTime) * WALK_SWAY_AMOUNT;

      weaponY += bobOffset;
      rotation += sway;
    }

    if (swinging) {
      float progress = swingTime / SWING_DURATION;
      float swingAngle = -45f + (90f * progress);
      rotation = swingAngle;
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
        !facingRight,
        false);
  }

  public Vector2 getHandAnchor() {
    return handAnchor.cpy();
  }

  public boolean isFacingRight() {
    return facingRight;
  }

  @Override
  public float getZIndex() {
    return -entity.getPosition().y + 0.01f;
  }
}
