package com.csse3200.game.Quests;

public class EnemiesKilledQuest {
  float globalEnemiesKilledSnapshot;
  float enemiesToKill;

  public EnemiesKilledQuest(int enemiesToKill) {
    if (enemiesToKill <= 0) {
      throw new IllegalArgumentException(
          "EnemiesKilledQuest was given a enemiesToKill that was less "
              + "than or equal to zero when it shouldn't have");
    }
    this.enemiesToKill = enemiesToKill;
    this.globalEnemiesKilledSnapshot = Quest.getGlobalEnemiesKilled();
  }

  public int checkQuestProgress() {
    if ((Quest.getGlobalEnemiesKilled() - globalEnemiesKilledSnapshot) == 0) {
      return 0;
    }
    if (((Quest.getGlobalEnemiesKilled() - globalEnemiesKilledSnapshot) / enemiesToKill) * 100
        > 100) {
      // To avoid giving out over 100%
      return 100;
    }
    return (int)
        (((Quest.getGlobalEnemiesKilled() - globalEnemiesKilledSnapshot) / enemiesToKill) * 100);
  }
}
