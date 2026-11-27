import dev.david.tessel.core.content.*
import dev.david.tessel.core.net.*
import dev.david.tessel.core.sim.*
import kotlin.random.Random
import kotlin.math.abs

fun main() {
    var checks = 0
    fun pass(s: String, block: () -> Unit) { block(); println("PASS ${++checks}: $s") }
    val rng = Random(378)
    val w = World(seed = 8).apply { reset(players = 8, seed = 8); spawning = false }
    repeat(350) { w.enemies.add(it + 1, it % 17, rng.nextFloat() * ARENA_W, rng.nextFloat() * ARENA_H, 8f) }
    repeat(240) { w.coins.add(500 + it, rng.nextFloat()*ARENA_W,rng.nextFloat()*ARENA_H,0f,0f,1) }
    val buf = Wire.buffer(MAX_MESSAGE * 2)
    val encoder = SnapshotPacket.Encoder(); val decoder = SnapshotPacket.Decoder()
    fun raw() { Protocol.writeSnapshot(buf, w, false, false, 0, Events(), Events(), Events(), 1, Echo()) }
    raw(); val expected = Wire.finish(buf); raw(); val packed = encoder.encode(buf)
    pass("busy snapshot compressed losslessly (${expected.size} -> ${packed.size} bytes)") {
        check(packed.size < expected.size); val b = checkNotNull(decoder.decode(packed)); val got = ByteArray(b.remaining()); b.get(got); check(got.contentEquals(expected.copyOfRange(4,expected.size)))
    }
    pass("corrupt, truncated and oversized packets rejected") {
        check(decoder.decode(packed.copyOf(packed.size - 1)) == null)
        check(decoder.decode(packed + byteArrayOf(0)) == null)
        val invalid = packed.copyOf(); invalid[4]=0; invalid[5]=0; invalid[6]=1; invalid[7]=0; check(decoder.decode(invalid)==null)
        val corrupt = packed.copyOf(); corrupt[corrupt.lastIndex]=(corrupt.last().toInt() xor 1).toByte(); check(decoder.decode(corrupt)==null)
    }
    pass("incompressible snapshot stays raw") {
        val b=Wire.buffer(3000); Wire.begin(b,Wire.SNAPSHOT); b.put(rng.nextBytes(2000)); val out=encoder.encode(b); check(out.size==2004 && Wire.typeOf(out)==Wire.SNAPSHOT)
    }
    pass("client consumes compressed host state; kill event reference retained") {
        val c=ClientSession(1,8){_,_->true}; val host=HostSession(w){p,bytes,_-> if(p==1)c.receive(bytes,1_000_000_000L);true}; host.connect(1)
        val e=Events();e.add(Events.ENEMY_KILLED,Bestiary.DOT,1f,2f,ref=381);host.collect(e);host.pump(1_000_000_000L,false,false)
        val s=checkNotNull(c.latest);check(s.enemyCount==350 && s.coinCount==240 && s.events.ref[0]==381);host.close();c.close()
    }
    pass("host keeps 30 Hz combat and 10 Hz pause; unpause sends immediately") {
        val world=World().apply { reset(players=2,seed=8) };var count=0;val h=HostSession(world){_,_,_->count++;true};h.connect(1)
        for(i in 0 until 180)h.pump(1_000_000_000L+i*16_666_667L,false,false);check(count in 89..91);val start=count
        for(i in 0 until 180)h.pump(4_000_000_000L+i*16_666_667L,true,false);check(count-start in 29..31);val pause=count
        h.pump(7_000_000_000L,false,false);check(count==pause+1);h.close()
    }
    val grid=ShotCandidates();grid.rebuild(w.enemies)
    pass("grid and full-scan collision outcomes match on 2,000 randomized shots") {
        var total=0L
        repeat(2000) {
            val x=rng.nextFloat()*ARENA_W;val y=rng.nextFloat()*ARENA_H;val vx=rng.nextFloat()*70-35;val vy=rng.nextFloat()*70-35;val r=2+rng.nextFloat()*9;val pierce=rng.nextInt(0,5)
            fun pool()=BulletPool(1).apply {add(1,x,y,vx,vy,10,1f,r,pierce,1);this.x[0]=x+vx;this.y[0]=y+vy}
            val a=pool();val b=pool();val ra=Reports();val rb=Reports()
            total+=grid.query(x,y,x+vx,y+vy,r)
            Pilot.cullShots(a,w,ra);Pilot.cullShots(b,w,rb,grid)
            check(a.count==b.count && ra.hits==rb.hits)
            for(j in 0 until ra.hits)check(ra.hitId[j]==rb.hitId[j])
            if(a.count>0)check(a.pierce[0]==b.pierce[0]&&a.lastHit[0]==b.lastHit[0])
        }
        println("  candidates: $total vs ${2000L*w.enemies.count} full-scan checks")
    }
    pass("typed effects preserve character stats, compound boosts, healing and health") {
        val p=w.players;p.chooseCharacter(0,CharacterDef.RAM,w.difficulty.bonusHp);val lv=p.levels[0]
        lv[Upgrades.DAMAGE]=4;lv[Upgrades.CORE_BREAKER]=1;lv[Upgrades.SPEED]=5;lv[Upgrades.AFTERBURNER]=1;lv[Upgrades.TITAN_PLATING]=1;lv[Upgrades.RAM_DAMAGE]=2;lv[Upgrades.FIELD_MEDIC]=1;lv[Upgrades.HEAL_POWER]=3
        check(abs(p.damage(0)-Balance.BULLET_DAMAGE*1.6f*0.85f*1.8f)<0.0001f);check(abs(p.moveSpeed(0)-Balance.PLAYER_SPEED*1.3f*1.1f*1.18f)<0.001f);check(p.ramDamage(0)==5f);check(w.shop.healAmount(w,0)==7);check(Upgrades.amount(Upgrades.TITAN_PLATING,UpgradeStat.MAX_HP)==4f)
    }
    encoder.close();decoder.close();println("$checks performance checks passed")
}
