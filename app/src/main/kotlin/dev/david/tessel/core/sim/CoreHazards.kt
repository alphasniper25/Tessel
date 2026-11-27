package dev.david.tessel.core.sim

import dev.david.tessel.core.content.Bestiary
import dev.david.tessel.core.content.Balance
import kotlin.math.*

/** Fixed-size final-boss hazard template. No entity allocation and no client hit guessing. */
class CoreHazards(private val w: World) {
    val x=FloatArray(CAP);val y=FloatArray(CAP);private val life=IntArray(CAP)
    var count=0;private set
    var shield=0f;private set
    private var shieldMax=0f
    private var quiet=0
    var hidden=0;private set
    var warning=0;private set
    var headX=0f;private set
    var headY=0f;private set
    private var elapsed=0;private var nextBeam=4*TICK_HZ;private var firing=0;private var target=-1;private var form=0;private var waved=false
    val beamActive get()=warning>0 || firing>0 || count>0
    val shieldShare get()=if(shieldMax>0f) (shield/shieldMax).coerceIn(0f,1f) else 0f
    fun reset(){count=0;shield=0f;shieldMax=0f;quiet=0;hidden=0;warning=0;headX=0f;headY=0f;elapsed=0;nextBeam=4*TICK_HZ;firing=0;target=-1;form=0;waved=false}
    fun show(s:Float,h:Int,ww:Int,hx:Float,hy:Float,n:Int,xs:FloatArray,ys:FloatArray){shieldMax=1f;shield=s;hidden=h;warning=ww;headX=hx;headY=hy;count=n;xs.copyInto(x,endIndex=n);ys.copyInto(y,endIndex=n)}
    fun absorb(damage:Float):Float {if(hidden>0)return 0f;quiet=0;val used=min(shield,damage);shield-=used;return damage-used}
    fun tick(i:Int,phase:Int){
        val e=w.enemies;val p=w.players
        if(phase!=form){reset();form=phase;shieldMax=e.maxHp[i]*.10f;shield=shieldMax}
        elapsed++
        if(++quiet>3*TICK_HZ) shield=min(shieldMax,shield+shieldMax/(12*TICK_HZ))
        if(hidden>0)hidden--
        if(!waved && e.hp[i]<e.maxHp[i]*.50f){
            waved=true;hidden=4*TICK_HZ;warning=0;firing=0;count=0
            repeat(10){k->val a=k*2f*PI.toFloat()/10;val sx=(ARENA_W/2+cos(a)*(ARENA_W*.43f));val sy=(ARENA_H/2+sin(a)*(ARENA_H*.40f));w.spawnEnemy(if(k%3==0)Bestiary.SLAB else Bestiary.PIP,sx,sy)}
        }
        if(nextBeam>0)nextBeam--
        if(hidden==0 && !beamActive && w.core.strike==CoreFight.STRIKE_NONE && nextBeam==0){
            nextBeam=12*TICK_HZ
            target=w.nearestPlayer(e.x[i],e.y[i]); if(target>=0){warning=TICK_HZ;val dx=p.x[target]-e.x[i];val dy=p.y[target]-e.y[i];val d=sqrt(dx*dx+dy*dy).coerceAtLeast(.1f);val at=(d-170f).coerceAtLeast(0f);headX=e.x[i]+dx/d*at;headY=e.y[i]+dy/d*at}
        }
        if(warning>0 && --warning==0) firing=3*TICK_HZ
        if(firing>0 && hidden==0){
            firing--
            if(target in 0 until w.playerCount && p.alive(target)){
                val dx=p.x[target]-headX;val dy=p.y[target]-headY;val d=sqrt(dx*dx+dy*dy).coerceAtLeast(.01f);val v=min(d,210f*TICK_SECONDS)
                headX+=dx/d*v;headY+=dy/d*v
            }
            if(firing%8==0 && count<CAP){x[count]=headX;y[count]=headY;life[count]=2*TICK_HZ;count++}
        }
        var j=0
        while(j<count){if(--life[j]<=0){val k=--count;for(q in j until k){x[q]=x[q+1];y[q]=y[q+1];life[q]=life[q+1]};continue};j++}
        for(a in 0 until w.playerCount){
            if(!p.alive(a)||p.invuln[a]>0)continue
            for(k in 0 until count){val dx=p.x[a]-x[k];val dy=p.y[a]-y[k];val r=RADIUS+Balance.PLAYER_RADIUS*.65f
                if(dx*dx+dy*dy<r*r){w.hurtPlayer(a,Cause.shot(Bestiary.CORE),if(w.difficulty==dev.david.tessel.core.content.Difficulty.EASY)3 else 5,allowRam=false);break}}
        }
    }
    companion object {const val CAP=24;const val RADIUS=20f}
}
