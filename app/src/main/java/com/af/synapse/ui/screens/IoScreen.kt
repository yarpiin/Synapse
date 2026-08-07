package com.af.synapse.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.af.synapse.R
import com.af.synapse.data.CpuManager
import com.af.synapse.data.GenericManager
import com.af.synapse.data.IoManager
import com.af.synapse.ui.components.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun IoScreen() {
    var block by remember { mutableStateOf("") }
    var availableScheds by remember { mutableStateOf<List<String>>(emptyList()) }
    var currentSched by remember { mutableStateOf("") }
    var tunables by remember { mutableStateOf(emptyList<com.af.synapse.data.GovernorTunable>()) }
    
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val detectedBlock = IoManager.getInternalStorageBlock()
            val scheds = IoManager.getAvailableSchedulers(detectedBlock)
            val current = IoManager.getCurrentScheduler(detectedBlock)
            val t = IoManager.getSchedulerTunables(detectedBlock)
            
            withContext(Dispatchers.Main) {
                block = detectedBlock
                availableScheds = scheds
                currentSched = current
                tunables = t
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text(
            text = stringResource(R.string.io_desc),
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontStyle = FontStyle.Italic,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
        )

        if (block.isNotEmpty()) {
            // Read-ahead
            SettingsSeekBar(
                title = stringResource(R.string.io_read_ahead),
                description = stringResource(R.string.io_read_ahead_desc),
                path = "/sys/block/$block/queue/read_ahead_kb",
                min = 0f, max = 2048f, unit = " KB",
                step = 32f
            )

            // Scheduler Selection
            if (availableScheds.isNotEmpty()) {
                SettingsDropdown(
                    label = stringResource(R.string.io_sched),
                    description = stringResource(R.string.io_sched_desc),
                    currentValue = currentSched,
                    options = availableScheds,
                    onSelect = { sched ->
                        scope.launch {
                            withContext(Dispatchers.IO) { 
                                IoManager.setScheduler(block, sched)
                                delay(200)
                                val current = IoManager.getCurrentScheduler(block)
                                val t = IoManager.getSchedulerTunables(block)
                                withContext(Dispatchers.Main) {
                                    currentSched = current
                                    tunables = t
                                }
                            }
                        }
                    }
                )
            }

            if (GenericManager.exists("/sys/module/mmc_core/parameters/use_spi_crc")) {
                SettingsSwitch(
                    label = stringResource(R.string.io_crc),
                    description = stringResource(R.string.io_crc_desc),
                    path = "/sys/module/mmc_core/parameters/use_spi_crc"
                )
            }

            Text(
                text = "General I/O Tunables", 
                fontWeight = FontWeight.Black, 
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 1.sp
            )
            
            SettingsSwitch(label = "Add Random", description = "Draw entropy from storage.", path = "/sys/block/$block/queue/add_random")
            SettingsSwitch(label = "I/O Stats", description = "Maintain I/O statistics.", path = "/sys/block/$block/queue/iostats")
            SettingsSwitch(label = "Rotational", description = "Treat device as rotational storage.", path = "/sys/block/$block/queue/rotational")

            SettingsSeekBar(title = "NR Requests", path = "/sys/block/$block/queue/nr_requests", min = 128f, max = 2048f, unit = "")

            if (tunables.isNotEmpty()) {
                Text(
                    text = "Scheduler Tunables",
                    fontWeight = FontWeight.Black, 
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.sp
                )
                tunables.forEach { tunable ->
                    SettingsTunableItem(tunable) { newVal ->
                        scope.launch {
                            withContext(Dispatchers.IO) {
                                CpuManager.setTunable(tunable.path, newVal)
                                delay(200)
                                val t = IoManager.getSchedulerTunables(block)
                                withContext(Dispatchers.Main) {
                                    tunables = t
                                }
                            }
                        }
                    }
                }
            }
        } else {
            Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        Spacer(modifier = Modifier.height(80.dp))
    }
}
