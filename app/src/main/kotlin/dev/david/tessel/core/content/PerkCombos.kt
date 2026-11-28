package dev.david.tessel.core.content

/** Synergies derive from the two actual run perks. They are never random drops or purchases.
 * Removing a perk (including Core theft) immediately disables the corresponding synergy. */
enum class PerkCombo(val title:String,val first:Int,val second:Int,val hint:String,val effect:String,val third:Int=-1){
    LAST_LIGHT("Phoenix Pulse",Perks.SECOND_WIND,Perks.LAST_STAND,"When the last ember survives, its light consumes the storm.",
        "When Second Wind saves you, clear poison and freeze, and destroy hostile projectiles within 120 units."),
    REACTOR_SYNC("Kinetic Capacitor",Perks.OVERCLOCK,Perks.HEAVY_ROUNDS,"A quiet reactor stores more than firepower.",
        "After 8 seconds without a hit, the next projectile hit absorbs 1 HP of damage. Any hit resets the charge."),
    FIELD_RUNNER("Purge Protocol",Perks.SIPHON,Perks.QUICK_FEET,"A hunter outruns the venom in its veins.",
        "Every 10th enemy kill clears poison, burn and freeze immediately."),
    REACTIVE_PLATING("Living Armor",Perks.THICK_SKIN,Perks.SHOCKWAVE,"The cracked hull remembers how to mend.",
        "At or below half health, regenerate 1 HP every 8 seconds of combat, or keep faster existing regeneration."),
    SALVAGE_CONTRACT("Salvage Cache",Perks.MAGNET_CORE,Perks.HAGGLER,"Some scrap opens a door that coins cannot.",
        "Gain one additional free reroll each shop, independent of purchased refresh skills."),
    SURVEYOR("Surveyor",Perks.RANGEFINDER,Perks.HARDPOINT,"The survey finds resources beyond the target.","Gain an additional 20% coin pickup radius."),
    SOLAR_REBIRTH("Solar Rebirth",Perks.SECOND_WIND,Perks.LAST_STAND,"Three embers, one rebirth.","When Phoenix Pulse triggers, recover to 3 HP instead of 1, and clear hostile shots within 180 units.",Perks.VITAL_CORE),
    STORM_CELL("Storm Cell",Perks.OVERCLOCK,Perks.HEAVY_ROUNDS,"A charged shield feeds the reactor.","A charged Kinetic Capacitor block grants a 3-second combat surge.",Perks.RAPID_CYCLE),
    PHASE_ESCAPE("Phase Escape",Perks.SIPHON,Perks.QUICK_FEET,"The tenth mark opens a safe path.","Every Purge Protocol activation also grants one second of damage immunity.",Perks.AFTERBURNER),
    REPRISAL("Reprisal",Perks.THICK_SKIN,Perks.SHOCKWAVE,"A wounded hull feeds on victory.","Every tenth kill while at or below half health restores 1 HP.",Perks.DEADEYE),
    RECLAMATION("Reclamation",Perks.MAGNET_CORE,Perks.HAGGLER,"The machine repays a free exchange.","Each free shop reroll restores 1 missing HP.",Perks.VACUUM),
    HORIZON("Horizon",Perks.RANGEFINDER,Perks.HARDPOINT,"Beyond the farthest line lies speed.","Move 15% faster while at full health.",Perks.LONGSHOT);
    val inputs:List<Int> = if(third<0)listOf(first,second)else listOf(first,second,third)
    val mask:Int=inputs.fold(0){acc,id->acc or (1 shl id)}
    fun active(perks:Int)=perks and mask==mask
    fun progress(perks:Int)=Integer.bitCount(perks and mask)
}
