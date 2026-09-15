package com.example.wirelessmic

import android.annotation.SuppressLint
import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.AutomaticGainControl
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.NoiseSuppressor
import android.util.Log
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/**
 * Owns the AudioRecord -> AudioTrack pipeline. Reads raw PCM16 mono frames
 * from the phone's built-in microphone and writes them straight to the
 * AudioTrack, applying gain + effects along the way. Runs entirely on its
 * own thread to keep the loop tight and avoid GC pauses on the audio path
 * where possible.
 *
 * Routing policy (important): input is pinned to the phone's own
 * TYPE_BUILTIN_MIC regardless of what's connected over Bluetooth, and
 * output is pinned to whichever Bluetooth device (A2DP preferred, SCO as
 * fallback) is currently connected. See BluetoothRouteHelper.
 */
class AudioEngine(private val audioManager: AudioManager) {

    companion object {
        private const val TAG = "AudioEngine"
        private const val SAMPLE_RATE = 48000
        private const val CHANNEL_IN = AudioFormat.CHANNEL_IN_MONO
        private const val CHANNEL_OUT = AudioFormat.CHANNEL_OUT_MONO
        private const val ENCODING = AudioFormat.ENCODING_PCM_16BIT

        // Manual gain is allowed well above unity on purpose (see EffectSettings.manualGain).
        // The native soft-limiter keeps this from turning into harsh digital clipping.
        const val MAX_MANUAL_GAIN = 8.0f
    }

    data class EffectSettings(
        // Off by default: AGC and noise suppression both actively fight the
        // "keep every sound exactly as loud, don't duck it down" behavior
        // that was asked for. The user can still re-enable them from the UI.
        val noiseSuppression: Boolean = false,
        val agc: Boolean = false,
        val manualGain: Float = 1.0f,       // 1.0 = unity, up to MAX_MANUAL_GAIN
        val bassBoostStrength: Short = 0,   // 0..1000
        val loudnessGainMillibel: Float = 0f // 0..2000
    )

    private var audioRecord: AudioRecord? = null
    private var audioTrack: AudioTrack? = null

    private var noiseSuppressor: NoiseSuppressor? = null
    private var automaticGainControl: AutomaticGainControl? = null
    private var echoCanceler: AcousticEchoCanceler? = null
    private var bassBoost: BassBoost? = null
    private var equalizer: Equalizer? = null
    private var loudnessEnhancer: LoudnessEnhancer? = null

    private val running = AtomicBoolean(false)
    private var captureThread: Thread? = null

    private val settings = AtomicReference(EffectSettings())

    var onError: ((String) -> Unit)? = null

    fun updateSettings(newSettings: EffectSettings) {
        settings.set(newSettings.copy(manualGain = newSettings.manualGain.coerceIn(0f, MAX_MANUAL_GAIN)))
        applyEffectToggle(settings.get())
    }

    /**
     * Starts the capture/playback loop.
     *
     * [preferredOutputDevice] should be the currently-connected Bluetooth
     * output device (A2DP preferred, see BluetoothRouteHelper.findBluetoothOutputDevice).
     * [builtInMicDevice], if available, is used to explicitly pin capture to
     * the phone's own microphone so a connected Bluetooth headset's mic is
     * never picked up instead.
     */
    @SuppressLint("MissingPermission")
    fun start(preferredOutputDevice: AudioDeviceInfo?, builtInMicDevice: AudioDeviceInfo?): Boolean {
        if (running.get()) return true

        val minRecBuf = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_IN, ENCODING)
        val minTrackBuf = AudioTrack.getMinBufferSize(SAMPLE_RATE, CHANNEL_OUT, ENCODING)

        if (minRecBuf <= 0 || minTrackBuf <= 0) {
            onError?.invoke("دستگاه از این نرخ نمونه‌برداری پشتیبانی نمی‌کند.")
            return false
        }

        // Buffer sized ~2.5x the platform minimum: reasonable balance between
        // latency and stability. Bluetooth A2DP has more inherent latency
        // than SCO, so if you hear crackling, raise this multiplier.
        val recBufSize = (minRecBuf * 2.5).toInt()
        val trackBufSize = (minTrackBuf * 2.5).toInt()

        try {
            // UNPROCESSED gives raw mic samples with none of the phone's own
            // automatic gain/noise processing baked in. Not every device
            // supports it, so we fall back to plain MIC if construction fails.
            val record = createAudioRecord(MediaRecorder.AudioSource.UNPROCESSED, recBufSize)
                ?: createAudioRecord(MediaRecorder.AudioSource.MIC, recBufSize)

            if (record == null || record.state != AudioRecord.STATE_INITIALIZED) {
                record?.release()
                onError?.invoke("راه‌اندازی AudioRecord ناموفق بود.")
                return false
            }
            audioRecord = record

            // Pin capture to the phone's own microphone explicitly so a
            // connected Bluetooth headset mic is never used instead.
            builtInMicDevice?.let { record.preferredDevice = it }

            val trackAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()

            val format = AudioFormat.Builder()
                .setSampleRate(SAMPLE_RATE)
                .setChannelMask(CHANNEL_OUT)
                .setEncoding(ENCODING)
                .build()

            val track = AudioTrack.Builder()
                .setAudioAttributes(trackAttributes)
                .setAudioFormat(format)
                .setBufferSizeInBytes(trackBufSize)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            if (track.state != AudioTrack.STATE_INITIALIZED) {
                track.release()
                record.release()
                onError?.invoke("راه‌اندازی AudioTrack ناموفق بود.")
                return false
            }
            audioTrack = track

            preferredOutputDevice?.let { track.preferredDevice = it }

            setupEffects(record, track)
            applyEffectToggle(settings.get())

            record.startRecording()
            track.play()
            running.set(true)

            captureThread = Thread({ captureLoop(recBufSize) }, "AudioCaptureThread").apply {
                priority = Thread.MAX_PRIORITY
                start()
            }
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start audio engine", e)
            onError?.invoke("خطا در راه‌اندازی موتور صدا: ${e.message}")
            stop()
            return false
        }
    }

    private fun createAudioRecord(source: Int, bufferSize: Int): AudioRecord? {
        return try {
            AudioRecord(source, SAMPLE_RATE, CHANNEL_IN, ENCODING, bufferSize)
        } catch (e: Exception) {
            Log.w(TAG, "AudioSource $source unavailable on this device", e)
            null
        }
    }

    /**
     * Re-points the already-running AudioTrack at a different Bluetooth
     * output device without tearing down the whole engine — used when the
     * user connects/switches a Bluetooth device mid-stream.
     */
    fun switchOutputDevice(device: AudioDeviceInfo?) {
        device?.let { audioTrack?.preferredDevice = it }
    }

    private fun setupEffects(record: AudioRecord, track: AudioTrack) {
        // Input-side effects, tied to the AudioRecord's session.
        if (NoiseSuppressor.isAvailable()) {
            noiseSuppressor = NoiseSuppressor.create(record.audioSessionId)
        }
        if (AutomaticGainControl.isAvailable()) {
            automaticGainControl = AutomaticGainControl.create(record.audioSessionId)
        }
        if (AcousticEchoCanceler.isAvailable()) {
            echoCanceler = AcousticEchoCanceler.create(record.audioSessionId)
        }

        // Output-side effects, tied to the AudioTrack's session.
        try {
            bassBoost = BassBoost(0, track.audioSessionId).apply { enabled = false }
            equalizer = Equalizer(0, track.audioSessionId).apply { enabled = false }
            loudnessEnhancer = LoudnessEnhancer(track.audioSessionId).apply { enabled = false }
        } catch (e: Exception) {
            Log.w(TAG, "Some output effects unavailable on this device", e)
        }
    }

    private fun applyEffectToggle(s: EffectSettings) {
        noiseSuppressor?.enabled = s.noiseSuppression
        automaticGainControl?.enabled = s.agc

        bassBoost?.let {
            it.enabled = s.bassBoostStrength > 0
            if (s.bassBoostStrength > 0) it.setStrength(s.bassBoostStrength)
        }
        loudnessEnhancer?.let {
            it.enabled = s.loudnessGainMillibel > 0
            it.setTargetGain(s.loudnessGainMillibel.toInt())
        }
    }

    private fun captureLoop(bufferSizeBytes: Int) {
        val shortBuffer = ShortArray(bufferSizeBytes / 2)
        val record = audioRecord ?: return
        val track = audioTrack ?: return
        val nativeAvailable = NativeGain.isAvailable()

        while (running.get()) {
            val samplesRead = record.read(shortBuffer, 0, shortBuffer.size)
            if (samplesRead <= 0) continue

            val gain = settings.get().manualGain
            if (gain != 1.0f) {
                if (nativeAvailable) {
                    // Native path also applies a soft-knee limiter above ~90%
                    // of full scale, so pushing gain up to 8x gets louder
                    // without turning into harsh square-wave clipping.
                    NativeGain.applyGainWithLimiter(shortBuffer, samplesRead, gain)
                } else {
                    applyGainKotlin(shortBuffer, samplesRead, gain)
                }
            }

            track.write(shortBuffer, 0, samplesRead, AudioTrack.WRITE_BLOCKING)
        }
    }

    /** Pure-Kotlin fallback (hard clip) if the native library failed to load. */
    private fun applyGainKotlin(buffer: ShortArray, length: Int, gain: Float) {
        for (i in 0 until length) {
            val amplified = (buffer[i] * gain).toInt()
            buffer[i] = amplified.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
    }

    fun stop() {
        running.set(false)
        captureThread?.join(500)
        captureThread = null

        runCatching { audioRecord?.stop() }
        runCatching { audioTrack?.stop() }

        noiseSuppressor?.release(); noiseSuppressor = null
        automaticGainControl?.release(); automaticGainControl = null
        echoCanceler?.release(); echoCanceler = null
        bassBoost?.release(); bassBoost = null
        equalizer?.release(); equalizer = null
        loudnessEnhancer?.release(); loudnessEnhancer = null

        audioRecord?.release(); audioRecord = null
        audioTrack?.release(); audioTrack = null
    }

    fun isRunning(): Boolean = running.get()
}
