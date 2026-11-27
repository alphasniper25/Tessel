package dev.david.tessel.core.content

import kotlin.math.pow
import kotlin.math.roundToInt

enum class UpgradeStat { DAMAGE, DAMAGE_BONUS, FIRE_RATE, VOLLEY, VELOCITY, PIERCE, RANGE, SPEED, SPEED_BONUS, MAX_HP, WEALTH, MAGNET, CALIBER, RAM, HEAL }

data class StatEffect(val stat: UpgradeStat, val amount: Float)

class UpgradeDef(
    val name: String,
    val blurb: String,
    val basePrice: Int,
    val maxLevel: Int,
    /** What one level adds: a share of the base value, or a whole count for counted upgrades. */
    val step: Float,
    val family: CardFamily = CardFamily.GUN,
    val secondaryStep: Float = 0f,
    val stat: UpgradeStat,
    val secondaryStat: UpgradeStat? = null,
) {
    val effects = if (secondaryStat == null) listOf(StatEffect(stat, step)) else listOf(StatEffect(stat, step), StatEffect(secondaryStat, secondaryStep))
}

/** Everything the between-stage shop sells. Ids index into each player's level array. */
object Upgrades {
    const val DAMAGE = 0
    const val FIRE_RATE = 1
    const val MULTISHOT = 2
    const val VELOCITY = 3
    const val PIERCE = 4
    const val RANGE = 5
    const val SPEED = 6
    const val MAX_HEALTH = 7
    const val WEALTH = 8
    const val MAGNET = 9
    const val CALIBER = 10
    const val RAM_DAMAGE = 11
    const val HEAL_POWER = 12
    const val FIELD_MEDIC = 13
    const val AFTERBURNER = 14
    const val TITAN_PLATING = 15
    const val CORE_BREAKER = 16

    private val defs = arrayOf(
        UpgradeDef("Damage", "+15% damage per bullet", basePrice = 15, maxLevel = 25, step = 0.15f, stat = UpgradeStat.DAMAGE),
        UpgradeDef("Fire Rate", "+10% shots per second", basePrice = 18, maxLevel = 20, step = 0.10f, stat = UpgradeStat.FIRE_RATE),
        UpgradeDef("Multishot", "One more bullet per shot", basePrice = 45, maxLevel = 8, step = 1f, stat = UpgradeStat.VOLLEY),
        UpgradeDef("Velocity", "+10% bullet speed", basePrice = 10, maxLevel = 10, step = 0.10f, stat = UpgradeStat.VELOCITY),
        UpgradeDef("Pierce", "Bullets go through one more enemy", basePrice = 40, maxLevel = 5, step = 1f, stat = UpgradeStat.PIERCE),
        UpgradeDef("Range", "+12% bullet range", basePrice = 10, maxLevel = 10, step = 0.12f, stat = UpgradeStat.RANGE),
        UpgradeDef("Speed", "+6% move speed", basePrice = 15, maxLevel = 10, step = 0.06f, family = CardFamily.MOBILITY, stat = UpgradeStat.SPEED),
        UpgradeDef("Max Health", "One more health pip, filled", basePrice = 30, maxLevel = 10, step = 1f, family = CardFamily.HEALTH, stat = UpgradeStat.MAX_HP),
        UpgradeDef("Wealth", "+15% coin value", basePrice = 25, maxLevel = 10, step = 0.15f, family = CardFamily.ECONOMY, stat = UpgradeStat.WEALTH),
        UpgradeDef("Magnet", "+25% coin pickup reach", basePrice = 8, maxLevel = 8, step = 0.25f, family = CardFamily.ECONOMY, stat = UpgradeStat.MAGNET),
        UpgradeDef("Caliber", "+15% bullet size", basePrice = 12, maxLevel = 8, step = 0.15f, stat = UpgradeStat.CALIBER),
        UpgradeDef("Ram Damage", "+1 contact retaliation damage", basePrice = 22, maxLevel = 15, step = 1f, family = CardFamily.RAM, stat = UpgradeStat.RAM),
        UpgradeDef("Heal Power", "Shop healing restores one more pip", basePrice = 24, maxLevel = 5, step = 1f, family = CardFamily.HEALTH, stat = UpgradeStat.HEAL),
        UpgradeDef("Field Medic", "Shop healing restores two extra pips", basePrice = 32, maxLevel = 1, step = 2f, family = CardFamily.HEALTH, stat = UpgradeStat.HEAL),
        UpgradeDef("Afterburner", "+18% movement speed", basePrice = 65, maxLevel = 1, step = 0.18f, family = CardFamily.MOBILITY, stat = UpgradeStat.SPEED_BONUS),
        UpgradeDef("Titan Plating", "+4 max HP, filled; +3 retaliation damage", basePrice = 160, maxLevel = 1, step = 4f, family = CardFamily.RAM, secondaryStep = 3f, stat = UpgradeStat.MAX_HP, secondaryStat = UpgradeStat.RAM),
        UpgradeDef("Core Breaker", "+80% bullet damage", basePrice = 240, maxLevel = 1, step = 0.8f, family = CardFamily.GUN, stat = UpgradeStat.DAMAGE_BONUS),
    )

    val count get() = defs.size

    // Compiled once. The hot simulation traverses only the relevant effect(s), without filtering or allocation.
    private class Contribution(val id: Int, val amount: Float)
    private val byStat = Array(UpgradeStat.entries.size) { i ->
        defs.flatMapIndexed { id, def -> def.effects.filter { it.stat.ordinal == i }.map { Contribution(id, it.amount) } }.toTypedArray()
    }

    fun delta(levels: IntArray, stat: UpgradeStat): Float {
        var result = 0f
        for (c in byStat[stat.ordinal]) result += levels[c.id] * c.amount
        return result
    }

    /** Used when buying a card with an immediate effect, or setting up a developer run. */
    fun amount(id: Int, stat: UpgradeStat): Float {
        val d = defs[id]
        return (if (d.stat == stat) d.step else 0f) + (if (d.secondaryStat == stat) d.secondaryStep else 0f)
    }

    /** What [levels] levels bought together add, for a rare upgrade's card. */
    fun blurb(id: Int, levels: Int): String {
        if (levels <= 1) return defs[id].blurb
        val pct = { step: Float -> "+${(step * 100 * levels).roundToInt()}%" }
        val more = when (levels) {
            2 -> "Two"
            3 -> "Three"
            else -> "$levels"
        }
        return when (id) {
            DAMAGE -> "${pct(defs[id].step)} damage per bullet"
            FIRE_RATE -> "${pct(defs[id].step)} shots per second"
            MULTISHOT -> "$more more bullets per shot"
            VELOCITY -> "${pct(defs[id].step)} bullet speed"
            PIERCE -> "Bullets go through ${more.lowercase()} more enemies"
            RANGE -> "${pct(defs[id].step)} bullet range"
            SPEED -> "${pct(defs[id].step)} move speed"
            MAX_HEALTH -> "$more more health pips, filled"
            WEALTH -> "${pct(defs[id].step)} coin value"
            MAGNET -> "${pct(defs[id].step)} coin pickup reach"
            CALIBER -> "${pct(defs[id].step)} bullet size"
            RAM_DAMAGE -> "+$levels contact retaliation damage"
            HEAL_POWER -> "Shop healing restores $levels more pips"
            else -> defs[id].blurb
        }
    }

    fun of(id: Int) = defs[id]

    /**
     * Each level costs 30% more than the one before, and everything costs
     * more each stage, because coins are worth more each stage too. Buying the
     * same upgrade later costs more; a locked card retains its quoted price.
     */
    fun price(id: Int, level: Int, stage: Int) =
        (defs[id].basePrice * LEVEL_GROWTH.pow(level) * inflation(stage)).roundToInt()

    fun inflation(stage: Int) = STAGE_GROWTH.pow(stage - 1)

    private const val LEVEL_GROWTH = 1.3
    private const val STAGE_GROWTH = 1.17

    const val HEAL_AMOUNT = 2
    const val HEALS_PER_SHOP = 2

    /** Keeps pace with coin value, so a heal always costs about the same number of kills. */
    fun healPrice(stage: Int) = (14 * Balance.COIN_GROWTH.pow(stage - 1)).roundToInt()

    fun rerollPrice(stage: Int, rerolls: Int) = ((REROLL_BASE + REROLL_STEP * rerolls) * inflation(stage)).roundToInt()

    private const val REROLL_BASE = 5
    private const val REROLL_STEP = 3
}
