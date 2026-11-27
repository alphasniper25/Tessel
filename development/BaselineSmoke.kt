import dev.david.tessel.core.content.*
import dev.david.tessel.core.sim.*
import kotlin.math.abs
fun main() {
    val inputs = Array(MAX_PLAYERS) { PlayerInput() }
    val w = World(seed = 7).apply { spawning = false }
    val start = w.players.x[0]
    inputs[0].moveX = 1f
    repeat(TICK_HZ) { w.step(inputs) }
    check(abs(w.players.x[0] - start - Balance.PLAYER_SPEED) < 0.02f)
    println("PASS: movement at 60 simulation ticks per second")
    inputs[0].moveX = 0f
    var remaining = 120 * TICK_HZ
    while(w.phase != Phase.SHOP && remaining-- > 0) w.step(inputs)
    check(w.phase == Phase.SHOP)
    println("PASS: stage transitions to shop")
    w.players.coins[0] = 123
    val cp = checkNotNull(Checkpoint.read(w.capture().write()))
    val restored = World(seed = 8)
    restored.restore(cp, seed = 8) { true }
    check(restored.phase == Phase.SHOP && restored.players.coins[0] == 123)
    println("PASS: checkpoint round trip restores coins and shop phase")
    restored.nextStage()
    check(restored.phase == Phase.FIGHT && restored.stage == w.stage + 1)
    println("PASS: next stage after checkpoint")
    check(Upgrades.count == 17 && Perks.count == 10)
    println("PASS: current upgrade and perk counts")
}
