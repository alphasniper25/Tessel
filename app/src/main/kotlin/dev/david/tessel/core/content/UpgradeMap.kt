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
    fun positions(includeLegacy:Boolean=false):List<UpgradePosition> = buildList{
        for(group in groups){
            val ids=(0 until Upgrades.count).filter{(includeLegacy||!legacy(it))&&Upgrades.of(it).family==group}
            val a=angle(group.ordinal)
            var cursor=0
            // Each discipline has a central root and fans into successive rings.
            for((ring,capacity) in listOf(1,2,3,6).withIndex()){
                val count=min(capacity,ids.size-cursor)
                if(count<=0)break
                val radius=listOf(125f,225f,325f,425f)[ring]
                for(i in 0 until count){
                    val offset=if(count==1)0f else -26f+52f*i/(count-1)
                    val (x,y)=point(radius,a+offset)
                    add(UpgradePosition(ids[cursor++],x,y,group))
                }
            }
            check(cursor==ids.size)
        }
    }
    /** Every attainable rank gets its own socket around its type icon. The local
     * orbit shrinks to the nearest neighboring track's space, preventing overlap.
     * Dense tracks fan into two or three nested circles; zoom to inspect them. */
    fun ranks(anchor:UpgradePosition,positions:List<UpgradePosition>,cap:Int=Upgrades.runCap(anchor.id)):List<RankPosition> {
        val gap=positions.filter{it.id!=anchor.id}.minOfOrNull{hypot(it.x-anchor.x,it.y-anchor.y)}?:110f
        val reach=(gap*.43f).coerceIn(23f,55f)
        if(cap<=0)return emptyList()
        return buildList{
            val rings=if(cap<=12)1 else if(cap<=28)2 else 3
            val counts=when(rings){1->listOf(cap);2->listOf(min(10,cap),cap-min(10,cap));else->listOf(8,12,cap-20)}
            var rank=1
            for((ring,count) in counts.withIndex()){
                val radius=if(rings==1)reach else reach*(.58f+.42f*ring/(rings-1))
                for(i in 0 until count){
                    val a=-90f+360f*i/count+if(ring%2==1)180f/count else 0f
                    val (x,y)=point(radius,a)
                    add(RankPosition(rank++,anchor.x+x,anchor.y+y))
                }
            }
        }
    }
    fun links(positions:List<UpgradePosition>):List<MapLink> = buildList{
        for(group in groups){
            val ring=positions.filter{it.family==group}.groupBy{hypot(it.x,it.y).roundToInt()}.toSortedMap().values.toList()
            if(ring.isEmpty())continue
            add(MapLink(HUB_ID,ring.first().first().id))
            for(i in 1 until ring.size){
                for(n in ring[i]){
                    val parents=ring[i-1].sortedBy{(it.x-n.x).pow(2)+(it.y-n.y).pow(2)}
                    add(MapLink(parents.first().id,n.id))
                    if(parents.size>1&&i>1)add(MapLink(parents[1].id,n.id))
                }
                ring[i].sortedBy{atan2(it.y,it.x)}.zipWithNext().forEach{(a,b)->add(MapLink(a.id,b.id))}
            }
        }
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
