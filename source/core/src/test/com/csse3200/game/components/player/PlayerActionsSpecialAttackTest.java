package com.csse3200.game.components.player;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.Filter;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.physics.BodyUserData;
import com.csse3200.game.physics.PhysicsLayer;
import com.csse3200.game.physics.components.HitboxComponent;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class PlayerActionsSpecialAttackTest {
  @Test
  void shouldDealTripleDamageToOneNearbyEnemyAndRespectCooldown() {
    PlayerActions actions = new PlayerActions();
    CombatStatsComponent playerStats = new CombatStatsComponent(100, 10);
    StaminaComponent stamina = new StaminaComponent();
    HitboxComponent hitbox = mock(HitboxComponent.class);
    Fixture playerFixture = mock(Fixture.class);
    when(hitbox.getFixture()).thenReturn(playerFixture);

    Entity player =
        new Entity()
            .addComponent(playerStats)
            .addComponent(stamina)
            .addComponent(hitbox)
            .addComponent(actions);
    player.create();

    CombatStatsComponent enemyStats = new CombatStatsComponent(100, 0);
    Entity enemy = new Entity().addComponent(enemyStats);
    enemy.setPosition(1f, 0f);
    AtomicReference<Entity> effectTarget = new AtomicReference<>();
    player.getEvents().addListener("specialAttackHit", effectTarget::set);
    player.getEvents().trigger("collisionStart", playerFixture, npcFixtureFor(enemy));

    player.getEvents().trigger("specialAttack");
    player.getEvents().trigger("specialAttack");

    assertEquals(70, enemyStats.getHealth());
    assertEquals(95f, stamina.getStamina());
    assertEquals(enemy, effectTarget.get());
  }

  private Fixture npcFixtureFor(Entity enemy) {
    Fixture fixture = mock(Fixture.class);
    Body body = mock(Body.class);
    BodyUserData bodyUserData = new BodyUserData();
    bodyUserData.entity = enemy;
    Filter filter = new Filter();
    filter.categoryBits = PhysicsLayer.NPC;

    when(fixture.getBody()).thenReturn(body);
    when(fixture.getFilterData()).thenReturn(filter);
    when(body.getUserData()).thenReturn(bodyUserData);
    return fixture;
  }
}
