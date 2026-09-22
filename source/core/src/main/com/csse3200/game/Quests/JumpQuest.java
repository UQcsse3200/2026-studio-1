package com.csse3200.game.Quests;

public class JumpQuest {
  float jumpsToDoTotal;
  float globalJumpsSnapshot;
  float jumpsToDo;

  public JumpQuest(int jumpsToDo) {
    if (jumpsToDo <= 0) {
      throw new IllegalArgumentException(
          "EnemiesKilledQuest was given a jumpsToDo that was less "
              + "than or equal to zero when it shouldn't have");
    }
    this.jumpsToDoTotal = jumpsToDo + Quest.getGlobalJumps();
    this.jumpsToDo = jumpsToDo;
    this.globalJumpsSnapshot = Quest.getGlobalJumps();
  }

  // Gives out the quest progress in a percentage
  public int checkQuestProgress() {
    if (Quest.getGlobalJumps() - globalJumpsSnapshot == 0) {
      return 0;
    }
    if (((Quest.getGlobalJumps() - globalJumpsSnapshot) / jumpsToDo) * 100 > 100) {
      // To avoid giving out over 100%
      return 100;
    }
    return (int) (((Quest.getGlobalJumps() - globalJumpsSnapshot) / jumpsToDo) * 100);
  }
}
