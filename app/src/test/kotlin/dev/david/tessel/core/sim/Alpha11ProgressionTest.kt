package dev.david.tessel.core.sim

import dev.david.tessel.core.content.*
import org.junit.Assert.*
import org.junit.Test

class Alpha11ProgressionTest {
    @Test fun skillPrerequisitesAndOdds() {
        var mask = 0
        assertEquals(Perks.REFRESH_I, SkillTree.next(mask,0))
        mask = mask or (1 shl Perks.REFRESH_I)
        assertEquals(1, SkillTree.freeRefreshes(mask))
        assertEquals(Perks.REFRESH_II, SkillTree.next(mask,0))
        mask = mask or (1 shl Perks.REFRESH_II) or (1 shl Perks.REFRESH_III)
        assertEquals(3, SkillTree.freeRefreshes(mask))
        assertEquals(-1, SkillTree.next(mask,0))
        assertEquals(0, SkillTree.luck(mask))
        assertEquals(6, SkillTree.luck(mask or (7 shl Perks.FORTUNE_I)))
        assertEquals(.85, SkillTree.priceFactor(mask or (7 shl Perks.TRADE_I)), .0001)
    }

    @Test fun refreshIsFreeOnceAndPaidPriceDoesNotAdvance() {
        val w = World(seed=8).apply { spawning=false }
        val idle = Array(MAX_PLAYERS) { PlayerInput() }
        while(w.phase != Phase.SHOP) w.step(idle)
        w.players.perks[0] = 1 shl Perks.REFRESH_I
        w.players.coins[0] = 100
        assertEquals(0,w.shop.rerollPrice(w,0))
        assertTrue(w.shop.reroll(w,0))
        assertEquals(100,w.players.coins[0])
        assertEquals(Upgrades.rerollPrice(w.stage,0),w.shop.rerollPrice(w,0))
    }

    @Test fun reserveCardsUnlockAfterEarlyGame() {
        for(u in Upgrades.RESERVE_POWER..Upgrades.RESERVE_DRIVE) {
            assertFalse(Upgrades.availableAt(u,14))
            assertTrue(Upgrades.availableAt(u,15))
            for(frame in TankEvolution.entries) assertTrue(Upgrades.offered(u,frame))
        }
    }


    @Test fun stage17WithEveryOriginalUpgradeCappedStillHasThreeCards() {
        val w = World(seed=9)
        w.skipTo(17)
        for(u in 0 until Upgrades.RESERVE_POWER) w.players.levels[0][u] = Upgrades.of(u).maxLevel
        w.shop.open(w)
        assertEquals(setOf(Upgrades.RESERVE_POWER, Upgrades.RESERVE_HULL, Upgrades.RESERVE_DRIVE),
            (0 until SHOP_SLOTS).map { w.shop.item(0,it) }.toSet())
        assertEquals(2,SkillTree.unspent(17,0))
        assertTrue(w.shop.pickPerk(w,0))
        assertTrue(w.shop.pickPerk(w,0))
        assertFalse(w.shop.perkPending(0))
        assertEquals(2,SkillTree.freeRefreshes(w.players.perks[0]))
    }

    @Test fun checkpointPreservesSeatSkillsReserveRanksAndUsedFreeRefresh() {
        val w=World(seed=8).apply { spawning=false }
        val idle=Array(MAX_PLAYERS){PlayerInput()}
        while(w.phase != Phase.SHOP) w.step(idle)
        w.players.perks[0] = 1 shl Perks.REFRESH_I
        w.players.levels[0][Upgrades.RESERVE_POWER] = 3
        w.players.coins[0] = 300
        assertTrue(w.shop.reroll(w,0))
        val cp=Checkpoint.read(w.capture().write())!!
        assertEquals(3,cp.levels[0][Upgrades.RESERVE_POWER])
        assertEquals(1,SkillTree.freeRefreshes(cp.perks[0]))
        val restored=World(seed=10)
        restored.restore(cp,10L){true}
        assertEquals(Upgrades.rerollPrice(restored.stage,0),restored.shop.rerollPrice(restored,0))
        assertEquals(300,restored.players.coins[0])
    }

    @Test fun olderMaxedStage17ShopCanRecoverWithoutSpendingCoins() {
        val cp=Checkpoint(Difficulty.NORMAL,17,100,1,true,3)
        cp.loadedUpgradeCount=Upgrades.RESERVE_POWER
        cp.present[0]=true
        cp.hp[0]=10
        cp.maxHp[0]=10
        cp.stockSaved[0]=true
        for(u in 0 until Upgrades.RESERVE_POWER) cp.levels[0][u]=Upgrades.of(u).maxLevel
        val w=World(seed=8)
        w.restore(cp,8L){true}
        assertTrue((0 until SHOP_SLOTS).all { w.shop.item(0,it) >= Upgrades.RESERVE_POWER })
        assertTrue(w.shop.perkPending(0))
        assertEquals(0,w.players.coins[0])
    }
}
