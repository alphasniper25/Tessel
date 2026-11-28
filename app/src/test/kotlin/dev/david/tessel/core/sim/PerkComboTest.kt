package dev.david.tessel.core.sim

import dev.david.tessel.core.content.*
import org.junit.Assert.*
import org.junit.Test

class PerkComboTest {
    @Test fun everyComboRequiresBothPerksAndDisablesOnRemoval(){
        for(c in PerkCombo.entries){
            assertFalse(c.active(0));assertFalse(c.active(1 shl c.first));assertFalse(c.active(1 shl c.second))
            assertTrue(c.active(c.mask));assertEquals(2,c.progress(c.mask))
            assertFalse(c.active(c.mask and (1 shl c.first).inv()))
        }
        assertEquals(10,PerkCombo.entries.flatMap{listOf(it.first,it.second)}.toSet().size)
    }
    @Test fun lastLightAddsDamageOnlyAtLowHealth(){
        val p=PlayerState().apply{reset(1)}
        p.perks[0]=1 shl Perks.LAST_STAND;p.hp[0]=2
        val before=p.damage(0)
        p.perks[0]=PerkCombo.LAST_LIGHT.mask
        assertEquals(before*1.15f,p.damage(0),.001f)
        p.hp[0]=3;val full=p.damage(0);p.perks[0]=1 shl Perks.LAST_STAND
        assertEquals(full,p.damage(0),.001f)
    }
    @Test fun reactorOnlyReducesPenaltyDuringOverclock(){
        val p=PlayerState().apply{reset(1)}
        p.perks[0]=PerkCombo.REACTOR_SYNC.mask;p.sinceHit[0]=0
        val low=p.fireRate(0);p.sinceHit[0]=60*60
        assertEquals(low/.85f*.93f*1.3f,p.fireRate(0),.001f)
    }
    @Test fun fieldRunnerAndReactivePlatingHaveRealModifiers(){
        val p=PlayerState().apply{reset(1)}
        assertEquals(25,p.siphonInterval(0));assertEquals(1f,p.shockwaveFactor(0),0f)
        p.perks[0]=PerkCombo.FIELD_RUNNER.mask or PerkCombo.REACTIVE_PLATING.mask
        assertEquals(20,p.siphonInterval(0));assertEquals(1.4f,p.shockwaveFactor(0),.0001f)
    }
    @Test fun salvageContractUsesNewQuoteButPreservesLockedPrice(){
        val w=World(seed=12);w.reset(seed=12);w.skipTo(30);w.devOpenShop()
        w.shop.devReward(w,dev.david.tessel.core.dev.DevReward.NONE)
        w.shop.devStock(w,listOf(CardCode.pack(Upgrades.DAMAGE,CardRarity.COMMON),CardCode.pack(Upgrades.SPEED,CardRarity.COMMON),-1))
        w.players.perks[0]=1 shl Perks.HAGGLER
        val locked=w.shop.price(w,0,0);val normal=w.shop.price(w,0,1)
        assertTrue(w.shop.setLocked(w,0,0,true))
        w.players.perks[0]=PerkCombo.SALVAGE_CONTRACT.mask
        assertEquals(locked,w.shop.price(w,0,0));assertTrue(w.shop.price(w,0,1)<normal)
    }
}
