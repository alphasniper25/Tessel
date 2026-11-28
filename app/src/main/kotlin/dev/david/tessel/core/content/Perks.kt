package dev.david.tessel.core.content

class PerkDef(val name: String, val blurb: String)

/** Run-long special effects, offered three at a time after each boss. Ids are bit positions in a player's perk mask. */
object Perks {
    const val SECOND_WIND = 0
    const val THICK_SKIN = 1
    const val SIPHON = 2
    const val OVERCLOCK = 3
    const val HEAVY_ROUNDS = 4
    const val QUICK_FEET = 5
    const val MAGNET_CORE = 6
    const val LAST_STAND = 7
    const val SHOCKWAVE = 8
    const val HAGGLER = 9
    const val REFRESH_I = 10
    const val REFRESH_II = 11
    const val REFRESH_III = 12
    const val FORTUNE_I = 13
    const val FORTUNE_II = 14
    const val FORTUNE_III = 15
    const val TRADE_I = 16
    const val TRADE_II = 17
    const val TRADE_III = 18
    // IDs 10–18 are retired skill bits, kept for old saves. New perks append.
    const val RANGEFINDER = 19
    const val HARDPOINT = 20
    const val AFTERBURNER = 21
    const val LONGSHOT = 22
    const val DEADEYE = 23
    const val VITAL_CORE = 24
    const val VACUUM = 25
    const val RAPID_CYCLE = 26

    private val defs = arrayOf(
        PerkDef("Second Wind", "Once a stage, a hit that would down you leaves you on one health."),
        PerkDef("Thick Skin", "Two more health pips, filled."),
        PerkDef("Siphon", "Every 25th kill heals one pip."),
        PerkDef("Overclock", "Fire 30% faster after eight seconds without a hit."),
        PerkDef("Heavy Rounds", "+30% damage and one more pierce, but 15% slower fire."),
        PerkDef("Quick Feet", "+15% move speed."),
        PerkDef("Magnet Core", "Coins are drawn in from twice as far."),
        PerkDef("Last Stand", "+50% damage while you have two health or fewer."),
        PerkDef("Shockwave", "A hit on you damages and shoves the enemies close by."),
        PerkDef("Haggler", "Shop upgrades cost 10% less."),
        PerkDef("Refresh I", "One free shop refresh each stage."),
        PerkDef("Refresh II", "Two free shop refreshes each stage in total."),
        PerkDef("Refresh III", "Three free shop refreshes each stage in total."),
        PerkDef("Fortune I", "Epic and legendary odds each gain 2 percentage points on new cards."),
        PerkDef("Fortune II", "Epic and legendary odds each gain 4 points in total."),
        PerkDef("Fortune III", "Epic and legendary odds each gain 6 points in total."),
        PerkDef("Trade I", "5% discount on newly priced upgrade cards."),
        PerkDef("Trade II", "10% discount on newly priced upgrade cards."),
        PerkDef("Trade III", "15% discount on newly priced upgrade cards."),
        PerkDef("Rangefinder", "+10% bullet range and drone command range."),
        PerkDef("Hardpoint", "+10% bullet, drone and ram damage."),
        PerkDef("Afterburner", "+10% movement speed."),
        PerkDef("Longshot", "+15% bullet range and drone command range."),
        PerkDef("Deadeye", "+12% bullet, drone and ram damage."),
        PerkDef("Vital Core", "Two extra max health pips, filled."),
        PerkDef("Vacuum", "+25% coin pickup radius."),
        PerkDef("Rapid Cycle", "+10% fire rate and drone replacement rate."),
    )

    val count get() = defs.size

    fun of(id: Int) = defs[id]
    fun isRunPerk(id:Int)=id in defs.indices&&!SkillTree.isNode(id)
    val runIds get()=defs.indices.filter(::isRunPerk)
    fun hullBonus(id:Int)=if(id==THICK_SKIN||id==VITAL_CORE)2 else 0

    /** How many perks a boss offers at once. */
    const val OFFERED = 3
}

/** Run-local, three independent prerequisite chains. The existing saved perk mask
 * carries them for each seat, without adding a new currency or checkpoint field. */
object SkillTree {
    const val ORIGINAL_PERKS = 10
    const val MASK = (1 shl 19) - (1 shl ORIGINAL_PERKS)
    private val milestones = intArrayOf(3, 13, 19, 26, 32, 38, 44)
    private val roots = intArrayOf(Perks.REFRESH_I, Perks.FORTUNE_I, Perks.TRADE_I)
    fun milestone(stage: Int) = stage in milestones
    fun earned(stage: Int) = milestones.count { it <= stage }
    fun nextMilestone(stage: Int) = milestones.firstOrNull { it > stage }
    fun schedule() = milestones.toList()
    fun node(branch: Int, rank: Int) = roots[branch] + rank - 1
    fun name(branch: Int) = listOf("Refresh","Fortune","Trade")[branch]
    fun unspent(stage: Int, mask: Int) = (milestones.count { it <= stage } - Integer.bitCount(mask and MASK)).coerceAtLeast(0)
    fun isNode(id: Int) = id in Perks.REFRESH_I..Perks.TRADE_III
    fun rank(mask: Int, branch: Int): Int {
        val root = roots[branch]
        var n = 0
        while (n < 3 && mask and (1 shl (root + n)) != 0) n++
        return n
    }
    fun next(mask: Int, branch: Int): Int {
        val n = rank(mask, branch)
        return if (n == 3) -1 else roots[branch] + n
    }
    fun freeRefreshes(mask: Int) = rank(mask, 0)
    fun luck(mask: Int) = 2 * rank(mask, 1)
    fun priceFactor(mask: Int) = 1.0 - .05 * rank(mask, 2)
}

/** Extra traits an enemy can carry, shown by a bright outline. */
object Elite {
    const val NONE = 0
    const val ARMORED = 1
    const val FAST = 2
    const val SPLITTING = 3
    const val BURSTING = 4
    const val COUNT = 5

    /** Marks a boss the Core has borrowed. Never picked at random, so it sits past [COUNT]. */
    const val TETHERED = 5

    private val names = arrayOf("", "Armored", "Fast", "Splitting", "Bursting", "Tethered")

    fun name(trait: Int) = names[trait]

    /** Health added to an elite on top of its kind's. */
    const val HEALTH = 1.6f

    /** Shots taken by an armored elite deal this share of their damage. */
    const val ARMOR = 0.5f
    const val SPEED = 1.5f

    /** A kill sometimes drops a surge: faster and harder shots for a while. */
    const val SURGE_CHANCE = 0.25f
    const val SURGE_SECONDS = 15
    const val SURGE_FIRE = 1.35f
    const val SURGE_DAMAGE = 1.25f

    /** Chance that an enemy turning up at [stage] is elite. */
    fun chance(stage: Int) = if (stage < 6) 0f else minOf(0.04f + 0.012f * (stage - 6), 0.18f)
}
