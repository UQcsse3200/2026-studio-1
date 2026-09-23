package com.csse3200.game.components;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.csse3200.game.areas.terrain.TileType;
import com.csse3200.game.areas.terrain.map.TileDefinition;
import com.csse3200.game.extensions.GameExtension;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class HazardDamageComponentTest {
  private static final int DEFAULT_DAMAGE = 10;

  @Test
  void carriesTheDamageItWasGiven() {
    assertEquals(15, new HazardDamageComponent(15).getDamage());
  }

  @Test
  void treatsNegativeDamageAsZero() {
    assertEquals(0, new HazardDamageComponent(-5).getDamage());
  }

  @Test
  void takesDamageFromTheTilesLegendEntry() {
    TileDefinition styx =
        new TileDefinition(TileType.HAZARD, "river-styx.png", Map.of("damage", "15"));
    TileDefinition firepit =
        new TileDefinition(TileType.HAZARD, "firepit.png", Map.of("damage", "25"));

    assertEquals(15, new HazardDamageComponent(styx.getInt("damage", DEFAULT_DAMAGE)).getDamage());
    assertEquals(
        25, new HazardDamageComponent(firepit.getInt("damage", DEFAULT_DAMAGE)).getDamage());
  }

  @Test
  void fallsBackToTheLevelDefaultWhenTheMapSetsNoDamage() {
    TileDefinition plain = new TileDefinition(TileType.HAZARD, "spikes.png");

    assertEquals(
        DEFAULT_DAMAGE,
        new HazardDamageComponent(plain.getInt("damage", DEFAULT_DAMAGE)).getDamage());
  }
}
