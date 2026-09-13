package com.csse3200.game.components;

import com.csse3200.game.Quests.Quest;

public class QuestGiverComponent {
    int uniqueNPCID;
    public QuestGiverComponent(){
        uniqueNPCID = Quest.giveOutUniqueNPCID();
    }
    public boolean logJumpQuest(String Reward, int jumpsToDo){
        if(Quest.logJumpQuest(uniqueNPCID, jumpsToDo)){
            return true;
        }else{
            return false;
        }
    }
    public void clearJumpQuest(){
        Quest.clearJumpQuest(uniqueNPCID);
    }
    public int checkJumpQuestComplete(){
        return Quest.checkJumpQuestComplete(uniqueNPCID);
    }
}
