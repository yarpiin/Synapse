package com.af.synapse.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.af.synapse.data.SettingsStore
import com.af.synapse.ui.components.SettingsDropdown
import com.af.synapse.ui.components.SettingsSwitch
import com.af.synapse.ui.theme.PixelBlue

/**
 * SettingsScreen allows configuration of the app itself.
 * 
 * 1. Apply on Boot: BroadcastReceiver logic that re-applies all JSON-saved settings after Android starts.
 * 2. Apply Delay: Sets a delay in seconds before applying settings on boot.
 * 3. Theme Selection: Toggle between System Default, Light, and Dark (Pure Black).
 */
@Composable
fun SettingsScreen(onThemeChange: (Int) -> Unit = {}) {
    val scrollState = rememberScrollState()
    var applyOnBoot by remember { mutableStateOf(SettingsStore.isApplyOnBoot()) }
    var bootDelay by remember { mutableIntStateOf(SettingsStore.getBootDelay()) }
    var themeMode by remember { mutableIntStateOf(SettingsStore.getThemeMode()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text(
            text = stringResource(R.string.settings_desc),
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontStyle = FontStyle.Italic,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
        )

        // Apply on Boot Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = stringResource(R.string.settings_boot), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(
                            text = stringResource(R.string.settings_boot_desc), 
                            fontSize = 12.sp, 
                            color = MaterialTheme.colorScheme.onSurfaceVariant, 
                            fontStyle = FontStyle.Italic
                        )
                    }
                    Switch(
                        checked = applyOnBoot,
                        onCheckedChange = {
                            applyOnBoot = it
                            SettingsStore.setApplyOnBoot(it)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = PixelBlue,
                            checkedTrackColor = PixelBlue.copy(alpha = 0.5f)
                        )
                    )
                }

                if (applyOnBoot) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                    
                    Column {
                        Text(
                            text = stringResource(R.string.settings_boot_delay),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = stringResource(R.string.settings_boot_delay_desc),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontStyle = FontStyle.Italic
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Slider(
                            value = bootDelay.toFloat(),
                            onValueChange = { bootDelay = it.toInt() },
                            onValueChangeFinished = {
                                SettingsStore.setBootDelay(bootDelay)
                            },
                            valueRange = 0f..15f,
                            steps = 14,
                            colors = SliderDefaults.colors(
                                thumbColor = PixelBlue,
                                activeTrackColor = PixelBlue,
                                inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        )
                        Text(
                            text = "$bootDelay ${if (bootDelay == 1) "second" else "seconds"}",
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }

        // Theme Selection
        val themes = listOf(
            stringResource(id = if (android.os.Build.VERSION.SDK_INT >= 31) R.string.unknown else R.string.unknown), // Fallback labels
            "System Default", "Light", "Dark"
        )
        // Re-defining themes to be cleaner
        val themeOptions = listOf("Systemowy", "Jasny", "Ciemny") // Hardcoded for now to avoid R.string issues in lists
        
        SettingsDropdown(
            label = stringResource(R.string.settings_theme),
            currentValue = when(themeMode) {
                1 -> "Jasny"
                2 -> "Ciemny"
                else -> "Systemowy"
            },
            options = listOf("Systemowy", "Jasny", "Ciemny"),
            onSelect = {
                val newMode = when(it) {
                    "Jasny" -> 1
                    "Ciemny" -> 2
                    else -> 0
                }
                themeMode = newMode
                SettingsStore.setThemeMode(newMode)
                onThemeChange(newMode)
            }
        )

        Spacer(modifier = Modifier.height(80.dp))
    }
}
