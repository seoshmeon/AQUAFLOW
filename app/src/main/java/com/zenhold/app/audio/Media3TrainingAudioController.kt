package com.zenhold.app.audio

import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator
import android.speech.tts.TextToSpeech
import androidx.core.content.getSystemService
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.zenhold.app.R
import com.zenhold.app.domain.model.CueStyle
import com.zenhold.app.domain.model.VibrationStrength
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Uses separate Media3 players so the short bell can be mixed over the ambient loop. */
@Singleton
class Media3TrainingAudioController @Inject constructor(
    @ApplicationContext private val context: Context,
) : TrainingAudioController {
    private var musicVolume = PREPARATION_MUSIC_VOLUME
    private var cueVolume = 0.7f
    private var vibrationEnabled = true
    private var cueStyle = CueStyle.Bell
    private var vibrationStrength = VibrationStrength.Medium
    private var textToSpeechReady = false
    private var textToSpeech: TextToSpeech? = null
    private var pendingSpeech: Pair<String, String>? = null
    private val musicPlayer = ExoPlayer.Builder(context).build().apply {
        repeatMode = Player.REPEAT_MODE_ONE
        volume = PREPARATION_MUSIC_VOLUME
        setHandleAudioBecomingNoisy(true)
        setAudioAttributes(mediaAttributes(), true)
    }
    private val cuePlayer = ExoPlayer.Builder(context).build().apply {
        volume = cueVolume
        // The cue should mix with (not steal focus from) the preparation player.
        setAudioAttributes(mediaAttributes(), false)
    }

    init {
        textToSpeech = TextToSpeech(context) { status ->
            textToSpeechReady = status == TextToSpeech.SUCCESS
            if (textToSpeechReady) {
                val russian = Locale.forLanguageTag("ru-RU")
                val engine = textToSpeech ?: return@TextToSpeech
                val languageResult = engine.setLanguage(russian)
                if (languageResult == TextToSpeech.LANG_MISSING_DATA ||
                    languageResult == TextToSpeech.LANG_NOT_SUPPORTED
                ) engine.language = Locale.getDefault()
                engine.setSpeechRate(0.86f)
                engine.setPitch(0.96f)
                pendingSpeech?.let { (text, utteranceId) ->
                    engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
                    pendingSpeech = null
                }
            }
        }
    }

    override fun configure(
        musicVolumePercent: Int,
        cueVolumePercent: Int,
        vibrationEnabled: Boolean,
        cueStyle: CueStyle,
        vibrationStrength: VibrationStrength,
    ) {
        musicVolume = musicVolumePercent.coerceIn(0, 100) / 100f
        cueVolume = cueVolumePercent.coerceIn(0, 100) / 100f
        this.vibrationEnabled = vibrationEnabled
        this.cueStyle = cueStyle
        this.vibrationStrength = vibrationStrength
        musicPlayer.volume = musicVolume
        cuePlayer.volume = cueVolume
    }

    override suspend fun startPreparationMusic() {
        val file = ensureAmbientFile()
        musicPlayer.volume = musicVolume
        musicPlayer.setMediaItem(MediaItem.fromUri(file.toURI().toString()))
        musicPlayer.prepare()
        musicPlayer.play()
    }

    override fun stopPreparationMusic() {
        musicPlayer.pause()
        musicPlayer.clearMediaItems()
    }

    /** Plays the bundled handpan track locally, without requiring a network connection. */
    override fun startHoldingMusic() {
        val uri = "android.resource://${context.packageName}/${R.raw.handpan_vdoh}"
        musicPlayer.volume = musicVolume
        musicPlayer.setMediaItem(MediaItem.fromUri(uri))
        musicPlayer.prepare()
        musicPlayer.play()
    }

    override fun stopHoldingMusic() {
        musicPlayer.pause()
        musicPlayer.clearMediaItems()
        musicPlayer.volume = musicVolume
    }

    override suspend fun playTransitionCue() {
        when (cueStyle) {
            CueStyle.Bell, CueStyle.Soft -> {
                val file = if (cueStyle == CueStyle.Bell) ensureBellFile() else ensureSoftCueFile()
                cuePlayer.setMediaItem(MediaItem.fromUri(file.toURI().toString()))
                cuePlayer.prepare()
                cuePlayer.play()
                vibrate()
            }
            CueStyle.VibrationOnly -> vibrate()
            CueStyle.Silent -> Unit
        }
    }

    override fun speakPreparationGuidance() {
        speak("Устройтесь удобно. Расслабьте лицо, плечи и живот. Дышите естественно.", "preparation")
    }

    override fun speakRecoveryGuidance() {
        speak("Спокойно вернитесь к дыханию. Не спешите начинать следующий подход.", "recovery")
    }

    override fun stopVoiceGuidance() {
        pendingSpeech = null
        textToSpeech?.stop()
    }

    override fun release() {
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        textToSpeech = null
        pendingSpeech = null
        musicPlayer.release()
        cuePlayer.release()
    }

    private fun speak(text: String, utteranceId: String) {
        if (textToSpeechReady) {
            textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
        } else {
            // TTS initializes asynchronously; keep the first cue instead of silently losing it.
            pendingSpeech = text to utteranceId
        }
    }

    private fun mediaAttributes() = AudioAttributes.Builder()
        .setUsage(C.USAGE_MEDIA)
        .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
        .build()

    private fun vibrate() {
        if (!vibrationEnabled) return
        val vibrator = context.getSystemService<Vibrator>() ?: return
        val pattern = when (vibrationStrength) {
            VibrationStrength.Gentle -> longArrayOf(0, 45, 55, 70)
            VibrationStrength.Medium -> longArrayOf(0, 80, 60, 140)
            VibrationStrength.Strong -> longArrayOf(0, 130, 70, 220)
        }
        vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
    }

    private suspend fun ensureAmbientFile(): File = withContext(Dispatchers.IO) {
        val target = File(context.cacheDir, "zenhold_ambient.wav")
        if (!target.exists()) {
            writeWave(target, durationSeconds = 12.0) { t ->
                val fade = minOf(1.0, t / 2.0, (12.0 - t) / 2.0).coerceAtLeast(0.0)
                fade * (0.32 * sin(2 * PI * 110.0 * t) + 0.18 * sin(2 * PI * 164.81 * t))
            }
        }
        target
    }

    private suspend fun ensureBellFile(): File = withContext(Dispatchers.IO) {
        val target = File(context.cacheDir, "zenhold_bell.wav")
        if (!target.exists()) {
            writeWave(target, durationSeconds = 3.5) { t ->
                val envelope = exp(-1.45 * t)
                envelope * (
                    0.46 * sin(2 * PI * 523.25 * t) +
                        0.27 * sin(2 * PI * 1046.5 * t) +
                        0.12 * sin(2 * PI * 1567.98 * t)
                    )
            }
        }
        target
    }

    private suspend fun ensureSoftCueFile(): File = withContext(Dispatchers.IO) {
        val target = File(context.cacheDir, "aquaflow_soft_cue.wav")
        if (!target.exists()) {
            writeWave(target, durationSeconds = 1.8) { t ->
                val envelope = exp(-2.2 * t)
                envelope * (0.32 * sin(2 * PI * 392.0 * t) + 0.12 * sin(2 * PI * 587.33 * t))
            }
        }
        target
    }

    private fun writeWave(file: File, durationSeconds: Double, sample: (Double) -> Double) {
        val sampleRate = 16_000
        val sampleCount = (sampleRate * durationSeconds).toInt()
        BufferedOutputStream(FileOutputStream(file)).use { output ->
            fun writeInt(value: Int) = repeat(4) { shift -> output.write(value shr (8 * shift)) }
            fun writeShort(value: Int) = repeat(2) { shift -> output.write(value shr (8 * shift)) }

            output.write("RIFF".toByteArray())
            writeInt(36 + sampleCount * 2)
            output.write("WAVEfmt ".toByteArray())
            writeInt(16)
            writeShort(1)
            writeShort(1)
            writeInt(sampleRate)
            writeInt(sampleRate * 2)
            writeShort(2)
            writeShort(16)
            output.write("data".toByteArray())
            writeInt(sampleCount * 2)
            repeat(sampleCount) { index ->
                val normalized = sample(index.toDouble() / sampleRate).coerceIn(-1.0, 1.0)
                writeShort((normalized * Short.MAX_VALUE).toInt())
            }
        }
    }

    private companion object {
        const val PREPARATION_MUSIC_VOLUME = 0.22f
    }
}
