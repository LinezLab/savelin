package com.pinfold.app.ui.details

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.pinfold.app.data.model.BookmarkWithTags
import com.pinfold.app.ui.glass.GlassBackButton
import com.pinfold.app.ui.home.HomeViewModel
import java.io.File

@Composable
fun DetailsScreen(
    item: BookmarkWithTags,
    viewModel: HomeViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bookmark = item.bookmark
    val context = LocalContext.current
    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) viewModel.attachImage(bookmark, uri)
    }

    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        GlassBackButton(onBack = onBack)
        bookmark.imagePath?.takeIf { File(it).exists() }?.let { path ->
            AsyncImage(
                model = ImageRequest.Builder(context).data(File(path)).build(),
                contentDescription = bookmark.title,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Text(bookmark.title.ifBlank { bookmark.platform }, style = MaterialTheme.typography.headlineMedium)
        Text(bookmark.url, style = MaterialTheme.typography.bodySmall)
        if (bookmark.caption.isNotBlank()) {
            Text(bookmark.caption, style = MaterialTheme.typography.bodyMedium)
        }
        if (bookmark.description.isNotBlank()) {
            Text(bookmark.description, style = MaterialTheme.typography.bodyMedium)
        }
        Text("Source: ${bookmark.fetchStatus} · ${bookmark.platform}", style = MaterialTheme.typography.labelMedium)

        Button(onClick = { viewModel.refetch(bookmark) }, modifier = Modifier.fillMaxWidth()) {
            Text("Refetch image")
        }
        OutlinedButton(onClick = { pickImage.launch("image/*") }, modifier = Modifier.fillMaxWidth()) {
            Text("Add custom image")
        }
        OutlinedButton(onClick = { viewModel.toggleImportant(bookmark) }, modifier = Modifier.fillMaxWidth()) {
            Text(if (bookmark.isImportant) "Remove star" else "Add to Important")
        }
        OutlinedButton(
            onClick = { viewModel.setArchived(bookmark, !bookmark.isArchived) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (bookmark.isArchived) "Unarchive" else "Archive")
        }
        OutlinedButton(
            onClick = { viewModel.setReminder(bookmark, System.currentTimeMillis() + 3_600_000L) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Remind me in 1 hour")
        }
        OutlinedButton(onClick = { viewModel.delete(bookmark) }, modifier = Modifier.fillMaxWidth()) {
            Text("Delete")
        }
    }
}
