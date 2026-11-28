package dev.david.tessel.core.content

import kotlin.math.*

/** Positions represent categories, never purchase prerequisites.
 * Stable upgrade IDs are independent of offers, rarity or purchase history. */
data class UpgradePosition(val id:Int,val x:Float,val y:Float,val family:CardFamily)
data class MapLink(val a:Int,val b:Int)
data class RankPosition(val rank:Int,val x:Float,val y:Float)
object UpgradeMap {
    // Equal axes deliberately keep the map circular, not stretched to the display.
    const val WIDTH=1080f
    const val HEIGHT=1080f
    const val HUB_ID=-2
    val groups=CardFamily.entries
    fun label(family:CardFamily)=when(family){
        CardFamily.GUN->"Firepower"
        CardFamily.HEALTH->"Hull"
        CardFamily.RAM->"Impact"
        CardFamily.MOBILITY->"Mobility"
        CardFamily.ECONOMY->"Economy"
    }
    fun legacy(id:Int)=id==Upgrades.MULTISHOT||id==Upgrades.VELOCITY
    fun angle(index:Int)=-90f+index*72f
    fun point(radius:Float,degrees:Float):Pair<Float,Float>{
        val a=degrees*PI.toFloat()/180f
        return radius*cos(a) to radius*sin(a)
    }
    const val NODE_RADIUS=30f
    const val FIRST_RADIUS=365f
    const val RANK_STEP=85f
    // A type has its own radial track. Every rank is the same size, not a satellite.
    fun positions(includeLegacy:Boolean=false):List<UpgradePosition>{
        val ids=groups.flatMap{group->(0 until Upgrades.count).filter{(includeLegacy||!legacy(it))&&Upgrades.of(it).family==group}}
        return ids.mapIndexed{i,id->val (x,y)=point(FIRST_RADIUS,-90f+360f*i/ids.size);UpgradePosition(id,x,y,Upgrades.of(id).family)}
    }
    fun ranks(anchor:UpgradePosition,positions:List<UpgradePosition>,cap:Int=Upgrades.runCap(anchor.id)):List<RankPosition>{
        val a=atan2(anchor.y,anchor.x)
        return (1..cap).map{rank->val r=FIRST_RADIUS+(rank-1)*RANK_STEP;RankPosition(rank,r*cos(a),r*sin(a))}
    }
    fun visibleCount(cap:Int,owned:Int,offered:Int)=minOf(cap,maxOf(1,owned+maxOf(1,offered)))
    fun extent(owned:List<Int>,offers:Map<Int,Int>,includeLegacy:Boolean=false):Float{
        val visible=positions(includeLegacy).maxOf{visibleCount(Upgrades.runCap(it.id),owned.getOrElse(it.id){0},offers[it.id]?:0)}
        return maxOf(WIDTH,2f*(FIRST_RADIUS+(visible-1)*RANK_STEP+100f))
    }
    fun links(positions:List<UpgradePosition>):List<MapLink> = buildList{
        for(group in groups){
            val row=positions.filter{it.family==group}
            for(n in row)add(MapLink(HUB_ID,n.id))
            row.zipWithNext().forEach{(a,b)->add(MapLink(a.id,b.id))}
        }
    }

}

data class MarketCamera(val zoom:Float=1f,val x:Float=0f,val y:Float=0f,val extent:Float=UpgradeMap.WIDTH){
    fun scale(w:Float,h:Float)=min(w/extent,h/extent).coerceAtLeast(.001f)*zoom
    fun sx(a:Float,w:Float,h:Float)=w*.5f+x+a*scale(w,h)
    fun sy(a:Float,w:Float,h:Float)=h*.5f+y+a*scale(w,h)
    fun gesture(w:Float,h:Float,cx:Float,cy:Float,dx:Float,dy:Float,factor:Float):MarketCamera{
        val z=(zoom*factor).coerceIn(1f,maxOf(5.5f,extent/UpgradeMap.WIDTH*5.5f));val ratio=z/zoom;val sc=scale(w,h)*ratio
        val bx=max(0f,(extent*sc-w)*.5f)+w*.12f
        val by=max(0f,(extent*sc-h)*.5f)+h*.12f
        return MarketCamera(z,
            (cx-w*.5f-(cx-w*.5f-x)*ratio+dx).coerceIn(-bx,bx),
            (cy-h*.5f-(cy-h*.5f-y)*ratio+dy).coerceIn(-by,by),extent)
    }
}
