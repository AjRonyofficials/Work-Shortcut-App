package com.example.util

import android.app.Activity
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import com.example.service.OverlayStateManager

/**
 * Invisible translucent activity that grants foreground focus
 * to read clipboard on Android 10+ without OS background restrictions.
 */
class ClipboardReaderActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val target = intent.getStringExtra(EXTRA_TARGET) ?: "A"

        try {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            val clipText = clipboard?.primaryClip?.getItemAt(0)?.text?.toString()?.trim() ?: ""

            if (clipText.isEmpty()) {
                Toast.makeText(this, "Clipboard is empty! Copy text first.", Toast.LENGTH_SHORT).show()
            } else {
                when (target) {
                    "2FA" -> {
                        OverlayStateManager.processGet2FaWithText(this, clipText)
                    }
                    else -> {
                        OverlayStateManager.pasteToColumnDirect(this, target, clipText)
                    }
                }
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Paste failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }

        finish()
        overridePendingTransition(0, 0)
    }

    companion object {
        const val EXTRA_TARGET = "extra_target"

        fun triggerPaste(context: Context, target: String) {
            val intent = Intent(context, ClipboardReaderActivity::class.java).apply {
                putExtra(EXTRA_TARGET, target)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION or Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
            }
            context.startActivity(intent)
        }
    }
}
