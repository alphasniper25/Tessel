package dev.david.tessel.core.sim

import dev.david.tessel.core.content.*
import org.junit.Assert.*
import org.junit.Test

class SkillTreeAlpha12Test {
    private fun shop(stage:Int,players:Int=1):World {
        val w=World(seed=12L);w.reset(players=players,seed=12L);w.skipTo(stage);w.shop.open(w);return w
    }
    private fun take(w:World,p:Int,branch:Int) {
        val id=SkillTree.next(w.players.perks[p],branch)
        val slot=(0 until Perks.OFFERED).first { w.shop.perkOffer(p,it)==id }
        assertTrue(w.shop.pickPerk(w,p,slot))
    }
    @Test fun firstPointIsEarlyAndAllSevenAreBounded() {
        assertEquals(listOf(3,13,19,26,32,38,44),SkillTree.schedule())
        assertEquals(0,SkillTree.earned(2));assertEquals(1,SkillTree.earned(3));assertEquals(7,SkillTree.earned(50))
    }
    @Test fun noPointIsOfferedBeforeMilestone() {
        val w=shop(2);assertFalse(w.shop.perkPending(0));assertFalse(w.shop.pickPerk(w,0,0))
    }
    @Test fun rootsAreAvailableAtStageThree() {
        val w=shop(3)
        assertEquals((0..2).map{SkillTree.node(it,1)},(0..2).map{w.shop.perkOffer(0,it)})
    }
    @Test fun cannotRepeatPointOrSkipRanks() {
        val w=shop(3);take(w,0,0);assertEquals(1,SkillTree.rank(w.players.perks[0],0));assertEquals(0,SkillTree.unspent(3,w.players.perks[0]));assertFalse(w.shop.pickPerk(w,0,0))
    }
    @Test fun sevenPointsCannotBuyAllNineNodes() {
        val w=shop(44)
        repeat(3){take(w,0,0)};repeat(3){take(w,0,1)};take(w,0,2)
        assertEquals(3,SkillTree.rank(w.players.perks[0],0));assertEquals(3,SkillTree.rank(w.players.perks[0],1));assertEquals(1,SkillTree.rank(w.players.perks[0],2));assertFalse(w.shop.perkPending(0))
    }
    @Test fun rankThreeStopsThatBranch() {
        val w=shop(32);repeat(3){take(w,0,0)};assertEquals(-1,SkillTree.next(w.players.perks[0],0));assertEquals(-1,w.shop.perkOffer(0,0))
    }
    @Test fun seatsChooseIndependentBranches() {
        val w=shop(3,2);take(w,0,0);take(w,1,1)
        assertEquals(1,SkillTree.freeRefreshes(w.players.perks[0]));assertEquals(0,SkillTree.freeRefreshes(w.players.perks[1]))
        assertEquals(0,SkillTree.luck(w.players.perks[0]));assertEquals(2,SkillTree.luck(w.players.perks[1]))
    }
    @Test fun freeRefreshDoesNotAdvancePaidPriceOrSpendCoins() {
        val w=shop(3);take(w,0,0);w.players.coins[0]=1000
        assertEquals(0,w.shop.rerollPrice(w,0));assertTrue(w.shop.reroll(w,0))
        assertEquals(1000,w.players.coins[0]);assertEquals(Upgrades.rerollPrice(3,0),w.shop.rerollPrice(w,0))
    }
    @Test fun newShopResetsFreeRefreshAllowance() {
        val w=shop(3);take(w,0,0);w.players.coins[0]=1000
        assertTrue(w.shop.reroll(w,0));w.skipTo(4);w.shop.open(w)
        assertEquals(0,w.shop.rerollPrice(w,0))
    }
    @Test fun fortuneAndTradeAreCappedAndRanked() {
        val mask=(7 shl Perks.FORTUNE_I) or (7 shl Perks.TRADE_I)
        assertEquals(6,SkillTree.luck(mask));assertEquals(.85,SkillTree.priceFactor(mask),.0001)
        assertEquals(0,SkillTree.luck(0));assertEquals(1.0,SkillTree.priceFactor(0),.0001)
    }
    @Test fun oldCheckpointAtStageThreeRecoversPointAndKeepsFormat() {
        val cp=Checkpoint(Difficulty.NORMAL,3,200,1,true,0)
        cp.present[0]=true;cp.hp[0]=10;cp.maxHp[0]=10
        val w=World(seed=2L);w.restore(Checkpoint.read(cp.write())!!,2L){true}
        assertTrue(w.shop.perkPending(0));take(w,0,0)
        val restored=Checkpoint.read(w.capture().write())!!
        assertEquals(1,SkillTree.freeRefreshes(restored.perks[0]));assertEquals(7,Checkpoint.FORMAT)
    }
    @Test fun lockedQuoteSurvivesTradeRank() {
        val w=shop(3);val quote=w.shop.price(w,0,0)
        assertTrue(w.shop.setLocked(w,0,0,true));take(w,0,2)
        assertEquals(quote,w.shop.price(w,0,0))
    }

    @Test fun fortuneRollsMatchTheDocumentedProbabilityIncrease() {
        val normal=dev.david.tessel.core.rng.Rng(721L)
        val lucky=dev.david.tessel.core.rng.Rng(721L)
        val base=IntArray(CardRarity.entries.size)
        val improved=IntArray(CardRarity.entries.size)
        repeat(20000) {
            base[CardRarity.roll(normal).ordinal]++
            improved[CardRarity.roll(lucky,6).ordinal]++
        }
        val epic=CardRarity.EPIC.ordinal;val legendary=CardRarity.LEGENDARY.ordinal
        assertTrue(improved[epic]>base[epic])
        assertTrue(improved[legendary]>base[legendary])
        assertTrue(improved[legendary] in 1200..1600)
        assertEquals(base.sum(),improved.sum())
    }
}
