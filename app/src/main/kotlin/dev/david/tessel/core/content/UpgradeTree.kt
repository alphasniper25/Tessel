package dev.david.tessel.core.content

import kotlin.math.roundToInt

enum class ShopMode(val label:String){ CLASSIC("Card shop"), TREE("Upgrade tree") }
enum class UpgradeSector(val title:String){
    POWER("Power"), HULL("Hull & repair"), MOBILITY("Mobility"), ECONOMY("Economy"),
    WEAPON("Weapons"), DRONES("Drone command"), RAM("Impact")
}
data class UpgradeLane(val upgrade:Int,val sector:UpgradeSector,val ranks:Int,val firstStage:Int,val stageStep:Int,
    val parent:Int=-1,val parentRank:Int=1,val specialized:Boolean=false)
data class UpgradeNode(val id:Int,val lane:UpgradeLane,val rank:Int){
    val upgrade get()=lane.upgrade
}
enum class UpgradeDiscovery { HIDDEN, REVEALED, OWNED }
/** Rank IDs are stable and the single authoritative ownership is the existing
 * per-upgrade level array. There is no second currency, rarity, RNG or duplicated save state. */
object UpgradeTree{
    private fun lane(id:Int,sector:UpgradeSector,first:Int=1,step:Int=3,parent:Int=-1,parentRank:Int=1,specialized:Boolean=false,cap:Int=Upgrades.runCap(id))=
        UpgradeLane(id,sector,cap,first,step,parent,parentRank,specialized)
    val lanes=listOf(
        lane(Upgrades.DAMAGE,UpgradeSector.POWER,1,3),
        lane(Upgrades.RESERVE_POWER,UpgradeSector.POWER,15,2,Upgrades.DAMAGE,6,cap=16),
        lane(Upgrades.MAX_HEALTH,UpgradeSector.HULL,1,3),
        lane(Upgrades.HEAL_POWER,UpgradeSector.HULL,3,5,Upgrades.MAX_HEALTH),
        lane(Upgrades.REGEN,UpgradeSector.HULL,5,8,Upgrades.MAX_HEALTH,2),
        lane(Upgrades.RESERVE_HULL,UpgradeSector.HULL,15,4,Upgrades.MAX_HEALTH,4,cap=9),
        lane(Upgrades.FIELD_MEDIC,UpgradeSector.HULL,12,1,Upgrades.HEAL_POWER,2),
        lane(Upgrades.GUARDIAN_KIT,UpgradeSector.HULL,24,1,Upgrades.MAX_HEALTH,5),
        lane(Upgrades.SPEED,UpgradeSector.MOBILITY,1,3),
        lane(Upgrades.RESERVE_DRIVE,UpgradeSector.MOBILITY,15,2,Upgrades.SPEED,5,cap=16),
        lane(Upgrades.AFTERBURNER,UpgradeSector.MOBILITY,20,1,Upgrades.SPEED,4),
        lane(Upgrades.MAGNET,UpgradeSector.ECONOMY,1,4),
        lane(Upgrades.WEALTH,UpgradeSector.ECONOMY,1,4),
        lane(Upgrades.SCAVENGER,UpgradeSector.ECONOMY,13,1,Upgrades.MAGNET,3),
        lane(Upgrades.FIRE_RATE,UpgradeSector.WEAPON,5,3,Upgrades.DAMAGE,1,true),
        lane(Upgrades.RANGE,UpgradeSector.WEAPON,5,3,Upgrades.DAMAGE,1,true),
        lane(Upgrades.PIERCE,UpgradeSector.WEAPON,7,9,Upgrades.RANGE,1,true),
        lane(Upgrades.CALIBER,UpgradeSector.WEAPON,7,4,Upgrades.DAMAGE,2,true),
        lane(Upgrades.PHASE_CAPACITOR,UpgradeSector.WEAPON,25,1,Upgrades.FIRE_RATE,4,true),
        lane(Upgrades.DRONE_POWER,UpgradeSector.DRONES,15,5,Upgrades.DAMAGE,2,true),
        lane(Upgrades.DRONE_RELAY,UpgradeSector.DRONES,29,1,Upgrades.DRONE_POWER,3,true),
        lane(Upgrades.RAM_DAMAGE,UpgradeSector.RAM,5,3,Upgrades.DAMAGE,1,true),
        lane(Upgrades.SHIELD_RECHARGE,UpgradeSector.RAM,7,7,Upgrades.MAX_HEALTH,1,true),
        lane(Upgrades.TITAN_PLATING,UpgradeSector.RAM,25,1,Upgrades.RAM_DAMAGE,5,true)
    )
    val nodes=buildList{
        for(l in lanes)for(rank in 1..l.ranks)add(UpgradeNode(size,l,rank))
    }
    private val byRank=nodes.associateBy{it.upgrade to it.rank}
    init{require(nodes.size<=256 && lanes.map{it.upgrade}.distinct().size==lanes.size)}
    fun node(upgrade:Int,rank:Int)=byRank[upgrade to rank]
    fun visibleLane(l:UpgradeLane,frame:TankEvolution,lastEvolution:Int):Boolean{
        if(!l.specialized)return true
        // No weapon-specific future branches until the stage-five choice has been made.
        if(frame==TankEvolution.BASIC&&lastEvolution<5)return false
        if(!Upgrades.offered(l.upgrade,frame))return false
        return when(l.sector){
            UpgradeSector.DRONES->frame.drones
            UpgradeSector.RAM->frame.melee
            UpgradeSector.WEAPON->!frame.melee
            else->true
        }
    }
    fun visibleNodes(frame:TankEvolution,lastEvolution:Int)=nodes.filter{visibleLane(it.lane,frame,lastEvolution)}
    fun sector(n:UpgradeNode,frame:TankEvolution)=if(frame.drones&&n.lane.sector==UpgradeSector.WEAPON)UpgradeSector.DRONES else n.lane.sector
    fun minStage(n:UpgradeNode,frame:TankEvolution):Int{
        val l=n.lane
        // Gun specializations have different pacing without removing a valid stat path.
        val shift=if(!l.specialized||frame==TankEvolution.BASIC||frame.drones||frame.melee)0 else when{
            BuildFilter.SNIPER.matches(frame)&&l.upgrade in listOf(Upgrades.RANGE,Upgrades.PIERCE)->-2
            BuildFilter.RAPID.matches(frame)&&l.upgrade==Upgrades.FIRE_RATE->-2
            BuildFilter.CANNON.matches(frame)&&l.upgrade==Upgrades.CALIBER->-2
            BuildFilter.TWIN.matches(frame)&&l.upgrade==Upgrades.CALIBER->-1
            else->0
        }
        // Every rank remains purchasable eventually; late reserves are staggered through stage 49.
        return (l.firstStage+(n.rank-1)*l.stageStep+shift).coerceIn(if(l.specialized)5 else 1,49)
    }
    fun prerequisite(n:UpgradeNode):UpgradeNode?=if(n.rank>1)node(n.upgrade,n.rank-1)
        else if(n.lane.parent>=0)node(n.lane.parent,n.lane.parentRank) else null
    fun discovery(n:UpgradeNode,levels:List<Int>):UpgradeDiscovery{
        if(levels.getOrElse(n.upgrade){0}>=n.rank)return UpgradeDiscovery.OWNED
        val prev=prerequisite(n)
        return if(prev==null||levels.getOrElse(prev.upgrade){0}>=prev.rank)UpgradeDiscovery.REVEALED else UpgradeDiscovery.HIDDEN
    }
    fun eligible(n:UpgradeNode,frame:TankEvolution,lastEvolution:Int,stage:Int,levels:List<Int>)=
        visibleLane(n.lane,frame,lastEvolution)&&stage>=minStage(n,frame)&&
            levels.getOrElse(n.upgrade){0}==n.rank-1&&discovery(n,levels)==UpgradeDiscovery.REVEALED
    fun price(n:UpgradeNode,stage:Int,character:CharacterDef,perkMask:Int):Int{
        // Guaranteed selection has a modest premium over random single-rank cards.
        var price=Upgrades.price(n.upgrade,n.rank-1,stage)*1.15
        if(character==CharacterDef.BALANCED)price*=.9
        if(perkMask and (1 shl Perks.HAGGLER)!=0)price*=.9
        return price.roundToInt().coerceAtLeast(1)
    }
    fun title(n:UpgradeNode,frame:TankEvolution)=when(n.upgrade){
        Upgrades.DAMAGE->"Power"
        Upgrades.RESERVE_POWER->"Reserve power"
        Upgrades.FIRE_RATE->if(frame.drones)"Replacement rate" else "Fire rate"
        Upgrades.RANGE->if(frame.drones)"Command range" else "Range"
        Upgrades.RAM_DAMAGE->"Hull impact"
        else->Upgrades.of(n.upgrade).name
    }
    fun effect(n:UpgradeNode,frame:TankEvolution)=when(n.upgrade){
        Upgrades.DAMAGE->"+15% base damage / drone impact / ram scaling"
        Upgrades.RESERVE_POWER->"+3% base damage / impact"
        Upgrades.RANGE->if(frame.drones)"+6% base command range (up to command limit)" else "+12% base projectile range"
        Upgrades.FIRE_RATE->if(frame.drones)"+10% base drone replacement rate (minimum interval applies)" else "+10% base firing rate"
        else->Upgrades.of(n.upgrade).blurb
    }
}
