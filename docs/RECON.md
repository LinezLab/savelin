# Artefact recon findings

Source read: `Artefact` main branch (downloaded 2026-11-21), specifically `android/app/src/main/java/com/chethan616/clearpdf/**` and the vendored `android/backdrop/**` module. Only source was read; no app or catalog was run (STRICT RULE §7).

## Build setup to reuse

- Gradle wrapper: copy `gradlew`, `gradlew.bat`, `gradle/wrapper/` verbatim, then edit `distributionUrl` if AGP requires a different Gradle.
- Versions (`android/gradle/libs.versions.toml`): AGP 9.2.1, Kotlin 2.3.21, Compose BOM 2026.09.00, Material3 1.5.0-beta02, lifecycle 2.9.4, activity-compose 1.11.0, coroutines 1.10.2, `io.github.kyant0:shapes` 1.0.0, compileSdk 37, minSdk 29, Java 17.
- The glass library is a **local module** `android/backdrop` (Kyant0 AndroidLiquidGlass, Apache-2.0), not a Maven artifact. It needs `freeCompilerArgs += "-Xcontext-parameters"`. It is ~2800 lines and compiles against minSdk 29; AGSL/RenderEffect paths are guarded at runtime.
- Signing: release config reads `CLEARPDF_UPLOAD_*` env vars / `key.properties`, falls back to debug key. We will rename env vars to `KEYSTORE_*` per spec §7.5.
- Manifest: `allowBackup=false`, `enableOnBackInvokedCallback=true`, no INTERNET permission (we must add it), permission removals for `READ_PHONE_STATE`/`WRITE_EXTERNAL_STORAGE`/`READ_EXTERNAL_STORAGE` introduced transitively by `com.kyant.shapes`.

## Visual language

- Wallpaper ("Aurora"): soft vertical gradients over a dark midnight indigo or a pale periwinkle sky, plus floating coloured blooms; all glass refracts this.
- Ink/accent palette (`Candy`): Tomato `#FF5B4F`, Tangerine `#FF9F0A`, Lemon `#FFC21A`, Lime `#34C759`, Mint `#00BFB3`, Sky `#0A84FF`, Grape `#5E5CE6`, Bubblegum `#FF4F8B`.
- Light glass wash = `Color.White.copy(alpha = 0.26f)`; dark = `#C9D2FF.copy(alpha = 0.11f)`; frost = `#F3F6FC@0.82` / `#1A1B3C@0.90`; rim = white@0.70 light / white@0.26 dark.
- Cards: pastel two-stop vertical gradients with tonal ink/accent; dark theme turns them into saturated "jewels". Corner radii: 28.dp general glass, 20.dp panels, capsule for chips/bars.
- Typography: Outfit variable font for all UI (300–800), Plus Jakarta Sans for the "Recent" card, DM Serif Display/Fraunces/Bricolage/Doto for accents. Type scale: Hero 54/56, Display 42/46, Headline 26/30, Title 18/22, Body 15/22, Label 13/18, Caption 11.5/15.

## Glass design system (`ui/glass/`)

- `GlassStage` composes four layers bottom-up: wallpaper → screens → chrome → overlay portal, each a `rememberLayerBackdrop()` combined into the next `LocalBackdrop`. When an overlay is open the scene is flattened with `CompositingStrategy.Offscreen` so moving sheets do not re-blur the scene.
- `Modifier.glass(shape, wash, blur, lensHeight, lensAmount)` is the core still surface: `vibrancy()`/`colorControls()` → `blur()` → `lens()` via `glassEffects()`, plus `glassHighlight()` specular rim. `GlassCard`/`GlassHost` add sibling layers so nested glass refracts the card instead of punching a hole.
- `Modifier.wash()` / `washPanel()` is the cheap glass-look fallback for scrolling lists (background + gradient rim, no backdrop pass).
- `GlassTabs` is the floating pill: capsule with a lens "blob" over the selected tab; drag moves/scales the blob (`DampedDragAnimation`), icons scale to ~1.18 on press, tinted duplicate of the labels is recorded into a layer for the lens to reveal, velocity skews the blob.
- `Buttons.kt`, `Controls.kt`, `Overlays.kt`, `Press.kt`, `MorphButton.kt`, `LiquidMorph.kt` provide glass buttons, toggles, sliders, sheets/dialogs/menus and a single `pressProgress` Animatable driving scale/refraction/highlight. `Haptics.kt` defines `Feel` (Tap/Tick/Bloom/...).

## Motion constants (`ui/glass/Motion.kt`)

- `MotionStyle(bounce=.5, speed=1, press=1, wobble=1, stretch=1, bloom=1)`; presets Calm (.2/1.45/.6/0/.5/.5), Normal (defaults), Bouncy (.75/.92/1.3/1.6/1.2/1.2), Jelly (.95/.82/1.6/2.2/1.5/1.5); default = Bouncy.
- `damping = designed * (1 + (0.5 - bounce) * 1.2)` clamped .12–.98; `stiffness = designed * speed` clamped .4–2.5.
- Spring specs: unfold .68/420, unfoldLag .6/340, morph .58/420, pop .45/500, fold 1/900, shape .6/105, morphOpen .6/150 (floor .3), morphClose/morphChosen critically damped. Rest threshold .0005.
- `EaseCurve.Drawer = (0.32,0.72,0,1)` (UIKit drawer) is the default screen/sheet curve; `Fluid` durations push 420, pop 360, rise 400, dismiss 260, quick 260, fade 200, all multiplied by a user duration slider (0.3–3). `PressedScale = 0.97`.

## Reconciliation with the build spec

- Spec's five glass presets differ from Artefact's four (`Default/Liquid`, `Frosted`, `Crystal`, `Lite` → rename `Water`, `Solid`). We keep Artefact's effect stack and add the two extra presets plus the element registry/Appearance screen.
- Spec's motion styles map onto `MotionStyle` multipliers; spec's "Glass spring" becomes an additional preset. Spec's duration tokens become `Fluid` ms values divided by 300.
- Artefact has no INTERNET, URL fetching, Room, WorkManager, navigation component, or reminders. Those are entirely new. Its `GlassStage`/glass/motion code is directly reusable after renaming the package from `com.chethan616.clearpdf.ui` to the new app package.
- Honest limit: this recon did not read the Backdrop sample catalog components (LiquidButton etc.); the app's own glass composables are the reference implementation instead.
