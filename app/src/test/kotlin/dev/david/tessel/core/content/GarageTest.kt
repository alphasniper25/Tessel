package dev.david.tessel.core.content

import dev.david.tessel.core.net.*
import dev.david.tessel.core.sim.*
import dev.david.tessel.core.stats.Totals
import dev.david.tessel.platform.game.Viewport
import org.junit.Assert.*
import org.junit.Test

class GarageTest {
    @Test fun purchasesAreOnlyCosmeticAndCannotOverspend() {
        val t = Totals().apply { completed = 1 }
        val initial = GarageCatalog.defaultMask
        val purchased = GarageCatalog.purchase(t,initial,8)
        assertEquals(75L,GarageCatalog.balance(t,purchased))
        assertEquals(purchased,GarageCatalog.purchase(t,purchased,8))
        assertEquals(purchased,GarageCatalog.purchase(t,purchased,14))
        val w = World(); val damage=w.players.damage(0); val speed=w.players.moveSpeed(0); val hp=w.players.maxHp[0]
        w.players.appearance[0]=TankLook().equip(8,purchased)
        assertEquals(damage,w.players.damage(0),0f); assertEquals(speed,w.players.moveSpeed(0),0f); assertEquals(hp,w.players.maxHp[0])
    }
    @Test fun compactAppearanceRoundTripsAllCatalogCombinations() {
        for (body in GarageCatalog.bySlot[0]) for (turret in GarageCatalog.bySlot[1]) for (paint in GarageCatalog.bySlot[2]) for(skin in GarageCatalog.bySlot[3]) {
            val look=TankLook(body.id,turret.id,paint.id,skin.id)
            assertEquals(look,TankLook.fromCode(look.code()))
        }
        assertEquals(TankLook(),TankLook.fromCode(0xffff))
    }
    @Test fun lobbyCheckpointAndSnapshotPreserveAppearance() {
        val look = TankLook(2,6,9,14)
        val choice=LobbyChoice(CharacterDef.RAM,0,true,look)
        assertEquals(9 to choice,Wire.readLobbyChoice(Wire.lobbyChoice(9,choice)))
        val w=World().apply { reset(players=2); players.chooseCharacter(1,CharacterDef.RAM,difficulty.bonusHp); players.appearance[1]=look }
        val cp=Checkpoint.read(w.capture().write())!!
        assertEquals(look,cp.appearance[1])
        val buf=Wire.buffer(MAX_MESSAGE*2); Protocol.writeSnapshot(buf,w,false,false,0,Events(),Events(),Events(),1,Echo())
        val snapshot=Snapshot(); Protocol.readSnapshot(Wire.open(Wire.finish(buf)),snapshot)
        assertEquals(CharacterDef.RAM,snapshot.character[1]); assertEquals(look,snapshot.appearance[1])
    }
    @Test fun cameraFollowsWithoutPanningBeyondArena() {
        val v=Viewport(); v.follow(1600,720,1.25f,800f,360f)
        assertEquals(800f,v.screenX(800f),.001f); assertEquals(360f,v.screenY(360f),.001f)
        v.follow(1600,720,1.25f,0f,0f); assertEquals(0f,v.left,.001f); assertEquals(0f,v.top,.001f)
        v.follow(1600,720,1.25f,1600f,720f);assertEquals(1600f,v.right,.001f);assertEquals(720f,v.bottom,.001f)
        v.follow(1600,720,1f,400f,300f); assertEquals(1f,v.scale,.001f);assertEquals(0f,v.left,.001f)
    }
}
