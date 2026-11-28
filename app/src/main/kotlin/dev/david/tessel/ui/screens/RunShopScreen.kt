package dev.david.tessel.ui.screens

import android.graphics.Paint
import android.graphics.Typeface
import android.view.HapticFeedbackConstants
import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
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
                0->UpgradeMarket(shop,{feedback();onBuy(it)},{i,v->feedback();onLock(i,v)},onDevRank)
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
                SecondaryButton(if(shop.rerollPrice==0)"Reroll · FREE (${shop.freeRefreshesLeft})" else "Reroll · ${shop.rerollPrice}",{feedback();onReroll()},Modifier.weight(1f),enabled=shop.canReroll&&!shop.selfReady)
            }
            PrimaryButton(if(shop.selfReady)"Waiting" else if(shop.coop)"Ready" else "Next Stage",{feedback();onNext()},Modifier.weight(.85f),enabled=!pending&&!shop.selfReady&&!exit)
        }
    }
}

private data class MarketLabel(val item:MarketItem,val x:Float,val y:Float,val right:Boolean)
private data class MarketItem(val id:Int,val x:Float,val y:Float,val icon:MarketIcon,val tint:Color,
    val visibleLabel:String?=null,val caption:String?=null,val selectable:Boolean=false,val owned:Boolean=false,val lit:Boolean=false,val locked:Boolean=false,val sigils:List<Int> = emptyList(),val ranks:List<RankPosition> = emptyList(),val rankOwned:Int=0,val rankOffered:Int=0,val centerText:String?=null,val fusion:Boolean=false)

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
private fun UpgradeMarket(shop:ShopView,onBuy:(Int)->Unit,onLock:(Int,Boolean)->Unit,onDevRank:(Int,Int)->Unit){
    val c=Theme.colors
    val positions=remember(shop.devPreview){UpgradeMap.positions(shop.devPreview)}
    val links=remember(positions){UpgradeMap.links(positions)}
    val offers=shop.offers.filter{!it.empty&&it.upgradeId>=0}.groupBy{it.upgradeId}
    val totalOffers=offers.values.sumOf{it.size}
    var selected by rememberSaveable{mutableIntStateOf(-1)}
    var selectedSlot by rememberSaveable{mutableIntStateOf(-1)}
    val choices=offers[selected].orEmpty()
    val selectedOffer=choices.firstOrNull{it.slot==selectedSlot}?:choices.firstOrNull()
    val rankPositions=remember(positions){positions.associate{p->p.id to UpgradeMap.ranks(p,positions)}}
    fun rank(id:Int)=shop.stats?.levels?.getOrElse(id){0}?:if(shop.devPreview)shop.debugLevels.getOrElse(id){0}else offers[id]?.firstOrNull()?.level?:0
    LaunchedEffect(offers.keys,shop.devPreview){if(!shop.devPreview&&selected !in offers)selected=-1}
    BackHandler(enabled=selected>=0){selected=-1}
    val mapExtent=upgradeExtent(shop)
    val items=positions.map{p->
        val group=offers[p.id].orEmpty()
        val offer=group.maxByOrNull{it.rarity.ordinal}
        val tint=if(offer==null)familyTint(p.family,c.dark) else rarityColor(offer.rarity,c.dark,c.accent,c.coin)
        MarketItem(p.id,p.x,p.y,MarketIcon.upgrade(p.id),tint,
            offer?.name,if(group.size>1)"${group.size} offers · ${rank(p.id)}/${Upgrades.runCap(p.id)}" else offer?.let{if(it.maxed)"MAX" else "${it.price} · +${it.levels}"},
            selectable=offer!=null||shop.devPreview,lit=offer!=null,locked=group.any{it.locked},
            ranks=rankPositions.getValue(p.id).take(UpgradeMap.visibleCount(Upgrades.runCap(p.id),rank(p.id),group.maxOfOrNull{it.levels}?:0)),
            rankOwned=rank(p.id),rankOffered=group.maxOfOrNull{it.levels}?:0)
    }
    BoxWithConstraints(Modifier.fillMaxSize().clip(RoundedCornerShape(10.dp)).background(c.surface)){
        MarketCanvas(items,links,selected,{id->selected=id;selectedSlot=-1},tag="upgrade-map",
            labels=UpgradeMap.groups.mapNotNull{g->
                val row=positions.filter{it.family==g}
                if(row.isEmpty())null else {
                    val a=atan2(row.sumOf{it.y.toDouble()},row.sumOf{it.x.toDouble()})
                    val r=mapExtent*.5f-45f
                    Triple(UpgradeMap.label(g).uppercase(),(r*cos(a)).toFloat(),(r*sin(a)).toFloat())
                }
            },
            footer="$totalOffers / 3 offers · Full circles are individual ranks · Branches expand as you buy",frame=shop.frame,character=shop.character,look=shop.look,extent=mapExtent)
        val id=selected.takeIf{it in 0 until Upgrades.count}
        if(id!=null&&(selectedOffer!=null||shop.devPreview)){
            val wide=maxWidth>=720.dp
            val mod=if(wide)Modifier.align(Alignment.CenterEnd).width(minOf(440.dp,maxWidth*.50f)).fillMaxHeight(.97f)
                else if(maxWidth>=580.dp)Modifier.align(Alignment.CenterEnd).width(310.dp).fillMaxHeight(.96f)
                else Modifier.align(Alignment.BottomCenter).fillMaxWidth().fillMaxHeight(.90f)
            val def=Upgrades.of(id);val offer=selectedOffer
            val owned=rank(id);val cap=if(shop.devPreview)maxOf(owned,Upgrades.runCap(id))else Upgrades.runCap(id)
            val changes=remember(shop.stats,id,offer?.levels){shop.stats?.forecast(id,offer?.levels?:0).orEmpty()}
            val tint=if(offer==null)c.accent else rarityColor(offer.rarity,c.dark,c.accent,c.coin)
            InspectPanel(mod,def.name,offer?.rarity?.label ?: "Developer inspection",MarketIcon.upgrade(id),if(offer==null)c.accent else rarityColor(offer.rarity,c.dark,c.accent,c.coin),{selected=-1},
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
                            if(offer!=null){
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
                            Text("Each purchased rank fills one circle in the map.",Type.caption,c.ink3)
                        }
                    }
                    if(wide&&offer!=null){
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
    LaunchedEffect(activeKey){
        val unlocked=activeKey.lastOrNull{it !in lastFusions}
        if(unlocked!=null){rewards=false;catalog=false;selected=comboBase+unlocked}
        lastFusions=activeKey
    }
    BackHandler(enabled=selected>=0||pending&&rewards||catalog){if(catalog)catalog=false else if(selected>=0)selected=-1 else rewards=false}
    Column(Modifier.fillMaxSize()){
        Row(Modifier.fillMaxWidth().padding(bottom=4.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(6.dp)){
            Text("Frame · ${shop.frame.label}",Type.footnote,c.ink2,Modifier.weight(1f),maxLines=1)
            if(pending)MarketButton(if(rewards)"Perk map" else "Choose reward",{rewards=!rewards;selected=-1;catalog=false})
            MarketButton(if(catalog)"Rounds" else if(shop.devPreview)"Dev perks" else "Other rewards",{catalog=!catalog;rewards=false;selected=-1})
            Text("${activeFusions.size} fusion"+if(activeFusions.size==1)""else "s",Type.footnote,c.ink2)
        }
        // Three independent pools, explicit stage and selection status at all times.
        Row(Modifier.fillMaxWidth().padding(bottom=4.dp),horizontalArrangement=Arrangement.spacedBy(5.dp)){
            for((i,stage) in PerkRounds.stages.withIndex()){
                val chosen=PerkRounds.choice(shop.rewardRounds,i)
                val available=stage==shop.stage&&pending&&shop.perkOffers.any{Perks.isRunPerk(it.id)}
                val done=chosen>=0
                val tint=if(done)c.accent else if(available)c.coin else c.ink2
                val shape=RoundedCornerShape(7.dp)
                Row(Modifier.weight(1f).clip(shape).background(if(available)tint.copy(alpha=.12f)else c.surface)
                    .border(1.dp,tint.copy(alpha=if(available||done).85f else .3f),shape)
                    .clickable{rewards=false;catalog=false;selected=PerkTimeline.ROUND_BASE+i}.padding(horizontal=8.dp,vertical=5.dp),
                    horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){
                    Text("ROUND ${i+1}",Type.subhead,tint)
                    Text(when{done->"✓ CHOSEN";available->"CHOOSE NOW";shop.stage<stage->"LOCKED · $stage";else->"PASSED · $stage"},Type.caption,tint)
                }
            }
        }
        if(!pending&&activeFusions.isNotEmpty()){
            val fusion=activeFusions.maxBy{it.inputs.size}
            val shape=RoundedCornerShape(7.dp)
            Row(Modifier.fillMaxWidth().padding(bottom=4.dp).clip(shape).background(c.coin.copy(alpha=.15f)).border(1.dp,c.coin,shape)
                .clickable{selected=comboBase+fusion.ordinal;catalog=false}.padding(horizontal=10.dp,vertical=6.dp),
                horizontalArrangement=Arrangement.spacedBy(8.dp),verticalAlignment=Alignment.CenterVertically){
                Text("✓ FUSION UNLOCKED",Type.subhead,c.coin)
                Text(fusion.title,Type.headline,c.ink,Modifier.weight(1f))
                Text("${fusion.inputs.size}-ROUND COMBO",Type.caption,c.coin)
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
                                val combos=PerkCombo.entries.filter{offer.id in it.inputs}
                                for((j,combo) in combos.withIndex()){
                                    val side=if(j%2==0)1f else -1f
                                    perkSigil(combo.ordinal,center+Offset(size.width*.23f*side,0f),size.minDimension*.17f,c.perk.copy(alpha=.14f))
                                }
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
                for((i,stage) in PerkRounds.stages.withIndex()){
                    val perk=PerkRounds.choice(shop.rewardRounds,i)
                    val valid=Perks.isRunPerk(perk)
                    val (x,y)=PerkTimeline.roundPoint(i)
                    val active=valid&&shop.perkMask and (1 shl perk)!=0
                    val current=stage==shop.stage&&pending
                    val tint=if(active)c.perk else if(current)c.coin else c.ink2
                    val sigils=if(valid)PerkCombo.entries.filter{perk in it.inputs}.map{it.ordinal}else emptyList()
                    add(MarketItem(PerkTimeline.ROUND_BASE+i,x,y,
                        if(valid)MarketIcon.perk(perk) else MarketIcon.LINK,tint,
                        if(valid)Perks.of(perk).name else if(current)"Choose reward"else null,
                        if(valid)"Round ${i+1} · stage $stage"+if(active)""else " · lost" else null,
                        true,active,current,sigils=sigils,centerText=if(valid)null else "$stage"))
                }
                for(combo in activeFusions){
                    val (x,y)=PerkTimeline.fusionPoint(combo.ordinal)
                    add(MarketItem(comboBase+combo.ordinal,x,y,MarketIcon.LINK,c.coin,combo.title,"${combo.inputs.size}-ROUND FUSION",true,true,true,sigils=listOf(combo.ordinal),fusion=true))
                }
            }
            val edges=PerkTimeline.links(shop.rewardRounds,shop.perkMask)
            MarketCanvas(items,edges,selected,{selected=it},tag="perk-map",labels=emptyList(),
                footer="Three exclusive perk rounds · Matching faint symbols are hidden clues",frame=shop.frame,character=shop.character,look=shop.look)
            if(catalog){
                Column(Modifier.align(Alignment.CenterEnd).fillMaxHeight().widthIn(max=330.dp).fillMaxWidth(.48f).background(c.ground,RoundedCornerShape(8.dp)).padding(8.dp)){
                    Text(if(shop.devPreview)"Developer perk controls" else "Other recorded rewards",Type.headline,c.ink)
                    Column(Modifier.weight(1f).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(5.dp)){
                        if(!shop.devPreview){
                            for((i,id) in shop.rewardRounds.withIndex())if(RewardRounds.valid(id)&&!Perks.isRunPerk(id)){
                                Text("Stage ${RewardRounds.stage(i)} · ${TankEvolution.rewardName(id)}",Type.subhead,c.ink)
                            }
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
                val round=(selected-PerkTimeline.ROUND_BASE).takeIf{it in PerkRounds.stages.indices}
                val perk=round?.let{PerkRounds.choice(shop.rewardRounds,it)}?:-1
                val mod=if(maxWidth>=580.dp)Modifier.align(Alignment.CenterEnd).width(310.dp).fillMaxHeight(.96f) else Modifier.align(Alignment.BottomCenter).fillMaxWidth().fillMaxHeight(.8f)
                if(combo!=null)InspectPanel(mod,combo.title,"${combo.inputs.size}-round fusion",MarketIcon.LINK,c.coin,{selected=-1},body={
                    Text("✓ FUSION UNLOCKED",Type.headline,c.coin)
                    Text(combo.effect,Type.headline,c.ink)
                    Text("Discovered from",Type.caption,c.ink2)
                    for(id in combo.inputs)Text("Round ${PerkRounds.roundOf(id)+1} · ${Perks.of(id).name}",Type.subhead,c.ink)
                    Text("Original perks stay active. This is an extra ability.",Type.footnote,c.ink2)
                },actions={Text("ACTIVE · automatic",Type.subhead,c.accent)})
                else if(round!=null){
                    val stage=PerkRounds.stages[round]
                    val valid=Perks.isRunPerk(perk)
                    val own=valid&&shop.perkMask and (1 shl perk)!=0
                    InspectPanel(mod,if(valid)Perks.of(perk).name else "Round ${round+1}","Perk choice · stage $stage",
                        if(valid)MarketIcon.perk(perk)else MarketIcon.LINK,if(own)c.perk else c.coin,{selected=-1},body={
                            if(valid){
                                Text(Perks.of(perk).blurb,Type.subhead,c.ink)
                                for(fusion in PerkCombo.entries.filter{perk in it.inputs}){
                                    Canvas(Modifier.fillMaxWidth().height(24.dp)){perkSigil(fusion.ordinal,center,size.minDimension*.3f,c.perk.copy(alpha=.24f))}
                                    Text(if(fusion.active(shop.perkMask))"Fused into ${fusion.title}" else fusion.hint,Type.footnote,c.ink2)
                                }
                            }else{
                                PurchaseRequirement("Stage","${shop.stage} / $stage",shop.stage>=stage)
                                Text(if(stage>shop.stage)"Defeat stage $stage to choose one of three rolled perks." else if(stage==shop.stage&&pending)"A reward is waiting for you." else "This choice was not recorded in your save.",Type.subhead,c.ink2)
                                Text("Round ${round+1} pool · 6 exclusive perks",Type.caption,c.ink2)
                                for(id in PerkRounds.pools[round])Text(Perks.of(id).name,Type.footnote,c.ink)
                            }
                        },actions={
                            if(stage==shop.stage&&pending&&!valid)PrimaryButton("Choose reward",{rewards=true;selected=-1},Modifier.fillMaxWidth())
                            else if(valid&&shop.devPreview)SecondaryButton(if(own)"DEV · Remove" else "DEV · Grant",{onDevPerk(perk,!own)},Modifier.fillMaxWidth())
                            else if(valid)Text(if(own)"Recorded choice" else "No longer active",Type.caption,c.ink2)
                        })
                }
            }
        }
    }
}

@Composable
private fun MarketCanvas(items:List<MarketItem>,links:List<MapLink>,selected:Int,inspect:(Int)->Unit,tag:String,labels:List<Triple<String,Float,Float>>,footer:String,frame:TankEvolution,character:CharacterDef,look:TankLook,extent:Float=UpgradeMap.WIDTH){
    val c=Theme.colors;var zoom by rememberSaveable(tag){mutableFloatStateOf(1f)}
    var px by rememberSaveable(tag){mutableFloatStateOf(0f)};var py by rememberSaveable(tag){mutableFloatStateOf(0f)}
    var viewport by remember{mutableStateOf(IntSize.Zero)}
    val liveItems by rememberUpdatedState(items);val callback by rememberUpdatedState(inspect);val liveExtent by rememberUpdatedState(extent)
    val camera=MarketCamera(zoom,px,py,liveExtent)
    val paint=remember{Paint(Paint.ANTI_ALIAS_FLAG).apply{textAlign=Paint.Align.CENTER;typeface=Typeface.DEFAULT_BOLD}}
    val art=remember{TankArt()}
    fun update(next:MarketCamera){zoom=next.zoom;px=next.x;py=next.y}
    fun resize(factor:Float){
        val w=viewport.width.toFloat();val h=viewport.height.toFloat()
        if(w>0f&&h>0f)update(MarketCamera(zoom,px,py,liveExtent).gesture(w,h,w/2,h/2,0f,0f,factor))
    }
    Box(Modifier.fillMaxSize()){
        Canvas(Modifier.fillMaxSize().onSizeChanged{viewport=it}.testTag(tag)
            .semantics{contentDescription=footer;customActions=items.filter{it.selectable}.map{n->
                CustomAccessibilityAction("Inspect "+(n.visibleLabel?:if(tag=="upgrade-map")Upgrades.of(n.id).name else "node")){callback(n.id);true}
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
                    val reach=if(closest.ranks.isEmpty())44f else UpgradeMap.NODE_RADIUS
                    if(closest.selectable&&distance(closest)<=max(9.dp.toPx(),reach*cam.scale(w,h)))callback(closest.id) else callback(-1)
                }
            }}) {
            val w=size.width;val h=size.height;val scale=camera.scale(w,h)
            fun pos(n:MarketItem)=Offset(camera.sx(n.x,w,h),camera.sy(n.y,w,h))
            val center=Offset(camera.sx(0f,w,h),camera.sy(0f,w,h))
            val step=(80f*scale).coerceAtLeast(12f)
            var gx=((center.x%step)+step)%step
            while(gx<w){drawLine(c.ink.copy(alpha=.035f),Offset(gx,0f),Offset(gx,h));gx+=step}
            var gy=((center.y%step)+step)%step
            while(gy<h){drawLine(c.ink.copy(alpha=.035f),Offset(0f,gy),Offset(w,gy));gy+=step}
            val rings=if(tag=="perk-map")emptyList()else (1..(items.maxOfOrNull{it.ranks.size}?:1)).map{UpgradeMap.FIRST_RADIUS+(it-1)*UpgradeMap.RANK_STEP}
            for(r in rings)drawCircle(c.ink.copy(alpha=.065f),r*scale,center,style=Stroke(1.dp.toPx()))
            val byId=items.associateBy{it.id}
            for(edge in links){
                val a=byId[edge.a];val b=byId[edge.b]?:continue
                if(a==null&&edge.a!=UpgradeMap.HUB_ID)continue
                val active=(a?.owned?:true)&&b.owned
                val lit=(a?.lit==true)||b.lit
                if(tag=="perk-map"&&active)drawLine(c.coin.copy(alpha=.18f),a?.let{pos(it)}?:center,pos(b),strokeWidth=6.dp.toPx())
                drawLine(if(active)c.coin else if(lit)b.tint.copy(alpha=.48f) else c.ink.copy(alpha=if(a?.owned==true||b.owned).25f else .12f),
                    a?.let{pos(it)}?:center,pos(b),strokeWidth=if(active&&tag=="perk-map")2.8.dp.toPx() else if(active||lit)1.8.dp.toPx() else 1.dp.toPx())
            }
            // Repeated low-contrast three-glyph sequences are a visual clue, not labels.
            if(tag=="perk-map")for(n in items.filter{it.sigils.isNotEmpty()}){
                val at=pos(n);val delta=at-center;val len=delta.getDistance().coerceAtLeast(1f)
                val dir=delta/len;val tangent=Offset(-dir.y,dir.x)
                for((i,sigil) in n.sigils.withIndex()){
                    val mark=at+dir*(60f*scale).coerceAtLeast(17.dp.toPx())+tangent*((i-(n.sigils.size-1)*.5f)*24.dp.toPx())
                    perkSigil(sigil,mark,(12f*scale).coerceIn(3.dp.toPx(),11.dp.toPx()),c.perk.copy(alpha=if(n.owned).25f else .13f))
                }
            }
            // At overview, use the horizontal margins for clean text callouts.
            // No text label can cover the tank or another node in this mode.
            val edgeRadius=if(tag=="upgrade-map")extent*.5f-55f else 487f
            val leftX=center.x-edgeRadius*scale-22.dp.toPx()
            val rightX=center.x+edgeRadius*scale+22.dp.toPx()
            val outside=zoom<=1.15f&&leftX>90.dp.toPx()&&w-rightX>90.dp.toPx()
            val lineGap=29.dp.toPx()
            val top=28.dp.toPx();val bottom=h-30.dp.toPx()
            val capacity=((bottom-top)/lineGap).toInt().coerceAtLeast(1)+1
            val candidates=items.filter{it.visibleLabel!=null&&(it.lit||it.owned||it.id==selected||(it.id>=100&&it.caption!="0/2"))}
                .filter{val a=pos(it);a.x in 0f..w&&a.y in 0f..h}
                .sortedByDescending{(if(it.id==selected)8 else 0)+(if(it.lit)4 else 0)+(if(it.owned)2 else 0)}
            val callouts=mutableListOf<MarketLabel>()
            if(outside){
                val left=mutableListOf<MarketItem>();val right=mutableListOf<MarketItem>()
                for(n in candidates){
                    val dx=pos(n).x-center.x
                    val wantRight=if(abs(dx)<3.dp.toPx())right.size<=left.size else dx>0f
                    val preferred=if(wantRight)right else left
                    val other=if(wantRight)left else right
                    if(preferred.size<capacity)preferred.add(n) else if(other.size<capacity)other.add(n)
                }
                for((side,group) in listOf(false to left,true to right)){
                    val sorted=group.sortedBy{pos(it).y}
                    val ys=FloatArray(sorted.size){i->pos(sorted[i]).y.coerceIn(top,maxOf(top,bottom))}
                    for(i in 1 until ys.size)ys[i]=maxOf(ys[i],ys[i-1]+lineGap)
                    if(ys.isNotEmpty()){
                        ys[ys.lastIndex]=minOf(ys.last(),bottom)
                        for(i in ys.lastIndex-1 downTo 0)ys[i]=minOf(ys[i],ys[i+1]-lineGap)
                    }
                    for((i,n) in sorted.withIndex()){
                        val x=if(side)rightX else leftX
                        callouts.add(MarketLabel(n,x,ys[i],side))
                        val at=pos(n)
                        val end=Offset(x+if(side)-7.dp.toPx() else 7.dp.toPx(),ys[i]-4.dp.toPx())
                        val elbow=Offset(center.x+if(side)(edgeRadius-24f)*scale else -(edgeRadius-24f)*scale,ys[i]-4.dp.toPx())
                        val stroke=1.dp.toPx()
                        drawLine(n.tint.copy(alpha=.25f),at,elbow,stroke)
                        drawLine(n.tint.copy(alpha=.55f),elbow,end,stroke)
                    }
                }
            }
            treeTank(art,center,(78f*scale).coerceIn(13.dp.toPx(),48.dp.toPx()),character,look,frame,c.ink,c.ground,c.accent)
            for((label,x,y) in labels){
                val at=Offset(camera.sx(x,w,h),camera.sy(y,w,h));paint.color=c.ink2.toArgb();paint.textSize=10.dp.toPx()
                if(at.x in 0f..w&&at.y in 0f..h)drawContext.canvas.nativeCanvas.drawText(label,at.x,at.y,paint)
            }
            for(n in items){
                val at=pos(n)
                val radius=if(n.ranks.isNotEmpty())UpgradeMap.NODE_RADIUS*scale else 48f*scale
                val strong=n.lit||n.owned||n.id==selected
                if(n.ranks.isNotEmpty()){
                    // Every rank, including the first, uses exactly the same radius.
                    // The chain expands into empty space, not smaller satellite dots.
                    var previous:Offset?=null
                    for(cell in n.ranks){
                        val p=Offset(camera.sx(cell.x,w,h),camera.sy(cell.y,w,h))
                        val bought=cell.rank<=n.rankOwned
                        val offered=cell.rank in (n.rankOwned+1)..(n.rankOwned+n.rankOffered)
                        previous?.let{q->drawLine(n.tint.copy(alpha=if(bought||offered).70f else .18f),q,p,strokeWidth=if(bought||offered)1.8.dp.toPx() else 1.dp.toPx())}
                        previous=p
                        if(p.x+radius<0||p.x-radius>w||p.y+radius<0||p.y-radius>h)continue
                        if(offered)drawCircle(n.tint.copy(alpha=.18f),radius+3.dp.toPx(),p)
                        drawCircle(if(bought)n.tint else if(offered)n.tint.copy(alpha=.18f) else c.ground,radius,p)
                        drawCircle(n.tint.copy(alpha=if(bought||offered)1f else .35f),radius,p,style=Stroke(if(offered||n.id==selected)1.9.dp.toPx() else .9.dp.toPx()))
                        marketGlyph(n.icon,p,radius*.57f,if(bought)c.ground else n.tint.copy(alpha=if(offered)1f else .50f))
                    }
                }else{
                    if(at.x+radius<0||at.x-radius>w||at.y+radius<0||at.y-radius>h)continue
                    if(n.fusion){
                        drawCircle(n.tint.copy(alpha=.10f),radius+9.dp.toPx(),at)
                        drawCircle(n.tint.copy(alpha=.20f),radius+4.dp.toPx(),at)
                    }else if(strong)drawCircle(n.tint.copy(alpha=.12f),radius+4.dp.toPx(),at)
                    drawCircle(if(n.owned&&!n.fusion)n.tint else if(n.lit)n.tint.copy(alpha=.18f) else c.ground,radius,at)
                    drawCircle(n.tint.copy(alpha=if(strong)1f else .32f),radius,at,style=Stroke(if(n.fusion||n.id==selected)2.2.dp.toPx() else 1.15.dp.toPx()))
                    if(n.centerText!=null){
                        paint.textSize=(radius*1.03f).coerceAtMost(14.dp.toPx());paint.color=n.tint.copy(alpha=if(strong)1f else .85f).toArgb()
                        drawContext.canvas.nativeCanvas.drawText(n.centerText,at.x,at.y-(paint.ascent()+paint.descent())*.5f,paint)
                    }else marketGlyph(n.icon,at,radius*.57f,if(n.owned&&!n.fusion)c.ground else n.tint.copy(alpha=if(strong)1f else .40f))
                }
                if(n.locked)drawCircle(c.coin,3.dp.toPx(),at+Offset(radius*.75f,-radius*.75f))
                if(!outside&&n.visibleLabel!=null&&(n.id==selected||zoom>=1.8f&&(n.lit||n.owned))){
                    val y=at.y+radius+11.dp.toPx()
                    if(y<h-3.dp.toPx()){
                        paint.textSize=10.dp.toPx();paint.color=(if(strong)c.ink else c.ink2).toArgb()
                        val half=paint.measureText(n.visibleLabel)*.5f
                        val x=at.x.coerceIn(half+2f,maxOf(half+2f,w-half-2f))
                        drawContext.canvas.nativeCanvas.drawText(n.visibleLabel,x,y,paint)
                        if(n.caption!=null&&y+11.dp.toPx()<h){
                            paint.textSize=9.dp.toPx();paint.color=(if(strong)n.tint else c.ink3).toArgb()
                            drawContext.canvas.nativeCanvas.drawText(n.caption,x,y+11.dp.toPx(),paint)
                        }
                    }
                }
            }
            for(label in callouts){
                val n=label.item
                val name=n.visibleLabel?:continue
                paint.textAlign=if(label.right)Paint.Align.LEFT else Paint.Align.RIGHT
                val available=(if(label.right)w-label.x else label.x)-8.dp.toPx()
                paint.textSize=11.dp.toPx()
                val measured=paint.measureText(name)
                if(measured>available)paint.textSize*=available.coerceAtLeast(1f)/measured
                paint.color=c.ink.toArgb()
                drawContext.canvas.nativeCanvas.drawText(name,label.x,label.y,paint)
                if(n.caption!=null){
                    paint.textSize=9.dp.toPx();paint.color=n.tint.toArgb()
                    drawContext.canvas.nativeCanvas.drawText(n.caption,label.x,label.y+11.dp.toPx(),paint)
                }
                paint.textAlign=Paint.Align.CENTER
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

