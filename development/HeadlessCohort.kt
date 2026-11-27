package dev.david.tessel.core.sim

import dev.david.tessel.core.content.*
import dev.david.tessel.core.stats.*
import dev.david.tessel.core.rng.Rng
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.io.File
import kotlin.math.*

/** Full production World.step, no Android, no boosted health or forced stage completion.
 * Synthetic agents, not people. A month is 30 modeled days with one session/day by default.
 * Each focal profile preserves ledger totals, missions and class preference across days.
 * Team runs simulate a second seat locally, not NFC/Wi-Fi. */
private class CohortAgent(val skill:Int, val style:Int, private val rng:Rng) {
    private var heading = rng.range(0f,6.28f)
    private var ax=1f; private var ay=0f; private var mx=0f; private var my=0f; private var fire=false
    private var wait=0
    fun drive(w:World,p:Int,out:PlayerInput) {
        out.clear(); val pl=w.players; val x=pl.x[p]; val y=pl.y[p]
        if(!pl.alive(p)) { val q=w.nearestPlayer(x,y); if(q>=0){val dx=pl.x[q]-x;val dy=pl.y[q]-y;val d=sqrt(dx*dx+dy*dy).coerceAtLeast(1f);out.moveX=dx/d;out.moveY=dy/d};return }
        if(--wait<=0) {
            wait=when(skill){0->18;1->8;else->4};heading+=rng.range(-.45f,.45f)
            mx=cos(heading)*.8f; my=sin(heading)*.8f; fire=false
            val e=w.enemies; var near=-1;var dist=Float.MAX_VALUE
            for(j in 0 until e.count){if(e.hp[j]<=0 || w.core.docked(j))continue;val dx=e.x[j]-x;val dy=e.y[j]-y;val d2=dx*dx+dy*dy;if(d2<dist){dist=d2;near=j}}
            if(near>=0){
                val d=sqrt(dist).coerceAtLeast(.1f);val ux=(e.x[near]-x)/d;val uy=(e.y[near]-y)/d
                val frame=pl.evolution[p]; val target=if(w.boss>=0 && e.hp[w.boss]>0 && !w.core.docked(w.boss) && d>145f && !frame.melee)w.boss else near
                val dx=e.x[target]-x;val dy=e.y[target]-y;val td=sqrt(dx*dx+dy*dy).coerceAtLeast(.1f)
                val err=rng.range(-1f,1f)*when(skill){0->.30f;1->.10f;else->.035f}
                ax=cos(atan2(dy,dx)+err);ay=sin(atan2(dy,dx)+err);fire=td<pl.range(p)*.96f && rng.nextFloat() > if(skill==0) .16f else .025f
                if(frame.melee){
                    if(pl.shieldCooldown[p]==0 || skill==0 && rng.nextFloat()<.40f){mx=ux;my=uy}else {mx=-ux*.85f;my=-uy*.85f}
                } else {
                    val preferred=when(style){0->190f;1->260f;2->340f;else->155f}
                    if(d<preferred){mx=-ux+uy*.35f;my=-uy-ux*.35f}else if(td>pl.range(p)*.8f){mx=dx/td;my=dy/td}else{mx=uy*.8f;my=-ux*.8f}
                }
                if(skill>0){
                    val h=w.hostile; var evadeX=0f;var evadeY=0f
                    for(j in 0 until h.count){val hx=h.x[j]-x;val hy=h.y[j]-y;val ds=hx*hx+hy*hy;if(ds in 1f..110f*110f && hx*h.vx[j]+hy*h.vy[j]<0f){val k=(110f-sqrt(ds))/110f;evadeX-=hx/sqrt(ds)*k;evadeY-=hy/sqrt(ds)*k}}
                    mx+=evadeX*(if(skill==2)2f else .9f);my+=evadeY*(if(skill==2)2f else .9f)
                }
                if(d>230f && !frame.melee && style==0){val c=w.coins; var nearest=110f*110f;var at=-1;for(j in 0 until c.count){val cx=c.x[j]-x;val cy=c.y[j]-y;val ds=cx*cx+cy*cy;if(ds<nearest){nearest=ds;at=j}};if(at>=0){mx+=(c.x[at]-x)/sqrt(nearest).coerceAtLeast(1f);my+=(c.y[at]-y)/sqrt(nearest).coerceAtLeast(1f)}}
            }
            // Avoid walls; learn to revive partners as a distinct strategy.
            if(x<100)mx+=1.3f;if(x>ARENA_W-100)mx-=1.3f;if(y<100)my+=1.3f;if(y>ARENA_H-100)my-=1.3f
            if(skill>0 && w.playerCount>1 && !pl.alive(1-p) && pl.present[1-p] && (near<0 || dist>100f*100f)){val dx=pl.x[1-p]-x;val dy=pl.y[1-p]-y;val d=sqrt(dx*dx+dy*dy).coerceAtLeast(1f);if(d>35f){mx=dx/d;my=dy/d}}
            if(skill>0 && w.core.strike==CoreFight.STRIKE_WARN){
                val c=CoreRules.CELL;val col=(x/c).toInt();val row=(y/c).toInt()
                if((col+row) and 1 == w.core.strikeParity){
                    val left=x-col*c;val right=(col+1)*c-x;val top=y-row*c;val bottom=(row+1)*c-y
                    var best=Float.MAX_VALUE;var ux=0f;var uy=0f
                    if(col>0&&left<best){best=left;ux=-1f;uy=0f};if((col+1)*c<ARENA_W&&right<best){best=right;ux=1f;uy=0f}
                    if(row>0&&top<best){best=top;ux=0f;uy=-1f};if((row+1)*c<ARENA_H&&bottom<best){ux=0f;uy=1f};mx=ux;my=uy
                } else {mx=0f;my=0f}
            }
            if(skill>0){val h=w.core.hazards;for(k in 0 until h.count){val dx=x-h.x[k];val dy=y-h.y[k];val d=sqrt(dx*dx+dy*dy).coerceAtLeast(1f);if(d<95f){mx+=dx/d*(95-d)/25;my+=dy/d*(95-d)/25}}}
            val len=sqrt(mx*mx+my*my).coerceAtLeast(1f);mx/=len;my/=len
        }
        out.moveX=mx;out.moveY=my;out.aimX=ax;out.aimY=ay;out.fire=fire
    }
}
private fun cohortShop(w:World,p:Int,style:Int,skill:Int,rng:Rng,secret:Boolean, motive:Int):Int {
    val s=w.shop;val pl=w.players; var bought=0
    val offers=(0 until TankEvolution.REWARD_SLOTS).filter{s.rewardOffer(p,it)>=0}
    if(offers.isNotEmpty()){
        val evolution=offers.filter {TankEvolution.fromReward(s.rewardOffer(p,it))!=null}
        val stay=offers.firstOrNull { s.rewardOffer(p,it)==TankEvolution.STAY_BASIC }
        val slot=if(secret && stay!=null) stay else if(evolution.isNotEmpty()) {
            if(secret && w.stage>=TankEvolution.SECRET_STAGE && pl.evolution[p]==TankEvolution.BASIC) evolution[style%evolution.size]
            else if(pl.evolution[p]==TankEvolution.BASIC) evolution.firstOrNull {TankEvolution.fromReward(s.rewardOffer(p,it))!!.ordinal==style+1}?:evolution.first()
            else evolution[rng.int(0,evolution.lastIndex)]
        }else offers.maxByOrNull {when(s.rewardOffer(p,it)){Perks.SIPHON,Perks.SECOND_WIND,Perks.THICK_SKIN->if(pl.hp[p]<pl.maxHp[p]/2)8 else 4;Perks.OVERCLOCK->5;else->3}}?:offers.first()
        check(s.pickPerk(w,p,slot))
    }
    repeat(2){if(pl.hp[p] <= pl.maxHp[p]-(if(skill==0)3 else 2) && s.canHeal(w,p))s.heal(w,p)}
    repeat(when(motive){0,7->2;2,3,6,9->5;else->4}){
        val k=(0 until SHOP_SLOTS).filter{s.canBuy(w,p,it)}.maxByOrNull { slot ->
            val u=s.item(p,slot);val frame=pl.evolution[p]
            val weight=when(u){Upgrades.DAMAGE->9;Upgrades.FIRE_RATE->if(frame.melee)0 else 8;Upgrades.MAX_HEALTH->9;Upgrades.REGEN->10;Upgrades.PIERCE->7;Upgrades.SPEED->6;Upgrades.WEALTH->5;Upgrades.RAM_DAMAGE->if(frame.melee)10 else 1;Upgrades.SHIELD_RECHARGE->12;Upgrades.DRONE_POWER->10;else->3}
            weight*s.levelsBought(w,p,slot).toDouble()/s.price(w,p,slot).coerceAtLeast(1)*(if(skill==0 || motive==5) rng.range(.45f,1.65f) else 1f)
        }
        if(k!=null){if(s.buy(w,p,k))bought++}
        else if((skill==2 || motive==3 || motive==6) && it==1 && s.canReroll(w,p) && pl.coins[p]>s.rerollPrice(w,p)*3)s.reroll(w,p)
    }
    return bought
}
/** 50 archetypes: ten motivations × five preferred paths. Gender is a label, not a behavior parameter. */
private val motives = arrayOf("Casual", "Social", "Completionist", "Optimizer", "Challenge", "Experimenter", "Collector", "Short session", "Returning", "Mastery")
private val styles = arrayOf("Twin", "Machine", "Sniper", "Rammer", "Secret")
private fun cohortProfile(id:Int,days:Int,out:File,maxSeconds:Int,sessionLimit:Int) {
    val archetype=id%50; val motive=archetype/5;val preference=archetype%5
    val gender=if((id/50)%2==0) "female" else "male"
    val personal=Rng(9001L+id*173L); val initialSkill=when(motive){0,7->personal.int(0,1);3,4,9->personal.int(1,2);else->personal.int(0,2)}
    val cls=CharacterDef.entries[personal.int(0,CharacterDef.entries.lastIndex)]; val ledger=Totals()
    var priorRuns=0; var recentStage=0;var wins=0
    out.bufferedWriter().use { writer ->
        for(day in 1..days) {
            val schedule=Rng((id+1L)*334363L+day*4057L)
            val planned=when(motive){0,7->1;1,5,8->schedule.int(1,2);2,3,6->schedule.int(2,3);4,9->schedule.int(2,4);else->1}
            for(session in 1..minOf(planned,sessionLimit)) {
                val seed=(id+1L)*100003L+day*7919L+session*99991L; val rng=Rng(seed xor 0xCAFE)
                val skill=(initialSkill+if(motive in 2..6 || motive==9){if(priorRuns>=24)1 else 0}else if(priorRuns>=45)1 else 0).coerceAtMost(2)
                val difficulty=when(motive){0,7->if(recentStage<10)Difficulty.EASY else Difficulty.NORMAL;4->if(wins>1 || recentStage>=30)Difficulty.IMPOSSIBLE else Difficulty.HARD;9->if(wins>1)Difficulty.HARD else Difficulty.NORMAL;else->if(recentStage<5 && priorRuns>=2)Difficulty.EASY else Difficulty.NORMAL}
                val team=if(motive==1 || schedule.nextFloat()<.24f)2 else 1
                val branch=if(motive==5 || (motive==8 && day%7==0)) rng.int(0,4) else preference
                val secret=branch==4; val style=if(secret) rng.int(0,2) else branch
                val w=World(seed);w.reset(team,seed,difficulty)
                for(p in 0 until team){w.players.chooseCharacter(p,if(p==0)cls else CharacterDef.entries[(id+day+session)%4],difficulty.bonusHp);w.players.missionMask[p]=if(p==0)CardMission.mask(ledger) else 0}
                val bots=Array(team){p->CohortAgent(skill,(style+p)%4,Rng(seed+p*19))};val input=Array(MAX_PLAYERS){PlayerInput()}
                var purchases=0;var firstCoins=-1;var firstBuy=-1;var peak=0;var steps=0;var tier1="None"
                val limit=minOf(maxSeconds,if(motive==7) 420 else if(motive==0) 1050 else maxSeconds)
                while(!w.over && steps<limit*TICK_HZ){
                    if(w.phase==Phase.SHOP){if(w.stage==1)firstCoins=w.players.coins[0];for(p in 0 until team){val n=cohortShop(w,p,(style+p)%4,skill,rng,secret && p==0,motive);if(p==0){purchases+=n;if(w.stage==1)firstBuy=n}};if(w.stage==5)tier1=w.players.evolution[0].label;w.nextStage()}
                    for(p in 0 until team)bots[p].drive(w,p,input[p]);w.step(input);w.events.clear();steps++;peak=max(peak,w.enemies.count)
                }
                val pl=w.players;val cause=pl.killedBy[0];val ended=w.over;val unlocks=CardMission.mask(ledger)
                ledger.add(RunEntry(day*10L+session,difficulty,w.stage,steps/TICK_HZ,w.score(0),pl.kills[0],pl.earned[0],w.bossesDown,team,cause,!ended,pl.dealt[0].toInt(),pl.fired[0],pl.taken[0],pl.downs[0],pl.revived[0],pl.bought(0),w.victory))
                recentStage=w.stage; priorRuns++; if(w.victory)wins++
                writer.appendLine(listOf(id,archetype,motives[motive],styles[preference],gender,day,session,skill,styles[branch],cls.label,difficulty.name,team,seed,w.stage,w.victory,ended,steps/TICK_HZ,firstCoins,firstBuy,purchases,tier1,pl.evolution[0].label,pl.kills[0],pl.earned[0],pl.taken[0],w.bossesDown,peak,cause,unlocks,CardMission.mask(ledger)).joinToString(","));writer.flush()
            }
        }
    }
}
fun main(args:Array<String>){
    val players=args.getOrNull(0)?.toInt()?:1000;val days=args.getOrNull(1)?.toInt()?:30;val threads=args.getOrNull(2)?.toInt()?:4;val root=File(args.getOrNull(3)?:"cohort-results");val maxSeconds=args.getOrNull(4)?.toInt()?:2100;val stride=args.getOrNull(5)?.toInt()?:1;val offset=args.getOrNull(6)?.toInt()?:0;val sessionLimit=args.getOrNull(7)?.toInt()?:4
    require(players>0 && days>0 && threads>0 && maxSeconds>0 && stride>0 && offset>=0 && sessionLimit in 1..4);root.mkdirs();val pool=Executors.newFixedThreadPool(threads)
    val jobs=(0 until players).map{id->pool.submit {cohortProfile(offset+id*stride,days,File(root,"p%05d.csv".format(id)),maxSeconds,sessionLimit)}}
    pool.shutdown();for(j in jobs)j.get();pool.awaitTermination(1,TimeUnit.MINUTES)
    var count=0
    File(root,"sessions.csv").bufferedWriter().use { w->w.appendLine("profile,archetype,motive,preferred_branch,gender,day,session,skill,chosen_branch,class,difficulty,team,seed,stage,victory,ended,seconds,first_coins,first_buys,purchases,first_evolution,final_evolution,kills,earned,hits,bosses,peak_enemies,cause,unlock_before,unlock_after");for(id in 0 until players)File(root,"p%05d.csv".format(id)).forEachLine {w.appendLine(it);count++} }
    println("Completed $players profiles × $days days; $count sessions")
}
