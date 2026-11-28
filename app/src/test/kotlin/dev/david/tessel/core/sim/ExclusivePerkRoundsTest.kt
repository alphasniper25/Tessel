package dev.david.tessel.core.sim

import dev.david.tessel.core.content.*
import org.junit.Assert.*
import org.junit.Test

class ExclusivePerkRoundsTest{
    @Test fun eighteenPerksHaveOneAndOnlyOneRound(){
        assertEquals(18,Perks.runIds.size)
        assertEquals(Perks.runIds.toSet(),PerkRounds.pools.flatten().toSet())
        assertEquals(18,PerkRounds.pools.flatten().distinct().size)
        assertTrue(PerkRounds.pools.all{it.size==6})
    }
    @Test fun threeBossShopsOfferOnlyTheirPoolAndNeverRepeatAChoice(){
        for((round,stage) in PerkRounds.stages.withIndex()){
            val w=World(seed=19L+stage);w.skipTo(stage);w.devOpenShop()
            val offers=(0 until Perks.OFFERED).map{w.shop.perkOffer(0,it)}
            assertEquals(3,offers.distinct().size)
            assertTrue(offers.all{it in PerkRounds.pools[round]})
            assertTrue(w.shop.pickPerk(w,0,0));assertFalse(w.shop.pickPerk(w,0,1))
            assertEquals(offers[0],PerkRounds.choice(w.players.rewardRounds[0].toList(),round))
            w.shop.open(w);assertFalse(w.shop.perkPending(0))
            val cp=Checkpoint.read(w.capture().write())!!
            assertEquals(offers[0],PerkRounds.choice(cp.rewardRounds[0].toList(),round))
        }
    }
    @Test fun nonRoundBossShopsDoNotOfferOrdinaryPerks(){
        for(stage in listOf(5,15,25,35,40,45,50)){
            val w=World(seed=10);w.skipTo(stage);w.devOpenShop()
            assertTrue((0 until Perks.OFFERED).all{w.shop.perkOffer(0,it)<0})
        }
    }
    @Test fun newPerkStatsAndHealthAreReversible(){
        val w=World(seed=25);val p=w.players
        val hp=p.maxHp[0];val damage=p.damage(0);val rate=p.fireRate(0);val range=p.range(0);val magnet=p.magnet(0)
        w.devSetPerk(Perks.VITAL_CORE,true);assertEquals(hp+2,p.maxHp[0])
        w.devSetPerk(Perks.VITAL_CORE,false);assertEquals(hp,p.maxHp[0])
        w.devSetPerk(Perks.HARDPOINT,true);assertEquals(damage*1.1f,p.damage(0),.001f)
        w.devSetPerk(Perks.DEADEYE,true);assertEquals(damage*1.1f*1.12f,p.damage(0),.001f)
        w.devSetPerk(Perks.RAPID_CYCLE,true);assertEquals(rate*1.1f,p.fireRate(0),.001f)
        w.devSetPerk(Perks.LONGSHOT,true);assertEquals(range*1.15f,p.range(0),.001f)
        w.devSetPerk(Perks.VACUUM,true);assertEquals(magnet*1.25f,p.magnet(0),.001f)
    }
}
