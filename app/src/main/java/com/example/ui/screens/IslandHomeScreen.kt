package com.example.ui.screens

import android.content.Context
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ai.AiInsight
import com.example.data.model.AppCategory
import com.example.data.model.ManagedApp
import com.example.ui.components.AppCard
import com.example.ui.theme.AlertRed
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.NeonGreen

@Composable
fun IslandHomeScreen(
    isWorkProfileActive: Boolean,
    islandApps: List<ManagedApp>,
    aiInsights: List<AiInsight>,
    isVpnActive: Boolean,
    onCreateWorkProfile: () -> Unit,
    onDestroyIsland: () -> Unit,
    onFreezeAll: () -> Unit,
    onUnfreezeAll: () -> Unit,
    onToggleVpn: () -> Unit,
    onToggleFreeze: (ManagedApp) -> Unit,
    onLaunchApp: (ManagedApp) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<AppCategory?>(null) }

    val filteredApps = islandApps.filter { app ->
        val matchesSearch = app.appName.contains(searchQuery, ignoreCase = true) ||
                app.packageName.contains(searchQuery, ignoreCase = true)
        val matchesCategory = selectedCategory == null || app.category == selectedCategory
        matchesSearch && matchesCategory
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("island_home_list"),
        contentPadding = PaddingValues(top = 12.dp, bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // --- 1. Work Profile Provisioning / Management Card ---
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("card_work_profile_status"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isWorkProfileActive) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    }
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isWorkProfileActive) Icons.Default.CheckCircle else Icons.Default.Shield,
                                contentDescription = null,
                                tint = if (isWorkProfileActive) NeonGreen else CyberCyan,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isWorkProfileActive) "پروفایل کاری ایزوله فعال است" else "فضای ایزوله Work Profile ساخته نشده",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (isWorkProfileActive)
                            "اپلیکیشن‌های موجود در این بخش به صورت کامل در یک کانتینر ایزوله سیستمی اجرا می‌شوند و به داده‌های شخصی شما دسترسی ندارند."
                        else
                            "با ساخت Work Profile رسمی اندروید، جزیره‌ای ایزوله برای کلون کردن نامحدود برنامه‌ها و فریز خودکار ایجاد کنید.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (!isWorkProfileActive) {
                            Button(
                                onClick = onCreateWorkProfile,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("btn_create_profile")
                            ) {
                                Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = "ساخت Work Profile", fontSize = 12.sp)
                            }
                        } else {
                            OutlinedButton(
                                onClick = onDestroyIsland,
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = AlertRed),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("btn_destroy_island")
                            ) {
                                Icon(imageVector = Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = "حذف کامل جزیره", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }

        // --- 2. Batch Actions Grid ---
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onFreezeAll,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("btn_freeze_all")
                ) {
                    Icon(imageVector = Icons.Default.AcUnit, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "فریز همه", fontSize = 11.sp, color = CyberCyan)
                }

                OutlinedButton(
                    onClick = onUnfreezeAll,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("btn_unfreeze_all")
                ) {
                    Icon(imageVector = Icons.Default.LockOpen, contentDescription = null, tint = NeonGreen, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "آن‌فریز همه", fontSize = 11.sp, color = NeonGreen)
                }

                OutlinedButton(
                    onClick = onToggleVpn,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (isVpnActive) CyberCyan.copy(alpha = 0.15f) else Color.Transparent
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("btn_toggle_vpn")
                ) {
                    Icon(imageVector = Icons.Default.VpnKey, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = if (isVpnActive) "VPN روشن" else "VPN ایزوله", fontSize = 11.sp)
                }
            }
        }

        // --- 3. AI Insights Card ---
        if (aiInsights.isNotEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = CyberCyan,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "توصیه هوش مصنوعی محلی",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = CyberCyan
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        val first = aiInsights.first()
                        Text(
                            text = first.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // --- 4. Search & Filter Header ---
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_search_island"),
                placeholder = { Text("جستجوی اپلیکیشن در جزیره...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )
        }

        // Category Filter Chips
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                item {
                    FilterChip(
                        selected = selectedCategory == null,
                        onClick = { selectedCategory = null },
                        label = { Text("همه (${islandApps.size})") }
                    )
                }
                item {
                    FilterChip(
                        selected = selectedCategory == AppCategory.MESSENGER,
                        onClick = { selectedCategory = AppCategory.MESSENGER },
                        label = { Text("پیام‌رسان") }
                    )
                }
                item {
                    FilterChip(
                        selected = selectedCategory == AppCategory.SOCIAL,
                        onClick = { selectedCategory = AppCategory.SOCIAL },
                        label = { Text("شبکه اجتماعی") }
                    )
                }
                item {
                    FilterChip(
                        selected = selectedCategory == AppCategory.GAMING,
                        onClick = { selectedCategory = AppCategory.GAMING },
                        label = { Text("بازی‌ها") }
                    )
                }
            }
        }

        // --- 5. Apps List ---
        if (filteredApps.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "اپلیکیشنی در این فیلتر یافت نشد",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "از برگه «شخصی (Main)» برنامه‌ها را به جزیره کلون کنید.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        } else {
            items(filteredApps, key = { it.uniqueId }) { app ->
                AppCard(
                    app = app,
                    onToggleFreeze = { onToggleFreeze(app) },
                    onLaunch = { onLaunchApp(app) }
                )
            }
        }
    }
}
