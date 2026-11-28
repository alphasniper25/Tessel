package dev.david.tessel

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import dev.david.tessel.core.content.*
import dev.david.tessel.core.sim.*
import dev.david.tessel.core.stats.*
import dev.david.tessel.platform.game.*
import dev.david.tessel.platform.net.*
import dev.david.tessel.platform.store.*
import dev.david.tessel.platform.update.Updater
import dev.david.tessel.ui.components.Sheet
import dev.david.tessel.ui.screens.*
import dev.david.tessel.ui.theme.TesselTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * Pixel captures of production Compose screens using Robolectric native Android
 * graphics. Test data supplies representative in-game states; no re-drawn UI.
 * No assembled APK or device-network service is required.
 */
abstract class MenuGalleryBase {
    @get:Rule val compose=createAndroidComposeRule<ComponentActivity>()
    val out=File(System.getProperty("shots.dir")?:"docs","menu-gallery").apply{mkdirs()}
    val ledger=Ledger().apply{
        record(RunEntry(1700000000000L,Difficulty.NORMAL,17,940,10240,850,4140,3,1,Cause.NONE,false,23000,7500,17,0,0,32))
    }
    fun show(content:@Composable ()->Unit){
        compose.setContent{TesselTheme{Box(Modifier.fillMaxSize()){content()}}}
        compose.mainClock.advanceTimeBy(1200)
        compose.waitForIdle()
    }
    fun capture(name:String,advance:Long=500L){
        compose.mainClock.advanceTimeBy(advance)
        compose.waitForIdle()
        val bmp=compose.onRoot().captureToImage().asAndroidBitmap()
        check(bmp.width>100&&bmp.height>100)
        File(out,"$name.png").outputStream().use{bmp.compress(Bitmap.CompressFormat.PNG,100,it)}
    }
    fun click(label:String){compose.onNodeWithText(label).performClick();compose.waitForIdle()}
    fun scroll(label:String){compose.onNodeWithText(label).performScrollTo();compose.waitForIdle()}
    private lateinit var currentShop:ShopView
    fun tapUpgrade(id:Int,developer:Boolean=false){
        val n=UpgradeMap.positions(developer).first{it.id==id}
        compose.onNodeWithTag("upgrade-map").performTouchInput{
            val cam=MarketCamera(extent=upgradeExtent(currentShop))
            click(Offset(cam.sx(n.x,width.toFloat(),height.toFloat()),cam.sy(n.y,width.toFloat(),height.toFloat())))
        }
        compose.waitForIdle()
    }
    fun tapSkill(id:Int){
        val n=SkillGraph.nodes[id]
        compose.onNodeWithTag("skill-map").performTouchInput{
            val cam=SkillMapCamera()
            click(Offset(cam.sx(n.x,width.toFloat(),height.toFloat()),cam.sy(n.y,width.toFloat(),height.toFloat())))
        }
        compose.waitForIdle()
    }
    fun tapPerkCombo(index:Int){
        val (x,y)=PerkTimeline.fusionPoint(index)
        compose.onNodeWithTag("perk-map").performTouchInput{
            val cam=MarketCamera()
            click(Offset(cam.sx(x,width.toFloat(),height.toFloat()),cam.sy(y,width.toFloat(),height.toFloat())))
        }
        compose.waitForIdle()
    }
    fun sampleShop(stage:Int=27,mask:Long=0L,perks:Int=0,developer:Boolean=false,reward:Int=0,wallet:Int=418,repeatMagnet:Boolean=false,rewardOverride:List<PerkOffer>?=null):ShopView{
        val w=World(seed=721L)
        w.reset(seed=721L,difficulty=Difficulty.NORMAL)
        w.skipTo(stage)
        w.players.coins[0]=wallet;w.players.hp[0]=9;w.players.maxHp[0]=12
        w.devSetFrame(if(stage>=25)TankEvolution.FOCUS else if(stage>=15)TankEvolution.TRIPLET else TankEvolution.BASIC)
        if(stage>=15){w.devSetUpgrade(Upgrades.DAMAGE,3);w.devSetUpgrade(Upgrades.MAGNET,2);w.devSetUpgrade(Upgrades.MAX_HEALTH,2)}
        w.devSetSkillMask(mask);w.players.perks[0]=perks
        val pl=w.players
        if(stage>5)pl.recordReward(0,5,TankEvolution.REWARD_BASE+TankEvolution.TWIN.ordinal)
        if(stage>15)pl.recordReward(0,15,TankEvolution.REWARD_BASE+TankEvolution.TRIPLET.ordinal)
        if(stage>25)pl.recordReward(0,25,TankEvolution.REWARD_BASE+TankEvolution.FOCUS.ordinal)
        for((i,at) in PerkRounds.stages.withIndex())if(stage>=at)
            PerkRounds.pools[i].firstOrNull{perks and (1 shl it)!=0}?.let{pl.recordReward(0,at,it)}
        w.devOpenShop()
        // Choose three valid real offers to make the different rarities visible.
        w.shop.devStock(w,if(repeatMagnet)listOf(CardCode.pack(Upgrades.MAGNET,CardRarity.COMMON),CardCode.pack(Upgrades.MAGNET,CardRarity.RARE),CardCode.pack(Upgrades.DAMAGE,CardRarity.UNCOMMON))else listOf(CardCode.pack(Upgrades.DAMAGE,CardRarity.RARE),CardCode.pack(Upgrades.MAX_HEALTH,CardRarity.COMMON),CardCode.pack(Upgrades.WEALTH,CardRarity.UNCOMMON)))
        w.shop.devReward(w,when(reward){1->dev.david.tessel.core.dev.DevReward.PERKS;2->dev.david.tessel.core.dev.DevReward.EVOLUTION;else->dev.david.tessel.core.dev.DevReward.NONE})
        return snapshotShop(w,developer,rewardOverride)
    }
    fun snapshotShop(w:World,developer:Boolean=false,rewardOverride:List<PerkOffer>?=null):ShopView{
        val pl=w.players
        val s=w.shop
        val offers=List(SHOP_SLOTS){slot->
            val id=s.item(0,slot)
            if(id<0)ShopOffer(slot,"Empty","",0,0,0,true,false,empty=true)
            else ShopOffer(slot,Upgrades.of(id).name,Upgrades.of(id).blurb,pl.levels[0][id],Upgrades.runCap(id),s.price(w,0,slot),s.maxed(w,0,slot),s.canBuy(w,0,slot),levels=s.levelsBought(w,0,slot),rarity=s.rarity(0,slot),locked=s.locked(0,slot),upgradeId=id)
        }
        val rewards=(0 until TankEvolution.REWARD_SLOTS).mapNotNull{slot->
            val id=s.rewardOffer(0,slot)
            if(id<0)null else PerkOffer(slot,TankEvolution.rewardName(id),TankEvolution.rewardDetail(id),TankEvolution.fromReward(id),id=id)
        }
        return ShopView(w.stage,pl.coins[0],pl.hp[0],pl.maxHp[0],offers,s.healPrice(w,0),s.healsLeft(0),s.canHeal(w,0),s.rerollPrice(w,0),s.canReroll(w,0),
            perkOffers=rewardOverride?:rewards,healAmount=s.healAmount(w,0),perkMask=pl.perks[0],skillNodes=pl.skillMask(0),freeRefreshesLeft=s.freeRefreshesLeft(w,0),
            devPreview=developer,debugLevels=if(developer)pl.levels[0].toList() else emptyList(),frame=pl.evolution[0],stats=ShopStatState.from(pl,0,w.difficulty,w.stage),rewardRounds=pl.rewardRounds[0].toList())
    }
    @Composable fun Market(s:ShopView,onReroll:()->Unit={},onBuy:(Int)->Unit={},onPerk:(Int)->Unit={}){
        currentShop=s
        RunShopScreen(s,false,onBuy, {}, onReroll, {}, onPerk, {_,_->}, {}, {}, {}, {_,_->}, {_,_->}, {_,_->})
    }
}
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk=[35],qualifiers="w914dp-h411dp-land-night-420dpi")
class ScreenshotsMenuGallery:MenuGalleryBase(){
    @Test fun home(){
        show{HomeScreen(Settings(),ledger,{}, {}, {}, {},playerName="Player")}
        capture("01-home")
    }
    @Test fun play(){
        val cp=Checkpoint(Difficulty.NORMAL,17,17*3900,1,true,3)
        cp.present[0]=true;cp.hp[0]=9;cp.maxHp[0]=12;cp.coins[0]=418
        show{PlayPanel(listOf(SoloSave(1,1700000000000L,cp)),Difficulty.NORMAL,1700001000000L,{}, {}, {}, {},onNewCharacter={},initialCharacter=CharacterDef.RAM,onBack={})}
        capture("02-play-setup")
    }
    @Test fun profile(){
        show{PlayerProfileScreen("Player",ledger,{}, {}, {}, {},onDeveloper={})}
        capture("03-profile-developer-unlocked")
    }
    @Test fun settings(){
        val updater=Updater(compose.activity)
        show{SettingsScreen(Settings(devUnlocked=true),updater,false,{}, {},fromProfile=true)}
        capture("04-settings-performance")
        for((label,name) in listOf("Stick size" to "05-settings-controls","Sound effects" to "06-settings-audio","Camera zoom" to "07-settings-display","Break Reminders" to "08-settings-reminders","Version" to "09-settings-version")){
            scroll(label);capture(name)
        }
    }
    @Test fun pin(){
        show{Sheet(visible=true,onDismiss={}){CodePanel({}, {})}}
        capture("10-developer-pin")
    }
    @Test fun developerOptions(){
        var settings by mutableStateOf(Settings(devUnlocked=true,dev=Dev(startStage=17,startCoins=500)))
        show{DevScreen(settings,"1.1.0-alpha.17",false,{settings=it(settings)}, {}, {}, {_,_->}, {_,_->},{_,_,_->},{},{},onBack={})}
        capture("11-developer-overview")
        for((label,name) in listOf("Start Coins" to "12-developer-wallet","Elite Enemies" to "13-developer-perks","Starting Upgrades" to "14-developer-upgrade-ranks","Starting frame · Basic" to "15-developer-tank","Shop reward" to "16-developer-shop-offers","All skills" to "17-developer-skills","Enemies and bosses" to "18-developer-enemies","Victory screen" to "19-developer-screen-tests","Audio cues" to "20-developer-sounds","Reset Developer Settings" to "21-developer-reset")){
            scroll(label);capture(name)
        }
    }
    @Test fun garage(){
        var settings by mutableStateOf(Settings())
        show{GarageScreen(settings,ledger,{settings=it(settings)},{})}
        capture("22-garage")
        for((label,name) in listOf("Body" to "23-garage-bodies","Turret" to "24-garage-turrets","Paint" to "25-garage-paints","Skin" to "26-garage-skins")){
            scroll(label);click(label);capture(name)
        }
    }
    @Test fun skillsHome(){
        show{SkillTreeScreen(0,0L,0,false,{}, {},character=CharacterDef.RAM)}
        capture("27-skills-locked-home")
        tapSkill(0);capture("28-skills-stage-requirement")
    }
    @Test fun marketEarly(){
        val shop=sampleShop(stage=1)
        show{Market(shop)}
        capture("29-upgrades-early")
        click("Skills");tapSkill(0);capture("30-shop-skills-locked")
        click("×");click("Perks");capture("31-perks-stage-five-gate")
    }
    @Test fun marketAdvanced(){
        val shop=sampleShop(mask=SkillGraph.bit(0) or SkillGraph.bit(9),perks=PerkCombo.LAST_LIGHT.mask)
        show{Market(shop)}
        capture("32-upgrades-radial")
        tapUpgrade(Upgrades.DAMAGE);capture("33-upgrade-inspection")
        scroll("Damage per bullet");capture("59-damage-forecast")
        click("×");click("Skills");capture("34-skills-discovery")
        tapSkill(9);capture("35-skill-inspection")
        click("×");click("Perks");capture("36-perks-radial-sigils")
        tapPerkCombo(0);capture("37-perk-combo-active")
        click("×")
        val (x,y)=PerkTimeline.perkPoint(Perks.SECOND_WIND)
        compose.onNodeWithTag("perk-map").performTouchInput{val cam=MarketCamera();click(Offset(cam.sx(x,width.toFloat(),height.toFloat()),cam.sy(y,width.toFloat(),height.toFloat())))}
        capture("38-perk-choice-detail")
    }
    @Test fun repeatOffersAndForecast(){
        show{Market(sampleShop(stage=17,wallet=420,repeatMagnet=true))}
        capture("60-repeatable-rank-net")
        tapUpgrade(Upgrades.MAGNET);capture("61-magnet-first-offer")
        scroll("Pickup radius");capture("62-magnet-radius-forecast")
        scroll("#2 · Rare");click("#2 · Rare");capture("63-magnet-rare-offer")
        scroll("Pickup radius");capture("64-magnet-rare-forecast")
    }
    @Test fun skillMoneyCheck(){
        show{Market(sampleShop(stage=5,wallet=12))}
        click("Skills");tapSkill(0);capture("65-skill-stage-met-money-missing")
    }
    @Test fun perkRoundsBeforeFusion(){
        show{Market(sampleShop(stage=17,perks=1 shl Perks.SECOND_WIND))}
        click("Perks");capture("66-perk-rounds-before-fusion")
        val (x,y)=PerkTimeline.perkPoint(Perks.SECOND_WIND)
        compose.onNodeWithTag("perk-map").performTouchInput{val cam=MarketCamera();click(Offset(cam.sx(x,width.toFloat(),height.toFloat()),cam.sy(y,width.toFloat(),height.toFloat())))}
        capture("67-perk-round-inspection")
    }
    @Test fun threeRoundFusionAndRoundChoice(){
        show{Market(sampleShop(stage=32,perks=PerkCombo.PHASE_ESCAPE.mask))}
        click("Perks");capture("68-three-round-fusion-map")
        tapPerkCombo(PerkCombo.PHASE_ESCAPE.ordinal);capture("69-three-round-fusion-detail")
    }
    @Test fun actualRevealAnimationFrames(){
        val combo=PerkCombo.PHASE_ESCAPE
        // The real production screen receives a representative valid reward snapshot.
        val choices=listOf(Perks.AFTERBURNER,Perks.VITAL_CORE,Perks.LONGSHOT).mapIndexed{i,id->PerkOffer(i,Perks.of(id).name,Perks.of(id).blurb,id=id)}
        val before=sampleShop(stage=30,perks=combo.mask and (1 shl Perks.AFTERBURNER).inv(),reward=1,rewardOverride=choices)
        var live by mutableStateOf(before)
        val after=sampleShop(stage=30,perks=combo.mask)
        show{Market(live){slot->if(slot==0)live=after}}
        capture("72-before-final-perk")
        compose.mainClock.autoAdvance=false
        compose.onAllNodesWithText("Take")[0].performClick()
        capture("73-fusion-reveal-pulse",240)
        capture("74-fusion-reveal-node",360)
        capture("75-fusion-reveal-settled",1000)
        compose.mainClock.autoAdvance=true
    }
    @Test fun expandedRankMesh(){
        show{Market(sampleShop(stage=17,wallet=420,repeatMagnet=true))}
        capture("76-upgrade-full-circle-mesh")
        click("+");capture("77-upgrade-full-circle-zoom")
    }
    @Test fun fullCatalogAndReroll(){
        val initial=sampleShop(stage=17,wallet=420,repeatMagnet=true)
        val new=sampleShop(stage=17,wallet=410)
        var live by mutableStateOf(initial)
        // The real Compose screen receives a representative post-reroll shop
        // snapshot; engine reroll itself is verified by the Shop unit tests.
        show{Market(live,onReroll={live=new})}
        capture("78-full-upgrade-catalog")
        click("Reroll · ${initial.rerollPrice}");capture("79-upgrade-rerolled")
        tapUpgrade(Upgrades.MAGNET);capture("80-unrolled-upgrade-inspection")
        click("×");click("Skills");capture("81-skills-no-lines")
        click("Perks");capture("82-perks-no-lines")
    }
    @Test fun sequentialUpgradePurchases(){
        val w=World(seed=73);w.reset(seed=73,difficulty=Difficulty.NORMAL)
        w.players.coins[0]=1000;w.devOpenShop()
        val roll=CardCode.pack(Upgrades.MAGNET,CardRarity.COMMON)
        w.shop.devStock(w,listOf(roll,roll,roll))
        w.shop.devReward(w,dev.david.tessel.core.dev.DevReward.NONE)
        var live by mutableStateOf(snapshotShop(w))
        show{Market(live,onBuy={slot->check(w.shop.buy(w,0,slot));live=snapshotShop(w)})}
        capture("83-upgrade-sequence-zero")
        tapUpgrade(Upgrades.MAGNET)
        repeat(3){rank->
            val price=w.shop.price(w,0,rank)
            click("Buy $price")
            check(w.players.levels[0][Upgrades.MAGNET]==rank+1)
            capture("84-upgrade-sequence-filled-${rank+1}-details")
        }
        click("×");capture("87-upgrade-sequence-three-filled")
    }
    @Test fun roundThreeChoices(){
        show{Market(sampleShop(stage=30,perks=PerkCombo.LAST_LIGHT.mask,reward=1))}
        capture("70-third-round-choice")
        click("Perk map");capture("71-third-round-available")
    }
    @Test fun marketBossRewards(){
        val shop=sampleShop(stage=10,reward=1)
        show{Market(shop)}
        capture("39-perk-choice")
        click("Perk map");capture("40-perk-map-during-reward")
    }
    @Test fun marketEvolution(){
        val shop=sampleShop(stage=5,reward=2)
        show{Market(shop)}
        capture("41-evolution-choice")
    }
    @Test fun devMarket(){
        val shop=sampleShop(developer=true)
        show{Market(shop)}
        tapUpgrade(Upgrades.FIRE_RATE,true);capture("42-developer-live-upgrade")
        click("×");click("Skills");capture("43-developer-all-skill-nodes")
        click("Perks");capture("44-developer-perk-map")
    }
    @Test fun exitMarket(){
        show{Market(sampleShop())}
        click("Exit");capture("45-shop-exit")
    }
    @Test fun statistics(){
        show{StatsScreen(ledger,1700001000000L,{}, {})}
        capture("46-statistics-overview")
        for((label,name) in listOf("Runs" to "47-statistics-runs","Logbook" to "48-statistics-logbook","Achievements" to "49-statistics-achievements")){
            click(label);capture(name)
        }
    }
    @Test fun multiplayer(){
        val game=GameSurfaceView(compose.activity)
        val lobby=CoopLobby(compose.activity,game)
        show{LobbyScreen(lobby,LobbyState.Idle,null,Settings(),{})}
        capture("50-multiplayer-entry")
    }
    @Test fun breakReminder(){
        show{Sheet(visible=true,onDismiss={}){BreakPanel(false,25,null,{}, {})}}
        capture("51-break-reminder")
    }
    @Test fun victory(){
        val run=RunSummary(seconds=2100,stage=50,kills=2500,coins=18400,difficulty=Difficulty.NORMAL,won=true)
        show{RunOverCard(true,run,{}, {})}
        capture("52-victory")
    }
}

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk=[35],qualifiers="w411dp-h914dp-port-night-420dpi")
class ScreenshotsMenuGalleryPortrait:MenuGalleryBase(){
    @Test fun arsenal(){
        show{CardCollectionScreen(ledger,{})}
        capture("53-arsenal-tank-list")
        scroll("Drone");click("Drone")
        capture("54-arsenal-drone-filter")
        click("Overseer");capture("55-arsenal-tank-diagram")
        click("Arsenal");click("Cards");capture("56-arsenal-cards")
        click("Damage");capture("57-arsenal-card-detail")
        click("Arsenal");click("Missions");capture("58-arsenal-missions")
    }
}
