package com.pinfold.app.ui.detail

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.pinfold.app.R
import com.pinfold.app.data.model.Bookmark
import com.pinfold.app.data.model.BookmarkWithTags
import com.pinfold.app.diagnostics.DiagnosticsLog
import com.pinfold.app.ui.components.AppIcons
import com.pinfold.app.ui.components.DiagnosticsToast
import com.pinfold.app.ui.components.GlassMenuItem
import com.pinfold.app.ui.components.GlassMenuOverlay
import com.pinfold.app.ui.glass.Feel
import com.pinfold.app.ui.glass.GlassPill
import com.pinfold.app.ui.glass.GlassTextField
import com.pinfold.app.ui.glass.LocalHaptics
import com.pinfold.app.ui.glass.SavelinGlassDialog
import com.pinfold.app.ui.glass.theme.Candy
import com.pinfold.app.ui.glass.theme.ClearType
import com.pinfold.app.ui.glass.theme.clear
import com.pinfold.app.ui.home.HomeViewModel
import com.pinfold.app.ui.home.SONIL_LOGO_TEXT
import java.io.File
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

internal const val HERO_BOTTOM_DISSOLVE_FRACTION = 0.42f
internal const val SWIPE_MIN_THRESHOLD_PX = 96f
internal const val SWIPE_HEIGHT_FRACTION = 0.15f
internal const val COLLAPSED_TITLE_LINES = 2
internal const val COLLAPSED_DESCRIPTION_LINES = 3
internal const val METADATA_BOTTOM_FRACTION = 0.35f
internal const val DESCRIPTION_STARTS_EXPANDED = true
internal val METADATA_PANEL_TOP_GAP: Dp = 24.dp
internal val METADATA_PANEL_SIDE_MARGIN: Dp = 20.dp
internal val METADATA_SECTION_GAP: Dp = 3.dp
internal val METADATA_BODY_VERTICAL_GAP: Dp = METADATA_SECTION_GAP
internal val DETAIL_ACTIONS_RESERVED_HEIGHT: Dp = 68.dp
internal val DESCRIPTION_SCROLL_MAX_HEIGHT: Dp = 200.dp
internal val DESCRIPTION_INNER_PADDING: Dp = 16.dp
internal val DESCRIPTION_DISSOLVE_EDGE_DP: Dp = 28.dp
internal val BOTTOM_BUTTON_VERTICAL_MARGIN: Dp = 4.dp
internal val OPEN_TEXT_BLEND_MODE: BlendMode = BlendMode.Overlay
internal val OPEN_TEXT_COLOR: Color = Color.White
internal val OPEN_TEXT_LUMINANCE_BLEND_MODE: BlendMode = BlendMode.Screen
internal val OPEN_TEXT_LUMINANCE_ALPHA: Float = 0.5f
internal val OPEN_TEXT_FONT_SIZE: TextUnit = 30.sp
internal val OPEN_TEXT_LINE_HEIGHT: TextUnit = 34.sp
internal val OPEN_TEXT_FONT_WEIGHT: FontWeight = FontWeight.SemiBold
internal val OPEN_TEXT_LETTER_SPACING: TextUnit = 2.sp
internal val DETAIL_GLYPH_BLEND_MODE: BlendMode = BlendMode.Exclusion
internal val DETAIL_GLYPH_LUMINANCE_BLEND_MODE: BlendMode = BlendMode.Screen
internal val DETAIL_GLYPH_LUMINANCE_ALPHA: Float = 0.5f
internal val DETAIL_GLYPH_COLOR: Color = Candy.Mint
internal val DETAIL_GLYPH_BUTTON_SIZE: Dp = 44.dp
internal val DETAIL_GLYPH_ICON_SIZE: Dp = 24.dp
internal val DETAIL_METADATA_INK: Color = Color.White
internal val DETAIL_LOGO_BLEND_MODE: BlendMode = BlendMode.Overlay
internal val DETAIL_LOGO_LUMINANCE_BLEND_MODE: BlendMode = BlendMode.Screen
internal val DETAIL_LOGO_LUMINANCE_ALPHA: Float = 0.5f
internal val DETAIL_LOGO_FONT_SIZE: TextUnit = 22.sp

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

internal fun metadataBottomOffset(maxHeight: Dp): Dp =
    maxHeight * METADATA_BOTTOM_FRACTION

internal fun titleBottomOffsetPx(maxHeightPx: Float, fraction: Float): Float =
    maxHeightPx * fraction.coerceIn(0f, 1f)

internal fun metadataSectionGap(): Float = METADATA_SECTION_GAP.value

internal fun metadataPanelMaxHeight(maxHeight: Dp, bottomOffset: Dp): Dp =
    maxOf(0.dp, maxHeight - bottomOffset - METADATA_PANEL_TOP_GAP)

internal fun titleTopOffset(maxHeight: Dp, bottomOffset: Dp, panelHeight: Dp): Dp =
    maxOf(0.dp, maxHeight - bottomOffset - panelHeight)

internal fun descriptionMaxLines(expanded: Boolean): Int =
    if (expanded) Int.MAX_VALUE else COLLAPSED_DESCRIPTION_LINES

internal fun collapsedMaxLines(expanded: Boolean): Int = descriptionMaxLines(expanded)

internal fun descriptionOnly(bookmark: Bookmark): String = bookmark.description.trim()

internal fun descriptionDissolveStops(edge: Float): Array<Pair<Float, Color>> = arrayOf(
    0f to Color.Transparent,
    edge to Color.Black,
    1f - edge to Color.Black,
    1f to Color.Transparent,
)

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
    val descriptionExpanded = DESCRIPTION_STARTS_EXPANDED
    var editTitle by remember { mutableStateOf("") }
    var editDescription by remember { mutableStateOf("") }
    var editNotes by remember { mutableStateOf("") }

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
            val density = LocalDensity.current
            val bottomOffset = with(density) {
                titleBottomOffsetPx(screenHeightPx, METADATA_BOTTOM_FRACTION).toDp()
            }
            val titleAnchorTop = maxOf(0.dp, maxHeight - bottomOffset)
            val navInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
            val actionsClearance = DETAIL_ACTIONS_RESERVED_HEIGHT + navInset
            val belowTitleHeight = maxOf(0.dp, bottomOffset - actionsClearance)
            val title = titleFor(bookmark)
            if (title.isNotBlank()) {
                Box(
                    Modifier.align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .padding(horizontal = METADATA_PANEL_SIDE_MARGIN)
                        .padding(bottom = bottomOffset),
                ) {
                    DetailTitleRow(
                        title = title,
                        titleExpanded = titleExpanded,
                        onToggleTitle = { titleExpanded = !titleExpanded },
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
            Column(
                Modifier.align(Alignment.TopStart)
                    .fillMaxWidth()
                    .padding(horizontal = METADATA_PANEL_SIDE_MARGIN)
                    .padding(top = titleAnchorTop)
                    .heightIn(max = belowTitleHeight),
            ) {
                DetailMetadataBody(
                    item = item,
                    descriptionExpanded = descriptionExpanded,
                )
            }
        }

        Box(
            Modifier.align(Alignment.TopStart)
                .windowInsetsPadding(WindowInsets.statusBars.union(WindowInsets.displayCutout))
                .padding(start = METADATA_PANEL_SIDE_MARGIN, top = 8.dp)
                .semantics { contentDescription = "Sonil" },
        ) {
            StickySonilLogo()
        }

        Row(
            Modifier.align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 16.dp)
                .padding(vertical = BOTTOM_BUTTON_VERTICAL_MARGIN)
                .fillMaxWidth()
                .padding(horizontal = METADATA_PANEL_SIDE_MARGIN),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            BareGlyph(
                icon = AppIcons.Back,
                label = "Back",
                onClick = onBack,
            )
            GlassPill(
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
                height = 38.dp,
                horizontalPadding = 14.dp,
            ) {
                OverlayOpenText()
            }
            BareGlyph(
                icon = AppIcons.More,
                label = "More options",
                onClick = { menuOpen = true },
            )
        }

        if (menuOpen) {
            GlassMenuOverlay(
                title = titleFor(bookmark),
                onDismiss = { menuOpen = false },
                ink = clear.hero,
                items = listOf(
                    GlassMenuItem(AppIcons.Edit, "Edit", onClick = {
                        editTitle = bookmark.title
                        editDescription = bookmark.description
                        editNotes = bookmark.notes
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
                        item.tags.map { it.name },
                    )
                    editOpen = false
                },
                dismiss = "Cancel",
                ink = clear.hero,
            ) {
                GlassTextField(value = editTitle, onValueChange = { editTitle = it }, placeholder = "Title", ink = clear.hero)
                GlassTextField(value = editDescription, onValueChange = { editDescription = it }, placeholder = "Description", singleLine = false, ink = clear.hero)
                GlassTextField(value = editNotes, onValueChange = { editNotes = it }, placeholder = "Notes", singleLine = false, ink = clear.hero)
            }
        }

        if (copyToast) DiagnosticsToast("Link copied") { copyToast = false }
    }
}

@Composable
private fun DetailTitleRow(
    title: String,
    titleExpanded: Boolean,
    onToggleTitle: () -> Unit,
    onShare: () -> Unit,
) {
    val ink = DETAIL_METADATA_INK
    Row(verticalAlignment = Alignment.CenterVertically) {
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
        Spacer(Modifier.width(12.dp))
        BareGlyph(
            icon = AppIcons.Share,
            label = "Share",
            onClick = onShare,
        )
    }
}

@Composable
private fun StickySonilLogo() {
    val measurer = rememberTextMeasurer()
    val ink = clear.hero
    val style = TextStyle(
        fontFamily = FontFamily(Font(R.font.nikea)),
        fontSize = DETAIL_LOGO_FONT_SIZE,
        color = ink,
    )
    val textLayout = remember(measurer, style) { measurer.measure(SONIL_LOGO_TEXT, style = style, maxLines = 1) }
    val density = LocalDensity.current
    val width = with(density) { textLayout.size.width.toDp() }
    val height = with(density) { textLayout.size.height.toDp() }
    Box(Modifier.size(width, height), contentAlignment = Alignment.Center) {
        Canvas(
            Modifier.size(width, height).graphicsLayer {
                compositingStrategy = CompositingStrategy.Offscreen
                blendMode = DETAIL_LOGO_BLEND_MODE
            },
        ) {
            drawText(textLayout, color = ink)
        }
        Canvas(
            Modifier.size(width, height).graphicsLayer {
                compositingStrategy = CompositingStrategy.Offscreen
                blendMode = DETAIL_LOGO_LUMINANCE_BLEND_MODE
            },
        ) {
            drawText(textLayout, color = Color.White.copy(alpha = DETAIL_LOGO_LUMINANCE_ALPHA))
        }
    }
}

@Composable
private fun OverlayOpenText() {
    val measurer = rememberTextMeasurer()
    val style = ClearType.Headline.copy(
        fontWeight = OPEN_TEXT_FONT_WEIGHT,
        color = OPEN_TEXT_COLOR,
        fontSize = OPEN_TEXT_FONT_SIZE,
        lineHeight = OPEN_TEXT_LINE_HEIGHT,
        letterSpacing = OPEN_TEXT_LETTER_SPACING,
    )
    val textLayout = remember(measurer, style) { measurer.measure("Open", style = style, maxLines = 1) }
    val density = LocalDensity.current
    val textWidth = with(density) { textLayout.size.width.toDp() }
    val textHeight = with(density) { textLayout.size.height.toDp() }
    Box(Modifier.size(textWidth, textHeight), contentAlignment = Alignment.Center) {
        Canvas(
            Modifier.size(textWidth, textHeight).graphicsLayer {
                compositingStrategy = CompositingStrategy.Offscreen
                blendMode = OPEN_TEXT_BLEND_MODE
            },
        ) {
            drawText(textLayout, color = OPEN_TEXT_COLOR)
        }
        Canvas(
            Modifier.size(textWidth, textHeight).graphicsLayer {
                compositingStrategy = CompositingStrategy.Offscreen
                blendMode = OPEN_TEXT_LUMINANCE_BLEND_MODE
            },
        ) {
            drawText(textLayout, color = Color.White.copy(alpha = OPEN_TEXT_LUMINANCE_ALPHA))
        }
    }
}

@Composable
private fun ColumnScope.DetailMetadataBody(
    item: BookmarkWithTags,
    descriptionExpanded: Boolean,
) {
    val bookmark = item.bookmark
    val description = descriptionOnly(bookmark)
    val stats = detailStats(bookmark)
    val descriptionScrollState = rememberScrollState()
    val hasPrimary = description.isNotBlank()
    val fallback = if (hasPrimary) emptyList() else fallbackMetadataLines(bookmark)
    if (!hasPrimary && fallback.isEmpty()) return

    val soft = DETAIL_METADATA_INK
    Column(Modifier.padding(vertical = METADATA_BODY_VERTICAL_GAP)) {
        if (stats.isNotEmpty()) {
            Row(
                Modifier.padding(top = METADATA_SECTION_GAP),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                stats.forEach { stat ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AppIcons.Icon(stat.icon, null, tint = soft, size = 14.dp)
                        Spacer(Modifier.width(6.dp))
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
            val scrollModifier = if (descriptionExpanded) {
                Modifier
                    .heightIn(max = DESCRIPTION_SCROLL_MAX_HEIGHT)
                    .pointerInput(Unit) {
                        detectVerticalDragGestures { change, _ -> change.consume() }
                    }
                    .verticalScroll(descriptionScrollState)
            } else {
                Modifier
            }
            Box(
                Modifier
                    .padding(top = METADATA_SECTION_GAP)
                    .weight(1f, fill = false)
                    .then(scrollModifier)
                    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                    .drawWithContent {
                        drawContent()
                        val edge = (DESCRIPTION_DISSOLVE_EDGE_DP.toPx() / size.height).coerceIn(0f, 0.45f)
                        drawRect(
                            brush = Brush.verticalGradient(
                                colorStops = descriptionDissolveStops(edge),
                                startY = 0f,
                                endY = size.height,
                            ),
                            blendMode = BlendMode.DstIn,
                        )
                    }
            ) {
                Text(
                    text = description,
                    color = soft,
                    style = ClearType.Body,
                    maxLines = descriptionMaxLines(descriptionExpanded),
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(vertical = DESCRIPTION_INNER_PADDING),
                )
            }
        }
        if (!hasPrimary) {
            Column(
                Modifier.padding(top = METADATA_SECTION_GAP),
                verticalArrangement = Arrangement.spacedBy(8.dp),
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
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BareGlyph(
    icon: Int,
    label: String,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    size: Dp = DETAIL_GLYPH_BUTTON_SIZE,
    iconSize: Dp = DETAIL_GLYPH_ICON_SIZE,
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
        Box(
            Modifier.graphicsLayer {
                compositingStrategy = CompositingStrategy.Offscreen
                blendMode = DETAIL_GLYPH_BLEND_MODE
            },
            contentAlignment = Alignment.Center,
        ) {
            AppIcons.Icon(icon, null, tint = DETAIL_GLYPH_COLOR, size = iconSize)
        }
        Box(
            Modifier.graphicsLayer {
                compositingStrategy = CompositingStrategy.Offscreen
                blendMode = DETAIL_GLYPH_LUMINANCE_BLEND_MODE
            },
            contentAlignment = Alignment.Center,
        ) {
            AppIcons.Icon(
                icon,
                null,
                tint = Color.White.copy(alpha = DETAIL_GLYPH_LUMINANCE_ALPHA),
                size = iconSize,
            )
        }
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
