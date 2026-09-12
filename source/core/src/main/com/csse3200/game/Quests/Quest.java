package com.csse3200.game.Quests;
import java.util.ArrayList;
public class Quest {
    private static ArrayList<Integer> uniqueNPCID = new ArrayList<Integer>();
    //The unique NPCID lets us know if the NPC has a quest is active.
    private static ArrayList<Boolean> questActiveForNPCID = new ArrayList<Boolean>();

    //Quest trackers
    private static ArrayList<JumpQuest> jumpQuestTracker = new ArrayList<JumpQuest>();
    public static int giveOutUniqueNPCID(){
        int uniqueID = uniqueNPCID.size();
        uniqueNPCID.add(uniqueID);
        questActiveForNPCID.add(false);
        //Add space for a possible jump quest later on.
        jumpQuestTracker.add(uniqueID, null);
        return uniqueID;
    }
    public static void logJumpQuest(int NPCId, int jumpsToDo){
        jumpQuestTracker.add(NPCId, new JumpQuest(jumpsToDo));
        questActiveForNPCID.add(NPCId,true);
    }
    public static int checkJumpQuestComplete(int NPCId){
        return jumpQuestTracker.get(NPCId).checkQuestProgress();
    }
    public static void clearJumpQuest(int NPCId){
        jumpQuestTracker.set(NPCId,null);
        questActiveForNPCID.set(NPCId,null);
    }
}
