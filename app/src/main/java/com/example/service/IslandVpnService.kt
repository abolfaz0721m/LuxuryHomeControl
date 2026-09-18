package com.example.service

import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor

/**
 * سرویس VPN اختصاصی برای هر اپلیکیشن (Per-App Isolated VPN)
 */
class IslandVpnService : VpnService() {

    private var vpnInterface: ParcelFileDescriptor? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == ACTION_DISCONNECT) {
            disconnect()
            return START_NOT_STICKY
        }

        val targetPackages = intent?.getStringArrayListExtra(EXTRA_PACKAGES) ?: arrayListOf()
        connect(targetPackages)
        return START_STICKY
    }

    private fun connect(targetPackages: List<String>) {
        if (vpnInterface != null) return

        try {
            val builder = Builder()
                .setSession("Island Pro Max Isolated Sandbox")
                .addAddress("10.8.0.2", 32)
                .addDnsServer("1.1.1.1")
                .addRoute("0.0.0.0", 0)

            for (pkg in targetPackages) {
                try {
                    builder.addAllowedApplication(pkg)
                } catch (e: Exception) {
                    // Package not installed or invalid
                }
            }

            vpnInterface = builder.establish()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun disconnect() {
        try {
            vpnInterface?.close()
            vpnInterface = null
            stopSelf()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        disconnect()
        super.onDestroy()
    }

    companion object {
        const val ACTION_CONNECT = "com.example.vpn.CONNECT"
        const val ACTION_DISCONNECT = "com.example.vpn.DISCONNECT"
        const val EXTRA_PACKAGES = "com.example.vpn.EXTRA_PACKAGES"
    }
}
