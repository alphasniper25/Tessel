package dev.david.tessel.ui.screens

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import dev.david.tessel.core.content.*

internal enum class MarketIcon(val skill:SkillBonus?=null){
    DAMAGE(SkillBonus.DAMAGE), FIRE(SkillBonus.FIRE_RATE), RANGE(SkillBonus.RANGE), MOVE(SkillBonus.SPEED),
    HULL(SkillBonus.HEALTH), HEAL(SkillBonus.HEAL_POWER), MAGNET(SkillBonus.PICKUP), DEAL(SkillBonus.CARD_DISCOUNT),
    LUCK(SkillBonus.LUCK), REFRESH(SkillBonus.FREE_REFRESH), PIERCE, CALIBER, COIN, RAM, DRONE, REGEN, VOLLEY, SHIELD, WAVE, LINK;
    companion object{
        fun upgrade(id:Int)=when(Upgrades.of(id).stat){
            UpgradeStat.DAMAGE,UpgradeStat.DAMAGE_BONUS->DAMAGE
            UpgradeStat.FIRE_RATE,UpgradeStat.VELOCITY->FIRE
            UpgradeStat.VOLLEY->VOLLEY
            UpgradeStat.PIERCE->PIERCE
            UpgradeStat.RANGE->RANGE
            UpgradeStat.SPEED,UpgradeStat.SPEED_BONUS->MOVE
            UpgradeStat.MAX_HP->HULL
            UpgradeStat.WEALTH->COIN
            UpgradeStat.MAGNET->MAGNET
            UpgradeStat.CALIBER->CALIBER
            UpgradeStat.RAM->RAM
            UpgradeStat.HEAL->HEAL
            UpgradeStat.REGEN->REGEN
            UpgradeStat.SHIELD_RECHARGE->SHIELD
            UpgradeStat.DRONE_POWER->DRONE
        }
        fun perk(id:Int)=when(id){
            Perks.SECOND_WIND->REGEN;Perks.THICK_SKIN->HULL;Perks.SIPHON->HEAL
            Perks.OVERCLOCK->FIRE;Perks.HEAVY_ROUNDS->PIERCE;Perks.QUICK_FEET->MOVE
            Perks.MAGNET_CORE->MAGNET;Perks.LAST_STAND->DAMAGE;Perks.SHOCKWAVE->WAVE;else->DEAL
        }
    }
}
internal fun DrawScope.marketGlyph(icon:MarketIcon,at:Offset,r:Float,color:Color){
    icon.skill?.let{skillGlyph(it,at,r,color);return}
    val stroke=(r*.16f).coerceAtLeast(1f)
    fun p(x:Float,y:Float)=at+Offset(x*r,y*r)
    fun l(x:Float,y:Float,xx:Float,yy:Float)=drawLine(color,p(x,y),p(xx,yy),stroke,StrokeCap.Round)
    fun path(vararg points:Float,closed:Boolean=false){
        val q=Path();q.moveTo(p(points[0],points[1]).x,p(points[0],points[1]).y)
        var i=2;while(i<points.size){val v=p(points[i],points[i+1]);q.lineTo(v.x,v.y);i+=2}
        if(closed)q.close();drawPath(q,color,style=Stroke(stroke,cap=StrokeCap.Round))
    }
    when(icon){
        MarketIcon.PIERCE->{l(-.9f,0f,.9f,0f);path(.4f,-.5f,.95f,0f,.4f,.5f);l(-.35f,-.65f,-.35f,.65f)}
        MarketIcon.CALIBER->{drawCircle(color,r*.82f,at,style=Stroke(stroke));drawCircle(color,r*.33f,at)}
        MarketIcon.COIN->{drawCircle(color,r*.9f,at,style=Stroke(stroke));l(0f,-.43f,0f,.43f)}
        MarketIcon.RAM->{path(-.8f,-.65f,.2f,-.65f,.9f,0f,.2f,.65f,-.8f,.65f,closed=true);l(-.5f,0f,.44f,0f)}
        MarketIcon.DRONE->{path(0f,-.95f,.95f,.75f,-.95f,.75f,closed=true);drawCircle(color,r*.12f,at)}
        MarketIcon.REGEN->{drawArc(color,35f,265f,false,p(-.9f,-.9f),Size(1.8f*r,1.8f*r),style=Stroke(stroke));path(.32f,-.96f,.86f,-.45f,.25f,-.30f);l(-.35f,0f,.35f,0f);l(0f,-.35f,0f,.35f)}
        MarketIcon.VOLLEY->{for(y in listOf(-.55f,0f,.55f)){l(-.8f,y,.85f,y);path(.5f,y-.2f,.85f,y,.5f,y+.2f)}}
        MarketIcon.SHIELD->{skillGlyph(SkillBonus.HEALTH,at,r,color);l(-.35f,0f,.35f,0f);l(0f,-.35f,0f,.35f)}
        MarketIcon.WAVE->{drawCircle(color,r*.3f,at);drawCircle(color,r*.88f,at,style=Stroke(stroke));for(i in 0..3){val a=i*1.5707963f;l(kotlin.math.cos(a)*.5f,kotlin.math.sin(a)*.5f,kotlin.math.cos(a),kotlin.math.sin(a))}}
        MarketIcon.LINK->{drawCircle(color,r*.48f,p(-.42f,0f),style=Stroke(stroke));drawCircle(color,r*.48f,p(.42f,0f),style=Stroke(stroke));l(-.25f,0f,.25f,0f)}
        else->Unit
    }
}
