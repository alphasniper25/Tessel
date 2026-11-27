import dev.david.tessel.core.content.*
import dev.david.tessel.core.stats.*
import dev.david.tessel.core.net.*
import dev.david.tessel.core.sim.*
import kotlin.math.abs
private var checks = 0
private fun pass(label: String, test: () -> Unit) { test(); checks++; println("PASS $checks: $label") }
private fun world(mask: Int = 0): World = World(seed = 127).also { it.players.missionMask[0] = mask; it.players.coins[0] = 100000000 }
fun main() {
    pass("new profile starts at level 1 with all mission cards locked") {
        val t = Totals(); check(CardMission.mask(t)==0); check(AccountProgress.xp(t)==0L); check(AccountProgress.level(0)==1)
    }
    val totals = Totals().apply { coins=500; deepest=15; bosses=20; completed=1; seconds=600; kills=100 }
    pass("four independent mission thresholds and XP curve") {
        check(CardMission.mask(totals)==CardMission.allMask); check(AccountProgress.xp(totals)==1760L)
        check(AccountProgress.level(99)==1); check(AccountProgress.level(100)==2); check(AccountProgress.level(300)==3)
        val almost = Totals().apply { coins=499; deepest=14; bosses=19 }; check(CardMission.mask(almost)==0)
        val individual = Totals().apply { coins=500 }; check(CardMission.mask(individual)==CardMission.FIELD_MEDIC.bit)
    }
    pass("ledger roundtrip retains progression and unlocks") {
        val l=Ledger(); l.record(RunEntry(1,Difficulty.NORMAL,15,600,1000,100,500,20,1,-1,false,10,20,0,0,0,2,true))
        val loaded=checkNotNull(Ledger.read(l.write())); check(CardMission.mask(loaded.totals)==CardMission.allMask); check(AccountProgress.xp(loaded.totals)==1760L)
    }
    pass("locked mission cards never appear in 30,000 slots") {
        val w=world(); repeat(10000) { w.shop.open(w); for (s in 0 until SHOP_SLOTS) check(CardMission.forUpgrade(w.shop.item(0,s))==null) }
    }
    pass("unlocked mission cards appear only at their declared rarity") {
        val w=world(CardMission.allMask); val seen=mutableSetOf<Int>()
        repeat(30000) { w.shop.open(w); for(s in 0 until SHOP_SLOTS) CardMission.forUpgrade(w.shop.item(0,s))?.let { check(w.shop.rarity(0,s)==it.rarity); seen+=it.upgrade } }; check(seen.size==CardMission.entries.size)
    }
    pass("partially unlocked player only draws eligible mission cards") {
        val w=world(CardMission.FIELD_MEDIC.bit); var seen=false
        repeat(10000) { w.shop.open(w); for(s in 0 until SHOP_SLOTS) CardMission.forUpgrade(w.shop.item(0,s))?.let { check(it==CardMission.FIELD_MEDIC); seen=true } }; check(seen)
    }
    pass("host keeps separate card pools per player") {
        val w=World(seed=23); w.reset(2,23); w.players.missionMask[1]=CardMission.allMask; var found=false
        repeat(20000) { w.shop.open(w); for(s in 0 until SHOP_SLOTS) { check(CardMission.forUpgrade(w.shop.item(0,s))==null); if(CardMission.forUpgrade(w.shop.item(1,s))!=null) found=true } }; check(found)
    }
    pass("mission card purchases apply actual effects once and cost coins") {
        val w=world(CardMission.allMask); val s=w.shop
        for (m in CardMission.entries) {
            var slot=-1
            var attempts=0
            while(slot<0 && attempts++<200000) { s.open(w); slot=(0 until SHOP_SLOTS).firstOrNull { s.item(0,it)==m.upgrade } ?: -1 }; check(slot>=0)
            val before=w.players.coins[0]; val price=s.price(w,0,slot)
            val hp=w.players.maxHp[0]; val dmg=w.players.damage(0); val speed=w.players.moveSpeed(0); val ram=w.players.ramDamage(0); val heal=s.healAmount(w,0)
            check(s.buy(w,0,slot)); check(!s.buy(w,0,slot)); check(w.players.coins[0]==before-price); check(w.players.levels[0][m.upgrade]==1)
            when(m) {
                CardMission.FIELD_MEDIC -> check(s.healAmount(w,0)==heal+2)
                CardMission.AFTERBURNER -> check(abs(w.players.moveSpeed(0)-speed*1.18f)<0.001f)
                CardMission.TITAN_PLATING -> { check(w.players.maxHp[0]==hp+4); check(w.players.ramDamage(0)==ram+3f) }
                CardMission.CORE_BREAKER -> check(abs(w.players.damage(0)-dmg*1.8f)<0.001f)
            }
        }
    }
    pass("checkpoint keeps per-player unlock mask and starting character") {
        val w=world(CardMission.allMask); w.players.chooseCharacter(0,CharacterDef.RAM,w.difficulty.bonusHp)
        val cp=checkNotNull(Checkpoint.read(w.capture().write())); val restored=World(seed=9); restored.restore(cp,9){true}
        check(restored.players.missionMask[0]==CardMission.allMask); check(restored.players.character[0]==CharacterDef.RAM)
    }
    pass("lobby requires host and every joined player, supports 8 players") {
        val lobby=LobbyReady(); lobby.reset(); val seats=(0 until MAX_PLAYERS).toList(); check(!lobby.allReady(listOf(0)))
        val epoch=lobby.revision; for(p in seats.dropLast(1)) check(lobby.set(p,epoch,LobbyChoice(ready=true)))
        check(!lobby.allReady(seats)); check(lobby.set(seats.last(),epoch,LobbyChoice(ready=true))); check(lobby.allReady(seats))
        lobby.set(0,epoch,LobbyChoice(ready=false)); check(!lobby.allReady(seats))
    }
    pass("membership change invalidates every ready flag and rejects stale packets") {
        val lobby=LobbyReady(); val epoch=lobby.revision; lobby.set(0,epoch,LobbyChoice(CharacterDef.RAM,3,true)); lobby.set(3,epoch,LobbyChoice(CharacterDef.VANGUARD,1,true)); check(lobby.allReady(listOf(0,3)))
        lobby.rosterChanged(); check(!lobby.allReady(listOf(0,3))); check(!lobby.set(3,epoch,LobbyChoice(ready=true))); check(lobby.get(0).character==CharacterDef.RAM); check(lobby.get(3).missionMask==1)
    }
    pass("wire roundtrip preserves character, unlocks, readiness and recipient seat; rejects corrupt messages") {
        val lobby=LobbyReady(); val epoch=lobby.revision; val choice=LobbyChoice(CharacterDef.STRIKER,CardMission.allMask,true); lobby.set(5,epoch,choice)
        val bytes=Wire.lobbyChoice(epoch,choice); check(Wire.readLobbyChoice(bytes)==(epoch to choice)); check(Wire.readLobbyChoice(bytes.copyOf(13))==null)
        val status=lobby.status(listOf(0,5),5); val encoded=Wire.lobbyStatus(status); check(Wire.readLobbyStatus(encoded)==status); check(status.self==1); check(Wire.readLobbyStatus(encoded.copyOf(9))==null)
    }
    pass("shop ready and rematch actions cannot authorize the wrong phase; rematch requires both players") {
        val w=World(seed=19); w.reset(2,19); val h=HostSession(w){_,_,_->true}; h.connect(1)
        h.receive(1,Wire.action(Wire.RESTART,15),0); check(h.ready==0)
        h.receive(1,Wire.action(Wire.READY),0); check(h.ready==0)
        w.adopt(1,1,Phase.OVER,1,2,Difficulty.NORMAL,-1,0f)
        h.receive(1,Wire.action(Wire.READY),0); check(h.ready==0)
        h.receive(1,Wire.action(Wire.RESTART,1),0); check(!h.allRematchReady()); check(h.rematchMasks[1]==1)
        h.markRematchReady(0,15); check(h.allRematchReady()); check(h.rematchMasks[0]==15)
        h.disconnect(1); check(!h.allRematchReady()); h.connect(1); check(!h.allRematchReady())
    }
    pass("final uncapped mission card remains available even when its rarity was not rolled") {
        val w=world(CardMission.CORE_BREAKER.bit); for(u in 0 until Upgrades.count) w.players.levels[0][u]=Upgrades.of(u).maxLevel
        w.players.levels[0][Upgrades.CORE_BREAKER]=0
        repeat(30) { w.shop.open(w); check(w.shop.item(0,0)==Upgrades.CORE_BREAKER); check(w.shop.rarity(0,0)==CardRarity.LEGENDARY); check(w.shop.item(0,1)==-1) }
    }
    println("$checks targeted checks passed")
}
