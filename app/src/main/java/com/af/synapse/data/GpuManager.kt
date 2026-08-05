package com.af.synapse.data

import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File

object GpuManager {

    private val GPU_PATHS = listOf(
        "/sys/module/mali_kbase/drivers/platform:mali/1f000000.mali", // User specific
        "/sys/module/mali_kbase/drivers/platform.mali/1f000000.mali", // Alternative
        "/sys/devices/platform/1f000000.mali", // Common devfreq
        "/sys/class/kgsl/kgsl-3d0", // Adreno
        "/sys/devices/platform/1c500000.mali/devfreq/1c500000.mali", // Pixel 6/7/8/9 Mali
        "/sys/class/misc/mali0/device"
    )

    fun getGpuPath(): String? {
        for (path in GPU_PATHS) {
            if (File(path).exists()) return path
        }
        return null
    }

    fun getGpuFrequencyFlow() = flow {
        val path = getGpuPath()
        val curFreqPath = if (path != null) {
            when {
                File("$path/cur_freq").exists() -> "$path/cur_freq"
                File("$path/devfreq/cur_freq").exists() -> "$path/devfreq/cur_freq"
                File("$path/clock").exists() -> "$path/clock"
                else -> null
            }
        } else null

        while (true) {
            val freq = if (curFreqPath != null) {
                try {
                    val raw = File(curFreqPath).readText().trim().toLongOrNull() ?: 0L
                    formatFreq(raw)
                } catch (e: Exception) {
                    val out = Shell.cmd("cat $curFreqPath").exec().out.firstOrNull()
                    formatFreq(out?.toLongOrNull() ?: 0L)
                }
            } else 0L
            
            emit(freq)
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
            else -> null
        }
        
        val out = if (freqPath != null) Shell.cmd("cat $freqPath").exec().out.firstOrNull() else null
        return out?.split(" ")?.filter { it.isNotBlank() }?.mapNotNull { 
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
                else -> null
            }
        } else {
            when {
                File("$path/hint_min_freq").exists() -> "$path/hint_min_freq"
                File("$path/min_freq").exists() -> "$path/min_freq"
                File("$path/devfreq/min_freq").exists() -> "$path/devfreq/min_freq"
                File("$path/min_gpuclk").exists() -> "$path/min_gpuclk"
                else -> null
            }
        }
        if (freqPath != null) {
            val rawAvail = Shell.cmd("cat ${path}/available_frequencies").exec().out.firstOrNull() 
                ?: Shell.cmd("cat ${path}/gpu_available_frequencies").exec().out.firstOrNull()
                ?: "0"
            val multiplier = when {
                rawAvail.length > 10 -> 1000000 // Hz
                rawAvail.length > 7 -> 1000 // KHz
                else -> 1 // MHz
            }
            val value = (freqMhz * multiplier).toString()
            Shell.cmd("echo $value > $freqPath").exec()
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
        return out?.split(" ")?.filter { it.isNotBlank() } ?: emptyList()
    }

    fun getCurrentGovernor(): String {
        val path = getGpuPath() ?: return "unknown"
        val govPath = when {
            File("$path/governor").exists() -> "$path/governor"
            File("$path/devfreq/governor").exists() -> "$path/devfreq/governor"
            else -> null
        }
        return if (govPath != null) Shell.cmd("cat $govPath").exec().out.firstOrNull() ?: "unknown" else "unknown"
    }

    fun setGovernor(gov: String) {
        val path = getGpuPath() ?: return
        val govPath = when {
            File("$path/governor").exists() -> "$path/governor"
            File("$path/devfreq/governor").exists() -> "$path/devfreq/governor"
            else -> null
        }
        if (govPath != null) {
            Shell.cmd("echo $gov > $govPath").exec()
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
