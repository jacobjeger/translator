package com.megalife.translator.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.megalife.translator.data.local.PreferencesManager
import com.megalife.translator.data.model.LanguagePair
import com.megalife.translator.data.model.TranslationHistory
import com.megalife.translator.data.repository.HistoryRepository
import com.megalife.translator.data.repository.TranslationRepository
import kotlinx.coroutines.*

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val translationRepo = TranslationRepository()
    private val historyRepo = HistoryRepository(application)
    val prefs = PreferencesManager(application)

    private var translateJob: Job? = null

    private val _currentPairIndex = MutableLiveData(0)
    val currentPairIndex: LiveData<Int> = _currentPairIndex

    private val _translatedText = MutableLiveData("")
    val translatedText: LiveData<String> = _translatedText

    private val _statusMessage = MutableLiveData<String?>(null)
    val statusMessage: LiveData<String?> = _statusMessage

    private val _isTranslating = MutableLiveData(false)
    val isTranslating: LiveData<Boolean> = _isTranslating

    private val _contentBlocked = MutableLiveData(false)
    val contentBlocked: LiveData<Boolean> = _contentBlocked

    val currentPair: LanguagePair
        get() = LanguagePair.ALL_PAIRS[_currentPairIndex.value ?: 0]

    init {
        _currentPairIndex.value = prefs.defaultLanguagePairIndex
    }

    fun cyclePairForward() {
        val current = _currentPairIndex.value ?: 0
        _currentPairIndex.value = (current + 1) % LanguagePair.ALL_PAIRS.size
    }

    fun cyclePairBackward() {
        val current = _currentPairIndex.value ?: 0
        _currentPairIndex.value = if (current == 0) LanguagePair.ALL_PAIRS.size - 1 else current - 1
    }

    fun swapLanguages() {
        val current = _currentPairIndex.value ?: 0
        val pair = LanguagePair.ALL_PAIRS[current]
        val swapped = pair.swapped()
        // Find matching pair in ALL_PAIRS
        val newIndex = LanguagePair.ALL_PAIRS.indexOfFirst {
            it.sourceCode == swapped.sourceCode && it.targetCode == swapped.targetCode
        }
        if (newIndex >= 0) {
            _currentPairIndex.value = newIndex
        }
    }

    fun translateDebounced(text: String) {
        translateJob?.cancel()
        if (text.isBlank()) {
            _translatedText.value = ""
            _isTranslating.value = false
            return
        }

        _isTranslating.value = true
        translateJob = viewModelScope.launch {
            delay(500) // 500ms debounce
            translateNow(text)
        }
    }

    fun translateNow(text: String) {
        if (text.isBlank()) {
            _translatedText.value = ""
            return
        }
        _isTranslating.value = true
        _contentBlocked.value = false

        viewModelScope.launch {
            val pair = currentPair
            when (val result = translationRepo.translate(text, pair.sourceCode, pair.targetCode)) {
                is TranslationRepository.TranslationResult.Success -> {
                    _translatedText.value = result.translatedText
                    _isTranslating.value = false
                    _statusMessage.value = null

                    // Save to history
                    if (prefs.historyEnabled && result.translatedText.isNotBlank()) {
                        historyRepo.insert(
                            TranslationHistory(
                                sourceText = text,
                                translatedText = result.translatedText,
                                sourceLanguage = pair.sourceCode,
                                targetLanguage = pair.targetCode,
                                languagePairDisplay = pair.displayName
                            )
                        )
                    }
                }
                is TranslationRepository.TranslationResult.Error -> {
                    _isTranslating.value = false
                    _statusMessage.value = result.message
                }
                is TranslationRepository.TranslationResult.ContentBlocked -> {
                    _isTranslating.value = false
                    _contentBlocked.value = true
                }
                else -> {}
            }
        }
    }

    fun clearStatus() {
        _statusMessage.value = null
        _contentBlocked.value = false
    }
}
