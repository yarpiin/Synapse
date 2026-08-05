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
import com.af.synapse.data.BatteryManager
import com.af.synapse.data.GenericManager
import com.af.synapse.ui.components.*
import com.af.synapse.ui.theme.PixelBlue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

@Composable
fun BatteryScreen(isScrolling: () -> Boolean = { false }) {
    val scrollState = rememberScrollState()
    var stats by remember { mutableStateOf<BatteryManager.BatteryStats?>(null) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            // Initial fetch
            val initial = BatteryManager.getBatteryStats()
            withContext(Dispatchers.Main) { stats = initial }

            BatteryManager.getBatteryStatsFlow().collect {
                if (!isScrolling()) {
                    withContext(Dispatchers.Main) { stats = it }
                }
            }
        }
    }

    val displayStats = stats ?: BatteryManager.BatteryStats(
        0, 0f, 0f, "...", "...", 0, 0, 0, 0, 0f, "", false
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // 1. Dashboard: Large Level and Info
        Row(
            modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = stringResource(R.string.batt_level).uppercase(),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    color = PixelBlue,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "${displayStats.level}%",
                    fontSize = 64.sp,
                    fontWeight = FontWeight.Black,
                    fontStyle = FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = displayStats.status.uppercase(),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Column(
                modifier = Modifier.width(140.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                BatteryStatTile(label = stringResource(R.string.batt_temp), value = String.format(Locale.US, "%.1f°C", displayStats.temperature))
                BatteryStatTile(label = stringResource(R.string.batt_volt), value = String.format(Locale.US, "%.2fV", displayStats.voltageV))
            }
        }

        // 1.5 Live Power & Current Info
        BatterySection(title = stringResource(R.string.batt_live_charging)) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val powerValue = if (displayStats.isCharging) String.format(Locale.US, "%.1f W", displayStats.powerW) else "0.0 W"
                    BatteryStatTile(
                        label = stringResource(R.string.summary_charge),
                        value = powerValue,
                        modifier = Modifier.weight(1f)
                    )
                    
                    val currentVal = if (displayStats.currentMa > 0) "+${displayStats.currentMa}" else "${displayStats.currentMa}"
                    BatteryStatTile(
                        label = stringResource(R.string.batt_current),
                        value = "$currentVal mA",
                        modifier = Modifier.weight(1f)
                    )
                }
                
                if (displayStats.isCharging && displayStats.timeToFull.isNotEmpty()) {
                    BatteryCompactWideTile(
                        label = stringResource(R.string.batt_time_left),
                        value = displayStats.timeToFull,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // 2. Charging Controls
        BatterySection(title = stringResource(R.string.batt_ctrl)) {
            val fastChargePath = BatteryManager.getFastChargePath()
            if (fastChargePath != null) {
                SettingsSwitch(
                    label = stringResource(R.string.batt_fast_charge),
                    description = stringResource(R.string.batt_fast_charge_desc),
                    path = fastChargePath
                )
            }

            SettingsSwitch(
                label = stringResource(R.string.batt_enabled),
                description = stringResource(R.string.batt_enabled_desc),
                path = "/sys/class/power_supply/battery/charging_enabled"
            )
        }

        // 3. Battery Health & Capacity
        BatterySection(title = stringResource(R.string.batt_health_info)) {
            BatteryDetailRow(stringResource(R.string.batt_health_status), displayStats.health)
            BatteryDetailRow(stringResource(R.string.batt_cycles), "${displayStats.cycleCount} cycles")
            BatteryDetailRow(stringResource(R.string.batt_cap_design), "${displayStats.capacityDesign} mAh")
            BatteryDetailRow(stringResource(R.string.batt_cap_actual), "${displayStats.capacityActual} mAh")
            
            val healthPercent = if (displayStats.capacityDesign > 0) (displayStats.capacityActual * 100 / displayStats.capacityDesign).coerceIn(0, 100) else 100
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "${stringResource(R.string.batt_overall_health)}: $healthPercent%", fontWeight = FontWeight.Bold, color = PixelBlue)
            LinearProgressIndicator(
                progress = { healthPercent / 100f },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                color = PixelBlue,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }

        // 4. Pixel Specific (if on Pixel)
        if (GenericManager.exists("/sys/devices/platform/google,charger/charge_stop_level")) {
            BatterySection(title = stringResource(R.string.batt_pixel_limits)) {
                SettingsSeekBar(
                    title = stringResource(R.string.batt_stop_level),
                    description = stringResource(R.string.batt_stop_level_desc),
                    path = "/sys/devices/platform/google,charger/charge_stop_level",
                    min = 50f, max = 100f, unit = "%"
                )
                SettingsSeekBar(
                    title = stringResource(R.string.batt_start_level),
                    description = stringResource(R.string.batt_start_level_desc),
                    path = "/sys/devices/platform/google,charger/charge_start_level",
                    min = 0f, max = 95f, unit = "%"
                )
            }
        }

        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Composable
fun BatteryCompactWideTile(label: String, value: String, modifier: Modifier = Modifier) {
    val colorScheme = MaterialTheme.colorScheme
    val isDark = colorScheme.background == Color.Black
    val surfaceColor = if (isDark) Color(0xFF0F0F0F) else colorScheme.surfaceVariant.copy(alpha = 0.5f)
    val borderColor = Color.White.copy(alpha = 0.05f)

    Box(
        modifier = modifier
            .wrapContentHeight()
            .graphicsLayer { 
                clip = true
                shape = RoundedCornerShape(12.dp)
            }
            .background(surfaceColor, RoundedCornerShape(12.dp))
            .drawBehind {
                drawRect(borderColor, style = androidx.compose.ui.graphics.drawscope.Stroke(1.dp.toPx()))
            }
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = label.uppercase(),
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            color = PixelBlue.copy(alpha = 0.8f),
            modifier = Modifier.align(Alignment.CenterStart)
        )
        
        Text(
            text = value,
            fontSize = 16.sp,
            fontWeight = FontWeight.ExtraBold,
            fontStyle = FontStyle.Italic,
            color = colorScheme.onSurface,
            modifier = Modifier.align(Alignment.CenterEnd)
        )
    }
}

@Composable
fun BatteryDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
fun BatteryStatTile(label: String, value: String, modifier: Modifier = Modifier) {
    val colorScheme = MaterialTheme.colorScheme
    val isDark = colorScheme.background == Color.Black
    val surfaceColor = if (isDark) Color(0xFF0F0F0F) else colorScheme.surfaceVariant.copy(alpha = 0.5f)
    val borderColor = Color.White.copy(alpha = 0.05f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1.4f)
            .graphicsLayer { 
                clip = true
                shape = RoundedCornerShape(16.dp)
            }
            .background(surfaceColor, RoundedCornerShape(16.dp))
            .drawBehind {
                drawRect(borderColor, style = androidx.compose.ui.graphics.drawscope.Stroke(1.dp.toPx()))
            }
            .padding(12.dp)
    ) {
        Text(
            text = label.uppercase(),
            fontSize = 9.sp,
            fontWeight = FontWeight.Black,
            color = PixelBlue.copy(alpha = 0.7f),
            modifier = Modifier.align(Alignment.TopEnd)
        )
        
        Text(
            text = value,
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold,
            fontStyle = FontStyle.Italic,
            color = colorScheme.onSurface,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

@Composable
fun BatterySection(title: String, content: @Composable ColumnScope.() -> Unit) {
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
