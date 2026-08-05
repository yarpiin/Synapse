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
import com.af.synapse.ui.components.*

@Composable
fun CpuBoostScreen() {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text(
            text = stringResource(R.string.boost_desc),
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontStyle = FontStyle.Italic,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
        )

        // 1. CPU Input Boost
        if (GenericManager.isDirectory("/sys/module/cpu_boost")) {
            AdvancedSection(title = stringResource(R.string.boost_input)) {
                SettingsSwitch(label = "Enable", description = stringResource(R.string.boost_input_desc), path = "/sys/module/cpu_boost/parameters/input_boost_enabled")
                SettingsSeekBar(title = "Duration (ms)", path = "/sys/module/cpu_boost/parameters/input_boost_ms", min = 0f, max = 500f, unit = " ms")
            }
        }

        // 2. Adreno Boost
        if (GenericManager.isDirectory("/sys/class/kgsl/kgsl-3d0/devfreq/adrenoboost")) {
            AdvancedSection(title = stringResource(R.string.boost_adreno)) {
                Text(text = stringResource(R.string.boost_adreno_desc), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontStyle = FontStyle.Italic)
                Spacer(Modifier.height(8.dp))
                val level = GenericManager.readFile("/sys/class/kgsl/kgsl-3d0/devfreq/adrenoboost")
                SettingsDropdown(
                    label = "Boost Level",
                    currentValue = when(level) {
                        "1" -> "Low"
                        "2" -> "Medium"
                        "3" -> "High"
                        else -> "Disabled"
                    },
                    options = listOf("Disabled", "Low", "Medium", "High"),
                    onSelect = {
                        val v = when(it) {
                            "Low" -> "1"
                            "Medium" -> "2"
                            "High" -> "3"
                            else -> "0"
                        }
                        GenericManager.writeFile("/sys/class/kgsl/kgsl-3d0/devfreq/adrenoboost", v)
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(80.dp))
    }
}
