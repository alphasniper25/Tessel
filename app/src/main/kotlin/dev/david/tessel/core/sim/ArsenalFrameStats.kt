package dev.david.tessel.core.sim

import dev.david.tessel.core.content.*
import java.util.Locale

/** Read-only technical sheet derived from the same frame and baseline player
 * formulas used in combat. No ledger, World, networking or saves involved. */
data class ArsenalStat(val label:String,val value:String)
data class ArsenalFrameStats(val primary:List<ArsenalStat>,val secondary:List<ArsenalStat>,val limitation:String){
    companion object {
        private fun f(v:Float)=String.format(Locale.US,"%.2f",v).trimEnd('0').trimEnd('.')
        fun forFrame(frame:TankEvolution):ArsenalFrameStats{
            val p=PlayerState().apply{
                reset(1)
                evolution[0]=frame
                maxHp[0]=Balance.PLAYER_HP+frame.hullBonus
                hp[0]=maxHp[0]
            }
            val main=mutableListOf(ArsenalStat("Base HP",p.maxHp[0].toString()),ArsenalStat("Movement","${f(p.moveSpeed(0))} u/s"))
            val extra=mutableListOf(
                ArsenalStat("Unlock",if(frame.tier==0)"Starting frame" else if(frame.secret)"Clear stage 40 as Basic" else "Clear stage ${CodexCatalog.unlockStage(frame)}"),
                ArsenalStat("Hull bonus","+${frame.hullBonus} HP")
            )
            if(frame.parent>=0)extra.add(1,ArsenalStat("Evolves from",TankEvolution.fromId(frame.parent).label))
            val drone=frame.droneSpec
            val limitation=when{
                drone!=null->{
                    main+=ArsenalStat("Drone impact",f(p.damage(0)*drone.impact))
                    val interval=(drone.respawn*TICK_HZ).toInt().coerceAtLeast(30)/TICK_HZ.toFloat()
                    main+=ArsenalStat("Replacement","${f(interval)} s")
                    extra+=listOf(
                        ArsenalStat("Drone capacity","${drone.cap} units"),
                        ArsenalStat("Drone health","${drone.hp} HP each"),
                        ArsenalStat("Command range","${f(drone.reach)} u"),
                        ArsenalStat("Drone speed","${f(drone.speed)} u/s"),
                        ArsenalStat("Defense","Absorb shots; each block costs 1 drone HP"),
                        ArsenalStat("Control","Aim to command the wing; release to guard")
                    )
                    "Destroyable units; serial replacement"
                }
                frame.melee->{
                    main+=ArsenalStat("Ram impact",f(p.ramDamage(0)))
                    main+=ArsenalStat("Shield recharge","${f(p.shieldTicks(0)/TICK_HZ.toFloat())} s")
                    extra+=listOf(ArsenalStat("Regeneration","${p.regenSeconds(0)} s per HP"),
                        ArsenalStat("Impact radius","${f(frame.impactRadius)} u"),
                        ArsenalStat("Projectile armor",frame.projectileArmor.toString()))
                    "Must make contact"
                }
                else->{
                    main+=ArsenalStat("Shot damage",f(p.damage(0)))
                    main+=ArsenalStat("Fire rate","${f(p.fireRate(0))}/s")
                    extra+=listOf(ArsenalStat("Barrels",p.volley(0).toString()),
                        ArsenalStat("Range","${f(p.range(0))} u"),
                        ArsenalStat("Extra pierce",p.pierce(0).toString()))
                    when{BuildFilter.CANNON.matches(frame)->"Slow reload";BuildFilter.SNIPER.matches(frame)->"Precise aim";BuildFilter.RAPID.matches(frame)->"Lower per-shot power";else->"Positioning and aim"}
                }
            }
            return ArsenalFrameStats(main,extra,limitation)
        }
    }
}
