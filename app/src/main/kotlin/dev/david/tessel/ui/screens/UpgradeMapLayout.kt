package dev.david.tessel.ui.screens

import dev.david.tessel.core.content.*
import kotlin.math.*

data class PlacedUpgrade(val node:UpgradeNode,val x:Float,val y:Float,val radius:Float)
data class UpgradeRegion(val sector:UpgradeSector,val x:Float,val y:Float)
data class UpgradeLink(val from:Int?,val to:Int)

/** Center-out constellation, inspired by the original 60-node map. Geometry
 * never defines purchase rules: each connection is a real prerequisite.
 * Shared paths are placed before specialization, so their locations do not
 * jump when a tank evolves. A compact serpentine path folds long rank chains
 * into radial shells instead of stretching them into horizontal ladders. */
class UpgradeMapLayout(frame:TankEvolution,lastEvolution:Int){
    companion object{
        const val ASPECT_X=1.25f
        const val ASPECT_Y=.67f
        const val MIN_SPACING=53f
        private val directions=mapOf(
            Upgrades.DAMAGE to -106f,Upgrades.RESERVE_POWER to -137f,
            Upgrades.MAX_HEALTH to 28f,Upgrades.HEAL_POWER to -2f,
            Upgrades.REGEN to 57f,Upgrades.RESERVE_HULL to 40f,
            Upgrades.FIELD_MEDIC to 12f,Upgrades.GUARDIAN_KIT to 14f,
            Upgrades.SPEED to 103f,Upgrades.RESERVE_DRIVE to 118f,Upgrades.AFTERBURNER to 77f,
            Upgrades.MAGNET to 177f,Upgrades.WEALTH to 149f,Upgrades.SCAVENGER to -166f,
            Upgrades.FIRE_RATE to -65f,Upgrades.RANGE to -22f,Upgrades.PIERCE to -9f,
            Upgrades.CALIBER to -43f,Upgrades.PHASE_CAPACITOR to -79f,
            Upgrades.DRONE_POWER to -43f,Upgrades.DRONE_RELAY to -31f,
            Upgrades.RAM_DAMAGE to -48f,Upgrades.SHIELD_RECHARGE to -9f,Upgrades.TITAN_PLATING to -27f
        )
        private val shifts=floatArrayOf(0f,2f,-2f,4f,-4f,6f,-6f,9f,-9f,12f,-12f,16f,-16f,20f,-20f,25f,-25f,30f,-30f)
        private fun depth(n:UpgradeNode):Int{
            var d=1;var p=UpgradeTree.prerequisite(n)
            while(p!=null){d++;p=UpgradeTree.prerequisite(p)}
            return d
        }
        private fun place(nodes:List<UpgradeNode>):List<PlacedUpgrade>{
            val done=linkedMapOf<Int,PlacedUpgrade>()
            val order=nodes.sortedWith(compareBy<UpgradeNode>({it.lane.specialized},{depth(it)},{it.id}))
            for(n in order){
                val parent=UpgradeTree.prerequisite(n)?.let{done[it.id]}
                val columns=if(n.lane.ranks>=7)3 else 2
                val slot=if(n.rank>1)(n.rank-2)%columns else 0
                val shell=if(n.rank>1)(n.rank-2)/columns else 0
                val start=if(parent==null)170f else parent.radius+when{
                    n.rank==1->85f
                    n.rank==2||slot==0->78f
                    else->3f
                }
                val offset=if(n.rank==1)0f else (slot-(columns-1)*.5f)*83f*(if(shell%2==0)1f else -1f)
                val theta=(directions.getValue(n.upgrade)*PI/180).toFloat()
                var best:PlacedUpgrade?=null
                var score=Float.POSITIVE_INFINITY
                // Deterministic bounded angular/radial search. Only menu layout,
                // no simulation work or allocation in the renderer's tick loop.
                for(step in 0..64){
                    if(step*2.7f>score)break
                    val radius=start+step*18f
                    for(shift in shifts){
                        val cost=step*2.7f+abs(shift)*.7f
                        if(cost>=score)continue
                        val a=theta+(shift*PI/180).toFloat()+atan2(offset,radius)
                        val x=radius*cos(a)*ASPECT_X;val y=radius*sin(a)*ASPECT_Y
                        if(done.values.any{val dx=it.x-x;val dy=it.y-y;dx*dx+dy*dy<MIN_SPACING*MIN_SPACING})continue
                        best=PlacedUpgrade(n,x,y,radius);score=cost
                    }
                }
                // The finite catalog is covered by all-frame spacing tests.
                // Keep a collision-free outward fallback for future appended nodes.
                if(best==null){
                    var radius=start+65*18f
                    while(best==null){
                        val x=radius*cos(theta)*ASPECT_X;val y=radius*sin(theta)*ASPECT_Y
                        if(done.values.none{val dx=it.x-x;val dy=it.y-y;dx*dx+dy*dy<MIN_SPACING*MIN_SPACING})
                            best=PlacedUpgrade(n,x,y,radius)
                        radius+=MIN_SPACING/ASPECT_Y
                    }
                }
                done[n.id]=requireNotNull(best)
            }
            return done.values.toList()
        }
        // Fixed common geometry and extents across the campaign. Cache only the
        // four actual visibility variants, not per-rank purchase state.
        private val basic by lazy{place(UpgradeTree.visibleNodes(TankEvolution.BASIC,0))}
        private val gun by lazy{place(UpgradeTree.visibleNodes(TankEvolution.TWIN,5))}
        private val drone by lazy{place(UpgradeTree.visibleNodes(TankEvolution.OVERSEER,15))}
        private val ram by lazy{place(UpgradeTree.visibleNodes(TankEvolution.RAMMER,5))}
        private val variants by lazy{listOf(basic,gun,drone,ram)}
        private val mapWidth by lazy{2f*(variants.flatten().maxOf{abs(it.x)}+125f)}
        private val mapHeight by lazy{2f*(variants.flatten().maxOf{abs(it.y)}+110f)}
    }
    private val visibleIds=UpgradeTree.visibleNodes(frame,lastEvolution).map{it.id}.toSet()
    val points:List<PlacedUpgrade> = (when{
        frame==TankEvolution.BASIC&&lastEvolution<5->basic
        frame.drones->drone
        frame.melee->ram
        else->gun
    }).filter{it.node.id in visibleIds}
    val width:Float=mapWidth
    val height:Float=mapHeight
    private val byId=points.associateBy{it.node.id}
    val links=points.map{p->UpgradeLink(UpgradeTree.prerequisite(p.node)?.id?.takeIf{it in byId},p.node.id)}
    val regions=points.groupBy{UpgradeTree.sector(it.node,frame)}.map{(sector,group)->
        // Keep sector labels on the outside of their own cluster.
        val outer=group.maxBy{it.radius}
        UpgradeRegion(sector,outer.x*1.065f,outer.y*1.065f)
    }
    val rings:List<Float> = (150..(points.maxOfOrNull{it.radius}?:150f).roundToInt() step 150).map{it.toFloat()}
    fun point(id:Int)=byId[id]
}
data class UpgradeMapCamera(val zoom:Float=1f,val x:Float=0f,val y:Float=0f){
    fun scale(layout:UpgradeMapLayout,w:Float,h:Float)=min(w/layout.width,h/layout.height).coerceAtLeast(.001f)*zoom
    fun sx(layout:UpgradeMapLayout,w:Float,h:Float,x:Float)=w*.5f+this.x+x*scale(layout,w,h)
    fun sy(layout:UpgradeMapLayout,w:Float,h:Float,y:Float)=h*.5f+this.y+y*scale(layout,w,h)
    fun gesture(layout:UpgradeMapLayout,w:Float,h:Float,cx:Float,cy:Float,dx:Float,dy:Float,factor:Float):UpgradeMapCamera{
        val z=(zoom*factor).coerceIn(1f,7f)
        val ratio=z/zoom;val scale=scale(layout,w,h)*ratio
        val bx=max(0f,(layout.width*scale-w)*.5f)+w*.18f
        val by=max(0f,(layout.height*scale-h)*.5f)+h*.18f
        return UpgradeMapCamera(z,(cx-w*.5f-(cx-w*.5f-x)*ratio+dx).coerceIn(-bx,bx),
            (cy-h*.5f-(cy-h*.5f-y)*ratio+dy).coerceIn(-by,by))
    }
    fun focus(layout:UpgradeMapLayout,w:Float,h:Float,point:PlacedUpgrade):UpgradeMapCamera{
        val z=max(zoom,3f)
        val scale=UpgradeMapCamera(z).scale(layout,w,h)
        return UpgradeMapCamera(z,-point.x*scale,-point.y*scale).gesture(layout,w,h,w*.5f,h*.5f,0f,0f,1f)
    }
    fun hit(layout:UpgradeMapLayout,w:Float,h:Float,px:Float,py:Float,radius:Float,levels:List<Int>):Int?{
        var best=max(radius,30f*scale(layout,w,h)).let{it*it};var found:UpgradeNode?=null
        for(p in layout.points){
            val dx=sx(layout,w,h,p.x)-px;val dy=sy(layout,w,h,p.y)-py;val d=dx*dx+dy*dy
            if(d<=best){found=p.node;best=d}
        }
        return found?.takeIf{UpgradeTree.discovery(it,levels)!=UpgradeDiscovery.HIDDEN}?.id
    }
}
