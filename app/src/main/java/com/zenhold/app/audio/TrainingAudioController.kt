package com.zenhold.app.audio

interface TrainingAudioController {
    suspend fun startPreparationMusic()
    fun stopPreparationMusic()
    fun startHoldingMusic()
    fun stopHoldingMusic()
    suspend fun playTransitionCue()
    fun release()
}
