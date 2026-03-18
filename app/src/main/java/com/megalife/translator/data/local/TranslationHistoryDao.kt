package com.megalife.translator.data.local

import androidx.room.*
import com.megalife.translator.data.model.TranslationHistory

@Dao
interface TranslationHistoryDao {

    @Query("SELECT * FROM translation_history ORDER BY timestamp DESC LIMIT 20")
    suspend fun getAll(): List<TranslationHistory>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(history: TranslationHistory)

    @Delete
    suspend fun delete(history: TranslationHistory)

    @Query("DELETE FROM translation_history")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM translation_history")
    suspend fun getCount(): Int

    @Query("DELETE FROM translation_history WHERE id IN (SELECT id FROM translation_history ORDER BY timestamp ASC LIMIT :count)")
    suspend fun deleteOldest(count: Int)
}
