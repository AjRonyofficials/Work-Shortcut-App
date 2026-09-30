package com.example.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.widget.Toast

data class AppInfoItem(
    val appName: String,
    val packageName: String,
    val isSelected: Boolean = false
)

object AppManagerHelper {

    fun getInstalledLauncherApps(context: Context): List<AppInfoItem> {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfos = try {
            pm.queryIntentActivities(intent, 0)
        } catch (_: Exception) {
            emptyList()
        }

        return resolveInfos.mapNotNull { info ->
            try {
                val pkgName = info.activityInfo.packageName
                // exclude self
                if (pkgName == context.packageName) return@mapNotNull null
                val label = info.loadLabel(pm).toString()
                AppInfoItem(appName = label, packageName = pkgName)
            } catch (_: Exception) {
                null
            }
        }.distinctBy { it.packageName }.sortedBy { it.appName.lowercase() }
    }

    /**
     * Opens the direct System App Info Storage settings for the targeted package
     * so user can 1-tap "Clear Storage / Clear Cache"
     */
    fun openAppDetailsForClearData(context: Context, packageName: String, appName: String = "App") {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:$packageName")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Toast.makeText(context, "Opening $appName: Tap Storage -> Clear Data", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Unable to open app settings: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Clears local application cache files in the background
     */
    fun clearSelfCache(context: Context): Boolean {
        return try {
            context.cacheDir.deleteRecursively()
            context.externalCacheDir?.deleteRecursively()
            true
        } catch (_: Exception) {
            false
        }
    }
}
