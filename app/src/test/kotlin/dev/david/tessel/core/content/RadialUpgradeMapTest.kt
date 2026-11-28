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

    @Test fun allRankNodesUseSameRadiusAndExpandOutwardsWithoutOverlap(){
        for(dev in listOf(false,true)){
            val roots=UpgradeMap.positions(dev)
            val all=roots.flatMap{UpgradeMap.ranks(it,roots)}
            for(root in roots){
                val ranks=UpgradeMap.ranks(root,roots)
                assertEquals(Upgrades.runCap(root.id),ranks.size)
                assertEquals(UpgradeMap.FIRST_RADIUS,hypot(ranks.first().x,ranks.first().y),.002f)
                for((a,b) in ranks.zipWithNext())assertTrue(hypot(b.x,b.y)+.002f>=hypot(a.x,a.y))
            }
            for(i in all.indices)for(j in i+1 until all.size){
                val a=all[i];val b=all[j]
                assertTrue(hypot(a.x-b.x,a.y-b.y)>2*UpgradeMap.NODE_RADIUS)
            }
            val whole=UpgradeMap.extent(List(Upgrades.count){0},emptyMap(),dev)
            val after=UpgradeMap.extent(List(Upgrades.count){if(it==Upgrades.MAGNET)5 else 0},mapOf(Upgrades.MAGNET to 3),dev)
            assertEquals(whole,after,0f)
            assertTrue(whole<2200f)
            assertEquals(UpgradeMap.rankCount(dev),all.size)
            assertEquals(8,UpgradeMap.visibleCount(8,0,0))
            assertTrue(all.all{hypot(it.x,it.y)+UpgradeMap.NODE_RADIUS<=whole*.5f})
        }
    }
}
