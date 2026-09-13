package com.csse3200.game.Quests;
import com.csse3200.game.rendering.DebugRenderer;

import java.util.ArrayList;
public class Quest {
    private static ArrayList<Integer> uniqueNPCID = new ArrayList<Integer>();
    //The unique NPCID lets us know if the NPC has a quest is active.
    private static ArrayList<Boolean> questActiveForNPCID = new ArrayList<Boolean>();

    //Quest trackers
    private static ArrayList<JumpQuest> jumpQuestTracker = new ArrayList<JumpQuest>();
    //Statistics tracker
    private static int globalJumps = 1;
    public static int giveOutUniqueNPCID(){
        int uniqueID = uniqueNPCID.size();
        uniqueNPCID.add(uniqueID);
        questActiveForNPCID.add(false);
        //Add space for a possible jump quest later on.
        jumpQuestTracker.add(uniqueID, null);
        return uniqueID;
    }
    public static boolean logJumpQuest(int NPCId, int jumpsToDo){
        if(questActiveForNPCID.get(NPCId)){
            return false;
        }else {
            jumpQuestTracker.add(NPCId, new JumpQuest(globalJumps,jumpsToDo));
            questActiveForNPCID.add(NPCId, true);
            return true;
        }
    }
    public static int checkJumpQuestComplete(int NPCId){
        return jumpQuestTracker.get(NPCId).checkQuestProgress();
    }
    public static void clearJumpQuest(int NPCId){
        jumpQuestTracker.set(NPCId,null);
        questActiveForNPCID.set(NPCId,null);
    }
    public static ArrayList<JumpQuest> getJumpQuests(){
        return jumpQuestTracker;
    }
    public static void incrementGlobalJumps(){
        globalJumps++;
    }
    public static int getGlobalJumps(){
        return globalJumps;
    }
}
