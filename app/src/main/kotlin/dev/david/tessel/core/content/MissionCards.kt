package dev.david.tessel.core.content

import dev.david.tessel.core.stats.Totals

/** Mission cards are distinct upgrade IDs. Unlocks are earned, never bought.
 * IDs and bit positions are persistent: append entries, do not reorder. */
enum class CardMission(val upgrade: Int, val rarity: CardRarity, val goal: String, val target: Int) {
    FIELD_MEDIC(Upgrades.FIELD_MEDIC, CardRarity.COMMON, "Collect 500 coins across runs", 500),
    AFTERBURNER(Upgrades.AFTERBURNER, CardRarity.RARE, "Reach stage 15 in a run", 15),
    TITAN_PLATING(Upgrades.TITAN_PLATING, CardRarity.EPIC, "Defeat 20 bosses across runs", 20),
    CORE_BREAKER(Upgrades.CORE_BREAKER, CardRarity.LEGENDARY, "Complete the 50-stage campaign", 1);

    val bit: Int get() = 1 shl ordinal
    fun progress(t: Totals): Int = when (this) {
        FIELD_MEDIC -> t.coins
        AFTERBURNER -> t.deepest
        TITAN_PLATING -> t.bosses
        CORE_BREAKER -> t.completed
    }.coerceAtLeast(0)
    fun unlocked(t: Totals) = progress(t) >= target

    companion object {
        val allMask: Int get() = (1 shl entries.size) - 1
        fun mask(t: Totals) = entries.fold(0) { acc, m -> if (m.unlocked(t)) acc or m.bit else acc }
        fun forUpgrade(id: Int) = entries.firstOrNull { it.upgrade == id }
        fun eligible(id: Int, mask: Int, tier: CardRarity? = null): Boolean {
            val m = forUpgrade(id) ?: return true
            return mask and m.bit != 0 && (tier == null || tier == m.rarity)
        }
    }
}

/** Account XP is derived from the durable ledger, avoiding a second reward transaction.
 * Applies to completed/ended runs, not live checkpoints or practice/dev runs. */
object AccountProgress {
    fun xp(t: Totals): Long = t.seconds.coerceAtLeast(0).toLong() / 10 +
        t.kills.coerceAtLeast(0).toLong() * 2 + t.bosses.coerceAtLeast(0).toLong() * 50 +
        t.completed.coerceAtLeast(0).toLong() * 500
    fun threshold(level: Int): Long = 100L * (level - 1) * level / 2
    fun level(xp: Long): Int {
        var lo = 1; var hi = 1_000_000
        while (lo < hi) {
            val mid = (lo + hi + 1) / 2
            if (threshold(mid) <= xp) lo = mid else hi = mid - 1
        }
        return lo
    }
}
