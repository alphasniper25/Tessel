package dev.david.tessel.core.net

import dev.david.tessel.core.content.Balance
import dev.david.tessel.core.content.Bestiary
import dev.david.tessel.core.sim.ARENA_H
import dev.david.tessel.core.sim.ARENA_W
import dev.david.tessel.core.sim.BulletPool
import dev.david.tessel.core.sim.PlayerInput
import dev.david.tessel.core.sim.TICK_HZ
import dev.david.tessel.core.sim.TICK_SECONDS
import dev.david.tessel.core.sim.World
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * A joining phone's own player, run locally so the thumb never waits for the
 * network. Movement is reported to the host, which accepts it. Shots fly
 * here, and what they hit, as drawn on this phone, is what counts: each hit
 * is reported and the host applies it.
 */
class Pilot {
    var x = 0f
        private set
    var y = 0f
        private set
    var aimX = 1f
        private set
    var aimY = 0f
        private set
    var placed = false
        private set

    val shots = BulletPool(256)
    private var reload = 0f
    private var nextId = -1

    fun reset() {
        placed = false
        reload = 0f
        shots.clear()
    }

    fun place(px: Float, py: Float) {
        x = px
        y = py
        placed = true
    }

    /** One 60 Hz step. Returns true when a volley left the barrel. */
    fun step(input: PlayerInput, s: Snapshot, me: Int): Boolean {
        val standing = s.hp[me] > 0
        val travel = s.moveSpeed[me] * (if (standing) 1f else Balance.DOWNED_SPEED) * TICK_SECONDS
        val held = s.stun[me] > 0
        var mx = if (held) 0f else input.moveX
        var my = if (held) 0f else input.moveY
        val len = sqrt(mx * mx + my * my)
        if (len > 1f) {
            mx /= len
            my /= len
        }
        val r = Balance.PLAYER_RADIUS
        x = (x + mx * travel).coerceIn(r, ARENA_W - r)
        y = (y + my * travel).coerceIn(r, ARENA_H - r)

        var ax=input.aimX; var ay=input.aimY; var firing=input.fire
        if(s.evolution[me].drones && !firing){
            var best=s.range[me]*s.range[me]; var target=-1
            for(j in 0 until s.enemyCount){val dx=s.enemyX[j]-x;val dy=s.enemyY[j]-y;val ds=dx*dx+dy*dy;if(ds<best){best=ds;target=j}}
            if(target>=0){ax=s.enemyX[target]-x;ay=s.enemyY[target]-y;firing=true}
        }
        val aimLen = sqrt(ax*ax+ay*ay)
        val aiming = aimLen > 0.0001f
        if (aiming) {
            aimX = ax / aimLen
            aimY = ay / aimLen
        }

        shots.remember()
        for (i in 0 until shots.count) {
            shots.x[i] += shots.vx[i] * TICK_SECONDS
            shots.y[i] += shots.vy[i] * TICK_SECONDS
            shots.life[i] -= 1
            if (shots.x[i] < -24f || shots.x[i] > ARENA_W + 24f || shots.y[i] < -24f || shots.y[i] > ARENA_H + 24f) shots.life[i] = 0
        }
        shots.sweep()

        reload -= TICK_SECONDS
        var fired = false
        if (standing && firing && aiming && !held && !s.evolution[me].melee) {
            if (reload <= 0f) {
                volley(s, me)
                reload += 1f / s.fireRate[me]
                fired = true
            }
        } else if (reload < 0f) {
            reload = 0f
        }
        return fired
    }

    /** Shots that reached a drawn enemy are used up, and each hit goes into [reports] for the host. */
    fun cull(view: World, reports: Reports, candidates: ShotCandidates? = null) = cullShots(shots, view, reports, candidates)

    private fun volley(s: Snapshot, me: Int) {
        val speed = s.bulletSpeed[me]
        val life = (s.range[me] / speed * TICK_HZ).toInt()
        val frame=s.evolution[me]
        val muzzle = frame.muzzle()
        val n = s.volley[me].coerceAtLeast(0)
        val aim = atan2(aimY, aimX)
        for (k in 0 until n) {
            val origin=frame.originAngle(aim,k,n); val offset=if(frame.drones)0f else frame.offset(k,n)
            val ox=x+cos(origin)*muzzle-aimY*offset; val oy=y+sin(origin)*muzzle+aimX*offset
            val a=if(frame.drones)frame.droneAim(aim,ox,oy,s.range[me],s.enemyCount,{s.enemyX[it]},{s.enemyY[it]},{s.enemyHp[it]>0f})else origin
            shots.add(nextId--, ox, oy, cos(a)*speed, sin(a)*speed, life, 0f, s.bulletRadius[me], s.pierce[me], me)
        }
    }

    companion object {
        /**
         * Checks each shot's path since the last step against the drawn
         * enemies, so a fast shot cannot skip over a small one. With
         * [reports], hits are recorded for the host; without, the shots are
         * someone else's and only vanish.
         */
        fun cullShots(shots: BulletPool, view: World, reports: Reports?, candidates: ShotCandidates? = null) {
            val e = view.enemies
            for (i in 0 until shots.count) {
                if (shots.life[i] <= 0) continue
                val ax = shots.prevX[i]
                val ay = shots.prevY[i]
                val sx = shots.x[i] - ax
                val sy = shots.y[i] - ay
                val len2 = (sx * sx + sy * sy).coerceAtLeast(0.0001f)
                val n = candidates?.query(ax, ay, shots.x[i], shots.y[i], shots.radius[i]) ?: e.count
                for (k in 0 until n) {
                    val j = candidates?.indices?.get(k) ?: k
                    if (shots.hasHit(i,e.id[j]) || e.hp[j] <= 0f || (e.type[j]==Bestiary.CORE && view.core.hazards.hidden>0)) continue
                    val reach = shots.radius[i] + Bestiary.of(e.type[j]).radius
                    // Closest point of this step's path to the enemy's centre.
                    val along = (((e.x[j] - ax) * sx + (e.y[j] - ay) * sy) / len2).coerceIn(0f, 1f)
                    val dx = e.x[j] - (ax + sx * along)
                    val dy = e.y[j] - (ay + sy * along)
                    if (dx * dx + dy * dy >= reach * reach) continue
                    shots.recordHit(i,e.id[j])
                    if (reports != null) {
                        val speed = sqrt(shots.vx[i] * shots.vx[i] + shots.vy[i] * shots.vy[i]).coerceAtLeast(0.001f)
                        reports.hit(e.id[j] and 0xFFFF, shots.vx[i] / speed, shots.vy[i] / speed)
                    }
                    if (shots.pierce[i] > 0) {
                        shots.pierce[i]--
                    } else {
                        shots.life[i] = 0
                        break
                    }
                }
            }
            shots.sweep()
        }
    }
}
