package com.af.synapse.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

object UpdateManager {
    private const val GITHUB_API_URL = "https://api.github.com/repos/yarpiin/Synapse/releases/latest"
    private val gson = Gson()

    data class GitHubRelease(
        val tag_name: String,
        val html_url: String,
        val assets: List<GitHubAsset>
    )

    data class GitHubAsset(
        val name: String,
        val browser_download_url: String
    )

    data class UpdateInfo(
        val version: String,
        val downloadUrl: String,
        val releaseUrl: String,
        val isNewer: Boolean
    )

    suspend fun checkForUpdates(context: Context): UpdateInfo? = withContext(Dispatchers.IO) {
        try {
            val url = URL(GITHUB_API_URL)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("Accept", "application/vnd.github.v3+json")
            connection.connect()

            if (connection.responseCode == 200) {
                val json = connection.inputStream.bufferedReader().use { it.readText() }
                val release = gson.fromJson(json, GitHubRelease::class.java)
                
                val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                val currentVersion = pInfo.versionName ?: "1.0"
                val latestVersion = release.tag_name.replace("v", "")
                
                val isNewer = isVersionNewer(currentVersion, latestVersion)
                val downloadUrl = release.assets.find { it.name.endsWith(".apk") }?.browser_download_url ?: release.html_url

                UpdateInfo(latestVersion, downloadUrl, release.html_url, isNewer)
            } else null
        } catch (e: Exception) {
            null
        }
    }

    private fun isVersionNewer(current: String, latest: String): Boolean {
        val currParts = current.split(".").mapNotNull { it.toIntOrNull() }
        val lateParts = latest.split(".").mapNotNull { it.toIntOrNull() }
        
        for (i in 0 until minOf(currParts.size, lateParts.size)) {
            if (lateParts[i] > currParts[i]) return true
            if (lateParts[i] < currParts[i]) return false
        }
        return lateParts.size > currParts.size
    }

    fun openDownload(context: Context, url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
}
