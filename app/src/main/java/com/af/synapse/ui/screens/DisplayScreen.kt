package com.af.synapse.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.af.synapse.R
import com.af.synapse.data.DisplayManager
import com.af.synapse.data.GenericManager
import com.af.synapse.ui.components.SettingsSeekBar
import com.af.synapse.ui.components.SettingsSwitch

@Composable
fun DisplayScreen() {
    val scrollState = rememberScrollState()
    
    var kcalRgb by remember { mutableStateOf(DisplayManager.getKcalRgb()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Preview Image
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            shape = RoundedCornerShape(24.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.display_preview),
                contentDescription = "Display Preview",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        // Description
        Text(
            text = "Display Control is used to alter the output levels of a device display.",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontStyle = FontStyle.Italic,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
        )

        // 1. KCAL Control
        val kcalPath = "/sys/devices/platform/kcal_ctrl.0"
        if (GenericManager.isDirectory(kcalPath)) {
            DisplaySection(title = "KCAL Control") {
                SettingsSwitch(
                    label = "KCAL Enable",
                    description = "Enables/Disables RGB Multiplier Control.",
                    path = "$kcalPath/kcal_enable"
                )
                
                // RGB Multipliers (Simplified for now, could be 3 sliders)
                KcalRgbSliders(
                    currentRgb = kcalRgb,
                    onRgbChange = { r, g, b ->
                        kcalRgb = Triple(r, g, b)
                        DisplayManager.setKcalRgb(r, g, b)
                    }
                )

                SettingsSwitch(
                    label = "KCAL Inverted Colors",
                    description = "Enables/Disables Display Inversion Mode.",
                    path = "$kcalPath/kcal_invert"
                )

                SettingsSeekBar(title = "KCAL Min", path = "$kcalPath/kcal_min", min = 0f, max = 255f, unit = "")
                SettingsSeekBar(title = "Contrast", path = "$kcalPath/kcal_cont", min = 128f, max = 383f, unit = "")
                SettingsSeekBar(title = "Saturation", path = "$kcalPath/kcal_sat", min = 128f, max = 383f, unit = "")
                SettingsSeekBar(title = "Hue", path = "$kcalPath/kcal_hue", min = 0f, max = 1536f, unit = "")
                SettingsSeekBar(title = "Value", path = "$kcalPath/kcal_val", min = 128f, max = 383f, unit = "")
            }
        }

        // 2. FB0 Features (SRE, CABC, etc)
        val fb0Path = "/sys/devices/virtual/graphics/fb0"
        if (GenericManager.isDirectory(fb0Path)) {
            DisplaySection(title = "Advanced Features") {
                if (GenericManager.exists("$fb0Path/sre")) {
                    SettingsSeekBar(title = "SRE Control", description = "Sunlight Readability Enhancement.", path = "$fb0Path/sre", min = 0f, max = 3f, unit = "")
                }
                if (GenericManager.exists("$fb0Path/cabc")) {
                    SettingsSeekBar(title = "CABC Control", description = "Content Adaptive Backlight Control.", path = "$fb0Path/cabc", min = 0f, max = 3f, unit = "")
                }
                if (GenericManager.exists("$fb0Path/color_enhance")) {
                    SettingsSwitch(label = "Color Enhancement", description = "Vivid color output.", path = "$fb0Path/color_enhance")
                }
                if (GenericManager.exists("$fb0Path/dci_p3")) {
                    SettingsSwitch(label = "DCI-P3 Mode", description = "Professional color gamut.", path = "$fb0Path/dci_p3")
                }
            }
        }

        // 3. Samsung mDNIe
        if (GenericManager.exists("/sys/class/mdnie/mdnie/mode")) {
            DisplaySection(title = "Samsung mDNIe") {
                SettingsSwitch(label = "mDNIe Bypass", description = "Disable standard Samsung calibration.", path = "/sys/class/mdnie/mdnie/bypass")
                // Modes could be a dropdown, but keeping it simple for now
                SettingsSeekBar(title = "Display Mode", path = "/sys/class/mdnie/mdnie/mode", min = 0f, max = 4f, unit = "")
            }
        }

        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Composable
fun KcalRgbSliders(currentRgb: Triple<Int, Int, Int>, onRgbChange: (Int, Int, Int) -> Unit) {
    var r by remember { mutableFloatStateOf(currentRgb.first.toFloat()) }
    var g by remember { mutableFloatStateOf(currentRgb.second.toFloat()) }
    var b by remember { mutableFloatStateOf(currentRgb.third.toFloat()) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("RGB Multipliers", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
        
        KcalSingleSlider("Red", r, androidx.compose.ui.graphics.Color.Red) { r = it; onRgbChange(r.toInt(), g.toInt(), b.toInt()) }
        KcalSingleSlider("Green", g, androidx.compose.ui.graphics.Color.Green) { g = it; onRgbChange(r.toInt(), g.toInt(), b.toInt()) }
        KcalSingleSlider("Blue", b, androidx.compose.ui.graphics.Color.Blue) { b = it; onRgbChange(r.toInt(), g.toInt(), b.toInt()) }
    }
}

@Composable
fun KcalSingleSlider(label: String, value: Float, color: androidx.compose.ui.graphics.Color, onValueChange: (Float) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.width(60.dp), fontSize = 12.sp)
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = 0f..256f,
            modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(thumbColor = color, activeTrackColor = color)
        )
        Text("${value.toInt()}", modifier = Modifier.width(40.dp), textAlign = TextAlign.End, fontSize = 12.sp)
    }
}

@Composable
fun DisplaySection(title: String, content: @Composable ColumnScope.() -> Unit) {
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
