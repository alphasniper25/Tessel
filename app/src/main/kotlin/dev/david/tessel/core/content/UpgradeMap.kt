package dev.david.tessel.core.content

import kotlin.math.*

/** A presentation graph, not a second progression engine. Links group related
 * existing upgrades and never gate the three normal random offers. */
data class UpgradeMapNode(val id:Int,val x:Float,val y:Float,val parent:Int=-1)
object UpgradeMap {
    const val WIDTH=1200f
    const val HEIGHT=620f
    val groups=listOf(CardFamily.GUN,CardFamily.HEALTH,CardFamily.RAM,CardFamily.MOBILITY,CardFamily.ECONOMY)
    fun legacy(id:Int)=id==Upgrades.MULTISHOT||id==Upgrades.VELOCITY
    fun label(family:CardFamily)=when(family){
        CardFamily.GUN->"Firepower";CardFamily.HEALTH->"Hull";CardFamily.RAM->"Impact";CardFamily.MOBILITY->"Mobility";CardFamily.ECONOMY->"Economy"
    }
    fun anchor(family:CardFamily):Pair<Float,Float>{
        val angle=(-90+groups.indexOf(family)*72)*PI/180
        return (cos(angle)*155).toFloat() to (sin(angle)*82).toFloat()
    }
    val nodes=buildList{
        for((index,family) in groups.withIndex()){
            val ids=(0 until Upgrades.count).filter{Upgrades.of(it).family==family&&!legacy(it)}
            val angle=(-90+index*72)*PI/180
            // Two rows along each sector. Large categories spread across more angular slots.
            val inner=(ids.size+1)/2
            for((i,id) in ids.withIndex()){
                val outer=i>=inner
                val count=if(outer)ids.size-inner else inner
                val j=if(outer)i-inner else i
                val fraction=if(count<=1)0.0 else j.toDouble()/(count-1)-.5
                val a=angle+fraction*(if(outer)1.03 else .88)
                val r=if(outer)510.0 else 315.0
                val x=(cos(a)*r).toFloat()
                val y=(sin(a)*r*.50).toFloat()
                add(UpgradeMapNode(id,x,y,if(outer)ids[(j*inner/count.coerceAtLeast(1)).coerceAtMost(inner-1)] else -1))
            }
        }
    }
}
