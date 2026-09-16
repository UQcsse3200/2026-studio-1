package com.csse3200.game.physics;

import com.badlogic.gdx.physics.box2d.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Box2D collision events fire globally on the physics world, not per-object. The contact listener
 * receives these events, finds the entities involved in the collision, and triggers events on them.
 *
 * <p>On contact start: evt = "collisionStart", params = ({@link Fixture} thisFixture, {@link
 * Fixture} otherFixture)
 *
 * <p>On contact end: evt = "collisionEnd", params = ({@link Fixture} thisFixture, {@link Fixture}
 * otherFixture)
 */
public class PhysicsContactListener implements ContactListener {
  private static final Logger logger = LoggerFactory.getLogger(PhysicsContactListener.class);
  private static final float PLATFORM_SURFACE_TOLERANCE = 0.05f;

  @Override
  public void beginContact(Contact contact) {
    triggerEventOn(contact.getFixtureA(), "collisionStart", contact.getFixtureB());
    triggerEventOn(contact.getFixtureB(), "collisionStart", contact.getFixtureA());
  }

  @Override
  public void endContact(Contact contact) {
    triggerEventOn(contact.getFixtureA(), "collisionEnd", contact.getFixtureB());
    triggerEventOn(contact.getFixtureB(), "collisionEnd", contact.getFixtureA());
  }

  @Override
  public void preSolve(Contact contact, Manifold oldManifold) {
    Fixture fixtureA = contact.getFixtureA();
    Fixture fixtureB = contact.getFixtureB();

    if (shouldDisablePlatformContact(fixtureA, fixtureB)
        || shouldDisablePlatformContact(fixtureB, fixtureA)) {
      contact.setEnabled(false);
    }
  }

  @Override
  public void postSolve(Contact contact, ContactImpulse impulse) {
    // Nothing to do after resolving contact
  }

  /**
   * Returns whether contact between a one-way platform and another fixture should be ignored.
   * Contact is disabled while the other body moves upward or remains below the platform surface.
   */
  static boolean shouldDisablePlatformContact(Fixture platform, Fixture other) {
    if (!isOnLayer(platform, PhysicsLayer.PLATFORM) || other.isSensor()) {
      return false;
    }

    Body platformBody = platform.getBody();
    Body otherBody = other.getBody();
    float platformTop = platformBody.getPosition().y + entityHeight(platformBody);
    float otherBottom = otherBody.getPosition().y;

    return otherBody.getLinearVelocity().y > 0f
        || otherBottom < platformTop - PLATFORM_SURFACE_TOLERANCE;
  }

  private static boolean isOnLayer(Fixture fixture, short layer) {
    return PhysicsLayer.contains(layer, fixture.getFilterData().categoryBits);
  }

  private static float entityHeight(Body body) {
    Object data = body.getUserData();
    if (data instanceof BodyUserData bodyData && bodyData.entity != null) {
      return bodyData.entity.getScale().y;
    }
    return 0f;
  }

  private void triggerEventOn(Fixture fixture, String evt, Fixture otherFixture) {
    BodyUserData userData = (BodyUserData) fixture.getBody().getUserData();
    if (userData != null && userData.entity != null) {
      logger.debug("{} on entity {}", evt, userData.entity);
      userData.entity.getEvents().trigger(evt, fixture, otherFixture);
    }
  }
}
