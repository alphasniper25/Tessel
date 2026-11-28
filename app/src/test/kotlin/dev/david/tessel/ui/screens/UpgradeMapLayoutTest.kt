package dev.david.tessel.ui.screens

import dev.david.tessel.core.content.*
import org.junit.Assert.*
import org.junit.Test

class UpgradeMapLayoutTest{
    @Test fun allCompatibleNodesFitAndIncompatiblePathsHaveNoCoordinates(){
        for(frame in TankEvolution.entries)for(last in listOf(0,5,40)){
            val layout=UpgradeMapLayout(frame,last)
            assertEquals(UpgradeTree.visibleNodes(frame,last).map{it.id}.toSet(),layout.points.map{it.node.id}.toSet())
            for(p in layout.points){
                assertTrue(p.x in -layout.width/2..layout.width/2&&p.y in -layout.height/2..layout.height/2)
                val camera=UpgradeMapCamera()
                assertTrue(camera.sx(layout,740f,260f,p.x) in 0f..740f)
                assertTrue(camera.sy(layout,740f,260f,p.y) in 0f..260f)
                val levels=List(Upgrades.count){0}
                val expected=if(UpgradeTree.discovery(p.node,levels)==UpgradeDiscovery.HIDDEN)null else p.node.id
                assertEquals(expected,camera.hit(layout,740f,260f,camera.sx(layout,740f,260f,p.x),camera.sy(layout,740f,260f,p.y),12f,levels))
            }
        }
    }
    @Test fun zoomFocusAndPanKeepNodeCoordinatesConsistent(){
        val layout=UpgradeMapLayout(TankEvolution.RAMMER,5)
        val p=layout.points.first{it.node.upgrade==Upgrades.DAMAGE}
        val c=UpgradeMapCamera()
        val z=c.focus(layout,900f,280f,p)
        assertTrue(z.zoom>=3f)
        val x=z.sx(layout,900f,280f,p.x);val y=z.sy(layout,900f,280f,p.y)
        assertEquals(p.node.id,z.hit(layout,900f,280f,x,y,12f,List(Upgrades.count){0}))
    }

    @Test fun radialGraphHasRealPrerequisiteLinksAndEveryChainGrowsOutward(){
        for(frame in TankEvolution.entries)for(last in listOf(0,5,40)){
            val layout=UpgradeMapLayout(frame,last)
            assertEquals(layout.points.size,layout.links.size)
            val rootIds=layout.links.filter{it.from==null}.map{it.to}.toSet()
            assertEquals(layout.points.filter{UpgradeTree.prerequisite(it.node)==null}.map{it.node.id}.toSet(),rootIds)
            for(link in layout.links){
                val node=layout.point(link.to)!!
                assertEquals(UpgradeTree.prerequisite(node.node)?.id,link.from)
                if(link.from!=null)assertTrue(node.radius>layout.point(link.from)!!.radius)
            }
            for(i in layout.points.indices)for(j in i+1 until layout.points.size){
                val a=layout.points[i];val b=layout.points[j]
                assertTrue("Overlapping ${frame.label}: ${a.node.id}, ${b.node.id}",kotlin.math.hypot(a.x-b.x,a.y-b.y)>=UpgradeMapLayout.MIN_SPACING-.01f)
            }
        }
    }

    @Test fun sharedPositionsAndFitDoNotJumpWhenNewSpecializationsAppear(){
        val basic=UpgradeMapLayout(TankEvolution.BASIC,0)
        assertEquals(95,basic.points.size)
        for(frame in TankEvolution.entries){
            val next=UpgradeMapLayout(frame,40)
            assertEquals(basic.width,next.width,0f)
            assertEquals(basic.height,next.height,0f)
            for(p in basic.points)assertEquals(p,next.point(p.node.id))
        }
    }

    @Test fun fitAndZoomAllowEveryRevealedRankToBeHitOnRepresentativeScreens(){
        val owned=List(Upgrades.count){u->Upgrades.runCap(u)}
        for(frame in listOf(TankEvolution.BASIC,TankEvolution.TWIN,TankEvolution.RAMMER,TankEvolution.OVERSEER)){
            val layout=UpgradeMapLayout(frame,40)
            for((w,h) in listOf(640f to 240f,740f to 280f,1536f to 600f,1200f to 800f)){
                val c=UpgradeMapCamera()
                for(p in layout.points){
                    val x=c.sx(layout,w,h,p.x);val y=c.sy(layout,w,h,p.y)
                    assertTrue(x>0f&&x<w&&y>0f&&y<h)
                    assertEquals(p.node.id,c.hit(layout,w,h,x,y,12f,owned))
                }
            }
        }
    }
}
