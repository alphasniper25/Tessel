package dev.david.tessel.core.content

import kotlin.math.*

/** Positions represent categories, never purchase prerequisites.
 * Stable upgrade IDs are independent of offers, rarity or purchase history. */
data class UpgradePosition(val id:Int,val x:Float,val y:Float,val family:CardFamily)
data class MapLink(val a:Int,val b:Int)
object UpgradeMap {
    const val WIDTH=1320f
    const val HEIGHT=590f
    val groups=CardFamily.entries
    fun label(family:CardFamily)=when(family){
        CardFamily.GUN->"Firepower"
        CardFamily.HEALTH->"Hull"
        CardFamily.RAM->"Impact"
        CardFamily.MOBILITY->"Mobility"
        CardFamily.ECONOMY->"Economy"
    }
    fun legacy(id:Int)=id==Upgrades.MULTISHOT||id==Upgrades.VELOCITY
    fun positions(includeLegacy:Boolean=false):List<UpgradePosition> = buildList{
        for(group in groups){
            val ids=(0 until Upgrades.count).filter{(includeLegacy||!legacy(it))&&Upgrades.of(it).family==group}
            val (lo,hi,top,bottom)=when(group){
                CardFamily.GUN->listOf(-560f,-70f,-190f,-63f)
                CardFamily.HEALTH->listOf(110f,530f,-190f,-63f)
                CardFamily.RAM->listOf(-540f,-325f,164f,164f)
                CardFamily.MOBILITY->listOf(-110f,105f,164f,164f)
                CardFamily.ECONOMY->listOf(320f,535f,164f,164f)
            }
            val two=group==CardFamily.GUN||group==CardFamily.HEALTH
            val cols=if(two)(ids.size+1)/2 else ids.size
            for((i,id) in ids.withIndex()){
                val row=i/cols;val col=i%cols
                val x=if(cols<=1)(lo+hi)*.5f else lo+(hi-lo)*col/(cols-1)
                add(UpgradePosition(id,x,if(row==0)top else bottom,group))
            }
        }
    }
    fun links(positions:List<UpgradePosition>):List<MapLink> = buildList{
        val reps=mutableListOf<UpgradePosition>()
        for(group in groups){
            val rows=positions.filter{it.family==group}.groupBy{it.y}.values.map{it.sortedBy{n->n.x}}
            for(row in rows)row.zipWithNext().forEach{(a,b)->add(MapLink(a.id,b.id))}
            if(rows.size>1)for(a in rows[0]){
                val b=rows[1].minByOrNull{abs(it.x-a.x)}!!
                add(MapLink(a.id,b.id))
            }
            rows.lastOrNull()?.let{row->reps.add(row[row.size/2])}
        }
        // Connect each category through the central trunk. No additional gating.
        reps.zipWithNext().forEach{(a,b)->add(MapLink(a.id,b.id))}
    }
}

data class MarketCamera(val zoom:Float=1f,val x:Float=0f,val y:Float=0f){
    fun scale(w:Float,h:Float)=min(w/UpgradeMap.WIDTH,h/UpgradeMap.HEIGHT).coerceAtLeast(.001f)*zoom
    fun sx(a:Float,w:Float,h:Float)=w*.5f+x+a*scale(w,h)
    fun sy(a:Float,w:Float,h:Float)=h*.5f+y+a*scale(w,h)
    fun gesture(w:Float,h:Float,cx:Float,cy:Float,dx:Float,dy:Float,factor:Float):MarketCamera{
        val z=(zoom*factor).coerceIn(1f,5.5f);val ratio=z/zoom;val sc=scale(w,h)*ratio
        val bx=max(0f,(UpgradeMap.WIDTH*sc-w)*.5f)+w*.12f
        val by=max(0f,(UpgradeMap.HEIGHT*sc-h)*.5f)+h*.12f
        return MarketCamera(z,
            (cx-w*.5f-(cx-w*.5f-x)*ratio+dx).coerceIn(-bx,bx),
            (cy-h*.5f-(cy-h*.5f-y)*ratio+dy).coerceIn(-by,by))
    }
}
