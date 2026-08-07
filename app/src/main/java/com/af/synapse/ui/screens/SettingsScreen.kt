package com.af.synapse.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.af.synapse.R
import com.af.synapse.data.ProfileManager
import com.af.synapse.data.SettingsStore
import com.af.synapse.ui.components.SettingsDropdown
import com.af.synapse.ui.components.SettingsSwitch
import kotlin.math.*

@Composable
fun SettingsScreen(
    onThemeChange: (Int) -> Unit = {},
    onAccentColorChange: (Int) -> Unit = {}
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    var applyOnBoot by remember { mutableStateOf(SettingsStore.isApplyOnBoot()) }
    var bootDelay by remember { mutableIntStateOf(SettingsStore.getBootDelay()) }
    var themeMode by remember { mutableIntStateOf(SettingsStore.getThemeMode()) }
    
    val profiles by ProfileManager.profilesFlow.collectAsState()
    var selectedBootProfile by remember { mutableStateOf(SettingsStore.getBootProfile()) }

    var accentColor by remember { mutableIntStateOf(SettingsStore.getAccentColor()) }
    var showColorPicker by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        ProfileManager.refreshProfiles(context)
    }

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

        // 1. Apply on Boot Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = stringResource(R.string.settings_boot), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(
                            text = "Automatycznie zastosuj wybrany profil po restarcie urządzenia.", 
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
                            checkedThumbColor = Color.White,
                            checkedTrackColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }

                if (applyOnBoot) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                    Column {
                        Text(text = "Profil Startowy", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        if (profiles.isEmpty()) {
                            Text("Brak zapisanych profili.", fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                        } else {
                            SettingsDropdown(
                                label = "",
                                currentValue = if (selectedBootProfile.isEmpty()) "Wybierz profil..." else selectedBootProfile,
                                options = profiles,
                                onSelect = {
                                    selectedBootProfile = it
                                    SettingsStore.setBootProfile(it)
                                }
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                    Column {
                        Text(text = stringResource(R.string.settings_boot_delay), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Slider(
                            value = bootDelay.toFloat(),
                            onValueChange = { bootDelay = it.toInt() },
                            onValueChangeFinished = { SettingsStore.setBootDelay(bootDelay) },
                            valueRange = 0f..15f,
                            steps = 14
                        )
                        Text(
                            text = "$bootDelay sekund",
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // 2. Personalizacja
        Text(text = "Personalizacja".uppercase(), fontSize = 12.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary, letterSpacing = 1.5.sp)
        
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Theme Mode
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

                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))

                // Accent Color Picker
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { showColorPicker = true },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(text = "Kolor akcentu", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(text = "Dostosuj kolor przewodni aplikacji", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (accentColor != 0xFF1A73E8.toInt()) {
                            TextButton(onClick = {
                                accentColor = 0xFF1A73E8.toInt()
                                SettingsStore.setAccentColor(accentColor)
                                onAccentColorChange(accentColor)
                            }) {
                                Text("Resetuj", fontSize = 12.sp)
                            }
                        }
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(accentColor))
                                .border(2.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(80.dp))
    }

    if (showColorPicker) {
        ColorPickerDialog(
            initialColor = Color(accentColor),
            onDismiss = { showColorPicker = false },
            onColorSelected = { color ->
                accentColor = color.toArgb()
                SettingsStore.setAccentColor(accentColor)
                onAccentColorChange(accentColor)
                showColorPicker = false
            }
        )
    }
}

@Composable
fun ColorPickerDialog(
    initialColor: Color,
    onDismiss: () -> Unit,
    onColorSelected: (Color) -> Unit
) {
    var hsv by remember { 
        val hsvArr = FloatArray(3)
        android.graphics.Color.colorToHSV(initialColor.toArgb(), hsvArr)
        mutableStateOf(Triple(hsvArr[0], hsvArr[1], hsvArr[2])) 
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Wybierz kolor akcentu") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(modifier = Modifier.size(200.dp)) {
                    HueSaturationPicker(
                        hue = hsv.first,
                        saturation = hsv.second,
                        onChanged = { h, s -> hsv = Triple(h, s, hsv.third) }
                    )
                }
                
                Column {
                    Text(text = "Jasność", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Slider(
                        value = hsv.third,
                        onValueChange = { hsv = Triple(hsv.first, hsv.second, it) },
                        valueRange = 0f..1f,
                        colors = SliderDefaults.colors(
                            thumbColor = Color.Gray,
                            activeTrackColor = Color.Gray.copy(alpha = 0.5f)
                        )
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.hsv(hsv.first, hsv.second, hsv.third))
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onColorSelected(Color.hsv(hsv.first, hsv.second, hsv.third)) }) {
                Text("Zapisz")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Anuluj")
            }
        }
    )
}

@Composable
fun HueSaturationPicker(
    hue: Float,
    saturation: Float,
    onChanged: (Float, Float) -> Unit
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val radius = constraints.maxWidth / 2f
        
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures { change, _ ->
                        val x = change.position.x - radius
                        val y = change.position.y - radius
                        
                        val angle = atan2(y.toDouble(), x.toDouble()) * (180 / PI)
                        val finalHue = if (angle < 0) (angle + 360).toFloat() else angle.toFloat()
                        
                        val dist = sqrt(x * x + y * y)
                        val finalSat = (dist / radius).coerceIn(0f, 1f)
                        
                        onChanged(finalHue, finalSat)
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(radius, radius)
                
                val colors = (0..360 step 2).map { Color.hsv(it.toFloat(), 1f, 1f) }
                drawCircle(
                    brush = Brush.sweepGradient(colors, center),
                    radius = radius
                )
                
                drawCircle(
                    brush = Brush.radialGradient(
                        0f to Color.White,
                        1f to Color.Transparent,
                        center = center,
                        radius = radius
                    ),
                    radius = radius
                )

                val angleRad = (hue * PI / 180).toFloat()
                val handleX = radius + cos(angleRad) * saturation * radius
                val handleY = radius + sin(angleRad) * saturation * radius
                
                drawCircle(
                    color = Color.Black,
                    radius = 8.dp.toPx(),
                    center = Offset(handleX, handleY),
                    style = Stroke(width = 2.dp.toPx())
                )
                drawCircle(
                    color = Color.White,
                    radius = 6.dp.toPx(),
                    center = Offset(handleX, handleY),
                    style = Stroke(width = 2.dp.toPx())
                )
            }
        }
    }
}
