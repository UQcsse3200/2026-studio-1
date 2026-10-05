package com.csse3200.game.rendering;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.csse3200.game.components.pet.PetType;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.services.ServiceLocator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.MockedConstruction;

@ExtendWith(GameExtension.class)
class PetProjectileRenderComponentTest {
  private Body body;

  @BeforeEach
  void setUp() {
    ServiceLocator.registerEntityService(new EntityService());
    ServiceLocator.registerRenderService(new RenderService());
    body = mock(Body.class);
  }

  @Test
  void shouldRotateArtworkWithCurrentVelocity() {
    try (MockedConstruction<Texture> textures = mockTextures()) {
      Entity projectile = createProjectile(PetType.BIRD);
      PetProjectileRenderComponent renderer =
          projectile.getComponent(PetProjectileRenderComponent.class);
      SpriteBatch batch = mock(SpriteBatch.class);
      Texture texture = textures.constructed().getFirst();

      when(body.getLinearVelocity()).thenReturn(new Vector2(1f, 1f));
      renderer.render(batch);
      verifyDraw(batch, texture, 45f);
      when(body.getLinearVelocity()).thenReturn(new Vector2(0f, -1f));
      renderer.render(batch);
      verifyDraw(batch, texture, 270f);
      projectile.dispose();
    }
  }

  @ParameterizedTest
  @EnumSource(PetType.class)
  void shouldReleaseEachProjectilesOwnTextureOnce(PetType type) {
    try (MockedConstruction<Texture> textures = mockTextures()) {
      Entity first = createProjectile(type);
      Entity second = createProjectile(type);
      assertEquals(2, textures.constructed().size());

      first.dispose();
      first.dispose();
      second.dispose();

      verify(textures.constructed().get(0)).dispose();
      verify(textures.constructed().get(1)).dispose();
    }
  }

  private MockedConstruction<Texture> mockTextures() {
    return mockConstruction(
        Texture.class,
        (texture, context) -> {
          Pixmap pixels = (Pixmap) context.arguments().getFirst();
          when(texture.getWidth()).thenReturn(pixels.getWidth());
          when(texture.getHeight()).thenReturn(pixels.getHeight());
        });
  }

  private Entity createProjectile(PetType type) {
    PhysicsComponent physics = mock(PhysicsComponent.class);
    when(physics.getBody()).thenReturn(body);
    Entity projectile =
        new Entity().addComponent(physics).addComponent(new PetProjectileRenderComponent(type));
    projectile.setPosition(2f, 3f);
    projectile.setScale(0.4f, 0.2f);
    ServiceLocator.getEntityService().register(projectile);
    return projectile;
  }

  private void verifyDraw(SpriteBatch batch, Texture texture, float angle) {
    verify(batch)
        .draw(
            eq(texture),
            eq(2f),
            eq(3f),
            eq(0.2f),
            eq(0.1f),
            eq(0.4f),
            eq(0.2f),
            eq(1f),
            eq(1f),
            eq(angle),
            eq(0),
            eq(0),
            eq(32),
            eq(16),
            eq(false),
            eq(false));
  }
}
