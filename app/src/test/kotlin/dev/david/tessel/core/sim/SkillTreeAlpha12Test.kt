package dev.david.tessel.core.sim

import dev.david.tessel.core.content.*
import dev.david.tessel.core.net.*
import org.junit.Assert.*
import org.junit.Test

/** Regression coverage for the replacement of the nine-node milestone tree. */
class SkillTreeAlpha12Test {
    private fun shop(stage:Int,players:Int=1,coins:Int=100_000):World {
        val cp=Checkpoint(Difficulty.NORMAL,stage,stage*1800,players,true,0)
        repeat(players){p->cp.present[p]=true;cp.hp[p]=10;cp.maxHp[p]=10;cp.coins[p]=coins}
        return World(seed=12L).apply{restore(cp,12L){true}}
    }
    private fun buy(w:World,id:Int,p:Int=0){
        val before=w.players.coins[p]
        val quote=SkillGraph.price(w.players.skillMask(p),id)
        assertTrue("purchase $id",w.shop.buySkill(w,p,id))
        assertEquals(before-quote,w.players.coins[p])
        assertTrue(SkillGraph.owns(w.players.skillMask(p),id))
    }
    @Test fun allSixtyNodesAreUniqueConnectedAndHavePositiveEffects(){
        assertEquals(60,SkillGraph.nodes.size)
        assertEquals(60,SkillGraph.nodes.map{it.id}.toSet().size)
        assertEquals(6,SkillGraph.nodes.count{it.root})
        assertEquals(6,SkillGraph.nodes.count{it.bridge})
        assertTrue(SkillGraph.nodes.all{it.price>0&&it.amount>0})
        var mask=0L
        repeat(60){
            val node=SkillGraph.nodes.first{SkillGraph.eligible(mask,it.id,50)}
            mask=mask or SkillGraph.bit(node.id)
        }
        assertEquals(SkillGraph.MASK,mask)
        assertEquals(60,SkillGraph.count(mask))
    }
    @Test fun treeHasNoMandatoryMilestoneRewardsAndIsLockedBeforeStageThree(){
        val w=shop(2)
        assertFalse(w.shop.perkPending(0))
        assertFalse(w.shop.buySkill(w,0,0))
        val atThree=shop(3)
        assertFalse(atThree.shop.perkPending(0))
        assertEquals(6,SkillGraph.nodes.count{atThree.shop.canBuySkill(atThree,0,it.id)})
        atThree.nextStage()
        assertEquals(Phase.FIGHT,atThree.phase)
        assertEquals(0L,atThree.players.skillMask(0))
    }
    @Test fun coinAndStageAndConnectionAndDuplicateValidation(){
        val w=shop(3,coins=SkillGraph.nodes[0].price-1)
        assertFalse(w.shop.buySkill(w,0,0))
        w.players.coins[0]++
        buy(w,0)
        assertEquals(0,w.players.coins[0])
        assertFalse(w.shop.buySkill(w,0,0))
        w.players.coins[0]=100_000
        assertFalse(w.shop.buySkill(w,0,1))
        assertFalse(w.shop.buySkill(w,0,-1))
        assertFalse(w.shop.buySkill(w,0,60))
        assertFalse(w.shop.buySkill(w,-1,0))
        val late=shop(23)
        assertFalse(late.shop.buySkill(late,0,5))
        buy(late,0);buy(late,2);buy(late,5)
        assertFalse(late.shop.buySkill(late,0,8))
    }
    @Test fun aCrossRouteCanEnterAnotherSectorWithoutItsRoot(){
        val w=shop(23)
        for(id in listOf(0,2,5,54,12))buy(w,id)
        assertFalse(SkillGraph.owns(w.players.skillMask(0),9))
        assertTrue(SkillGraph.owns(w.players.skillMask(0),12))
        buy(w,36)
        assertEquals(6,SkillGraph.count(w.players.skillMask(0)))
        assertEquals(2,w.players.skill(0,SkillBonus.LUCK))
    }
    @Test fun coinsCompeteWithNormalShopCardsAndStockIsNotChanged(){
        val w=shop(3);val stock=(0 until SHOP_SLOTS).map{w.shop.raw(0,it)}
        buy(w,0);assertEquals(stock,(0 until SHOP_SLOTS).map{w.shop.raw(0,it)})
        assertTrue(w.shop.buy(w,0,0))
        assertTrue(SkillGraph.owns(w.players.skillMask(0),0))
    }
    @Test fun freeRefreshesAreSeparateFromPaidRefreshesAndPersist(){
        val w=shop(7)
        val paid=Upgrades.rerollPrice(7,0);val start=w.players.coins[0]
        assertTrue(w.shop.reroll(w,0));assertEquals(start-paid,w.players.coins[0])
        buy(w,0);buy(w,1)
        val before=w.players.coins[0]
        repeat(2){assertEquals(0,w.shop.rerollPrice(w,0));assertTrue(w.shop.reroll(w,0))}
        assertEquals(before,w.players.coins[0])
        assertEquals(Upgrades.rerollPrice(7,1),w.shop.rerollPrice(w,0))
        val cp=Checkpoint.read(w.capture().write())!!
        assertEquals(2,cp.freeRerollsUsed[0]);assertEquals(1,cp.rerolls[0])
        val back=World(seed=13L).apply{restore(cp,13L){true}}
        assertEquals(w.players.skillMask(0),back.players.skillMask(0))
        assertEquals(w.shop.rerollPrice(w,0),back.shop.rerollPrice(back,0))
        back.nextStage();back.shop.open(back)
        assertEquals(2,back.shop.freeRefreshesLeft(back,0))
    }
    @Test fun legacySevenFormatMigratesRanksAndConsumedRefreshesWithoutChargingCoins(){
        val cp=Checkpoint(Difficulty.NORMAL,27,100,1,true,4)
        cp.present[0]=true;cp.hp[0]=10;cp.maxHp[0]=10;cp.coins[0]=321
        cp.perks[0]=(3 shl Perks.REFRESH_I) or (7 shl Perks.FORTUNE_I) or (1 shl Perks.TRADE_I) or (1 shl Perks.THICK_SKIN)
        cp.perkOffer[0][0]=Perks.TRADE_II
        cp.rerolls[0]=3
        // Format 8 appends precisely a long and byte to each seat. One seat here.
        val current=cp.write()
        val legacy=current.copyOf(current.size-9-RewardRounds.COUNT).also{it[2]=7}
        val read=Checkpoint.read(legacy)!!
        assertEquals(9,Checkpoint.FORMAT);assertEquals(321,read.coins[0])
        assertEquals(2,read.freeRerollsUsed[0]);assertEquals(1,read.rerolls[0])
        assertEquals(-1,read.perkOffer[0][0])
        val w=World(seed=12L).apply{restore(read,12L){true}}
        assertEquals(6,SkillGraph.count(w.players.skillMask(0)))
        assertEquals(2,w.players.skill(0,SkillBonus.FREE_REFRESH))
        assertEquals(6,w.players.skill(0,SkillBonus.LUCK))
        assertEquals(5,w.players.skill(0,SkillBonus.CARD_DISCOUNT))
        assertEquals(321,w.players.coins[0])
        assertTrue(w.players.has(0,Perks.THICK_SKIN))
    }
    @Test fun seatsAndNetworkSnapshotsKeepIndependentGraphAndPrices(){
        val w=shop(23,2)
        buy(w,0,0);buy(w,9,1);buy(w,18,1)
        assertEquals(1,w.players.skill(0,SkillBonus.FREE_REFRESH))
        assertEquals(0,w.players.skill(1,SkillBonus.FREE_REFRESH))
        assertEquals(2,w.players.skill(1,SkillBonus.LUCK))
        val buf=Wire.buffer(MAX_MESSAGE*2)
        Protocol.writeSnapshot(buf,w,false,false,0,Events(),Events(),Events(),1,Echo())
        val s=Snapshot();Protocol.readSnapshot(Wire.open(Wire.finish(buf)),s)
        assertEquals(w.players.skillMask(0),s.skillNodes[0])
        assertEquals(w.players.skillMask(1),s.skillNodes[1])
        assertEquals(w.shop.healPrice(w,1),s.healPrice)
        assertEquals(w.shop.rerollPrice(w,1),s.rerollPrice)
        assertEquals(w.players.coins[1],s.coins[1])
        assertEquals(0,s.freeRefreshesLeft)
    }
    @Test fun remotePurchaseRequiresConnectedUnreadySeatAndRejectsDuplicate(){
        val w=shop(23,2)
        val host=HostSession(w){_,_,_->true}
        val req=Wire.action(Wire.BUY_SKILL,0)
        val before=w.players.coins[1]
        host.receive(1,req,1L);assertEquals(before,w.players.coins[1])
        host.connect(1);host.receive(1,req,2L);host.receive(1,req,3L)
        assertEquals(before-SkillGraph.nodes[0].price,w.players.coins[1])
        assertEquals(0L,w.players.skillMask(0))
        host.markReady(1);host.receive(1,Wire.action(Wire.BUY_SKILL,9),4L)
        assertFalse(SkillGraph.owns(w.players.skillMask(1),9))
        host.close()
    }
    @Test fun discountsDoNotRewriteLockedQuotes(){
        val w=shop(3);val quote=w.shop.price(w,0,0)
        assertTrue(w.shop.setLocked(w,0,0,true));buy(w,18)
        assertEquals(quote,w.shop.price(w,0,0))
    }
    @Test fun hullMobilityFirepowerAndHealBonusesActuallyApply(){
        val w=shop(23)
        val p=w.players
        val speed=p.moveSpeed(0);val damage=p.damage(0);val fire=p.fireRate(0);val range=p.range(0);val pickup=p.magnet(0)
        val hp=p.maxHp[0];val heal=w.shop.healAmount(w,0)
        for(id in listOf(27,28,36,37,45,46,49))buy(w,id)
        assertEquals(hp+1,p.maxHp[0]);assertEquals(hp+1,p.hp[0])
        assertEquals(heal+1,w.shop.healAmount(w,0))
        assertEquals(speed*1.02f,p.moveSpeed(0),.001f)
        assertEquals(pickup*1.05f,p.magnet(0),.001f)
        assertEquals(damage*1.02f,p.damage(0),.001f)
        assertEquals(fire*1.02f,p.fireRate(0),.001f)
        assertEquals(range*1.04f,p.range(0),.001f)
    }
    @Test fun fortuneRollsMatchTheDocumentedProbabilityIncrease(){
        val normal=dev.david.tessel.core.rng.Rng(721L)
        val lucky=dev.david.tessel.core.rng.Rng(721L)
        val base=IntArray(CardRarity.entries.size);val improved=IntArray(CardRarity.entries.size)
        repeat(20000){base[CardRarity.roll(normal).ordinal]++;improved[CardRarity.roll(lucky,6).ordinal]++}
        val epic=CardRarity.EPIC.ordinal;val legendary=CardRarity.LEGENDARY.ordinal
        assertTrue(improved[epic]>base[epic]);assertTrue(improved[legendary]>base[legendary])
        assertTrue(improved[legendary] in 1200..1600);assertEquals(base.sum(),improved.sum())
    }
    @Test fun allBonusesAreBoundedAndNoSkillCarryoverToNewRun(){
        val total=SkillGraph.totals(SkillGraph.MASK)
        assertEquals(3,total[SkillBonus.FREE_REFRESH.ordinal])
        assertEquals(11,total[SkillBonus.LUCK.ordinal])
        assertEquals(20,total[SkillBonus.CARD_DISCOUNT.ordinal])
        assertTrue(total[SkillBonus.DAMAGE.ordinal]<=20)
        assertTrue(total[SkillBonus.SPEED.ordinal]<=20)
        val w=shop(44);buy(w,0);w.reset(seed=42L)
        assertEquals(0L,w.players.skillMask(0))
    }
}
