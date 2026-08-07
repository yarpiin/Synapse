package com.af.synapse.data

import android.content.Context
import com.topjohnwu.superuser.Shell
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

object ProfileManager {
    private const val PROFILES_DIR = "profiles"
    private val gson = Gson()
    
    private val _profilesFlow = MutableStateFlow<List<String>>(emptyList())
    val profilesFlow = _profilesFlow.asStateFlow()

    fun refreshProfiles(context: Context) {
        val dir = File(context.filesDir, PROFILES_DIR)
        if (!dir.exists()) dir.mkdirs()
        _profilesFlow.value = dir.listFiles()?.map { it.nameWithoutExtension }?.sorted() ?: emptyList()
    }

    fun getProfiles(context: Context): List<String> {
        refreshProfiles(context)
        return _profilesFlow.value
    }

    private fun snapshotCurrentSettings() {
        // CPU
        CpuManager.getAvailableClusters().forEach { id ->
            SettingsStore.trackSetting("/sys/devices/system/cpu/cpufreq/policy$id/scaling_governor", CpuManager.getCurrentGovernor(id))
            SettingsStore.trackSetting("/sys/devices/system/cpu/cpufreq/policy$id/scaling_min_freq", (CpuManager.getMinFrequency(id) * 1000).toString())
            SettingsStore.trackSetting("/sys/devices/system/cpu/cpufreq/policy$id/scaling_max_freq", (CpuManager.getMaxFrequency(id) * 1000).toString())
            CpuManager.getGovernorTunables(id).forEach { t -> SettingsStore.trackSetting(t.path, t.value) }
        }

        // GPU
        if (GpuManager.getGpuPath() != null) {
            val govPath = GpuManager.getCurrentGovernorPath()
            if (!govPath.isNullOrEmpty()) SettingsStore.trackSetting(govPath, GpuManager.getCurrentGovernor())
            val minPath = GpuManager.getMinFreqPath()
            if (!minPath.isNullOrEmpty()) SettingsStore.trackSetting(minPath, (GpuManager.getMinFrequency() * 1000).toString())
            val maxPath = GpuManager.getMaxFreqPath()
            if (!maxPath.isNullOrEmpty()) SettingsStore.trackSetting(maxPath, (GpuManager.getMaxFrequency() * 1000).toString())
            GpuManager.getGovernorTunables().forEach { t -> SettingsStore.trackSetting(t.path, t.value) }
        }

        // I/O
        val block = IoManager.getInternalStorageBlock()
        SettingsStore.trackSetting("/sys/block/$block/queue/scheduler", IoManager.getCurrentScheduler(block))
        SettingsStore.trackSetting("/sys/block/$block/queue/read_ahead_kb", GenericManager.readFile("/sys/block/$block/queue/read_ahead_kb"))

        // Memory
        val vmPaths = listOf("swappiness", "vfs_cache_pressure", "dirty_ratio", "dirty_background_ratio", "extra_free_kbytes", "laptop_mode")
        vmPaths.forEach { p ->
            val path = "/proc/sys/vm/$p"
            if (GenericManager.exists(path)) SettingsStore.trackSetting(path, GenericManager.readFile(path))
        }
        
        if (GenericManager.exists("/sys/block/zram0/disksize")) {
            SettingsStore.trackSetting("/sys/block/zram0/disksize", GenericManager.readFile("/sys/block/zram0/disksize"))
            SettingsStore.trackSetting("/sys/block/zram0/comp_algorithm", GenericManager.readFile("/sys/block/zram0/comp_algorithm").substringAfter("[").substringBefore("]").trim())
        }

        // Advanced (Workqueues etc)
        if (GenericManager.exists("/sys/module/workqueue/parameters/power_efficient")) {
            SettingsStore.trackSetting("/sys/module/workqueue/parameters/power_efficient", if (GenericManager.readBool("/sys/module/workqueue/parameters/power_efficient")) "Y" else "N")
        }
    }

    fun saveProfile(context: Context, name: String) {
        val dir = File(context.filesDir, PROFILES_DIR)
        if (!dir.exists()) dir.mkdirs()
        snapshotCurrentSettings()
        
        val settings = mutableMapOf<String, String>()
        SettingsStore.getTrackedPaths().forEach { path ->
            val value = SettingsStore.getValue(path)
            if (value.isNotEmpty()) settings[path] = value
        }
        
        File(dir, "$name.json").writeText(gson.toJson(settings))
        refreshProfiles(context)
    }

    fun applyProfile(context: Context, name: String) {
        val file = File(context.filesDir, "$PROFILES_DIR/$name.json")
        if (!file.exists()) return
        
        val settings: Map<String, String> = gson.fromJson(file.readText(), object : TypeToken<Map<String, String>>() {}.type)
        val commands = mutableListOf<String>()
        
        settings.forEach { (path, value) ->
            if (path.contains("zram") && path.endsWith("disksize")) {
                Shell.cmd("swapoff /dev/block/zram0 2>/dev/null", "echo 1 > /sys/block/zram0/reset", "echo $value > $path", "mkswap /dev/block/zram0 2>/dev/null", "swapon /dev/block/zram0 2>/dev/null").exec()
            } else {
                commands.add("chmod 644 \"$path\" 2>/dev/null")
                commands.add("echo \"$value\" > \"$path\"")
            }
            SettingsStore.trackSetting(path, value)
        }
        if (commands.isNotEmpty()) Shell.cmd(*commands.toTypedArray()).exec()
    }

    fun deleteProfile(context: Context, name: String) {
        val file = File(context.filesDir, "$PROFILES_DIR/$name.json")
        if (file.exists()) file.delete()
        refreshProfiles(context)
    }
}
