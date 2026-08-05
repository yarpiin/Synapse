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
import com.topjohnwu.superuser.Shell

@Composable
fun MiscScreen() {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text(
            text = stringResource(R.string.misc_desc),
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontStyle = FontStyle.Italic,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
        )

        // 1. Advanced Reboot
        AdvancedSection(title = stringResource(R.string.misc_reboot)) {
            Text(text = stringResource(R.string.misc_reboot_desc), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontStyle = FontStyle.Italic)
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { Shell.cmd("reboot").exec() }, modifier = Modifier.weight(1f)) { Text("System", fontSize = 12.sp) }
                Button(onClick = { Shell.cmd("reboot recovery").exec() }, modifier = Modifier.weight(1f)) { Text("Recovery", fontSize = 12.sp) }
                Button(onClick = { Shell.cmd("reboot bootloader").exec() }, modifier = Modifier.weight(1f)) { Text("Bootloader", fontSize = 12.sp) }
            }
        }

        // 2. TCP Congestion Control
        if (GenericManager.exists("/proc/sys/net/ipv4/tcp_congestion_control")) {
            AdvancedSection(title = stringResource(R.string.misc_tcp)) {
                val current = GenericManager.readFile("/proc/sys/net/ipv4/tcp_congestion_control")
                val available = Shell.cmd("cat /proc/sys/net/ipv4/tcp_available_congestion_control").exec().out.firstOrNull()?.split(" ") ?: listOf(current)
                
                SettingsDropdown(
                    label = "Algorytm TCP",
                    description = stringResource(R.string.misc_tcp_desc),
                    currentValue = current,
                    options = available,
                    onSelect = { GenericManager.writeFile("/proc/sys/net/ipv4/tcp_congestion_control", it) }
                )
            }
        }

        // 3. Android Logging
        if (GenericManager.exists("/sys/module/logger/parameters/log_enabled")) {
            AdvancedSection(title = stringResource(R.string.misc_logging)) {
                SettingsSwitch(
                    label = "Logcat",
                    description = stringResource(R.string.misc_logging_desc),
                    path = "/sys/module/logger/parameters/log_enabled"
                )
            }
        }

        // 4. SELinux Status
        AdvancedSection(title = "SELinux") {
            val status = remember { Shell.cmd("getenforce").exec().out.firstOrNull() ?: "Unknown" }
            Text(text = "Status: $status", color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp)
        }

        Spacer(modifier = Modifier.height(80.dp))
    }
}
