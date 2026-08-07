package com.af.synapse.data

import android.content.Context
import android.content.SharedPreferences
import com.topjohnwu.superuser.Shell

object SettingsStore {
    private const val PREFS_NAME = "applied_settings"
    private const val TRACKED_PATHS_KEY = "tracked_paths"
    private const val BOOT_PREFS = "boot_settings"
    private const val APPLY_ON_BOOT_KEY = "apply_on_boot"
    private const val BOOT_DELAY_KEY = "boot_delay"
    private const val THEME_PREF_KEY = "theme_mode"
    private const val BOOT_PROFILE_KEY = "boot_profile"
    private const val ACCENT_COLOR_KEY = "accent_color"

    private lateinit var prefs: SharedPreferences
    private lateinit var bootPrefs: SharedPreferences

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        bootPrefs = context.getSharedPreferences(BOOT_PREFS, Context.MODE_PRIVATE)
    }

    fun trackSetting(path: String, value: String) {
        prefs.edit().putString(path, value).commit()
        val tracked = getTrackedPaths().toMutableSet()
        if (tracked.add(path)) {
            prefs.edit().putStringSet(TRACKED_PATHS_KEY, tracked).commit()
        }
    }

    fun getTrackedPaths(): Set<String> = prefs.getStringSet(TRACKED_PATHS_KEY, emptySet()) ?: emptySet()

    fun getValue(path: String): String = prefs.getString(path, "") ?: ""

    fun setApplyOnBoot(enabled: Boolean) {
        bootPrefs.edit().putBoolean(APPLY_ON_BOOT_KEY, enabled).commit()
    }

    fun isApplyOnBoot(): Boolean = bootPrefs.getBoolean(APPLY_ON_BOOT_KEY, false)

    fun setBootDelay(seconds: Int) {
        bootPrefs.edit().putInt(BOOT_DELAY_KEY, seconds).commit()
    }

    fun getBootDelay(): Int = bootPrefs.getInt(BOOT_DELAY_KEY, 0)

    fun setThemeMode(mode: Int) {
        bootPrefs.edit().putInt(THEME_PREF_KEY, mode).commit()
    }

    fun getThemeMode(): Int = bootPrefs.getInt(THEME_PREF_KEY, 0)

    fun setBootProfile(name: String) {
        bootPrefs.edit().putString(BOOT_PROFILE_KEY, name).commit()
    }

    fun getBootProfile(): String = bootPrefs.getString(BOOT_PROFILE_KEY, "") ?: ""

    fun setAccentColor(color: Int) {
        bootPrefs.edit().putInt(ACCENT_COLOR_KEY, color).commit()
    }

    fun getAccentColor(): Int = bootPrefs.getInt(ACCENT_COLOR_KEY, 0xFF1A73E8.toInt())

    fun applyAllSettings() {
        val tracked = getTrackedPaths()
        if (tracked.isEmpty()) return

        val normalCommands = mutableListOf<String>()
        var zramSize: String? = null
        var zramAlgo: String? = null

        tracked.forEach { path ->
            val value = getValue(path)
            if (value.isNotEmpty()) {
                when (path) {
                    "/sys/block/zram0/disksize" -> zramSize = value
                    "/sys/block/zram0/comp_algorithm" -> zramAlgo = value
                    else -> {
                        normalCommands.add("chmod 644 $path 2>/dev/null")
                        normalCommands.add("echo \"$value\" > \"$path\"")
                    }
                }
            }
        }

        if (normalCommands.isNotEmpty()) {
            Shell.cmd(*normalCommands.toTypedArray()).exec()
        }

        // Apply Z-RAM with extreme caution during boot/init
        if (zramSize != null || zramAlgo != null) {
            val finalSize = zramSize ?: Shell.cmd("cat /sys/block/zram0/disksize").exec().out.firstOrNull() ?: "0"
            val finalAlgo = zramAlgo ?: Shell.cmd("cat /sys/block/zram0/comp_algorithm").exec().out.firstOrNull()?.let { 
                it.substringAfter("[").substringBefore("]").trim()
            } ?: "lzo"
            
            if (finalSize != "0") {
                Shell.cmd(
                    "swapoff /dev/block/zram0 2>/dev/null",
                    "echo 1 > /sys/block/zram0/reset",
                    "echo $finalAlgo > /sys/block/zram0/comp_algorithm",
                    "echo $finalSize > /sys/block/zram0/disksize",
                    "mkswap /dev/block/zram0",
                    "swapon /dev/block/zram0"
                ).exec()
            }
        }
    }
}
