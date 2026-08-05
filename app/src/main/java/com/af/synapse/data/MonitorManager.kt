package com.af.synapse.data

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

object MonitorManager {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    val ramStats = MutableStateFlow<MemoryManager.RamStats?>(null)
    val temperatures = MutableStateFlow<List<ThermalManager.TemperatureSensor>>(emptyList())
    
    private var monitoringJob: Job? = null

    fun startMonitoring() {
        if (monitoringJob != null) return
        
        monitoringJob = scope.launch {
            // Initial burst fetch for instant UI population
            launch {
                try {
                    val stats = MemoryManager.getRamStats()
                    ramStats.value = stats
                } catch (e: Exception) {}
            }
            
            // RAM Monitoring loop
            launch {
                while (isActive) {
                    delay(60000)
                    try {
                        val stats = MemoryManager.getRamStats()
                        ramStats.value = stats
                    } catch (e: Exception) {}
                }
            }
            
            // Thermal Monitoring loop
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
