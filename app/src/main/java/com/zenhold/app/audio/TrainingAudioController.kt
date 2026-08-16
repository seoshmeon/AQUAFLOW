package com.zenhold.app.audio

interface TrainingAudioController {
    fun configure(musicVolumePercent: Int, cueVolumePercent: Int, vibrationEnabled: Boolean)
    suspend fun startPreparationMusic()
    fun stopPreparationMusic()
    fun startHoldingMusic()
    fun stopHoldingMusic()
    suspend fun playTransitionCue()
    fun release()
}
