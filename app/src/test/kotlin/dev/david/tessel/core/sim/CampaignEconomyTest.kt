package dev.david.tessel.core.sim
import dev.david.tessel.core.content.*
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.pow

class CampaignEconomyTest {
    @Test fun lateNominalInflationAndRealBonusPurchasingPowerAreReduced(){
        for(stage in 1..50){
            val oldScale=1.12.pow(stage-1)
            assertTrue(Upgrades.inflation(stage)<=oldScale+.00001)
            assertTrue(CampaignEconomy.pickupBase(stage)<=1.105.pow(stage-1)+.0001)
            val oldCap=(if(stage<=4)39 else if(stage<10)25 else 23)+(if(stage<10)45 else 36)
            val current=StageRewards.coins(stage,Int.MAX_VALUE,100000,Difficulty.NORMAL)
            assertTrue(current<=oldCap*oldScale+1)
        }
        assertTrue(StageRewards.coins(50,100000,1000)<1500)
        assertTrue(CampaignEconomy.bands.zipWithNext().all{(a,b)->b.through>a.through})
    }
    @Test fun baseSkillPricesAreStageGatedNotInfiniteProgression(){
        val nodes=SkillGraph.nodes
        assertEquals(60,nodes.size)
        assertTrue(nodes.all{it.price>0})
        assertTrue(nodes.filter{it.root}.all{it.minStage==3&&it.price in 50..90})
        assertTrue(nodes.filter{it.minStage==35}.all{it.price>1000})
        assertTrue(nodes.sumOf{it.price}>10000)
        assertFalse(SkillGraph.canBuy(0,0,2,100000))
        assertTrue(SkillGraph.canBuy(0,0,3,nodes[0].price))
    }
    @Test fun skillPricesRiseAfterSixPurchasesAndUseTheSameQuoteForEveryRoute(){
        val six=(1L shl 6)-1
        val seven=(1L shl 7)-1
        assertEquals(SkillGraph.nodes[9].price,SkillGraph.price(six,9))
        val price=SkillGraph.price(seven,9)
        assertTrue(price>SkillGraph.nodes[9].price)
        assertEquals(price,SkillGraph.price(seven shl 18,9))
        assertFalse(SkillGraph.canBuy(seven,9,50,price-1))
        assertTrue(SkillGraph.canBuy(seven,9,50,price))
    }
    @Test fun wealthyWavePayoutIsMonotonicAndIndependentOfPickupBatching(){
        assertEquals(100000L,CampaignEconomy.pickupPayout(9,100000))
        for(stage in 1..50){
            val allowance=CampaignEconomy.pickupAllowance(stage)
            assertEquals(allowance,CampaignEconomy.pickupPayout(stage,allowance))
            var sum=0L
            var previous=0L
            for(raw in 1L..5000L){
                val next=CampaignEconomy.pickupPayout(stage,raw)
                assertTrue(next>=previous&&next-previous<=1L)
                sum+=next-previous;previous=next
            }
            assertEquals(CampaignEconomy.pickupPayout(stage,5000),sum)
        }
        val allowance=CampaignEconomy.pickupAllowance(49)
        assertEquals(allowance+2500L,CampaignEconomy.pickupPayout(49,allowance+10000L))
    }
    @Test fun starterRewardsStillAllowAffordableUpgrades(){
        for(d in Difficulty.entries){
            val earned=StageRewards.coins(1,0,0,d)
            assertTrue(earned>=Upgrades.price(Upgrades.DAMAGE,0,1)*2)
            assertTrue(earned>=Upgrades.healPrice(1))
        }
    }
    @Test fun allFrameStatCurvesAreFiniteAndRelevant(){
        for(frame in TankEvolution.entries)for(c in CharacterDef.entries){
            val p=PlayerState();p.reset(1);p.evolution[0]=frame;p.character[0]=c
            val base=if(frame.melee)p.ramDamage(0) else p.damage(0)
            p.levels[0][Upgrades.DAMAGE]=Upgrades.runCap(Upgrades.DAMAGE)
            val improved=if(frame.melee)p.ramDamage(0) else p.damage(0)
            assertTrue(frame.label,base.isFinite()&&base>0f)
            assertTrue(frame.label,improved.isFinite()&&improved>=base)
            assertTrue(frame.label,p.moveSpeed(0).isFinite()&&p.moveSpeed(0)>0f)
            if(frame.drones)assertTrue(frame.droneSpec!!.cap<=8)
        }
    }
}
