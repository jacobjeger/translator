package com.megalife.translator

import android.app.Application

class TranslatorApp : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: TranslatorApp
            private set
    }
}
