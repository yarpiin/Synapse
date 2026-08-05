package com.af.synapse.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File

object ProfileManager {
    private const val PROFILES_DIR = "profiles"
    private val gson = Gson()

    fun getProfiles(context: Context): List<String> {
        val dir = File(context.filesDir, PROFILES_DIR)
        if (!dir.exists()) dir.mkdirs()
        return dir.listFiles()?.map { it.nameWithoutExtension } ?: emptyList()
    }

    fun saveProfile(context: Context, name: String) {
        val dir = File(context.filesDir, PROFILES_DIR)
        if (!dir.exists()) dir.mkdirs()
        
        val settings = mutableMapOf<String, String>()
        SettingsStore.getTrackedPaths().forEach { path ->
            settings[path] = SettingsStore.getValue(path)
        }
        
        val json = gson.toJson(settings)
        File(dir, "$name.json").writeText(json)
    }

    fun applyProfile(context: Context, name: String) {
        val file = File(context.filesDir, "$PROFILES_DIR/$name.json")
        if (!file.exists()) return
        
        val json = file.readText()
        val type = object : TypeToken<Map<String, String>>() {}.type
        val settings: Map<String, String> = gson.fromJson(json, type)
        
        settings.forEach { (path, value) ->
            GenericManager.writeFile(path, value)
        }
    }

    fun deleteProfile(context: Context, name: String) {
        val file = File(context.filesDir, "$PROFILES_DIR/$name.json")
        if (file.exists()) file.delete()
    }
}
