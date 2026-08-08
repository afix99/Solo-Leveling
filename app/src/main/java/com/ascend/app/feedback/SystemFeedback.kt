package com.ascend.app.feedback

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Sound and haptics for the moments that matter.
 *
 * Tones are synthesised at playback rather than shipped as audio files. Three
 * reasons: no megabytes added to the APK for a handful of blips, no licensing
 * question over sourced samples, and the notes can be defined as intervals —
 * so a rank-up is literally a rising triad rather than an arbitrary recording,
 * and the whole palette stays in tune with itself.
 *
 * Everything is off by default. A habit tracker that chirps in a meeting is a
 * habit tracker that gets uninstalled, so the user opts in.
 */
object SystemFeedback {

    private const val SAMPLE_RATE = 44_100

    /** A2-rooted minor scale. Sharing one root keeps every cue consonant. */
    internal object Note {
        const val A4 = 440.0
        const val C5 = 523.25
        const val D5 = 587.33
        const val E5 = 659.25
        const val G5 = 783.99
        const val A5 = 880.0
        const val C6 = 1046.5
        const val E6 = 1318.5
        const val F3 = 174.61
        const val A3 = 220.0
    }

    /** One synthesised note: frequency, length, and where it sits in the cue. */
    internal data class Tone(
        val hz: Double,
        val ms: Int,
        val startMs: Int = 0,
        val gain: Double = 1.0,
    )

    /**
     * The cues, defined musically.
     *
     * Ascending intervals for progress, a falling minor third for loss. The
     * shapes are deliberately different in length as well as pitch, so they
     * stay distinguishable when heard through a pocket.
     */
    enum class Cue(internal val tones: List<Tone>) {
        /** Short, dry tick. Fires many times a day, so it must not be a tune. */
        HABIT_DONE(listOf(Tone(Note.A5, 70, gain = 0.5))),

        /** Two-note lift — something advanced. */
        XP_GAIN(listOf(Tone(Note.E5, 60, gain = 0.4), Tone(Note.A5, 90, startMs = 55, gain = 0.4))),

        /** Rising minor triad: the System acknowledging a level. */
        LEVEL_UP(
            listOf(
                Tone(Note.A4, 110),
                Tone(Note.C5, 110, startMs = 90),
                Tone(Note.E5, 240, startMs = 180),
            ),
        ),

        /** Full octave climb, longer tail. Reserved for rank changes. */
        RANK_UP(
            listOf(
                Tone(Note.A4, 130),
                Tone(Note.C5, 130, startMs = 110),
                Tone(Note.E5, 130, startMs = 220),
                Tone(Note.A5, 200, startMs = 330),
                Tone(Note.C6, 200, startMs = 400, gain = 0.7),
                Tone(Note.E6, 460, startMs = 470, gain = 0.55),
            ),
        ),

        /** Quest or gate cleared: bright, two bright notes and a shimmer. */
        QUEST_CLEARED(
            listOf(
                Tone(Note.D5, 90),
                Tone(Note.G5, 90, startMs = 80),
                Tone(Note.C6, 260, startMs = 165, gain = 0.6),
            ),
        ),

        /** Falling third, low and short. A penalty should land, not scold. */
        PENALTY(listOf(Tone(Note.A3, 150, gain = 0.6), Tone(Note.F3, 300, startMs = 130, gain = 0.5))),

        /** Focus session complete — calm, not celebratory. */
        FOCUS_COMPLETE(
            listOf(Tone(Note.C5, 200, gain = 0.5), Tone(Note.G5, 420, startMs = 190, gain = 0.4)),
        ),
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /**
     * Plays [cue] if enabled, and vibrates if haptics are enabled.
     *
     * Fire-and-forget on a background scope: synthesis is a few milliseconds of
     * float maths, but it has no business happening on the frame the user just
     * tapped.
     */
    fun play(context: Context, cue: Cue, soundEnabled: Boolean, hapticsEnabled: Boolean) {
        if (hapticsEnabled) vibrate(context, cue)
        if (!soundEnabled) return
        scope.launch { runCatching { playCue(cue) } }
    }

    private fun playCue(cue: Cue) {
        val samples = render(cue)
        if (samples.isEmpty()) return

        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    // SONIFICATION, not MEDIA: these are UI cues, so they should
                    // duck under music rather than pausing it.
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build(),
            )
            .setTransferMode(AudioTrack.MODE_STATIC)
            .setBufferSizeInBytes(samples.size * 2)
            .build()

        track.write(samples, 0, samples.size)
        track.setNotificationMarkerPosition(samples.size)
        track.setPlaybackPositionUpdateListener(
            object : AudioTrack.OnPlaybackPositionUpdateListener {
                override fun onMarkerReached(t: AudioTrack?) {
                    runCatching { t?.release() }
                }

                override fun onPeriodicNotification(t: AudioTrack?) = Unit
            },
        )
        track.play()
    }

    /**
     * Renders a cue to 16-bit PCM.
     *
     * Each tone is a sine with an exponential decay envelope and a short fade-in.
     * The fade-in matters more than it sounds like it should: starting a sine at
     * full amplitude produces a click, which reads as a glitch rather than a note.
     */
    internal fun render(cue: Cue): ShortArray {
        val totalMs = cue.tones.maxOf { it.startMs + it.ms } + 40
        val total = SAMPLE_RATE * totalMs / 1000
        val mix = DoubleArray(total)

        for (tone in cue.tones) {
            val start = SAMPLE_RATE * tone.startMs / 1000
            val length = SAMPLE_RATE * tone.ms / 1000
            val attack = (SAMPLE_RATE * 0.006).toInt().coerceAtLeast(1)

            for (i in 0 until length) {
                val index = start + i
                if (index >= total) break
                val t = i.toDouble() / SAMPLE_RATE

                val decay = exp(-3.2 * i.toDouble() / length)
                val attackGain = if (i < attack) i.toDouble() / attack else 1.0

                // A quiet second harmonic stops the tone sounding like a test
                // signal without making it a synth patch.
                val fundamental = sin(2.0 * PI * tone.hz * t)
                val harmonic = 0.18 * sin(4.0 * PI * tone.hz * t)

                mix[index] += (fundamental + harmonic) * decay * attackGain * tone.gain * 0.42
            }
        }

        return ShortArray(total) { i ->
            (mix[i].coerceIn(-1.0, 1.0) * Short.MAX_VALUE).toInt().toShort()
        }
    }

    private fun vibrate(context: Context, cue: Cue) {
        val vibrator = vibratorOf(context) ?: return
        if (!vibrator.hasVibrator()) return

        // Patterns mirror the audio shape so the two read as one event.
        val pattern: LongArray = when (cue) {
            Cue.HABIT_DONE, Cue.XP_GAIN -> longArrayOf(0, 18)
            Cue.LEVEL_UP -> longArrayOf(0, 26, 60, 26)
            Cue.RANK_UP -> longArrayOf(0, 30, 50, 30, 50, 70)
            Cue.QUEST_CLEARED -> longArrayOf(0, 22, 50, 40)
            Cue.PENALTY -> longArrayOf(0, 90)
            Cue.FOCUS_COMPLETE -> longArrayOf(0, 40, 90, 40)
        }

        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(pattern, -1)
            }
        }
    }

    private fun vibratorOf(context: Context): Vibrator? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            manager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
}
