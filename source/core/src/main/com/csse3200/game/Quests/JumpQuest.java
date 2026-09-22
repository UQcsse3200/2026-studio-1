package com.csse3200.game.Quests;

public class JumpQuest {
  float jumpsToDoTotal;
  float globalJumps = Quest.getGlobalJumps();

  public JumpQuest(int jumpsToDo, int jumpsHaveDone) {
    this.jumpsToDoTotal = jumpsToDo + jumpsHaveDone;
  }

  // Gives out the quest progress in a percentage
  public int checkQuestProgress() {
    if ((Quest.getGlobalJumps() / jumpsToDoTotal) * 100 > 100) {
      // To avoid giving out over 100%
      return 100;
    }
    return (int) ((Quest.getGlobalJumps() / jumpsToDoTotal) * 100);
  }
}
