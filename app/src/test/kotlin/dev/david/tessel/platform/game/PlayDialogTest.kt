package dev.david.tessel.platform.game

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.click
import androidx.compose.ui.geometry.Offset
import dev.david.tessel.core.content.SkillGraph
import dev.david.tessel.ui.screens.SkillMapCamera
import dev.david.tessel.core.content.Difficulty
import dev.david.tessel.core.sim.Checkpoint
import dev.david.tessel.platform.net.CoopLobby
import dev.david.tessel.platform.store.LedgerStore
import dev.david.tessel.platform.store.SettingsStore
import dev.david.tessel.platform.store.SoloSaves
import dev.david.tessel.platform.update.Updater
import dev.david.tessel.ui.TesselApp
import dev.david.tessel.ui.theme.TesselTheme
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.nio.file.Files

/**
 * The home screen with the real app shell on top: what a tap on Play does
 * with runs waiting, and that the dialog it opens can be reached.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w914dp-h411dp-land-night-420dpi")
class PlayDialogTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    private lateinit var dir: File
    private lateinit var game: GameSurfaceView
    private lateinit var saves: SoloSaves
    private val orientationRequests=mutableListOf<Boolean>()
    private fun tapSkill(id:Int){
        val n=SkillGraph.nodes[id]
        compose.onNodeWithTag("skill-map").performTouchInput {
            val camera=SkillMapCamera()
            click(Offset(camera.sx(n.x,width.toFloat(),height.toFloat()),camera.sy(n.y,width.toFloat(),height.toFloat())))
        }
        compose.waitForIdle()
    }

    @Before
    fun setUp() {
        dir = Files.createTempDirectory("tessel-dialog").toFile()
        game = GameSurfaceView(compose.activity)
        saves = SoloSaves(dir)
        game.saves = saves
        val ledger = LedgerStore(File(dir, "ledger.dat"))
        game.ledger = ledger
        val lobby = CoopLobby(compose.activity, game)
        val store = SettingsStore(compose.activity)
        store.update { it.copy(tutorialSeen = true) }
        val updater = Updater(compose.activity)
        compose.setContent { TesselTheme { TesselApp(game, lobby, store, ledger, saves, updater, null,onArsenalOrientation={orientationRequests.add(it)}) } }
    }

    @After
    fun tearDown() {
        dir.deleteRecursively()
    }

    private fun waitForMode(mode: Mode) {
        val until = System.currentTimeMillis() + 5_000
        while (game.mode.value != mode) {
            compose.waitForIdle()
            if (System.currentTimeMillis() > until) throw AssertionError("still ${game.mode.value}, wanted $mode")
            Thread.sleep(10)
        }
    }

    @Test
    @Config(qualifiers = "w740dp-h320dp-land-night-420dpi")
    fun newRunActionIsFullyVisibleOnShortLandscapeWithoutScrolling() {
        compose.mainClock.advanceTimeBy(1000)
        compose.onNodeWithText("Play").performClick()
        compose.mainClock.advanceTimeBy(1500)
        compose.onNodeWithText("Rapid").performClick()
        val preview=compose.onNodeWithText("Fire 150%",substring=true)
        preview.assertIsDisplayed()
        val previewBounds=preview.getUnclippedBoundsInRoot()
        val screen=compose.onRoot().getUnclippedBoundsInRoot()
        assertTrue("Tank stat row must fit: $previewBounds / $screen",previewBounds.bottom<=screen.bottom)
        val action = compose.onNodeWithText("New Run")
        action.assertIsDisplayed()
        val bounds = action.getUnclippedBoundsInRoot()
        val root = compose.onRoot().getUnclippedBoundsInRoot()
        assertTrue("New Run must be above the bottom edge: $bounds / $root", bounds.bottom <= root.bottom)
        action.performClick()
        waitForMode(Mode.PLAYING)
    }

    @Test
    fun withNothingSavedPlayStillOpensTheDialogWhereTheDifficultyIsChosen() {
        compose.mainClock.advanceTimeBy(1000)
        compose.onNodeWithText("Play").performClick()
        compose.mainClock.advanceTimeBy(1500)
        compose.waitForIdle()
        compose.onAllNodesWithText("New Run").onLast().performClick()
        waitForMode(Mode.PLAYING)
    }

    @Test
    fun withARunSavedPlayOpensADialogThatNewRunCanBePressedIn() {
        saves.write(1, Checkpoint(Difficulty.NORMAL, 4, 4 * 3900, 1, true, 0))
        compose.mainClock.advanceTimeBy(1000)
        compose.onNodeWithText("Play").performClick()
        compose.mainClock.advanceTimeBy(1500)
        compose.waitForIdle()
        // The title says New Run too; the button is the last of them.
        compose.onAllNodesWithText("New Run").onLast().performClick()
        waitForMode(Mode.PLAYING)
        // The new run takes its own slot a moment after it starts.
        val until = System.currentTimeMillis() + 5_000
        while (saves.saves.value.size < 2 && System.currentTimeMillis() < until) Thread.sleep(10)
        assertEquals(2, saves.saves.value.size)
    }

    @Test
    fun aSavedRunCanBeContinuedFromTheDialog() {
        saves.write(1, Checkpoint(Difficulty.NORMAL, 4, 4 * 3900, 1, true, 0))
        compose.mainClock.advanceTimeBy(1000)
        compose.onNodeWithText("Play").performClick()
        compose.mainClock.advanceTimeBy(1500)
        compose.waitForIdle()
        compose.onNodeWithText("Continue").performClick()
        waitForMode(Mode.SHOP)
    }

    @Test
    fun skillTreeIsVisibleFromHomeBeforeAnyRunAndCanReturn() {
        compose.mainClock.advanceTimeBy(1000)
        compose.onNodeWithText("Skill Tree").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("0 / 60 unlocked").assertIsDisplayed()
        compose.onNodeWithText("6 / 60 discovered").assertIsDisplayed()
        compose.onNodeWithText("Quick Refresh").assertDoesNotExist()
        compose.onNodeWithText("+").performClick()
        compose.onNodeWithText("133%").assertIsDisplayed()
        compose.onNodeWithText("Fit").performClick()
        compose.onNodeWithText("100%").assertIsDisplayed()
        tapSkill(1)
        compose.onNodeWithText("Double Refresh").assertDoesNotExist()
        tapSkill(0)
        compose.onNodeWithText("Quick Refresh").assertIsDisplayed()
        compose.onNodeWithText("LOCKED · STAGE 3").assertIsDisplayed()
        compose.onNodeWithText("×").performClick()
        compose.onNodeWithText("Quick Refresh").assertDoesNotExist()
        compose.onNodeWithText("Home").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Play").assertIsDisplayed()
    }

    @Test
    fun codexCardCanOpenDetailsAndReturnToList() {
        compose.mainClock.advanceTimeBy(1000)
        compose.onNodeWithText("Arsenal").performScrollTo().performClick()
        compose.mainClock.advanceTimeBy(1000)
        compose.runOnIdle{assertEquals(true,orientationRequests.last())}
        compose.onNodeWithText("Cards").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Damage").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Run rank limit").assertIsDisplayed()
        compose.onNodeWithText("Arsenal").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Cards").assertIsDisplayed()
        compose.runOnIdle{assertEquals(true,orientationRequests.last())}
        compose.onNodeWithText("Home").performClick()
        compose.mainClock.advanceTimeBy(1000)
        compose.runOnIdle{assertEquals(false,orientationRequests.last())}
    }

    @Test
    fun savedStageThreeShopNeverOpensTreeAutomaticallyAndPurchaseCostsCoins(){
        val cp=Checkpoint(Difficulty.NORMAL,3,3*3900,1,true,0)
        cp.present[0]=true;cp.hp[0]=10;cp.maxHp[0]=10;cp.coins[0]=200
        saves.write(1,cp)
        compose.mainClock.advanceTimeBy(1000)
        compose.onNodeWithText("Play").performClick()
        compose.mainClock.advanceTimeBy(1500)
        compose.onNodeWithText("Continue").performClick()
        waitForMode(Mode.SHOP);compose.mainClock.advanceTimeBy(1000)
        compose.onNodeWithText("Next Stage").assertIsDisplayed()
        compose.onNodeWithText("Skills").performClick()
        compose.mainClock.advanceTimeBy(1000)
        tapSkill(0)
        val cost=SkillGraph.nodes[0].price
        compose.onNodeWithText("Unlock · $cost").performClick()
        val until=System.currentTimeMillis()+5000
        while(game.shop.value?.skillNodes!=1L&&System.currentTimeMillis()<until){compose.waitForIdle();Thread.sleep(10)}
        assertEquals(1L,game.shop.value?.skillNodes)
        assertEquals(200-cost,game.shop.value?.coins)
        compose.onNodeWithText("Unlocked").assertIsDisplayed()
        compose.onNodeWithText("8 / 60 discovered").assertIsDisplayed()
        compose.onNodeWithText("×").performClick()
        tapSkill(1)
        compose.onNodeWithText("Double Refresh").assertIsDisplayed()
        compose.onNodeWithText("LOCKED · STAGE 7").assertIsDisplayed()
        compose.onNodeWithText("×").performClick()
        tapSkill(3)
        compose.onNodeWithText("Triple Refresh").assertDoesNotExist()
        compose.onNodeWithText("Upgrades").performClick()
        compose.mainClock.advanceTimeBy(1000)
        compose.onNodeWithText("Next Stage").assertIsDisplayed()
    }

    @Test
    fun profileIsTheHomeEntryForSettingsAndUnconfiguredServices(){
        compose.mainClock.advanceTimeBy(1000)
        compose.onNodeWithContentDescription("Player profile").performClick()
        compose.mainClock.advanceTimeBy(1000)
        compose.onNodeWithText("Google sign-in").assertIsDisplayed()
        compose.onNodeWithText("Not connected · planned").assertIsDisplayed()
        compose.onNodeWithText("Google Play").assertIsDisplayed()
        compose.onNodeWithText("Settings").performScrollTo().performClick()
        compose.mainClock.advanceTimeBy(1000)
        compose.onNodeWithText("Profile").performClick()
        compose.mainClock.advanceTimeBy(1000)
        compose.onNodeWithText("Google sign-in").assertIsDisplayed()
        compose.onNodeWithText("Home").performClick()
        compose.mainClock.advanceTimeBy(1000)
        compose.onNodeWithText("Play").assertIsDisplayed()
    }
}
