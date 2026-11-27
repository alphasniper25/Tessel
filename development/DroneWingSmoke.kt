package dev.david.tessel.core.sim
import dev.david.tessel.core.content.*
import dev.david.tessel.core.net.*
import dev.david.tessel.platform.game.Viewport
import kotlin.math.*
fun main(){
 var passed=0
 fun test(name:String, f:()->Unit){f();println("PASS $name");passed++}
 fun world(frame:TankEvolution,players:Int=1)=World(731).apply{reset(players);spawning=false;practice=true;this.players.evolution[0]=frame}
 val inputs=Array(MAX_PLAYERS){PlayerInput()}
 fun step(w:World,n:Int){repeat(n){w.step(inputs)}}
 test("all templates respect eight living slots, spawn automatically, no bullets") {
  for(f in TankEvolution.entries.filter{it.drones}){val w=world(f);val s=f.droneSpec!!;check(s.cap in 1..8);step(w,900);check(w.droneWing.count(0)==s.cap);check(w.bullets.count==0);check(w.players.fired[0]==0);check(w.droneWing.hp.count{it>0}==s.cap)}
 }
 test("destroyed drone waits for serial replenishment; fire rate improves interval") {
  fun time(up:Int):Int{val w=world(TankEvolution.OVERSEER);w.players.levels[0][Upgrades.FIRE_RATE]=up;step(w,1);w.droneWing.hp[0]=0;var n=0;while(w.droneWing.count(0)==0&&n<200){step(w,1);n++};check(w.droneWing.count(0)==1);return n}
  val a=time(0);val b=time(10);check(a in 60..70);check(b in 30 until a)
 }
 test("loss at full cap waits the full replacement interval") {
  val w=world(TankEvolution.OVERSEER);step(w,900);w.droneWing.hp[0]=0;step(w,65);check(w.droneWing.count(0)==3);step(w,1);check(w.droneWing.count(0)==4)
 }
 test("idle orbit, aim-stick rally and release return") {
  val w=world(TankEvolution.OVERSEER);step(w,600);val x=w.players.x[0];val y=w.players.y[0];val d=w.droneWing
  fun distance(i:Int)=hypot(d.x[i]-x,d.y[i]-y)
  check((0..3).all{distance(it) in 48f..85f});inputs[0].fire=true;inputs[0].aimX=1f;step(w,150);check((0..3).all{d.x[it]>x+150f});inputs[0].clear();step(w,150);check((0..3).all{distance(it)<100f})
 }
 test("nearby threats take physical damage; distant threats do not attract idle wing") {
  val w=world(TankEvolution.OVERLORD);val p=w.players;val x=p.x[0];val y=p.y[0];step(w,600)
  val j=w.spawnEnemy(Bestiary.SLAB,x+510f,y);w.enemies.hp[j]=10000f;val hp=w.enemies.hp[j];step(w,20);check(w.enemies.hp[j]==hp);w.enemies.clear()
  w.spawnEnemy(Bestiary.SLAB,x+125f,y);step(w,180);check(p.dealt[0]>0f);check(w.bullets.count==0)
 }
 test("host owns remote drone damage, rejects phantom bullet hit") {
  val w=world(TankEvolution.BASIC,2);w.players.evolution[1]=TankEvolution.OVERSEER;w.remote[1]=true;val x=w.players.x[1];val y=w.players.y[1];val j=w.spawnEnemy(Bestiary.SLAB,x+110f,y);val old=w.enemies.hp[j];w.remoteHit(1,w.enemies.id[j] and 65535,1f,0f);check(w.enemies.hp[j]==old);step(w,300);check(w.players.dealt[1]>0f);check(w.bullets.count==0)
 }
 test("snapshot roundtrip with 64 units and disappearance") {
  val w=world(TankEvolution.MOTHERSHIP,MAX_PLAYERS);for(p in 0 until MAX_PLAYERS)w.players.evolution[p]=TankEvolution.MOTHERSHIP;step(w,900);val s=Snapshot();val buf=Wire.buffer(MAX_MESSAGE*2)
  fun roundtrip(){Protocol.writeSnapshot(buf,w,false,false,0,Events(),Events(),Events(),0,Echo());Protocol.readSnapshot(Wire.open(Wire.finish(buf)),s)}
  roundtrip();check(s.droneHp.count{it>0}==64);check(s.droneHp.contentEquals(w.droneWing.hp));check(abs(s.droneX[63]-w.droneWing.x[63])<1f);w.removePlayer(7);roundtrip();check(s.droneHp.count{it>0}==56)
 }
 test("hostile interception does not charge remote player for intercepted shot") {
  val w=world(TankEvolution.OVERSEER);w.practice=false;step(w,100);val d=w.droneWing;val hp=d.hp[0];check(d.intercept(d.x[0],d.y[0],5f));check(d.hp[0]==hp-1);d.markIntercepted(23,w.tick);val playerHp=w.players.hp[0];w.remoteHurt(0,23);check(w.players.hp[0]==playerHp)
 }
 test("shop filtering and checkpoint compatible") {val f=TankEvolution.OVERSEER;check(!Upgrades.offered(Upgrades.PIERCE,f));check(!Upgrades.offered(Upgrades.CALIBER,f));check(Upgrades.offered(Upgrades.FIRE_RATE,f));val w=world(f);val cp=Checkpoint.read(w.capture().write())!!;check(cp.evolution[0]==f);w.restore(cp,1){true};check(w.droneWing.count(0)==0)}
 test("camera scale and arena boundaries") {val v=Viewport();v.fit(1600,720);val base=v.scale;v.follow(1600,720,1.60f,960f,432f);check(abs(v.scale/base-1.6f)<.001f);check(abs(v.screenX(960f)-800f)<.01f);v.follow(1600,720,1.85f,0f,0f);check(v.left==0f&&v.top==0f);v.follow(1600,720,1.85f,ARENA_W,ARENA_H);check(abs(v.right-1600f)<.01f&&abs(v.bottom-720f)<.01f)}
 println("$passed/10 physical drone and camera checks passed")
}
