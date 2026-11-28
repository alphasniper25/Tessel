package dev.david.tessel.platform.game
import dev.david.tessel.platform.store.*
import org.junit.Assert.*
import org.junit.Test

class RenderPacerTest{
    @Test fun displayNativeDoesNotApplyAnArtificialFrameCap(){
        val pacer=RenderPacer()
        repeat(120){assertTrue(pacer.due(1_000_000_000L+it*8_333_333L,0))}
    }
    @Test fun thirtyAndSixtyTargetsWorkOn120Hz(){
        for(target in listOf(30,60)){
            val pacer=RenderPacer();var n=0
            repeat(120){if(pacer.due(1_000_000_000L+it*8_333_333L,target))n++}
            assertEquals(target,n)
        }
    }
    @Test fun sixtyTargetOn90HzDoesNotFallTo45(){
        val pacer=RenderPacer();var n=0
        repeat(90){if(pacer.due(1_000_000_000L+it*11_111_111L,60))n++}
        assertEquals(60,n)
    }
    @Test fun forceModeChangeDrawAndThenResumeTarget(){
        val p=RenderPacer()
        assertTrue(p.due(1_000_000_000L,30))
        assertFalse(p.due(1_008_333_333L,30))
        assertTrue(p.due(1_008_333_333L,20,true))
        assertFalse(p.due(1_016_666_666L,20))
        assertTrue(p.due(1_025_000_000L,0))
    }
    @Test fun presetsPreserveGameplayAndAccessibilityChoices(){
        val custom=Settings(leftHanded=true,stickSize=StickSize.LARGE,difficulty=dev.david.tessel.core.content.Difficulty.HARD,cameraZoom=1.85f)
        for(p in PerformancePreset.entries){
            val next=p.apply(custom)
            assertEquals(custom.difficulty,next.difficulty);assertEquals(custom.leftHanded,next.leftHanded)
            assertEquals(custom.stickSize,next.stickSize);assertEquals(custom.cameraZoom,next.cameraZoom,0f)
            assertTrue(p.matches(next))
        }
    }
}
