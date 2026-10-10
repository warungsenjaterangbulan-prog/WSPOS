package com.warungsenja.kasirterangbulan

import android.app.Application
import com.warungsenja.kasirterangbulan.di.AppContainer

class KasirApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
