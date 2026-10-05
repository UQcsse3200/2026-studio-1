package com.csse3200.game.components.lighting;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.csse3200.game.components.lighting.LightingConfig.Ambient;
import org.junit.jupiter.api.Test;

class LightingConfigParserTest {
  private static final float EPS = 1e-6f;

  private static LightingConfig parse(String json) {
    return LightingConfigParser.parse(new JsonReader().parse(json), "test-map");
  }

  // ---- presence and defaults ----

  @Test
  void missingBlock_givesNull() {
    assertNull(LightingConfigParser.parse(null, "test-map"));
  }

  @Test
  void emptyBlock_usesDefaultAmbientAndPlayer() {
    LightingConfig cfg = parse("{}");

    assertNotNull(cfg);
    assertEquals(LightingConfig.DEFAULT_AMBIENT.color(), cfg.ambient().color());
    assertEquals(LightingConfig.DEFAULT_AMBIENT.intensity(), cfg.ambient().intensity(), EPS);
    assertEquals(LightColour.ELYSIAN_FADE.getColour(), cfg.player().color());
    assertEquals(3f, cfg.player().radius(), EPS);
    assertEquals(0f, cfg.player().flicker(), EPS);
    assertEquals(0, cfg.subLevels().size());
  }

  // ---- ambient ----

  @Test
  void ambient_readsThemeNameAndIntensity() {
    LightingConfig cfg = parse("{\"ambient\":{\"color\":\"background_dark\",\"intensity\":0.35}}");

    assertEquals(LightColour.AMBIENT_DARK.getColour(), cfg.ambient().color());
    assertEquals(0.35f, cfg.ambient().intensity(), EPS);
  }

  @Test
  void ambient_readsHexColour() {
    LightingConfig cfg = parse("{\"ambient\":{\"color\":\"ffcc88\",\"intensity\":0.5}}");
    assertEquals(Color.valueOf("ffcc88"), cfg.ambient().color());
  }

  @Test
  void ambient_acceptsBritishSpelling_andItWinsOverAmericanSpelling() {
    LightingConfig british = parse("{\"ambient\":{\"colour\":\"oil_lamp\"}}");
    assertEquals(LightColour.OIL_LAMP.getColour(), british.ambient().color());

    LightingConfig both = parse("{\"ambient\":{\"colour\":\"oil_lamp\",\"color\":\"lava_glow\"}}");
    assertEquals(LightColour.OIL_LAMP.getColour(), both.ambient().color());
  }

  @Test
  void ambient_intensityIsClampedToUnitRange() {
    assertEquals(1f, parse("{\"ambient\":{\"intensity\":2.0}}").ambient().intensity(), EPS);
    assertEquals(0f, parse("{\"ambient\":{\"intensity\":-0.5}}").ambient().intensity(), EPS);
  }

  @Test
  void ambient_missingFields_fallBackToDefaults() {
    LightingConfig cfg = parse("{\"ambient\":{}}");
    assertEquals(LightingConfig.DEFAULT_AMBIENT.color(), cfg.ambient().color());
    assertEquals(LightingConfig.DEFAULT_AMBIENT.intensity(), cfg.ambient().intensity(), EPS);
  }

  @Test
  void ambient_unknownColourName_fallsBackToDefaultColour() {
    LightingConfig cfg = parse("{\"ambient\":{\"color\":\"not_a_colour\",\"intensity\":0.4}}");
    assertEquals(LightingConfig.DEFAULT_AMBIENT.color(), cfg.ambient().color());
    assertEquals(0.4f, cfg.ambient().intensity(), EPS);
  }

  /**
   * Malformed values must never stop a level loading. Expected to fail until the parser guards
   * getFloat.
   */
  @Test
  void ambient_nonNumericIntensity_fallsBackWithoutThrowing() {
    JsonValue json = new JsonReader().parse("{\"ambient\":{\"intensity\":\"loud\"}}");
    LightingConfig[] result = new LightingConfig[1];

    assertDoesNotThrow(() -> result[0] = LightingConfigParser.parse(json, "test-map"));
    assertEquals(LightingConfig.DEFAULT_AMBIENT.intensity(), result[0].ambient().intensity(), EPS);
  }

  // ---- sub-levels ----

  private static final String WITH_SUBLEVELS =
      """
            {
              "ambient": { "color": "background_dark", "intensity": 0.5 },
              "subLevels": {
                "dungeon": { "ambient": { "color": "oil_lamp", "intensity": 0.35 } },
                "nether":  { "ambient": { "color": "lava_glow" } },
                "bare":    {}
              }
            }
            """;

  @Test
  void subLevel_withFullAmbient_usesItsOwnValues() {
    Ambient a = parse(WITH_SUBLEVELS).ambientFor("dungeon");
    assertEquals(LightColour.OIL_LAMP.getColour(), a.color());
    assertEquals(0.35f, a.intensity(), EPS);
  }

  @Test
  void subLevel_withoutIntensity_inheritsMapWideIntensity() {
    Ambient a = parse(WITH_SUBLEVELS).ambientFor("nether");
    assertEquals(LightColour.LAVA_GLOW.getColour(), a.color());
    assertEquals(0.5f, a.intensity(), EPS);
  }

  @Test
  void subLevel_withoutAmbientKey_isNotRegistered() {
    LightingConfig cfg = parse(WITH_SUBLEVELS);
    assertFalse(cfg.subLevels().containsKey("bare"));
    assertEquals(cfg.ambient(), cfg.ambientFor("bare"));
  }

  @Test
  void ambientFor_unknownOrNullId_givesMapWideAmbient() {
    LightingConfig cfg = parse(WITH_SUBLEVELS);
    assertEquals(cfg.ambient(), cfg.ambientFor("missing"));
    assertEquals(cfg.ambient(), cfg.ambientFor(null));
  }

  @Test
  void subLevels_whenMapWideAmbientAbsent_inheritFromDefaults() {
    LightingConfig cfg =
        parse("{\"subLevels\":{\"nether\":{\"ambient\":{\"color\":\"lava_glow\"}}}}");
    assertEquals(
        LightingConfig.DEFAULT_AMBIENT.intensity(), cfg.ambientFor("nether").intensity(), EPS);
  }

  // ---- player ----

  @Test
  void player_readsRadiusColourAndFlicker() {
    LightingConfig cfg =
        parse("{\"player\":{\"radius\":4,\"color\":\"oil_lamp\",\"flicker\":0.3}}");

    assertEquals(4f, cfg.player().radius(), EPS);
    assertEquals(LightColour.OIL_LAMP.getColour(), cfg.player().color());
    assertEquals(0.3f, cfg.player().flicker(), EPS);
    assertEquals(32, cfg.player().rays());
  }

  @Test
  void player_missingFields_useDefaults() {
    LightingConfig cfg = parse("{\"player\":{}}");
    assertEquals(3f, cfg.player().radius(), EPS);
    assertEquals(LightColour.ELYSIAN_FADE.getColour(), cfg.player().color());
  }

  @Test
  void player_tinyRadius_isRaisedToMinimum() {
    assertEquals(0.5f, parse("{\"player\":{\"radius\":0.1}}").player().radius(), EPS);
  }

  @Test
  void player_unknownColour_fallsBackToDefault() {
    LightingConfig cfg = parse("{\"player\":{\"color\":\"nope\"}}");
    assertEquals(LightColour.ELYSIAN_FADE.getColour(), cfg.player().color());
  }
}
