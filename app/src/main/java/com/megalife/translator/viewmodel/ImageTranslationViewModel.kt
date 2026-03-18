package com.megalife.translator.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.megalife.translator.data.model.Language
import com.megalife.translator.data.repository.TranslationRepository
import com.megalife.translator.data.repository.TranslationResult
import com.megalife.translator.ocr.ImageOverlayRenderer
import com.megalife.translator.ocr.OcrProcessor
import com.megalife.translator.ocr.OcrTextBlock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed class ImageTranslationState {
    object Loading : ImageTranslationState()
    data class OcrRunning(val message: String) : ImageTranslationState()
    data class Translating(val message: String) : ImageTranslationState()
    data class Rendering(val message: String) : ImageTranslationState()
    data class Success(
        val overlayBitmap: Bitmap,
        val allTranslatedText: String
    ) : ImageTranslationState()
    object NoTextFound : ImageTranslationState()
    data class TranslationFailed(val extractedText: String, val error: String) : ImageTranslationState()
    object ContentBlocked : ImageTranslationState()
}

class ImageTranslationViewModel(application: Application) : AndroidViewModel(application) {

    private val translationRepo = TranslationRepository()
    private val ocrProcessor = OcrProcessor()
    private val overlayRenderer = ImageOverlayRenderer()

    private val _state = MutableLiveData<ImageTranslationState>(ImageTranslationState.Loading)
    val state: LiveData<ImageTranslationState> = _state

    private var originalBitmap: Bitmap? = null
    private var resultBitmap: Bitmap? = null

    fun processImage(uri: Uri, sourceLanguage: Language?, targetLanguage: Language) {
        viewModelScope.launch {
            _state.value = ImageTranslationState.Loading

            try {
                // Load bitmap
                val bitmap = withContext(Dispatchers.IO) {
                    val inputStream = getApplication<Application>().contentResolver.openInputStream(uri)
                    BitmapFactory.decodeStream(inputStream)
                } ?: run {
                    _state.value = ImageTranslationState.NoTextFound
                    return@launch
                }

                originalBitmap = bitmap
                processLoadedBitmap(bitmap, sourceLanguage, targetLanguage)
            } catch (e: Exception) {
                _state.value = ImageTranslationState.TranslationFailed("", e.message ?: "Failed to load image")
            }
        }
    }

    fun processImage(bitmap: Bitmap, sourceLanguage: Language?, targetLanguage: Language) {
        originalBitmap = bitmap
        viewModelScope.launch {
            processLoadedBitmap(bitmap, sourceLanguage, targetLanguage)
        }
    }

    private suspend fun processLoadedBitmap(
        bitmap: Bitmap,
        sourceLanguage: Language?,
        targetLanguage: Language
    ) {
        // Step 1: OCR
        _state.value = ImageTranslationState.OcrRunning("Detecting text…")
        val blocks: List<OcrTextBlock>
        try {
            blocks = ocrProcessor.processImage(bitmap)
        } catch (e: Exception) {
            _state.value = ImageTranslationState.TranslationFailed("", "OCR failed: ${e.message}")
            return
        }

        if (blocks.isEmpty()) {
            _state.value = ImageTranslationState.NoTextFound
            return
        }

        val extractedTexts = blocks.map { it.text }
        val allExtractedText = extractedTexts.joinToString("\n")

        // Step 2: Translate batch
        _state.value = ImageTranslationState.Translating("Translating text…")
        val result = translationRepo.translateBatch(
            texts = extractedTexts,
            fromLanguage = sourceLanguage?.code,
            toLanguage = targetLanguage.code
        )

        when (result) {
            is TranslationResult.BatchSuccess -> {
                // Step 3: Render overlay
                _state.value = ImageTranslationState.Rendering("Rendering translation…")
                val overlayBitmap = withContext(Dispatchers.Default) {
                    overlayRenderer.renderOverlay(bitmap, blocks, result.translations, targetLanguage)
                }
                resultBitmap = overlayBitmap
                val allTranslated = result.translations.joinToString("\n")
                _state.value = ImageTranslationState.Success(overlayBitmap, allTranslated)
            }
            is TranslationResult.ContentBlocked -> {
                _state.value = ImageTranslationState.ContentBlocked
            }
            is TranslationResult.Error -> {
                _state.value = ImageTranslationState.TranslationFailed(allExtractedText, result.message)
            }
            is TranslationResult.Success -> {
                // Shouldn't happen with batch, but handle gracefully
                _state.value = ImageTranslationState.TranslationFailed(allExtractedText, "Unexpected response format")
            }
        }
    }

    fun getResultBitmap(): Bitmap? = resultBitmap

    override fun onCleared() {
        super.onCleared()
        ocrProcessor.close()
    }
}
