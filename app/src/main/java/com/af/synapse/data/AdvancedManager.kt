package com.af.synapse.data

import java.io.File

object AdvancedManager {

    fun isAdvancedAvailable(): Boolean {
        val paths = listOf(
            "/sys/module/pm_8x60",
            "/sys/module/msm_pm",
            "/sys/kernel/mm/ksm",
            "/sys/kernel/mm/uksm",
            "/sys/kernel/sched/arch_power",
            "/sys/module/lowmemorykiller/parameters/enable_adaptive_lmk",
            "/sys/module/workqueue/parameters/power_efficient",
            "/sys/module/sync/parameters/fsync_enabled",
            "/sys/class/misc/fsynccontrol/fsync_enabled",
            "/sys/kernel/dyn_fsync/Dyn_fsync_active",
            "/sys/kernel/sched/gentle_fair_sleepers",
            "/sys/module/wakeup/parameters",
            "/sys/class/leds",
            "/sys/module/cpufreq/parameters/batterysaver"
        )
        return paths.any { GenericManager.exists(it) }
    }

    fun getKsmStats(): Map<String, String> {
        val stats = mutableMapOf<String, String>()
        val base = "/sys/kernel/mm/ksm/"
        if (File(base).exists()) {
            listOf("pages_shared", "pages_sharing", "pages_unshared", "full_scans").forEach {
                stats[it] = GenericManager.readFile(base + it)
            }
        }
        return stats
    }

    fun getLedTriggers(color: String): List<String> {
        val path = "/sys/class/leds/$color/trigger"
        if (!GenericManager.exists(path)) return emptyList()
        val out = GenericManager.readFile(path)
        // Format: none [heartbeat] mmc0
        return out.replace("[", "").replace("]", "").split(" ").filter { it.isNotBlank() }
    }

    fun getCurrentLedTrigger(color: String): String {
        val path = "/sys/class/leds/$color/trigger"
        if (!GenericManager.exists(path)) return "none"
        val out = GenericManager.readFile(path)
        return out.substringAfter("[").substringBefore("]").trim()
    }
}
