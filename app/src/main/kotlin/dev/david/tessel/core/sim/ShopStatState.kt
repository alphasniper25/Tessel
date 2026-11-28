package dev.david.tessel.core.sim

import dev.david.tessel.core.content.*

data class StatChange(val label:String,val before:Float,val after:Float,val unit:String="")
/** Immutable shop-only input. A forecast uses the same PlayerState equations as combat.
 * All co-op fields are transferred in the shop snapshot, not every fighting frame. */
data class ShopStatState(
    val levels:List<Int>,val hp:Int,val maxHp:Int,val character:CharacterDef,val frame:TankEvolution,
    val perks:Int,val skills:Long,val sinceHit:Int,val surge:Boolean,val difficulty:Difficulty,val stage:Int,val missionMask:Int
){
    companion object{
        fun from(p:PlayerState,seat:Int,difficulty:Difficulty,stage:Int)=ShopStatState(
            p.levels[seat].toList(),p.hp[seat],p.maxHp[seat],p.character[seat],p.evolution[seat],
            p.perks[seat],p.skillMask(seat),p.sinceHit[seat],p.surge[seat]>0,difficulty,stage,p.missionMask[seat])
    }
    private fun player():PlayerState=PlayerState().apply{
        reset(1);this.character[0]=this@ShopStatState.character;evolution[0]=frame
        this.hp[0]=this@ShopStatState.hp;this.maxHp[0]=this@ShopStatState.maxHp
        this.perks[0]=this@ShopStatState.perks;setSkills(0,skills)
        this.sinceHit[0]=this@ShopStatState.sinceHit;this.surge[0]=if(this@ShopStatState.surge)1 else 0
        for(i in 0 until Upgrades.count)this.levels[0][i]=this@ShopStatState.levels.getOrElse(i){0}
    }
    fun forecast(id:Int,ranks:Int):List<StatChange>{
        if(id !in 0 until Upgrades.count||ranks<=0)return emptyList()
        val p=player()
        fun values():Map<String,Pair<Float,String>> = linkedMapOf<String,Pair<Float,String>>().apply{
            val spec=frame.droneSpec
            if(spec!=null){
                put("Drone impact",p.damage(0)*spec.impact to "HP")
                val rate=(p.fireRate(0)/(Balance.FIRE_RATE*frame.rate)).coerceIn(.5f,2.4f)
                put("Drone replacement",(spec.respawn*TICK_HZ/rate).toInt().coerceAtLeast(30)/TICK_HZ.toFloat() to "s")
                put("Drone command range",(spec.reach*(1f+Upgrades.delta(p.levels[0],UpgradeStat.RANGE)*.5f+p.skill(0,SkillBonus.RANGE)*.005f)).coerceAtMost(540f) to "u")
            }else if(!frame.melee){
                put("Damage per bullet",p.damage(0) to "HP")
                put("Fire rate",p.fireRate(0) to "/s")
                put("Bullet range",p.range(0) to "u")
                put("Bullet radius",p.bulletRadius(0) to "u")
                put("Bullet speed",p.bulletSpeed(0) to "u/s")
                put("Pierce",p.pierce(0).toFloat() to "")
                put("Projectiles per shot",p.volley(0).toFloat() to "")
            }
            put("Ram impact",p.ramDamage(0) to "HP")
            put("Movement",p.moveSpeed(0) to "u/s")
            put("Current HP",p.hp[0].toFloat() to "HP")
            put("Maximum HP",p.maxHp[0].toFloat() to "HP")
            put("Pickup radius",p.magnet(0) to "u")
            put("Coin value before wave reduction",CampaignEconomy.pickupBase(stage)*(1f+p.wealth(0))*difficulty.coinValue to "coins")
            put("Shop heal",(Upgrades.HEAL_AMOUNT+Upgrades.delta(p.levels[0],UpgradeStat.HEAL).toInt()+p.skill(0,SkillBonus.HEAL_POWER)).toFloat() to "HP")
            put("Regeneration interval",p.regenSeconds(0).toFloat() to "s")
            if(frame.melee)put("Shield recharge",p.shieldTicks(0)/TICK_HZ.toFloat() to "s")
        }
        val before=values()
        val count=minOf(ranks,(Upgrades.runCap(id)-p.levels[0][id]).coerceAtLeast(0))
        p.levels[0][id]+=count
        val added=(count*Upgrades.amount(id,UpgradeStat.MAX_HP)).toInt()
        p.maxHp[0]+=added;p.hp[0]=minOf(p.hp[0]+added,p.maxHp[0])
        val after=values()
        return before.mapNotNull{(label,v)->val a=after.getValue(label);if(kotlin.math.abs(a.first-v.first)>.0001f)StatChange(label,v.first,a.first,a.second)else null}
    }
}
