package com.megalife.translator.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.megalife.translator.data.model.CachedTranslation

@Dao
interface TranslationCacheDao {

    @Query("SELECT * FROM translation_cache WHERE cacheKey IN (:keys)")
    suspend fun getByKeys(keys: List<String>): List<CachedTranslation>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<CachedTranslation>)

    @Query("DELETE FROM translation_cache WHERE createdAt < :cutoff")
    suspend fun deleteExpired(cutoff: Long)

    @Query("SELECT COUNT(*) FROM translation_cache")
    suspend fun getCount(): Int

    @Query("DELETE FROM translation_cache WHERE cacheKey IN (SELECT cacheKey FROM translation_cache ORDER BY createdAt ASC LIMIT :count)")
    suspend fun deleteOldest(count: Int)

    @Query("DELETE FROM translation_cache")
    suspend fun deleteAll()
}
