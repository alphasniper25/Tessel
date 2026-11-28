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
    const val NODE_RADIUS=37f
    const val FIRST_RADIUS=365f
    const val RANK_STEP=98f
    // A type has its own radial track. Every rank is the same size, not a satellite.
    fun positions(includeLegacy:Boolean=false):List<UpgradePosition>{
        val ids=groups.flatMap{group->(0 until Upgrades.count).filter{(includeLegacy||!legacy(it))&&Upgrades.of(it).family==group}}
        return ids.mapIndexed{i,id->val (x,y)=point(FIRST_RADIUS,-90f+360f*i/ids.size);UpgradePosition(id,x,y,Upgrades.of(id).family)}
    }
    /** Complete catalog, not just the owned/rolled frontier. Keep the original
     * type ring, then pack all remaining ranks into successive concentric rings.
     * Each ring allocates contiguous runs of equal-sized nodes to upgrade types.
     * Allocation uses caps only, so purchases and rerolls never move a node. */
    private fun layout(positions:List<UpgradePosition>):Map<Int,List<RankPosition>>{
        val out=positions.associate{it.id to mutableListOf(RankPosition(1,it.x,it.y))}
        var ring=1
        while(true){
            val active=positions.filter{out.getValue(it.id).size<Upgrades.runCap(it.id)}
            if(active.isEmpty())break
            val radius=FIRST_RADIUS+ring*RANK_STEP
            // Chord spacing leaves at least 8 units between node borders.
            val capacity=(PI/asin((NODE_RADIUS+4f)/radius.toDouble())).toInt()
            val remaining=active.associate{it.id to Upgrades.runCap(it.id)-out.getValue(it.id).size}
            val slots=active.associate{it.id to 1}.toMutableMap()
            val count=minOf(capacity,remaining.values.sum())
            check(count>=active.size)
            repeat(count-active.size){
                val next=active.filter{slots.getValue(it.id)<remaining.getValue(it.id)}
                    .maxByOrNull{remaining.getValue(it.id).toDouble()/(slots.getValue(it.id)+1)}!!
                slots[next.id]=slots.getValue(next.id)+1
            }
            // Stable ordered arcs; rotate the ring toward the original type
            // bearings so long tracks fill outer space without enormous spokes.
            val step=360f/count
            var at=0;var dx=0.0;var dy=0.0
            for(n in active){
                val size=slots.getValue(n.id)
                val desired=atan2(n.y,n.x)*180.0/PI
                val delta=(desired-(-90f+(at+(size-1)*.5f)*step))*PI/180
                dx+=size*cos(delta);dy+=size*sin(delta);at+=size
            }
            val rotation=(atan2(dy,dx)*180.0/PI).toFloat()
            at=0
            for(n in active){
                val row=out.getValue(n.id)
                repeat(slots.getValue(n.id)){
                    val (x,y)=point(radius,-90f+rotation+step*at++)
                    row.add(RankPosition(row.size+1,x,y))
                }
            }
            ring++
        }
        return out
    }
    private val regularLayout by lazy{layout(positions(false))}
    private val developerLayout by lazy{layout(positions(true))}
    fun ranks(anchor:UpgradePosition,positions:List<UpgradePosition>,cap:Int=Upgrades.runCap(anchor.id)):List<RankPosition>{
        val catalog=if(positions.any{legacy(it.id)})developerLayout else regularLayout
        return catalog.getValue(anchor.id).take(cap)
    }
    fun rankCount(includeLegacy:Boolean=false)=positions(includeLegacy).sumOf{Upgrades.runCap(it.id)}
    fun visibleCount(cap:Int,owned:Int,offered:Int)=cap.coerceAtLeast(0)
    fun extent(owned:List<Int>,offers:Map<Int,Int>,includeLegacy:Boolean=false):Float{
        val all=if(includeLegacy)developerLayout else regularLayout
        val radius=all.values.maxOf{nodes->nodes.maxOf{hypot(it.x,it.y)}}
        return maxOf(WIDTH,2f*(radius+80f))
    }
    fun links(positions:List<UpgradePosition>):List<MapLink> = buildList{
        for(group in groups){
            val row=positions.filter{it.family==group}
            if(row.isNotEmpty()){
                val middle=row.size/2
                add(MapLink(HUB_ID,row[middle].id))
                // Orient both arms outward from their shared root. Connections
                // remain visual only, but every track is reachable from the hub.
                for(i in middle downTo 1)add(MapLink(row[i].id,row[i-1].id))
                for(i in middle until row.lastIndex)add(MapLink(row[i].id,row[i+1].id))
            }
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
