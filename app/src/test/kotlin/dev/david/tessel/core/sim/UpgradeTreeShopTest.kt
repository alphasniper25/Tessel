package dev.david.tessel.core.sim

import dev.david.tessel.core.content.*
import dev.david.tessel.core.net.*
import org.junit.Assert.*
import org.junit.Test

class UpgradeTreeShopTest {
    private fun fixture(stage:Int,frame:TankEvolution=TankEvolution.BASIC,mode:ShopMode=ShopMode.TREE,team:Int=1,coins:Int=1000000,last:Int=if(frame==TankEvolution.BASIC)0 else 5):World{
        val cp=Checkpoint(Difficulty.NORMAL,stage,stage*TICK_HZ,team,true,0).also{it.shopMode=mode}
        repeat(team){p->cp.present[p]=true;cp.hp[p]=10;cp.maxHp[p]=10;cp.coins[p]=coins;cp.evolution[p]=frame;cp.lastEvolutionStage[p]=last}
        return World(71).apply{restore(cp,71L){true}}
    }
    private fun node(u:Int,rank:Int=1)=UpgradeTree.node(u,rank)!!
    private fun buy(w:World,u:Int,rank:Int=1,p:Int=0){
        val n=node(u,rank);val pl=w.players
        val cost=UpgradeTree.price(n,w.stage,pl.character[p],pl.perks[p])
        val before=pl.coins[p]
        assertTrue("Cannot buy ${Upgrades.of(u).name} rank $rank stage ${w.stage}",w.shop.buyTree(w,p,n.id))
        assertEquals(rank,pl.levels[p][u])
        assertEquals(before-if(w.infiniteMoney)0 else cost,pl.coins[p])
    }
    @Test fun catalogIsBoundedAndNoRarityOrRetiredCardsAreInTree(){
        assertTrue(UpgradeTree.nodes.size in 140..255)
        assertTrue(UpgradeTree.nodes.indices.all{UpgradeTree.nodes[it].id==it})
        assertNull(UpgradeTree.node(Upgrades.CORE_BREAKER,1))
        assertNull(UpgradeTree.node(Upgrades.MULTISHOT,1))
        assertNull(UpgradeTree.node(Upgrades.VELOCITY,1))
        assertTrue(UpgradeTree.lanes.all{it.ranks<=Upgrades.runCap(it.upgrade)})
    }
    @Test fun stageOneHasSharedAffordableRootsButNoSpecialization(){
        val w=fixture(1,coins=40)
        assertTrue(UpgradeTree.visibleNodes(TankEvolution.BASIC,0).none{it.lane.specialized})
        for(u in listOf(Upgrades.DAMAGE,Upgrades.MAX_HEALTH,Upgrades.SPEED,Upgrades.WEALTH,Upgrades.MAGNET))
            assertTrue(w.shop.canBuyTree(w,0,node(u).id))
        assertFalse(w.shop.canBuyTree(w,0,node(Upgrades.FIRE_RATE).id))
        buy(w,Upgrades.DAMAGE)
        assertFalse(w.shop.buyTree(w,0,node(Upgrades.DAMAGE).id))
        assertFalse(w.shop.buyTree(w,0,node(Upgrades.DAMAGE,2).id))
    }
    @Test fun treeModeHasNoCardSlotsRefreshesOrLegacySkillPurchases(){
        val w=fixture(6)
        assertTrue((0 until SHOP_SLOTS).all{w.shop.item(0,it)<0})
        assertFalse(w.shop.reroll(w,0));assertFalse(w.shop.buy(w,0,0));assertFalse(w.shop.buySkill(w,0,0))
        val legacy=fixture(6,mode=ShopMode.CLASSIC)
        assertFalse(legacy.shop.buyTree(legacy,0,node(Upgrades.DAMAGE).id))
        assertTrue((0 until SHOP_SLOTS).any{legacy.shop.item(0,it)>=0})
    }
    @Test fun evolutionRevealsOnlyCompatiblePathsAndKeepsSharedRanks(){
        val w=fixture(5)
        w.players.levels[0][Upgrades.DAMAGE]=2
        assertTrue(w.shop.perkPending(0))
        assertFalse(w.shop.canBuyTree(w,0,node(Upgrades.DAMAGE,3).id))
        val slot=(0 until TankEvolution.REWARD_SLOTS).first{TankEvolution.fromReward(w.shop.rewardOffer(0,it))==TankEvolution.RAMMER}
        assertTrue(w.shop.pickPerk(w,0,slot))
        assertEquals(TankEvolution.RAMMER,w.players.evolution[0])
        assertEquals(2,w.players.levels[0][Upgrades.DAMAGE])
        val visible=UpgradeTree.visibleNodes(TankEvolution.RAMMER,5)
        assertTrue(visible.any{it.upgrade==Upgrades.RAM_DAMAGE})
        assertFalse(visible.any{it.upgrade in listOf(Upgrades.FIRE_RATE,Upgrades.PIERCE,Upgrades.RANGE,Upgrades.CALIBER,Upgrades.DRONE_POWER)})
        buy(w,Upgrades.RAM_DAMAGE)
    }
    @Test fun sniperToDroneRefundsOnlyIncompatiblePaidRanksAndRevealsDroneBranch(){
        val w=fixture(15,TankEvolution.SNIPER)
        val pl=w.players;pl.levels[0][Upgrades.PIERCE]=1;pl.upgradeSpent[0][Upgrades.PIERCE]=75
        pl.levels[0][Upgrades.DAMAGE]=3
        val before=pl.coins[0]
        val slot=(0 until TankEvolution.REWARD_SLOTS).first{TankEvolution.fromReward(w.shop.rewardOffer(0,it))==TankEvolution.OVERSEER}
        assertTrue(w.shop.pickPerk(w,0,slot))
        assertEquals(before+75,pl.coins[0]);assertEquals(0,pl.levels[0][Upgrades.PIERCE]);assertEquals(3,pl.levels[0][Upgrades.DAMAGE])
        assertTrue(UpgradeTree.visibleNodes(TankEvolution.OVERSEER,15).any{it.upgrade==Upgrades.DRONE_POWER})
        assertFalse(UpgradeTree.visibleNodes(TankEvolution.OVERSEER,15).any{it.upgrade==Upgrades.CALIBER})
        buy(w,Upgrades.DRONE_POWER)
    }
    @Test fun everyFrameCanReachItsEntireCompatibleTreeAtStage49(){
        for(frame in TankEvolution.entries){
            val w=fixture(49,frame,last=40)
            w.infiniteMoney=true
            val visible=UpgradeTree.visibleNodes(frame,40)
            for(n in visible)assertTrue("${frame.label} ${n.upgrade} ${n.rank}",w.shop.buyTree(w,0,n.id))
            assertEquals(visible.size,w.players.bought(0))
            assertTrue(w.players.maxHp[0] in 1..127)
            assertTrue(w.players.damage(0).isFinite())
            assertTrue(w.players.moveSpeed(0).isFinite())
            assertTrue(w.players.ramDamage(0).isFinite())
            assertEquals(0,w.players.upgradeSpent[0].sum())
            assertTrue(UpgradeBuild.from(w.players,0).stats().isNotEmpty())
        }
    }
    @Test fun rankPricesAndRealEffectsMatchPurchaseAndDoNotResell(){
        val w=fixture(4)
        val pl=w.players;val base=pl.damage(0);val hp=pl.maxHp[0];val magnet=pl.magnet(0)
        buy(w,Upgrades.DAMAGE);buy(w,Upgrades.DAMAGE,2)
        assertEquals(base*1.30f,pl.damage(0),.001f)
        buy(w,Upgrades.MAX_HEALTH);assertEquals(hp+1,pl.maxHp[0])
        buy(w,Upgrades.MAGNET);assertEquals(magnet*1.25f,pl.magnet(0),.001f)
        assertFalse(w.shop.buyTree(w,0,node(Upgrades.DAMAGE,2).id))
        assertFalse(w.shop.buyTree(w,-1,node(Upgrades.MAGNET).id))
        assertFalse(w.shop.buyTree(w,0,255))
        assertFalse(w.shop.buyTree(w,0,-1))
    }
    @Test fun insufficientCoinsAndInfiniteMoneyDoNotAlterRealBalanceOrRefundAccounting(){
        val w=fixture(1,coins=0)
        assertFalse(w.shop.buyTree(w,0,node(Upgrades.DAMAGE).id))
        w.infiniteMoney=true;buy(w,Upgrades.DAMAGE);buy(w,Upgrades.MAX_HEALTH)
        assertEquals(0,w.players.coins[0]);assertEquals(0,w.players.upgradeSpent[0].sum())
        w.players.hp[0]=1;assertTrue(w.shop.heal(w,0));assertEquals(0,w.players.coins[0])
        val classic=fixture(3,mode=ShopMode.CLASSIC,coins=0);classic.infiniteMoney=true
        assertTrue(classic.shop.buy(classic,0,0));assertTrue(classic.shop.reroll(classic,0))
        assertTrue(classic.shop.buySkill(classic,0,0));assertEquals(0,classic.players.coins[0])
    }
    @Test fun savedModeAndRanksPersistWhileOldSaveDefaultsToClassicAndInfiniteNeverPersists(){
        val w=fixture(4);buy(w,Upgrades.DAMAGE)
        w.infiniteMoney=true
        val cp=Checkpoint.read(w.capture().write())!!
        assertEquals(ShopMode.TREE,cp.shopMode)
        val restored=World(92).apply{restore(cp,92){true}}
        assertEquals(ShopMode.TREE,restored.shopMode)
        assertFalse(restored.infiniteMoney)
        assertEquals(1,restored.players.levels[0][Upgrades.DAMAGE])
        val old=fixture(4,mode=ShopMode.CLASSIC).capture().write().also{it[2]=8}
        assertEquals(ShopMode.CLASSIC,Checkpoint.read(old)!!.shopMode)
    }
    @Test fun remoteTreePurchasesArePerSeatHostValidatedAndUnsignedIdsWork(){
        val w=fixture(49,TankEvolution.SPIKE,team=2,last=25)
        val h=HostSession(w){_,_,_->true}
        val n=node(Upgrades.RAM_DAMAGE)
        assertTrue(n.id>=128)
        w.players.levels[1][Upgrades.DAMAGE]=1
        val before=w.players.coins[1]
        h.receive(1,Wire.action(Wire.BUY_TREE,n.id),1L)
        assertEquals(before,w.players.coins[1])
        h.connect(1)
        h.receive(1,Wire.action(Wire.BUY_TREE,n.id),2L)
        h.receive(1,Wire.action(Wire.BUY_TREE,n.id),3L)
        assertEquals(1,w.players.levels[1][n.upgrade]);assertEquals(0,w.players.levels[0][n.upgrade])
        assertEquals(before-UpgradeTree.price(n,49,w.players.character[1],0),w.players.coins[1])
        h.markReady(1)
        h.receive(1,Wire.action(Wire.BUY_TREE,node(Upgrades.RAM_DAMAGE,2).id),4L)
        assertEquals(1,w.players.levels[1][n.upgrade])
        val buf=Wire.buffer(MAX_MESSAGE*2)
        Protocol.writeSnapshot(buf,w,false,false,0,Events(),Events(),Events(),1,Echo())
        val s=Snapshot();Protocol.readSnapshot(Wire.open(Wire.finish(buf)),s)
        assertEquals(ShopMode.TREE,s.shopMode);assertEquals(1,s.treeLevels[n.upgrade]);assertEquals(25,s.treeLastEvolution)
        h.close()
    }
    @Test fun earlySharedRanksAndSpecialistGatesPermitMeaningfulInvestment(){
        val w=fixture(4)
        for(rank in 1..5)buy(w,Upgrades.DAMAGE,rank)
        for(rank in 1..3)buy(w,Upgrades.MAX_HEALTH,rank)
        assertFalse(w.shop.canBuyTree(w,0,node(Upgrades.DAMAGE,6).id))
        assertTrue(UpgradeTree.visibleNodes(TankEvolution.BASIC,0).none{it.lane.specialized})
        for(frame in TankEvolution.entries)for(lane in UpgradeTree.lanes){
            val gates=(1..lane.ranks).map{UpgradeTree.minStage(node(lane.upgrade,it),frame)}
            assertTrue(gates.zipWithNext().all{(a,b)->a<=b})
        }
        val ram=node(Upgrades.RAM_DAMAGE)
        assertEquals(5,UpgradeTree.minStage(node(Upgrades.RAM_DAMAGE,2),TankEvolution.RAMMER))
        assertTrue(UpgradeTree.price(ram,5,CharacterDef.BALANCED,0)<=Upgrades.price(ram.upgrade,0,5))
        assertEquals(15,UpgradeTree.minStage(node(Upgrades.DRONE_POWER,2),TankEvolution.OVERSEER))
    }
    @Test fun stageGatesAndDiscoveryWorkEvenInInfiniteMoney(){
        val w=fixture(1,coins=0);w.infiniteMoney=true
        val n=node(Upgrades.DAMAGE,2)
        assertEquals(UpgradeDiscovery.HIDDEN,UpgradeTree.discovery(n,w.players.levels[0].asList()))
        buy(w,Upgrades.DAMAGE)
        assertEquals(UpgradeDiscovery.REVEALED,UpgradeTree.discovery(n,w.players.levels[0].asList()))
        assertFalse(w.shop.buyTree(w,0,n.id))
        assertFalse(w.shop.buyTree(w,0,node(Upgrades.RAM_DAMAGE).id))
        w.nextStage();assertFalse(w.shop.buyTree(w,0,node(Upgrades.SPEED).id))
    }
}
