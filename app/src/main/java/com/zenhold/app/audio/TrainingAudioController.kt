package com.zenhold.app.audio

import com.zenhold.app.domain.model.CueStyle

interface TrainingAudioController {
    fun configure(
        musicVolumePercent: Int,
        cueVolumePercent: Int,
        vibrationEnabled: Boolean,
        cueStyle: CueStyle = CueStyle.Bell,
    )
    suspend fun startPreparationMusic()
    fun stopPreparationMusic()
    fun startHoldingMusic()
    fun stopHoldingMusic()
    suspend fun playTransitionCue()
    fun release()
}
