# Apple Music "Now Playing" background recon

Read-only design research for the Savelin detail screen. No player controls, transport,
lyrics, progress bar or music-app navigation are in scope — only the **background language**.

Public design references (fetched 2026-11-23):

1. Aadish Verma, *Reverse engineering Apple Music's background gradient* — https://www.aadishv.dev/music
2. Sam Henri Gold's FLEX breakdown of the Apple Music visualiser (linked from source 1) — https://x.com/samhenrigold/status/1765220903963574290
3. Blake Crosley, *Apple Music: Editorial Voice Meets Spatial Sound* — https://blakecrosley.com/guides/design/apple-music
4. Jana, *How to Create Apple Music's Background Blur Effect Using Blurhash* — https://hackernoon.com/how-to-create-apple-musics-background-blur-effect-using-blurhash
5. MDN, `mask-image` — https://developer.mozilla.org/en-US/docs/Web/CSS/Reference/Properties/mask-image
6. Apple HIG, *Playing audio* — https://developer.apple.com/design/human-interface-guidelines/playing-audio

## Design observations

- The artwork drives the whole environment: a few dominant colours are sampled and clamped, then
  used to tint the background rather than applying an app-default fill (source 3).
- The ambient field reads as several transformed, heavily blurred copies of the same artwork plus
  soft pools of derived colour — not a single flat gradient (sources 1, 2).
- A heavily downsampled copy is enough: a tiny representation stretched and blurred already gives
  an authentic ambient background and is cheap on a phone (source 4).
- Immersive treatments add a dark, image-derived wash so overlaid content stays readable, and they
  honour reduced-motion by replacing transitions with an instant change (sources 1, 3).
- The hero art sits in the upper portion at full bleed, with no card, border or shadow (source 3).

## Our visual recipe

1. The root is full-screen and tinted from the artwork's dark base tone.
2. Downsample the stored preview, bucket opaque pixels by hue band and saturation tier, average
   each bucket, and rank by population with a small saturation bonus. The winner is the dominant
   tone; a clamped, darkened version becomes the base; the top buckets plus hue-shifted companions
   become the accents. With no image, a deterministic hash of the bookmark id produces the same
   shape of palette.
3. Draw an enlarged, heavily blurred copy of the preview, then five soft overlapping radial blooms
   placed at fixed balanced positions using palette accents.
4. Add a second, more blurred copy across the lower portion plus a dark fade built from the base
   tone, so the bottom diffuses smoothly without a solid bar.
5. A subtle vertical readability scrim and a faint deterministic grain sit above the field.
6. The sharp hero is drawn full-width at its natural aspect ratio in the upper portion, with no
   card or frame. An oversized blurred copy of the same image sits behind it and continues past
   its lower edge, so a long alpha ramp on the hero reveals matching colour with no hard boundary.
7. Switching bookmarks crossfades the whole ambient; Reduce motion snaps instantly. Palette and
   bitmaps are cached per bookmark id and sampled off the main thread.
8. The only overlay is a small safe Back affordance. There are no controls of any kind and none of
   the app's older glass detail containers or chips.

## Implementation map

- `ArtworkPalette.kt` — pure colour maths: downsampling, bucketing/quantisation, fallback palette,
  bloom placement and the edge/diffusion curves.
- `ArtworkAmbience.kt` — decoding, per-id cache, redacted diagnostics and crossfade decisions.
- `AmbientBackdrop.kt` — the layered ambient field composable.
- `DetailScreen.kt` — hero, halo, edge feather, crossfade and Back affordance.

Tests cover palette hues, dark/neutral inputs, downsampling, bloom placement, the hero edge ramp,
the halo coverage curve, crossfade decisions and redacted diagnostics.
