package com.af.synapse.ui.screens

import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.af.synapse.R
import com.af.synapse.data.UpdateManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun AboutScreen() {
    val scrollState = rememberScrollState()
    val uriHandler = LocalUriHandler.current
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var isChecking by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Main App Info Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.about_main_desc),
                    fontSize = 15.sp,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = stringResource(R.string.about_inspired),
                    fontSize = 13.sp,
                    fontStyle = FontStyle.Italic,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = stringResource(R.string.about_ai),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )
            }
        }

        // Updater Section
        OutlinedButton(
            onClick = {
                if (!isChecking) {
                    isChecking = true
                    scope.launch {
                        val update = UpdateManager.checkForUpdates(context)
                        isChecking = false
                        if (update != null) {
                            if (update.isNewer) {
                                // Dialog is already handled in MainActivity if we use a shared state, 
                                // but for manual check, we can show a Toast or open the page.
                                UpdateManager.openDownload(context, update.releaseUrl)
                            } else {
                                Toast.makeText(context, "Aplikacja jest aktualna", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(context, "Nie udało się sprawdzić aktualizacji", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp)
        ) {
            if (isChecking) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
            } else {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Sprawdź aktualizacje")
            }
        }

        // Credits Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                AboutSection(title = "Original Creators") {
                    CreditItem("apb_axel", "http://forum.xda-developers.com/member.php?u=5658634")
                    CreditItem("AndreiLux", "http://forum.xda-developers.com/member.php?u=4167023")
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))

                AboutSection(title = "Special Thanks") {
                    CreditItem("ak", "http://forum.xda-developers.com/member.php?u=3685904")
                    CreditItem("osm0sis", "http://forum.xda-developers.com/member.php?u=4544860")
                    CreditItem("eng.stk", "http://forum.xda-developers.com/member.php?u=3873953")
                }
            }
        }

        // Developer Info & Support Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.about_dev),
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                
                val xdaInteraction = remember { MutableInteractionSource() }
                val isXdaPressed by xdaInteraction.collectIsPressedAsState()
                val xdaScale by animateFloatAsState(if (isXdaPressed) 0.96f else 1f, label = "bounce")

                OutlinedButton(
                    onClick = { uriHandler.openUri("https://xdaforums.com/m/yarpiin.5288056/") },
                    modifier = Modifier.fillMaxWidth().graphicsLayer(scaleX = xdaScale, scaleY = xdaScale),
                    shape = RoundedCornerShape(14.dp),
                    interactionSource = xdaInteraction
                ) {
                    Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("XDA Forums")
                }

                val tgInteraction = remember { MutableInteractionSource() }
                val isTgPressed by tgInteraction.collectIsPressedAsState()
                val tgScale by animateFloatAsState(if (isTgPressed) 0.96f else 1f, label = "bounce")

                OutlinedButton(
                    onClick = { uriHandler.openUri("https://t.me/+Uxz6juveqO3DP7ii") },
                    modifier = Modifier.fillMaxWidth().graphicsLayer(scaleX = tgScale, scaleY = tgScale),
                    shape = RoundedCornerShape(14.dp),
                    interactionSource = tgInteraction
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Telegram")
                }

                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = stringResource(R.string.about_support),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontStyle = FontStyle.Italic
                )
                
                val donateInteraction = remember { MutableInteractionSource() }
                val isDonatePressed by donateInteraction.collectIsPressedAsState()
                val donateScale by animateFloatAsState(if (isDonatePressed) 0.96f else 1f, label = "bounce")

                Button(
                    onClick = { uriHandler.openUri("https://paypal.me/yarpiin") },
                    modifier = Modifier.fillMaxWidth().graphicsLayer(scaleX = donateScale, scaleY = donateScale),
                    shape = RoundedCornerShape(14.dp),
                    interactionSource = donateInteraction,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.Favorite, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.about_donate))
                }
            }
        }

        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Composable
fun AboutSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = title,
            fontSize = 12.sp,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = 1.sp,
            textAlign = TextAlign.Center
        )
        content()
    }
}

@Composable
fun CreditItem(name: String, url: String) {
    val uriHandler = LocalUriHandler.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.92f else 1f, label = "bounce")

    TextButton(
        onClick = { uriHandler.openUri(url) },
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        modifier = Modifier.height(36.dp).graphicsLayer(scaleX = scale, scaleY = scale),
        interactionSource = interactionSource
    ) {
        Text(
            text = name,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
