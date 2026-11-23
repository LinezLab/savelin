package com.pinfold.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import com.pinfold.app.ui.glass.ComponentGlass
import com.pinfold.app.ui.glass.GlassSettings
import com.pinfold.app.ui.glass.GlassTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pinfold.app.diagnostics.DiagnosticsLog
import kotlinx.coroutines.flow.first
import com.pinfold.app.ui.glass.SavelinGlassDialog
import com.pinfold.app.ui.glass.Aurora
import com.pinfold.app.ui.glass.AuroraMoods
import com.pinfold.app.ui.glass.GlassStage
import com.pinfold.app.ui.glass.MotionSettings
import com.pinfold.app.ui.components.CrashReportNotice
import com.pinfold.app.ui.components.FilterRail
import com.pinfold.app.ui.components.FloatingPillNav
import com.pinfold.app.ui.background.effectiveBackgroundFps
import com.pinfold.app.ui.background.rememberAppInForeground
import com.pinfold.app.ui.background.rememberPowerSaveMode
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
    val appearance by viewModel.settings.collectAsState()
    val context = LocalContext.current
    val foreground = rememberAppInForeground()
    val powerSave = rememberPowerSaveMode()
    val backgroundCovered = tab == HomeTab.SETTINGS || detail != null || showAdd
    val backgroundPaused = !foreground || backgroundCovered || powerSave
    val backgroundAnimate = appearance.backgroundAnimate && !appearance.reduceMotion
    LaunchedEffect(backgroundAnimate, backgroundPaused) {
        DiagnosticsLog.uiAction("Background", "animate=" + backgroundAnimate + " paused=" + backgroundPaused + " fps=" + effectiveBackgroundFps(appearance.backgroundFps))
    }
    LaunchedEffect(Unit) {
        locked = viewModel.settings.first().biometricLock
    }
    // The Reduce motion switch lives in the data layer; mirror it into the glass motion engine so
    // every spring, pop-up and press in the app swaps to its short fade in the same frame.
    LaunchedEffect(appearance.reduceMotion) {
        MotionSettings.updateReduceMotion(appearance.reduceMotion)
    }
    LaunchedEffect(appearance.chipEffect) {
        ComponentGlass.setChipEffect(context, appearance.chipEffect)
    }
    LaunchedEffect(tab, settingsPage, detail?.bookmark?.id) {
        DiagnosticsLog.uiAction("Nav", "screen=" + when {
            detail != null -> "Details"
            tab == HomeTab.SETTINGS -> settingsPage.ifBlank { "Settings" }
            else -> "Home"
        })
    }
    BackHandler(enabled = detail != null || searchActive || settingsPage != "HOME" || tab == HomeTab.SETTINGS) {
        DiagnosticsLog.uiAction("Nav", "Back")
        when {
            detail != null -> detail = null
            searchActive -> viewModel.setSearchActive(false)
            settingsPage != "HOME" -> viewModel.closeSettingsPage()
            tab == HomeTab.SETTINGS -> viewModel.selectTab(HomeTab.HOME)
        }
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
        wallpaper = {
            val bg = appearance
            when (bg.backgroundType) {
                "SOLID" -> Box(Modifier.fillMaxSize().background(Color.hsv(bg.backgroundHue.coerceIn(0f, 360f), 0.55f, 0.92f)))
                "PICTURE" -> {
                    val wall = GlassSettings.customWallpaper
                    if (wall != null) Image(wall, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    else com.pinfold.app.ui.background.GeneratedBackground(
                        Modifier.fillMaxSize(), loopMs = bg.backgroundLoopSeconds * 1000, animate = backgroundAnimate, paused = backgroundPaused,
                        style = bg.backgroundStyle, hue = bg.backgroundHue, dim = bg.backgroundDim,
                        shape = bg.backgroundShape, source = bg.backgroundSource, fps = bg.backgroundFps,
                    )
                }
                else -> com.pinfold.app.ui.background.GeneratedBackground(
                    Modifier.fillMaxSize(), loopMs = bg.backgroundLoopSeconds * 1000, animate = backgroundAnimate, paused = backgroundPaused,
                    style = bg.backgroundStyle, hue = bg.backgroundHue, dim = bg.backgroundDim,
                    shape = bg.backgroundShape, source = bg.backgroundSource, fps = bg.backgroundFps,
                )
            }
        },
        screens = {
            val shadeColors = com.pinfold.app.ui.glass.theme.clear
            val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
            val shadeHeight = topInset + appearance.topShadeHeight.dp
            Box(Modifier.fillMaxSize()) {
                Box(
                    Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.statusBars.union(WindowInsets.displayCutout)),
                ) {
                    if (tab == HomeTab.SETTINGS) {
                        if (settingsPage == "GLASS_MOTION") {
                            com.pinfold.app.ui.settings.GlassMotionScreen(viewModel)
                        } else if (settingsPage == "BACKGROUND") {
                            com.pinfold.app.ui.settings.BackgroundScreen(viewModel)
                        } else if (settingsPage == "DRIVE_SYNC") {
                            com.pinfold.app.ui.settings.DriveSyncScreen(viewModel)
                        } else {
                            SettingsScreen(viewModel)
                        }
                    } else {
                        HomeScreen(viewModel, onOpen = { detail = it })
                    }
                }
                detail?.let { item ->
                    DetailsScreen(item = item, viewModel = viewModel, onBack = { detail = null })
                }
                if (appearance.topShade) {
                    val glass = GlassSettings.style
                    val wash = shadeColors.glass.copy(alpha = (appearance.topShadeStrength * glass.tint).coerceIn(0f, 0.65f))
                    Box(
                        Modifier.align(Alignment.TopCenter).fillMaxWidth().height(shadeHeight).zIndex(5f)
                            .background(Brush.verticalGradient(listOf(wash, Color.Transparent))),
                    )
                }
                if (showAdd) {
                    SavelinGlassDialog(
                        onDismiss = { showAdd = false },
                        title = "Save a link",
                        confirm = "Save",
                        onConfirm = {
                            if (draft.isNotBlank()) viewModel.saveLink(draft)
                            draft = ""
                            showAdd = false
                        },
                        dismiss = "Cancel",
                    ) {
                        GlassTextField(
                            value = draft,
                            onValueChange = { draft = it },
                            placeholder = "Paste a link or shared text",
                        )
                    }
                }
                CrashReportNotice()
            }
        },
        chrome = {
            Box(Modifier.fillMaxSize().imePadding()) {
                FloatingPillNav(
                    searchActive = searchActive,
                    query = query,
                    visible = navVisible && detail == null && !showAdd && tab != HomeTab.SETTINGS,
                    onSearchToggle = { DiagnosticsLog.uiAction("Nav", if (searchActive) "Search close" else "Search open"); viewModel.setSearchActive(!searchActive) },
                    onQueryChange = viewModel::onQueryChange,
                    onAdd = { showAdd = true },
                    onAddLong = { showAdd = true },
                    onSettings = { DiagnosticsLog.uiAction("Nav", "Settings"); viewModel.selectTab(HomeTab.SETTINGS); viewModel.setSearchActive(false) },
                    onSettingsLong = { viewModel.selectTab(HomeTab.SETTINGS); viewModel.setSearchActive(false) },
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }
            val dueCount = viewModel.reminderIds.collectAsState().value.size
            FilterRail(
                important = viewModel.importantFilter.collectAsState().value,
                due = viewModel.reminderFilter.collectAsState().value,
                dueCount = dueCount,
                visible = navVisible && detail == null && !showAdd && tab != HomeTab.SETTINGS && !searchActive,
                onImportant = { DiagnosticsLog.uiAction("Home", "Filter important"); viewModel.toggleImportantFilter() },
                onDue = { DiagnosticsLog.uiAction("Home", "Filter due"); viewModel.toggleReminderFilter() },
            )
        }
    )
}
