package com.csse3200.game.components;

import com.csse3200.game.Quests.Quest;

public class QuestGiverComponent {
    int uniqueNPCID;
    public QuestGiverComponent(){
        uniqueNPCID = Quest.giveOutUniqueNPCID();
    }
    public void logJumpQuest(String Reward, int jumpsToDo){
        Quest.logJumpQuest(uniqueNPCID, jumpsToDo);
    }
    public void clearJumpQuest(){
        Quest.clearJumpQuest(uniqueNPCID);
    }
    public int checkJumpQuestComplete(){
        return Quest.checkJumpQuestComplete(uniqueNPCID);
    }
}
