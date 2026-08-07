package com.af.synapse.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.af.synapse.R
import com.af.synapse.data.ProfileManager

import androidx.compose.foundation.shape.RoundedCornerShape

@Composable
fun ProfileScreen() {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val profiles by ProfileManager.profilesFlow.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }
    var newProfileName by remember { mutableStateOf("") }

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
            text = stringResource(R.string.prof_desc),
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontStyle = FontStyle.Italic,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
        )

        val interactionSource = remember { MutableInteractionSource() }
        val isPressed by interactionSource.collectIsPressedAsState()
        val scale by animateFloatAsState(if (isPressed) 0.96f else 1f, label = "bounce")

        Button(
            onClick = { showCreateDialog = true },
            modifier = Modifier.fillMaxWidth().graphicsLayer(scaleX = scale, scaleY = scale),
            interactionSource = interactionSource
        ) {
            Text(stringResource(R.string.prof_create))
        }

        if (profiles.isEmpty()) {
            Text(
                text = stringResource(R.string.prof_none),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            profiles.forEach { profileName ->
                ProfileItem(
                    name = profileName,
                    onApply = { ProfileManager.applyProfile(context, profileName) },
                    onDelete = { 
                        ProfileManager.deleteProfile(context, profileName)
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(80.dp))
    }

    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text(stringResource(R.string.prof_save_title)) },
            text = {
                TextField(
                    value = newProfileName,
                    onValueChange = { newProfileName = it },
                    label = { Text(stringResource(R.string.prof_name_hint)) }
                )
            },
            confirmButton = {
                val confirmInteraction = remember { MutableInteractionSource() }
                val isConfirmPressed by confirmInteraction.collectIsPressedAsState()
                val confirmScale by animateFloatAsState(if (isConfirmPressed) 0.92f else 1f, label = "bounce")

                TextButton(
                    onClick = {
                        if (newProfileName.isNotBlank()) {
                            ProfileManager.saveProfile(context, newProfileName)
                            newProfileName = ""
                            showCreateDialog = false
                        }
                    },
                    interactionSource = confirmInteraction,
                    modifier = Modifier.graphicsLayer(scaleX = confirmScale, scaleY = confirmScale)
                ) { Text(stringResource(R.string.prof_apply)) }
            },
            dismissButton = {
                val dismissInteraction = remember { MutableInteractionSource() }
                val isDismissPressed by dismissInteraction.collectIsPressedAsState()
                val dismissScale by animateFloatAsState(if (isDismissPressed) 0.92f else 1f, label = "bounce")

                TextButton(
                    onClick = { showCreateDialog = false },
                    interactionSource = dismissInteraction,
                    modifier = Modifier.graphicsLayer(scaleX = dismissScale, scaleY = dismissScale)
                ) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun ProfileItem(name: String, onApply: () -> Unit, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Text(text = name, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val applyInteraction = remember { MutableInteractionSource() }
                val isApplyPressed by applyInteraction.collectIsPressedAsState()
                val applyScale by animateFloatAsState(if (isApplyPressed) 0.92f else 1f, label = "bounce")

                OutlinedButton(
                    onClick = onApply,
                    interactionSource = applyInteraction,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.graphicsLayer(scaleX = applyScale, scaleY = applyScale)
                ) { 
                    Text(stringResource(R.string.prof_apply), fontSize = 11.sp) 
                }

                val deleteInteraction = remember { MutableInteractionSource() }
                val isDeletePressed by deleteInteraction.collectIsPressedAsState()
                val deleteScale by animateFloatAsState(if (isDeletePressed) 0.92f else 1f, label = "bounce")

                Button(
                    onClick = onDelete,
                    interactionSource = deleteInteraction,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.graphicsLayer(scaleX = deleteScale, scaleY = deleteScale),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { 
                    Text(stringResource(R.string.prof_delete), fontSize = 11.sp)
                }
            }
        }
    }
}
