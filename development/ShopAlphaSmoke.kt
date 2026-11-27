import dev.david.tessel.core.content.*
import dev.david.tessel.core.sim.*
import dev.david.tessel.core.net.*
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs

private var checks = 0
private fun pass(label: String, test: () -> Unit) { test(); checks++; println("PASS $checks: $label") }
private fun shopWorld(count: Int = 1): World {
    val w = World(seed = 127)
    w.reset(count, 127)
    w.spawning = false
    toShop(w)
    return w
}
private fun toShop(w: World) {
    val idle = Array(MAX_PLAYERS) { PlayerInput() }
    var limit = 100 * TICK_HZ
    while(w.phase != Phase.SHOP && limit-- > 0) w.step(idle)
    check(w.phase == Phase.SHOP)
}
private fun snapshot(w: World, player: Int): Snapshot {
    val b = Wire.buffer(MAX_MESSAGE * 2)
    Protocol.writeSnapshot(b,w,false,false,0,Events(),Events(),Events(),player,Echo())
    return Snapshot().also { Protocol.readSnapshot(Wire.open(Wire.finish(b)),it) }
}
private fun oldSave(): ByteArray {
    // Real format-3 layout: original eleven upgrade IDs, five slots, one-byte counters.
    val b = ByteBuffer.allocate(200).order(ByteOrder.LITTLE_ENDIAN)
    b.put('T'.code.toByte()); b.put('C'.code.toByte()); b.put(3); b.put(Difficulty.NORMAL.ordinal.toByte())
    b.putShort(4); b.putInt(234); b.put(1); b.put(1); b.putShort(0); b.put(11); b.put(5)
    b.put(1); b.putLong(12); b.put(0); b.putShort(5); b.putShort(6)
    b.putInt(123); b.putInt(200); b.putInt(7); b.putInt(70); b.putInt(30); b.putFloat(14f)
    b.putInt(1); b.putInt(0); b.putInt(0)
    repeat(11) { b.put(if (it == Upgrades.DAMAGE) 2.toByte() else 0.toByte()) }
    for (v in intArrayOf(Upgrades.SPEED or 0x40, Upgrades.MAX_HEALTH, Upgrades.DAMAGE, Upgrades.VELOCITY, Upgrades.RANGE)) b.put(v.toByte())
    b.put(2); b.put(0); b.putInt(0); repeat(Perks.OFFERED) { b.put((-1).toByte()) }
    return b.array().copyOf(b.position())
}
fun main() {
    val w=shopWorld(2); val s=w.shop
    pass("exactly three unique offers") { check(SHOP_SLOTS == 3); check((0 until SHOP_SLOTS).map { s.item(0,it) }.toSet().size == 3) }
    val raw=s.raw(0,2); val quote=s.price(w,0,2)
    pass("lock unaffordable card without spending; cannot buy without coins") {
        w.players.coins[0]=0; check(s.setLocked(w,0,2,true)); check(!s.buy(w,0,2)); check(w.players.coins[0]==0)
        check(!s.setLocked(w,0,2,true)); check(!s.setLocked(w,0,-1,true))
    }
    pass("reroll pays once and preserves locked raw card and price without duplicates") {
        w.players.coins[0]=100000; val cost=s.rerollPrice(w,0); check(s.reroll(w,0)); check(w.players.coins[0]==100000-cost)
        check(s.raw(0,2)==raw && s.price(w,0,2)==quote && s.locked(0,2)); check((0..2).map{s.item(0,it)}.toSet().size==3)
    }
    pass("purchase grants correct levels once and empties slot") {
        val u=s.item(0,0); val n=s.levelsBought(w,0,0); val p=s.price(w,0,0); val c=w.players.coins[0]
        check(s.buy(w,0,0)); check(!s.buy(w,0,0)); check(w.players.levels[0][u]==n); check(w.players.coins[0]==c-p); check(s.item(0,0)==-1)
    }
    val cp=checkNotNull(Checkpoint.read(w.capture().write())); val restored=World(seed=999)
    pass("checkpoint round trip keeps locked price and purchased empty slot") {
        restored.restore(cp,999){true}; check(restored.shop.raw(0,2)==raw); check(restored.shop.locked(0,2)); check(restored.shop.price(restored,0,2)==quote); check(restored.shop.item(0,0)==-1)
    }
    pass("locked quote persists into next shop") {
        restored.spawning=false; restored.nextStage(); toShop(restored); check(restored.shop.raw(0,2)==raw); check(restored.shop.price(restored,0,2)==quote)
    }
    pass("new run clears prior locks") { restored.reset(); check(!restored.shop.locked(0,2)); check(restored.shop.item(0,2)==-1) }
    pass("all three locked disables reroll") { for(i in 0..2) if(!s.locked(1,i)) check(s.setLocked(w,1,i,true)); w.players.coins[1]=10000; check(!s.reroll(w,1)); check(w.players.coins[1]==10000) }
    pass("network snapshot preserves prices, levels, rarity, and locks for recipient") {
        val ss=snapshot(w,1); check(ss.hasShop); for(i in 0..2){check(ss.shopItem[i]==s.item(1,i));check(ss.shopRarity[i]==s.rarity(1,i));check(ss.shopLevels[i]==s.levelsBought(w,1,i));check(ss.shopPrice[i]==s.price(w,1,i));check(ss.shopLocked[i]==s.locked(1,i))}
    }
    pass("host rejects invalid actions, honors unlock and purchase only in shop, freezes ready players") {
        val h=HostSession(w){_,_,_->true}; h.connect(1); var changes=0; h.onShopChanged={changes++}; h.receive(1,Wire.action(Wire.BUY,-1),0); check(s.locked(1,0)); h.receive(1,Wire.action(Wire.UNLOCK_CARD,0),0); check(!s.locked(1,0));check(changes==1); h.markReady(1); h.receive(1,Wire.action(Wire.LOCK_CARD,0),0); check(!s.locked(1,0));
        w.nextStage(); val c=w.players.coins[1]; h.receive(1,Wire.action(Wire.BUY,1),0); check(w.players.coins[1]==c)
    }
    pass("old format-3 save migrates rare card, coins, levels, and balanced default") { val old=checkNotNull(Checkpoint.read(oldSave())); check(old.coins[0]==123);check(old.levels[0][Upgrades.DAMAGE]==2); check(CardCode.item(old.stock[0][0])==Upgrades.SPEED);check(CardCode.tier(old.stock[0][0])==CardRarity.RARE);check(old.character[0]==CharacterDef.BALANCED);check(old.locked[0].none{it}) }
    pass("character stats and checkpoint identity") {
        val c=World(seed=21); c.players.chooseCharacter(0,CharacterDef.RAM,c.difficulty.bonusHp);check(abs(c.players.moveSpeed(0)-Balance.PLAYER_SPEED*1.2f)<0.001f);check(abs(c.players.damage(0)-0.85f)<0.001f);check(c.players.ramDamage(0)==2.5f);check(Checkpoint.read(c.capture().write())!!.character[0]==CharacterDef.RAM)
    }
    pass("Ram contact hits enemy and still costs the player health") {
        val r=World(seed=33);r.spawning=false;r.players.chooseCharacter(0,CharacterDef.RAM,r.difficulty.bonusHp)
        val hp=r.players.hp[0]; val j=r.spawnEnemy(Bestiary.DOT,r.players.x[0]+1f,r.players.y[0]);check(j>=0)
        val before=r.enemies.hp[j];repeat(5){r.step(Array(MAX_PLAYERS){PlayerInput()})}
        check(r.players.hp[0]<hp);check(r.enemies.count==0 || r.enemies.hp[j]<before)
    }
    pass("heal power increases paid healing without exceeding max HP") { val h=shopWorld();h.players.hp[0]=1;h.players.coins[0]=1000;h.players.levels[0][Upgrades.HEAL_POWER]=2;check(h.shop.healAmount(h,0)==4);check(h.shop.heal(h,0));check(h.players.hp[0]==5) }
    pass("all sold slots remain empty on restore") { val x=shopWorld();x.players.coins[0]=100000;for(i in 0..2)check(x.shop.buy(x,0,i));val y=World(seed=1);y.restore(Checkpoint.read(x.capture().write())!!,2){true};check((0..2).all{y.shop.item(0,it)==-1}) }
    pass("deterministic weighted sampling; rarity independent of character (20,000 first slots each)") {
        val counts=mutableMapOf<CharacterDef,IntArray>()
        for (c in CharacterDef.entries) {
            val x=World(seed=71); x.players.chooseCharacter(0,c,x.difficulty.bonusHp)
            val fam=IntArray(CardFamily.entries.size);val rar=IntArray(CardRarity.entries.size)
            repeat(20000){ x.shop.open(x);fam[Upgrades.of(x.shop.item(0,0)).family.ordinal]++;rar[x.shop.rarity(0,0).ordinal]++ }
            counts[c]=fam
            val expected=CardRarity.entries.map{it.weight/100.0}
            for(i in rar.indices)check(abs(rar[i]/20000.0-expected[i])<0.015)
            println("  ${c.label}: families="+fam.joinToString { "%.1f%%".format(it/200.0) }+"; rarity="+rar.joinToString { "%.1f%%".format(it/200.0) })
        }
        val baseline=counts.getValue(CharacterDef.BALANCED)
        for((c,f) in listOf(CharacterDef.VANGUARD to CardFamily.HEALTH,CharacterDef.VANGUARD to CardFamily.RAM,CharacterDef.RAM to CardFamily.RAM,CharacterDef.RAM to CardFamily.MOBILITY,CharacterDef.STRIKER to CardFamily.GUN)) check(counts.getValue(c)[f.ordinal]>baseline[f.ordinal]*1.1)
    }
    println("$checks targeted checks passed")
}
