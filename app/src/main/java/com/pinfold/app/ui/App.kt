package com.pinfold.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.pinfold.app.ui.glass.Aurora
import com.pinfold.app.ui.glass.AuroraMoods
import com.pinfold.app.ui.glass.GlassStage

@Composable
fun App() {
    GlassStage(
        wallpaper = { Aurora(AuroraMoods.Desk) },
        screens = {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Pinfold")
            }
        },
        chrome = {},
    )
}
