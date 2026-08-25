package com.megalife.translator.ui

import android.app.AlertDialog
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.lifecycle.ViewModelProvider
import com.megalife.translator.BuildConfig
import com.megalife.translator.R
import com.megalife.translator.data.local.PreferencesManager
import com.megalife.translator.data.model.LanguagePair
import com.megalife.translator.viewmodel.HistoryViewModel

class SettingsActivity : BaseActivity() {

    private lateinit var prefs: PreferencesManager
    private lateinit var historyViewModel: HistoryViewModel

    private lateinit var settingLanguagePair: LinearLayout
    private lateinit var settingInstantTranslate: LinearLayout
    private lateinit var settingTts: LinearLayout
    private lateinit var settingTtsSpeed: LinearLayout
    private lateinit var settingFontSize: LinearLayout
    private lateinit var settingHistory: LinearLayout
    private lateinit var settingClearHistory: TextView

    private lateinit var tvSettingLanguage: TextView
    private lateinit var tvSettingInstant: TextView
    private lateinit var tvSettingTts: TextView
    private lateinit var tvSettingTtsSpeed: TextView
    private lateinit var tvSettingFontSize: TextView
    private lateinit var tvSettingHistory: TextView
    private lateinit var tvAppVersion: TextView

    private val focusableViews = mutableListOf<View>()
    private var focusIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        prefs = PreferencesManager(this)
        historyViewModel = ViewModelProvider(this)[HistoryViewModel::class.java]

        bindViews()
        setupFocusables()
        refreshDisplay()

        focusIndex = 0
        updateFocus()
    }

    private fun bindViews() {
        settingLanguagePair = findViewById(R.id.settingLanguagePair)
        settingInstantTranslate = findViewById(R.id.settingInstantTranslate)
        settingTts = findViewById(R.id.settingTts)
        settingTtsSpeed = findViewById(R.id.settingTtsSpeed)
        settingFontSize = findViewById(R.id.settingFontSize)
        settingHistory = findViewById(R.id.settingHistory)
        settingClearHistory = findViewById(R.id.settingClearHistory)

        tvSettingLanguage = findViewById(R.id.tvSettingLanguage)
        tvSettingInstant = findViewById(R.id.tvSettingInstant)
        tvSettingTts = findViewById(R.id.tvSettingTts)
        tvSettingTtsSpeed = findViewById(R.id.tvSettingTtsSpeed)
        tvSettingFontSize = findViewById(R.id.tvSettingFontSize)
        tvSettingHistory = findViewById(R.id.tvSettingHistory)
        tvAppVersion = findViewById(R.id.tvAppVersion)
    }

    private fun setupFocusables() {
        focusableViews.clear()
        focusableViews.addAll(listOf(
            settingLanguagePair,     // 0
            settingInstantTranslate, // 1
            settingTts,              // 2
            settingTtsSpeed,         // 3
            settingFontSize,         // 4
            settingHistory,          // 5
            settingClearHistory      // 6
        ))
    }

    private fun refreshDisplay() {
        val pair = LanguagePair.ALL_PAIRS[prefs.defaultLanguagePairIndex]
        tvSettingLanguage.text = pair.displayName
        tvSettingInstant.text = if (prefs.instantTranslateEnabled) "On" else "Off"
        tvSettingTts.text = if (prefs.ttsEnabled) "On" else "Off"
        tvSettingTtsSpeed.text = when (prefs.ttsSpeed) {
            0 -> getString(R.string.tts_speed_slow)
            2 -> getString(R.string.tts_speed_fast)
            else -> getString(R.string.tts_speed_normal)
        }
        tvSettingFontSize.text = when (prefs.fontSizeLevel) {
            0 -> getString(R.string.font_size_small)
            2 -> getString(R.string.font_size_large)
            else -> getString(R.string.font_size_medium)
        }
        tvSettingHistory.text = if (prefs.historyEnabled) "On" else "Off"
        tvAppVersion.text = BuildConfig.VERSION_NAME
    }

    override fun handleDpadEvent(keyCode: Int, event: KeyEvent?): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_UP -> {
                if (focusIndex > 0) {
                    focusIndex--
                    updateFocus()
                }
                return true
            }
            KeyEvent.KEYCODE_DPAD_DOWN -> {
                if (focusIndex < focusableViews.size - 1) {
                    focusIndex++
                    updateFocus()
                }
                return true
            }
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                handleSettingAction()
                return true
            }
            KeyEvent.KEYCODE_DPAD_LEFT -> {
                handleSettingDecrease()
                return true
            }
            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                handleSettingIncrease()
                return true
            }
            KeyEvent.KEYCODE_BACK -> {
                finish()
                return true
            }
        }
        return false
    }

    private fun handleSettingAction() {
        when (focusIndex) {
            0 -> { // Language pair - cycle
                val newIndex = (prefs.defaultLanguagePairIndex + 1) % LanguagePair.ALL_PAIRS.size
                prefs.defaultLanguagePairIndex = newIndex
                refreshDisplay()
            }
            1 -> { // Toggle instant translate
                prefs.instantTranslateEnabled = !prefs.instantTranslateEnabled
                refreshDisplay()
            }
            2 -> { // Toggle TTS
                prefs.ttsEnabled = !prefs.ttsEnabled
                refreshDisplay()
            }
            3 -> { // Cycle TTS speed
                prefs.ttsSpeed = (prefs.ttsSpeed + 1) % 3
                refreshDisplay()
            }
            4 -> { // Cycle font size
                prefs.fontSizeLevel = (prefs.fontSizeLevel + 1) % 3
                refreshDisplay()
            }
            5 -> { // Toggle history
                prefs.historyEnabled = !prefs.historyEnabled
                // Turning history off also drops cached source text.
                if (!prefs.historyEnabled) historyViewModel.clearCache()
                refreshDisplay()
            }
            6 -> { // Clear history
                showClearHistoryDialog()
            }
        }
    }

    private fun handleSettingIncrease() {
        when (focusIndex) {
            0 -> {
                val newIndex = (prefs.defaultLanguagePairIndex + 1) % LanguagePair.ALL_PAIRS.size
                prefs.defaultLanguagePairIndex = newIndex
                refreshDisplay()
            }
            3 -> {
                prefs.ttsSpeed = (prefs.ttsSpeed + 1).coerceAtMost(2)
                refreshDisplay()
            }
            4 -> {
                prefs.fontSizeLevel = (prefs.fontSizeLevel + 1).coerceAtMost(2)
                refreshDisplay()
            }
        }
    }

    private fun handleSettingDecrease() {
        when (focusIndex) {
            0 -> {
                val current = prefs.defaultLanguagePairIndex
                val newIndex = if (current == 0) LanguagePair.ALL_PAIRS.size - 1 else current - 1
                prefs.defaultLanguagePairIndex = newIndex
                refreshDisplay()
            }
            3 -> {
                prefs.ttsSpeed = (prefs.ttsSpeed - 1).coerceAtLeast(0)
                refreshDisplay()
            }
            4 -> {
                prefs.fontSizeLevel = (prefs.fontSizeLevel - 1).coerceAtLeast(0)
                refreshDisplay()
            }
        }
    }

    private fun showClearHistoryDialog() {
        AlertDialog.Builder(this, R.style.AppTheme)
            .setTitle(R.string.clear_history_confirm_title)
            .setMessage(R.string.clear_history_confirm_message)
            .setNegativeButton(R.string.btn_cancel, null)
            .setPositiveButton(R.string.btn_ok) { _, _ ->
                historyViewModel.deleteAll()
            }
            .show()
    }

    private fun updateFocus() {
        for ((i, view) in focusableViews.withIndex()) {
            if (i == focusIndex) {
                view.requestFocus()
            } else {
                view.clearFocus()
            }
        }
    }
}
