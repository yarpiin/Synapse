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
            val stats = try {
                val lines = File("/proc/meminfo").readLines()
                parseMemInfo(lines)
            } catch (e: Exception) {
                val memInfo = Shell.cmd("cat /proc/meminfo").exec().out
                parseMemInfo(memInfo)
            }
            emit(stats)
            delay(5000) // 5 seconds is better for "live" feel without much overhead
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

        // Adjust total to show physical RAM (round up to nearest GB)
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
        val diskSizeStr = Shell.cmd("cat /sys/block/zram0/disksize").exec().out.firstOrNull() ?: "0"
        // Use decimal to match diagnostic apps (1 GB = 1,000,000,000 bytes)
        val totalMb = (diskSizeStr.toLongOrNull() ?: 0L) / 1000 / 1000
        
        val mmStat = Shell.cmd("cat /sys/block/zram0/mm_stat").exec().out.firstOrNull()
        val usedMb = if (mmStat != null) {
            val parts = mmStat.trim().split(Regex("\\s+"))
            // mm_stat 3rd field is orig_data_size in bytes
            if (parts.size >= 3) (parts[2].toLongOrNull() ?: 0L) / 1000 / 1000 else 0L
        } else {
            0L
        }
        
        val percent = if (totalMb > 0) ((usedMb.toDouble() / totalMb.toDouble()) * 100).toInt() else 0
        return ZRamStats(totalMb, usedMb, percent)
    }

    fun getZRamCompAlgorithms(): List<String> {
        val out = Shell.cmd("cat /sys/block/zram0/comp_algorithm").exec().out.firstOrNull() ?: ""
        // Format: lzo [lz4] deflate
        return out.replace("[", "").replace("]", "").split(" ").filter { it.isNotBlank() }
    }

    fun getCurrentZRamAlgorithm(): String {
        val out = Shell.cmd("cat /sys/block/zram0/comp_algorithm").exec().out.firstOrNull() ?: ""
        return out.substringAfter("[").substringBefore("]").trim()
    }

    fun setZRamSize(mb: Int) {
        val bytes = mb.toLong() * 1000 * 1000 // Use decimal for Z-RAM
        val bytesStr = bytes.toString()
        Shell.cmd(
            "swapoff /dev/block/zram0",
            "echo 1 > /sys/block/zram0/reset",
            "echo $bytesStr > /sys/block/zram0/disksize",
            "mkswap /dev/block/zram0",
            "swapon /dev/block/zram0"
        ).exec()
        SettingsStore.trackSetting("/sys/block/zram0/disksize", bytesStr)
    }

    fun setZRamAlgorithm(algo: String) {
        Shell.cmd(
            "swapoff /dev/block/zram0",
            "echo 1 > /sys/block/zram0/reset",
            "echo $algo > /sys/block/zram0/comp_algorithm",
            "swapon /dev/block/zram0"
        ).exec()
        SettingsStore.trackSetting("/sys/block/zram0/comp_algorithm", algo)
    }
}
