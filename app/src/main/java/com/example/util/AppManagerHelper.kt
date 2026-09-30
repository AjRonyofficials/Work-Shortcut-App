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
     * Triggers zero-touch automated clearing of data and cache via AccessibilityService
     */
    fun openAppDetailsForClearData(context: Context, packageName: String, appName: String = "App") {
        com.example.service.AutoCleanAccessibilityService.startAutoClean(context, packageName, appName)
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
