package dev.david.tessel.core.content

/** Three actual perk choices, independent of evolution stages. Existing reward
 * history still stores the original nine boss decision slots for migration. */
object PerkRounds {
    val stages=listOf(10,20,30)
    val pools=listOf(
        listOf(Perks.SECOND_WIND,Perks.OVERCLOCK,Perks.SIPHON,Perks.THICK_SKIN,Perks.MAGNET_CORE,Perks.RANGEFINDER),
        listOf(Perks.LAST_STAND,Perks.HEAVY_ROUNDS,Perks.QUICK_FEET,Perks.SHOCKWAVE,Perks.HAGGLER,Perks.HARDPOINT),
        listOf(Perks.VITAL_CORE,Perks.RAPID_CYCLE,Perks.AFTERBURNER,Perks.DEADEYE,Perks.VACUUM,Perks.LONGSHOT)
    )
    fun roundAt(stage:Int)=stages.indexOf(stage)
    fun roundOf(id:Int)=pools.indexOfFirst{id in it}
    fun choice(history:List<Int>,round:Int):Int{
        val index=RewardRounds.index(stages[round])
        return history.getOrElse(index){-1}.takeIf{Perks.isRunPerk(it)}?:-1
    }
}
