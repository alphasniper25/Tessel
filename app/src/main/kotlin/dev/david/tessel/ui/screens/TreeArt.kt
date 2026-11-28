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
