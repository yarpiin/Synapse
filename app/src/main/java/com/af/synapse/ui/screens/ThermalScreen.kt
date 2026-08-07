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
import com.af.synapse.data.GenericManager
import com.af.synapse.data.MonitorManager
import com.af.synapse.data.ThermalManager
import com.af.synapse.ui.components.SettingsSeekBar
import com.af.synapse.ui.components.SettingsSwitch
import java.util.Locale

@Composable
fun ThermalScreen(isScrolling: () -> Boolean = { false }) {
    val temperatures by MonitorManager.temperatures.collectAsState()
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text(
            text = stringResource(R.string.thermal_desc),
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontStyle = FontStyle.Italic,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
        )

        TemperatureGrid(temperatures)

        val msmPath = remember {
            when {
                GenericManager.isDirectory("/sys/module/msm_thermal") -> "/sys/module/msm_thermal"
                GenericManager.isDirectory("/sys/module/msm_thermal_v2") -> "/sys/module/msm_thermal_v2"
                else -> null
            }
        }

        msmPath?.let { p ->
            ThermalSection(title = "Intellithermal") {
                SettingsSwitch(label = "Enable", description = "Kernel-based replacement for Thermald.", path = "$p/parameters/enabled")
                if (GenericManager.exists("$p/parameters/intelli_enabled")) {
                    SettingsSwitch(label = "Intellithermal Optimized", description = "Optimized thermal control algorithms.", path = "$p/parameters/intelli_enabled")
                }
                SettingsSwitch(label = "Debug Mode", description = "Activate logging for thermal events.", path = "$p/parameters/thermal_debug_mode")
                
                if (GenericManager.exists("$p/core_control/enabled")) {
                    SettingsSwitch(label = "Core Control", description = "Allows disabling CPU cores when hot.", path = "$p/core_control/enabled")
                }
                
                SettingsSeekBar(title = stringResource(R.string.thermal_throttle_temp), description = stringResource(R.string.thermal_throttle_temp_desc), path = "$p/parameters/limit_temp_degC", min = 50f, max = 100f, unit = "ºC")
                SettingsSeekBar(title = stringResource(R.string.thermal_core_temp), description = stringResource(R.string.thermal_core_temp_desc), path = "$p/parameters/core_limit_temp_degC", min = 50f, max = 100f, unit = "ºC")
                SettingsSeekBar(title = "Temperature Hysteresis", path = "$p/parameters/temp_hysteresis_degC", min = 0f, max = 20f, unit = "ºC")
            }
        }

        if (GenericManager.isDirectory("/sys/kernel/msm_thermal/conf")) {
            val p = "/sys/kernel/msm_thermal/conf"
            ThermalSection(title = "MSM Thermal Config") {
                SettingsSeekBar(title = "Allowed Low Temp", path = "$p/allowed_low_low", min = 40f, max = 100f, unit = "ºC")
                SettingsSeekBar(title = "Allowed Mid Temp", path = "$p/allowed_mid_low", min = 40f, max = 100f, unit = "ºC")
                SettingsSeekBar(title = "Allowed Max Temp", path = "$p/allowed_max_low", min = 40f, max = 100f, unit = "ºC")
                SettingsSeekBar(title = "Shutdown Temp", path = "$p/shutdown_temp", min = 40f, max = 100f, unit = "ºC")
                SettingsSeekBar(title = "Poll Interval", path = "$p/check_interval_ms", min = 0f, max = 3000f, unit = " ms", step = 50f)
            }
        }

        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Composable
fun TemperatureGrid(sensors: List<ThermalManager.TemperatureSensor>) {
    if (sensors.isEmpty()) return

    val chunks = remember(sensors) { sensors.chunked(2) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        chunks.forEach { rowSensors ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rowSensors.forEach { sensor ->
                    key(sensor.key) {
                        TemperatureTileOptimized(sensor)
                    }
                }
                if (rowSensors.size == 1) Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun RowScope.TemperatureTileOptimized(sensor: ThermalManager.TemperatureSensor) {
    val colorScheme = MaterialTheme.colorScheme
    val isDark = colorScheme.background == Color.Black
    val surfaceColor = if (isDark) Color(0xFF0F0F0F) else colorScheme.surfaceVariant.copy(alpha = 0.5f)
    val borderColor = Color.White.copy(alpha = 0.05f)

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
            }
            .padding(12.dp)
    ) {
        val label = when (sensor.label) {
            "Battery" -> "Bateria"
            else -> sensor.label
        }
        Text(
            text = label.uppercase(),
            fontSize = 9.sp,
            fontWeight = FontWeight.Black,
            color = colorScheme.primary,
            modifier = Modifier.align(Alignment.TopEnd)
        )
        
        Text(
            text = String.format(Locale.US, "%.1f°C", sensor.value),
            fontSize = 24.sp,
            fontWeight = FontWeight.Black,
            fontStyle = FontStyle.Italic,
            color = when {
                sensor.value > 70 -> Color(0xFFF44336)
                sensor.value > 45 -> Color(0xFFFF9800)
                else -> colorScheme.onSurface
            },
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

@Composable
fun ThermalSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = title,
            fontSize = 18.sp,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = 1.2.sp
        )
        content()
    }
}
