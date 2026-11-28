package dev.david.tessel.core.sim
import dev.david.tessel.core.content.*
import dev.david.tessel.core.net.*
import org.junit.Assert.*
import org.junit.Test

class RankShopForecastTest {
    @Test fun independentlyRolledMagnetOffersAdvanceSameRankTrackWithoutExceedingCap(){
        val w=World(seed=23);w.devOpenShop();val p=w.players
        p.coins[0]=100000
        val s=w.shop
        s.devStock(w,listOf(CardCode.pack(Upgrades.MAGNET,CardRarity.COMMON),CardCode.pack(Upgrades.MAGNET,CardRarity.RARE),CardCode.pack(Upgrades.MAGNET,CardRarity.LEGENDARY)))
        val f=ShopStatState.from(p,0,w.difficulty,w.stage)
        val change=f.forecast(Upgrades.MAGNET,1).first{it.label=="Pickup radius"}
        assertEquals(p.magnet(0),change.before,.001f)
        assertTrue(s.buy(w,0,0));assertEquals(change.after,p.magnet(0),.001f)
        assertEquals(1,p.levels[0][Upgrades.MAGNET]);assertEquals(3,s.levelsBought(w,0,1))
        assertTrue(s.setLocked(w,0,2,true));val price=s.price(w,0,2)
        assertTrue(s.buy(w,0,1));assertEquals(4,p.levels[0][Upgrades.MAGNET])
        assertEquals(4,s.levelsBought(w,0,2));assertTrue(s.price(w,0,2)<price)
        assertTrue(s.buy(w,0,2));assertEquals(8,p.levels[0][Upgrades.MAGNET])
        assertFalse(s.buy(w,0,2))
    }
    @Test fun exactForecastMatchesRealDamageAndHealthWithCurrentModifiers(){
        val w=World(seed=22);w.skipTo(11);w.devSetFrame(TankEvolution.TWIN)
        val p=w.players;p.coins[0]=100000;p.perks[0]=1 shl Perks.HEAVY_ROUNDS
        p.setSkills(0,SkillGraph.bit(45));w.devSetUpgrade(Upgrades.DAMAGE,4);w.devOpenShop()
        w.shop.devStock(w,listOf(CardCode.pack(Upgrades.DAMAGE,CardRarity.RARE),CardCode.pack(Upgrades.MAX_HEALTH,CardRarity.UNCOMMON),-1))
        val a=ShopStatState.from(p,0,w.difficulty,w.stage)
        val damage=a.forecast(Upgrades.DAMAGE,3).first{it.label=="Damage per bullet"}
        assertEquals(p.damage(0),damage.before,.001f)
        assertTrue(w.shop.buy(w,0,0));assertEquals(p.damage(0),damage.after,.001f)
        val health=ShopStatState.from(p,0,w.difficulty,w.stage).forecast(Upgrades.MAX_HEALTH,2).first{it.label=="Maximum HP"}
        assertTrue(w.shop.buy(w,0,1));assertEquals(p.maxHp[0].toFloat(),health.after,.001f)
    }
    @Test fun rankNetworkHasOneSocketForEveryAttainableRank(){
        val positions=UpgradeMap.positions()
        for(n in positions){
            val ranks=UpgradeMap.ranks(n,positions)
            assertEquals(Upgrades.runCap(n.id),ranks.size)
            assertEquals((1..Upgrades.runCap(n.id)).toList(),ranks.map{it.rank})
        }
    }
    @Test fun actualRewardChoicesRoundTripThroughSaveAndShopSnapshot(){
        val w=World(seed=31);w.skipTo(5);w.devOpenShop()
        val chosen=w.shop.rewardOffer(0,0);assertTrue(w.shop.pickPerk(w,0,0))
        val p=w.players;assertEquals(chosen,p.rewardRounds[0][0])
        p.levels[0][Upgrades.MAGNET]=3;p.sinceHit[0]=123
        val cp=Checkpoint.read(w.capture().write())!!
        assertEquals(chosen,cp.rewardRounds[0][0])
        val back=World(seed=42).apply{restore(cp,42L){true}}
        assertArrayEquals(p.rewardRounds[0],back.players.rewardRounds[0])
        val buf=Wire.buffer(MAX_MESSAGE*2)
        Protocol.writeSnapshot(buf,w,false,false,0,Events(),Events(),Events(),0,Echo())
        val snap=Snapshot();Protocol.readSnapshot(Wire.open(Wire.finish(buf)),snap)
        assertArrayEquals(p.rewardRounds[0],snap.shopRewardRounds)
        assertEquals(3,snap.shopAllLevels[Upgrades.MAGNET]);assertEquals(123,snap.shopSinceHit)
    }

    @Test fun commonOffersFillMagnetRanksOneThenTwoThenThree(){
        val w=World(seed=73);w.devOpenShop();val p=w.players
        p.coins[0]=10000
        val s=w.shop
        val roll=CardCode.pack(Upgrades.MAGNET,CardRarity.COMMON)
        s.devStock(w,listOf(roll,roll,roll))
        val cap=Upgrades.runCap(Upgrades.MAGNET)
        for(slot in 0..2){
            val before=p.levels[0][Upgrades.MAGNET]
            assertEquals(slot,before)
            assertEquals(listOf(slot+1),(1..cap).filter{UpgradeMap.offeredRank(it,before,s.levelsBought(w,0,slot))})
            assertTrue(s.buy(w,0,slot))
            assertEquals((1..slot+1).toList(),(1..cap).filter{UpgradeMap.ownedRank(it,p.levels[0][Upgrades.MAGNET])})
        }
    }
}
