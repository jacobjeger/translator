package com.megalife.translator.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context

object ClipboardHelper {

    fun getText(context: Context): String? {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = clipboard.primaryClip ?: return null
        if (clip.itemCount == 0) return null
        return clip.getItemAt(0).text?.toString()
    }

    fun setText(context: Context, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("translation", text)
        clipboard.setPrimaryClip(clip)
    }
}
