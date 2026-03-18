package com.megalife.translator.ui

import android.app.AlertDialog
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import com.megalife.translator.BuildConfig
import com.megalife.translator.R
import com.megalife.translator.data.local.PreferencesManager
import com.megalife.translator.data.model.LanguagePair
import com.megalife.translator.viewmodel.HistoryViewModel

class SettingsActivity : BaseActivity() {

    private val historyViewModel: HistoryViewModel by viewModels()
    private lateinit var prefs: PreferencesManager

    private lateinit var settingDefaultLang: LinearLayout
    private lateinit var tvDefaultLangValue: TextView
    private lateinit var settingInstantTranslate: LinearLayout
    private lateinit var tvInstantTranslateValue: TextView
    private lateinit var settingTts: LinearLayout
    private lateinit var tvTtsValue: TextView
    private lateinit var settingTtsSpeed: LinearLayout
    private lateinit var tvTtsSpeedValue: TextView
    private lateinit var settingFontSize: LinearLayout
    private lateinit var tvFontSizeValue: TextView
    private lateinit var settingHistory: LinearLayout
    private lateinit var tvHistoryValue: TextView
    private lateinit var settingClearHistory: TextView
    private lateinit var tvAppVersion: TextView

    // Focus: 0=defaultLang, 1=instantTranslate, 2=tts, 3=ttsSpeed, 4=fontSize, 5=history, 6=clearHistory
    private var focusIndex = 0
    private lateinit var focusableViews: List<View>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        prefs = PreferencesManager(this)
        initViews()
        updateAllValues()
        setFocus(0)
    }

    private fun initViews() {
        settingDefaultLang = findViewById(R.id.settingDefaultLang)
        tvDefaultLangValue = findViewById(R.id.tvDefaultLangValue)
        settingInstantTranslate = findViewById(R.id.settingInstantTranslate)
        tvInstantTranslateValue = findViewById(R.id.tvInstantTranslateValue)
        settingTts = findViewById(R.id.settingTts)
        tvTtsValue = findViewById(R.id.tvTtsValue)
        settingTtsSpeed = findViewById(R.id.settingTtsSpeed)
        tvTtsSpeedValue = findViewById(R.id.tvTtsSpeedValue)
        settingFontSize = findViewById(R.id.settingFontSize)
        tvFontSizeValue = findViewById(R.id.tvFontSizeValue)
        settingHistory = findViewById(R.id.settingHistory)
        tvHistoryValue = findViewById(R.id.tvHistoryValue)
        settingClearHistory = findViewById(R.id.settingClearHistory)
        tvAppVersion = findViewById(R.id.tvAppVersion)

        tvAppVersion.text = BuildConfig.VERSION_NAME

        focusableViews = listOf(
            settingDefaultLang,
            settingInstantTranslate,
            settingTts,
            settingTtsSpeed,
            settingFontSize,
            settingHistory,
            settingClearHistory
        )
    }

    private fun updateAllValues() {
        val pairIndex = prefs.defaultLanguagePairIndex.coerceIn(0, LanguagePair.ALL_PAIRS.size - 1)
        tvDefaultLangValue.text = LanguagePair.ALL_PAIRS[pairIndex].displayName
        tvInstantTranslateValue.text = if (prefs.instantTranslateEnabled) "ON" else "OFF"
        tvTtsValue.text = if (prefs.ttsEnabled) "ON" else "OFF"
        tvTtsSpeedValue.text = when (prefs.ttsSpeed) {
            PreferencesManager.TTS_SPEED_SLOW -> getString(R.string.setting_tts_slow)
            PreferencesManager.TTS_SPEED_FAST -> getString(R.string.setting_tts_fast)
            else -> getString(R.string.setting_tts_normal)
        }
        tvFontSizeValue.text = when (prefs.fontSizeOption) {
            PreferencesManager.FONT_SIZE_SMALL -> getString(R.string.setting_font_small)
            PreferencesManager.FONT_SIZE_LARGE -> getString(R.string.setting_font_large)
            else -> getString(R.string.setting_font_medium)
        }
        tvHistoryValue.text = if (prefs.historyEnabled) "ON" else "OFF"
    }

    private fun setFocus(index: Int) {
        focusIndex = index.coerceIn(0, focusableViews.size - 1)
        for ((i, view) in focusableViews.withIndex()) {
            if (i == focusIndex) {
                view.requestFocus()
            } else {
                view.clearFocus()
            }
        }
    }

    private fun handleSettingAction() {
        when (focusIndex) {
            0 -> {
                // Cycle default language pair
                var index = prefs.defaultLanguagePairIndex
                index = (index + 1) % LanguagePair.ALL_PAIRS.size
                prefs.defaultLanguagePairIndex = index
                updateAllValues()
            }
            1 -> {
                // Toggle instant translate
                prefs.instantTranslateEnabled = !prefs.instantTranslateEnabled
                updateAllValues()
            }
            2 -> {
                // Toggle TTS
                prefs.ttsEnabled = !prefs.ttsEnabled
                updateAllValues()
            }
            3 -> {
                // Cycle TTS speed
                val current = prefs.ttsSpeed
                prefs.ttsSpeed = (current + 1) % 3
                updateAllValues()
            }
            4 -> {
                // Cycle font size
                val current = prefs.fontSizeOption
                prefs.fontSizeOption = (current + 1) % 3
                updateAllValues()
            }
            5 -> {
                // Toggle history
                prefs.historyEnabled = !prefs.historyEnabled
                updateAllValues()
            }
            6 -> {
                // Clear all history with confirmation
                AlertDialog.Builder(this, R.style.Theme_MegaLifeTranslator)
                    .setTitle(R.string.setting_clear_history)
                    .setMessage(R.string.setting_clear_history_confirm)
                    .setNegativeButton(R.string.btn_cancel) { dialog, _ ->
                        dialog.dismiss()
                    }
                    .setPositiveButton(R.string.btn_delete) { _, _ ->
                        historyViewModel.deleteAll()
                        Toast.makeText(this, R.string.history_cleared, Toast.LENGTH_SHORT).show()
                    }
                    .show()
            }
        }
    }

    override fun handleDpadEvent(keyCode: Int, event: KeyEvent?): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_UP -> {
                if (focusIndex > 0) setFocus(focusIndex - 1)
                return true
            }
            KeyEvent.KEYCODE_DPAD_DOWN -> {
                if (focusIndex < focusableViews.size - 1) setFocus(focusIndex + 1)
                return true
            }
            KeyEvent.KEYCODE_DPAD_CENTER -> {
                handleSettingAction()
                return true
            }
            KeyEvent.KEYCODE_BACK -> {
                finish()
                return true
            }
        }
        return false
    }
}
