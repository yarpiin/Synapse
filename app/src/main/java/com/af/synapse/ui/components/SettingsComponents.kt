package com.af.synapse.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.af.synapse.data.CpuManager
import com.af.synapse.data.GenericManager
import com.af.synapse.data.GovernorTunable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SettingsSwitch(label: String, description: String, path: String) {
    if (!GenericManager.exists(path)) return
    
    var checked by remember(path) { mutableStateOf(GenericManager.readBool(path)) }
    val scope = rememberCoroutineScope()
    val primaryColor = MaterialTheme.colorScheme.primary

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = label, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(text = description, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontStyle = FontStyle.Italic)
            }
            Switch(
                checked = checked,
                onCheckedChange = { newValue ->
                    scope.launch {
                        withContext(Dispatchers.IO) {
                            GenericManager.writeBool(path, newValue)
                            delay(150)
                            val actual = GenericManager.readBool(path)
                            withContext(Dispatchers.Main) { checked = actual }
                        }
                    }
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = primaryColor,
                    uncheckedThumbColor = Color.Gray,
                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                    checkedIconColor = primaryColor,
                    uncheckedIconColor = Color.White
                )
            )
        }
    }
}

@Composable
fun SettingsSeekBar(
    title: String, 
    description: String = "", 
    path: String, 
    min: Float, 
    max: Float, 
    unit: String,
    step: Float = 1f
) {
    if (!GenericManager.exists(path)) return
    
    var value by remember(path) { mutableFloatStateOf(GenericManager.readFile(path).toFloatOrNull() ?: min) }
    val scope = rememberCoroutineScope()
    val primaryColor = MaterialTheme.colorScheme.primary

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        if (description.isNotEmpty()) {
            Text(text = description, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontStyle = FontStyle.Italic)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Slider(
            value = value,
            onValueChange = { value = it },
            onValueChangeFinished = {
                scope.launch {
                    withContext(Dispatchers.IO) {
                        GenericManager.writeFile(path, value.toInt().toString())
                        delay(150)
                        val actual = GenericManager.readFile(path).toFloatOrNull() ?: min
                        withContext(Dispatchers.Main) { value = actual }
                    }
                }
            },
            valueRange = min..max,
            steps = if (step > 0 && (max - min) / step > 1) ((max - min) / step).toInt() - 1 else 0,
            colors = SliderDefaults.colors(
                thumbColor = primaryColor,
                activeTrackColor = primaryColor,
                inactiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
            )
        )
        Text(
            text = "${value.toInt()}$unit",
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold,
            color = primaryColor
        )
    }
}

@Composable
fun SettingsDropdown(
    label: String,
    description: String = "",
    currentValue: String,
    options: List<String>,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.96f else 1f, label = "bounce")
    val primaryColor = MaterialTheme.colorScheme.primary

    Column(modifier = Modifier.fillMaxWidth()) {
        if (label.isNotEmpty()) {
            Text(text = label, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
        if (description.isNotEmpty()) {
            Text(text = description, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontStyle = FontStyle.Italic)
        }
        if (label.isNotEmpty() || description.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
        }
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = { expanded = true },
                modifier = Modifier.fillMaxWidth().graphicsLayer(scaleX = scale, scaleY = scale),
                shape = RoundedCornerShape(12.dp),
                interactionSource = interactionSource,
                border = androidx.compose.foundation.BorderStroke(1.dp, primaryColor.copy(alpha = 0.5f))
            ) {
                Text(text = currentValue.uppercase(), fontWeight = FontWeight.Bold, color = primaryColor)
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.fillMaxWidth(0.9f)
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.uppercase(), color = if (option == currentValue) primaryColor else MaterialTheme.colorScheme.onSurface) },
                        onClick = {
                            onSelect(option)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsTunableItem(tunable: GovernorTunable, onSet: (String) -> Unit) {
    var textValue by remember(tunable.value) { mutableStateOf(tunable.value) }
    val focusManager = LocalFocusManager.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.94f else 1f, label = "bounce")
    val primaryColor = MaterialTheme.colorScheme.primary

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = textValue,
            onValueChange = { textValue = it },
            label = { Text(tunable.name) },
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = primaryColor,
                focusedLabelColor = primaryColor,
                unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
            )
        )
        Button(
            onClick = {
                onSet(textValue)
                focusManager.clearFocus()
            },
            shape = RoundedCornerShape(12.dp),
            interactionSource = interactionSource,
            modifier = Modifier.graphicsLayer(scaleX = scale, scaleY = scale),
            colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
        ) {
            Text("SET")
        }
    }
}

@Composable
fun CommonFrequencySeekBar(
    title: String,
    description: String,
    currentValue: Long,
    values: List<Long>,
    onValueChange: (Long) -> Unit
) {
    if (values.isEmpty()) return
    
    val currentIndex = values.indexOf(currentValue).coerceAtLeast(0).toFloat()
    var sliderValue by remember(currentValue) { mutableFloatStateOf(currentIndex) }
    val primaryColor = MaterialTheme.colorScheme.primary

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        if (description.isNotEmpty()) {
            Text(text = description, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontStyle = FontStyle.Italic)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Slider(
            value = sliderValue,
            onValueChange = { sliderValue = it },
            onValueChangeFinished = {
                onValueChange(values[sliderValue.toInt()])
            },
            valueRange = 0f..(values.size - 1).coerceAtLeast(1).toFloat(),
            steps = if (values.size > 2) values.size - 2 else 0,
            colors = SliderDefaults.colors(
                thumbColor = primaryColor,
                activeTrackColor = primaryColor,
                inactiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
            )
        )
        Text(
            text = "${values.getOrNull(sliderValue.toInt()) ?: 0} MHz",
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold,
            color = primaryColor
        )
    }
}
