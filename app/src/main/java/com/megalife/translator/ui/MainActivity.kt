package com.megalife.translator.ui

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.TypedValue
import android.view.KeyEvent
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.ViewModelProvider
import com.megalife.translator.R
import com.megalife.translator.data.model.LanguagePair
import com.megalife.translator.util.ClipboardHelper
import com.megalife.translator.util.PermissionHelper
import com.megalife.translator.util.TtsManager
import com.megalife.translator.viewmodel.MainViewModel

class MainActivity : BaseActivity() {

    private lateinit var viewModel: MainViewModel
    private lateinit var ttsManager: TtsManager

    private lateinit var tvLanguagePair: TextView
    private lateinit var etSourceText: EditText
    private lateinit var tvTranslationOutput: TextView
    private lateinit var tvStatus: TextView
    private lateinit var btnCamera: TextView
    private lateinit var btnGallery: TextView
    private lateinit var btnHistory: TextView
    private lateinit var btnSettings: TextView

    // Focus management: 0=language pair, 1=input, 2=output, 3-6=action buttons
    private var focusIndex = 1
    private val focusableViews = mutableListOf<View>()

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            launchCamera()
        } else {
            Toast.makeText(this, R.string.permission_denied, Toast.LENGTH_SHORT).show()
        }
    }

    private val galleryPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            launchGallery()
        } else {
            Toast.makeText(this, R.string.permission_denied, Toast.LENGTH_SHORT).show()
        }
    }

    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let {
            val intent = Intent(this, ImageTranslationActivity::class.java)
            intent.putExtra("image_uri", it.toString())
            intent.putExtra("source_lang", viewModel.currentPair.sourceCode)
            intent.putExtra("target_lang", viewModel.currentPair.targetCode)
            startActivityWithFade(intent)
        }
    }

    // Result from history screen to reload a translation
    private val historyLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val data = result.data
            val sourceText = data?.getStringExtra("source_text") ?: return@registerForActivityResult
            val translatedText = data.getStringExtra("translated_text") ?: ""
            val pairIndex = data.getIntExtra("pair_index", -1)
            
            etSourceText.setText(sourceText)
            tvTranslationOutput.text = translatedText
            if (pairIndex >= 0) {
                // Will trigger re-observation
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        viewModel = ViewModelProvider(this)[MainViewModel::class.java]
        ttsManager = TtsManager(this)

        bindViews()
        setupFocusables()
        observeViewModel()
        applyFontSize()

        // Clipboard auto-detect
        val clipText = ClipboardHelper.getText(this)
        if (!clipText.isNullOrBlank() && etSourceText.text.isNullOrBlank()) {
            etSourceText.setText(clipText)
            viewModel.translateNow(clipText)
        }

        // Text change listener for instant translation
        etSourceText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (viewModel.prefs.instantTranslateEnabled) {
                    viewModel.translateDebounced(s?.toString() ?: "")
                }
            }
        })

        // Set initial focus
        focusIndex = 1
        updateFocus()
    }

    private fun bindViews() {
        tvLanguagePair = findViewById(R.id.tvLanguagePair)
        etSourceText = findViewById(R.id.etSourceText)
        tvTranslationOutput = findViewById(R.id.tvTranslationOutput)
        tvStatus = findViewById(R.id.tvStatus)
        btnCamera = findViewById(R.id.btnCamera)
        btnGallery = findViewById(R.id.btnGallery)
        btnHistory = findViewById(R.id.btnHistory)
        btnSettings = findViewById(R.id.btnSettings)
    }

    private fun setupFocusables() {
        focusableViews.clear()
        focusableViews.addAll(listOf(
            tvLanguagePair,   // 0
            etSourceText,     // 1
            tvTranslationOutput, // 2
            btnCamera,        // 3
            btnGallery,       // 4
            btnHistory,       // 5
            btnSettings       // 6
        ))
    }

    private fun observeViewModel() {
        viewModel.currentPairIndex.observe(this) { index ->
            val pair = LanguagePair.ALL_PAIRS[index]
            tvLanguagePair.text = pair.displayName

            // Set text direction based on target language
            if (LanguagePair.isRtl(pair.targetCode)) {
                tvTranslationOutput.textDirection = View.TEXT_DIRECTION_RTL
            } else {
                tvTranslationOutput.textDirection = View.TEXT_DIRECTION_LTR
            }

            // Set input direction based on source language
            if (LanguagePair.isRtl(pair.sourceCode)) {
                etSourceText.textDirection = View.TEXT_DIRECTION_RTL
            } else {
                etSourceText.textDirection = View.TEXT_DIRECTION_LTR
            }

            // Re-translate if there's text
            val text = etSourceText.text?.toString()
            if (!text.isNullOrBlank()) {
                viewModel.translateNow(text)
            }
        }

        viewModel.translatedText.observe(this) { text ->
            tvTranslationOutput.text = text
        }

        viewModel.statusMessage.observe(this) { message ->
            if (message != null) {
                tvStatus.text = message
                tvStatus.visibility = View.VISIBLE
            } else {
                tvStatus.visibility = View.GONE
            }
        }

        viewModel.isTranslating.observe(this) { translating ->
            if (translating) {
                tvStatus.text = getString(R.string.translating)
                tvStatus.visibility = View.VISIBLE
            } else if (viewModel.statusMessage.value == null) {
                tvStatus.visibility = View.GONE
            }
        }

        viewModel.contentBlocked.observe(this) { blocked ->
            if (blocked) {
                etSourceText.text?.clear()
                tvTranslationOutput.text = ""
                val msg = getString(R.string.content_blocked)
                tvStatus.text = msg
                tvStatus.visibility = View.VISIBLE
                Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
                focusIndex = 1
                updateFocus()
            }
        }
    }

    private fun applyFontSize() {
        val sp = viewModel.prefs.fontSizeSp
        etSourceText.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp)
        tvTranslationOutput.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp)
    }

    override fun handleDpadEvent(keyCode: Int, event: KeyEvent?): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_UP -> {
                if (focusIndex > 0) {
                    focusIndex--
                    // Skip from action buttons to output
                    if (focusIndex == 2 && tvTranslationOutput.text.isNullOrBlank()) {
                        focusIndex = 1
                    }
                    updateFocus()
                }
                return true
            }
            KeyEvent.KEYCODE_DPAD_DOWN -> {
                if (focusIndex < focusableViews.size - 1) {
                    focusIndex++
                    // Jump from output to first action button
                    if (focusIndex == 2 && tvTranslationOutput.text.isNullOrBlank()) {
                        focusIndex = 3
                    }
                    updateFocus()
                }
                return true
            }
            KeyEvent.KEYCODE_DPAD_LEFT -> {
                if (focusIndex == 0) {
                    viewModel.cyclePairBackward()
                } else if (focusIndex in 3..6) {
                    focusIndex = (focusIndex - 1).coerceAtLeast(3)
                    updateFocus()
                }
                return true
            }
            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                if (focusIndex == 0) {
                    viewModel.cyclePairForward()
                } else if (focusIndex in 3..5) {
                    focusIndex = (focusIndex + 1).coerceAtMost(6)
                    updateFocus()
                }
                return true
            }
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                event?.startTracking() // Enable long press detection
                handleCenterPress()
                return true
            }
            KeyEvent.KEYCODE_STAR -> {
                // Clear input and output
                etSourceText.text?.clear()
                tvTranslationOutput.text = ""
                viewModel.clearStatus()
                focusIndex = 1
                updateFocus()
                return true
            }
            KeyEvent.KEYCODE_POUND -> {
                // Swap languages
                viewModel.swapLanguages()
                return true
            }
            // Number keys type directly into input field
            KeyEvent.KEYCODE_0, KeyEvent.KEYCODE_1, KeyEvent.KEYCODE_2,
            KeyEvent.KEYCODE_3, KeyEvent.KEYCODE_4, KeyEvent.KEYCODE_5,
            KeyEvent.KEYCODE_6, KeyEvent.KEYCODE_7, KeyEvent.KEYCODE_8,
            KeyEvent.KEYCODE_9 -> {
                val digit = keyCode - KeyEvent.KEYCODE_0
                etSourceText.append(digit.toString())
                if (focusIndex != 1) {
                    focusIndex = 1
                    updateFocus()
                }
                return true
            }
        }
        return false
    }

    override fun handleLongPress(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER) {
            // Long press CENTER reads translation aloud
            val text = tvTranslationOutput.text?.toString()
            if (!text.isNullOrBlank() && viewModel.prefs.ttsEnabled) {
                ttsManager.speak(
                    text,
                    viewModel.currentPair.targetCode,
                    viewModel.prefs.ttsSpeedFloat
                )
            }
            return true
        }
        return false
    }

    private fun handleCenterPress() {
        when (focusIndex) {
            0 -> {
                // Language pair - cycle forward
                viewModel.cyclePairForward()
            }
            1 -> {
                // Input field - translate now (for manual mode)
                val text = etSourceText.text?.toString()
                if (!text.isNullOrBlank()) {
                    viewModel.translateNow(text)
                }
            }
            2 -> {
                // Output field - copy to clipboard
                copyTranslation()
            }
            3 -> {
                // Camera button
                if (PermissionHelper.hasCameraPermission(this)) {
                    launchCamera()
                } else {
                    PermissionHelper.requestCameraWithRationale(
                        this, cameraPermissionLauncher,
                        getString(R.string.permission_camera_rationale)
                    )
                }
            }
            4 -> {
                // Gallery button
                launchGallery()
            }
            5 -> {
                // History button
                val intent = Intent(this, HistoryActivity::class.java)
                historyLauncher.launch(intent)
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
            }
            6 -> {
                // Settings button
                val intent = Intent(this, SettingsActivity::class.java)
                startActivityWithFade(intent)
            }
        }
    }

    private fun copyTranslation() {
        val text = tvTranslationOutput.text?.toString()
        if (!text.isNullOrBlank()) {
            ClipboardHelper.setText(this, text)
            window.decorView.rootView.performHapticFeedback(
                android.view.HapticFeedbackConstants.LONG_PRESS
            )
            Toast.makeText(this, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show()
        }
    }

    private fun launchCamera() {
        val intent = Intent(this, CameraActivity::class.java)
        intent.putExtra("source_lang", viewModel.currentPair.sourceCode)
        intent.putExtra("target_lang", viewModel.currentPair.targetCode)
        startActivityWithFade(intent)
    }

    private fun launchGallery() {
        galleryLauncher.launch(
            androidx.activity.result.PickVisualMediaRequest(
                ActivityResultContracts.PickVisualMedia.ImageOnly
            )
        )
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

    override fun onResume() {
        super.onResume()
        applyFontSize()
    }

    override fun onDestroy() {
        super.onDestroy()
        ttsManager.shutdown()
    }
}
