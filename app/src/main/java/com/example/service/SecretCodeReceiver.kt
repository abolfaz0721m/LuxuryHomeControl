package com.example.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.example.MainActivity

/**
 * دریافت‌کننده کد مخفی دایلر تلفن (*#*#1337#*#*) برای ورود پنهانی به Island Pro Max
 */
class SecretCodeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if ("android.provider.Telephony.SECRET_CODE" == intent.action) {
            Toast.makeText(context, "Island Pro Max: بازگشایی حالت پنهان", Toast.LENGTH_SHORT).show()
            val launchIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("EXTRA_UNLOCKED_VIA_SECRET_CODE", true)
            }
            context.startActivity(launchIntent)
        }
    }
}
