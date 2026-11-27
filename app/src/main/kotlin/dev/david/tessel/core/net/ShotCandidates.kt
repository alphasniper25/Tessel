package dev.david.tessel.core.net

import dev.david.tessel.core.content.Bestiary
import dev.david.tessel.core.sim.ARENA_W
import dev.david.tessel.core.sim.ARENA_H
import dev.david.tessel.core.sim.OUTSIDE
import dev.david.tessel.core.sim.MAX_ENEMIES
import dev.david.tessel.core.sim.EnemyPool
import dev.david.tessel.core.sim.Grid

/** Reused broad phase for client bullet sweeps. Keeps original enemy-index order,
 * so pierce/first-hit decisions stay identical to the full scan. */
class ShotCandidates {
    private val grid = Grid(64f, -OUTSIDE, -OUTSIDE, ARENA_W + OUTSIDE, ARENA_H + OUTSIDE, MAX_ENEMIES)
    private val maxRadius = (0 until Bestiary.size).maxOf { Bestiary.of(it).radius }
    val indices = IntArray(MAX_ENEMIES)
    fun rebuild(enemies: EnemyPool) {
        grid.clear()
        for (i in 0 until enemies.count) grid.insert(i, enemies.x[i], enemies.y[i])
    }
    fun query(ax: Float, ay: Float, bx: Float, by: Float, radius: Float): Int {
        val reach = radius + maxRadius
        var n = 0
        for (row in grid.row(minOf(ay, by) - reach)..grid.row(maxOf(ay, by) + reach)) {
            for (col in grid.col(minOf(ax, bx) - reach)..grid.col(maxOf(ax, bx) + reach)) {
                var j = grid.head[row * grid.cols + col]
                while (j >= 0) { indices[n++] = j; j = grid.next[j] }
            }
        }
        java.util.Arrays.sort(indices, 0, n)
        return n
    }
}
