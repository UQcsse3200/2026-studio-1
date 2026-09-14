package com.csse3200.game.components;

import com.csse3200.game.Quests.Quest;

public class QuestGiverComponent  extends Component {
    public int uniqueNPCID;
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
        if(Quest.checkJumpQuestComplete(uniqueNPCID)==-1){
            throw new  NullPointerException("A QuestGiver component tried to call checkJumpQuestComplete when " +
                    "there isn't a quest to check the progress of i.e. it returned null");
        }else{
            return Quest.checkJumpQuestComplete(uniqueNPCID);
        }
    }
}
