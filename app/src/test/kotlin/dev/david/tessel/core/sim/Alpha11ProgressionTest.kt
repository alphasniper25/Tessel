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

}
