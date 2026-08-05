package com.af.synapse.data

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

object MonitorManager {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    val ramStats = MutableStateFlow<MemoryManager.RamStats?>(null)
    val zramStats = MutableStateFlow<MemoryManager.ZRamStats?>(null)
    val temperatures = MutableStateFlow<List<ThermalManager.TemperatureSensor>>(emptyList())
    
    private var monitoringJob: Job? = null

    fun startMonitoring() {
        if (monitoringJob != null) return
        
        monitoringJob = scope.launch {
            // High priority initial fetch for all critical data
            try {
                val rStats = MemoryManager.getRamStats()
                val zStats = MemoryManager.getZRamStats()
                
                // Thermal detection can be slow, but we need initial values
                // Use a quick scan if possible (already handled in ThermalManager.getTemperaturesFlow)
                
                withContext(Dispatchers.Main) {
                    ramStats.value = rStats
                    zramStats.value = zStats
                }
            } catch (e: Exception) {}

            // Start background loops
            
            // 1. RAM & Z-RAM Monitoring (Slow)
            launch {
                while (isActive) {
                    try {
                        val r = MemoryManager.getRamStats()
                        val z = MemoryManager.getZRamStats()
                        ramStats.value = r
                        zramStats.value = z
                    } catch (e: Exception) {}
                    delay(30000)
                }
            }
            
            // 2. Thermal Monitoring (Medium)
            launch {
                ThermalManager.getTemperaturesFlow().collect { list ->
                    temperatures.value = list
                }
            }
        }
    }
    
    fun stopMonitoring() {
        monitoringJob?.cancel()
        monitoringJob = null
    }
}
