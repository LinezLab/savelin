package com.pinfold.app.ui.details

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.pinfold.app.data.model.BookmarkWithTags
import com.pinfold.app.ui.components.NavIcons
import com.pinfold.app.ui.glass.Capsule
import com.pinfold.app.ui.glass.GlassBackButton
import com.pinfold.app.ui.glass.GlassCircle
import com.pinfold.app.ui.glass.RoundedRectangle
import com.pinfold.app.ui.glass.glass
import com.pinfold.app.ui.glass.theme.Candy
import com.pinfold.app.ui.glass.theme.clear
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
    val glass = clear
    var expanded by remember { mutableStateOf(false) }
    var cameraMenu by remember { mutableStateOf(false) }
    var moreMenu by remember { mutableStateOf(false) }
    var notes by remember(bookmark.id) { mutableStateOf(bookmark.notes) }
    val imagePath = bookmark.imagePath?.takeIf { File(it).exists() }
    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) viewModel.attachImage(bookmark, uri)
    }

    val gradient = Brush.verticalGradient(
        listOf(
            Candy.Bubblegum.copy(alpha = 0.85f),
            Candy.Grape.copy(alpha = 0.9f),
            Candy.Tangerine.copy(alpha = 0.8f),
        ),
    )

    Box(modifier.fillMaxSize().background(gradient)) {
        if (imagePath != null) {
            AsyncImage(
                model = ImageRequest.Builder(context).data(File(imagePath)).build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().blur(28.dp),
            )
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(
                        listOf(Color.Black.copy(alpha = 0.55f), Color.Black.copy(alpha = 0.78f)),
                    ),
                ),
            )
        } else {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.35f)))
        }

        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Spacer(Modifier.height(48.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                GlassBackButton(onBack = onBack)
                val share = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, bookmark.url)
                }
                GlassCircle(onClick = { context.startActivity(Intent.createChooser(share, "Share")) }, size = 44.dp, label = "Share") {
                    Text("↗", color = glass.ink)
                }
                GlassCircle(onClick = { moreMenu = true }, size = 44.dp, label = "More") {
                    Text("⋯", color = glass.ink)
                }
            }

            if (imagePath != null) {
                BoxWithConstraints(Modifier.fillMaxWidth()) {
                    val height = minOf(maxHeight * 0.55f, 420.dp)
                    Box(
                        Modifier.fillMaxWidth().height(height).clip(RoundedRectangle(28.dp))
                            .border(1.dp, glass.rim, RoundedRectangle(28.dp)),
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(context).data(File(imagePath)).build(),
                            contentDescription = bookmark.displayTitle.ifBlank { bookmark.title },
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.35f)),
                        )
                        GlassCircle(
                            onClick = { cameraMenu = true },
                            modifier = Modifier.align(Alignment.BottomEnd).padding(12.dp),
                            size = 44.dp,
                            label = "Image options",
                        ) { Text("◉", color = glass.ink) }
                    }
                }
            } else {
                Box(
                    Modifier.fillMaxWidth().height(220.dp).clip(RoundedRectangle(28.dp))
                        .background(Brush.verticalGradient(listOf(Candy.Bubblegum, Candy.Grape, Candy.Tangerine))),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("No preview. Add image", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }

            if (cameraMenu) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GlassChipButton("Refetch") { viewModel.refetch(bookmark); cameraMenu = false }
                    GlassChipButton("Add custom") { pickImage.launch("image/*"); cameraMenu = false }
                    GlassChipButton("Remove") { viewModel.removeImage(bookmark); cameraMenu = false }
                }
            }
            if (moreMenu) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    GlassChipButton("Refetch image") { viewModel.refetch(bookmark); moreMenu = false }
                    GlassChipButton("Add custom image") { pickImage.launch("image/*"); moreMenu = false }
                    GlassChipButton("Copy link") {
                        val manager = context.getSystemService(android.content.ClipboardManager::class.java)
                        manager?.setPrimaryClip(android.content.ClipData.newPlainText("link", bookmark.url))
                        moreMenu = false
                    }
                    GlassChipButton("Delete") { viewModel.delete(bookmark); onBack() }
                }
            }

            Column(
                Modifier.fillMaxWidth().glass(RoundedRectangle(28.dp), glass.frost, blur = 8.dp).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GlassChipButton(bookmark.platform.ifBlank { "Web" }) {}
                        if (bookmark.authorHandle.isNotBlank()) GlassChipButton("@${bookmark.authorHandle}") {}
                        if (bookmark.postedAt.isNotBlank()) GlassChipButton(bookmark.postedAt) {}
                    }
                    GlassChipButton(if (bookmark.isImportant) "★" else "☆") { viewModel.toggleImportant(bookmark) }
                }
                Text(
                    bookmark.displayTitle.ifBlank { bookmark.title.ifBlank { bookmark.url } },
                    style = MaterialTheme.typography.headlineSmall,
                    color = glass.ink,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(shortUrl(bookmark.url), style = MaterialTheme.typography.bodySmall, color = glass.inkSoft)
                    Text("copy", style = MaterialTheme.typography.labelSmall, color = Candy.Sky,
                        modifier = Modifier.clickable {
                            val manager = context.getSystemService(android.content.ClipboardManager::class.java)
                            manager?.setPrimaryClip(android.content.ClipData.newPlainText("link", bookmark.url))
                        })
                }
                if (bookmark.likesText.isNotBlank() || bookmark.commentsText.isNotBlank()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (bookmark.likesText.isNotBlank()) GlassChipButton("♥ ${bookmark.likesText}") {}
                        if (bookmark.commentsText.isNotBlank()) GlassChipButton("💬 ${bookmark.commentsText}") {}
                    }
                }
                if (bookmark.fetchStatus != com.pinfold.app.data.model.FetchStatus.OK) {
                    GlassChipButton("Preview incomplete. Refetch") { viewModel.refetch(bookmark) }
                }
                Divider(color = glass.line)
                if (bookmark.caption.isNotBlank()) {
                    SelectionContainer {
                        Text(
                            bookmark.caption,
                            style = MaterialTheme.typography.bodyMedium,
                            color = glass.ink,
                            maxLines = if (expanded) Int.MAX_VALUE else 4,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Text(if (expanded) "Show less" else "Show more", color = Candy.Sky,
                        modifier = Modifier.clickable { expanded = !expanded })
                }
                Divider(color = glass.line)
                val tags = item.tags.map { it.name } + bookmark.hashtags.split(',').filter { it.isNotBlank() }
                if (tags.isNotEmpty()) {
                    Text("Tags", style = MaterialTheme.typography.labelLarge, color = glass.ink)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        tags.take(6).forEach { GlassChipButton(it) {} }
                    }
                }
                Divider(color = glass.line)
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it; viewModel.setNotes(bookmark, it) },
                    label = { Text("Notes") },
                    modifier = Modifier.fillMaxWidth(),
                )
                GlassChipButton(if (System.currentTimeMillis() > 0) "Add reminder" else "Add reminder") {
                    viewModel.setReminder(bookmark, System.currentTimeMillis() + 3_600_000L)
                }
                Text(
                    "Saved ${android.text.format.DateFormat.getDateFormat(context).format(java.util.Date(bookmark.createdAt))} · Opened ${bookmark.openCount} times",
                    style = MaterialTheme.typography.labelSmall,
                    color = glass.inkSoft,
                )
            }
            Spacer(Modifier.height(96.dp))
        }

        Row(
            Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 18.dp)
                .glass(Capsule, glass.glass, blur = 3.dp, lensHeight = 14.dp, lensAmount = 26.dp).padding(6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            GlassChipButton("Open in ${bookmark.platform.ifBlank { "browser" }}") {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(bookmark.url))
                context.startActivity(intent)
            }
            GlassCircle(onClick = { viewModel.toggleImportant(bookmark) }, size = 42.dp, label = "Important") {
                Text(if (bookmark.isImportant) "★" else "☆", color = glass.ink)
            }
            GlassCircle(onClick = { viewModel.setReminder(bookmark, System.currentTimeMillis() + 3_600_000L) }, size = 42.dp, label = "Reminder") {
                Text("🔔", color = glass.ink)
            }
            GlassCircle(onClick = { moreMenu = true }, size = 42.dp, label = "More") {
                Text("⋯", color = glass.ink)
            }
        }
    }
}

@Composable
private fun GlassChipButton(label: String, onClick: () -> Unit) {
    val glass = clear
    Text(
        label,
        color = glass.ink,
        style = MaterialTheme.typography.labelMedium,
        modifier = Modifier.clip(Capsule).glass(Capsule, glass.glass, blur = 2.dp).clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        maxLines = 1,
    )
}

private fun shortUrl(url: String): String {
    val withoutScheme = url.substringAfter("://").removeSuffix("/")
    return if (withoutScheme.length <= 44) withoutScheme else {
        withoutScheme.take(22) + "…" + withoutScheme.takeLast(18)
    }
}
