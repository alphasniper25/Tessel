package dev.david.tessel.core.net

import dev.david.tessel.core.content.CharacterDef
import dev.david.tessel.core.content.CardMission
import dev.david.tessel.core.sim.MAX_PLAYERS

data class LobbyChoice(val character: CharacterDef = CharacterDef.BALANCED, val missionMask: Int = 0, val ready: Boolean = false, val appearance: dev.david.tessel.core.content.TankLook = dev.david.tessel.core.content.TankLook())
data class LobbyStatus(val revision: Int, val self: Int, val choices: List<LobbyChoice>)

/** Host owns the roster epoch; old ready packets cannot authorize a changed roster. */
class LobbyReady {
    var revision = 0; private set
    private val choices = Array(MAX_PLAYERS) { LobbyChoice() }
    fun reset() { revision++; choices.fill(LobbyChoice()) }
    fun rosterChanged() { revision++; for (p in choices.indices) choices[p] = choices[p].copy(ready = false) }
    fun clear(seat: Int) { choices[seat] = LobbyChoice() }
    fun get(seat: Int) = choices[seat]
    fun set(seat: Int, epoch: Int, choice: LobbyChoice): Boolean {
        if (epoch != revision || seat !in choices.indices) return false
        choices[seat] = choice.copy(missionMask = choice.missionMask and CardMission.allMask)
        return true
    }
    fun allReady(seats: List<Int>) = seats.size >= 2 && seats.all { it in choices.indices && choices[it].ready }
    fun status(seats: List<Int>, self: Int) = LobbyStatus(revision, seats.indexOf(self), seats.map { choices[it] })
}
