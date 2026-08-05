package com.af.synapse.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.af.synapse.data.GenericManager
import com.af.synapse.data.GovernorTunable
import com.af.synapse.ui.components.*

@Composable
fun WakeScreen() {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Description
        Text(
            text = "Wake gesture driver allows to assign custom wake actions with sweep2wake and doubletap2wake. Works with kernels and devices if they have implemented wake gestures.",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontStyle = FontStyle.Italic,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
        )

        // 1. Touch Wake
        if (GenericManager.exists("/sys/devices/virtual/misc/touchwake/enabled")) {
            WakeSection(title = "Touch Wake") {
                SettingsSwitch(
                    label = "Enable",
                    description = "Wake device via one tap in the middle of the screen.",
                    path = "/sys/devices/virtual/misc/touchwake/enabled"
                )
                SettingsSeekBar(
                    title = "Touch Wake Delay",
                    description = "Delay until touch controls are disabled after screen off. 0 for infinite.",
                    path = "/sys/devices/virtual/misc/touchwake/delay",
                    min = 0f, max = 600000f, unit = " ms", step = 1000f
                )
            }
        }

        // 2. LGE Touch Core DT2W
        if (GenericManager.exists("/sys/module/lge_touch_core/parameters/doubletap_to_wake")) {
            WakeSection(title = "DoubleTap2Wake (LGE)") {
                val p = "/sys/module/lge_touch_core/parameters"
                SettingsSwitch(label = "Enable", description = "Wake by double tapping the screen.", path = "$p/doubletap_to_wake")
                if (GenericManager.exists("$p/debug_mask")) {
                    SettingsSwitch(label = "Debug Mask", description = "Activate debug for DT2W.", path = "$p/debug_mask")
                }
                if (GenericManager.exists("$p/doubletap_pwrkey_suspend")) {
                    SettingsSwitch(label = "Power Key Suspend", description = "Disable wake controls if screen off by power key.", path = "$p/doubletap_pwrkey_suspend")
                }
                if (GenericManager.exists("$p/doubletap_area")) {
                    SettingsDropdown(
                        label = "Doubletap Area",
                        currentValue = when (GenericManager.readFile("$p/doubletap_area")) {
                            "1" -> "Bottom half"
                            "2" -> "Top half"
                            "3" -> "Center"
                            else -> "Full"
                        },
                        options = listOf("Full", "Bottom half", "Top half", "Center"),
                        onSelect = {
                            val v = when (it) {
                                "Bottom half" -> "1"
                                "Top half" -> "2"
                                "Center" -> "3"
                                else -> "0"
                            }
                            GenericManager.writeFile("$p/doubletap_area", v)
                        }
                    )
                }
            }
        }

        // 3. Android Touch
        if (GenericManager.isDirectory("/sys/android_touch")) {
            val p = "/sys/android_touch"
            WakeSection(title = "Android Touch Gestures") {
                if (GenericManager.exists("$p/wake_gestures")) {
                    SettingsSwitch(label = "Wake Gestures", description = "Enable custom wake gestures.", path = "$p/wake_gestures")
                }
                if (GenericManager.exists("$p/doubletap2wake")) {
                    SettingsSwitch(label = "DoubleTap2Wake", description = "Wake by double tapping.", path = "$p/doubletap2wake")
                }
                if (GenericManager.exists("$p/sweep2wake")) {
                    val s2w = GenericManager.readFile("$p/sweep2wake")
                    SettingsDropdown(
                        label = "Sweep2Wake",
                        currentValue = when (s2w) {
                            "1" -> "Swipe Right"
                            "2" -> "Swipe Left"
                            "4" -> "Swipe Up"
                            "8" -> "Swipe Down"
                            else -> "Disabled"
                        },
                        options = listOf("Disabled", "Swipe Right", "Swipe Left", "Swipe Up", "Swipe Down"),
                        onSelect = {
                            val v = when (it) {
                                "Swipe Right" -> "1"
                                "Swipe Left" -> "2"
                                "Swipe Up" -> "4"
                                "Swipe Down" -> "8"
                                else -> "0"
                            }
                            GenericManager.writeFile("$p/sweep2wake", v)
                        }
                    )
                }
                if (GenericManager.exists("$p/sweep2sleep")) {
                    SettingsSwitch(label = "Sweep2Sleep", description = "Sleep by swiping screen.", path = "$p/sweep2sleep")
                }
                if (GenericManager.exists("$p/vib_strength")) {
                    SettingsSeekBar(title = "Vibrator Strength", description = "Intensity for wake controls.", path = "$p/vib_strength", min = 0f, max = 90f, unit = "%")
                }
            }
        }

        // 4. Touchpanel (OP/Generic)
        if (GenericManager.isDirectory("/proc/touchpanel")) {
            val p = "/proc/touchpanel"
            WakeSection(title = "Touchpanel Gestures") {
                if (GenericManager.exists("$p/double_tap_enable")) {
                    SettingsSwitch(label = "Double Tap", description = "Double tap screen to wake.", path = "$p/double_tap_enable")
                }
                if (GenericManager.exists("$p/camera_enable")) {
                    SettingsSwitch(label = "Circle to Camera", description = "Draw O to open camera.", path = "$p/camera_enable")
                }
                if (GenericManager.exists("$p/music_enable")) {
                    SettingsSwitch(label = "Gestures to Music", description = "Vertical two fingers play/pause, left/right arrows for tracks.", path = "$p/music_enable")
                }
                if (GenericManager.exists("$p/flashlight_enable")) {
                    SettingsSwitch(label = "V to Flashlight", description = "Draw V to toggle flashlight.", path = "$p/flashlight_enable")
                }
            }
        }

        // 5. Magnetic / Lid
        if (GenericManager.exists("/sys/module/lid/parameters/enable_lid")) {
            WakeSection(title = "Magnetic Sensor") {
                SettingsSwitch(label = "Lid Support", description = "Turn off screen when lid is closed.", path = "/sys/module/lid/parameters/enable_lid")
            }
        }

        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Composable
fun WakeSection(title: String, content: @Composable ColumnScope.() -> Unit) {
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
