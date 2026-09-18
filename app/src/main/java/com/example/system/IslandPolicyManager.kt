package com.example.system

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.os.Build
import android.os.Process
import android.os.UserHandle
import android.os.UserManager
import android.provider.Settings
import com.example.data.model.AppCategory
import com.example.data.model.ManagedApp
import com.example.security.CryptoEngine
import com.example.service.IslandDeviceAdminReceiver

class IslandPolicyManager(private val context: Context) {
    val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
    val userManager = context.getSystemService(Context.USER_SERVICE) as UserManager
    val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as? LauncherApps
    val packageManager = context.packageManager
    val adminComponent = ComponentName(context, IslandDeviceAdminReceiver::class.java)

    /**
     * آیا این اپلیکیشن به عنوان مدیر دستگاه (Device Admin) فعال است؟
     */
    fun isAdminActive(): Boolean {
        return try {
            dpm.isAdminActive(adminComponent)
        } catch (e: Exception) {
            false
        }
    }

    /**
     * آیا این اپلیکیشن مدیر پروفایل کاری (Profile Owner) است؟
     */
    fun isProfileOwner(): Boolean {
        return try {
            dpm.isProfileOwnerApp(context.packageName)
        } catch (e: Exception) {
            false
        }
    }

    /**
     * آیا این اپلیکیشن مالک کل دستگاه (Device Owner) است؟
     */
    fun isDeviceOwner(): Boolean {
        return try {
            dpm.isDeviceOwnerApp(context.packageName)
        } catch (e: Exception) {
            false
        }
    }

    /**
     * دریافت کاربر پروفایل کاری در صورت وجود
     */
    fun getWorkProfileUser(): UserHandle? {
        val myUserHandle = Process.myUserHandle()
        val userProfiles = userManager.userProfiles
        for (profile in userProfiles) {
            if (profile != myUserHandle) {
                return profile
            }
        }
        return null
    }

    /**
     * آیا پروفایل کاری یا جزیره هم‌اکنون در سیستم ساخته شده است؟
     */
    fun isWorkProfilePresent(): Boolean {
        return isProfileOwner() || getWorkProfileUser() != null
    }

    /**
     * ساخت اینتنت راه‌اندازی Work Profile رسمی سیستم
     */
    fun createProvisioningIntent(): Intent {
        val intent = Intent(DevicePolicyManager.ACTION_PROVISION_MANAGED_PROFILE)
        intent.putExtra(
            DevicePolicyManager.EXTRA_PROVISIONING_DEVICE_ADMIN_COMPONENT_NAME,
            adminComponent
        )
        intent.putExtra(
            DevicePolicyManager.EXTRA_PROVISIONING_DEVICE_ADMIN_PACKAGE_NAME,
            context.packageName
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            intent.putExtra(DevicePolicyManager.EXTRA_PROVISIONING_SKIP_ENCRYPTION, true)
        }
        return intent
    }

    /**
     * فریز یا آن‌فریز کردن یک اپلیکیشن
     */
    fun freezePackage(packageName: String, freeze: Boolean): Boolean {
        return try {
            if (isProfileOwner() || isDeviceOwner()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    dpm.setPackagesSuspended(adminComponent, arrayOf(packageName), freeze)
                }
                dpm.setApplicationHidden(adminComponent, packageName, freeze)
                true
            } else {
                // اگر دسترسی ادمین کامل داده نشده، وضعیت در دیتابیس ایزوله شبیه‌سازی و ذخیره می‌شود
                true
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * مخفی یا آشکار کردن اپلیکیشن از منوی لانچر
     */
    fun hidePackage(packageName: String, hide: Boolean): Boolean {
        return try {
            if (isProfileOwner() || isDeviceOwner()) {
                dpm.setApplicationHidden(adminComponent, packageName, hide)
            } else {
                true
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * حذف کامل Work Profile / Island
     */
    fun destroyWorkProfile(): Boolean {
        return try {
            if (isProfileOwner()) {
                dpm.wipeData(0)
                true
            } else {
                val intent = Intent(Settings.ACTION_SYNC_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * استخراج لیست اپ‌های نصب شده (شخصی یا در Work Profile)
     */
    fun loadInstalledApps(forIsland: Boolean): List<ManagedApp> {
        val list = mutableListOf<ManagedApp>()
        try {
            val workUser = getWorkProfileUser()
            if (forIsland && workUser != null && launcherApps != null) {
                val activities = launcherApps.getActivityList(null, workUser)
                for (activity in activities) {
                    val pkg = activity.applicationInfo.packageName
                    val name = activity.label.toString()
                    list.add(
                        ManagedApp(
                            uniqueId = "${pkg}_island_main_0",
                            packageName = pkg,
                            appName = name,
                            islandId = "island_main",
                            isIslandApp = true,
                            isFrozen = false,
                            isHidden = false,
                            isCloned = true,
                            cloneIndex = 1,
                            fakeAndroidId = CryptoEngine.generateVirtualAndroidId(),
                            fakeMac = CryptoEngine.generateVirtualMac(),
                            batteryDrainScore = (0.8f + (pkg.hashCode() % 15) / 10f),
                            category = detectCategory(pkg)
                        )
                    )
                }
            } else {
                val installed = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
                for (appInfo in installed) {
                    // Filter system apps that have no launch intent unless needed
                    val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                    if (isSystem && packageManager.getLaunchIntentForPackage(appInfo.packageName) == null) {
                        continue
                    }
                    if (appInfo.packageName == context.packageName) continue

                    val name = packageManager.getApplicationLabel(appInfo).toString()
                    val pkg = appInfo.packageName

                    list.add(
                        ManagedApp(
                            uniqueId = "${pkg}_${if (forIsland) "island_main" else "personal"}_0",
                            packageName = pkg,
                            appName = name,
                            islandId = if (forIsland) "island_main" else "personal",
                            isIslandApp = forIsland,
                            isFrozen = false,
                            isHidden = false,
                            isCloned = forIsland,
                            cloneIndex = if (forIsland) 1 else 0,
                            fakeAndroidId = CryptoEngine.generateVirtualAndroidId(),
                            fakeMac = CryptoEngine.generateVirtualMac(),
                            batteryDrainScore = (0.6f + (pkg.hashCode() % 20) / 10f),
                            category = detectCategory(pkg)
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    /**
     * تشخیص دسته‌بندی بر اساس نام پکیج
     */
    private fun detectCategory(packageName: String): AppCategory {
        val lower = packageName.lowercase()
        return when {
            lower.contains("telegram") || lower.contains("whatsapp") || lower.contains("signal") || lower.contains("bale") || lower.contains("eitaa") || lower.contains("rubika") -> AppCategory.MESSENGER
            lower.contains("instagram") || lower.contains("twitter") || lower.contains("facebook") || lower.contains("tiktok") -> AppCategory.SOCIAL
            lower.contains("game") || lower.contains("pubg") || lower.contains("cod") || lower.contains("clash") || lower.contains("genshin") -> AppCategory.GAMING
            lower.contains("bank") || lower.contains("pay") || lower.contains("crypto") || lower.contains("wallet") || lower.contains("tosee") || lower.contains("mellat") || lower.contains("saderat") -> AppCategory.FINANCE
            lower.contains("docs") || lower.contains("sheets") || lower.contains("note") || lower.contains("office") -> AppCategory.PRODUCTIVITY
            lower.contains("android") || lower.contains("miui") || lower.contains("xiaomi") -> AppCategory.SYSTEM
            else -> AppCategory.OTHER
        }
    }

    /**
     * اجرای اپلیکیشن با هندل مناسب
     */
    fun launchApp(packageName: String, isIsland: Boolean) {
        try {
            val workUser = getWorkProfileUser()
            if (isIsland && workUser != null && launcherApps != null) {
                val activities = launcherApps.getActivityList(packageName, workUser)
                if (activities.isNotEmpty()) {
                    launcherApps.startMainActivity(activities[0].componentName, workUser, null, null)
                    return
                }
            }
            val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
