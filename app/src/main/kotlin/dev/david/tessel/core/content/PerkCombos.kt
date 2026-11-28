package dev.david.tessel.core.content

/** Synergies derive from the two actual run perks. They are never random drops or purchases.
 * Removing a perk (including Core theft) immediately disables the corresponding synergy. */
enum class PerkCombo(val title:String,val first:Int,val second:Int,val hint:String,val effect:String){
    LAST_LIGHT("Last Light",Perks.SECOND_WIND,Perks.LAST_STAND,"Survive the last hit; make your final pips count.",
        "At 1–2 HP, deal an additional 15% gun, drone and ram damage."),
    REACTOR_SYNC("Reactor Sync",Perks.OVERCLOCK,Perks.HEAVY_ROUNDS,"Combine patience with a heavier payload.",
        "While Overclock is active, Heavy Rounds slows firing/refill by 7% instead of 15%."),
    FIELD_RUNNER("Field Runner",Perks.SIPHON,Perks.QUICK_FEET,"Keep moving; restore yourself by hunting.",
        "Siphon heals every 20 kills instead of every 25."),
    REACTIVE_PLATING("Reactive Plating",Perks.THICK_SKIN,Perks.SHOCKWAVE,"Let reinforced armor send a warning back.",
        "Shockwave deals 40% more damage."),
    SALVAGE_CONTRACT("Salvage Contract",Perks.MAGNET_CORE,Perks.HAGGLER,"Turn collected scrap into a better deal.",
        "An extra 5% multiplicative discount on newly priced upgrades; existing locks keep their quote.");
    val mask:Int get()=(1 shl first) or (1 shl second)
    fun active(perks:Int)=perks and mask==mask
    fun progress(perks:Int)=Integer.bitCount(perks and mask)
}
