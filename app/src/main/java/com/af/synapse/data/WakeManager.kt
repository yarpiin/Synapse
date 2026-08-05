package com.af.synapse.data

import java.io.File

object WakeManager {

    private val WAKE_PATHS = listOf(
        "/sys/devices/virtual/misc/touchwake",
        "/sys/module/lge_touch_core/parameters",
        "/sys/android_touch",
        "/proc/touchpanel",
        "/sys/devices/virtual/input/clearpad",
        "/sys/module/sweep2wake/parameters",
        "/sys/module/input_core/parameters/pwrkey_suspend",
        "/sys/module/qpnp_power_on/parameters/pwrkey_suspend",
        "/sys/module/lid/parameters/enable_lid"
    )

    fun isWakeAvailable(): Boolean {
        for (path in WAKE_PATHS) {
            if (GenericManager.exists(path)) return true
        }
        return false
    }
}
