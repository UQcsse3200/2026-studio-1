package com.csse3200.game.rendering;

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.components.pet.PetType;
import com.csse3200.game.physics.components.PhysicsComponent;

/** Draws small procedural pet projectiles, with no external sprite assets. */
public class PetProjectileRenderComponent extends RenderComponent {
  private final PetType type;
  private Texture texture;
  private PhysicsComponent physics;

  /** Selects the dart, diamond, or orb artwork for this companion. */
  public PetProjectileRenderComponent(PetType type) {
    if (type == null) {
      throw new IllegalArgumentException("Projectile art requires a pet type");
    }
    this.type = type;
  }

  @Override
  public void create() {
    Pixmap pixels = createPixmap(type);
    try {
      texture = new Texture(pixels);
      texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
    } finally {
      pixels.dispose();
    }
    physics = entity.getComponent(PhysicsComponent.class);
    super.create();
  }

  @Override
  protected void draw(SpriteBatch batch) {
    if (texture == null || entity.isDisposed()) {
      return;
    }
    Vector2 position = entity.getPosition();
    Vector2 scale = entity.getScale();
    float rotation = physics == null ? 0f : physics.getBody().getLinearVelocity().angleDeg();
    batch.draw(
        texture,
        position.x,
        position.y,
        scale.x / 2f,
        scale.y / 2f,
        scale.x,
        scale.y,
        1f,
        1f,
        rotation,
        0,
        0,
        texture.getWidth(),
        texture.getHeight(),
        false,
        false);
  }

  @Override
  public void dispose() {
    super.dispose();
    if (texture != null) {
      texture.dispose();
      texture = null;
    }
  }

  /** Creates CPU artwork; the caller owns and must dispose the returned pixmap. */
  static Pixmap createPixmap(PetType type) {
    Pixmap pixels = new Pixmap(32, type == PetType.BIRD ? 16 : 32, Pixmap.Format.RGBA8888);
    pixels.setBlending(Pixmap.Blending.None);
    pixels.setColor(0x00000000);
    pixels.fill();
    switch (type) {
      case BIRD -> drawDart(pixels);
      case BAT -> drawDiamond(pixels);
      case SPIRIT -> drawOrb(pixels);
    }
    return pixels;
  }

  private static void drawDart(Pixmap pixels) {
    pixels.setColor(0xe79a2480);
    pixels.fillTriangle(1, 2, 14, 8, 1, 13);
    pixels.setColor(0xa85a19ff);
    pixels.fillTriangle(7, 1, 30, 8, 7, 14);
    pixels.setColor(0xffca43ff);
    pixels.fillTriangle(9, 3, 28, 8, 9, 12);
    pixels.setColor(0xfff3b0ff);
    pixels.fillTriangle(10, 6, 27, 8, 10, 9);
  }

  private static void drawDiamond(Pixmap pixels) {
    pixels.setColor(0xa55bff50);
    pixels.fillCircle(16, 16, 14);
    pixels.setColor(0x522889ff);
    pixels.fillTriangle(2, 16, 16, 2, 30, 16);
    pixels.fillTriangle(2, 16, 16, 30, 30, 16);
    pixels.setColor(0xb56affff);
    pixels.fillTriangle(6, 16, 16, 6, 26, 16);
    pixels.fillTriangle(6, 16, 16, 26, 26, 16);
    pixels.setColor(0xf1d4ffff);
    pixels.fillTriangle(11, 16, 16, 11, 21, 16);
    pixels.fillTriangle(11, 16, 16, 21, 21, 16);
  }

  private static void drawOrb(Pixmap pixels) {
    pixels.setColor(0x55dbed35);
    pixels.fillCircle(16, 16, 15);
    pixels.setColor(0x55dbed80);
    pixels.fillCircle(16, 16, 12);
    pixels.setColor(0x6ce4f6ff);
    pixels.fillCircle(16, 16, 9);
    pixels.setColor(0xc4fbffff);
    pixels.fillCircle(18, 14, 6);
    pixels.setColor(0xffffffff);
    pixels.fillCircle(20, 12, 2);
  }
}
