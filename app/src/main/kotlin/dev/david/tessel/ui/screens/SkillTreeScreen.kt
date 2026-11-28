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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
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
    onSpend:(Int)->Unit,onBack:()->Unit,backLabel:String="Home") {
    val c=Theme.colors
    var selected by rememberSaveable { mutableIntStateOf(-1) }
    var zoom by rememberSaveable { mutableFloatStateOf(1f) }
    var px by rememberSaveable { mutableFloatStateOf(0f) }
    var py by rememberSaveable { mutableFloatStateOf(0f) }
    var viewport by remember { mutableStateOf(IntSize.Zero) }
    var pending by remember { mutableStateOf<Long?>(null) }
    val currentMask by rememberUpdatedState(mask)
    val camera=SkillMapCamera(zoom,px,py)
    val states=remember(mask){Array(SkillGraph.COUNT){SkillGraph.visibility(mask,it)}}
    val count=SkillGraph.count(mask)
    val discovered=states.count{it!=SkillVisibility.HIDDEN}
    val paint=remember{Paint(Paint.ANTI_ALIAS_FLAG).apply{textAlign=Paint.Align.CENTER;typeface=Typeface.DEFAULT_BOLD}}
    fun update(next:SkillMapCamera){zoom=next.zoom;px=next.x;py=next.y}
    fun resize(factor:Float){
        val w=viewport.width.toFloat();val h=viewport.height.toFloat()
        if(w>0f&&h>0f)update(SkillMapCamera(zoom,px,py).gesture(w,h,w/2,h/2,0f,0f,factor))
    }
    fun inspect(id:Int){if(SkillGraph.visibility(currentMask,id)!=SkillVisibility.HIDDEN)selected=id}
    LaunchedEffect(mask,pending){
        if(pending!=null){if(pending==mask)delay(1600L);pending=null}
    }
    LaunchedEffect(mask){if(selected>=0&&SkillGraph.visibility(mask,selected)==SkillVisibility.HIDDEN)selected=-1}
    BackHandler{if(selected>=0)selected=-1 else onBack()}
    Column(Modifier.fillMaxSize().background(c.ground).windowInsetsPadding(WindowInsets.displayCutout).padding(horizontal=Space.x2)){
        Row(Modifier.fillMaxWidth().height(48.dp),verticalAlignment=Alignment.CenterVertically){
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
                .pointerInput(Unit){
                    detectTapGestures { at ->
                        val id=SkillMapCamera(zoom,px,py).hitDiscovered(size.width.toFloat(),size.height.toFloat(),at.x,at.y,12.dp.toPx(),currentMask)
                        if(id!=null)inspect(id) else selected=-1
                    }
                }) {
                val w=size.width;val h=size.height;val scale=camera.scale(w,h)
                fun point(x:Float,y:Float)=Offset(camera.sx(x,w,h),camera.sy(y,w,h))
                fun point(id:Int):Offset=SkillGraph.nodes[id].let{point(it.x,it.y)}
                val hub=point(0f,0f)
                val step=(80f*scale).coerceAtLeast(12f)
                var gx=((hub.x%step)+step)%step
                while(gx<w){drawLine(c.ink.copy(alpha=.035f),Offset(gx,0f),Offset(gx,h));gx+=step}
                var gy=((hub.y%step)+step)%step
                while(gy<h){drawLine(c.ink.copy(alpha=.035f),Offset(0f,gy),Offset(w,gy));gy+=step}
                for(r in listOf(180f,310f,450f,610f,780f))
                    drawOval(c.ink.copy(alpha=.06f),topLeft=point(-r*1.25f,-r*.67f),size=Size(r*2.5f*scale,r*1.34f*scale),style=Stroke(1.dp.toPx()))
                for(sector in SkillSector.entries){
                    val root=sector.ordinal*9;val tint=sectorTint(sector,c.dark)
                    val owned=states[root]==SkillVisibility.OWNED
                    drawLine(tint.copy(alpha=if(owned).9f else .43f),hub,point(root),strokeWidth=if(owned)2.dp.toPx() else 1.dp.toPx())
                }
                for(edge in SkillGraph.edges){
                    val a=states[edge.a];val b=states[edge.b]
                    val both=a==SkillVisibility.OWNED&&b==SkillVisibility.OWNED
                    val frontier=(a==SkillVisibility.OWNED||b==SkillVisibility.OWNED)&&a!=SkillVisibility.HIDDEN&&b!=SkillVisibility.HIDDEN
                    // Faint anonymous scaffolding outlines the full map without exposing hidden skills.
                    val n=SkillGraph.nodes[edge.a]
                    val tint=if(both||frontier)if(n.bridge||SkillGraph.nodes[edge.b].bridge)c.coin else sectorTint(n.sector,c.dark) else c.ink
                    drawLine(tint.copy(alpha=if(both).9f else if(frontier).58f else .085f),point(edge.a),point(edge.b),strokeWidth=if(both)2.2.dp.toPx() else 1.dp.toPx())
                }
                val hr=(29f*scale).coerceAtLeast(6.dp.toPx())
                drawCircle(c.ground,hr+2.dp.toPx(),hub)
                drawCircle(c.accent,hr,hub,style=Stroke(1.5.dp.toPx()))
                drawCircle(c.accent,hr*.25f,hub)
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
                // Root sector names remain public, but deeper identities are not disclosed.
                for(sector in SkillSector.entries){
                    val outer=SkillGraph.nodes[sector.ordinal*9+8]
                    val at=point(outer.x*1.055f,outer.y*1.055f)
                    paint.color=sectorTint(sector,c.dark).copy(alpha=.85f).toArgb()
                    paint.textSize=9.dp.toPx()
                    val label=sector.label.uppercase()
                    val half=paint.measureText(label)*.5f
                    val x=at.x.coerceIn(half+3f,maxOf(half+3f,w-half-3f))
                    val dy=if(outer.y<0)-9.dp.toPx() else 12.dp.toPx()
                    val y=(at.y+dy).coerceIn(paint.textSize+2f,maxOf(paint.textSize+2f,h-3f))
                    if(at.x in 0f..w&&at.y in 0f..h)drawContext.canvas.nativeCanvas.drawText(label,x,y,paint)
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
                Text(if(clearedStage<SkillGraph.FIRST_STAGE)"Purchases start after stage 3" else "Drag · Pinch · Tap a revealed node",Type.caption,c.ink3)
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
                        Text(when{own->"UNLOCKED";clearedStage<n.minStage->"LOCKED · STAGE ${n.minStage}";!canSpend->"PURCHASE IN SHOP";coins<price->"NOT ENOUGH COINS";else->"AVAILABLE"},Type.caption,if(own||ready)c.accent else c.ink2)
                        if(!own){
                            Row(verticalAlignment=Alignment.CenterVertically){Text("Cost",Type.footnote,c.ink2,Modifier.weight(1f));CoinAmount(price,style=Type.headline)}
                            Text("Unlocking reveals connected nodes.",Type.footnote,c.ink2)
                        }
                        if(backLabel=="Home")Text("Preview only. Earn run coins to purchase skills.",Type.footnote,c.ink2)
                        Text("First 6 nodes use base prices. Skills reset on a new run.",Type.caption,c.ink3)
                    }
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
