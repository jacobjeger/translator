package com.megalife.translator.ui

import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.KeyEvent
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import com.megalife.translator.R
import com.megalife.translator.data.model.Language
import com.megalife.translator.util.ClipboardHelper
import com.megalife.translator.viewmodel.ImageTranslationState
import com.megalife.translator.viewmodel.ImageTranslationViewModel
import java.io.File

class ImageTranslationActivity : BaseActivity() {

    companion object {
        const val EXTRA_IMAGE_URI = "image_uri"
        const val EXTRA_IMAGE_PATH = "image_path"
        const val EXTRA_SOURCE_LANG = "source_lang"
        const val EXTRA_TARGET_LANG = "target_lang"
    }

    private val viewModel: ImageTranslationViewModel by viewModels()

    private lateinit var ivTranslatedImage: ImageView
    private lateinit var loadingOverlay: LinearLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var tvLoadingStatus: TextView
    private lateinit var tvNoText: TextView
    private lateinit var scrollExtractedText: ScrollView
    private lateinit var tvExtractedText: TextView
    private lateinit var btnCopyAll: Button
    private lateinit var btnSaveImage: Button
    private lateinit var btnBack: Button

    private var allTranslatedText = ""
    private var zoomLevel = 1.0f

    // Focus: 0=copyAll, 1=saveImage, 2=back
    private var focusIndex = 0
    private lateinit var focusableButtons: List<View>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_translation)

        initViews()
        initObservers()

        val sourceLangCode = intent.getStringExtra(EXTRA_SOURCE_LANG)
        val targetLangCode = intent.getStringExtra(EXTRA_TARGET_LANG) ?: "he"
        val sourceLang = sourceLangCode?.let { Language.fromCode(it) }
        val targetLang = Language.fromCode(targetLangCode) ?: Language.HEBREW

        // Load image from URI or file path
        val imageUri = intent.getStringExtra(EXTRA_IMAGE_URI)
        val imagePath = intent.getStringExtra(EXTRA_IMAGE_PATH)

        when {
            imageUri != null -> {
                viewModel.processImage(Uri.parse(imageUri), sourceLang, targetLang)
            }
            imagePath != null -> {
                val bitmap = BitmapFactory.decodeFile(imagePath)
                if (bitmap != null) {
                    viewModel.processImage(bitmap, sourceLang, targetLang)
                } else {
                    Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            else -> {
                finish()
            }
        }
    }

    private fun initViews() {
        ivTranslatedImage = findViewById(R.id.ivTranslatedImage)
        loadingOverlay = findViewById(R.id.loadingOverlay)
        progressBar = findViewById(R.id.progressBar)
        tvLoadingStatus = findViewById(R.id.tvLoadingStatus)
        tvNoText = findViewById(R.id.tvNoText)
        scrollExtractedText = findViewById(R.id.scrollExtractedText)
        tvExtractedText = findViewById(R.id.tvExtractedText)
        btnCopyAll = findViewById(R.id.btnCopyAll)
        btnSaveImage = findViewById(R.id.btnSaveImage)
        btnBack = findViewById(R.id.btnBack)

        focusableButtons = listOf(btnCopyAll, btnSaveImage, btnBack)
    }

    private fun initObservers() {
        viewModel.state.observe(this) { state ->
            when (state) {
                is ImageTranslationState.Loading -> {
                    showLoading(getString(R.string.processing_image))
                }
                is ImageTranslationState.OcrRunning -> {
                    showLoading(state.message)
                }
                is ImageTranslationState.Translating -> {
                    showLoading(state.message)
                }
                is ImageTranslationState.Rendering -> {
                    showLoading(state.message)
                }
                is ImageTranslationState.Success -> {
                    hideLoading()
                    ivTranslatedImage.setImageBitmap(state.overlayBitmap)
                    allTranslatedText = state.allTranslatedText
                    setFocus(0)
                }
                is ImageTranslationState.NoTextFound -> {
                    hideLoading()
                    tvNoText.visibility = View.VISIBLE
                    setFocus(2) // Focus on Back button
                }
                is ImageTranslationState.TranslationFailed -> {
                    hideLoading()
                    if (state.extractedText.isNotBlank()) {
                        scrollExtractedText.visibility = View.VISIBLE
                        tvExtractedText.text = state.extractedText
                        allTranslatedText = state.extractedText
                    }
                    Toast.makeText(this, R.string.translation_failed, Toast.LENGTH_LONG).show()
                    setFocus(0)
                }
                is ImageTranslationState.ContentBlocked -> {
                    hideLoading()
                    Toast.makeText(this, R.string.content_blocked, Toast.LENGTH_SHORT).show()
                    setFocus(2) // Focus on Back
                }
            }
        }
    }

    private fun showLoading(message: String) {
        loadingOverlay.visibility = View.VISIBLE
        tvLoadingStatus.text = message
        tvNoText.visibility = View.GONE
        scrollExtractedText.visibility = View.GONE
    }

    private fun hideLoading() {
        loadingOverlay.visibility = View.GONE
    }

    private fun setFocus(index: Int) {
        focusIndex = index.coerceIn(0, focusableButtons.size - 1)
        for ((i, view) in focusableButtons.withIndex()) {
            if (i == focusIndex) {
                view.requestFocus()
            } else {
                view.clearFocus()
            }
        }
    }

    private fun saveImageToGallery() {
        val bitmap = viewModel.getResultBitmap() ?: return

        try {
            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, "translation_${System.currentTimeMillis()}.png")
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/MegaLifeTranslator")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }

            val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            if (uri != null) {
                contentResolver.openOutputStream(uri)?.use { outputStream ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentValues.clear()
                    contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                    contentResolver.update(uri, contentValues, null, null)
                }

                Toast.makeText(this, R.string.image_saved, Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, R.string.image_save_failed, Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, R.string.image_save_failed, Toast.LENGTH_SHORT).show()
        }
    }

    override fun handleDpadEvent(keyCode: Int, event: KeyEvent?): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_UP -> {
                // Zoom in
                zoomLevel = (zoomLevel + 0.25f).coerceAtMost(3.0f)
                ivTranslatedImage.scaleX = zoomLevel
                ivTranslatedImage.scaleY = zoomLevel
                return true
            }
            KeyEvent.KEYCODE_DPAD_DOWN -> {
                // Zoom out
                zoomLevel = (zoomLevel - 0.25f).coerceAtLeast(0.5f)
                ivTranslatedImage.scaleX = zoomLevel
                ivTranslatedImage.scaleY = zoomLevel
                return true
            }
            KeyEvent.KEYCODE_DPAD_LEFT -> {
                if (focusIndex > 0) setFocus(focusIndex - 1)
                return true
            }
            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                if (focusIndex < focusableButtons.size - 1) setFocus(focusIndex + 1)
                return true
            }
            KeyEvent.KEYCODE_DPAD_CENTER -> {
                event?.startTracking()
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
        if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
            // Long-press CENTER saves image
            saveImageToGallery()
            return true
        }
        return false
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER && event?.isTracking == true && !event.isCanceled) {
            // Short press CENTER
            when (focusIndex) {
                0 -> {
                    // Copy all text
                    if (allTranslatedText.isNotBlank()) {
                        ClipboardHelper.setText(this, allTranslatedText)
                        Toast.makeText(this, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show()
                    }
                }
                1 -> {
                    // Save image
                    saveImageToGallery()
                }
                2 -> {
                    // Back
                    finish()
                }
            }
            return true
        }
        return super.onKeyUp(keyCode, event)
    }
}
