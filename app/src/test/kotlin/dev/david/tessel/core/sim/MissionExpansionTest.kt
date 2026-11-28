package dev.david.tessel.core.sim
import dev.david.tessel.core.content.*
import dev.david.tessel.core.net.*
import dev.david.tessel.core.stats.*
import org.junit.Assert.*
import org.junit.Test

class MissionExpansionTest{
    @Test fun thresholdsUseDurableTotalsAndStableIds(){
        val t=Totals();assertEquals(0,CardMission.mask(t));assertEquals(8,CardMission.entries.size)
        t.kills=999;t.shots=9999;t.revives=4;t.deepest=24
        assertFalse(CardMission.SCAVENGER.unlocked(t));assertFalse(CardMission.PHASE_CAPACITOR.unlocked(t))
        assertFalse(CardMission.GUARDIAN_KIT.unlocked(t));assertFalse(CardMission.DRONE_RELAY.unlocked(t))
        t.kills++;t.shots++;t.revives++;t.deepest++
        for(m in CardMission.entries.drop(4))assertTrue(m.unlocked(t))
        assertEquals(255,CardMission.allMask);assertEquals(1,CardMission.FIELD_MEDIC.bit);assertEquals(128,CardMission.DRONE_RELAY.bit)
    }
    @Test fun newMissionCardsRespectRarityUnlockAndTankCompatibility(){
        for(m in CardMission.entries.drop(4)){
            assertFalse(CardMission.eligible(m.upgrade,0))
            assertTrue(CardMission.eligible(m.upgrade,m.bit,m.rarity))
            for(tier in CardRarity.entries.filter{it!=m.rarity})assertFalse(CardMission.eligible(m.upgrade,m.bit,tier))
            assertTrue(Upgrades.availableAt(m.upgrade,1))
        }
        assertFalse(Upgrades.offered(Upgrades.DRONE_RELAY,TankEvolution.BASIC))
        assertTrue(Upgrades.offered(Upgrades.DRONE_RELAY,TankEvolution.OVERSEER))
        assertFalse(Upgrades.offered(Upgrades.PHASE_CAPACITOR,TankEvolution.SPIKE))
    }
    @Test fun allEightMissionBitsRoundTripThroughLobbyRematchAndSave(){
        val all=CardMission.allMask
        val choice=LobbyChoice(CharacterDef.BALANCED,all,true)
        assertEquals(1 to choice,Wire.readLobbyChoice(Wire.lobbyChoice(1,choice)))
        val w=World(seed=9L);w.reset(2,9L);w.players.missionMask[1]=all
        val cp=Checkpoint.read(w.capture().write())!!
        assertEquals(all,cp.missionMask[1])
        w.adopt(1,1,Phase.OVER,1,2,Difficulty.NORMAL,-1,0f)
        val host=HostSession(w){_,_,_->true};host.connect(1)
        host.receive(1,Wire.action(Wire.RESTART,all),0L)
        assertEquals(all,host.rematchMasks[1]);host.close()
    }
    @Test fun everyNewCardHasRealStatEffects(){
        val p=PlayerState();p.reset(1)
        val magnet=p.magnet(0);p.levels[0][Upgrades.SCAVENGER]=1
        assertEquals(magnet*1.30f,p.magnet(0),.001f);assertEquals(.10f,p.wealth(0),.001f)
        val fire=p.fireRate(0);val range=p.range(0);p.levels[0][Upgrades.PHASE_CAPACITOR]=1
        assertEquals(fire*1.12f,p.fireRate(0),.001f);assertEquals(range*1.10f,p.range(0),.001f)
        p.evolution[0]=TankEvolution.OVERSEER;val damage=p.damage(0);p.levels[0][Upgrades.DRONE_RELAY]=1
        assertEquals(damage*1.25f,p.damage(0),.001f)
        assertEquals(2f,Upgrades.amount(Upgrades.GUARDIAN_KIT,UpgradeStat.MAX_HP),0f)
        assertEquals(1f,Upgrades.amount(Upgrades.GUARDIAN_KIT,UpgradeStat.HEAL),0f)
    }
}
