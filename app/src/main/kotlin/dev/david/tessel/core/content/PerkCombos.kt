package dev.david.tessel.core.content

/** Synergies are derived from currently owned boss perks. No extra saved bits,
 * reward draws, currency or network messages. A stolen ingredient temporarily
 * disables the synergy, and returning it restores the synergy. */
enum class PerkCombo(val title:String,val first:Int,val second:Int,val hint:String,val effect:String){
    PHOENIX("Phoenix Protocol",Perks.SECOND_WIND,Perks.LAST_STAND,"Survive the last hit; make your last two HP count.","20% more bullet/drone damage while at 1–2 HP."),
    SIEGE("Siege Cycle",Perks.OVERCLOCK,Perks.HEAVY_ROUNDS,"Combine precision timing with heavy fire.","10% faster volleys/drone replacement while Overclock is active."),
    BULWARK("Reactive Armor",Perks.THICK_SKIN,Perks.SHOCKWAVE,"A tougher hull turns incoming hits into retaliation.","25% more Shockwave damage."),
    SALVAGE("Salvage Current",Perks.QUICK_FEET,Perks.MAGNET_CORE,"Keep moving; bring the field's salvage with you.","20% more pickup radius."),
    MEDIC("Field Quartermaster",Perks.SIPHON,Perks.HAGGLER,"Sustain your hull and bargain for supplies.","Purchased healing restores 1 additional HP.");
    val bits get()=(1 shl first) or (1 shl second)
    fun active(mask:Int)=mask and bits==bits
    fun hinted(mask:Int)=mask and bits!=0
}
