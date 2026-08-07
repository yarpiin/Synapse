package com.af.synapse.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.af.synapse.R
import com.af.synapse.data.GenericManager
import com.af.synapse.ui.components.*
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun MiscScreen() {
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()

    var tcpCurrent by remember { mutableStateOf("") }
    var tcpAvailable by remember { mutableStateOf<List<String>>(emptyList()) }
    var selinuxStatus by remember { mutableStateOf("Unknown") }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val current = GenericManager.readFile("/proc/sys/net/ipv4/tcp_congestion_control").trim()
            val available = Shell.cmd("cat /proc/sys/net/ipv4/tcp_available_congestion_control")
                .exec().out.firstOrNull()?.split(" ")?.filter { it.isNotBlank() } ?: listOf(current)
            val se = Shell.cmd("getenforce").exec().out.firstOrNull() ?: "Unknown"

            withContext(Dispatchers.Main) {
                tcpCurrent = current
                tcpAvailable = available
                selinuxStatus = se
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
                RebootButton("System", { Shell.cmd("reboot").exec() }, Modifier.weight(1f))
                RebootButton("Recovery", { Shell.cmd("reboot recovery").exec() }, Modifier.weight(1f))
                RebootButton("Bootloader", { Shell.cmd("reboot bootloader").exec() }, Modifier.weight(1f))
            }
        }

        // 2. TCP Congestion Control
        if (tcpAvailable.isNotEmpty()) {
            AdvancedSection(title = stringResource(R.string.misc_tcp)) {
                SettingsDropdown(
                    label = "Algorytm TCP",
                    description = stringResource(R.string.misc_tcp_desc),
                    currentValue = tcpCurrent,
                    options = tcpAvailable,
                    onSelect = { selected ->
                        scope.launch {
                            withContext(Dispatchers.IO) {
                                GenericManager.writeFile("/proc/sys/net/ipv4/tcp_congestion_control", selected)
                                delay(150)
                                tcpCurrent = GenericManager.readFile("/proc/sys/net/ipv4/tcp_congestion_control").trim()
                            }
                        }
                    }
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

        // 4. SELinux Status & Control
        AdvancedSection(title = "SELinux") {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    val primaryColor = MaterialTheme.colorScheme.primary
                    Text(text = "Aktualny Status", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, fontSize = 14.sp)
                    Text(text = selinuxStatus, color = if (selinuxStatus.contains("Enforcing", true)) primaryColor else MaterialTheme.colorScheme.error, fontWeight = androidx.compose.ui.text.font.FontWeight.Black)
                }
                
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { 
                            scope.launch(Dispatchers.IO) {
                                Shell.cmd("setenforce 1").exec()
                                delay(200)
                                selinuxStatus = Shell.cmd("getenforce").exec().out.firstOrNull() ?: "Unknown"
                            }
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Enforcing", fontSize = 11.sp)
                    }
                    
                    Button(
                        onClick = { 
                            scope.launch(Dispatchers.IO) {
                                Shell.cmd("setenforce 0").exec()
                                delay(200)
                                selinuxStatus = Shell.cmd("getenforce").exec().out.firstOrNull() ?: "Unknown"
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Permissive", fontSize = 11.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Composable
fun RebootButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.94f else 1f, label = "bounce")

    Button(
        onClick = onClick,
        modifier = modifier.graphicsLayer(scaleX = scale, scaleY = scale),
        shape = RoundedCornerShape(12.dp),
        interactionSource = interactionSource
    ) {
        Text(label, fontSize = 12.sp)
    }
}
