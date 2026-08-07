package com.af.synapse.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.af.synapse.R
import com.af.synapse.data.ProfileManager
import com.af.synapse.data.SettingsStore
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.*

class BootReceiver : BroadcastReceiver() {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val NOTIFICATION_ID = 1001
    private val CHANNEL_ID = "synapse_boot_channel_v3"

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == Intent.ACTION_BOOT_COMPLETED || 
            action == Intent.ACTION_LOCKED_BOOT_COMPLETED || 
            action == "android.intent.action.QUICKBOOT_POWERON") {
            
            SettingsStore.init(context)
            if (!SettingsStore.isApplyOnBoot()) return

            val pendingResult = goAsync()
            
            scope.launch {
                val delaySeconds = SettingsStore.getBootDelay()
                val bootProfile = SettingsStore.getBootProfile()

                if (delaySeconds > 0) {
                    showNotification(context, "Synapse - Boot", "Oczekiwanie $delaySeconds s na zastosowanie ustawień...", true)
                    delay(delaySeconds * 1000L)
                } else {
                    showNotification(context, "Synapse - Boot", "Inicjalizacja profilu startowego...", true)
                }

                // Ensure root shell is available
                Shell.getShell { shell ->
                    if (shell.isRoot) {
                        if (bootProfile.isNotEmpty()) {
                            ProfileManager.applyProfile(context, bootProfile)
                            showNotification(context, "Synapse - Boot", "Zastosowano profil: $bootProfile", false)
                        } else {
                            showNotification(context, "Synapse - Boot", "Profil nie wybrany - wczytano domyślne ustawienia", false)
                        }
                    } else {
                        showNotification(context, "Synapse - Boot", "Błąd: Brak dostępu Root przy starcie!", false)
                    }
                    pendingResult.finish()
                }
            }
        }
    }

    private fun showNotification(context: Context, title: String, message: String, ongoing: Boolean) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Use IMPORTANCE_HIGH to ensure sound/heads-up
            val channel = NotificationChannel(CHANNEL_ID, "Synapse Boot Status", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Informuje o statusie stosowania ustawień przy starcie systemu"
                enableVibration(true)
                enableLights(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH) // High priority for sound
            .setOngoing(ongoing)
            .setAutoCancel(!ongoing)
            .setSound(soundUri)
            .setDefaults(NotificationCompat.DEFAULT_ALL)

        notificationManager.notify(NOTIFICATION_ID, builder.build())
    }
}
