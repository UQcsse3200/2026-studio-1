package com.csse3200.game.Quests;

import java.util.ArrayList;

public class JumpQuest {
    private static int globalJumps = 0;
    int jumpsToDoTotal;
    public JumpQuest (int jumpsToDo){
        this.jumpsToDoTotal = jumpsToDo + globalJumps;
    }
    //Gives out the quest progress in a percentage
    public int checkQuestProgress(){
        if((jumpsToDoTotal/globalJumps)*100 >100){
            //To avoid giving out over 100%
            return 100;
        }
        return (jumpsToDoTotal/globalJumps)*100;
    }
    public static void incrementGlobalJumps(){
        globalJumps++;
    }
}
