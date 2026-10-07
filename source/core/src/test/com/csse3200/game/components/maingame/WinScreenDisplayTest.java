package com.csse3200.game.components.maingame;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.scenes.scene2d.Stage;
import com.csse3200.game.GdxGame;
import com.csse3200.game.GdxGame.ScreenType;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.spawn.EnemyRegistry;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import com.csse3200.game.win.BossRoster;
import com.csse3200.game.win.QuestLedger;
import com.csse3200.game.win.TortoiseLedger;
import com.csse3200.game.win.WinEvaluator;
import com.csse3200.game.win.WinResult;
import com.csse3200.game.win.WinTier;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Tests {@link WinScreenDisplay}: what the win screen says for each outcome, and that the buttons
 * and keyboard navigation it already had still work.
 *
 * <p>Two styles are used. The wording is built by small static methods that take a {@link
 * WinResult} and give back text, so those are tested directly with no screen at all. The display
 * itself is then created on a mocked stage, the way {@code PauseMenuDisplayTest} does it, to check
 * the text reaches the labels and the popup shows.
 */
@ExtendWith(GameExtension.class)
class WinScreenDisplayTest {
  private static final List<String> NONE = List.of();

  private GdxGame game;
  private WinScreenDisplay display;
  private Entity ui;

  @BeforeEach
  void beforeEach() {
    RenderService renderService = new RenderService();
    renderService.setStage(mock(Stage.class));
    ServiceLocator.registerRenderService(renderService);
    ServiceLocator.registerResourceService(mock(ResourceService.class));
    GameTime time = mock(GameTime.class);
    when(time.getDeltaTime()).thenReturn(0.02f);
    ServiceLocator.registerTimeSource(time);

    QuestLedger.reset();
    TortoiseLedger.reset();
    TortoiseLedger.setTotal(0);
    EnemyRegistry.loadFrom(new ArrayList<>());

    game = mock(GdxGame.class);
    display = new WinScreenDisplay(game);
    ui = new Entity().addComponent(display);
    ui.create();
  }

  @AfterEach
  void afterEach() {
    QuestLedger.reset();
    TortoiseLedger.reset();
    TortoiseLedger.setTotal(0);
    EnemyRegistry.loadFrom(new ArrayList<>());
  }

  private static WinResult result(
      WinTier tier,
      int defeated,
      int required,
      List<String> missing,
      int quests,
      int questsNeeded) {
    return new WinResult(tier, tier.isWin(), defeated, required, missing, quests, questsNeeded);
  }

  private static List<String> names(int count) {
    List<String> names = new ArrayList<>();
    for (int i = 1; i <= count; i++) {
      names.add("Guardian " + i);
    }
    return names;
  }

  // ---------- the wording, with no screen ----------

  @Nested
  class Wording {

    @ParameterizedTest(name = "{0} reads \"{1}\"")
    @CsvSource({
      "NONE, Rank 0 of 3",
      "VICTORY, Rank 1 of 3",
      "GLORY, Rank 2 of 3",
      "LEGEND, Rank 3 of 3"
    })
    void shouldShowTheTierAsARankOutOfThree(WinTier tier, String expected) {
      assertEquals(expected, WinScreenDisplay.rankText(tier));
    }

    @ParameterizedTest(name = "{0} of {1} guardians")
    @CsvSource({"0, 8", "3, 8", "8, 8", "0, 0"})
    void shouldCountTheGuardians(int defeated, int required) {
      WinResult r = result(WinTier.VICTORY, defeated, required, names(required - defeated), 0, 3);

      assertEquals(
          "Guardians defeated: " + defeated + " of " + required, WinScreenDisplay.guardiansText(r));
    }

    @ParameterizedTest(name = "{0} of {1} quests")
    @CsvSource({"0, 3", "2, 3", "3, 3", "7, 3", "0, 0"})
    void shouldCountTheQuests(int done, int needed) {
      WinResult r = result(WinTier.VICTORY, 0, 0, NONE, done, needed);

      assertEquals("Quests completed: " + done + " of " + needed, WinScreenDisplay.questsText(r));
    }

    @Test
    void shouldSayNoTortoisesAreHiddenWhileNoneExist() {
      WinResult r = result(WinTier.VICTORY, 0, 0, NONE, 0, 3);

      assertEquals("Tortoises found: none are hidden yet", WinScreenDisplay.tortoisesText(r));
    }

    @ParameterizedTest(name = "{0} of {1} tortoises")
    @CsvSource({"0, 5", "4, 5", "1, 2"})
    void shouldCountTheTortoisesWithoutTheHonourWhenSomeAreStillHidden(int found, int total) {
      WinResult r = new WinResult(WinTier.VICTORY, true, 0, 0, NONE, 0, 3, found, total);

      assertEquals("Tortoises found: " + found + " of " + total, WinScreenDisplay.tortoisesText(r));
    }

    @Test
    void shouldNameTheTortoiseChampionWhenEveryTortoiseIsFound() {
      WinResult r = new WinResult(WinTier.VICTORY, true, 0, 0, NONE, 0, 3, 5, 5);

      assertEquals(
          "Tortoises found: 5 of 5 - Tortoise Champion", WinScreenDisplay.tortoisesText(r));
    }

    @Test
    void shouldListNothingMissingAtLegend() {
      WinResult r = result(WinTier.LEGEND, 8, 8, NONE, 3, 3);

      assertEquals("", WinScreenDisplay.missingText(r));
    }

    @Test
    void shouldNameEveryMissingGuardianWhenThereAreFourOrFewer() {
      WinResult r = result(WinTier.VICTORY, 6, 8, names(2), 3, 3);

      assertEquals("Still standing:\nGuardian 1\nGuardian 2", WinScreenDisplay.missingText(r));
    }

    @Test
    void shouldNameExactlyFourWithoutAnAndMoreLine() {
      WinResult r = result(WinTier.VICTORY, 4, 8, names(4), 3, 3);

      assertEquals(
          "Still standing:\nGuardian 1\nGuardian 2\nGuardian 3\nGuardian 4",
          WinScreenDisplay.missingText(r));
    }

    @ParameterizedTest(name = "{0} missing: four named and {1} more")
    @CsvSource({"5, 1", "6, 2", "8, 4"})
    void shouldNameFourGuardiansAndSumUpTheRest(int missing, int more) {
      WinResult r = result(WinTier.VICTORY, 8 - missing, 8, names(missing), 3, 3);

      assertEquals(
          "Still standing:\nGuardian 1\nGuardian 2\nGuardian 3\nGuardian 4\nand " + more + " more",
          WinScreenDisplay.missingText(r));
    }

    @ParameterizedTest(name = "{0} of 3 quests: \"{1}\"")
    @CsvSource({"0, 3 more quests", "1, 2 more quests", "2, 1 more quest"})
    void shouldSayHowManyQuestsAreStillNeededAtGlory(int done, String expected) {
      WinResult r = result(WinTier.GLORY, 8, 8, NONE, done, 3);

      assertEquals(expected, WinScreenDisplay.missingText(r));
    }

    @Test
    void shouldListMissingGuardiansAndThenTheQuestsStillNeeded() {
      WinResult r = result(WinTier.VICTORY, 7, 8, names(1), 1, 3);

      assertEquals("Still standing:\nGuardian 1\n2 more quests", WinScreenDisplay.missingText(r));
    }

    @Test
    void shouldNotMentionQuestsOnceEnoughAreDone() {
      WinResult r = result(WinTier.VICTORY, 7, 8, names(1), 5, 3);

      assertEquals("Still standing:\nGuardian 1", WinScreenDisplay.missingText(r));
    }
  }

  // ---------- the display on a stage ----------

  @Nested
  class OnScreen {

    @Test
    void shouldStartHidden() {
      assertFalse(display.isVisible());
    }

    @ParameterizedTest(name = "{0} shows the title {1}")
    @CsvSource({"VICTORY, VICTORY", "GLORY, GLORY", "LEGEND, LEGEND"})
    void shouldShowTheTiersTitleAndSubtitle(WinTier tier, String title) {
      display.showWinScreen(result(tier, 0, 0, NONE, 0, 0));

      assertTrue(display.isVisible());
      assertEquals(title, display.titleLabel.getText().toString());
      assertEquals(tier.getSubtitle(), display.subtitleLabel.getText().toString());
      assertEquals("Rank " + tier.getLevel() + " of 3", display.rankLabel.getText().toString());
    }

    @Test
    void shouldPutEveryCountOnItsOwnLabel() {
      WinResult r = new WinResult(WinTier.VICTORY, true, 6, 8, names(2), 1, 3, 2, 5);

      display.showWinScreen(r);

      assertEquals("Guardians defeated: 6 of 8", display.guardiansLabel.getText().toString());
      assertEquals("Quests completed: 1 of 3", display.questsLabel.getText().toString());
      assertEquals("Tortoises found: 2 of 5", display.tortoisesLabel.getText().toString());
      assertEquals(
          "Still standing:\nGuardian 1\nGuardian 2\n2 more quests",
          display.missingLabel.getText().toString());
    }

    @Test
    void shouldReplaceTheTextWhenShownAgainForADifferentResult() {
      display.showWinScreen(result(WinTier.VICTORY, 0, 8, names(8), 0, 3));

      display.showWinScreen(result(WinTier.LEGEND, 8, 8, NONE, 3, 3));

      assertEquals("LEGEND", display.titleLabel.getText().toString());
      assertEquals("", display.missingLabel.getText().toString());
    }

    @Test
    void shouldShowAPlainVictoryForTheDebugCommandInAFreshGame() {
      display.showWinScreen();

      assertTrue(display.isVisible());
      assertEquals("VICTORY", display.titleLabel.getText().toString());
      assertEquals("Guardians defeated: 0 of 8", display.guardiansLabel.getText().toString());
    }

    @Test
    void shouldShowTheLiveProgressForTheDebugCommand() {
      EnemyRegistry.loadFrom(new ArrayList<>(BossRoster.getRequiredIds()));
      for (int i = 0; i < WinEvaluator.QUESTS_REQUIRED_FOR_LEGEND; i++) {
        QuestLedger.recordCompleted(QuestLedger.JUMP);
      }

      display.showWinScreen();

      assertEquals("LEGEND", display.titleLabel.getText().toString());
      assertEquals("Guardians defeated: 8 of 8", display.guardiansLabel.getText().toString());
    }

    @Test
    void shouldStartANewGameFromPlayAgainWhichIsSelectedFirst() {
      ui.getEvents().trigger("winConfirmSelection");

      verify(game).setScreen(ScreenType.MAIN_GAME);
    }

    @Test
    void shouldGoToTheMainMenuAfterMovingDownOnce() {
      ui.getEvents().trigger("winNavigateDown");
      ui.getEvents().trigger("winConfirmSelection");

      verify(game).setScreen(ScreenType.MAIN_MENU);
    }

    @Test
    void shouldWrapAroundWhenMovingUpFromTheFirstButton() {
      ui.getEvents().trigger("winNavigateUp");
      ui.getEvents().trigger("winConfirmSelection");

      verify(game).setScreen(ScreenType.MAIN_MENU);
    }

    @Test
    void shouldComeBackToPlayAgainAfterMovingDownTwice() {
      ui.getEvents().trigger("winNavigateDown");
      ui.getEvents().trigger("winNavigateDown");
      ui.getEvents().trigger("winConfirmSelection");

      verify(game).setScreen(ScreenType.MAIN_GAME);
    }
  }
}
