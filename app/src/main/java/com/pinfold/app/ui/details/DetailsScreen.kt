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
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.pinfold.app.data.model.BookmarkWithTags
import com.pinfold.app.diagnostics.DiagnosticsLog
import com.pinfold.app.network.MetadataCleaner
import com.pinfold.app.ui.components.AppIcons
import com.pinfold.app.ui.components.GlassMenuItem
import com.pinfold.app.ui.components.rememberPhotoInk
import com.pinfold.app.ui.components.rememberPhotoPreview
import com.pinfold.app.ui.background.GeneratedBackground
import com.pinfold.app.ui.glass.Capsule
import com.pinfold.app.ui.glass.GlassChip
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
    val rawImagePath = bookmark.imagePath
    val imagePath = rawImagePath?.takeIf { File(it).exists() }
    val mode = resolveDetailsBackgroundMode(settings.backgroundDetails, imagePath != null)
    val ambientSelection = rememberDetailsAmbient(imagePath, bookmark.id, bookmark.platform, bookmark.siteName)
    val ambient = ambientSelection.current

    LaunchedEffect(bookmark.id, settings.backgroundDetails, mode) {
        logArtwork("Details entered id=${bookmark.id} setting=${settings.backgroundDetails} resolvedMode=${mode.name}")
    }
    LaunchedEffect(bookmark.id, rawImagePath) {
        logArtwork(
            "Image check id=${bookmark.id} pathPresent=${rawImagePath != null} " +
                "exists=${imagePath != null} file=${rawImagePath?.let { File(it).name } ?: "none"}",
        )
    }
    val lastCrossfadeId = remember { mutableStateOf<Long?>(null) }
    LaunchedEffect(bookmark.id) {
        val previous = lastCrossfadeId.value
        val changed = previous != null && previous != bookmark.id
        logArtwork("Crossfade target id=${bookmark.id} previous=$previous changed=$changed")
        ArtworkDiagnostics.update { it.copy(crossfadeChanged = changed) }
        lastCrossfadeId.value = bookmark.id
    }
    LaunchedEffect(bookmark.id, mode, imagePath, ambient) {
        val palette = ambient.palette
        val accents = palette.colors.map { it.argbHex() }
        val decision = ArtworkDecision(
            bookmarkId = bookmark.id,
            setting = settings.backgroundDetails,
            mode = mode,
            imageAvailable = imagePath != null,
            imageFile = imagePath?.let { File(it).name },
            source = ambient.source,
            bucketCount = palette.bucketCount,
            accents = accents,
            anyBlackOrTransparent = palette.colors.any { isEffectivelyBlackOrTransparent(it) },
            composed = mode == DetailsBackgroundMode.ARTWORK,
            crossfadeChanged = false,
        )
        ArtworkDiagnostics.record(decision)
        logArtwork(
            "Decision id=${bookmark.id} mode=${mode.name} image=${imagePath != null} " +
                "source=${ambient.source.name} buckets=${palette.bucketCount} " +
                "accents=${accents.joinToString(",")} blackOrTransparent=${decision.anyBlackOrTransparent}",
        )
    }
    CompositionLocalProvider(LocalClearColors provides DarkClear) {
        DetailsContent(item, viewModel, onBack, modifier, imagePath, ambient, ambientSelection.settled, mode)
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
    ambient: ArtworkAmbient,
    settledAmbient: ArtworkAmbient?,
    mode: DetailsBackgroundMode,
) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsState()
    val bookmark = item.bookmark
    val colors = clear
    val ink = colors.onGlass
    val textInk = Color.White

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
            targetState = settledAmbient,
            animationSpec = if (settings.reduceMotion) snap() else tween(420),
            label = "detailsAmbient",
        ) { targetAmbient ->
            if (targetAmbient != null) {
                DetailsBackground(targetAmbient.key.hashCode().toLong(), mode, targetAmbient, settings, colors)
            }
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
                                                ambient.palette.base.copy(alpha = 0.74f),
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
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            bookmark.siteName.ifBlank { bookmark.platform.ifBlank { "Web" } },
                            style = ambientTextStyle(MaterialTheme.typography.labelLarge, textInk),
                        )
                        if (bookmark.platform.lowercase() in setOf("youtube", "tiktok", "vimeo")) {
                            AppIcons.Icon(AppIcons.Open, "Play video", tint = textInk, size = 16.dp)
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
                Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    AmbientTag(bookmark.platform.ifBlank { "Web" }, textInk)
                    if (bookmark.authorHandle.isNotBlank()) AmbientTag("@${bookmark.authorHandle}", textInk)
                    if (bookmark.postedAt.isNotBlank()) AmbientTag(bookmark.postedAt, textInk)
                    if (bookmark.isImportant) AmbientTag("Important", textInk, AppIcons.Important)
                }

                var titleExpanded by remember { mutableStateOf(false) }
                Text(
                    bookmark.displayTitle.ifBlank { bookmark.title.ifBlank { bookmark.url } },
                    style = ambientTextStyle(MaterialTheme.typography.headlineSmall, textInk),
                    maxLines = if (titleExpanded) Int.MAX_VALUE else 4,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth().pressable(onClick = { titleExpanded = !titleExpanded }),
                )

                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (showFullUrl) bookmark.url else shortUrl(bookmark.url),
                        style = ambientTextStyle(MaterialTheme.typography.bodySmall, textInk.copy(alpha = 0.86f)),
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

                FlowRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    if (bookmark.likesText.isNotBlank()) {
                        AmbientTag("Likes ${bookmark.likesText}", textInk, AppIcons.Likes)
                    }
                    if (bookmark.commentsText.isNotBlank()) {
                        AmbientTag("Comments ${bookmark.commentsText}", textInk, AppIcons.Comments)
                    }
                }

                val caption = MetadataCleaner.cleanForDisplay(bookmark.caption)
                if (caption.isNotBlank()) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                        SelectionContainer(Modifier.weight(1f)) {
                            Text(
                                caption,
                                style = ambientTextStyle(MaterialTheme.typography.bodyMedium, textInk),
                                maxLines = if (expandedCaption) Int.MAX_VALUE else 4,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        GlassCircle(onClick = { expandedCaption = !expandedCaption }, size = 44.dp, label = "Expand caption", ink = ink) {
                            AppIcons.Icon(AppIcons.Expand, null, tint = ink)
                        }
                    }
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    item.tags.forEach { AmbientTag(it.name, textInk.copy(alpha = 0.92f)) }
                    AmbientTag("", textInk.copy(alpha = 0.72f), AppIcons.Add)
                }

                AmbientNotesField(
                    value = notes,
                    onValueChange = { notes = it; viewModel.setNotes(bookmark, it) },
                    ink = textInk,
                )

                Row(
                    Modifier.fillMaxWidth().pressable(onClick = { reminderOpen = true }),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    AppIcons.Icon(AppIcons.Snooze, null, tint = textInk, size = 18.dp)
                    Text(
                        reminderText.ifBlank { "Add reminder" },
                        style = ambientTextStyle(MaterialTheme.typography.bodyMedium, textInk),
                    )
                }

                Text(
                    "Saved ${android.text.format.DateFormat.getDateFormat(context).format(java.util.Date(bookmark.createdAt))} · Opened ${bookmark.openCount} times",
                    style = ambientTextStyle(MaterialTheme.typography.labelSmall, textInk.copy(alpha = 0.78f)),
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
    ambient: ArtworkAmbient,
    settings: com.pinfold.app.data.settings.AppSettings,
    colors: com.pinfold.app.ui.glass.theme.ClearColors,
) {
    when (mode) {
        DetailsBackgroundMode.ARTWORK -> ArtworkAmbientBackground(ambient, seed = bookmarkId)
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

private fun ambientTextStyle(style: TextStyle, color: Color): TextStyle = style.copy(
    color = color,
    shadow = Shadow(color = Color.Black.copy(alpha = 0.72f), offset = Offset(0f, 1f), blurRadius = 10f),
)

@Composable
private fun AmbientTag(label: String, color: Color, icon: Int? = null) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        if (icon != null) AppIcons.Icon(icon, null, tint = color, size = 15.dp)
        if (label.isNotBlank()) {
            Text(
                label,
                style = ambientTextStyle(MaterialTheme.typography.bodySmall, color),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun AmbientNotesField(value: String, onValueChange: (String) -> Unit, ink: Color) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        textStyle = ambientTextStyle(MaterialTheme.typography.bodyMedium, ink),
        cursorBrush = SolidColor(ink),
        decorationBox = { inner ->
            Box(Modifier.fillMaxWidth()) {
                if (value.isEmpty()) {
                    Text("Notes", style = ambientTextStyle(MaterialTheme.typography.bodyMedium, ink.copy(alpha = 0.64f)))
                }
                inner()
            }
        },
    )
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
