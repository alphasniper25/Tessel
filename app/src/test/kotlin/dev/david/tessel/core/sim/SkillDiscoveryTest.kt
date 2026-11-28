package dev.david.tessel.core.sim

import dev.david.tessel.core.content.*
import org.junit.Assert.*
import org.junit.Test

class SkillDiscoveryTest {
    @Test fun freshRunRevealsOnlySixRoots(){
        assertEquals(6,SkillGraph.nodes.count{SkillGraph.visibility(0L,it.id)==SkillVisibility.REVEALED})
        assertEquals(54,SkillGraph.nodes.count{SkillGraph.visibility(0L,it.id)==SkillVisibility.HIDDEN})
        assertTrue(SkillGraph.nodes.all{(SkillGraph.visibility(0L,it.id)==SkillVisibility.REVEALED)==it.root})
    }
    @Test fun discoveryIsExactlyOneEdgeFromOwnedNodesForEveryFrameOfProgression(){
        for(n in SkillGraph.nodes){
            val mask=SkillGraph.bit(n.id)
            val neighbors=SkillGraph.neighbors(n.id).map{it.id}.toSet()
            for(q in SkillGraph.nodes){
                val expected=when{q.id==n.id->SkillVisibility.OWNED;q.root||q.id in neighbors->SkillVisibility.REVEALED;else->SkillVisibility.HIDDEN}
                assertEquals("${n.id}->${q.id}",expected,SkillGraph.visibility(mask,q.id))
            }
        }
    }
    @Test fun crossRoutesRevealTheirAdjacentSectorWithoutBuyingItsRoot(){
        var mask=0L
        for(id in listOf(0,2,5,54)){
            assertEquals(SkillVisibility.REVEALED,SkillGraph.visibility(mask,id))
            mask=mask or SkillGraph.bit(id)
        }
        assertEquals(SkillVisibility.REVEALED,SkillGraph.visibility(mask,12))
        assertEquals(SkillVisibility.HIDDEN,SkillGraph.visibility(mask,13))
        assertFalse(SkillGraph.canBuy(mask,12,12,100000))
        assertTrue(SkillGraph.canBuy(mask,12,23,100000))
    }
    @Test fun restoredOwnershipReconstructsDiscoveryWithoutAnExtraSaveField(){
        val cp=Checkpoint(Difficulty.NORMAL,23,10,1,true,3)
        cp.present[0]=true;cp.hp[0]=10;cp.maxHp[0]=10;cp.skillNodes[0]=SkillGraph.bit(0) or SkillGraph.bit(2)
        val read=Checkpoint.read(cp.write())!!
        assertEquals(cp.skillNodes[0],read.skillNodes[0])
        for(n in SkillGraph.nodes)assertEquals(SkillGraph.visibility(cp.skillNodes[0],n.id),SkillGraph.visibility(read.skillNodes[0],n.id))
        assertEquals(SkillVisibility.HIDDEN,SkillGraph.visibility(0L,2))
    }
}
