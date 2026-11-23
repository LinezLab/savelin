# Apple Music Now Playing: why the hero artwork has no visible bottom edge

Question answered here: when the sharp "hero" artwork in Apple Music's Now Playing screen meets the
ambient background, why is there no seam/edge at the bottom of the artwork, and how do we reproduce
that in Savelin without a card, border or hard cut?

Research date: 2026-11-23. Read-only web research; nothing in this document is copied code from a
proprietary app — it is the published technique plus our own Compose translation.

## Sources

1. Aadish Verma, *Reverse engineering Apple Music's background gradient* (2025-11-09) —
   https://www.aadishv.dev/music
   - Quotes Sam Henri Gold's FLEX breakdown: the visualiser is built by "layering copies of the
     artwork", twisting each copy and applying a blur shader, all on the GPU.
   - The decompiled web build stacks **four copies of the same album art** at 25/50/80/125% of the
     viewport width, **oversaturates** them, applies a twist and then a Gaussian/Kawase blur.
   - Confirms a dark, image-derived wash and `prefers-reduced-motion` handling.
2. Sam Henri Gold, FLEX breakdown of the Apple Music visualiser (linked from source 1) —
   https://x.com/samhenrigold/status/1765220903963574290
3. Blake Crosley, *Apple Music: Editorial Voice Meets Spatial Sound* —
   https://blakecrosley.com/guides/design/apple-music
   - The artwork is "the generative source of the visual design": two to three dominant colours are
     extracted and clamped, then tint the whole Now Playing environment.
   - The immersive/lyrics treatment is the same art at full bleed with a very heavy blur plus a dark
     wash (`backdrop-filter: blur(60px) saturate(1.5)` + `rgba(0,0,0,.5)` in the article's CSS sketch).
   - Full-bleed art is a first-class mode; no card frame is part of the language we reproduce.
4. Jana, *How to Create Apple Music's Background Blur Effect Using Blurhash* —
   https://hackernoon.com/how-to-create-apple-musics-background-blur-effect-using-blurhash
   - A 32x32 encoding of the *same cover* is stretched and blurred to make the ambient background;
     a low-resolution copy of the same image is sufficient and cheap.
5. MDN, `mask-image` CSS property —
   https://developer.mozilla.org/en-US/docs/Web/CSS/Reference/Properties/mask-image
   - A gradient is a valid mask image. Masking "hides sections of the element ... based on the alpha
     channel". The docs' own example uses `radial-gradient(black 50%, transparent 60%)` to create
     "a round image with a soft edge": alpha is interpolated across the gradient, so the edge is a
     **ramp**, not a step. This is the mechanism that removes a hard boundary.
6. Apple HIG, *Playing audio* —
   https://developer.apple.com/design/human-interface-guidelines/playing-audio.md
   (read for completeness; it governs audio behaviour, not the artwork edge.)

Stack Overflow questions 71257653 and 41094962 were attempted but returned HTTP 403 to the fetcher;
their substance (duplicate the artwork, scale it up, blur it heavily, put the crisp art on top) is
already covered by sources 1-4 and by `docs/APPLE-MUSIC-RECON.md`.

## What actually removes the edge

The seam is not hidden by painting a band over the artwork. It is removed by making the **alpha of
the sharp artwork itself fall to zero over a long distance**, while an **enlarged, blurred copy of the
very same image** is already behind it and continues past the seam:

1. **One source image.** The sharp hero and the ambient field are the same artwork. The blurred copy
   is a heavily downsampled render of that file (sources 1, 3, 4). Matching the source guarantees
   matching hues at the transition; a generic gradient or a different image cannot do this.
2. **Colours are matched, not invented.** Palette tones are sampled/clamped from the artwork and the
   blurred copy keeps its saturation, often deliberately oversaturated (sources 1, 3). A dark wash
   sampled from the image (`base` mixed toward black) unifies the lower region (sources 1, 3).
3. **A long feathered alpha ramp, never a hard cut.** The sharp layer's mask goes from fully opaque
   to fully transparent over a large fraction of the artwork's height. In alpha-compositing terms the
   mask multiplies the destination alpha (`DstIn` in Compose, `mask-image` on the web). Because the
   ramp is continuous and reaches exactly zero at the seam, there is no row of pixels where the
   sharp layer abruptly stops; no edge can be perceived (source 5). A short band that starts with an
   abrupt "sharp -> blurred" step fails precisely because that step is a discontinuity.
4. **An oversized blurred halo that begins before, and extends past, the seam.** The blurred copy is
   scaled up (>1) and blurred by a large radius so its own borders are outside the visible area. It
   is already visible through the feathering sharp layer at the seam and continues below it before
   fading out with its own soft tail (sources 1, 4). The eye therefore tracks a gradual loss of focus
   in the same colours — not a boundary between two objects.
5. **No card, border or shadow.** Apple's full-bleed presentation has no frame around the hero
   (source 3; our recon deliberately excludes the card frame from its sketch).

Put differently: at the seam the composition is `sharp(alpha -> 0) over blurred-same-image(alpha ~1)
over image-derived ambient`. The only signal that changes is focus and a little brightness, and both
change slowly. That is why the edge of the hero image will not be visible.

## Savelin implementation (this change)

`DetailScreen.kt` now draws, in order, above the untouched v1.7.8 ambient backdrop:

- `HeroAmbientHalo` — the same `ambience.image` bitmap, enlarged (`HERO_HALO_SCALE = 1.46`), heavily
  blurred (`HERO_HALO_BLUR_DP = 60`), nearly opaque, extending `HERO_HALO_OVERSHOOT_DP = 220` below
  the hero and fading out over its last `HERO_HALO_TAIL_FEATHER_DP = 120`. The halo's tail is checked
  to begin **below** the hero edge, so it is at full strength at the seam.
- the sharp hero (`AsyncImage`, unchanged crop/aspect rules) with an `Offscreen`/`DstIn` alpha mask:
  opaque until `1 - HERO_EDGE_FEATHER_FRACTION = 0.66` of its height, then a continuous linear ramp to
  transparent at the very bottom (`HERO_EDGE_FEATHER_FRACTION = 0.34`).
- a soft wash sampled from `ambience.tone.base` (`HERO_EDGE_WASH_ALPHA = 0.26`) that is transparent at
  both ends of the feather band, so it cannot draw a line where it starts or ends.

The ambient background from v1.7.8 (`NowPlayingBackdrop.kt`, `ArtworkAmbience.kt`) is unchanged, as
are the Back affordance, crossfade, caching and `SavelinArtwork` diagnostics.

`ArtworkAmbienceTest.kt` asserts the properties that make the edge invisible: the feather is long,
its alpha is monotonic and sampled continuously with no step larger than 0.02, it reaches exactly 1
and 0 at the right places, the halo is oversized and still opaque at the seam, and the wash is dark
and derived from the artwork tone.

## Replaced attempt

Commits `39369f1`, `2a65ade`, `7e791d2` blurred a **short 26% band over the sharp hero** and then laid
a tone fade on top. That introduced a new boundary where the band began (sharp -> blurred over a short
distance) and where the tone fade ended, and the band's cropped/zoomed bitmap did not align with the
sharp artwork. It was reverted in `a6806ac` and is replaced here by the researched full-image feather
+ oversized-halo approach.
