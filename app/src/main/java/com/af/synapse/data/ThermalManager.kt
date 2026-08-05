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
            val allTemps = mutableMapOf<Int, Float>()
            
            if (cachedZones == null) {
                val zones = mutableListOf<Pair<Int, String>>()
                val mappings = mapOf(
                    20 to "SoC (Avg)",
                    3 to "GPU",
                    15 to "Battery",
                    0 to "Big Cores",
                    1 to "Mid Cores",
                    2 to "Little Cores"
                )
                
                mappings.forEach { (idx, label) ->
                    if (GenericManager.exists("/sys/class/thermal/thermal_zone$idx/temp")) {
                        zones.add(idx to label)
                    }
                }
                
                if (zones.isEmpty()) {
                    for (i in 0..15) {
                        if (GenericManager.exists("/sys/class/thermal/thermal_zone$i/temp")) {
                            zones.add(i to "Zone $i")
                        }
                    }
                }
                cachedZones = zones
            }

            cachedZones?.forEach { (idx, label) ->
                val temp = readZoneTemp(idx)
                if (temp != null && temp > 0) {
                    sensors.add(TemperatureSensor(label, temp, "zone_$idx"))
                    allTemps[idx] = temp
                }
            }

            val peakTemp = allTemps.filter { it.key != 15 }.values.maxOrNull() ?: 0f
            if (peakTemp > 0) {
                sensors.add(0, TemperatureSensor("SoC Peak", peakTemp, "soc_peak"))
            }

            emit(sensors)
            delay(60000)
        }
    }.flowOn(Dispatchers.IO)

    private fun readZoneTemp(idx: Int): Float? {
        val path = "/sys/class/thermal/thermal_zone$idx/temp"
        val out = Shell.cmd("cat $path 2>/dev/null").exec().out.firstOrNull()
        return parseTemp(out ?: "")
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
