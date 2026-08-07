package com.af.synapse.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.af.synapse.R
import com.af.synapse.data.GenericManager
import com.af.synapse.data.VoltageManager
import com.af.synapse.ui.components.*
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun VoltageScreen() {
    val scrollState = rememberScrollState()
    
    var vddLevels by remember { mutableStateOf<List<VoltageManager.VoltageEntry>>(emptyList()) }
    var uvMvTable by remember { mutableStateOf<List<VoltageManager.VoltageEntry>>(emptyList()) }
    var customCoreVoltages by remember { mutableStateOf<List<VoltageManager.VoltageEntry>>(emptyList()) }
    var customIvaVoltages by remember { mutableStateOf<List<VoltageManager.VoltageEntry>>(emptyList()) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val vdd = VoltageManager.getVddLevels()
            val uv = VoltageManager.getUvMvTable()
            val core = VoltageManager.getCustomVoltages("/sys/devices/virtual/misc/customvoltage/core_voltages")
            val iva = VoltageManager.getCustomVoltages("/sys/devices/virtual/misc/customvoltage/iva_voltages")
            
            withContext(Dispatchers.Main) {
                vddLevels = vdd
                uvMvTable = uv
                customCoreVoltages = core
                customIvaVoltages = iva
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
            text = stringResource(R.string.volt_desc),
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontStyle = FontStyle.Italic,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
        )

        // 1. Global Voltage Offset
        if (vddLevels.isNotEmpty() || uvMvTable.isNotEmpty()) {
            val primaryColor = MaterialTheme.colorScheme.primary
            AdvancedSection(title = stringResource(R.string.volt_global)) {
                var globalOffset by remember { mutableFloatStateOf(0f) }
                
                Slider(
                    value = globalOffset,
                    onValueChange = { globalOffset = it },
                    onValueChangeFinished = {
                        val offset = globalOffset.toInt()
                        if (vddLevels.isNotEmpty()) {
                            vddLevels.forEach { entry ->
                                VoltageManager.setVddVoltage(entry.frequency, entry.voltage + offset)
                            }
                        } else if (uvMvTable.isNotEmpty()) {
                            uvMvTable.forEach { entry ->
                                VoltageManager.setUvMvVoltage(entry.frequency, entry.voltage + offset)
                            }
                        }
                    },
                    valueRange = -300f..300f,
                    steps = 119, // 5mV steps
                    colors = SliderDefaults.colors(thumbColor = primaryColor)
                )
                Text(
                    text = "${globalOffset.toInt()} mV",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.End,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                )
            }
        }

        // 2. Individual Frequency Voltages
        if (vddLevels.isNotEmpty()) {
            val primaryColor = MaterialTheme.colorScheme.primary
            AdvancedSection(title = stringResource(R.string.volt_freq)) {
                vddLevels.forEach { entry ->
                    var currentVolt by remember(entry.voltage) { mutableFloatStateOf(entry.voltage.toFloat()) }
                    Column {
                        Text(text = entry.label, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                        Slider(
                            value = currentVolt,
                            onValueChange = { currentVolt = it },
                            onValueChangeFinished = {
                                VoltageManager.setVddVoltage(entry.frequency, currentVolt.toInt())
                            },
                            valueRange = 500000f..1400000f,
                            steps = 179, // 5000 uV steps or adjust as needed
                            colors = SliderDefaults.colors(thumbColor = primaryColor)
                        )
                        Text(text = "${currentVolt.toInt()} uV", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                    }
                }
            }
        } else if (uvMvTable.isNotEmpty()) {
            val primaryColor = MaterialTheme.colorScheme.primary
            AdvancedSection(title = stringResource(R.string.volt_freq)) {
                uvMvTable.forEach { entry ->
                    var currentVolt by remember(entry.voltage) { mutableFloatStateOf(entry.voltage.toFloat()) }
                    Column {
                        Text(text = entry.label, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                        Slider(
                            value = currentVolt,
                            onValueChange = { currentVolt = it },
                            onValueChangeFinished = {
                                VoltageManager.setUvMvVoltage(entry.frequency, currentVolt.toInt())
                            },
                            valueRange = 500f..1400f,
                            steps = 900,
                            colors = SliderDefaults.colors(thumbColor = primaryColor)
                        )
                        Text(text = "${currentVolt.toInt()} mV", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                    }
                }
            }
        }

        // 3. Boost Voltage
        if (GenericManager.exists("/sys/module/acpuclock_krait/parameters/boost")) {
            AdvancedSection(title = "Boost Voltage") {
                SettingsSwitch(label = "Enable Boost", description = "Apply a 25 mV over-volt.", path = "/sys/module/acpuclock_krait/parameters/boost")
            }
        }

        // 4. Core Voltages
        if (customCoreVoltages.isNotEmpty()) {
            val primaryColor = MaterialTheme.colorScheme.primary
            AdvancedSection(title = stringResource(R.string.volt_core)) {
                customCoreVoltages.forEach { entry ->
                    val path = "/sys/devices/virtual/misc/customvoltage/core_voltages"
                    var currentVolt by remember(entry.voltage) { mutableFloatStateOf(entry.voltage.toFloat()) }
                    Column {
                        Text(text = entry.label, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                        Slider(
                            value = currentVolt,
                            onValueChange = { currentVolt = it },
                            onValueChangeFinished = {
                                // For customvoltage, we might need a specific write method if it's an index-based write
                                Shell.cmd("chmod 644 $path 2>/dev/null", "echo \"${entry.frequency} ${currentVolt.toInt()}\" > $path").exec()
                                com.af.synapse.data.SettingsStore.trackSetting("corevolt_${entry.frequency}", "${entry.frequency} ${currentVolt.toInt()}")
                            },
                            valueRange = 700f..1500f,
                            colors = SliderDefaults.colors(thumbColor = primaryColor)
                        )
                        Text(text = "${currentVolt.toInt()} ${entry.unit}", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                    }
                }
            }
        }

        // 5. IVA Voltages
        if (customIvaVoltages.isNotEmpty()) {
            val primaryColor = MaterialTheme.colorScheme.primary
            AdvancedSection(title = stringResource(R.string.volt_iva)) {
                customIvaVoltages.forEach { entry ->
                    val path = "/sys/devices/virtual/misc/customvoltage/iva_voltages"
                    var currentVolt by remember(entry.voltage) { mutableFloatStateOf(entry.voltage.toFloat()) }
                    Column {
                        Text(text = entry.label, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                        Slider(
                            value = currentVolt,
                            onValueChange = { currentVolt = it },
                            onValueChangeFinished = {
                                Shell.cmd("chmod 644 $path 2>/dev/null", "echo \"${entry.frequency} ${currentVolt.toInt()}\" > $path").exec()
                                com.af.synapse.data.SettingsStore.trackSetting("ivavolt_${entry.frequency}", "${entry.frequency} ${currentVolt.toInt()}")
                            },
                            valueRange = 700f..1500f,
                            colors = SliderDefaults.colors(thumbColor = primaryColor)
                        )
                        Text(text = "${currentVolt.toInt()} ${entry.unit}", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(80.dp))
    }
}
