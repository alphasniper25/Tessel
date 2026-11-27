package dev.david.tessel.core.sim
import dev.david.tessel.core.content.*

fun main(){
    var passed=0
    fun test(label:String,f:()->Unit){f();println("PASS $label");passed++}
    test("shielded ram hits nearby targets and consumes one charge") {
        val w=World(71);w.spawning=false; val p=w.players;p.evolution[0]=TankEvolution.RAMMER
        val x=p.x[0];val y=p.y[0];val a=w.spawnEnemy(Bestiary.DOT,x+24f,y);val b=w.spawnEnemy(Bestiary.SLAB,x+55f,y);val far=w.spawnEnemy(Bestiary.SLAB,x+200f,y)
        val hp=p.hp[0];val farHp=w.enemies.hp[far];w.hurtPlayer(0,Cause.touch(Bestiary.DOT));check(p.hp[0]==hp);check(p.shieldCooldown[0]>0);check(w.enemies.hp[a]<=0f&&w.enemies.hp[b]<=0f);check(w.enemies.hp[far]==farHp)
    }
    test("ram damage cards have multiplicative melee effect; regeneration improves") {
        val p=PlayerState();p.reset(1);p.evolution[0]=TankEvolution.FORTRESS;val d=p.ramDamage(0);p.levels[0][Upgrades.RAM_DAMAGE]=4;check(p.ramDamage(0)>d*1.25f);check(p.regenSeconds(0)==3);p.levels[0][Upgrades.REGEN]=1;check(p.regenSeconds(0)==2)
    }
    test("Prism pulse travels through three targets once") {
        val w=World(72);w.spawning=false;val p=w.players;p.evolution[0]=TankEvolution.PRISM;val x=p.x[0];val y=p.y[0];for(dist in listOf(150f,330f,500f))w.spawnEnemy(Bestiary.SLAB,x+dist,y)
        val input=Array(MAX_PLAYERS){PlayerInput()};input[0].aimX=1f;input[0].fire=true;w.step(input);input[0].fire=false;repeat(12){w.step(input)};check(p.kills[0]==3);check(p.fired[0]==1);check(w.enemies.count==0)
    }
    test("Siphon survives theft of another perk") {
        val w=World(73);w.spawning=false;w.invincible=true;w.skipTo(50);val i=w.spawnEnemy(Bestiary.CORE,500f,400f);w.enemies.mode[i]=1;w.core.begin();w.players.perks[0]=(1 shl Perks.SIPHON) or (1 shl Perks.MAGNET_CORE);repeat(9*TICK_HZ){w.core.think(i,1f)};check(w.players.has(0,Perks.SIPHON));check(!w.players.has(0,Perks.MAGNET_CORE))
    }
    println("$passed/4 branch checks passed")
}
