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
        while (i < 8) {
            if (GenericManager.exists("/sys/devices/system/cpu/cpufreq/policy$i")) {
                clusters.add(i)
            } else if (i == 0 && GenericManager.exists("/sys/devices/system/cpu/cpu0/cpufreq")) {
                if (clusters.isEmpty()) clusters.add(0)
            }
            i++
        }
        return clusters
    }

    fun getClusterCpus(policyIndex: Int): List<Int> {
        val policyPath = "/sys/devices/system/cpu/cpufreq/policy$policyIndex/affected_cpus"
        val cpuPath = "/sys/devices/system/cpu/cpu$policyIndex/cpufreq/affected_cpus"
        
        val out = Shell.cmd("cat $policyPath 2>/dev/null").exec().out.firstOrNull()
            ?: Shell.cmd("cat $cpuPath 2>/dev/null").exec().out.firstOrNull()
            
        return if (out != null) {
            parseCpuList(out)
        } else {
            if (policyIndex == 0) {
                val allCpus = Shell.cmd("ls /sys/devices/system/cpu/ | grep -E 'cpu[0-9]+'").exec().out
                allCpus.mapNotNull { it.removePrefix("cpu").toIntOrNull() }.sorted()
            } else emptyList()
        }
    }

    private fun parseCpuList(list: String): List<Int> {
        val result = mutableListOf<Int>()
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
                try {
                    val out = Shell.cmd("cat /sys/devices/system/cpu/cpu$id/cpufreq/scaling_cur_freq 2>/dev/null").exec().out.firstOrNull()
                    freqs[id] = (out?.toLongOrNull() ?: 0L) / 1000
                } catch (e: Exception) {
                    freqs[id] = 0L
                }
            }
            emit(freqs)
            delay(500)
        }
    }.flowOn(Dispatchers.IO)

    fun getCurrentFrequencies(cpuIds: List<Int>): Map<Int, Long> {
        val freqs = mutableMapOf<Int, Long>()
        for (id in cpuIds) {
            val out = Shell.cmd("cat /sys/devices/system/cpu/cpu$id/cpufreq/scaling_cur_freq 2>/dev/null").exec().out.firstOrNull()
            freqs[id] = (out?.toLongOrNull() ?: 0L) / 1000
        }
        return freqs
    }

    fun getAvailableFrequencies(cpuId: Int): List<Long> {
        val out = Shell.cmd("cat /sys/devices/system/cpu/cpu$cpuId/cpufreq/scaling_available_frequencies 2>/dev/null").exec().out.firstOrNull()
        return out?.split(" ")?.filter { it.isNotBlank() }?.mapNotNull { it.toLongOrNull()?.div(1000) }?.sorted() ?: emptyList()
    }

    fun getMinFrequency(cpuId: Int): Long {
        val out = Shell.cmd("cat /sys/devices/system/cpu/cpu$cpuId/cpufreq/scaling_min_freq 2>/dev/null").exec().out.firstOrNull()
        return (out?.toLongOrNull() ?: 0L) / 1000
    }

    fun getMaxFrequency(cpuId: Int): Long {
        val out = Shell.cmd("cat /sys/devices/system/cpu/cpu$cpuId/cpufreq/scaling_max_freq 2>/dev/null").exec().out.firstOrNull()
        return (out?.toLongOrNull() ?: 0L) / 1000
    }

    fun setFrequency(cpuIds: List<Int>, freqMhz: Long, isMax: Boolean) {
        val path = if (isMax) "scaling_max_freq" else "scaling_min_freq"
        val value = (freqMhz * 1000).toString()
        val commands = mutableListOf<String>()
        for (id in cpuIds) {
            val fullPath = "/sys/devices/system/cpu/cpu$id/cpufreq/$path"
            commands.add("chmod 644 $fullPath 2>/dev/null")
            commands.add("echo \"$value\" > \"$fullPath\"")
            SettingsStore.trackSetting(fullPath, value)
        }
        Shell.cmd(*commands.toTypedArray()).exec()
    }

    fun getAvailableGovernors(cpuId: Int): List<String> {
        val out = Shell.cmd("cat /sys/devices/system/cpu/cpu$cpuId/cpufreq/scaling_available_governors 2>/dev/null").exec().out.firstOrNull()
        return out?.split(" ")?.filter { it.isNotBlank() } ?: listOf("schedutil")
    }

    fun getCurrentGovernor(cpuId: Int): String {
        return Shell.cmd("cat /sys/devices/system/cpu/cpu$cpuId/cpufreq/scaling_governor 2>/dev/null").exec().out.firstOrNull() ?: "unknown"
    }

    fun setGovernor(cpuIds: List<Int>, governor: String) {
        val commands = mutableListOf<String>()
        for (id in cpuIds) {
            val fullPath = "/sys/devices/system/cpu/cpu$id/cpufreq/scaling_governor"
            commands.add("chmod 644 $fullPath 2>/dev/null")
            commands.add("echo \"$governor\" > \"$fullPath\"")
            SettingsStore.trackSetting(fullPath, governor)
        }
        Shell.cmd(*commands.toTypedArray()).exec()
        Thread.sleep(50)
    }

    fun getGovernorTunables(cpuId: Int): List<GovernorTunable> {
        val gov = getCurrentGovernor(cpuId)
        val paths = listOf(
            "/sys/devices/system/cpu/cpufreq/policy$cpuId/$gov",
            "/sys/devices/system/cpu/cpufreq/$gov",
            "/sys/devices/system/cpu/cpu$cpuId/cpufreq/$gov"
        )
        
        var dirPath: String? = null
        for (p in paths) {
            if (GenericManager.isDirectory(p)) {
                dirPath = p
                break
            }
        }

        if (dirPath == null) return emptyList()

        val files = Shell.cmd("ls $dirPath").exec().out
        val exclude = listOf("boostpulse", "cpu_utilization", "multi_phase_freq_tbl", "profile", "profile_list", "up_threshold_h", "up_threshold_l", "version_profiles")

        return files.filter { it.isNotBlank() && !exclude.contains(it) }.map { fileName ->
            val fullPath = "$dirPath/$fileName"
            GovernorTunable(
                name = fileName,
                value = Shell.cmd("cat \"$fullPath\"").exec().out.firstOrNull() ?: "",
                path = fullPath
            )
        }
    }

    fun setTunable(path: String, value: String) {
        Shell.cmd(
            "chmod 644 \"$path\" 2>/dev/null",
            "echo \"$value\" > \"$path\""
        ).exec()
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
            if (GenericManager.exists(path)) return true
        }
        return false
    }
}

data class GovernorTunable(val name: String, var value: String, val path: String)
