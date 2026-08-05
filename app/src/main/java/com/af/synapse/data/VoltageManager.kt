package com.af.synapse.data

import com.topjohnwu.superuser.Shell
import java.io.File

object VoltageManager {

    data class VoltageEntry(
        val label: String,
        val originalValue: String,
        val frequency: String,
        val voltage: Int,
        val unit: String
    )

    fun isVoltageAvailable(): Boolean {
        return File("/sys/devices/system/cpu/cpufreq/vdd_table/vdd_levels").exists() ||
               File("/sys/devices/system/cpu/cpu0/cpufreq/UV_mV_table").exists() ||
               File("/sys/devices/virtual/misc/customvoltage/core_voltages").exists() ||
               File("/sys/module/acpuclock_krait/parameters/boost").exists()
    }

    fun getVddLevels(): List<VoltageEntry> {
        val path = "/sys/devices/system/cpu/cpufreq/vdd_table/vdd_levels"
        if (!File(path).exists()) return emptyList()
        
        val lines = Shell.cmd("cat $path").exec().out
        return lines.mapNotNull { line ->
            val parts = line.split(Regex("[:\\s]+"))
            if (parts.size >= 2) {
                val freq = parts[0]
                val volt = parts[1].toIntOrNull() ?: 0
                VoltageEntry(
                    label = "${freq.toLong() / 1000} MHz",
                    originalValue = line,
                    frequency = freq,
                    voltage = volt,
                    unit = "uV"
                )
            } else null
        }
    }

    fun setVddVoltage(frequency: String, voltage: Int) {
        val path = "/sys/devices/system/cpu/cpufreq/vdd_table/vdd_levels"
        Shell.cmd(
            "chmod 644 $path 2>/dev/null",
            "echo \"$frequency $voltage\" > $path"
        ).exec()
        SettingsStore.trackSetting("cpuvolt_$frequency", "$frequency $voltage")
    }

    fun getUvMvTable(): List<VoltageEntry> {
        val path = "/sys/devices/system/cpu/cpu0/cpufreq/UV_mV_table"
        if (!File(path).exists()) return emptyList()
        
        val lines = Shell.cmd("cat $path").exec().out
        return lines.mapNotNull { line ->
            val parts = line.trim().split(Regex("[:\\s]+"))
            if (parts.size >= 2) {
                val freqLabel = parts[0].replace("mhz", "", true)
                val volt = parts[1].toIntOrNull() ?: 0
                val unit = parts.getOrNull(2) ?: "mV"
                VoltageEntry(
                    label = "$freqLabel MHz",
                    originalValue = line,
                    frequency = parts[0],
                    voltage = volt,
                    unit = unit
                )
            } else null
        }
    }

    fun setUvMvVoltage(frequency: String, voltage: Int) {
        val path = "/sys/devices/system/cpu/cpu0/cpufreq/UV_mV_table"
        Shell.cmd(
            "chmod 644 $path 2>/dev/null",
            "echo \"$frequency $voltage\" > $path"
        ).exec()
        SettingsStore.trackSetting("uvmv_$frequency", "$frequency $voltage")
    }

    fun getCustomVoltages(path: String): List<VoltageEntry> {
        if (!File(path).exists()) return emptyList()
        val lines = Shell.cmd("cat $path").exec().out
        return lines.mapIndexed { index, line ->
            val parts = line.trim().split(Regex("\\s+"))
            val volt = parts[0].toIntOrNull() ?: 0
            val unit = parts.getOrNull(1) ?: "mV"
            VoltageEntry(
                label = "Voltage ${index + 1}",
                originalValue = line,
                frequency = index.toString(),
                voltage = volt,
                unit = unit
            )
        }
    }
}
