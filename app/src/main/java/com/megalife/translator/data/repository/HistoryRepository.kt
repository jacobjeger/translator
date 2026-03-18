package com.megalife.translator.data.repository

import android.content.Context
import com.megalife.translator.data.local.AppDatabase
import com.megalife.translator.data.model.TranslationHistory

class HistoryRepository(context: Context) {

    private val dao = AppDatabase.getInstance(context).translationHistoryDao()

    suspend fun getAll(): List<TranslationHistory> = dao.getAll()

    suspend fun insert(history: TranslationHistory) {
        val count = dao.getCount()
        if (count >= MAX_HISTORY) {
            dao.deleteOldest(count - MAX_HISTORY + 1)
        }
        dao.insert(history)
    }

    suspend fun delete(history: TranslationHistory) = dao.delete(history)

    suspend fun deleteAll() = dao.deleteAll()

    companion object {
        private const val MAX_HISTORY = 20
    }
}
