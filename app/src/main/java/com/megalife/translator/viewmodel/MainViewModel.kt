package com.megalife.translator.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.megalife.translator.data.local.AppDatabase
import com.megalife.translator.data.local.PreferencesManager
import com.megalife.translator.data.model.LanguagePair
import com.megalife.translator.data.model.TranslationHistory
import com.megalife.translator.data.repository.HistoryRepository
import com.megalife.translator.data.repository.TranslationRepository
import com.megalife.translator.data.repository.TranslationResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val translationRepo = TranslationRepository()
    private val historyRepo = HistoryRepository(AppDatabase.getInstance(application))
    val prefs = PreferencesManager(application)

    private var translateJob: Job? = null

    // Current language pair index
    private val _currentPairIndex = MutableLiveData(0)
    val currentPairIndex: LiveData<Int> = _currentPairIndex

    private val _currentPair = MutableLiveData(LanguagePair.ALL_PAIRS[0])
    val currentPair: LiveData<LanguagePair> = _currentPair

    private val _translatedText = MutableLiveData("")
    val translatedText: LiveData<String> = _translatedText

    private val _isTranslating = MutableLiveData(false)
    val isTranslating: LiveData<Boolean> = _isTranslating

    private val _errorMessage = MutableLiveData<String?>(null)
    val errorMessage: LiveData<String?> = _errorMessage

    private val _contentBlocked = MutableLiveData(false)
    val contentBlocked: LiveData<Boolean> = _contentBlocked

    init {
        val defaultIndex = prefs.defaultLanguagePairIndex.coerceIn(0, LanguagePair.ALL_PAIRS.size - 1)
        _currentPairIndex.value = defaultIndex
        _currentPair.value = LanguagePair.ALL_PAIRS[defaultIndex]
    }

    fun cycleLanguagePair(forward: Boolean) {
        val pairs = LanguagePair.ALL_PAIRS
        var index = _currentPairIndex.value ?: 0
        index = if (forward) {
            (index + 1) % pairs.size
        } else {
            (index - 1 + pairs.size) % pairs.size
        }
        _currentPairIndex.value = index
        _currentPair.value = pairs[index]
    }

    fun swapLanguages() {
        val current = _currentPair.value ?: return
        val reversed = current.reversed()
        // Find this reversed pair in ALL_PAIRS
        val index = LanguagePair.ALL_PAIRS.indexOfFirst {
            it.sourceLanguage == reversed.sourceLanguage && it.targetLanguage == reversed.targetLanguage
        }
        if (index >= 0) {
            _currentPairIndex.value = index
            _currentPair.value = LanguagePair.ALL_PAIRS[index]
        }
    }

    fun translateDebounced(text: String) {
        translateJob?.cancel()
        if (text.isBlank()) {
            _translatedText.value = ""
            _isTranslating.value = false
            _contentBlocked.value = false
            return
        }

        translateJob = viewModelScope.launch {
            delay(500) // 500ms debounce
            performTranslation(text)
        }
    }

    fun translateImmediate(text: String) {
        if (text.isBlank()) {
            _translatedText.value = ""
            return
        }
        viewModelScope.launch {
            performTranslation(text)
        }
    }

    private suspend fun performTranslation(text: String) {
        _isTranslating.value = true
        _contentBlocked.value = false
        _errorMessage.value = null

        val pair = _currentPair.value ?: return
        val result = translationRepo.translate(
            text = text,
            fromLanguage = pair.sourceLanguage.code,
            toLanguage = pair.targetLanguage.code
        )

        _isTranslating.value = false

        when (result) {
            is TranslationResult.Success -> {
                _translatedText.value = result.translatedText

                // Save to history if enabled
                if (prefs.historyEnabled) {
                    historyRepo.insert(
                        TranslationHistory(
                            sourceText = text,
                            translatedText = result.translatedText,
                            sourceLanguageCode = pair.sourceLanguage.code,
                            targetLanguageCode = pair.targetLanguage.code,
                            languagePairDisplay = pair.displayName
                        )
                    )
                }
            }
            is TranslationResult.ContentBlocked -> {
                _contentBlocked.value = true
                _translatedText.value = ""
            }
            is TranslationResult.Error -> {
                _errorMessage.value = result.message
            }
            is TranslationResult.BatchSuccess -> {
                _translatedText.value = result.translations.joinToString("\n")
            }
        }
    }

    fun clearTranslation() {
        translateJob?.cancel()
        _translatedText.value = ""
        _isTranslating.value = false
        _contentBlocked.value = false
        _errorMessage.value = null
    }
}
