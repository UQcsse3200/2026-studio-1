package com.csse3200.game.Quests;

import java.util.ArrayList;

public class JumpQuest {
    float jumpsToDoTotal;
    float globalJumps = 1;
    public JumpQuest (int jumpsToDo, int jumpsHaveDone){
        this.jumpsToDoTotal = jumpsToDo + jumpsHaveDone;
    }
    //Gives out the quest progress in a percentage
    public int checkQuestProgress(){
        if((globalJumps/jumpsToDoTotal)*100 >100){
            //To avoid giving out over 100%
            return 100;
        }
        System.out.println("global jumps "+globalJumps);
        System.out.println("jumpstodo "+jumpsToDoTotal);
        System.out.println((globalJumps/jumpsToDoTotal));
        return (int) ((globalJumps/jumpsToDoTotal)*100);
    }
    public void checkGlobalJumps(){
        globalJumps=Quest.getGlobalJumps();
    }
}
