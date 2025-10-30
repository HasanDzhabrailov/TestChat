package com.example.testchat

import android.app.Application
import com.example.testchat.core.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class MessengerApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@MessengerApplication)
            modules(appModule)
        }
    }
}
