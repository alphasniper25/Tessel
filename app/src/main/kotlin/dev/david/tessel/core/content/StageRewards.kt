package dev.david.tessel.core.content

import kotlin.math.roundToInt

/** Per-seat stage completion reward. Only current-stage income and kills count. */
object StageRewards {
    fun share(stage: Int) = (1f - .10f * (stage.coerceAtLeast(1) - 1)).coerceAtLeast(.25f)
    fun coins(stage: Int, collected: Int, kills: Int): Int {
        val scale = Upgrades.inflation(stage.coerceIn(1, 50))
        val base = (if(stage<=4) 39 else if(stage<10) 25 else 23) * scale
        val performance = collected.coerceAtLeast(0) * share(stage) + kills.coerceAtLeast(0) * .35 * scale
        return (base + performance.coerceAtMost((if(stage<10) 45 else 36) * scale)).roundToInt()
    }
}
