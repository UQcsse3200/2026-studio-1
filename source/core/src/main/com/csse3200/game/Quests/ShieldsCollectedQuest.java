package com.csse3200.game.Quests;

public class ShieldsCollectedQuest {
  float shieldsToCollect;
  float globalShieldsCollectedSnapshot;

  public ShieldsCollectedQuest(int shieldsToCollect) {
    if (shieldsToCollect <= 0) {
      throw new IllegalArgumentException(
          "ShieldsCollectedQuest was given a shieldsToCollect that was less "
              + "than or equal to zero when it shouldn't have");
    }
    this.shieldsToCollect = shieldsToCollect;
    this.globalShieldsCollectedSnapshot = Quest.getGlobalShieldsCollected();
  }

  public int checkQuestProgress() {
    if (Quest.getGlobalShieldsCollected() - globalShieldsCollectedSnapshot == 0) {
      return 0;
    }
    if ((((Quest.getGlobalShieldsCollected() - globalShieldsCollectedSnapshot) / shieldsToCollect)
            * 100)
        > 100) {
      return 100;
    }
    return (int)
        (((Quest.getGlobalShieldsCollected() - globalShieldsCollectedSnapshot) / shieldsToCollect)
            * 100);
  }
}
