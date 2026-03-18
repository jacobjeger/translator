package com.megalife.translator.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.megalife.translator.data.local.AppDatabase
import com.megalife.translator.data.model.TranslationHistory
import com.megalife.translator.data.repository.HistoryRepository
import kotlinx.coroutines.launch

class HistoryViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = HistoryRepository(AppDatabase.getInstance(application))

    private val _historyItems = MutableLiveData<List<TranslationHistory>>(emptyList())
    val historyItems: LiveData<List<TranslationHistory>> = _historyItems

    fun loadHistory() {
        viewModelScope.launch {
            _historyItems.value = repository.getAll()
        }
    }

    fun deleteItem(item: TranslationHistory) {
        viewModelScope.launch {
            repository.delete(item)
            loadHistory()
        }
    }

    fun deleteAll() {
        viewModelScope.launch {
            repository.deleteAll()
            _historyItems.value = emptyList()
        }
    }
}
