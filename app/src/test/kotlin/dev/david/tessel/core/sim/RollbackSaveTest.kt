package dev.david.tessel.core.sim

import dev.david.tessel.core.content.*
import org.junit.Assert.*
import org.junit.Test

class RollbackSaveTest {
    @Test fun readsReworkFormatNineAndKeepsOriginalProgress(){
        val cp=Checkpoint(Difficulty.NORMAL,23,123,1,true,2)
        cp.present[0]=true;cp.hp[0]=10;cp.maxHp[0]=12;cp.coins[0]=420
        cp.perks[0]=(1 shl Perks.SECOND_WIND) or (1 shl 24)
        cp.skillNodes[0]=SkillGraph.bit(0) or SkillGraph.bit(1)
        cp.freeRerollsUsed[0]=3
        val old=cp.write()
        val rework=old.copyOf(old.size+9).also{it[2]=9;for(i in old.size until it.size)it[i]=(-1).toByte()}
        val back=Checkpoint.read(rework)!!
        assertEquals(23,back.stage);assertEquals(420,back.coins[0]);assertEquals(cp.skillNodes[0],back.skillNodes[0]);assertEquals(1 shl Perks.SECOND_WIND,back.perks[0]);assertEquals(10,back.maxHp[0]);assertEquals(10,back.hp[0]);assertEquals(3,back.freeRerollsUsed[0]);assertEquals(8,back.write()[2].toInt())
    }
}
