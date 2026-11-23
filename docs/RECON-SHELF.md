# RECON: Shelf URL preview method (iambhvsh/shelf)

Scope study of <https://github.com/iambhvsh/shelf> (branch `app`, GPL-3.0), a fork of Savr,
and the library it credits for metadata extraction,
[Android-Link-Preview](https://github.com/vishalkumarsinghvi/Android-Link-Preview).
Goal: reproduce Shelf's *method* as Savelin's primary URL preview step, then fall back to the
existing resolver chain. No Shelf code was copied into this repository.

## What Shelf actually does (source, not just README)

Shelf's fetch feature is `app/src/main/java/in/iambhvsh/shelf/features/fetch/`
(`LinkMetadataParser.kt`, `LinkPreviewService.kt`, `NetworkFetcher.kt`, `MetadataExtractor.kt`,
`UrlUtils.kt`, `ContentTypeDetector.kt`, `Data.kt`). It is a vendored, Kotlin port of
Android-Link-Preview (same seven class names and structure), modified for Shelf.

### Request pattern ("background crawling that handles basic anti-bot protection")

- **User-Agent retry loop, in this exact order** (Shelf's `LinkMetadataParser.AGENTS`):
  1. `WhatsApp/2.24.8.76 A`
  2. `Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/96.0.4664.45 Safari/537.36`
  Each agent is tried in turn; the first result with a non-blank title **or** description
  (and not the literal string `null`) wins. This is the "basic anti-bot protection" handling:
  messaging-app and crawler identities often receive server-rendered tags that a plain browser UA does not.
  (The upstream library tries seven agents: WhatsApp, `Mozilla/5.0`, two `facebookexternalhit`
  variants, `Twitterbot/1.0`, desktop Chrome Linux, desktop Chrome Mac. Shelf trimmed it to two.)
- **Direct HTTP** via OkHttp (`NetworkFetcher`): `followRedirects(true)`, `followSslRedirects(true)`,
  `connectTimeout(5s)`, `readTimeout(5s)`, shared singleton client.
- **Headers**: `User-Agent: <agent>`, `Accept-Language: en-US,en;q=0.9`. No cookies, no Referer.
- **Response gate**: only `response.isSuccessful` **and** `Content-Type` containing `text/html`;
  body is streamed to `Jsoup.parse(stream, null, finalUrl)` with a **final post-redirect URL as base URI**.
- **No third-party preview service**: the page itself is fetched directly. No proxy/Microlink-style API.
- **Caching**: none in the fetch layer (no OkHttp cache, no in-memory preview cache). The extracted
  `imageUrl` is persisted on the bookmark; Coil loads that remote URL. Savelin deliberately keeps its
  stronger local WebP + blurhash copy instead.

### Metadata fields parsed (`MetadataExtractor`)

- title: `meta[property=og:title]`, fallback `document.title()`
- description: `meta[property=og:description]`, fallback `meta[name=description]`
- image **selection order** (first non-blank wins):
  1. `meta[property=og:image]`
  2. `meta[name=twitter:image]`
  3. `meta[itemprop=image]`
  4. `meta[name=image]`
  5. `link[rel=image_src]` `href`
  6. `link[rel=apple-touch-icon]` `href`
  7. `link[rel=icon]` `href`
  8. first `img[src][width][height]` with both width >= 200 and height >= 200
- relative / protocol-relative image URLs resolved with `UrlUtils.makeAbsoluteUrl` against the final URL.
- `ContentTypeDetector` classifies the URL as WEBPAGE / PROFILE / VIDEO from URL patterns; it affects
  presentation only, not which tags are read.

## Licence decision

| Component | Licence | Evidence |
| --- | --- | --- |
| Shelf (`iambhvsh/shelf`) | **GPL-3.0** | GitHub API `license.spdx_id = GPL-3.0`; README "GNU General Public License v3.0". |
| Android-Link-Preview | MIT *per README* | README: "licensed under the MIT License". No `LICENSE` file exists in the repo; JitPack coordinates `com.github.vishalkumarsinghvi:Android-Link-Preview:1.1` (`publishing` block) resolve (HTTP 200). |
| Savelin | permissive third-party mix (MIT/Apache-2.0/ISC), see `NOTICE` | no GPL component is currently bundled. |

**Decision: do not bundle the library and do not copy Shelf's source.** The method to reproduce is
Shelf's *GPL-3.0* modified port (two UAs, 5s timeouts, text/html gate, extra image selectors), not the
MIT upstream library; pinning `1.1` would not give Shelf's behaviour. The upstream MIT claim is also
not backed by a committed licence file. Accordingly `ShelfStyleResolver` is an **independent
implementation in Kotlin using the OkHttp + Jsoup dependencies Savelin already has** — zero new
dependencies, no new repository, no JitPack artifact, no GPL code copied. Attribution is recorded in
`NOTICE` and here.

## Savelin integration

- `ShelfStyleResolver` is the **first** step in `PreviewFetcher` (before `PlatformFastPaths` and the
  two `OpenGraphResolver` passes) for every host **except Instagram**. Instagram links
  (`instagram.com`, `www.instagram.com`, `m.instagram.com`, including normalized variants) use the
  original resolver chain as the first/primary path and skip Shelf entirely.
- If the primary step yields a usable image, the remaining chain is skipped (one fetch, no duplicate
  requests).
- If it yields metadata but no image — or no usable result at all — the current chain runs, including
  the mobile/crawler retry and the opt-in proxy. `merge()` prefers primary metadata and appends all
  image candidates.
- If the primary image URL exists but `ImageStore` cannot download/decode it, `LinkFetchWorker`
  calls `PreviewFetcher.fetchFallback()` for current-system image URLs and stores the first that works.
- Local WebP re-encode, blurhash and blur preview are unchanged; `MetadataCleaner` still owns all
  display cleanup.
- Diagnostics: `onBreadcrumb(step, outcome, host)` emits host-only INFO breadcrumbs (no URL/title)
  through `DiagnosticsLog` for each attempt and its redacted reason.

## Attribution

Method concept: Shelf (<https://github.com/iambhvsh/shelf>, GPL-3.0) and Android-Link-Preview
(<https://github.com/vishalkumarsinghvi/Android-Link-Preview>, MIT per README). Savelin's
implementation is original code written for this project; no source from either project is bundled.
