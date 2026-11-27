import dev.david.tessel.core.content.*
import dev.david.tessel.core.stats.Totals
import dev.david.tessel.core.net.*
import dev.david.tessel.core.sim.*
import dev.david.tessel.platform.game.Viewport
import kotlin.math.abs
fun main() {
    var count=0
    fun pass(s:String, check:()->Unit){check();println("PASS ${++count}: $s")}
    val totals=Totals(); val initial=GarageCatalog.defaultMask
    pass("starter parts, no unaffordable purchase") { check(GarageCatalog.items.filter{it.cost==0}.all{GarageCatalog.owned(initial,it.id)});check(GarageCatalog.purchase(totals,initial,8)==initial);check(TankLook().equip(8,initial)==TankLook()) }
    totals.seconds=900;totals.kills=100;totals.bosses=2;totals.completed=1
    val bought=GarageCatalog.purchase(totals,initial,8)
    pass("credits derive from play, purchase once and no negative balance") { check(GarageCatalog.earned(totals)==170L);check(GarageCatalog.balance(totals,bought)==145L);check(GarageCatalog.purchase(totals,bought,8)==bought);check(GarageCatalog.purchase(totals,bought,99)==bought) }
    pass("equipment save validation") { val look=TankLook().equip(8,bought);check(look.paint==8);check(TankLook.decode(look.encode(),bought)==look);check(TankLook.decode("1,6,99,14",initial)==TankLook());check(GarageCatalog.classShape(CharacterDef.RAM)==HullVisual.DIAMOND) }
    pass("zoom clamp, full arena restore and screen-space center") { val v=Viewport();v.fit(1600,720);check(v.scale==1f);v.follow(1600,720,1.25f,800f,360f);check(abs(v.screenX(800f)-800f)<.001f&&abs(v.screenY(360f)-360f)<.001f);v.follow(1600,720,1.25f,0f,0f);check(v.left==0f&&v.top==0f);v.follow(1600,720,1.25f,1600f,720f);check(abs(v.right-1600f)<.001f&&abs(v.bottom-720f)<.001f);v.follow(1600,720,1f,0f,0f);check(v.left==0f&&v.scale==1f) }
    pass("selected class survives snapshot and client view") {val w=World().apply {reset(players=2);players.chooseCharacter(1,CharacterDef.RAM,difficulty.bonusHp)}; val c=ClientSession(1,2){_,_->true};val h=HostSession(w){_,bytes,_->c.receive(bytes,1_000_000_000L);true};h.connect(1);h.pump(1_000_000_000L,false,false);c.frame(1_000_000_000L);check(c.latest!!.character[1]==CharacterDef.RAM);check(c.view.players.character[1]==CharacterDef.RAM);h.close();c.close()}
    pass("compact cosmetic codes and unrecognized parts fall back") {
        val look = TankLook(2,6,9,14); check(TankLook.fromCode(look.code())==look); check(TankLook.fromCode(0)==TankLook());check(TankLook.fromCode(0xffff)==TankLook())
    }
    pass("appearance persists through checkpoints and lobby choice") {
        val look=TankLook(2,6,9,14); val choice=LobbyChoice(CharacterDef.RAM,0,true,look);check(Wire.readLobbyChoice(Wire.lobbyChoice(8,choice))==8 to choice)
        val w=World().apply {reset(players=2); players.appearance[1]=look};val cp=checkNotNull(Checkpoint.read(w.capture().write()));check(cp.appearance[1]==look); val copy=World();copy.restore(cp,1){true};check(copy.players.appearance[1]==look)
        // Version 5 records are identical except for the appearance word after missionMask.
        val one=World();val old=one.capture().write();val nameLength=old[25].toInt() and 255;val start=26+nameLength;val offset=start+4+36+Upgrades.count+SHOP_SLOTS*2+4+1+4+Perks.OFFERED+1+4
        val legacy=old.copyOfRange(0,offset)+old.copyOfRange(offset+2,old.size);legacy[2]=5;check(Checkpoint.read(legacy)?.appearance?.get(0)==TankLook())
    }
    pass("custom appearance is transmitted and copied to client world") {
        val look=TankLook(2,6,9,14); val w=World().apply{reset(players=2); players.appearance[1]=look};val c=ClientSession(1,2){_,_->true};val h=HostSession(w){_,bytes,_->c.receive(bytes,1_000_000_000L);true};h.connect(1);h.pump(1_000_000_000L,false,false);c.frame(1_000_000_000L);check(c.latest!!.appearance[1]==look);check(c.view.players.appearance[1]==look);h.close();c.close()
    }
    pass("shop count exactly three") {check(SHOP_SLOTS==3)}
    println("$count garage checks passed")
}
