package com.af.synapse.data

import com.topjohnwu.superuser.Shell
import java.io.File

object MiscManager {

    fun isMiscAvailable(): Boolean {
        val paths = listOf(
            "/sys/kernel/charge_levels",
            "/sys/kernel/cpufreq_hardlimit",
            "/sys/module/state_notifier",
            "/proc/sys/net/ipv4/tcp_congestion_control",
            "/sys/kernel/power_suspend",
            "/sys/class/misc/btk_control",
            "/sys/class/timed_output/vibrator",
            "/sys/kernel/fast_charge",
            "/sys/class/misc/batterylifeextender",
            "/sys/module/wakeup/parameters/enable_msm_hsic_ws"
        )
        for (path in paths) {
            if (GenericManager.exists(path)) return true
        }
        return true // Always show or check individually
    }

    fun getTcpAvailableCongestion(): List<String> {
        val out = GenericManager.readFile("/proc/sys/net/ipv4/tcp_available_congestion_control")
        return out.split(" ").filter { it.isNotBlank() }
    }

    fun getCurrentTcpCongestion(): String {
        return GenericManager.readFile("/proc/sys/net/ipv4/tcp_congestion_control").trim()
    }

    fun setTcpCongestion(algo: String) {
        GenericManager.writeFile("/proc/sys/net/ipv4/tcp_congestion_control", algo)
    }

    fun getVibrationPath(): String? {
        val paths = listOf(
            "/sys/class/timed_output/vibrator/amp",
            "/sys/devices/virtual/timed_output/vibrator/vtg_level",
            "/sys/class/misc/vibratorcontrol/vibrator_strength",
            "/sys/vibrator/pwmvalue"
        )
        return paths.find { GenericManager.exists(it) }
    }

    fun reboot(mode: String = "") {
        val cmd = if (mode.isEmpty()) "reboot" else "reboot $mode"
        Shell.cmd(cmd).exec()
    }
}
