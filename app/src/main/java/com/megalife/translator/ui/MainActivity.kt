package com.megalife.translator.ui

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.TypedValue
import android.view.KeyEvent
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import com.megalife.translator.R
import com.megalife.translator.data.local.PreferencesManager
import com.megalife.translator.data.model.Language
import com.megalife.translator.util.ClipboardHelper
import com.megalife.translator.util.PermissionHelper
import com.megalife.translator.util.TtsManager
import com.megalife.translator.viewmodel.MainViewModel

class MainActivity : BaseActivity() {

    private val viewModel: MainViewModel by viewModels()

    private lateinit var tvLanguagePair: TextView
    private lateinit var etSourceText: EditText
    private lateinit var tvTranslationOutput: TextView
    private lateinit var tvTranslating: TextView
    private lateinit var btnCamera: Button
    private lateinit var btnGallery: Button
    private lateinit var btnHistory: Button

    private lateinit var ttsManager: TtsManager
    private lateinit var prefs: PreferencesManager

    // Focus tracking: 0=langPair, 1=input, 2=output, 3=camera, 4=gallery, 5=history
    private var focusIndex = 1
    private lateinit var focusableViews: List<View>

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) launchCamera()
    }

    private val galleryPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) launchGallery()
    }

    private val galleryPickerLauncher = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val intent = Intent(this, ImageTranslationActivity::class.java).apply {
                putExtra(ImageTranslationActivity.EXTRA_IMAGE_URI, uri.toString())
                putExtra(ImageTranslationActivity.EXTRA_SOURCE_LANG, viewModel.currentPair.value?.sourceLanguage?.code)
                putExtra(ImageTranslationActivity.EXTRA_TARGET_LANG, viewModel.currentPair.value?.targetLanguage?.code)
            }
            startActivityWithFade(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        prefs = PreferencesManager(this)
        ttsManager = TtsManager(this)

        initViews()
        initObservers()
        applyFontSize()

        // Clipboard auto-detect on launch
        val clipText = ClipboardHelper.getText(this)
        if (!clipText.isNullOrBlank() && etSourceText.text.isEmpty()) {
            etSourceText.setText(clipText)
            viewModel.translateImmediate(clipText)
        }

        setFocus(1) // Start on input field
    }

    private fun initViews() {
        tvLanguagePair = findViewById(R.id.tvLanguagePair)
        etSourceText = findViewById(R.id.etSourceText)
        tvTranslationOutput = findViewById(R.id.tvTranslationOutput)
        tvTranslating = findViewById(R.id.tvTranslating)
        btnCamera = findViewById(R.id.btnCamera)
        btnGallery = findViewById(R.id.btnGallery)
        btnHistory = findViewById(R.id.btnHistory)

        focusableViews = listOf(tvLanguagePair, etSourceText, tvTranslationOutput, btnCamera, btnGallery, btnHistory)

        // Text change listener for instant translation
        etSourceText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (prefs.instantTranslateEnabled) {
                    viewModel.translateDebounced(s?.toString() ?: "")
                }
            }
        })
    }

    private fun initObservers() {
        viewModel.currentPair.observe(this) { pair ->
            tvLanguagePair.text = pair.displayName
        }

        viewModel.translatedText.observe(this) { text ->
            tvTranslationOutput.text = text
        }

        viewModel.isTranslating.observe(this) { translating ->
            tvTranslating.visibility = if (translating) View.VISIBLE else View.GONE
        }

        viewModel.contentBlocked.observe(this) { blocked ->
            if (blocked) {
                etSourceText.text.clear()
                tvTranslationOutput.text = ""
                Toast.makeText(this, R.string.content_blocked, Toast.LENGTH_SHORT).show()
                setFocus(1) // Return focus to input field
            }
        }

        viewModel.errorMessage.observe(this) { msg ->
            if (msg != null) {
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun applyFontSize() {
        val fontSize = prefs.fontSizeSp
        etSourceText.setTextSize(TypedValue.COMPLEX_UNIT_SP, fontSize)
        tvTranslationOutput.setTextSize(TypedValue.COMPLEX_UNIT_SP, fontSize)
    }

    override fun onResume() {
        super.onResume()
        applyFontSize()
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

    override fun handleDpadEvent(keyCode: Int, event: KeyEvent?): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_UP -> {
                if (focusIndex > 0) {
                    // From action buttons row, go to output
                    if (focusIndex in 3..5) setFocus(2)
                    else setFocus(focusIndex - 1)
                } else {
                    // From language pair, go to settings
                    startActivityWithFade(Intent(this, SettingsActivity::class.java))
                }
                return true
            }
            KeyEvent.KEYCODE_DPAD_DOWN -> {
                if (focusIndex < 3) {
                    setFocus(focusIndex + 1)
                } else if (focusIndex == 2) {
                    setFocus(3) // From output to first button
                }
                // UP from buttons goes to history screen
                if (focusIndex in 3..5) {
                    // Already at bottom row, do nothing
                }
                return true
            }
            KeyEvent.KEYCODE_DPAD_LEFT -> {
                if (focusIndex == 0) {
                    // Cycle language pairs backward
                    viewModel.cycleLanguagePair(false)
                } else if (focusIndex in 3..5 && focusIndex > 3) {
                    setFocus(focusIndex - 1)
                }
                return true
            }
            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                if (focusIndex == 0) {
                    // Cycle language pairs forward
                    viewModel.cycleLanguagePair(true)
                } else if (focusIndex in 3..4) {
                    setFocus(focusIndex + 1)
                }
                return true
            }
            KeyEvent.KEYCODE_DPAD_CENTER -> {
                event?.startTracking() // Enable long press detection
                return true
            }
            KeyEvent.KEYCODE_STAR -> {
                // * clears input and output
                etSourceText.text.clear()
                viewModel.clearTranslation()
                Toast.makeText(this, R.string.input_cleared, Toast.LENGTH_SHORT).show()
                setFocus(1)
                return true
            }
            KeyEvent.KEYCODE_POUND -> {
                // # swaps languages and re-translates
                viewModel.swapLanguages()
                Toast.makeText(this, R.string.languages_swapped, Toast.LENGTH_SHORT).show()
                val text = etSourceText.text.toString()
                // Swap source text with translation output
                val translated = tvTranslationOutput.text.toString()
                if (translated.isNotBlank()) {
                    etSourceText.setText(translated)
                    viewModel.translateImmediate(translated)
                }
                return true
            }
            // Number keys 0-9 type directly into input field
            in KeyEvent.KEYCODE_0..KeyEvent.KEYCODE_9 -> {
                val char = (keyCode - KeyEvent.KEYCODE_0 + '0'.code).toChar()
                etSourceText.append(char.toString())
                if (focusIndex != 1) setFocus(1)
                return true
            }
        }
        return false
    }

    override fun handleLongPress(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
            // Long-press CENTER reads translation aloud
            val text = tvTranslationOutput.text.toString()
            if (text.isNotBlank() && prefs.ttsEnabled) {
                val targetLang = viewModel.currentPair.value?.targetLanguage ?: Language.ENGLISH
                ttsManager.speak(text, targetLang, prefs.ttsSpeedFloat)
            }
            return true
        }
        return false
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER && event?.isTracking == true && !event.isCanceled) {
            // Short press CENTER
            when (focusIndex) {
                0 -> {
                    // On language selector — do nothing special, LEFT/RIGHT cycles
                }
                1, 2 -> {
                    // Copy translation to clipboard
                    val text = tvTranslationOutput.text.toString()
                    if (text.isNotBlank()) {
                        ClipboardHelper.setText(this, text)
                        window.decorView.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
                        Toast.makeText(this, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show()
                    }
                }
                3 -> {
                    // Camera button
                    if (PermissionHelper.hasCameraPermission(this)) {
                        launchCamera()
                    } else {
                        PermissionHelper.requestWithRationale(
                            this,
                            getString(R.string.camera_permission_rationale),
                            cameraPermissionLauncher,
                            Manifest.permission.CAMERA
                        )
                    }
                }
                4 -> {
                    // Gallery button
                    launchGallery()
                }
                5 -> {
                    // History button
                    startActivityWithFade(Intent(this, HistoryActivity::class.java))
                }
            }
            return true
        }
        return super.onKeyUp(keyCode, event)
    }

    private fun launchCamera() {
        val intent = Intent(this, CameraActivity::class.java).apply {
            putExtra(CameraActivity.EXTRA_SOURCE_LANG, viewModel.currentPair.value?.sourceLanguage?.code)
            putExtra(CameraActivity.EXTRA_TARGET_LANG, viewModel.currentPair.value?.targetLanguage?.code)
        }
        startActivityWithFade(intent)
    }

    private fun launchGallery() {
        galleryPickerLauncher.launch(
            androidx.activity.result.PickVisualMediaRequest(
                ActivityResultContracts.PickVisualMedia.ImageOnly
            )
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        ttsManager.shutdown()
    }
}
