package dev.david.tessel.ui.screens

import android.view.HapticFeedbackConstants
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import dev.david.tessel.core.content.*
import dev.david.tessel.platform.game.ShopOffer
import dev.david.tessel.platform.game.ShopView
import dev.david.tessel.platform.game.TankArt
import dev.david.tessel.ui.components.*
import dev.david.tessel.ui.theme.*
import kotlin.math.*

/** Every transaction still addresses an existing authoritative offer slot or skill ID.
 * No normal purchase is ever created by this screen. */
@Composable
fun RunShopScreen(shop:ShopView,vibrate:Boolean,
    onBuy:(Int)->Unit,onHeal:()->Unit,onReroll:()->Unit,onNext:()->Unit,
    onPerk:(Int)->Unit,onLock:(Int,Boolean)->Unit,onSkill:(Int)->Unit,onSave:()->Unit,
    onDevOptions:()->Unit,onDevRank:(Int,Int)->Unit,onDevPerk:(Int,Boolean)->Unit,onDevSkill:(Int,Boolean)->Unit){
    val c=Theme.colors;val view=LocalView.current
    fun feedback(){if(vibrate)view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)}
    var tab by rememberSaveable { mutableIntStateOf(if(shop.perkOffers.isNotEmpty())2 else 0) }
    var exit by rememberSaveable{mutableStateOf(false)}
    var rerollRequest by rememberSaveable{mutableIntStateOf(0)}
    val pending=shop.perkOffers.isNotEmpty()
    val signature=shop.perkOffers.map{it.id}
    LaunchedEffect(shop.stage,signature){if(pending)tab=2}
    BackHandler{if(exit)exit=false else exit=true}
    Column(Modifier.fillMaxSize().background(c.ground).windowInsetsPadding(WindowInsets.displayCutout).padding(horizontal=Space.x2,vertical=4.dp)){
        Row(Modifier.fillMaxWidth().heightIn(min=44.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)){
            BackButton("Exit",{exit=true})
            Column(Modifier.weight(1f)){
                Text(if(shop.devPreview)"TEST SHOP · Stage ${shop.stage}" else "Stage ${shop.stage} cleared",Type.headline,c.ink,maxLines=1)
                val team=if(shop.coop)" · ${shop.readyCount}/${shop.teamSize} ready" else ""
                Text("${shop.hp}/${shop.maxHp} HP$team",Type.footnote,c.ink2,maxLines=1)
            }
            CoinAmount(shop.coins,style=Type.headline)
            if(shop.devPreview)MarketButton("Dev",{onDevOptions()})
            Box(Modifier.widthIn(min=235.dp,max=340.dp).weight(1.5f)){
                Segmented(listOf("Upgrades","Skills","Perks"),tab,{tab=it;feedback()})
            }
        }
        if(exit){
            Column(Modifier.weight(1f).fillMaxWidth().background(c.surface,RoundedCornerShape(10.dp)).padding(16.dp),verticalArrangement=Arrangement.Center,horizontalAlignment=Alignment.CenterHorizontally){
                Text(if(shop.devPreview)"Discard this test run?" else if(shop.coop)"Save and stop for everyone?" else "Save this shop and return home?",Type.title3,c.ink)
                Text(if(shop.devPreview)"Sandbox purchases never enter your records." else "Your current shop state and purchases will be saved.",Type.footnote,c.ink2)
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
                    SecondaryButton("Cancel",{exit=false})
                    PrimaryButton(if(shop.devPreview)"Leave Test" else "Save & Exit",onSave)
                }
            }
        }else Box(Modifier.weight(1f).fillMaxWidth().padding(vertical=4.dp)){
            when(tab){
                0->UpgradeMarket(shop,{feedback();onBuy(it)},{i,v->feedback();onLock(i,v)},onDevRank,rerollRequest)
                1->SkillTreeScreen(shop.stage,shop.skillNodes,shop.coins,!pending&&!shop.selfReady,
                    {feedback();onSkill(it)},{tab=0},"Shop",embedded=true,
                    onDevSkill=if(shop.devPreview)onDevSkill else null,frame=shop.frame,character=shop.character,look=shop.look)
                else->PerkMarket(shop,{feedback();onPerk(it)},onDevPerk)
            }
        }
        Divider()
        Row(Modifier.fillMaxWidth().padding(top=4.dp),horizontalArrangement=Arrangement.spacedBy(6.dp),verticalAlignment=Alignment.CenterVertically){
            if(pending){
                Text("Choose your reward in Perks",Type.subhead,c.perk,Modifier.weight(1.7f))
                if(tab!=2)SecondaryButton("Choose reward",{tab=2},Modifier.weight(1f))
            }else{
                SecondaryButton("Heal +${shop.healAmount} · ${shop.healPrice} · ${shop.healsLeft} left",{feedback();onHeal()},Modifier.weight(1.25f),enabled=shop.canHeal&&!shop.selfReady)
                SecondaryButton(if(shop.rerollPrice==0)"Reroll · FREE (${shop.freeRefreshesLeft})" else "Reroll · ${shop.rerollPrice}",{feedback();rerollRequest++;onReroll()},Modifier.weight(1f),enabled=shop.canReroll&&!shop.selfReady)
            }
            PrimaryButton(if(shop.selfReady)"Waiting" else if(shop.coop)"Ready" else "Next Stage",{feedback();onNext()},Modifier.weight(.85f),enabled=!pending&&!shop.selfReady&&!exit)
        }
    }
}

private data class RankRoll(val levels:Int,val rarity:CardRarity,val tint:Color,val affordable:Boolean,val locked:Boolean)
private data class MarketItem(val id:Int,val x:Float,val y:Float,val icon:MarketIcon,val tint:Color,
    val inspectLabel:String,val selectable:Boolean=true,val owned:Boolean=false,
    val ranks:List<RankPosition> = emptyList(),val rankOwned:Int=0,
    val rolls:List<RankRoll> = emptyList(),val fusion:Boolean=false)

private fun rarityColor(tier:CardRarity,dark:Boolean,normal:Color,coin:Color):Color=when(tier){
    CardRarity.COMMON->normal
    CardRarity.UNCOMMON->Color(if(dark)0xFF76CF8A else 0xFF08743B)
    CardRarity.RARE->Color(if(dark)0xFF91B7FF else 0xFF315FB1)
    CardRarity.EPIC->Color(if(dark)0xFFCBA1FA else 0xFF784BA5)
    CardRarity.LEGENDARY->coin
}
private fun familyTint(f:CardFamily,dark:Boolean):Color=Color(if(dark)listOf(0xFFFFAF7E,0xFFF19B9B,0xFFC7A6F3,0xFF88C8FF,0xFFEAC46C)[f.ordinal]
    else listOf(0xFFAE541D,0xFFAE4B58,0xFF7750A5,0xFF2868A4,0xFF93630B)[f.ordinal])

internal fun upgradeExtent(shop:ShopView):Float{
    val offers=shop.offers.filter{!it.empty&&it.upgradeId>=0}.groupBy{it.upgradeId}.mapValues{it.value.maxOf{v->v.levels}}
    val levels=shop.stats?.levels?:List(Upgrades.count){id->if(shop.devPreview)shop.debugLevels.getOrElse(id){0}else shop.offers.firstOrNull{!it.empty&&it.upgradeId==id}?.level?:0}
    return UpgradeMap.extent(levels,offers,shop.devPreview)
}
@Composable
private fun UpgradeMarket(shop:ShopView,onBuy:(Int)->Unit,onLock:(Int,Boolean)->Unit,onDevRank:(Int,Int)->Unit,rerollRequest:Int){
    val c=Theme.colors
    val positions=remember(shop.devPreview){UpgradeMap.positions(shop.devPreview)}
    val rootLinks=remember(positions){UpgradeMap.links(positions)}
    val offers=shop.offers.filter{!it.empty&&it.upgradeId>=0}.groupBy{it.upgradeId}
    val totalOffers=offers.values.sumOf{it.size}
    var selected by rememberSaveable{mutableIntStateOf(-1)}
    var selectedSlot by rememberSaveable{mutableIntStateOf(-1)}
    val choices=offers[selected].orEmpty()
    val selectedOffer=choices.firstOrNull{it.slot==selectedSlot}?:choices.firstOrNull()
    val rankPositions=remember(positions){positions.associate{p->p.id to UpgradeMap.ranks(p,positions)}}
    fun rank(id:Int)=shop.stats?.levels?.getOrElse(id){0}?:if(shop.devPreview)shop.debugLevels.getOrElse(id){0}else offers[id]?.firstOrNull()?.level?:0
    LaunchedEffect(rerollRequest){selected=-1;selectedSlot=-1}
    BackHandler(enabled=selected>=0){selected=-1}
    val mapExtent=upgradeExtent(shop)
    val affordable=offers.values.flatten().count{it.affordable&&!it.maxed&&!shop.selfReady&&shop.perkOffers.isEmpty()}
    val items=positions.map{p->
        val group=offers[p.id].orEmpty()
        MarketItem(p.id,p.x,p.y,MarketIcon.upgrade(p.id),familyTint(p.family,c.dark),Upgrades.of(p.id).name,
            ranks=rankPositions.getValue(p.id),rankOwned=rank(p.id),
            rolls=group.filter{!it.maxed}.map{offer->RankRoll(offer.levels,offer.rarity,rarityColor(offer.rarity,c.dark,c.accent,c.coin),
                offer.affordable&&!shop.selfReady&&shop.perkOffers.isEmpty(),offer.locked)})
    }
    BoxWithConstraints(Modifier.fillMaxSize().clip(RoundedCornerShape(10.dp)).background(c.surface)){
        MarketCanvas(items,selected,{id->selected=id;selectedSlot=-1},tag="upgrade-map",
            footer="$totalOffers / 3 offers · $affordable affordable · ${UpgradeMap.rankCount(shop.devPreview)} ranks",frame=shop.frame,character=shop.character,look=shop.look,extent=mapExtent,rootLinks=rootLinks)
        val id=selected.takeIf{candidate->positions.any{it.id==candidate}}
        if(id!=null){
            val wide=maxWidth>=720.dp
            val mod=if(wide)Modifier.align(Alignment.CenterEnd).width(minOf(440.dp,maxWidth*.50f)).fillMaxHeight(.97f)
                else if(maxWidth>=580.dp)Modifier.align(Alignment.CenterEnd).width(310.dp).fillMaxHeight(.96f)
                else Modifier.align(Alignment.BottomCenter).fillMaxWidth().fillMaxHeight(.90f)
            val def=Upgrades.of(id);val offer=selectedOffer
            val owned=rank(id);val cap=if(shop.devPreview)maxOf(owned,Upgrades.runCap(id))else Upgrades.runCap(id)
            val changes=remember(shop.stats,id,offer?.levels){shop.stats?.forecast(id,offer?.levels?:0).orEmpty()}
            val tint=if(offer==null)c.accent else rarityColor(offer.rarity,c.dark,c.accent,c.coin)
            InspectPanel(mod,def.name,offer?.rarity?.label ?: if(shop.devPreview)"Developer inspection" else "Not rolled",MarketIcon.upgrade(id),if(offer==null)c.accent else rarityColor(offer.rarity,c.dark,c.accent,c.coin),{selected=-1},
                body={
                    if(choices.size>1){
                        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(4.dp)){
                            for(option in choices){
                                val active=option.slot==offer?.slot
                                val color=rarityColor(option.rarity,c.dark,c.accent,c.coin)
                                Box(Modifier.heightIn(min=34.dp).clip(RoundedCornerShape(6.dp)).background(if(active)c.track else c.surface)
                                    .border(1.dp,if(active)color else c.line,RoundedCornerShape(6.dp)).clickable{selectedSlot=option.slot}.padding(horizontal=8.dp,vertical=5.dp),
                                    contentAlignment=Alignment.Center){Text("#${option.slot+1} · ${option.rarity.label}",Type.footnote,color)}
                            }
                        }
                    }
                    val requirements:@Composable ()->Unit={
                        Column(verticalArrangement=Arrangement.spacedBy(2.dp)){
                            if(offer==null){
                                Text("Requirements",Type.caption,c.ink2)
                                PurchaseRequirement("Shop roll","Not offered",false)
                                PurchaseRequirement("Stage","${shop.stage} / ${Upgrades.firstStage(id)}",shop.stage>=Upgrades.firstStage(id))
                                PurchaseRequirement("Rank slots","${(cap-owned).coerceAtLeast(0)} left",owned<cap)
                                if(!Upgrades.offered(id,shop.frame))PurchaseRequirement("Tank","Incompatible",false)
                            }else{
                                Text("Requirements",Type.caption,c.ink2)
                                PurchaseRequirement("Stage","${shop.stage} / ${Upgrades.firstStage(id)}",shop.stage>=Upgrades.firstStage(id))
                                PurchaseRequirement("Coins","${shop.coins} / ${offer.price}",shop.coins>=offer.price)
                                PurchaseRequirement("Rank slots","${(cap-owned).coerceAtLeast(0)} left",!offer.maxed)
                                if(!Upgrades.offered(id,shop.frame))PurchaseRequirement("Tank","Incompatible",false)
                                if(shop.stats!=null&&!CardMission.eligible(id,shop.stats.missionMask,offer.rarity))PurchaseRequirement("Mission","Locked",false)
                                if(shop.perkOffers.isNotEmpty()||shop.selfReady)PurchaseRequirement("Shop","Wait",false)
                            }
                        }
                    }
                    val forecast:@Composable ()->Unit={
                        Column(verticalArrangement=Arrangement.spacedBy(4.dp)){
                            if(offer!=null){
                                Text("Your stats after purchase",Type.caption,c.ink2)
                                for(change in changes){
                                    Column(Modifier.fillMaxWidth().background(c.surface,RoundedCornerShape(6.dp)).padding(5.dp)){
                                        Text(change.label,Type.footnote,c.ink2)
                                        Text("${statNumber(change.before,change.label)} → ${statNumber(change.after,change.label)} ${change.unit}",Type.headline,tint)
                                    }
                                }
                                if(changes.isEmpty()&&shop.stats!=null)Text("No further effective increase at the current stat cap.",Type.footnote,c.ink2)
                            }
                            Text("Rank $owned → ${(owned+(offer?.levels?:0)).coerceAtMost(cap)} / $cap",Type.subhead,c.ink)
                            Text(if(owned>=cap)"All ranks filled" else if(offer!=null)"Fills ranks ${owned+1}–${(owned+offer.levels).coerceAtMost(cap)} in order" else "Next rank: ${owned+1}",Type.caption,c.ink3)
                        }
                    }
                    if(wide){
                        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp),verticalAlignment=Alignment.Top){
                            Box(Modifier.weight(1f)){requirements()}
                            Box(Modifier.weight(1f)){forecast()}
                        }
                    }else{
                        forecast();requirements()
                    }
                    Text(offer?.blurb?:def.blurb,Type.footnote,c.ink2)
                    if(shop.stats!=null&&offer!=null)Text("Current build · includes active modifiers",Type.caption,c.ink3)
                    if(shop.devPreview){
                        Text("DEV · exact rank (0–${def.maxLevel})",Type.caption,c.coin)
                        var rankInput by remember(id,owned){mutableStateOf(owned.toString())}
                        Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(5.dp)){
                            BasicTextField(rankInput,{rankInput=it.filter{ch->ch.isDigit()}.take(3)},singleLine=true,textStyle=Type.body.copy(color=c.ink),cursorBrush=SolidColor(c.accent),modifier=Modifier.weight(1f).background(c.track,RoundedCornerShape(6.dp)).padding(8.dp))
                            MarketButton("Set",{onDevRank(id,(rankInput.toIntOrNull()?:0).coerceIn(0,def.maxLevel))})
                            MarketButton("Max",{onDevRank(id,def.maxLevel)})
                            MarketButton("0",{onDevRank(id,0)})
                        }
                    }
                },actions={
                    if(offer!=null){
                        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(5.dp)){
                            SecondaryButton(if(offer.locked)"Unlock" else "Lock",{onLock(offer.slot,!offer.locked)},Modifier.weight(.8f),enabled=!offer.maxed&&!shop.selfReady&&shop.perkOffers.isEmpty())
                            PrimaryButton(if(offer.maxed)"Maxed" else "Buy ${offer.price}",{onBuy(offer.slot)},Modifier.weight(1.2f),enabled=offer.affordable&&!offer.maxed&&!shop.selfReady&&shop.perkOffers.isEmpty())
                        }
                    }else Text("Not on sale · normal purchases require a rolled offer.",Type.caption,c.ink3)
                })
        }
    }
}

@Composable
private fun PerkMarket(shop:ShopView,onPerk:(Int)->Unit,onDevPerk:(Int,Boolean)->Unit){
    val c=Theme.colors;val pending=shop.perkOffers.isNotEmpty()
    val rewardKey=shop.perkOffers.map{it.id}
    var rewards by rememberSaveable(shop.stage,rewardKey.toString()){mutableStateOf(pending)}
    var selected by rememberSaveable{mutableIntStateOf(-1)}
    var catalog by rememberSaveable{mutableStateOf(false)}
    val comboBase=PerkTimeline.FUSION_BASE
    val unrecorded=PerkTimeline.unrecorded(shop.rewardRounds,shop.perkMask)
    val activeFusions=PerkCombo.entries.filter{it.active(shop.perkMask)}
    val activeKey=activeFusions.map{it.ordinal}
    var lastFusions by remember { mutableStateOf(activeKey) }
    var revealing by remember { mutableStateOf(emptySet<Int>()) }
    val reveal=remember { Animatable(1f) }
    LaunchedEffect(activeKey){
        val unlocked=activeKey.filter{it !in lastFusions}
        lastFusions=activeKey
        if(unlocked.isNotEmpty()){
            // Pulse and reveal the new golden node. No connecting lines.
            // No popup, automatic inspector, banner or replay when reopening the tab.
            rewards=false;catalog=false;selected=-1
            reveal.snapTo(0f);revealing=unlocked.map{comboBase+it}.toSet()
            reveal.animateTo(1f,tween(950,easing=FastOutSlowInEasing))
            revealing=emptySet()
        }else{revealing=emptySet();reveal.snapTo(1f)}
    }
    BackHandler(enabled=selected>=0||pending&&rewards||catalog){if(catalog)catalog=false else if(selected>=0)selected=-1 else rewards=false}
    Column(Modifier.fillMaxSize()){
        Row(Modifier.fillMaxWidth().padding(bottom=4.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(6.dp)){
            Text("Frame · ${shop.frame.label}",Type.footnote,c.ink2,Modifier.weight(1f),maxLines=1)
            if(pending)MarketButton(if(rewards)"Perk map" else "Choose reward",{rewards=!rewards;selected=-1;catalog=false})
            MarketButton(if(catalog)"Rounds" else if(shop.devPreview)"Dev perks" else "Other rewards",{catalog=!catalog;rewards=false;selected=-1})
        }
        // Compact status strip. The only permanent fusion indicator is its golden map node.
        Row(Modifier.fillMaxWidth().padding(horizontal=5.dp,vertical=2.dp),horizontalArrangement=Arrangement.SpaceBetween){
            for((i,stage) in PerkRounds.stages.withIndex()){
                val chosen=PerkRounds.choice(shop.rewardRounds,i)
                val available=stage==shop.stage&&pending&&shop.perkOffers.any{Perks.isRunPerk(it.id)}
                val done=chosen>=0
                val tint=if(done)c.accent else if(available)c.coin else c.ink2
                val status=when{done->"✓ Chosen";available->"Choose now";shop.stage<stage->"Locked";else->"Passed"}
                Text("R${i+1} · $stage · $status",Type.footnote,tint,maxLines=1,
                    modifier=Modifier.clickable{rewards=false;catalog=false;selected=PerkTimeline.perkNode(if(chosen in PerkRounds.pools[i])chosen else PerkRounds.pools[i].first())})
            }
        }
        if(pending&&rewards){
            Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(4.dp),verticalArrangement=Arrangement.spacedBy(5.dp)){
                val round=PerkRounds.roundAt(shop.stage)
                Text(if(shop.perkOffers.any{it.evolution!=null})"Choose a tank evolution" else if(round>=0)"Round ${round+1} · Choose one perk" else "Choose one boss perk",Type.headline,c.ink)
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)){
                    for(offer in shop.perkOffers){
                        Column(Modifier.width(182.dp).heightIn(min=142.dp).background(c.surface,RoundedCornerShape(10.dp)).border(1.dp,c.line,RoundedCornerShape(10.dp)).padding(9.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){
                            if(offer.evolution!=null)TankPreview(shop.character,shop.look,Modifier.size(46.dp),offer.evolution)
                            else if(Perks.isRunPerk(offer.id))Canvas(Modifier.fillMaxWidth().height(38.dp)){
                                marketGlyph(MarketIcon.perk(offer.id),center,size.minDimension*.38f,c.perk)
                            }
                            Text(offer.name,Type.headline,c.ink,maxLines=2)
                            Text(offer.blurb,Type.footnote,c.ink2)
                            PrimaryButton(if(offer.evolution!=null)"Evolve" else if(offer.id==TankEvolution.STAY_BASIC)"Stay Basic" else "Take",{onPerk(offer.slot)},Modifier.fillMaxWidth(),enabled=!shop.selfReady)
                        }
                    }
                }
            }
        }else BoxWithConstraints(Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(c.surface)){
            val items=buildList{
                for((i,pool) in PerkRounds.pools.withIndex()){
                    val chosen=PerkRounds.choice(shop.rewardRounds,i)
                    for(id in pool){
                        val (x,y)=PerkTimeline.perkPoint(id)
                        val recorded=chosen==id
                        val active=(recorded||shop.devPreview)&&shop.perkMask and (1 shl id)!=0
                        add(MarketItem(PerkTimeline.perkNode(id),x,y,MarketIcon.perk(id),c.perk,
                            Perks.of(id).name,owned=active))
                    }
                }
                for(combo in activeFusions){
                    val (x,y)=PerkTimeline.fusionPoint(combo.ordinal)
                    add(MarketItem(comboBase+combo.ordinal,x,y,MarketIcon.LINK,c.coin,combo.title,owned=true,fusion=true))
                }
            }
            MarketCanvas(items,selected,{selected=it},tag="perk-map",
                footer="Three rings · One choice per round",frame=shop.frame,character=shop.character,look=shop.look,
                revealing=revealing,revealProgress=reveal.value)
            if(catalog){
                Column(Modifier.align(Alignment.CenterEnd).fillMaxHeight().widthIn(max=330.dp).fillMaxWidth(.48f).background(c.ground,RoundedCornerShape(8.dp)).padding(8.dp)){
                    Text(if(shop.devPreview)"Developer perk controls" else "Other recorded rewards",Type.headline,c.ink)
                    Column(Modifier.weight(1f).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(5.dp)){
                        if(!shop.devPreview){
                            for((i,id) in shop.rewardRounds.withIndex())if(RewardRounds.valid(id)&&!Perks.isRunPerk(id))
                                Text("Stage ${RewardRounds.stage(i)} · ${TankEvolution.rewardName(id)}",Type.subhead,c.ink)
                            if(unrecorded.isEmpty())Text("No other active perks",Type.footnote,c.ink2)
                        }
                        for(id in if(shop.devPreview)Perks.runIds else unrecorded){
                            val own=shop.perkMask and (1 shl id)!=0
                            Text(Perks.of(id).name+" · Round ${PerkRounds.roundOf(id)+1}",Type.subhead,c.ink)
                            Text(Perks.of(id).blurb,Type.footnote,c.ink2)
                            if(shop.devPreview)SecondaryButton(if(own)"DEV · Remove" else "DEV · Grant",{onDevPerk(id,!own)},Modifier.fillMaxWidth())
                            Divider()
                        }
                    }
                }
            }else if(selected>=0){
                val combo=PerkCombo.entries.getOrNull(selected-comboBase)?.takeIf{it.active(shop.perkMask)}
                val perk=(selected-PerkTimeline.ROUND_BASE).takeIf{Perks.isRunPerk(it)}
                val mod=if(maxWidth>=580.dp)Modifier.align(Alignment.CenterEnd).width(310.dp).fillMaxHeight(.96f) else Modifier.align(Alignment.BottomCenter).fillMaxWidth().fillMaxHeight(.8f)
                if(combo!=null)InspectPanel(mod,combo.title,"${combo.inputs.size}-perk fusion",MarketIcon.LINK,c.coin,{selected=-1},body={
                    Text(combo.effect,Type.headline,c.ink)
                    Text("Discovered from",Type.caption,c.ink2)
                    for(id in combo.inputs)Text("Round ${PerkRounds.roundOf(id)+1} · ${Perks.of(id).name}",Type.subhead,c.ink)
                },actions={Text("Active",Type.subhead,c.accent)})
                else if(perk!=null){
                    val round=PerkRounds.roundOf(perk)
                    val stage=PerkRounds.stages[round]
                    val chosen=PerkRounds.choice(shop.rewardRounds,round)
                    val own=shop.perkMask and (1 shl perk)!=0
                    val offer=shop.perkOffers.firstOrNull{it.id==perk}
                    InspectPanel(mod,Perks.of(perk).name,"Round ${round+1} · stage $stage",MarketIcon.perk(perk),if(own)c.perk else c.ink2,{selected=-1},body={
                        Text(Perks.of(perk).blurb,Type.subhead,c.ink)
                        if(!own){
                            PurchaseRequirement("Stage","${shop.stage} / $stage",shop.stage>=stage)
                            Text(when{chosen>=0->"Another perk was chosen this round.";offer!=null->"Available in this round's reward.";shop.stage<stage->"Defeat stage $stage for the perk choice.";else->"Not selected or offered in this run."},Type.footnote,c.ink2)
                        }else Text("Active for this run",Type.footnote,c.accent)
                    },actions={
                        if(shop.devPreview)SecondaryButton(if(own)"DEV · Remove" else "DEV · Grant",{onDevPerk(perk,!own)},Modifier.fillMaxWidth())
                        else if(offer!=null&&!own&&chosen<0)PrimaryButton("Take",{selected=-1;onPerk(offer.slot)},Modifier.fillMaxWidth(),enabled=!shop.selfReady)
                        else if(chosen==perk&&!own)Text("No longer active",Type.caption,c.ink2)
                    })
                }
            }
        }
    }
}

@Composable
private fun MarketCanvas(items:List<MarketItem>,selected:Int,inspect:(Int)->Unit,tag:String,footer:String,frame:TankEvolution,character:CharacterDef,look:TankLook,extent:Float=UpgradeMap.WIDTH,revealing:Set<Int> = emptySet(),revealProgress:Float=1f,rootLinks:List<MapLink> = emptyList()){
    val c=Theme.colors;var zoom by rememberSaveable(tag){mutableFloatStateOf(1f)}
    var px by rememberSaveable(tag){mutableFloatStateOf(0f)};var py by rememberSaveable(tag){mutableFloatStateOf(0f)}
    var viewport by remember{mutableStateOf(IntSize.Zero)}
    val liveItems by rememberUpdatedState(items);val callback by rememberUpdatedState(inspect);val liveExtent by rememberUpdatedState(extent)
    val camera=MarketCamera(zoom,px,py,liveExtent)
    val art=remember{TankArt()}
    fun update(next:MarketCamera){zoom=next.zoom;px=next.x;py=next.y}
    fun resize(factor:Float){
        val w=viewport.width.toFloat();val h=viewport.height.toFloat()
        if(w>0f&&h>0f)update(MarketCamera(zoom,px,py,liveExtent).gesture(w,h,w/2,h/2,0f,0f,factor))
    }
    Box(Modifier.fillMaxSize()){
        Canvas(Modifier.fillMaxSize().onSizeChanged{viewport=it}.testTag(tag)
            .semantics{contentDescription=footer;customActions=items.filter{it.selectable}.map{n->
                CustomAccessibilityAction("Inspect "+n.inspectLabel){callback(n.id);true}
            }}
            .pointerInput(tag){detectTransformGestures{centroid,pan,change,_->
                update(MarketCamera(zoom,px,py,liveExtent).gesture(size.width.toFloat(),size.height.toFloat(),centroid.x,centroid.y,pan.x,pan.y,change))
            }}
            .pointerInput(tag){detectTapGestures{at->
                val cam=MarketCamera(zoom,px,py,liveExtent);val w=size.width.toFloat();val h=size.height.toFloat()
                fun distance(n:MarketItem):Float{
                    fun atDistance(x:Float,y:Float)=hypot(cam.sx(x,w,h)-at.x,cam.sy(y,w,h)-at.y)
                    return if(n.ranks.isEmpty())atDistance(n.x,n.y) else n.ranks.minOf{atDistance(it.x,it.y)}
                }
                val closest=liveItems.minByOrNull{distance(it)}
                if(closest!=null){
                    val reach=if(closest.ranks.isEmpty())PerkTimeline.NODE_RADIUS else UpgradeMap.NODE_RADIUS
                    if(closest.selectable&&distance(closest)<=max(9.dp.toPx(),reach*cam.scale(w,h)))callback(closest.id) else callback(-1)
                }
            }}) {
            val w=size.width;val h=size.height;val scale=camera.scale(w,h)
            fun pos(n:MarketItem)=Offset(camera.sx(n.x,w,h),camera.sy(n.y,w,h))
            val center=Offset(camera.sx(0f,w,h),camera.sy(0f,w,h))
            // Hub spokes and unobtrusive sibling edges are visual only.
            // Draw behind all circles. Skills and perks pass an empty edge list.
            val byId=items.associateBy{it.id}
            for(edge in rootLinks){
                val end=byId[edge.b]?:continue
                val start=byId[edge.a]
                if(edge.a==UpgradeMap.HUB_ID){
                    val ready=end.rolls.filter{it.affordable}.maxByOrNull{it.rarity.ordinal}
                    val active=end.rankOwned>0||ready!=null
                    drawLine((if(end.rankOwned>0)end.tint else ready?.tint?:end.tint).copy(alpha=if(active).82f else .32f),
                        center,pos(end),strokeWidth=if(active)2.dp.toPx() else 1.dp.toPx())
                }else if(start!=null){
                    drawLine(end.tint.copy(alpha=.24f),pos(start),pos(end),strokeWidth=.85.dp.toPx())
                }
            }
            treeTank(art,center,(78f*scale).coerceIn(13.dp.toPx(),48.dp.toPx()),character,look,frame,c.ink,c.ground,c.accent)
            for(n in items){
                val at=pos(n)
                val radius=if(n.ranks.isNotEmpty())UpgradeMap.NODE_RADIUS*scale else PerkTimeline.NODE_RADIUS*scale
                if(n.ranks.isNotEmpty()){
                    // Draw each independent rank chain, never connect two
                    // unrelated upgrade types. Owned progress is contiguous.
                    for((a,b) in n.ranks.zipWithNext()){
                        val p=Offset(camera.sx(a.x,w,h),camera.sy(a.y,w,h))
                        val q=Offset(camera.sx(b.x,w,h),camera.sy(b.y,w,h))
                        val bought=UpgradeMap.ownedRank(b.rank,n.rankOwned)
                        val offered=n.rolls.any{it.affordable&&UpgradeMap.offeredRank(b.rank,n.rankOwned,it.levels)}
                        val active=bought||offered
                        val roll=n.rolls.filter{it.affordable&&UpgradeMap.offeredRank(b.rank,n.rankOwned,it.levels)}.maxByOrNull{it.rarity.ordinal}
                        drawLine((roll?.tint?:n.tint).copy(alpha=if(active).80f else .28f),p,q,
                            strokeWidth=if(active)2.dp.toPx() else 1.dp.toPx())
                    }
                    for(cell in n.ranks){
                        val p=Offset(camera.sx(cell.x,w,h),camera.sy(cell.y,w,h))
                        if(p.x+radius<0||p.x-radius>w||p.y+radius<0||p.y-radius>h)continue
                        val bought=UpgradeMap.ownedRank(cell.rank,n.rankOwned)
                        val roll=if(bought)null else n.rolls.filter{UpgradeMap.offeredRank(cell.rank,n.rankOwned,it.levels)}
                            .maxWithOrNull(compareBy<RankRoll>{it.affordable}.thenBy{it.rarity.ordinal})
                        val offered=roll!=null
                        val ready=roll?.affordable==true
                        val tint=roll?.tint?:n.tint
                        if(ready){
                            // Bright frontier; higher rarity bundles show the
                            // immediately following ranks, never skip one.
                            val first=cell.rank==n.rankOwned+1
                            drawCircle(tint.copy(alpha=if(first).20f else .09f),radius+4.dp.toPx(),p)
                            drawCircle(tint.copy(alpha=if(first).38f else .20f),radius+2.dp.toPx(),p)
                        }
                        drawCircle(if(bought)n.tint.copy(alpha=.76f) else if(ready)tint.copy(alpha=.28f) else if(offered)tint.copy(alpha=.08f) else c.ground,radius,p)
                        drawCircle(tint.copy(alpha=if(ready)1f else if(offered).73f else if(bought).85f else .30f),radius,p,
                            style=Stroke(if(ready)2.2.dp.toPx() else if(offered||n.id==selected)1.4.dp.toPx() else .9.dp.toPx()))
                        marketGlyph(n.icon,p,radius*.57f,if(bought)c.ground else tint.copy(alpha=if(ready)1f else if(offered).72f else .43f))
                        if(roll?.locked==true)drawCircle(c.coin,2.dp.toPx(),p+Offset(radius*.75f,-radius*.75f))
                    }
                }else{
                    if(at.x+radius<0||at.x-radius>w||at.y+radius<0||at.y-radius>h)continue
                    if(n.fusion){
                        val fraction=if(n.id in revealing)revealProgress else 1f
                        val grow=fraction.coerceIn(0f,1f)
                        if(grow<=0f)continue
                        val r=radius*(.65f+.35f*grow)
                        if(n.id in revealing)drawCircle(n.tint.copy(alpha=.28f*(1f-grow)),r+18.dp.toPx()*grow,at)
                        drawCircle(n.tint.copy(alpha=.13f*grow),r+4.dp.toPx(),at)
                        drawCircle(n.tint.copy(alpha=grow),r,at)
                        marketGlyph(n.icon,at,r*.57f,c.ground.copy(alpha=grow))
                    }else{
                        if(n.owned)drawCircle(n.tint.copy(alpha=.10f),radius+3.dp.toPx(),at)
                        drawCircle(if(n.owned)n.tint else c.ground,radius,at)
                        drawCircle(n.tint.copy(alpha=if(n.owned)1f else if(n.id==selected).7f else .28f),radius,at,style=Stroke(if(n.id==selected)2.2.dp.toPx() else 1.1.dp.toPx()))
                        marketGlyph(n.icon,at,radius*.57f,if(n.owned)c.ground else n.tint.copy(alpha=.42f))
                    }
                }
            }
        }
        Row(Modifier.align(Alignment.TopStart).padding(5.dp),horizontalArrangement=Arrangement.spacedBy(3.dp),verticalAlignment=Alignment.CenterVertically){
            MarketButton("−",{resize(.75f)});MarketButton("+",{resize(1.33f)});MarketButton("Fit",{update(MarketCamera(extent=extent))})
            Text("${(zoom*100).roundToInt()}%",Type.caption,c.ink2,Modifier.padding(horizontal=4.dp))
        }
        Text(footer,Type.caption,c.ink3,Modifier.align(Alignment.BottomStart).padding(5.dp))
    }
}

@Composable
private fun InspectPanel(modifier:Modifier,title:String,subtitle:String,icon:MarketIcon,tint:Color,onClose:()->Unit,body:@Composable ColumnScope.()->Unit,actions:@Composable ColumnScope.()->Unit){
    val c=Theme.colors;val shape=RoundedCornerShape(10.dp)
    Column(modifier.padding(5.dp).clip(shape).background(c.ground).border(1.dp,c.line,shape).pointerInput(Unit){detectTapGestures{}}.padding(9.dp)){
        Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(7.dp)){
            Canvas(Modifier.size(34.dp)){drawCircle(tint.copy(alpha=.14f),size.minDimension*.47f);marketGlyph(icon,center,size.minDimension*.32f,tint)}
            Column(Modifier.weight(1f)){Text(title,Type.headline,c.ink,maxLines=2);Text(subtitle,Type.caption,tint,maxLines=1)}
            MarketButton("×",onClose)
        }
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(vertical=6.dp),verticalArrangement=Arrangement.spacedBy(5.dp),content=body)
        Column(Modifier.fillMaxWidth(),content=actions)
    }
}
@Composable
private fun MarketButton(label:String,action:()->Unit){
    val c=Theme.colors
    Box(Modifier.heightIn(min=38.dp).widthIn(min=38.dp).clip(RoundedCornerShape(7.dp)).background(c.track).clickable(onClick=action).padding(horizontal=8.dp),contentAlignment=Alignment.Center){Text(label,Type.subhead,c.ink)}
}

private fun statNumber(value:Float,label:String):String{
    if(label=="Regeneration interval"&&value==0f)return "Off"
    return String.format(java.util.Locale.US,"%.2f",value).trimEnd('0').trimEnd('.')
}

