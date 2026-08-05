package com.af.synapse.data

import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File

object MemoryManager {

    data class RamStats(
        val totalMb: Long,
        val usedMb: Long,
        val freeMb: Long,
        val usedPercent: Int
    )

    data class ZRamStats(
        val totalMb: Long,
        val usedMb: Long,
        val usedPercent: Int
    )

    fun getRamStatsFlow() = flow {
        while (true) {
            emit(getRamStats())
            delay(5000)
        }
    }.flowOn(Dispatchers.IO)

    fun getRamStats(): RamStats {
        val lines = try {
            File("/proc/meminfo").readLines()
        } catch (e: Exception) {
            Shell.cmd("cat /proc/meminfo").exec().out
        }
        return parseMemInfo(lines)
    }

    fun parseMemInfo(lines: List<String>): RamStats {
        var total = 0L
        var free = 0L
        var buffers = 0L
        var cached = 0L
        var sReclaimable = 0L

        lines.forEach { line ->
            when {
                line.startsWith("MemTotal:") -> total = extractKb(line)
                line.startsWith("MemFree:") -> free = extractKb(line)
                line.startsWith("Buffers:") -> buffers = extractKb(line)
                line.startsWith("Cached:") -> cached = extractKb(line)
                line.startsWith("SReclaimable:") -> sReclaimable = extractKb(line)
            }
        }

        val actualTotalMb = total / 1024
        val physicalGb = kotlin.math.ceil(actualTotalMb / 1024.0).toLong()
        val totalMb = physicalGb * 1024
        
        val availableKb = free + buffers + cached + sReclaimable
        val usedKb = total - availableKb
        val usedMb = usedKb / 1024
        val freeMb = actualTotalMb - usedMb
        val usedPercent = if (total > 0) ((usedKb.toDouble() / total.toDouble()) * 100).toInt() else 0

        return RamStats(totalMb, usedMb, freeMb, usedPercent)
    }

    private fun extractKb(line: String): Long {
        return line.split(Regex("\\s+"))[1].toLongOrNull() ?: 0L
    }

    fun getZRamStats(): ZRamStats {
        val path = "/sys/block/zram0"
        val diskSizeStr = try {
            File("$path/disksize").readText().trim()
        } catch (e: Exception) {
            Shell.cmd("cat $path/disksize").exec().out.firstOrNull() ?: "0"
        }
        
        val totalMb = (diskSizeStr.toLongOrNull() ?: 0L) / 1024 / 1024
        
        val mmStat = try {
            File("$path/mm_stat").readText().trim()
        } catch (e: Exception) {
            Shell.cmd("cat $path/mm_stat").exec().out.firstOrNull()
        }
        
        val usedMb = if (mmStat != null) {
            val parts = mmStat.trim().split(Regex("\\s+"))
            if (parts.size >= 3) (parts[2].toLongOrNull() ?: 0L) / 1024 / 1024 else 0L
        } else {
            0L
        }
        
        val percent = if (totalMb > 0) ((usedMb.toDouble() / totalMb.toDouble()) * 100).toInt() else 0
        return ZRamStats(totalMb, usedMb, percent)
    }

    fun getZRamCompAlgorithms(): List<String> {
        val out = Shell.cmd("cat /sys/block/zram0/comp_algorithm").exec().out.firstOrNull() ?: ""
        return out.replace("[", "").replace("]", "").split(" ").filter { it.isNotBlank() }
    }

    fun getCurrentZRamAlgorithm(): String {
        val out = Shell.cmd("cat /sys/block/zram0/comp_algorithm").exec().out.firstOrNull() ?: ""
        return out.substringAfter("[").substringBefore("]").trim()
    }

    fun setZRamSize(mb: Int) {
        val bytes = mb.toLong() * 1024 * 1024
        val bytesStr = bytes.toString()
        SettingsStore.trackSetting("/sys/block/zram0/disksize", bytesStr)

        Shell.cmd(
            "swapoff /dev/block/zram0 2>/dev/null",
            "echo 1 > /sys/block/zram0/reset",
            "echo $bytesStr > /sys/block/zram0/disksize",
            "mkswap /dev/block/zram0",
            "swapon /dev/block/zram0"
        ).exec()
    }

    fun setZRamAlgorithm(algo: String) {
        SettingsStore.trackSetting("/sys/block/zram0/comp_algorithm", algo)
        Shell.cmd(
            "swapoff /dev/block/zram0 2>/dev/null",
            "echo 1 > /sys/block/zram0/reset",
            "echo $algo > /sys/block/zram0/comp_algorithm",
            "swapon /dev/block/zram0"
        ).exec()
    }
}
