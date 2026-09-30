package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import com.example.model.PythonPackageItem
import com.example.ui.theme.DarkOutline
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.LogSuccess
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonRed
import com.example.ui.theme.TerminalBg
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import com.example.viewmodel.BotViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PackagesScreen(
    viewModel: BotViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val installedPackages by viewModel.installedPackages.collectAsState()
    val pipLogs by viewModel.pipLogEntries.collectAsState()
    val isInstalling by viewModel.isInstalling.collectAsState()
    val installProgress by viewModel.installProgress.collectAsState()
    val installStatusText by viewModel.installStatusText.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val scanDiscovered by viewModel.scanDiscovered.collectAsState()

    var manualPackageName by remember { mutableStateOf("") }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Section A: Auto Scan & Install Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkOutline)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = NeonAmber, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "اسکن و نصب خودکار وابستگی‌ها (Auto Pip)",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = NeonAmber
                        )
                    }

                    Text(
                        text = "اسکن تمام ایمپورت‌های کدهای پایتون و فایل requirements.txt و نصب یکجای پکیج‌های مورد نیاز.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        modifier = Modifier.padding(vertical = 6.dp)
                    )

                    Button(
                        onClick = { viewModel.scanDependencies() },
                        enabled = !isScanning && !isInstalling,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonAmber, contentColor = Color(0xFF3F2B00))
                    ) {
                        if (isScanning) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color(0xFF3F2B00), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("در حال اسکن فایل‌های پایتون…", fontWeight = FontWeight.Bold)
                        } else {
                            Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("اسکن فایل‌ها و requirements.txt", fontWeight = FontWeight.Bold)
                        }
                    }

                    if (scanDiscovered.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "پکیج‌های یافت‌شده (${scanDiscovered.size}):",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary
                        )

                        FlowRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            scanDiscovered.forEach { pkg ->
                                val isAlreadyInstalled = installedPackages.any { it.name.equals(pkg, ignoreCase = true) }
                                FilterChip(
                                    selected = isAlreadyInstalled,
                                    onClick = { },
                                    label = { Text(pkg, style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace)) },
                                    leadingIcon = {
                                        if (isAlreadyInstalled) {
                                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = NeonGreen, modifier = Modifier.size(14.dp))
                                        }
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color(0xFF132B1E),
                                        selectedLabelColor = NeonGreen,
                                        containerColor = DarkSurfaceVariant,
                                        labelColor = TextPrimary
                                    )
                                )
                            }
                        }

                        Button(
                            onClick = { viewModel.installDiscoveredPackages() },
                            enabled = !isInstalling,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = Color(0xFF00391A))
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("نصب همه پکیج‌های یافت‌شده", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Section B: Manual Pip Install Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkOutline)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "نصب دستی پکیج (Manual Pip Install)",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = NeonCyan
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = manualPackageName,
                            onValueChange = { manualPackageName = it },
                            placeholder = { Text("مثال: aiohttp==3.9.0 یا yt-dlp", style = MaterialTheme.typography.bodySmall, color = TextTertiary) },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonCyan,
                                unfocusedBorderColor = DarkOutline,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedContainerColor = TerminalBg,
                                unfocusedContainerColor = TerminalBg
                            )
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Button(
                            onClick = {
                                if (manualPackageName.isNotBlank()) {
                                    viewModel.installManualPackage(manualPackageName.trim())
                                    manualPackageName = ""
                                }
                            },
                            enabled = !isInstalling,
                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color(0xFF00363D))
                        ) {
                            Text("نصب", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Installation Progress Bar (if active)
        if (isInstalling) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF13222B)),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(installStatusText, style = MaterialTheme.typography.bodySmall, color = NeonCyan)
                            Text("${(installProgress * 100).toInt()}%", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = NeonCyan)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { installProgress },
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                            color = NeonCyan,
                            trackColor = DarkOutline
                        )
                    }
                }
            }
        }

        // Section C: Installed Packages Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "پکیج‌های نصب‌شده در ران‌تایم (${installedPackages.size})",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )
            }
        }

        // Installed Packages Items
        items(installedPackages, key = { it.name }) { pkg ->
            PackageItemCard(
                pkg = pkg,
                onUninstall = {
                    viewModel.uninstallPackage(pkg.name)
                    Toast.makeText(context, "پکیج ${pkg.name} حذف شد", Toast.LENGTH_SHORT).show()
                }
            )
        }

        // Section D: Pip Console Output
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = TerminalBg),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkOutline)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "pip@runtime:~# logs",
                            style = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold),
                            color = NeonGreen
                        )

                        IconButton(
                            onClick = { viewModel.clearPipLogs() },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.CleaningServices, contentDescription = "پاک‌سازی", tint = TextSecondary, modifier = Modifier.size(16.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF04060A))
                                .padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            if (pipLogs.isEmpty()) {
                                Text(
                                    text = "// لاگ‌های نصب pip در اینجا نمایش داده خواهند شد...",
                                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontSize = 11.sp),
                                    color = TextTertiary
                                )
                            } else {
                                pipLogs.takeLast(12).forEach { line ->
                                    val isSuccess = line.contains("Successfully", ignoreCase = true)
                                    Text(
                                        text = line,
                                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontSize = 11.sp),
                                        color = if (isSuccess) LogSuccess else TextSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PackageItemCard(
    pkg: PythonPackageItem,
    onUninstall: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkOutline)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(NeonGreen.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Extension, contentDescription = null, tint = NeonGreen, modifier = Modifier.size(18.dp))
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = pkg.name,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace),
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(DarkSurfaceVariant)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "v${pkg.version}",
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, fontSize = 10.sp),
                                color = NeonCyan
                            )
                        }
                    }

                    if (pkg.summary.isNotEmpty()) {
                        Text(
                            text = pkg.summary,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = TextTertiary,
                            maxLines = 1
                        )
                    }
                }
            }

            if (pkg.isCustom) {
                IconButton(onClick = onUninstall, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "حذف پکیج", tint = NeonRed, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}
