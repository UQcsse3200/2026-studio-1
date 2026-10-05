package com.csse3200.game.rendering;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class GlowRenderComponentTest {
  @Test
  void aLightFlickersButNeverGoesOutOrBlowsOut() {
    for (float elapsed = 0f; elapsed < 10f; elapsed += 0.05f) {
      float alpha = GlowRenderComponent.alphaAt(elapsed, 0f);

      assertTrue(alpha > 0.3f && alpha < 0.6f, "alpha " + alpha + " at " + elapsed);
    }
  }

  @Test
  void neighbouringLightsDoNotPulseInStep() {
    assertNotEquals(
        GlowRenderComponent.alphaAt(1f, 0f), GlowRenderComponent.alphaAt(1f, 1.7f), 1e-3f);
  }
}
