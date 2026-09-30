package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Power
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DarkOutline
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonRed
import com.example.ui.theme.TerminalBg
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import com.example.viewmodel.BotViewModel

@Composable
fun SettingsScreen(
    viewModel: BotViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val bootAutoStart by viewModel.bootAutoStart.collectAsState()
    val autoRestartOnCrash by viewModel.autoRestartOnCrash.collectAsState()
    val wakeLockEnabled by viewModel.wakeLockEnabled.collectAsState()
    val maskTokens by viewModel.maskTokensInLogs.collectAsState()

    val apiToken by viewModel.apiToken.collectAsState()
    val roomId by viewModel.roomId.collectAsState()
    val botPrefix by viewModel.botPrefix.collectAsState()
    val ownerId by viewModel.ownerId.collectAsState()

    var showToken by remember { mutableStateOf(false) }

    // Check battery optimization status
    val powerManager = remember { context.getSystemService(Context.POWER_SERVICE) as? PowerManager }
    val isIgnoringBatteryOptimizations = remember(context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && powerManager != null) {
            powerManager.isIgnoringBatteryOptimizations(context.packageName)
        } else true
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Section 1: 24/7 Stability & Service Settings
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkOutline)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Power, contentDescription = null, tint = NeonGreen, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "تنظیمات پایداری و اجرای ۲۴/۷",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = NeonGreen
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Boot Start Toggle
                    SettingToggleRow(
                        title = "اجرای خودکار پس از روشن شدن گوشی (Boot)",
                        description = "با فعال بودن این گزینه، ربات پس از ریاستارت یا روشن شدن گوشی به صورت خودکار در پس‌زمینه اجرا می‌شود.",
                        checked = bootAutoStart,
                        onCheckedChange = { viewModel.updateBootAutoStart(it) }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Crash Restart Toggle
                    SettingToggleRow(
                        title = "ریاستارت خودکار در صورت کرش (Exponential Backoff)",
                        description = "در صورت بروز خطای پیش‌بینی نشده یا قطعی اینترنت، سرویس با تاخیر فزاینده (۵، ۱۵، ۳۰، ۶۰ ثانیه) ربات را دوباره استارت می‌زند.",
                        checked = autoRestartOnCrash,
                        onCheckedChange = { viewModel.updateAutoRestartOnCrash(it) }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // WakeLock Toggle
                    SettingToggleRow(
                        title = "قفل بیدار ماندن پردازنده (Partial WakeLock)",
                        description = "جلوگیری از به خواب رفتن CPU هنگام خاموش بودن صفحه نمایش گوشی جهت اجرای بی‌وقفه ربات.",
                        checked = wakeLockEnabled,
                        onCheckedChange = { viewModel.updateWakeLockEnabled(it) }
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Battery Optimization Exemption
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isIgnoringBatteryOptimizations) Color(0xFF132B1E) else Color(0xFF2E1C0A))
                            .border(1.dp, if (isIgnoringBatteryOptimizations) NeonGreen else NeonAmber, RoundedCornerShape(10.dp))
                            .padding(12.dp)
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.BatteryAlert,
                                    contentDescription = null,
                                    tint = if (isIgnoringBatteryOptimizations) NeonGreen else NeonAmber,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isIgnoringBatteryOptimizations) "معافیت باتری فعال است (Doze غیرفعال)" else "نیازمند معافیت از بهینه‌سازی باتری",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (isIgnoringBatteryOptimizations) NeonGreen else NeonAmber
                                )
                            }
                            Text(
                                text = "سیستم‌عامل اندروید در صورت عدم معافیت، اپ‌های پس‌زمینه را در حالت Doze متوقف می‌کند.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                            if (!isIgnoringBatteryOptimizations) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Button(
                                    onClick = {
                                        try {
                                            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                                data = Uri.parse("package:${context.packageName}")
                                            }
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                                            context.startActivity(intent)
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = NeonAmber, contentColor = Color(0xFF3F2B00))
                                ) {
                                    Text("درخواست معافیت از بهینه‌سازی باتری", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section 2: OEM Task Killers Guide
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkOutline)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "راهنمای گوشی‌های شیائومی، سامسونگ و هواوی",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = NeonCyan
                        )
                    }

                    Text(
                        text = "برخی برندها (مثل MIUI/HyperOS شیائومی یا OneUI سامسونگ) فرآیندهای پس‌زمینه را به شدت متوقف می‌کنند. جهت تضمین ۲۴/۷، اپلیکیشن را در بخش Recent Apps قفل کنید (Lock) و Autostart را در تنظیمات فعال کنید.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        modifier = Modifier.padding(vertical = 6.dp)
                    )

                    OutlinedButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://dontkillmyapp.com"))
                            context.startActivity(intent)
                        },
                        border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonCyan)
                    ) {
                        Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("مشاهده راهنمای کامل Don't Kill My App")
                    }
                }
            }
        }

        // Section 3: Highrise Environment Variables (.env)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkOutline)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Key, contentDescription = null, tint = NeonAmber, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "متغیرهای محیطی و کلیدهای امنیتی (.env)",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = NeonAmber
                        )
                    }

                    Text(
                        text = "اطلاعات حساس روم و توکن API به صورت امن در حافظه رمزنگاری‌شده اپلیکیشن نگهداری می‌شوند.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // API Token Field
                    OutlinedTextField(
                        value = apiToken,
                        onValueChange = { viewModel.updateApiToken(it) },
                        label = { Text("Highrise API Token") },
                        placeholder = { Text("hr_token_...") },
                        visualTransformation = if (showToken) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showToken = !showToken }) {
                                Icon(
                                    imageVector = if (showToken) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = null,
                                    tint = TextSecondary
                                )
                            }
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonGreen,
                            unfocusedBorderColor = DarkOutline,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedContainerColor = TerminalBg,
                            unfocusedContainerColor = TerminalBg
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Room ID Field
                    OutlinedTextField(
                        value = roomId,
                        onValueChange = { viewModel.updateRoomId(it) },
                        label = { Text("Room ID (شناسه روم)") },
                        placeholder = { Text("مثال: 65df89bc213ef9081a2e4c3b") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonGreen,
                            unfocusedBorderColor = DarkOutline,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedContainerColor = TerminalBg,
                            unfocusedContainerColor = TerminalBg
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Prefix
                        OutlinedTextField(
                            value = botPrefix,
                            onValueChange = { viewModel.updateBotPrefix(it) },
                            label = { Text("پیشوند دستورات (Prefix)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonGreen,
                                unfocusedBorderColor = DarkOutline,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedContainerColor = TerminalBg,
                                unfocusedContainerColor = TerminalBg
                            )
                        )

                        // Owner ID
                        OutlinedTextField(
                            value = ownerId,
                            onValueChange = { viewModel.updateOwnerId(it) },
                            label = { Text("Owner ID (سازنده)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonGreen,
                                unfocusedBorderColor = DarkOutline,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedContainerColor = TerminalBg,
                                unfocusedContainerColor = TerminalBg
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Mask Tokens in Logs Toggle
                    SettingToggleRow(
                        title = "ماسک کردن خودکار توکن در لاگ‌ها",
                        description = "جلوگیری از چاپ شدن توکن محرمانه هایرایز در کنسول عمومی یا هنگام اشتراک‌گذاری لاگ‌ها.",
                        checked = maskTokens,
                        onCheckedChange = { viewModel.updateMaskTokens(it) }
                    )
                }
            }
        }

        // Section 4: Notes on Radio & FFMPEG
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkOutline)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Radio, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "یادداشت درباره استریم رادیو و ffmpeg",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = NeonCyan
                        )
                    }

                    Text(
                        text = "همانطور که در اسپک ربات ذکر شده، ماژول استریم صوتی رادیو (Shoutcast/Icecast) نیازمند باینری نیتیو ffmpeg مخصوص اندروید است. در صورت تمایل می‌توانید باینری استاتیک arm64 را از طریق فایلمنیجر وارد کرده و مسیر FFMPEG_PATH را تنظیم فرمایید.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun SettingToggleRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = TextPrimary
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = TextTertiary,
                modifier = Modifier.padding(top = 2.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = NeonGreen,
                checkedTrackColor = Color(0xFF005327)
            )
        )
    }
}
