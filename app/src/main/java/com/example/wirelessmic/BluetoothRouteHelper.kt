package com.example.wirelessmic

import android.content.Context
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Handler
import android.os.Looper

/**
 * Detects the currently-connected Bluetooth *output* device (A2DP preferred
 * for full audio quality, SCO as a fallback for older headsets that only
 * support the telephony profile) and the phone's own built-in microphone.
 *
 * Deliberately does NOT touch AudioManager.mode / startBluetoothSco(): that
 * API forces BOTH the mic and speaker to route through the Bluetooth device
 * (like a phone call), which is the opposite of what this app needs — mic
 * always from the phone, output to whatever Bluetooth device is connected.
 *
 * Pairing/connecting the Bluetooth device itself still happens in Android's
 * own Bluetooth settings — apps cannot initiate that handshake. This class
 * only detects a connection that already exists and hands back the right
 * AudioDeviceInfo to route to.
 */
class BluetoothRouteHelper(
    private val context: Context,
    private val audioManager: AudioManager
) {

    private var deviceCallback: AudioDeviceCallback? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    /** Best available Bluetooth output: A2DP (full quality) first, SCO as a fallback. */
    fun findBluetoothOutputDevice(): AudioDeviceInfo? {
        val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        return devices.firstOrNull { it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP }
            ?: devices.firstOrNull { it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO }
    }

    /** The phone's own built-in microphone, used to pin capture away from any Bluetooth mic. */
    fun findBuiltInMicDevice(): AudioDeviceInfo? {
        val devices = audioManager.getDevices(AudioManager.GET_DEVICES_INPUTS)
        return devices.firstOrNull { it.type == AudioDeviceInfo.TYPE_BUILTIN_MIC }
    }

    fun isBluetoothOutputConnected(): Boolean = findBluetoothOutputDevice() != null

    fun connectedDeviceName(): String? {
        val device = findBluetoothOutputDevice() ?: return null
        return runCatching { device.productName?.toString() }.getOrNull()
    }

    /**
     * Registers a listener that fires whenever a Bluetooth audio device is
     * plugged/unplugged (connected/disconnected), so the UI and an active
     * stream can react automatically — no manual "connect" step needed once
     * the device is paired and turned on.
     */
    fun startWatchingDevices(onChanged: () -> Unit) {
        stopWatchingDevices()
        val callback = object : AudioDeviceCallback() {
            override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>) {
                if (addedDevices.any { isBluetoothType(it) }) onChanged()
            }
            override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>) {
                if (removedDevices.any { isBluetoothType(it) }) onChanged()
            }
        }
        deviceCallback = callback
        audioManager.registerAudioDeviceCallback(callback, mainHandler)
    }

    fun stopWatchingDevices() {
        deviceCallback?.let { audioManager.unregisterAudioDeviceCallback(it) }
        deviceCallback = null
    }

    private fun isBluetoothType(info: AudioDeviceInfo) =
        info.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP || info.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO
}
