# Why the hero artwork has no visible lower edge

Question: when the sharp hero artwork meets the ambient background, why is there no seam, and how
does Savelin achieve that without a card, border or hard cut?

Research date: 2026-11-23. Read-only design study; the implementation below is our own Compose
code, based on the published technique and the visual goal.

## Mechanism

The boundary is removed by fading the **alpha of the sharp artwork itself** to zero over a long
distance while an **enlarged, blurred copy of the same image** is already behind it and continues
past the hero's lower edge:

1. One source image feeds both layers, so hues match exactly at the transition.
2. The hero mask goes from fully opaque to fully transparent over `HERO_EDGE_FEATHER_FRACTION`
   (0.36) of its height. In Compose this is an offscreen layer with a `DstIn` vertical gradient.
   Because the ramp is continuous and reaches exactly zero, there is no row of pixels where the
   sharp layer abruptly stops (see the `mask-image` alpha discussion in MDN, source 5 of
   `docs/APPLE-MUSIC-RECON.md`).
3. The halo is a copy of the same artwork scaled by `HALO_SCALE` (1.46) and blurred by
   `HALO_BLUR_DP` (58dp). It starts above the hero edge, stays fully opaque through it, overshoots
   by `HALO_OVERSHOOT_DP` (210dp) and fades out over its last `HALO_TAIL_DP` (120dp), so its own
   fade cannot create a second boundary.
4. A dark wash sampled from the artwork base is transparent at both ends of the feather band, so
   it cannot draw a line where it starts or ends.
5. There is no card, border or shadow anywhere in the composition.

At the hero's lower edge the composition is simply `sharp(alpha -> 0)` over `blurred-same-image`
over the ambient field. Focus and brightness change slowly; nothing snaps.

## Implementation

- `heroEdgeAlpha()` in `ArtworkPalette.kt` defines the long continuous ramp.
- `haloTailStart()` / `haloAlpha()` keep the halo opaque at the hero edge and fade it below.
- `HeroHalo()` and `SharpHero()` in `DetailScreen.kt` apply the two `DstIn` masks.
- `HeroEdgeWash()` adds the image-derived, double-transparent wash.
- `AmbientCompositionTest.kt` asserts a long monotonic ramp with no perceptible step, exact 1/0
  endpoints, and a halo that is oversized and still opaque at the edge.
