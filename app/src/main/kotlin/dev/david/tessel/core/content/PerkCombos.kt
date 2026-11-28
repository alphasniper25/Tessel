package dev.david.tessel.core.content

/** Five deliberately sparse recipes. They are discovered only by ownership,
 * never by visible hints or random drops. Losing any input disables the fusion. */
enum class PerkCombo(val title:String,val first:Int,val second:Int,val effect:String,val third:Int=-1){
    LAST_LIGHT("Phoenix Pulse",Perks.SECOND_WIND,Perks.LAST_STAND,
        "When Second Wind saves you, clear poison and freeze, and destroy hostile projectiles within 120 units."),
    REACTOR_SYNC("Kinetic Capacitor",Perks.OVERCLOCK,Perks.HEAVY_ROUNDS,
        "After 8 seconds without a hit, the next projectile hit absorbs 1 HP of damage. Any hit resets the charge."),
    SALVAGE_CONTRACT("Salvage Cache",Perks.MAGNET_CORE,Perks.HAGGLER,
        "Gain one additional free reroll each shop, independent of purchased refresh skills."),
    PHASE_ESCAPE("Phase Escape",Perks.SIPHON,Perks.QUICK_FEET,
        "Every tenth enemy kill clears poison, burn and freeze, and grants one second of damage immunity.",Perks.AFTERBURNER),
    REPRISAL("Reprisal",Perks.THICK_SKIN,Perks.SHOCKWAVE,
        "Every tenth enemy kill while at or below half health restores 1 HP.",Perks.DEADEYE);
    val inputs:List<Int> = if(third<0)listOf(first,second)else listOf(first,second,third)
    val mask:Int=inputs.fold(0){acc,id->acc or (1 shl id)}
    fun active(perks:Int)=perks and mask==mask
    fun progress(perks:Int)=Integer.bitCount(perks and mask)
}
