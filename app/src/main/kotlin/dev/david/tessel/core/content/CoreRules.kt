package dev.david.tessel.core.content

/** Every number of the final fight. Distances are arena units, times are in seconds. */
object CoreRules {
    /** The stage that ends in the Core. */
    const val STAGE = 50
    /** The final arena is driven by telegraphed hazards and reinforcement waves, not constant crowd spam. */
    const val STEADY_SPAWNS = .08f
    const val HARD_STEADY_SPAWNS = .14f
    const val REINFORCEMENT_SPAWNS = .22f
    /** Avoid compounding the ordinary boss-repeat multiplier on all nine final-fight bosses. */
    const val FINAL_ROUND_SCALE = 1.35f

    /** Health, before the usual boss scaling. The second form has less. */
    const val HP_FIRST = 2200f
    const val HP_SECOND = 900f

    /** A borrowed boss has this share of its usual health. */
    const val ESCORT_HP = 0.30f

    // Grid strikes.
    const val CELL = 112f
    const val WARN_SECONDS = 1.9f
    const val HIT_SECONDS = 0.4f
    const val STUN_SECONDS = 1.5f

    /** Gap between strikes at the start of the first form, and how much faster it gets. */
    const val STRIKE_GAP = 4.6f
    const val STRIKE_GAP_MIN = 3.2f

    /** Every second of the fight makes the first form this much faster; losing health adds up to [HURRY_BY_HEALTH]. */
    const val HURRY_PER_SECOND = 0.007f
    const val HURRY_BY_HEALTH = 0.6f

    /** The second form strikes this much less often, since eight bosses are already out. */
    const val SECOND_FORM_SLOWER = 1.5f

    // Perk theft.
    const val FIRST_THEFT_SECONDS = 7f
    const val THEFT_GAP = 22f
    const val THEFT_GAP_MIN = 9f
    const val THEFT_GAP_SHRINK = 2.5f

    // The second form.
    const val TRANSFORM_SECONDS = 3.5f
    const val SENT_OUT = 2
    const val BUBBLE_RX = 560f
    const val BUBBLE_RY = 255f
    const val BUBBLE_RADIUS = 60f

    /** A boss below this share of its health flies back to be mended. */
    const val RECALL_BELOW = 0.3f

    /** Share of its health a boss in its bubble regains each second, and the share at which it goes out again. */
    const val MEND_PER_SECOND = 0.1f
    const val READY_AT = 0.9f
    const val RECALL_SPEED = 260f

    /** Health a strike takes, by difficulty. */
    fun strikeDamage(d: Difficulty) = when (d) {
        Difficulty.EASY -> 1
        Difficulty.NORMAL, Difficulty.HARD -> 2
        Difficulty.IMPOSSIBLE -> 3
    }
}
