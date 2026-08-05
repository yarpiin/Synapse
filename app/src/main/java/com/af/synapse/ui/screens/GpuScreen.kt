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
import com.af.synapse.data.GpuManager
import com.af.synapse.ui.components.*
import com.af.synapse.ui.theme.PixelBlue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun GpuScreen(isScrolling: () -> Boolean = { false }) {
    val scope = rememberCoroutineScope()
    val gpuFrequencyFlow = remember { GpuManager.getGpuFrequencyFlow() }
    
    val gpuFreqState = remember { mutableLongStateOf(0L) }
    val history = remember { mutableStateListOf<Long>() }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val max = GpuManager.getMaxFrequency()
            withContext(Dispatchers.Main) { gpuFreqState.longValue = max }
            
            gpuFrequencyFlow.collect { freq ->
                if (isScrolling()) return@collect
                withContext(Dispatchers.Main) {
                    gpuFreqState.longValue = freq
                    if (freq > 0) {
                        history.add(freq)
                        if (history.size > 45) history.removeAt(0)
                    }
                }
            }
        }
    }
    
    val availableFreqs = remember { GpuManager.getAvailableFrequencies() }
    val availableGovs = remember { GpuManager.getAvailableGovernors() }
    
    val minBound = remember(availableFreqs) { availableFreqs.firstOrNull() ?: 0L }
    val maxBound = remember(availableFreqs) { availableFreqs.lastOrNull() ?: 1000L }

    var minFreq by remember { mutableLongStateOf(0L) }
    var maxFreq by remember { mutableLongStateOf(0L) }
    var currentGov by remember { mutableStateOf("") }
    var tunables by remember { mutableStateOf(emptyList<com.af.synapse.data.GovernorTunable>()) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val min = GpuManager.getMinFrequency()
            val max = GpuManager.getMaxFrequency()
            val gov = GpuManager.getCurrentGovernor()
            val t = GpuManager.getGovernorTunables()
            withContext(Dispatchers.Main) {
                minFreq = min
                maxFreq = max
                currentGov = gov
                tunables = t
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text(
            text = stringResource(R.string.gpu_desc),
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontStyle = FontStyle.Italic,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
        )

        GpuTileFinal(
            freqState = gpuFreqState,
            history = history,
            min = minBound,
            max = maxBound
        )

        CommonFrequencySeekBar(
            title = stringResource(R.string.gpu_min_freq),
            description = stringResource(R.string.gpu_min_freq_desc),
            currentValue = minFreq,
            values = availableFreqs,
            onValueChange = { minFreq = it; GpuManager.setFrequency(it, false) }
        )

        CommonFrequencySeekBar(
            title = stringResource(R.string.gpu_max_freq),
            description = stringResource(R.string.gpu_max_freq_desc),
            currentValue = maxFreq,
            values = availableFreqs,
            onValueChange = { maxFreq = it; GpuManager.setFrequency(it, true) }
        )

        SettingsDropdown(
            label = stringResource(R.string.gpu_governor),
            description = stringResource(R.string.gpu_governor_desc),
            currentValue = currentGov,
            options = availableGovs,
            onSelect = { gov ->
                scope.launch {
                    withContext(Dispatchers.IO) { GpuManager.setGovernor(gov) }
                    currentGov = gov
                    delay(250)
                    val newTunables = withContext(Dispatchers.IO) { GpuManager.getGovernorTunables() }
                    tunables = newTunables
                }
            }
        )

        if (tunables.isNotEmpty()) {
            Text(stringResource(R.string.cpu_tunables), fontWeight = FontWeight.Black, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
            tunables.forEach { tunable ->
                SettingsTunableItem(tunable) { newVal ->
                    scope.launch(Dispatchers.IO) { CpuManager.setTunable(tunable.path, newVal) }
                }
            }
        }

        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Composable
fun GpuTileFinal(freqState: State<Long>, history: List<Long>, min: Long, max: Long) {
    val isDark = MaterialTheme.colorScheme.background == Color.Black
    val accentColor = if (isDark) Color.White else PixelBlue
    val surfaceColor = if (isDark) Color(0xFF0F0F0F) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    val borderColor = Color.White.copy(alpha = 0.05f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .graphicsLayer { 
                clip = true
                shape = RoundedCornerShape(24.dp)
            }
            .background(surfaceColor, RoundedCornerShape(24.dp))
            .drawBehind {
                drawRect(borderColor, style = androidx.compose.ui.graphics.drawscope.Stroke(1.dp.toPx()))
                
                if (history.isEmpty()) return@drawBehind
                val barWidth = size.width / 45f
                val range = (max - min).coerceAtLeast(1L).toFloat()
                val barColor = accentColor.copy(alpha = if (isDark) 0.15f else 0.1f)
                
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
        GpuFrequencyDisplay(
            freqState = freqState,
            modifier = Modifier.padding(24.dp).align(Alignment.Center)
        )
        
        Text(
            text = "GPU CLOCK",
            modifier = Modifier.padding(16.dp).align(Alignment.TopStart),
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            color = PixelBlue.copy(alpha = 0.7f),
            letterSpacing = 2.sp
        )
    }
}

@Composable
fun GpuFrequencyDisplay(freqState: State<Long>, modifier: Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = if (freqState.value > 0) "${freqState.value}" else "-",
            fontSize = 52.sp,
            fontWeight = FontWeight.Black,
            fontStyle = FontStyle.Italic,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "MHz",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = PixelBlue
        )
    }
}
