package dev.david.tessel.core.content

/** Read-only classification for the codex; no progression or serialized IDs change. */
enum class BuildFilter(val label:String) {
    ALL("All"), TWIN("Twin"), RAPID("Rapid"), CANNON("Cannon"), SNIPER("Sniper"),
    DRONE("Drone"), RAMMER("Rammer"), SECRET("Secret");

    fun matches(frame:TankEvolution):Boolean = when(this) {
        ALL -> true
        SECRET -> frame.secret
        DRONE -> frame.drones
        RAMMER -> frame.melee
        CANNON -> frame in listOf(TankEvolution.DESTROYER,TankEvolution.ANNIHILATOR,TankEvolution.ARTILLERY)
        TWIN -> frame in listOf(TankEvolution.TWIN,TankEvolution.TRIPLET,TankEvolution.QUAD,TankEvolution.FOCUS,TankEvolution.FAN,TankEvolution.CROSSFIRE,TankEvolution.QUAD_GUARD)
        RAPID -> frame in listOf(TankEvolution.MACHINE,TankEvolution.MINIGUN,TankEvolution.GATLING,TankEvolution.SUPPRESSOR)
        SNIPER -> frame in listOf(TankEvolution.SNIPER,TankEvolution.ASSASSIN,TankEvolution.RANGER,TankEvolution.MARKSMAN,TankEvolution.PRISM)
    }
}
object CodexCatalog {
    fun legacy(id:Int)=id==Upgrades.MULTISHOT || id==Upgrades.VELOCITY
    fun firstStage(id:Int)=Upgrades.firstStage(id)
    fun unlockStage(frame:TankEvolution)=if(frame.secret)40 else when(frame.tier){0->1;1->5;2->15;3->25;else->35}
    fun compatible(id:Int,filter:BuildFilter)=!legacy(id) && TankEvolution.entries.any { filter.matches(it) && Upgrades.offered(id,it) }
    fun role(frame:TankEvolution)=when {
        frame.laser -> "Railgun · extreme range"
        frame.drones -> "Drone commander"
        frame.melee -> "Impact & regeneration"
        BuildFilter.CANNON.matches(frame) -> "Heavy cannon · slow reload"
        BuildFilter.SNIPER.matches(frame) -> "Precision & penetration"
        BuildFilter.RAPID.matches(frame) -> "Sustained fire"
        frame.barrels>1 -> "Multi-barrel coverage"
        else -> "Standard gun"
    }
}
