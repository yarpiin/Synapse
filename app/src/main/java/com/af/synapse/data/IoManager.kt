package com.af.synapse.data

import com.topjohnwu.superuser.Shell
import java.io.File

object IoManager {

    fun getInternalStorageBlock(): String {
        val blocks = listOf("sda", "mmcblk0", "sdb", "sdc", "vda", "nvme0n1")
        for (block in blocks) {
            if (GenericManager.exists("/sys/block/$block/queue/scheduler")) {
                return block
            }
        }
        return "sda"
    }

    fun getReadAhead(block: String): Long {
        val out = Shell.cmd("cat \"/sys/block/$block/queue/read_ahead_kb\"").exec().out.firstOrNull()
        return out?.toLongOrNull() ?: 0L
    }

    fun setReadAhead(block: String, kb: Long) {
        val path = "/sys/block/$block/queue/read_ahead_kb"
        Shell.cmd(
            "chmod 644 \"$path\" 2>/dev/null",
            "echo \"$kb\" > \"$path\""
        ).exec()
        SettingsStore.trackSetting(path, kb.toString())
    }

    fun getAvailableSchedulers(block: String): List<String> {
        val out = Shell.cmd("cat \"/sys/block/$block/queue/scheduler\"").exec().out.firstOrNull() ?: ""
        // Format: noop [deadline] cfq OR none [mq-deadline] kyber bfq
        return out.replace("[", "").replace("]", "").split(Regex("\\s+")).filter { it.isNotBlank() }
    }

    fun getCurrentScheduler(block: String): String {
        val out = Shell.cmd("cat \"/sys/block/$block/queue/scheduler\"").exec().out.firstOrNull() ?: ""
        return out.substringAfter("[").substringBefore("]").trim()
    }

    fun setScheduler(block: String, sched: String) {
        val path = "/sys/block/$block/queue/scheduler"
        Shell.cmd(
            "chmod 644 \"$path\" 2>/dev/null",
            "echo \"$sched\" > \"$path\""
        ).exec()
        SettingsStore.trackSetting(path, sched)
    }

    fun getSchedulerTunables(block: String): List<GovernorTunable> {
        val path = "/sys/block/$block/queue/iosched"
        if (!GenericManager.isDirectory(path)) return emptyList()

        val files = Shell.cmd("ls \"$path\"").exec().out
        return files.filter { it.isNotBlank() }.map { fileName ->
            val fullPath = "$path/$fileName"
            GovernorTunable(
                name = fileName,
                value = Shell.cmd("cat \"$fullPath\"").exec().out.firstOrNull() ?: "",
                path = fullPath
            )
        }
    }
}
