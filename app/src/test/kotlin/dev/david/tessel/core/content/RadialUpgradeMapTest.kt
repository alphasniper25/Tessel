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
            assertTrue(nodes.all{hypot(it.x,it.y)>=UpgradeMap.FIRST_RADIUS-.002f})
            for(a in nodes)for(b in nodes)if(a.id!=b.id)
                assertTrue("Overlap ${a.id},${b.id}",hypot(a.x-b.x,a.y-b.y)>55f)
        }
    }
    @Test fun marketCameraUsesUniformScaleAndFitsWholeCircle(){
        val cam=MarketCamera(extent=UpgradeMap.extent(emptyList(),emptyMap(),true))
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
                assertEquals(root.x,ranks.first().x,0f);assertEquals(root.y,ranks.first().y,0f)
                assertEquals((1..Upgrades.runCap(root.id)).toList(),ranks.map{it.rank})
                for((a,b) in ranks.zipWithNext())assertTrue("Rank path gap ${root.id}: ${a.rank} to ${b.rank}",hypot(a.x-b.x,a.y-b.y)<150f)
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

    @Test fun purchasesAndRarityBundlesAlwaysFillAContiguousPrefix(){
        for(cap in 1..40)for(owned in 0..cap)for(bundle in 0..5){
            val ranks=1..cap
            assertEquals((1..owned).toList(),ranks.filter{UpgradeMap.ownedRank(it,owned)})
            val end=minOf(cap,owned+bundle)
            assertEquals(((owned+1)..end).toList(),ranks.filter{UpgradeMap.offeredRank(it,owned,bundle)})
            assertEquals((1..end).toList(),ranks.filter{UpgradeMap.ownedRank(it,owned)||UpgradeMap.offeredRank(it,owned,bundle)})
        }
    }
}
