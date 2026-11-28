package dev.david.tessel.core.sim

import dev.david.tessel.core.content.*
import org.junit.Assert.*
import org.junit.Test

class PerkComboTest {
    @Test fun everyFusionNeedsBothPerksAndDisablesOnRemoval(){
        for(c in PerkCombo.entries){
            assertFalse(c.active(0));assertFalse(c.active(1 shl c.first));assertFalse(c.active(1 shl c.second))
            assertTrue(c.active(c.mask));assertEquals(2,c.progress(c.mask))
            assertFalse(c.active(c.mask and (1 shl c.first).inv()))
        }
        assertEquals(10,PerkCombo.entries.flatMap{listOf(it.first,it.second)}.toSet().size)
    }
    @Test fun phoenixPulseClearsNearbyShotsAndNegativeStatusWhenSecondWindTriggers(){
        val w=World(seed=12);val p=w.players
        p.perks[0]=PerkCombo.LAST_LIGHT.mask;p.hp[0]=1;p.poisonTicks[0]=120;p.stun[0]=20
        val x=p.x[0];val y=p.y[0]
        w.hostile.add(1,x+20f,y,0f,0f,120,1f,5f,0,-1)
        w.hostile.add(2,x+200f,y,0f,0f,120,1f,5f,0,-1)
        w.hurtPlayer(0,Cause.shot(Bestiary.FANNER),loss=3,allowRam=false)
        assertEquals(1,p.hp[0]);assertTrue(p.windUsed[0])
        assertEquals(0,p.poisonTicks[0]);assertEquals(0,p.stun[0])
        assertEquals(0,w.hostile.life[0]);assertTrue(w.hostile.life[1]>0)
    }
    @Test fun capacitorAbsorbsOnePointAndThenNeedsAnotherEightSeconds(){
        val w=World(seed=12);val p=w.players;p.perks[0]=PerkCombo.REACTOR_SYNC.mask;p.sinceHit[0]=OVERCLOCK_TICKS
        val before=p.hp[0]
        w.hurtPlayer(0,Cause.shot(Bestiary.FANNER),loss=2,allowRam=false)
        assertEquals(before-1,p.hp[0]);assertFalse(p.capacitorReady(0))
        w.hurtPlayer(0,Cause.shot(Bestiary.FANNER),loss=2,allowRam=false)
        assertEquals(before-3,p.hp[0])
    }
    @Test fun purgeProtocolClearsDebuffsOnTenthKillAndLivingArmorRegeneratesAtHalf(){
        val w=World(seed=12);val p=w.players
        p.perks[0]=PerkCombo.FIELD_RUNNER.mask or PerkCombo.REACTIVE_PLATING.mask
        p.kills[0]=9;p.poisonTicks[0]=120;p.stun[0]=20
        val e=w.spawnEnemy(Bestiary.PIP,p.x[0]+30f,p.y[0])
        w.damageEnemy(e,100000f,1f,0f,0)
        assertEquals(10,p.kills[0]);assertEquals(0,p.poisonTicks[0]);assertEquals(0,p.stun[0])
        p.hp[0]=p.maxHp[0];assertEquals(0,p.regenSeconds(0))
        p.hp[0]=p.maxHp[0]/2;assertEquals(8,p.regenSeconds(0))
        p.evolution[0]=TankEvolution.RAMMER;assertEquals(4,p.regenSeconds(0))
    }
    @Test fun salvageCacheAddsOneFreeRefreshAndPreservesSpentRefreshes(){
        val w=World(seed=12);w.reset(seed=12);w.devOpenShop()
        w.shop.devReward(w,dev.david.tessel.core.dev.DevReward.NONE)
        w.players.perks[0]=PerkCombo.SALVAGE_CONTRACT.mask
        assertEquals(1,w.shop.freeRefreshesLeft(w,0));assertEquals(0,w.shop.rerollPrice(w,0))
        assertTrue(w.shop.reroll(w,0));assertEquals(0,w.shop.freeRefreshesLeft(w,0))
        val cp=Checkpoint.read(w.capture().write())!!
        assertEquals(1,cp.freeRerollsUsed[0])
    }
    @Test fun fusionLinksOnlyAppearWithBothRecordedComponents(){
        val rounds=MutableList(RewardRounds.COUNT){-1}
        rounds[1]=Perks.SECOND_WIND;rounds[3]=Perks.LAST_STAND
        assertTrue(PerkTimeline.links(rounds,1 shl Perks.SECOND_WIND).isEmpty())
        val links=PerkTimeline.links(rounds,PerkCombo.LAST_LIGHT.mask)
        assertEquals(listOf(MapLink(201,100),MapLink(203,100)),links)
        assertTrue(PerkTimeline.unrecorded(rounds,PerkCombo.LAST_LIGHT.mask).isEmpty())
        assertEquals(listOf(Perks.SECOND_WIND,Perks.LAST_STAND),PerkTimeline.unrecorded(List(9){-1},PerkCombo.LAST_LIGHT.mask))
    }
}
