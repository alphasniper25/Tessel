package dev.david.tessel.core.sim

import dev.david.tessel.core.content.*
import kotlin.math.*

/** An isolated, deterministic eight-second combat loop using the actual game rules.
 * No saves, ledger, network, account or live game World are accessed. */
class CodexDemo(val frame:TankEvolution) {
    companion object { const val LOOP_TICKS=8*TICK_HZ }
    val world=World(seed=721L)
    private val input=Array(MAX_PLAYERS){PlayerInput()}
    var loops=0;private set
    init { reset() }
    private fun reset() {
        world.reset(seed=721L)
        world.spawning=false
        world.practice=false // preserve actual melee contact mechanics
        world.invincible=true // this is a demonstration, not a scored run
        world.players.evolution[0]=frame
        world.players.maxHp[0]=Balance.PLAYER_HP+frame.hullBonus
        world.players.hp[0]=world.players.maxHp[0]
        world.players.x[0]=ARENA_W*.43f;world.players.y[0]=ARENA_H*.5f
        world.players.remember()
        world.devSpeed.fill(.45f)
        world.devHealth.fill(1f)
        input[0].clear()
        for(i in 0..3) spawn(i)
    }
    private fun spawn(i:Int) {
        val a=(i%5-2)*.40f
        val reach=if(frame.melee)100f else if(frame.drones)175f else 230f
        val px=world.players.x[0];val py=world.players.y[0]
        val j=world.spawnEnemy(if(i%3==0)Bestiary.SLAB else Bestiary.PIP,
            (px+cos(a)*reach).coerceIn(50f,ARENA_W-50f),(py+sin(a)*reach).coerceIn(50f,ARENA_H-50f))
        if(j>=0){world.enemies.hp[j]*=1.5f;world.enemies.maxHp[j]=world.enemies.hp[j]}
    }
    /** Returns true when the visual effects should reset to begin a fresh loop. */
    fun advance(ticks:Int=2):Boolean {
        var resetLoop=false
        world.events.clear()
        repeat(ticks.coerceIn(1,8)) {
            if(world.tick>=LOOP_TICKS){reset();loops++;resetLoop=true}
            if(world.tick%45==0 && world.enemies.count<4)spawn(world.tick/45)
            val p=world.players;val e=world.enemies
            val px=p.x[0];val py=p.y[0]
            var nearest=-1;var best=Float.MAX_VALUE
            for(i in 0 until e.count){val dx=e.x[i]-px;val dy=e.y[i]-py;val d=dx*dx+dy*dy;if(d<best){best=d;nearest=i}}
            val out=input[0];out.clear()
            if(nearest>=0){
                val dx=e.x[nearest]-px;val dy=e.y[nearest]-py
                val d=sqrt(dx*dx+dy*dy).coerceAtLeast(.01f)
                out.aimX=dx/d;out.aimY=dy/d;out.fire=!frame.melee
                if(frame.melee){out.moveX=out.aimX;out.moveY=out.aimY}
                else {
                    val drift=sin(world.tick*.022f)*.33f
                    val retreat=if(d<100f)-.48f else if(d>260f).28f else 0f
                    out.moveX=-out.aimY*drift+out.aimX*retreat
                    out.moveY=out.aimX*drift+out.aimY*retreat
                }
            }
            world.step(input)
        }
        return resetLoop
    }
}
