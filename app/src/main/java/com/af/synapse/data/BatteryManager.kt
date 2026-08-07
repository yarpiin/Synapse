package com.af.synapse.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File
import java.util.Locale

object BatteryManager {

    data class BatteryStats(
        val level: Int,
        val temperature: Float,
        val voltageV: Float,
        val status: String,
        val health: String,
        val currentMa: Long,
        val capacityDesign: Long,
        val capacityActual: Long,
        val cycleCount: Int,
        val powerW: Float,
        val timeToFull: String,
        val isCharging: Boolean
    )

    private fun getBatteryPath(): String {
        val paths = listOf("/sys/class/power_supply/battery", "/sys/class/power_supply/google_battery")
        return paths.find { GenericManager.exists(it) } ?: "/sys/class/power_supply/battery"
    }

    fun getBatteryStats(): BatteryStats {
        val path = getBatteryPath()
        
        val level = GenericManager.readFile("$path/capacity").toIntOrNull() ?: 0
        val temp = (GenericManager.readFile("$path/temp").toFloatOrNull() ?: 0f) / 10f
        
        val rawVolt = GenericManager.readFile("$path/voltage_now").toFloatOrNull() ?: 0f
        val voltV = when {
            rawVolt > 1000000 -> rawVolt / 1000000f
            rawVolt > 1000 -> rawVolt / 1000f
            else -> rawVolt
        }
        
        val status = GenericManager.readFile("$path/status").trim()
        val isCharging = status.equals("Charging", true) || status.equals("Full", true)
        val health = GenericManager.readFile("$path/health").trim().ifBlank { "Good" }
        
        val rawCurrent = GenericManager.readFile("$path/current_now").toLongOrNull() ?: 0L
        val currentMa = when {
            Math.abs(rawCurrent) > 50000 -> rawCurrent / 1000
            else -> rawCurrent
        }
        
        val capDesign = GenericManager.readFile("$path/charge_full_design").toLongOrNull() ?: 0L
        val capActual = GenericManager.readFile("$path/charge_full").toLongOrNull() ?: 0L
        val cycles = GenericManager.readFile("$path/cycle_count").toIntOrNull() ?: 0

        // Only calculate power (W) when charging, otherwise 0.0W
        val powerW = if (isCharging) {
            (Math.abs(currentMa) * voltV) / 1000f
        } else {
            0.0f
        }

        val rawSeconds = GenericManager.readFile("$path/time_to_full_now").toLongOrNull() ?: 0L
        val timeToFull = if (isCharging && rawSeconds > 0 && rawSeconds < 20000) {
            val hours = rawSeconds / 3600
            val minutes = (rawSeconds % 3600) / 60
            if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
        } else ""

        return BatteryStats(
            level, temp, voltV, status, health, currentMa,
            capDesign / 1000, capActual / 1000, cycles,
            powerW, timeToFull, isCharging
        )
    }

    fun getBatteryStatsFlow() = flow {
        while (true) {
            emit(getBatteryStats())
            delay(5000)
        }
    }.flowOn(Dispatchers.IO)

    fun isFastChargeAvailable(): Boolean {
        return File("/sys/kernel/fast_charge/force_fast_charge").exists() ||
               File("/sys/module/msm_otg/parameters/fast_charge").exists()
    }

    fun getFastChargePath(): String? {
        return when {
            File("/sys/kernel/fast_charge/force_fast_charge").exists() -> "/sys/kernel/fast_charge/force_fast_charge"
            File("/sys/module/msm_otg/parameters/fast_charge").exists() -> "/sys/module/msm_otg/parameters/fast_charge"
            else -> null
        }
    }
}
