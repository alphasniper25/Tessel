package dev.david.tessel.ui.screens

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import dev.david.tessel.core.content.Bestiary
import dev.david.tessel.core.content.TankEvolution
import dev.david.tessel.core.sim.CodexDemo
import dev.david.tessel.core.sim.Events
import dev.david.tessel.design.Palette
import dev.david.tessel.platform.game.Renderer
import dev.david.tessel.platform.game.Shards
import dev.david.tessel.platform.game.Viewport
import dev.david.tessel.platform.input.StickView
import dev.david.tessel.ui.components.SecondaryButton
import dev.david.tessel.ui.components.Text
import dev.david.tessel.ui.theme.*
import kotlinx.coroutines.delay

/** Only the currently opened tank runs a simulation, at 30 rendered frames per second.
 * The native game renderer draws into a reusable bitmap; there are no video assets. */
@Composable
fun CodexCombatPreview(frame:TankEvolution,modifier:Modifier=Modifier) {
    val c=Theme.colors
    val sim=remember(frame){CodexDemo(frame)}
    val renderer=remember(c.dark){Renderer(1f,if(c.dark)Palette.Dark else Palette.Light)}
    val viewport=remember { Viewport() }
    val shards=remember(frame){Shards(96)}
    val stick=remember{StickView()}
    val bitmap=remember { Bitmap.createBitmap(640,320,Bitmap.Config.ARGB_8888) }
    val native=remember(bitmap){android.graphics.Canvas(bitmap)}
    val img=remember(bitmap){bitmap.asImageBitmap()}
    var refresh by remember(frame){mutableIntStateOf(0)}
    var running by remember(frame){mutableStateOf(true)}
    LaunchedEffect(sim,running) {
        while(running) {
            delay(33L)
            if(sim.advance(2))shards.clear()
            val ev=sim.world.events
            for(i in 0 until ev.count) if(ev.kind[i]==Events.ENEMY_KILLED && ev.who[i] in 0 until Bestiary.size) {
                val type=ev.who[i];val def=Bestiary.of(type)
                shards.burst(def.shape,ev.x[i],ev.y[i],def.radius,ev.angle[i],ev.dx[i],ev.dy[i],renderer.bodyOf(type),renderer.facetOf(type))
            }
            shards.update(1f/30f)
            refresh++
        }
    }
    Column(modifier) {
        Canvas(Modifier.fillMaxWidth().aspectRatio(2f).clip(RoundedCornerShape(12.dp)).background(c.surface)) {
            // Observe the state inside the draw phase; the rest of the codex need not recompose.
            val tick=refresh
            if(tick>=0){
                val w=sim.world
                viewport.preview(bitmap.width,bitmap.height,w.players.x[0]+65f,w.players.y[0])
                renderer.draw(native,w,1f,viewport,shards,stick,stick,0,preview=true)
                drawImage(img,dstSize=IntSize(size.width.toInt().coerceAtLeast(1),size.height.toInt().coerceAtLeast(1)))
            }
        }
        Row(Modifier.fillMaxWidth(),verticalAlignment=androidx.compose.ui.Alignment.CenterVertically) {
            Text("LIVE TRAINING · 8s loop",Type.caption,c.accent,Modifier.weight(1f))
            SecondaryButton(if(running)"Pause" else "Play",{running=!running})
        }
    }
}
