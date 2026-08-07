package com.af.synapse.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.af.synapse.R
import com.af.synapse.data.MemoryManager
import com.af.synapse.data.MonitorManager
import com.af.synapse.ui.components.SettingsDropdown
import com.af.synapse.ui.components.SettingsSeekBar
import com.af.synapse.ui.components.SettingsSwitch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun MemoryScreen(isScrolling: () -> Boolean = { false }) {
    val ramStats by MonitorManager.ramStats.collectAsState()
    val globalZramStats by MonitorManager.zramStats.collectAsState()
    
    var zramAlgo by remember { mutableStateOf("") }
    val zramAlgos = remember { mutableStateListOf<String>() }
    
    var isApplyingZram by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Screen-local stats initialized to current global value
    var localZramStats by remember { mutableStateOf(MonitorManager.zramStats.value) }
    
    // Sync local with global if not applying
    LaunchedEffect(globalZramStats) {
        if (!isApplyingZram) {
            localZramStats = globalZramStats
        }
    }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            // High priority refresh for this screen
            val stats = MemoryManager.getZRamStats()
            val algos = MemoryManager.getZRamCompAlgorithms()
            val currentAlgo = MemoryManager.getCurrentZRamAlgorithm()
            withContext(Dispatchers.Main) {
                localZramStats = stats
                zramAlgos.clear()
                zramAlgos.addAll(algos)
                zramAlgo = currentAlgo
            }
        }
    }

    val displayZram = localZramStats ?: MemoryManager.ZRamStats(0, 0, 0)

    if (isApplyingZram) {
        val primaryColor = MaterialTheme.colorScheme.primary
        AlertDialog(
            onDismissRequest = { },
            confirmButton = { },
            text = { 
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally, 
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
                ) {
                    CircularProgressIndicator(color = primaryColor)
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = stringResource(R.string.zram_applying),
                        fontStyle = FontStyle.Italic,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            },
            properties = androidx.compose.ui.window.DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text(
            text = stringResource(R.string.mem_desc),
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontStyle = FontStyle.Italic,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
        )

        RamDisplayRow(ramStats)

        Column {
            val primaryColor = MaterialTheme.colorScheme.primary
            Text(
                text = stringResource(R.string.mem_zram_usage), 
                fontWeight = FontWeight.Black, 
                fontSize = 12.sp,
                color = primaryColor,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { (displayZram.usedPercent / 100f).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp)
                    .clip(RoundedCornerShape(12.dp)),
                color = primaryColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = "${displayZram.usedMb} MB", fontSize = 12.sp, fontStyle = FontStyle.Italic)
                Text(text = "${displayZram.totalMb} MB", fontSize = 12.sp, fontStyle = FontStyle.Italic)
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))

        DynamicZRamSeekBar(
            currentValueMb = displayZram.totalMb,
            maxMb = 8192L,
            onValueChange = { newValueMb ->
                scope.launch {
                    isApplyingZram = true
                    withContext(Dispatchers.IO) {
                        MemoryManager.setZRamSize(newValueMb.toInt())
                        delay(4000) 
                        val newStats = MemoryManager.getZRamStats()
                        withContext(Dispatchers.Main) {
                            localZramStats = newStats
                            isApplyingZram = false
                        }
                    }
                }
            }
        )

        SettingsDropdown(
            label = stringResource(R.string.mem_zram_algo),
            description = stringResource(R.string.mem_zram_algo_desc),
            currentValue = zramAlgo,
            options = zramAlgos,
            onSelect = { algo ->
                scope.launch {
                    isApplyingZram = true
                    withContext(Dispatchers.IO) {
                        MemoryManager.setZRamAlgorithm(algo)
                        delay(4000)
                        val newStats = MemoryManager.getZRamStats()
                        withContext(Dispatchers.Main) {
                            zramAlgo = algo
                            localZramStats = newStats
                            isApplyingZram = false
                        }
                    }
                }
            }
        )

        SettingsSeekBar(title = stringResource(R.string.mem_swappiness), description = stringResource(R.string.mem_swappiness_desc), path = "/proc/sys/vm/swappiness", min = 0f, max = 200f, unit = "%")
        SettingsSeekBar(title = stringResource(R.string.mem_vfs_cache), description = stringResource(R.string.mem_vfs_cache_desc), path = "/proc/sys/vm/vfs_cache_pressure", min = 0f, max = 200f, unit = "%")
        SettingsSeekBar(title = stringResource(R.string.mem_dirty_bg_ratio), description = stringResource(R.string.mem_dirty_bg_ratio_desc), path = "/proc/sys/vm/dirty_background_ratio", min = 0f, max = 100f, unit = "%")
        SettingsSeekBar(title = stringResource(R.string.mem_dirty_ratio), description = stringResource(R.string.mem_dirty_ratio_desc), path = "/proc/sys/vm/dirty_ratio", min = 0f, max = 100f, unit = "%")
        
        if (com.af.synapse.data.GenericManager.exists("/proc/sys/vm/extra_free_kbytes")) {
            SettingsSeekBar(
                title = stringResource(R.string.mem_extra_free),
                description = stringResource(R.string.mem_extra_free_desc),
                path = "/proc/sys/vm/extra_free_kbytes",
                min = 0f, max = 100000f, unit = " KB"
            )
        }
        
        SettingsSwitch(
            label = stringResource(R.string.mem_laptop_mode), 
            description = stringResource(R.string.mem_laptop_mode_desc),
            path = "/proc/sys/vm/laptop_mode"
        )

        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Composable
fun RamDisplayRow(ramStats: MemoryManager.RamStats?) {
    Row(
        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(
            modifier = Modifier.weight(1f).fillMaxHeight(),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = stringResource(R.string.summary_ram).uppercase(),
                fontSize = 14.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 1.sp
            )
            val totalGb = (ramStats?.totalMb?.toDouble() ?: 0.0) / 1024.0
            Text(
                text = if (totalGb > 0) String.format("%.1f", totalGb) else "--",
                fontSize = 64.sp,
                fontWeight = FontWeight.Black,
                fontStyle = FontStyle.Italic,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Gigabytes",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Column(
            modifier = Modifier.width(140.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            RamStatTile(
                label = "Used",
                percentage = ramStats?.usedPercent ?: 0,
                valueMb = ramStats?.usedMb ?: 0
            )
            RamStatTile(
                label = "Free",
                percentage = (100 - (ramStats?.usedPercent ?: 0)),
                valueMb = ramStats?.freeMb ?: 0
            )
        }
    }
}

@Composable
fun DynamicZRamSeekBar(
    currentValueMb: Long,
    maxMb: Long,
    onValueChange: (Long) -> Unit
) {
    var sliderValue by remember(currentValueMb) { mutableFloatStateOf(currentValueMb.toFloat()) }

    val primaryColor = MaterialTheme.colorScheme.primary
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.mem_zram_size), 
            fontWeight = FontWeight.Bold, 
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = stringResource(R.string.mem_zram_size_desc),
            fontSize = 12.sp, 
            color = MaterialTheme.colorScheme.onSurfaceVariant, 
            fontStyle = FontStyle.Italic
        )
        Spacer(modifier = Modifier.height(8.dp))
        Slider(
            value = sliderValue,
            onValueChange = { sliderValue = it },
            onValueChangeFinished = {
                onValueChange(sliderValue.toLong())
            },
            valueRange = 0f..maxMb.toFloat(),
            steps = if (maxMb >= 128) (maxMb / 128).toInt() - 1 else 0,
            colors = SliderDefaults.colors(
                thumbColor = primaryColor,
                activeTrackColor = primaryColor,
                inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )
        
        val displayValue = if (sliderValue >= 1000f) {
            String.format("%.2f GB", sliderValue / 1000f)
        } else {
            "${sliderValue.toInt()} MB"
        }
        
        Text(
            text = displayValue,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold,
            fontStyle = FontStyle.Italic,
            color = primaryColor
        )
    }
}

@Composable
fun RamStatTile(label: String, percentage: Int, valueMb: Long) {
    val colorScheme = MaterialTheme.colorScheme
    val isDark = colorScheme.background == Color.Black
    val surfaceColor = if (isDark) Color(0xFF0F0F0F) else colorScheme.surfaceVariant.copy(alpha = 0.5f)
    val borderColor = Color.White.copy(alpha = 0.05f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1.2f)
            .graphicsLayer { 
                clip = true
                shape = RoundedCornerShape(16.dp)
            }
            .background(surfaceColor, RoundedCornerShape(16.dp))
            .drawBehind {
                drawRect(borderColor, style = androidx.compose.ui.graphics.drawscope.Stroke(1.dp.toPx()))
            }
            .padding(10.dp)
    ) {
        Text(
            text = label.uppercase(),
            fontSize = 9.sp,
            fontWeight = FontWeight.Black,
            color = colorScheme.primary.copy(alpha = 0.7f),
            modifier = Modifier.align(Alignment.TopEnd)
        )
        
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "$percentage%",
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                fontStyle = FontStyle.Italic,
                color = colorScheme.onSurface
            )
            Text(
                text = "$valueMb MB",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = colorScheme.primary
            )
        }
    }
}
