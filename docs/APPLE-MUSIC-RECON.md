# Apple Music "Now Playing" background recon

Read-only design research for the rebuilt Savelin detail screen. No player controls, transport,
lyrics, progress bar or Apple Music navigation are in scope — only the **background language**.

Sources (fetched 2026-11-23):

1. Aadish Verma, *Reverse engineering Apple Music's background gradient* — https://www.aadishv.dev/music
2. Sam Henri Gold's FLEX breakdown of the Apple Music visualiser (linked from source 1) — https://x.com/samhenrigold/status/1765220903963574290
3. Blake Crosley, *Apple Music: Editorial Voice Meets Spatial Sound* — https://blakecrosley.com/guides/design/apple-music
4. Jana, *How to Create Apple Music's Background Blur Effect Using Blurhash* — https://hackernoon.com/how-to-create-apple-musics-background-blur-effect-using-blurhash
5. StackOverflow, *How did Apple create the blur behind the album cover in Apple Music?* — https://stackoverflow.com/questions/41094962/how-did-apple-create-the-blur-behind-the-album-cover-in-apple-music
6. StackOverflow, *SwiftUI – How does Apple Music create their background gradient?* — https://stackoverflow.com/questions/71257653/swiftui-how-does-apple-music-create-their-background-gradient
7. Apple HIG, *Playing audio* — https://developer.apple.com/design/human-interface-guidelines/playing-audio
8. r/AppleMusic discussion of the full-screen album-art setting — https://www.reddit.com/r/AppleMusic/comments/1cr8hn6/fullscreen_album_art_within_apple_music_app/

## What the real screen does

- **The artwork is the source of the design, not decoration.** Apple Music extracts two to three
  dominant colours from the cover and tints the whole Now Playing environment with them; the art is
  "the generative source of the visual design" (source 3). Extracted colours are clamped so the
  result is not garish, and the UI picks a contrasting text colour from them.
- **It is not a single flat gradient.** Sam Henri Gold's reverse engineering (source 2, quoted in
  source 1) describes a mesh/fluid gradient made by layering *several copies of the same artwork*,
  twisting each copy and then applying a blur shader, all on the GPU. The web implementation that
  was decompiled in source 1 stacks four square copies at roughly 25%, 50%, 80% and 125% of the
  viewport width, oversaturates the artwork, applies a twist and then a Gaussian/Kawase blur. The
  mobile version differs in detail, but the recipe is the same: multiple transformed copies of the
  art + blur.
- **The classic approximation is simpler and still reads correctly:** duplicate the album image,
  scale it up, apply a strong Gaussian blur (often plus an exposure/saturation bump), and put the
  crisp artwork on top (sources 5 and 6). This is why an enlarged, blurred copy of the same image
  feels like Apple Music while a generic gradient does not.
- **Low-resolution artwork can drive the blur.** The blurhash technique (source 4) encodes a tiny
  32×32 representation of the cover and renders it stretched as the background. It confirms that a
  heavily downsampled copy is enough for an authentic ambient background and is very cheap on
  mobile.
- **The lyrics/immersive view shows the same idea at maximum strength:** artwork used as a full-
  bleed background with a very heavy blur (`backdrop-filter: blur(60px) saturate(1.5)` in source 3's
  CSS sketch) plus a dark wash so content stays readable.
- **Full-bleed art is a first-class mode.** Apple Music offers a full-screen album-art presentation
  (source 8), and the art sits in the upper portion of the screen with the colour environment
  flowing out of it; there are no cards, borders or shadows around the hero image (source 3).
- **Motion is explicit.** Apple's implementation honours `prefers-reduced-motion` (source 1 caps the
  animation ticker and branches on the media query), so a reduced-motion user should get an instant
  change rather than a crossfade.

## Visual recipe to reproduce

1. Root is full-screen and derived from the artwork base tone — no app-default navy, no theme fill.
2. A heavily downsampled copy of the artwork is enlarged (~1.3–1.4×), blurred heavily and stretched
   to fill the viewport, with saturation retained so colour bleeds out of the sharp image.
3. Two to three soft radial colour blooms, sampled from the artwork's own palette, add depth where
   the blurred image is dark. They are diffuse light, not chips or panels.
4. The bottom gets a second, even more blurred copy of the same image plus a dark fade built from
   the artwork's dominant dark tone. This is the "heavy bottom diffusion" that keeps future content
   legible without drawing a solid bar.
5. A subtle top-to-bottom readability gradient sits above all background layers.
6. The sharp original artwork is drawn full-width in the upper portion with its natural aspect
   ratio (cropped only when an extreme portrait would otherwise overflow), with **no** card,
   container, border or shadow.
7. Overlays are minimal: only a small safe Back affordance for now. No controls of any kind.
8. Bookmark switches crossfade the whole ambient; Reduce motion swaps instantly. Palette and blurred
   bitmap are cached per bookmark so revisits are immediate and no work repeats on the main thread.
9. When no stored image exists, generate a deterministic multi-colour ambient from platform/site —
   never a flat colour fallback.

## What we deliberately do NOT copy

Transport buttons, scrubber/progress, lyrics, shuffle/repeat/volume, AirPlay, "Up Next", Apple
Music's tab/nav chrome, the album-art card frame with drop shadow, or any of Savelin's previous
glass detail system (glass circles, chips, menus, reminder dialogs, notes fields).
