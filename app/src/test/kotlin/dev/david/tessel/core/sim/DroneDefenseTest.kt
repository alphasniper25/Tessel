package dev.david.tessel.core.sim
import dev.david.tessel.core.content.*
import org.junit.Assert.*
import org.junit.Test

class DroneDefenseTest {
    private val idle=Array(MAX_PLAYERS){PlayerInput()}
    private fun wingWorld(remote:Boolean=false):World {
        val w=World(seed=81L)
        if(remote){w.reset(2,81L);w.players.x[0]=200f;w.remote[1]=true}
        w.spawning=false
        w.players.evolution[if(remote)1 else 0]=TankEvolution.OVERSEER
        w.step(idle)
        return w
    }
    private fun placeDrone(w:World,p:Int,dx:Float,dy:Float):Int{
        val d=w.droneWing;val i=p*DroneWing.LIMIT
        for(k in 0 until DroneWing.LIMIT)d.hp[p*DroneWing.LIMIT+k]=0
        d.hp[i]=3;d.x[i]=w.players.x[p]+dx;d.y[i]=w.players.y[p]+dy
        d.prevX[i]=d.x[i];d.prevY[i]=d.y[i]
        return i
    }
    @Test fun sweptContactCatchesFastCrossingsAndRejectsMisses(){
        assertTrue(contactTime(0f,0f,100f,0f,50f,0f,50f,0f,10f) in .39f..41e-2f)
        assertEquals(Float.POSITIVE_INFINITY,contactTime(0f,30f,100f,30f,50f,0f,50f,0f,10f),0f)
        assertEquals(0f,contactTime(0f,0f,0f,0f,0f,0f,0f,0f,10f),0f)
    }
    @Test fun shotCrossingDroneIsConsumedAndCostsOneHp(){
        val w=wingWorld();val p=w.players
        val slot=placeDrone(w,0,-55f,0f);val health=p.hp[0]
        w.hostile.add(101,p.x[0]-100f,p.y[0],7200f,0f,20,1f,5.5f,0,-1)
        w.events.clear();w.step(idle)
        assertEquals(health,p.hp[0]);assertEquals(2,w.droneWing.hp[slot])
        assertEquals(0,w.hostile.count);assertTrue(w.events.has(Events.DRONE_BLOCK))
        assertTrue(w.droneWing.interceptedRecently(101,w.tick))
    }
    @Test fun droneBehindTankCannotUndoEarlierHit(){
        val w=wingWorld();val p=w.players
        val slot=placeDrone(w,0,55f,0f);val hp=p.hp[0]
        w.hostile.add(102,p.x[0]-100f,p.y[0],12000f,0f,20,1f,5.5f,0,-1)
        w.events.clear();w.step(idle)
        assertTrue(p.hp[0]<hp);assertEquals(3,w.droneWing.hp[slot])
        assertFalse(w.events.has(Events.DRONE_BLOCK))
    }
    @Test fun remoteDronePilotIsProtectedByHostAndCannotReportPhantomProjectileHit(){
        val w=wingWorld(true);val p=w.players;val slot=placeDrone(w,1,-55f,0f);val hp=p.hp[1]
        w.hostile.add(103,p.x[1]-100f,p.y[1],7200f,0f,20,1f,5.5f,0,-1)
        w.remoteHurt(1,103);assertEquals(hp,p.hp[1])
        w.events.clear();w.step(idle)
        assertEquals(hp,p.hp[1]);assertEquals(2,w.droneWing.hp[slot]);assertTrue(w.events.has(Events.DRONE_BLOCK))
        w.remoteHurt(1,103);assertEquals(hp,p.hp[1])
    }
    @Test fun remoteDronePilotTakesRealUnblockedShot(){
        val w=wingWorld(true);val p=w.players;placeDrone(w,1,55f,0f)
        val hp=p.hp[1]
        w.hostile.add(104,p.x[1]-100f,p.y[1],7200f,0f,20,1f,5.5f,0,-1)
        w.step(idle);assertTrue(p.hp[1]<hp)
    }
    @Test fun aimingCommandsPhysicalFormationAndReleaseReturnsIt(){
        val w=wingWorld();val p=w.players
        repeat(500){w.step(idle)}
        val command=Array(MAX_PLAYERS){PlayerInput()}
        command[0].fire=true;command[0].aimX=1f
        repeat(150){w.step(command)}
        val d=w.droneWing
        assertTrue((0 until 4).all{d.x[it]>p.x[0]+150f})
        repeat(180){w.step(idle)}
        assertTrue((0 until 4).all{kotlin.math.hypot(d.x[it]-p.x[0],d.y[it]-p.y[0])<110f})
        assertEquals(0,w.bullets.count);assertTrue(d.count(0)<=8)
    }
}
