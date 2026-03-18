package com.megalife.translator.ui

import android.content.Intent
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.KeyEvent
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.megalife.translator.R

abstract class BaseActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        // Haptic feedback on every keypress
        val rootView = window.decorView.rootView
        rootView.performHapticFeedback(
            HapticFeedbackConstants.KEYBOARD_TAP,
            HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
        )
        
        if (handleDpadEvent(keyCode, event)) {
            return true
        }

        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyLongPress(keyCode: Int, event: KeyEvent?): Boolean {
        if (handleLongPress(keyCode, event)) {
            return true
        }
        return super.onKeyLongPress(keyCode, event)
    }

    abstract fun handleDpadEvent(keyCode: Int, event: KeyEvent?): Boolean

    open fun handleLongPress(keyCode: Int, event: KeyEvent?): Boolean = false

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
    }

    fun startActivityWithFade(intent: Intent) {
        startActivity(intent)
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
    }

    protected fun updateFocusVisual(views: List<View>, focusIndex: Int) {
        for ((i, view) in views.withIndex()) {
            view.isSelected = i == focusIndex
            view.isFocused
            if (i == focusIndex) {
                view.requestFocus()
            }
        }
    }
}
