package com.csse3200.game.components;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.csse3200.game.components.player.PlayerActions;
import com.csse3200.game.entities.Entity;

import com.csse3200.game.components.Component;
import com.csse3200.game.physics.components.PhysicsComponent;

public class PlatformerComponent extends Component{
    private Vector2 jumpDirection = Vector2.Zero.cpy();

    public boolean jumping = false;

    private boolean doubleJumpPowerup = false;
    private int doubleJumpRemaining = 0;
    private int maxDoubleJump = 0;

    private boolean superJumpPowerup = false;
    private int superJumpScaler = 1;

    private int baseJumpScaler = 3;

    PlayerActions playerActions;
    private PhysicsComponent physicsComponent;

    public PlatformerComponent(int baseJumpScaler){
        this.baseJumpScaler = baseJumpScaler;
    }

    public PlatformerComponent(int baseJumpScaler ,boolean doubleJumpPowerup, int maxDoubleJump, boolean superJumpPowerup, int superJumpScaler){
        this.baseJumpScaler = baseJumpScaler;
        this.doubleJumpPowerup = doubleJumpPowerup;
        this.maxDoubleJump = maxDoubleJump;
        this.doubleJumpRemaining = maxDoubleJump;
        this.superJumpPowerup = superJumpPowerup;
        this.superJumpScaler =superJumpScaler;
    }

    @Override
    public void create() {
        playerActions = entity.getComponent(PlayerActions.class);
        physicsComponent = entity.getComponent(PhysicsComponent.class);
        entity.getEvents().addListener("jump", this::jump);
    }

    public void updateJump(Vector2 MAX_SPEED){
        if(jumping){
            Body body = physicsComponent.getBody();
            Vector2 desiredVelocity = jumpDirection.cpy().scl(MAX_SPEED);
            Vector2 jumpImpulse = desiredVelocity.scl(body.getMass());
            body.applyLinearImpulse(jumpImpulse, body.getWorldCenter(), true);
            jumpDirection.y = 0;
            jumping = false;
        }
    }
    public boolean isGrounded(){
        Body body = physicsComponent.getBody();
        //If there's no y velocity then that means the player must not falling or jumping
        //i.e. they're on a platform
        //You could alternatively implement a raycast to determine ifGrounded
        return (body.getLinearVelocity().y ==0);
    }

    void jump(Vector2 direction){
        if(isGrounded() || (doubleJumpPowerup && doubleJumpRemaining >0)){
            this.jumpDirection.y = direction.y;
            this.jumpDirection.y *= baseJumpScaler;
            if (!isGrounded()) doubleJumpRemaining--;
            if(superJumpPowerup) this.jumpDirection.y*= superJumpScaler;
            jumping = true;
        }
        if (doubleJumpPowerup && isGrounded()){
            doubleJumpRemaining = maxDoubleJump;
        }
    }
}
