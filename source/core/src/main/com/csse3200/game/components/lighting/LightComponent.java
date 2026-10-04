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
  private final Entity follow; // null for fixed lights
  private PointLight light;
  private float phase = (float) (Math.random() * 6.28);

  public LightComponent(LightSpec spec, Entity follow) {
    this.spec = spec;
    this.follow = follow;
  }

  @Override
  public void create() {
    RayHandler rh = ServiceLocator.getLightService().getRayHandler();
    Vector2 p = entity.getCenterPosition();
    light = new PointLight(rh, spec.rays(), spec.color(), spec.radius(), p.x, p.y);
    light.setXray(true);
    light.setSoft(false);
    light.setStaticLight(follow == null && spec.flicker() == 0f);
  }

  @Override
  public void update() {
    if (follow != null) light.setPosition(follow.getCenterPosition());
    if (spec.flicker() > 0f) {
      phase += Gdx.graphics.getDeltaTime() * 9f;
      float n =
          1f - spec.flicker() * (0.5f + 0.5f * MathUtils.sin(phase) * MathUtils.sin(phase * 0.37f));
      light.setDistance(spec.radius() * n);
    }
  }

  @Override
  public void dispose() {
    if (light != null) light.remove();
  }
}
