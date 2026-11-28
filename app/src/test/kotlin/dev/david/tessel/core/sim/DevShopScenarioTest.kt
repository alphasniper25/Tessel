package dev.david.tessel.core.sim

import dev.david.tessel.core.content.*
import dev.david.tessel.core.dev.DevReward
import org.junit.Assert.*
import org.junit.Test

class DevShopScenarioTest {
    @Test fun previewOpensAtSelectedStageWithoutRewardOrEarnedCoins(){
        val w=World(seed=23);w.reset(seed=23);w.skipTo(25);w.players.coins[0]=1234
        w.devOpenShop()
        assertEquals(25,w.stage);assertEquals(Phase.SHOP,w.phase)
        assertEquals(1234,w.players.coins[0]);assertEquals(0,w.players.earned[0])
        assertEquals(0,w.enemies.count)
    }
    @Test fun forcedOffersAreUniqueAndStillUseTheActualPurchaseEngine(){
        val w=World(seed=42);w.reset(seed=42);w.players.coins[0]=10000;w.devOpenShop()
        val raw=CardCode.pack(Upgrades.DAMAGE,CardRarity.RARE)
        w.shop.devStock(w,listOf(raw,raw,-1))
        assertEquals(raw,w.shop.raw(0,0));assertEquals(3,(0 until SHOP_SLOTS).map{w.shop.item(0,it)}.toSet().size)
        val price=w.shop.price(w,0,0)
        assertTrue(w.shop.buy(w,0,0));assertEquals(10000-price,w.players.coins[0])
        assertEquals(3,w.players.levels[0][Upgrades.DAMAGE])
        assertFalse(w.shop.buy(w,0,0))
    }
    @Test fun exactRankAndPerkEditsAreReversibleAndDoNotStackHealth(){
        val w=World(seed=12);val hp=w.players.maxHp[0]
        w.devSetUpgrade(Upgrades.MAX_HEALTH,4);w.devSetUpgrade(Upgrades.MAX_HEALTH,4)
        assertEquals(hp+4,w.players.maxHp[0])
        w.devSetUpgrade(Upgrades.MAX_HEALTH,1);assertEquals(hp+1,w.players.maxHp[0])
        w.devSetPerk(Perks.THICK_SKIN,true);w.devSetPerk(Perks.THICK_SKIN,true)
        assertEquals(hp+3,w.players.maxHp[0])
        w.devSetPerk(Perks.THICK_SKIN,false);assertEquals(hp+1,w.players.maxHp[0])
        w.devSetSkillMask(SkillGraph.MASK);w.devSetSkillMask(0L);assertEquals(hp+1,w.players.maxHp[0])
    }
    @Test fun forcedRewardsRemainNormalChoiceSlots(){
        val w=World(seed=42);w.devOpenShop();w.shop.devReward(w,DevReward.PERKS)
        assertTrue(w.shop.perkPending(0));assertTrue(w.shop.pickPerk(w,0,0))
        assertFalse(w.shop.perkPending(0));assertFalse(w.shop.pickPerk(w,0,1))
        w.shop.devReward(w,DevReward.EVOLUTION);assertTrue(w.shop.perkPending(0))
        val id=w.shop.rewardOffer(0,0);assertTrue(TankEvolution.fromReward(id)!=null)
        assertTrue(w.shop.pickPerk(w,0,0));assertEquals(TankEvolution.fromReward(id),w.players.evolution[0])
    }
}
