package dev.david.tessel.core.content

import kotlin.math.*

/** Stable node IDs. The center is a free navigation hub, not a purchased node.
 * Six nine-node sectors and six bridges share one per-seat 60-bit mask.
 * Coin costs are fixed at a node's minimum stage, so changing route later is not penalized. */
enum class SkillBonus {
    FREE_REFRESH, LUCK, CARD_DISCOUNT, REROLL_DISCOUNT, HEAL_DISCOUNT,
    HEALTH, HEAL_POWER, SPEED, PICKUP, DAMAGE, FIRE_RATE, RANGE;
    fun description(amount:Int):String=when(this){
        FREE_REFRESH -> "+$amount free refresh per shop"
        LUCK -> "+$amount percentage point(s) each to Epic and Legendary initial rolls"
        CARD_DISCOUNT -> "+$amount% discount on newly priced upgrade cards"
        REROLL_DISCOUNT -> "+$amount% discount on paid shop refreshes"
        HEAL_DISCOUNT -> "+$amount% discount on shop healing"
        HEALTH -> "+$amount maximum HP (filled if alive)"
        HEAL_POWER -> "+$amount HP restored per purchased heal"
        SPEED -> "+$amount% movement speed"
        PICKUP -> "+$amount% coin pickup radius"
        DAMAGE -> "+$amount% bullet, drone impact and ram damage"
        FIRE_RATE -> "+$amount% firing and drone replacement rate"
        RANGE -> "+$amount% projectile range; half bonus to drone command range"
    }
    fun mark(amount:Int):String=when(this){
        FREE_REFRESH -> "R+$amount"
        LUCK -> "L+$amount"
        CARD_DISCOUNT -> "C-$amount"
        REROLL_DISCOUNT -> "R-$amount"
        HEAL_DISCOUNT -> "H-$amount"
        HEALTH -> "HP+$amount"
        HEAL_POWER -> "H+$amount"
        SPEED -> "V+$amount"
        PICKUP -> "M+$amount"
        DAMAGE -> "D+$amount"
        FIRE_RATE -> "F+$amount"
        RANGE -> "A+$amount"
    }
}
enum class SkillSector(val label:String,val short:String) {
    REFRESH("Refresh","R"), FORTUNE("Fortune","F"), TRADE("Trade","T"),
    HULL("Hull","H"), MOBILITY("Mobility","M"), FIREPOWER("Firepower","P")
}
data class SkillNode(val id:Int,val name:String,val sector:SkillSector,val bonus:SkillBonus,
    val amount:Int,val minStage:Int,val price:Int,val x:Float,val y:Float,val bridge:Boolean=false) {
    val description get()=bonus.description(amount)
    val root get()=!bridge && id%9==0
}
data class SkillEdge(val a:Int,val b:Int)
object SkillGraph {
    const val COUNT=60
    const val FIRST_STAGE=3
    const val MASK=(1L shl COUNT)-1L
    const val WIDTH=2080f
    const val HEIGHT=1200f
    private data class Spec(val name:String,val effect:SkillBonus,val amount:Int)
    private fun s(name:String,effect:SkillBonus,amount:Int)=Spec(name,effect,amount)
    private val sectors=listOf(
        listOf(s("Quick Refresh",SkillBonus.FREE_REFRESH,1),s("Double Refresh",SkillBonus.FREE_REFRESH,1),s("Efficient Cycle",SkillBonus.REROLL_DISCOUNT,5),s("Triple Refresh",SkillBonus.FREE_REFRESH,1),s("Fast Refit",SkillBonus.REROLL_DISCOUNT,5),s("Shop Circuit",SkillBonus.REROLL_DISCOUNT,5),s("Lean Rotation",SkillBonus.REROLL_DISCOUNT,5),s("Recycling",SkillBonus.REROLL_DISCOUNT,5),s("Perfect Cycle",SkillBonus.REROLL_DISCOUNT,10)),
        listOf(s("Lucky Find",SkillBonus.LUCK,2),s("Fortune Hunter",SkillBonus.LUCK,2),s("Treasure Magnet",SkillBonus.PICKUP,5),s("Golden Chance",SkillBonus.LUCK,2),s("Rare Signal",SkillBonus.LUCK,1),s("Salvage Field",SkillBonus.PICKUP,5),s("Fate Shift",SkillBonus.LUCK,1),s("Prospector",SkillBonus.PICKUP,5),s("Jackpot",SkillBonus.LUCK,2)),
        listOf(s("Haggling",SkillBonus.CARD_DISCOUNT,5),s("Wholesale",SkillBonus.CARD_DISCOUNT,5),s("Medical Deal",SkillBonus.HEAL_DISCOUNT,5),s("Master Trader",SkillBonus.CARD_DISCOUNT,5),s("Supply Cache",SkillBonus.HEAL_DISCOUNT,5),s("Market Access",SkillBonus.CARD_DISCOUNT,3),s("Field Contract",SkillBonus.HEAL_DISCOUNT,5),s("Bulk Purchase",SkillBonus.CARD_DISCOUNT,2),s("Quartermaster",SkillBonus.HEAL_DISCOUNT,10)),
        listOf(s("Field Medic",SkillBonus.HEAL_POWER,1),s("Reinforced Hull",SkillBonus.HEALTH,1),s("First Aid",SkillBonus.HEAL_DISCOUNT,3),s("Alloy Plating",SkillBonus.HEALTH,1),s("Repair Kit",SkillBonus.HEAL_POWER,1),s("Composite Hull",SkillBonus.HEALTH,1),s("Trauma Care",SkillBonus.HEAL_DISCOUNT,5),s("Heavy Plating",SkillBonus.HEALTH,1),s("Titan Core",SkillBonus.HEALTH,2)),
        listOf(s("Thrusters",SkillBonus.SPEED,2),s("Tractor Beam",SkillBonus.PICKUP,5),s("Agility",SkillBonus.SPEED,2),s("Vector Drive",SkillBonus.SPEED,2),s("Attractor",SkillBonus.PICKUP,8),s("Quick Turn",SkillBonus.SPEED,2),s("Afterburner",SkillBonus.SPEED,3),s("Salvage Sweep",SkillBonus.PICKUP,10),s("Hyperdrive",SkillBonus.SPEED,4)),
        listOf(s("Power Cell",SkillBonus.DAMAGE,2),s("Pulse Control",SkillBonus.FIRE_RATE,2),s("Impact Core",SkillBonus.DAMAGE,3),s("Overdrive",SkillBonus.FIRE_RATE,3),s("Targeting Array",SkillBonus.RANGE,4),s("High Energy",SkillBonus.DAMAGE,3),s("Rapid Reactor",SkillBonus.FIRE_RATE,4),s("Amplifier",SkillBonus.DAMAGE,4),s("Apex Reactor",SkillBonus.DAMAGE,5))
    )
    private val bridges=listOf(s("Scavenger",SkillBonus.PICKUP,5),s("Rare Market",SkillBonus.LUCK,1),s("Field Supplies",SkillBonus.HEAL_DISCOUNT,5),s("Light Armor",SkillBonus.HEALTH,1),s("Kinetic Drive",SkillBonus.SPEED,2),s("Rapid Refit",SkillBonus.FIRE_RATE,2))
    private val radii=floatArrayOf(180f,310f,310f,450f,450f,450f,610f,610f,780f)
    private val angleOffset=floatArrayOf(0f,-13f,13f,-18f,0f,18f,-13f,13f,0f)
    private val stages=intArrayOf(3,7,7,13,13,13,23,23,35)
    private val bases=intArrayOf(48,58,58,75,75,75,95,95,125)
    private fun position(radius:Float,angle:Float):Pair<Float,Float>{
        val a=angle*PI.toFloat()/180f
        return radius*cos(a)*1.25f to radius*sin(a)*.67f
    }
    private fun price(base:Int,stage:Int)=(base*Upgrades.inflation(stage)).roundToInt()
    val nodes:List<SkillNode> = buildList {
        for(sector in SkillSector.entries){
            val k=sector.ordinal
            for(i in 0..8){
                val spec=sectors[k][i]
                val (x,y)=position(radii[i],-150f+k*60f+angleOffset[i])
                add(SkillNode(k*9+i,spec.name,sector,spec.effect,spec.amount,stages[i],price(bases[i],stages[i]),x,y))
            }
        }
        for(k in 0..5){
            val spec=bridges[k]
            val (x,y)=position(470f,-120f+k*60f)
            add(SkillNode(54+k,spec.name,SkillSector.entries[k],spec.effect,spec.amount,17,price(85,17),x,y,true))
        }
    }
    val edges:List<SkillEdge> = buildList {
        for(k in 0..5){
            val offset=k*9
            for((a,b) in listOf(0 to 1,0 to 2,1 to 3,1 to 4,2 to 4,2 to 5,3 to 6,4 to 6,4 to 7,5 to 7,6 to 8,7 to 8))
                add(SkillEdge(offset+a,offset+b))
            add(SkillEdge(offset+5,54+k))
            add(SkillEdge(54+k,((k+1)%6)*9+3))
        }
    }
    private val adjacency=LongArray(COUNT).also{ a->
        for(edge in edges){a[edge.a]=a[edge.a] or bit(edge.b);a[edge.b]=a[edge.b] or bit(edge.a)}
    }
    init { require(nodes.size==COUNT && nodes.indices.all{nodes[it].id==it}) }
    fun bit(id:Int)=if(id in 0 until COUNT) 1L shl id else 0L
    fun owns(mask:Long,id:Int)=mask and bit(id)!=0L
    fun connected(mask:Long,id:Int)=nodes.getOrNull(id)?.let{it.root || mask and adjacency[id]!=0L}?:false
    fun eligible(mask:Long,id:Int,stage:Int):Boolean{
        val n=nodes.getOrNull(id)?:return false
        return !owns(mask,id) && stage>=n.minStage && connected(mask,id)
    }
    fun canBuy(mask:Long,id:Int,stage:Int,coins:Int)=eligible(mask,id,stage) && coins>=nodes[id].price
    fun neighbors(id:Int):List<SkillNode> = if(id in 0 until COUNT)nodes.filter{adjacency[id] and bit(it.id)!=0L}else emptyList()
    fun count(mask:Long)=java.lang.Long.bitCount(mask and MASK)
    fun fill(mask:Long,out:IntArray){
        out.fill(0)
        for(node in nodes)if(owns(mask,node.id))out[node.bonus.ordinal]+=node.amount
    }
    fun totals(mask:Long)=IntArray(SkillBonus.entries.size).also{fill(mask,it)}
    /** Preserve each purchased legacy rank with the equivalent root/left/inner path, no coin charge. */
    fun fromLegacy(perks:Int):Long{
        var mask=0L
        for(branch in 0..2){
            val rank=SkillTree.rank(perks,branch)
            val path=intArrayOf(0,1,3)
            for(i in 0 until rank)mask=mask or bit(branch*9+path[i])
        }
        return mask
    }
}
