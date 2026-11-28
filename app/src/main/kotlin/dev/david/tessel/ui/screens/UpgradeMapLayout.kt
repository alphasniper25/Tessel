package dev.david.tessel.ui.screens

import dev.david.tessel.core.content.*
import kotlin.math.*

data class PlacedUpgrade(val node:UpgradeNode,val x:Float,val y:Float)
data class UpgradeLaneLabel(val lane:UpgradeLane,val x:Float,val y:Float)
data class UpgradeRegion(val sector:UpgradeSector,val y:Float)

/** Layout is derived from the chosen frame, not hardcoded per tank. Rank paths
 * run left to right with prerequisite links between lanes. Future incompatible lanes have no coordinates. */
class UpgradeMapLayout(frame:TankEvolution,lastEvolution:Int){
    val points:List<PlacedUpgrade>
    val labels:List<UpgradeLaneLabel>
    val regions:List<UpgradeRegion>
    val width:Float
    val height:Float
    private val byId:Map<Int,PlacedUpgrade>
    init{
        val visible=UpgradeTree.lanes.filter{UpgradeTree.visibleLane(it,frame,lastEvolution)}
        val lanes=visible.groupBy{if(frame.drones&&it.sector==UpgradeSector.WEAPON)UpgradeSector.DRONES else it.sector}
        val raw=mutableListOf<PlacedUpgrade>()
        val labs=mutableListOf<UpgradeLaneLabel>()
        val regs=mutableListOf<UpgradeRegion>()
        val placed=mutableMapOf<Int,PlacedUpgrade>()
        var y=145f
        for((sector,group) in lanes){
            regs+=UpgradeRegion(sector,y-80f)
            for(lane in group){
                val root=UpgradeTree.node(lane.upgrade,1)!!
                val parent=UpgradeTree.prerequisite(root)?.let{placed[it.id]}
                val x0=if(parent!=null)parent.x+140f else 185f
                labs+=UpgradeLaneLabel(lane,x0,y-48f)
                for(rank in 1..lane.ranks){
                    val n=UpgradeTree.node(lane.upgrade,rank)!!
                    val point=PlacedUpgrade(n,x0+(rank-1)*104f,y)
                    raw+=point;placed[n.id]=point
                }
                y+=114f
            }
            y+=52f
        }
        width=(raw.maxOfOrNull{it.x}?:400f)+150f
        height=y+25f
        points=raw.map{it.copy(x=it.x-width*.5f,y=it.y-height*.5f)}
        labels=labs.map{it.copy(x=it.x-width*.5f,y=it.y-height*.5f)}
        regions=regs.map{it.copy(y=it.y-height*.5f)}
        byId=points.associateBy{it.node.id}
    }
    fun point(id:Int)=byId[id]
}
data class UpgradeMapCamera(val zoom:Float=1f,val x:Float=0f,val y:Float=0f){
    fun scale(layout:UpgradeMapLayout,w:Float,h:Float)=min(w/layout.width,h/layout.height).coerceAtLeast(.001f)*zoom
    fun sx(layout:UpgradeMapLayout,w:Float,h:Float,x:Float)=w*.5f+this.x+x*scale(layout,w,h)
    fun sy(layout:UpgradeMapLayout,w:Float,h:Float,y:Float)=h*.5f+this.y+y*scale(layout,w,h)
    fun gesture(layout:UpgradeMapLayout,w:Float,h:Float,cx:Float,cy:Float,dx:Float,dy:Float,factor:Float):UpgradeMapCamera{
        val z=(zoom*factor).coerceIn(1f,9f)
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
