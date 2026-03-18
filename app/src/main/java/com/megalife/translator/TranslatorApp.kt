package com.megalife.translator

import android.app.Application
import android.util.Log
import com.megalife.translator.data.repository.TranslationRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class TranslatorApp : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()

        // Debug-only: test Azure connection on startup
        if (BuildConfig.DEBUG) {
            appScope.launch(Dispatchers.IO) {
                try {
                    val repo = TranslationRepository()
                    val result = repo.testConnection()
                    Log.d("Translator", "Test translation 'Hello world' → Hebrew: $result")
                } catch (e: Exception) {
                    Log.e("Translator", "Connection test failed", e)
                }
            }
        }
    }
}
