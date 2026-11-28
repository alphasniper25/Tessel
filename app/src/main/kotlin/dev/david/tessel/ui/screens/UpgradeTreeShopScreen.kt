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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import dev.david.tessel.core.content.*
import dev.david.tessel.core.sim.UpgradeBuild
import dev.david.tessel.platform.game.ShopView
import dev.david.tessel.ui.components.*
import dev.david.tessel.ui.theme.*
import kotlinx.coroutines.delay
import kotlin.math.*

private fun upgradeTint(s:UpgradeSector,dark:Boolean):Color {
    val colors=if(dark)listOf(0xFFFFAE78,0xFFF19B9B,0xFF88C8FF,0xFFEAC46C,0xFFC7A6F3,0xFF5FD5BE,0xFFE79572)
        else listOf(0xFFAE541D,0xFFAE4B58,0xFF2868A4,0xFF93630B,0xFF7750A5,0xFF087E70,0xFFA6472D)
    return Color(colors[s.ordinal])
}
private fun glyph(n:UpgradeNode)=when(Upgrades.of(n.upgrade).stat){
    UpgradeStat.DAMAGE,UpgradeStat.DAMAGE_BONUS,UpgradeStat.RAM,UpgradeStat.DRONE_POWER,UpgradeStat.CALIBER->SkillBonus.DAMAGE
    UpgradeStat.FIRE_RATE,UpgradeStat.VOLLEY,UpgradeStat.SHIELD_RECHARGE->SkillBonus.FIRE_RATE
    UpgradeStat.RANGE,UpgradeStat.PIERCE,UpgradeStat.VELOCITY->SkillBonus.RANGE
    UpgradeStat.SPEED,UpgradeStat.SPEED_BONUS->SkillBonus.SPEED
    UpgradeStat.MAX_HP->SkillBonus.HEALTH
    UpgradeStat.WEALTH->SkillBonus.CARD_DISCOUNT
    UpgradeStat.MAGNET->SkillBonus.PICKUP
    UpgradeStat.HEAL,UpgradeStat.REGEN->SkillBonus.HEAL_POWER
}

/** The main intermission. Same composable is used for read-only home/pause inspection.
 * All purchases and evolutions are commands; the UI waits for the authoritative returned build. */
@Composable
fun UpgradeTreeShopScreen(stage:Int,build:UpgradeBuild,coins:Int,shop:ShopView?=null,
    backLabel:String="Home",infinite:Boolean=false,onBack:()->Unit={},onBuy:(Int)->Unit={},
    onPerk:(Int)->Unit={},onHeal:()->Unit={},onNext:()->Unit={},onSave:()->Unit={}){
    val c=Theme.colors
    val layout=remember(build.frame,build.lastEvolution){UpgradeMapLayout(build.frame,build.lastEvolution)}
    var camera by remember(layout){mutableStateOf(UpgradeMapCamera())}
    var selected by rememberSaveable{mutableIntStateOf(-1)}
    var specs by rememberSaveable{mutableStateOf(false)}
    var confirmExit by rememberSaveable{mutableStateOf(false)}
    var viewport by remember{mutableStateOf(IntSize.Zero)}
    var pending by remember{mutableIntStateOf(-1)}
    var rewardPending by remember{mutableIntStateOf(-1)}
    val currentBuild by rememberUpdatedState(build)
    val currentLayout by rememberUpdatedState(layout)
    val hasReward=shop?.perkOffers?.isNotEmpty()==true
    val ready=shop?.selfReady==true
    val canSpend=shop!=null&&!ready&&!hasReward
    val points=layout.points
    val states=remember(build.levels,layout){points.associate{it.node.id to UpgradeTree.discovery(it.node,build.levels)}}
    val owned=states.count{it.value==UpgradeDiscovery.OWNED}
    val discovered=states.count{it.value!=UpgradeDiscovery.HIDDEN}
    val paint=remember{Paint(Paint.ANTI_ALIAS_FLAG).apply{typeface=Typeface.DEFAULT_BOLD}}
    val selectedNode=UpgradeTree.nodes.getOrNull(selected)?.takeIf{layout.point(it.id)!=null&&states[it.id]!=UpgradeDiscovery.HIDDEN}
    fun closeOrBack(){when{confirmExit->confirmExit=false;specs->specs=false;selected>=0->selected=-1;shop!=null->confirmExit=true;else->onBack()}}
    fun resize(factor:Float){
        val w=viewport.width.toFloat();val h=viewport.height.toFloat()
        if(w>0f&&h>0f)camera=camera.gesture(layout,w,h,w/2,h/2,0f,0f,factor)
    }
    fun select(id:Int){if(currentLayout.point(id)!=null&&UpgradeTree.discovery(UpgradeTree.nodes[id],currentBuild.levels)!=UpgradeDiscovery.HIDDEN){selected=id;specs=false}}
    fun jump(sector:UpgradeSector){
        val p=points.firstOrNull{UpgradeTree.sector(it.node,build.frame)==sector}?:return
        if(viewport.width>0&&viewport.height>0)camera=camera.focus(layout,viewport.width.toFloat(),viewport.height.toFloat(),p)
    }
    LaunchedEffect(layout){selected=-1;specs=false}
    LaunchedEffect(build.levels,pending){
        if(pending>=0){val n=UpgradeTree.nodes[pending];if(build.levels[n.upgrade]<n.rank)delay(1600L);pending=-1}
    }
    LaunchedEffect(shop?.perkOffers?.map{it.id},rewardPending){
        if(rewardPending>=0){if(hasReward)delay(1600L);rewardPending=-1}
    }
    BackHandler{closeOrBack()}
    Column(Modifier.fillMaxSize().background(c.ground).windowInsetsPadding(WindowInsets.displayCutout).padding(horizontal=Space.x2)){
        Row(Modifier.fillMaxWidth().height(48.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(6.dp)){
            BackButton(if(shop!=null)"Exit" else backLabel,{if(shop!=null)confirmExit=true else onBack()})
            Text(if(shop!=null)"Stage $stage" else build.frame.label,Type.subhead,c.ink,Modifier.weight(1f),maxLines=1)
            if(infinite)Text("∞",Type.headline,c.coin) else if(shop!=null||backLabel!="Home")CoinAmount(coins,style=Type.headline)
            SecondaryButton("Specs",{specs=!specs;selected=-1})
            if(shop!=null)PrimaryButton(if(ready)"Waiting" else if(shop.coop)"Ready" else "Next Stage",onNext,enabled=!hasReward&&!ready)
        }
        Row(Modifier.fillMaxWidth().heightIn(min=38.dp).horizontalScroll(rememberScrollState()),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(7.dp)){
            Text("$owned / ${points.size} ranks",Type.caption,c.ink2,Modifier.padding(horizontal=4.dp))
            if(shop!=null)MapControl("Repair +${shop.healAmount} · ${shop.healPrice}",onHeal,shop.canHeal&&!ready&&!hasReward)
            for(region in layout.regions)MapControl(region.sector.title,{jump(region.sector)},true)
        }
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth().padding(bottom=4.dp).clip(RoundedCornerShape(10.dp)).background(c.surface)){
            val wide=maxWidth>=580.dp
            Canvas(Modifier.fillMaxSize().onSizeChanged{viewport=it}.testTag("upgrade-map")
                .semantics{
                    contentDescription="Upgrade map. $discovered discovered of ${points.size}. Pinch to zoom. Drag to pan."
                    customActions=points.filter{states[it.node.id]!=UpgradeDiscovery.HIDDEN}.map{p->
                        CustomAccessibilityAction("Inspect ${UpgradeTree.title(p.node,build.frame)} rank ${p.node.rank}"){select(p.node.id);true}
                    }
                }
                .pointerInput(Unit){detectTransformGestures{centroid,pan,factor,_->
                    camera=camera.gesture(currentLayout,size.width.toFloat(),size.height.toFloat(),centroid.x,centroid.y,pan.x,pan.y,factor)
                }}
                .pointerInput(Unit){detectTapGestures{at->
                    val id=camera.hit(currentLayout,size.width.toFloat(),size.height.toFloat(),at.x,at.y,12.dp.toPx(),currentBuild.levels)
                    if(id!=null)select(id) else selected=-1
                }}){
                val w=size.width;val h=size.height;val scale=camera.scale(layout,w,h)
                fun pos(x:Float,y:Float)=Offset(camera.sx(layout,w,h,x),camera.sy(layout,w,h,y))
                fun pos(p:PlacedUpgrade)=pos(p.x,p.y)
                val center=pos(0f,0f);val step=(80f*scale).coerceAtLeast(14.dp.toPx())
                var gx=((center.x%step)+step)%step
                while(gx<w){drawLine(c.ink.copy(alpha=.035f),Offset(gx,0f),Offset(gx,h));gx+=step}
                var gy=((center.y%step)+step)%step
                while(gy<h){drawLine(c.ink.copy(alpha=.035f),Offset(0f,gy),Offset(w,gy));gy+=step}
                for(p in points){
                    val parent=UpgradeTree.prerequisite(p.node)?.let{layout.point(it.id)}
                    if(parent!=null){
                        val a=pos(parent);val b=pos(p)
                        val own=states[p.node.id]==UpgradeDiscovery.OWNED
                        val revealed=states[p.node.id]!=UpgradeDiscovery.HIDDEN
                        val tint=if(revealed)upgradeTint(UpgradeTree.sector(p.node,build.frame),c.dark) else c.ink
                        val stroke=if(own)2.dp.toPx() else 1.dp.toPx()
                        val color=tint.copy(alpha=if(own).9f else if(revealed).55f else .10f)
                        if(parent.y==p.y)drawLine(color,a,b,stroke)
                        else {val elbow=Offset(b.x-40f*scale,a.y);val lower=Offset(elbow.x,b.y);drawLine(color,a,elbow,stroke);drawLine(color,elbow,lower,stroke);drawLine(color,lower,b,stroke)}
                    }
                }
                for(region in layout.regions){
                    val at=pos(-layout.width*.5f+35f,region.y)
                    if(at.y in 0f..h){
                        paint.color=upgradeTint(region.sector,c.dark).toArgb();paint.textAlign=Paint.Align.LEFT
                        paint.textSize=(20f*scale).coerceIn(9.dp.toPx(),15.dp.toPx())
                        drawContext.canvas.nativeCanvas.drawText(region.sector.title.uppercase(),maxOf(5f,at.x),at.y,paint)
                    }
                }
                for(label in layout.labels){
                    val root=UpgradeTree.node(label.lane.upgrade,1)!!
                    if(states[root.id]==UpgradeDiscovery.HIDDEN)continue
                    val at=pos(label.x,label.y)
                    if(at.x in 0f..w&&at.y in 0f..h&&scale*104f>=25.dp.toPx()){
                        paint.textAlign=Paint.Align.LEFT;paint.color=c.ink2.toArgb();paint.textSize=(18f*scale).coerceIn(9.dp.toPx(),13.dp.toPx())
                        drawContext.canvas.nativeCanvas.drawText(UpgradeTree.title(root,build.frame),at.x-10.dp.toPx(),at.y,paint)
                    }
                }
                for(p in points){
                    val at=pos(p);val n=p.node
                    val r=(30f*scale).coerceIn(3.2.dp.toPx(),27.dp.toPx())
                    if(at.x+r<0||at.x-r>w||at.y+r<0||at.y-r>h)continue
                    val state=states[n.id]
                    if(state==UpgradeDiscovery.HIDDEN){
                        drawCircle(c.ink.copy(alpha=.13f),r*.70f,at,style=Stroke(1.dp.toPx()))
                        drawCircle(c.ink.copy(alpha=.15f),r*.13f,at);continue
                    }
                    val own=state==UpgradeDiscovery.OWNED
                    val affordable=canSpend&&UpgradeTree.eligible(n,build.frame,build.lastEvolution,stage,build.levels)&&(infinite||coins>=UpgradeTree.price(n,stage,build.character,build.perkMask))
                    val tint=upgradeTint(UpgradeTree.sector(n,build.frame),c.dark)
                    val radius=maxOf(r,5.dp.toPx())
                    if(n.id==selected)drawCircle(tint.copy(alpha=.22f),radius+4.dp.toPx(),at)
                    drawCircle(if(own)tint else if(affordable)tint.copy(alpha=.18f) else c.ground,radius,at)
                    drawCircle(tint.copy(alpha=if(own||affordable||n.id==selected)1f else .62f),radius,at,style=Stroke(if(n.id==selected)2.dp.toPx() else 1.2.dp.toPx()))
                    skillGlyph(glyph(n),at,radius*.55f,if(own)c.ground else tint)
                    if(scale*104f>=35.dp.toPx()){
                        paint.textAlign=Paint.Align.CENTER;paint.color=c.ink2.toArgb();paint.textSize=9.dp.toPx()
                        drawContext.canvas.nativeCanvas.drawText(n.rank.toString(),at.x,at.y+radius+12.dp.toPx(),paint)
                    }
                }
            }
            Row(Modifier.align(Alignment.TopStart).padding(5.dp),horizontalArrangement=Arrangement.spacedBy(4.dp),verticalAlignment=Alignment.CenterVertically){
                MapControl("−",{resize(.72f)});MapControl("+",{resize(1.38f)});MapControl("Fit",{camera=UpgradeMapCamera()})
                Text("${(camera.zoom*100).roundToInt()}%",Type.caption,c.ink2)
            }
            Column(Modifier.align(Alignment.BottomStart).padding(6.dp)){
                Text("$discovered / ${points.size} discovered",Type.caption,c.ink2)
                Text(if(build.frame==TankEvolution.BASIC&&build.lastEvolution<5)"Specialization revealed after your stage 5 choice" else "Tap a revealed node · drag or pinch",Type.caption,c.ink3)
            }
            val panelMod=if(wide)Modifier.align(Alignment.CenterEnd).padding(5.dp).width(300.dp).fillMaxHeight(.97f)
                else Modifier.align(Alignment.BottomCenter).padding(5.dp).fillMaxWidth().fillMaxHeight(.82f)
            if(selectedNode!=null&&!specs&&!hasReward&&!confirmExit){
                val n=selectedNode
                val price=UpgradeTree.price(n,stage.coerceAtLeast(1),build.character,build.perkMask)
                val own=states[n.id]==UpgradeDiscovery.OWNED
                val eligible=UpgradeTree.eligible(n,build.frame,build.lastEvolution,stage,build.levels)
                val buy=canSpend&&eligible&&(infinite||coins>=price)&&pending<0
                MapPanel(panelMod,UpgradeTree.title(n,build.frame)+" · "+n.rank,{selected=-1}){
                    Text(UpgradeTree.effect(n,build.frame),Type.subhead,c.ink)
                    Text(when{own->"Unlocked";stage<UpgradeTree.minStage(n,build.frame)->"Locked · stage ${UpgradeTree.minStage(n,build.frame)}";shop==null->"Purchase between stages";!eligible->"Requires previous rank";!infinite&&coins<price->"Not enough coins";else->"Available"},Type.caption,c.ink2)
                    Text("Current rank ${build.levels[n.upgrade]} / ${n.lane.ranks}",Type.footnote,c.ink2)
                    if(!own)Row(verticalAlignment=Alignment.CenterVertically){Text("Cost",Type.footnote,c.ink2,Modifier.weight(1f));CoinAmount(price,style=Type.headline)}
                    PrimaryButton(if(own)"Unlocked" else if(infinite)"Unlock · ∞" else "Unlock · $price",{if(buy){pending=n.id;onBuy(n.id)}},Modifier.fillMaxWidth(),enabled=buy)
                }
            }
            if(specs&&!hasReward&&!confirmExit){
                MapPanel(panelMod,"Current build",{specs=false}){
                    Row(verticalAlignment=Alignment.CenterVertically){
                        TankPreview(build.character,build.look,Modifier.size(68.dp),build.frame)
                        Column(Modifier.weight(1f)){Text(build.frame.label,Type.title3,c.ink);Text(build.character.label,Type.caption,c.ink2)}
                    }
                    for(stat in remember(build){build.stats()})Row(Modifier.fillMaxWidth().padding(vertical=2.dp),horizontalArrangement=Arrangement.spacedBy(6.dp)){
                        Text(stat.label,Type.footnote,c.ink2,Modifier.weight(1f));Text(stat.value,Type.subhead,c.ink)
                    }
                    Divider()
                    Text("Perks",Type.headline,c.ink)
                    if(build.perks().isEmpty())Text("No perks yet",Type.footnote,c.ink2)
                    for(id in build.perks())Column{Text(Perks.of(id).name,Type.subhead,c.ink);Text(Perks.of(id).blurb,Type.footnote,c.ink2)}
                    Divider()
                    Text("Evolution",Type.headline,c.ink)
                    Text(build.nextEvolutionStage()?.let{"Next choice after stage $it"}?:"Final frame",Type.subhead,c.ink)
                    if(build.frame==TankEvolution.BASIC&&build.lastEvolution<5)Text("Choose your tank after stage 5 to reveal its specialization.",Type.footnote,c.ink2)
                    else for(frame in build.nextFrames())Text(frame.label+" · "+frame.detail,Type.footnote,c.ink2)
                    Text("Stats use current ranks and permanent perks; temporary combat effects may differ.",Type.caption,c.ink3)
                }
            }
            if(hasReward&&!confirmExit){
                val evolution=shop!!.perkOffers.any{it.evolution!=null}
                Box(Modifier.fillMaxSize().background(c.ground.copy(alpha=.86f)).pointerInput(Unit){detectTapGestures{}}){
                    Column(Modifier.align(Alignment.Center).widthIn(max=760.dp).fillMaxWidth(.94f).fillMaxHeight().padding(10.dp)){
                        Text(if(evolution)"Choose your tank" else "Choose a boss perk",Type.title2,c.ink)
                        Text(if(evolution)"Your choice reveals the matching upgrade paths." else "This perk stays with you for the run.",Type.footnote,c.ink2)
                        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(vertical=7.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){
                            for(offer in shop.perkOffers){
                                Row(Modifier.fillMaxWidth().background(c.surface,RoundedCornerShape(8.dp)).padding(8.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)){
                                    offer.evolution?.let{TankPreview(build.character,build.look,Modifier.size(56.dp),it)}
                                    Column(Modifier.weight(1f)){Text(offer.name,Type.headline,c.ink);Text(offer.blurb,Type.footnote,c.ink2)}
                                    SecondaryButton("Choose",{if(rewardPending<0){rewardPending=offer.slot;onPerk(offer.slot)}},enabled=!ready&&rewardPending<0)
                                }
                            }
                        }
                    }
                }
            }
            if(confirmExit)Box(Modifier.fillMaxSize().background(c.ground.copy(alpha=.85f)).pointerInput(Unit){detectTapGestures{}}){
                Column(Modifier.align(Alignment.Center).widthIn(max=420.dp).fillMaxWidth(.85f).background(c.surface,RoundedCornerShape(12.dp)).padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
                    Text(if(infinite)"End developer trial?" else "Save and exit?",Type.title3,c.ink)
                    Text(if(infinite)"This trial is not saved or recorded." else if(shop?.coop==true)"This saves the campaign and stops for everyone." else "Continue this campaign later from its shop.",Type.footnote,c.ink2)
                    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){SecondaryButton("Cancel",{confirmExit=false},Modifier.weight(1f));PrimaryButton(if(infinite)"Exit trial" else "Save & Exit",onSave,Modifier.weight(1f))}
                }
            }
        }
    }
}

@Composable
private fun MapControl(label:String,onClick:()->Unit,enabled:Boolean=true){
    val c=Theme.colors
    Box(Modifier.heightIn(min=36.dp).widthIn(min=40.dp).clip(RoundedCornerShape(7.dp)).background(c.track)
        .clickable(enabled=enabled,onClick=onClick).padding(horizontal=9.dp,vertical=5.dp),contentAlignment=Alignment.Center){
        Text(label,Type.footnote,if(enabled)c.ink else c.ink3,maxLines=1)
    }
}
@Composable
private fun MapPanel(modifier:Modifier,title:String,onClose:()->Unit,content:@Composable ColumnScope.()->Unit){
    val c=Theme.colors;val shape=RoundedCornerShape(12.dp)
    Column(modifier.clip(shape).background(c.ground).border(1.dp,c.line,shape).pointerInput(Unit){detectTapGestures{}}.padding(9.dp)){
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Text(title,Type.headline,c.ink,Modifier.weight(1f),maxLines=2);MapControl("×",onClose)}
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(top=5.dp),verticalArrangement=Arrangement.spacedBy(7.dp),content=content)
    }
}
