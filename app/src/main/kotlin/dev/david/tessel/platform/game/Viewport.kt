package dev.david.tessel.platform.game

import dev.david.tessel.core.sim.ARENA_H
import dev.david.tessel.core.sim.ARENA_W
import kotlin.math.min

/**
 * Fits the fixed-size arena to the screen, centred, never stretched. On a
 * 20:9 phone it fills the screen exactly; on other shapes thin bars remain,
 * while each player in co-op uses a separate local camera.
 */
class Viewport {
    var scale = 1f
        private set
    var left = 0f
        private set
    var top = 0f
        private set

    val right get() = left + ARENA_W * scale
    val bottom get() = top + ARENA_H * scale

    fun fit(width: Int, height: Int) {
        scale = min(width.coerceAtLeast(1) / ARENA_W, height.coerceAtLeast(1) / ARENA_H)
        left = (width - ARENA_W * scale) / 2f
        top = (height - ARENA_H * scale) / 2f
    }

    /** Camera is local-only; simulation coordinates and input remain unchanged. */
    fun follow(width: Int, height: Int, zoom: Float, x: Float, y: Float) {
        fit(width, height)
        scale *= zoom.coerceIn(1f, 1.85f)
        if(width<=0 || height<=0)return
        val spanX = ARENA_W * scale; val spanY = ARENA_H * scale
        left = if (spanX <= width) (width - spanX) / 2f else (width / 2f - x * scale).coerceIn(width - spanX, 0f)
        top = if (spanY <= height) (height - spanY) / 2f else (height / 2f - y * scale).coerceIn(height - spanY, 0f)
    }

    /** A co-op player never switches to a shared team framing or the full-arena overview. */
    fun followLocal(width: Int, height: Int, zoom: Float, x: Float, y: Float, coop: Boolean) =
        follow(width, height, if (coop && zoom < 1.35f) 1.60f else zoom, x, y)

    /** Legacy helper for an optional overview; never used by the live player camera. */
    fun followTeam(width: Int, height: Int, zoom: Float, minX: Float, maxX: Float, minY: Float, maxY: Float) {
        fit(width, height)
        val base = scale
        if (base <= 0f) return
        val target = min(width / (maxX - minX + 200f), height / (maxY - minY + 160f))
        follow(width, height, min(zoom, (target / base).coerceAtLeast(1f)), (minX + maxX) / 2f, (minY + maxY) / 2f)
    }

    /** Close, isolated codex training camera. Never used in live gameplay. */
    fun preview(width: Int,height: Int,x: Float,y: Float) {
        scale=min(width/540f,height/270f).coerceAtLeast(.01f)
        left=width/2f-x*scale
        top=height/2f-y*scale
    }

    fun screenX(worldX: Float) = left + worldX * scale
    fun screenY(worldY: Float) = top + worldY * scale
}
