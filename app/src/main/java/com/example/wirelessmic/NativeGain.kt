package com.example.wirelessmic

/**
 * Thin wrapper around the native `wirelessmic_native` library.
 * Applying gain to a PCM16 buffer in native code avoids the overhead of
 * looping over samples in Kotlin/JVM on every audio callback.
 */
object NativeGain {

    private var loaded = false

    init {
        loaded = try {
            System.loadLibrary("wirelessmic_native")
            true
        } catch (e: UnsatisfiedLinkError) {
            // Falls back to the pure-Kotlin path in AudioEngine if the .so
            // failed to load (e.g. unsupported ABI). The app must stay usable
            // even without the native optimization.
            false
        }
    }

    fun isAvailable(): Boolean = loaded

    /**
     * Multiplies every sample in [buffer] (first [length] entries) by [gain],
     * in place, with hard int16 clipping protection. Kept for compatibility;
     * prefer [applyGainWithLimiter] for anything above unity gain.
     */
    external fun applyGain(buffer: ShortArray, length: Int, gain: Float)

    /**
     * Same as [applyGain] but runs the result through a soft-knee limiter so
     * gain well above 1.0 (loud boosts) saturates smoothly instead of
     * clipping harshly. This is what AudioEngine uses.
     */
    external fun applyGainWithLimiter(buffer: ShortArray, length: Int, gain: Float)
}
