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
    const val FIRST_RADIUS=267f
    const val RANK_STEP=98f
    private fun ids(includeLegacy:Boolean)=groups.flatMap{group->
        (0 until Upgrades.count).filter{(includeLegacy||!legacy(it))&&Upgrades.of(it).family==group}
    }
    /** Each type owns a separate angular sector. Fill a row of equally sized
     * circles, then continue outward on the closest end of the next row.
     * Serpentine order gives every rank exactly one predecessor and successor.
     * No rank, purchase, or offer can change this geometry. */
    private fun layout(includeLegacy:Boolean):Map<Int,List<RankPosition>>{
        val ids=ids(includeLegacy)
        val power=if(includeLegacy).9 else 1.0
        val weights=ids.associateWith{Upgrades.runCap(it).toDouble().pow(power)}
        val sum=weights.values.sum()
        val free=360.0-5.0*ids.size
        val out=linkedMapOf<Int,List<RankPosition>>()
        var angle=-90.0
        for(id in ids){
            val width=5.0+free*weights.getValue(id)/sum
            val cap=Upgrades.runCap(id)
            val ranks=mutableListOf<RankPosition>()
            var radius=FIRST_RADIUS
            while(ranks.size<cap){
                // Center clearance from each sector boundary and neighbors.
                val gap=2.0*asin((NODE_RADIUS+4f)/radius.toDouble())*180.0/PI
                val count=minOf(cap-ranks.size,(width/gap+1e-8).toInt())
                if(count>0){
                    val row=(0 until count).map{i->
                        point(radius,(angle+width*.5+(i-(count-1)*.5)*gap).toFloat())
                    }
                    val last=ranks.lastOrNull()
                    fun distance(p:Pair<Float,Float>)=if(last==null)0f else hypot(p.first-last.x,p.second-last.y)
                    val ordered=if(last!=null&&distance(row.last())<distance(row.first()))row.reversed()else row
                    for((x,y) in ordered)ranks.add(RankPosition(ranks.size+1,x,y))
                }
                radius+=RANK_STEP
                check(radius<5000f)
            }
            out[id]=ranks
            angle+=width
        }
        return out
    }
    private val regularLayout by lazy{layout(false)}
    private val developerLayout by lazy{layout(true)}
    private fun catalog(includeLegacy:Boolean)=if(includeLegacy)developerLayout else regularLayout
    fun positions(includeLegacy:Boolean=false):List<UpgradePosition> = catalog(includeLegacy).map{(id,ranks)->
        val first=ranks.first()
        UpgradePosition(id,first.x,first.y,Upgrades.of(id).family)
    }
    fun ranks(anchor:UpgradePosition,positions:List<UpgradePosition>,cap:Int=Upgrades.runCap(anchor.id)):List<RankPosition> =
        catalog(positions.any{legacy(it.id)}).getValue(anchor.id).take(cap)
    fun rankCount(includeLegacy:Boolean=false)=ids(includeLegacy).sumOf{Upgrades.runCap(it)}
    fun visibleCount(cap:Int,owned:Int,offered:Int)=cap.coerceAtLeast(0)
    /** Ranks are a contiguous prefix: the next roll always begins at owned+1. */
    fun ownedRank(rank:Int,owned:Int)=rank in 1..owned
    fun offeredRank(rank:Int,owned:Int,levels:Int)=rank>owned&&rank<=owned+levels.coerceAtLeast(0)
    fun extent(owned:List<Int>,offers:Map<Int,Int>,includeLegacy:Boolean=false):Float{
        val radius=catalog(includeLegacy).values.maxOf{nodes->nodes.maxOf{hypot(it.x,it.y)}}
        return maxOf(WIDTH,2f*(radius+80f))
    }
    /** Every type starts at the tank. Short sibling links make the five
     * families a coherent radial mesh without creating purchase prerequisites.
     * Reject links that would pass through any other rank's circle. */
    fun links(positions:List<UpgradePosition>):List<MapLink> = buildList{
        val all=positions.flatMap{p->ranks(p,positions).map{p.id to it}}
        for(root in positions)add(MapLink(HUB_ID,root.id))
        for(group in groups){
            val row=positions.filter{it.family==group}
            for((a,b) in row.zipWithNext()){
                val dx=b.x-a.x;val dy=b.y-a.y
                val len2=dx*dx+dy*dy
                if(len2<=0f||sqrt(len2)>300f)continue
                val clear=all.none{(id,node)->
                    if(node.rank==1&&(id==a.id||id==b.id))false else {
                        val t=(((node.x-a.x)*dx+(node.y-a.y)*dy)/len2).coerceIn(0f,1f)
                        hypot(node.x-a.x-t*dx,node.y-a.y-t*dy)<NODE_RADIUS+5f
                    }
                }
                if(clear)add(MapLink(a.id,b.id))
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
