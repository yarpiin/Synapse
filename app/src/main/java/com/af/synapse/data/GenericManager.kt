package com.af.synapse.data

import com.topjohnwu.superuser.Shell
import java.io.File

object GenericManager {

    fun exists(path: String): Boolean {
        // Direct file exists check is often blocked by SELinux even if root, so shell check is safer
        return Shell.cmd("if [ -e \"$path\" ]; then echo 1; fi").exec().out.firstOrNull() == "1"
    }

    fun isDirectory(path: String): Boolean {
        return Shell.cmd("if [ -d \"$path\" ]; then echo 1; fi").exec().out.firstOrNull() == "1"
    }

    fun readFile(path: String): String {
        return Shell.cmd("cat \"$path\"").exec().out.firstOrNull() ?: ""
    }

    fun writeFile(path: String, value: String) {
        // Ensure root write with explicit chmod
        Shell.cmd(
            "chmod 644 \"$path\" 2>/dev/null",
            "echo \"$value\" > \"$path\""
        ).exec()
        SettingsStore.trackSetting(path, value)
    }

    fun readBool(path: String): Boolean {
        val out = readFile(path)
        return out == "1" || out.lowercase() == "y" || out.lowercase() == "true" || out.lowercase() == "on"
    }

    fun writeBool(path: String, value: Boolean) {
        writeFile(path, if (value) "1" else "0")
    }
}
