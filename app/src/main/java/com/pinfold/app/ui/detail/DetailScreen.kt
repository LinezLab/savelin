package com.pinfold.app.ui.detail

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.pinfold.app.data.model.Bookmark
import com.pinfold.app.data.model.BookmarkWithTags
import com.pinfold.app.data.model.Tag
import com.pinfold.app.diagnostics.DiagnosticsLog
import com.pinfold.app.ui.components.AppIcons
import com.pinfold.app.ui.components.DiagnosticsToast
import com.pinfold.app.ui.components.GlassMenuItem
import com.pinfold.app.ui.components.GlassMenuOverlay
import com.pinfold.app.ui.glass.Capsule
import com.pinfold.app.ui.glass.Feel
import com.pinfold.app.ui.glass.GlassComponent
import com.pinfold.app.ui.glass.GlassTextField
import com.pinfold.app.ui.glass.LightGlassChip
import com.pinfold.app.ui.glass.LocalHaptics
import com.pinfold.app.ui.glass.SavelinGlass
import com.pinfold.app.ui.glass.SavelinGlassDialog
import com.pinfold.app.ui.glass.theme.ClearType
import com.pinfold.app.ui.glass.theme.clear
import com.pinfold.app.ui.home.HomeViewModel
import java.io.File
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

internal const val HERO_BOTTOM_DISSOLVE_FRACTION = 0.42f
internal const val SWIPE_MIN_THRESHOLD_PX = 96f
internal const val SWIPE_HEIGHT_FRACTION = 0.15f
internal const val HASHTAG_LIMIT = 12
internal const val COLLAPSED_TITLE_LINES = 2
internal const val COLLAPSED_DESCRIPTION_LINES = 3

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

internal fun metadataTopOffset(heroHeight: Dp, gap: Dp): Dp = heroHeight + gap

internal fun collapsedMaxLines(expanded: Boolean): Int =
    if (expanded) Int.MAX_VALUE else COLLAPSED_DESCRIPTION_LINES

internal fun hashtagPills(bookmark: Bookmark, tags: List<Tag>): List<String> {
    val unique = LinkedHashMap<String, String>()
    fun add(raw: String) {
        val bare = raw.trim().trimStart('#').trim()
        if (bare.isEmpty()) return
        val label = "#$bare"
        unique.putIfAbsent(label.lowercase(Locale.getDefault()), label)
    }
    bookmark.hashtags.split(',').forEach(::add)
    tags.forEach { add(it.name) }
    return unique.values.take(HASHTAG_LIMIT)
}

internal fun formattedCreatedAt(epochMillis: Long): String = runCatching {
    val pattern = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault())
    LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), ZoneId.systemDefault()).format(pattern)
}.getOrDefault("")

internal data class DetailStat(val icon: Int, val value: String)

internal fun detailStats(bookmark: Bookmark): List<DetailStat> {
    val stats = mutableListOf<DetailStat>()
    val posted = bookmark.postedAt.trim().ifBlank { formattedCreatedAt(bookmark.createdAt) }
    if (posted.isNotBlank()) stats.add(DetailStat(AppIcons.DateTime, posted))
    val likes = bookmark.likesText.trim()
    if (likes.isNotBlank()) stats.add(DetailStat(AppIcons.Likes, likes))
    val comments = bookmark.commentsText.trim()
    if (comments.isNotBlank()) stats.add(DetailStat(AppIcons.Comments, comments))
    return stats
}

@OptIn(ExperimentalFoundationApi::class)
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
    var titleExpanded by remember { mutableStateOf(false) }
    var descriptionExpanded by remember { mutableStateOf(false) }
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
            }
            .pointerInput(Unit) {
                detectTapGestures(onDoubleTap = { viewModel.toggleImportant(bookmark) })
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
            Column(
                Modifier.fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(top = metadataTopOffset(heroHeight, 12.dp))
                    .padding(bottom = 96.dp),
            ) {
                DetailMetadata(
                    item = item,
                    titleExpanded = titleExpanded,
                    onToggleTitle = { titleExpanded = !titleExpanded },
                    descriptionExpanded = descriptionExpanded,
                    onToggleDescription = { descriptionExpanded = !descriptionExpanded },
                    onShare = {
                        val send = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, bookmark.url)
                        }
                        context.startActivity(Intent.createChooser(send, "Share"))
                    },
                )
            }
        }

        val ink = clear.onGlass
        Box(
            Modifier.align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 16.dp)
                .widthIn(min = 280.dp)
                .then(SavelinGlass.modifierFor(GlassComponent.NAVIGATION, Capsule))
                .clip(Capsule),
            contentAlignment = Alignment.Center,
        ) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                BareGlyph(
                    icon = AppIcons.Back,
                    label = "Back",
                    onClick = onBack,
                )
                Text(
                    text = "Open",
                    color = ink,
                    style = ClearType.Title.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    modifier = Modifier.combinedClickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        role = Role.Button,
                        onClick = {
                            DiagnosticsLog.uiAction("Detail", "Open link")
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(bookmark.url)))
                        },
                        onLongClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                            clipboard?.setPrimaryClip(ClipData.newPlainText("Savelin link", bookmark.url))
                            copyToast = true
                        },
                    ).semantics { contentDescription = "Open link, long press to copy" },
                )
                BareGlyph(
                    icon = AppIcons.More,
                    label = "More options",
                    onClick = { menuOpen = true },
                )
            }
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
                    GlassMenuItem(
                        if (hasReminder) AppIcons.ReminderMuted else AppIcons.Reminder,
                        if (hasReminder) "Clear reminder" else "Set reminder",
                        onClick = {
                            if (hasReminder) viewModel.clearReminder(bookmark)
                            else viewModel.setReminder(bookmark, System.currentTimeMillis() + 3_600_000L)
                        },
                    ),
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
private fun DetailMetadata(
    item: BookmarkWithTags,
    titleExpanded: Boolean,
    onToggleTitle: () -> Unit,
    descriptionExpanded: Boolean,
    onToggleDescription: () -> Unit,
    onShare: () -> Unit,
) {
    val bookmark = item.bookmark
    val title = titleFor(bookmark)
    val description = bookmark.description.trim()
    val pills = hashtagPills(bookmark, item.tags)
    val stats = detailStats(bookmark)
    val hasPrimary = title.isNotBlank() || description.isNotBlank() || pills.isNotEmpty()
    val fallback = if (hasPrimary) emptyList() else fallbackMetadataLines(bookmark)
    if (!hasPrimary && fallback.isEmpty()) return

    val ink = clear.onGlass
    val soft = clear.onGlassSoft
    if (title.isNotBlank()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BareGlyph(
                icon = AppIcons.Share,
                label = "Share",
                onClick = onShare,
                size = 32.dp,
                iconSize = 17.dp,
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = title,
                color = ink,
                style = ClearType.Headline.copy(fontWeight = FontWeight.SemiBold),
                maxLines = if (titleExpanded) Int.MAX_VALUE else COLLAPSED_TITLE_LINES,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onToggleTitle,
                ),
            )
        }
    }
    if (stats.isNotEmpty()) {
        Row(
            Modifier.padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            stats.forEach { stat ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AppIcons.Icon(stat.icon, null, tint = soft, size = 14.dp)
                    Spacer(Modifier.width(4.dp))
                    Text(
                        stat.value,
                        color = soft,
                        style = ClearType.Caption,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
    if (description.isNotBlank()) {
        Text(
            text = description,
            color = soft,
            style = ClearType.Body,
            maxLines = collapsedMaxLines(descriptionExpanded),
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 10.dp).clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onToggleDescription,
            ),
        )
    }
    if (pills.isNotEmpty()) {
        FlowRow(
            Modifier.padding(top = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            pills.forEach { pill -> LightGlassChip(label = pill) }
        }
    }
    if (!hasPrimary) {
        Column(
            Modifier.padding(top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            fallback.forEach { line ->
                Text(
                    line,
                    color = soft,
                    style = ClearType.Caption,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BareGlyph(
    icon: Int,
    label: String,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    size: Dp = 44.dp,
    iconSize: Dp = 20.dp,
) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val haptics = LocalHaptics.current
    val dark = clear.dark
    Box(
        Modifier.size(size)
            .drawBehind {
                drawRoundRect(
                    color = Color.White.copy(alpha = if (pressed) {
                        if (dark) 0.20f else 0.34f
                    } else if (dark) 0.08f else 0.14f),
                    cornerRadius = CornerRadius(minOf(this.size.width, this.size.height) / 2),
                )
            }
            .graphicsLayer { val s = if (pressed) 0.9f else 1f; scaleX = s; scaleY = s }
            .combinedClickable(
                interactionSource = source,
                indication = null,
                role = Role.Button,
                onClick = { haptics?.play(Feel.Tap); onClick() },
                onLongClick = onLongClick?.let { l -> { haptics?.play(Feel.Bloom); l() } },
            )
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        AppIcons.Icon(icon, null, tint = clear.onGlass, size = iconSize)
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
