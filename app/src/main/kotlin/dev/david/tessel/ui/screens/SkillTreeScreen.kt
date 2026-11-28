package dev.david.tessel.ui.screens

import android.graphics.Paint
import android.graphics.Typeface
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
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import dev.david.tessel.core.content.SkillBonus
import dev.david.tessel.core.content.SkillGraph
import dev.david.tessel.core.content.SkillSector
import dev.david.tessel.ui.components.*
import dev.david.tessel.ui.theme.*
import kotlinx.coroutines.delay
import kotlin.math.*

private fun sectorTint(sector:SkillSector,dark:Boolean):Color {
    val colors=if(dark)listOf(0xFF5FD5BE,0xFFC7A6F3,0xFFEAC46C,0xFFF19B9B,0xFF88C8FF,0xFFFFAE78)
    else listOf(0xFF087E70,0xFF7750A5,0xFF93630B,0xFFAE4B58,0xFF2868A4,0xFFAE541D)
    return Color(colors[sector.ordinal])
}

/** A continuous, zoomable constellation: the map itself is the navigation.
 * No automatic opening, no forced points. All purchases go through the authoritative shop. */
@Composable
fun SkillTreeScreen(clearedStage:Int,mask:Long,coins:Int,canSpend:Boolean,
    onSpend:(Int)->Unit,onBack:()->Unit,backLabel:String="Home") {
    val c=Theme.colors
    var selected by rememberSaveable { mutableIntStateOf(0) }
    var zoom by rememberSaveable { mutableFloatStateOf(1f) }
    var px by rememberSaveable { mutableFloatStateOf(0f) }
    var py by rememberSaveable { mutableFloatStateOf(0f) }
    var viewport by remember { mutableStateOf(IntSize.Zero) }
    var pending by remember { mutableStateOf<Long?>(null) }
    val node=SkillGraph.nodes[selected]
    val owned=SkillGraph.owns(mask,selected)
    val connected=SkillGraph.connected(mask,selected)
    val affordable=SkillGraph.canBuy(mask,selected,clearedStage,coins)
    val ready=canSpend && affordable && pending==null
    val camera=SkillMapCamera(zoom,px,py)
    fun update(next:SkillMapCamera){zoom=next.zoom;px=next.x;py=next.y}
    fun resize(factor:Float) {
        val w=viewport.width.toFloat();val h=viewport.height.toFloat()
        if(w>0f&&h>0f)update(SkillMapCamera(zoom,px,py).gesture(w,h,w/2,h/2,0f,0f,factor))
    }
    LaunchedEffect(mask,pending){
        if(pending!=null){if(pending==mask)delay(1600L);pending=null}
    }
    val tint=sectorTint(node.sector,c.dark)
    val count=SkillGraph.count(mask)
    val total=remember(mask){SkillGraph.totals(mask)}
    val paint=remember{Paint(Paint.ANTI_ALIAS_FLAG).apply{textAlign=Paint.Align.CENTER;typeface=Typeface.DEFAULT_BOLD}}
    val scroll=rememberScrollState()
    Column(Modifier.fillMaxSize().background(c.ground).pointerInput(Unit){detectTapGestures{}}.windowInsetsPadding(WindowInsets.displayCutout).padding(horizontal=Space.x3)) {
        Row(Modifier.fillMaxWidth().height(48.dp),verticalAlignment=Alignment.CenterVertically){
            BackButton(backLabel,onBack)
            Spacer(Modifier.weight(1f))
            Text("Skill Tree",Type.title2,c.ink)
            Spacer(Modifier.weight(1f))
            Text("$count / 60",Type.subhead,c.ink2)
            Spacer(Modifier.width(10.dp))
            if(backLabel!="Home")CoinAmount(coins,style=Type.headline) else Text("Preview",Type.subhead,c.accent)
        }
        Text(if(clearedStage<SkillGraph.FIRST_STAGE)"Clear stage 3 to unlock skill purchases · spend run coins in the shop" else "Stage $clearedStage cleared · choose your own route · purchases are optional",
            Type.footnote,c.ink2,Modifier.padding(horizontal=4.dp,bottom=5.dp),maxLines=1)
        Row(Modifier.fillMaxWidth().weight(1f).padding(bottom=6.dp),horizontalArrangement=Arrangement.spacedBy(10.dp)) {
            Box(Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(14.dp)).background(c.surface).border(1.dp,c.line,RoundedCornerShape(14.dp))) {
                Canvas(Modifier.fillMaxSize().onSizeChanged{viewport=it}
                    .semantics{contentDescription="Skill constellation map. Pinch to zoom and drag to pan. Tap a node to inspect."}
                    .pointerInput(Unit){
                        detectTransformGestures { centroid,pan,change,_ ->
                            update(SkillMapCamera(zoom,px,py).gesture(size.width.toFloat(),size.height.toFloat(),centroid.x,centroid.y,pan.x,pan.y,change))
                        }
                    }
                    .pointerInput(Unit){
                        detectTapGestures { at ->
                            SkillMapCamera(zoom,px,py).hit(size.width.toFloat(),size.height.toFloat(),at.x,at.y,15.dp.toPx())?.let{selected=it}
                        }
                    }) {
                    val w=size.width;val h=size.height
                    val scale=camera.scale(w,h)
                    fun point(x:Float,y:Float)=Offset(camera.sx(x,w,h),camera.sy(y,w,h))
                    fun point(id:Int):Offset=SkillGraph.nodes[id].let{point(it.x,it.y)}
                    val hub=point(0f,0f)
                    // Quiet drafting grid and rings remain world-anchored as the map moves.
                    val step=(80f*scale).coerceAtLeast(10f)
                    var gx=((hub.x%step)+step)%step
                    while(gx<w){drawLine(c.ink.copy(alpha=.045f),Offset(gx,0f),Offset(gx,h));gx+=step}
                    var gy=((hub.y%step)+step)%step
                    while(gy<h){drawLine(c.ink.copy(alpha=.045f),Offset(0f,gy),Offset(w,gy));gy+=step}
                    for(r in listOf(180f,310f,450f,610f,780f)){
                        drawOval(c.ink.copy(alpha=.075f),topLeft=point(-r*1.25f,-r*.67f),
                            size=androidx.compose.ui.geometry.Size(r*2.5f*scale,r*1.34f*scale),style=Stroke(1.dp.toPx()))
                    }
                    for(sector in SkillSector.entries){
                        val root=sector.ordinal*9
                        val color=sectorTint(sector,c.dark)
                        val active=SkillGraph.owns(mask,root)
                        drawLine(if(active)color else color.copy(alpha=.35f),hub,point(root),strokeWidth=if(active)2.2.dp.toPx() else 1.dp.toPx())
                    }
                    for(edge in SkillGraph.edges){
                        val a=SkillGraph.nodes[edge.a];val b=SkillGraph.nodes[edge.b]
                        val aOwned=SkillGraph.owns(mask,a.id);val bOwned=SkillGraph.owns(mask,b.id)
                        val active=aOwned&&bOwned
                        val path=(aOwned||bOwned)&&(SkillGraph.eligible(mask,if(aOwned)b.id else a.id,clearedStage))
                        val color=if(a.bridge||b.bridge)c.coin else sectorTint(a.sector,c.dark)
                        drawLine(color.copy(alpha=when{active->.95f;path->.65f;else->.23f}),
                            point(a.id),point(b.id),strokeWidth=if(active)2.3.dp.toPx() else 1.15.dp.toPx())
                    }
                    val hr=(29f*scale).coerceAtLeast(7.dp.toPx())
                    drawCircle(c.ground,hr+2.dp.toPx(),hub)
                    drawCircle(c.accent,hr,hub,style=Stroke(1.7.dp.toPx()))
                    drawCircle(c.accent.copy(alpha=.65f),hr*.38f,hub)
                    for(n in SkillGraph.nodes){
                        val at=point(n.id)
                        val radius=(if(n.bridge)21f else if(n.root||n.id%9==8)27f else 22f)*scale
                        if(at.x+radius<0||at.x-radius>w||at.y+radius<0||at.y-radius>h)continue
                        val own=SkillGraph.owns(mask,n.id)
                        val avail=SkillGraph.eligible(mask,n.id,clearedStage)
                        val buy=avail&&coins>=n.price
                        val color=sectorTint(n.sector,c.dark)
                        val selectedNode=n.id==selected
                        val outline=if(n.bridge)c.coin else color
                        if(selectedNode)drawCircle(outline.copy(alpha=.25f),radius+5.dp.toPx(),at)
                        drawCircle(if(own)color else if(buy)color.copy(alpha=.22f) else c.ground,radius,at)
                        drawCircle(outline.copy(alpha=if(own||buy||selectedNode)1f else if(avail).70f else .38f),radius,at,
                            style=Stroke(if(selectedNode)2.4.dp.toPx() else 1.1.dp.toPx()))
                        if(own)drawCircle(c.ground.copy(alpha=.9f),radius*.24f,at)
                        else if(n.bridge){
                            drawLine(outline.copy(alpha=.65f),at-Offset(radius*.43f,0f),at+Offset(radius*.43f,0f),strokeWidth=1.dp.toPx())
                            drawLine(outline.copy(alpha=.65f),at-Offset(0f,radius*.43f),at+Offset(0f,radius*.43f),strokeWidth=1.dp.toPx())
                        }else if(radius>8.dp.toPx()){
                            paint.color=(if(buy)c.ink else outline.copy(alpha=.76f)).toArgb()
                            paint.textSize=(radius*.65f).coerceAtMost(12.dp.toPx())
                            drawContext.canvas.nativeCanvas.drawText(n.bonus.mark(n.amount),at.x,at.y-(paint.ascent()+paint.descent())*.5f,paint)
                        }else drawCircle(outline.copy(alpha=if(avail).8f else .32f),radius*.24f,at)
                    }
                    // Six sector headers at the outer edge, kept understated at overview zoom.
                    for(sector in SkillSector.entries){
                        val outer=SkillGraph.nodes[sector.ordinal*9+8]
                        val at=point(outer.x*1.06f,outer.y*1.06f)
                        paint.color=sectorTint(sector,c.dark).toArgb();paint.textSize=(9.dp.toPx()*zoom.coerceAtMost(1.35f))
                        val dy=if(outer.y<0)-9.dp.toPx() else 13.dp.toPx()
                        drawContext.canvas.nativeCanvas.drawText(sector.label.uppercase(),at.x,at.y+dy,paint)
                    }
                }
                Row(Modifier.align(Alignment.TopStart).padding(5.dp),horizontalArrangement=Arrangement.spacedBy(3.dp)) {
                    MapButton("−",{resize(.75f)})
                    MapButton("+",{resize(1.33f)})
                    MapButton("Fit",{update(SkillMapCamera())})
                    Text("${(zoom*100).roundToInt()}%",Type.caption,c.ink2,Modifier.align(Alignment.CenterVertically).padding(horizontal=3.dp))
                }
                Text("Drag · Pinch · Tap nodes",Type.caption,c.ink3,Modifier.align(Alignment.BottomStart).padding(6.dp))
            }
            Column(Modifier.widthIn(min=182.dp,max=245.dp).weight(.43f).fillMaxHeight().clip(RoundedCornerShape(14.dp)).background(c.surface).padding(9.dp)) {
                Column(Modifier.weight(1f).verticalScroll(scroll),verticalArrangement=Arrangement.spacedBy(5.dp)){
                    Text(if(node.bridge)"CROSSROUTE" else node.sector.label.uppercase(),Type.caption,tint)
                    Text(node.name,Type.title3,c.ink)
                    Text(node.description,Type.subhead,c.ink)
                    Text(when{owned->"UNLOCKED";clearedStage<node.minStage->"LOCKED · STAGE ${node.minStage}";!connected->"LOCKED · CONNECT A NODE";!affordable->"NOT ENOUGH COINS";!canSpend->"PURCHASE IN SHOP";else->"AVAILABLE"},Type.caption,if(owned||ready)tint else c.ink2)
                    if(!owned){
                        Row(verticalAlignment=Alignment.CenterVertically){Text("Cost",Type.footnote,c.ink2,Modifier.weight(1f));CoinAmount(node.price,style=Type.headline)}
                        if(!connected)Text("Connect from "+SkillGraph.neighbors(selected).joinToString(" / "){it.name},Type.footnote,c.ink2)
                    }
                    if(backLabel=="Home")Text("Preview only. Earn coins during a run.",Type.footnote,c.ink2)
                    if(backLabel=="Shop"&&!canSpend)Text("Finish the boss reward or wait for the team.",Type.footnote,c.ink2)
                    if(count>0){
                        Text("YOUR BONUSES",Type.caption,c.ink3)
                        for(b in SkillBonus.entries)if(total[b.ordinal]>0)Text(b.description(total[b.ordinal]),Type.footnote,c.ink2)
                    }
                    Text("Skills reset on a new run. Cross-connections let you change direction.",Type.caption,c.ink3)
                }
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(4.dp)){
                    MapButton("‹",{selected=(selected+59)%60},Modifier.weight(1f))
                    MapButton("›",{selected=(selected+1)%60},Modifier.weight(1f))
                }
                Spacer(Modifier.height(4.dp))
                PrimaryButton(if(owned)"Unlocked" else "Unlock · ${node.price}",{if(ready){pending=mask;onSpend(node.id)}},Modifier.fillMaxWidth(),enabled=ready)
            }
        }
    }
}

@Composable
private fun MapButton(label:String,action:()->Unit,modifier:Modifier=Modifier){
    val c=Theme.colors
    Box(modifier.height(36.dp).clip(RoundedCornerShape(8.dp)).background(c.track).clickable(onClick=action).padding(horizontal=10.dp),
        contentAlignment=Alignment.Center){Text(label,Type.subhead,c.ink)}
}
