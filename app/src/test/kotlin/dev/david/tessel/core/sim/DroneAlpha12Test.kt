package dev.david.tessel.core.sim

import dev.david.tessel.core.content.*
import org.junit.Assert.*
import org.junit.Test

class DroneAlpha12Test {
    @Test fun capAndPhysicalAttacksAcrossAllFiveDroneFrames() {
        for(frame in TankEvolution.entries.filter{it.drones}) {
            val w=World(seed=24L);w.spawning=false;w.practice=true;w.players.evolution[0]=frame
            val inputs=Array(MAX_PLAYERS){PlayerInput()}
            repeat(480){w.step(inputs)}
            assertEquals(frame.label,frame.droneSpec!!.cap,w.droneWing.count(0))
            assertEquals(0,w.bullets.count)
            val j=w.spawnEnemy(Bestiary.SLAB,w.players.x[0]+110f,w.players.y[0])
            w.enemies.hp[j]=10000f;w.enemies.maxHp[j]=10000f
            repeat(180){w.step(inputs)}
            assertTrue(frame.label,w.players.dealt[0]>0f)
            assertEquals(0,w.bullets.count)
            assertTrue(w.droneWing.count(0)<=8)
        }
    }
}
