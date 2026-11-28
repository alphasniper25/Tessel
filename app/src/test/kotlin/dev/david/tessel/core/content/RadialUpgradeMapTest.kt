package dev.david.tessel.core.content
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.*

class RadialUpgradeMapTest {
    @Test fun allNodesHaveStableIdsAndReachTheCentralTank(){
        for(dev in listOf(false,true)){
            val nodes=UpgradeMap.positions(dev)
            assertEquals(if(dev)Upgrades.count else Upgrades.count-2,nodes.size)
            assertEquals(nodes.size,nodes.map{it.id}.toSet().size)
            val edges=UpgradeMap.links(nodes)
            val seen=mutableSetOf(UpgradeMap.HUB_ID)
            repeat(nodes.size){for(e in edges)if(e.a in seen)seen.add(e.b)}
            assertEquals(nodes.map{it.id}.toSet(),seen-UpgradeMap.HUB_ID)
            assertTrue(nodes.all{hypot(it.x,it.y) in 124f..426f})
            for(a in nodes)for(b in nodes)if(a.id!=b.id)
                assertTrue("Overlap ${a.id},${b.id}",hypot(a.x-b.x,a.y-b.y)>55f)
        }
    }
    @Test fun marketCameraUsesUniformScaleAndFitsWholeCircle(){
        val cam=MarketCamera()
        for((w,h) in listOf(900f to 300f,640f to 220f,400f to 800f)){
            for(n in UpgradeMap.positions(true)){
                assertTrue(cam.sx(n.x,w,h) in 0f..w)
                assertTrue(cam.sy(n.y,w,h) in 0f..h)
            }
        }
    }
}
