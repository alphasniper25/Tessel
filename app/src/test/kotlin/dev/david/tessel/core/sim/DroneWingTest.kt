package dev.david.tessel.core.sim
import dev.david.tessel.core.content.*
import org.junit.Assert.*
import org.junit.Test
class DroneWingTest {
 @Test fun physicalWingsAreCappedAndNeverEmitBullets(){val w=World(12);w.spawning=false;w.practice=true;w.players.evolution[0]=TankEvolution.MOTHERSHIP;val input=Array(MAX_PLAYERS){PlayerInput()};repeat(900){w.step(input)};assertEquals(8,w.droneWing.count(0));assertEquals(0,w.bullets.count);assertEquals(0,w.players.fired[0]);w.removePlayer(0);assertEquals(0,w.droneWing.count(0))}
 @Test fun reloadControlsReplenishment(){fun time(level:Int):Int{val w=World(12);w.spawning=false;w.practice=true;w.players.evolution[0]=TankEvolution.OVERSEER;w.players.levels[0][Upgrades.FIRE_RATE]=level;val input=Array(MAX_PLAYERS){PlayerInput()};w.step(input);w.droneWing.hp[0]=0;var n=0;while(w.droneWing.count(0)==0&&n<200){w.step(input);n++};return n};assertTrue(time(10)<time(0))}
}
