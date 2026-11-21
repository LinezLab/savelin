package com.pinfold.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
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
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.first
import com.pinfold.app.ui.glass.Aurora
import com.pinfold.app.ui.glass.AuroraMoods
import com.pinfold.app.ui.glass.GlassStage
import com.pinfold.app.ui.components.FloatingPillNav
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
    var locked by remember { mutableStateOf(false) }
    val tab by viewModel.currentTab.collectAsState()
    val searchActive by viewModel.searchActive.collectAsState()
    val query by viewModel.searchQuery.collectAsState()
    val navVisible by viewModel.navVisible.collectAsState()
    val settingsPage by viewModel.settingsPage.collectAsState()

    LaunchedEffect(Unit) {
        locked = viewModel.settings.first().biometricLock
    }

    if (locked) {
        BiometricGate(onUnlock = { locked = false })
        return
    }

    LaunchedEffect(sharedText) {
        if (!sharedText.isNullOrBlank()) {
            draft = sharedText
            showAdd = true
        }
    }

    GlassStage(
        wallpaper = { Aurora(AuroraMoods.Desk) },
        screens = {
            Box(Modifier.fillMaxSize()) {
                Box(
                    Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.statusBars.union(WindowInsets.displayCutout)),
                ) {
                    if (tab == HomeTab.SETTINGS) {
                        if (settingsPage == "GLASS_MOTION") {
                            com.pinfold.app.ui.settings.GlassMotionScreen(viewModel)
                        } else {
                            SettingsScreen(viewModel)
                        }
                    } else {
                        HomeScreen(viewModel, onOpen = { detail = it })
                    }
                }
                val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
                Box(
                    Modifier.fillMaxWidth().height(topInset + 8.dp).align(Alignment.TopCenter)
                        .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.40f), Color.Transparent))),
                )
                detail?.let { item ->
                    DetailsScreen(item = item, viewModel = viewModel, onBack = { detail = null })
                }
            }
        },
        chrome = {
            Box(Modifier.fillMaxSize().imePadding()) {
                FloatingPillNav(
                    searchActive = searchActive,
                    query = query,
                    visible = navVisible && detail == null && !showAdd && tab != HomeTab.SETTINGS,
                    onSearchToggle = { viewModel.setSearchActive(!searchActive) },
                    onQueryChange = viewModel::onQueryChange,
                    onAdd = { showAdd = true },
                    onAddLong = { showAdd = true },
                    onSettings = { viewModel.selectTab(HomeTab.SETTINGS); viewModel.setSearchActive(false) },
                    onSettingsLong = { viewModel.selectTab(HomeTab.SETTINGS); viewModel.setSearchActive(false) },
                    modifier = Modifier.align(Alignment.BottomCenter),
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
