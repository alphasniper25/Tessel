package dev.david.tessel.core.sim

import dev.david.tessel.core.content.*
import org.junit.Assert.*
import org.junit.Test

class ArsenalFrameStatsTest {
    @Test fun allThirtyThreeFramesUseTheSameFourCalloutsAndCorrectCombatStats(){
        assertEquals(33,TankEvolution.entries.size)
        for(frame in TankEvolution.entries){
            val spec=ArsenalFrameStats.forFrame(frame)
            val p=PlayerState().apply{reset(1);evolution[0]=frame}
            val main=spec.primary.associate{it.label to it.value}
            val detail=spec.secondary.associate{it.label to it.value}
            assertEquals(frame.label,4,main.size)
            assertEquals(Balance.PLAYER_HP+frame.hullBonus,main.getValue("Base HP").toInt())
            assertEquals(p.moveSpeed(0),main.getValue("Movement").substringBefore(" ").toFloat(),.011f)
            if(frame.drones){
                val d=frame.droneSpec!!
                assertEquals(p.damage(0)*d.impact,main.getValue("Drone impact").toFloat(),.011f)
                assertEquals(d.cap,detail.getValue("Drone capacity").substringBefore(" ").toInt())
                assertEquals(d.hp,detail.getValue("Drone health").substringBefore(" ").toInt())
                assertTrue("Shot damage" !in main && "Barrels" !in detail)
                assertEquals((d.respawn*TICK_HZ).toInt().coerceAtLeast(30)/TICK_HZ.toFloat(),main.getValue("Replacement").substringBefore(" ").toFloat(),.011f)
            }else if(frame.melee){
                assertEquals(p.ramDamage(0),main.getValue("Ram impact").toFloat(),.011f)
                assertEquals(p.shieldTicks(0)/TICK_HZ.toFloat(),main.getValue("Shield recharge").substringBefore(" ").toFloat(),.011f)
                assertTrue("Shot damage" !in main && "Barrels" !in detail)
                assertTrue("Drone capacity" !in detail)
            }else{
                assertEquals(p.damage(0),main.getValue("Shot damage").toFloat(),.011f)
                assertEquals(p.fireRate(0),main.getValue("Volley rate").substringBefore("/").toFloat(),.011f)
                assertEquals(p.volley(0),detail.getValue("Barrels").toInt())
                assertTrue("Drone capacity" !in detail && "Ram impact" !in main)
            }
            if(frame.secret)assertEquals("Clear stage 40 as Basic",detail["Unlock"])
            assertTrue(spec.limitation.isNotBlank())
        }
    }
}
