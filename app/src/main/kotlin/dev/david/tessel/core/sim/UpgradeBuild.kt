package dev.david.tessel.core.sim

import dev.david.tessel.core.content.*
import java.util.Locale
import kotlin.math.roundToInt

data class PurchasedUpgrade(val node:UpgradeNode,val sector:UpgradeSector,val title:String,val rank:Int,val effect:String)

/** Immutable per-player projection; UI cannot mutate the authoritative World. */
data class UpgradeBuild(val frame:TankEvolution=TankEvolution.BASIC,
    val character:CharacterDef=CharacterDef.BALANCED,val look:TankLook=TankLook(),
    val levels:List<Int> = List(Upgrades.count){0},val lastEvolution:Int=0,
    val perkMask:Int=0,val hp:Int=Balance.PLAYER_HP,val maxHp:Int=Balance.PLAYER_HP){
    companion object {
        fun from(pl:PlayerState,p:Int)=UpgradeBuild(pl.evolution[p],pl.character[p],pl.appearance[p],pl.levels[p].toList(),
            pl.lastEvolutionStage[p],pl.perks[p],pl.hp[p],pl.maxHp[p])
        private fun f(v:Float)=String.format(Locale.US,"%.2f",v).trimEnd('0').trimEnd('.')
    }
    fun perks()=(0 until SkillTree.ORIGINAL_PERKS).filter{perkMask and (1 shl it)!=0}
    /** A rank summary is computed from owned levels, never from revealed nodes. */
    fun purchased():List<PurchasedUpgrade> = UpgradeTree.lanes.mapNotNull{lane->
        val rank=levels.getOrElse(lane.upgrade){0}
        if(rank<=0)return@mapNotNull null
        val node=UpgradeTree.node(lane.upgrade,rank.coerceAtMost(lane.ranks))?:return@mapNotNull null
        PurchasedUpgrade(node,UpgradeTree.sector(node,frame),UpgradeTree.title(node,frame),rank,
            UpgradeTree.effect(node,frame)+" per rank")
    }
    fun stats():List<ArsenalStat>{
        val p=PlayerState().apply{
            reset(1)
            evolution[0]=frame
            character[0]=this@UpgradeBuild.character
            this@UpgradeBuild.levels.forEachIndexed{i,n->levels[0][i]=n}
            perks[0]=perkMask
            this.hp[0]=this@UpgradeBuild.hp
            this.maxHp[0]=this@UpgradeBuild.maxHp
        }
        val stats=mutableListOf(ArsenalStat("Hull","$hp / $maxHp HP"),ArsenalStat("Movement","${f(p.moveSpeed(0))} u/s"))
        val d=frame.droneSpec
        when{
            d!=null->{
                val rate=(p.fireRate(0)/(Balance.FIRE_RATE*frame.rate)).coerceIn(.5f,2.4f)
                val refill=(d.respawn*TICK_HZ/rate).toInt().coerceAtLeast(30)/TICK_HZ.toFloat()
                val reach=(d.reach*(1f+Upgrades.delta(p.levels[0],UpgradeStat.RANGE)*.5f)).coerceAtMost(540f)
                stats+=listOf(ArsenalStat("Drone impact",f(p.damage(0)*d.impact)),ArsenalStat("Replacement","${f(refill)} s"),
                    ArsenalStat("Drone capacity","${d.cap}"),ArsenalStat("Command range","${f(reach)} u"))
            }
            frame.melee->{
                stats+=listOf(ArsenalStat("Hull impact",f(p.ramDamage(0))),ArsenalStat("Shield recharge","${f(p.shieldTicks(0)/TICK_HZ.toFloat())} s"))
            }
            else->stats+=listOf(ArsenalStat("Shot damage",f(p.damage(0))),ArsenalStat("Volleys / second",f(p.fireRate(0))),
                ArsenalStat("Range","${f(p.range(0))} u"),ArsenalStat("Pierce",p.pierce(0).toString()))
        }
        stats+=ArsenalStat("Regeneration",if(p.regenSeconds(0)>0)"${p.regenSeconds(0)} s / HP" else "None")
        stats+=ArsenalStat("Coin value","+${(p.wealth(0)*100).roundToInt()}%")
        stats+=ArsenalStat("Pickup radius","${f(p.magnet(0))} u")
        stats+=ArsenalStat("Heal amount","${Upgrades.HEAL_AMOUNT+Upgrades.delta(p.levels[0],UpgradeStat.HEAL).toInt()} HP")
        return stats
    }
    fun nextFrames():List<TankEvolution>{
        if(frame==TankEvolution.BASIC&&lastEvolution<5)return emptyList()
        return TankEvolution.options(frame,if(frame==TankEvolution.BASIC&&lastEvolution>=25)40 else 0).toList()
    }
    fun nextEvolutionStage():Int?=when{
        frame==TankEvolution.BASIC->listOf(5,15,25,40).firstOrNull{it>lastEvolution}
        frame.tier==1->15
        frame.tier==2->25
        frame.tier==3&&TankEvolution.options(frame).isNotEmpty()->35
        else->null
    }
}
