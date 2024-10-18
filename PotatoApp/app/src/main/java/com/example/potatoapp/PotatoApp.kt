package com.example.potatoapp

import android.app.Application
import android.content.Context

class PotatoApp : Application() {

    init {
        instance = this
    }

    companion object {
        private var instance: PotatoApp? = null

        fun getAppContext(): Context {
            return instance?.applicationContext
                ?: throw IllegalStateException("Application context is not initialized.")
        }
    }

    override fun onCreate() {
        super.onCreate()
        // Inisialisasi lainnya jika diperlukan
    }
}

