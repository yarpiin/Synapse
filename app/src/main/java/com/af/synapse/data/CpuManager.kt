package com.af.synapse.data

import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File

object CpuManager {

    fun getAvailableClusters(): List<Int> {
        val clusters = mutableListOf<Int>()
        var i = 0
        while (i < 8) { // Check up to 8 policies (typical max)
            if (File("/sys/devices/system/cpu/cpufreq/policy$i").exists()) {
                clusters.add(i)
            } else if (i == 0 && File("/sys/devices/system/cpu/cpu0/cpufreq").exists()) {
                // Some older kernels might not use 'policyX' naming but have cpufreq
                if (clusters.isEmpty()) clusters.add(0)
            }
            i++
        }
        return clusters
    }

    fun getClusterCpus(policyIndex: Int): List<Int> {
        val policyPath = "/sys/devices/system/cpu/cpufreq/policy$policyIndex/affected_cpus"
        val cpuPath = "/sys/devices/system/cpu/cpu$policyIndex/cpufreq/affected_cpus"
        
        val out = Shell.cmd("cat $policyPath").exec().out.firstOrNull()
            ?: Shell.cmd("cat $cpuPath").exec().out.firstOrNull()
            
        return if (out != null) {
            parseCpuList(out)
        } else {
            // Fallback for devices without clearly defined affected_cpus
            if (policyIndex == 0) {
                val allCpus = Shell.cmd("ls /sys/devices/system/cpu/ | grep -E 'cpu[0-9]+'").exec().out
                allCpus.mapNotNull { it.removePrefix("cpu").toIntOrNull() }.sorted()
            } else emptyList()
        }
    }

    private fun parseCpuList(list: String): List<Int> {
        val result = mutableListOf<Int>()
        // handle space or comma separated lists
        val parts = list.split(Regex("[\\s,]+"))
        for (part in parts) {
            if (part.contains("-")) {
                val range = part.split("-")
                val start = range[0].toIntOrNull() ?: continue
                val end = range[1].toIntOrNull() ?: continue
                for (i in start..end) result.add(i)
            } else if (part.isNotBlank()) {
                part.toIntOrNull()?.let { result.add(it) }
            }
        }
        return result.distinct().sorted()
    }

    fun getCpuFrequencyFlow(cpuIds: List<Int>) = flow {
        while (true) {
            val freqs = mutableMapOf<Int, Long>()
            for (id in cpuIds) {
                // Direct file reading is MUCH faster than shell execution
                try {
                    val file = File("/sys/devices/system/cpu/cpu$id/cpufreq/scaling_cur_freq")
                    if (file.exists()) {
                        val freq = file.readText().trim().toLongOrNull() ?: 0L
                        freqs[id] = freq / 1000
                    }
                } catch (e: Exception) {
                    // Fallback to shell if direct read fails
                    val out = Shell.cmd("cat /sys/devices/system/cpu/cpu$id/cpufreq/scaling_cur_freq 2>/dev/null").exec().out.firstOrNull()
                    freqs[id] = (out?.toLongOrNull() ?: 0L) / 1000
                }
            }
            emit(freqs)
            delay(500) // 500ms for live-like experience
        }
    }.flowOn(Dispatchers.IO)

    fun getCurrentFrequencies(cpuIds: List<Int>): Map<Int, Long> {
        val freqs = mutableMapOf<Int, Long>()
        for (id in cpuIds) {
            try {
                val file = File("/sys/devices/system/cpu/cpu$id/cpufreq/scaling_cur_freq")
                if (file.exists()) {
                    val freq = file.readText().trim().toLongOrNull() ?: 0L
                    freqs[id] = freq / 1000
                    continue
                }
            } catch (e: Exception) {}
            
            val out = Shell.cmd("cat /sys/devices/system/cpu/cpu$id/cpufreq/scaling_cur_freq 2>/dev/null").exec().out.firstOrNull()
            freqs[id] = (out?.toLongOrNull() ?: 0L) / 1000
        }
        return freqs
    }

    fun getAvailableFrequencies(cpuId: Int): List<Long> {
        val out = Shell.cmd("cat /sys/devices/system/cpu/cpu$cpuId/cpufreq/scaling_available_frequencies").exec().out.firstOrNull()
        return out?.split(" ")?.filter { it.isNotBlank() }?.mapNotNull { it.toLongOrNull()?.div(1000) }?.sorted() ?: emptyList()
    }

    fun getMinFrequency(cpuId: Int): Long {
        val out = Shell.cmd("cat /sys/devices/system/cpu/cpu$cpuId/cpufreq/scaling_min_freq").exec().out.firstOrNull()
        return (out?.toLongOrNull() ?: 0L) / 1000
    }

    fun getMaxFrequency(cpuId: Int): Long {
        val out = Shell.cmd("cat /sys/devices/system/cpu/cpu$cpuId/cpufreq/scaling_max_freq").exec().out.firstOrNull()
        return (out?.toLongOrNull() ?: 0L) / 1000
    }

    fun setFrequency(cpuIds: List<Int>, freqMhz: Long, isMax: Boolean) {
        val path = if (isMax) "scaling_max_freq" else "scaling_min_freq"
        val value = (freqMhz * 1000).toString()
        val cmd = StringBuilder()
        for (id in cpuIds) {
            val fullPath = "/sys/devices/system/cpu/cpu$id/cpufreq/$path"
            cmd.append("echo $value > $fullPath; ")
            SettingsStore.trackSetting(fullPath, value)
        }
        Shell.cmd(cmd.toString()).exec()
    }

    fun getAvailableGovernors(cpuId: Int): List<String> {
        val out = Shell.cmd("cat /sys/devices/system/cpu/cpu$cpuId/cpufreq/scaling_available_governors").exec().out.firstOrNull()
        return out?.split(" ")?.filter { it.isNotBlank() } ?: listOf("schedutil")
    }

    fun getCurrentGovernor(cpuId: Int): String {
        return Shell.cmd("cat /sys/devices/system/cpu/cpu$cpuId/cpufreq/scaling_governor").exec().out.firstOrNull() ?: "unknown"
    }

    fun setGovernor(cpuIds: List<Int>, governor: String) {
        val cmd = StringBuilder()
        for (id in cpuIds) {
            val fullPath = "/sys/devices/system/cpu/cpu$id/cpufreq/scaling_governor"
            cmd.append("echo $governor > $fullPath; ")
            SettingsStore.trackSetting(fullPath, governor)
        }
        Shell.cmd(cmd.toString()).exec()
        Thread.sleep(50)
    }

    fun getGovernorTunables(cpuId: Int): List<GovernorTunable> {
        val gov = getCurrentGovernor(cpuId)
        // Check multiple possible paths for tunables
        val paths = listOf(
            "/sys/devices/system/cpu/cpufreq/policy$cpuId/$gov", // Per-policy
            "/sys/devices/system/cpu/cpufreq/$gov",            // Global
            "/sys/devices/system/cpu/cpu$cpuId/cpufreq/$gov"   // Per-cpu
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

        val exclude = listOf("boostpulse", "cpu_utilization", "multi_phase_freq_tbl", "profile", "profile_list", "up_threshold_h", "up_threshold_l", "version_profiles")

        return dir.listFiles()?.filter { it.isFile && !exclude.contains(it.name) }?.map {
            GovernorTunable(
                name = it.name,
                value = Shell.cmd("cat ${it.absolutePath}").exec().out.firstOrNull() ?: "",
                path = it.absolutePath
            )
        } ?: emptyList()
    }

    fun setTunable(path: String, value: String) {
        Shell.cmd("echo $value > $path").exec()
        SettingsStore.trackSetting(path, value)
    }

    fun isCpuBoostAvailable(): Boolean {
        val paths = listOf(
            "/sys/module/cpu_boost",
            "/sys/kernel/cpu_input_boost",
            "/sys/module/cpu_input_boost",
            "/sys/kernel/fp_boost",
            "/sys/class/kgsl/kgsl-3d0/devfreq/adrenoboost"
        )
        for (path in paths) {
            if (File(path).exists()) return true
        }
        // Fallback check via shell if needed, but File().exists() usually works for these sysfs nodes
        return false
    }
}

data class GovernorTunable(val name: String, var value: String, val path: String)
