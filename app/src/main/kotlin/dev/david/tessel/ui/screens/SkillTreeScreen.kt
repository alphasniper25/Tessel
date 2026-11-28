package dev.david.tessel.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
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
import dev.david.tessel.platform.game.TankArt
import dev.david.tessel.ui.components.*
import dev.david.tessel.ui.theme.*
import kotlinx.coroutines.delay
import kotlin.math.*

private fun sectorTint(sector:SkillSector,dark:Boolean):Color {
    val colors=if(dark)listOf(0xFF5FD5BE,0xFFC7A6F3,0xFFEAC46C,0xFFF19B9B,0xFF88C8FF,0xFFFFAE78)
    else listOf(0xFF087E70,0xFF7750A5,0xFF93630B,0xFFAE4B58,0xFF2868A4,0xFFAE541D)
    return Color(colors[sector.ordinal])
}

/** Full-window discovery map. Hidden silhouettes show scope without leaking
 * names or effects. Ownership is the only source of discovery. */
@Composable
fun SkillTreeScreen(clearedStage:Int,mask:Long,coins:Int,canSpend:Boolean,
    onSpend:(Int)->Unit,onBack:()->Unit,backLabel:String="Home",
    embedded:Boolean=false,onDevSkill:((Int,Boolean)->Unit)?=null,
    frame:TankEvolution=TankEvolution.BASIC,character:CharacterDef=CharacterDef.BALANCED,look:TankLook=TankLook()) {
    val c=Theme.colors
    var selected by rememberSaveable { mutableIntStateOf(-1) }
    var zoom by rememberSaveable { mutableFloatStateOf(1f) }
    var px by rememberSaveable { mutableFloatStateOf(0f) }
    var py by rememberSaveable { mutableFloatStateOf(0f) }
    var viewport by remember { mutableStateOf(IntSize.Zero) }
    var pending by remember { mutableStateOf<Long?>(null) }
    val currentMask by rememberUpdatedState(mask)
    val camera=SkillMapCamera(zoom,px,py)
    val revealAll=onDevSkill!=null
    val states=remember(mask,revealAll){Array(SkillGraph.COUNT){id->
        val state=SkillGraph.visibility(mask,id)
        if(revealAll&&state==SkillVisibility.HIDDEN)SkillVisibility.REVEALED else state
    }}
    val count=SkillGraph.count(mask)
    val discovered=states.count{it!=SkillVisibility.HIDDEN}
    val art=remember{TankArt()}
    fun update(next:SkillMapCamera){zoom=next.zoom;px=next.x;py=next.y}
    fun resize(factor:Float){
        val w=viewport.width.toFloat();val h=viewport.height.toFloat()
        if(w>0f&&h>0f)update(SkillMapCamera(zoom,px,py).gesture(w,h,w/2,h/2,0f,0f,factor))
    }
    fun inspect(id:Int){if(revealAll||SkillGraph.visibility(currentMask,id)!=SkillVisibility.HIDDEN)selected=id}
    LaunchedEffect(mask,pending){
        if(pending!=null){if(pending==mask)delay(1600L);pending=null}
    }
    LaunchedEffect(mask,revealAll){if(!revealAll&&selected>=0&&SkillGraph.visibility(mask,selected)==SkillVisibility.HIDDEN)selected=-1}
    BackHandler{if(selected>=0)selected=-1 else onBack()}
    Column(Modifier.fillMaxSize().background(c.ground).windowInsetsPadding(WindowInsets.displayCutout).padding(horizontal=Space.x2)){
        if(!embedded) Row(Modifier.fillMaxWidth().height(48.dp),verticalAlignment=Alignment.CenterVertically){
            BackButton(backLabel,onBack)
            Spacer(Modifier.weight(1f))
            Text("$count / ${SkillGraph.COUNT} unlocked",Type.subhead,c.ink)
            Spacer(Modifier.width(10.dp))
            if(backLabel=="Home")Text("Preview",Type.footnote,c.ink2) else CoinAmount(coins,style=Type.headline)
        }
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth().padding(bottom=4.dp).clip(RoundedCornerShape(10.dp)).background(c.surface)) {
            val wide=maxWidth>=580.dp
            Canvas(Modifier.fillMaxSize().onSizeChanged{viewport=it}.testTag("skill-map")
                .semantics{
                    contentDescription="Skill map. $discovered discovered out of ${SkillGraph.COUNT}. Pinch to zoom. Drag to pan."
                    customActions=SkillGraph.nodes.filter{states[it.id]!=SkillVisibility.HIDDEN}.map{n->
                        CustomAccessibilityAction("Inspect ${n.name}"){inspect(n.id);true}
                    }
                }
                .pointerInput(Unit){
                    detectTransformGestures { centroid,pan,change,_ ->
                        update(SkillMapCamera(zoom,px,py).gesture(size.width.toFloat(),size.height.toFloat(),centroid.x,centroid.y,pan.x,pan.y,change))
                    }
                }
                .pointerInput(revealAll){
                    detectTapGestures { at ->
                        val cam=SkillMapCamera(zoom,px,py)
                        val id=if(revealAll)cam.hit(size.width.toFloat(),size.height.toFloat(),at.x,at.y,12.dp.toPx())
                        else cam.hitDiscovered(size.width.toFloat(),size.height.toFloat(),at.x,at.y,12.dp.toPx(),currentMask)
                        if(id!=null)inspect(id) else selected=-1
                    }
                }) {
                val w=size.width;val h=size.height;val scale=camera.scale(w,h)
                fun point(x:Float,y:Float)=Offset(camera.sx(x,w,h),camera.sy(y,w,h))
                fun point(id:Int):Offset=SkillGraph.nodes[id].let{point(it.x,it.y)}
                val hub=point(0f,0f)
                // The graph still governs purchases/discovery. The overview
                // deliberately renders only icons, silhouettes and the tank.
                val hr=(85f*scale).coerceIn(13.dp.toPx(),48.dp.toPx())
                treeTank(art,hub,hr,character,look,frame,c.ink,c.ground,c.accent)
                for(n in SkillGraph.nodes){
                    val at=point(n.id)
                    val radius=((if(n.root||n.id%9==8)29f else 24f)*scale).coerceIn(5.dp.toPx(),35.dp.toPx())
                    if(at.x+radius<0||at.x-radius>w||at.y+radius<0||at.y-radius>h)continue
                    val state=states[n.id]
                    if(state==SkillVisibility.HIDDEN){
                        drawCircle(c.ink.copy(alpha=.085f),radius*.62f,at,style=Stroke(1.dp.toPx()))
                        drawCircle(c.ink.copy(alpha=.12f),radius*.12f,at)
                        continue
                    }
                    val own=state==SkillVisibility.OWNED
                    val buy=canSpend&&SkillGraph.canBuy(mask,n.id,clearedStage,coins)
                    val tint=if(n.bridge)c.coin else sectorTint(n.sector,c.dark)
                    if(n.id==selected)drawCircle(tint.copy(alpha=.24f),radius+4.dp.toPx(),at)
                    drawCircle(if(own)tint else if(buy)tint.copy(alpha=.17f) else c.ground,radius,at)
                    drawCircle(tint.copy(alpha=if(own||buy||n.id==selected)1f else .62f),radius,at,style=Stroke(if(n.id==selected)2.dp.toPx() else 1.25.dp.toPx()))
                    skillGlyph(n.bonus,at,radius*.57f,if(own)c.ground else tint.copy(alpha=if(buy)1f else .82f))
                    if(own)drawCircle(tint,radius*.15f,at+Offset(radius*.78f,-radius*.78f))
                }

            }
            Row(Modifier.align(Alignment.TopStart).padding(5.dp),horizontalArrangement=Arrangement.spacedBy(3.dp),verticalAlignment=Alignment.CenterVertically){
                MapButton("−",{resize(.75f)})
                MapButton("+",{resize(1.33f)})
                MapButton("Fit",{update(SkillMapCamera())})
                Text("${(zoom*100).roundToInt()}%",Type.caption,c.ink2,Modifier.padding(horizontal=4.dp))
            }
            Column(Modifier.align(Alignment.BottomStart).padding(7.dp)){
                Text("$discovered / ${SkillGraph.COUNT} discovered",Type.footnote,c.ink2)
                Text("Drag · Pinch · Tap a revealed node",Type.caption,c.ink3)
            }
            val n=SkillGraph.nodes.getOrNull(selected)?.takeIf{states[it.id]!=SkillVisibility.HIDDEN}
            if(n!=null){
                val own=states[n.id]==SkillVisibility.OWNED
                val price=SkillGraph.price(mask,n.id)
                val ready=canSpend&&SkillGraph.canBuy(mask,n.id,clearedStage,coins)&&pending==null
                val shape=RoundedCornerShape(12.dp)
                val panel=if(wide)Modifier.align(Alignment.CenterEnd).padding(6.dp).width(286.dp).fillMaxHeight(.96f)
                    else Modifier.align(Alignment.BottomCenter).padding(6.dp).fillMaxWidth().fillMaxHeight(.70f)
                Column(panel.clip(shape).background(c.ground).border(1.dp,c.line,shape).pointerInput(Unit){detectTapGestures{}}.padding(9.dp)){
                    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(7.dp)){
                        val tint=sectorTint(n.sector,c.dark)
                        Canvas(Modifier.size(38.dp)){
                            val r=size.minDimension*.46f
                            drawCircle(tint.copy(alpha=.15f),r)
                            drawCircle(tint,r,style=Stroke(1.5.dp.toPx()))
                            skillGlyph(n.bonus,center,r*.55f,tint)
                        }
                        Column(Modifier.weight(1f)){
                            Text(n.name,Type.headline,c.ink,maxLines=2)
                            Text(if(n.bridge)"Cross-route" else n.sector.label,Type.caption,tint)
                        }
                        MapButton("×",{selected=-1})
                    }
                    Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(vertical=6.dp),verticalArrangement=Arrangement.spacedBy(7.dp)){
                        Text(n.description,Type.subhead,c.ink)
                        if(own)Text("UNLOCKED",Type.caption,c.accent)
                        else{
                            Text(if(clearedStage<n.minStage)"LOCKED · STAGE ${n.minStage}" else "Requirements",Type.subhead,if(clearedStage<n.minStage)c.coin else c.ink)
                            PurchaseRequirement("Stage","$clearedStage / ${n.minStage}",clearedStage>=n.minStage)
                            PurchaseRequirement("Coins","$coins / $price",coins>=price)
                            PurchaseRequirement("Connected route",if(SkillGraph.connected(mask,n.id))"Reached" else "Unlock neighbor",SkillGraph.connected(mask,n.id))
                            if(!canSpend)PurchaseRequirement("Shop access",if(backLabel=="Home")"In-run shop only" else "Finish reward / ready",false)
                            Text("Unlocking reveals connected nodes.",Type.footnote,c.ink2)
                        }
                        if(backLabel=="Home")Text("Preview only. Earn run coins to purchase skills.",Type.footnote,c.ink2)
                        Text("First 6 nodes use base prices. Skills reset on a new run.",Type.caption,c.ink3)
                    }
                    if(onDevSkill!=null)SecondaryButton(if(own)"DEV · Remove" else "DEV · Grant",{onDevSkill(n.id,!own)},Modifier.fillMaxWidth())
                    PrimaryButton(if(own)"Unlocked" else "Unlock · $price",{if(ready){pending=mask;onSpend(n.id)}},Modifier.fillMaxWidth(),enabled=ready)
                }
            }
        }
    }
}

@Composable
private fun MapButton(label:String,action:()->Unit){
    val c=Theme.colors
    Box(Modifier.heightIn(min=44.dp).widthIn(min=44.dp).clip(RoundedCornerShape(8.dp)).background(c.track).clickable(onClick=action).padding(horizontal=9.dp),contentAlignment=Alignment.Center){
        Text(label,Type.subhead,c.ink)
    }
}
