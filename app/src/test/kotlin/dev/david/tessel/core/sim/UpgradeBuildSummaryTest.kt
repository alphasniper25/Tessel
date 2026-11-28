package dev.david.tessel.core.sim

import dev.david.tessel.core.content.*
import org.junit.Assert.*
import org.junit.Test

class UpgradeBuildSummaryTest{
    @Test fun onlyPurchasedRanksAppearInStats(){
        val empty=UpgradeBuild()
        assertTrue(empty.purchased().isEmpty())
        val levels=MutableList(Upgrades.count){0}
        levels[Upgrades.DAMAGE]=3
        levels[Upgrades.MAGNET]=1
        val build=UpgradeBuild(levels=levels.toList())
        val purchased=build.purchased()
        assertEquals(setOf(Upgrades.DAMAGE,Upgrades.MAGNET),purchased.map{it.node.upgrade}.toSet())
        assertEquals(3,purchased.first{it.node.upgrade==Upgrades.DAMAGE}.rank)
        assertEquals(1,purchased.first{it.node.upgrade==Upgrades.MAGNET}.rank)
        assertTrue(build.stats().any{it.label=="Shot damage"})
    }
    @Test fun tankSpecificNamesAndRanksUseTheActualBuild(){
        val levels=MutableList(Upgrades.count){0}
        levels[Upgrades.DRONE_POWER]=2
        levels[Upgrades.FIRE_RATE]=3
        val b=UpgradeBuild(frame=TankEvolution.OVERSEER,levels=levels.toList(),lastEvolution=15)
        assertEquals(3,b.purchased().first{it.node.upgrade==Upgrades.FIRE_RATE}.rank)
        assertEquals("Replacement rate",b.purchased().first{it.node.upgrade==Upgrades.FIRE_RATE}.title)
        assertTrue(b.stats().any{it.label=="Drone impact"})
        assertFalse(b.stats().any{it.label=="Shot damage"})
    }
}
