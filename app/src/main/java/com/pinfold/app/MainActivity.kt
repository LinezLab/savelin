package com.pinfold.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.pinfold.app.ui.App
import com.pinfold.app.ui.glass.theme.ProvideGlassTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            ProvideGlassTheme {
                App()
            }
        }
    }
}
