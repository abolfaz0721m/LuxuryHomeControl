package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.model.BotStatus
import com.example.ui.components.StatusLedIndicator
import com.example.ui.screens.ConsoleScreen
import com.example.ui.screens.FilesScreen
import com.example.ui.screens.PackagesScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.DarkOutline
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonRed
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import com.example.viewmodel.BotViewModel

enum class BotNavTab(val title: String, val icon: ImageVector) {
    CONSOLE("کنسول و لاگ", Icons.Default.Terminal),
    FILES("فایل‌ها", Icons.Default.Description),
    PACKAGES("پکیج‌ها (pip)", Icons.Default.Extension),
    SETTINGS("تنظیمات", Icons.Default.Settings)
}

class MainActivity : ComponentActivity() {

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                val viewModel: BotViewModel = viewModel()
                var currentTab by remember { mutableStateOf(BotNavTab.CONSOLE) }
                val botStatus by viewModel.botStatus.collectAsState()

                // Permission launcher for notifications on Android 13+
                val notifPermissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { /* Handle permission response gracefully */ }

                LaunchedEffect(Unit) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                            notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                }

                // Handle back press
                BackHandler(enabled = currentTab != BotNavTab.CONSOLE) {
                    currentTab = BotNavTab.CONSOLE
                }

                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        containerColor = DarkSurface,
                        topBar = {
                            CenterAlignedTopAppBar(
                                title = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        StatusLedIndicator(status = botStatus)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Highrise 24/7 Runner",
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = TextPrimary
                                        )
                                    }
                                },
                                actions = {
                                    val statusBadgeText = when (botStatus) {
                                        BotStatus.RUNNING -> "۲۴/۷ فعال"
                                        BotStatus.STARTING -> "راه‌اندازی"
                                        BotStatus.RESTARTING -> "اتصال مجدد"
                                        BotStatus.ERROR -> "خطا"
                                        BotStatus.STOPPED -> "متوقف"
                                    }
                                    val statusColor = when (botStatus) {
                                        BotStatus.RUNNING -> NeonGreen
                                        BotStatus.STARTING, BotStatus.RESTARTING -> NeonAmber
                                        BotStatus.ERROR -> NeonRed
                                        BotStatus.STOPPED -> TextSecondary
                                    }

                                    Box(
                                        modifier = Modifier
                                            .padding(end = 12.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(statusColor.copy(alpha = 0.15f))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = statusBadgeText,
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                                            color = statusColor
                                        )
                                    }
                                },
                                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                                    containerColor = DarkSurfaceElevated,
                                    titleContentColor = TextPrimary
                                )
                            )
                        },
                        bottomBar = {
                            NavigationBar(
                                containerColor = DarkSurfaceElevated,
                                tonalElevation = 4.dp
                            ) {
                                BotNavTab.values().forEach { tab ->
                                    val isSelected = currentTab == tab
                                    NavigationBarItem(
                                        selected = isSelected,
                                        onClick = { currentTab = tab },
                                        icon = {
                                            Icon(
                                                imageVector = tab.icon,
                                                contentDescription = tab.title
                                            )
                                        },
                                        label = {
                                            Text(
                                                text = tab.title,
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                    fontSize = 11.sp
                                                )
                                            )
                                        },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = NeonGreen,
                                            selectedTextColor = NeonGreen,
                                            unselectedIconColor = TextSecondary,
                                            unselectedTextColor = TextTertiary,
                                            indicatorColor = Color(0xFF00391A)
                                        )
                                    )
                                }
                            }
                        }
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            when (currentTab) {
                                BotNavTab.CONSOLE -> ConsoleScreen(viewModel = viewModel)
                                BotNavTab.FILES -> FilesScreen(viewModel = viewModel)
                                BotNavTab.PACKAGES -> PackagesScreen(viewModel = viewModel)
                                BotNavTab.SETTINGS -> SettingsScreen(viewModel = viewModel)
                            }
                        }
                    }
                }
            }
        }
    }
}
