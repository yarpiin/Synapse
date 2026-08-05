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

    private lateinit var prefs: SharedPreferences
    private lateinit var bootPrefs: SharedPreferences

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        bootPrefs = context.getSharedPreferences(BOOT_PREFS, Context.MODE_PRIVATE)
    }

    fun trackSetting(path: String, value: String) {
        prefs.edit().putString(path, value).apply()
        val tracked = getTrackedPaths().toMutableSet()
        if (tracked.add(path)) {
            prefs.edit().putStringSet(TRACKED_PATHS_KEY, tracked).apply()
        }
    }

    fun getTrackedPaths(): Set<String> = prefs.getStringSet(TRACKED_PATHS_KEY, emptySet()) ?: emptySet()

    fun getValue(path: String): String = prefs.getString(path, "") ?: ""

    fun setApplyOnBoot(enabled: Boolean) {
        bootPrefs.edit().putBoolean(APPLY_ON_BOOT_KEY, enabled).apply()
    }

    fun isApplyOnBoot(): Boolean = bootPrefs.getBoolean(APPLY_ON_BOOT_KEY, false)

    fun setBootDelay(seconds: Int) {
        bootPrefs.edit().putInt(BOOT_DELAY_KEY, seconds).apply()
    }

    fun getBootDelay(): Int = bootPrefs.getInt(BOOT_DELAY_KEY, 0)

    fun setThemeMode(mode: Int) {
        bootPrefs.edit().putInt(THEME_PREF_KEY, mode).apply()
    }

    fun getThemeMode(): Int = bootPrefs.getInt(THEME_PREF_KEY, 0) // 0: Auto, 1: Light, 2: Dark

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
                    else -> normalCommands.add("echo $value > $path")
                }
            }
        }

        // Apply normal settings first
        if (normalCommands.isNotEmpty()) {
            Shell.cmd(*normalCommands.toTypedArray()).exec()
        }

        // Apply Z-RAM if tracked
        if (zramSize != null || zramAlgo != null) {
            val zCmd = mutableListOf("swapoff /dev/block/zram0", "echo 1 > /sys/block/zram0/reset")
            zramAlgo?.let { zCmd.add("echo $it > /sys/block/zram0/comp_algorithm") }
            zramSize?.let { zCmd.add("echo $it > /sys/block/zram0/disksize") }
            zCmd.add("mkswap /dev/block/zram0")
            zCmd.add("swapon /dev/block/zram0")
            Shell.cmd(*zCmd.toTypedArray()).exec()
        }
    }
}
