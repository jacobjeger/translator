package com.megalife.translator.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.megalife.translator.data.model.CachedTranslation
import com.megalife.translator.data.model.TranslationHistory

@Database(
    entities = [TranslationHistory::class, CachedTranslation::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun translationHistoryDao(): TranslationHistoryDao

    abstract fun translationCacheDao(): TranslationCacheDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /** Adds the translation cache table; leaves existing history untouched. */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `translation_cache` (" +
                        "`cacheKey` TEXT NOT NULL, " +
                        "`translatedText` TEXT NOT NULL, " +
                        "`createdAt` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`cacheKey`))"
                )
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "translator_db"
                ).addMigrations(MIGRATION_1_2).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
