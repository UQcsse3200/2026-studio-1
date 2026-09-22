package com.csse3200.game.Quests;

public class EnemiesKilledQuest {
    float enemiesToKillTotal;
    public EnemiesKilledQuest(int enemiesToKill, int enemiesHaveKilled){
        this.enemiesToKillTotal = enemiesToKill + enemiesHaveKilled;
    }
    public int checkQuestProgress(){
        if ((Quest.getEnemiesKilled() / enemiesToKillTotal) * 100 > 100) {
            // To avoid giving out over 100%
            return 100;
        }
        return (int) ((Quest.getEnemiesKilled() / enemiesToKillTotal) * 100);
    }
}
