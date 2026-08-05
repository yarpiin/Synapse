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
import com.af.synapse.data.HotplugManager
import com.af.synapse.ui.components.*

@Composable
fun HotplugScreen() {
    val scrollState = rememberScrollState()
    val easGroups = remember { HotplugManager.getEasGroups() }
    val cpusetGroups = remember { HotplugManager.getCpusetGroups() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text(
            text = stringResource(R.string.hotplug_warning),
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontStyle = FontStyle.Italic,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
        )

        // Sched Util Clamp
        if (GenericManager.exists("/proc/sys/kernel/sched_util_clamp_min")) {
            HotplugSection(title = "Sched Util Clamp") {
                SettingsTunableItem(com.af.synapse.data.GovernorTunable("Min Clamp", GenericManager.readFile("/proc/sys/kernel/sched_util_clamp_min"), "/proc/sys/kernel/sched_util_clamp_min")) { GenericManager.writeFile("/proc/sys/kernel/sched_util_clamp_min", it) }
                SettingsTunableItem(com.af.synapse.data.GovernorTunable("Max Clamp", GenericManager.readFile("/proc/sys/kernel/sched_util_clamp_max"), "/proc/sys/kernel/sched_util_clamp_max")) { GenericManager.writeFile("/proc/sys/kernel/sched_util_clamp_max", it) }
            }
        }

        // Energy Aware Scheduling (EAS)
        if (easGroups.isNotEmpty()) {
            HotplugSection(title = stringResource(R.string.hotplug_eas)) {
                Text(
                    text = stringResource(R.string.hotplug_eas_desc),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontStyle = FontStyle.Italic
                )
                
                easGroups.forEach { group ->
                    val groupDisplay = if (group.isEmpty()) "ROOT" else group.uppercase()
                    val basePath = if (group.isEmpty()) "/dev/cpuctl" else "/dev/cpuctl/$group"
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = groupDisplay, fontSize = 14.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                    
                    val pathMin = "$basePath/cpu.uclamp.min"
                    val pathMax = "$basePath/cpu.uclamp.max"
                    
                    if (GenericManager.exists(pathMin)) {
                        SettingsTunableItem(com.af.synapse.data.GovernorTunable("Uclamp Min", GenericManager.readFile(pathMin), pathMin)) { GenericManager.writeFile(pathMin, it) }
                    }
                    if (GenericManager.exists(pathMax)) {
                        SettingsTunableItem(com.af.synapse.data.GovernorTunable("Uclamp Max", GenericManager.readFile(pathMax), pathMax)) { GenericManager.writeFile(pathMax, it) }
                    }
                }
            }
        }

        // CPUSET
        if (cpusetGroups.isNotEmpty()) {
            HotplugSection(title = stringResource(R.string.hotplug_cpuset)) {
                Text(
                    text = stringResource(R.string.hotplug_cpuset_desc),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontStyle = FontStyle.Italic
                )

                cpusetGroups.forEach { group ->
                    val groupDisplay = if (group.isEmpty()) "ROOT" else group.uppercase()
                    val path = if (group.isEmpty()) "/dev/cpuset/cpus" else "/dev/cpuset/$group/cpus"
                    
                    if (GenericManager.exists(path)) {
                        SettingsTunableItem(com.af.synapse.data.GovernorTunable("$groupDisplay CPUS", GenericManager.readFile(path), path)) { GenericManager.writeFile(path, it) }
                    }
                }
            }
        }

        // Intelliplug
        if (GenericManager.isDirectory("/sys/module/intelli_plug/parameters") || GenericManager.isDirectory("/sys/kernel/intelli_plug")) {
            val isLegacy = !GenericManager.exists("/sys/kernel/intelli_plug/full_mode_profile")
            val p = if (isLegacy) "/sys/module/intelli_plug/parameters" else "/sys/kernel/intelli_plug"
            
            HotplugSection(title = "Intelliplug") {
                SettingsSwitch(label = "Enable", description = "Kernel replacement for MPDecision.", path = "$p/intelli_plug_active")
                SettingsSwitch(label = "Touch Boost", description = "Boosts cores on screen touch.", path = "$p/touch_boost_active")
                SettingsSeekBar(title = "Min Cores Online", path = "$p/min_cpus_online", min = 1f, max = 8f, unit = "")
                SettingsSeekBar(title = "Max Cores Online", path = "$p/max_cpus_online", min = 1f, max = 8f, unit = "")
            }
        }

        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Composable
fun HotplugSection(title: String, content: @Composable ColumnScope.() -> Unit) {
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
