# Now-playing background reconstruction

Research date: 2026-11-23. Source read from the public repository
`https://github.com/kushagrasinghx/BitChord` at commit
`21356206de5594ca0bdaf4680ca2db68a2287729` (cloned read-only into a temp directory; nothing from
the clone was added to this repository's app sources). This document is the only place in Savelin
where that source repository is named.

## What the reference actually draws

The player has three selectable backdrops (`shared/.../data/settings/PlayerEnums.kt`,
`PlayerTheme`): `DEFAULT`, `LEGACY`, `SIMPLE`. `AppSettings.playerTheme` defaults to `DEFAULT`,
so the look on screen for an ordinary song is the **artwork mesh**, not the four-blob gradient that
older write-ups called the Apple Music background. The other two are explicit user choices and are
documented here for completeness.

### 1. `DEFAULT` — `ArtworkMeshBackdrop` (the current now-playing screen)

Files: `sharedUi/src/jvmSharedMain/kotlin/com/music/bitchord/ui/player/ArtworkMeshBackdrop.kt`,
`NowPlayingScreen.kt` (`PlayerMeshBackdrop`), `ColorUtils.kt`.

Construction (`meshOf`, `MESH_*` constants):

- Decode the artwork at `MESH_PX = 120` via Coil with `forPixelAccess()`; no quantiser is used.
- Average RGB into a `MESH_GRID = 6` by 6 grid (sampling stride capped by `MESH_SAMPLE = 128`).
  A cell mean, not a palette swatch: a cover that is nine-tenths black stays nine-tenths black.
- Read rows bottom-to-top, so **grid row 0 is the artwork's own bottom edge**.
- Every cell is lifted once: HSL saturation x `MESH_VIBRANCE = 1.12`, lightness floored at
  `MESH_FLOOR = 0.045` (HSL helpers ported from `androidx.core.graphics.ColorUtils`).
- `rotatedBelowSeam`: row 0 is kept literal; rows 1..5 are cyclically shifted by a seed-derived
  amount and mirrored half the time. A cyclic shift preserves each cell's neighbours, so the result
  reads as a smooth colour field rather than a reflection or a patchwork of islands.
- `resampled`: smoothstep interpolation of the 6x6 control points into `MESH_TEX = 32` texels;
  three box-blur passes with radius `MESH_BLUR_RADIUS = 1` bake in the softness of a ~32dp live blur.
  The texture is held as a tiny bitmap so one bilinear `drawImage` interpolates the whole mesh.

Composition (`ArtworkMeshBackdrop`, `drawMesh`):

- The mesh is **anchored to the seam** (the artwork's bottom edge, passed as a `Dp` lambda and read
  during draw). From the seam down, the full 32x32 texture is stretched over the remaining height.
- Above the seam, only texture row 0 is drawn, stretched from the top of the screen to the seam:
  the sleeve's last colour is held behind the artwork so there is no join to hide.
- The seam glides to a new target with `MESH_SEAM_MOVE_MS = 320` (`FastOutSlowInEasing`), and snaps
  when either end is the top of the screen.
- A single scrim over everything: `Brush.verticalGradient(0f -> Black 0.06f, 1f -> Black 0.30f)`.
  Opaque fallback floor `Color(0xFF121212)` until an artwork has been read.
- Crossfade: a second mesh is drawn over the outgoing one with `MESH_FADE_MS = 900`,
  `FastOutSlowInEasing`; a mid-fade change promotes the incoming mesh to be the new outgoing one.
  `reduceAnimation` (the reference's Reduce motion setting) snaps instead of fading.

### 2. `LEGACY` — `MeshGradientBackground` (v1.5, opt-in)

Files: `MeshGradient.kt`, palette via `ColorUtils`, `PaletteQuantizer.kt`, `FadeCurve.kt`.

- Four colours from the artwork: Coil decode at 128px, Palette colour cut with 24 swatches, sorted
  by population, near-duplicates dropped (hue gap < 15 degrees and lightness gap < 0.12), topped up
  from the artwork's own hues when fewer than four exist. Only if there is nothing at all are the
  built-in fallback colours used.
- Each colour is tuned: HSL saturation x 1.35, lightness clamped to 0.28..0.58. The base is the first
  tuned colour with lightness forced to 0.12.
- Four radial-gradient blobs at anchors (0.20,0.25), (0.80,0.20), (0.75,0.80), (0.25,0.75), radius
  `maxDimension * 0.62`, centre alpha `BLOB_ALPHA = 0.85`, falloff along smootherstep
  (`6t^5 - 15t^4 + 10t^3`, 16 stops) so no rim shows.
- The whole canvas is scaled 1.3x, clipped to bounds, blurred `64.dp`, then scrimmed
  Black 0.10 -> Black 0.38. Blobs drift 0.16 of the canvas with irrational-ish speeds
  (1, -0.7, 0.85, -1.15) for an 8s settle on open/track change; continuous orbit is off by default.
  Colour crossfade is 1400ms; Reduce motion snaps colours and stops drift.
- `SIMPLE` instead uses `FullArtworkBlurBackdrop`: one 128px copy of the cover, three CPU box-blur
  passes (radius `min(w,h)/12`), drawn full-bleed with a 0.34 -> 0.48 -> 0.64 black scrim, crossfade
  320ms. The blurred copy is prefetched and cached by artwork key, never blurred live on the main
  thread.

### Edge dissolve

The mesh is the reason there is no edge: the first row is the exact colour the artwork ended on and
is held above the seam, so the sharp artwork can feather its own bottom alpha to zero over a long
band without exposing a different colour underneath. The reference also uses offscreen `DstIn` masks
with smootherstep ramps (`FadingEdges.kt`) when it needs a line to run out rather than cut.

### Settings that control the look

- `PlayerTheme` (DEFAULT / LEGACY / SIMPLE), default `DEFAULT`, persisted as `playerTheme` with an
  old `legacyMeshGradient` preference migration.
- `PlayerSettings.reduceAnimation` — snaps palette/mesh crossfades and stops drift.
- `PlayerSettings.reduceDynamicBlur` and `disableBlendEffects` affect the frosted panels over the
  backdrop, not the backdrop itself.
- Legacy knobs that are off by default: `driftMillis = 8000`, `continuous = false`,
  `blurRadius = 64.dp`, `colorFadeMillis = 1400`, `animated = true`.

## Savelin decision

Savelin's detail page keeps its sharp hero and Back affordance only — no player controls. Its ambient
layer is rebuilt as the `DEFAULT` mesh: the stored preview is downsampled to a 6x6 mesh, its bottom
row is held to the hero seam, the mesh is stretched below it, softened with the same baked blur and
covered by the same 0.06 -> 0.30 scrim. Crossfades between bookmarks use the mesh's 900ms
`FastOutSlowInEasing`; Reduce motion snaps. The existing long hero feather supplies the edge
dissolve. Missing previews get a deterministic, palette-derived mesh instead of a flat colour.
No player-theme setting is added: other screens stay untouched and `reduceMotion` remains the only
motion control here.

The iOS/web write-up behind the older `docs/APPLE-MUSIC-ARTWORK-EDGE.md` (four copies of the artwork,
twist, heavy blur) is a different lineage; this document records the Android reference actually
asked for. Its hero feather and oversized halo are retained only where they still serve the mesh.

## Licence

The reference is GPL-3.0 (see its root `LICENSE`, and README: "BitChord is free software under the
GPLv3 ... any distribution must come with the Corresponding Source under the same license"). The
mesh algorithm above is a close, independent Kotlin translation of that GPL code, so Savelin carries
the full text in `third_party/BITCHORD-GPL.txt` and an attribution in `NOTICE`. No reference file
was copied verbatim and no reference identifier, package, class, string or comment appears in app
code. Maintainers should still review whether this amounts to a derivative work for the purposes of
the whole application before a release is published.
