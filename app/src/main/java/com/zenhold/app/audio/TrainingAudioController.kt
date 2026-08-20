package com.zenhold.app.audio

import com.zenhold.app.domain.model.CueStyle
import com.zenhold.app.domain.model.VibrationStrength

interface TrainingAudioController {
    fun configure(
        musicVolumePercent: Int,
        cueVolumePercent: Int,
        vibrationEnabled: Boolean,
        cueStyle: CueStyle = CueStyle.Bell,
        vibrationStrength: VibrationStrength = VibrationStrength.Medium,
    )
    suspend fun startPreparationMusic()
    fun stopPreparationMusic()
    fun startHoldingMusic()
    fun stopHoldingMusic()
    suspend fun playTransitionCue()
    fun speakPreparationGuidance()
    fun speakRecoveryGuidance()
    fun stopVoiceGuidance()
    fun release()
}
