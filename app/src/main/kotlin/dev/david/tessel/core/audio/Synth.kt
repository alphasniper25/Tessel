package dev.david.tessel.core.audio

import java.io.ByteArrayOutputStream
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * Every sound in the game is made here from sine sweeps and a little noise, so
 * there are no audio files to license. Each one is a short burst of 16-bit
 * mono samples with a quick fade in and a falling envelope.
 */
object Synth {
    const val RATE = 22050

    enum class Sound { SHOT, HIT, KILL, COIN, HURT, DOWN, BOSS, CLEAR, BUY, SURGE,
        RAPID, CANNON, SNIPER, RAIL, DRONE, RAM, BOSS_DOWN, CORE_CHANGE, REVIVE, HEAL, REROLL, LOCK, UNLOCK, SKILL, EVOLVE, MENU, START, SHOP, WIN, LOSS, WARNING, PERK_LOST }

    fun weapon(frame: dev.david.tessel.core.content.TankEvolution): Sound = when {
        frame.laser -> Sound.RAIL
        frame.drones -> Sound.DRONE
        frame.melee -> Sound.RAM
        frame.damage >= 4.4f && frame.rate <= .35f -> Sound.CANNON
        frame.reach >= 1.5f -> Sound.SNIPER
        frame.rate >= 1.6f -> Sound.RAPID
        else -> Sound.SHOT
    }

    fun make(sound: Sound): ShortArray = when (sound) {
        Sound.SHOT -> layer(tone(.065f,720f,220f,.30f,.13f),tone(.035f,1450f,600f,.12f,.25f))
        Sound.RAPID -> layer(tone(.034f,1160f,560f,.20f,.22f),tone(.03f,340f,170f,.10f,.12f))
        Sound.CANNON -> layer(tone(.23f,225f,70f,.48f,.10f,4f),tone(.10f,900f,180f,.27f,.64f,8f))
        Sound.SNIPER -> layer(tone(.12f,1600f,350f,.23f,.22f),tone(.16f,260f,100f,.32f,.11f))
        Sound.RAIL -> layer(tone(.20f,1900f,260f,.31f,.07f,4f),tone(.17f,380f,110f,.26f,.25f,5f))
        Sound.DRONE -> layer(tone(.085f,820f,380f,.22f,.13f),tone(.06f,230f,140f,.18f,.24f))
        Sound.RAM -> layer(tone(.15f,185f,65f,.43f,.15f,4f),tone(.065f,680f,210f,.20f,.38f))
        Sound.HIT -> tone(.038f,420f,160f,.20f,.42f,8f)
        Sound.KILL -> layer(tone(.10f,610f,220f,.27f,.20f),tone(.055f,1120f,360f,.13f,.29f))
        Sound.COIN -> notes(floatArrayOf(880f,1320f),.055f,.19f)
        Sound.HURT -> layer(tone(.20f,240f,80f,.35f,.22f,3.8f),tone(.14f,370f,130f,.19f,.17f))
        Sound.DOWN -> notes(floatArrayOf(440f,330f,196f),.14f,.29f)
        Sound.BOSS -> layer(tone(.62f,105f,160f,.36f,.09f,1.9f),tone(.48f,207f,294f,.22f,.04f,2.4f))
        Sound.BOSS_DOWN -> notes(floatArrayOf(392f,523.25f,659.25f,783.99f),.105f,.28f)
        Sound.CORE_CHANGE -> layer(tone(.75f,85f,210f,.35f,.12f,1.5f),notes(floatArrayOf(220f,311f,415f,554f),.15f,.23f))
        Sound.CLEAR -> notes(floatArrayOf(523.25f,659.25f,783.99f),.095f,.23f)
        Sound.BUY -> notes(floatArrayOf(660f,990f),.055f,.21f)
        Sound.SURGE -> layer(tone(.25f,520f,1550f,.26f,.05f,2.8f),tone(.22f,260f,775f,.15f))
        Sound.REVIVE -> notes(floatArrayOf(392f,523.25f,698.46f),.09f,.25f)
        Sound.HEAL -> notes(floatArrayOf(440f,554.37f,659.25f),.075f,.22f)
        Sound.REROLL -> tone(.10f,500f,1150f,.19f,.18f,4f)
        Sound.LOCK -> tone(.06f,440f,260f,.24f,.2f)
        Sound.UNLOCK -> tone(.07f,330f,660f,.20f,.09f)
        Sound.SKILL -> notes(floatArrayOf(523.25f,659.25f,1046.5f),.085f,.26f)
        Sound.EVOLVE -> layer(notes(floatArrayOf(330f,440f,660f,880f),.12f,.26f),tone(.45f,165f,440f,.16f,.04f,2f))
        Sound.MENU -> tone(.045f,660f,490f,.15f,.05f)
        Sound.START -> notes(floatArrayOf(392f,587.33f),.075f,.21f)
        Sound.SHOP -> notes(floatArrayOf(523.25f,783.99f),.09f,.20f)
        Sound.WIN -> layer(notes(floatArrayOf(392f,523.25f,659.25f,783.99f,1046.5f),.14f,.28f),tone(.7f,196f,392f,.13f,.01f,2f))
        Sound.LOSS -> notes(floatArrayOf(392f,311f,220f),.16f,.24f)
        Sound.WARNING -> notes(floatArrayOf(740f,540f),.095f,.26f)
        Sound.PERK_LOST -> notes(floatArrayOf(660f,440f,294f),.09f,.22f)
    }

    /** A sweep from [from] to [to] hertz over [seconds], with an optional share of noise. */
    fun tone(
        seconds: Float,
        from: Float,
        to: Float,
        volume: Float,
        noise: Float = 0f,
        decay: Float = 6f,
        square: Boolean = false,
    ): ShortArray {
        val n = (seconds * RATE).toInt().coerceAtLeast(1)
        val out = ShortArray(n)
        var phase = 0.0
        var seed = 12345
        for (i in 0 until n) {
            val t = i / n.toFloat()
            val freq = from + (to - from) * t
            phase += 2.0 * PI * freq / RATE
            var s = sin(phase).toFloat()
            if (square) s = if (s >= 0f) 0.6f else -0.6f
            seed = seed * 1103515245 + 12345
            val white = ((seed ushr 16) and 0x7FFF) / 16384f - 1f
            val mixed = s * (1f - noise) + white * noise
            val fade = minOf(1f, i / (RATE * 0.002f))
            val tail = minOf(1f, (n-1-i) / (RATE * .006f))
            val envelope = exp(-decay * t) * fade * tail
            out[i] = (mixed * envelope * volume * Short.MAX_VALUE).toInt().coerceIn(-32768, 32767).toShort()
        }
        return out
    }

    private fun notes(pitches: FloatArray, duration: Float, gain: Float) =
        join(*pitches.map { tone(duration,it,it,gain,decay=3.6f) }.toTypedArray())
    private fun layer(vararg parts: ShortArray): ShortArray {
        val n=parts.maxOf { it.size }
        return ShortArray(n) { i -> parts.sumOf { if(i < it.size) it[i].toInt() else 0 }.coerceIn(-30000,30000).toShort() }
    }

    fun join(vararg parts: ShortArray): ShortArray {
        val out = ShortArray(parts.sumOf { it.size })
        var at = 0
        for (p in parts) {
            p.copyInto(out, at)
            at += p.size
        }
        return out
    }

    /** The samples as a mono 16-bit WAV file. */
    fun wav(samples: ShortArray): ByteArray {
        val data = samples.size * 2
        val out = ByteArrayOutputStream(44 + data)
        fun int(v: Int) = out.write(byteArrayOf(v.toByte(), (v shr 8).toByte(), (v shr 16).toByte(), (v shr 24).toByte()))
        fun short(v: Int) = out.write(byteArrayOf(v.toByte(), (v shr 8).toByte()))
        out.write("RIFF".toByteArray())
        int(36 + data)
        out.write("WAVEfmt ".toByteArray())
        int(16)
        short(1)
        short(1)
        int(RATE)
        int(RATE * 2)
        short(2)
        short(16)
        out.write("data".toByteArray())
        int(data)
        for (s in samples) short(s.toInt())
        return out.toByteArray()
    }
}
