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
        // Optimized for modern multi-cluster SoCs (e.g. Tensor G4)
        for (i in 0..8) {
            val path = "/sys/devices/system/cpu/cpufreq/policy$i"
            if (GenericManager.exists(path)) {
                clusters.add(i)
            }
        }
        // Fallback for older devices
        if (clusters.isEmpty() && GenericManager.exists("/sys/devices/system/cpu/cpu0/cpufreq")) {
            clusters.add(0)
        }
        return clusters
    }

    fun getClusterCpus(policyId: Int): List<Int> {
        val path = "/sys/devices/system/cpu/cpufreq/policy$policyId/affected_cpus"
        val out = Shell.cmd("cat \"$path\" 2>/dev/null").exec().out.firstOrNull()
        return if (!out.isNullOrBlank()) {
            parseCpuList(out)
        } else {
            // Hardcoded fallback for known Tensor layouts if path fails
            when(policyId) {
                0 -> listOf(0, 1, 2, 3)
                4 -> listOf(4, 5, 6)
                7 -> listOf(7)
                else -> listOf(policyId)
            }
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
                val out = Shell.cmd("cat /sys/devices/system/cpu/cpu$id/cpufreq/scaling_cur_freq 2>/dev/null").exec().out.firstOrNull()
                freqs[id] = (out?.toLongOrNull() ?: 0L) / 1000
            }
            emit(freqs)
            delay(500)
        }
    }.flowOn(Dispatchers.IO)

    fun getAvailableFrequencies(policyId: Int): List<Long> {
        val path = "/sys/devices/system/cpu/cpufreq/policy$policyId/scaling_available_frequencies"
        val out = Shell.cmd("cat \"$path\" 2>/dev/null").exec().out.firstOrNull()
        return out?.split(" ")?.filter { it.isNotBlank() }?.mapNotNull { it.toLongOrNull()?.div(1000) }?.sorted() ?: emptyList()
    }

    fun getMinFrequency(policyId: Int): Long {
        val path = "/sys/devices/system/cpu/cpufreq/policy$policyId/scaling_min_freq"
        val out = Shell.cmd("cat \"$path\" 2>/dev/null").exec().out.firstOrNull()
        return (out?.toLongOrNull() ?: 0L) / 1000
    }

    fun getMaxFrequency(policyId: Int): Long {
        val path = "/sys/devices/system/cpu/cpufreq/policy$policyId/scaling_max_freq"
        val out = Shell.cmd("cat \"$path\" 2>/dev/null").exec().out.firstOrNull()
        return (out?.toLongOrNull() ?: 0L) / 1000
    }

    fun setFrequency(policyId: Int, freqMhz: Long, isMax: Boolean) {
        val fileName = if (isMax) "scaling_max_freq" else "scaling_min_freq"
        val path = "/sys/devices/system/cpu/cpufreq/policy$policyId/$fileName"
        val value = (freqMhz * 1000).toString()
        
        Shell.cmd(
            "chmod 644 \"$path\" 2>/dev/null",
            "echo \"$value\" > \"$path\""
        ).exec()
        SettingsStore.trackSetting(path, value)
    }

    fun getAvailableGovernors(policyId: Int): List<String> {
        val path = "/sys/devices/system/cpu/cpufreq/policy$policyId/scaling_available_governors"
        val out = Shell.cmd("cat \"$path\" 2>/dev/null").exec().out.firstOrNull()
        return out?.split(" ")?.filter { it.isNotBlank() } ?: listOf("schedutil")
    }

    fun getCurrentGovernor(policyId: Int): String {
        val path = "/sys/devices/system/cpu/cpufreq/policy$policyId/scaling_governor"
        return Shell.cmd("cat \"$path\" 2>/dev/null").exec().out.firstOrNull() ?: "unknown"
    }

    fun setGovernor(policyId: Int, governor: String) {
        val path = "/sys/devices/system/cpu/cpufreq/policy$policyId/scaling_governor"
        Shell.cmd(
            "chmod 644 \"$path\" 2>/dev/null",
            "echo \"$governor\" > \"$path\""
        ).exec()
        SettingsStore.trackSetting(path, governor)
    }

    fun getGovernorTunables(policyId: Int): List<GovernorTunable> {
        val gov = getCurrentGovernor(policyId)
        val path = "/sys/devices/system/cpu/cpufreq/policy$policyId/$gov"
        
        if (!GenericManager.isDirectory(path)) return emptyList()

        val files = Shell.cmd("ls \"$path\"").exec().out
        val exclude = listOf("boostpulse", "cpu_utilization", "multi_phase_freq_tbl", "profile", "profile_list", "up_threshold_h", "up_threshold_l", "version_profiles")
        
        // Safety filter for sched_pixel to prevent screen-off issues
        val whitelist = if (gov.contains("sched_pixel", true)) {
            listOf("down_rate_limit_scale_pow", "down_rate_limit_ua", "up_rate_limit_us", "down_rate_limit_us")
        } else null

        return files.filter { fileName ->
            fileName.isNotBlank() &&
            !exclude.contains(fileName) && 
            (whitelist == null || whitelist.contains(fileName))
        }.map { fileName ->
            val fullPath = "$path/$fileName"
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
        return paths.any { GenericManager.exists(it) }
    }
}

data class GovernorTunable(val name: String, var value: String, val path: String)
