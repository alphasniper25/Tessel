package dev.david.tessel.platform.game
import dev.david.tessel.core.sim.ARENA_W
import dev.david.tessel.core.sim.ARENA_H
import org.junit.Assert.*
import org.junit.Test

class ScreenFitTest{
    @Test fun safeAreaAndControlsStayInsideEveryRepresentativeDisplay(){
        for((w,h) in listOf(640 to 288,740 to 320,1280 to 720,1920 to 864,2560 to 1080,2560 to 1600,320 to 640,0 to 0)){
            for(d in listOf(1f,2f,3.5f)){
                val b=ScreenFit.safe(w,h,36,12,24,8)
                assertTrue(b.width>0&&b.height>0)
                val r=ScreenFit.stickRadius(68f*d,b)
                for(x in listOf(b.left,b.right,(b.left+b.right)/2)){
                    val c=ScreenFit.centered(x,r,b.left,b.right)
                    assertTrue(c in b.left..b.right)
                }
                assertTrue(ScreenFit.hudCenterWidth(b,d)<=b.width)
                assertTrue(ScreenFit.textScale(1500f,b.width*.4f) in 0f..1f)
            }
        }
    }
    @Test fun localCameraPreservesAspectAndPlayerVisibility(){
        for((w,h) in listOf(640 to 288,1280 to 720,1920 to 864,2560 to 1080,2560 to 1600,320 to 640)){
            for(z in listOf(1f,1.60f,1.85f)){
                for(x in listOf(14f,ARENA_W*.5f,ARENA_W-14f))for(y in listOf(14f,ARENA_H*.5f,ARENA_H-14f)){
                    val v=Viewport();v.followLocal(w,h,z,x,y,true)
                    assertTrue(v.scale>0f)
                    assertTrue(v.screenX(x) in -.01f..w.toFloat()+.01f)
                    assertTrue(v.screenY(y) in -.01f..h.toFloat()+.01f)
                    assertEquals(ARENA_W/ARENA_H,(v.right-v.left)/(v.bottom-v.top),.001f)
                }
            }
        }
    }
}
