package dev.david.tessel.core.content

import dev.david.tessel.core.stats.Totals

/** Mission cards are distinct upgrade IDs. Unlocks are earned, never bought.
 * IDs and bit positions are persistent: append entries, do not reorder. */
enum class MissionMetric(val read: (Totals) -> Int) { COINS({ it.coins }), DEPTH({ it.deepest }), BOSSES({ it.bosses }), WINS({ it.completed }), KILLS({it.kills}), SHOTS({it.shots}), REVIVES({it.revives}) }

enum class CardMission(val upgrade: Int, val rarity: CardRarity, val goal: String, val target: Int, val metric: MissionMetric) {
    FIELD_MEDIC(Upgrades.FIELD_MEDIC, CardRarity.COMMON, "Collect 500 coins across runs", 500, MissionMetric.COINS),
    AFTERBURNER(Upgrades.AFTERBURNER, CardRarity.RARE, "Reach stage 15 in a run", 15, MissionMetric.DEPTH),
    TITAN_PLATING(Upgrades.TITAN_PLATING, CardRarity.EPIC, "Defeat 20 bosses across runs", 20, MissionMetric.BOSSES),
    CORE_BREAKER(Upgrades.CORE_BREAKER, CardRarity.LEGENDARY, "Complete the 50-stage campaign", 1, MissionMetric.WINS),
    SCAVENGER(Upgrades.SCAVENGER,CardRarity.UNCOMMON,"Defeat 1,000 enemies across runs",1000,MissionMetric.KILLS),
    PHASE_CAPACITOR(Upgrades.PHASE_CAPACITOR,CardRarity.RARE,"Fire 10,000 shots or make drone contacts across runs",10000,MissionMetric.SHOTS),
    GUARDIAN_KIT(Upgrades.GUARDIAN_KIT,CardRarity.RARE,"Revive five teammates across runs",5,MissionMetric.REVIVES),
    DRONE_RELAY(Upgrades.DRONE_RELAY,CardRarity.EPIC,"Reach stage 25 in a run",25,MissionMetric.DEPTH);

    val bit: Int get() = 1 shl ordinal
    fun progress(t: Totals): Int = metric.read(t).coerceAtLeast(0)
    fun unlocked(t: Totals) = progress(t) >= target

    companion object {
        val allMask: Int get() = (1 shl entries.size) - 1
        fun mask(t: Totals) = entries.fold(0) { acc, m -> if (m.unlocked(t)) acc or m.bit else acc }
        private val byUpgrade = Array<CardMission?>(Upgrades.count) { id -> entries.firstOrNull { it.upgrade == id } }
        fun forUpgrade(id: Int) = byUpgrade.getOrNull(id)
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
