package com.csse3200.game.components.lighting;

import box2dLight.PointLight;
import box2dLight.RayHandler;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.components.Component;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.services.ServiceLocator;

public class LightComponent extends Component {
  private final LightSpec spec;
  private final Vector2 fixedPos; // null when following
  private final Entity follow; // null when fixed
  private PointLight light;
  private float phase = MathUtils.random(0f, MathUtils.PI2);

  public LightComponent(LightSpec spec, Vector2 fixedPos) {
    this.spec = spec;
    this.fixedPos = new Vector2(fixedPos);
    this.follow = null;
  }

  public LightComponent(LightSpec spec, Entity follow) {
    this.spec = spec;
    this.fixedPos = null;
    this.follow = follow;
  }

  @Override
  public void create() {
    RayHandler rh = ServiceLocator.getLightService().getRayHandler();
    Vector2 p = follow != null ? follow.getCenterPosition() : fixedPos;
    light = new PointLight(rh, spec.rays(), spec.color(), spec.radius(), p.x, p.y);
    light.setXray(true);
    light.setSoft(false);
    light.setStaticLight(follow == null && spec.flicker() == 0f);
  }

  @Override
  public void update() {
    if (light == null) return;
    if (follow != null) {
      light.setPosition(follow.getCenterPosition());
    }
    if (spec.flicker() > 0f) {
      phase += Gdx.graphics.getDeltaTime() * 9f;
      float wobble = MathUtils.sin(phase) * MathUtils.sin(phase * 0.37f); // -1..1
      light.setDistance(spec.radius() * (1f - spec.flicker() * (0.5f + 0.5f * wobble)));
    }
  }

  @Override
  public void dispose() {
    if (light != null) {
      light.remove();
      light = null;
    }
  }
}
