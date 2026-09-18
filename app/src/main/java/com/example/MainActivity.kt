package com.example

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import com.example.ui.MainViewModel
import com.example.ui.components.TopIslandBar
import com.example.ui.screens.IslandHomeScreen
import com.example.ui.screens.MultiSpaceScreen
import com.example.ui.screens.PersonalAppsScreen
import com.example.ui.screens.SecurityScreen
import com.example.ui.screens.ToolsMiuiScreen
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                // RTL layout support for Persian language
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    IslandApp(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun IslandApp(viewModel: MainViewModel) {
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedTab by remember { mutableIntStateOf(0) }

    val islandApps by viewModel.islandApps.collectAsState()
    val personalApps by viewModel.personalApps.collectAsState()
    val profiles by viewModel.profiles.collectAsState()
    val securityLogs by viewModel.securityLogs.collectAsState()
    val aiInsights by viewModel.aiInsights.collectAsState()
    val chatHistory by viewModel.chatHistory.collectAsState()

    val isPanicActive by viewModel.isPanicTriggered.collectAsState()
    val isStealthMode by viewModel.isStealthMode.collectAsState()
    val isGameBooster by viewModel.isGameBooster.collectAsState()
    val isVpnActive by viewModel.isVpnActive.collectAsState()
    val activeIslandId by viewModel.activeIslandId.collectAsState()
    val userMessage by viewModel.userMessage.collectAsState()

    val isWorkProfileActive = viewModel.policyManager.isWorkProfilePresent()

    // Activity launcher for Work Profile provisioning
    val provisioningLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.showToast("پروفایل کاری با موفقیت پیکربندی شد")
        } else {
            viewModel.showToast("فرآیند ساخت جزیره لغو شد یا دسترسی داده نشد")
        }
    }

    LaunchedEffect(userMessage) {
        userMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearUserMessage()
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("island_main_scaffold"),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopIslandBar(
                isWorkProfileActive = isWorkProfileActive,
                isPanicActive = isPanicActive,
                isVpnActive = isVpnActive,
                onPanicClick = {
                    if (isPanicActive) viewModel.restoreFromPanic() else viewModel.triggerPanic()
                }
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier.testTag("island_bottom_nav")
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Shield, contentDescription = "Island") },
                    label = { Text("جزیره") },
                    colors = NavigationBarItemDefaults.colors(indicatorColor = CyberCyan.copy(alpha = 0.2f)),
                    modifier = Modifier.testTag("tab_island")
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.Apps, contentDescription = "Personal") },
                    label = { Text("شخصی") },
                    colors = NavigationBarItemDefaults.colors(indicatorColor = CyberCyan.copy(alpha = 0.2f)),
                    modifier = Modifier.testTag("tab_personal")
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.Dashboard, contentDescription = "Spaces") },
                    label = { Text("فضاها") },
                    colors = NavigationBarItemDefaults.colors(indicatorColor = CyberCyan.copy(alpha = 0.2f)),
                    modifier = Modifier.testTag("tab_spaces")
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = { Icon(Icons.Default.Security, contentDescription = "Security") },
                    label = { Text("امنیت") },
                    colors = NavigationBarItemDefaults.colors(indicatorColor = CyberCyan.copy(alpha = 0.2f)),
                    modifier = Modifier.testTag("tab_security")
                )
                NavigationBarItem(
                    selected = selectedTab == 4,
                    onClick = { selectedTab = 4 },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Tools") },
                    label = { Text("ابزارها") },
                    colors = NavigationBarItemDefaults.colors(indicatorColor = CyberCyan.copy(alpha = 0.2f)),
                    modifier = Modifier.testTag("tab_tools")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                0 -> IslandHomeScreen(
                    isWorkProfileActive = isWorkProfileActive,
                    islandApps = islandApps,
                    aiInsights = aiInsights,
                    isVpnActive = isVpnActive,
                    onCreateWorkProfile = {
                        try {
                            val intent = viewModel.policyManager.createProvisioningIntent()
                            provisioningLauncher.launch(intent)
                        } catch (e: Exception) {
                            viewModel.showToast("خطا در ایجاد Work Profile: ${e.message}")
                        }
                    },
                    onDestroyIsland = { viewModel.destroyIsland() },
                    onFreezeAll = { viewModel.freezeAllIslandApps() },
                    onUnfreezeAll = { viewModel.unfreezeAllIslandApps() },
                    onToggleVpn = { viewModel.toggleVpn(!isVpnActive) },
                    onToggleFreeze = { viewModel.toggleAppFreeze(it) },
                    onLaunchApp = { viewModel.policyManager.launchApp(it.packageName, isIsland = true) }
                )
                1 -> PersonalAppsScreen(
                    personalApps = personalApps,
                    onCloneApp = { viewModel.cloneAppToIsland(it) },
                    onLaunchApp = { viewModel.policyManager.launchApp(it.packageName, isIsland = false) }
                )
                2 -> MultiSpaceScreen(
                    profiles = profiles,
                    activeIslandId = activeIslandId,
                    isGameBoosterActive = isGameBooster,
                    onSelectSpace = { viewModel.switchActiveIsland(it) },
                    onToggleGameBooster = { viewModel.toggleGameBooster(it) }
                )
                3 -> SecurityScreen(
                    isPanicActive = isPanicActive,
                    isStealthMode = isStealthMode,
                    isRooted = viewModel.isRooted,
                    securityLogs = securityLogs,
                    onTriggerPanic = { viewModel.triggerPanic() },
                    onRestorePanic = { viewModel.restoreFromPanic() },
                    onToggleStealth = { viewModel.toggleStealth(it) }
                )
                4 -> ToolsMiuiScreen(
                    miuiSummary = viewModel.miuiSummary,
                    isMiuiOptimizationOn = viewModel.isMiuiOptimizationActive,
                    isIgnoringBattery = viewModel.isIgnoringBatteryOptimizations,
                    chatHistory = chatHistory,
                    onSendMessage = { viewModel.sendChatMessage(it) },
                    onExportBackup = {
                        val backup = viewModel.createEncryptedBackup()
                        viewModel.showToast("بکاپ رمزنگاری‌شده با طول ${backup.length} کاراکتر ایجاد شد.")
                    }
                )
            }
        }
    }
}

/**
 * Kept for test compatibility (e.g. GreetingScreenshotTest)
 */
@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(text = "Hello $name!", modifier = modifier)
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    MyApplicationTheme { Greeting("Android") }
}
