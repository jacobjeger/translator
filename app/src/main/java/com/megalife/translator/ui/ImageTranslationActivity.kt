package com.megalife.translator.ui

import android.content.ContentValues
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.KeyEvent
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import com.megalife.translator.R
import com.megalife.translator.util.ClipboardHelper
import com.megalife.translator.viewmodel.ImageTranslationViewModel

class ImageTranslationActivity : BaseActivity() {

    private lateinit var viewModel: ImageTranslationViewModel

    private lateinit var ivTranslatedImage: ImageView
    private lateinit var loadingContainer: LinearLayout
    private lateinit var tvLoadingStatus: TextView
    private lateinit var tvError: TextView
    private lateinit var btnCopyAll: TextView
    private lateinit var btnSaveImage: TextView
    private lateinit var btnBack: TextView

    private var zoomLevel = 1.0f
    private var focusIndex = 0 // 0=copy, 1=save, 2=back
    private val actionButtons = mutableListOf<View>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_translation)

        viewModel = ViewModelProvider(this)[ImageTranslationViewModel::class.java]

        bindViews()
        observeViewModel()

        val imagePath = intent.getStringExtra("image_path")
        val imageUri = intent.getStringExtra("image_uri")
        val sourceLang = intent.getStringExtra("source_lang") ?: "en"
        val targetLang = intent.getStringExtra("target_lang") ?: "he"

        viewModel.processImage(imagePath, imageUri, sourceLang, targetLang)
    }

    private fun bindViews() {
        ivTranslatedImage = findViewById(R.id.ivTranslatedImage)
        loadingContainer = findViewById(R.id.loadingContainer)
        tvLoadingStatus = findViewById(R.id.tvLoadingStatus)
        tvError = findViewById(R.id.tvError)
        btnCopyAll = findViewById(R.id.btnCopyAll)
        btnSaveImage = findViewById(R.id.btnSaveImage)
        btnBack = findViewById(R.id.btnBack)

        actionButtons.addAll(listOf(btnCopyAll, btnSaveImage, btnBack))
    }

    private fun observeViewModel() {
        viewModel.isProcessing.observe(this) { processing ->
            loadingContainer.visibility = if (processing) View.VISIBLE else View.GONE
        }

        viewModel.resultBitmap.observe(this) { bitmap ->
            bitmap?.let {
                ivTranslatedImage.setImageBitmap(it)
            }
        }

        viewModel.errorMessage.observe(this) { error ->
            if (error != null) {
                tvError.text = error
                tvError.visibility = View.VISIBLE
            } else {
                tvError.visibility = View.GONE
            }
        }

        viewModel.noTextFound.observe(this) { noText ->
            if (noText) {
                tvError.text = getString(R.string.no_text_found)
                tvError.visibility = View.VISIBLE
                focusIndex = 2 // Focus on Back button
                updateFocus()
            }
        }
    }

    override fun handleDpadEvent(keyCode: Int, event: KeyEvent?): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_UP -> {
                zoomLevel = (zoomLevel + 0.25f).coerceAtMost(3.0f)
                ivTranslatedImage.scaleX = zoomLevel
                ivTranslatedImage.scaleY = zoomLevel
                return true
            }
            KeyEvent.KEYCODE_DPAD_DOWN -> {
                zoomLevel = (zoomLevel - 0.25f).coerceAtLeast(0.5f)
                ivTranslatedImage.scaleX = zoomLevel
                ivTranslatedImage.scaleY = zoomLevel
                return true
            }
            KeyEvent.KEYCODE_DPAD_LEFT -> {
                if (focusIndex > 0) {
                    focusIndex--
                    updateFocus()
                }
                return true
            }
            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                if (focusIndex < actionButtons.size - 1) {
                    focusIndex++
                    updateFocus()
                }
                return true
            }
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                event?.startTracking()
                handleCenterPress()
                return true
            }
            KeyEvent.KEYCODE_BACK -> {
                finish()
                return true
            }
        }
        return false
    }

    override fun handleLongPress(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER) {
            saveImageToGallery()
            return true
        }
        return false
    }

    private fun handleCenterPress() {
        when (focusIndex) {
            0 -> copyAllText()
            1 -> saveImageToGallery()
            2 -> finish()
        }
    }

    private fun copyAllText() {
        val text = viewModel.allTranslatedText.value
        if (!text.isNullOrBlank()) {
            ClipboardHelper.setText(this, text)
            window.decorView.rootView.performHapticFeedback(
                android.view.HapticFeedbackConstants.LONG_PRESS
            )
            Toast.makeText(this, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveImageToGallery() {
        val bitmap = viewModel.getResultBitmap() ?: return

        try {
            val filename = "translated_${System.currentTimeMillis()}.png"
            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Translator")
                }
            }

            val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            uri?.let {
                contentResolver.openOutputStream(it)?.use { outputStream ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                }
                Toast.makeText(this, R.string.image_saved, Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to save: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateFocus() {
        for ((i, view) in actionButtons.withIndex()) {
            if (i == focusIndex) {
                view.requestFocus()
            } else {
                view.clearFocus()
            }
        }
    }
}
