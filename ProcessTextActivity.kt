package com.example.markdowntextcleaner

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast

/**
 * Handles the long-press "Process Text" selection menu entry (the same menu that hosts
 * "Copy", "Share", "Translate", etc. in any app). This activity is intentionally invisible:
 * it never shows any UI, so selecting "Markdown Text Cleaner" from that menu never switches
 * you away from whatever app you were in.
 *
 * - If the selected text is editable (a text field, a note, a message draft), the cleaned
 *   text is handed back to Android, which replaces the selection in place automatically.
 * - If the selected text is read-only (a webpage, a PDF, a chat you can't edit), Android has
 *   no way to replace it, so the cleaned text is instead copied to the clipboard for you to
 *   paste manually wherever you like.
 *
 * Either way, this activity finishes itself immediately - no window, no focus change.
 */
class ProcessTextActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val selectedText = intent?.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)?.toString()
        if (selectedText == null) {
            finish()
            return
        }

        val isReadOnly = intent.getBooleanExtra(Intent.EXTRA_PROCESS_TEXT_READONLY, true)
        val cleaned = cleanMarkdownText(selectedText)

        if (!isReadOnly) {
            // Editable selection: hand the cleaned text back so Android replaces it in place.
            val resultIntent = Intent()
            resultIntent.putExtra(Intent.EXTRA_PROCESS_TEXT, cleaned as CharSequence)
            setResult(RESULT_OK, resultIntent)
            copyToClipboard(cleaned, showToast = false)
        } else {
            // Read-only selection: can't replace it, so copy the result to the clipboard
            // and let the user know via a toast, without ever opening any app window.
            copyToClipboard(cleaned, showToast = true)
        }

        finish()
    }

    private fun cleanMarkdownText(text: String): String {
        var result = text.replace(Regex("[\\r\\n]+"), " ")
        result = result.replace(Regex("\\s+"), " ")
        return result.trim()
    }

    private fun copyToClipboard(text: String, showToast: Boolean) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Cleaned Text", text)
        clipboard.setPrimaryClip(clip)
        if (showToast) {
            Toast.makeText(this, "Copied cleaned text to clipboard", Toast.LENGTH_SHORT).show()
        }
    }
}
