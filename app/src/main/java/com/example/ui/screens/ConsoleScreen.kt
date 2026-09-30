package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Dangerous
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.BotStatus
import com.example.model.LogLevel
import com.example.ui.components.MetricBadge
import com.example.ui.components.StatusLedIndicator
import com.example.ui.theme.DarkOutline
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.LogError
import com.example.ui.theme.LogHighlight
import com.example.ui.theme.LogInfo
import com.example.ui.theme.LogSuccess
import com.example.ui.theme.LogVerbose
import com.example.ui.theme.LogWarning
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonRed
import com.example.ui.theme.TerminalBg
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import com.example.viewmodel.BotViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ConsoleScreen(
    viewModel: BotViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val status by viewModel.botStatus.collectAsState()
    val logs by viewModel.logs.collectAsState()
    val uptime by viewModel.uptimeSeconds.collectAsState()
    val msgCount by viewModel.messageCount.collectAsState()
    val backoff by viewModel.currentBackoff.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var autoScroll by remember { mutableStateOf(true) }
    var terminalInput by remember { mutableStateOf("") }

    val listState = rememberLazyListState()

    val filteredLogs = remember(logs, searchQuery) {
        if (searchQuery.isBlank()) logs
        else logs.filter { it.message.contains(searchQuery, ignoreCase = true) || it.tag.contains(searchQuery, ignoreCase = true) }
    }

    LaunchedEffect(filteredLogs.size, autoScroll) {
        if (autoScroll && filteredLogs.isNotEmpty()) {
            listState.animateScrollToItem(filteredLogs.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        // Status Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
            shape = RoundedCornerShape(14.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkOutline)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        StatusLedIndicator(status = status)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            val statusLabel = when (status) {
                                BotStatus.RUNNING -> "در حال اجرای ۲۴/۷ (آنلاین)"
                                BotStatus.STARTING -> "در حال راه‌اندازی ربات…"
                                BotStatus.RESTARTING -> "اتصال مجدد (Backoff: ${backoff}s)"
                                BotStatus.ERROR -> "خطا در اجرا"
                                BotStatus.STOPPED -> "ربات متوقف است"
                            }
                            val statusColor = when (status) {
                                BotStatus.RUNNING -> NeonGreen
                                BotStatus.STARTING, BotStatus.RESTARTING -> NeonAmber
                                BotStatus.ERROR -> NeonRed
                                BotStatus.STOPPED -> TextSecondary
                            }
                            Text(
                                text = statusLabel,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = statusColor
                            )
                            Text(
                                text = "فایل اجرایی: ${viewModel.prefs.mainScriptName}",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextTertiary
                            )
                        }
                    }

                    // Force test crash button to prove auto-recovery
                    if (status == BotStatus.RUNNING) {
                        IconButton(
                            onClick = { viewModel.triggerCrashForTesting() },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Dangerous,
                                contentDescription = "تست کرش خودکار",
                                tint = NeonRed
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Metric Badges Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val uptimeFormatted = "%02d:%02d:%02d".format(uptime / 3600, (uptime % 3600) / 60, uptime % 60)
                    MetricBadge(label = "آپتایم:", value = uptimeFormatted, color = NeonGreen)
                    MetricBadge(label = "پیام‌ها:", value = msgCount.toString(), color = NeonCyan)
                    MetricBadge(label = "PID:", value = viewModel.engine.pid.toString(), color = TextSecondary)
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Control Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (status != BotStatus.RUNNING) {
                        Button(
                            onClick = { viewModel.startBot() },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = Color(0xFF00391A))
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("شروع ۲۴/۷", fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Button(
                            onClick = { viewModel.stopBot() },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = NeonRed, contentColor = Color.White)
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("توقف", fontWeight = FontWeight.Bold)
                        }
                    }

                    OutlinedButton(
                        onClick = { viewModel.restartBot() },
                        modifier = Modifier.weight(1f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonCyan)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("ریاستارت")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Terminal Console Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            colors = CardDefaults.cardColors(containerColor = TerminalBg),
            shape = RoundedCornerShape(14.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkOutline)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Terminal Header Toolbar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkSurfaceVariant)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "terminal@highrise:~#",
                            style = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold),
                            color = NeonGreen
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("اسکرول", style = MaterialTheme.typography.labelSmall, color = TextTertiary)
                        Switch(
                            checked = autoScroll,
                            onCheckedChange = { autoScroll = it },
                            modifier = Modifier.padding(horizontal = 4.dp),
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = NeonGreen,
                                checkedTrackColor = Color(0xFF005327)
                            )
                        )

                        IconButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Highrise Bot Logs", viewModel.getFormattedLogs())
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "لاگ‌ها در کلیپ‌بورد کپی شدند", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "کپی لاگ", tint = TextSecondary, modifier = Modifier.size(16.dp))
                        }

                        IconButton(
                            onClick = { viewModel.clearLogs() },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.CleaningServices, contentDescription = "پاک‌سازی", tint = TextSecondary, modifier = Modifier.size(16.dp))
                        }
                    }
                }

                // Search Filter Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("فیلتر لاگ‌ها (مثال: error, tip, chat)...", style = MaterialTheme.typography.bodySmall, color = TextTertiary) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonGreen,
                        unfocusedBorderColor = DarkOutline,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedContainerColor = Color(0xFF070A10),
                        unfocusedContainerColor = Color(0xFF070A10)
                    )
                )

                // Log Entries (Always LTR Monospace for readability)
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    val sdf = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (filteredLogs.isEmpty()) {
                            item {
                                Text(
                                    text = "// منتظر شروع ربات یا دریافت رویداد...",
                                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                    color = TextTertiary,
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                        }

                        items(filteredLogs, key = { it.id }) { log ->
                            val color = when (log.level) {
                                LogLevel.INFO -> LogInfo
                                LogLevel.SUCCESS -> LogSuccess
                                LogLevel.WARN -> LogWarning
                                LogLevel.ERROR -> LogError
                                LogLevel.BOT -> LogHighlight
                                LogLevel.SYS -> LogVerbose
                            }
                            val timeStr = sdf.format(Date(log.timestamp))

                            Row(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = "[$timeStr] ",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp
                                    ),
                                    color = TextTertiary
                                )
                                Text(
                                    text = log.message,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 12.sp,
                                        fontWeight = if (log.level == LogLevel.ERROR || log.level == LogLevel.SUCCESS) FontWeight.Bold else FontWeight.Normal
                                    ),
                                    color = color
                                )
                            }
                        }
                    }
                }

                // Quick Command Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val quickCmds = listOf("!help", "!wallet", "!status", "!emote dance-wave", "!tip 5", "!radio", "!ping")
                    quickCmds.forEach { cmd ->
                        FilterChip(
                            selected = false,
                            onClick = { viewModel.sendTerminalCommand(cmd) },
                            label = { Text(cmd, style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace)) },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = DarkSurfaceElevated,
                                labelColor = NeonCyan
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = false,
                                borderColor = DarkOutline
                            )
                        )
                    }
                }

                // Interactive Command Sender
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = terminalInput,
                        onValueChange = { terminalInput = it },
                        placeholder = { Text("ارسال مستقیم دستور به ربات (مثال: !wallet)...", style = MaterialTheme.typography.bodySmall, color = TextTertiary) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = DarkOutline,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedContainerColor = DarkSurfaceElevated,
                            unfocusedContainerColor = DarkSurfaceElevated
                        )
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    IconButton(
                        onClick = {
                            if (terminalInput.isNotBlank()) {
                                viewModel.sendTerminalCommand(terminalInput)
                                terminalInput = ""
                            }
                        },
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(NeonCyan)
                            .size(48.dp)
                    ) {
                        Icon(Icons.Default.Send, contentDescription = "ارسال", tint = Color(0xFF00363D))
                    }
                }
            }
        }
    }
}
