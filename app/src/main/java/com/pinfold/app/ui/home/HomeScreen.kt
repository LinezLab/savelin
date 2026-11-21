package com.pinfold.app.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
    val context = LocalContext.current
    val gridState = rememberLazyStaggeredGridState()

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
                FilterChip(
                    selected = false,
                    onClick = { viewModel.setSearchActive(true) },
                    label = { Text("Search") },
                )
                FilterChip(
                    selected = false,
                    onClick = { viewModel.toggleImportantFilter() },
                    label = { Text("Important") },
                )
                FilterChip(
                    selected = false,
                    onClick = { viewModel.toggleReminderFilter() },
                    label = { Text("Due") },
                )
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
            columns = StaggeredGridCells.Adaptive(160.dp),
            modifier = Modifier.fillMaxSize().weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalItemSpacing = 12.dp,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(items, key = { it.bookmark.id }) { item ->
                BookmarkCard(
                    item = item,
                    hasReminder = item.bookmark.id in reminderIds,
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
    }
}

@Composable
private fun BookmarkCard(
    item: BookmarkWithTags,
    hasReminder: Boolean,
    onClick: () -> Unit,
    imageModel: () -> Any?,
) {
    val bookmark = item.bookmark
    val model = imageModel()
    Box(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick),
    ) {
        if (model != null) {
            AsyncImage(
                model = model,
                contentDescription = bookmark.displayTitle.ifBlank { bookmark.title },
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().aspectRatio(4f / 3f),
            )
        } else {
            FallbackTile(bookmark.platform.ifBlank { "Web" }, bookmark.displayTitle.ifBlank { bookmark.title })
        }
        Row(
            Modifier.fillMaxWidth().padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Box(
                Modifier.clip(RoundedCornerShape(12.dp))
                    .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.45f))
                    .padding(horizontal = 8.dp, vertical = 3.dp),
            ) {
                Text(bookmark.platform.ifBlank { "Web" }, color = androidx.compose.ui.graphics.Color.White, style = MaterialTheme.typography.labelSmall)
            }
            if (bookmark.isImportant || hasReminder) {
                Box(Modifier.clip(androidx.compose.foundation.shape.CircleShape).background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.45f)).padding(6.dp)) {
                    Text(
                        listOfNotNull(if (bookmark.isImportant) "★" else null, if (hasReminder) "●" else null).joinToString(""),
                        color = androidx.compose.ui.graphics.Color.White,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
        }
        Column(
            Modifier.fillMaxWidth().align(Alignment.BottomStart)
                .background(androidx.compose.ui.graphics.Brush.verticalGradient(
                    listOf(androidx.compose.ui.graphics.Color.Transparent, androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.78f)),
                ))
                .padding(12.dp),
        ) {
            Text(
                bookmark.displayTitle.ifBlank { bookmark.title.ifBlank { bookmark.url } },
                color = androidx.compose.ui.graphics.Color.White,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            val subtitle = if (bookmark.authorHandle.isNotBlank()) "@${bookmark.authorHandle}" else bookmark.url.substringAfter("://").substringBefore('/')
            Text(subtitle, color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.labelSmall, maxLines = 1)
            val tags = (item.tags.map { it.name } + bookmark.hashtags.split(',').filter { it.isNotBlank() }).take(2)
            if (tags.isNotEmpty()) {
                Text(tags.joinToString("  "), color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.labelSmall, maxLines = 1)
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
        Modifier.fillMaxWidth().aspectRatio(4f / 3f)
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
