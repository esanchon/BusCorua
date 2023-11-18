package com.esanchon.buscorua.presentation

import android.app.Application
import android.content.Context

class BusApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        appContext = applicationContext
    }

    companion object {
        var appContext: Context? = null
            private set
    }
}
