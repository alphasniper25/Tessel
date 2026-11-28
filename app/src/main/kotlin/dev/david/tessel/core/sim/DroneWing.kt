package dev.david.tessel.core.sim
import dev.david.tessel.core.content.*
import kotlin.math.*
/** Fixed owner/slot pool. Simulation and damage are host-authoritative. */
class DroneWing {
    companion object { const val LIMIT=8;const val CAPACITY=MAX_PLAYERS*LIMIT;const val RADIUS=8f }
    val hp=IntArray(CAPACITY);val x=FloatArray(CAPACITY);val y=FloatArray(CAPACITY)
    val prevX=FloatArray(CAPACITY);val prevY=FloatArray(CAPACITY);val angle=FloatArray(CAPACITY)
    private val nextSpawn=IntArray(MAX_PLAYERS);private val contact=IntArray(CAPACITY)
    private var anyLiving=false
    private val intercepted=IntArray(65536){Int.MIN_VALUE}
    private val targetSlot=IntArray(CAPACITY){-1}
    private val bossSlots=IntArray(MAX_ENEMIES)
    private val guardSlot=IntArray(MAX_PLAYERS){-1}
    private val guardX=FloatArray(MAX_PLAYERS)
    private val guardY=FloatArray(MAX_PLAYERS)
    private val frames=IntArray(MAX_PLAYERS){-1};private val target=IntArray(CAPACITY){-1}
    fun count(p:Int):Int{var n=0;for(i in p*LIMIT until (p+1)*LIMIT)if(hp[i]>0)n++;return n}
    fun clearOwner(p:Int){for(i in p*LIMIT until (p+1)*LIMIT){hp[i]=0;target[i]=-1;targetSlot[i]=-1;contact[i]=0};nextSpawn[p]=0;frames[p]=-1;guardSlot[p]=-1}
    fun clear(){for(p in 0 until MAX_PLAYERS)clearOwner(p);anyLiving=false;intercepted.fill(Int.MIN_VALUE)}
    fun interceptedRecently(id:Int,tick:Int):Boolean {val whenHit=intercepted[id and 65535];return whenHit!=Int.MIN_VALUE && tick-whenHit in 0..(8*TICK_HZ)}
    fun markIntercepted(id:Int,tick:Int){intercepted[id and 65535]=tick}
    fun step(w:World,inputs:Array<PlayerInput>){
        val pl=w.players;val e=w.enemies;anyLiving=false
        var hasOwner=false;for(p in 0 until w.playerCount){if(pl.alive(p)&&pl.evolution[p].drones&&!w.duel.on)hasOwner=true else if(frames[p]>=0)clearOwner(p)}
        if(!hasOwner)return
        var bossCount=0;for(j in 0 until e.count)if(Bestiary.of(e.type[j]).boss)bossSlots[bossCount++]=j
        for(p in 0 until w.playerCount){
            val f=pl.evolution[p];val s=f.droneSpec
            if(s==null||!pl.alive(p)||w.duel.on){if(frames[p]>=0)clearOwner(p);continue}
            if(frames[p]!=f.ordinal){clearOwner(p);frames[p]=f.ordinal}
            val base=p*LIMIT;val px=pl.x[p];val py=pl.y[p]
            val reach=(s.reach*(1f+Upgrades.delta(pl.levels[p],UpgradeStat.RANGE)*.5f+pl.skill(p,SkillBonus.RANGE)*.005f)).coerceAtMost(540f)
            if(count(p)<s.cap && nextSpawn[p]>0 && pl.stun[p]==0)nextSpawn[p]--
            if(nextSpawn[p]==0&&pl.stun[p]==0){for(k in 0 until s.cap){val i=base+k;if(hp[i]>0)continue
                val a=k*2f*PI.toFloat()/s.cap;x[i]=(px+cos(a)*25f).coerceIn(RADIUS,ARENA_W-RADIUS);y[i]=(py+sin(a)*25f).coerceIn(RADIUS,ARENA_H-RADIUS)
                prevX[i]=x[i];prevY[i]=y[i];angle[i]=a;hp[i]=s.hp;contact[i]=15;target[i]=-1
                val rate=(pl.fireRate(p)/(Balance.FIRE_RATE*f.rate)).coerceIn(.5f,2.4f)
                nextSpawn[p]=(s.respawn*TICK_HZ/rate).toInt().coerceAtLeast(30);break
            }}
            val input=inputs[p];val manual=input.fire&&pl.stun[p]==0&&input.aimX*input.aimX+input.aimY*input.aimY>.01f
            val aim=atan2(pl.aimY[p],pl.aimX[p]);val cx=px+cos(aim)*reach*.8f;val cy=py+sin(aim)*reach*.8f
            if(manual||pl.stun[p]>0)guardSlot[p]=-1 else if((w.tick+p)%6==0)selectGuard(w,p,s)
            for(k in 0 until s.cap){val i=base+k;if(hp[i]<=0)continue
                anyLiving=true;prevX[i]=x[i];prevY[i]=y[i];if(contact[i]>0)contact[i]--
                val a=k*2f*PI.toFloat()/s.cap+w.tick*TICK_SECONDS*.85f
                var tx=px+cos(a)*s.orbit;var ty=py+sin(a)*s.orbit
                val active=(k+(w.tick/(4*TICK_HZ))*((s.cap+2)/3))%s.cap<(s.cap+2)/3
                if(manual){tx=cx+cos(a)*24f;ty=cy+sin(a)*24f
                    if((w.tick+k)%12==0){var best=reach*reach;var id=-1;var slot=-1
                        for(j in 0 until e.count){if(!eligible(w,j))continue;val dx=e.x[j]-px;val dy=e.y[j]-py;val d=dx*dx+dy*dy
                            val along=dx*pl.aimX[p]+dy*pl.aimY[p]
                            if(d<best&&along>0f&&along*along>=d*.80f){best=d;id=e.id[j];slot=j}}
                        target[i]=id;targetSlot[i]=slot
                    }
                    val j=targetSlot[i];if(j in 0 until e.count && e.id[j]==target[i] && eligible(w,j)){val dx=e.x[j]-px;val dy=e.y[j]-py;val d=dx*dx+dy*dy;val along=dx*pl.aimX[p]+dy*pl.aimY[p];if(d<=reach*reach&&along>0f&&along*along>=d*.80f){tx=e.x[j];ty=e.y[j]}}
                }
                else if(active&&pl.stun[p]==0){
                    if((w.tick+k)%12==0){var best=reach*reach;var id=-1;var slot=-1
                        for(j in 0 until e.count){if(!eligible(w,j))continue;val dx=e.x[j]-px;val dy=e.y[j]-py;val d=dx*dx+dy*dy;if(d<best){best=d;id=e.id[j];slot=j}}
                        target[i]=id;targetSlot[i]=slot
                    }
                    val j=targetSlot[i];if(j in 0 until e.count && e.id[j]==target[i] && eligible(w,j)){val dx=e.x[j]-px;val dy=e.y[j]-py;if(dx*dx+dy*dy<=reach*reach){tx=e.x[j];ty=e.y[j]}}
                }else target[i]=-1
                if(!manual&&i==guardSlot[p]&&pl.stun[p]==0){tx=guardX[p];ty=guardY[p];target[i]=-1}
                val ox=x[i]-px;val oy=y[i]-py;if(ox*ox+oy*oy>(reach+85f)*(reach+85f)){tx=px;ty=py;target[i]=-1}
                var dx=tx.coerceIn(RADIUS,ARENA_W-RADIUS)-x[i];var dy=ty.coerceIn(RADIUS,ARENA_H-RADIUS)-y[i]
                for(q in base until base+s.cap){if(q==i||hp[q]<=0)continue;val sx=x[i]-x[q];val sy=y[i]-y[q];val ds=sx*sx+sy*sy;if(ds in .01f..400f){dx+=sx*(400f-ds)/ds.coerceAtLeast(16f);dy+=sy*(400f-ds)/ds.coerceAtLeast(16f)}}
                val d=sqrt(dx*dx+dy*dy);if(d>.01f){val travel=min(d,s.speed*TICK_SECONDS);x[i]=(x[i]+dx/d*travel).coerceIn(RADIUS,ARENA_W-RADIUS);y[i]=(y[i]+dy/d*travel).coerceIn(RADIUS,ARENA_H-RADIUS);angle[i]=atan2(dy,dx)}
                if(contact[i]>0)continue
                var hit=-1;w.forEnemiesNear(x[i],y[i],RADIUS+Bestiary.maxRegularRadius){j->if(hit<0&&touches(w,j,i))hit=j}
                if(hit<0)for(kb in 0 until bossCount){val j=bossSlots[kb];if(touches(w,j,i)){hit=j;break}}
                if(hit>=0){val hx=e.x[hit]-x[i];val hy=e.y[hit]-y[i];val dist=sqrt(hx*hx+hy*hy).coerceAtLeast(.1f)
                    w.damageEnemy(hit,pl.damage(p)*s.impact,hx/dist,hy/dist,p);pl.fired[p]++;hp[i]--;contact[i]=(s.contact*TICK_HZ).toInt()
                    x[i]=(x[i]-hx/dist*24f).coerceIn(RADIUS,ARENA_W-RADIUS);y[i]=(y[i]-hy/dist*24f).coerceIn(RADIUS,ARENA_H-RADIUS)
                }
            }
        }
    }
    private fun eligible(w:World,j:Int):Boolean {
        val e=w.enemies;val type=e.type[j]
        if(e.hp[j]<=0 || w.core.docked(j)) return false
        if(Bestiary.of(type).boss && (e.mode[j]==0 || (type==Bestiary.GYRE && e.mode[j]==1 && e.elite[j]!=Elite.TETHERED))) return false
        return type!=Bestiary.CORE || (e.mode[j]!=2 && w.core.hazards.hidden==0)
    }
    private fun touches(w:World,j:Int,i:Int):Boolean{if(!eligible(w,j))return false;val e=w.enemies;val r=RADIUS+Bestiary.of(e.type[j]).radius;val dx=e.x[j]-x[i];val dy=e.y[j]-y[i];return dx*dx+dy*dy<r*r}
    /** At most one idle drone breaks orbit toward a predicted incoming projectile.
     * It must travel physically to the intercept point; no guaranteed shield.
     * Manual aim still sends the entire formation. */
    private fun selectGuard(w:World,p:Int,s:DroneSpec){
        guardSlot[p]=-1
        val h=w.hostile
        val px=w.players.x[p];val py=w.players.y[p]
        var bestTime=.32f
        var gx=0f;var gy=0f
        for(i in 0 until h.count){
            if(h.life[i]<=0)continue
            val ox=h.x[i]-px;val oy=h.y[i]-py
            val vx=h.vx[i];val vy=h.vy[i]
            val a=vx*vx+vy*vy
            if(a<1f)continue
            val t=-(ox*vx+oy*vy)/a
            if(t<=0f||t>=bestTime)continue
            val tx=ox+vx*t;val ty=oy+vy*t
            if(tx*tx+ty*ty>30f*30f)continue
            // Guard at an inner ring ahead of the tank, not by chasing distant bullets.
            val len=sqrt(ox*ox+oy*oy).coerceAtLeast(.001f)
            if(len>240f)continue
            val ring=min(s.orbit*.82f,65f)
            gx=px+ox/len*ring;gy=py+oy/len*ring;bestTime=t
        }
        if(bestTime>=.32f)return
        val base=p*LIMIT
        var nearest=Float.POSITIVE_INFINITY
        for(k in 0 until s.cap){
            val i=base+k;if(hp[i]<=0)continue
            val attack=(k+(w.tick/(4*TICK_HZ))*((s.cap+2)/3))%s.cap<(s.cap+2)/3
            if(attack)continue
            val dx=x[i]-gx;val dy=y[i]-gy;val dist=dx*dx+dy*dy
            if(dist<nearest){nearest=dist;guardSlot[p]=i}
        }
        guardX[p]=gx;guardY[p]=gy
    }
    /** Consume exactly one HP from the first living drone touched along the bullet's path. */
    fun interceptSwept(ax:Float,ay:Float,bx:Float,by:Float,radius:Float,before:Float=Float.POSITIVE_INFINITY):Int {
        if(!anyLiving)return -1
        var best=before;var slot=-1
        val r=radius+RADIUS
        for(i in 0 until CAPACITY){
            if(hp[i]<=0)continue
            val t=contactTime(ax,ay,bx,by,prevX[i],prevY[i],x[i],y[i],r)
            if(t<best){best=t;slot=i}
        }
        if(slot>=0)hp[slot]--
        return slot
    }
    fun intercept(px:Float,py:Float,radius:Float)=interceptSwept(px,py,px,py,radius)>=0
}
