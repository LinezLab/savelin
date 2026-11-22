package com.pinfold.app.ui.details

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.DpOffset
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.pinfold.app.data.model.BookmarkWithTags
import com.pinfold.app.network.MetadataCleaner
import com.pinfold.app.ui.components.AppIcons
import com.pinfold.app.ui.glass.Capsule
import com.pinfold.app.ui.glass.GlassChip
import com.pinfold.app.ui.glass.GlassCircle
import com.pinfold.app.ui.glass.GlassSettings
import com.pinfold.app.ui.glass.RoundedRectangle
import com.pinfold.app.ui.glass.glass
import com.pinfold.app.ui.glass.theme.Candy
import com.pinfold.app.ui.glass.theme.clear
import com.pinfold.app.ui.home.HomeViewModel
import kotlinx.coroutines.delay
import java.io.File

@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun DetailsScreen(
    item: BookmarkWithTags,
    viewModel: HomeViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val bookmark = item.bookmark
    val colors = clear
    val style = GlassSettings.style
    val ink = colors.ink

    var expandedCaption by remember { mutableStateOf(false) }
    var cameraMenu by remember { mutableStateOf(false) }
    var moreMenu by remember { mutableStateOf(false) }
    var reminderOpen by remember { mutableStateOf(false) }
    var showFullUrl by remember { mutableStateOf(false) }
    var copied by remember { mutableStateOf(false) }
    var notes by remember(bookmark.id) { mutableStateOf(bookmark.notes) }
    var reminderText by remember(bookmark.id) { mutableStateOf("") }
    val imagePath = bookmark.imagePath?.takeIf { File(it).exists() }
    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) viewModel.attachImage(bookmark, uri)
    }
    val target = remember(bookmark.url) { OpenTargetResolver.resolve(context, bookmark.url) }
    val openName = target.label ?: bookmark.siteName.ifBlank {
        bookmark.url.substringAfter("://").substringBefore('/').removePrefix("www.")
    }

    LaunchedEffect(copied) {
        if (copied) {
            delay(1500)
            copied = false
        }
    }

    fun copyUrl(original: Boolean) {
        val value = if (original) bookmark.url else com.pinfold.app.network.UrlNormalizer.normalize(bookmark.url)
        val manager = context.getSystemService(android.content.ClipboardManager::class.java)
        manager?.setPrimaryClip(android.content.ClipData.newPlainText("link", value))
        copied = true
    }

    Box(modifier.fillMaxSize().background(colors.wall.last())) {
        if (imagePath != null) {
            AsyncImage(
                model = ImageRequest.Builder(context).data(File(imagePath)).build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().blur(28.dp),
            )
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Black.copy(0.5f), Color.Black.copy(0.72f)))))
        }

        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .windowInsetsPadding(WindowInsets.statusBars.union(WindowInsets.displayCutout))
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val heroHeight = minOf(maxHeight * 0.5f, 380.dp)
                Box(
                    Modifier.fillMaxWidth().height(heroHeight).clip(RoundedRectangle(28.dp))
                        .glass(RoundedRectangle(28.dp), colors.glass, blur = style.blur.dp,
                            lensHeight = style.refractionHeight.dp, lensAmount = style.refractionAmount.dp),
                ) {
                    if (imagePath != null) {
                        AsyncImage(
                            model = ImageRequest.Builder(context).data(File(imagePath)).build(),
                            contentDescription = bookmark.displayTitle,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Candy.Bubblegum, Candy.Grape, Candy.Tangerine))))
                    }
                    Box(Modifier.align(Alignment.BottomEnd).padding(12.dp)) {
                        GlassCircle(
                            onClick = { pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                            onLongClick = { cameraMenu = true },
                            size = 44.dp,
                            label = "Image options",
                        ) { AppIcons.Icon(AppIcons.Camera, "Image options", tint = ink) }
                        DropdownMenu(expanded = cameraMenu, onDismissRequest = { cameraMenu = false }, offset = DpOffset((-180).dp, (-140).dp)) {
                            DropdownMenuItem(text = { Text("Refetch image") }, onClick = { viewModel.refetch(bookmark); cameraMenu = false })
                            DropdownMenuItem(text = { Text("Remove custom image") }, onClick = { viewModel.removeImage(bookmark); cameraMenu = false })
                        }
                    }
                }
            }

            Column(
                Modifier.fillMaxWidth()
                    .glass(RoundedRectangle(28.dp), colors.glass, blur = style.blur.dp,
                        lensHeight = style.refractionHeight.dp, lensAmount = style.refractionAmount.dp)
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    GlassTag(bookmark.platform.ifBlank { "Web" })
                    if (bookmark.authorHandle.isNotBlank()) GlassTag("@${bookmark.authorHandle}")
                    if (bookmark.postedAt.isNotBlank()) GlassTag(bookmark.postedAt)
                    if (bookmark.isImportant) AppIcons.Icon(AppIcons.Important, "Important", tint = ink, size = 16.dp)
                }

                var titleExpanded by remember { mutableStateOf(false) }
                Text(
                    bookmark.displayTitle.ifBlank { bookmark.title.ifBlank { bookmark.url } },
                    style = MaterialTheme.typography.headlineSmall,
                    color = ink,
                    maxLines = if (titleExpanded) Int.MAX_VALUE else 4,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth().combinedClickable(
                        remember { androidx.compose.foundation.interaction.MutableInteractionSource() }, null,
                    ) { titleExpanded = !titleExpanded },
                )

                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (showFullUrl) bookmark.url else shortUrl(bookmark.url),
                        color = ink,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = if (showFullUrl) Int.MAX_VALUE else 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth(0.8f).combinedClickable(
                            remember { androidx.compose.foundation.interaction.MutableInteractionSource() }, null,
                            onClick = { showFullUrl = !showFullUrl },
                            onLongClick = { copyUrl(original = true) },
                        ),
                    )
                    Spacer(Modifier.size(8.dp))
                    GlassCircle(onClick = { copyUrl(false) }, size = 44.dp, label = "Copy link") {
                        AppIcons.Icon(if (copied) AppIcons.Selected else AppIcons.Copy, "Copy link", tint = ink)
                    }
                }

                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (bookmark.likesText.isNotBlank()) {
                        GlassTag("Likes ${bookmark.likesText}", icon = AppIcons.Likes)
                    }
                    if (bookmark.commentsText.isNotBlank()) {
                        GlassTag("Comments ${bookmark.commentsText}", icon = AppIcons.Comments)
                    }
                }

                Divider(color = colors.line)
                val caption = MetadataCleaner.cleanForDisplay(bookmark.caption)
                if (caption.isNotBlank()) {
                    SelectionContainer {
                        Text(caption, style = MaterialTheme.typography.bodyMedium, color = ink,
                            maxLines = if (expandedCaption) Int.MAX_VALUE else 4, overflow = TextOverflow.Ellipsis)
                    }
                    Box(Modifier.size(44.dp).combinedClickable(
                        remember { androidx.compose.foundation.interaction.MutableInteractionSource() }, null,
                    ) { expandedCaption = !expandedCaption }, contentAlignment = Alignment.Center) {
                        AppIcons.Icon(AppIcons.Expand, "Expand caption", tint = ink)
                    }
                }

                Divider(color = colors.line)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    item.tags.forEach { GlassTag(it.name) }
                    GlassTag("+", icon = AppIcons.Add)
                }

                Divider(color = colors.line)
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it; viewModel.setNotes(bookmark, it) },
                    label = { Text("Notes") },
                    modifier = Modifier.fillMaxWidth(),
                )

                GlassChip(onClick = { reminderOpen = true }, height = 40.dp, label = "Reminder") {
                    AppIcons.Icon(AppIcons.Snooze, null, tint = ink, size = 16.dp)
                    Text(
                        reminderText.ifBlank { "Add reminder" },
                        color = ink,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }

                Text(
                    "Saved ${android.text.format.DateFormat.getDateFormat(context).format(java.util.Date(bookmark.createdAt))} · Opened ${bookmark.openCount} times",
                    color = ink,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
            Spacer(Modifier.height(120.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()))
        }

        Row(
            Modifier.zIndex(10f).align(Alignment.BottomCenter).fillMaxWidth()
                .navigationBarsPadding().padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
                .height(60.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            GlassCircle(onClick = onBack, onLongClick = onBack, size = 56.dp, label = "Back") {
                AppIcons.Icon(AppIcons.Back, "Back", tint = ink)
            }
            Box(
                Modifier.fillMaxWidth().height(60.dp)
                    .glass(Capsule, colors.glass, blur = style.blur.dp,
                        lensHeight = style.refractionHeight.dp, lensAmount = style.refractionAmount.dp)
                    .clip(Capsule).padding(6.dp),
            ) {
                Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.fillMaxWidth(0.8f)) {
                        GlassChip(
                            onClick = { openLink(context, bookmark.url, target.packageName) },
                            height = 48.dp,
                            label = "Open in $openName",
                        ) {
                            Text(
                                "Open in $openName",
                                color = ink,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    Box {
                        GlassCircle(onClick = { moreMenu = true }, onLongClick = { moreMenu = true }, size = 48.dp, label = "More") {
                            AppIcons.Icon(AppIcons.More, "More", tint = ink)
                        }
                        DropdownMenu(expanded = moreMenu, onDismissRequest = { moreMenu = false }, offset = DpOffset(0.dp, (-180).dp)) {
                            DropdownMenuItem(
                                text = { Text(if (bookmark.isImportant) "Remove important" else "Mark important") },
                                leadingIcon = { AppIcons.Icon(AppIcons.Important, null, tint = ink, size = 20.dp) },
                                onClick = { viewModel.toggleImportant(bookmark); moreMenu = false },
                            )
                            DropdownMenuItem(
                                text = { Text(if (reminderText.isBlank()) "Set reminder" else "Edit reminder") },
                                leadingIcon = { AppIcons.Icon(AppIcons.Reminder, null, tint = ink, size = 20.dp) },
                                onClick = { reminderOpen = true; moreMenu = false },
                            )
                            DropdownMenuItem(
                                text = { Text("Share") },
                                leadingIcon = { AppIcons.Icon(AppIcons.Share, null, tint = ink, size = 20.dp) },
                                onClick = {
                                    val share = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, bookmark.url) }
                                    context.startActivity(Intent.createChooser(share, "Share"))
                                    moreMenu = false
                                },
                            )
                        }
                    }
                }
            }
        }

        if (reminderOpen) {
            Box(Modifier.zIndex(20f).fillMaxSize().background(Color.Black.copy(alpha = 0.55f))) {
                Column(
                    Modifier.align(Alignment.Center).fillMaxWidth().padding(24.dp)
                        .glass(RoundedRectangle(24.dp), colors.glass, blur = style.blur.dp,
                            lensHeight = style.refractionHeight.dp, lensAmount = style.refractionAmount.dp)
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text("Set reminder", style = MaterialTheme.typography.headlineSmall, color = ink)
                    Text(bookmark.displayTitle.ifBlank { bookmark.title }, color = ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    OutlinedTextField(
                        value = reminderText,
                        onValueChange = { reminderText = it },
                        label = { Text("When") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("In 1 hour", "Tonight", "Tomorrow morning", "Next week").forEach { label ->
                            GlassChip(onClick = { reminderText = label }, height = 36.dp, label = label) {
                                Text(label, color = ink, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = { reminderOpen = false }) { Text("Cancel") }
                        TextButton(onClick = {
                            if (reminderText.isNotBlank()) {
                                viewModel.setReminder(bookmark, System.currentTimeMillis() + 3_600_000L)
                                reminderOpen = false
                            }
                        }) { Text("Set reminder") }
                        if (reminderText.isNotBlank()) {
                            TextButton(onClick = { reminderText = ""; reminderOpen = false }) { Text("Remove reminder") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GlassTag(label: String, icon: Int? = null) {
    GlassChip(onClick = {}, height = 32.dp, contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp), label = label) {
        if (icon != null) AppIcons.Icon(icon, null, tint = clear.ink, size = 16.dp)
        Text(label, color = clear.ink, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

private fun openLink(context: android.content.Context, url: String, pkg: String?) {
    val intent = if (pkg != null) {
        context.packageManager.getLaunchIntentForPackage(pkg)?.apply { data = Uri.parse(url) }
            ?: Intent(Intent.ACTION_VIEW, Uri.parse(url))
    } else Intent(Intent.ACTION_VIEW, Uri.parse(url))
    context.startActivity(intent)
}

private fun shortUrl(url: String): String {
    val withoutScheme = url.substringAfter("://").removePrefix("www.").removeSuffix("/")
    return if (withoutScheme.length <= 44) withoutScheme else withoutScheme.take(22) + "…" + withoutScheme.takeLast(18)
}
