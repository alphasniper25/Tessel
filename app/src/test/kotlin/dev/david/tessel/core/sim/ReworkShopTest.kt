package dev.david.tessel.core.sim
import dev.david.tessel.core.content.*
import org.junit.Assert.*
import org.junit.Test

class ReworkShopTest{
    @Test fun allModernUpgradeIdsAppearExactlyOnceAndNoLegacyNodes(){
        val ids=UpgradeMap.nodes.map{it.id}
        assertEquals(ids.toSet().size,ids.size)
        assertEquals((0 until Upgrades.count).filterNot{UpgradeMap.legacy(it)}.toSet(),ids.toSet())
        assertTrue(UpgradeMap.nodes.all{kotlin.math.abs(it.x)<UpgradeMap.WIDTH/2&&kotlin.math.abs(it.y)<UpgradeMap.HEIGHT/2})
    }
    @Test fun forcedShopUsesSameStockRulesAndDoesNotAwardFakeCoins(){
        val w=World(seed=27L)
        w.skipTo(17);w.players.coins[0]=5000
        w.devOpenShop()
        assertEquals(Phase.SHOP,w.phase);assertEquals(17,w.stage);assertEquals(5000,w.players.coins[0]);assertEquals(0,w.players.earned[0])
        assertEquals(3,(0 until SHOP_SLOTS).map{w.shop.item(0,it)}.toSet().size)
        assertTrue(w.shop.devStock(w,Upgrades.DAMAGE,0,CardRarity.RARE))
        assertEquals(Upgrades.DAMAGE,w.shop.item(0,0))
        val price=w.shop.price(w,0,0)
        assertTrue(w.shop.setLocked(w,0,0,true))
        assertTrue(w.shop.reroll(w,0))
        assertEquals(price,w.shop.price(w,0,0))
        val before=w.players.coins[0]
        assertTrue(w.shop.buy(w,0,0));assertEquals(3,w.players.levels[0][Upgrades.DAMAGE])
        assertEquals(before-price,w.players.coins[0])
        assertFalse(w.shop.buy(w,0,0))
        assertFalse(w.shop.devStock(w,Upgrades.MULTISHOT,0,CardRarity.COMMON))
    }
    @Test fun trialSettersApplyHealthDeltasAndClearWithoutDoubleCounting(){
        val w=World(seed=2L);val hp=w.players.maxHp[0]
        repeat(2){w.devSetUpgrade(Upgrades.MAX_HEALTH,3)}
        assertEquals(hp+3,w.players.maxHp[0])
        w.devSetUpgrade(Upgrades.MAX_HEALTH,1);assertEquals(hp+1,w.players.maxHp[0])
        repeat(2){w.devSetPerk(Perks.THICK_SKIN,true)}
        assertEquals(hp+3,w.players.maxHp[0])
        w.devSetPerk(Perks.THICK_SKIN,false)
        w.devSetUpgrade(Upgrades.MAX_HEALTH,0)
        val id=SkillGraph.nodes.first{it.bonus==SkillBonus.HEALTH}.id
        w.devSetSkills(SkillGraph.bit(id));assertEquals(hp+SkillGraph.nodes[id].amount,w.players.maxHp[0])
        w.devSetSkills(0L);assertEquals(hp,w.players.maxHp[0])
        w.devSetFrame(TankEvolution.DREADNOUGHT);assertEquals(TankEvolution.DREADNOUGHT,w.players.evolution[0])
        w.devSetFrame(TankEvolution.RAMMER);assertEquals(hp+TankEvolution.RAMMER.hullBonus,w.players.maxHp[0])
        w.devSetFrame(TankEvolution.BASIC);assertEquals(hp,w.players.maxHp[0])
    }
    @Test fun comboEffectsRequireBothIngredientsAndAreDerivedFromOwnership(){
        val p=PlayerState().apply{reset(1)}
        for(combo in PerkCombo.entries){
            assertFalse(combo.active(0));assertFalse(combo.active(1 shl combo.first));assertFalse(combo.active(1 shl combo.second));assertTrue(combo.active(combo.bits))
        }
        p.hp[0]=2;p.perks[0]=1 shl Perks.LAST_STAND
        val last=p.damage(0);p.perks[0]=PerkCombo.PHOENIX.bits;assertEquals(last*1.2f,p.damage(0),.001f)
        p.hp[0]=3;val high=p.damage(0);p.perks[0]=1 shl Perks.LAST_STAND;assertEquals(high,p.damage(0),.001f)
        p.sinceHit[0]=10000;p.perks[0]=1 shl Perks.OVERCLOCK
        val fire=p.fireRate(0);p.perks[0]=PerkCombo.SIEGE.bits;assertEquals(fire*.85f*1.1f,p.fireRate(0),.001f)
        p.perks[0]=1 shl Perks.MAGNET_CORE;val magnet=p.magnet(0)
        p.perks[0]=PerkCombo.SALVAGE.bits;assertEquals(magnet*1.20f,p.magnet(0),.001f)
        val w=World(seed=7L);val heal=w.shop.healAmount(w,0)
        w.players.perks[0]=PerkCombo.MEDIC.bits;assertEquals(heal+1,w.shop.healAmount(w,0))
        w.players.perks[0]=1 shl Perks.HAGGLER;assertEquals(heal,w.shop.healAmount(w,0))
    }
    @Test fun comboOwnershipPersistsWithoutNewCheckpointFields(){
        val w=World(seed=5L);w.players.perks[0]=PerkCombo.PHOENIX.bits
        val cp=Checkpoint.read(w.capture().write())!!
        assertEquals(8,Checkpoint.FORMAT)
        assertTrue(PerkCombo.PHOENIX.active(cp.perks[0]))
    }
}
