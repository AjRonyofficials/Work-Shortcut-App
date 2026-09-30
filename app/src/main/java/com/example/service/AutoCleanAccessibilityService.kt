package com.example.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast

class AutoCleanAccessibilityService : AccessibilityService() {

    companion object {
        var instance: AutoCleanAccessibilityService? = null
            private set

        var targetPackage: String? = null
            private set

        var isAutomating: Boolean = false
            private set

        private var step: Int = 0
        private var lastActionTime: Long = 0L

        fun isServiceRunning(): Boolean = instance != null

        fun startAutoClean(context: Context, packageName: String, appName: String = "App") {
            targetPackage = packageName
            isAutomating = true
            step = 0
            lastActionTime = System.currentTimeMillis()

            if (instance == null) {
                // Prompt user to enable Accessibility Service if not active
                Toast.makeText(
                    context,
                    "Please turn ON 'Work Shortcut' in Accessibility for zero-tap auto cleaner!",
                    Toast.LENGTH_LONG
                ).show()

                try {
                    val accIntent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(accIntent)
                } catch (_: Exception) {
                    openAppDetailsFallback(context, packageName, appName)
                }
                return
            }

            // Launch App Details Settings for target package
            openAppDetailsFallback(context, packageName, appName)
        }

        private fun openAppDetailsFallback(context: Context, packageName: String, appName: String) {
            try {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:$packageName")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                Toast.makeText(context, "Auto-cleaning $appName...", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Error opening $appName: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        isAutomating = false
    }

    override fun onInterrupt() {
        isAutomating = false
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (!isAutomating) return
        val currentTarget = targetPackage ?: return

        // Timeout check (15s safety)
        if (System.currentTimeMillis() - lastActionTime > 15000) {
            isAutomating = false
            targetPackage = null
            return
        }

        val rootNode = rootInActiveWindow ?: return
        val pkg = event?.packageName?.toString() ?: ""

        // Process only within settings packages
        if (pkg.contains("settings", ignoreCase = true) || pkg.contains("packageinstaller", ignoreCase = true)) {
            handleAutoCleanStep(rootNode)
        }
    }

    private fun handleAutoCleanStep(rootNode: AccessibilityNodeInfo) {
        val now = System.currentTimeMillis()
        if (now - lastActionTime < 400) return // debounce rapid clicks

        // Step 0: Find and click "Storage & cache" or "Storage"
        if (step == 0) {
            val storageNode = findNodeByKeywords(
                rootNode,
                listOf(
                    "storage & cache",
                    "storage",
                    "storage and cache",
                    "internal shared storage",
                    "স্টোরেজ",
                    "স্টোরেজ ও ক্যাশ"
                )
            )
            if (storageNode != null) {
                clickNode(storageNode)
                step = 1
                lastActionTime = now
                return
            }
        }

        // Step 1: In Storage screen, find "Clear cache" and "Clear data" / "Clear storage"
        if (step in 1..2) {
            val clearCacheNode = findNodeByKeywords(
                rootNode,
                listOf(
                    "clear cache",
                    "ক্যাশে মুছুন",
                    "ক্লিয়ার ক্যাশ"
                )
            )
            if (clearCacheNode != null && clearCacheNode.isEnabled) {
                clickNode(clearCacheNode)
            }

            val clearDataNode = findNodeByKeywords(
                rootNode,
                listOf(
                    "clear storage",
                    "clear data",
                    "delete data",
                    "স্টোরেজ মুছুন",
                    "ডেটা মুছুন",
                    "ক্লিয়ার ডেটা"
                )
            )
            if (clearDataNode != null && clearDataNode.isEnabled) {
                clickNode(clearDataNode)
                step = 3
                lastActionTime = now
                return
            } else if (clearCacheNode != null) {
                // If only cache could be cleared, advance
                step = 3
                lastActionTime = now
                return
            }
        }

        // Step 3: Handle Confirmation Dialog (OK, Delete, Clear)
        if (step == 3) {
            val confirmNode = findNodeByKeywords(
                rootNode,
                listOf(
                    "ok",
                    "delete",
                    "clear",
                    "confirm",
                    "ঠিক আছে",
                    "মুছুন"
                )
            )
            if (confirmNode != null && confirmNode.isEnabled) {
                clickNode(confirmNode)
                step = 4
                lastActionTime = now

                // Auto finish: delay briefly then press back to close settings
                mainHandler.postDelayed({
                    performGlobalAction(GLOBAL_ACTION_BACK)
                    mainHandler.postDelayed({
                        performGlobalAction(GLOBAL_ACTION_BACK)
                        Toast.makeText(applicationContext, "Auto Clear Complete!", Toast.LENGTH_SHORT).show()
                        isAutomating = false
                        targetPackage = null
                    }, 400)
                }, 400)
                return
            } else {
                // If no confirmation popped up, we might already be done
                step = 4
                mainHandler.postDelayed({
                    performGlobalAction(GLOBAL_ACTION_BACK)
                    mainHandler.postDelayed({
                        performGlobalAction(GLOBAL_ACTION_BACK)
                        Toast.makeText(applicationContext, "Auto Clear Complete!", Toast.LENGTH_SHORT).show()
                        isAutomating = false
                        targetPackage = null
                    }, 350)
                }, 350)
            }
        }
    }

    private fun findNodeByKeywords(root: AccessibilityNodeInfo, keywords: List<String>): AccessibilityNodeInfo? {
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)

        while (queue.isNotEmpty()) {
            val node = queue.removeFirst()
            val text = (node.text?.toString() ?: "") + " " + (node.contentDescription?.toString() ?: "")

            if (text.isNotBlank()) {
                val lowerText = text.lowercase()
                for (kw in keywords) {
                    if (lowerText.contains(kw)) {
                        return node
                    }
                }
            }

            for (i in 0 until node.childCount) {
                node.getChild(i)?.let { queue.add(it) }
            }
        }
        return null
    }

    private fun clickNode(node: AccessibilityNodeInfo): Boolean {
        var current: AccessibilityNodeInfo? = node
        while (current != null) {
            if (current.isClickable) {
                return current.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            }
            current = current.parent
        }
        // Fallback: try click directly
        return node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
    }
}
