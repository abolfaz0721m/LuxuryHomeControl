package com.example.wirelessmic

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Holds the current effect-slider state and forwards changes to the bound
 * Service. The Service itself is the source of truth for streaming/Bluetooth
 * status (StreamUiState); this ViewModel just survives configuration changes
 * for the slider values and keeps MainActivity thin.
 */
class AudioStreamViewModel : ViewModel() {

    private val _effectSettings = MutableStateFlow(AudioEngine.EffectSettings())
    val effectSettings: StateFlow<AudioEngine.EffectSettings> = _effectSettings.asStateFlow()

    private var boundService: AudioStreamService? = null

    fun attachService(service: AudioStreamService?) {
        boundService = service
        service?.updateEffectSettings(_effectSettings.value)
    }

    fun setNoiseSuppression(enabled: Boolean) = update { it.copy(noiseSuppression = enabled) }
    fun setAgc(enabled: Boolean) = update { it.copy(agc = enabled) }

    /**
     * [progress] is 0..800 from the SeekBar, mapped to a 0.0..8.0 linear gain
     * (100 = unity/1.0x). Values above ~1.0 rely on the native soft-limiter
     * in AudioEngine/native-gain.cpp to avoid harsh clipping at high boost.
     */
    fun setManualGainFromSeek(progress: Int) = update { it.copy(manualGain = progress / 100f) }

    /** [progress] is 0..1000, matching BassBoost.setStrength's valid range. */
    fun setBassBoostFromSeek(progress: Int) = update { it.copy(bassBoostStrength = progress.toShort()) }

    /** [progress] is 0..2000 millibel, matching LoudnessEnhancer's target gain range. */
    fun setLoudnessFromSeek(progress: Int) = update { it.copy(loudnessGainMillibel = progress.toFloat()) }

    private fun update(transform: (AudioEngine.EffectSettings) -> AudioEngine.EffectSettings) {
        val newValue = transform(_effectSettings.value)
        _effectSettings.value = newValue
        boundService?.updateEffectSettings(newValue)
    }
}
