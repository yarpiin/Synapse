package com.af.synapse.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.af.synapse.R
import com.af.synapse.data.AdvancedManager
import com.af.synapse.data.GenericManager
import com.af.synapse.ui.components.*

@Composable
fun AdvancedScreen() {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text(
            text = stringResource(R.string.adv_desc),
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontStyle = FontStyle.Italic,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
        )

        // 1. Krait C-States
        val pmPath = when {
            GenericManager.isDirectory("/sys/module/pm_8x60") -> "/sys/module/pm_8x60"
            GenericManager.isDirectory("/sys/module/msm_pm") -> "/sys/module/msm_pm"
            else -> null
        }
        if (pmPath != null && GenericManager.isDirectory("$pmPath/modes/cpu0")) {
            AdvancedSection(title = stringResource(R.string.adv_krait)) {
                val base = "$pmPath/modes/cpu0"
                Text(text = stringResource(R.string.adv_krait_desc), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontStyle = FontStyle.Italic)
                Spacer(Modifier.height(8.dp))
                SettingsSwitch(label = "C0 (WFI)", description = "Fastest wake-up state.", path = "$base/wfi/idle_enabled")
                SettingsSwitch(label = "C1 (Retention)", description = "Retains CPU state at lower voltage.", path = "$base/retention/idle_enabled")
                SettingsSwitch(label = "C2 (Stand Alone PC)", description = "Core is completely powered off.", path = "$base/standalone_power_collapse/idle_enabled")
                SettingsSwitch(label = "C3 (Power Collapse)", description = "Deepest sleep, powers off core and shared resources.", path = "$base/power_collapse/idle_enabled")
            }
        }

        // 2. Kernel Samepage Merging (KSM)
        if (GenericManager.exists("/sys/kernel/mm/ksm/run")) {
            AdvancedSection(title = stringResource(R.string.adv_ksm)) {
                val ksmStats = remember { AdvancedManager.getKsmStats() }
                Text(text = stringResource(R.string.adv_ksm_desc), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontStyle = FontStyle.Italic)
                Spacer(Modifier.height(8.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    ksmStats.forEach { (key, value) ->
                        Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                            Text(text = value, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text(text = key.replace("pages_", "").replace("_", " "), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                
                SettingsSwitch(label = stringResource(R.string.adv_ksm), description = "Merge identical pages to save RAM.", path = "/sys/kernel/mm/ksm/run")
                if (GenericManager.exists("/sys/kernel/mm/ksm/deferred_timer")) {
                    SettingsSwitch(label = "Deferred Timer", description = "Prevents KSM from waking the CPU.", path = "/sys/kernel/mm/ksm/deferred_timer")
                }
                SettingsSeekBar(title = "Pages to Scan", path = "/sys/kernel/mm/ksm/pages_to_scan", min = 4f, max = 1024f, unit = "", step = 4f)
                SettingsSeekBar(title = "Sleep Interval", path = "/sys/kernel/mm/ksm/sleep_millisecs", min = 50f, max = 3000f, unit = " ms", step = 50f)
            }
        }

        // 3. UKSM
        if (GenericManager.exists("/sys/kernel/mm/uksm/run")) {
            AdvancedSection(title = stringResource(R.string.adv_uksm)) {
                Text(text = stringResource(R.string.adv_uksm_desc), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontStyle = FontStyle.Italic)
                Spacer(Modifier.height(8.dp))
                SettingsSwitch(label = stringResource(R.string.adv_uksm), description = "Faster memory merging algorithm.", path = "/sys/kernel/mm/uksm/run")
                SettingsSeekBar(title = "Sleep Interval", path = "/sys/kernel/mm/uksm/sleep_millisecs", min = 0f, max = 3000f, unit = " ms", step = 100f)
                SettingsSeekBar(title = "Max CPU %", path = "/sys/kernel/mm/uksm/max_cpu_percentage", min = 10f, max = 99f, unit = "%")
            }
        }

        // 4. Low Memory Killer (LMK)
        if (GenericManager.exists("/sys/module/lowmemorykiller/parameters/enable_adaptive_lmk")) {
            AdvancedSection(title = stringResource(R.string.adv_adaptive_lmk)) {
                Text(text = stringResource(R.string.adv_adaptive_lmk_desc), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontStyle = FontStyle.Italic)
                Spacer(Modifier.height(8.dp))
                SettingsSwitch(label = stringResource(R.string.adv_adaptive_lmk), description = "Dynamically adjusts kill thresholds.", path = "/sys/module/lowmemorykiller/parameters/enable_adaptive_lmk")
            }
        }

        // 5. Workqueues
        if (GenericManager.exists("/sys/module/workqueue/parameters/power_efficient")) {
            AdvancedSection(title = stringResource(R.string.adv_workqueues)) {
                SettingsSwitch(label = "Power Efficient", description = stringResource(R.string.adv_workqueues_desc), path = "/sys/module/workqueue/parameters/power_efficient")
            }
        }

        // 6. FSYNC
        val fsyncPath = when {
            GenericManager.exists("/sys/module/sync/parameters/fsync_enabled") -> "/sys/module/sync/parameters/fsync_enabled"
            GenericManager.exists("/sys/class/misc/fsynccontrol/fsync_enabled") -> "/sys/class/misc/fsynccontrol/fsync_enabled"
            else -> null
        }
        if (fsyncPath != null || GenericManager.exists("/sys/kernel/dyn_fsync/Dyn_fsync_active")) {
            AdvancedSection(title = stringResource(R.string.adv_fsync)) {
                Text(text = stringResource(R.string.adv_fsync_desc), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontStyle = FontStyle.Italic)
                Spacer(Modifier.height(8.dp))
                fsyncPath?.let { 
                    SettingsSwitch(label = "FSYNC", description = "Sync writes to disk.", path = it)
                }
                if (GenericManager.exists("/sys/kernel/dyn_fsync/Dyn_fsync_active")) {
                    SettingsSwitch(label = "Dynamic FSYNC", description = "Disables sync when screen is on.", path = "/sys/kernel/dyn_fsync/Dyn_fsync_active")
                }
            }
        }

        // 7. Wakelock Control
        val wakeupParams = "/sys/module/wakeup/parameters"
        if (GenericManager.isDirectory(wakeupParams)) {
            AdvancedSection(title = stringResource(R.string.adv_wakelocks)) {
                Text(text = stringResource(R.string.adv_wakelocks_desc), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontStyle = FontStyle.Italic)
                Spacer(Modifier.height(8.dp))
                val wakelocks = listOf(
                    "enable_ipa_ws" to "IPA",
                    "enable_msm_hsic_ws" to "MSM HSIC",
                    "enable_bluesleep_ws" to "BlueSleep",
                    "enable_wlan_wake_ws" to "WLAN Wake",
                    "enable_wlan_rx_wake_ws" to "WLAN RX",
                    "enable_timerfd_ws" to "TimerFD",
                    "enable_netlink_ws" to "Netlink"
                )
                wakelocks.forEach { (file, label) ->
                    if (GenericManager.exists("$wakeupParams/$file")) {
                        SettingsSwitch(label = "Wakelock $label", description = "Block $label from preventing sleep.", path = "$wakeupParams/$file")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Composable
fun AdvancedSection(title: String, content: @Composable ColumnScope.() -> Unit) {
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
