package com.example.service

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast

/**
 * رسیور مدیریت دستگاه و Work Profile (Device Admin & Profile Owner Receiver)
 */
class IslandDeviceAdminReceiver : DeviceAdminReceiver() {

    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        Toast.makeText(context, "Island Pro Max: دسترسی مدیریت دستگاه فعال شد", Toast.LENGTH_SHORT).show()
    }

    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        Toast.makeText(context, "Island Pro Max: دسترسی مدیریت دستگاه لغو شد", Toast.LENGTH_SHORT).show()
    }

    override fun onProfileProvisioningComplete(context: Context, intent: Intent) {
        super.onProfileProvisioningComplete(context, intent)
        Toast.makeText(context, "Island Pro Max: پروفایل کاری (Work Profile) با موفقیت ساخته شد!", Toast.LENGTH_LONG).show()
    }
}
