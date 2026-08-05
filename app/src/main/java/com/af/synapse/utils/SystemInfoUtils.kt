package com.af.synapse.utils

import android.os.Build
import com.topjohnwu.superuser.Shell

object SystemInfoUtils {

    fun getCpuName(): String {
        return try {
            val props = listOf("ro.soc.model", "ro.board.platform", "ro.product.board")
            var foundValue: String? = null
            for (prop in props) {
                val value = Shell.cmd("getprop $prop").exec().out.firstOrNull()
                if (!value.isNullOrBlank() && value != "unknown") {
                    foundValue = value
                    break
                }
            }
            formatCpuName(foundValue ?: Build.HARDWARE)
        } catch (e: Exception) {
            formatCpuName(Build.HARDWARE)
        }
    }

    private fun formatCpuName(name: String): String {
        val n = name.lowercase()
        return when {
            n.contains("komodo") -> "Google Tensor G4 (Komodo)"
            n.contains("caiman") -> "Google Tensor G4 (Caiman)"
            n.contains("tokay") -> "Google Tensor G4 (Tokay)"
            n.contains("zuma") -> "Google Tensor G3"
            n.contains("gs201") -> "Google Tensor G2"
            n.contains("gs101") -> "Google Tensor"
            else -> name.uppercase()
        }
    }

    fun getGpuName(): String {
        return try {
            val sf = Shell.cmd("dumpsys SurfaceFlinger | grep GLES").exec().out.firstOrNull()
            if (!sf.isNullOrBlank() && !sf.contains("ganesh", ignoreCase = true)) {
                val parts = sf.substringAfter("GLES:").split(",")
                if (parts.size >= 2) return parts[1].trim()
            }

            val board = Build.BOARD.lowercase()
            when {
                board.contains("komodo") || board.contains("caiman") || board.contains("tokay") -> "Mali-G715 MC12"
                board.contains("zuma") -> "Mali-G715"
                board.contains("pantheon") -> "Mali-G710"
                board.contains("whitechapel") -> "Mali-G78"
                else -> sf?.substringAfter("GLES:")?.trim() ?: "Mali / Adreno"
            }
        } catch (e: Exception) {
            "Mali / Adreno"
        }
    }

    fun getKernelVersion(): String {
        return System.getProperty("os.version") ?: "Unknown Kernel"
    }

    fun getRomBranding(): String {
        return try {
            val props = mapOf(
                "ro.lineage.version" to "LineageOS",
                "ro.modversion" to "Custom ROM",
                "ro.evolution.version" to "Evolution X",
                "ro.pixys.version" to "PixysOS",
                "ro.crdroid.version" to "crDroid"
            )

            for ((prop, name) in props) {
                val value = Shell.cmd("getprop $prop").exec().out.firstOrNull()
                if (!value.isNullOrBlank()) return name
            }

            val displayId = Shell.cmd("getprop ro.build.display.id").exec().out.firstOrNull()?.lowercase() ?: ""
            when {
                displayId.contains("lineage") -> "LineageOS"
                displayId.contains("evolution") -> "Evolution X"
                displayId.contains("pixelos") -> "PixelOS"
                displayId.contains("project-elixir") -> "Project Elixir"
                Build.BRAND.equals("google", ignoreCase = true) -> "Google Stock"
                else -> "${Build.BRAND.replaceFirstChar { it.uppercase() }} Stock"
            }
        } catch (e: Exception) {
            "Stock ROM"
        }
    }
}
