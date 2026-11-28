package dev.david.tessel.core.sim

import dev.david.tessel.core.content.*
import org.junit.Assert.*
import org.junit.Test

class CodexDemoTest {
    @Test fun everyFrameRunsEightSecondCombatAndRestartsWithoutChangingFrame() {
        for(frame in TankEvolution.entries) {
            val demo=CodexDemo(frame)
            val w=demo.world
            var maxDrones=0;var maxBullets=0
            repeat(CodexDemo.LOOP_TICKS/2) {
                demo.advance(2);maxDrones=maxOf(maxDrones,w.droneWing.count(0));maxBullets=maxOf(maxBullets,w.bullets.count)
            }
            assertEquals(frame.label,frame,w.players.evolution[0])
            assertEquals(frame.label,Phase.FIGHT,w.phase)
            if(frame.drones) {
                assertTrue(frame.label,maxDrones in 1..frame.droneSpec!!.cap)
                assertEquals(frame.label,0,maxBullets)
            } else if(frame.melee) {
                assertEquals(frame.label,0,maxBullets)
                assertTrue(frame.label,w.players.dealt[0]>0f)
            } else assertTrue(frame.label,w.players.fired[0]>0)
            assertTrue(frame.label,demo.advance(2));assertEquals(1,demo.loops)
            assertEquals(frame,w.players.evolution[0])
        }
    }
    @Test fun filtersIncludeAllFramesAndRetainSecretAndDroneOverlap() {
        assertTrue(TankEvolution.entries.all{BuildFilter.ALL.matches(it)})
        assertEquals(5,TankEvolution.entries.count{BuildFilter.DRONE.matches(it)})
        assertTrue(BuildFilter.SECRET.matches(TankEvolution.DREADNOUGHT))
        assertTrue(BuildFilter.DRONE.matches(TankEvolution.DREADNOUGHT))
        assertFalse(CodexCatalog.compatible(Upgrades.PIERCE,BuildFilter.DRONE))
        assertTrue(CodexCatalog.compatible(Upgrades.DRONE_POWER,BuildFilter.DRONE))
        assertFalse(CodexCatalog.compatible(Upgrades.MULTISHOT,BuildFilter.ALL))
    }
}
