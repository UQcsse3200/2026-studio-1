package com.csse3200.game.components.player;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.physics.BodyUserData;
import com.csse3200.game.physics.components.HitboxComponent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class PlayerProjectileHitComponentTest {
  @Test
  void shouldRespectTargetShieldAndAvoidDamage() {
    CombatStatsComponent ownerStats = new CombatStatsComponent(100, 10);
    Entity owner = new Entity().addComponent(ownerStats);
    owner.create();

    CombatStatsComponent targetStats = new CombatStatsComponent(100, 0);
    targetStats.setShieldHits(1);
    Entity target = new Entity().addComponent(targetStats);
    target.create();

    Entity projectile = createProjectile(owner);
    fireCollision(projectile, target);

    assertEquals(100, targetStats.getHealth());
    assertEquals(0, targetStats.getShieldHits());
  }

  @Test
  void shouldTriggerOwnerKillEventWhenProjectileKillsTarget() {
    CombatStatsComponent ownerStats = new CombatStatsComponent(100, 10);
    Entity owner = new Entity().addComponent(ownerStats);
    owner.create();
    int[] killEvents = {0};
    owner.getEvents().addListener("enemyKilled", () -> killEvents[0]++);

    CombatStatsComponent targetStats = new CombatStatsComponent(5, 0);
    Entity target = new Entity().addComponent(targetStats);
    target.create();

    Entity projectile = createProjectile(owner);
    fireCollision(projectile, target);

    assertEquals(0, targetStats.getHealth());
    assertEquals(1, killEvents[0]);
  }

  private Entity createProjectile(Entity owner) {
    HitboxComponent hitbox = mock(HitboxComponent.class);
    Fixture fixture = mock(Fixture.class);
    when(hitbox.getFixture()).thenReturn(fixture);

    Entity projectile =
        new Entity().addComponent(hitbox).addComponent(new PlayerProjectileHitComponent(10, owner));
    projectile.create();
    return projectile;
  }

  private void fireCollision(Entity projectile, Entity target) {
    Fixture targetFixture = mock(Fixture.class);
    Body body = mock(Body.class);
    BodyUserData userData = new BodyUserData();
    userData.entity = target;
    when(targetFixture.getBody()).thenReturn(body);
    when(body.getUserData()).thenReturn(userData);

    projectile
        .getEvents()
        .trigger(
            "collisionStart",
            projectile.getComponent(HitboxComponent.class).getFixture(),
            targetFixture);
  }
}
