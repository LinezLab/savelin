package com.pinfold.app.ui.details

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.unit.DpOffset
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.pinfold.app.data.model.BookmarkWithTags
import com.pinfold.app.network.MetadataCleaner
import com.pinfold.app.ui.components.AppIcons
import com.pinfold.app.ui.glass.Capsule
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
    val glass = clear
    val bookmark = item.bookmark
    var expandedCaption by remember { mutableStateOf(false) }
    var cameraMenu by remember { mutableStateOf(false) }
    var moreMenu by remember { mutableStateOf(false) }
    var openMenu by remember { mutableStateOf(false) }
    var reminderMenu by remember { mutableStateOf(false) }
    var showFullUrl by remember { mutableStateOf(false) }
    var copied by remember { mutableStateOf(false) }
    var notes by remember(bookmark.id) { mutableStateOf(bookmark.notes) }
    val imagePath = bookmark.imagePath?.takeIf { File(it).exists() }
    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) viewModel.attachImage(bookmark, uri)
    }
    val target = remember(bookmark.url) { OpenTargetResolver.resolve(context, bookmark.url) }

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

    val gradient = Brush.verticalGradient(
        listOf(Candy.Bubblegum.copy(alpha = 0.85f), Candy.Grape.copy(alpha = 0.9f), Candy.Tangerine.copy(alpha = 0.8f)),
    )

    Box(modifier.fillMaxSize().background(gradient)) {
        if (imagePath != null) {
            AsyncImage(
                model = ImageRequest.Builder(context).data(File(imagePath)).build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().blur(28.dp),
            )
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Black.copy(0.55f), Color.Black.copy(0.78f)))))
        } else {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.35f)))
        }

        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .windowInsetsPadding(WindowInsets.statusBars.union(WindowInsets.displayCutout))
                .padding(horizontal = 16.dp),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(16.dp),
        ) {
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val height = minOf(maxHeight * 0.55f, 420.dp)
                Box(
                    Modifier.fillMaxWidth().height(height).clip(RoundedRectangle(28.dp))
                        .border(1.dp, glass.rim, RoundedRectangle(28.dp)),
                ) {
                    if (imagePath != null) {
                        AsyncImage(
                            model = ImageRequest.Builder(context).data(File(imagePath)).build(),
                            contentDescription = bookmark.displayTitle.ifBlank { bookmark.title },
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.35f)),
                        )
                    } else {
                        Box(
                            Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Candy.Bubblegum, Candy.Grape, Candy.Tangerine))),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                AppIcons.Icon(AppIcons.NoPreview, null, tint = Color.White, size = 32.dp)
                                Text("No preview. Add image", color = Color.White)
                            }
                        }
                    }
                    Box(Modifier.align(Alignment.BottomEnd).padding(12.dp)) {
                        GlassCircle(
                            onClick = { pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                            onLongClick = { cameraMenu = true },
                            size = 44.dp,
                            label = "Image options",
                        ) { AppIcons.Icon(AppIcons.Camera, "Image options", tint = glass.ink) }
                        DropdownMenu(expanded = cameraMenu, onDismissRequest = { cameraMenu = false }, offset = DpOffset((-180).dp, (-140).dp)) {
                            DropdownMenuItem(
                                text = { Text("Refetch image") },
                                leadingIcon = { AppIcons.Icon(AppIcons.Refetch, null, size = 20.dp) },
                                onClick = { viewModel.refetch(bookmark); cameraMenu = false },
                            )
                            DropdownMenuItem(
                                text = { Text("Remove image") },
                                leadingIcon = { AppIcons.Icon(AppIcons.NoPreview, null, size = 20.dp) },
                                onClick = { viewModel.removeImage(bookmark); cameraMenu = false },
                            )
                        }
                    }
                }
            }

            Column(
                Modifier.fillMaxWidth().glass(RoundedRectangle(28.dp), glass.frost, blur = 8.dp).padding(20.dp),
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(16.dp),
            ) {
                FlowRow(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
                    GlassChipLabel(bookmark.platform.ifBlank { "Web" }, icon = AppIcons.Browser)
                    if (bookmark.authorHandle.isNotBlank()) GlassChipLabel("@${bookmark.authorHandle}", icon = null)
                    if (bookmark.postedAt.isNotBlank()) GlassChipLabel(bookmark.postedAt, icon = null)
                    if (bookmark.isImportant) AppIcons.Icon(AppIcons.Important, "Important", tint = clear.ink, size = 16.dp)
                }

                var titleExpanded by remember { mutableStateOf(false) }
                Text(
                    bookmark.displayTitle.ifBlank { bookmark.title.ifBlank { bookmark.url } },
                    style = MaterialTheme.typography.headlineSmall,
                    color = glass.ink,
                    maxLines = if (titleExpanded) Int.MAX_VALUE else 4,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth().combinedClickable(remember { androidx.compose.foundation.interaction.MutableInteractionSource() }, null) {
                        titleExpanded = !titleExpanded
                    },
                )

                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    val shown = if (showFullUrl) bookmark.url else shortUrl(bookmark.url)
                    Text(
                        shown,
                        color = glass.inkSoft,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = if (showFullUrl) Int.MAX_VALUE else 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f).combinedClickable(
                            remember { androidx.compose.foundation.interaction.MutableInteractionSource() }, null,
                            onClick = { showFullUrl = !showFullUrl },
                            onLongClick = { copyUrl(original = true) },
                        ),
                    )
                    Spacer(Modifier.width(8.dp))
                    Box {
                        GlassCircle(onClick = { copyUrl(false) }, size = 40.dp, label = "Copy link") {
                            AppIcons.Icon(if (copied) AppIcons.Selected else AppIcons.Copy, "Copy link", tint = glass.ink)
                        }
                    }
                }

                Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
                    if (bookmark.likesText.isNotBlank()) {
                        GlassChipLabel(bookmark.likesText, icon = AppIcons.Likes)
                    }
                    if (bookmark.commentsText.isNotBlank()) {
                        GlassChipLabel(bookmark.commentsText, icon = AppIcons.Comments)
                    }
                }

                if (bookmark.fetchStatus != com.pinfold.app.data.model.FetchStatus.OK) {
                    GlassChipButton("Preview incomplete — refetch") { viewModel.refetch(bookmark) }
                }

                Divider(color = glass.line)
                val caption = MetadataCleaner.cleanForDisplay(bookmark.caption)
                if (caption.isNotBlank()) {
                    SelectionContainer {
                        Text(
                            caption,
                            style = MaterialTheme.typography.bodyMedium,
                            color = glass.ink,
                            maxLines = if (expandedCaption) Int.MAX_VALUE else 4,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Box(
                        Modifier.size(40.dp).clickable { expandedCaption = !expandedCaption },
                        contentAlignment = Alignment.Center,
                    ) {
                        AppIcons.Icon(if (expandedCaption) AppIcons.Expand else AppIcons.Expand, "Expand caption", tint = clear.ink)
                    }
                }

                Divider(color = glass.line)
                FlowRow(
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
                ) {
                    AppIcons.Icon(AppIcons.Tags, "Tags", tint = glass.inkSoft, size = 20.dp)
                    item.tags.forEach { tag -> GlassChipLabel(tag.name, icon = null) }
                    GlassChipButton("+") { }
                }

                Divider(color = glass.line)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AppIcons.Icon(AppIcons.Notes, "Notes", tint = glass.inkSoft, size = 20.dp)
                    Spacer(Modifier.width(8.dp))
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it; viewModel.setNotes(bookmark, it) },
                        label = { Text("Notes") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                Box {
                    GlassChipButton(if (bookmark.postedAt.isNotBlank()) "Reminder set" else "Add reminder") { reminderMenu = true }
                    DropdownMenu(expanded = reminderMenu, onDismissRequest = { reminderMenu = false }, offset = DpOffset(0.dp, (-120).dp)) {
                        listOf("In 1 hour" to 3_600_000L, "Tonight" to 86_400_000L, "Tomorrow morning" to 108_000_000L).forEach { (label, delta) ->
                            DropdownMenuItem(text = { Text(label) }, onClick = {
                                viewModel.setReminder(bookmark, System.currentTimeMillis() + delta)
                                reminderMenu = false
                            })
                        }
                    }
                }

                FlowRow(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp)) {
                    Text("Saved ${android.text.format.DateFormat.getDateFormat(context).format(java.util.Date(bookmark.createdAt))}", color = glass.inkSoft, style = MaterialTheme.typography.labelSmall)
                    Text("Opened ${bookmark.openCount} times", color = glass.inkSoft, style = MaterialTheme.typography.labelSmall)
                }
            }
            Spacer(Modifier.height(120.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()))
        }

        Row(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().navigationBarsPadding()
                .padding(horizontal = 14.dp, vertical = 14.dp)
                .glass(
                    Capsule,
                    glass.glass,
                    blur = GlassSettings.style.blur.dp,
                    lensHeight = GlassSettings.style.refractionHeight.dp,
                    lensAmount = GlassSettings.style.refractionAmount.dp,
                ).clip(Capsule).padding(6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            PillButton(AppIcons.Back, "Back", onClick = onBack)
            Box(Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).border(1.dp, clear.line, RoundedCornerShape(12.dp))) {
                Box {
                    GlassCircle(onClick = { openLink(context, bookmark.url, target.packageName) }, onLongClick = { openMenu = true }, size = 48.dp, label = "Open link") {
                        val icon: ImageBitmap? = target.icon
                        if (icon != null) {
                            androidx.compose.foundation.Image(icon, "Open in ${target.label ?: "app"}", modifier = Modifier.size(24.dp))
                        } else {
                            AppIcons.Icon(AppIcons.Open, "Open link", tint = glass.ink)
                        }
                    }
                    DropdownMenu(expanded = openMenu, onDismissRequest = { openMenu = false }, offset = DpOffset((-140).dp, (-150).dp)) {
                        DropdownMenuItem(text = { Text("Open in browser") }, onClick = {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(bookmark.url)))
                            openMenu = false
                        })
                        DropdownMenuItem(text = { Text("Open in app") }, onClick = {
                            openLink(context, bookmark.url, target.packageName)
                            openMenu = false
                        })
                    }
                }
            }
            PillButton(AppIcons.Important, "Important", active = bookmark.isImportant, onClick = { viewModel.toggleImportant(bookmark) })
            PillButton(if (bookmark.postedAt.isNotBlank() || reminderSet(bookmark.id)) AppIcons.ReminderSet else AppIcons.Reminder, "Reminder", onClick = { reminderMenu = true })
            PillButton(AppIcons.Share, "Share", onClick = {
                val share = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, bookmark.url) }
                context.startActivity(Intent.createChooser(share, "Share"))
            })
            Box {
                PillButton(AppIcons.More, "More", onClick = { moreMenu = true })
                DropdownMenu(expanded = moreMenu, onDismissRequest = { moreMenu = false }, offset = DpOffset((-160).dp, (-260).dp)) {
                    DropdownMenuItem(text = { Text("Edit") }, leadingIcon = { AppIcons.Icon(AppIcons.Edit, null, size = 20.dp) }, onClick = { moreMenu = false })
                    DropdownMenuItem(text = { Text("Refetch image") }, leadingIcon = { AppIcons.Icon(AppIcons.Refetch, null, size = 20.dp) }, onClick = { viewModel.refetch(bookmark); moreMenu = false })
                    DropdownMenuItem(text = { Text("Add custom image") }, leadingIcon = { AppIcons.Icon(AppIcons.CustomImage, null, size = 20.dp) }, onClick = { pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)); moreMenu = false })
                    DropdownMenuItem(text = { Text("Move to collection") }, leadingIcon = { AppIcons.Icon(AppIcons.MoveToCollection, null, size = 20.dp) }, onClick = { moreMenu = false })
                    DropdownMenuItem(text = { Text("Archive") }, leadingIcon = { AppIcons.Icon(AppIcons.Archive, null, size = 20.dp) }, onClick = { viewModel.setArchived(bookmark, true); onBack() })
                    DropdownMenuItem(text = { Text("Open in browser") }, leadingIcon = { AppIcons.Icon(AppIcons.Browser, null, size = 20.dp) }, onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(bookmark.url))); moreMenu = false })
                    DropdownMenuItem(text = { Text("Delete") }, leadingIcon = { AppIcons.Icon(AppIcons.Delete, null, size = 20.dp) }, onClick = { viewModel.delete(bookmark); onBack() })
                }
            }
        }
    }
}

@Composable
private fun PillButton(icon: Int, label: String, active: Boolean = false, onClick: () -> Unit, onLongClick: (() -> Unit)? = null) {
    GlassCircle(onClick = onClick, onLongClick = onLongClick, size = 48.dp, label = label) {
        AppIcons.Icon(icon, label, tint = clear.ink)
    }
}

@Composable
private fun GlassChipLabel(label: String, icon: Int?) {
    com.pinfold.app.ui.glass.GlassChip(onClick = {}, height = 34.dp, label = label) {
        if (icon != null) AppIcons.Icon(icon, null, size = 16.dp)
        Text(label, color = clear.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun GlassChipButton(label: String, onClick: () -> Unit) {
    com.pinfold.app.ui.glass.GlassChip(onClick = onClick, height = 34.dp, label = label) {
        Text(label, color = clear.ink, maxLines = 1)
    }
}

private fun reminderSet(bookmarkId: Long): Boolean = false

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
