package dev.david.tessel.ui.screens

import android.graphics.Paint
import android.graphics.Typeface
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.david.tessel.core.content.*
import dev.david.tessel.platform.game.ShopView
import dev.david.tessel.platform.game.ShopOffer
import dev.david.tessel.ui.components.*
import dev.david.tessel.ui.theme.*
import kotlin.math.*

private fun familyColor(f:CardFamily,dark:Boolean):Color {
    val colors=if(dark)listOf(0xFFFFAE78,0xFFF19B9B,0xFFC7A6F3,0xFF88C8FF,0xFFEAC46C)
    else listOf(0xFFAE541D,0xFFAE4B58,0xFF7750A5,0xFF2868A4,0xFF93630B)
    return Color(colors[f.ordinal])
}
private fun rarityColor(t:CardRarity,normal:Color,dark:Boolean):Color=when(t){
    CardRarity.COMMON->normal
    CardRarity.UNCOMMON->Color(if(dark)0xFF7ED69A else 0xFF277E46)
    CardRarity.RARE->Color(if(dark)0xFF89B9FF else 0xFF385CB0)
    CardRarity.EPIC->Color(if(dark)0xFFD1A6F6 else 0xFF774FA8)
    CardRarity.LEGENDARY->Color(if(dark)0xFFFFD16C else 0xFFA36C06)
}
private fun upgradeGlyph(id:Int):SkillBonus=when(Upgrades.of(id).stat){
    UpgradeStat.DAMAGE,UpgradeStat.DAMAGE_BONUS,UpgradeStat.RAM,UpgradeStat.DRONE_POWER->SkillBonus.DAMAGE
    UpgradeStat.FIRE_RATE,UpgradeStat.VOLLEY->SkillBonus.FIRE_RATE
    UpgradeStat.RANGE,UpgradeStat.PIERCE,UpgradeStat.CALIBER->SkillBonus.RANGE
    UpgradeStat.SPEED,UpgradeStat.SPEED_BONUS,UpgradeStat.VELOCITY->SkillBonus.SPEED
    UpgradeStat.MAX_HP,UpgradeStat.SHIELD_RECHARGE->SkillBonus.HEALTH
    UpgradeStat.HEAL,UpgradeStat.REGEN->SkillBonus.HEAL_POWER
    UpgradeStat.WEALTH->SkillBonus.CARD_DISCOUNT
    UpgradeStat.MAGNET->SkillBonus.PICKUP
}

@Composable
fun TreeShopScreen(shop:ShopView,onBuy:(Int)->Unit,onHeal:()->Unit,onReroll:()->Unit,onNext:()->Unit,
    onPerk:(Int)->Unit,onLock:(Int,Boolean)->Unit,onSkill:(Int)->Unit,onExit:()->Unit,
    onDev:(()->Unit)?=null,onDevRank:(Int,Int)->Unit={_,_->},onDevOffer:(Int,Int,CardRarity)->Unit={_,_,_->},
    onDevPerk:(Int)->Unit={},onDevSkill:(Int)->Unit={},onDevFrame:(TankEvolution)->Unit={}){
    val c=Theme.colors
    var tab by rememberSaveable(shop.stage){mutableIntStateOf(if(shop.perkOffers.isNotEmpty())2 else 0)}
    var exit by remember{mutableStateOf(false)}
    var inspect by rememberSaveable(shop.stage){mutableIntStateOf(-1)}
    val pending=shop.perkOffers.isNotEmpty()
    val dev=shop.devTrial&&onDev!=null
    LaunchedEffect(shop.stage,pending){if(pending)tab=2}
    BackHandler(enabled=tab!=1){if(inspect>=0){inspect=-1}else if(tab!=0){tab=0}else exit=!exit}
    Column(Modifier.fillMaxSize().background(c.ground).windowInsetsPadding(WindowInsets.displayCutout).padding(horizontal=Space.x2)){
        Row(Modifier.fillMaxWidth().padding(vertical=4.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)){
            BackButton("Exit",{exit=true})
            Column(Modifier.weight(1f)){
                Text("Stage ${shop.stage} cleared",Type.headline,c.ink)
                Text("HP ${shop.hp}/${shop.maxHp}"+if(dev)" · TEST RUN" else if(shop.coop)" · ${shop.readyCount}/${shop.teamSize} ready" else "",Type.caption,c.ink2)
            }
            CoinAmount(shop.coins,style=Type.headline)
            Row(Modifier.widthIn(max=312.dp),horizontalArrangement=Arrangement.spacedBy(3.dp)){
                listOf("Upgrades","Skills","Perks").forEachIndexed{i,name->
                    Box(Modifier.clip(RoundedCornerShape(6.dp)).background(if(i==tab)c.ink else c.track)
                        .clickable{tab=i;inspect=-1}.padding(horizontal=10.dp,vertical=10.dp),contentAlignment=Alignment.Center){
                        Text(name+(if(i==2&&pending)" •" else ""),Type.subhead,if(i==tab)c.ground else c.ink)
                    }
                }
            }
        }
        if(exit){
            Row(Modifier.fillMaxWidth().padding(8.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)){
                Text(if(dev)"Discard the test run?" else if(shop.coop)"Save and stop for everyone?" else "Save and return home?",Type.subhead,c.ink,Modifier.weight(1f))
                SecondaryButton("Cancel",{exit=false})
                PrimaryButton(if(dev)"Exit trial" else "Save & Exit",onExit)
            }
        }
        Box(Modifier.weight(1f).fillMaxWidth()){
            when(tab){
                0->UpgradeTreeMap(shop,inspect,{inspect=it},onBuy,onLock,if(dev)onDevRank else null,if(dev)onDevOffer else null)
                1->SkillTreeScreen(shop.stage,shop.skillNodes,shop.coins,!pending&&!shop.selfReady,onSkill,{tab=0},backLabel="Shop",embedded=true,devToggle=if(dev)onDevSkill else null)
                else->PerkTree(shop,onPerk,if(dev)onDevPerk else null,if(dev)onDevFrame else null)
            }
        }
        Row(Modifier.fillMaxWidth().padding(vertical=4.dp),horizontalArrangement=Arrangement.spacedBy(6.dp),verticalAlignment=Alignment.CenterVertically){
            SecondaryButton("Heal +${shop.healAmount} · ${shop.healPrice}",onHeal,Modifier.weight(1f),enabled=shop.canHeal&&!shop.selfReady&&!pending)
            SecondaryButton(if(shop.rerollPrice==0)"Reroll · FREE (${shop.freeRefreshesLeft})" else "Reroll · ${shop.rerollPrice}",{inspect=-1;onReroll()},Modifier.weight(1f),enabled=shop.canReroll&&!pending&&!shop.selfReady)
            if(dev)SecondaryButton("Developer Options",onDev!!,Modifier.weight(1f))
            PrimaryButton(if(shop.selfReady)"Waiting" else if(shop.coop)"Ready" else "Next Stage",onNext,Modifier.weight(1.05f),enabled=!pending&&!shop.selfReady)
        }
    }
}

@Composable
private fun UpgradeTreeMap(shop:ShopView,selected:Int,onSelect:(Int)->Unit,onBuy:(Int)->Unit,onLock:(Int,Boolean)->Unit,
    onDevRank:((Int,Int)->Unit)?,onDevOffer:((Int,Int,CardRarity)->Unit)?){
    val c=Theme.colors;val dev=onDevRank!=null
    val offers=remember(shop){shop.offers.filter{!it.empty&&it.id>=0}.associateBy{it.id}}
    val currentOffers by rememberUpdatedState(offers)
    val currentSelect by rememberUpdatedState(onSelect)
    val nodes=UpgradeMap.nodes
    var zoom by rememberSaveable{mutableFloatStateOf(1f)}
    var panX by rememberSaveable{mutableFloatStateOf(0f)}
    var panY by rememberSaveable{mutableFloatStateOf(0f)}
    val paint=remember{Paint(Paint.ANTI_ALIAS_FLAG).apply{textAlign=Paint.Align.CENTER;typeface=Typeface.DEFAULT_BOLD}}
    var tier by rememberSaveable{mutableIntStateOf(0)}
    var slot by rememberSaveable{mutableIntStateOf(0)}
    val focused=offers[selected]
    BoxWithConstraints(Modifier.fillMaxSize().clip(RoundedCornerShape(10.dp)).background(c.surface)){
        val panelWidth=minOf(290.dp,maxWidth*.42f)
        Canvas(Modifier.fillMaxSize().testTag("upgrade-map")
            .semantics {
                contentDescription="Upgrade tree. ${offers.size} current offers. Hidden names on inactive nodes."
                customActions=nodes.filter{dev||it.id in offers}.map{n->CustomAccessibilityAction("Inspect ${Upgrades.of(n.id).name}"){onSelect(n.id);true}}
            }
            .pointerInput(Unit){detectTransformGestures{centroid,pan,factor,_->
                val old=zoom;zoom=(zoom*factor).coerceIn(1f,4f)
                val ratio=zoom/old
                panX=(centroid.x-size.width/2f-(centroid.x-size.width/2f-panX)*ratio+pan.x).coerceIn(-size.width*zoom*.5f,size.width*zoom*.5f)
                panY=(centroid.y-size.height/2f-(centroid.y-size.height/2f-panY)*ratio+pan.y).coerceIn(-size.height*zoom*.5f,size.height*zoom*.5f)
            }}
            .pointerInput(dev){detectTapGestures{at->
                val scale=min(size.width/UpgradeMap.WIDTH,size.height/UpgradeMap.HEIGHT)*zoom
                var best=26.dp.toPx().pow(2);var hit=-1
                for(n in nodes){
                    val dx=size.width/2f+panX+n.x*scale-at.x;val dy=size.height/2f+panY+n.y*scale-at.y
                    val d=dx*dx+dy*dy;if(d<best){best=d;hit=n.id}
                }
                currentSelect(if(dev||hit in currentOffers)hit else -1)
            }}){
            val w=size.width;val h=size.height;val scale=min(w/UpgradeMap.WIDTH,h/UpgradeMap.HEIGHT)*zoom
            fun point(x:Float,y:Float)=Offset(w*.5f+panX+x*scale,h*.5f+panY+y*scale)
            val hub=point(0f,0f)
            val step=(60f*scale).coerceAtLeast(12f)
            var gx=((hub.x%step)+step)%step
            while(gx<w){drawLine(c.ink.copy(alpha=.045f),Offset(gx,0f),Offset(gx,h));gx+=step}
            var gy=((hub.y%step)+step)%step
            while(gy<h){drawLine(c.ink.copy(alpha=.045f),Offset(0f,gy),Offset(w,gy));gy+=step}
            for(f in UpgradeMap.groups){
                val (x,y)=UpgradeMap.anchor(f);val p=point(x,y);val tint=familyColor(f,c.dark)
                drawLine(tint.copy(alpha=.3f),hub,p,1.3.dp.toPx());drawCircle(tint.copy(alpha=.65f),4.dp.toPx(),p)
            }
            for(n in nodes){
                val f=Upgrades.of(n.id).family;val (x,y)=UpgradeMap.anchor(f)
                val parent=nodes.firstOrNull{it.id==n.parent}
                val tint=familyColor(f,c.dark);val active=offers[n.id]
                drawLine(tint.copy(alpha=if(active!=null).85f else .20f),if(parent==null)point(x,y) else point(parent.x,parent.y),point(n.x,n.y),if(active!=null)2.dp.toPx() else 1.dp.toPx())
            }
            drawCircle(c.ground,13.dp.toPx(),hub);drawCircle(c.ink2,13.dp.toPx(),hub,style=Stroke(1.dp.toPx()));drawCircle(c.ink2,3.dp.toPx(),hub)
            for(n in nodes){
                val p=point(n.x,n.y);val o=offers[n.id];val owned=shop.upgradeRanks.getOrElse(n.id){0}
                val normal=familyColor(Upgrades.of(n.id).family,c.dark)
                val tint=if(o!=null)rarityColor(o.rarity,normal,c.dark) else normal
                val r=(22f*scale).coerceIn(9.dp.toPx(),26.dp.toPx())
                if(p.x+r<0||p.x-r>w||p.y+r<0||p.y-r>h)continue
                if(o!=null)drawCircle(tint.copy(alpha=.13f),r+6.dp.toPx(),p)
                drawCircle(if(o!=null)tint.copy(alpha=.18f) else c.ground,r,p)
                drawCircle(tint.copy(alpha=if(o!=null||selected==n.id)1f else if(owned>0).65f else .28f),r,p,style=Stroke(if(o!=null)2.dp.toPx() else 1.dp.toPx()))
                skillGlyph(upgradeGlyph(n.id),p,r*.59f,tint.copy(alpha=if(o!=null||dev).98f else .42f))
                if(owned>0){drawCircle(tint,3.dp.toPx(),p+Offset(r*.70f,-r*.70f))}
                if(o!=null){
                    paint.textSize=10.dp.toPx();paint.color=tint.toArgb()
                    val label=o.name
                    val half=paint.measureText(label)*.5f
                    val x=p.x.coerceIn(half+2f,maxOf(half+2f,w-half-2f))
                    val y=(p.y+r+paint.textSize+2f).coerceAtMost(h-2f)
                    drawContext.canvas.nativeCanvas.drawText(label,x,y,paint)
                    if(o.locked)drawCircle(c.coin,4.dp.toPx(),p+Offset(-r*.75f,-r*.75f))
                }
            }
        }
        Row(Modifier.align(Alignment.TopStart).padding(5.dp),horizontalArrangement=Arrangement.spacedBy(4.dp),verticalAlignment=Alignment.CenterVertically){
            SecondaryButton("−",{zoom=(zoom/1.3f).coerceAtLeast(1f);if(zoom==1f){panX=0f;panY=0f}})
            SecondaryButton("+",{zoom=(zoom*1.3f).coerceAtMost(4f)})
            SecondaryButton("Fit",{zoom=1f;panX=0f;panY=0f})
            Text("${(zoom*100).roundToInt()}%",Type.caption,c.ink2)
        }
        Column(Modifier.align(Alignment.BottomStart).padding(6.dp)){
            Text("${offers.size} / 3 offers · ${nodes.size} upgrade nodes",Type.caption,c.ink)
            Text(if(dev)"Developer trial · all nodes inspectable" else "Tap a lit node · inactive icons are not for sale",Type.caption,c.ink2)
        }
        if(selected in 0 until Upgrades.count&&(focused!=null||dev)){
            val d=Upgrades.of(selected)
            val rank=shop.upgradeRanks.getOrElse(selected){focused?.level?:0}
            Column(Modifier.align(Alignment.CenterEnd).padding(5.dp).width(panelWidth).fillMaxHeight(.97f)
                .clip(RoundedCornerShape(10.dp)).background(c.ground).border(1.dp,c.line,RoundedCornerShape(10.dp))
                .pointerInput(Unit){detectTapGestures{}}.padding(9.dp)){
                Row(verticalAlignment=Alignment.CenterVertically){
                    Column(Modifier.weight(1f)){
                        Text(d.name,Type.headline,c.ink)
                        if(focused!=null)Text(focused.rarity.label,Type.caption,rarityColor(focused.rarity,c.accent,c.dark))
                    }
                    SecondaryButton("×",{onSelect(-1)})
                }
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(5.dp)){
                    Text(focused?.blurb?:d.blurb,Type.subhead,c.ink)
                    Text("Rank $rank / ${Upgrades.runCap(selected)}",Type.footnote,c.ink2)
                    if(focused!=null){
                        Text("+${focused.levels} rank(s) · ${focused.price} coins",Type.footnote,c.ink2)
                        SecondaryButton(if(focused.locked)"Unlock offer" else "Lock offer",{onLock(focused.slot,!focused.locked)},Modifier.fillMaxWidth(),enabled=!shop.selfReady&&!focused.maxed)
                    }else Text("Not offered",Type.caption,c.ink2)
                    if(dev){
                        Row(horizontalArrangement=Arrangement.spacedBy(4.dp)){
                            SecondaryButton("−1",{onDevRank!!(selected,rank-1)},Modifier.weight(1f))
                            SecondaryButton("+1",{onDevRank!!(selected,rank+1)},Modifier.weight(1f))
                            SecondaryButton("Max",{onDevRank!!(selected,d.maxLevel)},Modifier.weight(1f))
                        }
                        val mission=CardMission.forUpgrade(selected)
                        val rarities=if(mission!=null)listOf(mission.rarity) else CardRarity.entries
                        val chosen=rarities[tier.coerceIn(0,rarities.lastIndex)]
                        SecondaryButton("Rarity · ${chosen.label}",{tier=(tier+1)%rarities.size},Modifier.fillMaxWidth())
                        SecondaryButton("Slot ${slot+1} · Force offer",{onDevOffer!!(selected,slot,chosen);slot=(slot+1)%3},Modifier.fillMaxWidth(),
                            enabled=Upgrades.availableAt(selected,shop.stage)&&Upgrades.offered(selected,shop.frame))
                    }
                }
                if(focused!=null)PrimaryButton("Buy · ${focused.price}",{onBuy(focused.slot);onSelect(-1)},Modifier.fillMaxWidth(),enabled=focused.affordable&&!shop.selfReady&&!focused.maxed&&shop.perkOffers.isEmpty())
            }
        }
    }
}

@Composable
private fun PerkTree(shop:ShopView,onReward:(Int)->Unit,onDevPerk:((Int)->Unit)?,onDevFrame:((TankEvolution)->Unit)?){
    val c=Theme.colors
    var selected by rememberSaveable{mutableIntStateOf(-1)}
    var combo by rememberSaveable{mutableIntStateOf(-1)}
    var frames by remember{mutableStateOf(false)}
    fun owned(id:Int)=shop.perkMask and (1 shl id)!=0
    Column(Modifier.fillMaxSize().clip(RoundedCornerShape(10.dp)).background(c.surface).padding(6.dp)){
        if(shop.perkOffers.isNotEmpty()){
            Text(if(shop.perkOffers.any{it.evolution!=null})"Choose your evolution" else "Choose one boss perk",Type.headline,c.ink)
            LazyRow(horizontalArrangement=Arrangement.spacedBy(7.dp),modifier=Modifier.fillMaxWidth().heightIn(max=126.dp)){
                items(shop.perkOffers,key={it.slot}){o->
                    Column(Modifier.width(180.dp).clip(RoundedCornerShape(8.dp)).border(1.dp,c.perk,RoundedCornerShape(8.dp)).background(c.ground)
                        .clickable(enabled=!shop.selfReady){onReward(o.slot)}.padding(7.dp)){
                        if(o.evolution!=null)TankPreview(CharacterDef.BALANCED,TankLook(),Modifier.fillMaxWidth().height(28.dp),o.evolution)
                        Text(o.name,Type.subhead,c.ink,maxLines=2)
                        Text(o.blurb,Type.caption,c.ink2,maxLines=3)
                        Text(if(o.evolution!=null)"Evolve" else "Take",Type.caption,c.accent)
                    }
                }
            }
        }else Row(verticalAlignment=Alignment.CenterVertically){
            Text(shop.frame.label,Type.headline,c.ink,Modifier.weight(1f))
            Text("${PerkCombo.entries.count{it.active(shop.perkMask)}} / ${PerkCombo.entries.size} combos active",Type.caption,c.ink2)
        }
        if(onDevFrame!=null){
            SecondaryButton(if(frames)"Close frame selector" else "DEV · Select any tank",{frames=!frames})
            if(frames)LazyRow(horizontalArrangement=Arrangement.spacedBy(4.dp)){items(TankEvolution.entries){f->SecondaryButton(f.label,{onDevFrame(f);frames=false})}}
        }
        if(!frames)Row(Modifier.weight(1f).fillMaxWidth().padding(top=5.dp),horizontalArrangement=Arrangement.spacedBy(5.dp)){
            for((i,pair) in PerkCombo.entries.withIndex()){
                val active=pair.active(shop.perkMask)
                Column(Modifier.weight(1f).fillMaxHeight(),horizontalAlignment=Alignment.CenterHorizontally){
                    for(id in listOf(pair.first,pair.second)){
                        val own=owned(id)
                        Box(Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(if(own)c.perk.copy(alpha=.22f) else c.ground)
                            .border(1.dp,if(own)c.perk else c.line,RoundedCornerShape(8.dp))
                            .clickable{selected=id;combo=-1}.padding(4.dp),contentAlignment=Alignment.Center){
                            Text(Perks.of(id).name,Type.footnote,if(own)c.perk else c.ink2,align=androidx.compose.ui.text.style.TextAlign.Center,maxLines=2)
                        }
                        Box(Modifier.width(1.dp).height(5.dp).background(if(own)c.perk else c.line))
                    }
                    Box(Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(if(active)c.perk else c.ground)
                        .border(1.dp,if(active)c.perk else c.line,RoundedCornerShape(8.dp)).clickable{combo=i;selected=-1}.padding(4.dp),contentAlignment=Alignment.Center){
                        Text(if(active)pair.title else "◇  Combo",Type.footnote,if(active)c.ground else c.ink2,align=androidx.compose.ui.text.style.TextAlign.Center,maxLines=2)
                    }
                }
            }
        }
        val p=PerkCombo.entries.getOrNull(combo)
        if(selected>=0||p!=null){
            Row(Modifier.fillMaxWidth().padding(top=5.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(6.dp)){
                Column(Modifier.weight(1f)){
                    if(p!=null){
                        Text(if(p.active(shop.perkMask))p.title else "Synergy hint",Type.subhead,c.ink)
                        Text(if(p.active(shop.perkMask))p.effect else p.hint,Type.caption,c.ink2,maxLines=2)
                    }else{
                        Text(Perks.of(selected).name,Type.subhead,c.ink)
                        Text(Perks.of(selected).blurb,Type.caption,c.ink2,maxLines=2)
                    }
                }
                if(onDevPerk!=null&&selected>=0)SecondaryButton(if(owned(selected))"DEV Remove" else "DEV Grant",{onDevPerk(selected)})
                SecondaryButton("×",{selected=-1;combo=-1})
            }
        }
    }
}
