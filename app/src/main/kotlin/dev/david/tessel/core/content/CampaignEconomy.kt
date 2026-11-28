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
        Band(1,34,32,.28,.65f),
        Band(5,30,30,.27,.55f),
        Band(15,24,26,.25,.35f),
        Band(30,20,23,.22,.22f),
        Band(50,17,19,.20,.16f)
    )
    // Interpolate anchors; no abrupt income cliff when a player enters a new zone.
    private val stageBands=Array(50){i->
        val stage=i+1
        val next=bands.indexOfFirst{stage<=it.through}
        if(next<=0)bands.first() else {
            val a=bands[next-1];val b=bands[next];val t=(stage-a.through).toDouble()/(b.through-a.through)
            Band(stage,(a.base+(b.base-a.base)*t).roundToInt(),(a.bonusCap+(b.bonusCap-a.bonusCap)*t).roundToInt(),
                a.killBonus+(b.killBonus-a.killBonus)*t,(a.pickupShare+(b.pickupShare-a.pickupShare)*t).toFloat())
        }
    }
    fun band(stage:Int)=stageBands[stage.coerceIn(1,50)-1]
    fun priceScale(stage:Int)=PRICE_GROWTH.pow(stage.coerceIn(1,50)-1)
    fun pickupBase(stage:Int)=PICKUP_GROWTH.pow(stage.coerceIn(1,50)-1).toFloat()
    fun clearReward(stage:Int,collected:Int,kills:Int,difficulty:Difficulty):Int{
        val b=band(stage);val scale=priceScale(stage)
        val multiplier=when(difficulty){Difficulty.EASY->.93;Difficulty.NORMAL->1.0;Difficulty.HARD->1.05;Difficulty.IMPOSSIBLE->1.10}
        val extra=collected.coerceAtLeast(0)*b.pickupShare+kills.coerceAtLeast(0)*b.killBonus*scale
        return ((b.base*scale+extra.coerceAtMost(b.bonusCap*scale))*multiplier).roundToInt().coerceAtLeast(1)
    }
    // Each seat receives full pickup value up to a generous wave allowance.
    // Extra pickups still pay, but the marginal rate gradually falls from stage 10.
    private val pickupAllowances=LongArray(50){i->(75*priceScale(i+1)).roundToInt().toLong()}
    private val excessRates=IntArray(50){i->100-((i+1-10).coerceIn(0,20)*75/20)}
    fun pickupAllowance(stage:Int)=pickupAllowances[stage.coerceIn(1,50)-1]
    fun pickupPayout(stage:Int,raw:Long):Long{
        val value=raw.coerceAtLeast(0L)
        val allowance=pickupAllowance(stage)
        val rate=excessRates[stage.coerceIn(1,50)-1]
        val extra=(value-allowance).coerceAtLeast(0L)
        return minOf(value,allowance)+(extra/100)*rate+(extra%100)*rate/100
    }
    /** Base skill prices are stage-gated; the common ownership surcharge
     * is applied by SkillGraph, regardless of which route was chosen. */
    fun skillPrice(base:Int,stage:Int)=(base*priceScale(stage)*1.12).roundToInt()
}
