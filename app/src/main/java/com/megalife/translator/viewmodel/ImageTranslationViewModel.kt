package com.megalife.translator.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.*
import com.megalife.translator.data.repository.TranslationRepository
import com.megalife.translator.ocr.ImageOverlayRenderer
import com.megalife.translator.ocr.OcrBlock
import com.megalife.translator.ocr.OcrProcessor
import kotlinx.coroutines.launch
import java.io.File

class ImageTranslationViewModel(application: Application) : AndroidViewModel(application) {

    private val translationRepo = TranslationRepository(application)
    private val ocrProcessor = OcrProcessor()
    private val overlayRenderer = ImageOverlayRenderer()

    private val _isProcessing = MutableLiveData(false)
    val isProcessing: LiveData<Boolean> = _isProcessing

    private val _resultBitmap = MutableLiveData<Bitmap?>()
    val resultBitmap: LiveData<Bitmap?> = _resultBitmap

    private val _allTranslatedText = MutableLiveData("")
    val allTranslatedText: LiveData<String> = _allTranslatedText

    private val _errorMessage = MutableLiveData<String?>(null)
    val errorMessage: LiveData<String?> = _errorMessage

    private val _noTextFound = MutableLiveData(false)
    val noTextFound: LiveData<Boolean> = _noTextFound

    private var originalBitmap: Bitmap? = null

    fun processImage(imagePath: String?, imageUri: String?, sourceLang: String, targetLang: String) {
        viewModelScope.launch {
            _isProcessing.value = true
            _errorMessage.value = null
            _noTextFound.value = false

            try {
                val bitmap = loadBitmap(imagePath, imageUri)
                if (bitmap == null) {
                    _errorMessage.value = "Failed to load image"
                    _isProcessing.value = false
                    return@launch
                }

                originalBitmap = bitmap

                // Run OCR
                val blocks = ocrProcessor.processImage(bitmap)

                if (blocks.isEmpty()) {
                    _noTextFound.value = true
                    _resultBitmap.value = bitmap
                    _isProcessing.value = false
                    return@launch
                }

                // Translate all blocks in one batch
                val texts = blocks.map { it.text }
                val result = translationRepo.translateBatch(texts, sourceLang, targetLang)

                when (result) {
                    is TranslationRepository.TranslationResult.BatchSuccess -> {
                        // Render overlay
                        val overlayBitmap = overlayRenderer.renderOverlay(
                            bitmap, blocks, result.translations, targetLang
                        )
                        _resultBitmap.value = overlayBitmap
                        _allTranslatedText.value = result.translations.joinToString("\n")
                    }
                    is TranslationRepository.TranslationResult.ContentBlocked -> {
                        _resultBitmap.value = bitmap
                        _allTranslatedText.value = texts.joinToString("\n")
                        _errorMessage.value = "Can't translate explicit content"
                    }
                    is TranslationRepository.TranslationResult.Error -> {
                        // Show original text with error
                        _resultBitmap.value = bitmap
                        _allTranslatedText.value = texts.joinToString("\n")
                        _errorMessage.value = "Translation failed — showing original text"
                    }
                    else -> {}
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error: ${e.message}"
            }

            _isProcessing.value = false
        }
    }

    private fun loadBitmap(imagePath: String?, imageUri: String?): Bitmap? {
        return try {
            if (imagePath != null) {
                BitmapFactory.decodeFile(imagePath)
            } else if (imageUri != null) {
                val uri = Uri.parse(imageUri)
                val inputStream = getApplication<Application>().contentResolver.openInputStream(uri)
                BitmapFactory.decodeStream(inputStream).also { inputStream?.close() }
            } else null
        } catch (e: Exception) {
            null
        }
    }

    fun getResultBitmap(): Bitmap? = _resultBitmap.value

    override fun onCleared() {
        super.onCleared()
        ocrProcessor.close()
    }
}
