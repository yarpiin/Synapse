package com.af.synapse.data

import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File

object GpuManager {

    private val GPU_PATHS = listOf(
        "/sys/module/mali_kbase/drivers/platform:mali/1f000000.mali",
        "/sys/module/mali_kbase/drivers/platform.mali/1f000000.mali",
        "/sys/devices/platform/1f000000.mali",
        "/sys/class/kgsl/kgsl-3d0",
        "/sys/devices/platform/1c500000.mali/devfreq/1c500000.mali",
        "/sys/class/misc/mali0/device",
        "/sys/devices/platform/kgsl-3d0.0/kgsl/kgsl-3d0",
        "/sys/devices/platform/kgsl-2d0.0/kgsl/kgsl-2d0",
        "/sys/devices/platform/omap/pvrsrvkm.0/sgxfreq",
        "/sys/kernel/tegra_gpu",
        "/sys/devices/platform/dfrgx/devfreq/dfrgx",
        "/sys/kernel/gpu",
        "/sys/devices/platform/17500000.mali"
    )

    fun getGpuPath(): String? {
        for (path in GPU_PATHS) {
            if (File(path).exists()) return path
        }
        return null
    }

    fun getCurrentFrequency(): Long {
        val path = getGpuPath() ?: return 0L
        val curFreqPath = when {
            File("$path/cur_freq").exists() -> "$path/cur_freq"
            File("$path/devfreq/cur_freq").exists() -> "$path/devfreq/cur_freq"
            File("$path/clock").exists() -> "$path/clock"
            File("$path/gpuclk").exists() -> "$path/gpuclk"
            File("$path/frequency").exists() -> "$path/frequency"
            File("$path/gpu_rate").exists() -> "$path/gpu_rate"
            File("$path/gpu_clock").exists() -> "$path/gpu_clock"
            else -> null
        }
        return if (curFreqPath != null) {
            try {
                val raw = File(curFreqPath).readText().trim().toLongOrNull() ?: 0L
                formatFreq(raw)
            } catch (e: Exception) {
                val out = Shell.cmd("cat $curFreqPath").exec().out.firstOrNull()
                formatFreq(out?.toLongOrNull() ?: 0L)
            }
        } else 0L
    }

    fun getGpuFrequencyFlow() = flow {
        while (true) {
            emit(getCurrentFrequency())
            delay(1000)
        }
    }.flowOn(Dispatchers.IO)

    private fun formatFreq(raw: Long): Long {
        return when {
            raw > 100000000 -> raw / 1000000 // Hz to MHz
            raw > 100000 -> raw / 1000 // KHz to MHz
            else -> raw
        }
    }

    fun getAvailableFrequencies(): List<Long> {
        val path = getGpuPath() ?: return emptyList()
        val freqPath = when {
            File("$path/available_frequencies").exists() -> "$path/available_frequencies"
            File("$path/gpu_available_frequencies").exists() -> "$path/gpu_available_frequencies"
            File("$path/devfreq/available_frequencies").exists() -> "$path/devfreq/available_frequencies"
            File("$path/dvfs_table").exists() -> "$path/dvfs_table"
            File("$path/frequency_list").exists() -> "$path/frequency_list"
            File("$path/gpu_available_rates").exists() -> "$path/gpu_available_rates"
            File("$path/gpu_freq_table").exists() -> "$path/gpu_freq_table"
            else -> null
        }
        
        val out = if (freqPath != null) Shell.cmd("cat $freqPath").exec().out.firstOrNull() else null
        return out?.split(Regex("[\\s,]+"))?.filter { it.isNotBlank() }?.mapNotNull { 
            val raw = it.toLongOrNull() ?: return@mapNotNull null
            formatFreq(raw)
        }?.distinct()?.sorted() ?: emptyList()
    }

    fun getMinFrequency(): Long {
        val path = getGpuPath() ?: return 0L
        val minPath = when {
            File("$path/hint_min_freq").exists() -> "$path/hint_min_freq"
            File("$path/min_freq").exists() -> "$path/min_freq"
            File("$path/devfreq/min_freq").exists() -> "$path/devfreq/min_freq"
            File("$path/min_gpuclk").exists() -> "$path/min_gpuclk"
            File("$path/gpu_floor_rate").exists() -> "$path/gpu_floor_rate"
            File("$path/gpu_min_clock").exists() -> "$path/gpu_min_clock"
            else -> null
        }
        val out = if (minPath != null) Shell.cmd("cat $minPath").exec().out.firstOrNull() else null
        return formatFreq(out?.toLongOrNull() ?: 0L)
    }

    fun getMaxFrequency(): Long {
        val path = getGpuPath() ?: return 0L
        val maxPath = when {
            File("$path/hint_max_freq").exists() -> "$path/hint_max_freq"
            File("$path/max_freq").exists() -> "$path/max_freq"
            File("$path/devfreq/max_freq").exists() -> "$path/devfreq/max_freq"
            File("$path/max_gpuclk").exists() -> "$path/max_gpuclk"
            File("$path/frequency_limit").exists() -> "$path/frequency_limit"
            File("$path/gpu_cap_rate").exists() -> "$path/gpu_cap_rate"
            File("$path/gpu_max_clock").exists() -> "$path/gpu_max_clock"
            else -> null
        }
        val out = if (maxPath != null) Shell.cmd("cat $maxPath").exec().out.firstOrNull() else null
        return formatFreq(out?.toLongOrNull() ?: 0L)
    }

    fun setFrequency(freqMhz: Long, isMax: Boolean) {
        val path = getGpuPath() ?: return
        val freqPath = if (isMax) {
            when {
                File("$path/hint_max_freq").exists() -> "$path/hint_max_freq"
                File("$path/max_freq").exists() -> "$path/max_freq"
                File("$path/devfreq/max_freq").exists() -> "$path/devfreq/max_freq"
                File("$path/max_gpuclk").exists() -> "$path/max_gpuclk"
                File("$path/frequency_limit").exists() -> "$path/frequency_limit"
                File("$path/gpu_cap_rate").exists() -> "$path/gpu_cap_rate"
                File("$path/gpu_max_clock").exists() -> "$path/gpu_max_clock"
                else -> null
            }
        } else {
            when {
                File("$path/hint_min_freq").exists() -> "$path/hint_min_freq"
                File("$path/min_freq").exists() -> "$path/min_freq"
                File("$path/devfreq/min_freq").exists() -> "$path/devfreq/min_freq"
                File("$path/min_gpuclk").exists() -> "$path/min_gpuclk"
                File("$path/gpu_floor_rate").exists() -> "$path/gpu_floor_rate"
                File("$path/gpu_min_clock").exists() -> "$path/gpu_min_clock"
                else -> null
            }
        }
        if (freqPath != null) {
            val availFreqs = getAvailableFrequencies()
            val multiplier = when {
                availFreqs.any { it > 100000000 } -> 1000000
                availFreqs.any { it > 100000 } -> 1000
                else -> 1
            }
            val value = (freqMhz * multiplier).toString()
            Shell.cmd(
                "chmod 644 $freqPath 2>/dev/null",
                "echo $value > $freqPath"
            ).exec()
            SettingsStore.trackSetting(freqPath, value)
        }
    }

    fun getAvailableGovernors(): List<String> {
        val path = getGpuPath() ?: return emptyList()
        val govPath = when {
            File("$path/available_governors").exists() -> "$path/available_governors"
            File("$path/devfreq/available_governors").exists() -> "$path/available_governors"
            File("$path/governor").exists() -> "$path/governor"
            else -> null
        }
        val out = if (govPath != null) Shell.cmd("cat $govPath").exec().out.firstOrNull() else null
        return out?.split(Regex("[\\s,]+"))?.filter { it.isNotBlank() } ?: emptyList()
    }

    fun getCurrentGovernor(): String {
        val path = getGpuPath() ?: return "unknown"
        val govPath = when {
            File("$path/governor").exists() -> "$path/governor"
            File("$path/devfreq/governor").exists() -> "$path/devfreq/governor"
            File("$path/pwrscale/trustzone/governor").exists() -> "$path/pwrscale/trustzone/governor"
            File("$path/power_policy").exists() -> "$path/power_policy"
            File("$path/gpu_governor").exists() -> "$path/gpu_governor"
            else -> null
        }
        return if (govPath != null) Shell.cmd("cat $govPath").exec().out.firstOrNull() ?: "unknown" else "unknown"
    }

    fun setGovernor(gov: String) {
        val path = getGpuPath() ?: return
        val govPath = when {
            File("$path/governor").exists() -> "$path/governor"
            File("$path/devfreq/governor").exists() -> "$path/devfreq/governor"
            File("$path/pwrscale/trustzone/governor").exists() -> "$path/pwrscale/trustzone/governor"
            File("$path/power_policy").exists() -> "$path/power_policy"
            File("$path/gpu_governor").exists() -> "$path/gpu_governor"
            else -> null
        }
        if (govPath != null) {
            Shell.cmd(
                "chmod 644 $govPath 2>/dev/null",
                "echo $gov > $govPath"
            ).exec()
            SettingsStore.trackSetting(govPath, gov)
            Thread.sleep(50)
        }
    }

    fun getGovernorTunables(): List<GovernorTunable> {
        val gpuPath = getGpuPath() ?: return emptyList()
        val gov = getCurrentGovernor()
        
        val paths = listOf(
            "$gpuPath/devfreq/$gov",
            "$gpuPath/$gov"
        )
        
        var dir: File? = null
        for (p in paths) {
            val f = File(p)
            if (f.exists() && f.isDirectory) {
                dir = f
                break
            }
        }
        
        if (dir == null) return emptyList()

        return dir.listFiles()?.filter { it.isFile }?.map {
            GovernorTunable(
                name = it.name,
                value = Shell.cmd("cat ${it.absolutePath}").exec().out.firstOrNull() ?: "",
                path = it.absolutePath
            )
        } ?: emptyList()
    }

    fun isGpuBoostAvailable(): Boolean {
        val path = getGpuPath() ?: return false
        return File("$path/devfreq/adrenoboost").exists() || File("$path/adrenoboost").exists()
    }
}
