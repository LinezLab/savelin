package com.pinfold.app.ui.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.pinfold.app.R
import com.pinfold.app.data.model.BookmarkWithTags
import com.pinfold.app.diagnostics.DiagnosticsLog
import com.pinfold.app.ui.components.AppIcons
import com.pinfold.app.ui.components.GlassMenuItem
import com.pinfold.app.ui.components.GlassMenuOverlay
import com.pinfold.app.ui.components.ReminderPickerDialog
import com.pinfold.app.ui.components.rememberPhotoInk
import com.pinfold.app.ui.components.rememberPhotoPreviewWithLuminance
import com.pinfold.app.ui.glass.GlassChip
import com.pinfold.app.ui.glass.BarGlyphButton
import com.pinfold.app.ui.glass.GlassCircle
import com.pinfold.app.ui.glass.GlassPill
import com.pinfold.app.ui.glass.LightGlassChip
import com.pinfold.app.ui.glass.MotionSettings
import com.pinfold.app.ui.glass.SavelinGlassDialog
import com.pinfold.app.ui.glass.SpringMotion
import com.pinfold.app.ui.glass.pressable
import com.pinfold.app.ui.glass.theme.Candy
import com.pinfold.app.ui.glass.theme.clear
import java.io.File

internal const val SONIL_LOGO_FONT_RES_NAME = "nikea"
internal const val SONIL_LOGO_TEXT = "Sonil"
private val SonilLogoFont = FontFamily(Font(R.font.nikea))

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    modifier: Modifier = Modifier,
    onOpen: (BookmarkWithTags) -> Unit = {},
    selectedIds: Set<Long> = emptySet(),
    onStartSelection: (Long) -> Unit = {},
    onToggleSelected: (Long) -> Unit = {},
) {
    val items by viewModel.items.collectAsState()
    val distinctItems = remember(items) { items.distinctBy { it.bookmark.id } }
    val query by viewModel.searchQuery.collectAsState()
    val searchActive by viewModel.searchActive.collectAsState()
    val reminderIds by viewModel.reminderIds.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val context = LocalContext.current
    val gridState = viewModel.gridState
    val scrolling by rememberGridScrolling(gridState)
    val selectionMode = selectedIds.isNotEmpty()
    val spacing = when (settings.cardSpacing) {
        "NORMAL" -> 8.dp
        "ROOMY" -> 12.dp
        else -> 5.dp
    }
    var quickMenu by remember { mutableStateOf<BookmarkWithTags?>(null) }
    var reminderTarget by remember { mutableStateOf<BookmarkWithTags?>(null) }

    Box(modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
        if (searchActive && query.isNotBlank()) {
            Text(
                "${distinctItems.size} result${if (distinctItems.size == 1) "" else "s"}",
                style = MaterialTheme.typography.labelMedium,
                color = clear.onGlass,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            if (query.trim().startsWith("http")) {
                GlassPill(
                    onClick = { viewModel.saveLink(query.trim()); viewModel.onQueryChange("") },
                    modifier = Modifier.padding(horizontal = 16.dp),
                ) { Text("Add this link", color = com.pinfold.app.ui.glass.theme.clear.ink) }
            }
            if (items.isEmpty()) {
                Text(
                    "No matches. Try a tag or a site name.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = clear.onGlass,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
        }
        LazyVerticalStaggeredGrid(
            state = gridState,
            columns = StaggeredGridCells.Fixed(settings.cardColumns.coerceIn(1, 4)),
            modifier = Modifier.fillMaxSize().weight(1f),
            contentPadding = PaddingValues(
                start = 4.dp,
                top = 0.dp,
                end = 4.dp,
                bottom = 88.dp + androidx.compose.foundation.layout.WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding(),
            ),
            verticalItemSpacing = spacing,
            horizontalArrangement = Arrangement.spacedBy(spacing),
        ) {
            item(span = StaggeredGridItemSpan.FullLine) {
                Text(
                    text = SONIL_LOGO_TEXT,
                    fontFamily = SonilLogoFont,
                    fontSize = 64.sp,
                    color = clear.ink,
                    modifier = Modifier.padding(start = 16.dp, top = 4.dp, bottom = 4.dp),
                )
            }
            items(distinctItems, key = { "bookmark-${it.bookmark.id}" }, contentType = { "bookmark" }) { item ->
                BookmarkCard(
                    modifier = Modifier.animateItem(
                        placementSpec = SpringMotion.listPlace(),
                        fadeInSpec = SpringMotion.listFade(),
                        fadeOutSpec = SpringMotion.listFade(),
                    ),
                    item = item,
                    scrolling = scrolling,
                    selected = item.bookmark.id in selectedIds,
                    selectionMode = selectionMode,
                    onStartSelection = { onStartSelection(item.bookmark.id) },
                    onToggleSelected = { onToggleSelected(item.bookmark.id) },
                    hasReminder = item.bookmark.id in reminderIds,
                    titleLines = settings.cardTitleLines,
                    showAuthorLine = settings.cardShowAuthorLine,
                    captionStyle = settings.cardCaptionStyle,
                    limitTall = settings.limitTallImages,
                    onMore = { DiagnosticsLog.uiAction("Home", "More menu"); quickMenu = item },
                    onClick = {
                        DiagnosticsLog.uiAction("Home", "Open details")
                        viewModel.recordOpen(item.bookmark)
                        onOpen(item)
                    },
                    imageModel = {
                        item.bookmark.imagePath?.let { path ->
                            if (File(path).exists()) {
                                ImageRequest.Builder(context).data(File(path)).build()
                            } else null
                        }
                    },
                )
            }
        }
    }
        quickMenu?.let { chosen ->
            GlassMenuOverlay(
                title = chosen.bookmark.displayTitle.ifBlank { chosen.bookmark.title },
                onDismiss = { quickMenu = null },
                items = listOf(
                    GlassMenuItem(AppIcons.Open, "Open", onClick = {
                        context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(chosen.bookmark.url)))
                    }),
                    GlassMenuItem(AppIcons.Card, "Details", onClick = { onOpen(chosen) }),
                    GlassMenuItem(AppIcons.Important, "Important", onClick = { viewModel.toggleImportant(chosen.bookmark) }),
                    GlassMenuItem(
                        if (chosen.bookmark.id in reminderIds) AppIcons.ReminderMuted else AppIcons.Reminder,
                        if (chosen.bookmark.id in reminderIds) "Clear reminder" else "Reminder",
                        onClick = {
                            if (chosen.bookmark.id in reminderIds) viewModel.clearReminder(chosen.bookmark)
                            else reminderTarget = chosen
                        },
                    ),
                    GlassMenuItem(AppIcons.Share, "Share", onClick = {
                        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(android.content.Intent.EXTRA_TEXT, chosen.bookmark.url)
                        }
                        context.startActivity(android.content.Intent.createChooser(intent, "Share"))
                    }),
                    GlassMenuItem(AppIcons.Delete, "Delete", destructive = true, onClick = { viewModel.delete(chosen.bookmark) }),
                ),
            )
        }
        reminderTarget?.let { target ->
            ReminderPickerDialog(
                onDismiss = { reminderTarget = null },
                onPick = { trigger -> viewModel.setReminder(target.bookmark, trigger) },
            )
        }
}
}

@Composable
private fun BookmarkCard(
    modifier: Modifier = Modifier,
    item: BookmarkWithTags,
    scrolling: Boolean,
    selected: Boolean,
    selectionMode: Boolean,
    onStartSelection: () -> Unit,
    onToggleSelected: () -> Unit,
    hasReminder: Boolean,
    titleLines: Int,
    showAuthorLine: Boolean,
    captionStyle: String,
    limitTall: Boolean,
    onMore: () -> Unit,
    onClick: () -> Unit,
    imageModel: () -> Any?,
) {
    val appear = remember { Animatable(if (scrolling) 1f else 0f) }
    LaunchedEffect(Unit) { if (!scrolling) appear.animateTo(1f, SpringMotion.listFade()) }
    val bookmark = item.bookmark
    val model = remember(bookmark.imagePath) { imageModel() }
    val previewResult = rememberPhotoPreviewWithLuminance(bookmark.imagePath, bookmark.blurhash)
    val preview = previewResult.bitmap
    val photoInk = rememberPhotoInk(previewResult.luminance)
    val ratio = when {
        bookmark.imageWidth > 0 && bookmark.imageHeight > 0 -> {
            val raw = bookmark.imageWidth.toFloat() / bookmark.imageHeight.toFloat()
            if (limitTall && raw < 0.4f) 0.4f else if (limitTall && raw > 4f) 4f else raw
        }
        else -> 0.8f
    }
    Column(
        modifier.fillMaxWidth()
            .then(
                if (selected) Modifier
                    .background(clear.rim.copy(alpha = 0.18f), RoundedCornerShape(20.dp))
                    .border(2.dp, clear.rim, RoundedCornerShape(20.dp))
                else Modifier
            )
            .graphicsLayer {
                val p = appear.value.coerceIn(0f, 1f)
                alpha = p
                if (!scrolling && !MotionSettings.reduceMotion) {
                    val s = 0.94f + 0.06f * p
                    scaleX = s
                    scaleY = s
                    transformOrigin = TransformOrigin(0.5f, 0.2f)
                } else {
                    scaleX = 1f
                    scaleY = 1f
                }
            }
            .pressable(
                onClick = { if (selectionMode) onToggleSelected() else onClick() },
                onLongClick = onStartSelection,
                pressedRim = RoundedCornerShape(20.dp),
            ),
    ) {
        Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))) {
            if (model != null) {
                AsyncImage(
                    model = model,
                    contentDescription = bookmark.displayTitle.ifBlank { bookmark.title },
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().aspectRatio(ratio),
                )
            } else {
                FallbackTile(bookmark.platform.ifBlank { "Web" }, bookmark.displayTitle.ifBlank { bookmark.title })
            }
            Row(
                Modifier.fillMaxWidth().padding(6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val sourceName = bookmark.siteName.ifBlank { bookmark.platform.ifBlank { "Web" } }
                LightGlassChip(
                    label = sourceName,
                    shape = RoundedCornerShape(10.dp),
                    photo = preview,
                    photoInk = photoInk,
                    flat = scrolling,
                    icon = {
                        Box(
                            Modifier.size(14.dp).clip(CircleShape)
                                .background((photoInk ?: clear.ink).copy(alpha = 0.28f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                sourceName.take(1).uppercase(),
                                color = photoInk ?: clear.ink,
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                    },
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (bookmark.platform.lowercase() in setOf("youtube", "tiktok", "vimeo")) {
                        LightGlassChip(
                            shape = CircleShape,
                            photo = preview,
                            photoInk = photoInk,
                            flat = scrolling,
                            contentDescription = "Play video",
                        ) {
                            AppIcons.Icon(AppIcons.Open, "Play video", tint = photoInk ?: clear.ink, size = 14.dp)
                        }
                    }
                    if (bookmark.isImportant || hasReminder) {
                        LightGlassChip(
                            shape = CircleShape,
                            photo = preview,
                            photoInk = photoInk,
                            flat = scrolling,
                            contentDescription = "Bookmark status",
                        ) {
                            if (bookmark.isImportant) AppIcons.Icon(AppIcons.Important, "Important", tint = photoInk ?: clear.ink, size = 14.dp)
                            if (hasReminder) AppIcons.Icon(AppIcons.Reminder, "Reminder", tint = photoInk ?: clear.ink, size = 14.dp)
                        }
                    }
                }
            }
        }
        if (captionStyle != "IMAGE_ONLY") {
            Row(Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.fillMaxWidth(0.85f)) {
                    if (titleLines > 0) {
                        Text(
                            bookmark.displayTitle.ifBlank { bookmark.title.ifBlank { bookmark.url } },
                            color = clear.onGlass,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = titleLines,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    if (showAuthorLine) {
                        val subtitle = if (bookmark.authorHandle.isNotBlank()) "@${bookmark.authorHandle}" else bookmark.url.substringAfter("://").substringBefore('/')
                        Text(subtitle, color = clear.onGlassSoft, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                    }
                }
                if (scrolling) {
                    BarGlyphButton(onClick = onMore, label = "More options", size = 40.dp) {
                        AppIcons.Icon(AppIcons.More, null, tint = clear.onGlass, size = 20.dp)
                    }
                } else {
                    GlassCircle(
                        onClick = onMore,
                        size = 40.dp,
                        label = "More options",
                        ink = clear.onGlass,
                    ) {
                        AppIcons.Icon(AppIcons.More, null, tint = clear.onGlass, size = 20.dp)
                    }
                }
            }
        }
    }
}

@Composable
private fun FallbackTile(platform: String, title: String) {
    val colors = remember(platform) {
        when (platform.lowercase()) {
            "instagram" -> listOf(androidx.compose.ui.graphics.Color(0xFFF9CE34), androidx.compose.ui.graphics.Color(0xFFEE2A7B), androidx.compose.ui.graphics.Color(0xFF6228D7))
            "youtube" -> listOf(androidx.compose.ui.graphics.Color(0xFFFF0000), androidx.compose.ui.graphics.Color(0xFF7F0000))
            "tiktok" -> listOf(androidx.compose.ui.graphics.Color(0xFF25F4EE), androidx.compose.ui.graphics.Color(0xFFFE2C55))
            else -> listOf(androidx.compose.ui.graphics.Color(0xFF5E5CE6), androidx.compose.ui.graphics.Color(0xFF0A84FF))
        }
    }
    val gradient = remember(colors) { androidx.compose.ui.graphics.Brush.verticalGradient(colors) }
    Box(
        Modifier.fillMaxWidth().aspectRatio(4f / 5f)
            .background(gradient),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(platform, color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.55f), style = MaterialTheme.typography.headlineMedium)
            Text(
                title,
                color = androidx.compose.ui.graphics.Color.White,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 12.dp),
            )
            Text("No preview. Add image", color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.9f), style = MaterialTheme.typography.labelSmall)
        }
    }
}

/** True while the grid's first visible row is moving, plus a short settle window after it stops. */
@Composable
private fun rememberGridScrolling(state: LazyStaggeredGridState): Boolean {
    var scrolling by remember { mutableStateOf(false) }
    LaunchedEffect(state) {
        var previous = state.firstVisibleItemIndex to state.firstVisibleItemScrollOffset
        var settle: Job? = null
        snapshotFlow { state.firstVisibleItemIndex to state.firstVisibleItemScrollOffset }
            .collect { current ->
                if (current != previous) {
                    previous = current
                    scrolling = true
                    settle?.cancel()
                    settle = launch {
                        delay(120)
                        scrolling = false
                    }
                }
            }
    }
    return scrolling
}
