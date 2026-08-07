package com.af.synapse

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.af.synapse.data.CpuManager
import com.af.synapse.data.GpuManager
import com.af.synapse.data.VoltageManager
import com.af.synapse.ui.screens.*
import com.af.synapse.ui.theme.SynapseTheme
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        com.af.synapse.data.SettingsStore.init(this)
        
        // Request Notification Permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        
        // Initialize Shell with Root request
        Shell.setDefaultBuilder(Shell.Builder.create()
            .setFlags(Shell.FLAG_REDIRECT_STDERR)
            .setTimeout(20)
        )

        setContent {
            var themeMode by remember { mutableIntStateOf(com.af.synapse.data.SettingsStore.getThemeMode()) }
            var accentColor by remember { mutableIntStateOf(com.af.synapse.data.SettingsStore.getAccentColor()) }
            var isRootGranted by remember { mutableStateOf(true) }
            var updateInfo by remember { mutableStateOf<com.af.synapse.data.UpdateManager.UpdateInfo?>(null) }

            // Background check for root and updates
            LaunchedEffect(Unit) {
                withContext(Dispatchers.IO) {
                    val isRoot = Shell.getShell().isRoot
                    val update = com.af.synapse.data.UpdateManager.checkForUpdates(this@MainActivity)
                    
                    withContext(Dispatchers.Main) {
                        isRootGranted = isRoot
                        if (update?.isNewer == true) {
                            updateInfo = update
                        }
                        if (!isRoot) {
                            Toast.makeText(this@MainActivity, "Root access required for most features!", Toast.LENGTH_LONG).show()
                        }
                        com.af.synapse.data.MonitorManager.startMonitoring()
                    }
                }
            }

            if (updateInfo != null) {
                AlertDialog(
                    onDismissRequest = { updateInfo = null },
                    title = { Text("Aktualizacja dostępna") },
                    text = { Text("Nowa wersja Synapse (${updateInfo?.version}) jest dostępna na GitHubie. Czy chcesz pobrać?") },
                    confirmButton = {
                        Button(onClick = { 
                            com.af.synapse.data.UpdateManager.openDownload(this@MainActivity, updateInfo?.downloadUrl ?: "")
                            updateInfo = null
                        }) {
                            Text("Pobierz APK")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { updateInfo = null }) {
                            Text("Później")
                        }
                    }
                )
            }

            SynapseTheme(themeOverride = themeMode, accentColorOverride = Color(accentColor)) {
                val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
                val coroutineScope = rememberCoroutineScope()
                
                val clusters = remember { CpuManager.getAvailableClusters() }
                val isGpuAvailable = remember { GpuManager.getGpuPath() != null }
                val isAdvancedAvailable = remember { com.af.synapse.data.AdvancedManager.isAdvancedAvailable() }
                val isVoltageAvailable = remember { VoltageManager.isVoltageAvailable() }
                
                val menuItems = remember(clusters, isGpuAvailable, isAdvancedAvailable, isVoltageAvailable, isRootGranted) {
                    val items = mutableListOf<NavigationItem>()
                    items.add(NavigationItem(R.string.nav_summary, Icons.Default.Info, 0, ScreenType.SUMMARY))
                    
                    if (isRootGranted) {
                        var index = 1
                        clusters.forEachIndexed { i, policyId ->
                            val (titleRes, descRes) = if (clusters.size == 1) {
                                R.string.summary_cpu to R.string.cpu_generic_desc
                            } else {
                                when (i) {
                                    0 -> R.string.nav_cpu_silver to R.string.cpu_silver_desc
                                    1 -> R.string.nav_cpu_gold to R.string.cpu_gold_desc
                                    2 -> R.string.nav_cpu_perf to R.string.cpu_perf_desc
                                    else -> R.string.summary_cpu to R.string.cpu_generic_desc
                                }
                            }
                            items.add(NavigationItem(titleRes, Icons.Default.Settings, index++, ScreenType.CPU_CLUSTER, policyId, descRes))
                        }
                        
                        if (isGpuAvailable) {
                            items.add(NavigationItem(R.string.nav_gpu, Icons.Default.Refresh, index++, ScreenType.GPU))
                        }

                        items.add(NavigationItem(R.string.nav_battery, Icons.Default.BatteryChargingFull, index++, ScreenType.BATTERY))
                        items.add(NavigationItem(R.string.nav_thermal, Icons.Default.Warning, index++, ScreenType.THERMAL))
                        items.add(NavigationItem(R.string.nav_memory, Icons.Default.Menu, index++, ScreenType.MEMORY))
                        items.add(NavigationItem(R.string.nav_io, Icons.AutoMirrored.Filled.Send, index++, ScreenType.IO))

                        if (isVoltageAvailable) {
                            items.add(NavigationItem(R.string.nav_voltage, Icons.Default.Bolt, index++, ScreenType.VOLTAGE))
                        }

                        if (isAdvancedAvailable) {
                            items.add(NavigationItem(R.string.nav_advanced, Icons.Default.Star, index++, ScreenType.ADVANCED))
                        }

                        items.add(NavigationItem(R.string.nav_misc, Icons.Default.Build, index++, ScreenType.MISC))
                        items.add(NavigationItem(R.string.nav_profiles, Icons.Default.AccountBox, index++, ScreenType.PROFILES))
                        items.add(NavigationItem(R.string.nav_settings, Icons.Default.Settings, index++, ScreenType.SETTINGS))
                    }
                    
                    items.add(NavigationItem(R.string.nav_about, Icons.Default.Info, items.size, ScreenType.ABOUT))
                    
                    // Re-index to ensure correct pager navigation
                    items.mapIndexed { idx, item -> item.copy(index = idx) }
                }

                val pagerState = rememberPagerState(pageCount = { menuItems.size })

                ModalNavigationDrawer(
                    drawerState = drawerState,
                    drawerContent = {
                        ModalDrawerSheet(
                            drawerContainerColor = MaterialTheme.colorScheme.background,
                            drawerShape = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .verticalScroll(rememberScrollState())
                            ) {
                                Spacer(modifier = Modifier.height(24.dp))
                                menuItems.forEach { item ->
                                    val navInteraction = remember { MutableInteractionSource() }
                                    val isNavPressed by navInteraction.collectIsPressedAsState()
                                    val navScale by animateFloatAsState(if (isNavPressed) 0.98f else 1f, label = "bounce")

                                    NavigationDrawerItem(
                                        label = { 
                                            Text(
                                                stringResource(item.titleRes),
                                                fontWeight = FontWeight.SemiBold,
                                                letterSpacing = 0.5.sp
                                            ) 
                                        },
                                        selected = pagerState.currentPage == item.index,
                                        onClick = {
                                            coroutineScope.launch {
                                                pagerState.animateScrollToPage(item.index)
                                                drawerState.close()
                                            }
                                        },
                                        icon = { Icon(item.icon, contentDescription = null, modifier = Modifier.size(22.dp)) },
                                        interactionSource = navInteraction,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp).graphicsLayer(scaleX = navScale, scaleY = navScale),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = NavigationDrawerItemDefaults.colors(
                                            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                            selectedIconColor = MaterialTheme.colorScheme.primary,
                                            selectedTextColor = MaterialTheme.colorScheme.primary,
                                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    )
                                }
                                Spacer(modifier = Modifier.height(24.dp))
                            }
                        }
                    }
                ) {
                    Scaffold(
                        containerColor = MaterialTheme.colorScheme.background,
                        topBar = {
                            @OptIn(ExperimentalMaterial3Api::class)
                            CenterAlignedTopAppBar(
                                title = { 
                                    val currentItem = menuItems.getOrNull(pagerState.currentPage)
                                    Text(
                                        if (currentItem != null) stringResource(currentItem.titleRes) else "Synapse",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    ) 
                                },
                                navigationIcon = {
                                    val interactionSource = remember { MutableInteractionSource() }
                                    val isPressed by interactionSource.collectIsPressedAsState()
                                    val scale by animateFloatAsState(if (isPressed) 0.85f else 1f, label = "bounce")

                                    IconButton(
                                        onClick = { coroutineScope.launch { drawerState.open() } },
                                        interactionSource = interactionSource,
                                        modifier = Modifier.graphicsLayer(scaleX = scale, scaleY = scale)
                                    ) {
                                        Icon(
                                            Icons.Default.Menu,
                                            contentDescription = "Menu",
                                            modifier = Modifier.size(26.dp)
                                        )
                                    }
                                },
                                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                                    containerColor = MaterialTheme.colorScheme.background
                                )
                            )
                        }
                    ) { innerPadding ->
                        val isScrollingLambda = remember { { pagerState.isScrollInProgress } }

                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding),
                            key = { page -> menuItems.getOrNull(page)?.titleRes ?: page },
                            beyondViewportPageCount = 1
                        ) { page ->
                            val item = menuItems.getOrNull(page)
                            
                            key(item?.titleRes ?: page) {
                                when (item?.type) {
                                    ScreenType.SUMMARY -> SummaryScreen()
                                    ScreenType.GPU -> GpuScreen(isScrolling = isScrollingLambda)
                                    ScreenType.BATTERY -> BatteryScreen(isScrolling = isScrollingLambda)
                                    ScreenType.THERMAL -> ThermalScreen()
                                    ScreenType.MEMORY -> MemoryScreen(isScrolling = isScrollingLambda)
                                    ScreenType.IO -> IoScreen()
                                    ScreenType.VOLTAGE -> VoltageScreen()
                                    ScreenType.ADVANCED -> AdvancedScreen()
                                    ScreenType.MISC -> MiscScreen()
                                    ScreenType.PROFILES -> ProfileScreen()
                                    ScreenType.SETTINGS -> SettingsScreen(
                                        onThemeChange = { themeMode = it },
                                        onAccentColorChange = { accentColor = it }
                                    )
                                    ScreenType.ABOUT -> AboutScreen()
                                    ScreenType.CPU_CLUSTER -> {
                                        val policyId = item.policyId ?: 0
                                        CpuClusterPage(
                                            policyId = policyId,
                                            descriptionRes = item.descRes ?: R.string.cpu_generic_desc,
                                            isScrolling = isScrollingLambda
                                        )
                                    }
                                    else -> Box(Modifier.fillMaxSize())
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

enum class ScreenType { SUMMARY, CPU_CLUSTER, GPU, BATTERY, THERMAL, MEMORY, IO, VOLTAGE, ADVANCED, MISC, PROFILES, SETTINGS, ABOUT }
data class NavigationItem(val titleRes: Int, val icon: ImageVector, val index: Int, val type: ScreenType, val policyId: Int? = null, val descRes: Int? = null)
