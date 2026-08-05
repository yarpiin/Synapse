package com.af.synapse.data

import com.topjohnwu.superuser.Shell
import java.io.File

object DisplayManager {

    fun isDisplayAvailable(): Boolean {
        val paths = listOf(
            "/sys/devices/platform/kcal_ctrl.0",
            "/sys/module/msm_drm/parameters",
            "/sys/class/misc/gammacontrol",
            "/sys/class/mdnie",
            "/sys/devices/virtual/graphics/fb0"
        )
        for (path in paths) {
            if (File(path).exists()) return true
        }
        return false
    }

    fun getKcalRgb(): Triple<Int, Int, Int> {
        val path = "/sys/devices/platform/kcal_ctrl.0/kcal"
        if (File(path).exists()) {
            val out = GenericManager.readFile(path).trim()
            val parts = out.split(" ")
            if (parts.size >= 3) {
                return Triple(
                    parts[0].toIntOrNull() ?: 256,
                    parts[1].toIntOrNull() ?: 256,
                    parts[2].toIntOrNull() ?: 256
                )
            }
        }
        return Triple(256, 256, 256)
    }

    fun setKcalRgb(r: Int, g: Int, b: Int) {
        val path = "/sys/devices/platform/kcal_ctrl.0/kcal"
        if (File(path).exists()) {
            GenericManager.writeFile(path, "$r $g $b")
        }
    }
}
