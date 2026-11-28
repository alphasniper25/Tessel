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
}
