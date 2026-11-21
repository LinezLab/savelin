package com.pinfold.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pinfold.app.ui.glass.Aurora
import com.pinfold.app.ui.glass.AuroraMoods
import com.pinfold.app.ui.glass.Capsule
import com.pinfold.app.ui.glass.GlassCircle
import com.pinfold.app.ui.glass.GlassStage
import com.pinfold.app.ui.glass.GlassTab
import com.pinfold.app.ui.glass.GlassTabs
import com.pinfold.app.ui.glass.theme.Candy
import com.pinfold.app.ui.glass.theme.clear
import com.pinfold.app.ui.home.HomeTab
import com.pinfold.app.ui.home.HomeViewModel
import com.pinfold.app.ui.home.HomeScreen
import com.pinfold.app.ui.details.DetailsScreen
import com.pinfold.app.ui.settings.SettingsScreen
import com.pinfold.app.data.model.BookmarkWithTags

@Composable
fun App(sharedText: String? = null) {
    val viewModel: HomeViewModel = viewModel()
    var showAdd by remember { mutableStateOf(false) }
    var draft by remember { mutableStateOf("") }
    var detail by remember { mutableStateOf<BookmarkWithTags?>(null) }
    val tab by viewModel.currentTab.collectAsState()

    LaunchedEffect(sharedText) {
        if (!sharedText.isNullOrBlank()) {
            draft = sharedText
            showAdd = true
        }
    }

    GlassStage(
        wallpaper = { Aurora(AuroraMoods.Desk) },
        screens = {
            Box(Modifier.fillMaxSize().padding(bottom = 88.dp)) {
                if (tab == HomeTab.SETTINGS) {
                    SettingsScreen(viewModel)
                } else {
                    HomeScreen(viewModel, onOpen = { detail = it })
                }
            }
            detail?.let { item ->
                DetailsScreen(item = item, viewModel = viewModel, onBack = { detail = null })
            }
        },
        chrome = {
            Box(Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 16.dp)) {
                GlassCircle(
                    onClick = { showAdd = true },
                    modifier = Modifier.align(Alignment.BottomEnd),
                    size = 60.dp,
                    tint = Candy.Sky,
                    label = "Add link",
                ) {
                    Text("+", color = androidx.compose.ui.graphics.Color.White)
                }
                GlassTabs(
                    tabs = listOf(
                        GlassTab("Home", null, Candy.Sky),
                        GlassTab("Saved", null, Candy.Grape),
                        GlassTab("Due", null, Candy.Tangerine),
                        GlassTab("Starred", null, Candy.Lemon),
                        GlassTab("Settings", null, Candy.Mint),
                    ),
                    selected = tab.ordinal,
                    onSelect = { viewModel.selectTab(HomeTab.entries[it]) },
                    modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(end = 72.dp),
                )
            }
        },
    )

    if (showAdd) {
        AlertDialog(
            onDismissRequest = { showAdd = false },
            title = { Text("Save a link") },
            text = {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    placeholder = { Text("Paste a link or shared text") },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (draft.isNotBlank()) viewModel.saveLink(draft)
                    draft = ""
                    showAdd = false
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showAdd = false }) { Text("Cancel") } },
        )
    }
}
