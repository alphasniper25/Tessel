package dev.david.tessel.ui.screens

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import dev.david.tessel.core.content.SkillBonus

/** One vector glyph per actual bonus. No images, fonts or icon-library dependency. */
internal fun DrawScope.skillGlyph(bonus:SkillBonus,at:Offset,r:Float,color:Color){
    val width=(r*.16f).coerceAtLeast(1f)
    fun p(x:Float,y:Float)=at+Offset(x*r,y*r)
    fun line(x:Float,y:Float,x2:Float,y2:Float)=drawLine(color,p(x,y),p(x2,y2),width,StrokeCap.Round)
    fun path(vararg points:Float,closed:Boolean=false){
        val q=Path();q.moveTo(p(points[0],points[1]).x,p(points[0],points[1]).y)
        var i=2
        while(i<points.size){val v=p(points[i],points[i+1]);q.lineTo(v.x,v.y);i+=2}
        if(closed)q.close()
        drawPath(q,color,style=Stroke(width,cap=StrokeCap.Round))
    }
    when(bonus){
        SkillBonus.FREE_REFRESH,SkillBonus.REROLL_DISCOUNT->{
            drawArc(color,35f,270f,false,p(-.83f,-.83f),Size(1.66f*r,1.66f*r),style=Stroke(width,cap=StrokeCap.Round))
            path(.34f,-.96f,.86f,-.52f,.26f,-.37f)
            if(bonus==SkillBonus.REROLL_DISCOUNT)line(-.35f,.10f,.35f,.10f)
        }
        SkillBonus.LUCK->{
            path(0f,-1f,.26f,-.3f,.95f,0f,.26f,.3f,0f,1f,-.26f,.3f,-.95f,0f,-.26f,-.3f,closed=true)
        }
        SkillBonus.CARD_DISCOUNT->{
            path(-.92f,-.5f,.2f,-.5f,.92f,.1f,.2f,.75f,-.92f,.75f,closed=true)
            drawCircle(color,r*.12f,p(.38f,.1f))
            line(-.62f,.12f,-.08f,.12f)
        }
        SkillBonus.HEAL_DISCOUNT,SkillBonus.HEAL_POWER->{
            path(-.25f,-.9f,.25f,-.9f,.25f,-.25f,.9f,-.25f,.9f,.25f,.25f,.25f,.25f,.9f,-.25f,.9f,-.25f,.25f,-.9f,.25f,-.9f,-.25f,-.25f,-.25f,closed=true)
            if(bonus==SkillBonus.HEAL_DISCOUNT)line(-.36f,.51f,.36f,.51f)
        }
        SkillBonus.HEALTH->path(-.8f,-.62f,0f,-.95f,.8f,-.62f,.7f,.28f,0f,.94f,-.7f,.28f,closed=true)
        SkillBonus.SPEED->{
            path(-.86f,-.65f,-.2f,0f,-.86f,.65f)
            path(-.13f,-.65f,.56f,0f,-.13f,.65f)
        }
        SkillBonus.PICKUP->{
            path(-.76f,-.75f,-.76f,.2f,-.4f,.73f,.4f,.73f,.76f,.2f,.76f,-.75f)
            line(-.76f,-.35f,-.43f,-.35f);line(.43f,-.35f,.76f,-.35f)
        }
        SkillBonus.DAMAGE->path(.18f,-1f,-.72f,.08f,-.08f,.08f,-.25f,1f,.76f,-.17f,.12f,-.17f,closed=true)
        SkillBonus.FIRE_RATE->{
            for(i in -1..1){val y=i*.52f;line(-.85f,y,.6f,y)}
            path(.30f,-.76f,.93f,0f,.30f,.76f)
        }
        SkillBonus.RANGE->{
            drawCircle(color,r*.73f,at,style=Stroke(width))
            line(-1f,0f,-.43f,0f);line(.43f,0f,1f,0f);line(0f,-1f,0f,-.43f);line(0f,.43f,0f,1f)
            drawCircle(color,r*.13f,at)
        }
    }
}
