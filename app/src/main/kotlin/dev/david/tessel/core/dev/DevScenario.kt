package dev.david.tessel.core.dev

import dev.david.tessel.core.content.TankEvolution

/** Transient scenario editor; deliberately not part of a campaign checkpoint or network action. */
enum class DevReward(val label:String){ AUTO("Normal"), NONE("None"), PERKS("Perk reward"), EVOLUTION("Evolution reward") }
data class DevScenario(
    val frame:TankEvolution=TankEvolution.BASIC,
    val skillMask:Long=0L,
    val hp:Int=-1,
    val stock:List<Int> = listOf(-1,-1,-1),
    val reward:DevReward=DevReward.AUTO,
    val allMissions:Boolean=true,
)
