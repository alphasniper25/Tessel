package dev.david.tessel.platform.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import dev.david.tessel.core.audio.Synth
import dev.david.tessel.core.audio.Synth.Sound
import java.io.File

/**
 * Plays the synthesised sounds. They are written once as small WAV files in the
 * cache folder and loaded into a [SoundPool], which mixes overlapping ones.
 * Frequent sounds are rate-limited so a crowd does not turn into a hiss.
 */
class Sfx(context: Context) {
    private val pool: SoundPool? = runCatching {
        SoundPool.Builder()
            .setMaxStreams(8)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
            .build()
    }.getOrNull()

    private val ids = IntArray(Sound.entries.size)
    private val lastAt = LongArray(Sound.entries.size) { -1_000_000L }
    private val loaded = java.util.concurrent.ConcurrentHashMap.newKeySet<Int>()

    @Volatile var enabled = true
    @Volatile var master = .75f
    @Volatile var uiEnabled = true

    init {
        val dir = File(context.cacheDir, "sfx-v12").apply { mkdirs() }
        pool?.let { p ->
            p.setOnLoadCompleteListener { _, id, status -> if(status == 0) loaded.add(id) }
            for (sound in Sound.entries) {
                runCatching {
                    val file = File(dir, sound.name.lowercase() + ".wav")
                    file.writeBytes(Synth.wav(Synth.make(sound)))
                    ids[sound.ordinal] = p.load(file.path, 1)
                }
            }
        }
    }

    @Synchronized fun play(sound: Sound, nowMillis: Long, gapMillis: Long = 0L, volume: Float = 1f) {
        if (!enabled) return
        val p = pool ?: return
        val i = sound.ordinal
        if (ids[i] == 0 || ids[i] !in loaded || nowMillis - lastAt[i] < gapMillis) return
        lastAt[i] = nowMillis
        val gain = (volume * master).coerceIn(0f,1f)
        val priority = when(sound) {
            Sound.BOSS,Sound.BOSS_DOWN,Sound.CORE_CHANGE,Sound.HURT,Sound.DOWN,Sound.REVIVE,Sound.WIN,Sound.LOSS,Sound.WARNING -> 4
            Sound.SHOT,Sound.RAPID,Sound.HIT,Sound.KILL,Sound.COIN -> 1
            else -> 2
        }
        p.play(ids[i],gain,gain,priority,0,1f)
    }

    fun release() {
        pool?.release()
    }
}
