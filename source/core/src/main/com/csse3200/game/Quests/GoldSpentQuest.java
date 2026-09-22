package com.csse3200.game.Quests;

public class GoldSpentQuest {
    float goldToSpend;
    float goldSpentSnapshot;
    public GoldSpentQuest(int goldToSpend){
        if(goldToSpend<=0){
            throw new IllegalArgumentException("The constructor for GoldSpentQuest recieved " +
                    "a 0 or less for the parameter goldToSpend when it shouldn't have");
        }
        this.goldToSpend = goldToSpend;
        this.goldSpentSnapshot = Quest.getGlobalGoldSpent();
    }
    public int checkQuestProgress(){
        if(Quest.getGlobalGoldSpent()-goldSpentSnapshot<=0){
            return 0;
        }
        if((((Quest.getGlobalGoldSpent()-goldSpentSnapshot)/goldToSpend) *100)>100){
            return 100;
        }

        return (int)(((Quest.getGlobalGoldSpent()-goldSpentSnapshot)/goldToSpend)*100);
    }
}
