package dev.david.tessel.core.sim

import dev.david.tessel.core.content.*
import org.junit.Assert.*
import org.junit.Test

class PerkComboTest {
    @Test fun everyFusionNeedsAllItsPerksAndDisablesOnRemoval(){
        for(c in PerkCombo.entries){
            assertFalse(c.active(0));assertFalse(c.active(1 shl c.first));assertFalse(c.active(1 shl c.second))
            assertTrue(c.active(c.mask));assertEquals(c.inputs.size,c.progress(c.mask))
            for(id in c.inputs)assertFalse(c.active(c.mask and (1 shl id).inv()))
            assertEquals(c.inputs.size,c.inputs.map{PerkRounds.roundOf(it)}.distinct().size)
        }
        assertEquals(5,PerkCombo.entries.size)
        assertEquals(3,PerkCombo.entries.count{it.inputs.size==2})
        assertEquals(2,PerkCombo.entries.count{it.inputs.size==3})
        var pairs=0;var triples=0
        for(a in PerkRounds.pools[0])for(b in PerkRounds.pools[1]){
            val mask=(1 shl a) or (1 shl b)
            if(PerkCombo.entries.any{it.active(mask)})pairs++
            for(c in PerkRounds.pools[2]){
                val complete=mask or (1 shl c)
                if(PerkCombo.entries.any{it.inputs.size==3&&it.active(complete)})triples++
            }
        }
        assertEquals(3,pairs);assertEquals(2,triples)
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
        assertEquals(listOf(MapLink(PerkTimeline.perkNode(Perks.SECOND_WIND),100),MapLink(PerkTimeline.perkNode(Perks.LAST_STAND),100)),links)
        assertTrue(PerkTimeline.unrecorded(rounds,PerkCombo.LAST_LIGHT.mask).isEmpty())
        assertEquals(listOf(Perks.SECOND_WIND,Perks.LAST_STAND),PerkTimeline.unrecorded(List(9){-1},PerkCombo.LAST_LIGHT.mask))
    }

    @Test fun tripleFusionCompletesOnlyWithThirdRoundAndLinksAllThreeActualNodes(){
        val w=World(seed=13);val p=w.players
        val combo=PerkCombo.PHASE_ESCAPE
        p.recordReward(0,10,Perks.SIPHON);p.recordReward(0,20,Perks.QUICK_FEET)
        p.perks[0]=(1 shl Perks.SIPHON) or (1 shl Perks.QUICK_FEET)
        assertTrue(PerkTimeline.links(p.rewardRounds[0].toList(),p.perks[0]).isEmpty())
        p.perks[0]=combo.mask;p.recordReward(0,30,Perks.AFTERBURNER)
        p.hp[0]=2;p.kills[0]=9;p.poisonTicks[0]=120;p.stun[0]=20;p.invuln[0]=0
        val e=w.spawnEnemy(Bestiary.PIP,p.x[0]+30f,p.y[0])
        w.damageEnemy(e,100000f,1f,0f,0)
        assertEquals(0,p.poisonTicks[0]);assertEquals(0,p.stun[0]);assertEquals(TICK_HZ,p.invuln[0])
        assertEquals(combo.inputs.map{MapLink(PerkTimeline.perkNode(it),PerkTimeline.FUSION_BASE+combo.ordinal)},
            PerkTimeline.links(p.rewardRounds[0].toList(),p.perks[0]))
    }
    @Test fun reprisalHealsAtHalfOnlyWithAllThreePerks(){
        val w=World(seed=14);val p=w.players
        p.perks[0]=PerkCombo.REPRISAL.mask;p.hp[0]=2;p.kills[0]=9
        val e=w.spawnEnemy(Bestiary.PIP,p.x[0]+30f,p.y[0])
        w.damageEnemy(e,100000f,1f,0f,0);assertEquals(3,p.hp[0])
        p.perks[0]=p.perks[0] and (1 shl Perks.DEADEYE).inv()
        p.kills[0]=19;p.hp[0]=2
        val e2=w.spawnEnemy(Bestiary.PIP,p.x[0]+50f,p.y[0])
        w.damageEnemy(e2,100000f,1f,0f,0);assertEquals(2,p.hp[0])
    }
    @Test fun allEighteenPerksAreOnThreeNonOverlappingConcentricRings(){
        val pts=Perks.runIds.map{PerkTimeline.perkPoint(it)}
        for(id in Perks.runIds){
            val (x,y)=PerkTimeline.perkPoint(id)
            assertEquals(PerkTimeline.radii[PerkRounds.roundOf(id)],kotlin.math.hypot(x,y),.001f)
        }
        for(i in pts.indices)for(j in i+1 until pts.size)
            assertTrue(kotlin.math.hypot(pts[i].first-pts[j].first,pts[i].second-pts[j].second)>2*PerkTimeline.NODE_RADIUS)
        for(i in PerkCombo.entries.indices){
            val (x,y)=PerkTimeline.fusionPoint(i)
            assertTrue(kotlin.math.hypot(x,y)>PerkTimeline.radii.last()+2*PerkTimeline.NODE_RADIUS)
        }
    }
}
