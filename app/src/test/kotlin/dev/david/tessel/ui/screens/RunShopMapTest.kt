package dev.david.tessel.ui.screens

import androidx.activity.ComponentActivity
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import dev.david.tessel.core.content.*
import dev.david.tessel.platform.game.*
import dev.david.tessel.ui.theme.TesselTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35], qualifiers="w914dp-h411dp-land-night-420dpi")
class RunShopMapTest{
    @get:Rule val compose=createAndroidComposeRule<ComponentActivity>()
    private fun offer(id:Int,slot:Int)=Upgrades.of(id).let{
        ShopOffer(slot,it.name,it.blurb,0,Upgrades.runCap(id),10,false,true,upgradeId=id)
    }
    private fun view(dev:Boolean=false,reward:Boolean=false)=ShopView(17,418,9,12,
        listOf(offer(Upgrades.DAMAGE,0),offer(Upgrades.MAX_HEALTH,1),offer(Upgrades.WEALTH,2)),25,2,true,9,true,
        perkOffers=if(reward)listOf(PerkOffer(0,Perks.of(Perks.SECOND_WIND).name,Perks.of(Perks.SECOND_WIND).blurb,id=Perks.SECOND_WIND))else emptyList(),
        devPreview=dev,debugLevels=if(dev)List(Upgrades.count){0}else emptyList())
    private var bought=-1
    private var chosen=-1
    private var granted=-1
    private var rank=-1
    private fun show(dev:Boolean=false,reward:Boolean=false){
        compose.setContent{TesselTheme{RunShopScreen(view(dev,reward),false,{bought=it},{},{},{},{chosen=it},{_,_->},{},{},{},{id,_->rank=id},{id,_->granted=id},{_,_->})}}
        compose.waitForIdle()
    }
    private fun tap(id:Int,dev:Boolean=false){
        val n=UpgradeMap.positions(dev).first{it.id==id}
        compose.onNodeWithTag("upgrade-map").performTouchInput{
            val cam=MarketCamera()
            click(Offset(cam.sx(n.x,width.toFloat(),height.toFloat()),cam.sy(n.y,width.toFloat(),height.toFloat())))
        }
        compose.waitForIdle()
    }
    @Test fun onlyRolledUpgradesCanBeInspectedAndPurchased(){
        show()
        compose.onNodeWithTag("upgrade-map").assertExists()
        compose.onNodeWithText("Damage").assertDoesNotExist()
        tap(Upgrades.FIRE_RATE)
        compose.onNodeWithText("Fire Rate").assertDoesNotExist()
        tap(Upgrades.DAMAGE)
        compose.onNodeWithText("Damage").assertExists()
        compose.onNodeWithText("Buy 10").performClick()
        assertEquals(0,bought)
        compose.onNodeWithText("×").performClick()
        compose.onNodeWithText("Skills").performClick()
        compose.onNodeWithTag("skill-map").assertExists()
        compose.onNodeWithText("Perks").performClick()
        compose.onNodeWithTag("perk-map").assertExists()
        compose.onNodeWithText("DEV · Grant").assertDoesNotExist()
    }
    @Test fun developerCanInspectAnUnofferedUpgradeAndEditRank(){
        show(dev=true)
        tap(Upgrades.FIRE_RATE,true)
        compose.onNodeWithText("Fire Rate").assertExists()
        compose.onNodeWithText("Max").performClick()
        assertEquals(Upgrades.FIRE_RATE,rank)
        compose.onNodeWithText("Buy 10").assertDoesNotExist()
    }
    @Test fun rewardCardsOnlyAppearOnPerksAndKeepOriginalSlot(){
        show(reward=true)
        compose.onNodeWithText("Second Wind").assertExists()
        compose.onNodeWithText("Take").performClick()
        assertEquals(0,chosen)
        compose.onNodeWithText("Upgrades").performClick()
        compose.onNodeWithTag("upgrade-map").assertExists()
        compose.onNodeWithText("Second Wind").assertDoesNotExist()
        compose.onNodeWithText("Next Stage").assertIsNotEnabled()
    }

    @Test fun repeatedOffersCanBeSelectedBySlotRatherThanType(){
        val stat=dev.david.tessel.core.sim.ShopStatState(
            List(Upgrades.count){if(it==Upgrades.MAGNET)2 else 0},9,12,
            CharacterDef.BALANCED,TankEvolution.BASIC,0,0L,0,false,Difficulty.NORMAL,17,0)
        val a=ShopOffer(0,"Magnet","+25% pickup radius",2,8,26,false,true,levels=1,rarity=CardRarity.COMMON,upgradeId=Upgrades.MAGNET)
        val b=ShopOffer(1,"Magnet","+75% pickup radius",2,8,90,false,true,levels=3,rarity=CardRarity.RARE,upgradeId=Upgrades.MAGNET)
        compose.setContent{TesselTheme{RunShopScreen(ShopView(17,420,9,12,listOf(a,b,offer(Upgrades.DAMAGE,2)),25,2,true,9,true,stats=stat),false,{bought=it},{},{},{},{chosen=it},{_,_->},{},{},{},{_,_->},{_,_->},{_,_->})}}
        compose.waitForIdle()
        tap(Upgrades.MAGNET)
        compose.onNodeWithText("Pickup radius").assertExists()
        compose.onNodeWithText("#2 · Rare").performClick()
        compose.onNodeWithText("Rank 2 → 5 / 8").assertExists()
        compose.onNodeWithText("Buy 90").performClick()
        assertEquals(1,bought)
    }
}
