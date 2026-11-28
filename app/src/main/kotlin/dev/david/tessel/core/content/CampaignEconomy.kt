package dev.david.tessel.core.content

import kotlin.math.pow
import kotlin.math.roundToInt

/** Economy bands are shared by all tank families. Coins and prices use the
 * same gentler denomination curve. Early affordability is retained; late
 * performance bonuses are deliberately smaller in real purchasing power. */
object CampaignEconomy {
    const val PRICE_GROWTH=1.075
    const val PICKUP_GROWTH=1.065
    data class Band(val through:Int,val base:Int,val bonusCap:Int,val killBonus:Double,val pickupShare:Float)
    val bands=listOf(
        Band(4,34,32,.28,.65f),
        Band(14,24,28,.25,.35f),
        Band(29,20,23,.22,.22f),
        Band(50,17,19,.20,.16f)
    )
    fun band(stage:Int)=bands.first{stage.coerceIn(1,50)<=it.through}
    fun priceScale(stage:Int)=PRICE_GROWTH.pow(stage.coerceIn(1,50)-1)
    fun pickupBase(stage:Int)=PICKUP_GROWTH.pow(stage.coerceIn(1,50)-1).toFloat()
    fun clearReward(stage:Int,collected:Int,kills:Int,difficulty:Difficulty):Int{
        val b=band(stage);val scale=priceScale(stage)
        val multiplier=when(difficulty){Difficulty.EASY->.93;Difficulty.NORMAL->1.0;Difficulty.HARD->1.05;Difficulty.IMPOSSIBLE->1.10}
        val extra=collected.coerceAtLeast(0)*b.pickupShare+kills.coerceAtLeast(0)*b.killBonus*scale
        return ((b.base*scale+extra.coerceAtMost(b.bonusCap*scale))*multiplier).roundToInt().coerceAtLeast(1)
    }
    /** Fixed skill prices: a late player can change direction, but the
     * investment competes with cards and healing rather than free milestones. */
    fun skillPrice(base:Int,stage:Int)=(base*priceScale(stage)*1.12).roundToInt()
}
