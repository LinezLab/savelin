package com.pinfold.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
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
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.pinfold.app.data.model.BookmarkWithTags
import java.io.File

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    modifier: Modifier = Modifier,
    onOpen: (BookmarkWithTags) -> Unit = {},
) {
    val items by viewModel.items.collectAsState()
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

    val lastScroll = remember { intArrayOf(0) }
    LaunchedEffect(gridState.firstVisibleItemScrollOffset, gridState.firstVisibleItemIndex) {
        if (!searchActive) {
            val offset = gridState.firstVisibleItemScrollOffset
            viewModel.setNavVisible(gridState.firstVisibleItemIndex == 0 || offset < lastScroll[0])
            lastScroll[0] = offset
        }
    }

    Column(modifier.fillMaxSize()) {
        if (!searchActive) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                com.pinfold.app.ui.glass.GlassChip(onClick = { viewModel.setSearchActive(true) }, height = 36.dp) {
                    com.pinfold.app.ui.components.AppIcons.Icon(com.pinfold.app.ui.components.AppIcons.Search, null, size = 16.dp)
                    Text("Search")
                }
                com.pinfold.app.ui.glass.GlassChip(onClick = { viewModel.toggleImportantFilter() }, height = 36.dp) {
                    com.pinfold.app.ui.components.AppIcons.Icon(com.pinfold.app.ui.components.AppIcons.Important, null, size = 16.dp)
                    Text("Important")
                }
                com.pinfold.app.ui.glass.GlassChip(onClick = { viewModel.toggleReminderFilter() }, height = 36.dp) {
                    com.pinfold.app.ui.components.AppIcons.Icon(com.pinfold.app.ui.components.AppIcons.Reminder, null, size = 16.dp)
                    Text("Due")
                }
            }
        }
        if (searchActive && query.isNotBlank()) {
            Text(
                "${items.size} result${if (items.size == 1) "" else "s"}",
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            if (query.trim().startsWith("http")) {
                androidx.compose.material3.Button(
                    onClick = { viewModel.saveLink(query.trim()); viewModel.onQueryChange("") },
                    modifier = Modifier.padding(horizontal = 16.dp),
                ) { Text("Add this link") }
            }
            if (items.isEmpty()) {
                Text(
                    "No matches. Try a tag or a site name.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
        }
        LazyVerticalStaggeredGrid(
            state = gridState,
            columns = StaggeredGridCells.Fixed(settings.cardColumns.coerceIn(1, 4)),
            modifier = Modifier.fillMaxSize().weight(1f).background(
                if (settings.trueBlack) androidx.compose.ui.graphics.Color.Black else MaterialTheme.colorScheme.background,
            ),
            contentPadding = PaddingValues(
                start = 4.dp,
                top = 8.dp,
                end = 4.dp,
                bottom = 88.dp + androidx.compose.foundation.layout.WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding(),
            ),
            verticalItemSpacing = spacing,
            horizontalArrangement = Arrangement.spacedBy(spacing),
        ) {
            items(items, key = { it.bookmark.id }) { item ->
                BookmarkCard(
                    item = item,
                    hasReminder = item.bookmark.id in reminderIds,
                    titleLines = settings.cardTitleLines,
                    showAuthorLine = settings.cardShowAuthorLine,
                    captionStyle = settings.cardCaptionStyle,
                    limitTall = settings.limitTallImages,
                    onMore = { quickMenu = item },
                    onClick = {
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
        quickMenu?.let { chosen ->
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { quickMenu = null },
                title = { Text(chosen.bookmark.displayTitle.ifBlank { chosen.bookmark.title }) },
                text = {
                    Column {
                        listOf("Open", "Details", "Important", "Reminder", "Share", "Delete").forEach { action ->
                            Text(
                                action,
                                modifier = Modifier.fillMaxWidth().clickable {
                                    when (action) {
                                        "Open" -> context.startActivity(
                                            android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(chosen.bookmark.url)),
                                        )
                                        "Details" -> onOpen(chosen)
                                        "Important" -> viewModel.toggleImportant(chosen.bookmark)
                                        "Reminder" -> viewModel.setReminder(chosen.bookmark, System.currentTimeMillis() + 3_600_000L)
                                        "Share" -> {
                                            val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                                type = "text/plain"
                                                putExtra(android.content.Intent.EXTRA_TEXT, chosen.bookmark.url)
                                            }
                                            context.startActivity(android.content.Intent.createChooser(intent, "Share"))
                                        }
                                        "Delete" -> viewModel.delete(chosen.bookmark)
                                    }
                                    quickMenu = null
                                }.padding(vertical = 10.dp),
                            )
                        }
                    }
                },
                confirmButton = { androidx.compose.material3.TextButton(onClick = { quickMenu = null }) { Text("Close") } },
            )
        }
    }
}

@Composable
private fun BookmarkCard(
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
    val bookmark = item.bookmark
    val model = imageModel()
    val ratio = when {
        bookmark.imageWidth > 0 && bookmark.imageHeight > 0 -> {
            val raw = bookmark.imageWidth.toFloat() / bookmark.imageHeight.toFloat()
            if (limitTall && raw < 0.4f) 0.4f else if (limitTall && raw > 4f) 4f else raw
        }
        else -> 0.8f
    }
    Column(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
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
            ) {
                Box(
                    Modifier.clip(RoundedCornerShape(10.dp))
                        .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.45f))
                        .padding(horizontal = 7.dp, vertical = 3.dp),
                ) {
                    Text(bookmark.platform.ifBlank { "Web" }, color = androidx.compose.ui.graphics.Color.White, style = MaterialTheme.typography.labelSmall)
                }
                if (bookmark.isImportant || hasReminder) {
                    Row(Modifier.clip(androidx.compose.foundation.shape.CircleShape).background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.45f)).padding(5.dp)) {
                        if (bookmark.isImportant) com.pinfold.app.ui.components.AppIcons.Icon(
                            com.pinfold.app.ui.components.AppIcons.Important, "Important", tint = androidx.compose.ui.graphics.Color.White, size = 14.dp,
                        )
                        if (hasReminder) com.pinfold.app.ui.components.AppIcons.Icon(
                            com.pinfold.app.ui.components.AppIcons.Reminder, "Reminder", tint = androidx.compose.ui.graphics.Color.White, size = 14.dp,
                        )
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
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = titleLines,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    if (showAuthorLine) {
                        val subtitle = if (bookmark.authorHandle.isNotBlank()) "@${bookmark.authorHandle}" else bookmark.url.substringAfter("://").substringBefore('/')
                        Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                    }
                }
                Box(Modifier.size(40.dp).clickable(onClick = onMore), contentAlignment = Alignment.Center) {
                    com.pinfold.app.ui.components.AppIcons.Icon(
                        com.pinfold.app.ui.components.AppIcons.More,
                        "More options",
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
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
