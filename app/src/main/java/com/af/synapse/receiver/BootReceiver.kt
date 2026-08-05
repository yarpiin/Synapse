package com.af.synapse.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.af.synapse.R
import com.af.synapse.data.SettingsStore
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.*

class BootReceiver : BroadcastReceiver() {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == "android.intent.action.QUICKBOOT_POWERON") {
            SettingsStore.init(context)
            
            if (SettingsStore.isApplyOnBoot()) {
                val pendingResult = goAsync()
                
                scope.launch {
                    val delaySeconds = SettingsStore.getBootDelay()
                    if (delaySeconds > 0) {
                        delay(delaySeconds * 1000L)
                    }

                    // Ensure root shell is available
                    Shell.getShell { shell ->
                        if (shell.isRoot) {
                            SettingsStore.applyAllSettings()
                            showNotification(context)
                        }
                        pendingResult.finish()
                    }
                }
            }
        }
    }

    private fun showNotification(context: Context) {
        val channelId = "boot_apply"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Apply on Boot", NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(channel)
        }

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Synapse")
            .setContentText("Settings applied successfully")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        notificationManager.notify(1001, builder.build())
    }
}
