# Savelin

A local-only Android app: share a link from any app and it appears as a Pinterest-style preview tile. No account, no analytics, no cloud, no tracking. Liquid-glass UI with a floating pill navigation bar and a full Appearance & Motion settings screen.

## What it does

- **Share to save.** Android's share sheet sends `text/plain` links (plus captions) straight into Savelin. Paste a link with **+** as well. The first URL in shared text is extracted, normalised and de-duplicated.
- **Universal on-device previews.** Fetches the saved page directly and extracts Open Graph, Twitter Cards, JSON-LD, article tags, canonical links, oEmbed discovery and in-page images with a charset-aware parser. A mobile User-Agent is tried first, with a crawler-identity retry for blocked pages. Instagram/YouTube/Reddit fast paths are quality accelerators only; every other site uses the same generic path. Adult and non-adult sites are treated identically.
- **Images stay on the device.** The preview image is downloaded, re-encoded as WebP (max 1080 px), stored in app-private storage with a blurhash, and never referenced by its expiring remote URL again.
- **Graceful fallbacks.** If a login wall or empty tags block the preview, Savelin shows a generated tile with **Refetch image** and **Add custom image** actions. An opt-in proxy template is available in Settings for the hard cases.
- **Organise.** Important, archived, tags, collections, notes, search (SQLite FTS), and reminders with notifications.
- **Appearance & Motion.** Five glass presets, five motion styles, transition picker, animation speed, haptics, hold-to-preview delay/effect, reduce-transparency/reduce-motion, low-end device mode and an FPS overlay.
- **Privacy controls.** Offline mode, a network-transparency log of every host contacted, biometric lock, and ZIP backup/export/import that keeps your data on your device.

> Honest limit: no account-free method can fetch previews for private accounts, login-walled posts, or content a platform deliberately hides from link-preview requests. Broad coverage comes from open standards, not from a magic API. Savelin reduces failures with extra fallbacks and manual images, but cannot guarantee a real image for every link.

## Why there is no public preview API

We evaluated hosted preview services (Microlink, Iframely Cloud, LinkPreview and similar). None offers unlimited, no-account access to thousands of sites: they require API keys, impose rate limits, and — decisively — the service would receive every URL you save, which contradicts this app's privacy promises. We therefore rejected all third-party preview and favicon services.

The MIT-licensed `link-preview-js` approach is the right model: fetch the page yourself and parse Open Graph/meta tags and content types locally. Savelin implements that approach in Kotlin inside `core-network`:

- **Open standards only:** Open Graph, Twitter Cards, JSON-LD, microdata-style `<time>`, `<link rel=canonical/image_src>`, and oEmbed discovered from and fetched on the visited site (never a public endpoint). Sites that publish none of these still get a best-effort parse of `<title>`, meta description and the largest content image.
- **Charset correctness:** the HTTP `Content-Type` charset, then `<meta charset>`/`http-equiv`, decides how raw bytes are decoded (UTF-8, Shift_JIS, EUC-JP, …) before Jsoup parses them. Bytes are never blindly decoded as UTF-8.
- **Media types:** HTML is parsed; a direct image is its own preview; video/audio/PDF/documents get a filename title and fallback tile; unknown types get a best-effort parse.
- **Images and icons:** candidates are ranked (og:image with dimensions, twitter:image, JSON-LD, image_src, then largest content image), tiny/tracking pixels are rejected, and the chosen image is downloaded at save time to WebP with a blurhash. Favicons come from the site's own `rel=icon`/apple-touch-icon/manifest/favicon.ico and are stored per host.
- **Tags:** `article:tag`, keywords, JSON-LD keywords and hashtags are lowercased, trimmed, deduplicated, capped at eight and added to the tag table automatically.

The optional proxy in Settings remains strictly opt-in; it is never used by default. No API key, SDK or phone-home code is included.

## Privacy guarantees

1. No account is required. An optional Google sign-in exists only for Drive sync (off by default).
2. No analytics, crash reporters, ads, or third-party SDKs that phone home.
3. All data is stored on-device; only if you turn on Drive sync is an end-to-end encrypted copy stored in your own Google Drive. There is no developer-run server.
4. The only network traffic is fetching pages you save, their own images/icons/manifests/oEmbed endpoints, and (only if you enable it) an opt-in proxy. There is no third-party preview API by default. Every host is logged in **Settings → Network transparency**; **Offline mode** disables fetches entirely.
5. Tracking parameters (`utm_*`, `fbclid`, `igsh`, …) are stripped from saved URLs.
6. Android cloud auto-backup is disabled (`allowBackup=false`), cleartext traffic is denied, no cookies are stored, and redirects are capped.

`INTERNET` is declared because link previews require it. This is compensated for by the transparency log, Offline mode, and the rule that no traffic happens unless a link is being fetched. An optional third-party proxy is off by default with a clear warning.

## Architecture

Kotlin, Jetpack Compose, Material 3, MVVM/UDF, single activity. Modules:

| Module | Purpose |
| --- | --- |
| `app` | Screens, share target, WorkManager workers, alarms, backup, DI-free ViewModels |
| `backdrop` | Vendored Kyant0 liquid-glass backdrop library (Apache-2.0) |
| `core-ui-glass` | Glass design system, motion engine, aurora wallpaper, tabs, buttons, overlays |
| `core-data` | Room entities/DAOs/FTS, DataStore settings, repositories |
| `core-network` | URL normaliser, universal charset-aware metadata/oEmbed parser, platform fast paths, favicon + image stores with blurhash |

Storage is Room + FTS4; settings are DataStore Preferences; images are WebP files in app-private storage. Fetching runs in WorkManager; reminders use AlarmManager + notifications.

## Build (GitHub Actions only)

The developer machine is low-end; **never build locally**. Every lint, test and packaging step runs in [`.github/workflows/android.yml`](.github/workflows/android.yml):

- `check` — `lintDebug testDebugUnitTest`.
- `build` — one release APK per ABI: `arm64-v8a`, `armeabi-v7a`, `x86_64`, `x86`, `universal` (`-Pabi=<abi>`). The matrix is throttled and retries transient Maven 429s.
- `release` — on `v*` tags only, publishes all five APKs with a generated release body containing a download table, sizes, SHA-256 checksums, and a grouped changelog.

The Gradle wrapper (Gradle 9.8.2) and all dependency versions are pinned in `gradle/libs.versions.toml`. Compile SDK 37, min SDK 29, Java 21 in CI (toolchain 17 bytecode).

### Signing

Release builds are signed in CI with a keystore supplied through repository secrets:

| Secret | Value |
| --- | --- |
| `KEYSTORE_BASE64` | base64 of the release `.jks` |
| `KEYSTORE_PASSWORD` | store password |
| `KEY_ALIAS` | key alias |
| `KEY_PASSWORD` | key password |

The keystore itself lives in the untracked `secrets/` directory and is **never committed**. Without these secrets CI falls back to a debug-signed APK, which is not suitable for distribution.

## Install

Download the APK for your device from the Releases page. `universal` works everywhere; `arm64-v8a` is the usual choice for modern phones. Verify with `sha256sum savelin-*.apk` against the checksums in the release notes.

## Licences and references

See [`NOTICE`](NOTICE) and [`third_party/`](third_party/) for Artefact (MIT/Apache-2.0), AndroidLiquidGlass (Apache-2.0) and PostVault (MIT) licences. Design and motion findings are in [`docs/RECON.md`](docs/RECON.md); the PostVault study and code recon are in [`docs/POSTVAULT.md`](docs/POSTVAULT.md) and [`docs/POSTVAULT-RECON.md`](docs/POSTVAULT-RECON.md).
