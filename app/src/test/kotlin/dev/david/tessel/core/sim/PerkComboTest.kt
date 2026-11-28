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
        assertEquals(18,PerkCombo.entries.flatMap{it.inputs}.toSet().size)
        assertEquals(6,PerkCombo.entries.count{it.inputs.size==2})
        assertEquals(6,PerkCombo.entries.count{it.inputs.size==3})
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
        assertEquals(listOf(MapLink(200,100),MapLink(201,100)),links)
        assertTrue(PerkTimeline.unrecorded(rounds,PerkCombo.LAST_LIGHT.mask).isEmpty())
        assertEquals(listOf(Perks.SECOND_WIND,Perks.LAST_STAND),PerkTimeline.unrecorded(List(9){-1},PerkCombo.LAST_LIGHT.mask))
    }

    @Test fun thirdRoundCompletesSolarRebirthAndAddsThirdLink(){
        val w=World(seed=12);val p=w.players
        p.perks[0]=PerkCombo.SOLAR_REBIRTH.mask
        p.recordReward(0,10,Perks.SECOND_WIND);p.recordReward(0,20,Perks.LAST_STAND);p.recordReward(0,30,Perks.VITAL_CORE)
        p.hp[0]=1
        val x=p.x[0];val y=p.y[0]
        w.hostile.add(1,x+150f,y,0f,0f,120,1f,5f,0,-1)
        w.hurtPlayer(0,Cause.shot(Bestiary.FANNER),loss=3,allowRam=false)
        assertEquals(3,p.hp[0]);assertEquals(0,w.hostile.life[0])
        val edges=PerkTimeline.links(p.rewardRounds[0].toList(),p.perks[0])
        assertEquals(3,edges.count{it.b==PerkTimeline.FUSION_BASE+PerkCombo.SOLAR_REBIRTH.ordinal})
    }
    @Test fun stormCellPhaseEscapeAndReprisalApplyEngineEffects(){
        val w=World(seed=13);val p=w.players
        p.perks[0]=PerkCombo.STORM_CELL.mask;p.sinceHit[0]=OVERCLOCK_TICKS
        w.hurtPlayer(0,Cause.shot(Bestiary.FANNER),loss=2,allowRam=false)
        assertEquals(3*TICK_HZ,p.surge[0])
        p.perks[0]=PerkCombo.PHASE_ESCAPE.mask or PerkCombo.REPRISAL.mask
        p.hp[0]=2;p.kills[0]=9;p.poisonTicks[0]=120;p.invuln[0]=0
        val e=w.spawnEnemy(Bestiary.PIP,p.x[0]+30f,p.y[0])
        w.damageEnemy(e,100000f,1f,0f,0)
        assertEquals(3,p.hp[0]);assertEquals(0,p.poisonTicks[0]);assertEquals(TICK_HZ,p.invuln[0])
    }
    @Test fun surveyorHorizonAndReclamationApplyRealStats(){
        val w=World(seed=14);val p=w.players
        val radius=p.magnet(0);val speed=p.moveSpeed(0)
        p.perks[0]=PerkCombo.HORIZON.mask
        assertEquals(radius*1.2f,p.magnet(0),.001f)
        assertEquals(speed*1.15f,p.moveSpeed(0),.001f)
        p.hp[0]--;assertEquals(speed,p.moveSpeed(0),.001f)
        w.devOpenShop();w.shop.devReward(w,dev.david.tessel.core.dev.DevReward.NONE)
        p.perks[0]=PerkCombo.RECLAMATION.mask
        val hp=p.hp[0];assertTrue(w.shop.reroll(w,0));assertEquals(hp+1,p.hp[0])
    }
}
