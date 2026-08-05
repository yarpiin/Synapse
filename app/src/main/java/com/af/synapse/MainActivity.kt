package com.af.synapse

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        com.af.synapse.data.SettingsStore.init(this)
        
        // Initialize Shell with Root request
        Shell.setDefaultBuilder(Shell.Builder.create()
            .setFlags(Shell.FLAG_REDIRECT_STDERR)
            .setTimeout(20)
        )
        
        // Background check for root
        val scope = CoroutineScope(Dispatchers.IO)
        scope.launch {
            val isRoot = Shell.getShell().isRoot
            withContext(Dispatchers.Main) {
                if (!isRoot) {
                    Toast.makeText(this@MainActivity, "Root access required!", Toast.LENGTH_LONG).show()
                }
                com.af.synapse.data.MonitorManager.startMonitoring()
            }
        }

        setContent {
            var themeMode by remember { mutableIntStateOf(com.af.synapse.data.SettingsStore.getThemeMode()) }

            SynapseTheme(themeOverride = themeMode) {
                val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
                val coroutineScope = rememberCoroutineScope()
                
                val clusters = remember { CpuManager.getAvailableClusters() }
                val isGpuAvailable = remember { GpuManager.getGpuPath() != null }
                val isAdvancedAvailable = remember { com.af.synapse.data.AdvancedManager.isAdvancedAvailable() }
                val isVoltageAvailable = remember { VoltageManager.isVoltageAvailable() }
                
                val menuItems = remember(clusters, isGpuAvailable, isAdvancedAvailable, isVoltageAvailable) {
                    val items = mutableListOf<NavigationItem>()
                    items.add(NavigationItem(R.string.nav_summary, Icons.Default.Info, 0, ScreenType.SUMMARY))
                    
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
                    items.add(NavigationItem(R.string.nav_about, Icons.Default.Info, index++, ScreenType.ABOUT))
                    
                    items
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
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
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
                                    IconButton(onClick = { coroutineScope.launch { drawerState.open() } }) {
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
                            key = { page -> menuItems.getOrNull(page)?.index ?: page },
                            beyondViewportPageCount = 1
                        ) { page ->
                            val item = menuItems.getOrNull(page)
                            
                            key(item?.index ?: page) {
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
                                    ScreenType.SETTINGS -> SettingsScreen(onThemeChange = { themeMode = it })
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
