# Artefact floating navigation recon (read-only, section 9)

Source read from the Artefact tarball in the local temp workspace (no network source fetch needed, no build run). Files: `android/app/src/main/java/com/chethan616/clearpdf/ui/home/CreateDock.kt`, `ui/glass/Tabs.kt`, `ui/glass/Buttons.kt`, `ui/glass/Motion.kt`, `ui/theme/ClearTheme.kt`, `ui/ClearApp.kt`, `ui/desk/FilesScreen.kt`.

## What actually exists

Artefact's home screen has **no bottom tab bar**. `ClearApp.kt:248-270` puts a single `CreateDock` in `GlassStage`'s `chrome` slot and only while `onDesk` (or while morphing into/from a note). Navigation between Home/Files/Tools is state in `AppViewModel.homeTab`, not a nav bar. The floating element is a **create capsule**: a white circular `+` button (58 dp) plus a 54 dp microphone glass circle inside one glass capsule. The `+` fans out creation chips above it (54 dp high chips, 10 dp apart); a long-held `+` is not specified, the tap toggles the fan.

The `GlassTabs` composable is a **segmented control**, used in Files (Recent/Starred/On-device), the sheet viewer and the PDF tools — not the home nav. It is the source for the draggable selection bubble.

## Exact values (Artefact's, not the section 9 starting numbers)

### CreateDock (`CreateDock.kt`)
- Placement: `Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 18.dp)` (line 291). No three-button-nav special case beyond the inset.
- Glass: `.glass(Capsule, c.glass, blur = 3.dp, lensHeight = 14.dp, lensAmount = 26.dp)` then `.graphicsLayer { clip = true; shape = Capsule }`, `.drawBehind { drawRect(c.capsule, alpha = lerp(0.5f, 1.9f, morph).coerceAtMost(1f)) }`, then `.padding(6.dp)` (lines 311-314). Capsule radius = height/2; no explicit stroke here — the rim comes from `Modifier.glass`'s `glassHighlight(style)`.
- Contents: `Row` with `Arrangement.spacedBy(8.dp)`, vertically centred (270-271). `+` = 58 dp white circle, `shadowElevation = 4.dp`, pressed scale **0.9**, glyph 22 dp, ink `DockInk = Color(0xFF1C2433)` (96, 352-362). Mic = 54 dp `GlassCircle`, `surface = Color.White.copy(alpha = 0.30f)`, 22 dp glyph (277-280).
- `+` glyph rotates 45° when the fan opens using `GlassMotion.morph()`; closes with `Fluid.dismiss()` (353).
- Scrim behind the fan: `Color.Black.copy(alpha = if (c.dark) 0.45f else 0.24f)`, fades with `Fluid.quick()` (223-235).
- Fan: `Alignment.BottomCenter`, `navigationBarsPadding()`, `padding(bottom = 96.dp)`, chips 54 dp high, 10 dp apart, content padding start 8 / end 22 dp, blur 6 dp, icon 38 dp circle with 19 dp glyph (390-443).
- Show/hide: present only on the desk; no scroll-hide. When a note opens, the whole capsule grows/morphs into the note's toolbar (`into`/`fromNote` `Animatable`, `spring(maxOf(MotionSettings.damping(0.86f), 0.8f), MotionSettings.stiffness(400f))`, timeout 450 ms) and translates to the toolbar's bottom. Disposed when leaving the desk; recording stops on `ON_STOP`.
- Jelly swell: `scaleX` 0.92–1.03, `scaleY` 0.93–1.06, `TransformOrigin(0.5f, 1f)`, driven by `MotionSettings.style.stretch` (293-308).

### GlassTabs (`Tabs.kt`, as read in section 1 recon)
- Height default **64.dp**; Files uses **48.dp** with `padding(horizontal = 16.dp, vertical = 6.dp).fillMaxWidth()` (FilesScreen 162-163).
- Capsule, inner height = `height - 8.dp`, panel padding 4.dp, tabs weighted equally.
- Body glass: `glassEffects(style, 4f.dp, 24f.dp, 24f.dp)`, wash = `c.glass`, highlight from `glassHighlight(style)`.
- Blob: lens `lens(10.dp * press * style.refractionHeight, 14.dp * press * style.refractionAmount, chromaticAberration = true)`, inner shadow `InnerShadow(radius = 8.dp * press, alpha = press)`, velocity skew of scaleX/scaleY by ±20 %.
- Drag: `DampedDragAnimation(scope, selected, 0f..count-1, 0.001f, 1f, 78f/56f)`; on release `spring(1f, 300f, 0.5f)` settles the panel offset. Icons scale to `lerp(1f, 1.18f, pressProgress)`. Haptic `Feel.Tick` on tab change.

### Theme ink / tint (`ClearTheme.kt`)
- Light: `glass = Color.White.copy(alpha = 0.26f)`, `capsule = Color(0xFF26324A).copy(alpha = 0.34f)`, `ink = Color(0xFF172033)`, `line = ink@0.08f`, `frost = Color(0xFFF3F6FC)@0.82f`.
- Dark: `glass = Color(0xFFC9D2FF)@0.11f`, `capsule = Color(0xFF0B0C22)@0.46f`, `ink = Color(0xFFEEF1FF)`, `line = ink@0.10f`, `frost = Color(0xFF1A1B3C)@0.90f`.
- `DockInk` is fixed dark `#1C2433` because the `+` is always a white circle in both themes.
- Accents (`Candy`): Tomato `#FF5B4F`, Tangerine `#FF9F0A`, Lemon `#FFC21A`, Lime `#34C759`, Mint `#00BFB3`, Sky `#0A84FF`, Grape `#5E5CE6`, Bubblegum `#FF4F8B`.

### Motion (`Motion.kt`)
- Press/drag/hold follow `MotionSettings` multipliers; `Fluid.pressIn` = spring(1, 1800), `pressOut` = spring(1, 420), `PressedScale = 0.97`; `GlassMotion.pop()` = spring(damping(0.45), stiffness 500), `morph()` = damping(0.58)/420, `settle()` = spring(1, Medium). Rest 0.0005. Haptics via `LocalHaptics` / `Feel`.

## Where section 9 differs from Artefact

- Section 9's three-button bar (Search / Plus / Settings) and draggable lens bubble over non-tabs are **new**; Artefact has neither. We keep Artefact's capsule proportions, glass stack, `DockInk` glyph colour, 6 dp inner padding, 18 dp bottom inset + navigation bars, 58 dp circular Plus, and the same motion/haptic specs.
- We adopt section 9's 64 dp height and 240–320 dp width (Artefact's dock is content-sized, ~90 dp, not a three-slot bar), continuous capsule corners, and the search morph.
- Artefact has no scroll-hide or search-morph to copy; those are implemented with `Fluid`/`GlassMotion` specs from above. No screenshots needed since the source was readable.
