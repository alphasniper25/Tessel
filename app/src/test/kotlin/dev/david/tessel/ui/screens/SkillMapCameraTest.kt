package dev.david.tessel.ui.screens
import dev.david.tessel.core.content.SkillGraph
import org.junit.Assert.*
import org.junit.Test

class SkillMapCameraTest{
    @Test fun overviewFitsEveryNodeAndAllCentersAreSelectable(){
        val w=900f;val h=380f;val cam=SkillMapCamera()
        for(n in SkillGraph.nodes){
            val x=cam.sx(n.x,w,h);val y=cam.sy(n.y,w,h)
            assertTrue(x in 0f..w&&y in 0f..h)
            assertEquals(n.id,cam.hit(w,h,x,y,15f))
        }
    }
    @Test fun zoomPreservesTheFingerAnchorAndPanHitTesting(){
        val w=900f;val h=380f;val n=SkillGraph.nodes[2];val c=SkillMapCamera()
        val x=c.sx(n.x,w,h);val y=c.sy(n.y,w,h)
        val z=c.gesture(w,h,x,y,0f,0f,2f)
        assertEquals(x,z.sx(n.x,w,h),.001f);assertEquals(y,z.sy(n.y,w,h),.001f)
        val p=z.gesture(w,h,x,y,20f,-10f,1f)
        assertEquals(n.id,p.hit(w,h,p.sx(n.x,w,h),p.sy(n.y,w,h),15f))
    }
    @Test fun zoomBoundsAndFitAreStable(){
        val c=SkillMapCamera()
        assertEquals(5.5f,c.gesture(900f,380f,450f,190f,0f,0f,50f).zoom,.001f)
        assertEquals(1f,c.gesture(900f,380f,450f,190f,0f,0f,.01f).zoom,.001f)
        assertNull(c.hit(900f,380f,0f,0f,15f))
    }

    @Test fun hiddenNodeCentersCannotBeInspectedEvenWithZoom(){
        val w=1200f;val h=520f
        val base=SkillMapCamera()
        val n=SkillGraph.nodes[1]
        for(cam in listOf(base,base.gesture(w,h,w/2,h/2,15f,-10f,2f))){
            val x=cam.sx(n.x,w,h);val y=cam.sy(n.y,w,h)
            assertNull(cam.hitDiscovered(w,h,x,y,15f,0L))
            assertEquals(1,cam.hitDiscovered(w,h,x,y,15f,SkillGraph.bit(0)))
        }
    }
}
