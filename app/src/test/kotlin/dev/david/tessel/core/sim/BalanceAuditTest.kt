package dev.david.tessel.core.sim

import dev.david.tessel.core.content.*
import org.junit.Test
import org.junit.Assume.assumeTrue
import org.junit.Assert.*
import java.io.File
import kotlin.math.*

/** Same deterministic policies run on alpha.14 and alpha.15.
 * Frame fixtures are controlled short encounters, not organic campaign runs.
 * Campaigns begin at stage one with ordinary coins, health and shop rules. */
class BalanceAuditTest {
    private fun directory():File{
        assumeTrue(System.getenv("TESSEL_BALANCE_AUDIT")=="1")
        return File(System.getenv("TESSEL_BALANCE_OUT") ?: "build/reports/balance").apply{mkdirs()}
    }
    private fun stageFor(f:TankEvolution)=if(f.secret)41 else when(f.tier){0->1;1->6;2->16;3->26;else->36}
    private fun family(f:TankEvolution):String=when{
        f.secret->"Secret"
        f.drones->"Drone"
        f.melee->"Rammer"
        BuildFilter.CANNON.matches(f)->"Cannon"
        BuildFilter.SNIPER.matches(f)->"Sniper"
        BuildFilter.RAPID.matches(f)->"Rapid"
        BuildFilter.TWIN.matches(f)->"Twin"
        else->"Basic"
    }
    private fun drive(w:World,p:Int,out:PlayerInput){
        out.clear()
        val pl=w.players;val px=pl.x[p];val py=pl.y[p];val f=pl.evolution[p]
        if(!pl.alive(p)){
            val q=w.nearestPlayer(px,py)
            if(q>=0){val dx=pl.x[q]-px;val dy=pl.y[q]-py;val d=hypot(dx,dy).coerceAtLeast(1f);out.moveX=dx/d;out.moveY=dy/d}
            return
        }
        val e=w.enemies;var near=-1;var best=Float.MAX_VALUE
        for(i in 0 until e.count){
            if(e.hp[i]<=0f||w.core.docked(i))continue
            val dx=e.x[i]-px;val dy=e.y[i]-py;val ds=dx*dx+dy*dy
            if(ds<best){best=ds;near=i}
        }
        var mx=cos(w.tick*.009f+p)*.55f;var my=sin(w.tick*.009f+p)*.55f
        if(near>=0){
            val dx=e.x[near]-px;val dy=e.y[near]-py;val d=sqrt(best).coerceAtLeast(.01f)
            val ux=dx/d;val uy=dy/d
            val target=if(!f.melee&&w.boss>=0&&w.boss<e.count&&e.hp[w.boss]>0f&&!w.core.docked(w.boss)&&d>170f)w.boss else near
            val ax=e.x[target]-px;val ay=e.y[target]-py;val td=hypot(ax,ay).coerceAtLeast(.01f)
            out.aimX=ax/td;out.aimY=ay/td
            val reach=if(f.drones)f.droneSpec!!.reach else pl.range(p)
            out.fire=!f.melee && td<reach*.97f && (!f.drones || w.tick%300<240)
            if(f.melee){val dir=if(pl.shieldCooldown[p]==0)1f else -.85f;mx=ux*dir;my=uy*dir}
            else {
                val preferred=if(f.drones)190f else if(f.reach>=1.5f)300f else 185f
                val radial=if(d<preferred) -.85f else if(td>reach*.8f) .9f else 0f
                mx=ux*radial+uy*.65f;my=uy*radial-ux*.65f
            }
        }
        val h=w.hostile
        for(i in 0 until h.count){
            val dx=h.x[i]-px;val dy=h.y[i]-py;val ds=dx*dx+dy*dy
            if(ds in 1f..10000f&&dx*h.vx[i]+dy*h.vy[i]<0f){
                val d=sqrt(ds);val strength=(100f-d)/100f
                mx-=dx/d*strength*1.35f;my-=dy/d*strength*1.35f
            }
        }
        // Opportunistic nearby pickups, without teleporting or injecting income.
        if(best>180f*180f&&!f.melee){
            val coins=w.coins;var ci=-1;var cd=145f*145f
            for(i in 0 until coins.count){val dx=coins.x[i]-px;val dy=coins.y[i]-py;val ds=dx*dx+dy*dy;if(ds<cd){ci=i;cd=ds}}
            if(ci>=0){val d=sqrt(cd).coerceAtLeast(1f);mx+=(coins.x[ci]-px)/d*.7f;my+=(coins.y[ci]-py)/d*.7f}
        }
        if(px<105f)mx+=1.3f;if(px>ARENA_W-105f)mx-=1.3f
        if(py<105f)my+=1.3f;if(py>ARENA_H-105f)my-=1.3f
        if(w.playerCount>1&&!pl.alive(1-p)&&best>105f*105f){
            val dx=pl.x[1-p]-px;val dy=pl.y[1-p]-py;val d=hypot(dx,dy).coerceAtLeast(1f);mx=dx/d;my=dy/d
        }
        if(w.core.strike==CoreFight.STRIKE_WARN){
            val size=CoreRules.CELL;val col=(px/size).toInt();val row=(py/size).toInt()
            if(((col+row) and 1)==w.core.strikeParity){
                val left=px-col*size;val right=(col+1)*size-px;val top=py-row*size;val bottom=(row+1)*size-py
                val m=min(min(left,right),min(top,bottom))
                mx=when(m){left->-1f;right->1f;else->0f};my=when(m){top->-1f;bottom->1f;else->0f}
            }
        }
        val len=hypot(mx,my).coerceAtLeast(1f);out.moveX=mx/len;out.moveY=my/len
    }
    private fun routeContains(candidate:TankEvolution,goal:TankEvolution):Boolean{
        var f=goal
        while(true){if(f==candidate)return true;if(f.parent<0)return false;f=TankEvolution.fromId(f.parent)}
    }
    private fun shop(w:World,p:Int,goal:TankEvolution,policy:Int):Int{
        val s=w.shop;val pl=w.players
        val offers=(0 until TankEvolution.REWARD_SLOTS).filter{s.rewardOffer(p,it)>=0}
        if(offers.isNotEmpty()){
            val best=offers.firstOrNull{
                val f=TankEvolution.fromReward(s.rewardOffer(p,it));f!=null&&routeContains(f,goal)
            } ?: offers.firstOrNull{goal.secret&&w.stage<TankEvolution.SECRET_STAGE&&s.rewardOffer(p,it)==TankEvolution.STAY_BASIC}
                ?:offers.maxByOrNull{when(s.rewardOffer(p,it)){Perks.SECOND_WIND,Perks.SIPHON,Perks.THICK_SKIN->10;Perks.HAGGLER->7;else->3}}!!
            check(s.pickPerk(w,p,best))
        }
        repeat(2){if(pl.hp[p]<=pl.maxHp[p]-3&&s.canHeal(w,p))s.heal(w,p)}
        val budget=pl.coins[p]
        // Three profiles spend roughly 20%, 35% or 50% of shop-entry cash on the graph.
        val fraction=when(policy%3){0->.20;1->.35;else->.50}
        var remaining=(budget*fraction).toInt()
        repeat(4){
            val n=SkillGraph.nodes.filter{it.price<=remaining&&s.canBuySkill(w,p,it.id)}.maxByOrNull{
                val preference=when(it.sector){SkillSector.FIREPOWER->if(pl.evolution[p].melee)4 else 6;SkillSector.HULL->5;SkillSector.REFRESH->6;SkillSector.TRADE->5;SkillSector.FORTUNE->4;SkillSector.MOBILITY->3}
                (preference+if(it.root)2 else 0).toDouble()/it.price
            }?:return@repeat
            if(s.buySkill(w,p,n.id))remaining-=n.price
        }
        var bought=0
        repeat(5){
            val k=(0 until SHOP_SLOTS).filter{s.canBuy(w,p,it)}.maxByOrNull{
                val id=s.item(p,it);val f=pl.evolution[p]
                val weight=when(Upgrades.of(id).family){CardFamily.GUN->if(f.melee)2 else 8;CardFamily.RAM->if(f.melee)10 else 1;CardFamily.HEALTH->8;CardFamily.MOBILITY->5;CardFamily.ECONOMY->4}
                weight*s.levelsBought(w,p,it).toDouble()/s.price(w,p,it).coerceAtLeast(1)
            }
            if(k!=null){if(s.buy(w,p,k))bought++}
            else if(s.canReroll(w,p)&&s.rerollPrice(w,p)==0)s.reroll(w,p)
        }
        return bought
    }

    @Test fun allTankFramesCombatAndScaling(){
        val dir=directory()
        File(dir,"frames.csv").bufferedWriter().use{out->
            out.appendLine("tank,family,tier,difficulty,fixture,stage,seconds,alive,damage,kills,hits,fired,drone_peak,base_damage,base_rate,base_speed,base_ram,base_range,upgraded_damage,upgraded_rate,upgraded_speed,upgraded_ram,enemy_hp_scale,boss_hp_scale")
            for(f in TankEvolution.entries)for(d in Difficulty.entries)for(bundle in listOf(0,1)){
                val seed=7519L+f.ordinal*101
                val w=World(seed);w.reset(seed=seed,difficulty=d);w.skipTo(stageFor(f));w.spawning=false
                val p=w.players;p.evolution[0]=f;p.maxHp[0]+=f.hullBonus;p.hp[0]=p.maxHp[0]
                val base=listOf(p.damage(0),p.fireRate(0),p.moveSpeed(0),p.ramDamage(0),p.range(0))
                val ranks=min(Upgrades.runCap(Upgrades.DAMAGE),2+f.tier*2)
                if(bundle==1){
                    w.devUpgrades(mapOf(Upgrades.DAMAGE to ranks,Upgrades.MAX_HEALTH to f.tier+1,
                        Upgrades.SPEED to 2,Upgrades.FIRE_RATE to if(f.melee)0 else ranks,
                        Upgrades.RAM_DAMAGE to if(f.melee)ranks else 0,Upgrades.DRONE_POWER to if(f.drones)3 else 0))
                }
                val powered=listOf(p.damage(0),p.fireRate(0),p.moveSpeed(0),p.ramDamage(0))
                val inp=Array(MAX_PLAYERS){PlayerInput()};var peak=0;var ticks=0
                repeat(18*TICK_HZ){step->
                    if(w.over)return@repeat
                    if(step%(3*TICK_HZ)==0){
                        for(i in 0..3){
                            val angle=(step/180*.9f+i*1.57f)
                            val reach=150f+i*25f
                            w.spawnEnemy(if(i%2==0)Bestiary.SLAB else Bestiary.PIP,
                                (p.x[0]+cos(angle)*reach).coerceIn(25f,ARENA_W-25f),
                                (p.y[0]+sin(angle)*reach).coerceIn(25f,ARENA_H-25f))
                        }
                    }
                    drive(w,0,inp[0]);w.step(inp);w.events.clear();ticks++
                    peak=max(peak,w.droneWing.count(0))
                }
                assertTrue(peak<=8);assertTrue(p.dealt[0].isFinite());assertTrue(p.coins[0]>=0)
                out.appendLine((listOf(f.label,family(f),f.tier,d.name,bundle,w.stage,ticks/TICK_HZ.toFloat(),!w.over,p.dealt[0],p.kills[0],p.taken[0],p.fired[0],peak)+base+powered+listOf(w.enemyHpScale(),w.bossHpScale())).joinToString(","))
                out.flush()
            }
        }
    }

    @Test fun matchedCampaignsMeasureActualShopAndSkillPacing(){
        val dir=directory()
        val goals=listOf(TankEvolution.FOCUS,TankEvolution.GATLING,TankEvolution.ANNIHILATOR,TankEvolution.RANGER,TankEvolution.MOTHERSHIP,TankEvolution.SPIKE)
        File(dir,"campaigns.csv").bufferedWriter().use{out->
            out.appendLine("difficulty,team,goal,seed,stage,victory,ended,seconds,earned,wallet,peak_wallet,card_purchases,upgrade_ranks,skill_nodes,skill_spend,first_skill_stage,damage,kills,hits,bosses,final_frame,peak_enemies")
            File(dir,"stages.csv").bufferedWriter().use{stages->
                stages.appendLine("difficulty,team,goal,seed,stage,earned_delta,wallet_before,wallet_after,card_purchases,upgrade_ranks,skill_nodes,skill_spend,hp,max_hp,frame")
                val cases=Difficulty.entries.flatMap{d->goals.mapIndexed{i,g->Triple(d,1,g)}}+
                    goals.map{Triple(Difficulty.NORMAL,2,it)}+listOf(TankEvolution.PRISM,TankEvolution.DREADNOUGHT,TankEvolution.TEMPEST).map{Triple(Difficulty.NORMAL,1,it)}
                for((idx,case) in cases.withIndex()){
                    val (d,team,goal)=case;val seed=8803L+idx*177L
                    val w=World(seed);w.reset(team,seed,d)
                    for(p in 0 until team)w.players.chooseCharacter(p,CharacterDef.entries[(idx+p)%CharacterDef.entries.size],d.bonusHp)
                    val inp=Array(MAX_PLAYERS){PlayerInput()}
                    var ticks=0;var purchases=0;var previousEarned=0;var peakWallet=0;var peak=0;var firstSkill=-1;var skillSpend=0
                    while(!w.over&&ticks<1800*TICK_HZ){
                        if(w.phase==Phase.SHOP){
                            val p=w.players;val before=p.coins[0];val oldMask=p.skillMask(0)
                            peakWallet=max(peakWallet,before)
                            val bought=shop(w,0,goal,idx);purchases+=bought
                            if(team==2)shop(w,1,goals[(idx+1)%goals.size],idx+1)
                            val newMask=p.skillMask(0);val spend=SkillGraph.nodes.filter{SkillGraph.owns(newMask,it.id)&&!SkillGraph.owns(oldMask,it.id)}.sumOf{it.price}
                            skillSpend+=spend;if(firstSkill<0&&newMask!=0L)firstSkill=w.stage
                            stages.appendLine(listOf(d.name,team,goal.label,seed,w.stage,p.earned[0]-previousEarned,before,p.coins[0],bought,p.bought(0),SkillGraph.count(newMask),spend,p.hp[0],p.maxHp[0],p.evolution[0].label).joinToString(","))
                            stages.flush();previousEarned=p.earned[0];w.nextStage()
                        }
                        for(p in 0 until team)drive(w,p,inp[p])
                        w.step(inp);w.events.clear();ticks++;peak=max(peak,w.enemies.count)
                    }
                    val p=w.players
                    assertTrue(p.coins[0]>=0);assertTrue(p.dealt[0].isFinite())
                    out.appendLine(listOf(d.name,team,goal.label,seed,w.stage,w.victory,w.over,ticks/TICK_HZ,p.earned[0],p.coins[0],peakWallet,purchases,p.bought(0),SkillGraph.count(p.skillMask(0)),skillSpend,firstSkill,p.dealt[0],p.kills[0],p.taken[0],w.bossesDown,p.evolution[0].label,peak).joinToString(","))
                    out.flush()
                }
            }
        }
    }

    @Test fun fullStageCurve(){
        val dir=directory()
        File(dir,"economy.csv").bufferedWriter().use{out->
            out.appendLine("difficulty,stage,price_scale,base_reward,mid_reward,cap_reward,damage_card,heal,first_reroll,root_skill,all_skills_cost,enemy_hp,boss_hp")
            for(d in Difficulty.entries)for(stage in 1..50){
                val w=World(11);w.reset(seed=11,difficulty=d);w.skipTo(stage)
                // Apply the world's actual clear reward for empty combat; the baseline
                // StageRewards signature has no difficulty argument, so use a tiny
                // real empty stage for cross-version parity where possible.
                val base=StageRewards.coins(stage,0,0)
                val mid=StageRewards.coins(stage,(20*Upgrades.inflation(stage)).roundToInt(),25)
                val cap=StageRewards.coins(stage,100000000,100000)
                out.appendLine(listOf(d.name,stage,Upgrades.inflation(stage),base,mid,cap,Upgrades.price(Upgrades.DAMAGE,0,stage),Upgrades.healPrice(stage),Upgrades.rerollPrice(stage,0),SkillGraph.nodes[0].price,SkillGraph.nodes.sumOf{it.price},w.enemyHpScale(),w.bossHpScale()).joinToString(","))
            }
        }
    }
}
