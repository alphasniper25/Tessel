package dev.david.tessel.core.content

/** Synergies derive from the two actual run perks. They are never random drops or purchases.
 * Removing a perk (including Core theft) immediately disables the corresponding synergy. */
enum class PerkCombo(val title:String,val first:Int,val second:Int,val hint:String,val effect:String){
    LAST_LIGHT("Phoenix Pulse",Perks.SECOND_WIND,Perks.LAST_STAND,"When the last ember survives, its light consumes the storm.",
        "When Second Wind saves you, clear poison and freeze, and destroy hostile projectiles within 120 units."),
    REACTOR_SYNC("Kinetic Capacitor",Perks.OVERCLOCK,Perks.HEAVY_ROUNDS,"A quiet reactor stores more than firepower.",
        "After 8 seconds without a hit, the next projectile hit absorbs 1 HP of damage. Any hit resets the charge."),
    FIELD_RUNNER("Purge Protocol",Perks.SIPHON,Perks.QUICK_FEET,"A hunter outruns the venom in its veins.",
        "Every 10th enemy kill clears poison, burn and freeze immediately."),
    REACTIVE_PLATING("Living Armor",Perks.THICK_SKIN,Perks.SHOCKWAVE,"The cracked hull remembers how to mend.",
        "At or below half health, regenerate 1 HP every 8 seconds of combat, or keep faster existing regeneration."),
    SALVAGE_CONTRACT("Salvage Cache",Perks.MAGNET_CORE,Perks.HAGGLER,"Some scrap opens a door that coins cannot.",
        "Gain one additional free reroll each shop, independent of purchased refresh skills.");
    val mask:Int get()=(1 shl first) or (1 shl second)
    fun active(perks:Int)=perks and mask==mask
    fun progress(perks:Int)=Integer.bitCount(perks and mask)
}
