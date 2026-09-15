package com.example.wirelessmic

import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Binder
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class StreamUiState(
    val isBluetoothConnected: Boolean = false,
    val connectedDeviceName: String? = null,
    val isStreaming: Boolean = false,
    val errorMessage: String? = null
)

/**
 * Foreground service that owns the lifetime of the audio pipeline. Bluetooth
 * output is auto-detected (see BluetoothRouteHelper) rather than manually
 * "connected" from this app — as soon as a Bluetooth headset/speaker is on
 * and paired in system settings, this service picks it up automatically and
 * keeps the mic always on the phone's own microphone.
 */
class AudioStreamService : Service() {

    inner class LocalBinder : Binder() {
        fun getService(): AudioStreamService = this@AudioStreamService
    }

    private val binder = LocalBinder()

    private lateinit var audioManager: AudioManager
    private lateinit var audioEngine: AudioEngine
    private lateinit var bluetoothRoute: BluetoothRouteHelper

    private val _uiState = MutableStateFlow(StreamUiState())
    val uiState: StateFlow<StreamUiState> = _uiState.asStateFlow()

    override fun onCreate() {
        super.onCreate()
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioEngine = AudioEngine(audioManager).apply {
            onError = { msg -> _uiState.value = _uiState.value.copy(errorMessage = msg, isStreaming = false) }
        }
        bluetoothRoute = BluetoothRouteHelper(this, audioManager)

        bluetoothRoute.startWatchingDevices { onBluetoothDevicesChanged() }
        refreshBluetoothState()
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, buildNotification())
        return START_STICKY
    }

    /** Re-checks which Bluetooth output device (if any) is currently connected. */
    fun refreshBluetoothState() {
        val connected = bluetoothRoute.isBluetoothOutputConnected()
        _uiState.value = _uiState.value.copy(
            isBluetoothConnected = connected,
            connectedDeviceName = bluetoothRoute.connectedDeviceName()
        )
    }

    private fun onBluetoothDevicesChanged() {
        refreshBluetoothState()
        // If we're already streaming and the Bluetooth device changed
        // (swapped headset, speaker turned on, etc.), re-point the live
        // AudioTrack at the new device instead of dropping the stream.
        if (_uiState.value.isStreaming) {
            audioEngine.switchOutputDevice(bluetoothRoute.findBluetoothOutputDevice())
        }
    }

    fun startStreaming(): Boolean {
        val outputDevice = bluetoothRoute.findBluetoothOutputDevice()
        if (outputDevice == null) {
            _uiState.value = _uiState.value.copy(errorMessage = getString(R.string.error_no_bt_device))
            return false
        }
        val micDevice = bluetoothRoute.findBuiltInMicDevice()
        val started = audioEngine.start(outputDevice, micDevice)
        _uiState.value = _uiState.value.copy(isStreaming = started)
        return started
    }

    fun stopStreaming() {
        audioEngine.stop()
        _uiState.value = _uiState.value.copy(isStreaming = false)
    }

    fun updateEffectSettings(settings: AudioEngine.EffectSettings) {
        audioEngine.updateSettings(settings)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, WirelessMicApp.NOTIFICATION_CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_text))
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        audioEngine.stop()
        bluetoothRoute.stopWatchingDevices()
        super.onDestroy()
    }

    companion object {
        private const val NOTIFICATION_ID = 1001
    }
}
