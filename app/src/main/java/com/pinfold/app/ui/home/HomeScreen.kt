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
    onClick: () -> Unit,
    imageModel: () -> Any?,
) {
    val bookmark = item.bookmark
    Card(
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    ) {
        val model = imageModel()
        if (model != null) {
            AsyncImage(
                model = model,
                contentDescription = bookmark.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().aspectRatio(4f / 3f),
            )
        } else {
            Box(
                Modifier.fillMaxWidth().aspectRatio(4f / 3f),
                contentAlignment = Alignment.Center,
            ) {
                Text(bookmark.platform.ifBlank { "Web" }, style = MaterialTheme.typography.labelLarge)
            }
        }
        Column(Modifier.padding(12.dp)) {
            Text(
                bookmark.title.ifBlank { bookmark.url },
                style = MaterialTheme.typography.titleSmall,
                maxLines = 2,
            )
            if (item.tags.isNotEmpty()) {
                Text(
                    item.tags.joinToString(" · ") { it.name },
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}
