package com.pinfold.app.ui.glass

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color

/** The ten style groups of Settings ▸ Liquid glass and motion ▸ Components. */
enum class GlassComponent(val label: String, val canMatchButtons: Boolean = false) {
    BUTTONS("Buttons"),
    CHIPS("Chips"),
    CARDS("Cards"),
    NAVIGATION("Navigation"),
    SEARCH_INPUTS("Search and inputs"),
    POPUPS("Pop-ups", true),
    SHEETS_DIALOGS("Sheets and dialogs", true),
    SNACKBARS_TOOLTIPS("Snackbars and tooltips", true),
    SETTINGS_SURFACES("Settings surfaces"),
    TOP_SHADE("Top glass shade"),
}

/** Global follows the main theme (controls disabled); Custom owns values; Match buttons uses Buttons'. */
enum class ComponentStyleMode { GLOBAL, CUSTOM, MATCH_BUTTONS }

/** Only neutral white, neutral black, or automatic (the theme decides) are allowed as a tint. */
enum class TintChoice { WHITE, BLACK, AUTOMATIC }

/**
 * Every value a group can own: the spec's shared list plus a few group extras. Immutable so a
 * resolved style can be cached and handed to components directly.
 */
@Immutable
data class ComponentGlassStyle(
    val preset: String = "LIQUID",
    val intensity: Float = 1f,
    val blur: Float = 1f,
    val refractionHeight: Float = 1f,
    val refractionAmount: Float = 1f,
    val edgeThickness: Float = 1f,
    val tintOpacity: Float = 1f,
    val tint: TintChoice = TintChoice.AUTOMATIC,
    val saturation: Float = 1.5f,
    val rimLight: Float = 1f,
    val specular: Float = 1f,
    val noise: Float = 0f,
    val shadow: Float = 1f,
    val cornerRadius: Float = 1f,
    val opacity: Float = 1f,
    /** Packed ARGB; 0 means "inherit the theme's content colour". */
    val contentColor: Long = 0L,
    val motionStyle: String = "NORMAL",
    val holdEffect: String = "MAGNIFY",
    val haptics: Boolean = true,
    val chipEffect: String = "LIGHT",
    val photoThrough: Boolean = false,
    val filterRail: Boolean = false,
    val titleHeader: Boolean = false,
) {
    fun contentColorOr(default: Color): Color = if (contentColor == 0L) default else Color(contentColor)

    /** Stable, locale-independent encoding for DataStore/SharedPreferences. */
    fun encode(): String = listOf(
        "v1", preset, intensity, blur, refractionHeight, refractionAmount, edgeThickness,
        tintOpacity, tint.name, saturation, rimLight, specular, noise, shadow, cornerRadius,
        opacity, contentColor, motionStyle, holdEffect, haptics, chipEffect, photoThrough,
        filterRail, titleHeader,
    ).joinToString("|")

    companion object {
        fun decode(value: String?): ComponentGlassStyle? {
            if (value.isNullOrBlank()) return null
            val p = value.split("|")
            if (p.size < 24 || p[0] != "v1") return null
            fun f(i: Int, fallback: Float) = p[i].toFloatOrNull() ?: fallback
            return ComponentGlassStyle(
                preset = p[1],
                intensity = f(2, 1f),
                blur = f(3, 1f),
                refractionHeight = f(4, 1f),
                refractionAmount = f(5, 1f),
                edgeThickness = f(6, 1f),
                tintOpacity = f(7, 1f),
                tint = runCatching { TintChoice.valueOf(p[8]) }.getOrDefault(TintChoice.AUTOMATIC),
                saturation = f(9, 1.5f),
                rimLight = f(10, 1f),
                specular = f(11, 1f),
                noise = f(12, 0f),
                shadow = f(13, 1f),
                cornerRadius = f(14, 1f),
                opacity = f(15, 1f),
                contentColor = p[16].toLongOrNull() ?: 0L,
                motionStyle = p[17],
                holdEffect = p[18],
                haptics = p[19].toBooleanStrictOrNull() ?: true,
                chipEffect = p[20],
                photoThrough = p[21].toBooleanStrictOrNull() ?: false,
                filterRail = p[22].toBooleanStrictOrNull() ?: false,
                titleHeader = p[23].toBooleanStrictOrNull() ?: false,
            )
        }
    }
}

/**
 * The one resolver. Chain: element override -> group Custom -> Buttons effective (for Match buttons
 * groups) -> main theme. Results are immutable and cached; [invalidate] recomputes them on a settings
 * change, and components read a style through the group provider instead of raw numbers.
 */
object ComponentGlass {
    private const val PREFS = "glass"
    private val modes = mutableMapOf<GlassComponent, ComponentStyleMode>()
    private val customs = mutableMapOf<GlassComponent, ComponentGlassStyle>()
    private val cache = mutableStateMapOf<GlassComponent, ComponentGlassStyle>()

    /** Increments on every settings change so the settings UI can show that values moved. */
    var revision by mutableStateOf(0)
        private set

    /** Light (default) or Full glass effect for chips, mirrored from the data layer. */
    var chipEffect by mutableStateOf("LIGHT")
        private set

    fun load(context: Context) {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        chipEffect = p.getString("chipEffect", "LIGHT") ?: "LIGHT"
        GlassComponent.entries.forEach { c ->
            modes[c] = runCatching {
                ComponentStyleMode.valueOf(p.getString("comp_${c.name}_mode", ComponentStyleMode.GLOBAL.name)!!)
            }.getOrDefault(ComponentStyleMode.GLOBAL)
            p.getString("comp_${c.name}_style", null)?.let { ComponentGlassStyle.decode(it) }?.let { customs[c] = it }
        }
        invalidate()
    }

    fun mode(component: GlassComponent): ComponentStyleMode = modes[component] ?: ComponentStyleMode.GLOBAL

    fun setChipEffect(context: Context, value: String) {
        chipEffect = value
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString("chipEffect", value).apply()
        invalidate()
    }

    /** A group going Custom starts from a copy of the values it currently shows. */
    fun custom(component: GlassComponent): ComponentGlassStyle = customs[component] ?: mainThemeStyle()

    fun setMode(context: Context, component: GlassComponent, mode: ComponentStyleMode) {
        modes[component] = mode
        if (mode == ComponentStyleMode.CUSTOM && !customs.containsKey(component)) customs[component] = effective(component)
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString("comp_${component.name}_mode", mode.name).apply()
        invalidate()
    }

    fun setCustom(context: Context, component: GlassComponent, style: ComponentGlassStyle) {
        customs[component] = style
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString("comp_${component.name}_style", style.encode()).apply()
        invalidate()
    }

    fun reset(context: Context, component: GlassComponent) {
        modes[component] = ComponentStyleMode.GLOBAL
        customs.remove(component)
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove("comp_${component.name}_mode")
            .remove("comp_${component.name}_style").apply()
        invalidate()
    }

    /** Copies the effective style of [source] into [target] as a new Custom style. */
    fun copyFrom(context: Context, target: GlassComponent, source: GlassComponent) {
        setCustom(context, target, effective(source))
        setMode(context, target, ComponentStyleMode.CUSTOM)
    }

    /** Where a group's values come from, for the settings UI's "source of values" labels. */
    fun source(component: GlassComponent): String = when (mode(component)) {
        ComponentStyleMode.CUSTOM -> "Custom values"
        ComponentStyleMode.MATCH_BUTTONS -> "Match buttons"
        ComponentStyleMode.GLOBAL -> "Main theme"
    }

    private fun mainThemeStyle(): ComponentGlassStyle {
        val s = GlassSettings.style
        return ComponentGlassStyle(
            preset = GlassStyle.Named.firstOrNull { it.second == s }?.first ?: "Custom",
            intensity = s.tint,
            blur = s.blur,
            refractionHeight = s.refractionHeight,
            refractionAmount = s.refractionAmount,
            rimLight = s.highlight,
            saturation = s.saturation,
            tintOpacity = 1f,
            opacity = 1f,
            motionStyle = MotionSettings.style.label,
            haptics = GlassSettings.haptics,
            chipEffect = chipEffect,
        )
    }

    /** Group extras that are on by design, before any user override. */
    private fun ComponentGlassStyle.forComponent(c: GlassComponent): ComponentGlassStyle = copy(
        photoThrough = photoThrough || c == GlassComponent.CHIPS || c == GlassComponent.CARDS,
        filterRail = filterRail || c == GlassComponent.NAVIGATION,
        titleHeader = titleHeader || c == GlassComponent.POPUPS,
    )

    private fun buttonsEffective(): ComponentGlassStyle =
        if (mode(GlassComponent.BUTTONS) == ComponentStyleMode.CUSTOM) custom(GlassComponent.BUTTONS) else mainThemeStyle()

    /** Resolved style for a group; [override] stands for a single element's own style. */
    fun resolve(component: GlassComponent, override: ComponentGlassStyle? = null): ComponentGlassStyle {
        if (override != null) return override
        return cache[component] ?: mainThemeStyle().forComponent(component).also { cache[component] = it }
    }

    /** The style a group currently resolves to (Copy style and the settings pages). */
    fun effective(component: GlassComponent): ComponentGlassStyle = resolve(component)

    fun invalidate() {
        revision++
        cache.clear()
        GlassComponent.entries.forEach { c ->
            cache[c] = when (mode(c)) {
                ComponentStyleMode.GLOBAL -> mainThemeStyle().forComponent(c)
                ComponentStyleMode.CUSTOM -> custom(c).forComponent(c)
                ComponentStyleMode.MATCH_BUTTONS -> buttonsEffective().forComponent(c)
            }
        }
    }
}

/** The resolved style a component receives from its group provider. */
val LocalComponentGlass = compositionLocalOf<ComponentGlassStyle> { ComponentGlass.resolve(GlassComponent.BUTTONS) }

@Composable
fun ProvideComponentGlass(component: GlassComponent, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalComponentGlass provides ComponentGlass.resolve(component), content = content)
}
