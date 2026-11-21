package com.pinfold.app.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
    val context = LocalContext.current

    Column(modifier.fillMaxSize()) {
        OutlinedTextField(
            value = query,
            onValueChange = viewModel::onQueryChange,
            placeholder = { Text("Search bookmarks") },
            singleLine = true,
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        )
        LazyVerticalStaggeredGrid(
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
