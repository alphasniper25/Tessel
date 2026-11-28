package dev.david.tessel.core.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SynthTest {
    @Test
    fun everySoundHasSamplesWithinRangeAndIsNotSilent() {
        for (sound in Synth.Sound.entries) {
            val s = Synth.make(sound)
            assertTrue("$sound has samples", s.size > Synth.RATE / 50)
            assertTrue("$sound is audible", s.any { kotlin.math.abs(it.toInt()) > 500 })
            assertTrue("$sound is not clipped hard", s.all { kotlin.math.abs(it.toInt()) < 32000 })
        }
    }

    @Test
    fun theWavHasAProperHeaderAndSize() {
        val samples = Synth.make(Synth.Sound.COIN)
        val wav = Synth.wav(samples)
        assertEquals("RIFF", String(wav, 0, 4))
        assertEquals("WAVE", String(wav, 8, 4))
        assertEquals(44 + samples.size * 2, wav.size)
    }

    @Test fun allClipsHaveSmoothBoundariesAndDistinctSignatures() {
        val clips=Synth.Sound.entries.map{Synth.make(it)}
        for(clip in clips){
            assertEquals(0,clip.first().toInt());assertEquals(0,clip.last().toInt())
            assertTrue(clip.size <= Synth.RATE)
        }
        assertEquals(clips.size,clips.map{it.contentHashCode()}.toSet().size)
    }
    @Test fun weaponVoicesFollowFrames() {
        val F=dev.david.tessel.core.content.TankEvolution.BASIC
        assertEquals(Synth.Sound.SHOT,Synth.weapon(F))
        assertEquals(Synth.Sound.CANNON,Synth.weapon(dev.david.tessel.core.content.TankEvolution.DESTROYER))
        assertEquals(Synth.Sound.SNIPER,Synth.weapon(dev.david.tessel.core.content.TankEvolution.RANGER))
        assertEquals(Synth.Sound.RAPID,Synth.weapon(dev.david.tessel.core.content.TankEvolution.MINIGUN))
        assertEquals(Synth.Sound.DRONE,Synth.weapon(dev.david.tessel.core.content.TankEvolution.OVERSEER))
        assertEquals(Synth.Sound.RAM,Synth.weapon(dev.david.tessel.core.content.TankEvolution.SPIKE))
        assertEquals(Synth.Sound.RAIL,Synth.weapon(dev.david.tessel.core.content.TankEvolution.PRISM))
    }
}
