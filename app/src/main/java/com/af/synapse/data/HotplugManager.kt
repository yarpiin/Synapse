package com.af.synapse.data

import com.topjohnwu.superuser.Shell
import java.io.File

object HotplugManager {

    private val HOTPLUG_PATHS = listOf(
        "/sys/kernel/hima_hotplug",
        "/sys/module/lazyplug",
        "/sys/module/autosmp",
        "/sys/kernel/autosmp",
        "/sys/module/msm_performance",
        "/sys/devices/system/cpu/cpu0/core_ctl",
        "/sys/module/intelli_plug",
        "/sys/kernel/intelli_plug",
        "/sys/kernel/bricked_hotplug",
        "/sys/kernel/msm_mpdecision",
        "/sys/module/msm_hotplug",
        "/sys/module/auto_hotplug",
        "/sys/module/dyn_hotplug",
        "/sys/module/blu_plug",
        "/sys/kernel/alucard_hotplug",
        "/sys/class/misc/mako_hotplug_control",
        "/sys/devices/virtual/misc/mako_hotplug_control",
        "/sys/kernel/thunderplug",
        "/sys/kernel/AiO_HotPlug",
        "/dev/cpuctl",
        "/dev/cpuset",
        "/proc/sys/kernel/sched_util_clamp_min"
    )

    fun isHotplugAvailable(): Boolean {
        for (path in HOTPLUG_PATHS) {
            if (GenericManager.exists(path)) return true
        }
        return false
    }

    fun getEasGroups(): List<String> {
        val groups = listOf(
            "", // Root group
            "foreground", "camera-daemon", "rt", "nnapi-hal", "application", 
            "kernel", "restricted", "top-app", "audio-app", "display", 
            "oiface_fg", "sf", "dex2oat", "foreground_window", "system", 
            "background", "h-background", "l-background", "system-background"
        )
        return groups.filter { group ->
            val path = if (group.isEmpty()) "/dev/cpuctl/cpu.uclamp.min" else "/dev/cpuctl/$group"
            GenericManager.exists(path)
        }
    }

    fun getCpusetGroups(): List<String> {
        val groups = listOf(
            "", "top-app", "application", "foreground_window", "background",
            "l-background", "h-background", "system-background", "restricted",
            "kernel", "rt", "nnapi-hal", "system", "camera-daemon", "sf",
            "oiface_fg", "display", "audio-app", "dex2oat"
        )
        return groups.filter { group ->
            val path = if (group.isEmpty()) "/dev/cpuset/cpus" else "/dev/cpuset/$group/cpus"
            GenericManager.exists(path)
        }
    }
}
