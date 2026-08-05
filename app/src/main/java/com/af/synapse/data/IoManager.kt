package com.af.synapse.data

import com.topjohnwu.superuser.Shell
import java.io.File

object IoManager {

    fun getInternalStorageBlock(): String {
        // Common paths: /sys/block/sda (UFS) or /sys/block/mmcblk0 (eMMC)
        return when {
            File("/sys/block/sda").exists() -> "sda"
            File("/sys/block/mmcblk0").exists() -> "mmcblk0"
            File("/sys/block/sdb").exists() -> "sdb"
            else -> "sda"
        }
    }

    fun getReadAhead(block: String): Long {
        val out = Shell.cmd("cat /sys/block/$block/queue/read_ahead_kb").exec().out.firstOrNull()
        return out?.toLongOrNull() ?: 0L
    }

    fun setReadAhead(block: String, kb: Long) {
        Shell.cmd("echo $kb > /sys/block/$block/queue/read_ahead_kb").exec()
    }

    fun getAvailableSchedulers(block: String): List<String> {
        val out = Shell.cmd("cat /sys/block/$block/queue/scheduler").exec().out.firstOrNull() ?: ""
        // Format: noop [deadline] cfq
        return out.replace("[", "").replace("]", "").split(" ").filter { it.isNotBlank() }
    }

    fun getCurrentScheduler(block: String): String {
        val out = Shell.cmd("cat /sys/block/$block/queue/scheduler").exec().out.firstOrNull() ?: ""
        return out.substringAfter("[").substringBefore("]").trim()
    }

    fun setScheduler(block: String, sched: String) {
        Shell.cmd("echo $sched > /sys/block/$block/queue/scheduler").exec()
    }

    fun getSchedulerTunables(block: String): List<GovernorTunable> {
        val sched = getCurrentScheduler(block)
        val path = "/sys/block/$block/queue/iosched"
        val dir = File(path)
        if (!dir.exists() || !dir.isDirectory) return emptyList()

        return dir.listFiles()?.filter { it.isFile }?.map {
            GovernorTunable(
                name = it.name,
                value = Shell.cmd("cat ${it.absolutePath}").exec().out.firstOrNull() ?: "",
                path = it.absolutePath
            )
        } ?: emptyList()
    }
}
