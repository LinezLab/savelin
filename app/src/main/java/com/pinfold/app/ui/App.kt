package com.pinfold.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import com.pinfold.app.ui.glass.ComponentGlass
import com.pinfold.app.ui.glass.GlassSettings
import com.pinfold.app.ui.glass.GlassToast
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
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.zIndex
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pinfold.app.diagnostics.CrashReporter
import com.pinfold.app.diagnostics.DiagnosticsLog
import kotlinx.coroutines.flow.first
import com.pinfold.app.ui.glass.LocalGlassOverlayHost
import com.pinfold.app.ui.glass.SavelinGlassDialog
import com.pinfold.app.ui.glass.Aurora
import com.pinfold.app.ui.glass.AuroraMoods
import com.pinfold.app.ui.glass.GlassStage
import com.pinfold.app.ui.glass.MotionSettings
import com.pinfold.app.ui.components.AppIcons
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
import com.pinfold.app.ui.detail.DetailScreen
import com.pinfold.app.ui.splash.SplashScreen
import com.pinfold.app.ui.settings.SettingsScreen
import com.pinfold.app.data.model.BookmarkWithTags
import kotlinx.coroutines.delay

internal val HOME_BACKGROUND_LIGHT: Color = Color.White
internal val HOME_BACKGROUND_DARK: Color = Color.Black

@Composable
fun App(
    sharedText: String? = null,
    clipboardShare: Pair<String, Long>? = null,
    openBookmark: Pair<Long, Long>? = null,
) {
    val viewModel: HomeViewModel = viewModel()
    var showAdd by remember { mutableStateOf(false) }
    var draft by remember { mutableStateOf("") }
    var detail by remember { mutableStateOf<BookmarkWithTags?>(null) }
    var consumedOpen by remember { mutableStateOf<Long?>(null) }
    var locked by remember { mutableStateOf(false) }
    var showSplash by remember { mutableStateOf(true) }
    var splashItem by remember { mutableStateOf<BookmarkWithTags?>(null) }
    val tab by viewModel.currentTab.collectAsState()
    val searchActive by viewModel.searchActive.collectAsState()
    val query by viewModel.searchQuery.collectAsState()
    val settingsPage by viewModel.settingsPage.collectAsState()
    val appearance by viewModel.settings.collectAsState()
    val items by viewModel.items.collectAsState()
    val ordered = remember(items) { items.distinctBy { it.bookmark.id } }
    val liveDetail = detail?.let { snap -> items.firstOrNull { it.bookmark.id == snap.bookmark.id } ?: snap }
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
    LaunchedEffect(appearance.privateVault) {
        if (appearance.privateVault) locked = true
    }
    LaunchedEffect(Unit) {
        splashItem = viewModel.randomWithImage()
    }
    // The Reduce motion switch lives in the data layer; mirror it into the glass motion engine so
    // every spring, pop-up and press in the app swaps to its short fade in the same frame.
    LaunchedEffect(appearance.reduceMotion) {
        MotionSettings.updateReduceMotion(appearance.reduceMotion)
    }
    LaunchedEffect(appearance.chipEffect) {
        ComponentGlass.setChipEffect(context, appearance.chipEffect)
    }
    LaunchedEffect(appearance.haptics) {
        GlassSettings.updateHaptics(context, appearance.haptics != "OFF")
    }
    LaunchedEffect(appearance.adaptiveLuminance) {
        GlassSettings.updateAdaptive(context, appearance.adaptiveLuminance)
    }
    LaunchedEffect(tab, settingsPage, detail?.bookmark?.id) {
        DiagnosticsLog.uiAction("Nav", "screen=" + when {
            detail != null -> "Details"
            tab == HomeTab.SETTINGS -> settingsPage.ifBlank { "Settings" }
            else -> "Home"
        })
    }
    if (showSplash) {
        SplashScreen(splashItem, reduceMotion = appearance.reduceMotion, onDone = { showSplash = false })
        return
    }
    if (locked) {
        BiometricGate(onUnlock = { locked = false })
        return
    }
    var linkToast by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(clipboardShare?.second) {
        val url = clipboardShare?.first ?: return@LaunchedEffect
        if (!viewModel.isSaved(url)) linkToast = url
    }
    LaunchedEffect(linkToast) {
        if (linkToast == null) return@LaunchedEffect
        delay(3600)
        linkToast = null
    }
    LaunchedEffect(sharedText) {
        if (!sharedText.isNullOrBlank()) {
            draft = sharedText
            showAdd = true
        }
    }
    LaunchedEffect(openBookmark?.second, items) {
        val request = openBookmark ?: return@LaunchedEffect
        if (consumedOpen == request.second) return@LaunchedEffect
        val target = items.firstOrNull { it.bookmark.id == request.first } ?: return@LaunchedEffect
        consumedOpen = request.second
        detail = target
        viewModel.recordOpen(target.bookmark)
    }
    GlassStage(
        wallpaper = {
            val dark = clear.dark
            Box(Modifier.fillMaxSize().background(if (dark) HOME_BACKGROUND_DARK else HOME_BACKGROUND_LIGHT).zIndex(-9999f))
        },
        screens = {
            val overlaysOpen = LocalGlassOverlayHost.current?.hasOverlays == true
            BackHandler(enabled = !overlaysOpen && (detail != null || searchActive || settingsPage != "HOME" || tab == HomeTab.SETTINGS)) {
                DiagnosticsLog.uiAction("Nav", "Back")
                when {
                    detail != null -> detail = null
                    searchActive -> viewModel.setSearchActive(false)
                    settingsPage != "HOME" -> viewModel.closeSettingsPage()
                    tab == HomeTab.SETTINGS -> viewModel.selectTab(HomeTab.HOME)
                }
            }
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
                liveDetail?.let { item ->
                    DetailScreen(
                        item = item,
                        reduceMotion = appearance.reduceMotion,
                        detailAmbientBackground = appearance.detailAmbientBackground,
                        onBack = { detail = null },
                        viewModel = viewModel,
                        onNavigate = { delta ->
                            val index = ordered.indexOfFirst { it.bookmark.id == item.bookmark.id }
                            if (index >= 0) {
                                val target = index + delta
                                if (target in ordered.indices) {
                                    val next = ordered[target]
                                    detail = next
                                    viewModel.recordOpen(next.bookmark)
                                }
                            }
                        },
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
                linkToast?.let { url ->
                    Box(
                        Modifier.align(Alignment.BottomCenter)
                            .navigationBarsPadding()
                            .padding(bottom = 96.dp),
                    ) {
                        GlassToast(
                            text = "Link detected",
                            action = "+",
                            onAction = {
                                viewModel.saveLink(url)
                                linkToast = null
                            },
                            actionIcon = {
                                AppIcons.Icon(AppIcons.Add, "Save link", tint = clear.onGlass, size = 18.dp)
                            },
                        )
                    }
                }
                CrashReportNotice()
            }
        },
        chrome = {
            val overlaysOpen = LocalGlassOverlayHost.current?.hasOverlays == true
            val homeChromeVisible = detail == null && !showAdd && tab != HomeTab.SETTINGS &&
                settingsPage == "HOME" && !overlaysOpen && CrashReporter.pendingLog.isNullOrBlank()
            if (homeChromeVisible) {
                Box(Modifier.fillMaxSize().imePadding()) {
                    FloatingPillNav(
                        searchActive = searchActive,
                        query = query,
                        visible = true,
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
                    visible = !searchActive,
                    onImportant = { DiagnosticsLog.uiAction("Home", "Filter important"); viewModel.toggleImportantFilter() },
                    onDue = { DiagnosticsLog.uiAction("Home", "Filter due"); viewModel.toggleReminderFilter() },
                    modifier = Modifier.align(Alignment.BottomEnd),
                )
            }
        }
    )
}
