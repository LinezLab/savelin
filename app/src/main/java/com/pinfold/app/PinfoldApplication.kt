package com.pinfold.app

import android.app.Application
import com.pinfold.app.diagnostics.CrashReporter
import com.pinfold.app.diagnostics.DiagnosticsLog
import com.pinfold.app.diagnostics.LogLevel
import com.pinfold.app.ui.glass.GlassSettings

class PinfoldApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        CrashReporter.install(this)
        GlassSettings.load(this)
        DiagnosticsLog.log(LogLevel.INFO, "App", "Savelin started")
    }
}
