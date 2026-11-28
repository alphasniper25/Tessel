package dev.david.tessel.ui.screens

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import dev.david.tessel.core.stats.Ledger
import dev.david.tessel.ui.theme.TesselTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35],qualifiers="w400dp-h850dp-port-night-420dpi")
class ArsenalPortraitTest {
    @get:Rule val compose=createAndroidComposeRule<ComponentActivity>()
    @Test fun portraitListUsesFullWidthRowsAndCardDetailsRemainReadable(){
        compose.setContent{TesselTheme{CardCollectionScreen(Ledger(),onBack={})}}
        compose.onNodeWithText("Machine Gun").assertIsDisplayed()
        val a=compose.onNodeWithText("Machine Gun").getUnclippedBoundsInRoot()
        val b=compose.onNodeWithText("Sniper").getUnclippedBoundsInRoot()
        assertTrue(b.top>a.bottom)
        compose.onNodeWithText("Cards").performClick()
        compose.onNodeWithText("Damage").performClick()
        compose.onNodeWithText("Run rank limit").assertIsDisplayed()
        compose.onNodeWithText("Arsenal").performClick()
        compose.onNodeWithText("Cards").assertIsDisplayed()
    }
    @Test fun allTankDetailsUseDiagramAndAutomaticPreviewWithoutPlaybackButton(){
        compose.setContent{TesselTheme{CardCollectionScreen(Ledger(),onBack={})}}
        compose.onNodeWithText("Machine Gun").performClick()
        compose.onNodeWithTag("tank-diagram").assertIsDisplayed()
        compose.onNodeWithText("Base HP").assertIsDisplayed()
        compose.onNodeWithText("Shot damage").assertIsDisplayed()
        compose.onNodeWithTag("combat-preview").assertIsDisplayed()
        compose.onNodeWithText("Pause").assertDoesNotExist()
        compose.onNodeWithText("Arsenal").performClick()
        compose.onNodeWithText("Machine Gun").assertIsDisplayed()
    }
}
