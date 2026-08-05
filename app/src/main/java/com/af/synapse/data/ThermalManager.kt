package com.af.synapse.data

import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File

object ThermalManager {

    data class TemperatureSensor(
        val label: String,
        val value: Float,
        val key: String
    )

    private var cachedZones: List<Pair<Int, String>>? = null

    fun getTemperaturesFlow() = flow {
        while (true) {
            val sensors = mutableListOf<TemperatureSensor>()
            
            if (cachedZones == null) {
                val zones = mutableListOf<Pair<Int, String>>()
                // Updated mappings based on user feedback
                val mappings = mapOf(
                    12 to "SoC",
                    0 to "CPU",
                    3 to "GPU",
                    15 to "Battery"
                )
                
                mappings.forEach { (idx, label) ->
                    if (GenericManager.exists("/sys/class/thermal/thermal_zone$idx/temp")) {
                        zones.add(idx to label)
                    }
                }
                
                cachedZones = zones
            }

            cachedZones?.forEach { (idx, label) ->
                val temp = readZoneTemp(idx)
                if (temp != null && temp > 0) {
                    sensors.add(TemperatureSensor(label, temp, "zone_$idx"))
                }
            }

            // Battery fallback from power_supply
            if (sensors.none { it.label == "Battery" }) {
                getBatteryTemperature()?.let { sensors.add(it) }
            }

            emit(sensors)
            delay(3000) // Faster refresh: 3 seconds
        }
    }.flowOn(Dispatchers.IO)

    private fun readZoneTemp(idx: Int): Float? {
        val path = "/sys/class/thermal/thermal_zone$idx/temp"
        val out = Shell.cmd("cat $path 2>/dev/null").exec().out.firstOrNull()
        return parseTemp(out ?: "")
    }

    private fun getBatteryTemperature(): TemperatureSensor? {
        val paths = listOf(
            "/sys/class/power_supply/battery/temp",
            "/sys/class/power_supply/google_battery/temp"
        )
        for (path in paths) {
            val out = Shell.cmd("cat $path 2>/dev/null").exec().out.firstOrNull()
            if (out != null) {
                parseTemp(out)?.let {
                    return TemperatureSensor("Battery", it, "battery_ps")
                }
            }
        }
        return null
    }

    private fun parseTemp(raw: String): Float? {
        val t = raw.trim().toFloatOrNull() ?: return null
        val absT = Math.abs(t)
        return when {
            absT > 10000 -> t / 1000f
            absT > 200 -> t / 10f
            else -> t
        }
    }

    fun isThermalAvailable(): Boolean = true
}
