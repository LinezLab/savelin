package com.pinfold.app.ui.details

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.unit.DpOffset
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import com.pinfold.app.ui.glass.GlassTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
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
import com.pinfold.app.diagnostics.DiagnosticsLog
import com.pinfold.app.diagnostics.LogLevel
import com.pinfold.app.network.MetadataCleaner
import com.pinfold.app.ui.components.AppIcons
import com.pinfold.app.ui.components.GlassMenuItem
import com.pinfold.app.ui.components.rememberPhotoInk
import com.pinfold.app.ui.components.rememberPhotoPreview
import com.pinfold.app.ui.background.GeneratedBackground
import com.pinfold.app.ui.glass.Capsule
import com.pinfold.app.ui.glass.GlassChip
import com.pinfold.app.ui.glass.LightGlassChip
import com.pinfold.app.ui.glass.GlassCircle
import com.pinfold.app.ui.glass.GlassComponent
import com.pinfold.app.ui.glass.photoThrough
import com.pinfold.app.ui.glass.GlassSettings
import com.pinfold.app.ui.glass.RoundedRectangle
import com.pinfold.app.ui.glass.SavelinGlass
import com.pinfold.app.ui.glass.SavelinGlassDialog
import com.pinfold.app.ui.glass.pressable
import com.pinfold.app.ui.glass.theme.Candy
import com.pinfold.app.ui.glass.theme.DarkClear
import com.pinfold.app.ui.glass.theme.LocalClearColors
import com.pinfold.app.ui.glass.theme.clear
import com.pinfold.app.ui.home.HomeViewModel
import kotlinx.coroutines.delay
import java.io.File

@Composable
fun DetailsScreen(
    item: BookmarkWithTags,
    viewModel: HomeViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val settings by viewModel.settings.collectAsState()
    val bookmark = item.bookmark
    val imagePath = bookmark.imagePath?.takeIf { File(it).exists() }
    val mode = resolveDetailsBackgroundMode(settings.backgroundDetails, imagePath != null)
    LaunchedEffect(bookmark.id, mode, imagePath) {
        DiagnosticsLog.log(
            LogLevel.INFO,
            "Details/background",
            "bookmark=" + bookmark.id + " mode=" + mode.name + " image=" + (imagePath?.let { File(it).name } ?: "none"),
        )
    }
    val ambient = rememberArtworkAmbient(imagePath, bookmark.id)
    if (mode == DetailsBackgroundMode.ARTWORK) {
        CompositionLocalProvider(LocalClearColors provides DarkClear) {
            DetailsContent(item, viewModel, onBack, modifier, imagePath, ambient, mode)
        }
    } else {
        DetailsContent(item, viewModel, onBack, modifier, imagePath, ambient, mode)
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
private fun DetailsContent(
    item: BookmarkWithTags,
    viewModel: HomeViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    imagePath: String?,
    ambient: ArtworkAmbient?,
    mode: DetailsBackgroundMode,
) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsState()
    val bookmark = item.bookmark
    val colors = clear
    val ink = colors.onGlass

    var expandedCaption by remember { mutableStateOf(false) }
    var cameraMenu by remember { mutableStateOf(false) }
    var moreMenu by remember { mutableStateOf(false) }
    var reminderOpen by remember { mutableStateOf(false) }
    var showFullUrl by remember { mutableStateOf(false) }
    var copied by remember { mutableStateOf(false) }
    var notes by remember(bookmark.id) { mutableStateOf(bookmark.notes) }
    var reminderText by remember(bookmark.id) { mutableStateOf("") }
    val heroPreview = rememberPhotoPreview(imagePath, bookmark.blurhash)
    val heroInk = rememberPhotoInk(heroPreview) ?: ink
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

    BackHandler(enabled = cameraMenu || moreMenu || reminderOpen) {
        DiagnosticsLog.uiAction("Details", "Back")
        when {
            cameraMenu -> cameraMenu = false
            moreMenu -> moreMenu = false
            reminderOpen -> reminderOpen = false
        }
    }

    fun copyUrl(original: Boolean) {
        val value = if (original) bookmark.url else com.pinfold.app.network.UrlNormalizer.normalize(bookmark.url)
        val manager = context.getSystemService(android.content.ClipboardManager::class.java)
        manager?.setPrimaryClip(android.content.ClipData.newPlainText("link", value))
        DiagnosticsLog.uiAction("Details", "Copy link")
        copied = true
    }

    Box(modifier.fillMaxSize()) {
        Crossfade(
            targetState = bookmark.id,
            animationSpec = if (settings.reduceMotion) snap() else tween(360),
            label = "detailsBackground",
        ) { id ->
            DetailsBackground(id, mode, imagePath, ambient, settings, colors)
        }

        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .windowInsetsPadding(WindowInsets.statusBars.union(WindowInsets.displayCutout))
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val heroHeight = minOf(maxHeight * 0.55f, 420.dp)
                val heroMaxWidth = maxWidth
                val heroFrame = Modifier.fillMaxWidth().height(heroHeight)
                Box(
                    if (mode == DetailsBackgroundMode.ARTWORK) heroFrame
                    else heroFrame.clip(RoundedRectangle(28.dp)).then(SavelinGlass.modifier(RoundedRectangle(28.dp))),
                ) {
                    if (imagePath != null) {
                        val ratio = when {
                            bookmark.imageWidth > 0 && bookmark.imageHeight > 0 ->
                                (bookmark.imageWidth.toFloat() / bookmark.imageHeight.toFloat()).coerceIn(0.35f, 3f)
                            else -> 0.8f
                        }
                        val imageHeight = minOf(heroHeight, heroMaxWidth / ratio)
                        val imageWidth = minOf(heroMaxWidth, imageHeight * ratio)
                        AsyncImage(
                            model = ImageRequest.Builder(context).data(File(imagePath)).build(),
                            contentDescription = bookmark.displayTitle,
                            contentScale = ContentScale.Fit,
                            modifier = if (mode == DetailsBackgroundMode.ARTWORK) Modifier.size(imageWidth, imageHeight).align(Alignment.Center)
                            else Modifier.fillMaxSize(),
                        )
                        if (mode == DetailsBackgroundMode.ARTWORK) {
                            Box(
                                Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(56.dp)
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(
                                                Color.Transparent,
                                                (ambient?.palette?.base ?: DarkClear.wall.last()).copy(alpha = 0.92f),
                                            ),
                                        ),
                                    ),
                            )
                        }
                    } else {
                        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(colors.wall)))
                    }
                    Row(
                        Modifier.align(Alignment.TopStart).padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        LightGlassChip(
                            label = bookmark.siteName.ifBlank { bookmark.platform.ifBlank { "Web" } },
                            shape = RoundedRectangle(10.dp),
                            photo = heroPreview,
                            photoInk = heroInk,
                        )
                        if (bookmark.platform.lowercase() in setOf("youtube", "tiktok", "vimeo")) {
                            LightGlassChip(
                                shape = CircleShape,
                                photo = heroPreview,
                                photoInk = heroInk,
                                contentDescription = "Play video",
                            ) {
                                AppIcons.Icon(AppIcons.Open, "Play video", tint = heroInk, size = 14.dp)
                            }
                        }
                    }
                    Box(
                        Modifier.align(Alignment.BottomEnd).padding(12.dp).size(44.dp)
                            .then(
                                if (heroPreview != null) Modifier.photoThrough(heroPreview, CircleShape, clear.dark)
                                else SavelinGlass.modifierFor(GlassComponent.CARDS, CircleShape)
                            )
                            .border(1.dp, clear.rim.copy(alpha = 0.7f), CircleShape)
                            .pressable(
                                onClick = { pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                                onLongClick = { cameraMenu = true },
                                pressedScale = 0.86f,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        AppIcons.Icon(AppIcons.Camera, "Image options", tint = heroInk)
                    }
                    if (cameraMenu) {
                        com.pinfold.app.ui.components.GlassMenuOverlay(
                            items = listOf(
                                GlassMenuItem(AppIcons.Refetch, "Refetch image", onClick = { viewModel.refetch(bookmark) }),
                                GlassMenuItem(AppIcons.CustomImage, "Choose picture", onClick = {
                                    pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                }),
                            ) + if (bookmark.customImage) listOf(
                                GlassMenuItem(AppIcons.NoPreview, "Remove custom image", onClick = { viewModel.removeImage(bookmark) }),
                            ) else emptyList(),
                            onDismiss = { cameraMenu = false },
                        )
                    }
                }
            }

            Column(
                Modifier.fillMaxWidth()
                    .then(SavelinGlass.modifier(RoundedRectangle(28.dp)))
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
                    if (bookmark.isImportant) {
                        LightGlassChip(shape = CircleShape, contentDescription = "Important") {
                            AppIcons.Icon(AppIcons.Important, null, tint = ink, size = 16.dp)
                        }
                    }
                }

                var titleExpanded by remember { mutableStateOf(false) }
                Text(
                    bookmark.displayTitle.ifBlank { bookmark.title.ifBlank { bookmark.url } },
                    style = MaterialTheme.typography.headlineSmall,
                    color = ink,
                    maxLines = if (titleExpanded) Int.MAX_VALUE else 4,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth().pressable(onClick = { titleExpanded = !titleExpanded }),
                )

                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (showFullUrl) bookmark.url else shortUrl(bookmark.url),
                        color = ink,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = if (showFullUrl) Int.MAX_VALUE else 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth(0.8f).pressable(
                            onClick = { showFullUrl = !showFullUrl },
                            onLongClick = { copyUrl(original = true) },
                        ),
                    )
                    Spacer(Modifier.size(8.dp))
                    GlassCircle(onClick = { copyUrl(false) }, size = 44.dp, label = "Copy link", ink = ink) {
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
                    GlassCircle(onClick = { expandedCaption = !expandedCaption }, size = 44.dp, label = "Expand caption", ink = ink) {
                        AppIcons.Icon(AppIcons.Expand, null, tint = ink)
                    }
                }

                Divider(color = colors.line)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    item.tags.forEach { GlassTag(it.name) }
                    GlassTag("", icon = AppIcons.Add)
                }

                Divider(color = colors.line)
                GlassTextField(
                    value = notes,
                    onValueChange = { notes = it; viewModel.setNotes(bookmark, it) },
                    placeholder = "Notes",
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

        BoxWithConstraints(
            Modifier.zIndex(10f).align(Alignment.BottomCenter).fillMaxWidth()
                .navigationBarsPadding().padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
                .height(56.dp),
        ) {
            Box(Modifier.align(Alignment.CenterStart)) {
                GlassCircle(onClick = onBack, onLongClick = onBack, size = 56.dp, label = "Back", ink = ink) {
                    AppIcons.Icon(AppIcons.Back, "Back", tint = ink)
                }
            }
            Box(
                Modifier.align(Alignment.Center)
                    .width((maxWidth - 248.dp).coerceAtLeast(160.dp))
                    .height(56.dp)
                    .then(SavelinGlass.modifier(Capsule))
                    .clip(Capsule),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "Open in $openName",
                    color = ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 24.dp)
                        .pressable(onClick = { openLink(context, bookmark.url, target.packageName, settings.openLinksIn == "BROWSER") }),
                )
            }
            Box(Modifier.align(Alignment.CenterEnd)) {
                GlassCircle(onClick = { moreMenu = true }, onLongClick = { moreMenu = true }, size = 56.dp, label = "More", ink = ink) {
                    AppIcons.Icon(AppIcons.More, "More", tint = ink)
                }
                if (moreMenu) {
                    com.pinfold.app.ui.components.GlassMenuOverlay(
                        items = listOf(
                            GlassMenuItem(AppIcons.Open, "Open link", onClick = {
                                openLink(context, bookmark.url, target.packageName, settings.openLinksIn == "BROWSER")
                            }),
                            GlassMenuItem(AppIcons.Copy, "Copy link", onClick = { copyUrl(original = true) }),
                            GlassMenuItem(
                                AppIcons.Important,
                                if (bookmark.isImportant) "Remove important" else "Mark important",
                                onClick = { viewModel.toggleImportant(bookmark) },
                            ),
                            GlassMenuItem(
                                AppIcons.Reminder,
                                if (reminderText.isBlank()) "Set reminder" else "Edit reminder",
                                onClick = { reminderOpen = true },
                            ),
                            GlassMenuItem(AppIcons.Share, "Share", onClick = {
                                val share = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, bookmark.url)
                                }
                                context.startActivity(Intent.createChooser(share, "Share"))
                            }),
                            GlassMenuItem(AppIcons.Delete, "Delete", destructive = true, onClick = {
                                viewModel.delete(bookmark)
                                onBack()
                            }),
                        ),
                        onDismiss = { moreMenu = false },
                    )
                }
            }
        }

        if (reminderOpen) {
            SavelinGlassDialog(
                onDismiss = { reminderOpen = false },
                title = "Set reminder",
                confirm = "Set reminder",
                onConfirm = {
                    if (reminderText.isNotBlank()) {
                        viewModel.setReminder(bookmark, reminderTime(reminderText))
                        reminderOpen = false
                    }
                },
                confirmEnabled = reminderText.isNotBlank(),
                dismiss = "Cancel",
            ) {
                Text(bookmark.displayTitle.ifBlank { bookmark.title }, color = ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                GlassTextField(value = reminderText, onValueChange = { reminderText = it }, placeholder = "When")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("In 1 hour", "Tonight", "Tomorrow morning", "Next week").forEach { label ->
                        GlassChip(onClick = { reminderText = label }, height = 36.dp, label = label) {
                            Text(label, color = ink, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                if (reminderText.isNotBlank()) {
                    GlassChip(onClick = { viewModel.clearReminder(bookmark); reminderText = ""; reminderOpen = false }, height = 36.dp, label = "Remove reminder") {
                        Text("Remove reminder", color = ink, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailsBackground(
    bookmarkId: Long,
    mode: DetailsBackgroundMode,
    imagePath: String?,
    ambient: ArtworkAmbient?,
    settings: com.pinfold.app.data.settings.AppSettings,
    colors: com.pinfold.app.ui.glass.theme.ClearColors,
) {
    when (mode) {
        DetailsBackgroundMode.ARTWORK -> {
            when {
                ambient != null -> ArtworkAmbientBackground(ambient, seed = bookmarkId)
                imagePath != null -> {
                    val context = LocalContext.current
                    Box(Modifier.fillMaxSize().background(DarkClear.wall.last()))
                    AsyncImage(
                        model = ImageRequest.Builder(context).data(File(imagePath)).build(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().alpha(0.6f),
                    )
                }
                else -> Box(Modifier.fillMaxSize().background(DarkClear.wall.last()))
            }
        }
        DetailsBackgroundMode.GENERATED -> GeneratedBackground(
            Modifier.fillMaxSize(), loopMs = settings.backgroundLoopSeconds * 1000,
            animate = settings.backgroundAnimate,
            style = settings.backgroundStyle, hue = settings.backgroundHue, dim = settings.backgroundDim,
            shape = settings.backgroundShape, source = settings.backgroundSource, fps = settings.backgroundFps,
        )
        DetailsBackgroundMode.THEME -> {
            val wall = GlassSettings.customWallpaper
            if (wall != null) Image(wall, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            else Box(Modifier.fillMaxSize().background(colors.wall.last()))
        }
    }
}

@Composable
private fun GlassTag(label: String, icon: Int? = null) {
    GlassChip(onClick = {}, height = 32.dp, contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp), label = label, ink = clear.onGlass) {
        if (icon != null) AppIcons.Icon(icon, null, tint = clear.onGlass, size = 16.dp)
        Text(label, color = clear.onGlass, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

private fun openLink(context: android.content.Context, url: String, pkg: String?, browser: Boolean) {
    val intent = if (!browser && pkg != null) {
        context.packageManager.getLaunchIntentForPackage(pkg)?.apply { data = Uri.parse(url) }
            ?: Intent(Intent.ACTION_VIEW, Uri.parse(url))
    } else Intent(Intent.ACTION_VIEW, Uri.parse(url))
    runCatching { context.startActivity(intent) }
}

/** Turns a reminder chip's label into a real trigger time, in the device's own time zone. */
private fun reminderTime(label: String): Long {
    val now = java.util.Calendar.getInstance()
    return when (label) {
        "Tonight" -> {
            val c = java.util.Calendar.getInstance()
            c.set(java.util.Calendar.HOUR_OF_DAY, 20)
            c.set(java.util.Calendar.MINUTE, 0)
            c.set(java.util.Calendar.SECOND, 0)
            if (c.timeInMillis <= now.timeInMillis) c.add(java.util.Calendar.DAY_OF_MONTH, 1)
            c.timeInMillis
        }
        "Tomorrow morning" -> {
            val c = java.util.Calendar.getInstance()
            c.add(java.util.Calendar.DAY_OF_MONTH, 1)
            c.set(java.util.Calendar.HOUR_OF_DAY, 9)
            c.set(java.util.Calendar.MINUTE, 0)
            c.set(java.util.Calendar.SECOND, 0)
            c.timeInMillis
        }
        "Next week" -> now.timeInMillis + 7L * 24L * 60L * 60L * 1000L
        else -> now.timeInMillis + 60L * 60L * 1000L
    }
}

private fun shortUrl(url: String): String {
    val withoutScheme = url.substringAfter("://").removePrefix("www.").removeSuffix("/")
    return if (withoutScheme.length <= 44) withoutScheme else withoutScheme.take(22) + "…" + withoutScheme.takeLast(18)
}
