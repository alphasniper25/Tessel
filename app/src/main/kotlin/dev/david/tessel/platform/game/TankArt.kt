package dev.david.tessel.platform.game

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import dev.david.tessel.core.content.TankEvolution
import dev.david.tessel.core.content.CharacterDef
import dev.david.tessel.core.content.GarageCatalog
import dev.david.tessel.core.content.TankLook
import dev.david.tessel.core.content.HullVisual
import dev.david.tessel.core.content.SkinVisual
import kotlin.math.*

/** Shared, allocation-free geometry for the garage preview and game. Radius is visual only. */
class TankArt {
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val line = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val oval = RectF()
    private val hull = Path()
    private val facetPath = Path()
    private val cosmeticFacets = IntArray(GarageCatalog.items.size) { i -> dev.david.tessel.design.mix(GarageCatalog.items[i].color, 0xFFFFFFFF.toInt(), .25f) }
    private val vertices = HullVisual.entries.map { shape ->
        val n = if (shape == HullVisual.HEXAGON) 6 else 4
        val start = if (shape == HullVisual.SQUARE) PI/4 else -PI/2
        FloatArray(n*2) { i -> val a=start+(i/2)*2*PI/n; if(i%2==0) cos(a).toFloat() else sin(a).toFloat() }
    }
    fun draw(c: Canvas, x: Float, y: Float, r: Float, ax: Float, ay: Float, character: CharacterDef, look: TankLook, defaultBody: Int, defaultFacet: Int, ground: Int, hurt: Boolean = false, alpha: Int = 255, evolution: TankEvolution = TankEvolution.BASIC, shieldCooldown: Int = 0) {
        val paint = GarageCatalog.items.getOrNull(look.paint)?.color ?: 0
        val body = if (paint == 0 || hurt) defaultBody else paint
        val facet = if (paint == 0 || hurt) defaultFacet else cosmeticFacets[look.paint]
        line.color = body; line.alpha = alpha; line.strokeCap = Paint.Cap.ROUND; line.strokeWidth = r * .5f
        val turret = GarageCatalog.items[look.turret]
        val n=if(evolution.drones) minOf(4,evolution.barrels) else evolution.barrels
        val aim=atan2(ay,ax)
        val length=if(evolution==TankEvolution.BASIC) turret.barrelLength else evolution.length
        for(k in 0 until n){
            val a=evolution.originAngle(aim,k,n); val dx=cos(a); val dy=sin(a)
            val off=if(evolution.drones)0f else (k-(n-1)/2f)*evolution.lateral*r; val ox=-ay*off; val oy=ax*off
            if(evolution.drones){
                val cx=x+dx*r*1.35f+ox; val cy=y+dy*r*1.35f+oy
                line.strokeWidth=r*.30f; c.drawLine(x+dx*r*.65f,y+dy*r*.65f,x+dx*r*1.35f,y+dy*r*1.35f,line)
                fill.color=body;fill.alpha=alpha;hull.reset();hull.moveTo(cx+dx*r*.36f,cy+dy*r*.36f)
                hull.lineTo(cx-dx*r*.30f-dy*r*.27f,cy-dy*r*.30f+dx*r*.27f);hull.lineTo(cx-dx*r*.30f+dy*r*.27f,cy-dy*r*.30f-dx*r*.27f);hull.close();c.drawPath(hull,fill)
            } else {
                line.strokeWidth=r*(if(n>1) .25f else if(evolution.damage>=4f) .72f else .5f)
                c.drawLine(x+ox+dx*r*.35f,y+oy+dy*r*.35f,x+ox+dx*r*length,y+oy+dy*r*length,line)
            }
        }
        val shape = GarageCatalog.hull(look,character)
        fill.color = body; fill.alpha = alpha
        if (shape == HullVisual.CIRCLE) {
            c.drawCircle(x,y,r,fill); fill.color=facet; fill.alpha=alpha
            oval.set(x-r,y-r,x+r,y+r); c.drawArc(oval, ROUND_SPLITS[0], ROUND_SPLITS[1]-ROUND_SPLITS[0], true, fill)
            line.color=ground; line.alpha=alpha; line.strokeCap=Paint.Cap.BUTT; line.strokeWidth=r*.08f
            for (k in 0..2) c.drawLine(x,y,x+SPLIT_COS[k]*r*1.02f,y+SPLIT_SIN[k]*r*1.02f,line)
        } else {
            val v = vertices[shape.ordinal]; val n = v.size/2
            hull.reset(); facetPath.reset(); facetPath.moveTo(x,y)
            for(k in 0 until n) { val px=x+v[k*2]*r; val py=y+v[k*2+1]*r
                if(k==0) hull.moveTo(px,py) else hull.lineTo(px,py)
                if(k<=2) facetPath.lineTo(px,py) }
            hull.close(); facetPath.close(); c.drawPath(hull,fill); fill.color=facet; fill.alpha=alpha; c.drawPath(facetPath,fill)
        }
        line.color=ground; line.alpha=alpha; line.strokeWidth=r*.10f
        if(GarageCatalog.items[look.skin].pattern==SkinVisual.STRIPE) c.drawLine(x-r*.55f,y+r*.35f,x+r*.55f,y-r*.35f,line)
        if(GarageCatalog.items[look.skin].pattern==SkinVisual.RING) c.drawCircle(x,y,r*.58f,line)
        if(evolution.melee){
            line.color=if(shieldCooldown==0) facet else body;line.alpha=if(shieldCooldown==0) 255 else 100
            line.strokeWidth=r*.11f;oval.set(x-r*1.35f,y-r*1.35f,x+r*1.35f,y+r*1.35f)
            c.drawArc(oval,-90f,if(shieldCooldown==0) 360f else (360f*(1f-shieldCooldown/(evolution.shieldSeconds*60f))).coerceIn(0f,360f),false,line)
        }
        fill.alpha=255; line.alpha=255
    }
}
