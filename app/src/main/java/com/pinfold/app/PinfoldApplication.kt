package com.pinfold.app

import android.app.Application
import com.pinfold.app.ui.glass.GlassSettings

class PinfoldApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        GlassSettings.load(this)
    }
}
