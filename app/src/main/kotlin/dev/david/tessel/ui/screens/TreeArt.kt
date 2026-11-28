package dev.david.tessel.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import dev.david.tessel.core.content.*
import dev.david.tessel.platform.game.TankArt
import dev.david.tessel.ui.components.Text
import dev.david.tessel.ui.theme.*

internal fun DrawScope.treeTank(art:TankArt,at:Offset,r:Float,character:CharacterDef,look:TankLook,frame:TankEvolution,
    ink:Color,ground:Color,accent:Color){
    drawCircle(ground,r+2.dp.toPx(),at)
    drawCircle(accent.copy(alpha=.65f),r,at,style=Stroke(1.5.dp.toPx()))
    art.draw(drawContext.canvas.nativeCanvas,at.x,at.y,r*.43f,.8f,-.6f,character,look,
        ink.toArgb(),ink.copy(alpha=.8f).toArgb(),ground.toArgb(),evolution=frame)
}

/** Matching fictional sigils, not random characters. The same three glyphs appear
 * behind both component perks and their combo, and do not require a font asset. */
internal fun DrawScope.perkSigil(code:Int,at:Offset,unit:Float,color:Color){
    val width=(unit*.12f).coerceAtLeast(.75f)
    for(k in 0..2){
        val x=at.x+(k-1)*unit*2.7f
        fun p(a:Float,b:Float)=Offset(x+a*unit,at.y+b*unit)
        fun l(a:Float,b:Float,c:Float,d:Float)=drawLine(color,p(a,b),p(c,d),width,StrokeCap.Round)
        when((code/(if(k==0)1 else if(k==1)7 else 49)+k*2)%7){
            0->{l(-.8f,1f,0f,-1f);l(0f,-1f,.8f,1f);l(-.45f,.1f,.45f,.1f)}
            1->{l(0f,-1f,0f,1f);l(0f,-1f,.85f,-.4f);l(.85f,-.4f,0f,.2f);l(0f,.2f,-.7f,.8f)}
            2->{l(-.8f,-1f,.8f,-1f);l(.8f,-1f,-.8f,1f);l(-.8f,1f,.8f,1f);l(0f,-.35f,0f,.35f)}
            3->{l(-.8f,0f,0f,-1f);l(0f,-1f,.8f,0f);l(.8f,0f,0f,1f);l(0f,1f,-.8f,0f);l(0f,-1f,0f,1f)}
            4->{l(-.7f,-1f,-.7f,1f);l(-.7f,-1f,.8f,0f);l(.8f,0f,-.7f,1f);l(-.7f,0f,.3f,0f)}
            5->{l(-.9f,-.7f,.9f,.7f);l(-.9f,.7f,.9f,-.7f);l(0f,-1f,0f,1f)}
            else->{l(-.85f,-1f,.85f,-1f);l(0f,-1f,0f,1f);l(-.8f,.3f,.8f,.3f);drawCircle(color,unit*.12f,p(.65f,.7f))}
        }
    }
}

@Composable
internal fun StageGateNotice(title:String,detail:String,modifier:Modifier=Modifier){
    val c=Theme.colors
    val shape=RoundedCornerShape(8.dp)
    Row(modifier.background(c.surface,shape).border(1.dp,c.coin.copy(alpha=.6f),shape).padding(horizontal=12.dp,vertical=6.dp),
        verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)){
        Canvas(Modifier.size(28.dp)){
            val d=size.minDimension
            drawArc(c.coin,180f,180f,false,Offset(d*.27f,d*.05f),Size(d*.46f,d*.62f),style=Stroke(2.dp.toPx()))
            drawRoundRect(c.coin,Offset(d*.16f,d*.43f),Size(d*.68f,d*.49f),androidx.compose.ui.geometry.CornerRadius(d*.08f),style=Stroke(2.dp.toPx()))
            drawCircle(c.coin,d*.07f,Offset(d*.5f,d*.63f))
        }
        Column(Modifier.weight(1f)){
            Text(title,Type.headline,c.coin,maxLines=2)
            Text(detail,Type.footnote,c.ink,maxLines=2)
        }
    }
}

/** Status uses both shape and color; never rely on red/green alone. */
@Composable
internal fun PurchaseRequirement(label:String,detail:String,met:Boolean){
    val c=Theme.colors
    val tint=if(met)c.accent else c.danger
    Row(Modifier.fillMaxWidth().padding(vertical=2.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)){
        Canvas(Modifier.size(21.dp)){
            val d=size.minDimension
            drawCircle(tint.copy(alpha=.12f),d*.48f)
            drawCircle(tint,d*.43f,style=Stroke(1.3.dp.toPx()))
            if(met){
                drawLine(tint,Offset(d*.27f,d*.51f),Offset(d*.43f,d*.67f),2.dp.toPx(),StrokeCap.Round)
                drawLine(tint,Offset(d*.43f,d*.67f),Offset(d*.75f,d*.32f),2.dp.toPx(),StrokeCap.Round)
            }else{
                drawLine(tint,Offset(d*.32f,d*.32f),Offset(d*.68f,d*.68f),1.8.dp.toPx(),StrokeCap.Round)
                drawLine(tint,Offset(d*.68f,d*.32f),Offset(d*.32f,d*.68f),1.8.dp.toPx(),StrokeCap.Round)
            }
        }
        Text(label,Type.subhead,c.ink,Modifier.weight(1f))
        Text(detail,Type.footnote,tint)
    }
}
