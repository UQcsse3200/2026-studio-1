package com.csse3200.game.components.loot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.player.BuffStat;
import com.csse3200.game.components.player.PlayerBuffComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class BuffEffectTest {
  private GameTime time;

  @BeforeEach
  void setUp() {
    time = mock(GameTime.class);
    when(time.getTime()).thenReturn(0L);
    ServiceLocator.registerTimeSource(time);
  }

  /**
   * Builds an entity that can hold buffs.
   *
   * @return a created entity
   */
  private Entity makeTarget() {
    Entity target =
        new Entity()
            .addComponent(new CombatStatsComponent(100, 10))
            .addComponent(new PlayerBuffComponent());
    target.create();
    return target;
  }

  @Test
  void shouldApplyBuffThroughBuffComponent() {
    Entity target = makeTarget();

    assertTrue(new BuffEffect(BuffStat.DAMAGE, 2f, 5f).apply(target));
    assertEquals(20, target.getComponent(CombatStatsComponent.class).getBaseAttack());
    assertTrue(target.getComponent(PlayerBuffComponent.class).hasBuff(BuffStat.DAMAGE));
  }

  @Test
  void shouldNotApplyWithoutBuffComponent() {
    Entity target = new Entity().addComponent(new CombatStatsComponent(100, 10));
    target.create();

    assertFalse(new BuffEffect(BuffStat.SPEED, 1.5f, 5f).apply(target));
    assertFalse(new BuffEffect(BuffStat.SPEED, 1.5f, 5f).apply(null));
  }

  @Test
  void shouldExposeConfiguredValues() {
    BuffEffect effect = new BuffEffect(BuffStat.SPEED, 1.5f, 8f);

    assertEquals(BuffStat.SPEED, effect.getStat());
    assertEquals(1.5f, effect.getMagnitude());
    assertEquals(8f, effect.getDurationSeconds());
  }

  @Test
  void shouldRejectInvalidArguments() {
    assertThrows(IllegalArgumentException.class, () -> new BuffEffect(null, 1.5f, 5f));
    assertThrows(IllegalArgumentException.class, () -> new BuffEffect(BuffStat.SPEED, 0f, 5f));
    assertThrows(IllegalArgumentException.class, () -> new BuffEffect(BuffStat.SPEED, 1.5f, 0f));
  }
}
