package com.pinfold.app.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import com.pinfold.app.ui.glass.Capsule
import com.pinfold.app.ui.glass.ComponentGlass
import com.pinfold.app.ui.glass.GlassComponent
import com.pinfold.app.ui.glass.SavelinGlass
import com.pinfold.app.ui.glass.pressable
import com.pinfold.app.ui.glass.GlassPill
import com.pinfold.app.ui.glass.GlassSettings
import com.pinfold.app.ui.glass.GlassToast
import com.pinfold.app.ui.glass.GlassTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
import com.pinfold.app.ui.components.PullCord
import com.pinfold.app.ui.components.FloatingPillNav
import com.pinfold.app.ui.components.shareText
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
import kotlinx.coroutines.launch

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
    var draftPrivate by remember { mutableStateOf(false) }
    var detail by remember { mutableStateOf<BookmarkWithTags?>(null) }
    var consumedOpen by remember { mutableStateOf<Long?>(null) }
    var locked by remember { mutableStateOf(false) }
    var pendingVault by remember { mutableStateOf(false) }
    var showSplash by remember { mutableStateOf(true) }
    var selectedIds by remember { mutableStateOf(setOf<Long>()) }
    var splashItem by remember { mutableStateOf<BookmarkWithTags?>(null) }
    val refreshProgress = remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var lastBulkRefresh by remember { mutableStateOf(0L) }
    val chromeScope = rememberCoroutineScope()
    val settingsPage by viewModel.settingsPage.collectAsState()
    val showBackToTop by remember {
        derivedStateOf {
            settingsPage != "VAULT" && (
                viewModel.gridState.firstVisibleItemIndex > 0 ||
                    viewModel.gridState.firstVisibleItemScrollOffset > 0
                )
        }
    }
    val tab by viewModel.currentTab.collectAsState()
    val searchActive by viewModel.searchActive.collectAsState()
    val query by viewModel.searchQuery.collectAsState()
    val appearance by viewModel.settings.collectAsState()
    val items by viewModel.items.collectAsState()
    val privateItems by viewModel.privateItems.collectAsState()
    val dueCount by viewModel.reminderIds.collectAsState()
    val importantFilter by viewModel.importantFilter.collectAsState()
    val reminderFilter by viewModel.reminderFilter.collectAsState()
    val ordered = remember(items) { items.distinctBy { it.bookmark.id } }
    val liveDetail = detail?.let { snap -> items.firstOrNull { it.bookmark.id == snap.bookmark.id } ?: snap }
    val selectionPool = remember(items, privateItems) { (items + privateItems).distinctBy { it.bookmark.id } }
    val selectedItems = remember(selectedIds, selectionPool) { selectionPool.filter { it.bookmark.id in selectedIds } }
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
    LaunchedEffect(showSplash) {
        if (showSplash && foreground) {
            lastBulkRefresh = System.currentTimeMillis()
            viewModel.refreshMissingImages { d, t -> refreshProgress.value = d to t }
        }
    }
    LaunchedEffect(foreground) {
        if (!foreground) return@LaunchedEffect
        val now = System.currentTimeMillis()
        if (now - lastBulkRefresh < 60_000L) return@LaunchedEffect
        lastBulkRefresh = now
        viewModel.refreshMissingImages { d, t -> refreshProgress.value = d to t }
    }
    if (showSplash) {
        SplashScreen(
            splashItem,
            reduceMotion = appearance.reduceMotion,
            shimmerEnabled = appearance.shimmerEnabled,
            shimmerColor = appearance.shimmerColor,
            shimmerSpeed = appearance.shimmerSpeed,
            onDone = { showSplash = false },
        )
        return
    }
    if (locked) {
        Box(Modifier.fillMaxSize()) {
            SplashScreen(
                splashItem,
                reduceMotion = appearance.reduceMotion,
                shimmerEnabled = appearance.shimmerEnabled,
                shimmerColor = appearance.shimmerColor,
                shimmerSpeed = appearance.shimmerSpeed,
                keepOnScreen = true,
                onDone = {},
            )
            Box(Modifier.fillMaxSize().zIndex(1f)) {
                BiometricGate(
                    onUnlock = {
                        if (pendingVault) { viewModel.openVault(); pendingVault = false }
                        locked = false
                    },
                    onCancel = { pendingVault = false; locked = false },
                )
            }
        }
        return
    }
    var linkToast by remember { mutableStateOf<String?>(null) }
    var statusToast by remember { mutableStateOf<String?>(null) }
    val onDeleteSelected: () -> Unit = {
        selectedItems.forEach { viewModel.delete(it.bookmark) }
        selectedIds = emptySet()
    }
    val onRefetchSelected: () -> Unit = {
        selectedItems.forEach { viewModel.refetch(it.bookmark) }
        selectedIds = emptySet()
    }
    val onCopySelected: () -> Unit = {
        val links = selectedItems.joinToString("\n") { it.bookmark.url }
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        clipboard?.setPrimaryClip(ClipData.newPlainText("Sonil links", links))
        statusToast = "Copied " + selectedIds.size + " links"
        selectedIds = emptySet()
    }
    val onShareSelected: () -> Unit = {
        val links = selectedItems.joinToString("\n") { it.bookmark.url }
        context.shareText(links, "Share links")
        selectedIds = emptySet()
    }
    val onRemoveStarSelected: () -> Unit = {
        selectedItems.forEach { viewModel.setImportant(it.bookmark, false) }
        statusToast = "Removed star from " + selectedIds.size + " links"
        selectedIds = emptySet()
    }
    val onClearReminderSelected: () -> Unit = {
        selectedItems.forEach { viewModel.clearReminder(it.bookmark) }
        statusToast = "Cleared reminders for " + selectedIds.size + " links"
        selectedIds = emptySet()
    }
    val onToggleImportantFilter: () -> Unit = {
        DiagnosticsLog.uiAction("Home", "Filter important")
        viewModel.toggleImportantFilter()
    }
    val onToggleReminderFilter: () -> Unit = {
        DiagnosticsLog.uiAction("Home", "Filter due")
        viewModel.toggleReminderFilter()
    }
    LaunchedEffect(clipboardShare?.second) {
        val url = clipboardShare?.first ?: return@LaunchedEffect
        if (!viewModel.isSaved(url)) linkToast = url
    }
    LaunchedEffect(linkToast) {
        if (linkToast == null) return@LaunchedEffect
        delay(10_000)
        linkToast = null
    }
    LaunchedEffect(statusToast) {
        if (statusToast == null) return@LaunchedEffect
        delay(2200)
        statusToast = null
    }
    LaunchedEffect(refreshProgress.value) {
        if (refreshProgress.value != null) {
            delay(1200)
            refreshProgress.value = null
        }
    }
    LaunchedEffect(sharedText) {
        if (!sharedText.isNullOrBlank()) {
            draft = sharedText
            showAdd = true
        }
    }
    LaunchedEffect(showAdd) {
        if (showAdd) draftPrivate = appearance.saveLinksPrivateByDefault
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
            BackHandler(enabled = !overlaysOpen && (selectedIds.isNotEmpty() || detail != null || searchActive || settingsPage != "HOME" || tab == HomeTab.SETTINGS)) {
                DiagnosticsLog.uiAction("Nav", "Back")
                when {
                    selectedIds.isNotEmpty() -> selectedIds = emptySet()
                    detail != null -> detail = null
                    searchActive -> viewModel.setSearchActive(false)
                    settingsPage != "HOME" -> viewModel.closeSettingsPage()
                    tab == HomeTab.SETTINGS -> viewModel.selectTab(HomeTab.HOME)
                }
            }
            Box(Modifier.fillMaxSize()) {
                Box(
                    Modifier.fillMaxSize().windowInsetsPadding(
                        if (appearance.immersiveMode) WindowInsets(0) else WindowInsets.statusBars.union(WindowInsets.displayCutout),
                    ),
                ) {
                    if (tab == HomeTab.SETTINGS) {
                        when {
                            settingsPage == "GLASS_MOTION" -> com.pinfold.app.ui.settings.GlassMotionScreen(viewModel)
                            settingsPage == "VAULT" -> Box(Modifier.fillMaxSize()) {
                                HomeScreen(
                                    viewModel,
                                    privateOnly = true,
                                    onOpen = { detail = it; selectedIds = emptySet() },
                                    selectedIds = selectedIds,
                                    onStartSelection = { id -> selectedIds = setOf(id) },
                                    onToggleSelected = { id -> selectedIds = if (id in selectedIds) selectedIds - id else selectedIds + id },
                                )
                            }
                            else -> SettingsScreen(viewModel)
                        }
                    } else {
                        HomeScreen(
                            viewModel,
                            onOpen = { detail = it; selectedIds = emptySet() },
                            selectedIds = selectedIds,
                            onStartSelection = { id -> selectedIds = setOf(id) },
                            onToggleSelected = { id -> selectedIds = if (id in selectedIds) selectedIds - id else selectedIds + id },
                        )
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
                            if (draft.isNotBlank()) viewModel.saveLink(draft, draftPrivate)
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
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            GlassPill(onClick = { draftPrivate = false }, height = 40.dp) {
                                Text("Public", color = if (!draftPrivate) clear.ink else clear.onGlassSoft)
                            }
                            GlassPill(onClick = { draftPrivate = true }, height = 40.dp) {
                                Text("Private", color = if (draftPrivate) clear.ink else clear.onGlassSoft)
                            }
                        }
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
                            onDismiss = { linkToast = null },
                        )
                    }
                }
                statusToast?.let { message ->
                    Box(
                        Modifier.align(Alignment.BottomCenter)
                            .navigationBarsPadding()
                            .padding(bottom = 96.dp),
                    ) {
                        GlassToast(text = message, action = null, onAction = {})
                    }
                }
                refreshProgress.value?.let { (done, total) ->
                    Box(
                        Modifier.align(Alignment.BottomCenter)
                            .navigationBarsPadding()
                            .padding(bottom = 160.dp),
                    ) {
                        GlassToast(text = "Refreshing covers $done/$total", action = null, onAction = {})
                    }
                }
                CrashReportNotice()
            }
        },
        chrome = {
            val overlaysOpen = LocalGlassOverlayHost.current?.hasOverlays == true
            val inVault = settingsPage == "VAULT"
            val homeChromeVisible = detail == null && !showAdd && !overlaysOpen &&
                CrashReporter.pendingLog.isNullOrBlank() &&
                ((tab != HomeTab.SETTINGS && settingsPage == "HOME") || inVault)
            if (homeChromeVisible) {
                Box(Modifier.fillMaxSize().imePadding()) {
                    if (appearance.pullCordEnabled) {
                        PullCord(
                            mode = appearance.themeMode,
                            onModeChange = { viewModel.setSetting("themeMode", it) },
                            modifier = Modifier.fillMaxSize(),
                            across = com.pinfold.app.ui.settings.pullCordAcross(appearance.pullCordPosition),
                            reduceMotion = appearance.reduceMotion,
                        )
                    }
                    FloatingPillNav(
                        searchActive = searchActive,
                        query = query,
                        visible = true,
                        onSearchToggle = { DiagnosticsLog.uiAction("Nav", if (searchActive) "Search close" else "Search open"); viewModel.setSearchActive(!searchActive) },
                        onQueryChange = viewModel::onQueryChange,
                        onAdd = { showAdd = true },
                        onAddLong = { showAdd = true },
                        onSettings = {
                            DiagnosticsLog.uiAction("Nav", "Settings")
                            viewModel.closeSettingsPage()
                            viewModel.selectTab(HomeTab.SETTINGS)
                            viewModel.setSearchActive(false)
                        },
                        onSettingsLong = {
                            viewModel.setSearchActive(false)
                            if (settingsPage != "VAULT" && appearance.longPressSettingsVault) {
                                pendingVault = true
                                locked = true
                            } else {
                                viewModel.closeSettingsPage()
                                viewModel.selectTab(HomeTab.SETTINGS)
                            }
                        },
                        modifier = Modifier.align(Alignment.BottomCenter),
                    )
                    if (showBackToTop && selectedIds.isEmpty()) {
                        Box(
                            Modifier.align(Alignment.BottomEnd)
                                .navigationBarsPadding()
                                .padding(end = 18.dp, bottom = 216.dp)
                                .size(56.dp)
                                .then(SavelinGlass.modifierFor(GlassComponent.NAVIGATION, Capsule))
                                .clip(Capsule)
                                .pressable(onClick = { chromeScope.launch { viewModel.gridState.animateScrollToItem(0) } })
                                .semantics { contentDescription = "Back to top" },
                            contentAlignment = Alignment.Center,
                        ) {
                            AppIcons.Icon(
                                AppIcons.Expand,
                                null,
                                modifier = Modifier.graphicsLayer { rotationZ = 180f },
                                tint = SavelinGlass.ink,
                                size = 22.dp,
                            )
                        }
                    }
                }
                FilterRail(
                    important = importantFilter,
                    due = reminderFilter,
                    dueCount = dueCount.size,
                    visible = !searchActive,
                    onImportant = onToggleImportantFilter,
                    onDue = onToggleReminderFilter,
                    modifier = Modifier.align(Alignment.BottomEnd),
                    selectionActive = selectedIds.isNotEmpty(),
                    selectedCount = selectedIds.size,
                    onDelete = onDeleteSelected,
                    onRefetch = onRefetchSelected,
                    onCopy = onCopySelected,
                    onShare = onShareSelected,
                    onRemoveStar = onRemoveStarSelected,
                    onClearReminder = onClearReminderSelected,
                )
            }
        }
    )
}
