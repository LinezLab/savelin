package com.pinfold.app.ui.detail

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.pinfold.app.data.model.Bookmark
import com.pinfold.app.data.model.BookmarkWithTags
import com.pinfold.app.diagnostics.DiagnosticsLog
import com.pinfold.app.ui.components.AppIcons
import com.pinfold.app.ui.components.DiagnosticsToast
import com.pinfold.app.ui.components.GlassMenuItem
import com.pinfold.app.ui.components.GlassMenuOverlay
import com.pinfold.app.ui.glass.GlassCircle
import com.pinfold.app.ui.glass.GlassTextField
import com.pinfold.app.ui.glass.LightGlassChip
import com.pinfold.app.ui.glass.SavelinGlassDialog
import com.pinfold.app.ui.glass.theme.clear
import com.pinfold.app.ui.home.HomeViewModel
import java.io.File

internal const val HERO_BOTTOM_DISSOLVE_FRACTION = 0.42f
internal const val SWIPE_MIN_THRESHOLD_PX = 96f
internal const val SWIPE_HEIGHT_FRACTION = 0.15f

internal fun smootherStep(t: Float): Float {
    val x = t.coerceIn(0f, 1f)
    return x * x * x * (x * (x * 6f - 15f) + 10f)
}

internal fun swipeThresholdPx(screenHeightPx: Float): Float =
    maxOf(SWIPE_MIN_THRESHOLD_PX, screenHeightPx * SWIPE_HEIGHT_FRACTION)

internal fun navigationDelta(accumulatedDragPx: Float, thresholdPx: Float): Int = when {
    accumulatedDragPx <= -thresholdPx -> 1
    accumulatedDragPx >= thresholdPx -> -1
    else -> 0
}

internal fun titleFor(bookmark: Bookmark): String = bookmark.displayTitle.ifBlank { bookmark.title }

internal fun urlHost(url: String): String =
    url.substringAfter("://", url).substringBefore('/').substringBefore('?').substringBefore('#').trim()

internal fun fallbackMetadataLines(bookmark: Bookmark): List<String> {
    val lines = mutableListOf<String>()
    if (bookmark.siteName.isNotBlank()) lines.add(bookmark.siteName)
    if (bookmark.platform.isNotBlank()) lines.add(bookmark.platform)
    if (bookmark.author.isNotBlank()) lines.add(bookmark.author)
    if (bookmark.authorHandle.isNotBlank()) lines.add("@${bookmark.authorHandle}")
    if (bookmark.postedAt.isNotBlank()) lines.add(bookmark.postedAt)
    val host = urlHost(bookmark.url)
    if (host.isNotBlank()) lines.add(host)
    return lines
}

@Composable
fun DetailScreen(
    item: BookmarkWithTags,
    reduceMotion: Boolean,
    onBack: () -> Unit,
    viewModel: HomeViewModel,
    onNavigate: (Int) -> Unit = {},
    detailAmbientBackground: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val bookmark = item.bookmark
    val path = remember(bookmark.id, bookmark.imagePath) {
        bookmark.imagePath?.takeIf { it.isNotBlank() }
            ?.let { File(it).takeIf { file -> file.isFile }?.absolutePath }
    }
    val selection = rememberAmbienceSelection(bookmark.id, path)
    val ambience = selection.settled ?: selection.current
    val aspect = remember(bookmark.imageWidth, bookmark.imageHeight) {
        if (bookmark.imageWidth > 0 && bookmark.imageHeight > 0) {
            (bookmark.imageWidth.toFloat() / bookmark.imageHeight.toFloat()).coerceIn(0.42f, 2.6f)
        } else 1f
    }
    val context = LocalContext.current
    val themeFloor = MaterialTheme.colorScheme.background
    val reminderIds by viewModel.reminderIds.collectAsState()
    val hasReminder = bookmark.id in reminderIds

    var menuOpen by remember { mutableStateOf(false) }
    var editOpen by remember { mutableStateOf(false) }
    var copyToast by remember { mutableStateOf(false) }
    var editTitle by remember { mutableStateOf("") }
    var editDescription by remember { mutableStateOf("") }
    var editNotes by remember { mutableStateOf("") }
    var editTags by remember { mutableStateOf("") }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) viewModel.attachImage(bookmark, uri)
    }

    val screenHeightPx = LocalWindowInfo.current.containerSize.height.toFloat()
    val swipeThreshold = remember(screenHeightPx) { swipeThresholdPx(screenHeightPx) }
    val dragAccumulated = remember { mutableStateOf(0f) }
    val dragConsumed = remember { mutableStateOf(false) }
    val navigate by rememberUpdatedState(onNavigate)

    LaunchedEffect(Unit) { DiagnosticsLog.uiAction("Detail", "Open") }

    Box(
        modifier
            .fillMaxSize()
            .background(themeFloor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {},
            )
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragEnd = {
                        dragAccumulated.value = 0f
                        dragConsumed.value = false
                    },
                    onDragCancel = {
                        dragAccumulated.value = 0f
                        dragConsumed.value = false
                    },
                ) { change, dragAmount ->
                    change.consume()
                    if (!dragConsumed.value) {
                        dragAccumulated.value += dragAmount
                        val delta = navigationDelta(dragAccumulated.value, swipeThreshold)
                        if (delta != 0) {
                            dragConsumed.value = true
                            navigate(delta)
                        }
                    }
                }
            },
    ) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val naturalHeight = maxWidth / aspect
            val heroHeight = minOf(naturalHeight, maxHeight * 0.72f)
            val heroScale = ContentScale.FillWidth
            val seamPx = if (detailAmbientBackground && path != null) {
                with(LocalDensity.current) { heroHeight.toPx() }
            } else 0f
            if (detailAmbientBackground) {
                AmbientBackdrop(ambience, seamPx, reduceMotion, themeFloor)
                LaunchedEffect(ambience.source, seamPx) {
                    ArtworkDiagnostics.record(
                        ArtworkSnapshot(source = ambience.source, seamPx = seamPx.toInt()),
                    )
                }
            }
            SharpHero(path, themeFloor, heroHeight, heroScale, context)
        }

        Box(
            Modifier.align(Alignment.TopStart)
                .windowInsetsPadding(WindowInsets.statusBars.union(WindowInsets.displayCutout))
                .padding(16.dp)
                .size(44.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.24f))
                .clickable(onClick = onBack, onClickLabel = "Back"),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.size(22.dp)) { drawBackChevron() }
        }

        DetailMetadata(
            item = item,
            modifier = Modifier.align(Alignment.BottomStart)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = 20.dp, end = 84.dp, bottom = 88.dp),
        )

        Row(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                DetailCircle(onClick = onBack, label = "Back", icon = AppIcons.Back)
            }
            DetailCircle(
                onClick = {
                    DiagnosticsLog.uiAction("Detail", "Open link")
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(bookmark.url)))
                },
                onLongClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                    clipboard?.setPrimaryClip(ClipData.newPlainText("Savelin link", bookmark.url))
                    copyToast = true
                },
                label = "Open link, long press to copy",
                icon = AppIcons.Open,
                size = 54.dp,
            )
            Box(Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
                DetailCircle(onClick = { menuOpen = true }, label = "More options", icon = AppIcons.More)
            }
        }

        Column(
            Modifier.align(Alignment.BottomEnd).navigationBarsPadding()
                .padding(end = 20.dp)
                .padding(bottom = 86.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            DetailCircle(
                onClick = {
                    val send = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, bookmark.url)
                    }
                    context.startActivity(Intent.createChooser(send, "Share"))
                },
                label = "Share",
                icon = AppIcons.Share,
            )
            DetailCircle(
                onClick = { viewModel.toggleImportant(bookmark) },
                label = if (bookmark.isImportant) "Remove important" else "Mark important",
                icon = if (bookmark.isImportant) AppIcons.ReminderSet else AppIcons.Important,
            )
            DetailCircle(
                onClick = {
                    if (hasReminder) viewModel.clearReminder(bookmark)
                    else viewModel.setReminder(bookmark, System.currentTimeMillis() + 3_600_000L)
                },
                label = if (hasReminder) "Clear reminder" else "Set reminder",
                icon = if (hasReminder) AppIcons.ReminderMuted else AppIcons.Reminder,
            )
        }

        if (menuOpen) {
            GlassMenuOverlay(
                title = titleFor(bookmark),
                onDismiss = { menuOpen = false },
                items = listOf(
                    GlassMenuItem(AppIcons.Edit, "Edit", onClick = {
                        editTitle = bookmark.title
                        editDescription = bookmark.description
                        editNotes = bookmark.notes
                        editTags = item.tags.joinToString(", ") { it.name }
                        editOpen = true
                    }),
                    GlassMenuItem(AppIcons.Refetch, "Refresh", onClick = { viewModel.refetch(bookmark) }),
                    GlassMenuItem(AppIcons.CustomImage, "Add custom image", onClick = { imagePicker.launch("image/*") }),
                    GlassMenuItem(AppIcons.Delete, "Delete", destructive = true, onClick = {
                        viewModel.delete(bookmark)
                        onBack()
                    }),
                ),
            )
        }

        if (editOpen) {
            SavelinGlassDialog(
                onDismiss = { editOpen = false },
                title = "Edit bookmark",
                confirm = "Save",
                onConfirm = {
                    viewModel.editBookmark(
                        bookmark,
                        editTitle.trim(),
                        editDescription.trim(),
                        editNotes,
                        editTags.split(',').map { it.trim() }.filter { it.isNotBlank() },
                    )
                    editOpen = false
                },
                dismiss = "Cancel",
            ) {
                GlassTextField(value = editTitle, onValueChange = { editTitle = it }, placeholder = "Title")
                GlassTextField(value = editDescription, onValueChange = { editDescription = it }, placeholder = "Description", singleLine = false)
                GlassTextField(value = editNotes, onValueChange = { editNotes = it }, placeholder = "Notes", singleLine = false)
                GlassTextField(value = editTags, onValueChange = { editTags = it }, placeholder = "Tags, comma separated")
            }
        }

        if (copyToast) DiagnosticsToast("Link copied") { copyToast = false }
    }
}

@Composable
private fun DetailMetadata(item: BookmarkWithTags, modifier: Modifier = Modifier) {
    val bookmark = item.bookmark
    val title = titleFor(bookmark)
    val description = bookmark.description.trim()
    val tags = item.tags
    val hasPrimary = title.isNotBlank() || description.isNotBlank() || tags.isNotEmpty()
    val fallback = if (hasPrimary) emptyList() else fallbackMetadataLines(bookmark)
    if (!hasPrimary && fallback.isEmpty()) return

    val ink = clear
    val wash = ink.glass.copy(alpha = if (ink.dark) 0.42f else 0.72f)
    Column(
        modifier
            .clip(RoundedCornerShape(22.dp))
            .background(Brush.verticalGradient(listOf(wash, Color.Transparent)))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (title.isNotBlank()) {
            Text(
                title,
                color = ink.onGlass,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (description.isNotBlank()) {
            Text(
                description,
                color = ink.onGlassSoft,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (tags.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                tags.forEach { tag -> LightGlassChip(label = tag.name) }
            }
        }
        if (!hasPrimary) {
            fallback.forEach { line ->
                Text(
                    line,
                    color = ink.onGlassSoft,
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun DetailCircle(
    onClick: () -> Unit,
    label: String,
    icon: Int,
    modifier: Modifier = Modifier,
    size: Dp = 46.dp,
    onLongClick: (() -> Unit)? = null,
) {
    val ink = clear.onGlass
    GlassCircle(
        onClick = onClick,
        modifier = modifier,
        size = size,
        label = label,
        onLongClick = onLongClick,
        ink = ink,
    ) {
        AppIcons.Icon(icon, label, tint = ink, size = 20.dp)
    }
}

@Composable
private fun BoxScope.SharpHero(
    path: String?,
    floorColor: Color,
    heroHeight: Dp,
    heroScale: ContentScale,
    context: android.content.Context,
) {
    Box(
        Modifier.align(Alignment.TopCenter)
            .fillMaxWidth()
            .height(heroHeight),
    ) {
        if (path != null) {
            AsyncImage(
                model = ImageRequest.Builder(context).data(File(path)).build(),
                contentDescription = null,
                contentScale = heroScale,
                modifier = Modifier.fillMaxSize()
                    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                    .drawWithContent {
                        drawContent()
                        drawRect(
                            brush = Brush.verticalGradient(
                                colorStops = Array(17) { i ->
                                    val t = i / 16f
                                    t to Color.Black.copy(alpha = 1f - smootherStep(t))
                                },
                                startY = size.height * (1f - HERO_BOTTOM_DISSOLVE_FRACTION),
                                endY = size.height,
                            ),
                            blendMode = BlendMode.DstIn,
                        )
                    },
            )
        } else {
            Box(Modifier.fillMaxSize().background(floorColor))
        }
    }
}

private fun DrawScope.drawBackChevron() {
    val chevron = Path().apply {
        moveTo(size.width * 0.18f, size.height * 0.36f)
        lineTo(size.width * 0.5f, size.height * 0.66f)
        lineTo(size.width * 0.82f, size.height * 0.36f)
    }
    drawPath(
        path = chevron,
        color = Color.White,
        style = Stroke(width = 2.4.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
    )
}
