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
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
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
import com.pinfold.app.ui.components.rememberPhotoInk
import com.pinfold.app.ui.components.rememberPhotoPreview
import com.pinfold.app.ui.glass.GlassChip
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
) {
    val items by viewModel.items.collectAsState()
    val distinctItems = remember(items) { items.distinctBy { it.bookmark.id } }
    val query by viewModel.searchQuery.collectAsState()
    val searchActive by viewModel.searchActive.collectAsState()
    val reminderIds by viewModel.reminderIds.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val context = LocalContext.current
    val gridState = viewModel.gridState
    val spacing = when (settings.cardSpacing) {
        "NORMAL" -> 8.dp
        "ROOMY" -> 12.dp
        else -> 5.dp
    }
    var quickMenu by remember { mutableStateOf<BookmarkWithTags?>(null) }

    Box(modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
        Text(
            text = SONIL_LOGO_TEXT,
            fontFamily = SonilLogoFont,
            fontSize = 48.sp,
            color = clear.ink,
            modifier = Modifier.padding(start = 16.dp, top = 4.dp, bottom = 4.dp),
        )
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
                top = 8.dp,
                end = 4.dp,
                bottom = 88.dp + androidx.compose.foundation.layout.WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding(),
            ),
            verticalItemSpacing = spacing,
            horizontalArrangement = Arrangement.spacedBy(spacing),
        ) {
            items(distinctItems, key = { "bookmark-${it.bookmark.id}" }, contentType = { "bookmark" }) { item ->
                BookmarkCard(
                    modifier = Modifier.animateItem(
                        placementSpec = SpringMotion.listPlace(),
                        fadeInSpec = SpringMotion.listFade(),
                        fadeOutSpec = SpringMotion.listFade(),
                    ),
                    item = item,
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
                    GlassMenuItem(AppIcons.Reminder, "Reminder", onClick = { viewModel.setReminder(chosen.bookmark, System.currentTimeMillis() + 3_600_000L) }),
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
}
}

@Composable
private fun BookmarkCard(
    modifier: Modifier = Modifier,
    item: BookmarkWithTags,
    hasReminder: Boolean,
    titleLines: Int,
    showAuthorLine: Boolean,
    captionStyle: String,
    limitTall: Boolean,
    onMore: () -> Unit,
    onClick: () -> Unit,
    imageModel: () -> Any?,
) {
    val appear = remember { Animatable(0f) }
    LaunchedEffect(Unit) { appear.animateTo(1f, SpringMotion.listFade()) }
    val bookmark = item.bookmark
    val model = imageModel()
    val preview = rememberPhotoPreview(bookmark.imagePath, bookmark.blurhash)
    val photoInk = rememberPhotoInk(preview)
    val ratio = when {
        bookmark.imageWidth > 0 && bookmark.imageHeight > 0 -> {
            val raw = bookmark.imageWidth.toFloat() / bookmark.imageHeight.toFloat()
            if (limitTall && raw < 0.4f) 0.4f else if (limitTall && raw > 4f) 4f else raw
        }
        else -> 0.8f
    }
    Column(
        modifier.fillMaxWidth()
            .graphicsLayer {
                val p = appear.value.coerceIn(0f, 1f)
                alpha = p
                if (!MotionSettings.reduceMotion) {
                    val s = 0.94f + 0.06f * p
                    scaleX = s
                    scaleY = s
                    transformOrigin = TransformOrigin(0.5f, 0.2f)
                }
            }
            .pressable(onClick = onClick, pressedRim = RoundedCornerShape(20.dp)),
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

@Composable
private fun FallbackTile(platform: String, title: String) {
    val colors = when (platform.lowercase()) {
        "instagram" -> listOf(androidx.compose.ui.graphics.Color(0xFFF9CE34), androidx.compose.ui.graphics.Color(0xFFEE2A7B), androidx.compose.ui.graphics.Color(0xFF6228D7))
        "youtube" -> listOf(androidx.compose.ui.graphics.Color(0xFFFF0000), androidx.compose.ui.graphics.Color(0xFF7F0000))
        "tiktok" -> listOf(androidx.compose.ui.graphics.Color(0xFF25F4EE), androidx.compose.ui.graphics.Color(0xFFFE2C55))
        else -> listOf(androidx.compose.ui.graphics.Color(0xFF5E5CE6), androidx.compose.ui.graphics.Color(0xFF0A84FF))
    }
    Box(
        Modifier.fillMaxWidth().aspectRatio(4f / 5f)
            .background(androidx.compose.ui.graphics.Brush.verticalGradient(colors)),
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
