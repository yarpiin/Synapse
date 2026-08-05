package com.af.synapse.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.af.synapse.data.CpuManager
import com.af.synapse.data.GenericManager
import com.af.synapse.data.GovernorTunable
import com.af.synapse.ui.theme.PixelBlue

@Composable
fun SettingsSwitch(label: String, description: String, path: String) {
    if (!GenericManager.exists(path)) return
    
    var checked by remember(path) { mutableStateOf(GenericManager.readBool(path)) }

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
                Text(
                    text = label, 
                    fontWeight = FontWeight.Bold, 
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = description, 
                    fontSize = 12.sp, 
                    color = MaterialTheme.colorScheme.onSurfaceVariant, 
                    fontStyle = FontStyle.Italic
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = {
                    checked = it
                    GenericManager.writeBool(path, it)
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = PixelBlue,
                    checkedTrackColor = PixelBlue.copy(alpha = 0.5f)
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
    
    val actualMax = if (path.endsWith("swappiness")) 200f else max
    var value by remember(path) { mutableFloatStateOf(GenericManager.readFile(path).toFloatOrNull() ?: min) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title, 
            fontWeight = FontWeight.Bold, 
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
        if (description.isNotEmpty()) {
            Text(
                text = description, 
                fontSize = 12.sp, 
                color = MaterialTheme.colorScheme.onSurfaceVariant, 
                fontStyle = FontStyle.Italic
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Slider(
            value = value,
            onValueChange = { value = it },
            onValueChangeFinished = {
                GenericManager.writeFile(path, value.toInt().toString())
            },
            valueRange = min..actualMax,
            steps = if (step > 0 && (actualMax - min) / step > 1) ((actualMax - min) / step).toInt() - 1 else 0,
            colors = SliderDefaults.colors(
                thumbColor = PixelBlue,
                activeTrackColor = PixelBlue,
                inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )
        Text(
            text = "${value.toInt()}$unit",
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold,
            fontStyle = FontStyle.Italic,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun SettingsFrequencySeekBar(
    title: String,
    description: String = "",
    path: String,
    cpuId: Int
) {
    if (!GenericManager.exists(path)) return
    
    val availableFreqs = remember { CpuManager.getAvailableFrequencies(cpuId) }
    if (availableFreqs.isEmpty()) return

    val currentVal = remember { (GenericManager.readFile(path).toLongOrNull() ?: 0L) / 1000 }
    
    CommonFrequencySeekBar(
        title = title,
        description = description,
        currentValue = currentVal,
        values = availableFreqs,
        onValueChange = { freq ->
            GenericManager.writeFile(path, (freq * 1000).toString())
        }
    )
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

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title, 
            fontWeight = FontWeight.Bold, 
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
        if (description.isNotEmpty()) {
            Text(
                text = description, 
                fontSize = 12.sp, 
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontStyle = FontStyle.Italic
            )
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
                thumbColor = PixelBlue,
                activeTrackColor = PixelBlue,
                inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )
        Text(
            text = "${values.getOrNull(sliderValue.toInt()) ?: 0} MHz",
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold,
            fontStyle = FontStyle.Italic,
            color = MaterialTheme.colorScheme.primary
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

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label, 
            fontWeight = FontWeight.Bold, 
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
        if (description.isNotEmpty()) {
            Text(
                text = description, 
                fontSize = 12.sp, 
                color = MaterialTheme.colorScheme.onSurfaceVariant, 
                fontStyle = FontStyle.Italic
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = { expanded = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(text = currentValue.uppercase(), fontWeight = FontWeight.Bold)
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.fillMaxWidth(0.9f)
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { 
                            Text(
                                option.uppercase(),
                                color = if (option == currentValue) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            ) 
                        },
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
                focusedBorderColor = PixelBlue,
                focusedLabelColor = PixelBlue
            )
        )
        Button(
            onClick = {
                onSet(textValue)
                focusManager.clearFocus()
            },
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PixelBlue)
        ) {
            Text("SET")
        }
    }
}
