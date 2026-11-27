package dev.david.tessel.platform.game

import org.junit.Assert.*
import org.junit.Test

class LocalViewportTest {
    @Test fun separatedPlayersGetDifferentCamerasEvenWithOverviewSetting() {
        val a = Viewport()
        val b = Viewport()
        a.followLocal(1920,864,1f,300f,432f,true)
        b.followLocal(1920,864,1f,1600f,432f,true)
        assertEquals(1.6f,a.scale,.001f)
        assertEquals(a.scale,b.scale,.001f)
        assertTrue(a.left > b.left)
        assertTrue(a.screenX(300f) in 0f..1920f)
        assertTrue(b.screenX(1600f) in 0f..1920f)
    }
    @Test fun soloOverviewIsStillAvailable() {
        val v=Viewport()
        v.followLocal(1920,864,1f,300f,432f,false)
        assertEquals(1f,v.scale,.001f)
    }
}
