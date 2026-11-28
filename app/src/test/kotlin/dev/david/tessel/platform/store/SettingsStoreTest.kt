package dev.david.tessel.platform.store

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SettingsStoreTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun breakRemindersAndTheTutorialFlagComeBackAfterARestart() {
        val first = SettingsStore(context)
        assertEquals(0, first.settings.value.breakEvery)
        assertFalse(first.settings.value.tutorialSeen)
        first.update { it.copy(breakEvery = 45, tutorialSeen = true) }
        val again = SettingsStore(context)
        assertEquals(45, again.settings.value.breakEvery)
        assertTrue(again.settings.value.tutorialSeen)
    }

    @Test
    fun developerOptionsComeBackAndResetClearsThem() {
        val first = SettingsStore(context)
        val dev = Dev(off = setOf(3, 4), boss = 18, tweaks = mapOf(5 to Tweak(2f, 5f, 3)), startStage = 20, startCoins = 5_000, invincible = true, noSpawns = true, speed = 2f)
        first.update { it.copy(devUnlocked = true, dev = dev) }
        val again = SettingsStore(context)
        assertEquals(dev, again.settings.value.dev)
        assertTrue(again.settings.value.devUnlocked)
        assertTrue(dev.active)
        again.update { it.copy(dev = Dev()) }
        assertFalse(SettingsStore(context).settings.value.dev.active)
        assertTrue("reset leaves the page unlocked", SettingsStore(context).settings.value.devUnlocked)
    }

    @Test fun performancePresetAndIndividualOverridesSurviveRestart(){
        val store=SettingsStore(context)
        store.update{PerformancePreset.SAVER.apply(it)}
        val low=SettingsStore(context).settings.value
        assertTrue(PerformancePreset.SAVER.matches(low))
        assertEquals(FpsLimit.THIRTY,low.fpsLimit);assertFalse(low.vibration);assertFalse(low.menuSounds)
        store.update{it.copy(fpsLimit=FpsLimit.NATIVE,vibration=true)}
        val custom=SettingsStore(context).settings.value
        assertEquals(FpsLimit.NATIVE,custom.fpsLimit);assertEquals(EffectsQuality.LOW,custom.effectsQuality)
        assertTrue(custom.vibration);assertTrue(PerformancePreset.entries.none{it.matches(custom)})
    }

    @Test fun shopModeAndInfiniteTrialPersistButTurningDeveloperModeOffRestoresDefault(){
        val store=SettingsStore(context)
        assertEquals(dev.david.tessel.core.content.ShopMode.TREE,store.settings.value.effectiveShopMode)
        store.update{it.copy(developerMode=true,shopMode=dev.david.tessel.core.content.ShopMode.CLASSIC,dev=it.dev.copy(infiniteMoney=true))}
        val restored=SettingsStore(context)
        assertTrue(restored.settings.value.infiniteTrial)
        assertEquals(dev.david.tessel.core.content.ShopMode.CLASSIC,restored.settings.value.effectiveShopMode)
        restored.update{it.copy(developerMode=false)}
        assertFalse(restored.settings.value.infiniteTrial)
        assertEquals(dev.david.tessel.core.content.ShopMode.TREE,restored.settings.value.effectiveShopMode)
    }
}
