package com.megalife.translator.data.repository

import com.megalife.translator.data.local.AppDatabase
import com.megalife.translator.data.model.TranslationHistory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class HistoryRepository(private val database: AppDatabase) {

    private val dao = database.translationHistoryDao()
    private val maxItems = 20

    suspend fun getAll(): List<TranslationHistory> = withContext(Dispatchers.IO) {
        dao.getAll()
    }

    suspend fun insert(history: TranslationHistory) = withContext(Dispatchers.IO) {
        // Enforce max 20 items — drop oldest if at limit
        if (dao.getCount() >= maxItems) {
            dao.deleteOldest()
        }
        dao.insert(history)
    }

    suspend fun delete(history: TranslationHistory) = withContext(Dispatchers.IO) {
        dao.delete(history)
    }

    suspend fun deleteAll() = withContext(Dispatchers.IO) {
        dao.deleteAll()
    }
}
