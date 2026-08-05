package com.af.synapse.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.af.synapse.R
import com.af.synapse.data.CpuManager
import com.af.synapse.data.GovernorTunable
import com.af.synapse.ui.components.*
import com.af.synapse.ui.theme.PixelBlue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun CpuClusterPage(
    policyId: Int,
    descriptionRes: Int,
    isScrolling: () -> Boolean = { false }
) {
    val cpuIds = remember(policyId) { CpuManager.getClusterCpus(policyId) }
    val scope = rememberCoroutineScope()
    
    val freqStates = remember(cpuIds) { cpuIds.associateWith { mutableLongStateOf(0L) } }
    val historyStates = remember(cpuIds) { cpuIds.associateWith { mutableStateListOf<Long>() } }

    var availableFreqs by remember { mutableStateOf<List<Long>>(emptyList()) }
    var availableGovs by remember { mutableStateOf<List<String>>(emptyList()) }
    var minFreq by remember { mutableLongStateOf(0L) }
    var maxFreq by remember { mutableLongStateOf(0L) }
    var currentGov by remember { mutableStateOf("") }
    var tunables by remember { mutableStateOf(emptyList<GovernorTunable>()) }

    LaunchedEffect(cpuIds) {
        withContext(Dispatchers.IO) {
            val firstCpu = cpuIds.firstOrNull() ?: 0
            val freqs = CpuManager.getAvailableFrequencies(firstCpu)
            val govs = CpuManager.getAvailableGovernors(firstCpu)
            val min = CpuManager.getMinFrequency(firstCpu)
            val max = CpuManager.getMaxFrequency(firstCpu)
            val gov = CpuManager.getCurrentGovernor(firstCpu)
            val t = CpuManager.getGovernorTunables(firstCpu)
            
            val firstRead = CpuManager.getCurrentFrequencies(cpuIds)

            withContext(Dispatchers.Main) {
                availableFreqs = freqs
                availableGovs = govs
                minFreq = min
                maxFreq = max
                currentGov = gov
                tunables = t
                firstRead.forEach { (id, value) -> freqStates[id]?.longValue = value }
            }

            CpuManager.getCpuFrequencyFlow(cpuIds).collect { newFreqs ->
                if (isScrolling()) return@collect
                
                withContext(Dispatchers.Main) {
                    newFreqs.forEach { (id, value) ->
                        freqStates[id]?.longValue = value
                        if (value > 0) {
                            val history = historyStates[id]
                            if (history != null) {
                                history.add(value)
                                if (history.size > 25) history.removeAt(0)
                            }
                        }
                    }
                }
            }
        }
    }
    
    val minBound = remember(availableFreqs) { availableFreqs.firstOrNull() ?: 0L }
    val maxBound = remember(availableFreqs) { availableFreqs.lastOrNull() ?: 3000L }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text(
            text = stringResource(descriptionRes),
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontStyle = FontStyle.Italic,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
        )

        // CPU Grid
        val chunks = remember(cpuIds) { cpuIds.chunked(2) }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            chunks.forEach { rowCpus ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    rowCpus.forEach { cpuId ->
                        CpuTileFinal(
                            cpuId = cpuId,
                            freqState = freqStates[cpuId]!!,
                            history = historyStates[cpuId]!!,
                            min = minBound,
                            max = maxBound
                        )
                    }
                    if (rowCpus.size == 1) Spacer(modifier = Modifier.weight(1f))
                }
            }
        }

        if (availableFreqs.isNotEmpty()) {
            CommonFrequencySeekBar(
                title = stringResource(R.string.cpu_min_freq),
                description = stringResource(R.string.cpu_min_freq_desc),
                currentValue = minFreq,
                values = availableFreqs,
                onValueChange = { minFreq = it; CpuManager.setFrequency(cpuIds, it, false) }
            )

            CommonFrequencySeekBar(
                title = stringResource(R.string.cpu_max_freq),
                description = stringResource(R.string.cpu_max_freq_desc),
                currentValue = maxFreq,
                values = availableFreqs,
                onValueChange = { maxFreq = it; CpuManager.setFrequency(cpuIds, it, true) }
            )
        }

        if (availableGovs.isNotEmpty()) {
            SettingsDropdown(
                label = stringResource(R.string.cpu_governor),
                description = stringResource(R.string.cpu_governor_desc),
                currentValue = currentGov,
                options = availableGovs,
                onSelect = { gov ->
                    scope.launch {
                        withContext(Dispatchers.IO) { CpuManager.setGovernor(cpuIds, gov) }
                        currentGov = gov
                        delay(250)
                        val newTunables = withContext(Dispatchers.IO) { CpuManager.getGovernorTunables(cpuIds.firstOrNull() ?: 0) }
                        tunables = newTunables
                    }
                }
            )
        }

        if (tunables.isNotEmpty()) {
            Text(stringResource(R.string.cpu_tunables), fontWeight = FontWeight.Black, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
            tunables.forEach { tunable ->
                SettingsTunableItem(tunable) { newVal ->
                    scope.launch {
                        withContext(Dispatchers.IO) { CpuManager.setTunable(tunable.path, newVal) }
                        delay(200)
                        withContext(Dispatchers.IO) {
                            val firstCpu = cpuIds.firstOrNull() ?: 0
                            val min = CpuManager.getMinFrequency(firstCpu)
                            val max = CpuManager.getMaxFrequency(firstCpu)
                            val gov = CpuManager.getCurrentGovernor(firstCpu)
                            val t = CpuManager.getGovernorTunables(firstCpu)
                            withContext(Dispatchers.Main) {
                                minFreq = min
                                maxFreq = max
                                currentGov = gov
                                tunables = t
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Composable
fun RowScope.CpuTileFinal(
    cpuId: Int,
    freqState: State<Long>,
    history: SnapshotStateList<Long>,
    min: Long,
    max: Long
) {
    val isDark = MaterialTheme.colorScheme.background == Color.Black
    val accentColor = if (isDark) Color.White else PixelBlue
    val surfaceColor = if (isDark) Color(0xFF0F0F0F) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    val borderColor = Color.White.copy(alpha = 0.05f)
    val labelColor = PixelBlue

    Box(
        modifier = Modifier
            .weight(1f)
            .aspectRatio(1.6f)
            .graphicsLayer { 
                clip = true
                shape = RoundedCornerShape(12.dp)
            }
            .background(surfaceColor, RoundedCornerShape(12.dp))
            .drawBehind {
                drawRect(borderColor, style = androidx.compose.ui.graphics.drawscope.Stroke(1.dp.toPx()))
                
                if (history.isEmpty()) return@drawBehind
                
                val barCount = 30
                val barWidth = size.width / barCount
                val range = (max - min).coerceAtLeast(1L).toFloat()
                val barAlpha = if (isDark) 0.12f else 0.08f
                val barColor = accentColor.copy(alpha = barAlpha)
                
                for (i in 0 until history.size) {
                    val f = history[i]
                    val h = ((f - min).toFloat() / range).coerceIn(0.1f, 1f) * size.height
                    drawRect(
                        color = barColor,
                        topLeft = Offset(i * barWidth, size.height - h),
                        size = Size(barWidth - 1.dp.toPx(), h)
                    )
                }
            }
    ) {
        Text(
            text = "CPU $cpuId",
            modifier = Modifier.padding(10.dp).align(Alignment.TopStart),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = labelColor
        )
        
        FrequencyValue(
            freqState = freqState,
            modifier = Modifier.padding(10.dp).align(Alignment.BottomEnd)
        )
    }
}

@Composable
fun FrequencyValue(freqState: State<Long>, modifier: Modifier) {
    Text(
        text = if (freqState.value > 0) "${freqState.value}" else "-",
        fontSize = 22.sp,
        fontWeight = FontWeight.Black,
        fontStyle = FontStyle.Italic,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier
    )
}
